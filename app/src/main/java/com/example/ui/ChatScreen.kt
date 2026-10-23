package com.example.ui

import android.content.Intent
import android.net.Uri
import android.speech.tts.TextToSpeech
import android.text.format.DateUtils
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.UsageStats
import com.example.data.model.AttachmentInfo
import com.example.data.model.ChatSessionEntity
import com.example.data.model.ProviderType
import com.example.data.remote.ConversationExporter
import com.example.data.remote.FileUtils
import com.example.model.AppSection
import com.example.model.ChatAttachment
import com.example.model.ChatMessage
import com.example.model.MessageRole
import com.example.model.MessageStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

private val DOCUMENT_MIME_TYPES = arrayOf(
    "application/pdf",
    "application/msword",
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    "application/vnd.ms-excel",
    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    "application/vnd.openxmlformats-officedocument.presentationml.presentation",
    "text/plain",
    "text/markdown",
    "text/csv",
    "application/rtf"
)

private val CODE_MIME_TYPES = arrayOf(
    "text/*",
    "application/json",
    "application/xml",
    "application/javascript",
    "application/x-yaml"
)

private val CHAT_SUGGESTIONS: List<Pair<String, ImageVector>> = listOf(
    "Explain a complex topic with a simple, real-world analogy." to Icons.Default.Psychology,
    "Summarize the key points of a document I attach." to Icons.Default.Description,
    "Review a code snippet and suggest concrete improvements." to Icons.Default.Code,
    "Draft a focused plan for my day with three priorities." to Icons.Default.Speed
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ChatScreen(viewModel: ChatViewModel) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val currentSection by viewModel.selectedSection.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val activeProvider by viewModel.activeProvider.collectAsState()
    val activeModel by viewModel.activeModel.collectAsState()
    val sessions by viewModel.sessions.collectAsState()
    val activeSessionId by viewModel.activeSessionId.collectAsState()
    val pendingAttachments by viewModel.pendingAttachments.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val isSessionReady by viewModel.isSessionReady.collectAsState()
    val isPreparingAttachment by viewModel.isPreparingAttachment.collectAsState()
    val usageStats by viewModel.usageStats.collectAsState()
    val isRagEnabled by viewModel.isRagEnabled.collectAsState()
    val themeId by viewModel.themeId.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val isAppUnlocked by viewModel.isAppUnlocked.collectAsState()
    val isAppLockEnabled by viewModel.isAppLockEnabled.collectAsState()
    val composerDraft by viewModel.composerDraft.collectAsState()
    val focusMessageId by viewModel.focusMessageId.collectAsState()

    val codeHandoff by viewModel.studioCodeHandoff.collectAsState()
    val markdownHandoff by viewModel.studioMarkdownHandoff.collectAsState()

    var inputPrompt by remember { mutableStateOf("") }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showProviderSheet by remember { mutableStateOf(false) }
    var showUsageSheet by remember { mutableStateOf(false) }
    var showStorageSheet by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var inspectingAttachment by remember { mutableStateOf<ChatAttachment?>(null) }
    var renameTarget by remember { mutableStateOf<ChatSessionEntity?>(null) }
    var renameText by remember { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<ChatSessionEntity?>(null) }
    var editMessageTarget by remember { mutableStateOf<ChatMessage?>(null) }
    var editMessageText by remember { mutableStateOf("") }
    var deleteMessageTarget by remember { mutableStateOf<ChatMessage?>(null) }

    if (isAppLockEnabled && !isAppUnlocked) {
        AppLockScreen(onUnlock = viewModel::unlockApp)
        return
    }

    val providerSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val usageSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val storageSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var ttsEngine by remember { mutableStateOf<TextToSpeech?>(null) }
    var ttsReady by remember { mutableStateOf(false) }
    var speakingMessageId by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        val engine = TextToSpeech(context) { status ->
            ttsReady = status == TextToSpeech.SUCCESS
        }
        engine.language = Locale.getDefault()
        ttsEngine = engine
        onDispose {
            engine.stop()
            engine.shutdown()
            ttsEngine = null
        }
    }

    fun toast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    fun attachPicked(uri: Uri?) {
        if (uri == null) return
        viewModel.attachUri(uri) { _, message -> toast(message) }
    }

    val browseLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { attachPicked(it) }
    val documentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { attachPicked(it) }
    val codeLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { attachPicked(it) }
    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { attachPicked(it) }

    fun toggleSpeech(message: ChatMessage) {
        if (speakingMessageId == message.id) {
            ttsEngine?.stop()
            speakingMessageId = null
            return
        }
        if (!ttsReady) {
            toast("Voice engine is still starting…")
            return
        }
        val plain = message.content
            .replace(Regex("```[\\s\\S]*?```"), " code block ")
            .replace(Regex("[#*_>`~|]"), "")
            .trim()
        if (plain.isBlank()) return
        ttsEngine?.speak(plain, TextToSpeech.QUEUE_FLUSH, null, "omni_${message.id}")
        speakingMessageId = message.id
    }

    val listState = rememberLazyListState()
    val lastMessageLength = messages.lastOrNull()?.content?.length ?: 0

    LaunchedEffect(messages.size, lastMessageLength) {
        if (focusMessageId != null) return@LaunchedEffect
        if (messages.isNotEmpty()) {
            if (isGenerating) listState.scrollToItem(messages.lastIndex)
            else listState.animateScrollToItem(messages.lastIndex)
        }
    }

    LaunchedEffect(composerDraft) {
        val draft = composerDraft ?: return@LaunchedEffect
        inputPrompt = if (inputPrompt.isBlank()) draft else "$inputPrompt\n\n$draft"
        viewModel.clearComposerDraft()
    }

    LaunchedEffect(focusMessageId, messages) {
        val targetId = focusMessageId ?: return@LaunchedEffect
        val index = messages.indexOfFirst { it.id == targetId }
        if (index >= 0) {
            listState.animateScrollToItem(index)
            delay(1_800L)
            viewModel.clearMessageFocus()
        }
    }

    LaunchedEffect(focusMessageId) {
        val targetId = focusMessageId ?: return@LaunchedEffect
        delay(6_000L)
        if (viewModel.focusMessageId.value == targetId) {
            viewModel.clearMessageFocus()
        }
    }

    BackHandler(enabled = drawerState.isOpen || inspectingAttachment != null || currentSection != AppSection.CHAT) {
        when {
            inspectingAttachment != null -> inspectingAttachment = null
            drawerState.isOpen -> coroutineScope.launch { drawerState.close() }
            currentSection != AppSection.CHAT -> viewModel.navigateToSection(AppSection.CHAT)
        }
    }

    when (currentSection) {
        AppSection.INTEL_HUB -> {
            IntelHubScreen(
                viewModel = viewModel,
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
                    val target = if (activeProvider.requiresApiKey) activeProvider else ProviderType.GEMINI
                    viewModel.setApiKey(target, key)
                },
                onOpenSettings = { showSettingsDialog = true }
            )
        }

        AppSection.CREATIVE_STUDIO -> {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Creative Studio", fontWeight = FontWeight.Bold) },
                        navigationIcon = {
                            IconButton(onClick = { viewModel.navigateToSection(AppSection.CHAT) }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        )
                    )
                }
            ) { innerPadding ->
                StudioScreen(
                    initialTab = 0,
                    preferencesManager = viewModel.preferencesManager,
                    onSendPromptToChat = { prompt ->
                        viewModel.navigateToSection(AppSection.CHAT)
                        viewModel.sendMessage(prompt)
                    },
                    modifier = Modifier.padding(innerPadding)
                )
            }
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

        AppSection.RAG_KNOWLEDGE -> {
            RagKnowledgeScreen(
                viewModel = viewModel,
                onBack = { viewModel.navigateToSection(AppSection.CHAT) }
            )
        }

        AppSection.CHAT_SEARCH -> {
            ChatSearchScreen(
                viewModel = viewModel,
                onBack = { viewModel.navigateToSection(AppSection.CHAT) }
            )
        }

        AppSection.AI_MEMORY -> {
            MemoryScreen(
                viewModel = viewModel,
                onBack = { viewModel.navigateToSection(AppSection.CHAT) }
            )
        }

        AppSection.PROMPT_LIBRARY -> {
            PromptLibraryScreen(
                viewModel = viewModel,
                onBack = { viewModel.navigateToSection(AppSection.CHAT) }
            )
        }

        AppSection.FOCUS_HABITS -> {
            FocusHabitsScreen(
                viewModel = viewModel,
                onBack = { viewModel.navigateToSection(AppSection.CHAT) }
            )
        }

        AppSection.BACKUP_RESTORE -> {
            BackupRestoreScreen(
                viewModel = viewModel,
                onBack = { viewModel.navigateToSection(AppSection.CHAT) }
            )
        }

        AppSection.CHAT -> {
            val activeSessionTitle = sessions.firstOrNull { it.id == activeSessionId }
                ?.title?.ifBlank { "New Chat" }
                ?: "OmniChat"
            val canSend = (inputPrompt.isNotBlank() || pendingAttachments.isNotEmpty()) && isSessionReady

            ModalNavigationDrawer(
                drawerState = drawerState,
                gesturesEnabled = currentSection == AppSection.CHAT,
                drawerContent = {
                    OmniChatDrawer(
                        sessions = sessions,
                        activeSessionId = activeSessionId,
                        currentSection = currentSection,
                        onNewChat = {
                            viewModel.newSession()
                            inputPrompt = ""
                            coroutineScope.launch { drawerState.close() }
                        },
                        onSelectSession = { id ->
                            viewModel.selectSession(id)
                            coroutineScope.launch { drawerState.close() }
                        },
                        onRenameSession = { session ->
                            renameText = session.title
                            renameTarget = session
                            coroutineScope.launch { drawerState.close() }
                        },
                        onDeleteSession = { session ->
                            deleteTarget = session
                            coroutineScope.launch { drawerState.close() }
                        },
                        onNavigate = { section ->
                            viewModel.navigateToSection(section)
                            coroutineScope.launch { drawerState.close() }
                        },
                        onOpenSettings = {
                            showSettingsDialog = true
                            coroutineScope.launch { drawerState.close() }
                        },
                        onOpenThemes = {
                            showThemeDialog = true
                            coroutineScope.launch { drawerState.close() }
                        },
                        onOpenUsage = {
                            showUsageSheet = true
                            coroutineScope.launch { drawerState.close() }
                        }
                    )
                }
            ) {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Column {
                                    Text(
                                        text = activeSessionTitle,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${activeProvider.displayName.substringBefore(" (")} • ${activeModel.name}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            },
                            navigationIcon = {
                                IconButton(
                                    onClick = { coroutineScope.launch { drawerState.open() } },
                                    modifier = Modifier.testTag("chat_drawer_toggle")
                                ) {
                                    Icon(Icons.Default.Menu, contentDescription = "Open chats & tools")
                                }
                            },
                            actions = {
                                IconButton(
                                    onClick = {
                                        viewModel.newSession()
                                        inputPrompt = ""
                                    },
                                    modifier = Modifier.testTag("new_chat_button")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "New chat")
                                }
                                IconButton(
                                    onClick = { viewModel.navigateToSection(AppSection.CHAT_SEARCH) },
                                    modifier = Modifier.testTag("open_chat_search_button")
                                ) {
                                    Icon(Icons.Default.Search, contentDescription = "Search chats")
                                }
                                IconButton(
                                    onClick = { viewModel.navigateToSection(AppSection.RAG_KNOWLEDGE) },
                                    modifier = Modifier.testTag("open_knowledge_base_button")
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = "Knowledge base")
                                }
                                IconButton(
                                    onClick = { showSettingsDialog = true },
                                    modifier = Modifier.testTag("open_settings_button")
                                ) {
                                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                                }
                                Box {
                                    IconButton(
                                        onClick = { showOverflowMenu = true },
                                        modifier = Modifier.testTag("chat_more_menu_button")
                                    ) {
                                        Icon(Icons.Default.MoreVert, contentDescription = "More actions")
                                    }
                                    DropdownMenu(
                                        expanded = showOverflowMenu,
                                        onDismissRequest = { showOverflowMenu = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Search chats") },
                                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                            onClick = {
                                                showOverflowMenu = false
                                                viewModel.navigateToSection(AppSection.CHAT_SEARCH)
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("AI Memory") },
                                            leadingIcon = { Icon(Icons.Default.Psychology, contentDescription = null) },
                                            onClick = {
                                                showOverflowMenu = false
                                                viewModel.navigateToSection(AppSection.AI_MEMORY)
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Prompt Library") },
                                            leadingIcon = { Icon(Icons.Default.Bookmarks, contentDescription = null) },
                                            onClick = {
                                                showOverflowMenu = false
                                                viewModel.navigateToSection(AppSection.PROMPT_LIBRARY)
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Focus & Habits") },
                                            leadingIcon = { Icon(Icons.Default.Timer, contentDescription = null) },
                                            onClick = {
                                                showOverflowMenu = false
                                                viewModel.navigateToSection(AppSection.FOCUS_HABITS)
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Backup & Restore") },
                                            leadingIcon = { Icon(Icons.Default.SettingsBackupRestore, contentDescription = null) },
                                            onClick = {
                                                showOverflowMenu = false
                                                viewModel.navigateToSection(AppSection.BACKUP_RESTORE)
                                            }
                                        )
                                        HorizontalDivider()
                                        DropdownMenuItem(
                                            text = { Text("Export chat as Markdown") },
                                            leadingIcon = { Icon(Icons.Default.Download, contentDescription = null) },
                                            onClick = {
                                                showOverflowMenu = false
                                                val sessionId = activeSessionId
                                                if (sessionId == null) {
                                                    toast("Open a conversation first.")
                                                } else {
                                                    viewModel.exportConversation(sessionId, ConversationExporter.Format.MARKDOWN) { _, message ->
                                                        toast(message)
                                                    }
                                                }
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Export chat as JSON") },
                                            leadingIcon = { Icon(Icons.Default.Download, contentDescription = null) },
                                            onClick = {
                                                showOverflowMenu = false
                                                val sessionId = activeSessionId
                                                if (sessionId == null) {
                                                    toast("Open a conversation first.")
                                                } else {
                                                    viewModel.exportConversation(sessionId, ConversationExporter.Format.JSON) { _, message ->
                                                        toast(message)
                                                    }
                                                }
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Share conversation") },
                                            leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                                            onClick = {
                                                showOverflowMenu = false
                                                val shareText = viewModel.currentConversationShareText()
                                                if (shareText.isNullOrBlank()) {
                                                    toast("There is nothing to share yet.")
                                                } else {
                                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                        type = "text/plain"
                                                        putExtra(Intent.EXTRA_SUBJECT, activeSessionTitle)
                                                        putExtra(Intent.EXTRA_TEXT, shareText)
                                                    }
                                                    context.startActivity(
                                                        Intent.createChooser(shareIntent, "Share conversation")
                                                    )
                                                }
                                            }
                                        )
                                        if (isAppLockEnabled) {
                                            HorizontalDivider()
                                            DropdownMenuItem(
                                                text = { Text("Lock app now") },
                                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                                                onClick = {
                                                    showOverflowMenu = false
                                                    viewModel.lockApp()
                                                }
                                            )
                                        }
                                    }
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
                        ProviderQuickBar(
                            activeProvider = activeProvider,
                            activeModelName = activeModel.name,
                            isRagEnabled = isRagEnabled,
                            usageStats = usageStats,
                            onOpenProviders = { showProviderSheet = true },
                            onToggleRag = { viewModel.toggleRag() },
                            onOpenUsage = { showUsageSheet = true }
                        )

                        when {
                            !isSessionReady -> {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            }

                            messages.isEmpty() -> {
                                EmptyChatState(
                                    modifier = Modifier.weight(1f),
                                    onSuggestion = { inputPrompt = it }
                                )
                            }

                            else -> {
                                LazyColumn(
                                    state = listState,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(messages, key = { it.id }) { message ->
                                        ChatMessageBubble(
                                            message = message,
                                            providerName = activeProvider.displayName.substringBefore(" ("),
                                            isLastAssistant = message.role == MessageRole.ASSISTANT &&
                                                    message.id == messages.lastOrNull()?.id,
                                            isGenerating = isGenerating,
                                            isSpeaking = speakingMessageId == message.id,
                                            isFocused = focusMessageId == message.id,
                                            onCopy = {
                                                clipboardManager.setText(AnnotatedString(message.content))
                                                toast("Copied to clipboard")
                                            },
                                            onSpeakToggle = { toggleSpeech(message) },
                                            onRegenerate = { viewModel.regenerateLastResponse() },
                                            onStop = { viewModel.stopGeneration() },
                                            onAttachmentClick = { inspectingAttachment = it },
                                            onOpenCode = { title, language, code ->
                                                viewModel.openCodeInStudio(
                                                    code = code,
                                                    filename = codeFileName(title, language),
                                                    language = language
                                                )
                                            },
                                            onEditMessage = {
                                                editMessageTarget = message
                                                editMessageText = message.content
                                            },
                                            onDeleteMessage = { deleteMessageTarget = message },
                                            onAskFollowUp = { prompt -> viewModel.sendMessage(prompt) }
                                        )
                                    }
                                }
                            }
                        }

                        if (pendingAttachments.isNotEmpty()) {
                            FlowRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                pendingAttachments.forEach { attachment ->
                                    AttachmentBadge(
                                        attachment = attachment.toAttachmentInfo(),
                                        onClick = { inspectingAttachment = attachment },
                                        onRemove = { viewModel.removeAttachment(attachment.id) }
                                    )
                                }
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .imePadding(),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            tonalElevation = 6.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                IconButton(
                                    onClick = { showStorageSheet = true },
                                    enabled = !isPreparingAttachment,
                                    modifier = Modifier.testTag("chat_attach_button")
                                ) {
                                    if (isPreparingAttachment) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Icon(Icons.Default.AttachFile, contentDescription = "Attach a file or photo")
                                    }
                                }

                                OutlinedTextField(
                                    value = inputPrompt,
                                    onValueChange = { inputPrompt = it },
                                    placeholder = {
                                        Text(
                                            if (activeProvider.requiresApiKey && !viewModel.isKeyConfigured(activeProvider)) {
                                                "Add an API key or switch provider…"
                                            } else {
                                                "Message ${activeModel.name}…"
                                            }
                                        )
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("chat_input_field"),
                                    maxLines = 5,
                                    shape = RoundedCornerShape(24.dp),
                                    keyboardOptions = KeyboardOptions(
                                        capitalization = KeyboardCapitalization.Sentences,
                                        imeAction = ImeAction.Send
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onSend = {
                                            if (canSend && !isGenerating) {
                                                viewModel.sendMessage(inputPrompt)
                                                inputPrompt = ""
                                            }
                                        }
                                    ),
                                    trailingIcon = {
                                        IconButton(
                                            onClick = { viewModel.navigateToSection(AppSection.PROMPT_LIBRARY) },
                                            modifier = Modifier.testTag("open_prompt_library_button")
                                        ) {
                                            Icon(
                                                Icons.Default.Bookmarks,
                                                contentDescription = "Prompt library",
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                )

                                VoiceInputButton(
                                    enabled = !isGenerating,
                                    onResult = { text ->
                                        inputPrompt = if (inputPrompt.isBlank()) text
                                        else "$inputPrompt $text"
                                    }
                                )

                                if (isGenerating) {
                                    IconButton(
                                        onClick = { viewModel.stopGeneration() },
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.errorContainer)
                                            .testTag("chat_stop_button")
                                    ) {
                                        Icon(
                                            Icons.Default.Stop,
                                            contentDescription = "Stop generating",
                                            tint = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                } else {
                                    IconButton(
                                        onClick = {
                                            if (canSend) {
                                                viewModel.sendMessage(inputPrompt)
                                                inputPrompt = ""
                                            }
                                        },
                                        enabled = canSend,
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(
                                                if (canSend) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.surfaceVariant
                                            )
                                            .testTag("chat_send_button")
                                    ) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.Send,
                                            contentDescription = "Send message",
                                            tint = if (canSend) MaterialTheme.colorScheme.onPrimary
                                            else MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showProviderSheet) {
        ProviderSelectorSheet(
            sheetState = providerSheetState,
            selectedProvider = activeProvider,
            selectedModel = activeModel,
            isKeyConfigured = viewModel::isKeyConfigured,
            getKeyForProvider = viewModel::keyForProvider,
            onUpdateKey = { provider, key -> viewModel.setApiKey(provider, key) },
            onModelSelected = { provider, model -> viewModel.setModel(provider, model) },
            onOpenSettings = { showSettingsDialog = true },
            onDismiss = { showProviderSheet = false }
        )
    }

    if (showUsageSheet) {
        UsageLimitsSheet(
            sheetState = usageSheetState,
            usageStats = usageStats,
            onSetDailyLimit = { viewModel.setDailyLimit(it) },
            onResetTodayStats = { viewModel.resetUsageStats() },
            onDismiss = { showUsageSheet = false }
        )
    }

    if (showStorageSheet) {
        DeviceStorageSheet(
            sheetState = storageSheetState,
            onDismiss = { showStorageSheet = false },
            onBrowsePhoneStorage = { browseLauncher.launch(arrayOf("*/*")) },
            onPickDocuments = { documentLauncher.launch(DOCUMENT_MIME_TYPES) },
            onPickPhotos = { photoLauncher.launch("image/*") },
            onPickCode = { codeLauncher.launch(CODE_MIME_TYPES) },
            onAddSampleFile = { sampleType ->
                viewModel.addSampleAttachment(sampleType) { _, message -> toast(message) }
            },
            onAttachCachedFile = { file ->
                viewModel.attachCachedFile(file) { _, message -> toast(message) }
            }
        )
    }

    if (showSettingsDialog) {
        SettingsDialog(
            preferencesManager = viewModel.preferencesManager,
            appLockManager = viewModel.appLockManager,
            onOpenThemePicker = {
                showSettingsDialog = false
                showThemeDialog = true
            },
            onOpenUsageLimits = {
                showSettingsDialog = false
                showUsageSheet = true
            },
            onDismiss = { showSettingsDialog = false },
            onClearAllChats = { viewModel.clearAllChats() },
            onKeyUpdated = { viewModel.refreshFromPreferences() },
            onAppLockChanged = { enabled -> viewModel.onAppLockConfigured(enabled) },
            onLockNow = {
                showSettingsDialog = false
                viewModel.lockApp()
            },
            onOpenBackupRestore = {
                showSettingsDialog = false
                viewModel.navigateToSection(AppSection.BACKUP_RESTORE)
            }
        )
    }

    if (showThemeDialog) {
        ThemeSelectorDialog(
            currentTheme = themeId,
            currentThemeMode = themeMode,
            onThemeSelected = { viewModel.setTheme(it) },
            onThemeModeSelected = { viewModel.setThemeMode(it) },
            onDismiss = { showThemeDialog = false }
        )
    }

    inspectingAttachment?.let { attachment ->
        AttachmentInspectorDialog(
            attachment = attachment,
            onDismiss = { inspectingAttachment = null },
            onOpenCode = { code, filename, language ->
                viewModel.openCodeInStudio(code, filename, language)
                inspectingAttachment = null
            },
            onOpenMarkdown = { content, filename ->
                viewModel.openMarkdownInStudio(content, filename)
                inspectingAttachment = null
            }
        )
    }

    renameTarget?.let { session ->
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("Rename chat") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text("Chat title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.renameSession(session.id, renameText)
                        renameTarget = null
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) { Text("Cancel") }
            }
        )
    }

    deleteTarget?.let { session ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete this chat?") },
            text = { Text("\"${session.title}\" and all of its messages will be permanently deleted.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteSession(session.id)
                        deleteTarget = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancel") }
            }
        )
    }

    editMessageTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { editMessageTarget = null },
            title = { Text("Edit message") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editMessageText,
                        onValueChange = { editMessageText = it },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 8
                    )
                    Text(
                        "Editing regenerates the reply. Messages sent after this one will be removed.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.editUserMessageAndRegenerate(target.id, editMessageText)
                        editMessageTarget = null
                    },
                    enabled = editMessageText.isNotBlank() && !isGenerating
                ) {
                    Text("Save & regenerate")
                }
            },
            dismissButton = {
                TextButton(onClick = { editMessageTarget = null }) { Text("Cancel") }
            }
        )
    }

    deleteMessageTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteMessageTarget = null },
            title = { Text("Delete message?") },
            text = { Text("This message will be permanently removed from the conversation.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteMessage(target.id)
                        deleteMessageTarget = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteMessageTarget = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ProviderQuickBar(
    activeProvider: ProviderType,
    activeModelName: String,
    isRagEnabled: Boolean,
    usageStats: UsageStats,
    onOpenProviders: () -> Unit,
    onToggleRag: () -> Unit,
    onOpenUsage: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AssistChip(
            onClick = onOpenProviders,
            label = {
                Text(
                    "${activeProvider.displayName.substringBefore(" (")} · $activeModelName",
                    maxLines = 1
                )
            },
            leadingIcon = {
                Icon(Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(16.dp))
            },
            trailingIcon = {
                Icon(Icons.Default.ArrowDropDown, contentDescription = "Change provider", modifier = Modifier.size(18.dp))
            },
            modifier = Modifier.testTag("provider_selector_chip")
        )

        FilterChip(
            selected = isRagEnabled,
            onClick = onToggleRag,
            label = { Text(if (isRagEnabled) "Knowledge ON" else "Knowledge OFF") },
            leadingIcon = {
                Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp))
            },
            modifier = Modifier.testTag("rag_toggle_chip")
        )

        AssistChip(
            onClick = onOpenUsage,
            label = {
                Text(
                    if (usageStats.dailyLimit > 0) {
                        "${usageStats.requestsToday}/${usageStats.dailyLimit} today"
                    } else {
                        "${usageStats.requestsToday} requests"
                    }
                )
            },
            leadingIcon = {
                Icon(
                    if (usageStats.isLimitReached) Icons.Default.ErrorOutline else Icons.Default.Speed,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            },
            modifier = Modifier.testTag("usage_chip")
        )
    }
}

@Composable
private fun EmptyChatState(
    modifier: Modifier = Modifier,
    onSuggestion: (String) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "How can I help today?",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Ask anything, attach a file, or pull grounded answers from your knowledge base.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(18.dp))

        CHAT_SUGGESTIONS.forEach { (prompt, icon) ->
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { onSuggestion(prompt) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = prompt,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChatMessageBubble(
    message: ChatMessage,
    providerName: String,
    isLastAssistant: Boolean,
    isGenerating: Boolean,
    isSpeaking: Boolean,
    isFocused: Boolean = false,
    onCopy: () -> Unit,
    onSpeakToggle: () -> Unit,
    onRegenerate: () -> Unit,
    onStop: () -> Unit,
    onAttachmentClick: (ChatAttachment) -> Unit,
    onOpenCode: (title: String, language: String, code: String) -> Unit,
    onEditMessage: () -> Unit = {},
    onDeleteMessage: () -> Unit = {},
    onAskFollowUp: (String) -> Unit = {}
) {
    val isUser = message.role == MessageRole.USER
    val isError = message.status == MessageStatus.ERROR
    val isStreaming = message.status == MessageStatus.STREAMING

    val bubbleColor = when {
        isError -> MaterialTheme.colorScheme.errorContainer
        isUser -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val contentColor = when {
        isError -> MaterialTheme.colorScheme.onErrorContainer
        isUser -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = "AI assistant",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            modifier = Modifier.widthIn(max = 620.dp),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            Surface(
                shape = RoundedCornerShape(
                    topStart = 18.dp,
                    topEnd = 18.dp,
                    bottomStart = if (isUser) 18.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 18.dp
                ),
                color = bubbleColor,
                contentColor = contentColor,
                border = if (isFocused) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    if (!isUser) {
                        Text(
                            text = message.modelName.ifBlank { providerName },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }

                    if (message.attachments.isNotEmpty()) {
                        FlowRow(
                            modifier = Modifier.padding(bottom = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            message.attachments.forEach { attachment ->
                                AttachmentBadge(
                                    attachment = attachment.toAttachmentInfo(),
                                    onClick = { onAttachmentClick(attachment) }
                                )
                            }
                        }
                    }

                    when {
                        isUser -> {
                            Text(
                                text = message.content,
                                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                                color = contentColor
                            )
                        }

                        isError -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = message.content,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = contentColor
                                )
                            }
                        }

                        message.content.isBlank() -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Thinking…", style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        else -> {
                            MarkdownText(
                                text = message.content,
                                textColor = contentColor,
                                style = MaterialTheme.typography.bodyMedium,
                                onOpenArtifact = { title, language, code ->
                                    onOpenCode(title, language, code)
                                }
                            )
                        }
                    }

                    if (isStreaming) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Streaming…", style = MaterialTheme.typography.labelSmall)
                            Spacer(modifier = Modifier.weight(1f))
                            TextButton(onClick = onStop) {
                                Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Stop")
                            }
                        }
                    }
                }
            }

            if (!isStreaming) {
                Row(
                    modifier = Modifier.padding(top = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!isUser) {
                        MessageAction(onCopy, Icons.Default.ContentCopy, "Copy reply")
                        MessageAction(
                            onSpeakToggle,
                            Icons.AutoMirrored.Filled.VolumeUp,
                            if (isSpeaking) "Stop reading" else "Read reply aloud",
                            active = isSpeaking
                        )
                        if (isLastAssistant && !isGenerating) {
                            MessageAction(onRegenerate, Icons.Default.Refresh, "Regenerate reply")
                        }
                    } else if (message.content.isNotBlank()) {
                        MessageAction(onCopy, Icons.Default.ContentCopy, "Copy message")
                        if (!isGenerating) {
                            MessageAction(onEditMessage, Icons.Default.Edit, "Edit & regenerate")
                            MessageAction(onDeleteMessage, Icons.Default.Delete, "Delete message")
                        }
                    }
                }

                if (!isUser && isLastAssistant && !isGenerating && message.content.isNotBlank()) {
                    FlowRow(
                        modifier = Modifier.padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(
                            "Explain simpler",
                            "Give a concrete example",
                            "What should I do next?"
                        ).forEach { suggestion ->
                            AssistChip(
                                onClick = { onAskFollowUp(suggestion) },
                                label = { Text(suggestion, fontSize = 11.sp) }
                            )
                        }
                    }
                }
            }
        }

        if (isUser) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = "You",
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun MessageAction(
    onClick: () -> Unit,
    icon: ImageVector,
    description: String,
    active: Boolean = false
) {
    IconButton(onClick = onClick, modifier = Modifier.size(28.dp)) {
        Icon(
            icon,
            contentDescription = description,
            modifier = Modifier.size(14.dp),
            tint = if (active) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun OmniChatDrawer(
    sessions: List<ChatSessionEntity>,
    activeSessionId: String?,
    currentSection: AppSection,
    onNewChat: () -> Unit,
    onSelectSession: (String) -> Unit,
    onRenameSession: (ChatSessionEntity) -> Unit,
    onDeleteSession: (ChatSessionEntity) -> Unit,
    onNavigate: (AppSection) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenThemes: () -> Unit,
    onOpenUsage: () -> Unit
) {
    ModalDrawerSheet {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            contentPadding = PaddingValues(top = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item {
                Text(
                    text = "OmniChat",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                Text(
                    text = "Multi-provider AI workspace",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            item {
                Button(
                    onClick = onNewChat,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                        .testTag("new_chat_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("New Chat")
                }
            }

            item { DrawerSectionLabel("Recent Chats") }

            if (sessions.isEmpty()) {
                item {
                    Text(
                        text = "No chats yet — start a conversation.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            items(sessions, key = { it.id }) { session ->
                SessionDrawerRow(
                    session = session,
                    isActive = session.id == activeSessionId,
                    onSelect = { onSelectSession(session.id) },
                    onRename = { onRenameSession(session) },
                    onDelete = { onDeleteSession(session) }
                )
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
                DrawerSectionLabel("Workspace")
            }

            item {
                DrawerNavRow(Icons.AutoMirrored.Filled.Chat, "Chat", currentSection == AppSection.CHAT) {
                    onNavigate(AppSection.CHAT)
                }
            }
            item {
                DrawerNavRow(Icons.Default.Widgets, "Intel Hub & Productivity", currentSection == AppSection.INTEL_HUB) {
                    onNavigate(AppSection.INTEL_HUB)
                }
            }
            item {
                DrawerNavRow(Icons.Default.Palette, "Creative Studio", currentSection == AppSection.CREATIVE_STUDIO) {
                    onNavigate(AppSection.CREATIVE_STUDIO)
                }
            }
            item {
                DrawerNavRow(Icons.Default.Code, "Code Studio", currentSection == AppSection.CODE_STUDIO) {
                    onNavigate(AppSection.CODE_STUDIO)
                }
            }
            item {
                DrawerNavRow(Icons.Default.Description, "Markdown Studio", currentSection == AppSection.MARKDOWN_STUDIO) {
                    onNavigate(AppSection.MARKDOWN_STUDIO)
                }
            }
            item {
                DrawerNavRow(Icons.Default.PlayCircle, "Media Player", currentSection == AppSection.MEDIA_PLAYER) {
                    onNavigate(AppSection.MEDIA_PLAYER)
                }
            }
            item {
                DrawerNavRow(
                    Icons.AutoMirrored.Filled.MenuBook,
                    "Knowledge Base (RAG)",
                    currentSection == AppSection.RAG_KNOWLEDGE
                ) {
                    onNavigate(AppSection.RAG_KNOWLEDGE)
                }
            }
            item {
                DrawerNavRow(Icons.Default.Search, "Search Chats", currentSection == AppSection.CHAT_SEARCH) {
                    onNavigate(AppSection.CHAT_SEARCH)
                }
            }
            item {
                DrawerNavRow(Icons.Default.Psychology, "AI Memory", currentSection == AppSection.AI_MEMORY) {
                    onNavigate(AppSection.AI_MEMORY)
                }
            }
            item {
                DrawerNavRow(Icons.Default.Bookmarks, "Prompt Library", currentSection == AppSection.PROMPT_LIBRARY) {
                    onNavigate(AppSection.PROMPT_LIBRARY)
                }
            }
            item {
                DrawerNavRow(Icons.Default.Timer, "Focus & Habits", currentSection == AppSection.FOCUS_HABITS) {
                    onNavigate(AppSection.FOCUS_HABITS)
                }
            }
            item {
                DrawerNavRow(
                    Icons.Default.SettingsBackupRestore,
                    "Backup & Restore",
                    currentSection == AppSection.BACKUP_RESTORE
                ) {
                    onNavigate(AppSection.BACKUP_RESTORE)
                }
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
                DrawerSectionLabel("Preferences")
            }
            item { DrawerNavRow(Icons.Default.Settings, "Settings & API Keys", false, onOpenSettings) }
            item { DrawerNavRow(Icons.Default.Palette, "Themes & Appearance", false, onOpenThemes) }
            item { DrawerNavRow(Icons.Default.Speed, "Usage & Limits", false, onOpenUsage) }
        }
    }
}

@Composable
private fun DrawerSectionLabel(text: String) {
    Text(
        text = text.uppercase(Locale.getDefault()),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 12.dp, top = 12.dp, bottom = 4.dp)
    )
}

@Composable
private fun SessionDrawerRow(
    session: ChatSessionEntity,
    isActive: Boolean,
    onSelect: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    val contentColor = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer
    else MaterialTheme.colorScheme.onSurface

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isActive) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        contentColor = contentColor,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("session_row_${session.id}")
    ) {
        Row(
            modifier = Modifier
                .clickable(onClick = onSelect)
                .padding(start = 12.dp, end = 2.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.AutoMirrored.Filled.Chat,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.title.ifBlank { "New Chat" },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = DateUtils.getRelativeTimeSpanString(session.updatedAt).toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(alpha = 0.7f)
                )
            }
            IconButton(onClick = onRename, modifier = Modifier.size(30.dp)) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Rename chat",
                    modifier = Modifier.size(14.dp),
                    tint = contentColor.copy(alpha = 0.7f)
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete chat",
                    modifier = Modifier.size(14.dp),
                    tint = contentColor.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun DrawerNavRow(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
        contentColor = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
        else MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AttachmentInspectorDialog(
    attachment: ChatAttachment,
    onDismiss: () -> Unit,
    onOpenCode: (code: String, filename: String, language: String) -> Unit,
    onOpenMarkdown: (content: String, filename: String) -> Unit
) {
    val isImage = attachment.mimeType.startsWith("image/")
    val isText = attachment.content.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(attachment.name, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "${attachment.mimeType} • ${FileUtils.formatFileSize(attachment.sizeBytes)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )

                if (isImage) {
                    val file = File(attachment.localUri)
                    if (file.exists()) {
                        AsyncImage(
                            model = file,
                            contentDescription = attachment.name,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 260.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                    } else {
                        Text(
                            text = "Image preview is unavailable offline.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    ) {
                        Text(
                            text = attachment.content.ifBlank { "[Binary or empty attachment]" },
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(10.dp)
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            onOpenCode(
                                attachment.content,
                                codeFileName(attachment.name, detectLanguage(attachment.name)),
                                detectLanguage(attachment.name)
                            )
                        },
                        enabled = isText,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Code Studio", fontSize = 11.sp)
                    }
                    Button(
                        onClick = { onOpenMarkdown(attachment.content, attachment.name) },
                        enabled = isText,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Markdown", fontSize = 11.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )
}

private fun ChatAttachment.toAttachmentInfo(): AttachmentInfo = AttachmentInfo(
    id = id,
    name = name,
    mimeType = mimeType,
    sizeBytes = sizeBytes,
    localUri = localUri,
    isImage = mimeType.startsWith("image/"),
    previewSnippet = if (!mimeType.startsWith("image/")) content.take(300) else null,
    content = content
)

private fun detectLanguage(fileName: String): String = when (fileName.substringAfterLast('.', "").lowercase()) {
    "kt", "kts" -> "kotlin"
    "py" -> "python"
    "js" -> "javascript"
    "ts" -> "typescript"
    "java" -> "java"
    "cpp", "cc", "h" -> "cpp"
    "rs" -> "rust"
    "go" -> "go"
    "html", "htm" -> "html"
    "css" -> "css"
    "json" -> "json"
    "md" -> "markdown"
    "sql" -> "sql"
    "sh" -> "bash"
    "xml" -> "xml"
    else -> "text"
}

private fun codeFileName(title: String, language: String): String {
    val cleaned = title.trim()
        .ifBlank { "snippet" }
        .replace(Regex("[^A-Za-z0-9._-]"), "_")
    if (cleaned.contains('.')) return cleaned
    val extension = when (language.lowercase()) {
        "kotlin" -> "kt"
        "python" -> "py"
        "javascript" -> "js"
        "typescript" -> "ts"
        "java" -> "java"
        "cpp" -> "cpp"
        "rust" -> "rs"
        "go" -> "go"
        "html" -> "html"
        "css" -> "css"
        "json" -> "json"
        "markdown" -> "md"
        "sql" -> "sql"
        "bash" -> "sh"
        "xml" -> "xml"
        else -> "txt"
    }
    return "$cleaned.$extension"
}
