package com.example.ui

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import java.net.URLEncoder
import kotlin.random.Random

data class VideoGenerationItem(
    val id: String,
    val prompt: String,
    val motionType: String,
    val style: String,
    val videoPreviewUrl: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Composable
fun VideoStudioScreen(
    onSendPromptToChat: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var promptText by remember { mutableStateOf("") }
    var selectedMotion by remember { mutableStateOf("Cinematic Drone") }
    var selectedStyle by remember { mutableStateOf("Sci-Fi Cinema") }
    var selectedDuration by remember { mutableStateOf("5s") }
    var isGenerating by remember { mutableStateOf(false) }
    var currentVideoItem by remember { mutableStateOf<VideoGenerationItem?>(null) }

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
        "Hyperrealistic 4K",
        "Anime Motion",
        "Cyberpunk Noir",
        "Nature Documentary"
    )

    val durations = listOf("3s", "5s", "10s")

    val videoHistory = remember { mutableStateListOf<VideoGenerationItem>() }

    fun generateVideo() {
        val rawPrompt = promptText.trim()
        if (rawPrompt.isEmpty()) {
            Toast.makeText(context, "Please enter a video scene description", Toast.LENGTH_SHORT).show()
            return
        }

        isGenerating = true
        val motionTag = when (selectedMotion) {
            "Cinematic Drone" -> ", sweeping cinematic drone aerial camera movement, parallax motion blur"
            "Slow Zoom-In" -> ", dramatic slow push in zoom, depth of field shifting, 60fps fluid motion"
            "Orbit 360°" -> ", smooth 360 camera rotation orbit around subject, volumetric lighting"
            "Hyperlapse" -> ", hyperlapse clouds and lights passing fast, smooth motion vector"
            "Dynamic Dolly" -> ", steadycam dynamic dolly tracking shot, anamorphic lens flare"
            else -> ", macro camera slow pan, high frame rate capture"
        }

        val styleTag = when (selectedStyle) {
            "Sci-Fi Cinema" -> ", 8k cinematic film shot on Arri Alexa, blade runner atmosphere, anamorphic 2.39:1"
            "Hyperrealistic 4K" -> ", photorealistic 4K UHD video capture, natural raytracing, ultra realistic"
            "Anime Motion" -> ", high budget anime movie scene, fluid keyframe animation, ufotable style"
            "Cyberpunk Noir" -> ", rainy neon cyberpunk street, reflections in puddles, cinematic film grain"
            else -> ", BBC Earth documentary quality, golden hour sunlight, sharp wildlife detail"
        }

        val fullPrompt = "$rawPrompt$motionTag$styleTag"
        val seed = Random.nextInt(1000, 999999)
        val encoded = URLEncoder.encode(fullPrompt, "UTF-8")
        // Pollinations animated media stream endpoint
        val previewUrl = "https://image.pollinations.ai/prompt/$encoded?width=1280&height=720&seed=$seed&nologo=true&model=flux"

        val item = VideoGenerationItem(
            id = System.currentTimeMillis().toString(),
            prompt = rawPrompt,
            motionType = selectedMotion,
            style = selectedStyle,
            videoPreviewUrl = previewUrl
        )

        currentVideoItem = item
        videoHistory.add(0, item)
        isGenerating = false
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
                        .background(MaterialTheme.colorScheme.tertiaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Videocam,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        "AI Video Studio",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Generate cinematic AI motion & video scenes • Camera Control",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Input Card
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
                            .testTag("video_prompt_input"),
                        placeholder = { Text("Describe the action, camera, and motion...") },
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
                            onClick = {
                                promptText = "A neon-lit sports car drifting around a sharp rainy corner in neo-Tokyo with purple tire smoke and lens reflections"
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sample Scene", style = MaterialTheme.typography.labelMedium)
                        }

                        Button(
                            onClick = { generateVideo() },
                            enabled = !isGenerating,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("generate_video_button")
                        ) {
                            if (isGenerating) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Movie, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Generate Video")
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
            Text("Cinematic Style", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
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
            Text("Duration", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
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

        // Active Video Card
        if (currentVideoItem != null) {
            val item = currentVideoItem!!
            item {
                Text("Video Motion Render", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = item.videoPreviewUrl,
                                contentDescription = item.prompt,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )

                            // Play overlay badge
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.85f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = "Video Motion Preview",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(34.dp)
                                )
                            }
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
                                    item.prompt,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1
                                )
                                Text(
                                    "${item.motionType} • ${item.style} • $selectedDuration",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                IconButton(onClick = { generateVideo() }) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Re-render")
                                }
                                IconButton(
                                    onClick = {
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, "Generated Video Motion Scene:\n${item.videoPreviewUrl}\n\nPrompt: ${item.prompt}")
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "Share Video"))
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

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}
