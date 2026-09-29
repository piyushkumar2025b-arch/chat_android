package com.example.data

import com.example.data.local.ChatDao
import com.example.data.model.AttachmentInfo
import com.example.data.model.ChatMessageEntity
import com.example.data.model.ChatSessionEntity
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class ChatRepository(private val chatDao: ChatDao) {
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val attachmentListType = Types.newParameterizedType(List::class.java, AttachmentInfo::class.java)
    private val attachmentAdapter = moshi.adapter<List<AttachmentInfo>>(attachmentListType)

    val allSessions: Flow<List<ChatSessionEntity>> = chatDao.getAllSessions()
    val allMessages: Flow<List<ChatMessageEntity>> = chatDao.getAllMessages()

    fun getMessagesForSession(sessionId: String): Flow<List<ChatMessageEntity>> {
        return chatDao.getMessagesForSession(sessionId)
    }

    suspend fun getSession(sessionId: String): ChatSessionEntity? {
        return chatDao.getSessionById(sessionId)
    }

    suspend fun createNewSession(providerId: String, modelId: String, initialTitle: String = "New Chat"): ChatSessionEntity {
        val newSession = ChatSessionEntity(
            id = UUID.randomUUID().toString(),
            title = initialTitle,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            providerId = providerId,
            modelId = modelId
        )
        chatDao.insertSession(newSession)
        return newSession
    }

    suspend fun updateSessionTitle(sessionId: String, title: String) {
        chatDao.updateSessionTitle(sessionId, title, System.currentTimeMillis())
    }

    suspend fun updateSessionModel(sessionId: String, providerId: String, modelId: String) {
        chatDao.updateSessionModel(sessionId, providerId, modelId, System.currentTimeMillis())
    }

    suspend fun deleteSession(sessionId: String) {
        chatDao.deleteMessagesForSession(sessionId)
        chatDao.deleteSession(sessionId)
    }

    suspend fun clearAll() {
        chatDao.clearAllMessages()
        chatDao.clearAllSessions()
    }

    suspend fun addMessage(
        sessionId: String,
        role: String,
        content: String,
        providerId: String,
        modelId: String,
        attachments: List<AttachmentInfo> = emptyList(),
        isError: Boolean = false
    ): ChatMessageEntity {
        val json = try {
            attachmentAdapter.toJson(attachments)
        } catch (e: Exception) {
            "[]"
        }

        val message = ChatMessageEntity(
            id = UUID.randomUUID().toString(),
            sessionId = sessionId,
            role = role,
            content = content,
            timestamp = System.currentTimeMillis(),
            providerId = providerId,
            modelId = modelId,
            attachmentsJson = json,
            isError = isError
        )
        chatDao.insertMessage(message)
        // Update session timestamp
        chatDao.updateSessionTitle(sessionId, getSession(sessionId)?.title ?: "Chat", System.currentTimeMillis())
        return message
    }

    suspend fun updateMessageContent(messageId: String, content: String, isError: Boolean = false) {
        chatDao.updateMessageContent(messageId, content, isError)
    }

    fun parseAttachments(json: String): List<AttachmentInfo> {
        return try {
            attachmentAdapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
