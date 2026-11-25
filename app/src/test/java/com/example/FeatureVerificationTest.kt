package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.CalendarEventEntity
import com.example.data.local.FocusSessionEntity
import com.example.data.local.HabitCheckInEntity
import com.example.data.local.HabitEntity
import com.example.data.local.MemoryEntity
import com.example.data.local.NoteEntity
import com.example.data.local.PromptTemplateEntity
import com.example.data.local.VaultSecurityManager
import com.example.data.model.ChatMessageEntity
import com.example.data.model.ChatSessionEntity
import com.example.data.remote.BackupManager
import com.example.ui.CalendarScreen
import com.example.ui.NotificationCenterScreen
import com.example.ui.PasswordSaverScreen
import com.example.ui.StudioScreen
import com.example.util.AppNotificationManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FeatureVerificationTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        context.getSharedPreferences(AppNotificationManager.PREFS_NAME, Context.MODE_PRIVATE)
            .edit().clear().commit()
        VaultSecurityManager.getInstance(context).disableMasterPin()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun notificationManager_respectsChannelTogglePreferences() {
        assertTrue(AppNotificationManager.isChannelEnabled(context, AppNotificationManager.CHANNEL_FITNESS))
        assertTrue(AppNotificationManager.isChannelEnabled(context, AppNotificationManager.CHANNEL_AI_TIPS))
        assertTrue(AppNotificationManager.isChannelEnabled(context, AppNotificationManager.CHANNEL_REMINDERS))

        val prefs = context.getSharedPreferences(AppNotificationManager.PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(AppNotificationManager.PREF_STEP_ALERTS, false).commit()

        assertFalse(AppNotificationManager.isChannelEnabled(context, AppNotificationManager.CHANNEL_FITNESS))
        assertFalse(
            AppNotificationManager.sendStepGoalAchieved(context, steps = 10_000, goal = 10_000)
        )
        assertTrue(AppNotificationManager.isChannelEnabled(context, AppNotificationManager.CHANNEL_AI_TIPS))
    }

    @Test
    fun notificationCenterScreen_togglePersistsPreference() {
        composeRule.setContent {
            NotificationCenterScreen(onBack = {})
        }

        composeRule.onNodeWithTag("notifications_list")
            .performScrollToNode(hasTestTag("switch_step_alerts"))
        composeRule.onNodeWithTag("switch_step_alerts").performClick()
        composeRule.waitForIdle()

        assertFalse(AppNotificationManager.isChannelEnabled(context, AppNotificationManager.CHANNEL_FITNESS))
    }

    @Test
    fun passwordSaver_unlocksEncryptsAndDecrypts() {
        runBlocking {
        composeRule.setContent {
            PasswordSaverScreen(
                productivityDao = db.productivityDao(),
                onBack = {}
            )
        }

        composeRule.onNodeWithTag("vault_pin_key_1").performClick()
        composeRule.onNodeWithTag("vault_pin_key_2").performClick()
        composeRule.onNodeWithTag("vault_pin_key_3").performClick()
        composeRule.onNodeWithTag("vault_pin_key_4").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("vault_add_fab").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("vault_title_input").performTextInput("Test Key")
        composeRule.onNodeWithTag("vault_secret_input").performTextInput("sk-test-secret-999")
        composeRule.onNodeWithTag("vault_save_button").performClick()
        composeRule.waitForIdle()

        val savedItems = db.productivityDao().getAllVaultItems().first()
        assertEquals(1, savedItems.size)
        val stored = savedItems.first()
        assertTrue(
            "Stored vault value must be encrypted, was: ${stored.secretValue}",
            stored.secretValue.startsWith("ENC:GCM:")
        )
        assertEquals(
            "sk-test-secret-999",
            VaultSecurityManager.getInstance(context).decrypt(stored.secretValue)
        )
        }
    }

    @Test
    fun calendarScreen_createsEventWithHighPriority() {
        runBlocking {
        composeRule.setContent {
            CalendarScreen(
                productivityDao = db.productivityDao(),
                onBack = {}
            )
        }

        composeRule.onNodeWithTag("calendar_add_fab").performClick()
        composeRule.onNodeWithTag("calendar_title_input").performTextInput("Design Review")
        composeRule.onNodeWithTag("calendar_priority_High").performClick()
        composeRule.onNodeWithTag("calendar_save_button").performClick()
        composeRule.waitForIdle()

        val events = db.productivityDao().getAllEvents().first()
        assertEquals(1, events.size)
        assertEquals("Design Review", events.first().title)
        assertEquals("High", events.first().priority)

        composeRule.onNodeWithText("Design Review").assertIsDisplayed()
        composeRule.onNodeWithText("High Priority").assertIsDisplayed()
        }
    }

    @Test
    fun studioScreen_switchesTabsWithoutNestedTopAppBars() {
        composeRule.setContent {
            StudioScreen(initialTab = 1)
        }

        composeRule.onNodeWithText("Sketch Canvas").assertIsDisplayed()
        composeRule.onNodeWithTag("send_image_to_chat_button").assertIsDisplayed()
        assertEquals(0, composeRule.onAllNodesWithText("Sketch Pad").fetchSemanticsNodes().size)

        composeRule.onNodeWithTag("studio_tab_3").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Main.kt").assertIsDisplayed()
        composeRule.onNodeWithTag("run_code_button").assertIsDisplayed()

        composeRule.onNodeWithTag("studio_tab_4").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Notes.md").assertIsDisplayed()
        composeRule.onNodeWithText("Live Preview").assertIsDisplayed()
    }

    @Test
    fun backupManager_roundTripExportsAndImportsAllTables() {
        runBlocking {
        db.chatDao().insertSession(
            ChatSessionEntity(
                id = "s1",
                title = "Trip Plan",
                createdAt = 100L,
                updatedAt = 200L,
                providerId = "gemini",
                modelId = "gemini-2.5-flash"
            )
        )
        db.chatDao().insertMessage(
            ChatMessageEntity(
                id = "m1",
                sessionId = "s1",
                role = "user",
                content = "Pack list please",
                timestamp = 150L
            )
        )
        db.productivityDao().insertNote(
            NoteEntity(id = "n1", title = "Checklist", content = "Passport", tags = "travel", updatedAt = 300L)
        )
        db.productivityDao().insertEvent(
            CalendarEventEntity(id = "e1", title = "Flight", description = "Gate A2", timestamp = 400L, location = "SFO", priority = "High")
        )
        db.memoryDao().insert(
            MemoryEntity(id = "mem1", content = "Prefers window seats", category = "Travel")
        )
        db.promptDao().insert(
            PromptTemplateEntity(id = "p1", title = "Summarize", content = "Give 3 bullets", category = "Writing")
        )
        db.focusDao().upsertHabit(HabitEntity(id = "h1", name = "Walk", emoji = "✅", targetPerWeek = 5))
        db.focusDao().insertCheckIn(HabitCheckInEntity(id = "c1", habitId = "h1", epochDay = 20000L))
        db.focusDao().insertFocusSession(FocusSessionEntity(id = "f1", label = "Deep Work", mode = "Focus", minutes = 25, wasCompleted = true))

        val (json, exportReport) = BackupManager(db).exportJson()
        assertEquals(1, exportReport.sessions)
        assertEquals(1, exportReport.messages)
        assertEquals(1, exportReport.notes)
        assertEquals(1, exportReport.events)
        assertEquals(1, exportReport.memories)
        assertEquals(1, exportReport.prompts)
        assertEquals(1, exportReport.habits)
        assertEquals(1, exportReport.checkIns)
        assertEquals(1, exportReport.focusSessions)

        val freshDb = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val importReport = BackupManager(freshDb).importJson(json)
            assertEquals(1, importReport.sessions)
            assertEquals(1, importReport.messages)
            assertEquals(1, importReport.notes)
            assertEquals(1, importReport.events)
            assertEquals(1, importReport.memories)
            assertEquals(1, importReport.prompts)
            assertEquals(1, importReport.habits)
            assertEquals(1, importReport.checkIns)
            assertEquals(1, importReport.focusSessions)

            assertEquals("Trip Plan", freshDb.chatDao().getAllSessionsOnce().first().title)
            assertEquals("High", freshDb.productivityDao().getAllEventsOnce().first().priority)
        } finally {
            freshDb.close()
        }
        }
    }
}
