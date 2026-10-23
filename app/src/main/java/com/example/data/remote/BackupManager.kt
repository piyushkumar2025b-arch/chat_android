package com.example.data.remote

import com.example.data.local.AppDatabase
import com.example.data.local.CalendarEventEntity
import com.example.data.local.FocusSessionEntity
import com.example.data.local.HabitCheckInEntity
import com.example.data.local.HabitEntity
import com.example.data.local.MemoryEntity
import com.example.data.local.NoteEntity
import com.example.data.local.PromptTemplateEntity
import com.example.data.model.ChatMessageEntity
import com.example.data.model.ChatSessionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Full local data portability. Vault secrets and API keys are intentionally
 * never written to a backup file.
 */
class BackupManager(private val db: AppDatabase) {

    data class BackupReport(
        val sessions: Int,
        val messages: Int,
        val notes: Int,
        val events: Int,
        val memories: Int,
        val prompts: Int,
        val habits: Int,
        val checkIns: Int,
        val focusSessions: Int
    ) {
        fun summary(): String =
            "$sessions chats · $messages messages · $notes notes · $memories memories · $prompts prompts · $habits habits"
    }

    data class ImportReport(
        val sessions: Int,
        val messages: Int,
        val notes: Int,
        val events: Int,
        val memories: Int,
        val prompts: Int,
        val habits: Int,
        val checkIns: Int,
        val focusSessions: Int
    ) {
        fun summary(): String =
            "Restored $sessions chats, $messages messages, $notes notes, $memories memories, $prompts prompts, $habits habits, $focusSessions focus sessions."
    }

    suspend fun exportJson(): Pair<String, BackupReport> = withContext(Dispatchers.IO) {
        val chatDao = db.chatDao()
        val productivityDao = db.productivityDao()
        val sessions = chatDao.getAllSessionsOnce()
        val messages = chatDao.getAllMessagesOnce()
        val notes = productivityDao.getAllNotesOnce()
        val events = productivityDao.getAllEventsOnce()
        val memories = db.memoryDao().getAllOnce()
        val prompts = db.promptDao().getAllOnce()
        val habits = db.focusDao().getHabitsOnce()
        val checkIns = db.focusDao().getAllCheckInsOnce()
        val focusSessions = db.focusDao().getFocusSessionsOnce()

        val root = JSONObject().apply {
            put("format", "omnichat-backup")
            put("version", 1)
            put("exportedAt", System.currentTimeMillis())
            put("vaultIncluded", false)
            put("securityNotice", "API keys and the encrypted secrets vault are excluded from this backup.")
            put("sessions", JSONArray().apply {
                sessions.forEach { put(it.toJson()) }
            })
            put("messages", JSONArray().apply {
                messages.forEach { put(it.toJson()) }
            })
            put("notes", JSONArray().apply {
                notes.forEach { note ->
                    put(JSONObject().apply {
                        put("id", note.id)
                        put("title", note.title)
                        put("content", note.content)
                        put("tags", note.tags)
                        put("updatedAt", note.updatedAt)
                    })
                }
            })
            put("events", JSONArray().apply {
                events.forEach { event ->
                    put(JSONObject().apply {
                        put("id", event.id)
                        put("title", event.title)
                        put("description", event.description)
                        put("timestamp", event.timestamp)
                        put("location", event.location)
                        put("priority", event.priority)
                    })
                }
            })
            put("memories", JSONArray().apply {
                memories.forEach { memory ->
                    put(JSONObject().apply {
                        put("id", memory.id)
                        put("content", memory.content)
                        put("category", memory.category)
                        put("isPinned", memory.isPinned)
                        put("createdAt", memory.createdAt)
                        put("updatedAt", memory.updatedAt)
                    })
                }
            })
            put("prompts", JSONArray().apply {
                prompts.forEach { prompt ->
                    put(JSONObject().apply {
                        put("id", prompt.id)
                        put("title", prompt.title)
                        put("content", prompt.content)
                        put("category", prompt.category)
                        put("isFavorite", prompt.isFavorite)
                        put("usageCount", prompt.usageCount)
                        put("lastUsedAt", prompt.lastUsedAt)
                        put("createdAt", prompt.createdAt)
                        put("updatedAt", prompt.updatedAt)
                    })
                }
            })
            put("habits", JSONArray().apply {
                habits.forEach { habit ->
                    put(JSONObject().apply {
                        put("id", habit.id)
                        put("name", habit.name)
                        put("emoji", habit.emoji)
                        put("targetPerWeek", habit.targetPerWeek)
                        put("isArchived", habit.isArchived)
                        put("createdAt", habit.createdAt)
                    })
                }
            })
            put("checkIns", JSONArray().apply {
                checkIns.forEach { checkIn ->
                    put(JSONObject().apply {
                        put("id", checkIn.id)
                        put("habitId", checkIn.habitId)
                        put("epochDay", checkIn.epochDay)
                        put("createdAt", checkIn.createdAt)
                    })
                }
            })
            put("focusSessions", JSONArray().apply {
                focusSessions.forEach { session ->
                    put(JSONObject().apply {
                        put("id", session.id)
                        put("label", session.label)
                        put("mode", session.mode)
                        put("minutes", session.minutes)
                        put("wasCompleted", session.wasCompleted)
                        put("createdAt", session.createdAt)
                    })
                }
            })
        }

        val report = BackupReport(
            sessions = sessions.size,
            messages = messages.size,
            notes = notes.size,
            events = events.size,
            memories = memories.size,
            prompts = prompts.size,
            habits = habits.size,
            checkIns = checkIns.size,
            focusSessions = focusSessions.size
        )
        root.toString(2) to report
    }

