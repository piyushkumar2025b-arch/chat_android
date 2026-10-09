package com.example.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas as ComposeCanvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Rotate90DegreesCw
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.remote.MediaSaver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream

enum class ColorPreset(val label: String, val icon: String) {
    ORIGINAL("Original", "✨"),
    VIBRANT("Vibrant", "🌈"),
    BW("Black & White", "🖤"),
    SEPIA("Vintage Sepia", "📜"),
    CYBERPUNK("Cyber Cool", "🔮"),
    WARM("Golden Warmth", "🌅"),
    INVERT("Negative / Invert", "🔄")
}

data class DrawPath(
    val points: List<Offset>,
    val color: Color,
    val strokeWidth: Float
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ImageEditorScreen(
    initialImageUri: Uri? = null,
    onBack: (() -> Unit)? = null,
    onSendImageToChat: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var sourceBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var processedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    // Active tool tab: 0 = Filters & Presets, 1 = Adjustments (Sliders), 2 = Transform & Crop, 3 = Draw & Text
    var selectedToolTab by remember { mutableIntStateOf(0) }

    // Adjustments
    var selectedPreset by remember { mutableStateOf(ColorPreset.ORIGINAL) }
    var brightness by remember { mutableFloatStateOf(0f) } // -100 to 100
    var contrast by remember { mutableFloatStateOf(1f) } // 0.5 to 2.0
    var saturation by remember { mutableFloatStateOf(1f) } // 0 to 2.0
    var rotationAngle by remember { mutableFloatStateOf(0f) }
    var isFlippedHorizontal by remember { mutableStateOf(false) }

    // Text Overlay
    var textWatermark by remember { mutableStateOf("") }
    var showTextOverlay by remember { mutableStateOf(false) }

    // Drawing Tool
    val drawPaths = remember { mutableStateListOf<DrawPath>() }
    var currentDrawColor by remember { mutableStateOf(Color.Red) }
    var currentBrushSize by remember { mutableFloatStateOf(8f) }
    var currentPoints by remember { mutableStateOf<List<Offset>>(emptyList()) }

    // Photo picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                isProcessing = true
                sourceBitmap = loadBitmapFromUri(context, uri)
                resetAdjustments(
                    onReset = {
                        brightness = 0f
                        contrast = 1f
                        saturation = 1f
                        rotationAngle = 0f
                        isFlippedHorizontal = false
                        selectedPreset = ColorPreset.ORIGINAL
                        textWatermark = ""
                        drawPaths.clear()
                    }
                )
                isProcessing = false
            }
        }
    }

    // Load initial or default sample image if null
    LaunchedEffect(initialImageUri) {
        if (initialImageUri != null) {
            isProcessing = true
            sourceBitmap = loadBitmapFromUri(context, initialImageUri)
            isProcessing = false
        } else if (sourceBitmap == null) {
            // Generate creative sample canvas with modern gradient
            sourceBitmap = createSampleArtworkBitmap()
        }
    }

    // Re-render processed bitmap when adjustments change
    LaunchedEffect(
        sourceBitmap,
        selectedPreset,
        brightness,
        contrast,
        saturation,
        rotationAngle,
        isFlippedHorizontal,
        textWatermark,
        drawPaths.size
    ) {
        val src = sourceBitmap ?: return@LaunchedEffect
        isProcessing = true
        withContext(Dispatchers.Default) {
            processedBitmap = applyImageEdits(
                source = src,
                preset = selectedPreset,
                brightness = brightness,
                contrast = contrast,
                saturation = saturation,
                rotationAngle = rotationAngle,
                flippedHorizontal = isFlippedHorizontal,
                watermark = textWatermark,
                drawings = drawPaths.toList()
            )
        }
        isProcessing = false
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Action Bar
        Surface(
            tonalElevation = 3.dp,
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
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Photo & Art Editor",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Filters • Curves • Brush • Export",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Pick new image button
                    IconButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.testTag("editor_pick_image_btn")
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Open Photo")
                    }

                    // Reset button
                    IconButton(
                        onClick = {
                            brightness = 0f
                            contrast = 1f
                            saturation = 1f
                            rotationAngle = 0f
                            isFlippedHorizontal = false
                            selectedPreset = ColorPreset.ORIGINAL
                            textWatermark = ""
                            drawPaths.clear()
                            Toast.makeText(context, "Adjustments reset", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.testTag("editor_reset_btn")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset Edits")
                    }

                    // Save / Download button
                    IconButton(
                        onClick = {
                            val bmp = processedBitmap ?: sourceBitmap
                            if (bmp != null) {
                                coroutineScope.launch {
                                    MediaSaver.saveBitmapToGallery(
                                        context = context,
                                        bitmap = bmp,
                                        title = "OmniChat_Edited"
                                    )
                                }
                            } else {
                                Toast.makeText(context, "No image to save", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.testTag("editor_save_btn")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "Save to Gallery", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        // Preview Canvas Area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color.Black.copy(alpha = 0.9f))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            val displayBmp = processedBitmap ?: sourceBitmap

            if (displayBmp != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.foundation.Image(
                        bitmap = displayBmp.asImageBitmap(),
                        contentDescription = "Edited Image Preview",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )

                    // Touch drawing overlay when in Draw mode (tab 3)
                    if (selectedToolTab == 3) {
                        ComposeCanvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(currentDrawColor, currentBrushSize) {
                                    detectDragGestures(
                                        onDragStart = { offset ->
                                            currentPoints = listOf(offset)
                                        },
                                        onDrag = { change, _ ->
                                            change.consume()
                                            currentPoints = currentPoints + change.position
                                        },
                                        onDragEnd = {
                                            if (currentPoints.isNotEmpty()) {
                                                drawPaths.add(
                                                    DrawPath(
                                                        points = currentPoints,
                                                        color = currentDrawColor,
                                                        strokeWidth = currentBrushSize
                                                    )
                                                )
                                                currentPoints = emptyList()
                                            }
                                        }
                                    )
                                }
                        ) {
                            if (currentPoints.size > 1) {
                                for (i in 0 until currentPoints.size - 1) {
                                    drawLine(
                                        color = currentDrawColor,
                                        start = currentPoints[i],
                                        end = currentPoints[i + 1],
                                        strokeWidth = currentBrushSize
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Image,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "No Image Selected",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pick Photo from Gallery")
                    }
                }
            }

            if (isProcessing) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.35f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        // Tools Tabs Header
        TabRow(
            selectedTabIndex = selectedToolTab,
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.fillMaxWidth().testTag("editor_tab_row")
        ) {
            Tab(
                selected = selectedToolTab == 0,
                onClick = { selectedToolTab = 0 },
                text = { Text("Presets") },
                icon = { Icon(Icons.Default.ColorLens, contentDescription = "Presets") }
            )
            Tab(
                selected = selectedToolTab == 1,
                onClick = { selectedToolTab = 1 },
                text = { Text("Adjust") },
                icon = { Icon(Icons.Default.Tune, contentDescription = "Adjust") }
            )
            Tab(
                selected = selectedToolTab == 2,
                onClick = { selectedToolTab = 2 },
                text = { Text("Transform") },
                icon = { Icon(Icons.Default.Rotate90DegreesCw, contentDescription = "Transform") }
            )
            Tab(
                selected = selectedToolTab == 3,
                onClick = { selectedToolTab = 3 },
                text = { Text("Draw & Text") },
                icon = { Icon(Icons.Default.Brush, contentDescription = "Draw & Text") }
            )
        }

        // Tool Control Panel
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(175.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                when (selectedToolTab) {
                    0 -> {
                        // Presets & Color Grading
                        Text(
                            text = "Filter Presets",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(ColorPreset.values()) { preset ->
                                FilterChip(
                                    selected = selectedPreset == preset,
                                    onClick = { selectedPreset = preset },
                                    label = { Text("${preset.icon} ${preset.label}") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    ),
                                    modifier = Modifier.testTag("filter_preset_${preset.name}")
                                )
                            }
                        }
                    }

                    1 -> {
                        // Sliders: Brightness, Contrast, Saturation
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Brightness: ${brightness.toInt()}",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.width(100.dp)
                            )
                            Slider(
                                value = brightness,
                                onValueChange = { brightness = it },
                                valueRange = -100f..100f,
                                modifier = Modifier.weight(1f).testTag("slider_brightness")
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Contrast: ${"%.1f".format(contrast)}x",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.width(100.dp)
                            )
                            Slider(
                                value = contrast,
                                onValueChange = { contrast = it },
                                valueRange = 0.5f..2.0f,
                                modifier = Modifier.weight(1f).testTag("slider_contrast")
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Saturation: ${"%.1f".format(saturation)}x",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.width(100.dp)
                            )
                            Slider(
                                value = saturation,
                                onValueChange = { saturation = it },
                                valueRange = 0.0f..2.0f,
                                modifier = Modifier.weight(1f).testTag("slider_saturation")
                            )
                        }
                    }

                    2 -> {
                        // Transform: Rotate & Flip
                        Text(
                            text = "Rotation & Orientation",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedButton(
                                onClick = { rotationAngle = (rotationAngle + 90f) % 360f },
                                modifier = Modifier.weight(1f).testTag("rotate_90_btn")
                            ) {
                                Icon(Icons.Default.Rotate90DegreesCw, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Rotate 90°")
                            }

                            OutlinedButton(
                                onClick = { isFlippedHorizontal = !isFlippedHorizontal },
                                modifier = Modifier.weight(1f).testTag("flip_h_btn")
                            ) {
                                Icon(Icons.Default.Flip, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isFlippedHorizontal) "Unflip" else "Flip H")
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Current angle: ${rotationAngle.toInt()}° • Horizontal mirror: $isFlippedHorizontal",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    3 -> {
                        // Drawing and Text overlay
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Pen Palette",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            if (drawPaths.isNotEmpty()) {
                                TextButton(onClick = { drawPaths.removeLastOrNull() }) {
                                    Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Undo Stroke")
                                }
                            }
                        }

                        // Color picker chips
                        val colors = listOf(Color.Red, Color.Blue, Color.Green, Color.Yellow, Color.White, Color.Black, Color(0xFFE040FB), Color(0xFFFF9800))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            items(colors) { col ->
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(col)
                                        .border(
                                            width = if (currentDrawColor == col) 3.dp else 1.dp,
                                            color = if (currentDrawColor == col) MaterialTheme.colorScheme.primary else Color.Gray,
                                            shape = CircleShape
                                        )
                                        .clickable { currentDrawColor = col }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        // Text watermark input
                        OutlinedTextField(
                            value = textWatermark,
                            onValueChange = { textWatermark = it },
                            placeholder = { Text("Add caption / watermark text overlay...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("input_watermark_text")
                        )
                    }
                }
            }
        }
    }
}

private fun resetAdjustments(onReset: () -> Unit) {
    onReset()
}

private suspend fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
    try {
        var input: InputStream? = context.contentResolver.openInputStream(uri)
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeStream(input, null, options)
        input?.close()

        val maxDim = 1600
        var sampleSize = 1
        while (options.outWidth / sampleSize > maxDim || options.outHeight / sampleSize > maxDim) {
            sampleSize *= 2
        }

        val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        input = context.contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(input, null, decodeOptions)
        input?.close()
        bitmap
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private fun createSampleArtworkBitmap(): Bitmap {
    val width = 800
    val height = 600
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    // Draw modern background gradient
    paint.shader = android.graphics.LinearGradient(
        0f, 0f, width.toFloat(), height.toFloat(),
        android.graphics.Color.rgb(30, 41, 59),
        android.graphics.Color.rgb(88, 28, 135),
        android.graphics.Shader.TileMode.CLAMP
    )
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

    // Draw modern geometric circles
    paint.shader = null
    paint.color = android.graphics.Color.argb(80, 236, 72, 153)
    canvas.drawCircle(width * 0.3f, height * 0.4f, 180f, paint)

    paint.color = android.graphics.Color.argb(90, 59, 130, 246)
    canvas.drawCircle(width * 0.7f, height * 0.6f, 160f, paint)

    // Center text
    paint.color = android.graphics.Color.WHITE
    paint.textSize = 42f
    paint.textAlign = Paint.Align.CENTER
    paint.isFakeBoldText = true
    canvas.drawText("OmniChat Creative Canvas", width / 2f, height / 2f - 20f, paint)

    paint.textSize = 24f
    paint.color = android.graphics.Color.rgb(203, 213, 225)
    canvas.drawText("Pick your photo or apply filters & drawings", width / 2f, height / 2f + 30f, paint)

    return bitmap
}

private fun applyImageEdits(
    source: Bitmap,
    preset: ColorPreset,
    brightness: Float,
    contrast: Float,
    saturation: Float,
    rotationAngle: Float,
    flippedHorizontal: Boolean,
    watermark: String,
    drawings: List<DrawPath>
): Bitmap {
    // 1. Matrix for rotation and flip
    val matrix = Matrix()
    if (rotationAngle != 0f) {
        matrix.postRotate(rotationAngle)
    }
    if (flippedHorizontal) {
        matrix.postScale(-1f, 1f)
    }

    val rotated = if (!matrix.isIdentity) {
        Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    } else {
        source
    }

    val output = Bitmap.createBitmap(rotated.width, rotated.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)

    // 2. ColorMatrix combining preset, saturation, contrast, and brightness
    val cm = ColorMatrix()

    // Apply preset first
    when (preset) {
        ColorPreset.ORIGINAL -> {}
        ColorPreset.BW -> cm.setSaturation(0f)
        ColorPreset.SEPIA -> {
            val sepia = ColorMatrix(
                floatArrayOf(
                    0.393f, 0.769f, 0.189f, 0f, 0f,
                    0.349f, 0.686f, 0.168f, 0f, 0f,
                    0.272f, 0.534f, 0.131f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            cm.postConcat(sepia)
        }
        ColorPreset.CYBERPUNK -> {
            val cyber = ColorMatrix(
                floatArrayOf(
                    0.5f, 0f, 1.2f, 0f, 10f,
                    0f, 1.1f, 0.3f, 0f, 0f,
                    0.8f, 0.2f, 1.4f, 0f, 20f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            cm.postConcat(cyber)
        }
        ColorPreset.VIBRANT -> {
            val vib = ColorMatrix()
            vib.setSaturation(1.6f)
            cm.postConcat(vib)
        }
        ColorPreset.WARM -> {
            val warm = ColorMatrix(
                floatArrayOf(
                    1.2f, 0f, 0f, 0f, 20f,
                    0f, 1.05f, 0f, 0f, 10f,
                    0f, 0f, 0.85f, 0f, -10f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            cm.postConcat(warm)
        }
        ColorPreset.INVERT -> {
            val invert = ColorMatrix(
                floatArrayOf(
                    -1f, 0f, 0f, 0f, 255f,
                    0f, -1f, 0f, 0f, 255f,
                    0f, 0f, -1f, 0f, 255f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            cm.postConcat(invert)
        }
    }

    // Apply saturation slider
    if (saturation != 1f && preset != ColorPreset.BW) {
        val satCm = ColorMatrix()
        satCm.setSaturation(saturation)
        cm.postConcat(satCm)
    }

    // Apply brightness and contrast
    val scale = contrast
    val translate = (brightness) + (1f - contrast) * 128f
    val bcCm = ColorMatrix(
        floatArrayOf(
            scale, 0f, 0f, 0f, translate,
            0f, scale, 0f, 0f, translate,
            0f, 0f, scale, 0f, translate,
            0f, 0f, 0f, 1f, 0f
        )
    )
    cm.postConcat(bcCm)

    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.colorFilter = ColorMatrixColorFilter(cm)
    canvas.drawBitmap(rotated, 0f, 0f, paint)

    // 3. Draw annotations if any
    if (drawings.isNotEmpty()) {
        val drawPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        for (dp in drawings) {
            drawPaint.color = dp.color.toArgb()
            drawPaint.strokeWidth = dp.strokeWidth * (rotated.width.toFloat() / 800f).coerceAtLeast(1f)
            if (dp.points.size > 1) {
                for (i in 0 until dp.points.size - 1) {
                    val p1 = dp.points[i]
                    val p2 = dp.points[i + 1]
                    // Scale offset relative to actual bitmap dimensions
                    canvas.drawLine(
                        p1.x * (rotated.width.toFloat() / 800f).coerceAtLeast(1f),
                        p1.y * (rotated.height.toFloat() / 600f).coerceAtLeast(1f),
                        p2.x * (rotated.width.toFloat() / 800f).coerceAtLeast(1f),
                        p2.y * (rotated.height.toFloat() / 600f).coerceAtLeast(1f),
                        drawPaint
                    )
                }
            }
        }
    }

    // 4. Draw watermark text if present
    if (watermark.isNotBlank()) {
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textSize = (rotated.height * 0.05f).coerceAtLeast(28f)
            isFakeBoldText = true
            setShadowLayer(8f, 2f, 2f, android.graphics.Color.BLACK)
        }
        canvas.drawText(
            watermark,
            rotated.width * 0.05f,
            rotated.height * 0.93f,
            textPaint
        )
    }

    return output
}
