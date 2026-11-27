package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.PreferencesManager
import com.example.data.remote.AiService
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.UUID

private data class LanguageOption(
    val name: String,
    val locale: Locale
)

private val SUPPORTED_LANGUAGES = listOf(
    LanguageOption("English", Locale.US),
    LanguageOption("Spanish", Locale.forLanguageTag("es-ES")),
    LanguageOption("French", Locale.FRANCE),
    LanguageOption("German", Locale.GERMANY),
    LanguageOption("Japanese", Locale.JAPAN),
    LanguageOption("Korean", Locale.KOREA),
    LanguageOption("Chinese", Locale.SIMPLIFIED_CHINESE),
    LanguageOption("Hindi", Locale.forLanguageTag("hi-IN")),
    LanguageOption("Arabic", Locale.forLanguageTag("ar-SA")),
    LanguageOption("Portuguese", Locale.forLanguageTag("pt-BR")),
    LanguageOption("Italian", Locale.ITALY),
    LanguageOption("Turkish", Locale.forLanguageTag("tr-TR")),
    LanguageOption("Indonesian", Locale.forLanguageTag("id-ID")),
    LanguageOption("Vietnamese", Locale.forLanguageTag("vi-VN")),
    LanguageOption("Thai", Locale.forLanguageTag("th-TH"))
)

private val QUICK_TRAVEL_PHRASES = listOf(
    "Hello, how are you?",
    "Where is the nearest train station?",
    "Can I have the bill, please?",
    "How much does this cost?",
    "I have a food allergy.",
    "Thank you very much!"
)