    suspend fun importJson(raw: String): ImportReport = withContext(Dispatchers.IO) {
        val root = try {
            JSONObject(raw)
        } catch (_: Exception) {
            throw IllegalArgumentException("That file is not valid JSON.")
        }
        require(root.optString("format") == "omnichat-backup") {
            "This is not an OmniChat backup file."
        }
        val version = root.optInt("version", 0)
        require(version == 1) {
            "Backup version $version is not supported by this build."
        }

        val sessions = root.optJSONArray("sessions").toSessions()
        val messages = root.optJSONArray("messages").toMessages()
        val notes = root.optJSONArray("notes").toNotes()
        val events = root.optJSONArray("events").toEvents()
        val memories = root.optJSONArray("memories").toMemories()
        val prompts = root.optJSONArray("prompts").toPrompts()
        val habits = root.optJSONArray("habits").toHabits()
        val checkIns = root.optJSONArray("checkIns").toCheckIns()
        val focusSessions = root.optJSONArray("focusSessions").toFocusSessions()

        if (sessions.isNotEmpty()) db.chatDao().insertSessions(sessions)
        if (messages.isNotEmpty()) db.chatDao().insertMessages(messages)
        if (notes.isNotEmpty()) db.productivityDao().insertNotes(notes)
        if (events.isNotEmpty()) db.productivityDao().insertEvents(events)
        if (memories.isNotEmpty()) db.memoryDao().insertAll(memories)
        if (prompts.isNotEmpty()) db.promptDao().insertAll(prompts)
        if (habits.isNotEmpty()) db.focusDao().insertHabits(habits)
        if (checkIns.isNotEmpty()) db.focusDao().insertCheckIns(checkIns)
        if (focusSessions.isNotEmpty()) db.focusDao().insertFocusSessions(focusSessions)

        ImportReport(
            sessions = sessions.size,
            messages = messages.size,
            notes = notes.size,
            events = events.size,
            memories = memories.size,
            prompts = prompts.size,
            habits = habits.size,
            checkIns = checkIns.size,
            focusSessions = focusSessions.size
        )
    }

