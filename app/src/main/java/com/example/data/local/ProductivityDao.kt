package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.CalendarEventEntity
import com.example.data.model.NoteItemEntity
import com.example.data.model.PasswordItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductivityDao {

    // --- Passwords ---
    @Query("SELECT * FROM passwords ORDER BY serviceName ASC")
    fun getAllPasswordsFlow(): Flow<List<PasswordItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPassword(item: PasswordItemEntity)

    @Update
    suspend fun updatePassword(item: PasswordItemEntity)

    @Delete
    suspend fun deletePassword(item: PasswordItemEntity)

    @Query("DELETE FROM passwords WHERE id = :id")
    suspend fun deletePasswordById(id: String)

    // --- Notes ---
    @Query("SELECT * FROM notes ORDER BY isPinned DESC, updatedAt DESC")
    fun getAllNotesFlow(): Flow<List<NoteItemEntity>>

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    suspend fun getNoteById(id: String): NoteItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(item: NoteItemEntity)

    @Update
    suspend fun updateNote(item: NoteItemEntity)

    @Delete
    suspend fun deleteNote(item: NoteItemEntity)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNoteById(id: String)

    // --- Calendar Events ---
    @Query("SELECT * FROM calendar_events WHERE year = :year AND month = :month AND day = :day ORDER BY startTime ASC")
    fun getEventsForDateFlow(year: Int, month: Int, day: Int): Flow<List<CalendarEventEntity>>

    @Query("SELECT * FROM calendar_events WHERE year = :year AND month = :month ORDER BY day ASC, startTime ASC")
    fun getEventsForMonthFlow(year: Int, month: Int): Flow<List<CalendarEventEntity>>

    @Query("SELECT * FROM calendar_events ORDER BY year ASC, month ASC, day ASC, startTime ASC")
    fun getAllEventsFlow(): Flow<List<CalendarEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(item: CalendarEventEntity)

    @Update
    suspend fun updateEvent(item: CalendarEventEntity)

    @Delete
    suspend fun deleteEvent(item: CalendarEventEntity)

    @Query("DELETE FROM calendar_events WHERE id = :id")
    suspend fun deleteEventById(id: String)
}
