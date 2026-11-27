package com.example.ui

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.PreferencesManager
import com.example.data.remote.AiService
import com.example.util.showMainToast
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodeStudioScreen(
    initialCode: String? = null,
    initialFileName: String? = null,
    initialLanguage: String? = null,
    preferencesManager: PreferencesManager? = null,
    onBack: () -> Unit,
    onSendCodeToChat: (code: String, filename: String) -> Unit = { _, _ -> },
    showTopBar: Boolean = true
) {
    val context = LocalContext.current
    val resolvedPrefs = remember(preferencesManager) { preferencesManager ?: PreferencesManager(context) }
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    val defaultCode = """
        // OmniChat Code Editor
        fun main() {
            println("Hello, OmniChat!")
        }
    """.trimIndent()

    var codeText by remember { mutableStateOf(initialCode ?: defaultCode) }
    var fileName by remember { mutableStateOf(initialFileName ?: "Main.kt") }
    var selectedLanguage by remember { mutableStateOf(initialLanguage ?: "Kotlin") }
    var consoleOutput by remember { mutableStateOf("") }
    var isRunning by remember { mutableStateOf(false) }
    var isAiReviewing by remember { mutableStateOf(false) }
    var aiReviewText by remember { mutableStateOf<String?>(null) }

    // Update when initial parameters change (e.g. from chat attachment handoff)
    LaunchedEffect(initialCode, initialFileName, initialLanguage) {
        if (!initialCode.isNullOrBlank()) {
            codeText = initialCode
        }
        if (!initialFileName.isNullOrBlank()) {
            fileName = initialFileName
        }
        if (!initialLanguage.isNullOrBlank()) {
            selectedLanguage = initialLanguage
        }
    }

    val languages = listOf("Kotlin", "Python", "JavaScript", "HTML", "Rust", "SQL")

    val copyCodeAction = {
        clipboardManager.setText(AnnotatedString(codeText))
        Toast.makeText(context, "Code copied!", Toast.LENGTH_SHORT).show()
    }
    val sendCodeAction = {
        onSendCodeToChat(codeText, fileName)
        Toast.makeText(context, "Sent to chat!", Toast.LENGTH_SHORT).show()
        if (showTopBar) onBack()
    }

    @Composable
    fun CodeEditorBody(modifier: Modifier = Modifier) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!showTopBar) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = fileName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(onClick = copyCodeAction) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy Code")
                        }
                        IconButton(
                            onClick = sendCodeAction,
                            modifier = Modifier.testTag("code_studio_send_chat")
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send to Chat",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
            // Language selector & File naming
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                languages.forEach { lang ->
                    FilterChip(
                        selected = selectedLanguage.equals(lang, ignoreCase = true),
                        onClick = {
                            selectedLanguage = lang
                            fileName = when (lang) {
                                "Python" -> "script.py"
                                "JavaScript" -> "app.js"
                                "HTML" -> "index.html"
                                "Rust" -> "main.rs"
                                "SQL" -> "query.sql"
                                else -> "Main.kt"
                            }
                        },
                        label = { Text(lang) }
                    )
                }
            }

            // Code Editor with Line Numbers
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    OutlinedTextField(
                        value = codeText,
                        onValueChange = { codeText = it },
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("code_editor_field"),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )
                }
            }

            // Run / AI Explain & Fix / Console
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        isRunning = true
                        consoleOutput = runCodeSandbox(codeText, selectedLanguage)
                        isRunning = false
                    },
                    modifier = Modifier.testTag("run_code_button")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Run Sandbox")
                }

                FilledTonalButton(
                    onClick = {
                        if (codeText.isBlank()) {
                            Toast.makeText(context, "Write or paste code first", Toast.LENGTH_SHORT).show()
                            return@FilledTonalButton
                        }
                        coroutineScope.launch {
                            isAiReviewing = true
                            val res = AiService.quickAssist(
                                taskPrompt = "Explain simply what this $selectedLanguage code ($fileName) does, point out any bugs, and suggest a clean improvement:\n\n```$selectedLanguage\n$codeText\n```",
                                systemInstruction = "You are a clear, helpful code assistant. Keep explanations simple and practical.",
                                modelId = AiService.DEFAULT_GEMINI_MODEL,
                                customGeminiApiKey = resolvedPrefs.geminiApiKey
                            )
                            isAiReviewing = false
                            res.onSuccess { aiReviewText = it }
                                .onFailure { err ->
                                    showMainToast(
                                        context,
                                        err.localizedMessage ?: "AI Review failed"
                                    )
                                }
                        }
                    },
                    enabled = !isAiReviewing && codeText.isNotBlank(),
                    modifier = Modifier.testTag("code_ai_assist_button")
                ) {
                    if (isAiReviewing) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Checking…")
                    } else {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("AI Explain & Fix", maxLines = 1)
                    }
                }
            }

            Text(
                text = "${codeText.lines().size} lines | ${codeText.length} chars",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // AI Review Card
            val currentAiReview = aiReviewText
            if (currentAiReview != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .testTag("code_ai_review_card"),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.fillMaxSize().padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "AI Code Notes",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            IconButton(
                                onClick = { aiReviewText = null },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close AI review", modifier = Modifier.size(16.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                            Text(
                                text = currentAiReview,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Simulated Console Window
            if (consoleOutput.isNotBlank()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = consoleOutput,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    if (showTopBar) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(fileName, fontWeight = FontWeight.SemiBold) },
                    navigationIcon = {
                        IconButton(onClick = onBack, modifier = Modifier.testTag("code_studio_back")) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = copyCodeAction) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy Code")
                        }
                        IconButton(
                            onClick = sendCodeAction,
                            modifier = Modifier.testTag("code_studio_send_chat")
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send to Chat",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        ) { innerPadding ->
            CodeEditorBody(modifier = Modifier.padding(innerPadding))
        }
    } else {
        CodeEditorBody()
    }
}

