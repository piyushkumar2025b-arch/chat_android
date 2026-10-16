package com.example.model

import android.graphics.Bitmap
import java.util.UUID

enum class AiProvider(
    val displayName: String,
    val defaultModel: String,
    val description: String,
    val requiresApiKey: Boolean
) {
    GEMINI(
        displayName = "Google Gemini",
        defaultModel = "gemini-2.5-flash",
        description = "High-speed multimodal reasoning & coding",
        requiresApiKey = false // Managed via Server-Side or free tier / BuildConfig
    ),
    GROQ(
        displayName = "Groq LLaMA 3",
        defaultModel = "llama-3.3-70b-versatile",
        description = "Ultra-fast low-latency LPU inference",
        requiresApiKey = true
    ),
    OPENROUTER(
        displayName = "OpenRouter",
        defaultModel = "meta-llama/llama-3.3-70b-instruct:free",
        description = "Unified gateway to Claude, DeepSeek, LLaMA",
        requiresApiKey = false // Has free models
    ),
    FREE_WEB(
        displayName = "Pollinations / Web AI",
        defaultModel = "openai-large",
        description = "Zero-setup free multi-agent intelligence",
        requiresApiKey = false
    )
}

enum class MessageRole {
    USER, ASSISTANT, SYSTEM
}

enum class MessageStatus {
    PENDING, STREAMING, COMPLETE, ERROR
}

data class ChatAttachment(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val mimeType: String,
    val sizeBytes: Long = 0L,
    val content: String = "",
    val bitmap: Bitmap? = null
)

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: MessageRole,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val modelName: String = "",
    val attachments: List<ChatAttachment> = emptyList(),
    val status: MessageStatus = MessageStatus.COMPLETE,
    val errorMessage: String? = null
)

enum class AppSection(val label: String) {
    CHAT("Chat"),
    INTEL_HUB("Intel Hub"),
    CODE_STUDIO("Code Studio"),
    MARKDOWN_STUDIO("Markdown Studio"),
    MEDIA_PLAYER("Media Player")
}
