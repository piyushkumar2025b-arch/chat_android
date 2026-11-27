package com.example.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.PreferencesManager
import com.example.data.remote.AiService
import com.example.data.remote.MediaSaver
import com.example.util.showMainToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.roundToInt

data class GeneratedImage(
    val id: String,
    val prompt: String,
    val bitmap: Bitmap,
    val style: String,
    val aspectRatio: String,
    val wasEditedFromPhoto: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

sealed interface ImageStudioUiState {
    data object Idle : ImageStudioUiState
    data class Loading(val statusText: String) : ImageStudioUiState
    data class Success(val item: GeneratedImage) : ImageStudioUiState
    data class Error(val message: String) : ImageStudioUiState
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ImageStudioScreen(
    preferencesManager: PreferencesManager? = null,
    onSendPromptToChat: (String) -> Unit = {},
    onSendImageToChat: (Bitmap) -> Unit = { _ -> onSendPromptToChat("[Image Created in Studio]") },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val resolvedPrefs = remember(preferencesManager) { preferencesManager ?: PreferencesManager(context) }
    val coroutineScope = rememberCoroutineScope()

    var promptText by remember { mutableStateOf("") }
    var selectedStyle by remember { mutableStateOf("Natural") }
    var selectedRatio by remember { mutableStateOf("1:1") }
    var autoSaveToGallery by remember { mutableStateOf(true) }
    var isSavingCurrent by remember { mutableStateOf(false) }
    var isImprovingPrompt by remember { mutableStateOf(false) }
    var referenceBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var uiState by remember { mutableStateOf<ImageStudioUiState>(ImageStudioUiState.Idle) }
    var previewFullscreenItem by remember { mutableStateOf<GeneratedImage?>(null) }

    val styles = listOf(
        "Natural",
        "Photorealistic",
        "Anime & Manga",
        "Digital Art",
        "3D Render",
        "Watercolor",
        "Minimalist"
    )
    val aspectRatios = listOf("1:1", "16:9", "9:16", "4:3")
    val imageHistory = remember { mutableStateListOf<GeneratedImage>() }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                val decoded = decodeScaledGalleryBitmap(context, uri, maxDimension = 1280)
                if (decoded != null) {
                    referenceBitmap = decoded
                    Toast.makeText(context, "Photo loaded for AI editing", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Could not open that photo", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun generateOrEdit() {
        val rawPrompt = promptText.trim()
        if (rawPrompt.isEmpty()) {
            Toast.makeText(context, "Please describe the image you want", Toast.LENGTH_SHORT).show()
            return
        }
        val sourceRef = referenceBitmap
        val styleChoice = selectedStyle
        val ratioChoice = selectedRatio

        coroutineScope.launch {
            uiState = ImageStudioUiState.Loading(
                if (sourceRef != null) "Editing photo with AI (may take 20–60s)…"
                else "Creating image with AI (may take 20–60s)…"
            )
            val result = AiService.generateOrEditImage(
                prompt = rawPrompt,
                inputBitmap = sourceRef,
                styleHint = styleChoice,
                aspectRatio = ratioChoice
            )
            result.onSuccess { bmp ->
                val created = GeneratedImage(
                    id = System.currentTimeMillis().toString(),
                    prompt = rawPrompt,
                    bitmap = bmp,
                    style = styleChoice,
                    aspectRatio = ratioChoice,
                    wasEditedFromPhoto = sourceRef != null
                )
                imageHistory.add(0, created)
                uiState = ImageStudioUiState.Success(created)
                if (autoSaveToGallery) {
                    MediaSaver.saveBitmapToGallery(context, bmp, rawPrompt)
                }
            }.onFailure { err ->
                val msg = err.localizedMessage?.takeIf { it.isNotBlank() }
                    ?: "Image generation failed. Please try again."
                uiState = ImageStudioUiState.Error(msg)
            }
        }
    }

    fun improvePromptWithAi() {
        val current = promptText.trim()
        val task = if (current.isEmpty()) {
            "Write one vivid, clear, 2-sentence image prompt for an imaginative scene. Return only the prompt text."
        } else {
            val modeNote = if (referenceBitmap != null) {
                "The user is editing an uploaded photo."
            } else {
                "The user is generating a brand-new image."
            }
            "Rewrite and enrich this image prompt so it produces clear, cohesive artwork ($modeNote Style: $selectedStyle). Keep the user's subject and return only the improved prompt text:\n\n$current"
        }

        coroutineScope.launch {
            isImprovingPrompt = true
            val res = AiService.quickAssist(
                taskPrompt = task,
                systemInstruction = "You write concise, clear image prompts. Return only the prompt with no quotes or explanation.",
                temperature = 0.6f,
                customGeminiApiKey = resolvedPrefs.geminiApiKey
            )
            isImprovingPrompt = false
            res.onSuccess { improved ->
                promptText = improved.trim().removeSurrounding("\"")
            }.onFailure { err ->
                showMainToast(
                    context,
                    err.localizedMessage ?: "Could not improve prompt right now"
                )
            }
        }
    }

    fun shareBitmapItem(item: GeneratedImage) {
        coroutineScope.launch {
            val saveRes = MediaSaver.saveBitmapToGallery(context, item.bitmap, item.prompt)
            saveRes.onSuccess { uri ->
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_TEXT, "Created in OmniChat: ${item.prompt}")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                runCatching {
                    context.startActivity(Intent.createChooser(shareIntent, "Share Image"))
                }.onFailure {
                    Toast.makeText(context, "No app found to share image", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val isGenerating = uiState is ImageStudioUiState.Loading

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
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Image,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        "AI Image Studio",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Create pictures from text or edit your own photos",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Prompt + Optional Photo Edit Input Card
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Optional reference photo bar
                    val refBmp = referenceBitmap
                    if (refBmp != null) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(
                                    bitmap = refBmp.asImageBitmap(),
                                    contentDescription = "Photo to edit",
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Photo Attached for Editing",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        "Describe what to change, remove, or restyle",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(
                                    onClick = { referenceBitmap = null },
                                    modifier = Modifier.testTag("clear_reference_photo_button")
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove photo")
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = promptText,
                        onValueChange = { promptText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("image_prompt_input"),
                        placeholder = {
                            Text(
                                if (referenceBitmap != null) "Describe what to change in this photo..."
                                else "Describe what you want to see..."
                            )
                        },
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
                            Text("Auto-Save to Gallery", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            Text("Saves to /Pictures/OmniChat", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = autoSaveToGallery,
                            onCheckedChange = { autoSaveToGallery = it },
                            modifier = Modifier.testTag("auto_save_gallery_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            enabled = !isGenerating,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("pick_photo_to_edit_button")
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (referenceBitmap != null) "Change Photo" else "Edit a Photo")
                        }

                        FilledTonalButton(
                            onClick = { improvePromptWithAi() },
                            enabled = !isImprovingPrompt && !isGenerating,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("improve_image_prompt_button")
                        ) {
                            if (isImprovingPrompt) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Improve Prompt")
                        }

                        Button(
                            onClick = { generateOrEdit() },
                            enabled = !isGenerating,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("generate_image_button")
                        ) {
                            if (isGenerating) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Creating…")
                            } else {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (referenceBitmap != null) "Apply Edit" else "Create Image")
                            }
                        }
                    }
                }
            }
        }

        // Art Style Selector
        item {
            Text("Style", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(styles) { style ->
                    FilterChip(
                        selected = selectedStyle == style,
                        onClick = { selectedStyle = style },
                        label = { Text(style) },
                        leadingIcon = if (selectedStyle == style) {
                            { Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        } else null
                    )
                }
            }
        }

        // Aspect Ratio Selector
        item {
            Text("Shape", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                aspectRatios.forEach { ratio ->
                    FilterChip(
                        selected = selectedRatio == ratio,
                        onClick = { selectedRatio = ratio },
                        label = { Text(ratio) }
                    )
                }
            }
        }

        // State-driven result / progress / error area
        when (val state = uiState) {
            is ImageStudioUiState.Loading -> {
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
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator()
                            Text(
                                state.statusText,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                "Powered by built-in Google Gemini Image AI",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            is ImageStudioUiState.Error -> {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                "Could not create image",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                state.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Button(
                                onClick = { generateOrEdit() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError
                                )
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Try Again")
                            }
                        }
                    }
                }
            }

            is ImageStudioUiState.Success -> {
                val item = state.item
                item {
                    Text("Latest Creation", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            val ratioFloat = when (item.aspectRatio) {
                                "16:9" -> 16f / 9f
                                "9:16" -> 9f / 16f
                                "4:3" -> 4f / 3f
                                else -> 1f
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(ratioFloat.coerceIn(0.6f, 1.8f))
                                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                                    .clickable { previewFullscreenItem = item },
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    bitmap = item.bitmap.asImageBitmap(),
                                    contentDescription = item.prompt,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }

                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            item.prompt,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            "${if (item.wasEditedFromPhoto) "Edited Photo" else item.style} • ${item.aspectRatio}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        IconButton(
                                            onClick = {
                                                coroutineScope.launch {
                                                    isSavingCurrent = true
                                                    MediaSaver.saveBitmapToGallery(context, item.bitmap, item.prompt)
                                                    isSavingCurrent = false
                                                }
                                            },
                                            enabled = !isSavingCurrent,
                                            modifier = Modifier.testTag("save_image_to_phone_button")
                                        ) {
                                            if (isSavingCurrent) {
                                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                            } else {
                                                Icon(Icons.Default.Download, contentDescription = "Save to Phone Gallery", tint = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                        IconButton(onClick = { generateOrEdit() }) {
                                            Icon(Icons.Default.Refresh, contentDescription = "Create another")
                                        }
                                        IconButton(onClick = { shareBitmapItem(item) }) {
                                            Icon(Icons.Default.Share, contentDescription = "Share image")
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            referenceBitmap = item.bitmap
                                            promptText = ""
                                            Toast.makeText(context, "Loaded image for editing", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Edit This", maxLines = 1)
                                    }

                                    FilledTonalButton(
                                        onClick = { onSendImageToChat(item.bitmap) },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Ask Chat", maxLines = 1)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            ImageStudioUiState.Idle -> Unit
        }

        // History Gallery
        val historyItems = if (uiState is ImageStudioUiState.Success) imageHistory.drop(1) else imageHistory.toList()
        if (historyItems.isNotEmpty()) {
            item {
                Text("Previous Creations", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            items(historyItems, key = { it.id }) { item ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { previewFullscreenItem = item }
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            bitmap = item.bitmap.asImageBitmap(),
                            contentDescription = item.prompt,
                            modifier = Modifier
                                .size(70.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                item.prompt,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "${item.style} • ${item.aspectRatio}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    MediaSaver.saveBitmapToGallery(context, item.bitmap, item.prompt)
                                }
                            }
                        ) {
                            Icon(Icons.Default.Download, contentDescription = "Save to Phone", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }

    // Fullscreen Dialog
    val fullItem = previewFullscreenItem
    if (fullItem != null) {
        Dialog(
            onDismissRequest = { previewFullscreenItem = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.92f)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = fullItem.bitmap.asImageBitmap(),
                    contentDescription = "Fullscreen Preview",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentScale = ContentScale.Fit
                )

                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                MediaSaver.saveBitmapToGallery(context, fullItem.bitmap, fullItem.prompt)
                            }
                        },
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            .testTag("fullscreen_download_image_button")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = "Save to Phone", tint = Color.White)
                    }

                    IconButton(
                        onClick = { previewFullscreenItem = null },
                        modifier = Modifier.background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }
            }
        }
    }
}

private suspend fun decodeScaledGalleryBitmap(
    context: Context,
    uri: Uri,
    maxDimension: Int
): Bitmap? = withContext(Dispatchers.IO) {
    runCatching {
        val raw = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.isMutableRequired = false
            }
        } else {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        } ?: return@withContext null

        val largest = max(raw.width, raw.height)
        if (largest <= maxDimension) {
            raw
        } else {
            val scale = maxDimension.toFloat() / largest.toFloat()
            val w = (raw.width * scale).roundToInt().coerceAtLeast(1)
            val h = (raw.height * scale).roundToInt().coerceAtLeast(1)
            Bitmap.createScaledBitmap(raw, w, h, true)
        }
    }.getOrNull()
}
