package com.example.ui

import android.content.ClipboardManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.PreferencesManager
import com.example.data.remote.AiService
import com.example.util.showMainToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.ceil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadAloudScreen(
    initialText: String = "",
    preferencesManager: PreferencesManager? = null,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val resolvedPrefs = remember(preferencesManager) { preferencesManager ?: PreferencesManager(context) }
    val scope = rememberCoroutineScope()
    var speechText by remember {
        mutableStateOf(
            initialText.ifBlank {
                "Welcome to OmniChat. I can read code documentation, summaries, notes, and assistant replies aloud to you."
            }
        )
    }
    var speechRate by remember { mutableFloatStateOf(1.0f) }
    var pitch by remember { mutableFloatStateOf(1.0f) }
    var isSpeaking by remember { mutableStateOf(false) }
    var ttsReady by remember { mutableStateOf(false) }
    var isSimplifying by remember { mutableStateOf(false) }

    val wordCount = remember(speechText) {
        speechText.trim().split(Regex("\\s+")).count { it.isNotBlank() }
    }
    val estMinutes = remember(wordCount, speechRate) {
        if (wordCount == 0) 0 else ceil(wordCount / (150.0 * speechRate)).toInt().coerceAtLeast(1)
    }

    var ttsInstance by remember { mutableStateOf<TextToSpeech?>(null) }

    DisposableEffect(Unit) {
        val mainHandler = Handler(Looper.getMainLooper())
        var engineRef: TextToSpeech? = null
        val tts = TextToSpeech(context) { status ->
            val ok = status == TextToSpeech.SUCCESS
            if (ok) {
                engineRef?.language = Locale.getDefault()
            }
            mainHandler.post { ttsReady = ok }
        }
        engineRef = tts
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {
                mainHandler.post { isSpeaking = false }
            }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                mainHandler.post { isSpeaking = false }
            }
            override fun onError(utteranceId: String?, errorCode: Int) {
                mainHandler.post { isSpeaking = false }
            }
            override fun onStop(utteranceId: String?, interrupted: Boolean) {
                mainHandler.post { isSpeaking = false }
            }
        })
        ttsInstance = tts

        onDispose {
            tts.stop()
            tts.shutdown()
            ttsInstance = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Read Aloud", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("tts_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                Icons.Default.RecordVoiceOver,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(30.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Device Voice Reader",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "$wordCount words · ~${estMinutes}m listen",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.testTag("tts_stats_label")
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AssistChip(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            val pasted = clipboard?.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString().orEmpty()
                            if (pasted.isNotBlank()) {
                                speechText = pasted
                                Toast.makeText(context, "Pasted from clipboard", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT).show()
                            }
                        },
                        leadingIcon = { Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = { Text("Paste") },
                        modifier = Modifier.testTag("tts_paste_button")
                    )
                    AssistChip(
                        onClick = { speechText = "" },
                        leadingIcon = { Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = { Text("Clear") },
                        modifier = Modifier.testTag("tts_clear_button")
                    )
                    AssistChip(
                        onClick = {
                            speechRate = 1.0f
                            pitch = 1.0f
                        },
                        leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = { Text("Reset 1.0x") },
                        modifier = Modifier.testTag("tts_reset_voice_button")
                    )
                }

                OutlinedTextField(
                    value = speechText,
                    onValueChange = { speechText = it },
                    label = { Text("Text to read aloud") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tts_text_input"),
                    minLines = 4,
                    maxLines = 8
                )

                OutlinedButton(
                    onClick = {
                        val cleaned = speechText.trim()
                        if (cleaned.isBlank()) return@OutlinedButton
                        isSimplifying = true
                        scope.launch {
                            val res = AiService.quickAssist(
                                taskPrompt = "Rewrite this text so it sounds natural, clear, and easy to listen to when read aloud (remove markdown symbols, URLs, or clutter):\n\n$cleaned",
                                systemInstruction = "Return only clean spoken-friendly prose.",
                                customGeminiApiKey = resolvedPrefs.geminiApiKey
                            )
                            isSimplifying = false
                            res.onSuccess { speechText = it.trim() }
                                .onFailure { err ->
                                    showMainToast(context, err.localizedMessage ?: "Could not simplify text")
                                }
                        }
                    },
                    enabled = !isSimplifying && speechText.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tts_ai_simplify_button")
                ) {
                    if (isSimplifying) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Preparing Script…")
                    } else {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Clean Up for Listening with AI")
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Speech Rate (${"%.1f".format(Locale.US, speechRate)}x)", style = MaterialTheme.typography.labelMedium)
                        Slider(
                            value = speechRate,
                            onValueChange = { speechRate = it },
                            valueRange = 0.5f..2.0f
                        )

                        Text("Voice Pitch (${"%.1f".format(Locale.US, pitch)})", style = MaterialTheme.typography.labelMedium)
                        Slider(
                            value = pitch,
                            onValueChange = { pitch = it },
                            valueRange = 0.5f..1.5f
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        if (!ttsReady) {
                            Toast.makeText(context, "Voice engine is still initializing…", Toast.LENGTH_SHORT).show()
                        } else if (speechText.isBlank()) {
                            Toast.makeText(context, "Enter text to read aloud.", Toast.LENGTH_SHORT).show()
                        } else {
                            ttsInstance?.setSpeechRate(speechRate)
                            ttsInstance?.setPitch(pitch)
                            ttsInstance?.speak(speechText, TextToSpeech.QUEUE_FLUSH, null, "omni_tts")
                            isSpeaking = true
                            Toast.makeText(context, "Speaking...", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Speak")
                }

                Button(
                    onClick = {
                        ttsInstance?.stop()
                        isSpeaking = false
                        Toast.makeText(context, "Stopped", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Stop")
                }
            }
        }
    }
}
