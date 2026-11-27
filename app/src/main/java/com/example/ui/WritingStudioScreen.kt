package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
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
import com.example.data.local.NoteEntity
import com.example.data.local.PreferencesManager
import com.example.data.local.ProductivityDao
import com.example.data.remote.AiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

private enum class WritingMode(val label: String, val instruction: String) {
    FIX_GRAMMAR("Fix Grammar", "Correct spelling, grammar, and punctuation while keeping the original meaning and voice."),
    SHORTER("Make Shorter", "Make this concise and punchy without losing key facts."),
    EXPAND("Expand", "Expand with clear helpful details and smooth transitions."),
    FRIENDLY("Friendly", "Rewrite in a warm, friendly, conversational tone."),
    FORMAL("Formal", "Rewrite in a clear, polite, professional tone."),
    EMAIL_REPLY("Email Reply", "Draft a polite, ready-to-send email reply to this message."),
    BULLETS("Bullet Points", "Turn this text into crisp, easy-to-scan bullet points."),
    SUBJECT_LINES("Titles & Subjects", "Suggest 5 clear, catchy titles or email subject lines for this text, numbered 1 to 5.")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WritingStudioScreen(
    productivityDao: ProductivityDao,
    preferencesManager: PreferencesManager,
    onBack: () -> Unit,
    onSendToChat: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var draftText by remember { mutableStateOf("") }
    var resultText by remember { mutableStateOf("") }
    var selectedMode by remember { mutableStateOf(WritingMode.FIX_GRAMMAR) }
    var isProcessing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var ttsReady by remember { mutableStateOf(false) }
    val tts = remember {
        TextToSpeech(context) { status ->
            ttsReady = status == TextToSpeech.SUCCESS
        }.apply {
            language = Locale.getDefault()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            runCatching {
                tts.stop()
                tts.shutdown()
            }
        }
    }

    val wordCount = remember(draftText) {
        draftText.trim().split(Regex("\\s+")).count { it.isNotBlank() }
    }

    fun rewrite(mode: WritingMode = selectedMode) {
        val cleaned = draftText.trim()
        if (cleaned.isBlank()) {
            Toast.makeText(context, "Write or paste some text first", Toast.LENGTH_SHORT).show()
            return
        }
        selectedMode = mode
        errorMessage = null
        isProcessing = true
        scope.launch {
            AiService.quickAssist(
                taskPrompt = "${mode.instruction}\n\nReturn ONLY the rewritten text without any intro or quotes:\n\n$cleaned",
                systemInstruction = "You are a skilled editor. Return only the improved text.",
                modelId = AiService.DEFAULT_GEMINI_MODEL,
                temperature = 0.4f,
                customGeminiApiKey = preferencesManager.geminiApiKey
            ).onSuccess { output ->
                resultText = output.trim()
                isProcessing = false
            }.onFailure { err ->
                errorMessage = err.localizedMessage ?: "Could not rewrite text."
                isProcessing = false
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Writing Helper", fontWeight = FontWeight.SemiBold) },
                    navigationIcon = {
                        IconButton(onClick = onBack, modifier = Modifier.testTag("writing_back")) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    WritingMode.entries.forEach { mode ->
                        FilterChip(
                            selected = selectedMode == mode,
                            onClick = { selectedMode = mode },
                            label = { Text(mode.label) },
                            modifier = Modifier.testTag("writing_mode_${mode.name}")
                        )
                    }
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Your Draft", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(
                                "$wordCount words · ${draftText.length} chars",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        OutlinedTextField(
                            value = draftText,
                            onValueChange = { draftText = it },
                            placeholder = { Text("Paste an email, message, or paragraph to polish…") },
                            minLines = 5,
                            maxLines = 10,
                            trailingIcon = {
                                if (draftText.isNotEmpty()) {
                                    IconButton(onClick = { draftText = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear draft")
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("writing_draft_input")
                        )

                        Button(
                            onClick = { rewrite() },
                            enabled = !isProcessing && draftText.isNotBlank(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("writing_rewrite_button")
                        ) {
                            if (isProcessing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Polishing…")
                            } else {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Apply ${selectedMode.label}")
                            }
                        }
                    }
                }
            }

            if (!errorMessage.isNullOrBlank()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Text(
                            text = errorMessage.orEmpty(),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            if (resultText.isNotBlank()) {
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                        modifier = Modifier.testTag("writing_result_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Polished Result (${selectedMode.label})",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                AssistChip(
                                    onClick = {
                                        draftText = resultText
                                        Toast.makeText(context, "Moved result back to draft", Toast.LENGTH_SHORT).show()
                                    },
                                    label = { Text("Use as draft") },
                                    leadingIcon = { Icon(Icons.Default.SwapVert, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                )
                            }

                            Text(
                                text = resultText,
                                style = MaterialTheme.typography.bodyMedium
                            )

                            val resultWords = remember(resultText) {
                                resultText.trim().split(Regex("\\s+")).count { it.isNotBlank() }
                            }
                            Text(
                                text = "$resultWords words · ${resultText.length} chars",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(
                                        onClick = {
                                            val clip = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                            clip?.setPrimaryClip(ClipData.newPlainText("Polished Writing", resultText))
                                            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.testTag("writing_copy_button")
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                                    }
                                    IconButton(
                                        onClick = {
                                            if (ttsReady) {
                                                tts.speak(resultText, TextToSpeech.QUEUE_FLUSH, null, "wr_${System.currentTimeMillis()}")
                                            }
                                        }
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Read aloud")
                                    }
                                    IconButton(
                                        onClick = {
                                            scope.launch {
                                                val title = resultText.lineSequence().firstOrNull { it.isNotBlank() }?.take(44) ?: "Polished Note"
                                                withContext(Dispatchers.IO) {
                                                    productivityDao.insertNote(
                                                        NoteEntity(
                                                            title = title,
                                                            content = resultText,
                                                            tags = "writing",
                                                            updatedAt = System.currentTimeMillis()
                                                        )
                                                    )
                                                }
                                                Toast.makeText(context, "Saved to Notes", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier.testTag("writing_save_note_button")
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.NoteAdd, contentDescription = "Save to Notes")
                                    }
                                }
                                IconButton(
                                    onClick = {
                                        onSendToChat("Review this ${selectedMode.label.lowercase()} draft and suggest any improvements:\n\n$resultText")
                                    },
                                    modifier = Modifier.testTag("writing_send_chat_button")
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send to Chat", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}