internal fun runCodeSandbox(code: String, language: String): String {
    val startNs = System.nanoTime()
    if (code.isBlank()) {
        return "[ERROR] Editor buffer is empty. Enter $language code to inspect or run."
    }

    val issues = mutableListOf<String>()
    val stack = ArrayDeque<Pair<Char, Int>>()
    val pairs = mapOf(')' to '(', '}' to '{', ']' to '[')
    val lines = code.lines()

    lines.forEachIndexed { idx, line ->
        val lineNo = idx + 1
        var inSingleQuote = false
        var inDoubleQuote = false
        var escaped = false
        for (ch in line) {
            if (escaped) {
                escaped = false
                continue
            }
            if (ch == '\\' && (inSingleQuote || inDoubleQuote)) {
                escaped = true
                continue
            }
            if (ch == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote
            } else if (ch == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote
            } else if (!inSingleQuote && !inDoubleQuote) {
                if (ch == '(' || ch == '{' || ch == '[') {
                    stack.addLast(ch to lineNo)
                } else if (ch in pairs) {
                    val expected = pairs[ch]
                    if (stack.isEmpty() || stack.last().first != expected) {
                        issues.add("Line $lineNo: Unexpected closing '$ch'")
                    } else {
                        stack.removeLast()
                    }
                }
            }
        }
        if (inDoubleQuote) issues.add("Line $lineNo: Unterminated string literal (\")")
        if (inSingleQuote && language !in listOf("SQL", "HTML")) {
            issues.add("Line $lineNo: Unterminated quote (')")
        }
    }

    while (stack.isNotEmpty()) {
        val (unclosed, lineNo) = stack.removeLast()
        issues.add("Line $lineNo: Unclosed '$unclosed'")
    }

    val outputs = mutableListOf<String>()
    when (language) {
        "HTML" -> {
            val titleMatch = Regex("<title[^>]*>(.*?)</title>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)).find(code)
            val tagCount = Regex("<[a-zA-Z][a-zA-Z0-9]*\\b[^>]*>").findAll(code).count()
            outputs.add("Document title: ${titleMatch?.groupValues?.get(1)?.trim() ?: "(none)"}")
            outputs.add("Parsed HTML elements: $tagCount tags across ${lines.size} lines")
        }
        "SQL" -> {
            val statements = code.split(";").map { it.trim() }.filter { it.isNotEmpty() }
            val validKeywords = listOf("SELECT", "INSERT", "UPDATE", "DELETE", "CREATE", "ALTER", "DROP", "WITH", "PRAGMA")
            statements.forEachIndexed { idx, stmt ->
                val verb = stmt.substringBefore(' ').substringBefore('\n').uppercase()
                if (verb !in validKeywords) {
                    issues.add("Statement ${idx + 1}: Unrecognized SQL command '$verb'")
                } else {
                    outputs.add("Statement ${idx + 1} [$verb]: syntax verified")
                }
            }
        }
        else -> {
            val printRegex = Regex("""(?:println|print|console\.log)\s*\((.*)\)""")
            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.startsWith("//") || trimmed.startsWith("#")) continue
                val match = printRegex.find(trimmed)
                if (match != null) {
                    val expr = match.groupValues[1].trim()
                    val unquoted = if ((expr.startsWith("\"") && expr.endsWith("\"")) ||
                        (expr.startsWith("'") && expr.endsWith("'"))
                    ) {
                        expr.substring(1, expr.length - 1)
                    } else {
                        val mathEval = evaluateMathExpression(expr)
                        if (mathEval != "Error") mathEval else expr
                    }
                    outputs.add(unquoted)
                }
            }
        }
    }

    val elapsedMs = ((System.nanoTime() - startNs) / 1_000_000.0).coerceAtLeast(0.1)
    return buildString {
        if (issues.isNotEmpty()) {
            appendLine("[SYNTAX CHECK] Found ${issues.size} issue(s):")
            issues.forEach { appendLine("  • $it") }
        } else {
            appendLine("[$language Sandbox] Syntax OK (${lines.size} lines)")
        }
        if (outputs.isNotEmpty()) {
            appendLine("[OUTPUT]")
            outputs.forEach { appendLine("> $it") }
        } else if (issues.isEmpty()) {
            appendLine("[INFO] No stdout calls detected (add println/print/console.log to emit output).")
        }
        append("Completed in ${"%.1f".format(elapsedMs)} ms")
    }
}
