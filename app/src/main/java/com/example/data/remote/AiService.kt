package com.example.data.remote

import com.example.data.model.AttachmentInfo
import com.example.data.model.ChatMessageEntity
import com.example.data.model.ProviderType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

object AiService {
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun sendMessage(
        provider: ProviderType,
        modelId: String,
        prompt: String,
        attachments: List<AttachmentInfo>,
        history: List<ChatMessageEntity>,
        apiKey: String,
        systemPrompt: String,
        temperature: Float,
        customBaseUrl: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            when (provider) {
                ProviderType.GEMINI -> callGemini(
                    modelId = modelId,
                    prompt = prompt,
                    attachments = attachments,
                    history = history,
                    apiKey = apiKey,
                    systemPrompt = systemPrompt,
                    temperature = temperature
                )
                ProviderType.GROQ -> callOpenAiCompatible(
                    endpointUrl = provider.defaultBaseUrl,
                    modelId = modelId,
                    prompt = prompt,
                    attachments = attachments,
                    history = history,
                    apiKey = apiKey,
                    systemPrompt = systemPrompt,
                    temperature = temperature,
                    extraHeaders = emptyMap()
                )
                ProviderType.OPENROUTER -> callOpenAiCompatible(
                    endpointUrl = provider.defaultBaseUrl,
                    modelId = modelId,
                    prompt = prompt,
                    attachments = attachments,
                    history = history,
                    apiKey = apiKey,
                    systemPrompt = systemPrompt,
                    temperature = temperature,
                    extraHeaders = mapOf(
                        "HTTP-Referer" to "https://omnichat.android",
                        "X-Title" to "OmniChat"
                    )
                )
                ProviderType.POLLINATIONS -> callPollinations(
                    modelId = modelId,
                    prompt = prompt,
                    attachments = attachments,
                    history = history,
                    systemPrompt = systemPrompt,
                    temperature = temperature
                )
                ProviderType.CUSTOM -> {
                    val url = if (customBaseUrl.isNotBlank()) customBaseUrl else "https://api.openai.com/v1/chat/completions"
                    callOpenAiCompatible(
                        endpointUrl = url,
                        modelId = modelId,
                        prompt = prompt,
                        attachments = attachments,
                        history = history,
                        apiKey = apiKey,
                        systemPrompt = systemPrompt,
                        temperature = temperature,
                        extraHeaders = emptyMap()
                    )
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun callGemini(
        modelId: String,
        prompt: String,
        attachments: List<AttachmentInfo>,
        history: List<ChatMessageEntity>,
        apiKey: String,
        systemPrompt: String,
        temperature: Float
    ): Result<String> {
        if (apiKey.isBlank()) {
            return Result.failure(IllegalStateException("Gemini API key is required. Please add it in Settings or via Secrets."))
        }

        // Build Gemini contents array
        val contentsArray = JSONArray()

        // Include recent history (last 10 messages)
        val recentHistory = history.takeLast(10)
        for (msg in recentHistory) {
            val role = if (msg.role == "user") "user" else "model"
            val partsArray = JSONArray()
            partsArray.put(JSONObject().put("text", msg.content))
            contentsArray.put(JSONObject().put("role", role).put("parts", partsArray))
        }

        // Current user message parts
        val currentParts = JSONArray()

        // Process attachments
        var textContextFromFiles = ""
        for (att in attachments) {
            val file = File(att.localUri)
            if (att.isImage) {
                val base64Data = FileUtils.convertImageToBase64(file)
                if (base64Data != null) {
                    val inlineData = JSONObject().apply {
                        put("mimeType", if (att.mimeType.isNotBlank()) att.mimeType else "image/jpeg")
                        put("data", base64Data)
                    }
                    currentParts.put(JSONObject().put("inlineData", inlineData))
                }
            } else {
                val docText = FileUtils.readFullTextContent(file)
                textContextFromFiles += "\n\n--- Attached File: ${att.name} (${att.mimeType}) ---\n$docText\n--- End of File ---\n"
            }
        }

        val fullTextPrompt = if (textContextFromFiles.isNotEmpty()) {
            "$textContextFromFiles\n\nUser Query: $prompt"
        } else {
            prompt
        }
        currentParts.put(JSONObject().put("text", fullTextPrompt))

        contentsArray.put(JSONObject().put("role", "user").put("parts", currentParts))

        val rootJson = JSONObject().apply {
            put("contents", contentsArray)
            if (systemPrompt.isNotBlank()) {
                val sysParts = JSONArray().put(JSONObject().put("text", systemPrompt))
                put("systemInstruction", JSONObject().put("parts", sysParts))
            }
            put("generationConfig", JSONObject().put("temperature", temperature))
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelId:generateContent?key=$apiKey"
        val request = Request.Builder()
            .url(url)
            .post(rootJson.toString().toRequestBody(jsonMediaType))
            .build()

        val response = httpClient.newCall(request).execute()
        val responseBody = response.body?.string().orEmpty()

        if (!response.isSuccessful) {
            val errorMsg = parseErrorMessage(responseBody, "Gemini error (${response.code})")
            return Result.failure(Exception(errorMsg))
        }

        val json = JSONObject(responseBody)
        val candidates = json.optJSONArray("candidates")
        if (candidates != null && candidates.length() > 0) {
            val candidate = candidates.getJSONObject(0)
            val content = candidate.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            if (parts != null && parts.length() > 0) {
                val reply = parts.getJSONObject(0).optString("text")
                if (reply.isNotBlank()) {
                    return Result.success(reply)
                }
            }
        }

        return Result.success("No text generated in response.")
    }

    private suspend fun callOpenAiCompatible(
        endpointUrl: String,
        modelId: String,
        prompt: String,
        attachments: List<AttachmentInfo>,
        history: List<ChatMessageEntity>,
        apiKey: String,
        systemPrompt: String,
        temperature: Float,
        extraHeaders: Map<String, String>
    ): Result<String> {
        val messagesArray = JSONArray()

        if (systemPrompt.isNotBlank()) {
            messagesArray.put(JSONObject().apply {
                put("role", "system")
                put("content", systemPrompt)
            })
        }

        val recentHistory = history.takeLast(10)
        for (msg in recentHistory) {
            messagesArray.put(JSONObject().apply {
                put("role", if (msg.role == "user") "user" else "assistant")
                put("content", msg.content)
            })
        }

        // Build current message with attachment support
        var fileTextAppendix = ""
        val imageBase64List = mutableListOf<String>()

        for (att in attachments) {
            val file = File(att.localUri)
            if (att.isImage) {
                val b64 = FileUtils.convertImageToBase64(file)
                if (b64 != null) {
                    imageBase64List.add(b64)
                }
            } else {
                val text = FileUtils.readFullTextContent(file)
                fileTextAppendix += "\n\n--- [Attached Document: ${att.name}] ---\n$text\n--- [End of Document] ---\n"
            }
        }

        val fullText = if (fileTextAppendix.isNotEmpty()) {
            "$fileTextAppendix\nUser Query: $prompt"
        } else {
            prompt
        }

        if (imageBase64List.isNotEmpty()) {
            // Multimodal content array for OpenAI format
            val contentArray = JSONArray()
            contentArray.put(JSONObject().put("type", "text").put("text", fullText))
            for (b64 in imageBase64List) {
                val imgObj = JSONObject().apply {
                    put("type", "image_url")
                    put("image_url", JSONObject().put("url", "data:image/jpeg;base64,$b64"))
                }
                contentArray.put(imgObj)
            }
            messagesArray.put(JSONObject().apply {
                put("role", "user")
                put("content", contentArray)
            })
        } else {
            messagesArray.put(JSONObject().apply {
                put("role", "user")
                put("content", fullText)
            })
        }

        val rootJson = JSONObject().apply {
            put("model", modelId)
            put("messages", messagesArray)
            put("temperature", temperature)
        }

        val reqBuilder = Request.Builder()
            .url(endpointUrl)
            .post(rootJson.toString().toRequestBody(jsonMediaType))

        if (apiKey.isNotBlank()) {
            reqBuilder.addHeader("Authorization", "Bearer $apiKey")
        }

        for ((k, v) in extraHeaders) {
            reqBuilder.addHeader(k, v)
        }

        val response = httpClient.newCall(reqBuilder.build()).execute()
        val responseBody = response.body?.string().orEmpty()

        if (!response.isSuccessful) {
            val errorMsg = parseErrorMessage(responseBody, "Request failed (${response.code})")
            return Result.failure(Exception(errorMsg))
        }

        val json = JSONObject(responseBody)
        val choices = json.optJSONArray("choices")
        if (choices != null && choices.length() > 0) {
            val firstChoice = choices.getJSONObject(0)
            val msg = firstChoice.optJSONObject("message")
            val content = msg?.optString("content")
            if (!content.isNullOrBlank()) {
                return Result.success(content)
            }
        }

        return Result.success("No response content received.")
    }

    private suspend fun callPollinations(
        modelId: String,
        prompt: String,
        attachments: List<AttachmentInfo>,
        history: List<ChatMessageEntity>,
        systemPrompt: String,
        temperature: Float
    ): Result<String> {
        // First try the OpenAI-compatible endpoint
        val openAiRes = callOpenAiCompatible(
            endpointUrl = "https://text.pollinations.ai/openai/chat/completions",
            modelId = modelId,
            prompt = prompt,
            attachments = attachments,
            history = history,
            apiKey = "",
            systemPrompt = systemPrompt,
            temperature = temperature,
            extraHeaders = emptyMap()
        )

        if (openAiRes.isSuccess) {
            return openAiRes
        }

        // Fallback to simple direct text GET endpoint for maximum reliability
        return try {
            var fileContext = ""
            for (att in attachments) {
                if (!att.isImage) {
                    val file = File(att.localUri)
                    fileContext += "\nFile (${att.name}): " + FileUtils.readFullTextContent(file, 20_000)
                }
            }

            val queryPrompt = if (fileContext.isNotEmpty()) "$prompt $fileContext" else prompt
            val encodedPrompt = java.net.URLEncoder.encode(queryPrompt, "UTF-8")
            val encodedSystem = java.net.URLEncoder.encode(systemPrompt, "UTF-8")
            val url = "https://text.pollinations.ai/$encodedPrompt?model=$modelId&system=$encodedSystem"

            val req = Request.Builder().url(url).get().build()
            val resp = httpClient.newCall(req).execute()
            val text = resp.body?.string().orEmpty()
            if (resp.isSuccessful && text.isNotBlank()) {
                Result.success(text)
            } else {
                openAiRes // Return original error
            }
        } catch (e: Exception) {
            openAiRes
        }
    }

    private fun parseErrorMessage(rawJson: String, defaultMsg: String): String {
        return try {
            val json = JSONObject(rawJson)
            if (json.has("error")) {
                val errorObj = json.get("error")
                if (errorObj is JSONObject) {
                    val msg = errorObj.optString("message")
                    if (msg.isNotBlank()) return msg
                } else if (errorObj is String && errorObj.isNotBlank()) {
                    return errorObj
                }
            }
            if (json.has("message")) {
                val msg = json.optString("message")
                if (msg.isNotBlank()) return msg
            }
            defaultMsg
        } catch (e: Exception) {
            if (rawJson.length in 1..200) rawJson else defaultMsg
        }
    }
}
