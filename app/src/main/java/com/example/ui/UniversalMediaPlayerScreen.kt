package com.example.ui

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class MediaKind {
    VIDEO,
    AUDIO,
    IMAGE,
    PDF_DOCUMENT,
    TEXT_DOCUMENT,
    ARCHIVE,
    UNKNOWN
}

data class MediaItem(
    val uri: Uri,
    val displayName: String,
    val mimeType: String,
    val kind: MediaKind,
    val sizeBytes: Long = 0L,
    val durationMs: Long = 0L
)

fun formatMediaDuration(millis: Long): String {
    if (millis <= 0L) return "00:00"
    val totalSeconds = millis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}

fun formatFileSize(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val kb = 1024L
    val mb = kb * 1024L
    val gb = mb * 1024L
    return when {
        bytes < kb -> "$bytes B"
        bytes < mb -> "%.1f KB".format(bytes.toDouble() / kb)
        bytes < gb -> "%.1f MB".format(bytes.toDouble() / mb)
        else -> "%.1f GB".format(bytes.toDouble() / gb)
    }
}

fun resolveMediaItemFromUri(context: Context, uri: Uri): MediaItem {
    val displayName = uri.lastPathSegment?.substringAfterLast('/') ?: "unknown"
    val ext = displayName.substringAfterLast('.', "").lowercase()

    val (kind, mimeType) = when (ext) {
        "mp4", "mkv", "webm", "avi", "mov" -> {
            MediaKind.VIDEO to "video/$ext"
        }
        "m3u8" -> {
            MediaKind.VIDEO to "application/x-mpegURL"
        }
        "mp3" -> {
            MediaKind.AUDIO to "audio/mpeg"
        }
        "wav", "ogg", "flac", "m4a", "aac" -> {
            MediaKind.AUDIO to "audio/$ext"
        }
        "png", "jpg", "jpeg", "webp", "gif" -> {
            MediaKind.IMAGE to "image/$ext"
        }
        "svg" -> {
            MediaKind.IMAGE to "image/svg+xml"
        }
        "pdf" -> {
            MediaKind.PDF_DOCUMENT to "application/pdf"
        }
        "apk" -> {
            MediaKind.ARCHIVE to "application/vnd.android.package-archive"
        }
        "zip", "tar", "gz", "7z", "rar" -> {
            MediaKind.ARCHIVE to "application/zip"
        }
        "csv" -> {
            MediaKind.TEXT_DOCUMENT to "text/csv"
        }
        "md", "markdown" -> {
            MediaKind.TEXT_DOCUMENT to "text/markdown"
        }
        "txt", "log" -> {
            MediaKind.TEXT_DOCUMENT to "text/plain"
        }
        "py" -> {
            MediaKind.TEXT_DOCUMENT to "text/x-python"
        }
        "yaml", "yml" -> {
            MediaKind.TEXT_DOCUMENT to "text/yaml"
        }
        "rs" -> {
            MediaKind.TEXT_DOCUMENT to "text/x-rust"
        }
        "xml" -> {
            MediaKind.TEXT_DOCUMENT to "text/xml"
        }
        "cpp", "c", "h", "hpp" -> {
            MediaKind.TEXT_DOCUMENT to "text/x-c++src"
        }
        "css" -> {
            MediaKind.TEXT_DOCUMENT to "text/css"
        }
        "js", "ts", "kt", "java", "json", "html" -> {
            MediaKind.TEXT_DOCUMENT to "text/plain"
        }
        else -> {
            MediaKind.UNKNOWN to "application/octet-stream"
        }
    }

    return MediaItem(
        uri = uri,
        displayName = displayName,
        mimeType = mimeType,
        kind = kind
    )
}

