package com.example.data.remote

import com.example.data.model.ProviderType
import com.example.model.ChatMessage
import com.example.model.MessageRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class ApiRequestException(
    val providerName: String,
    val statusCode: Int,
    val apiMessage: String
) : IOException("$providerName error ($statusCode): $apiMessage")

class MissingApiKeyException(providerName: String) :
    IllegalStateException("$providerName requires an API key. Tap Configure Key or open Settings to add one.")

object AiService {

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    /**
     * Streams an assistant reply token-by-token from the selected provider.
     * Providers that do not support SSE (Pollinations free tier) emit a single chunk.
     */
    fun streamResponse(
        provider: ProviderType,
        modelId: String,
        apiKey: String,
        history: List<ChatMessage>,
        userPrompt: String,
        contextDoc: String = "",
        systemPrompt: String = "",
        temperature: Float = 0.7f,
        maxTokens: Int = 2048,
        customBaseUrl: String = ""
    ): Flow<String> {
        if (provider.requiresApiKey && apiKey.isBlank()) {
            return flow { throw MissingApiKeyException(provider.displayName) }
        }

        val groundedPrompt = buildGroundedPrompt(userPrompt, contextDoc)
        val turns = history
            .filter { it.role == MessageRole.USER || it.role == MessageRole.ASSISTANT }
            .takeLast(12)

        return when (provider) {
            ProviderType.POLLINATIONS -> flow {
                emit(callPollinations(groundedPrompt, modelId, systemPrompt))
            }
            ProviderType.GEMINI -> streamGemini(
                apiKey = apiKey,
                modelId = modelId.ifBlank { "gemini-2.5-flash" },
                history = turns,
                prompt = groundedPrompt,
                systemPrompt = systemPrompt,
                temperature = temperature,
                maxTokens = maxTokens
            )
            ProviderType.CUSTOM -> {
                val endpoint = normalizeCustomEndpoint(customBaseUrl)
                flow {
                    if (endpoint.isBlank()) {
                        throw IllegalArgumentException("Custom endpoint URL is not configured. Open Settings to add one.")
                    }
                    emitAll(
                        streamOpenAiCompatible(
                            providerName = provider.displayName,
                            endpoint = endpoint,
                            apiKey = apiKey,
                            modelId = modelId.ifBlank { "gpt-4o-mini" },
                            history = turns,
                            prompt = groundedPrompt,
                            systemPrompt = systemPrompt,
                            temperature = temperature,
                            maxTokens = maxTokens,
                            extraHeaders = emptyList()
                        )
                    )
                }
            }
            else -> {
                val extraHeaders = if (provider == ProviderType.OPENROUTER) {
                    listOf(
                        "HTTP-Referer" to "https://omnichat.android",
                        "X-Title" to "OmniChat"
                    )
                } else emptyList()

                streamOpenAiCompatible(
                    providerName = provider.displayName,
                    endpoint = provider.defaultBaseUrl,
                    apiKey = apiKey,
                    modelId = modelId.ifBlank { defaultModelFor(provider) },
                    history = turns,
                    prompt = groundedPrompt,
                    systemPrompt = systemPrompt,
                    temperature = temperature,
                    maxTokens = maxTokens,
                    extraHeaders = extraHeaders
                )
            }
        }.flowOn(Dispatchers.IO)
    }

    /** Non-streaming convenience wrapper used by tools such as the persona test-drive. */
    suspend fun sendMessage(
        provider: ProviderType,
        modelId: String,
        prompt: String,
        apiKey: String = "",
        systemPrompt: String = "",
        temperature: Float = 0.7f,
        maxTokens: Int = 2048,
        customBaseUrl: String = ""
    ): Result<String> = runCatching {
        val builder = StringBuilder()
        streamResponse(
            provider = provider,
            modelId = modelId,
            apiKey = apiKey,
            history = emptyList(),
            userPrompt = prompt,
            systemPrompt = systemPrompt,
            temperature = temperature,
            maxTokens = maxTokens,
            customBaseUrl = customBaseUrl
        ).collect { builder.append(it) }
        val text = builder.toString()
        if (text.isBlank()) throw IOException("${provider.displayName} returned an empty response.")
        text
    }

