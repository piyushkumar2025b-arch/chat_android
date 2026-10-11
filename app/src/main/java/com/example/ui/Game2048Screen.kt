package com.example.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.AppNotificationManager
import kotlin.math.abs
import kotlin.random.Random

enum class Direction { UP, DOWN, LEFT, RIGHT }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Game2048Screen(
    onBack: (() -> Unit)? = null,
    onSendScoreToChat: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("omnichat_game2048", Context.MODE_PRIVATE) }

    var bestScore by remember { mutableIntStateOf(prefs.getInt("best_score", 0)) }
    var currentScore by remember { mutableIntStateOf(0) }
    var previousBoard by remember { mutableStateOf<Array<IntArray>?>(null) }
    var previousScore by remember { mutableIntStateOf(0) }

    var board by remember {
        mutableStateOf(Array(4) { IntArray(4) }.also { b ->
            spawnTile(b)
            spawnTile(b)
        })
    }

    var isGameOver by remember { mutableStateOf(false) }
    var hasWon by remember { mutableStateOf(false) }
    var userDismissedWin by remember { mutableStateOf(false) }

    fun updateScore(added: Int) {
        currentScore += added
        if (currentScore > bestScore) {
            bestScore = currentScore
            prefs.edit().putInt("best_score", bestScore).apply()
        }
    }

    fun makeMove(direction: Direction) {
        if (isGameOver) return

        val prevCopy = Array(4) { r -> board[r].clone() }
        val prevScoreCopy = currentScore

        val (newBoard, scoreGained, moved) = applyMove(board, direction)

        if (moved) {
            previousBoard = prevCopy
            previousScore = prevScoreCopy
            board = newBoard
            updateScore(scoreGained)

            // Spawn random tile
            spawnTile(board)

            // Check 2048 win
            if (!hasWon && !userDismissedWin && boardHas2048(board)) {
                hasWon = true
                AppNotificationManager.sendNotification(
                    context = context,
                    title = "🏆 2048 Tile Reached!",
                    body = "Congratulations! You conquered 2048 with a score of $currentScore!",
                    channelId = AppNotificationManager.CHANNEL_FITNESS
                )
            }

            // Check game over
            if (isBoardFull(board) && !hasMovesLeft(board)) {
                isGameOver = true
            }
        }
    }

    fun restartGame() {
        val newBoard = Array(4) { IntArray(4) }
        spawnTile(newBoard)
        spawnTile(newBoard)
        board = newBoard
        previousBoard = null
        currentScore = 0
        isGameOver = false
        hasWon = false
        userDismissedWin = false
    }

    fun undoMove() {
        if (previousBoard != null) {
            board = Array(4) { r -> previousBoard!![r].clone() }
            currentScore = previousScore
            previousBoard = null
            isGameOver = false
        }
    }

    var totalDragX by remember { mutableStateOf(0f) }
    var totalDragY by remember { mutableStateOf(0f) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top App Bar
        Surface(
            tonalElevation = 2.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEDC22E)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("2048", fontWeight = FontWeight.Black, fontSize = 11.sp, color = Color.White)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "2048 Classic",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilledTonalIconButton(
                        onClick = { undoMove() },
                        enabled = previousBoard != null
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo")
                    }
                    FilledTonalIconButton(
                        onClick = { restartGame() }
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Restart")
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Scores Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Score Box
                ElevatedCard(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("SCORE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Text(
                            text = "$currentScore",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Best Score Box
                ElevatedCard(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("BEST", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$bestScore",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFF59E0B)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Game Instructions & Tip
            Text(
                text = "Swipe in any direction or use arrow buttons to merge tiles!",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 4x4 Game Board with Swipe Gestures
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFBBADA0))
                    .padding(8.dp)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = {
                                totalDragX = 0f
                                totalDragY = 0f
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                totalDragX += dragAmount.x
                                totalDragY += dragAmount.y
                            },
                            onDragEnd = {
                                val minSwipe = 40f
                                if (abs(totalDragX) > abs(totalDragY)) {
                                    if (totalDragX > minSwipe) makeMove(Direction.RIGHT)
                                    else if (totalDragX < -minSwipe) makeMove(Direction.LEFT)
                                } else {
                                    if (totalDragY > minSwipe) makeMove(Direction.DOWN)
                                    else if (totalDragY < -minSwipe) makeMove(Direction.UP)
                                }
                            }
                        )
                    }
                    .testTag("game_2048_board")
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (row in 0..3) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (col in 0..3) {
                                val value = board[row][col]
                                TileView(
                                    value = value,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // D-Pad Touch Controls (Alternative to swiping for accessibility)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                FilledTonalButton(
                    onClick = { makeMove(Direction.UP) },
                    modifier = Modifier.size(52.dp),
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Up")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(40.dp)) {
                    FilledTonalButton(
                        onClick = { makeMove(Direction.LEFT) },
                        modifier = Modifier.size(52.dp),
                        shape = CircleShape
                    ) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Left")
                    }
                    FilledTonalButton(
                        onClick = { makeMove(Direction.RIGHT) },
                        modifier = Modifier.size(52.dp),
                        shape = CircleShape
                    ) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Right")
                    }
                }
                FilledTonalButton(
                    onClick = { makeMove(Direction.DOWN) },
                    modifier = Modifier.size(52.dp),
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Down")
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Share to Chat Action
            if (onSendScoreToChat != null) {
                Button(
                    onClick = {
                        val maxTile = board.maxOf { row -> row.maxOrNull() ?: 0 }
                        val summary = "🎮 2048 Game Result:\n• Score: $currentScore\n• Best Score: $bestScore\n• Highest Tile: $maxTile\nCan you beat my score?"
                        onSendScoreToChat(summary)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Share Score to Chat")
                }
            }
        }
    }

    // Game Over Dialog
    if (isGameOver) {
        AlertDialog(
            onDismissRequest = { isGameOver = false },
            title = { Text("Game Over! 🕹️") },
            text = {
                Text("No more moves available! Your final score is $currentScore (Best: $bestScore).")
            },
            confirmButton = {
                Button(onClick = { restartGame() }) {
                    Text("Play Again")
                }
            },
            dismissButton = {
                TextButton(onClick = { isGameOver = false }) {
                    Text("Review Board")
                }
            }
        )
    }

    // Win Dialog
    if (hasWon && !userDismissedWin) {
        AlertDialog(
            onDismissRequest = { userDismissedWin = true },
            title = { Text("🎉 You Reached 2048!") },
            text = {
                Text("Magnificent! You created the 2048 tile with a score of $currentScore. Would you like to keep going for 4096?")
            },
            confirmButton = {
                Button(onClick = { userDismissedWin = true }) {
                    Text("Keep Playing")
                }
            },
            dismissButton = {
                TextButton(onClick = { restartGame() }) {
                    Text("New Game")
                }
            }
        )
    }
}

@Composable
private fun TileView(value: Int, modifier: Modifier = Modifier) {
    val bgColor = when (value) {
        0 -> Color(0xFFCDC1B4)
        2 -> Color(0xFFEEE4DA)
        4 -> Color(0xFFEDE0C8)
        8 -> Color(0xFFF2B179)
        16 -> Color(0xFFF59563)
        32 -> Color(0xFFF67C5F)
        64 -> Color(0xFFF65E3B)
        128 -> Color(0xFFEDCF72)
        256 -> Color(0xFFEDCC61)
        512 -> Color(0xFFEDC850)
        1024 -> Color(0xFFEDC53F)
        2048 -> Color(0xFFEDC22E)
        else -> Color(0xFF3C3A32)
    }

    val textColor = if (value in 2..4) Color(0xFF776E65) else Color.White
    val fontSize = when {
        value >= 1024 -> 17.sp
        value >= 128 -> 20.sp
        else -> 24.sp
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        if (value > 0) {
            Text(
                text = "$value",
                fontWeight = FontWeight.Bold,
                fontSize = fontSize,
                color = textColor,
                textAlign = TextAlign.Center
            )
        }
    }
}

// 2048 Logic Helpers
private fun spawnTile(board: Array<IntArray>) {
    val emptyCells = mutableListOf<Pair<Int, Int>>()
    for (r in 0..3) {
        for (c in 0..3) {
            if (board[r][c] == 0) emptyCells.add(Pair(r, c))
        }
    }
    if (emptyCells.isNotEmpty()) {
        val (r, c) = emptyCells[Random.nextInt(emptyCells.size)]
        board[r][c] = if (Random.nextFloat() < 0.9f) 2 else 4
    }
}

private fun isBoardFull(board: Array<IntArray>): Boolean {
    for (r in 0..3) {
        for (c in 0..3) {
            if (board[r][c] == 0) return false
        }
    }
    return true
}

private fun hasMovesLeft(board: Array<IntArray>): Boolean {
    for (r in 0..3) {
        for (c in 0..3) {
            val v = board[r][c]
            if (v == 0) return true
            if (r < 3 && board[r + 1][c] == v) return true
            if (c < 3 && board[r][c + 1] == v) return true
        }
    }
    return false
}

private fun boardHas2048(board: Array<IntArray>): Boolean {
    for (r in 0..3) {
        for (c in 0..3) {
            if (board[r][c] >= 2048) return true
        }
    }
    return false
}

private fun applyMove(board: Array<IntArray>, dir: Direction): Triple<Array<IntArray>, Int, Boolean> {
    val newBoard = Array(4) { IntArray(4) }
    var score = 0
    var changed = false

    when (dir) {
        Direction.LEFT -> {
            for (r in 0..3) {
                val row = board[r].toList()
                val (newRow, pts) = slideAndMergeLine(row)
                score += pts
                for (c in 0..3) {
                    newBoard[r][c] = newRow[c]
                    if (newBoard[r][c] != board[r][c]) changed = true
                }
            }
        }
        Direction.RIGHT -> {
            for (r in 0..3) {
                val row = board[r].reversed()
                val (newRow, pts) = slideAndMergeLine(row)
                score += pts
                val finalRow = newRow.reversed()
                for (c in 0..3) {
                    newBoard[r][c] = finalRow[c]
                    if (newBoard[r][c] != board[r][c]) changed = true
                }
            }
        }
        Direction.UP -> {
            for (c in 0..3) {
                val col = listOf(board[0][c], board[1][c], board[2][c], board[3][c])
                val (newCol, pts) = slideAndMergeLine(col)
                score += pts
                for (r in 0..3) {
                    newBoard[r][c] = newCol[r]
                    if (newBoard[r][c] != board[r][c]) changed = true
                }
            }
        }
        Direction.DOWN -> {
            for (c in 0..3) {
                val col = listOf(board[3][c], board[2][c], board[1][c], board[0][c])
                val (newCol, pts) = slideAndMergeLine(col)
                score += pts
                val finalCol = newCol.reversed()
                for (r in 0..3) {
                    newBoard[r][c] = finalCol[r]
                    if (newBoard[r][c] != board[r][c]) changed = true
                }
            }
        }
    }

    return Triple(newBoard, score, changed)
}

private fun slideAndMergeLine(line: List<Int>): Pair<List<Int>, Int> {
    val nonZero = line.filter { it != 0 }
    val result = mutableListOf<Int>()
    var score = 0
    var i = 0

    while (i < nonZero.size) {
        if (i + 1 < nonZero.size && nonZero[i] == nonZero[i + 1]) {
            val merged = nonZero[i] * 2
            result.add(merged)
            score += merged
            i += 2
        } else {
            result.add(nonZero[i])
            i++
        }
    }

    while (result.size < 4) {
        result.add(0)
    }

    return Pair(result, score)
}
