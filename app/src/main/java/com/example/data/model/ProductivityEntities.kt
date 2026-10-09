package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "passwords")
data class PasswordItemEntity(
    @PrimaryKey
    val id: String,
    val serviceName: String,
    val usernameOrEmail: String,
    val encryptedPassword: String,
    val category: String = "General",
    val websiteUrl: String = "",
    val notes: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "notes")
data class NoteItemEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val content: String,
    val category: String = "Personal",
    val colorHex: String = "#3B82F6",
    val isPinned: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "calendar_events")
data class CalendarEventEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String = "",
    val year: Int,
    val month: Int, // 1 to 12
    val day: Int,   // 1 to 31
    val startTime: String = "09:00",
    val endTime: String = "10:00",
    val category: String = "Meeting",
    val colorHex: String = "#8B5CF6",
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
