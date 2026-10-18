package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents a document stored in the local RAG knowledge base.
 */
@Entity(tableName = "rag_documents")
data class KnowledgeDocumentEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val sourceType: String, // "PDF", "DOCX", "TXT", "CODE", "MANUAL", "SAMPLE"
    val filePath: String? = null,
    val chunkCount: Int = 0,
    val charCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Represents an individual chunk of text extracted from a knowledge document,
 * with its vector embedding representation.
 */
@Entity(
    tableName = "rag_chunks",
    indices = [Index(value = ["docId"]), Index(value = ["chunkIndex"])]
)
data class KnowledgeChunkEntity(
    @PrimaryKey
    val id: String,
    val docId: String,
    val docTitle: String,
    val chunkIndex: Int,
    val content: String,
    val charStart: Int,
    val charEnd: Int,
    val embeddingJson: String = "[]", // JSON array of Float numbers
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Result of a semantic retrieval search with similarity scores and ranking metrics.
 */
data class RetrievedChunk(
    val chunk: KnowledgeChunkEntity,
    val score: Float, // 0.0f to 1.0f
    val matchType: String = "HYBRID", // "SEMANTIC_VECTOR", "KEYWORD_BM25", "HYBRID"
    val rank: Int = 1
)

/**
 * Status of the local RAG knowledge base and embedding engine.
 */
data class RagEngineStats(
    val totalDocuments: Int = 0,
    val totalChunks: Int = 0,
    val isGeminiEmbeddingAvailable: Boolean = false,
    val isHybridSearchActive: Boolean = true,
    val activeEmbeddingModel: String = "gemini-embedding-2-preview"
)
