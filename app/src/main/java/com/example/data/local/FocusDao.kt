package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface FocusDao {

    @Query("SELECT * FROM habits WHERE isArchived = 0 ORDER BY createdAt ASC")
    fun observeHabits(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits ORDER BY createdAt ASC")
    suspend fun getHabitsOnce(): List<HabitEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHabit(habit: HabitEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabits(habits: List<HabitEntity>)

    @Query("DELETE FROM habits WHERE id = :habitId")
    suspend fun deleteHabitById(habitId: String)

    @Query("DELETE FROM habit_checkins WHERE habitId = :habitId")
    suspend fun deleteCheckInsForHabit(habitId: String)

    @Transaction
    suspend fun deleteHabitWithCheckIns(habitId: String) {
        deleteCheckInsForHabit(habitId)
        deleteHabitById(habitId)
    }

    @Query("SELECT * FROM habit_checkins ORDER BY epochDay ASC")
    fun observeAllCheckIns(): Flow<List<HabitCheckInEntity>>

    @Query("SELECT * FROM habit_checkins ORDER BY epochDay ASC")
    suspend fun getAllCheckInsOnce(): List<HabitCheckInEntity>

    @Query("SELECT * FROM habit_checkins WHERE habitId = :habitId AND epochDay = :epochDay LIMIT 1")
    suspend fun getCheckIn(habitId: String, epochDay: Long): HabitCheckInEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCheckIn(checkIn: HabitCheckInEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCheckIns(checkIns: List<HabitCheckInEntity>)

    @Query("DELETE FROM habit_checkins WHERE habitId = :habitId AND epochDay = :epochDay")
    suspend fun deleteCheckIn(habitId: String, epochDay: Long)

    @Query("SELECT * FROM focus_sessions ORDER BY createdAt DESC")
    fun observeFocusSessions(): Flow<List<FocusSessionEntity>>

    @Query("SELECT * FROM focus_sessions ORDER BY createdAt DESC")
    suspend fun getFocusSessionsOnce(): List<FocusSessionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFocusSession(session: FocusSessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFocusSessions(sessions: List<FocusSessionEntity>)

    @Query("DELETE FROM focus_sessions WHERE id = :sessionId")
    suspend fun deleteFocusSession(sessionId: String)

    @Query("DELETE FROM focus_sessions")
    suspend fun clearFocusSessions()
}