object CuratedMediaSources {
    val sampleSources: List<MediaItem> = listOf(
        MediaItem(Uri.parse("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"), "BigBuckBunny.mp4", "video/mp4", MediaKind.VIDEO, 158008374L, 596000L),
        MediaItem(Uri.parse("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4"), "ElephantsDream.mp4", "video/mp4", MediaKind.VIDEO, 140000000L, 653000L),
        MediaItem(Uri.parse("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4"), "ForBiggerBlazes.mp4", "video/mp4", MediaKind.VIDEO, 15000000L, 15000L),
        MediaItem(Uri.parse("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4"), "TearsOfSteel.mp4", "video/mp4", MediaKind.VIDEO, 220000000L, 734000L),
        MediaItem(Uri.parse("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/WeAreGoingOnBullrun.mp4"), "Bullrun.mp4", "video/mp4", MediaKind.VIDEO, 45000000L, 120000L),
        MediaItem(Uri.parse("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"), "SoundHelix-Song-1.mp3", "audio/mpeg", MediaKind.AUDIO, 4200000L, 372000L),
        MediaItem(Uri.parse("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3"), "SoundHelix-Song-2.mp3", "audio/mpeg", MediaKind.AUDIO, 3800000L, 421000L),
        MediaItem(Uri.parse("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3"), "SoundHelix-Song-3.mp3", "audio/mpeg", MediaKind.AUDIO, 4500000L, 345000L),
        MediaItem(Uri.parse("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3"), "SoundHelix-Song-4.mp3", "audio/mpeg", MediaKind.AUDIO, 5100000L, 480000L),
        MediaItem(Uri.parse("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3"), "SoundHelix-Song-5.mp3", "audio/mpeg", MediaKind.AUDIO, 3900000L, 312000L),
        MediaItem(Uri.parse("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-6.mp3"), "SoundHelix-Song-6.mp3", "audio/mpeg", MediaKind.AUDIO, 4300000L, 395000L),
        MediaItem(Uri.parse("https://example.com/docs/bitcoin_whitepaper.pdf"), "bitcoin_whitepaper.pdf", "application/pdf", MediaKind.PDF_DOCUMENT, 184292L, 0L),
        MediaItem(Uri.parse("https://example.com/docs/quantum_primer.pdf"), "quantum_primer.pdf", "application/pdf", MediaKind.PDF_DOCUMENT, 250000L, 0L),
        MediaItem(Uri.parse("https://example.com/docs/neural_networks.pdf"), "neural_networks.pdf", "application/pdf", MediaKind.PDF_DOCUMENT, 410000L, 0L),
        MediaItem(Uri.parse("https://example.com/docs/ai_safety_manifesto.md"), "ai_safety_manifesto.md", "text/markdown", MediaKind.TEXT_DOCUMENT, 12000L, 0L),
        MediaItem(Uri.parse("https://example.com/docs/data_pipeline.csv"), "data_pipeline.csv", "text/csv", MediaKind.TEXT_DOCUMENT, 45000L, 0L),
        MediaItem(Uri.parse("https://example.com/docs/kernel_config.yaml"), "kernel_config.yaml", "text/yaml", MediaKind.TEXT_DOCUMENT, 8200L, 0L),
        MediaItem(Uri.parse("https://example.com/docs/server.log"), "server.log", "text/plain", MediaKind.TEXT_DOCUMENT, 32000L, 0L),
        MediaItem(Uri.parse("https://example.com/docs/algorithm.py"), "algorithm.py", "text/x-python", MediaKind.TEXT_DOCUMENT, 4500L, 0L),
        MediaItem(Uri.parse("https://example.com/docs/core_math.rs"), "core_math.rs", "text/x-rust", MediaKind.TEXT_DOCUMENT, 6700L, 0L),
        MediaItem(Uri.parse("https://example.com/archives/system_backup.zip"), "system_backup.zip", "application/zip", MediaKind.ARCHIVE, 89000000L, 0L),
        MediaItem(Uri.parse("https://example.com/archives/release_bundle.apk"), "release_bundle.apk", "application/vnd.android.package-archive", MediaKind.ARCHIVE, 18000000L, 0L),
        MediaItem(Uri.parse("https://example.com/archives/dataset_2026.tar.gz"), "dataset_2026.tar.gz", "application/zip", MediaKind.ARCHIVE, 45000000L, 0L),
        MediaItem(Uri.parse("https://example.com/archives/firmware.7z"), "firmware.7z", "application/zip", MediaKind.ARCHIVE, 12000000L, 0L),
        MediaItem(Uri.parse("https://example.com/images/wallpaper_nebula.jpg"), "wallpaper_nebula.jpg", "image/jpeg", MediaKind.IMAGE, 3200000L, 0L),
        MediaItem(Uri.parse("https://example.com/images/vector_logo.svg"), "vector_logo.svg", "image/svg+xml", MediaKind.IMAGE, 15000L, 0L)
    )
}

