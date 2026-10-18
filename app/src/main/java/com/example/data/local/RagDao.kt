package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.data.model.KnowledgeChunkEntity
import com.example.data.model.KnowledgeDocumentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RagDao {

    @Query("SELECT * FROM rag_documents ORDER BY updatedAt DESC")
    fun getAllDocuments(): Flow<List<KnowledgeDocumentEntity>>

    @Query("SELECT * FROM rag_documents ORDER BY updatedAt DESC")
    suspend fun getAllDocumentsOnce(): List<KnowledgeDocumentEntity>

    @Query("SELECT * FROM rag_documents WHERE id = :docId LIMIT 1")
    suspend fun getDocumentById(docId: String): KnowledgeDocumentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(doc: KnowledgeDocumentEntity)

    @Query("DELETE FROM rag_documents WHERE id = :docId")
    suspend fun deleteDocument(docId: String)

    @Query("SELECT * FROM rag_chunks WHERE docId = :docId ORDER BY chunkIndex ASC")
    suspend fun getChunksForDoc(docId: String): List<KnowledgeChunkEntity>

    @Query("SELECT * FROM rag_chunks ORDER BY docId ASC, chunkIndex ASC")
    suspend fun getAllChunks(): List<KnowledgeChunkEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChunks(chunks: List<KnowledgeChunkEntity>)

    @Query("DELETE FROM rag_chunks WHERE docId = :docId")
    suspend fun deleteChunksForDoc(docId: String)

    @Query("SELECT COUNT(*) FROM rag_documents")
    suspend fun getDocumentCount(): Int

    @Query("SELECT COUNT(*) FROM rag_chunks")
    suspend fun getChunkCount(): Int

    @Transaction
    suspend fun deleteDocumentWithChunks(docId: String) {
        deleteChunksForDoc(docId)
        deleteDocument(docId)
    }

    @Query("DELETE FROM rag_documents")
    suspend fun clearAllDocuments()

    @Query("DELETE FROM rag_chunks")
    suspend fun clearAllChunks()

    @Transaction
    suspend fun clearEntireKnowledgeBase() {
        clearAllChunks()
        clearAllDocuments()
    }
}
