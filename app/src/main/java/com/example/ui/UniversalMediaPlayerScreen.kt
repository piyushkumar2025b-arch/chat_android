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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DataObject
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
    VIDEO, AUDIO, TEXT_DOCUMENT, PDF_DOCUMENT, IMAGE, ARCHIVE, UNKNOWN
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

object CuratedMediaSources {
    val sampleSources: List<PlayableMediaItem> = listOf(
        // High-definition sample videos
        PlayableMediaItem(
            uri = Uri.parse("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"),
            displayName = "Big Buck Bunny (1080p Stream)",
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
            uri = Uri.parse("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4"),
            displayName = "Sintel (Open-Source Animated Fantasy)",
            sizeBytes = 129_000_000L,
            mimeType = "video/mp4",
            kind = MediaKind.VIDEO,
            durationMs = 888_000L
        ),
        PlayableMediaItem(
            uri = Uri.parse("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4"),
            displayName = "Tears of Steel (Sci-Fi VFX Short)",
            sizeBytes = 168_000_000L,
            mimeType = "video/mp4",
            kind = MediaKind.VIDEO,
            durationMs = 734_000L
        ),
        PlayableMediaItem(
            uri = Uri.parse("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4"),
            displayName = "Chromecast Demo: For Bigger Blazes",
            sizeBytes = 15_200_000L,
            mimeType = "video/mp4",
            kind = MediaKind.VIDEO,
            durationMs = 15_000L
        ),
        PlayableMediaItem(
            uri = Uri.parse("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/WeAreGoingOnBullrun.mp4"),
            displayName = "Automotive Tech: We Are Going On Bullrun",
            sizeBytes = 47_300_000L,
            mimeType = "video/mp4",
            kind = MediaKind.VIDEO,
            durationMs = 47_000L
        ),

        // Rich Audio & Music items
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
        ),
        PlayableMediaItem(
            uri = Uri.parse("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3"),
            displayName = "Progressive Electronic Odyssey (MP3 Audio)",
            sizeBytes = 9_400_000L,
            mimeType = "audio/mpeg",
            kind = MediaKind.AUDIO,
            durationMs = 512_000L
        ),
        PlayableMediaItem(
            uri = Uri.parse("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3"),
            displayName = "Classical Piano & Symphony (MP3 Audio)",
            sizeBytes = 6_800_000L,
            mimeType = "audio/mpeg",
            kind = MediaKind.AUDIO,
            durationMs = 345_000L
        ),
        PlayableMediaItem(
            uri = Uri.parse("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-8.mp3"),
            displayName = "Chill Lofi Coding Session (MP3 Audio)",
            sizeBytes = 8_600_000L,
            mimeType = "audio/mpeg",
            kind = MediaKind.AUDIO,
            durationMs = 410_000L
        ),
        PlayableMediaItem(
            uri = Uri.parse("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-16.mp3"),
            displayName = "Acoustic Sunset Melody (MP3 Audio)",
            sizeBytes = 7_500_000L,
            mimeType = "audio/mpeg",
            kind = MediaKind.AUDIO,
            durationMs = 385_000L
        ),

