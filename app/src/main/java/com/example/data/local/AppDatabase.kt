package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.ChatMessageEntity
import com.example.data.model.ChatSessionEntity
import com.example.data.model.KnowledgeChunkEntity
import com.example.data.model.KnowledgeDocumentEntity

@Database(
    entities = [
        NoteEntity::class,
        CalendarEventEntity::class,
        VaultItemEntity::class,
        ChatMessageEntity::class,
        ChatSessionEntity::class,
        KnowledgeDocumentEntity::class,
        KnowledgeChunkEntity::class,
        MemoryEntity::class,
        PromptTemplateEntity::class,
        HabitEntity::class,
        HabitCheckInEntity::class,
        FocusSessionEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productivityDao(): ProductivityDao
    abstract fun chatDao(): ChatDao
    abstract fun ragDao(): RagDao
    abstract fun memoryDao(): MemoryDao
    abstract fun promptDao(): PromptDao
    abstract fun focusDao(): FocusDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "omnichat_database"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
