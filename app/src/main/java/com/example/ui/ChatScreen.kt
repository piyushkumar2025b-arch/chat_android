package com.example.ui

import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AiProvider
import com.example.model.AppSection
import com.example.model.ChatAttachment
import com.example.model.ChatMessage
import com.example.model.MessageRole
import com.example.model.MessageStatus
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val currentSection by viewModel.selectedSection.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val activeProvider by viewModel.activeProvider.collectAsState()
    val apiKeys by viewModel.apiKeys.collectAsState()
    val pendingAttachments by viewModel.pendingAttachments.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()

    val codeHandoff by viewModel.studioCodeHandoff.collectAsState()
    val markdownHandoff by viewModel.studioMarkdownHandoff.collectAsState()

    var inputPrompt by remember { mutableStateOf("") }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var inspectingAttachment by remember { mutableStateOf<ChatAttachment?>(null) }
    var showAttachmentSheet by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Comprehensive BackHandler
    BackHandler(enabled = true) {
        when {
            drawerState.isOpen -> coroutineScope.launch { drawerState.close() }
            inspectingAttachment != null -> inspectingAttachment = null
            currentSection != AppSection.CHAT -> viewModel.navigateToSection(AppSection.CHAT)
            else -> {
                // System default or toast
                Toast.makeText(context, "Press home to exit OmniChat", Toast.LENGTH_SHORT).show()
            }
        }
    }

    when (currentSection) {
        AppSection.INTEL_HUB -> {
            IntelHubScreen(
                productivityDao = viewModel.productivityDao,
                onBackToChat = { viewModel.navigateToSection(AppSection.CHAT) },
                onSendPromptToChat = { prompt ->
                    viewModel.navigateToSection(AppSection.CHAT)
                    viewModel.sendMessage(prompt)
                },
                onSendImageToChat = { bitmap ->
                    viewModel.navigateToSection(AppSection.CHAT)
                    viewModel.addAttachment(
                        ChatAttachment(
                            name = "Sketch_${System.currentTimeMillis()}.png",
                            mimeType = "image/png",
                            bitmap = bitmap,
                            content = "[Canvas Sketch Bitmap]"
                        )
                    )
                },
                onApplyApiKey = { key ->
                    viewModel.setApiKey(activeProvider, key)
                }
            )
        }
        AppSection.CODE_STUDIO -> {
            CodeStudioScreen(
                initialCode = codeHandoff?.first,
                initialFileName = codeHandoff?.second,
                initialLanguage = codeHandoff?.third,
                onBack = {
                    viewModel.clearCodeHandoff()
                    viewModel.navigateToSection(AppSection.CHAT)
                },
                onSendCodeToChat = { code, filename ->
                    viewModel.clearCodeHandoff()
                    viewModel.navigateToSection(AppSection.CHAT)
                    viewModel.addAttachment(
                        ChatAttachment(
                            name = filename,
                            mimeType = "text/x-kotlin",
                            content = code
                        )
                    )
                }
            )
        }
        AppSection.MARKDOWN_STUDIO -> {
            MarkdownStudioScreen(
                initialContent = markdownHandoff?.first,
                initialFileName = markdownHandoff?.second,
                onBack = {
                    viewModel.clearMarkdownHandoff()
                    viewModel.navigateToSection(AppSection.CHAT)
                },
                onSendMarkdownToChat = { content ->
                    viewModel.clearMarkdownHandoff()
                    viewModel.navigateToSection(AppSection.CHAT)
                    viewModel.sendMessage("Markdown Content:\n$content\n\nPlease review and optimize.")
                }
            )
        }
        AppSection.MEDIA_PLAYER -> {
            UniversalMediaPlayerScreen(
                onBack = { viewModel.navigateToSection(AppSection.CHAT) },
                onSendMediaToChat = { prompt ->
                    viewModel.navigateToSection(AppSection.CHAT)
                    viewModel.sendMessage(prompt)
                }
            )
        }
        AppSection.CHAT -> {
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    ModalDrawerSheet {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "OmniChat Workstation",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(16.dp)
                        )
                        Text(
                            text = "Multi-provider AI intelligence & Productivity Suite",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        NavigationDrawerItem(
                            label = { Text("Chat Workstation") },
                            icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null) },
                            selected = currentSection == AppSection.CHAT,
                            onClick = {
                                viewModel.navigateToSection(AppSection.CHAT)
                                coroutineScope.launch { drawerState.close() }
                            },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )

                        NavigationDrawerItem(
                            label = { Text("Intel Hub & Productivity") },
                            icon = { Icon(Icons.Default.Widgets, contentDescription = null) },
                            selected = false,
                            onClick = {
                                viewModel.navigateToSection(AppSection.INTEL_HUB)
                                coroutineScope.launch { drawerState.close() }
                            },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )

                        NavigationDrawerItem(
                            label = { Text("Code Studio") },
                            icon = { Icon(Icons.Default.Code, contentDescription = null) },
                            selected = false,
                            onClick = {
                                viewModel.navigateToSection(AppSection.CODE_STUDIO)
                                coroutineScope.launch { drawerState.close() }
                            },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )

                        NavigationDrawerItem(
                            label = { Text("Markdown Studio") },
                            icon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            selected = false,
                            onClick = {
                                viewModel.navigateToSection(AppSection.MARKDOWN_STUDIO)
                                coroutineScope.launch { drawerState.close() }
                            },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )

                        NavigationDrawerItem(
                            label = { Text("Media Player") },
                            icon = { Icon(Icons.Default.PlayCircle, contentDescription = null) },
                            selected = false,
                            onClick = {
                                viewModel.navigateToSection(AppSection.MEDIA_PLAYER)
                                coroutineScope.launch { drawerState.close() }
                            },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        NavigationDrawerItem(
                            label = { Text("AI Provider Keys & Models") },
                            icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                            selected = false,
                            onClick = {
                                showSettingsDialog = true
                                coroutineScope.launch { drawerState.close() }
                            },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            ) {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Column {
                                    Text("OmniChat", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        text = "${activeProvider.displayName} • ${activeProvider.defaultModel}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            },
                            navigationIcon = {
                                IconButton(
                                    onClick = { coroutineScope.launch { drawerState.open() } },
                                    modifier = Modifier.testTag("chat_drawer_toggle")
                                ) {
                                    Icon(Icons.Default.Menu, contentDescription = "Menu")
                                }
                            },
                            actions = {
                                IconButton(onClick = { viewModel.navigateToSection(AppSection.INTEL_HUB) }) {
                                    Icon(Icons.Default.Widgets, contentDescription = "Intel Hub", tint = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = { viewModel.clearChat() }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Clear Chat")
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
                        // Horizontal Provider Switcher
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AiProvider.values().forEach { prov ->
                                FilterChip(
                                    selected = activeProvider == prov,
                                    onClick = { viewModel.setProvider(prov) },
                                    label = { Text(prov.displayName) },
                                    leadingIcon = if (activeProvider == prov) {
                                        { Icon(Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null
                                )
                            }
                        }

                        // Message List
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(messages, key = { it.id }) { msg ->
                                val isUser = msg.role == MessageRole.USER
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                                ) {
                                    Card(
                                        shape = RoundedCornerShape(
                                            topStart = 18.dp,
                                            topEnd = 18.dp,
                                            bottomStart = if (isUser) 18.dp else 4.dp,
                                            bottomEnd = if (isUser) 4.dp else 18.dp
                                        ),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isUser) MaterialTheme.colorScheme.primaryContainer
                                            else MaterialTheme.colorScheme.surfaceContainerHigh
                                        ),
                                        modifier = Modifier.fillMaxWidth(0.92f)
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = if (isUser) "You" else msg.modelName.ifBlank { activeProvider.displayName },
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer
                                                    else MaterialTheme.colorScheme.primary
                                                )

                                                if (!isUser) {
                                                    IconButton(
                                                        onClick = {
                                                            clipboardManager.setText(AnnotatedString(msg.content))
                                                            Toast.makeText(context, "Copied response!", Toast.LENGTH_SHORT).show()
                                                        },
                                                        modifier = Modifier.size(24.dp)
                                                    ) {
                                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp))
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(6.dp))

                                            // Render Attachments if any
                                            if (msg.attachments.isNotEmpty()) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .horizontalScroll(rememberScrollState()),
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    msg.attachments.forEach { att ->
                                                        AssistChip(
                                                            onClick = { inspectingAttachment = att },
                                                            label = { Text(att.name) },
                                                            leadingIcon = {
                                                                Icon(
                                                                    if (att.mimeType.contains("image")) Icons.Default.Image
                                                                    else if (att.mimeType.contains("code") || att.name.endsWith(".kt")) Icons.Default.Code
                                                                    else Icons.Default.Description,
                                                                    contentDescription = null,
                                                                    modifier = Modifier.size(16.dp)
                                                                )
                                                            }
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(6.dp))
                                            }

                                            Text(
                                                text = msg.content,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer
                                                else MaterialTheme.colorScheme.onSurface
                                            )

                                            if (msg.status == MessageStatus.STREAMING) {
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text("Synthesizing response...", style = MaterialTheme.typography.labelSmall)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Pending Attachments preview tray
                        if (pendingAttachments.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Attached:", style = MaterialTheme.typography.labelSmall)
                                pendingAttachments.forEach { att ->
                                    AssistChip(
                                        onClick = { inspectingAttachment = att },
                                        label = { Text(att.name) },
                                        trailingIcon = {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = "Remove",
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .clickable { viewModel.removeAttachment(att.id) }
                                            )
                                        }
                                    )
                                }
                            }
                        }

                        // Prompt Input Bar
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            tonalElevation = 6.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                IconButton(
                                    onClick = { showAttachmentSheet = true },
                                    modifier = Modifier.testTag("chat_attach_button")
                                ) {
                                    Icon(Icons.Default.AttachFile, contentDescription = "Attach File")
                                }

                                OutlinedTextField(
                                    value = inputPrompt,
                                    onValueChange = { inputPrompt = it },
                                    placeholder = { Text("Ask ${activeProvider.displayName}...") },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("chat_input_field"),
                                    maxLines = 4,
                                    shape = RoundedCornerShape(24.dp)
                                )

                                IconButton(
                                    onClick = {
                                        if (inputPrompt.isNotBlank() || pendingAttachments.isNotEmpty()) {
                                            viewModel.sendMessage(inputPrompt)
                                            inputPrompt = ""
                                        }
                                    },
                                    enabled = !isGenerating,
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(
                                            if (isGenerating) MaterialTheme.colorScheme.surfaceVariant
                                            else MaterialTheme.colorScheme.primary
                                        )
                                        .testTag("chat_send_button")
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Send,
                                        contentDescription = "Send",
                                        tint = if (isGenerating) MaterialTheme.colorScheme.outline
                                        else MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Attachment Source Dialog
    if (showAttachmentSheet) {
        AlertDialog(
            onDismissRequest = { showAttachmentSheet = false },
            title = { Text("Attach Context to Prompt") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = {
                            viewModel.addAttachment(
                                ChatAttachment(
                                    name = "Algorithm.kt",
                                    mimeType = "text/x-kotlin",
                                    content = "fun fibonacci(n: Int): Int = if (n <= 1) n else fibonacci(n - 1) + fibonacci(n - 2)"
                                )
                            )
                            showAttachmentSheet = false
                            Toast.makeText(context, "Kotlin code attached!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Code, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sample Kotlin Code")
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.addAttachment(
                                ChatAttachment(
                                    name = "Architecture_Doc.md",
                                    mimeType = "text/markdown",
                                    content = "# Architecture Guidelines\n- Clean MVVM architecture\n- Material Design 3 spacing\n- SQLite Local persistence"
                                )
                            )
                            showAttachmentSheet = false
                            Toast.makeText(context, "Markdown doc attached!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Description, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Markdown Spec Document")
                    }

                    OutlinedButton(
                        onClick = {
                            showAttachmentSheet = false
                            viewModel.navigateToSection(AppSection.INTEL_HUB)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open Sketch & Image Studio")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAttachmentSheet = false }) { Text("Cancel") }
            }
        )
    }

    // Attachment Inspector Dialog - FIX BUG-05: Properly forwards code/content to Code & Markdown Studio!
    inspectingAttachment?.let { att ->
        AlertDialog(
            onDismissRequest = { inspectingAttachment = null },
            title = { Text(att.name) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "MIME: ${att.mimeType}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Text(
                            text = att.content.ifBlank { "[Binary or Graphic Attachment]" },
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(10.dp)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // FIX BUG-05: Forward code and filename to Code Studio
                        Button(
                            onClick = {
                                val code = att.content
                                val name = att.name
                                val lang = if (name.endsWith(".kt")) "Kotlin" else if (name.endsWith(".py")) "Python" else "JavaScript"
                                inspectingAttachment = null
                                viewModel.openCodeInStudio(code, name, lang)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Open in Code Studio", fontSize = 11.sp)
                        }

                        // FIX BUG-05: Forward markdown content and filename to Markdown Studio
                        Button(
                            onClick = {
                                val content = att.content
                                val name = att.name
                                inspectingAttachment = null
                                viewModel.openMarkdownInStudio(content, name)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Open in Markdown", fontSize = 11.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { inspectingAttachment = null }) { Text("Done") }
            }
        )
    }

    // Settings & API Key Dialog
    if (showSettingsDialog) {
        var groqKey by remember { mutableStateOf(apiKeys[AiProvider.GROQ] ?: "") }
        var openRouterKey by remember { mutableStateOf(apiKeys[AiProvider.OPENROUTER] ?: "") }
        var geminiKey by remember { mutableStateOf(apiKeys[AiProvider.GEMINI] ?: "") }

        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = { Text("AI Provider Settings & API Keys") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Enter custom API keys to route prompts to your own cloud endpoints, or use Free Web AI with zero credentials.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = geminiKey,
                        onValueChange = { geminiKey = it },
                        label = { Text("Google Gemini API Key") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = groqKey,
                        onValueChange = { groqKey = it },
                        label = { Text("Groq Cloud API Key") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = openRouterKey,
                        onValueChange = { openRouterKey = it },
                        label = { Text("OpenRouter API Key") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (geminiKey.isNotBlank()) viewModel.setApiKey(AiProvider.GEMINI, geminiKey)
                        if (groqKey.isNotBlank()) viewModel.setApiKey(AiProvider.GROQ, groqKey)
                        if (openRouterKey.isNotBlank()) viewModel.setApiKey(AiProvider.OPENROUTER, openRouterKey)
                        showSettingsDialog = false
                        Toast.makeText(context, "API Keys updated!", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Save Keys")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSettingsDialog = false }) { Text("Close") }
            }
        )
    }
}
