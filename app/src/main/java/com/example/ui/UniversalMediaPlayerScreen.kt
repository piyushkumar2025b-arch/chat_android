package com.example.ui

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import android.widget.MediaController
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class MediaKind {
    VIDEO, AUDIO, TEXT_DOCUMENT, PDF_DOCUMENT, IMAGE, UNKNOWN
}

data class PlayableMediaItem(
    val uri: Uri,
    val displayName: String,
    val sizeBytes: Long,
    val mimeType: String,
    val kind: MediaKind,
    val durationMs: Long = 0L,
    val textPreview: String? = null
)

@Composable
fun UniversalMediaPlayerScreen(
    onBack: () -> Unit = {},
    onSendToChat: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var activeMedia by remember { mutableStateOf<PlayableMediaItem?>(null) }
    val playlist = remember { mutableStateListOf<PlayableMediaItem>() }

    // Audio playback state
    var audioPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var videoMediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosMs by remember { mutableLongStateOf(0L) }
    var totalDurationMs by remember { mutableLongStateOf(0L) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var isLooping by remember { mutableStateOf(false) }
    var volumeLevel by remember { mutableFloatStateOf(1.0f) }

    // VideoView holder
    var videoViewInstance by remember { mutableStateOf<VideoView?>(null) }
    var showAddUrlDialog by remember { mutableStateOf(false) }
    var streamUrlInput by remember { mutableStateOf("") }

    // Pre-populate demo / bundled items if empty
    LaunchedEffect(Unit) {
        if (playlist.isEmpty()) {
            val demoMedia = listOf(
                PlayableMediaItem(
                    uri = Uri.parse("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"),
                    displayName = "Big Buck Bunny (HD Stream)",
                    sizeBytes = 158_000_000L,
                    mimeType = "video/mp4",
                    kind = MediaKind.VIDEO,
                    durationMs = 596_000L
                ),
                PlayableMediaItem(
                    uri = Uri.parse("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4"),
                    displayName = "Elephants Dream (4K CGI Film)",
                    sizeBytes = 109_000_000L,
                    mimeType = "video/mp4",
                    kind = MediaKind.VIDEO,
                    durationMs = 653_000L
                ),
                PlayableMediaItem(
                    uri = Uri.parse("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"),
                    displayName = "Synthesized Melodic Anthem (MP3 Audio)",
                    sizeBytes = 8_200_000L,
                    mimeType = "audio/mpeg",
                    kind = MediaKind.AUDIO,
                    durationMs = 372_000L
                ),
                PlayableMediaItem(
                    uri = Uri.parse("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3"),
                    displayName = "Ambient Chill Groove (MP3 Audio)",
                    sizeBytes = 7_100_000L,
                    mimeType = "audio/mpeg",
                    kind = MediaKind.AUDIO,
                    durationMs = 423_000L
                )
            )
            playlist.addAll(demoMedia)
            activeMedia = demoMedia.first()
        }
    }

    // Picker for ANY file (*/*)
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val resolvedItem = resolveMediaItemFromUri(context, uri)
            playlist.add(0, resolvedItem)
            activeMedia = resolvedItem
            Toast.makeText(context, "Loaded: ${resolvedItem.displayName}", Toast.LENGTH_SHORT).show()
        }
    }

    // Manage Audio Player lifecycle
    fun releaseAudioPlayer() {
        try {
            audioPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
        } catch (_: Exception) {}
        audioPlayer = null
        isPlaying = false
        currentPosMs = 0L
        totalDurationMs = 0L
    }

    fun startAudioPlayback(item: PlayableMediaItem) {
        releaseAudioPlayer()
        try {
            val loopSetting = isLooping
            val currentSpeed = playbackSpeed
            val mp = MediaPlayer().apply {
                setDataSource(context, item.uri)
                isLooping = loopSetting
                setVolume(volumeLevel, volumeLevel)
                setOnPreparedListener { player ->
                    totalDurationMs = player.duration.toLong()
                    player.start()
                    isPlaying = true
                    applySpeed(player, currentSpeed)
                }
                setOnCompletionListener {
                    if (!loopSetting) {
                        isPlaying = false
                        currentPosMs = 0L
                    }
                }
                setOnErrorListener { _, what, extra ->
                    Toast.makeText(context, "Audio playback error: $what, $extra", Toast.LENGTH_SHORT).show()
                    isPlaying = false
                    true
                }
                prepareAsync()
            }
            audioPlayer = mp
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to initialize playback: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Effect when activeMedia changes
    LaunchedEffect(activeMedia) {
        val cur = activeMedia ?: return@LaunchedEffect
        when (cur.kind) {
            MediaKind.AUDIO -> {
                videoViewInstance?.stopPlayback()
                startAudioPlayback(cur)
            }
            MediaKind.VIDEO -> {
                releaseAudioPlayer()
                videoViewInstance?.let { vv ->
                    vv.setVideoURI(cur.uri)
                    vv.start()
                    isPlaying = true
                }
            }
            else -> {
                releaseAudioPlayer()
                videoViewInstance?.stopPlayback()
            }
        }
    }

    // Progress updater coroutine
    LaunchedEffect(isPlaying, activeMedia) {
        while (isActive) {
            if (activeMedia?.kind == MediaKind.AUDIO && audioPlayer != null) {
                try {
                    if (audioPlayer?.isPlaying == true) {
                        currentPosMs = audioPlayer?.currentPosition?.toLong() ?: 0L
                        totalDurationMs = audioPlayer?.duration?.toLong() ?: 0L
                    }
                } catch (_: Exception) {}
            } else if (activeMedia?.kind == MediaKind.VIDEO && videoViewInstance != null) {
                try {
                    if (videoViewInstance?.isPlaying == true) {
                        currentPosMs = videoViewInstance?.currentPosition?.toLong() ?: 0L
                        totalDurationMs = videoViewInstance?.duration?.toLong() ?: 0L
                    }
                } catch (_: Exception) {}
            }
            delay(500)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            releaseAudioPlayer()
            videoViewInstance?.stopPlayback()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Toolbar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("media_player_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "Universal Media & File Player",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Play Video, Audio, Docs, Any Local/Cloud File",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilledTonalButton(
                        onClick = { showAddUrlDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("add_stream_url_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = "Stream URL",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add URL", fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = { filePickerLauncher.launch("*/*") },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("open_any_file_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = "Open File",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Open File", fontSize = 12.sp)
                    }
                }
            }
        }

        // Active Player Canvas / View Area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            val item = activeMedia
            if (item == null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Movie,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No File Selected",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Tap 'Open File' or choose an item from the library below to start playing.",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            } else {
                when (item.kind) {
                    MediaKind.VIDEO -> {
                        // Native Android VideoView with MediaController controls
                        key(item.uri) {
                            AndroidView(
                                factory = { ctx ->
                                    VideoView(ctx).apply {
                                        val mc = MediaController(ctx)
                                        mc.setAnchorView(this)
                                        setMediaController(mc)
                                        setVideoURI(item.uri)
                                        setOnPreparedListener { mp ->
                                            videoMediaPlayer = mp
                                            mp.isLooping = isLooping
                                            applySpeed(mp, playbackSpeed)
                                            totalDurationMs = mp.duration.toLong()
                                            start()
                                            isPlaying = true
                                        }
                                        setOnCompletionListener {
                                            if (isLooping) {
                                                start()
                                            } else {
                                                isPlaying = false
                                            }
                                        }
                                        setOnErrorListener { _, what, extra ->
                                            Toast.makeText(ctx, "Video playback note: ($what, $extra)", Toast.LENGTH_SHORT).show()
                                            isPlaying = false
                                            true
                                        }
                                        videoViewInstance = this
                                    }
                                },
                                update = { vv ->
                                    videoViewInstance = vv
                                },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("video_surface_view")
                            )
                        }
                    }

                    MediaKind.AUDIO -> {
                        // Animated Audio Waveform & Artwork Display
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(140.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .border(3.dp, MaterialTheme.colorScheme.primary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(72.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = item.displayName,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${formatFileSize(item.sizeBytes)} • ${item.mimeType}",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }

                    MediaKind.IMAGE -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            AsyncImage(
                                model = item.uri,
                                contentDescription = item.displayName,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp)
                            )
                        }
                    }

                    MediaKind.TEXT_DOCUMENT, MediaKind.PDF_DOCUMENT, MediaKind.UNKNOWN -> {
                        // Text Previewer
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = if (item.kind == MediaKind.PDF_DOCUMENT) Icons.Default.PictureAsPdf else Icons.Default.Description,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = item.displayName,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text(
                                    text = item.textPreview ?: "Document content loaded. Ready for viewing or sharing.",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Playback Controller Dock (For Video & Audio)
        if (activeMedia?.kind == MediaKind.AUDIO || activeMedia?.kind == MediaKind.VIDEO) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    // Timeline Slider & Timestamps
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatMediaDuration(currentPosMs),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Slider(
                            value = if (totalDurationMs > 0) currentPosMs.toFloat() / totalDurationMs else 0f,
                            onValueChange = { frac ->
                                val target = (frac * totalDurationMs).toLong()
                                currentPosMs = target
                                if (activeMedia?.kind == MediaKind.AUDIO) {
                                    try { audioPlayer?.seekTo(target.toInt()) } catch (_: Exception) {}
                                } else if (activeMedia?.kind == MediaKind.VIDEO) {
                                    try { videoViewInstance?.seekTo(target.toInt()) } catch (_: Exception) {}
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp)
                                .testTag("media_seek_bar")
                        )
                        Text(
                            text = formatMediaDuration(totalDurationMs),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Transport Buttons Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Replay 10s
                        IconButton(onClick = {
                            val target = (currentPosMs - 10000).coerceAtLeast(0L)
                            currentPosMs = target
                            if (activeMedia?.kind == MediaKind.AUDIO) audioPlayer?.seekTo(target.toInt())
                            else videoViewInstance?.seekTo(target.toInt())
                        }) {
                            Icon(Icons.Default.Replay10, contentDescription = "Rewind 10s")
                        }

                        // Play / Pause Main Button
                        Button(
                            onClick = {
                                if (activeMedia?.kind == MediaKind.AUDIO) {
                                    audioPlayer?.let { mp ->
                                        if (mp.isPlaying) {
                                            mp.pause()
                                            isPlaying = false
                                        } else {
                                            mp.start()
                                            isPlaying = true
                                        }
                                    }
                                } else if (activeMedia?.kind == MediaKind.VIDEO) {
                                    videoViewInstance?.let { vv ->
                                        if (vv.isPlaying) {
                                            vv.pause()
                                            isPlaying = false
                                        } else {
                                            vv.start()
                                            isPlaying = true
                                        }
                                    }
                                }
                            },
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .size(52.dp)
                                .testTag("play_pause_toggle_btn")
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        // Forward 10s
                        IconButton(onClick = {
                            val target = (currentPosMs + 10000).coerceAtMost(totalDurationMs)
                            currentPosMs = target
                            if (activeMedia?.kind == MediaKind.AUDIO) audioPlayer?.seekTo(target.toInt())
                            else videoViewInstance?.seekTo(target.toInt())
                        }) {
                            Icon(Icons.Default.Forward10, contentDescription = "Forward 10s")
                        }

                        // Loop Toggle
                        IconButton(onClick = {
                            isLooping = !isLooping
                            audioPlayer?.isLooping = isLooping
                            videoMediaPlayer?.isLooping = isLooping
                        }) {
                            Icon(
                                Icons.Default.Loop,
                                contentDescription = "Loop",
                                tint = if (isLooping) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Speed Cycle (0.75x, 1x, 1.25x, 1.5x, 2.0x)
                        TextButton(onClick = {
                            playbackSpeed = when (playbackSpeed) {
                                1.0f -> 1.25f
                                1.25f -> 1.5f
                                1.5f -> 2.0f
                                2.0f -> 0.75f
                                else -> 1.0f
                            }
                            audioPlayer?.let { applySpeed(it, playbackSpeed) }
                            videoMediaPlayer?.let { applySpeed(it, playbackSpeed) }
                        }) {
                            Text("${playbackSpeed}x", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Bottom Library & Actions Drawer
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.9f),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "File & Media Playlist (${playlist.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row {
                        OutlinedButton(
                            onClick = {
                                activeMedia?.let {
                                    val summary = "Media File: ${it.displayName}\nType: ${it.mimeType}\nSize: ${formatFileSize(it.sizeBytes)}\nDuration: ${formatMediaDuration(it.durationMs)}"
                                    onSendToChat(summary)
                                    Toast.makeText(context, "Sent file info to chat", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("send_file_to_chat_btn")
                        ) {
                            Text("Send to Chat", fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(playlist) { item ->
                        val isSelected = activeMedia?.uri == item.uri
                        Card(
                            onClick = { activeMedia = item },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("playlist_item_${item.displayName.hashCode()}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = when (item.kind) {
                                            MediaKind.VIDEO -> Icons.Default.Movie
                                            MediaKind.AUDIO -> Icons.Default.MusicNote
                                            MediaKind.IMAGE -> Icons.Default.PlayArrow
                                            MediaKind.PDF_DOCUMENT -> Icons.Default.PictureAsPdf
                                            else -> Icons.Default.Description
                                        },
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.displayName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${item.kind.name} • ${formatFileSize(item.sizeBytes)} • ${if (item.durationMs > 0) formatMediaDuration(item.durationMs) else item.mimeType}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                }

                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showAddUrlDialog) {
            AlertDialog(
                onDismissRequest = { showAddUrlDialog = false },
                title = { Text("Stream Any Media URL") },
                text = {
                    Column {
                        Text(
                            "Paste any video, music, or stream URL (MP4, MP3, HLS, WebM, etc.):",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = streamUrlInput,
                            onValueChange = { streamUrlInput = it },
                            placeholder = { Text("https://example.com/media.mp4") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("stream_url_input")
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val trimmed = streamUrlInput.trim()
                            if (trimmed.isNotBlank()) {
                                val uri = Uri.parse(trimmed)
                                val item = resolveMediaItemFromUri(context, uri)
                                playlist.add(0, item)
                                activeMedia = item
                                showAddUrlDialog = false
                                streamUrlInput = ""
                                Toast.makeText(context, "Added stream: ${item.displayName}", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.testTag("confirm_stream_url_btn")
                    ) {
                        Text("Play Stream")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddUrlDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

private fun applySpeed(player: MediaPlayer, speed: Float) {
    try {
        val params = player.playbackParams
        params.speed = speed
        player.playbackParams = params
    } catch (_: Exception) {}
}

internal fun formatMediaDuration(ms: Long): String {
    if (ms <= 0L) return "00:00"
    val seconds = (ms / 1000) % 60
    val minutes = (ms / (1000 * 60)) % 60
    val hours = ms / (1000 * 60 * 60)
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}

internal fun formatFileSize(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> String.format(Locale.US, "%.1f GB", gb)
        mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
        kb >= 1.0 -> String.format(Locale.US, "%.1f KB", kb)
        else -> "$bytes B"
    }
}

internal fun resolveMediaItemFromUri(context: Context, uri: Uri): PlayableMediaItem {
    var displayName = uri.lastPathSegment ?: "Media File"
    var sizeBytes = 0L
    var mimeType: String? = null

    if (uri.scheme == "content") {
        try {
            mimeType = context.contentResolver.getType(uri)
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) displayName = cursor.getString(nameIndex) ?: displayName
                    if (sizeIndex != -1) sizeBytes = cursor.getLong(sizeIndex)
                }
            }
        } catch (_: Exception) {}
    }

    if (mimeType.isNullOrBlank()) {
        val uriStr = uri.toString().lowercase(Locale.ROOT)
        mimeType = when {
            uriStr.endsWith(".mp4") || uriStr.endsWith(".mkv") || uriStr.endsWith(".webm") || uriStr.endsWith(".mov") || uriStr.endsWith(".3gp") || uriStr.endsWith(".m3u8") -> "video/mp4"
            uriStr.endsWith(".mp3") || uriStr.endsWith(".wav") || uriStr.endsWith(".ogg") || uriStr.endsWith(".m4a") || uriStr.endsWith(".flac") || uriStr.endsWith(".aac") -> "audio/mpeg"
            uriStr.endsWith(".png") || uriStr.endsWith(".jpg") || uriStr.endsWith(".jpeg") || uriStr.endsWith(".webp") || uriStr.endsWith(".gif") -> "image/jpeg"
            uriStr.endsWith(".pdf") -> "application/pdf"
            uriStr.endsWith(".txt") || uriStr.endsWith(".json") || uriStr.endsWith(".md") || uriStr.endsWith(".kt") || uriStr.endsWith(".html") -> "text/plain"
            uriStr.contains("video") -> "video/mp4"
            uriStr.contains("audio") || uriStr.contains("sound") -> "audio/mpeg"
            else -> "application/octet-stream"
        }
    }

    val kind = when {
        mimeType.startsWith("video/") -> MediaKind.VIDEO
        mimeType.startsWith("audio/") -> MediaKind.AUDIO
        mimeType.startsWith("image/") -> MediaKind.IMAGE
        mimeType.contains("pdf") -> MediaKind.PDF_DOCUMENT
        mimeType.startsWith("text/") || displayName.endsWith(".txt") || displayName.endsWith(".json") || displayName.endsWith(".md") || displayName.endsWith(".kt") || displayName.endsWith(".html") -> MediaKind.TEXT_DOCUMENT
        else -> MediaKind.UNKNOWN
    }

    var textPreview: String? = null
    if (kind == MediaKind.TEXT_DOCUMENT && uri.scheme == "content") {
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val reader = BufferedReader(InputStreamReader(stream))
                val sb = java.lang.StringBuilder()
                var lines = 0
                while (lines < 150) {
                    val line = reader.readLine() ?: break
                    sb.append(line).append("\n")
                    lines++
                }
                textPreview = sb.toString()
            }
        } catch (_: Exception) {}
    }

    var durationMs = 0L
    if (kind == MediaKind.VIDEO || kind == MediaKind.AUDIO) {
        try {
            val retriever = MediaMetadataRetriever()
            if (uri.scheme == "content") {
                retriever.setDataSource(context, uri)
            } else if (uri.scheme == "http" || uri.scheme == "https") {
                retriever.setDataSource(uri.toString(), HashMap())
            }
            val durStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            durationMs = durStr?.toLongOrNull() ?: 0L
            retriever.release()
        } catch (_: Exception) {}
    }

    return PlayableMediaItem(
        uri = uri,
        displayName = displayName,
        sizeBytes = sizeBytes,
        mimeType = mimeType,
        kind = kind,
        durationMs = durationMs,
        textPreview = textPreview
    )
}
