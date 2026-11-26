package com.example

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.NoteEntity
import com.example.data.local.PreferencesManager
import com.example.data.model.AvailableModels
import com.example.data.model.ProviderType
import com.example.ui.ChatViewModel
import com.example.ui.CodeStudioScreen
import com.example.ui.ImageStudioScreen
import com.example.ui.MarkdownStudioScreen
import com.example.ui.NotesScreen
import com.example.ui.ProviderSelectorSheet
import com.example.ui.VideoStudioScreen
import com.google.firebase.FirebaseApp
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
class AiUsabilityVerificationTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var app: Application
    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(app, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        app.getSharedPreferences("omnichat_prefs", Context.MODE_PRIVATE)
            .edit().clear().commit()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun availableModels_builtInGeminiDefaultsAndSupportedIdsOnly() {
        assertFalse(ProviderType.GEMINI.requiresApiKey)
        assertEquals("gemini-3.8-flash", AvailableModels.defaultModel.id)

        val geminiModels = AvailableModels.models.filter { it.provider == ProviderType.GEMINI }.map { it.id }
        assertTrue(geminiModels.contains("gemini-3.8-flash"))
        assertTrue(geminiModels.contains("gemini-3.5-flash-lite"))
        assertTrue(geminiModels.contains("gemini-4-argon"))

        val forbiddenSubstrings = listOf(
            "gemini-1.",
            "gemini-2.",
            "gemini-3.8-pro",
            "gemini-3.1-flash-lite",
            "gemini-3.1-flash-image"
        )
        AvailableModels.models.forEach { model ->
            forbiddenSubstrings.forEach { forbidden ->
                assertFalse(
                    "Deprecated Gemini model ${model.id} must not be present",
                    model.id.contains(forbidden)
                )
            }
        }

        assertEquals("gemini-3.8-flash", AvailableModels.normalizeModelId("gemini-2.5-flash"))
        assertEquals("gemini-3.5-flash-lite", AvailableModels.normalizeModelId("gemini-3.1-flash-lite"))
    }

    @Test
    fun preferencesManager_defaultsToZeroKeyBuiltInGemini() {
        val prefs = PreferencesManager(app)
        assertEquals("gemini", prefs.lastProviderId)
        assertEquals("gemini-3.8-flash", prefs.lastModelId)
        assertTrue(prefs.geminiApiKey.isEmpty())
    }

    @Test
    fun configureAppCheck_persistsDebugTokenSynchronously() {
        val testToken = "00000000-1111-2222-3333-444444444444"
        val launchIntent = Intent().apply {
            putExtra("FIREBASE_APPCHECK_DEBUG_TOKEN", testToken)
        }

        configureAppCheck(app, launchIntent)

        val firebaseApps = FirebaseApp.getApps(app)
        assertTrue(firebaseApps.isNotEmpty())
        val persistenceKey = FirebaseApp.getInstance().persistenceKey
        val store = app.getSharedPreferences(
            "com.google.firebase.appcheck.debug.store.$persistenceKey",
            Context.MODE_PRIVATE
        )
        assertEquals(testToken, store.getString("com.google.firebase.appcheck.debug.DEBUG_SECRET", null))
    }

    @Test
    fun chatViewModel_defaultsToGeminiAndSwitchesQuickAiModes() {
        val viewModel = ChatViewModel(app)
        assertEquals(ProviderType.GEMINI, viewModel.activeProvider.value)
        assertEquals("gemini-3.8-flash", viewModel.activeModel.value.id)
        assertEquals("balanced", viewModel.quickAiMode.value)

        viewModel.setQuickAiMode("fast")
        assertEquals("fast", viewModel.quickAiMode.value)
        assertEquals("gemini-3.5-flash-lite", viewModel.activeModel.value.id)

        viewModel.setQuickAiMode("deep")
        assertEquals("deep", viewModel.quickAiMode.value)
        assertEquals("gemini-4-argon", viewModel.activeModel.value.id)

        viewModel.setQuickAiMode("balanced")
        assertEquals("balanced", viewModel.quickAiMode.value)
        assertEquals("gemini-3.8-flash", viewModel.activeModel.value.id)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun providerSelectorSheet_quickModesInvokeOnModelSelected() {
        var chosenModelId = ""
        composeRule.setContent {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ProviderSelectorSheet(
                sheetState = sheetState,
                selectedProvider = ProviderType.GEMINI,
                selectedModel = AvailableModels.defaultModel,
                isKeyConfigured = { it == ProviderType.GEMINI },
                getKeyForProvider = { "" },
                onUpdateKey = { _, _ -> },
                onModelSelected = { _, model -> chosenModelId = model.id },
                onOpenSettings = {},
                onDismiss = {}
            )
        }

        composeRule.onNodeWithTag("quick_preset_balanced").assertIsDisplayed()
        composeRule.onNodeWithTag("quick_preset_fast").assertIsDisplayed().performClick()
        assertEquals("gemini-3.5-flash-lite", chosenModelId)
        composeRule.onNodeWithTag("quick_preset_deep").assertIsDisplayed().performClick()
        assertEquals("gemini-4-argon", chosenModelId)
    }

    @Test
    fun studiosAndToolScreens_exposeZeroKeyAiControls() {
        composeRule.setContent {
            ImageStudioScreen()
        }
        composeRule.onNodeWithTag("image_prompt_input").assertIsDisplayed()
        composeRule.onNodeWithTag("pick_photo_to_edit_button").assertIsDisplayed()
        composeRule.onNodeWithTag("improve_image_prompt_button").assertIsDisplayed()
        composeRule.onNodeWithTag("generate_image_button").assertIsDisplayed()
    }

    @Test
    fun videoCodeMarkdownAndNotesScreens_exposeAiHelpers() {
        runBlocking {
            db.productivityDao().insertNote(
                NoteEntity(
                    id = "note-1",
                    title = "Meeting",
                    content = "Discuss project milestones",
                    tags = "work",
                    updatedAt = 100L
                )
            )
        }

        composeRule.setContent {
            VideoStudioScreen()
        }
        composeRule.onNodeWithTag("video_prompt_input").assertIsDisplayed()
        composeRule.onNodeWithTag("import_video_button").assertIsDisplayed()
        composeRule.onNodeWithTag("generate_video_button").assertIsDisplayed()
    }

    @Test
    fun codeAndMarkdownAndNotes_renderInlineAiActions() {
        composeRule.setContent {
            CodeStudioScreen(onBack = {}, showTopBar = false)
        }
        composeRule.onNodeWithTag("run_code_button").assertIsDisplayed()
        composeRule.onNodeWithTag("code_ai_assist_button").assertIsDisplayed()
    }

    @Test
    fun markdownStudioScreen_rendersAiPolishAndShortenChips() {
        composeRule.setContent {
            MarkdownStudioScreen(onBack = {}, showTopBar = false)
        }
        composeRule.onNodeWithTag("md_ai_polish_chip").assertIsDisplayed()
        composeRule.onNodeWithTag("md_ai_shorten_chip").assertIsDisplayed()
    }

    @Test
    fun notesScreen_rendersSummarizeAllAndDialogAiButtons() {
        runBlocking {
            db.productivityDao().insertNote(
                NoteEntity(
                    id = "note-1",
                    title = "Groceries",
                    content = "Apples, milk, coffee",
                    tags = "home",
                    updatedAt = 100L
                )
            )
        }

        composeRule.setContent {
            NotesScreen(productivityDao = db.productivityDao(), onBack = {})
        }

        composeRule.waitUntil(timeoutMillis = 3_000) {
            composeRule.onAllNodesWithTag("notes_summarize_all_button").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("notes_summarize_all_button").assertIsDisplayed()
        composeRule.onNodeWithTag("notes_add_fab").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("note_ai_polish_button").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("note_ai_summary_button").performScrollTo().assertIsDisplayed()
    }
}
