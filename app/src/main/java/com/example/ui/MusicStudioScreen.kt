package com.example.ui

import android.content.Context
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.remote.AudioSynthesizer
import com.example.data.remote.SoundPreset
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

data class BuiltInSong(
    val id: String,
    val title: String,
    val artist: String,
    val genre: String,
    val bpm: Int,
    val preset: SoundPreset
)

enum class VisualizerMode(val label: String, val icon: String) {
    SPECTRUM_BARS("32-Band Spectrum", "📊"),
    BEAT_RADAR("Pulsing Beat Radar", "💫"),
    WAVEFORM("Waveform Ribbon", "〰️")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicStudioScreen(
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isPlaying by remember { mutableStateOf(false) }
    var currentSongTitle by remember { mutableStateOf("Synthwave Cyber Pulse") }
    var currentSongArtist by remember { mutableStateOf("Omni Beats AI Studio") }
    var currentBpm by remember { mutableIntStateOf(128) }
    var isUploadedSong by remember { mutableStateOf(false) }

    // Media Player reference for uploaded custom audio
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var customAudioUri by remember { mutableStateOf<Uri?>(null) }

    // Playback Tracking
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var totalDurationMs by remember { mutableLongStateOf(184000L) } // ~3:04
    var isLooping by remember { mutableStateOf(true) }

    // Tune & Equalizer Settings
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) } // 0.5x to 2.0x
    var pitchShift by remember { mutableFloatStateOf(1.0f) } // 0.8 to 1.2
    var volumeBooster by remember { mutableFloatStateOf(0.9f) } // 0.0 to 1.5
    var bassLevel by remember { mutableFloatStateOf(2.5f) } // -10 dB to +10 dB
    var midLevel by remember { mutableFloatStateOf(0.0f) }
    var trebleLevel by remember { mutableFloatStateOf(1.5f) }

    // Visualizer selection: 0 = 32-Band Spectrum, 1 = Pulsing Radar, 2 = Waveform Ribbon
    var visualizerMode by remember { mutableStateOf(VisualizerMode.SPECTRUM_BARS) }

    // Controls tab: 0 = Player & Track, 1 = Tune & Speed, 2 = 3-Band Equalizer
    var selectedControlTab by remember { mutableIntStateOf(0) }

    val builtInTracks = remember {
        listOf(
            BuiltInSong("track_1", "Synthwave Cyber Pulse", "Omni Studio", "Electronic", 128, SoundPreset.SCIFI_LASER),
            BuiltInSong("track_2", "Midnight Lo-Fi Chill", "Atmosphere AI", "Lo-Fi Hip Hop", 85, SoundPreset.SOOTHING_RAIN),
            BuiltInSong("track_3", "Deep 432Hz Resonance", "Zen Harmonics", "Ambient Meditation", 60, SoundPreset.AMBIENT_DRONE),
            BuiltInSong("track_4", "Sub-Bass Heavy Impact", "Bass Nation", "Future Bass", 140, SoundPreset.BASS_DROP),
            BuiltInSong("track_5", "Cosmic Starlight Pad", "Galaxy Beats", "Space Synth", 110, SoundPreset.COSMIC_PAD)
        )
    }

    // Audio file picker launcher
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                mediaPlayer?.release()
                mediaPlayer = null
                customAudioUri = uri

                val mp = MediaPlayer().apply {
                    setDataSource(context, uri)
                    prepare()
                    isLooping = true
                }
                mediaPlayer = mp
                isUploadedSong = true
                currentSongTitle = uri.lastPathSegment?.substringAfterLast('/')?.substringBeforeLast('.') ?: "My Audio File"
                currentSongArtist = "Local Uploaded Track"
                totalDurationMs = mp.duration.toLong().coerceAtLeast(1000L)
                currentPositionMs = 0L
                currentBpm = 120

                mp.start()
                isPlaying = true
                Toast.makeText(context, "Playing: $currentSongTitle", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Error loading audio: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Timer updater coroutine while playing
    LaunchedEffect(isPlaying, isUploadedSong) {
        while (isActive && isPlaying) {
            delay(80)
            if (isUploadedSong && mediaPlayer != null) {
                try {
                    currentPositionMs = mediaPlayer!!.currentPosition.toLong()
                } catch (_: Exception) {}
            } else {
                currentPositionMs += 80
                if (currentPositionMs >= totalDurationMs) {
                    if (isLooping) {
                        currentPositionMs = 0L
                    } else {
                        isPlaying = false
                    }
                }
            }
        }
    }

    // Apply speed and pitch changes to MediaPlayer
    LaunchedEffect(playbackSpeed, pitchShift) {
        val mp = mediaPlayer
        if (mp != null) {
            try {
                val params = PlaybackParams().apply {
                    speed = playbackSpeed
                    pitch = pitchShift
                }
                mp.playbackParams = params
            } catch (_: Exception) {}
        }
    }

    // Apply volume changes
    LaunchedEffect(volumeBooster) {
        val mp = mediaPlayer
        if (mp != null) {
            try {
                val vol = volumeBooster.coerceIn(0f, 1f)
                mp.setVolume(vol, vol)
            } catch (_: Exception) {}
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
            AudioSynthesizer.stopSound()
        }
    }

    // Visualizer beat pulse animations
    val infiniteTransition = rememberInfiniteTransition(label = "music_pulse")
    val pulseAnim by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween((60000 / currentBpm.coerceAtLeast(60)), easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Header
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
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Music & Beat Studio",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Audio Visualizer • Tune Equalizer • ${currentBpm} BPM",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Upload Song Button
                    FilledTonalButton(
                        onClick = {
                            audioPickerLauncher.launch(
                                arrayOf("audio/*", "application/ogg", "*/*")
                            )
                        },
                        modifier = Modifier.testTag("upload_song_btn")
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Upload Song", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        // Live Beat Visualizer Display Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0F172A),
                            Color(0xFF020617)
                        )
                    )
                )
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            when (visualizerMode) {
                VisualizerMode.SPECTRUM_BARS -> {
                    // 32-Band Dynamic Frequency Spectrum Bars
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("visualizer_spectrum_canvas")
                    ) {
                        val numBars = 32
                        val barSpacing = 6.dp.toPx()
                        val availableWidth = size.width - (numBars - 1) * barSpacing
                        val barWidth = availableWidth / numBars

                        val bassFactor = (bassLevel + 10f) / 20f
                        val midFactor = (midLevel + 10f) / 20f
                        val trebleFactor = (trebleLevel + 10f) / 20f

                        for (i in 0 until numBars) {
                            val bandProgress = i.toFloat() / numBars
                            val factor = when {
                                bandProgress < 0.33f -> bassFactor
                                bandProgress < 0.66f -> midFactor
                                else -> trebleFactor
                            }

                            // Calculate dynamic reactive bar height
                            val timeSec = currentPositionMs / 1000f
                            val wave1 = abs(sin((i * 0.45f) + (timeSec * (currentBpm / 20f))))
                            val wave2 = abs(sin((i * 0.8f) - (timeSec * 3f)))
                            val activity = if (isPlaying) (wave1 * 0.6f + wave2 * 0.4f) else 0.08f
                            val barHeight = ((size.height * 0.85f) * activity * factor * volumeBooster).coerceIn(8f, size.height * 0.95f)

                            val x = i * (barWidth + barSpacing)
                            val y = (size.height - barHeight) / 2f

                            val barBrush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF38BDF8),
                                    Color(0xFF818CF8),
                                    Color(0xFFC084FC)
                                ),
                                startY = y,
                                endY = y + barHeight
                            )

                            drawRoundRect(
                                brush = barBrush,
                                topLeft = Offset(x, y),
                                size = Size(barWidth, barHeight),
                                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                            )
                        }
                    }
                }

                VisualizerMode.BEAT_RADAR -> {
                    // Pulsing Radar Sphere with concentric ripple rings
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("visualizer_radar_canvas")
                    ) {
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val maxRadius = size.minDimension * 0.42f
                        val currentScale = if (isPlaying) pulseAnim else 1.0f

                        // Outer glowing rings
                        for (ring in 1..4) {
                            val r = (maxRadius * (ring / 4f) * currentScale).coerceAtLeast(10f)
                            drawCircle(
                                color = Color(0xFF6366F1).copy(alpha = (0.5f / ring) * if (isPlaying) 1f else 0.2f),
                                center = center,
                                radius = r,
                                style = Stroke(width = 3.dp.toPx())
                            )
                        }

                        // Center glowing orb
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFF38BDF8), Color(0xFF818CF8), Color(0xFF1E1B4B)),
                                center = center,
                                radius = (maxRadius * 0.35f) * currentScale
                            ),
                            center = center,
                            radius = (maxRadius * 0.35f) * currentScale
                        )
                    }
                }

                VisualizerMode.WAVEFORM -> {
                    // Continuous Waveform Oscilloscope Ribbon
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("visualizer_waveform_canvas")
                    ) {
                        val centerY = size.height / 2f
                        val path = Path()
                        val points = 80
                        val step = size.width / points
                        val amp = if (isPlaying) (size.height * 0.28f * (volumeBooster.coerceAtMost(1.2f))) else 10f

                        for (i in 0..points) {
                            val x = i * step
                            val freq = (i.toFloat() / points) * 4f * PI.toFloat()
                            val y = centerY + (sin(freq + wavePhase) * amp * sin(i.toFloat() / points * PI.toFloat()))
                            if (i == 0) path.moveTo(x, y.toFloat()) else path.lineTo(x, y.toFloat())
                        }

                        drawPath(
                            path = path,
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color(0xFF38BDF8), Color(0xFFE879F9), Color(0xFF34D399))
                            ),
                            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                }
            }

            // Visualizer Mode Selector Chips in Top-Right
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                VisualizerMode.values().forEach { mode ->
                    FilterChip(
                        selected = visualizerMode == mode,
                        onClick = { visualizerMode = mode },
                        label = { Text(mode.icon, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            // Live Song Title Tag in Bottom-Left
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.Black.copy(alpha = 0.6f),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isPlaying) Color(0xFF22C55E) else Color(0xFF94A3B8))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = currentSongTitle,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "$currentSongArtist • ${playbackSpeed}x speed",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            }
        }

        // Seek Bar & Time Display
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                Slider(
                    value = currentPositionMs.toFloat(),
                    onValueChange = { newPos ->
                        currentPositionMs = newPos.toLong()
                        if (isUploadedSong && mediaPlayer != null) {
                            try { mediaPlayer?.seekTo(newPos.toInt()) } catch (_: Exception) {}
                        }
                    },
                    valueRange = 0f..totalDurationMs.toFloat().coerceAtLeast(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("music_seek_slider")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        formatDuration(currentPositionMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        formatDuration(totalDurationMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Playback Controls Row (Play, Pause, Prev, Next, Loop)
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Loop toggle
                IconButton(onClick = {
                    isLooping = !isLooping
                    mediaPlayer?.isLooping = isLooping
                }) {
                    Icon(
                        Icons.Default.Repeat,
                        contentDescription = "Loop",
                        tint = if (isLooping) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Skip back 10s
                IconButton(onClick = {
                    currentPositionMs = (currentPositionMs - 10000).coerceAtLeast(0)
                    mediaPlayer?.let { try { it.seekTo(currentPositionMs.toInt()) } catch (_: Exception) {} }
                }) {
                    Icon(Icons.Default.FastRewind, contentDescription = "Rewind 10s")
                }

                // Primary Play / Pause Button
                Button(
                    onClick = {
                        if (isPlaying) {
                            isPlaying = false
                            if (isUploadedSong) {
                                try { mediaPlayer?.pause() } catch (_: Exception) {}
                            } else {
                                AudioSynthesizer.stopSound()
                            }
                        } else {
                            isPlaying = true
                            if (isUploadedSong) {
                                try { mediaPlayer?.start() } catch (_: Exception) {}
                            } else {
                                coroutineScope.launch {
                                    val currentPreset = builtInTracks.firstOrNull { it.title == currentSongTitle }?.preset
                                        ?: SoundPreset.SCIFI_LASER
                                    AudioSynthesizer.playSound(currentPreset, volumeBooster)
                                }
                            }
                        }
                    },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.size(56.dp).testTag("music_play_pause_btn")
                ) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Skip forward 10s
                IconButton(onClick = {
                    currentPositionMs = (currentPositionMs + 10000).coerceAtMost(totalDurationMs)
                    mediaPlayer?.let { try { it.seekTo(currentPositionMs.toInt()) } catch (_: Exception) {} }
                }) {
                    Icon(Icons.Default.FastForward, contentDescription = "Forward 10s")
                }

                // Stop Button
                IconButton(onClick = {
                    isPlaying = false
                    currentPositionMs = 0L
                    mediaPlayer?.let { try { it.pause(); it.seekTo(0) } catch (_: Exception) {} }
                    AudioSynthesizer.stopSound()
                }) {
                    Icon(Icons.Default.Stop, contentDescription = "Stop")
                }
            }
        }

        // Tabs: Tracks, Tune & Speed, Equalizer
        TabRow(
            selectedTabIndex = selectedControlTab,
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.fillMaxWidth().testTag("music_tabs_row")
        ) {
            Tab(
                selected = selectedControlTab == 0,
                onClick = { selectedControlTab = 0 },
                text = { Text("Tracks") },
                icon = { Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = null) }
            )
            Tab(
                selected = selectedControlTab == 1,
                onClick = { selectedControlTab = 1 },
                text = { Text("Tune & Speed") },
                icon = { Icon(Icons.Default.Speed, contentDescription = null) }
            )
            Tab(
                selected = selectedControlTab == 2,
                onClick = { selectedControlTab = 2 },
                text = { Text("Equalizer") },
                icon = { Icon(Icons.Default.Equalizer, contentDescription = null) }
            )
        }

        // Control Panel Body
        Surface(
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                when (selectedControlTab) {
                    0 -> {
                        // Track Selection
                        Text(
                            text = "Select Audio Track:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(builtInTracks) { track ->
                                val isSelected = currentSongTitle == track.title
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        mediaPlayer?.pause()
                                        isUploadedSong = false
                                        currentSongTitle = track.title
                                        currentSongArtist = track.artist
                                        currentBpm = track.bpm
                                        totalDurationMs = 184000L
                                        currentPositionMs = 0L
                                        isPlaying = true
                                        coroutineScope.launch {
                                            AudioSynthesizer.playSound(track.preset, volumeBooster)
                                        }
                                    },
                                    label = { Text("${track.title} (${track.bpm} BPM)") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                            }
                        }
                    }

                    1 -> {
                        // Tune, Speed & Pitch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Speed: ${"%.2f".format(playbackSpeed)}x",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.width(100.dp)
                            )
                            Slider(
                                value = playbackSpeed,
                                onValueChange = { playbackSpeed = it },
                                valueRange = 0.5f..2.0f,
                                modifier = Modifier.weight(1f).testTag("music_speed_slider")
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Pitch: ${"%.2f".format(pitchShift)}x",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.width(100.dp)
                            )
                            Slider(
                                value = pitchShift,
                                onValueChange = { pitchShift = it },
                                valueRange = 0.8f..1.2f,
                                modifier = Modifier.weight(1f).testTag("music_pitch_slider")
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Volume: ${(volumeBooster * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.width(100.dp)
                            )
                            Slider(
                                value = volumeBooster,
                                onValueChange = { volumeBooster = it },
                                valueRange = 0.0f..1.5f,
                                modifier = Modifier.weight(1f).testTag("music_volume_slider")
                            )
                        }
                    }

                    2 -> {
                        // 3-Band Equalizer (Bass, Mid, Treble)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "🎸 Bass: ${"%.1f".format(bassLevel)}dB",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.width(100.dp)
                            )
                            Slider(
                                value = bassLevel,
                                onValueChange = { bassLevel = it },
                                valueRange = -10f..10f,
                                modifier = Modifier.weight(1f).testTag("slider_bass")
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "🎤 Mid: ${"%.1f".format(midLevel)}dB",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.width(100.dp)
                            )
                            Slider(
                                value = midLevel,
                                onValueChange = { midLevel = it },
                                valueRange = -10f..10f,
                                modifier = Modifier.weight(1f).testTag("slider_mid")
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "🔔 Treble: ${"%.1f".format(trebleLevel)}dB",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.width(100.dp)
                            )
                            Slider(
                                value = trebleLevel,
                                onValueChange = { trebleLevel = it },
                                valueRange = -10f..10f,
                                modifier = Modifier.weight(1f).testTag("slider_treble")
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
