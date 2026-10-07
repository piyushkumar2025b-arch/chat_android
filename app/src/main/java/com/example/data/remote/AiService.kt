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

    @Volatile
    private var activeCall: okhttp3.Call? = null

    fun cancelActiveRequest() {
        try {
            activeCall?.cancel()
            activeCall = null
        } catch (_: Exception) {}
    }

    private fun executeRequest(request: Request): okhttp3.Response {
        val call = httpClient.newCall(request)
        activeCall = call
        return try {
            call.execute()
        } finally {
            if (activeCall === call) {
                activeCall = null
            }
        }
    }

    suspend fun sendMessage(
        provider: ProviderType,
        modelId: String,
        prompt: String,
        attachments: List<AttachmentInfo>,
        history: List<ChatMessageEntity>,
        apiKey: String,
        systemPrompt: String,
        temperature: Float,
        customBaseUrl: String,
        supportsVision: Boolean = false,
        maxTokens: Int = 8192
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
                    temperature = temperature,
                    maxTokens = maxTokens
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
                    extraHeaders = emptyMap(),
                    supportsVision = supportsVision,
                    maxTokens = maxTokens
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
                    ),
                    supportsVision = supportsVision,
                    maxTokens = maxTokens
                )
                ProviderType.POLLINATIONS -> callPollinations(
                    modelId = modelId,
                    prompt = prompt,
                    attachments = attachments,
                    history = history,
                    systemPrompt = systemPrompt,
                    temperature = temperature,
                    maxTokens = maxTokens
                )
                ProviderType.CEREBRAS -> callOpenAiCompatible(
                    endpointUrl = provider.defaultBaseUrl,
                    modelId = modelId,
                    prompt = prompt,
                    attachments = attachments,
                    history = history,
                    apiKey = apiKey,
                    systemPrompt = systemPrompt,
                    temperature = temperature,
                    extraHeaders = emptyMap(),
                    supportsVision = false,
                    maxTokens = maxTokens
                )
                ProviderType.HUGGINGFACE -> callOpenAiCompatible(
                    endpointUrl = provider.defaultBaseUrl,
                    modelId = modelId,
                    prompt = prompt,
                    attachments = attachments,
                    history = history,
                    apiKey = apiKey,
                    systemPrompt = systemPrompt,
                    temperature = temperature,
                    extraHeaders = emptyMap(),
                    supportsVision = false,
                    maxTokens = maxTokens
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
                        extraHeaders = emptyMap(),
                        supportsVision = supportsVision,
                        maxTokens = maxTokens
                    )
                }
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
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
        temperature: Float,
        maxTokens: Int = 8192
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
            val isPdf = att.mimeType == "application/pdf" || att.name.endsWith(".pdf", true)
            if (att.isImage) {
                val base64Data = FileUtils.convertImageToBase64(file)
                if (base64Data != null) {
                    val inlineData = JSONObject().apply {
                        put("mimeType", if (att.mimeType.isNotBlank()) att.mimeType else "image/jpeg")
                        put("data", base64Data)
                    }
                    currentParts.put(JSONObject().put("inlineData", inlineData))
                }
            } else if (isPdf) {
                val docText = FileUtils.readFullTextContent(file)
                textContextFromFiles += "\n\n--- Attached Document: ${att.name} (PDF) ---\n$docText\n--- End of Document ---\n"
                val pdfImgBase64 = FileUtils.convertPdfPageToBase64(file, 0)
                if (pdfImgBase64 != null) {
                    val inlineData = JSONObject().apply {
                        put("mimeType", "image/jpeg")
                        put("data", pdfImgBase64)
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
            put("generationConfig", JSONObject().apply {
                put("temperature", temperature)
                put("maxOutputTokens", maxTokens.coerceIn(1024, 8192))
            })
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelId:generateContent"
        val request = Request.Builder()
            .url(url)
            .addHeader("x-goog-api-key", apiKey)
            .post(rootJson.toString().toRequestBody(jsonMediaType))
            .build()

        val (responseCode, isSuccess, responseBody) = executeRequest(request).use { resp ->
            Triple(resp.code, resp.isSuccessful, resp.body?.string().orEmpty())
        }

        if (!isSuccess) {
            val errorMsg = parseErrorMessage(responseBody, "Gemini error ($responseCode)")
            return Result.failure(Exception(errorMsg))
        }

        val json = JSONObject(responseBody)
        val promptFeedback = json.optJSONObject("promptFeedback")
        val blockReason = promptFeedback?.optString("blockReason")
        if (!blockReason.isNullOrBlank()) {
            return Result.failure(Exception("Generation blocked by safety policy: $blockReason"))
        }

        val candidates = json.optJSONArray("candidates")
        if (candidates != null && candidates.length() > 0) {
            val candidate = candidates.getJSONObject(0)
            val finishReason = candidate.optString("finishReason")
            val content = candidate.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val textBuilder = StringBuilder()
            if (parts != null) {
                for (i in 0 until parts.length()) {
                    val p = parts.getJSONObject(i)
                    if (!p.optBoolean("thought", false)) {
                        val text = p.optString("text")
                        if (text.isNotBlank()) {
                            textBuilder.append(text)
                        }
                    }
                }
            }
            if (textBuilder.isNotBlank()) {
                return Result.success(textBuilder.toString())
            }
            if (finishReason == "SAFETY" || finishReason == "RECITATION") {
                return Result.failure(Exception("Response blocked due to $finishReason policy."))
            }
        }

        return Result.failure(Exception("No response content generated by Gemini."))
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
        extraHeaders: Map<String, String>,
        supportsVision: Boolean = false,
        maxTokens: Int = 8192
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
            val isPdf = att.mimeType == "application/pdf" || att.name.endsWith(".pdf", true)
            if (att.isImage) {
                if (supportsVision) {
                    val b64 = FileUtils.convertImageToBase64(file)
                    if (b64 != null) {
                        imageBase64List.add(b64)
                    }
                } else {
                    fileTextAppendix += "\n\n[Attached Image: ${att.name} (${FileUtils.formatFileSize(att.sizeBytes)})]\n"
                }
            } else if (isPdf) {
                val text = FileUtils.readFullTextContent(file)
                fileTextAppendix += "\n\n--- [Attached Document: ${att.name} (PDF)] ---\n$text\n--- [End of Document] ---\n"
                if (supportsVision) {
                    val pdfImgB64 = FileUtils.convertPdfPageToBase64(file, 0)
                    if (pdfImgB64 != null) {
                        imageBase64List.add(pdfImgB64)
                    }
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
            put("max_tokens", maxTokens.coerceIn(1024, 16384))
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

        val (responseCode, isSuccess, responseBody) = executeRequest(reqBuilder.build()).use { resp ->
            Triple(resp.code, resp.isSuccessful, resp.body?.string().orEmpty())
        }

        if (!isSuccess) {
            val errorMsg = parseErrorMessage(responseBody, "Request failed ($responseCode)")
            return Result.failure(Exception(errorMsg))
        }

        val json = JSONObject(responseBody)
        val choices = json.optJSONArray("choices")
        if (choices != null && choices.length() > 0) {
            val firstChoice = choices.getJSONObject(0)
            val msg = firstChoice.optJSONObject("message")
            val content = if (msg != null && !msg.isNull("content")) msg.optString("content") else null
            if (!content.isNullOrBlank() && content != "null") {
                return Result.success(content)
            }
            val reasoning = if (msg != null && !msg.isNull("reasoning")) msg.optString("reasoning") else null
            if (!reasoning.isNullOrBlank() && reasoning != "null") {
                return Result.success(reasoning)
            }
            val reasoningContent = if (msg != null && !msg.isNull("reasoning_content")) msg.optString("reasoning_content") else null
            if (!reasoningContent.isNullOrBlank() && reasoningContent != "null") {
                return Result.success(reasoningContent)
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
        temperature: Float,
        maxTokens: Int = 8192
    ): Result<String> {
        val effectiveModel = when (modelId) {
            "openai", "gpt-oss-20b", "openai-fast" -> modelId
            else -> "openai-fast"
        }

        // First try the OpenAI-compatible endpoint
        val hasImages = attachments.any { it.isImage }
        val openAiRes = callOpenAiCompatible(
            endpointUrl = "https://text.pollinations.ai/openai/chat/completions",
            modelId = effectiveModel,
            prompt = prompt,
            attachments = attachments,
            history = history,
            apiKey = "",
            systemPrompt = systemPrompt,
            temperature = temperature,
            extraHeaders = emptyMap(),
            supportsVision = hasImages,
            maxTokens = maxTokens
        )

        if (openAiRes.isSuccess) {
            return openAiRes
        }

        // Fallback to direct text POST/GET endpoint for maximum reliability
        return try {
            var fileContext = ""
            for (att in attachments) {
                if (!att.isImage) {
                    val file = File(att.localUri)
                    fileContext += "\n\n--- [Attached Document: ${att.name}] ---\n" + FileUtils.readFullTextContent(file, 40_000) + "\n--- [End of Document] ---\n"
                }
            }

            val queryPrompt = if (fileContext.isNotEmpty()) "$fileContext\nUser Query: $prompt" else prompt

            // Try POST first to avoid HTTP 414 on large documents
            val postJson = JSONObject().apply {
                val msgs = JSONArray()
                if (systemPrompt.isNotBlank()) {
                    msgs.put(JSONObject().put("role", "system").put("content", systemPrompt))
                }
                for (h in history.takeLast(10)) {
                    msgs.put(JSONObject().put("role", if (h.role == "user") "user" else "assistant").put("content", h.content))
                }
                msgs.put(JSONObject().put("role", "user").put("content", queryPrompt))
                put("messages", msgs)
                put("model", effectiveModel)
                put("max_tokens", maxTokens.coerceIn(1024, 16384))
            }
            val postReq = Request.Builder()
                .url("https://text.pollinations.ai/")
                .post(postJson.toString().toRequestBody(jsonMediaType))
                .build()

            val postText = executeRequest(postReq).use { resp ->
                if (resp.isSuccessful) resp.body?.string().orEmpty() else ""
            }
            if (postText.isNotBlank()) {
                return Result.success(postText)
            }

            // Secondary fallback to simple GET
            val encodedPrompt = java.net.URLEncoder.encode(queryPrompt.take(2000), "UTF-8")
            val encodedSystem = java.net.URLEncoder.encode(systemPrompt.take(500), "UTF-8")
            val url = "https://text.pollinations.ai/$encodedPrompt?model=$effectiveModel&system=$encodedSystem"

            val req = Request.Builder().url(url).get().build()
            val text = executeRequest(req).use { resp ->
                if (resp.isSuccessful) resp.body?.string().orEmpty() else ""
            }
            if (text.isNotBlank()) {
                Result.success(text)
            } else {
                openAiRes // Return original error
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
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
