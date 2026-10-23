package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryDao {

    @Query("SELECT * FROM ai_memories ORDER BY isPinned DESC, updatedAt DESC")
    fun observeAll(): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM ai_memories ORDER BY isPinned DESC, updatedAt DESC")
    suspend fun getAllOnce(): List<MemoryEntity>

    @Query("SELECT COUNT(*) FROM ai_memories")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(memory: MemoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(memories: List<MemoryEntity>)

    @Delete
    suspend fun delete(memory: MemoryEntity)

    @Query("DELETE FROM ai_memories")
    suspend fun clearAll()
}