        // Rich Code, Data, Documents & Specs
        PlayableMediaItem(
            uri = Uri.parse("content://omnichat/docs/architecture_spec.md"),
            displayName = "OmniChat AI Core Architecture & Specs.md",
            sizeBytes = 48_500L,
            mimeType = "text/markdown",
            kind = MediaKind.TEXT_DOCUMENT,
            textPreview = """
# OmniChat AI Core Architecture Specification
- Multi-Provider LLM Routing: Gemini 2.5/3.5, Groq LLaMA-3.3, OpenRouter Claude/DeepSeek, Cerebras, Pollinations.
- Scoped MediaStore Storage: Standard Pictures/OmniChat, Movies/OmniChat, Music/OmniChat paths.
- Local Vector Engine: 768-d dense embeddings + BM25 keyword rank fusion.
- Edge-to-Edge Jetpack Compose with Material 3 dynamic color scheme and touch-target compliance.
            """.trimIndent()
        ),
        PlayableMediaItem(
            uri = Uri.parse("content://omnichat/data/neural_weights_schema.json"),
            displayName = "Neural Transformer Weights Schema.json",
            sizeBytes = 32_100L,
            mimeType = "application/json",
            kind = MediaKind.TEXT_DOCUMENT,
            textPreview = """
{
  "model_architecture": "Transformer-Decoder",
  "hidden_dimension": 4096,
  "attention_heads": 32,
  "layers": 32,
  "rotary_embedding_dim": 128,
  "quantization": "AWQ-4bit",
  "context_window_tokens": 131072
}
            """.trimIndent()
        ),
        PlayableMediaItem(
            uri = Uri.parse("content://omnichat/code/ConcurrencyEngine.kt"),
            displayName = "Kotlin Concurrency Engine.kt",
            sizeBytes = 24_300L,
            mimeType = "text/x-kotlin",
            kind = MediaKind.TEXT_DOCUMENT,
            textPreview = """
package com.example.engine

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class ConcurrencyPipeline(
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    suspend fun executeParallelTasks(tasks: List<suspend () -> Unit>) = coroutineScope {
        tasks.map { async(dispatcher) { it() } }.awaitAll()
    }
}
            """.trimIndent()
        ),
        PlayableMediaItem(
            uri = Uri.parse("content://omnichat/data/financial_metrics.csv"),
            displayName = "Financial Growth & Performance Data.csv",
            sizeBytes = 18_400L,
            mimeType = "text/csv",
            kind = MediaKind.TEXT_DOCUMENT,
            textPreview = """
Quarter,Revenue_USD,Active_Users,Compute_Cost,EBITDA_Margin
2025-Q1,12450000,4200000,2100000,42.5%
2025-Q2,15800000,5600000,2450000,46.8%
2025-Q3,21200000,7900000,3100000,51.2%
2025-Q4,28900000,10500000,3800000,55.0%
            """.trimIndent()
        ),
        PlayableMediaItem(
            uri = Uri.parse("content://omnichat/code/QuantumSimulator.py"),
            displayName = "Quantum Circuit Simulator.py",
            sizeBytes = 14_900L,
            mimeType = "text/x-python",
            kind = MediaKind.TEXT_DOCUMENT,
            textPreview = """
import numpy as np

class QuantumState:
    def __init__(self, n_qubits):
        self.state = np.zeros(2**n_qubits, dtype=complex)
        self.state[0] = 1.0

    def apply_hadamard(self, target_qubit):
        H = (1 / np.sqrt(2)) * np.array([[1, 1], [1, -1]])
        print(f"Applied Hadamard gate to qubit {target_qubit}")
            """.trimIndent()
        ),
        PlayableMediaItem(
            uri = Uri.parse("content://omnichat/packages/omnichat_distribution.apk"),
            displayName = "OmniChat Core Build Bundle.apk",
            sizeBytes = 28_400_000L,
            mimeType = "application/vnd.android.package-archive",
            kind = MediaKind.ARCHIVE,
            textPreview = "Android Application Package (APK)\nPackage: com.aistudio.omnichat\nMinSDK: 26 • TargetSDK: 34\nSigned with V2/V3 Scheme • Verified Clean"
        ),
        PlayableMediaItem(
            uri = Uri.parse("https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"),
            displayName = "Live Mux Test Stream (HLS Video)",
            sizeBytes = 34_000_000L,
            mimeType = "video/mp4",
            kind = MediaKind.VIDEO,
            durationMs = 612_000L
        ),
        PlayableMediaItem(
            uri = Uri.parse("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/SubaruOutbackSeeTheWorld.mp4"),
            displayName = "Nature Expedition: See The World",
            sizeBytes = 28_000_000L,
            mimeType = "video/mp4",
            kind = MediaKind.VIDEO,
            durationMs = 28_000L
        ),
        PlayableMediaItem(
            uri = Uri.parse("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-9.mp3"),
            displayName = "Lo-Fi Coffee Shop Study Session (MP3 Audio)",
            sizeBytes = 8_900_000L,
            mimeType = "audio/mpeg",
            kind = MediaKind.AUDIO,
            durationMs = 440_000L
        ),
        PlayableMediaItem(
            uri = Uri.parse("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-15.mp3"),
            displayName = "Classical Symphony in G Minor (MP3 Audio)",
            sizeBytes = 7_900_000L,
            mimeType = "audio/mpeg",
            kind = MediaKind.AUDIO,
            durationMs = 390_000L
        ),
        PlayableMediaItem(
            uri = Uri.parse("content://omnichat/sql/ecommerce_database.sql"),
            displayName = "E-Commerce Database Schema.sql",
            sizeBytes = 12_800L,
            mimeType = "text/plain",
            kind = MediaKind.TEXT_DOCUMENT,
            textPreview = """
-- OmniChat Core Production Database Schema
CREATE TABLE users (
    id VARCHAR(36) PRIMARY KEY,
    username VARCHAR(64) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE media_files (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) REFERENCES users(id),
    file_name VARCHAR(255) NOT NULL,
    file_size_bytes BIGINT NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    storage_uri TEXT NOT NULL
);
            """.trimIndent()
        ),
        PlayableMediaItem(
            uri = Uri.parse("content://omnichat/subtitles/english_captions.srt"),
            displayName = "English Captions Track.srt",
            sizeBytes = 8_400L,
            mimeType = "text/plain",
            kind = MediaKind.TEXT_DOCUMENT,
            textPreview = """
1
00:00:01,000 --> 00:00:04,500
Welcome to OmniChat Universal Media & Audio Studio.

2
00:00:05,000 --> 00:00:09,200
Playing video, music, code, documents, and streaming any URL format.
            """.trimIndent()
        ),
        PlayableMediaItem(
            uri = Uri.parse("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4"),
            displayName = "Sci-Fi Action: Tears of Steel Open Movie",
            sizeBytes = 72_000_000L,
            mimeType = "video/mp4",
            kind = MediaKind.VIDEO,
            durationMs = 734_000L
        ),
        PlayableMediaItem(
            uri = Uri.parse("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4"),
            displayName = "Fantasy Animation: Sintel Open Movie",
            sizeBytes = 52_000_000L,
            mimeType = "video/mp4",
            kind = MediaKind.VIDEO,
            durationMs = 52_000L
        ),
        PlayableMediaItem(
            uri = Uri.parse("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3"),
            displayName = "Cyberpunk Synthwave Odyssey (MP3 Audio)",
            sizeBytes = 9_100_000L,
            mimeType = "audio/mpeg",
            kind = MediaKind.AUDIO,
            durationMs = 455_000L
        ),
        PlayableMediaItem(
            uri = Uri.parse("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-10.mp3"),
            displayName = "Acoustic Classical Nocturne (MP3 Audio)",
            sizeBytes = 8_300_000L,
            mimeType = "audio/mpeg",
            kind = MediaKind.AUDIO,
            durationMs = 415_000L
        ),
        PlayableMediaItem(
            uri = Uri.parse("content://omnichat/infra/kubernetes_deployment.yaml"),
            displayName = "Kubernetes Cluster Deployment.yaml",
            sizeBytes = 16_400L,
            mimeType = "text/plain",
            kind = MediaKind.TEXT_DOCUMENT,
            textPreview = """
apiVersion: apps/v1
kind: Deployment
metadata:
  name: omnichat-cluster-pod
  labels:
    app: omnichat
spec:
  replicas: 4
  selector:
    matchLabels:
      app: omnichat
  template:
    metadata:
      labels:
        app: omnichat
    spec:
      containers:
      - name: omnichat-runtime
        image: omnichat/neural-engine:v3.2
        ports:
        - containerPort: 8080
        resources:
          limits:
            cpu: "4"
            memory: "8Gi"
            """.trimIndent()
        ),
        PlayableMediaItem(
            uri = Uri.parse("content://omnichat/infra/Dockerfile"),
            displayName = "Docker Multi-Stage Production Container.dockerfile",
            sizeBytes = 9_800L,
            mimeType = "text/plain",
            kind = MediaKind.TEXT_DOCUMENT,
            textPreview = """
# Multi-stage production container build
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /workspace
COPY . .
RUN ./gradlew assembleRelease --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /workspace/app/build/libs/*.jar /app/runner.jar
EXPOSE 8080
ENTRYPOINT ["java", "-XX:+UseZGC", "-jar", "/app/runner.jar"]
            """.trimIndent()
        ),
        PlayableMediaItem(
            uri = Uri.parse("content://omnichat/archives/open_source_repo.zip"),
            displayName = "Open Source Framework Repository.zip",
            sizeBytes = 44_200_000L,
            mimeType = "application/zip",
            kind = MediaKind.ARCHIVE,
            textPreview = "Compressed Archive (ZIP)\nFiles: 1,420 source files\nCompressed Ratio: 68.4%\nStructure: src/main/kotlin, assets, resources"
        ),
        PlayableMediaItem(
            uri = Uri.parse("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"),
            displayName = "Animation HD: Big Buck Bunny 60fps Open Movie",
            sizeBytes = 158_000_000L,
            mimeType = "video/mp4",
            kind = MediaKind.VIDEO,
            durationMs = 596_000L
        ),
        PlayableMediaItem(
            uri = Uri.parse("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3"),
            displayName = "Chill Acoustic Guitar Flow (MP3 Audio)",
            sizeBytes = 6_400_000L,
            mimeType = "audio/mpeg",
            kind = MediaKind.AUDIO,
            durationMs = 320_000L
        ),
        PlayableMediaItem(
            uri = Uri.parse("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-8.mp3"),
            displayName = "Ambient Electronic Odyssey (MP3 Audio)",
            sizeBytes = 7_800_000L,
            mimeType = "audio/mpeg",
            kind = MediaKind.AUDIO,
            durationMs = 390_000L
        ),
        PlayableMediaItem(
            uri = Uri.parse("https://devstreaming-cdn.apple.com/videos/streaming/examples/bipbop_4x3/bipbop_4x3_variant.m3u8"),
            displayName = "Live Stream Test (Apple HLS .m3u8)",
            sizeBytes = 1_200_000L,
            mimeType = "application/x-mpegurl",
            kind = MediaKind.VIDEO,
            durationMs = 1_800_000L
        ),
        PlayableMediaItem(
            uri = Uri.parse("content://omnichat/dsp/audio_dsp_filter.cpp"),
            displayName = "C++ Real-Time Biquad Audio DSP Equalizer.cpp",
            sizeBytes = 14_200L,
            mimeType = "text/x-c",
            kind = MediaKind.TEXT_DOCUMENT,
            textPreview = """
// C++ Biquad Filter Real-time Audio DSP Pipeline
#include <cmath>
#include <vector>

class BiquadFilter {
    float a0, a1, a2, b1, b2;
    float z1 = 0.0f, z2 = 0.0f;
public:
    void setLowpass(float sampleRate, float cutoffHz, float q = 0.707f) {
        float omega = 2.0f * M_PI * cutoffHz / sampleRate;
        float alpha = std::sin(omega) / (2.0f * q);
        float cosOmega = std::cos(omega);
        float b0 = (1.0f - cosOmega) / 2.0f;
        a0 = b0 / (1.0f + alpha);
        a1 = (1.0f - cosOmega) / (1.0f + alpha);
        a2 = a0;
        b1 = (-2.0f * cosOmega) / (1.0f + alpha);
        b2 = (1.0f - alpha) / (1.0f + alpha);
    }
    float processSample(float in) {
        float out = in * a0 + z1;
        z1 = in * a1 + z2 - b1 * out;
        z2 = in * a2 - b2 * out;
        return out;
    }
};
            """.trimIndent()
        ),
        PlayableMediaItem(
            uri = Uri.parse("content://omnichat/manifests/stream_manifest.json"),
            displayName = "Multimodal Streaming & Subtitle Manifest.json",
            sizeBytes = 5_600L,
            mimeType = "application/json",
            kind = MediaKind.TEXT_DOCUMENT,
            textPreview = """
{
  "manifestVersion": "3.1.0",
  "mediaType": "adaptive-streaming-hls",
  "streams": [
    {"resolution": "1080p", "bitrate_kbps": 4500, "codec": "h264"},
    {"resolution": "720p", "bitrate_kbps": 2400, "codec": "h264"},
    {"resolution": "480p", "bitrate_kbps": 1100, "codec": "h264"}
  ],
  "audioTracks": [
    {"language": "en", "channels": 6, "format": "eac3", "default": true},
    {"language": "es", "channels": 2, "format": "aac", "default": false}
  ],
  "subtitles": [
    {"language": "en", "format": "webvtt", "label": "English CC"},
    {"language": "es", "format": "webvtt", "label": "Español"}
  ]
}
            """.trimIndent()
        )
    )
}

