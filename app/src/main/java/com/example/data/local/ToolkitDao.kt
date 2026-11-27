package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface ToolkitDao {

    // --- Flashcard Decks ---

    @Query("SELECT * FROM flashcard_decks ORDER BY updatedAt DESC")
    fun observeDecks(): Flow<List<FlashcardDeckEntity>>

    @Query("SELECT * FROM flashcard_decks ORDER BY updatedAt DESC")
    suspend fun getDecksOnce(): List<FlashcardDeckEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDeck(deck: FlashcardDeckEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDecks(decks: List<FlashcardDeckEntity>)

    @Query("DELETE FROM flashcard_decks WHERE id = :deckId")
    suspend fun deleteDeckById(deckId: String)

    @Query("DELETE FROM flashcards WHERE deckId = :deckId")
    suspend fun deleteCardsForDeck(deckId: String)

    @Transaction
    suspend fun deleteDeckWithCards(deckId: String) {
        deleteCardsForDeck(deckId)
        deleteDeckById(deckId)
    }

    // --- Flashcards ---

    @Query("SELECT * FROM flashcards ORDER BY createdAt ASC")
    fun observeAllCards(): Flow<List<FlashcardEntity>>

    @Query("SELECT * FROM flashcards WHERE deckId = :deckId ORDER BY createdAt ASC")
    fun observeCardsForDeck(deckId: String): Flow<List<FlashcardEntity>>

    @Query("SELECT * FROM flashcards ORDER BY createdAt ASC")
    suspend fun getAllCardsOnce(): List<FlashcardEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCard(card: FlashcardEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCards(cards: List<FlashcardEntity>)

    @Query("DELETE FROM flashcards WHERE id = :cardId")
    suspend fun deleteCardById(cardId: String)

    @Query("UPDATE flashcards SET mastered = :mastered, reviewCount = reviewCount + 1 WHERE id = :cardId")
    suspend fun updateCardMastery(cardId: String, mastered: Boolean)

    @Query("UPDATE flashcards SET mastered = 0 WHERE deckId = :deckId")
    suspend fun resetDeckMastery(deckId: String)

    // --- Expenses & Budget ---

    @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
    fun observeExpenses(): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
    suspend fun getExpensesOnce(): List<ExpenseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExpense(expense: ExpenseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenses(expenses: List<ExpenseEntity>)

    @Query("DELETE FROM expenses WHERE id = :expenseId")
    suspend fun deleteExpenseById(expenseId: String)

    @Query("DELETE FROM expenses")
    suspend fun clearExpenses()
}
