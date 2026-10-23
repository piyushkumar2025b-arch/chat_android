package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val emoji: String = "✅",
    val targetPerWeek: Int = 7,
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "habit_checkins",
    indices = [Index(value = ["habitId", "epochDay"], unique = true)]
)
data class HabitCheckInEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val habitId: String,
    val epochDay: Long,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "focus_sessions")
data class FocusSessionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val label: String = "Focus",
    val mode: String = "Focus",
    val minutes: Int,
    val wasCompleted: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
