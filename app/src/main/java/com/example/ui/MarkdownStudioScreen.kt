package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.remote.MediaSaver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class MarkdownTemplate(
    val title: String,
    val icon: String,
    val content: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarkdownStudioScreen(
    initialContent: String? = null,
    onBack: (() -> Unit)? = null,
    onSendToChat: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var markdownText by remember {
        mutableStateOf(
            initialContent ?: getSampleMarkdownTemplate()
        )
    }

    // 0 = Rendered Preview, 1 = Raw Editor, 2 = Split View
    var selectedViewMode by remember { mutableIntStateOf(0) }
    var fileName by remember { mutableStateOf("document.md") }

    // Metrics
    val wordCount = remember(markdownText) {
        if (markdownText.isBlank()) 0 else markdownText.trim().split("\\s+".toRegex()).size
    }
    val charCount = remember(markdownText) { markdownText.length }
    val readTimeMinutes = remember(wordCount) { (wordCount / 200).coerceAtLeast(1) }

    // File picker launcher for .md files
    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val content = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    }
                    if (content != null) {
                        markdownText = content
                        fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "imported_file.md"
                        selectedViewMode = 0 // Switch to preview
                        Toast.makeText(context, "Loaded $fileName", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Error opening file: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val templates = listOf(
        MarkdownTemplate("Project Spec", "🚀", getProjectSpecTemplate()),
        MarkdownTemplate("README.md", "📦", getSampleMarkdownTemplate()),
        MarkdownTemplate("Task List", "✅", getTodoListTemplate()),
        MarkdownTemplate("AI Prompt Guide", "🧠", getPromptGuideTemplate())
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
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
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Markdown Hub & Viewer",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$fileName • $wordCount words • ~$readTimeMinutes min read",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Open File
                    IconButton(
                        onClick = {
                            openDocumentLauncher.launch(
                                arrayOf("text/markdown", "text/plain", "text/*", "*/*")
                            )
                        },
                        modifier = Modifier.testTag("md_open_file_btn")
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = "Open .md file")
                    }

                    // Copy raw text
                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Markdown", markdownText))
                            Toast.makeText(context, "Copied Markdown to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.testTag("md_copy_btn")
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy Markdown")
                    }

                    // Save / Download .md
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                MediaSaver.saveDocumentToDownloads(
                                    context = context,
                                    fileName = fileName,
                                    content = markdownText,
                                    mimeType = "text/markdown"
                                )
                            }
                        },
                        modifier = Modifier.testTag("md_save_btn")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "Save .md to device", tint = MaterialTheme.colorScheme.primary)
                    }

                    // Send to Chat
                    if (onSendToChat != null) {
                        IconButton(
                            onClick = {
                                val prompt = "Review, improve, and format this Markdown document:\n\n```markdown\n$markdownText\n```"
                                onSendToChat(prompt)
                            },
                            modifier = Modifier.testTag("md_send_chat_btn")
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = "Analyze with AI", tint = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }
            }
        }

        // View Mode Tabs: Preview, Editor, Split View
        TabRow(
            selectedTabIndex = selectedViewMode,
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.fillMaxWidth().testTag("md_view_tab_row")
        ) {
            Tab(
                selected = selectedViewMode == 0,
                onClick = { selectedViewMode = 0 },
                text = { Text("Rendered Preview") },
                icon = { Icon(Icons.Default.Visibility, contentDescription = null) }
            )
            Tab(
                selected = selectedViewMode == 1,
                onClick = { selectedViewMode = 1 },
                text = { Text("Markdown Editor") },
                icon = { Icon(Icons.Default.Edit, contentDescription = null) }
            )
            Tab(
                selected = selectedViewMode == 2,
                onClick = { selectedViewMode = 2 },
                text = { Text("Split View") },
                icon = { Icon(Icons.Default.Code, contentDescription = null) }
            )
        }

        // Quick Templates Bar
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            modifier = Modifier.fillMaxWidth()
        ) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item {
                    Text(
                        "Templates:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                items(templates) { t ->
                    FilterChip(
                        selected = false,
                        onClick = {
                            markdownText = t.content
                            fileName = "${t.title.lowercase().replace(" ", "_")}.md"
                            Toast.makeText(context, "Loaded ${t.title}", Toast.LENGTH_SHORT).show()
                        },
                        label = { Text("${t.icon} ${t.title}", style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                }
            }
        }

        // Editor Quick Actions Toolbar (visible in Editor or Split mode)
        if (selectedViewMode == 1 || selectedViewMode == 2) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MarkdownToolbarButton(label = "H1", icon = Icons.Default.Title) {
                        markdownText = "\n# New Heading\n$markdownText"
                    }
                    MarkdownToolbarButton(label = "H2") {
                        markdownText = "$markdownText\n\n## Subheading\n"
                    }
                    MarkdownToolbarButton(label = "Bold", icon = Icons.Default.FormatBold) {
                        markdownText = "$markdownText **bold text** "
                    }
                    MarkdownToolbarButton(label = "Italic", icon = Icons.Default.FormatItalic) {
                        markdownText = "$markdownText *italic text* "
                    }
                    MarkdownToolbarButton(label = "Code", icon = Icons.Default.Code) {
                        markdownText = "$markdownText `inline code` "
                    }
                    MarkdownToolbarButton(label = "Block") {
                        markdownText = "$markdownText\n\n```kotlin\nfun main() {\n    println(\"Hello\")\n}\n```\n"
                    }
                    MarkdownToolbarButton(label = "List", icon = Icons.AutoMirrored.Filled.FormatListBulleted) {
                        markdownText = "$markdownText\n\n- [ ] Task item 1\n- [ ] Task item 2\n"
                    }
                    MarkdownToolbarButton(label = "Quote", icon = Icons.Default.FormatQuote) {
                        markdownText = "$markdownText\n\n> Quoted important callout message\n"
                    }
                    MarkdownToolbarButton(label = "Table", icon = Icons.Default.TableChart) {
                        markdownText = "$markdownText\n\n| Item | Category | Status |\n|:---|:---|:---|\n| OmniChat | AI App | Active |\n| Engine | Kotlin | 100% |\n"
                    }
                    MarkdownToolbarButton(label = "Link", icon = Icons.Default.Link) {
                        markdownText = "$markdownText [OmniChat Link](https://google.com) "
                    }
                }
            }
        }

        // Content Area based on Selected Mode
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            when (selectedViewMode) {
                0 -> {
                    // Full Rendered Preview
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .testTag("md_preview_column")
                    ) {
                        item {
                            MarkdownText(
                                text = markdownText,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(64.dp))
                        }
                    }
                }

                1 -> {
                    // Full Raw Editor
                    OutlinedTextField(
                        value = markdownText,
                        onValueChange = { markdownText = it },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                            .testTag("md_editor_field"),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp
                        ),
                        placeholder = { Text("Write or paste your Markdown here...") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )
                }

                2 -> {
                    // Split View (Top = Editor, Bottom = Live Preview)
                    Column(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(8.dp)
                        ) {
                            OutlinedTextField(
                                value = markdownText,
                                onValueChange = { markdownText = it },
                                modifier = Modifier.fillMaxSize().testTag("md_split_editor"),
                                textStyle = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 13.sp
                                ),
                                placeholder = { Text("Edit Markdown...") }
                            )
                        }

                        HorizontalDivider()

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(horizontal = 12.dp)
                        ) {
                            LazyColumn(modifier = Modifier.fillMaxSize().testTag("md_split_preview")) {
                                item {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "Live Preview:",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    MarkdownText(
                                        text = markdownText,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(32.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MarkdownToolbarButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = label, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
        }
    }
}

private fun getSampleMarkdownTemplate(): String = """
# 🚀 OmniChat Super Engine

Welcome to the **OmniChat Markdown Studio**. You can open, write, preview, and export `.md` files directly on your device.

---

## ⚡ Highlights & Key Features

- **Multi-Engine AI Assistance**: Gemini, Groq, OpenRouter, and free server-side endpoints.
- **Rich Media & Art Studio**: Generative images, motion videos, synthesizers, and audio visualizers.
- **Embedded Web Code Sandbox**: Run interactive HTML, CSS, JavaScript, and simulated Python.
- **Markdown Document Processor**: Full GFM specification with tables, code fences, and checklists.

---

### 📊 Capabilities Matrix

| Feature | Supported | Latency | Status |
|:---|:---:|:---:|:---|
| **Image Filters** | ✅ Yes | Instant | Production |
| **HTML Web Preview** | ✅ Yes | Real-time | Active |
| **Beat Visualizer** | ✅ Yes | 60 FPS | Smooth |
| **RAG Knowledge Store** | ✅ Yes | Local | Offline Ready |

---

### 💻 Code Example

```kotlin
fun main() {
    println("OmniChat is ready for high-velocity creation!")
}
```

> **Pro Tip**: Use the **Split View** tab to edit on top and watch real-time formatting updates below.

- [x] Create comprehensive image editor
- [x] Build Markdown file viewer and hub
- [x] Add code runner with in-site HTML preview
- [x] Implement beat visualizer & song studio
""".trimIndent()

private fun getProjectSpecTemplate(): String = """
# 📐 Technical Architecture Specification

## 1. Executive Summary
This document outlines the architectural specifications for the next-generation multi-modal client.

### Key Objectives
1. **Low Latency Processing**: High-speed local image transformations and audio visualizers.
2. **Unified Workspace**: Consolidated studio for code, documents, music, and graphics.

---

## 2. Component Diagram

```
+-------------------------------------------------------+
|                 OmniChat Client Shell                 |
+-------------------------------------------------------+
|  [Image Editor]  |  [Markdown Hub]  |  [Code Studio]  |
|  [Beat Player]   |  [News Stream]   |  [Maps Engine]  |
+-------------------------------------------------------+
```

## 3. Deployment Checklist
- [x] Android SDK 36 compatibility
- [x] Edge-to-edge WindowInsets
- [x] Zero network reliance for offline views
""".trimIndent()

private fun getTodoListTemplate(): String = """
# 📋 OmniChat Action Items & Sprint Plan

## 🎯 Priority Deliverables
- [x] Complete image editor suite with filters and brush annotations
- [x] Integrate full Markdown file opener and live preview
- [x] Provide code runner with in-app interactive HTML/JS sandbox
- [x] Design music player with audio tunes & rhythm visualizer

---

## 💡 Notes & Research
- Test playback on local audio files.
- Ensure HTML WebView properly intercepts console messages.
""".trimIndent()

private fun getPromptGuideTemplate(): String = """
# 🧠 AI Prompt Engineering Master Guide

## 1. System Persona Blueprint
```markdown
You are a senior staff engineer with deep expertise in Android, Kotlin, and Web systems.
Always prioritize clean architecture, accessible components, and resilient error recovery.
```

## 2. Few-Shot Pattern
- **Input**: "How do I optimize bitmap memory?"
- **Output**: "Use `BitmapFactory.Options.inSampleSize` and recycle when finished."
""".trimIndent()
