package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PromptDao {

    @Query("SELECT * FROM prompt_templates ORDER BY isFavorite DESC, updatedAt DESC")
    fun observeAll(): Flow<List<PromptTemplateEntity>>

    @Query("SELECT * FROM prompt_templates ORDER BY isFavorite DESC, updatedAt DESC")
    suspend fun getAllOnce(): List<PromptTemplateEntity>

    @Query("SELECT COUNT(*) FROM prompt_templates")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(prompt: PromptTemplateEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(prompts: List<PromptTemplateEntity>)

    @Update
    suspend fun update(prompt: PromptTemplateEntity)

    @Delete
    suspend fun delete(prompt: PromptTemplateEntity)

    @Query("UPDATE prompt_templates SET usageCount = usageCount + 1, lastUsedAt = :usedAt WHERE id = :id")
    suspend fun recordUsage(id: String, usedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM prompt_templates")
    suspend fun clearAll()
}
