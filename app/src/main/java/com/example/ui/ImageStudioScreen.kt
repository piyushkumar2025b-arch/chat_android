package com.example.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URLEncoder
import kotlin.random.Random

data class GeneratedImage(
    val id: String,
    val prompt: String,
    val imageUrl: String,
    val style: String,
    val aspectRatio: String,
    val timestamp: Long = System.currentTimeMillis()
)

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ImageStudioScreen(
    onSendPromptToChat: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var promptText by remember { mutableStateOf("") }
    var selectedStyle by remember { mutableStateOf("Photorealistic") }
    var selectedRatio by remember { mutableStateOf("1:1") }
    var currentSeed by remember { mutableStateOf(Random.nextInt(1000, 999999)) }
    var isGenerating by remember { mutableStateOf(false) }
    var currentImageUrl by remember { mutableStateOf<String?>(null) }
    var previewFullscreenUrl by remember { mutableStateOf<String?>(null) }

    val styles = listOf("Photorealistic", "Anime & Manga", "Cyberpunk", "Cinematic 3D", "Digital Art", "Fantasy Art", "Minimalist")
    val aspectRatios = listOf(
        Pair("1:1", Pair(1024, 1024)),
        Pair("16:9", Pair(1280, 720)),
        Pair("9:16", Pair(720, 1280)),
        Pair("4:3", Pair(1024, 768))
    )

    val imageHistory = remember { mutableStateListOf<GeneratedImage>() }

    fun generateImage() {
        val rawPrompt = promptText.trim()
        if (rawPrompt.isEmpty()) {
            Toast.makeText(context, "Please enter an image prompt", Toast.LENGTH_SHORT).show()
            return
        }

        isGenerating = true
        val styleTag = when (selectedStyle) {
            "Photorealistic" -> ", 8k resolution, cinematic lighting, photorealistic, ultra-detailed, 35mm photograph"
            "Anime & Manga" -> ", anime aesthetic, vibrant colors, makoto shinkai style, studio ghibli lighting, sharp outline"
            "Cyberpunk" -> ", cyberpunk city, neon glowing lights, futuristic octane render, dark cinematic atmosphere"
            "Cinematic 3D" -> ", unreal engine 5 render, cinematic volumetric lighting, 3D digital art, hyper-detailed"
            "Digital Art" -> ", modern digital illustration, artstation trending, vibrant palette, concept artwork"
            "Fantasy Art" -> ", mythical fantasy environment, magical glowing particles, epic majestic atmosphere"
            "Minimalist" -> ", minimalist graphic design, clean vectors, flat colors, modern aesthetic"
            else -> ""
        }

        val fullPrompt = "$rawPrompt$styleTag"
        val dims = aspectRatios.firstOrNull { it.first == selectedRatio }?.second ?: Pair(1024, 1024)
        val encoded = URLEncoder.encode(fullPrompt, "UTF-8")
        val seed = currentSeed
        val url = "https://image.pollinations.ai/prompt/$encoded?width=${dims.first}&height=${dims.second}&seed=$seed&nologo=true&model=flux"

        currentImageUrl = url
        imageHistory.add(0, GeneratedImage(
            id = System.currentTimeMillis().toString(),
            prompt = rawPrompt,
            imageUrl = url,
            style = selectedStyle,
            aspectRatio = selectedRatio
        ))
        isGenerating = false
    }

    fun enhancePrompt() {
        val p = promptText.trim()
        if (p.isEmpty()) {
            promptText = "A futuristic glowing cybernetic city floating in the sky with waterfalls and neon airships"
        } else {
            promptText = "$p, masterpiece, intricate dramatic lighting, volumetric shadows, award winning composition"
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
                        "Generate high-res images powered by Flux AI • 100% Free",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Prompt Input Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    OutlinedTextField(
                        value = promptText,
                        onValueChange = { promptText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("image_prompt_input"),
                        placeholder = { Text("Describe what you want to imagine...") },
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
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { enhancePrompt() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Enhance Prompt", style = MaterialTheme.typography.labelMedium)
                        }

                        Button(
                            onClick = {
                                currentSeed = Random.nextInt(1000, 999999)
                                generateImage()
                            },
                            enabled = !isGenerating,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("generate_image_button")
                        ) {
                            if (isGenerating) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Generate")
                            }
                        }
                    }
                }
            }
        }

        // Style Selector Chips
        item {
            Text("Art Style", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
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
            Text("Aspect Ratio", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                aspectRatios.forEach { (ratio, _) ->
                    FilterChip(
                        selected = selectedRatio == ratio,
                        onClick = { selectedRatio = ratio },
                        label = { Text(ratio) }
                    )
                }
            }
        }

        // Current Generated Preview
        if (currentImageUrl != null) {
            item {
                Text("Latest Creation", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        val ratioFloat = when (selectedRatio) {
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
                                .clickable { previewFullscreenUrl = currentImageUrl },
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = currentImageUrl,
                                contentDescription = promptText,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    promptText.ifEmpty { "Generated Art" },
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1
                                )
                                Text(
                                    "$selectedStyle • $selectedRatio",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                IconButton(
                                    onClick = {
                                        currentSeed = Random.nextInt(1000, 999999)
                                        generateImage()
                                    }
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Regenerate")
                                }
                                IconButton(
                                    onClick = {
                                        val url = currentImageUrl ?: return@IconButton
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, "Generated with OmniChat AI:\n$url\n\nPrompt: $promptText")
                                        }
                                        try {
                                            context.startActivity(Intent.createChooser(shareIntent, "Share Image"))
                                        } catch (_: Exception) {}
                                    }
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = "Share")
                                }
                            }
                        }
                    }
                }
            }
        }

        // History Gallery
        if (imageHistory.size > 1) {
            item {
                Text("Creation History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            items(imageHistory.drop(1)) { item ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { previewFullscreenUrl = item.imageUrl }
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = item.imageUrl,
                            contentDescription = item.prompt,
                            modifier = Modifier
                                .size(70.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.prompt, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 2)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("${item.style} • ${item.aspectRatio}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }

    // Fullscreen Dialog
    if (previewFullscreenUrl != null) {
        Dialog(
            onDismissRequest = { previewFullscreenUrl = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.92f)),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = previewFullscreenUrl,
                    contentDescription = "Fullscreen Preview",
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    contentScale = ContentScale.Fit
                )

                IconButton(
                    onClick = { previewFullscreenUrl = null },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(24.dp)
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }
        }
    }
}
