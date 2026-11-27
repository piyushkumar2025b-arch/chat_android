package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
        FocusSessionEntity::class,
        FlashcardDeckEntity::class,
        FlashcardEntity::class,
        ExpenseEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productivityDao(): ProductivityDao
    abstract fun chatDao(): ChatDao
    abstract fun ragDao(): RagDao
    abstract fun memoryDao(): MemoryDao
    abstract fun promptDao(): PromptDao
    abstract fun focusDao(): FocusDao
    abstract fun toolkitDao(): ToolkitDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `flashcard_decks` (
                        `id` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `topic` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `flashcards` (
                        `id` TEXT NOT NULL,
                        `deckId` TEXT NOT NULL,
                        `question` TEXT NOT NULL,
                        `answer` TEXT NOT NULL,
                        `mastered` INTEGER NOT NULL,
                        `reviewCount` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_flashcards_deckId` ON `flashcards` (`deckId`)"
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `expenses` (
                        `id` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `amount` REAL NOT NULL,
                        `category` TEXT NOT NULL,
                        `isIncome` INTEGER NOT NULL,
                        `note` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "omnichat_database"
                )
                    .addMigrations(MIGRATION_3_4)
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