    private fun defaultModelFor(provider: ProviderType): String = when (provider) {
        ProviderType.POLLINATIONS -> "openai"
        ProviderType.GEMINI -> "gemini-2.5-flash"
        ProviderType.GROQ -> "llama-3.3-70b-versatile"
        ProviderType.OPENROUTER -> "meta-llama/llama-3.3-70b-instruct:free"
        ProviderType.CEREBRAS -> "llama3.1-8b"
        ProviderType.HUGGINGFACE -> "Qwen/Qwen2.5-72B-Instruct"
        ProviderType.CUSTOM -> "gpt-4o-mini"
    }

    private fun buildGroundedPrompt(userPrompt: String, contextDoc: String): String {
        if (contextDoc.isBlank()) return userPrompt
        return buildString {
            appendLine("Use the retrieved context below to ground your answer. Cite it when relevant, and say so explicitly if it does not contain the answer.")
            appendLine()
            appendLine("<retrieved_context>")
            appendLine(contextDoc)
            appendLine("</retrieved_context>")
            appendLine()
            appendLine("User question:")
            append(userPrompt)
        }
    }

    private fun normalizeCustomEndpoint(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return ""
        val withScheme = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            trimmed
        } else {
            "https://$trimmed"
        }
        val noTrailingSlash = withScheme.trimEnd('/')
        return if (noTrailingSlash.endsWith("/chat/completions")) {
            noTrailingSlash
        } else {
            "$noTrailingSlash/chat/completions"
        }
    }

    private fun buildOpenAiMessages(
        history: List<ChatMessage>,
        prompt: String,
        systemPrompt: String
    ): JSONArray {
        val messages = JSONArray()
        if (systemPrompt.isNotBlank()) {
            messages.put(JSONObject().apply {
                put("role", "system")
                put("content", systemPrompt)
            })
        }
        for (message in history) {
            messages.put(JSONObject().apply {
                put("role", if (message.role == MessageRole.USER) "user" else "assistant")
                put("content", message.content)
            })
        }
        messages.put(JSONObject().apply {
            put("role", "user")
            put("content", prompt)
        })
        return messages
    }

    private fun streamOpenAiCompatible(
        providerName: String,
        endpoint: String,
        apiKey: String,
        modelId: String,
        history: List<ChatMessage>,
        prompt: String,
        systemPrompt: String,
        temperature: Float,
        maxTokens: Int,
        extraHeaders: List<Pair<String, String>>
    ): Flow<String> = callbackFlow {
        val body = JSONObject().apply {
            put("model", modelId)
            put("messages", buildOpenAiMessages(history, prompt, systemPrompt))
            put("temperature", temperature.toDouble())
            put("max_tokens", maxTokens)
            put("stream", true)
        }

        val requestBuilder = Request.Builder()
            .url(endpoint)
            .post(body.toString().toRequestBody(JSON_MEDIA_TYPE))
        if (apiKey.isNotBlank()) {
            requestBuilder.addHeader("Authorization", "Bearer $apiKey")
        }
        for ((name, value) in extraHeaders) {
            requestBuilder.addHeader(name, value)
        }

        val call = client.newCall(requestBuilder.build())
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                close(e)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use { resp ->
                    if (!resp.isSuccessful) {
                        close(ApiRequestException(providerName, resp.code, describeError(resp)))
                        return
                    }
                    val source = resp.body?.source()
                    if (source == null) {
                        close(IOException("$providerName returned an empty response body."))
                        return
                    }
                    try {
                        while (!source.exhausted()) {
                            val line = source.readUtf8Line() ?: break
                            val token = parseOpenAiSseLine(line)
                            if (!token.isNullOrEmpty()) {
                                trySend(token)
                            }
                        }
                        close()
                    } catch (e: Exception) {
                        close(e)
                    }
                }
            }
        })

        awaitClose { call.cancel() }
    }

    private fun streamGemini(
        apiKey: String,
        modelId: String,
        history: List<ChatMessage>,
        prompt: String,
        systemPrompt: String,
        temperature: Float,
        maxTokens: Int
    ): Flow<String> = callbackFlow {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelId" +
                ":streamGenerateContent?alt=sse&key=${URLEncoder.encode(apiKey, "UTF-8")}"

        val contents = JSONArray()
        // Gemini requires alternating user/model turns that start with a user turn.
        val validHistory = mutableListOf<ChatMessage>()
        var expectUser = true
        for (message in history) {
            val isUser = message.role == MessageRole.USER
            if (expectUser && isUser) {
                validHistory.add(message)
                expectUser = false
            } else if (!expectUser && !isUser) {
                validHistory.add(message)
                expectUser = true
            }
        }
        for (message in validHistory) {
            contents.put(JSONObject().apply {
                put("role", if (message.role == MessageRole.USER) "user" else "model")
                put("parts", JSONArray().put(JSONObject().put("text", message.content)))
            })
        }
        contents.put(JSONObject().apply {
            put("role", "user")
            put("parts", JSONArray().put(JSONObject().put("text", prompt)))
        })

        val body = JSONObject().apply {
            if (systemPrompt.isNotBlank()) {
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().put(JSONObject().put("text", systemPrompt)))
                })
            }
            put("contents", contents)
            put("generationConfig", JSONObject().apply {
                put("temperature", temperature.toDouble())
                put("maxOutputTokens", maxTokens)
            })
        }

        val request = Request.Builder()
            .url(url)
            .post(body.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        val call = client.newCall(request)
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                close(e)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use { resp ->
                    if (!resp.isSuccessful) {
                        close(ApiRequestException("Google Gemini", resp.code, describeError(resp)))
                        return
                    }
                    val source = resp.body?.source()
                    if (source == null) {
                        close(IOException("Gemini returned an empty response body."))
                        return
                    }
                    try {
                        while (!source.exhausted()) {
                            val line = source.readUtf8Line() ?: break
                            val token = parseGeminiSseLine(line)
                            if (!token.isNullOrEmpty()) {
                                trySend(token)
                            }
                        }
                        close()
                    } catch (e: Exception) {
                        close(e)
                    }
                }
            }
        })

        awaitClose { call.cancel() }
    }

    private fun callPollinations(
        prompt: String,
        modelId: String,
        systemPrompt: String
    ): String {
        val model = when (modelId) {
            "openai-fast" -> "openai"
            "" -> "openai"
            else -> modelId
        }
        val messages = buildOpenAiMessages(emptyList(), prompt, systemPrompt)
        val postBody = JSONObject().apply {
            put("model", model)
            put("messages", messages)
            put("stream", false)
        }
        var postFailure: Exception? = null
        try {
            val request = Request.Builder()
                .url("https://text.pollinations.ai/openai/chat/completions")
                .post(postBody.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val raw = response.body?.string().orEmpty()
                    val json = JSONObject(raw)
                    val content = json.optJSONArray("choices")
                        ?.optJSONObject(0)
                        ?.optJSONObject("message")
                        ?.optString("content")
                        .orEmpty()
                    if (content.isNotBlank()) return content
                } else {
                    postFailure = ApiRequestException(
                        "Pollinations",
                        response.code,
                        describeError(response)
                    )
                }
            }
        } catch (e: Exception) {
            postFailure = e
        }

        // Fallback to the plain-text endpoint for older Pollinations deployments.
        val combined = if (systemPrompt.isBlank()) prompt else "$systemPrompt\n\n$prompt"
        val url = "https://text.pollinations.ai/${URLEncoder.encode(combined, "UTF-8")}" +
                "?model=${URLEncoder.encode(model, "UTF-8")}&seed=42"
        val request = Request.Builder().url(url).get().build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw postFailure
                    ?: ApiRequestException("Pollinations", response.code, describeError(response))
            }
            val text = response.body?.string().orEmpty().trim()
            if (text.isBlank()) {
                throw IOException("Pollinations returned an empty response.")
            }
            return text
        }
    }

    private fun describeError(response: Response): String {
        val rawBody = try {
            response.body?.string().orEmpty()
        } catch (_: Exception) {
            ""
        }
        if (rawBody.isBlank()) return "HTTP ${response.code} ${response.message}"
        return try {
            val json = JSONObject(rawBody)
            json.optJSONObject("error")?.optString("message")?.takeIf { it.isNotBlank() }
                ?: json.optString("message").takeIf { it.isNotBlank() }
                ?: json.optString("detail").takeIf { it.isNotBlank() }
                ?: rawBody.take(300)
        } catch (_: Exception) {
            rawBody.take(300)
        }
    }

    internal fun parseOpenAiSseLine(line: String): String? {
        val trimmed = line.trim()
        if (!trimmed.startsWith("data:")) return null
        val data = trimmed.removePrefix("data:").trim()
        if (data.isEmpty() || data == "[DONE]") return null
        return try {
            val choices = JSONObject(data).optJSONArray("choices") ?: return null
            val delta = choices.optJSONObject(0)?.optJSONObject("delta") ?: return null
            if (delta.isNull("content")) null else delta.optString("content").takeIf { it.isNotEmpty() }
        } catch (_: Exception) {
            null
        }
    }

    internal fun parseGeminiSseLine(line: String): String? {
        val trimmed = line.trim()
        if (!trimmed.startsWith("data:")) return null
        val data = trimmed.removePrefix("data:").trim()
        if (data.isEmpty() || data == "[DONE]") return null
        return try {
            val candidates = JSONObject(data).optJSONArray("candidates") ?: return null
            val parts = candidates.optJSONObject(0)
                ?.optJSONObject("content")
                ?.optJSONArray("parts")
                ?: return null
            buildString {
                for (i in 0 until parts.length()) {
                    val text = parts.optJSONObject(i)?.optString("text").orEmpty()
                    append(text)
                }
            }.takeIf { it.isNotEmpty() }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun searchWeb(query: String): List<SearchResult> = withContext(Dispatchers.IO) {
        val results = mutableListOf<SearchResult>()
        try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val request = Request.Builder()
                .url("https://api.duckduckgo.com/?q=$encoded&format=json&no_html=1&skip_disambig=1")
                .build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val jsonStr = response.body?.string().orEmpty()
                    val root = JSONObject(jsonStr)

                    val abstractText = root.optString("AbstractText", "")
                    val abstractSource = root.optString("AbstractSource", "DuckDuckGo")
                    val abstractUrl = root.optString("AbstractURL", "")

                    if (abstractText.isNotBlank()) {
                        results.add(
                            SearchResult(
                                title = root.optString("Heading", query),
                                snippet = abstractText,
                                source = abstractSource,
                                url = abstractUrl
                            )
                        )
                    }

                    val related = root.optJSONArray("RelatedTopics")
                    if (related != null) {
                        for (i in 0 until minOf(related.length(), 5)) {
                            val obj = related.optJSONObject(i) ?: continue
                            val text = obj.optString("Text", "")
                            val firstUrl = obj.optString("FirstURL", "")
                            if (text.isNotBlank()) {
                                results.add(
                                    SearchResult(
                                        title = text.take(60),
                                        snippet = text,
                                        source = "DuckDuckGo Web",
                                        url = firstUrl
                                    )
                                )
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {
        }

        if (results.isEmpty()) {
            results.add(
                SearchResult(
                    title = "Knowledge Base: $query",
                    snippet = "No live web results were returned for this query. Try a different phrasing or provider.",
                    source = "Omni Intelligence",
                    url = "https://duckduckgo.com/?q=${URLEncoder.encode(query, "UTF-8")}"
                )
            )
        }
        results
    }
}

data class SearchResult(
    val title: String,
    val snippet: String,
    val source: String,
    val url: String
)