data class PlayableMediaItem(
    val id: String,
    val title: String,
    val artist: String,
    val durationText: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UniversalMediaPlayerScreen(
    onBack: () -> Unit,
    onSendMediaToChat: (String) -> Unit = {}
) {
    val context = LocalContext.current

    val samplePlaylist = remember {
        listOf(
            PlayableMediaItem("1", "Lo-Fi Coding Focus Beat", "AI Synth Labs", "2:45"),
            PlayableMediaItem("2", "Ambient Neural Atmosphere", "DeepMind Frequencies", "3:12"),
            PlayableMediaItem("3", "Cyberpunk Algorithm Pulse", "Matrix Soundworks", "2:30"),
            PlayableMediaItem("4", "Omni Meditation Wave", "Silicon Flow", "4:15")
        )
    }

    var currentTrack by remember { mutableStateOf(samplePlaylist.first()) }
    var isPlaying by remember { mutableStateOf(false) }
    var playbackProgress by remember { mutableFloatStateOf(0.35f) }
    var isLooping by remember { mutableStateOf(false) }

    // Mock/Simulated Android MediaPlayer handle
    var mediaPlayerInstance by remember { mutableStateOf<MediaPlayer?>(null) }

    // FIX BUG-08: Clean DisposableEffect ensures MediaPlayer stops and releases on unmount/screen exit!
    DisposableEffect(Unit) {
        onDispose {
            try {
                mediaPlayerInstance?.stop()
                mediaPlayerInstance?.release()
                mediaPlayerInstance = null
            } catch (_: Exception) {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Universal Media Studio", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("media_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            onSendMediaToChat("Now playing: \"${currentTrack.title}\" by ${currentTrack.artist}. Suggest similar tracks or background study playlists.")
                            Toast.makeText(context, "Track sent to OmniChat!", Toast.LENGTH_SHORT).show()
                            onBack()
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send Track to Chat", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Player Card / Vinyl Visualizer
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (isPlaying) Icons.Default.GraphicEq else Icons.Default.Audiotrack,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = currentTrack.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = currentTrack.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Progress Bar & Controls
            Column(modifier = Modifier.fillMaxWidth()) {
                Slider(
                    value = playbackProgress,
                    onValueChange = { playbackProgress = it },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("0:58", style = MaterialTheme.typography.labelSmall)
                    Text(currentTrack.durationText, style = MaterialTheme.typography.labelSmall)
                }

                // Controls row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { isLooping = !isLooping }) {
                        Icon(
                            Icons.Default.Repeat,
                            contentDescription = "Loop Track",
                            tint = if (isLooping) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .clickable {
                                isPlaying = !isPlaying
                                val msg = if (isPlaying) "Playing ${currentTrack.title}" else "Paused"
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    IconButton(onClick = {
                        val next = samplePlaylist[(samplePlaylist.indexOf(currentTrack) + 1) % samplePlaylist.size]
                        currentTrack = next
                    }) {
                        Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Next")
                    }
                }
            }

            Text("Audio Library", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

            // Playlist
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(samplePlaylist) { track ->
                    val isCurrent = track.id == currentTrack.id
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                currentTrack = track
                                isPlaying = true
                            },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCurrent) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Audiotrack,
                                contentDescription = null,
                                tint = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = track.title,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = track.artist,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = track.durationText,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        }
    }
}
