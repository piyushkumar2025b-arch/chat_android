package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppLockManager
import com.example.data.local.MemoryEntity
import com.example.data.model.ChatMessageEntity
import com.example.data.model.ChatSessionEntity
import com.example.data.remote.AssistantContextBuilder
import com.example.data.remote.ConversationExporter
import com.example.model.ChatMessage
import com.example.model.MessageRole
import com.example.model.MessageStatus
import com.example.util.StreakCalculator
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ProductivityFeaturesTest {

    // --- Assistant context building ---

    @Test
    fun contextBuilder_estimateTokens_hasMinimumOfOne() {
        assertEquals(1, AssistantContextBuilder.estimateTokens(""))
        assertEquals(1, AssistantContextBuilder.estimateTokens("hi"))
        assertEquals(25, AssistantContextBuilder.estimateTokens("a".repeat(100)))
    }

    @Test
    fun contextBuilder_injectsMemoriesOnlyWhenEnabled() {
        val memories = listOf(
            MemoryEntity(id = "m1", content = "Prefers Kotlin examples", category = "Preferences"),
            MemoryEntity(id = "m2", content = "Building OmniChat", category = "Work")
        )

        val disabled = AssistantContextBuilder.composeSystemPrompt("Base prompt", memories, false)
        assertEquals("Base prompt", disabled)
        assertFalse(disabled.contains("Prefers Kotlin examples"))

        val enabled = AssistantContextBuilder.composeSystemPrompt("Base prompt", memories, true)
        assertTrue(enabled.startsWith("Base prompt"))
        assertTrue(enabled.contains("# Long-term memory about the user"))
        assertTrue(enabled.contains("[Preferences] Prefers Kotlin examples"))
        assertTrue(enabled.contains("[Work] Building OmniChat"))
    }

    @Test
    fun contextBuilder_skipsBlankMemories() {
        val memories = listOf(
            MemoryEntity(id = "m1", content = "   ", category = "General"),
            MemoryEntity(id = "m2", content = "Likes dark mode", category = "Preferences")
        )
        val prompt = AssistantContextBuilder.composeSystemPrompt("", memories, true)
        assertFalse(prompt.contains("[General]"))
        assertTrue(prompt.contains("[Preferences] Likes dark mode"))
    }

    @Test
    fun contextBuilder_trimsHistoryToMostRecentMessagesWithinBudget() {
        val history = (1..5).map { index ->
            ChatMessage(
                id = "msg-$index",
                role = if (index % 2 == 1) MessageRole.USER else MessageRole.ASSISTANT,
                content = "x".repeat(400),
                status = MessageStatus.COMPLETE
            )
        }

        val trimmed = AssistantContextBuilder.trimHistory(history, maxTokens = 250)

        assertEquals(2, trimmed.size)
        assertEquals("msg-4", trimmed.first().id)
        assertEquals("msg-5", trimmed.last().id)
    }

    @Test
    fun contextBuilder_ignoresNonCompleteAndSystemMessages() {
        val history = listOf(
            ChatMessage(id = "user-1", role = MessageRole.USER, content = "hello"),
            ChatMessage(
                id = "streaming-1",
                role = MessageRole.ASSISTANT,
                content = "partial answer",
                status = MessageStatus.STREAMING
            ),
            ChatMessage(id = "system-1", role = MessageRole.SYSTEM, content = "system rules"),
            ChatMessage(
                id = "assistant-1",
                role = MessageRole.ASSISTANT,
                content = "complete answer",
                status = MessageStatus.COMPLETE
            )
        )

        val trimmed = AssistantContextBuilder.trimHistory(history, maxTokens = 10_000)

        assertEquals(listOf("user-1", "assistant-1"), trimmed.map { it.id })
    }

    // --- Streak helpers ---

    @Test
    fun streak_counter_countsCurrentRun() {
        val today = 20_000L
        val checked = setOf(today, today - 1, today - 2, today - 4)

        assertEquals(3, StreakCalculator.currentStreak(checked, today))
    }

    @Test
    fun streak_counter_survivesUncheckedTodayIfYesterdayWasChecked() {
        val today = 20_000L
        val checked = setOf(today - 1, today - 2, today - 3)

        assertEquals(3, StreakCalculator.currentStreak(checked, today))
        assertEquals(0, StreakCalculator.currentStreak(emptySet(), today))
    }

    @Test
    fun streak_lastDaysAndWindowAreConsecutive() {
        val today = 20_000L
        val days = StreakCalculator.lastDays(today, 7)

        assertEquals(7, days.size)
        assertEquals(today, days.last())
        assertEquals(today - 6, days.first())

        val checked = setOf(today, today - 2, today - 6)
        assertEquals(3, StreakCalculator.checksInWindow(checked, today, 7))
        assertEquals(2, StreakCalculator.checksInWindow(checked, today, 5))
    }

    @Test
    fun localEpochDay_isStableForSameLocalDay() {
        val zone = TimeZone.getTimeZone("UTC")
        val morning = 1_700_000_000_000L
        val laterSameDay = morning + 60 * 60 * 1000L

        assertEquals(
            StreakCalculator.localEpochDay(morning, zone),
            StreakCalculator.localEpochDay(laterSameDay, zone)
        )
    }

    // --- App lock ---

    @Test
    fun appLock_pinLifecycleAndRelockTimeout() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = AppLockManager(context)
        manager.disable()
        manager.clearBackgroundTimestamp()
        try {
            assertFalse(manager.isEnabled())
            assertFalse(manager.verify("1234"))

            assertTrue(manager.setPin("2468"))
            assertTrue(manager.isEnabled())
            assertTrue(manager.verify("2468"))
            assertFalse(manager.verify("0000"))
            assertFalse(manager.verify("abc"))
            assertFalse(manager.setPin("12"))

            manager.timeoutMillis = AppLockManager.THIRTY_SECONDS
            manager.markBackgrounded(now = 1_000L)
            assertFalse(manager.shouldRelock(now = 20_000L))
            assertTrue(manager.shouldRelock(now = 40_000L))
        } finally {
            manager.disable()
            manager.clearBackgroundTimestamp()
        }

        assertFalse(manager.isEnabled())
    }

    // --- Conversation export ---

    private val sampleSession = ChatSessionEntity(
        id = "session-1",
        title = "Roadmap / Plans",
        providerId = "gemini",
        modelId = "gemini-2.0-flash",
        createdAt = 1L,
        updatedAt = 2L
    )

    private val sampleMessages = listOf(
        ChatMessageEntity(
            id = "m1",
            sessionId = "session-1",
            role = "user",
            content = "Draft a launch checklist",
            timestamp = 10L
        ),
        ChatMessageEntity(
            id = "m2",
            sessionId = "session-1",
            role = "assistant",
            content = "1. Test\n2. Ship\n3. Learn",
            timestamp = 20L,
            modelId = "gemini-2.0-flash"
        )
    )

    @Test
    fun exporter_sanitizesFileNames() {
        assertEquals("Roadmap_Plans.md", ConversationExporter.fileName("Roadmap / Plans", ConversationExporter.Format.MARKDOWN))
        assertEquals("omnichat-export.json", ConversationExporter.fileName("   ", ConversationExporter.Format.JSON))
        assertEquals("omnichat-export.md", ConversationExporter.fileName("///", ConversationExporter.Format.MARKDOWN))
    }

    @Test
    fun exporter_markdownContainsTitleAndBothTurns() {
        val markdown = ConversationExporter.exportSession(
            sampleSession,
            sampleMessages,
            ConversationExporter.Format.MARKDOWN
        )

        assertTrue(markdown.contains("# Roadmap / Plans"))
        assertTrue(markdown.contains("### You"))
        assertTrue(markdown.contains("### Assistant · gemini-2.0-flash"))
        assertTrue(markdown.contains("Draft a launch checklist"))
        assertTrue(markdown.contains("1. Test"))
    }

    @Test
    fun exporter_jsonIsWellFormedAndComplete() {
        val json = ConversationExporter.exportSession(
            sampleSession,
            sampleMessages,
            ConversationExporter.Format.JSON
        )
        val parsed = JSONObject(json)

        assertEquals("session-1", parsed.getString("id"))
        assertEquals("Roadmap / Plans", parsed.getString("title"))
        val messages = parsed.getJSONArray("messages")
        assertEquals(2, messages.length())
        assertEquals("user", messages.getJSONObject(0).getString("role"))
        assertEquals("1. Test\n2. Ship\n3. Learn", messages.getJSONObject(1).getString("content"))
    }

    @Test
    fun exporter_shareTextLabelsSpeakers() {
        val text = ConversationExporter.shareText(sampleSession, sampleMessages)

        assertTrue(text.contains("Roadmap / Plans — OmniChat"))
        assertTrue(text.contains("You:"))
        assertTrue(text.contains("Assistant:"))
        assertTrue(text.endsWith("Shared from OmniChat"))
    }
}
