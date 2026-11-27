package com.example.ui

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.remote.AiService
import com.example.data.remote.MediaSaver
import kotlinx.coroutines.launch

data class VideoStoryboardItem(
    val id: String,
    val prompt: String,
    val motionType: String,
    val style: String,
    val duration: String,
    val scriptText: String = "",
    val keyframeBitmap: Bitmap? = null,
    val keyframeError: String? = null,
    val localVideoUri: Uri? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VideoStudioScreen(
    onSendPromptToChat: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var promptText by remember { mutableStateOf("") }
    var selectedMotion by remember { mutableStateOf("Cinematic Drone") }
    var selectedStyle by remember { mutableStateOf("Sci-Fi Cinema") }
    var selectedDuration by remember { mutableStateOf("5s") }
    var isGenerating by remember { mutableStateOf(false) }
    var generationError by remember { mutableStateOf<String?>(null) }
    var currentItem by remember { mutableStateOf<VideoStoryboardItem?>(null) }
    var autoSaveKeyframe by remember { mutableStateOf(true) }
    var isSavingMedia by remember { mutableStateOf(false) }
    var showPreviewDialog by remember { mutableStateOf(false) }

    val motionTypes = listOf(
        "Cinematic Drone",
        "Slow Zoom-In",
        "Orbit 360°",
        "Hyperlapse",
        "Dynamic Dolly",
        "Static Macro"
    )

    val styles = listOf(
        "Sci-Fi Cinema",
        "Photorealistic 4K",
        "Anime Motion",
        "Cyberpunk Noir",
        "Nature Film"
    )

    val durations = listOf("3s", "5s", "10s")
    val storyboardHistory = remember { mutableStateListOf<VideoStoryboardItem>() }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val name = uri.lastPathSegment?.substringAfterLast('/') ?: "Phone Video"
            val item = VideoStoryboardItem(
                id = System.currentTimeMillis().toString(),
                prompt = "Imported Video: $name",
                motionType = selectedMotion,
                style = "Local Video",
                duration = selectedDuration,
                scriptText = "Local video clip loaded from your device. Tap Play to open it in your video player, or describe a scene above to plan a new storyboard.",
                localVideoUri = uri
            )
            currentItem = item
            storyboardHistory.add(0, item)
            promptText = "Video Clip: $name"
            generationError = null
            Toast.makeText(context, "Loaded video: $name", Toast.LENGTH_SHORT).show()
        }
    }

    fun generateSceneAndStoryboard() {
        val rawPrompt = promptText.trim()
        if (rawPrompt.isEmpty()) {
            Toast.makeText(context, "Please describe the video scene first", Toast.LENGTH_SHORT).show()
            return
        }

        val motionChoice = selectedMotion
        val styleChoice = selectedStyle
        val durationChoice = selectedDuration

        coroutineScope.launch {
            isGenerating = true
            generationError = null

            val scriptPrompt = """
                Create a short, easy-to-follow $durationChoice video scene plan for:
                "$rawPrompt"
                Camera motion: $motionChoice
                Visual style: $styleChoice

                Include:
                1. Shot list with seconds (e.g., 0–2s, 2–5s)
                2. Camera movement & lighting notes
                3. One short voiceover or sound cue line
            """.trimIndent()

            val scriptResult = AiService.quickAssist(
                taskPrompt = scriptPrompt,
                systemInstruction = "You are a helpful video director writing clear, simple shot plans.",
                temperature = 0.6f
            )

            val imagePrompt = "$rawPrompt. Cinematic film keyframe still, camera angle: $motionChoice"
            val keyframeResult = AiService.generateOrEditImage(
                prompt = imagePrompt,
                styleHint = styleChoice,
                aspectRatio = "16:9"
            )

            val scriptText = scriptResult.getOrNull()
            val keyframeBmp = keyframeResult.getOrNull()

            if (scriptText == null && keyframeBmp == null) {
                generationError = keyframeResult.exceptionOrNull()?.localizedMessage
                    ?: scriptResult.exceptionOrNull()?.localizedMessage
                    ?: "Could not build the scene plan right now. Please try again."
                isGenerating = false
                return@launch
            }

            val created = VideoStoryboardItem(
                id = System.currentTimeMillis().toString(),
                prompt = rawPrompt,
                motionType = motionChoice,
                style = styleChoice,
                duration = durationChoice,
                scriptText = scriptText ?: "Scene script unavailable (${scriptResult.exceptionOrNull()?.localizedMessage ?: "error"}).",
                keyframeBitmap = keyframeBmp,
                keyframeError = keyframeResult.exceptionOrNull()?.localizedMessage
            )

            currentItem = created
            storyboardHistory.add(0, created)
            isGenerating = false

            if (autoSaveKeyframe && keyframeBmp != null) {
                MediaSaver.saveBitmapToGallery(context, keyframeBmp, "Scene_$rawPrompt")
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Videocam,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        "Video Storyboard Maker",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Write scene scripts, create keyframes, or open device clips",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Input Card
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    OutlinedTextField(
                        value = promptText,
                        onValueChange = { promptText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("video_prompt_input"),
                        placeholder = { Text("Describe the scene, action, and mood...") },
                        maxLines = 4,
                        trailingIcon = {
                            if (promptText.isNotEmpty()) {
                                IconButton(onClick = { promptText = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Auto-Save Keyframe Image", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            Text("Saves preview frame to /Pictures/OmniChat", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = autoSaveKeyframe,
                            onCheckedChange = { autoSaveKeyframe = it },
                            modifier = Modifier.testTag("auto_save_video_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = {
                                videoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                )
                            },
                            enabled = !isGenerating,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("import_video_button")
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Open Clip")
                        }

                        OutlinedButton(
                            onClick = {
                                promptText = "A silver sports car drifting around a rainy neon corner at night with glowing reflections"
                            },
                            enabled = !isGenerating,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sample Idea")
                        }

                        Button(
                            onClick = { generateSceneAndStoryboard() },
                            enabled = !isGenerating,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("generate_video_button")
                        ) {
                            if (isGenerating) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Building Scene…")
                            } else {
                                Icon(Icons.Default.Movie, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Build Storyboard")
                            }
                        }
                    }
                }
            }
        }

        // Camera Motion Chips
        item {
            Text("Camera Motion", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(motionTypes) { motion ->
                    FilterChip(
                        selected = selectedMotion == motion,
                        onClick = { selectedMotion = motion },
                        label = { Text(motion) },
                        leadingIcon = if (selectedMotion == motion) {
                            { Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        } else null
                    )
                }
            }
        }

        // Style Chips
        item {
            Text("Look & Mood", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(styles) { style ->
                    FilterChip(
                        selected = selectedStyle == style,
                        onClick = { selectedStyle = style },
                        label = { Text(style) }
                    )
                }
            }
        }

        // Duration Chips
        item {
            Text("Scene Length", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                durations.forEach { d ->
                    FilterChip(
                        selected = selectedDuration == d,
                        onClick = { selectedDuration = d },
                        label = { Text(d) }
                    )
                }
            }
        }

        if (isGenerating) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator()
                        Text(
                            "Writing shot script & creating keyframe preview…",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        val errMsg = generationError
        if (errMsg != null) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Could not build storyboard",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            errMsg,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Button(
                            onClick = { generateSceneAndStoryboard() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            )
                        ) {
                            Text("Try Again")
                        }
                    }
                }
            }
        }

        // Active Storyboard / Clip Card
        val activeItem = currentItem
        if (activeItem != null) {
            item {
                Text(
                    if (activeItem.localVideoUri != null) "Imported Video Clip" else "Scene Script & Keyframe",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        if (activeItem.keyframeBitmap != null || activeItem.localVideoUri != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(16f / 9f)
                                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                                    .clickable {
                                        if (activeItem.localVideoUri != null) {
                                            runCatching {
                                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                                    setDataAndType(activeItem.localVideoUri, "video/*")
                                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                }
                                                context.startActivity(intent)
                                            }.onFailure {
                                                Toast.makeText(context, "No video player app found", Toast.LENGTH_SHORT).show()
                                            }
                                        } else {
                                            showPreviewDialog = true
                                        }
                                    }
                                    .testTag("video_preview_box"),
                                contentAlignment = Alignment.Center
                            ) {
                                val bmp = activeItem.keyframeBitmap
                                if (bmp != null) {
                                    Image(
                                        bitmap = bmp.asImageBitmap(),
                                        contentDescription = activeItem.prompt,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            Icons.Default.PlayArrow,
                                            contentDescription = "Play Video",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Text(
                                            "Tap to play video clip",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        activeItem.prompt,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        "${activeItem.motionType} • ${activeItem.style} • ${activeItem.duration}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                isSavingMedia = true
                                                val bmp = activeItem.keyframeBitmap
                                                if (bmp != null) {
                                                    MediaSaver.saveBitmapToGallery(context, bmp, "Scene_${activeItem.prompt}")
                                                }
                                                if (activeItem.scriptText.isNotBlank()) {
                                                    val safeName = "SceneScript_${System.currentTimeMillis()}.txt"
                                                    MediaSaver.saveDocumentToDownloads(
                                                        context = context,
                                                        fileName = safeName,
                                                        content = "Scene: ${activeItem.prompt}\nCamera: ${activeItem.motionType}\nStyle: ${activeItem.style}\nLength: ${activeItem.duration}\n\n${activeItem.scriptText}"
                                                    )
                                                }
                                                isSavingMedia = false
                                            }
                                        },
                                        enabled = !isSavingMedia,
                                        modifier = Modifier.testTag("save_video_to_phone_button")
                                    ) {
                                        if (isSavingMedia) {
                                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                        } else {
                                            Icon(Icons.Default.Download, contentDescription = "Save Keyframe & Script", tint = MaterialTheme.colorScheme.primary)
                                        }
                                    }

                                    if (activeItem.localVideoUri == null) {
                                        IconButton(onClick = { generateSceneAndStoryboard() }) {
                                            Icon(Icons.Default.Refresh, contentDescription = "Rebuild Storyboard")
                                        }
                                    }

                                    IconButton(
                                        onClick = {
                                            val shareText = buildString {
                                                appendLine("Scene: ${activeItem.prompt}")
                                                appendLine("Camera: ${activeItem.motionType} | Style: ${activeItem.style} | Length: ${activeItem.duration}")
                                                if (activeItem.scriptText.isNotBlank()) {
                                                    appendLine()
                                                    appendLine(activeItem.scriptText)
                                                }
                                            }
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_TEXT, shareText)
                                            }
                                            runCatching {
                                                context.startActivity(Intent.createChooser(shareIntent, "Share Scene Plan"))
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = "Share Scene Plan")
                                    }
                                }
                            }

                            if (!activeItem.keyframeError.isNullOrBlank()) {
                                Text(
                                    "Keyframe preview note: ${activeItem.keyframeError}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }

                            if (activeItem.scriptText.isNotBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Shot Plan", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                                }
                                Text(
                                    text = activeItem.scriptText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            FilledTonalButton(
                                onClick = {
                                    onSendPromptToChat(
                                        "Refine this ${activeItem.duration} video scene (${activeItem.motionType}, ${activeItem.style}):\n${activeItem.prompt}\n\nCurrent shot plan:\n${activeItem.scriptText}"
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Continue Planning in Chat")
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }

    val activePreview = activeItemOrNull(currentItem)
    if (showPreviewDialog && activePreview != null) {
        val active = activePreview
        val bmp = active.keyframeBitmap
        AlertDialog(
            onDismissRequest = { showPreviewDialog = false },
            title = { Text("Keyframe Preview (${active.motionType})") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (bmp != null) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = active.prompt,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Text(
                        text = active.prompt,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSendPromptToChat("Video Scene (${active.motionType}, ${active.style}): ${active.prompt}\n\n${active.scriptText}")
                        showPreviewDialog = false
                    }
                ) {
                    Text("Send to Chat")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPreviewDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

private fun activeItemOrNull(item: VideoStoryboardItem?): VideoStoryboardItem? =
    item?.takeIf { it.keyframeBitmap != null }