private data class SavedTranslation(
    val id: String = UUID.randomUUID().toString(),
    val sourceText: String,
    val translatedText: String,
    val targetLanguage: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslatorScreen(
    preferencesManager: PreferencesManager,
    onBack: () -> Unit,
    onSendToChat: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var sourceLanguage by remember { mutableStateOf("Auto-Detect") }
    var targetLanguage by remember { mutableStateOf("Spanish") }
    var selectedTone by remember { mutableStateOf("Natural") }
    var sourceText by remember { mutableStateOf("") }
    var translatedText by remember { mutableStateOf("") }
    var isTranslating by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showSourceMenu by remember { mutableStateOf(false) }
    var showTargetMenu by remember { mutableStateOf(false) }

    var savedItems by remember {
        mutableStateOf(parseSavedTranslations(preferencesManager.savedTranslationsJson))
    }

    var ttsReady by remember { mutableStateOf(false) }
    val tts = remember {
        var engine: TextToSpeech? = null
        engine = TextToSpeech(context) { status ->
            ttsReady = status == TextToSpeech.SUCCESS
        }
        engine
    }

    DisposableEffect(Unit) {
        onDispose {
            runCatching {
                tts.stop()
                tts.shutdown()
            }
        }
    }

    fun persistFavorites(list: List<SavedTranslation>) {
        savedItems = list
        preferencesManager.savedTranslationsJson = serializeSavedTranslations(list)
    }

    val photoTranslateLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        errorMessage = null
        isTranslating = true
        scope.launch {
            val bitmap = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                runCatching {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
                    } else {
                        @Suppress("DEPRECATION")
                        MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                    }
                }.getOrNull()
            }
            if (bitmap == null) {
                errorMessage = "Could not load selected photo."
                isTranslating = false
                return@launch
            }
            AiService.translateImageText(
                bitmap = bitmap,
                targetLanguage = targetLanguage,
                tone = selectedTone,
                customGeminiApiKey = preferencesManager.geminiApiKey
            ).onSuccess { result ->
                val origLine = result.lines().firstOrNull { it.startsWith("Original:", ignoreCase = true) }
                    ?.substringAfter(":")?.trim()
                val transLine = result.lines().dropWhile { !it.startsWith("Translation:", ignoreCase = true) }
                    .joinToString("\n")
                    .substringAfter(":")
                    .trim()
                if (!origLine.isNullOrBlank()) {
                    sourceText = origLine
                }
                translatedText = transLine.ifBlank { result.trim() }
                isTranslating = false
            }.onFailure { err ->
                errorMessage = err.localizedMessage ?: "Photo translation failed."
                isTranslating = false
            }
        }
    }

    fun runTranslation(textToTranslate: String = sourceText) {
        val cleaned = textToTranslate.trim()
        if (cleaned.isBlank()) {
            Toast.makeText(context, "Enter text to translate", Toast.LENGTH_SHORT).show()
            return
        }
        sourceText = textToTranslate
        errorMessage = null
        isTranslating = true
        scope.launch {
            val fromPart = if (sourceLanguage == "Auto-Detect") {
                "Auto-detect the source language"
            } else {
                "Translate from $sourceLanguage"
            }
            val prompt = "$fromPart into $targetLanguage using a $selectedTone tone.\n" +
                "Return ONLY the translated text (and if helpful for non-Latin scripts like Japanese, Korean, Chinese, Arabic, Hindi, or Thai, include simple pronunciation in parentheses on a second line).\n\n" +
                cleaned
            AiService.quickAssist(
                taskPrompt = prompt,
                systemInstruction = "You are an accurate, natural translator. Return only the translation without extra commentary.",
                modelId = AiService.FAST_GEMINI_MODEL,
                temperature = 0.2f,
                customGeminiApiKey = preferencesManager.geminiApiKey
            ).onSuccess { result ->
                translatedText = result.trim()
                isTranslating = false
            }.onFailure { err ->
                errorMessage = err.localizedMessage ?: "Translation failed."
                isTranslating = false
            }
        }
    }

    val tones = listOf("Natural", "Casual", "Polite", "Business")
    val isCurrentSaved = translatedText.isNotBlank() && savedItems.any {
        it.translatedText == translatedText && it.targetLanguage == targetLanguage
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Translator", fontWeight = FontWeight.SemiBold) },
                    navigationIcon = {
                        IconButton(onClick = onBack, modifier = Modifier.testTag("translator_back")) {
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
            // Language selector bar
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = { showSourceMenu = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("translator_source_lang")
                            ) {
                                Text(sourceLanguage, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            DropdownMenu(
                                expanded = showSourceMenu,
                                onDismissRequest = { showSourceMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Auto-Detect") },
                                    onClick = {
                                        sourceLanguage = "Auto-Detect"
                                        showSourceMenu = false
                                    }
                                )
                                SUPPORTED_LANGUAGES.forEach { lang ->
                                    DropdownMenuItem(
                                        text = { Text(lang.name) },
                                        onClick = {
                                            sourceLanguage = lang.name
                                            showSourceMenu = false
                                        }
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = {
                                if (sourceLanguage != "Auto-Detect") {
                                    val oldTarget = targetLanguage
                                    targetLanguage = sourceLanguage
                                    sourceLanguage = oldTarget
                                    if (translatedText.isNotBlank()) {
                                        val oldResult = translatedText
                                        translatedText = sourceText
                                        sourceText = oldResult
                                    }
                                } else {
                                    Toast.makeText(context, "Pick a source language first to swap", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.testTag("translator_swap_button")
                        ) {
                            Icon(Icons.Default.SwapHoriz, contentDescription = "Swap languages")
                        }

                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = { showTargetMenu = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("translator_target_lang")
                            ) {
                                Text(targetLanguage, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            DropdownMenu(
                                expanded = showTargetMenu,
                                onDismissRequest = { showTargetMenu = false }
                            ) {
                                SUPPORTED_LANGUAGES.forEach { lang ->
                                    DropdownMenuItem(
                                        text = { Text(lang.name) },
                                        onClick = {
                                            targetLanguage = lang.name
                                            showTargetMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Tone selector chips
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tones.forEach { tone ->
                        FilterChip(
                            selected = selectedTone == tone,
                            onClick = { selectedTone = tone },
                            label = { Text(tone) },
                            modifier = Modifier.testTag("translator_tone_$tone")
                        )
                    }
                }
            }

            // Input card
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
                        OutlinedTextField(
                            value = sourceText,
                            onValueChange = { sourceText = it },
                            label = { Text("Text to translate") },
                            placeholder = { Text("Type or paste any sentence…") },
                            minLines = 3,
                            maxLines = 6,
                            trailingIcon = {
                                if (sourceText.isNotEmpty()) {
                                    IconButton(onClick = {
                                        sourceText = ""
                                        translatedText = ""
                                        errorMessage = null
                                    }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear text")
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("translator_input")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { runTranslation() },
                                enabled = !isTranslating && sourceText.isNotBlank(),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("translator_run_button")
                            ) {
                                if (isTranslating) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Translating…")
                                } else {
                                    Icon(Icons.Default.Translate, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Translate", maxLines = 1)
                                }
                            }
                            OutlinedButton(
                                onClick = {
                                    photoTranslateLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                enabled = !isTranslating,
                                modifier = Modifier.testTag("translator_photo_button")
                            ) {
                                Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Photo", maxLines = 1)
                            }
                        }
                    }
                }
            }

            // Error banner if any
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

            // Output card
            if (translatedText.isNotBlank()) {
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                        modifier = Modifier.testTag("translator_result_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "$targetLanguage ($selectedTone)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = translatedText,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(
                                        onClick = {
                                            if (ttsReady) {
                                                val targetLocale = SUPPORTED_LANGUAGES
                                                    .firstOrNull { it.name == targetLanguage }
                                                    ?.locale ?: Locale.US
                                                tts.language = targetLocale
                                                val mainLine = translatedText.lineSequence().firstOrNull().orEmpty()
                                                tts.speak(mainLine, TextToSpeech.QUEUE_FLUSH, null, "tr_${System.currentTimeMillis()}")
                                            } else {
                                                Toast.makeText(context, "Speech engine is warming up", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier.testTag("translator_speak_button")
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Listen")
                                    }
                                    IconButton(
                                        onClick = {
                                            val clip = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                            clip?.setPrimaryClip(ClipData.newPlainText("Translation", translatedText))
                                            Toast.makeText(context, "Copied translation", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.testTag("translator_copy_button")
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                                    }
                                    IconButton(
                                        onClick = {
                                            if (isCurrentSaved) {
                                                persistFavorites(
                                                    savedItems.filterNot {
                                                        it.translatedText == translatedText && it.targetLanguage == targetLanguage
                                                    }
                                                )
                                                Toast.makeText(context, "Removed from phrasebook", Toast.LENGTH_SHORT).show()
                                            } else {
                                                val entry = SavedTranslation(
                                                    sourceText = sourceText.trim(),
                                                    translatedText = translatedText,
                                                    targetLanguage = targetLanguage
                                                )
                                                persistFavorites((listOf(entry) + savedItems).take(30))
                                                Toast.makeText(context, "Saved to phrasebook", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier.testTag("translator_save_button")
                                    ) {
                                        Icon(
                                            if (isCurrentSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                            contentDescription = "Save phrase",
                                            tint = if (isCurrentSaved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = {
                                        onSendToChat("Translation ($targetLanguage):\n\"$sourceText\" -> \"$translatedText\"\nExplain nuances or extra examples.")
                                    },
                                    modifier = Modifier.testTag("translator_send_chat_button")
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Ask in Chat", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }

            // Quick phrasebook section
            item {
                Text(
                    text = "Quick Travel Phrases",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QUICK_TRAVEL_PHRASES.forEachIndexed { idx, phrase ->
                        FilterChip(
                            selected = sourceText == phrase,
                            onClick = { runTranslation(phrase) },
                            label = { Text(phrase, maxLines = 1) },
                            modifier = Modifier.testTag("translator_quick_phrase_$idx")
                        )
                    }
                }
            }

            if (savedItems.isNotEmpty()) {
                item {
                    Text(
                        text = "Saved Phrasebook (${savedItems.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
                items(savedItems, key = { it.id }) { item ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.clickable {
                            sourceText = item.sourceText
                            targetLanguage = item.targetLanguage
                            translatedText = item.translatedText
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = item.targetLanguage,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = item.translatedText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = item.sourceText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = {
                                    persistFavorites(savedItems.filterNot { it.id == item.id })
                                }
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete saved phrase",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

private fun parseSavedTranslations(raw: String): List<SavedTranslation> {
    return try {
        val arr = JSONArray(raw)
        buildList {
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val translated = obj.optString("translatedText")
                if (translated.isBlank()) continue
                add(
                    SavedTranslation(
                        id = obj.optString("id").ifBlank { UUID.randomUUID().toString() },
                        sourceText = obj.optString("sourceText", ""),
                        translatedText = translated,
                        targetLanguage = obj.optString("targetLanguage", "Spanish")
                    )
                )
            }
        }
    } catch (_: Exception) {
        emptyList()
    }
}

private fun serializeSavedTranslations(items: List<SavedTranslation>): String {
    return JSONArray().apply {
        items.forEach { item ->
            put(
                JSONObject().apply {
                    put("id", item.id)
                    put("sourceText", item.sourceText)
                    put("translatedText", item.translatedText)
                    put("targetLanguage", item.targetLanguage)
                }
            )
        }
    }.toString()
}