    private fun ChatSessionEntity.toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("title", title)
        put("createdAt", createdAt)
        put("updatedAt", updatedAt)
        put("providerId", providerId)
        put("modelId", modelId)
    }

    private fun ChatMessageEntity.toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("sessionId", sessionId)
        put("role", role)
        put("content", content)
        put("timestamp", timestamp)
        put("providerId", providerId)
        put("modelId", modelId)
        put("attachmentsJson", attachmentsJson)
        put("isError", isError)
    }

    private fun JSONArray?.toSessions(): List<ChatSessionEntity> = buildList {
        val array = this@toSessions ?: return@buildList
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            val id = item.optString("id")
            if (id.isBlank()) continue
            add(
                ChatSessionEntity(
                    id = id,
                    title = item.optString("title", "New Chat"),
                    createdAt = item.optLong("createdAt", System.currentTimeMillis()),
                    updatedAt = item.optLong("updatedAt", System.currentTimeMillis()),
                    providerId = item.optString("providerId", "pollinations"),
                    modelId = item.optString("modelId", "openai")
                )
            )
        }
    }.filter { it.id.isNotBlank() }

    private fun JSONArray?.toMessages(): List<ChatMessageEntity> = buildList {
        val array = this@toMessages ?: return@buildList
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            val id = item.optString("id")
            val sessionId = item.optString("sessionId")
            if (id.isBlank() || sessionId.isBlank()) continue
            add(
                ChatMessageEntity(
                    id = id,
                    sessionId = sessionId,
                    role = item.optString("role", "user"),
                    content = item.optString("content", ""),
                    timestamp = item.optLong("timestamp", System.currentTimeMillis()),
                    providerId = item.optString("providerId", ""),
                    modelId = item.optString("modelId", ""),
                    attachmentsJson = item.optString("attachmentsJson", "[]"),
                    isError = item.optBoolean("isError", false)
                )
            )
        }
    }

    private fun JSONArray?.toNotes(): List<NoteEntity> = buildList {
        val array = this@toNotes ?: return@buildList
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            val id = item.optString("id")
            if (id.isBlank()) continue
            add(
                NoteEntity(
                    id = id,
                    title = item.optString("title", "Note"),
                    content = item.optString("content", ""),
                    tags = item.optString("tags", ""),
                    updatedAt = item.optLong("updatedAt", System.currentTimeMillis())
                )
            )
        }
    }

    private fun JSONArray?.toEvents(): List<CalendarEventEntity> = buildList {
        val array = this@toEvents ?: return@buildList
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            val id = item.optString("id")
            if (id.isBlank()) continue
            add(
                CalendarEventEntity(
                    id = id,
                    title = item.optString("title", "Event"),
                    description = item.optString("description", ""),
                    timestamp = item.optLong("timestamp", System.currentTimeMillis()),
                    location = item.optString("location", ""),
                    priority = item.optString("priority", "Normal")
                )
            )
        }
    }

    private fun JSONArray?.toMemories(): List<MemoryEntity> = buildList {
        val array = this@toMemories ?: return@buildList
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            val id = item.optString("id")
            if (id.isBlank()) continue
            add(
                MemoryEntity(
                    id = id,
                    content = item.optString("content", ""),
                    category = item.optString("category", "General"),
                    isPinned = item.optBoolean("isPinned", false),
                    createdAt = item.optLong("createdAt", System.currentTimeMillis()),
                    updatedAt = item.optLong("updatedAt", System.currentTimeMillis())
                )
            )
        }
    }

    private fun JSONArray?.toPrompts(): List<PromptTemplateEntity> = buildList {
        val array = this@toPrompts ?: return@buildList
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            val id = item.optString("id")
            if (id.isBlank()) continue
            add(
                PromptTemplateEntity(
                    id = id,
                    title = item.optString("title", "Prompt"),
                    content = item.optString("content", ""),
                    category = item.optString("category", "General"),
                    isFavorite = item.optBoolean("isFavorite", false),
                    usageCount = item.optInt("usageCount", 0),
                    lastUsedAt = item.optLong("lastUsedAt", 0L),
                    createdAt = item.optLong("createdAt", System.currentTimeMillis()),
                    updatedAt = item.optLong("updatedAt", System.currentTimeMillis())
                )
            )
        }
    }

    private fun JSONArray?.toHabits(): List<HabitEntity> = buildList {
        val array = this@toHabits ?: return@buildList
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            val id = item.optString("id")
            if (id.isBlank()) continue
            add(
                HabitEntity(
                    id = id,
                    name = item.optString("name", "Habit"),
                    emoji = item.optString("emoji", "✅"),
                    targetPerWeek = item.optInt("targetPerWeek", 7).coerceIn(1, 7),
                    isArchived = item.optBoolean("isArchived", false),
                    createdAt = item.optLong("createdAt", System.currentTimeMillis())
                )
            )
        }
    }

    private fun JSONArray?.toCheckIns(): List<HabitCheckInEntity> = buildList {
        val array = this@toCheckIns ?: return@buildList
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            val id = item.optString("id")
            if (id.isBlank()) continue
            val habitId = item.optString("habitId")
            if (habitId.isBlank()) continue
            add(
                HabitCheckInEntity(
                    id = id,
                    habitId = habitId,
                    epochDay = item.optLong("epochDay", 0L),
                    createdAt = item.optLong("createdAt", System.currentTimeMillis())
                )
            )
        }
    }

    private fun JSONArray?.toFocusSessions(): List<FocusSessionEntity> = buildList {
        val array = this@toFocusSessions ?: return@buildList
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            val id = item.optString("id")
            if (id.isBlank()) continue
            add(
                FocusSessionEntity(
                    id = id,
                    label = item.optString("label", "Focus"),
                    mode = item.optString("mode", "Focus"),
                    minutes = item.optInt("minutes", 25).coerceAtLeast(1),
                    wasCompleted = item.optBoolean("wasCompleted", true),
                    createdAt = item.optLong("createdAt", System.currentTimeMillis())
                )
            )
        }
    }
}
