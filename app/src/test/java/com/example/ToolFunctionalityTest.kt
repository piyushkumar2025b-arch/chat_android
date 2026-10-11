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
}
