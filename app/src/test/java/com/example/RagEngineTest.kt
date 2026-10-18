package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.remote.RagEngine
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RagEngineTest {

    private lateinit var database: AppDatabase
    private lateinit var ragEngine: RagEngine

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        ragEngine = RagEngine(database.ragDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testChunkTextPreservesSentences() {
        val text = "Artificial Intelligence is transforming computing. Modern architectures utilize retrieval augmented generation. Dense vector embeddings enable semantic similarity comparison across large document corpora."
        val chunks = ragEngine.chunkText(text, chunkSize = 100, overlap = 20)
        assertTrue(chunks.isNotEmpty())
        for (chunk in chunks) {
            assertTrue(chunk.isNotBlank())
        }
    }

    @Test
    fun testGenerateLocalTfidfVectorDimensions() {
        val text = "Kotlin Jetpack Compose Android Clean Architecture"
        val vector = ragEngine.generateLocalTfidfVector(text, dimensions = 128)
        assertEquals(128, vector.size)
        // Magnitude should be approximately 1.0 (L2 normalized)
        var sumSquares = 0f
        for (v in vector) {
            sumSquares += v * v
        }
        val magnitude = kotlin.math.sqrt(sumSquares)
        assertTrue(magnitude in 0.99f..1.01f)
    }

    @Test
    fun testIngestAndSearchDocument() = runBlocking {
        val title = "Kotlin Programming Guidelines"
        val content = "Kotlin is a modern, statically typed language developed by JetBrains. It supports functional programming, coroutines for asynchronous programming, and extension functions. Android officially adopted Kotlin as its preferred language in 2019."

        val ingestResult = ragEngine.ingestDocument(title, content, sourceType = "TEST")
        assertTrue(ingestResult.isSuccess)

        val doc = ingestResult.getOrNull()
        assertNotNull(doc)
        assertEquals(title, doc?.title)
        assertTrue((doc?.chunkCount ?: 0) >= 1)

        // Search with semantic keyword match
        val results = ragEngine.search("Kotlin coroutines asynchronous", topK = 3)
        assertTrue(results.isNotEmpty())
        val topMatch = results.first()
        assertEquals(title, topMatch.chunk.docTitle)
        assertTrue(topMatch.score > 0.1f)

        // Verify grounding prompt generation
        val groundingPrompt = ragEngine.formatRagGroundingPrompt(results)
        assertTrue(groundingPrompt.contains("[KNOWLEDGE BASE RETRIEVAL GROUNDING (RAG)]"))
        assertTrue(groundingPrompt.contains("Kotlin Programming Guidelines"))
        assertTrue(groundingPrompt.contains("GUIDELINES: Answer the query accurately"))
    }

    @Test
    fun testPopulateStarterKnowledgeBase() = runBlocking {
        val result = ragEngine.populateStarterKnowledgeBase()
        assertTrue(result.isSuccess)
        val count = result.getOrNull() ?: 0
        assertTrue(count >= 3)

        val stats = ragEngine.getStats("")
        assertTrue(stats.totalDocuments >= 3)
        assertTrue(stats.totalChunks >= 3)
        assertEquals("Local Hybrid TF-IDF", stats.activeEmbeddingModel)

        // Verify query matches starter knowledge
        val results = ragEngine.search("Jetpack Compose WindowInsets", topK = 2)
        assertTrue(results.isNotEmpty())
    }
}
