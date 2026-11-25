package com.example.ui

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint as AndroidPaint
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.data.remote.MediaSaver
import kotlinx.coroutines.launch

enum class ColorPreset(val label: String) {
    BW("Black & White"),
    SEPIA("Sepia"),
    VIBRANT("Vibrant"),
    CYBERPUNK("Cyberpunk"),
    INVERT("Invert"),
    NORMAL("Normal")
}

data class DrawnLine(
    val start: Offset,
    val end: Offset,
    val color: Color,
    val strokeWidth: Float
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageEditorScreen(
    onBack: () -> Unit,
    onSendImageToChat: (Bitmap) -> Unit = {},
    showTopBar: Boolean = true
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val lines = remember { mutableStateListOf<DrawnLine>() }
    var currentColor by remember { mutableStateOf(Color(0xFF6366F1)) }
    var strokeWidth by remember { mutableFloatStateOf(10f) }
    var canvasSize by remember { mutableStateOf(IntSize(720, 720)) }

    val colorPalette = listOf(
        Color(0xFF6366F1), // Indigo
        Color(0xFFEF4444), // Red
        Color(0xFF10B981), // Emerald
        Color(0xFFF59E0B), // Amber
        Color(0xFF3B82F6), // Blue
        Color(0xFFEC4899), // Pink
        Color(0xFF1E293B), // Charcoal
        Color(0xFFFFFFFF)  // White
    )

    // Helper to render lines into Bitmap matching actual canvas bounds
    fun generateBitmap(): Bitmap {
        val width = canvasSize.width.coerceAtLeast(720)
        val height = canvasSize.height.coerceAtLeast(720)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = AndroidCanvas(bitmap)
        canvas.drawColor(android.graphics.Color.WHITE)

        val paint = AndroidPaint().apply {
            isAntiAlias = true
            strokeCap = AndroidPaint.Cap.ROUND
        }

        for (line in lines) {
            paint.color = line.color.toArgb()
            paint.strokeWidth = line.strokeWidth
            canvas.drawLine(line.start.x, line.start.y, line.end.x, line.end.y, paint)
        }
        return bitmap
    }

    val sendAction = {
        val bmp = generateBitmap()
        onSendImageToChat(bmp)
        Toast.makeText(context, "Image sent to chat", Toast.LENGTH_SHORT).show()
        if (showTopBar) onBack()
    }
    val clearAction = {
        lines.clear()
        Toast.makeText(context, "Canvas cleared", Toast.LENGTH_SHORT).show()
    }
    val saveAction = {
        val bmp = generateBitmap()
        coroutineScope.launch {
            MediaSaver.saveBitmapToGallery(context, bmp, "OmniSketch")
        }
        Unit
    }

    @Composable
    fun EditorBody(modifier: Modifier = Modifier) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!showTopBar) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Sketch Canvas",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = clearAction,
                            modifier = Modifier.testTag("clear_sketch_button")
                        ) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear Canvas")
                        }
                        IconButton(
                            onClick = saveAction,
                            modifier = Modifier.testTag("save_sketch_button")
                        ) {
                            Icon(Icons.Default.Save, contentDescription = "Save Drawing")
                        }
                        IconButton(
                            onClick = sendAction,
                            modifier = Modifier.testTag("send_image_to_chat_button")
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send to Chat",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
            // Drawing Surface
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .onSizeChanged { canvasSize = it }
                        .pointerInput(true) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                val start = change.position - dragAmount
                                val end = change.position
                                lines.add(DrawnLine(start, end, currentColor, strokeWidth))
                            }
                        }
                ) {
                    lines.forEach { line ->
                        drawLine(
                            color = line.color,
                            start = line.start,
                            end = line.end,
                            strokeWidth = line.strokeWidth,
                            cap = StrokeCap.Round
                        )
                    }
                }
            }

            // Brush Palette & Controls
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Brush Color", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        colorPalette.forEach { color ->
                            val isSelected = currentColor == color
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .clickable { currentColor = color },
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(
                                            width = if (isSelected) 3.dp else 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray,
                                            shape = CircleShape
                                        )
                                )
                            }
                        }
                    }

                    Text("Stroke Width (${strokeWidth.toInt()}px)", style = MaterialTheme.typography.labelMedium)
                    Slider(
                        value = strokeWidth,
                        onValueChange = { strokeWidth = it },
                        valueRange = 2f..40f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    if (showTopBar) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Sketch Pad", fontWeight = FontWeight.SemiBold) },
                    navigationIcon = {
                        IconButton(onClick = onBack, modifier = Modifier.testTag("image_editor_back")) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = sendAction,
                            modifier = Modifier.testTag("send_image_to_chat_button")
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send to Chat",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(
                            onClick = clearAction,
                            modifier = Modifier.testTag("clear_sketch_button")
                        ) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear Canvas")
                        }
                        IconButton(
                            onClick = saveAction,
                            modifier = Modifier.testTag("save_sketch_button")
                        ) {
                            Icon(Icons.Default.Save, contentDescription = "Save Drawing")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        ) { innerPadding ->
            EditorBody(modifier = Modifier.padding(innerPadding))
        }
    } else {
        EditorBody()
    }
}
