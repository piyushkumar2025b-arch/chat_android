package com.example.data.model

/**
 * A group of full-text search hits inside one conversation.
 */
data class ChatSearchGroup(
    val sessionId: String,
    val sessionTitle: String,
    val sessionUpdatedAt: Long,
    val titleMatched: Boolean,
    val hits: List<ChatMessageEntity>
) {
    val latestHitAt: Long
        get() = hits.maxOfOrNull { it.timestamp } ?: sessionUpdatedAt
}
