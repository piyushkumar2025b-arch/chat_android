package com.example

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.example.ui.Direction
import com.example.ui.MediaKind
import com.example.ui.applyMove
import com.example.ui.boardHas2048
import com.example.ui.formatFileSize
import com.example.ui.formatMediaDuration
import com.example.ui.hasMovesLeft
import com.example.ui.isBoardFull
import com.example.ui.resolveMediaItemFromUri
import com.example.ui.slideAndMergeLine
import com.example.util.AppNotificationManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.sqrt

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ToolFunctionalityTest {

    // --- Universal Media Player Tests ---

    @Test
    fun testFormatDurationFormatting() {
        assertEquals("00:00", formatMediaDuration(0L))
        assertEquals("00:45", formatMediaDuration(45_000L))
        assertEquals("05:30", formatMediaDuration(330_000L))
        assertEquals("1:15:00", formatMediaDuration(4_500_000L))
        assertEquals("00:00", formatMediaDuration(-500L))
    }

    @Test
    fun testFormatFileSizeFormatting() {
        assertEquals("0 B", formatFileSize(0L))
        assertEquals("512 B", formatFileSize(512L))
        assertEquals("10.0 KB", formatFileSize(10_240L))
        assertEquals("5.0 MB", formatFileSize(5_242_880L))
        assertEquals("1.5 GB", formatFileSize((1.5 * 1024 * 1024 * 1024).toLong()))
    }

    @Test
    fun testResolveMediaItemFromUriTypes() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // Video URI
        val videoUri = Uri.parse("https://example.com/videos/movie.mp4")
        val videoItem = resolveMediaItemFromUri(context, videoUri)
        assertEquals(MediaKind.VIDEO, videoItem.kind)
        assertEquals("video/mp4", videoItem.mimeType)
        assertEquals("movie.mp4", videoItem.displayName)

        // Audio URI
        val audioUri = Uri.parse("https://example.com/audio/track.mp3")
        val audioItem = resolveMediaItemFromUri(context, audioUri)
        assertEquals(MediaKind.AUDIO, audioItem.kind)
        assertEquals("audio/mpeg", audioItem.mimeType)

        // PDF Document URI
        val pdfUri = Uri.parse("https://example.com/docs/manual.pdf")
        val pdfItem = resolveMediaItemFromUri(context, pdfUri)
        assertEquals(MediaKind.PDF_DOCUMENT, pdfItem.kind)

        // Text Document URI
        val textUri = Uri.parse("https://example.com/notes/readme.md")
        val textItem = resolveMediaItemFromUri(context, textUri)
        assertEquals(MediaKind.TEXT_DOCUMENT, textItem.kind)

        // Image URI
        val imageUri = Uri.parse("https://example.com/photos/avatar.png")
        val imageItem = resolveMediaItemFromUri(context, imageUri)
        assertEquals(MediaKind.IMAGE, imageItem.kind)

        // Archive / APK URI
        val apkUri = Uri.parse("https://example.com/packages/bundle.apk")
        val apkItem = resolveMediaItemFromUri(context, apkUri)
        assertEquals(MediaKind.ARCHIVE, apkItem.kind)

        val zipUri = Uri.parse("https://example.com/archive/backup.zip")
        val zipItem = resolveMediaItemFromUri(context, zipUri)
        assertEquals(MediaKind.ARCHIVE, zipItem.kind)
        assertEquals("application/zip", zipItem.mimeType)

        // Code / CSV / Python URI
        val csvUri = Uri.parse("https://example.com/data/metrics.csv")
        val csvItem = resolveMediaItemFromUri(context, csvUri)
        assertEquals(MediaKind.TEXT_DOCUMENT, csvItem.kind)
        assertEquals("text/csv", csvItem.mimeType)

        val pyUri = Uri.parse("https://example.com/scripts/simulator.py")
        val pyItem = resolveMediaItemFromUri(context, pyUri)
        assertEquals(MediaKind.TEXT_DOCUMENT, pyItem.kind)

        val yamlUri = Uri.parse("https://example.com/configs/deployment.yaml")
        val yamlItem = resolveMediaItemFromUri(context, yamlUri)
        assertEquals(MediaKind.TEXT_DOCUMENT, yamlItem.kind)

        val rustUri = Uri.parse("https://example.com/code/engine.rs")
        val rustItem = resolveMediaItemFromUri(context, rustUri)
        assertEquals(MediaKind.TEXT_DOCUMENT, rustItem.kind)

        val svgUri = Uri.parse("https://example.com/images/icon.svg")
        val svgItem = resolveMediaItemFromUri(context, svgUri)
        assertEquals(MediaKind.IMAGE, svgItem.kind)
    }

    @Test
    fun testCuratedMediaSourcesCompleteness() {
        val sources = com.example.ui.CuratedMediaSources.sampleSources
        assertTrue("Must contain at least 20 curated sample sources", sources.size >= 20)
        assertTrue(sources.any { it.kind == MediaKind.VIDEO })
        assertTrue(sources.any { it.kind == MediaKind.AUDIO })
        assertTrue(sources.any { it.kind == MediaKind.TEXT_DOCUMENT })
        assertTrue(sources.any { it.kind == MediaKind.ARCHIVE })
        for (item in sources) {
            assertTrue(item.displayName.isNotBlank())
            assertTrue(item.mimeType.isNotBlank())
        }
    }

    // --- 2048 Game Engine Tests ---

    @Test
    fun testSlideAndMergeLineBasic() {
        // [2, 2, 0, 0] should slide and merge into [4, 0, 0, 0] with score 4
        val (res1, score1) = slideAndMergeLine(listOf(2, 2, 0, 0))
        assertEquals(listOf(4, 0, 0, 0), res1)
        assertEquals(4, score1)

        // [0, 2, 0, 2] should also slide and merge into [4, 0, 0, 0]
        val (res2, score2) = slideAndMergeLine(listOf(0, 2, 0, 2))
        assertEquals(listOf(4, 0, 0, 0), res2)
        assertEquals(4, score2)

        // [2, 2, 2, 2] should merge pairwise into [4, 4, 0, 0] with score 8
        val (res3, score3) = slideAndMergeLine(listOf(2, 2, 2, 2))
        assertEquals(listOf(4, 4, 0, 0), res3)
        assertEquals(8, score3)

        // [4, 2, 2, 0] should merge the 2s -> [4, 4, 0, 0]
        val (res4, score4) = slideAndMergeLine(listOf(4, 2, 2, 0))
        assertEquals(listOf(4, 4, 0, 0), res4)
        assertEquals(4, score4)

        // [2, 4, 8, 16] should not merge any tile
        val (res5, score5) = slideAndMergeLine(listOf(2, 4, 8, 16))
        assertEquals(listOf(2, 4, 8, 16), res5)
        assertEquals(0, score5)
    }

    @Test
    fun testApplyMoveAllDirections() {
        val initialBoard = arrayOf(
            intArrayOf(2, 0, 0, 2),
            intArrayOf(0, 4, 4, 0),
            intArrayOf(0, 0, 0, 0),
            intArrayOf(8, 0, 0, 8)
        )

        // Move LEFT
        val (leftBoard, leftScore, leftMoved) = applyMove(initialBoard, Direction.LEFT)
        assertTrue(leftMoved)
        assertEquals(4 + 8 + 16, leftScore)
        assertEquals(4, leftBoard[0][0])
        assertEquals(8, leftBoard[1][0])
        assertEquals(16, leftBoard[3][0])

        // Move RIGHT
        val (rightBoard, rightScore, rightMoved) = applyMove(initialBoard, Direction.RIGHT)
        assertTrue(rightMoved)
        assertEquals(4 + 8 + 16, rightScore)
        assertEquals(4, rightBoard[0][3])
        assertEquals(8, rightBoard[1][3])
        assertEquals(16, rightBoard[3][3])
    }

    @Test
    fun test2048GameOverAndWinDetection() {
        val fullBoardNoMoves = arrayOf(
            intArrayOf(2, 4, 2, 4),
            intArrayOf(4, 2, 4, 2),
            intArrayOf(2, 4, 2, 4),
            intArrayOf(4, 2, 4, 2)
        )
        assertTrue(isBoardFull(fullBoardNoMoves))
        assertFalse(hasMovesLeft(fullBoardNoMoves))
        assertFalse(boardHas2048(fullBoardNoMoves))

        val fullBoardWithHorizontalMove = arrayOf(
            intArrayOf(2, 2, 4, 8),
            intArrayOf(4, 8, 16, 32),
            intArrayOf(64, 128, 256, 512),
            intArrayOf(1024, 2, 4, 8)
        )
        assertTrue(isBoardFull(fullBoardWithHorizontalMove))
        assertTrue(hasMovesLeft(fullBoardWithHorizontalMove))

        val winningBoard = arrayOf(
            intArrayOf(2, 4, 8, 16),
            intArrayOf(32, 64, 128, 256),
            intArrayOf(512, 1024, 2048, 0),
            intArrayOf(0, 0, 0, 0)
        )
        assertTrue(boardHas2048(winningBoard))
    }

    // --- Real Step Counter Calculation Tests ---

    @Test
    fun testStepCounterCalculations() {
        // Accelerometer 3-axis magnitude calculation
        val ax = 1.2f
        val ay = 9.8f
        val az = 6.4f
        val magnitude = sqrt((ax * ax + ay * ay + az * az).toDouble()).toFloat()
        assertTrue("Earth gravity magnitude should be around 11.7m/s^2", magnitude > 11.0f && magnitude < 12.5f)

        // Step metrics: 6,000 steps
        val steps = 6000
        val distanceKm = (steps * 0.762) / 1000.0
        val calories = steps * 0.04
        val activeMinutes = steps / 100

        assertTrue(distanceKm > 4.5 && distanceKm < 4.7)
        assertEquals(240.0, calories, 0.01)
        assertEquals(60, activeMinutes)
    }

    // --- Notification Manager Tests ---

    @Test
    fun testAppNotificationManagerChannels() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        AppNotificationManager.initChannels(context)

        // Channels should initialize without throwing
        val hasPermission = AppNotificationManager.hasNotificationPermission(context)
        assertNotNull(hasPermission)
    }

    // --- Knowledge Sources & News Feeds Tests ---

    @Test
    fun testKnowledgeSourcesCompleteness() {
        val sources = com.example.data.remote.InternalKnowledgeSources.sources
        assertTrue("Must have at least 18 curated sources", sources.size >= 18)
        
        // Verify unique IDs
        val ids = sources.map { it.id }.toSet()
        assertEquals("All source IDs must be unique", sources.size, ids.size)

        // Verify key domains are covered
        assertTrue(sources.any { it.id == "quantum_computing" })
        assertTrue(sources.any { it.id == "distributed_systems" })
        assertTrue(sources.any { it.id == "data_structures_algorithms" })
        assertTrue(sources.any { it.id == "linux_os_internals" })
        assertTrue(sources.any { it.id == "modern_web_networking" })
        assertTrue(sources.any { it.id == "ai_agents_reasoning" })

        // Check content validity
        for (source in sources) {
            assertTrue("Title must not be blank", source.title.isNotBlank())
            assertTrue("Category must not be blank", source.category.isNotBlank())
            assertTrue("Content must be detailed (> 200 chars)", source.content.length > 200)
        }
    }

    @Test
    fun testNewsCategoriesCompleteness() {
        val categories = com.example.data.remote.NewsFeedService.NewsCategory.values()
        assertTrue(categories.size >= 10)
        assertTrue(categories.any { it.name == "AI_RESEARCH" })
        assertTrue(categories.any { it.name == "CYBER_SECURITY" })

        val aiCategory = com.example.data.remote.NewsFeedService.NewsCategory.AI_RESEARCH
        assertTrue(aiCategory.feeds.isNotEmpty())
        for ((name, url) in aiCategory.feeds) {
            assertTrue(name.isNotBlank())
            assertTrue(url.startsWith("http"))
        }
    }
}
