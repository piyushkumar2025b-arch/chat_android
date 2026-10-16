package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val content: String,
    val tags: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "calendar_events")
data class CalendarEventEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val location: String = "",
    val priority: String = "Normal"
)

@Entity(tableName = "vault_items")
data class VaultItemEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val username: String = "",
    val secretValue: String, // Masked password or API key
    val category: String = "Password",
    val notes: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)
