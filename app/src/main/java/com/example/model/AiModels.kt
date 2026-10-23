package com.example.model

import android.graphics.Bitmap
import java.util.UUID

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
    val bitmap: Bitmap? = null,
    val localUri: String = ""
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
    CREATIVE_STUDIO("Creative Studio"),
    CODE_STUDIO("Code Studio"),
    MARKDOWN_STUDIO("Markdown Studio"),
    MEDIA_PLAYER("Media Player"),
    RAG_KNOWLEDGE("Knowledge Base"),
    CHAT_SEARCH("Search Chats"),
    AI_MEMORY("AI Memory"),
    PROMPT_LIBRARY("Prompt Library"),
    FOCUS_HABITS("Focus & Habits"),
    BACKUP_RESTORE("Backup & Restore")
}
