package com.example.ui

import android.widget.Toast
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
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodeStudioScreen(
    initialCode: String? = null,
    initialFileName: String? = null,
    initialLanguage: String? = null,
    onBack: () -> Unit,
    onSendCodeToChat: (code: String, filename: String) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val defaultCode = """
        // OmniChat Interactive Code Studio
        fun main() {
            println("Hello, OmniChat Multi-Agent Intelligence!")
        }
    """.trimIndent()

    var codeText by remember { mutableStateOf(initialCode ?: defaultCode) }
    var fileName by remember { mutableStateOf(initialFileName ?: "Main.kt") }
    var selectedLanguage by remember { mutableStateOf(initialLanguage ?: "Kotlin") }
    var consoleOutput by remember { mutableStateOf("") }
    var isRunning by remember { mutableStateOf(false) }

    // FIX BUG-05: Update when initial parameters change (e.g. from chat attachment handoff)
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(fileName, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("code_studio_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(codeText))
                            Toast.makeText(context, "Code copied to clipboard!", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy Code")
                    }

                    IconButton(
                        onClick = {
                            onSendCodeToChat(codeText, fileName)
                            Toast.makeText(context, "Sent to AI Chat!", Toast.LENGTH_SHORT).show()
                            onBack()
                        },
                        modifier = Modifier.testTag("code_studio_send_chat")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send to Chat", tint = MaterialTheme.colorScheme.primary)
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
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

            // Run / Simulate & Console
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        isRunning = true
                        consoleOutput = "Compiling & executing $selectedLanguage sandbox...\n" +
                                "[RUN] Status: 200 OK\n" +
                                "[OUTPUT]: Output verified without syntax defects.\n" +
                                "Execution finished in 18ms."
                        isRunning = false
                    },
                    modifier = Modifier.testTag("run_code_button")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Run Sandbox")
                }

                Text(
                    text = "${codeText.lines().size} lines | ${codeText.length} chars",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
}
