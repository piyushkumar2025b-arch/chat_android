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
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.data.remote.AiService
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarkdownStudioScreen(
    initialContent: String? = null,
    initialFileName: String? = null,
    onBack: () -> Unit,
    onSendMarkdownToChat: (String) -> Unit = {},
    showTopBar: Boolean = true
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    val defaultDoc = """
        # Notes & Writing
        
        Welcome to **OmniChat Writing Pad**.
        
        ## Highlights
        - Clean, distraction-free writing
        - Live formatted preview
        - Send drafts straight to chat
        
        > "Simplicity is about subtracting the obvious and adding the meaningful."
        
        ### Code Snippet
        ```kotlin
        fun greet(name: String) = "Hello, ${'$'}name!"
        ```
    """.trimIndent()

    var markdownText by remember { mutableStateOf(initialContent ?: defaultDoc) }
    var fileName by remember { mutableStateOf(initialFileName ?: "Notes.md") }
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Editor, 1 = Preview
    var isAiRewriting by remember { mutableStateOf(false) }

    LaunchedEffect(initialContent, initialFileName) {
        if (!initialContent.isNullOrBlank()) {
            markdownText = initialContent
        }
        if (!initialFileName.isNullOrBlank()) {
            fileName = initialFileName
        }
    }

    val copyMdAction = {
        clipboardManager.setText(AnnotatedString(markdownText))
        Toast.makeText(context, "Text copied!", Toast.LENGTH_SHORT).show()
    }
    val sendMdAction = {
        onSendMarkdownToChat(markdownText)
        Toast.makeText(context, "Sent to chat!", Toast.LENGTH_SHORT).show()
        if (showTopBar) onBack()
    }

    @Composable
    fun MarkdownEditorBody(modifier: Modifier = Modifier) {
        Column(modifier = modifier.fillMaxSize()) {
            if (!showTopBar) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Text(
                        text = fileName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(onClick = copyMdAction) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy Text")
                        }
                        IconButton(
                            onClick = sendMdAction,
                            modifier = Modifier.testTag("md_studio_send_chat")
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
                    AssistChip(
                        onClick = {
                            if (markdownText.isBlank() || isAiRewriting) return@AssistChip
                            coroutineScope.launch {
                                isAiRewriting = true
                                val res = AiService.quickAssist(
                                    taskPrompt = "Polish the grammar, clarity, and structure of this Markdown document while keeping its meaning intact:\n\n$markdownText",
                                    systemInstruction = "Return only the improved Markdown text."
                                )
                                isAiRewriting = false
                                res.onSuccess { markdownText = it }
                                    .onFailure { err ->
                                        Toast.makeText(context, err.localizedMessage ?: "AI Polish failed", Toast.LENGTH_SHORT).show()
                                    }
                            }
                        },
                        enabled = !isAiRewriting && markdownText.isNotBlank(),
                        leadingIcon = {
                            if (isAiRewriting) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                            }
                        },
                        label = { Text("AI Polish") },
                        modifier = Modifier.testTag("md_ai_polish_chip")
                    )
                    AssistChip(
                        onClick = {
                            if (markdownText.isBlank() || isAiRewriting) return@AssistChip
                            coroutineScope.launch {
                                isAiRewriting = true
                                val res = AiService.quickAssist(
                                    taskPrompt = "Make this Markdown document shorter and easier to scan while keeping the key points:\n\n$markdownText",
                                    systemInstruction = "Return only the shortened Markdown text."
                                )
                                isAiRewriting = false
                                res.onSuccess { markdownText = it }
                                    .onFailure { err ->
                                        Toast.makeText(context, err.localizedMessage ?: "AI Shorten failed", Toast.LENGTH_SHORT).show()
                                    }
                            }
                        },
                        enabled = !isAiRewriting && markdownText.isNotBlank(),
                        label = { Text("Make Shorter") },
                        modifier = Modifier.testTag("md_ai_shorten_chip")
                    )
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

    if (showTopBar) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(fileName, fontWeight = FontWeight.SemiBold) },
                    navigationIcon = {
                        IconButton(onClick = onBack, modifier = Modifier.testTag("md_studio_back")) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = copyMdAction) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy Text")
                        }
                        IconButton(
                            onClick = sendMdAction,
                            modifier = Modifier.testTag("md_studio_send_chat")
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
            MarkdownEditorBody(modifier = Modifier.padding(innerPadding))
        }
    } else {
        MarkdownEditorBody()
    }
}
