package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "flashcard_decks")
data class FlashcardDeckEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val topic: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "flashcards",
    indices = [Index(value = ["deckId"])]
)
data class FlashcardEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val deckId: String,
    val question: String,
    val answer: String,
    val mastered: Boolean = false,
    val reviewCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val amount: Double,
    val category: String = "Food",
    val isIncome: Boolean = false,
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
