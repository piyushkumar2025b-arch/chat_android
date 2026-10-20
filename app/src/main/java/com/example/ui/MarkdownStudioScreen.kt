package com.example.ui

import android.widget.Toast
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
fun MarkdownStudioScreen(
    initialContent: String? = null,
    initialFileName: String? = null,
    onBack: () -> Unit,
    onSendMarkdownToChat: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val defaultDoc = """
        # Project Documentation
        
        Welcome to the **OmniChat Markdown Studio**!
        
        ## Features
        - Multi-provider AI reasoning (Gemini, Groq, OpenRouter)
        - Rich live markdown preview
        - Productivity suite with Room SQLite database
        
        > "Intelligence is the ability to adapt to change." - Stephen Hawking
        
        ### Quick Code Sample
        ```kotlin
        val omni = OmniChatEngine.init()
        omni.start()
        ```
    """.trimIndent()

    var markdownText by remember { mutableStateOf(initialContent ?: defaultDoc) }
    var fileName by remember { mutableStateOf(initialFileName ?: "Notes.md") }
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Editor, 1 = Preview

    // FIX BUG-05: Update when initial parameters change from chat attachment handoff
    LaunchedEffect(initialContent, initialFileName) {
        if (!initialContent.isNullOrBlank()) {
            markdownText = initialContent
        }
        if (!initialFileName.isNullOrBlank()) {
            fileName = initialFileName
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(fileName, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("md_studio_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(markdownText))
                            Toast.makeText(context, "Markdown copied to clipboard!", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy Markdown")
                    }

                    IconButton(
                        onClick = {
                            onSendMarkdownToChat(markdownText)
                            Toast.makeText(context, "Sent to AI Chat!", Toast.LENGTH_SHORT).show()
                            onBack()
                        },
                        modifier = Modifier.testTag("md_studio_send_chat")
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
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Editor") },
                    icon = { Icon(Icons.Default.Edit, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Live Preview") },
                    icon = { Icon(Icons.Default.Preview, contentDescription = null) }
                )
            }

            if (selectedTab == 0) {
                // Toolbar Helpers
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AssistChip(onClick = { markdownText += "\n# " }, label = { Text("H1") })
                    AssistChip(onClick = { markdownText += "\n## " }, label = { Text("H2") })
                    AssistChip(onClick = { markdownText += "**bold**" }, label = { Text("Bold") })
                    AssistChip(onClick = { markdownText += "*italic*" }, label = { Text("Italic") })
                    AssistChip(onClick = { markdownText += "\n- " }, label = { Text("List") })
                    AssistChip(onClick = { markdownText += "\n> " }, label = { Text("Quote") })
                    AssistChip(onClick = { markdownText += "\n```kotlin\n\n```" }, label = { Text("Code") })
                }

                OutlinedTextField(
                    value = markdownText,
                    onValueChange = { markdownText = it },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .testTag("markdown_editor_field"),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp
                    )
                )
            } else {
                // Live Markdown Formatted Preview
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    markdownText.lines().forEach { line ->
                        when {
                            line.startsWith("# ") -> {
                                Text(
                                    text = line.removePrefix("# "),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            line.startsWith("## ") -> {
                                Text(
                                    text = line.removePrefix("## "),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            line.startsWith("### ") -> {
                                Text(
                                    text = line.removePrefix("### "),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            line.startsWith("> ") -> {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = line.removePrefix("> "),
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }
                            line.startsWith("- ") -> {
                                Row {
                                    Text("• ", fontWeight = FontWeight.Bold)
                                    Text(line.removePrefix("- "), style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                            line.startsWith("```") -> {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = line,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                            }
                            else -> {
                                if (line.isNotBlank()) {
                                    Text(text = line, style = MaterialTheme.typography.bodyMedium)
                                } else {
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
