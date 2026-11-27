package com.example

import android.content.Context
import android.os.Looper
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.CalendarEventEntity
import com.example.data.local.ExpenseEntity
import com.example.data.local.FlashcardDeckEntity
import com.example.data.local.FlashcardEntity
import com.example.data.local.NoteEntity
import com.example.data.local.PreferencesManager
import com.example.data.remote.BackupManager
import com.example.ui.BudgetScreen
import com.example.ui.CalculatorScreen
import com.example.ui.CalendarScreen
import com.example.ui.DecisionToolsScreen
import com.example.ui.FlashcardsScreen
import com.example.ui.NotesScreen
import com.example.ui.ReadAloudScreen
import com.example.ui.TranslatorScreen
import com.example.ui.WritingStudioScreen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NewToolkitFeaturesTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var prefs: PreferencesManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        context.getSharedPreferences("omnichat_prefs", Context.MODE_PRIVATE)
            .edit().clear().commit()
        prefs = PreferencesManager(context)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun roomMigration3To4_createsToolkitTablesCleanly() {
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(null)
                .callback(object : SupportSQLiteOpenHelper.Callback(3) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL("CREATE TABLE IF NOT EXISTS notes (id TEXT NOT NULL PRIMARY KEY, title TEXT NOT NULL, content TEXT NOT NULL, tags TEXT NOT NULL, updatedAt INTEGER NOT NULL)")
                    }
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                })
                .build()
        )
        try {
            val sqliteDb = helper.writableDatabase
            AppDatabase.MIGRATION_3_4.migrate(sqliteDb)
            sqliteDb.query("SELECT COUNT(*) FROM flashcard_decks").use { c ->
                assertTrue(c.moveToFirst())
                assertEquals(0, c.getInt(0))
            }
            sqliteDb.query("SELECT COUNT(*) FROM flashcards").use { c ->
                assertTrue(c.moveToFirst())
                assertEquals(0, c.getInt(0))
            }
            sqliteDb.query("SELECT COUNT(*) FROM expenses").use { c ->
                assertTrue(c.moveToFirst())
                assertEquals(0, c.getInt(0))
            }
        } finally {
            helper.close()
        }
    }

    @Test
    fun flashcardsScreen_createsManualDeckAndCardAndTracksMastery() {
        runBlocking {
            composeRule.setContent {
                FlashcardsScreen(
                    toolkitDao = db.toolkitDao(),
                    productivityDao = db.productivityDao(),
                    onBack = {}
                )
            }

            composeRule.onNodeWithTag("flashcards_fab").performClick()
            composeRule.onNodeWithTag("flashcards_new_deck_input").performTextInput("World Capitals")
            composeRule.onNodeWithTag("flashcards_save_deck_button").performClick()
            composeRule.waitUntil(timeoutMillis = 3_000) {
                composeRule.onAllNodesWithText("World Capitals").fetchSemanticsNodes().isNotEmpty()
            }

            composeRule.onNodeWithTag("flashcards_fab").performClick()
            composeRule.onNodeWithTag("flashcards_new_question_input").performTextInput("Capital of Japan?")
            composeRule.onNodeWithTag("flashcards_new_answer_input").performTextInput("Tokyo")
            composeRule.onNodeWithTag("flashcards_save_card_button").performClick()
            composeRule.waitUntil(timeoutMillis = 3_000) {
                composeRule.onAllNodesWithText("Capital of Japan?").fetchSemanticsNodes().isNotEmpty()
            }

            composeRule.onAllNodesWithText("Capital of Japan?").onFirst().assertIsDisplayed()
            composeRule.onNodeWithTag("flashcard_flip_surface").performClick()
            composeRule.waitForIdle()
            composeRule.onNodeWithTag("flashcard_got_it_button").performClick()
            composeRule.waitForIdle()

            val deadline = System.currentTimeMillis() + 3_000
            while (System.currentTimeMillis() < deadline) {
                Shadows.shadowOf(Looper.getMainLooper()).idle()
                val latest = db.toolkitDao().getAllCardsOnce().firstOrNull()
                if (latest != null && latest.mastered) break
                Thread.sleep(20)
            }

            val cards = db.toolkitDao().getAllCardsOnce()
            assertEquals(1, cards.size)
            assertTrue(cards.first().mastered)
            assertEquals(1, cards.first().reviewCount)
        }
    }

    @Test
    fun budgetScreen_logsManualExpenseAndUpdatesSpentTotal() {
        runBlocking {
            composeRule.setContent {
                BudgetScreen(
                    toolkitDao = db.toolkitDao(),
                    preferencesManager = prefs,
                    onBack = {}
                )
            }

            composeRule.onNodeWithTag("budget_add_fab").performClick()
            composeRule.onNodeWithTag("budget_manual_title").performTextInput("Train Ticket")
            composeRule.onNodeWithTag("budget_manual_amount").performTextInput("25.50")
            composeRule.onNodeWithTag("budget_manual_save").performClick()

            val deadline = System.currentTimeMillis() + 3_000
            while (System.currentTimeMillis() < deadline) {
                Shadows.shadowOf(Looper.getMainLooper()).idle()
                if (db.toolkitDao().getExpensesOnce().isNotEmpty()) break
                Thread.sleep(20)
            }

            val saved = db.toolkitDao().observeExpenses().first()
            assertEquals(1, saved.size)
            assertEquals("Train Ticket", saved.first().title)
            assertEquals(25.50, saved.first().amount, 0.001)

            composeRule.waitUntil(timeoutMillis = 3_000) {
                composeRule.onAllNodesWithText("$25.50").fetchSemanticsNodes().isNotEmpty()
            }
            composeRule.onNodeWithTag("budget_list").performScrollToNode(hasText("Train Ticket"))
            composeRule.onNodeWithText("Train Ticket").assertIsDisplayed()
        }
    }

    @Test
    fun translatorScreen_rendersInteractiveControls() {
        composeRule.setContent {
            TranslatorScreen(preferencesManager = prefs, onBack = {})
        }
        composeRule.onNodeWithTag("translator_source_lang").assertIsDisplayed()
        composeRule.onNodeWithTag("translator_target_lang").assertIsDisplayed()
        composeRule.onNodeWithTag("translator_run_button").assertIsDisplayed()
        composeRule.onNodeWithTag("translator_photo_button").assertIsDisplayed()
    }

    @Test
    fun writingStudioScreen_rendersInteractiveControls() {
        composeRule.setContent {
            WritingStudioScreen(
                productivityDao = db.productivityDao(),
                preferencesManager = prefs,
                onBack = {}
            )
        }
        composeRule.onNodeWithTag("writing_draft_input").performTextInput("hello world")
        composeRule.onNodeWithTag("writing_rewrite_button").assertIsDisplayed()
    }

    @Test
    fun decisionToolsScreen_switchesTabsAndRollsDiceAndFlipsCoin() {
        composeRule.setContent {
            DecisionToolsScreen(onBack = {})
        }
        composeRule.onNodeWithTag("decision_spin_button").assertIsDisplayed()
        composeRule.onNodeWithTag("decision_tab_dice").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("decision_roll_dice_button").performClick()
        composeRule.onNodeWithTag("decision_flip_coin_button").performClick()
    }

    @Test
    fun backupManager_exportsAndImportsFlashcardsAndExpenses() {
        runBlocking {
            val deck = FlashcardDeckEntity(id = "d1", title = "Spanish", topic = "Languages")
            val card = FlashcardEntity(id = "fc1", deckId = "d1", question = "Hola", answer = "Hello", mastered = true, reviewCount = 2)
            val exp = ExpenseEntity(id = "ex1", title = "Coffee", amount = 4.75, category = "Food", isIncome = false)
            db.toolkitDao().upsertDeck(deck)
            db.toolkitDao().upsertCard(card)
            db.toolkitDao().upsertExpense(exp)

            val (json, exportReport) = BackupManager(db).exportJson()
            assertEquals(1, exportReport.flashcardDecks)
            assertEquals(1, exportReport.flashcards)
            assertEquals(1, exportReport.expenses)

            val freshDb = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
                .allowMainThreadQueries()
                .build()
            try {
                val importReport = BackupManager(freshDb).importJson(json)
                assertEquals(1, importReport.flashcardDecks)
                assertEquals(1, importReport.flashcards)
                assertEquals(1, importReport.expenses)
                assertEquals("Spanish", freshDb.toolkitDao().getDecksOnce().first().title)
                assertEquals("Coffee", freshDb.toolkitDao().getExpensesOnce().first().title)
            } finally {
                freshDb.close()
            }
        }
    }

    @Test
    fun budgetScreen_quickPresetAndClickToEdit_updatesExistingExpense() {
        runBlocking {
            composeRule.setContent {
                BudgetScreen(
                    toolkitDao = db.toolkitDao(),
                    preferencesManager = prefs,
                    onBack = {}
                )
            }

            composeRule.onNodeWithTag("budget_quick_Coffee").performClick()
            val deadline = System.currentTimeMillis() + 3_000
            while (System.currentTimeMillis() < deadline) {
                Shadows.shadowOf(Looper.getMainLooper()).idle()
                if (db.toolkitDao().getExpensesOnce().isNotEmpty()) break
                Thread.sleep(20)
            }
            val created = db.toolkitDao().getExpensesOnce().single()
            assertEquals("Coffee", created.title)
            assertEquals(5.0, created.amount, 0.001)

            composeRule.waitUntil(timeoutMillis = 3_000) {
                composeRule.onAllNodesWithText("$5.00").fetchSemanticsNodes().isNotEmpty()
            }
            composeRule.onNodeWithTag("budget_list").performScrollToNode(
                androidx.compose.ui.test.hasTestTag("budget_entry_${created.id}")
            )
            composeRule.onNodeWithTag("budget_entry_${created.id}").performClick()
            composeRule.waitForIdle()

            composeRule.onNodeWithTag("budget_manual_title").performTextInput(" Latte")
            composeRule.onNodeWithTag("budget_manual_save").performClick()

            val editDeadline = System.currentTimeMillis() + 3_000
            while (System.currentTimeMillis() < editDeadline) {
                Shadows.shadowOf(Looper.getMainLooper()).idle()
                if (db.toolkitDao().getExpensesOnce().firstOrNull()?.title?.contains("Latte") == true) break
                Thread.sleep(20)
            }
            val updated = db.toolkitDao().getExpensesOnce()
            assertEquals(1, updated.size)
            assertTrue(updated.single().title.contains("Latte"))
        }
    }

    @Test
    fun flashcardsScreen_studyFiltersSwapAndNeedPracticeWork() {
        runBlocking {
            val deck = FlashcardDeckEntity(id = "d_study", title = "Science", topic = "Physics")
            db.toolkitDao().upsertDeck(deck)
            db.toolkitDao().insertCards(
                listOf(
                    FlashcardEntity(id = "c1", deckId = "d_study", question = "Symbol for Water?", answer = "H2O", mastered = true, createdAt = 1L),
                    FlashcardEntity(id = "c2", deckId = "d_study", question = "Speed of Light unit?", answer = "m/s", mastered = false, createdAt = 2L)
                )
            )

            composeRule.setContent {
                FlashcardsScreen(
                    toolkitDao = db.toolkitDao(),
                    productivityDao = db.productivityDao(),
                    onBack = {}
                )
            }

            composeRule.waitUntil(timeoutMillis = 3_000) {
                composeRule.onAllNodesWithText("Science").fetchSemanticsNodes().isNotEmpty()
            }
            composeRule.onNodeWithText("Science").performClick()

            composeRule.waitUntil(timeoutMillis = 3_000) {
                composeRule.onAllNodesWithText("Card 1 of 2").fetchSemanticsNodes().isNotEmpty()
            }

            composeRule.onNodeWithTag("flashcards_swap_qa").performClick()
            composeRule.waitUntil(timeoutMillis = 3_000) {
                composeRule.onAllNodesWithText("H2O").fetchSemanticsNodes().isNotEmpty()
            }

            composeRule.onNodeWithTag("flashcards_filter_practice").performClick()
            composeRule.waitUntil(timeoutMillis = 3_000) {
                composeRule.onAllNodesWithText("Card 1 of 1").fetchSemanticsNodes().isNotEmpty()
            }
            composeRule.onAllNodesWithText("m/s").onFirst().assertIsDisplayed()
        }
    }

    @Test
    fun calculatorScreen_historyTapeAndTipSplitWorkAccurately() {
        composeRule.setContent {
            CalculatorScreen(onBack = {})
        }

        composeRule.onNodeWithTag("calc_key_2").performClick()
        composeRule.onNodeWithTag("calc_key_+").performClick()
        composeRule.onNodeWithTag("calc_key_3").performClick()
        composeRule.onNodeWithTag("calc_key_=").performClick()
        composeRule.onNodeWithTag("calc_history_chip_0").assertIsDisplayed()

        composeRule.onNodeWithTag("calc_tab_tip").performClick()
        composeRule.onNodeWithTag("tip_percent_20").performClick()
        composeRule.waitUntil(timeoutMillis = 3_000) {
            composeRule.onAllNodesWithText("$60.00").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("tip_per_person_value").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("$60.00").assertIsDisplayed()
    }

    @Test
    fun notesScreen_tagFilterStripFiltersNotes() {
        runBlocking {
            db.productivityDao().insertNote(
                NoteEntity(id = "n1", title = "Exam Review", content = "Chapters 1 to 4", tags = "study", updatedAt = 2L)
            )
            db.productivityDao().insertNote(
                NoteEntity(id = "n2", title = "Grocery Run", content = "Milk and bread", tags = "home", updatedAt = 1L)
            )

            composeRule.setContent {
                NotesScreen(
                    productivityDao = db.productivityDao(),
                    onBack = {}
                )
            }

            composeRule.waitUntil(timeoutMillis = 3_000) {
                composeRule.onAllNodesWithText("Exam Review").fetchSemanticsNodes().isNotEmpty()
            }
            composeRule.onNodeWithTag("notes_list").performScrollToNode(hasText("Grocery Run"))
            composeRule.onNodeWithText("Grocery Run").assertIsDisplayed()

            composeRule.onNodeWithTag("notes_tag_filter_study").performClick()
            composeRule.waitUntil(timeoutMillis = 3_000) {
                composeRule.onAllNodesWithText("Grocery Run").fetchSemanticsNodes().isEmpty()
            }
            composeRule.onNodeWithText("Exam Review").assertIsDisplayed()

            composeRule.onNodeWithTag("notes_tag_filter_All").performClick()
            composeRule.waitUntil(timeoutMillis = 3_000) {
                composeRule.onAllNodesWithText("Exam Review").fetchSemanticsNodes().isNotEmpty()
            }
            composeRule.onNodeWithTag("notes_list").performScrollToNode(hasText("Grocery Run"))
            composeRule.onNodeWithText("Grocery Run").assertIsDisplayed()
        }
    }

    @Test
    fun calendarScreen_priorityFilterAndQuickBarRender() {
        runBlocking {
            db.productivityDao().insertEvent(
                CalendarEventEntity(id = "ev1", title = "Board Demo", description = "Slides", location = "Room A", priority = "High", timestamp = 2000L)
            )
            db.productivityDao().insertEvent(
                CalendarEventEntity(id = "ev2", title = "Lunch Break", description = "Tacos", location = "Cafe", priority = "Low", timestamp = 1000L)
            )

            composeRule.setContent {
                CalendarScreen(
                    productivityDao = db.productivityDao(),
                    onBack = {}
                )
            }

            composeRule.onNodeWithTag("calendar_smart_input").assertIsDisplayed()
            composeRule.waitUntil(timeoutMillis = 3_000) {
                composeRule.onAllNodesWithText("Lunch Break").fetchSemanticsNodes().isNotEmpty()
            }

            composeRule.onNodeWithTag("calendar_list").performScrollToNode(
                androidx.compose.ui.test.hasTestTag("calendar_filter_High")
            )
            composeRule.onNodeWithTag("calendar_filter_High").performClick()
            composeRule.waitUntil(timeoutMillis = 3_000) {
                composeRule.onAllNodesWithText("Lunch Break").fetchSemanticsNodes().isEmpty()
            }
            composeRule.onNodeWithTag("calendar_list").performScrollToNode(hasText("Board Demo"))
            composeRule.onNodeWithText("Board Demo").assertIsDisplayed()
        }
    }

    @Test
    fun readAloudScreen_clearAndStatsUpdateWork() {
        composeRule.setContent {
            ReadAloudScreen(
                initialText = "Hello world from OmniChat",
                onBack = {}
            )
        }

        composeRule.onNodeWithTag("tts_stats_label").assertIsDisplayed()
        composeRule.onNodeWithText("4 words · ~1m listen").assertIsDisplayed()

        composeRule.onNodeWithTag("tts_clear_button").performClick()
        composeRule.waitUntil(timeoutMillis = 3_000) {
            composeRule.onAllNodesWithText("0 words · ~0m listen").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("0 words · ~0m listen").assertIsDisplayed()
    }

    @Test
    fun flashcardsScreen_resetProgress_clearsMasterySafely() {
        runBlocking {
            val deck = FlashcardDeckEntity(id = "d_reset", title = "Vocabulary", topic = "English")
            db.toolkitDao().upsertDeck(deck)
            db.toolkitDao().insertCards(
                listOf(
                    FlashcardEntity(id = "r1", deckId = "d_reset", question = "Eloquent", answer = "Fluent and persuasive", mastered = true, createdAt = 1L)
                )
            )

            composeRule.setContent {
                FlashcardsScreen(
                    toolkitDao = db.toolkitDao(),
                    productivityDao = db.productivityDao(),
                    onBack = {}
                )
            }

            composeRule.waitUntil(timeoutMillis = 3_000) {
                composeRule.onAllNodesWithText("Vocabulary").fetchSemanticsNodes().isNotEmpty()
            }
            composeRule.onNodeWithText("Vocabulary").performClick()
            composeRule.waitUntil(timeoutMillis = 3_000) {
                composeRule.onAllNodesWithText("Card 1 of 1").fetchSemanticsNodes().isNotEmpty()
            }

            composeRule.onNodeWithTag("flashcards_reset_progress").performClick()
            val deadline = System.currentTimeMillis() + 3_000
            while (System.currentTimeMillis() < deadline) {
                Shadows.shadowOf(Looper.getMainLooper()).idle()
                if (db.toolkitDao().getAllCardsOnce().firstOrNull()?.mastered == false) break
                Thread.sleep(20)
            }
            assertEquals(false, db.toolkitDao().getAllCardsOnce().single().mastered)
        }
    }

    @Test
    fun writingStudioScreen_subjectLinesModeAndCountsUpdate() {
        composeRule.setContent {
            WritingStudioScreen(
                productivityDao = db.productivityDao(),
                preferencesManager = prefs,
                onBack = {}
            )
        }

        composeRule.onNodeWithTag("writing_mode_SUBJECT_LINES").performScrollTo().performClick()
        composeRule.onNodeWithText("Apply Titles & Subjects").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("writing_draft_input").performScrollTo().performTextInput("Launch announcement for OmniChat")
        composeRule.onNodeWithText("4 words · 32 chars").assertIsDisplayed()
    }
}