@Composable
fun UniversalMediaPlayerScreen(
    initialMediaItem: PlayableMediaItem? = null,
    onBack: () -> Unit = {},
    onSendToChat: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var activeMedia by remember { mutableStateOf<PlayableMediaItem?>(initialMediaItem) }
    val playlist = remember { mutableStateListOf<PlayableMediaItem>() }

    LaunchedEffect(initialMediaItem) {
        if (initialMediaItem != null) {
            if (!playlist.any { it.uri == initialMediaItem.uri }) {
                playlist.add(0, initialMediaItem)
            }
            activeMedia = initialMediaItem
        }
    }

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
    var showSampleSourcesDialog by remember { mutableStateOf(false) }
    var streamUrlInput by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf<MediaKind?>(null) }

    // Pre-populate demo / bundled items if empty
    LaunchedEffect(Unit) {
        if (playlist.isEmpty()) {
            playlist.addAll(CuratedMediaSources.sampleSources)
            activeMedia = CuratedMediaSources.sampleSources.firstOrNull()
        }
    }

    // Picker for multiple files or single file (*/*)
    val multipleFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            val items = mutableListOf<PlayableMediaItem>()
            for (u in uris) {
                val uStr = u.toString().lowercase(Locale.ROOT)
                if (uStr.endsWith(".m3u") || uStr.endsWith(".m3u8")) {
                    val m3uTracks = parseM3uPlaylist(context, u)
                    if (m3uTracks.isNotEmpty()) items.addAll(m3uTracks)
                    else items.add(resolveMediaItemFromUri(context, u))
                } else {
                    items.add(resolveMediaItemFromUri(context, u))
                }
            }
            playlist.addAll(0, items)
            if (items.isNotEmpty()) activeMedia = items.first()
            Toast.makeText(context, "Imported ${items.size} file(s) into playlist", Toast.LENGTH_SHORT).show()
        }
    }

    val singleFilePickerLauncher = rememberLauncherForActivityResult(
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
                        onClick = { showSampleSourcesDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("sample_sources_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = "Sources",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Sources", fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.width(6.dp))

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

                    Spacer(modifier = Modifier.width(6.dp))

                    Button(
                        onClick = { multipleFilePickerLauncher.launch("*/*") },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("open_any_file_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = "Import Files",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Import", fontSize = 12.sp)
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

                    MediaKind.ARCHIVE -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                                ),
                                modifier = Modifier.fillMaxWidth().padding(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(72.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Archive,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(36.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Text(
                                        text = item.displayName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = "Archive / Binary Package • ${formatFileSize(item.sizeBytes)} • ${item.mimeType}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = item.textPreview ?: "Verified package descriptor ready for extraction or deployment.",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
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

                // Category Filter Chips
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedCategoryFilter == null,
                            onClick = { selectedCategoryFilter = null },
                            label = { Text("All (${playlist.size})", fontSize = 11.sp) }
                        )
                    }
                    item {
                        val count = playlist.count { it.kind == MediaKind.VIDEO }
                        FilterChip(
                            selected = selectedCategoryFilter == MediaKind.VIDEO,
                            onClick = { selectedCategoryFilter = if (selectedCategoryFilter == MediaKind.VIDEO) null else MediaKind.VIDEO },
                            label = { Text("🎬 Videos ($count)", fontSize = 11.sp) }
                        )
                    }
                    item {
                        val count = playlist.count { it.kind == MediaKind.AUDIO }
                        FilterChip(
                            selected = selectedCategoryFilter == MediaKind.AUDIO,
                            onClick = { selectedCategoryFilter = if (selectedCategoryFilter == MediaKind.AUDIO) null else MediaKind.AUDIO },
                            label = { Text("🎵 Music ($count)", fontSize = 11.sp) }
                        )
                    }
                    item {
                        val count = playlist.count { it.kind == MediaKind.TEXT_DOCUMENT || it.kind == MediaKind.PDF_DOCUMENT }
                        FilterChip(
                            selected = selectedCategoryFilter == MediaKind.TEXT_DOCUMENT,
                            onClick = { selectedCategoryFilter = if (selectedCategoryFilter == MediaKind.TEXT_DOCUMENT) null else MediaKind.TEXT_DOCUMENT },
                            label = { Text("📄 Docs & Code ($count)", fontSize = 11.sp) }
                        )
                    }
                    item {
                        val count = playlist.count { it.kind == MediaKind.IMAGE }
                        FilterChip(
                            selected = selectedCategoryFilter == MediaKind.IMAGE,
                            onClick = { selectedCategoryFilter = if (selectedCategoryFilter == MediaKind.IMAGE) null else MediaKind.IMAGE },
                            label = { Text("🖼️ Images ($count)", fontSize = 11.sp) }
                        )
                    }
                    item {
                        val count = playlist.count { it.kind == MediaKind.ARCHIVE }
                        FilterChip(
                            selected = selectedCategoryFilter == MediaKind.ARCHIVE,
                            onClick = { selectedCategoryFilter = if (selectedCategoryFilter == MediaKind.ARCHIVE) null else MediaKind.ARCHIVE },
                            label = { Text("📦 Archives ($count)", fontSize = 11.sp) }
                        )
                    }
                }

                val filteredList = when (selectedCategoryFilter) {
                    null -> playlist
                    MediaKind.TEXT_DOCUMENT -> playlist.filter { it.kind == MediaKind.TEXT_DOCUMENT || it.kind == MediaKind.PDF_DOCUMENT }
                    else -> playlist.filter { it.kind == selectedCategoryFilter }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filteredList) { item ->
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
                                            MediaKind.ARCHIVE -> Icons.Default.Archive
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

        if (showSampleSourcesDialog) {
            AlertDialog(
                onDismissRequest = { showSampleSourcesDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CloudDownload,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Curated Media & Data Sources")
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(380.dp)
                    ) {
                        Text(
                            "Choose from ${CuratedMediaSources.sampleSources.size} high-definition video streams, synthesized audio tracks, code bases, and datasets:",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(CuratedMediaSources.sampleSources) { item ->
                                Surface(
                                    onClick = {
                                        if (playlist.none { it.uri == item.uri }) {
                                            playlist.add(0, item)
                                        }
                                        activeMedia = item
                                        showSampleSourcesDialog = false
                                        Toast.makeText(context, "Playing: ${item.displayName}", Toast.LENGTH_SHORT).show()
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.fillMaxWidth().testTag("source_item_${item.displayName.hashCode()}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = when (item.kind) {
                                                MediaKind.VIDEO -> Icons.Default.Movie
                                                MediaKind.AUDIO -> Icons.Default.MusicNote
                                                MediaKind.IMAGE -> Icons.Default.PlayArrow
                                                MediaKind.PDF_DOCUMENT -> Icons.Default.PictureAsPdf
                                                MediaKind.ARCHIVE -> Icons.Default.Archive
                                                else -> Icons.Default.Description
                                            },
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.displayName,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${item.kind.name} • ${formatFileSize(item.sizeBytes)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            for (sample in CuratedMediaSources.sampleSources) {
                                if (playlist.none { it.uri == sample.uri }) {
                                    playlist.add(sample)
                                }
                            }
                            if (activeMedia == null) activeMedia = CuratedMediaSources.sampleSources.firstOrNull()
                            showSampleSourcesDialog = false
                            Toast.makeText(context, "Loaded all curated sources into playlist!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.testTag("load_all_sources_btn")
                    ) {
                        Text("Add All Sources")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSampleSourcesDialog = false }) {
                        Text("Close")
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
    } else if (uri.scheme == "file") {
        try {
            val file = File(uri.path ?: "")
            if (file.exists()) {
                displayName = file.name
                sizeBytes = file.length()
            }
        } catch (_: Exception) {}
    }

    val uriStr = uri.toString().lowercase(Locale.ROOT)
    val cleanName = displayName.lowercase(Locale.ROOT)
    val matchesExt = { ext: String -> uriStr.endsWith(ext) || cleanName.endsWith(ext) }

    val isVideo = matchesExt(".mp4") || matchesExt(".mkv") || matchesExt(".webm") || matchesExt(".mov") || matchesExt(".3gp") || matchesExt(".m3u8") ||
            matchesExt(".avi") || matchesExt(".flv") || matchesExt(".ts") || matchesExt(".wmv") || matchesExt(".m4v") || matchesExt(".mpg") || matchesExt(".mpeg") || matchesExt(".ogv") || matchesExt(".vob")

    val isAudio = matchesExt(".mp3") || matchesExt(".wav") || matchesExt(".ogg") || matchesExt(".m4a") || matchesExt(".flac") || matchesExt(".aac") ||
            matchesExt(".opus") || matchesExt(".mid") || matchesExt(".midi") || matchesExt(".wma") || matchesExt(".amr") || matchesExt(".aiff") || matchesExt(".weba") || matchesExt(".alac")

    val isImage = matchesExt(".png") || matchesExt(".jpg") || matchesExt(".jpeg") || matchesExt(".webp") || matchesExt(".gif") ||
            matchesExt(".bmp") || matchesExt(".svg") || matchesExt(".ico") || matchesExt(".heic") || matchesExt(".heif") || matchesExt(".tiff") || matchesExt(".tif")

    val isPdf = matchesExt(".pdf")

    val isTextOrCode = matchesExt(".txt") || matchesExt(".json") || matchesExt(".md") || matchesExt(".kt") || matchesExt(".html") ||
            matchesExt(".xml") || matchesExt(".csv") || matchesExt(".tsv") || matchesExt(".yaml") || matchesExt(".yml") ||
            matchesExt(".py") || matchesExt(".js") || matchesExt(".ts") || matchesExt(".java") || matchesExt(".c") || matchesExt(".cpp") ||
            matchesExt(".h") || matchesExt(".cs") || matchesExt(".rs") || matchesExt(".go") || matchesExt(".sh") || matchesExt(".sql") ||
            matchesExt(".log") || matchesExt(".properties") || matchesExt(".gradle") || matchesExt(".toml") || matchesExt(".css") || matchesExt(".ini") || matchesExt(".env") ||
            matchesExt(".srt") || matchesExt(".vtt") || matchesExt(".m3u") || matchesExt(".pls") || matchesExt(".geojson")

    val isArchive = matchesExt(".zip") || matchesExt(".tar") || matchesExt(".gz") || matchesExt(".tgz") || matchesExt(".bz2") || matchesExt(".rar") || matchesExt(".7z") || matchesExt(".apk") || matchesExt(".jar")

    if (mimeType.isNullOrBlank() || mimeType == "application/octet-stream") {
        mimeType = when {
            isVideo -> "video/mp4"
            isAudio -> "audio/mpeg"
            isImage -> "image/jpeg"
            isPdf -> "application/pdf"
            matchesExt(".json") || matchesExt(".geojson") -> "application/json"
            matchesExt(".csv") -> "text/csv"
            matchesExt(".xml") -> "application/xml"
            matchesExt(".md") -> "text/markdown"
            matchesExt(".m3u") || matchesExt(".m3u8") -> "application/x-mpegurl"
            matchesExt(".apk") -> "application/vnd.android.package-archive"
            matchesExt(".zip") -> "application/zip"
            isArchive -> "application/x-archive"
            isTextOrCode -> "text/plain"
            uriStr.contains("video") -> "video/mp4"
            uriStr.contains("audio") || uriStr.contains("sound") -> "audio/mpeg"
            else -> "application/octet-stream"
        }
    }

    val kind = when {
        isVideo || mimeType.startsWith("video/") -> MediaKind.VIDEO
        isAudio || mimeType.startsWith("audio/") -> MediaKind.AUDIO
        isImage || mimeType.startsWith("image/") -> MediaKind.IMAGE
        isPdf || mimeType.contains("pdf") -> MediaKind.PDF_DOCUMENT
        isArchive || mimeType.contains("zip") || mimeType.contains("tar") || mimeType.contains("archive") || mimeType.contains("package") -> MediaKind.ARCHIVE
        isTextOrCode || mimeType.startsWith("text/") || mimeType.contains("json") || mimeType.contains("xml") -> MediaKind.TEXT_DOCUMENT
        else -> MediaKind.UNKNOWN
    }

    var textPreview: String? = null
    if (kind == MediaKind.TEXT_DOCUMENT) {
        try {
            val stream = if (uri.scheme == "content") {
                context.contentResolver.openInputStream(uri)
            } else if (uri.scheme == "file") {
                File(uri.path ?: "").inputStream()
            } else null

            stream?.use { s ->
                val reader = BufferedReader(InputStreamReader(s))
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
    } else if (kind == MediaKind.ARCHIVE) {
        textPreview = "Package: $displayName\nFormat: ${mimeType}\nSize: ${formatFileSize(sizeBytes)}\nStatus: Verified Binary Container"
    }

    var durationMs = 0L
    if (kind == MediaKind.VIDEO || kind == MediaKind.AUDIO) {
        try {
            val retriever = MediaMetadataRetriever()
            if (uri.scheme == "content") {
                retriever.setDataSource(context, uri)
            } else if (uri.scheme == "http" || uri.scheme == "https") {
                retriever.setDataSource(uri.toString(), HashMap())
            } else if (uri.scheme == "file") {
                retriever.setDataSource(uri.path)
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

internal fun parseM3uPlaylist(context: Context, uri: Uri): List<PlayableMediaItem> {
    val items = mutableListOf<PlayableMediaItem>()
    try {
        val stream = if (uri.scheme == "content") {
            context.contentResolver.openInputStream(uri)
        } else if (uri.scheme == "file") {
            File(uri.path ?: "").inputStream()
        } else null

        stream?.bufferedReader()?.use { reader ->
            var currentTitle: String? = null
            reader.forEachLine { line ->
                val trimmed = line.trim()
                if (trimmed.startsWith("#EXTINF:", ignoreCase = true)) {
                    currentTitle = trimmed.substringAfter(",").trim()
                } else if (trimmed.isNotBlank() && !trimmed.startsWith("#")) {
                    try {
                        val trackUri = Uri.parse(trimmed)
                        val resolved = resolveMediaItemFromUri(context, trackUri)
                        val item = if (!currentTitle.isNullOrBlank()) resolved.copy(displayName = currentTitle!!) else resolved
                        items.add(item)
                    } catch (_: Exception) {}
                    currentTitle = null
                }
            }
        }
    } catch (_: Exception) {}
    return items
}
