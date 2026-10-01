package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ProviderType
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(viewModel: ChatViewModel) {
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val listState = rememberLazyListState()

    val currentSession by viewModel.currentSession.collectAsState()
    val sessions by viewModel.sessions.collectAsState()
    val messages by viewModel.currentMessages.collectAsState()
    val pendingAttachments by viewModel.pendingAttachments.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val selectedProvider by viewModel.selectedProvider.collectAsState()
    val selectedModel by viewModel.selectedModel.collectAsState()
    val speakingMessageId by viewModel.speakingMessageId.collectAsState()

    val currentTheme by viewModel.selectedTheme.collectAsState()
    val currentThemeMode by viewModel.themeMode.collectAsState()
    val keysRevision by viewModel.keysRevision.collectAsState()
    val usageStats by viewModel.usageStats.collectAsState()
    val currentSessionArtifacts by viewModel.currentSessionArtifacts.collectAsState()
    val allArtifacts by viewModel.allArtifacts.collectAsState()
    val selectedArtifact by viewModel.selectedArtifact.collectAsState()
    val currentSection by viewModel.currentSection.collectAsState()
    val isWebSearchEnabled by viewModel.isWebSearchEnabled.collectAsState()

    var inputText by remember { mutableStateOf("") }
    var showProviderSheet by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showQuickKeyDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showUsageLimitsSheet by remember { mutableStateOf(false) }
    var showArtifactsSheet by remember { mutableStateOf(false) }
    var showAttachmentMenu by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Activity result launcher for Photo Picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 5)
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.addAttachments(uris)
        }
    }

    // Activity result launcher for Documents / Code / PDF
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.addAttachments(uris)
        }
    }

    // Auto-scroll to bottom on new message
    LaunchedEffect(messages.size, isGenerating) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    BackHandler(enabled = drawerState.isOpen) {
        coroutineScope.launch { drawerState.close() }
    }

    BackHandler(enabled = !drawerState.isOpen && currentSection != AppSection.CHAT) {
        viewModel.navigateToSection(AppSection.CHAT)
    }

    val isCurrentKeyConfigured = remember(selectedProvider, keysRevision) {
        viewModel.isKeyConfigured(selectedProvider)
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(310.dp).testTag("navigation_drawer")
            ) {
                // Drawer Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "OmniChat",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Multi-Provider AI Assistant",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                // New Chat Button
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .clickable {
                            viewModel.createNewSession()
                            coroutineScope.launch { drawerState.close() }
                        }
                        .testTag("drawer_new_chat_button")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Start New Chat",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                // Chat History List
                Text(
                    text = "Recent Chats",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(sessions) { session ->
                        val isSelected = currentSession?.id == session.id
                        val dateFormatted = remember(session.updatedAt) {
                            SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(session.updatedAt))
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.selectSession(session.id)
                                    coroutineScope.launch { drawerState.close() }
                                }
                                .testTag("session_item_${session.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Chat,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = session.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = dateFormatted,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                IconButton(
                                    onClick = { viewModel.deleteSession(session.id) },
                                    modifier = Modifier.size(28.dp).testTag("delete_session_${session.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete chat",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                // Drawer Quick Navigation Items
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Choose AI Provider & Model
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                coroutineScope.launch { drawerState.close() }
                                showProviderSheet = true
                            }
                            .testTag("drawer_change_provider_button"),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("AI Provider & Model", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "${selectedProvider.displayName} • ${selectedModel.name}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Text("Switch", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    // Free Maps & Travel
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                coroutineScope.launch { drawerState.close() }
                                viewModel.navigateToSection(AppSection.MAPS)
                            }
                            .testTag("drawer_maps_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Explore, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Free Maps & Travel", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text("OpenStreetMap & AI itineraries", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    // YouTube Player & Search
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                coroutineScope.launch { drawerState.close() }
                                viewModel.navigateToSection(AppSection.YOUTUBE)
                            }
                            .testTag("drawer_youtube_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.VideoLibrary, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("YouTube Search & Player", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text("Play videos & AI summaries", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    // AI Persona (Feed AI How To Act)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                coroutineScope.launch { drawerState.close() }
                                viewModel.navigateToSection(AppSection.PERSONA)
                            }
                            .testTag("drawer_persona_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.tertiary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("AI Persona & Behavior", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text("Feed AI how to act & talk only to you", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    // AI Learning Academy
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                coroutineScope.launch { drawerState.close() }
                                viewModel.navigateToSection(AppSection.LEARN)
                            }
                            .testTag("drawer_learn_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.secondary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("AI Learning Academy", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text("Master LLMs, Prompting & Agents", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    // AI Studio (Image, Video, Sound)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                coroutineScope.launch { drawerState.close() }
                                viewModel.navigateToSection(AppSection.STUDIO)
                            }
                            .testTag("drawer_studio_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Palette, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("AI Creation Studio", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text("Images, Video Motion & SFX", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    // Live News
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                coroutineScope.launch { drawerState.close() }
                                viewModel.navigateToSection(AppSection.NEWS)
                            }
                            .testTag("drawer_news_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Newspaper, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Live News & Feed", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text("Realtime global news & AI summaries", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    // Web Search
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                coroutineScope.launch { drawerState.close() }
                                viewModel.navigateToSection(AppSection.SEARCH)
                            }
                            .testTag("drawer_search_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.secondary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Live Web Search", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text("Instant search & grounded AI answers", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    // Read Aloud
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                coroutineScope.launch { drawerState.close() }
                                viewModel.navigateToSection(AppSection.READ_ALOUD)
                            }
                            .testTag("drawer_read_aloud_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.RecordVoiceOver, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Read Aloud Narrator", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text("Voice synthesis for any text or file", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    // Artifacts Menu
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                coroutineScope.launch { drawerState.close() }
                                showArtifactsSheet = true
                            }
                            .testTag("drawer_artifacts_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Widgets, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Artifacts Menu", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = if (allArtifacts.isEmpty()) "No artifacts yet" else "${allArtifacts.size} artifacts generated",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (allArtifacts.isNotEmpty()) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = "${allArtifacts.size}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Usage & Rate Limits
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                coroutineScope.launch { drawerState.close() }
                                showUsageLimitsSheet = true
                            }
                            .testTag("drawer_limits_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.tertiary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Usage & Limits", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = if (usageStats.dailyLimit <= 0) "${usageStats.requestsToday} sent today • Unlimited" else "${usageStats.requestsToday}/${usageStats.dailyLimit} today (${usageStats.remainingToday} left)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // API Keys & Backup
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                coroutineScope.launch { drawerState.close() }
                                showQuickKeyDialog = true
                            }
                            .testTag("drawer_quick_keys_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("API Keys & Backup", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    // Themes & Styling
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                coroutineScope.launch { drawerState.close() }
                                showThemeDialog = true
                            }
                            .testTag("drawer_themes_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.ColorLens, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.secondary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Themes & Colors", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    // Settings
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                coroutineScope.launch { drawerState.close() }
                                showSettingsDialog = true
                            }
                            .testTag("drawer_settings_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Full Settings", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                Column {
                    TopAppBar(
                        title = {
                            if (currentSection == AppSection.CHAT) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Quick Model Switcher Button in Header
                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier
                                            .weight(1f, fill = false)
                                            .clickable { showProviderSheet = true }
                                            .testTag("provider_dropdown_trigger")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = when (selectedProvider) {
                                                    ProviderType.POLLINATIONS -> "🆓 ${selectedModel.name}"
                                                    ProviderType.GEMINI -> "⚡ ${selectedModel.name}"
                                                    ProviderType.GROQ -> "🚀 ${selectedModel.name}"
                                                    ProviderType.CEREBRAS -> "⚡ ${selectedModel.name}"
                                                    ProviderType.OPENROUTER -> "🌐 ${selectedModel.name}"
                                                    ProviderType.HUGGINGFACE -> "🤗 ${selectedModel.name}"
                                                    ProviderType.CUSTOM -> "⚙️ ${selectedModel.name}"
                                                },
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Icon(
                                                imageVector = Icons.Default.KeyboardArrowDown,
                                                contentDescription = "Switch provider",
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(6.dp))

                                    // Realtime Limit Pill Badge
                                    val isUnlimited = usageStats.dailyLimit <= 0
                                    val isNearCap = !isUnlimited && usageStats.remainingToday <= 5
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isNearCap) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                                        modifier = Modifier
                                            .clickable { showUsageLimitsSheet = true }
                                            .testTag("top_bar_limit_chip")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Speed,
                                                contentDescription = null,
                                                modifier = Modifier.size(13.dp),
                                                tint = if (isNearCap) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = if (isUnlimited) "∞" else "${usageStats.remainingToday} left",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isNearCap) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            } else {
                                Text(
                                    text = when (currentSection) {
                                        AppSection.STUDIO -> "AI Creation Studio"
                                        AppSection.MAPS -> "Free Maps & Travel"
                                        AppSection.YOUTUBE -> "YouTube Search & Player"
                                        AppSection.INTEL_HUB, AppSection.PERSONA -> "AI Persona & Behavior"
                                        AppSection.LEARN -> "AI Learning Academy"
                                        AppSection.NEWS -> "Live News & Intelligence"
                                        AppSection.SEARCH -> "Live Web Search"
                                        AppSection.READ_ALOUD -> "Read Aloud Narrator"
                                        else -> "OmniChat AI"
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        },
                        navigationIcon = {
                            if (currentSection == AppSection.CHAT) {
                                IconButton(
                                    onClick = { coroutineScope.launch { drawerState.open() } },
                                    modifier = Modifier.testTag("open_drawer_button")
                                ) {
                                    Icon(imageVector = Icons.Default.Menu, contentDescription = "Open chat history")
                                }
                            } else {
                                IconButton(
                                    onClick = { viewModel.navigateToSection(AppSection.CHAT) },
                                    modifier = Modifier.testTag("back_to_chat_button")
                                ) {
                                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Chat")
                                }
                            }
                        },
                        actions = {
                            // Artifacts Menu Button with Dynamic Counter Badge
                            IconButton(
                                onClick = { showArtifactsSheet = true },
                                modifier = Modifier.testTag("top_bar_artifacts_button")
                            ) {
                                BadgedBox(
                                    badge = {
                                        if (currentSessionArtifacts.isNotEmpty()) {
                                            Badge(
                                                containerColor = MaterialTheme.colorScheme.primary,
                                                contentColor = MaterialTheme.colorScheme.onPrimary
                                            ) {
                                                Text("${currentSessionArtifacts.size}")
                                            }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Widgets,
                                        contentDescription = "Artifacts Menu (${currentSessionArtifacts.size})",
                                        tint = if (currentSessionArtifacts.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Direct Usage & Limits Button
                            IconButton(
                                onClick = { showUsageLimitsSheet = true },
                                modifier = Modifier.testTag("top_bar_limits_button")
                            ) {
                                Icon(imageVector = Icons.Default.BarChart, contentDescription = "Usage Limits")
                            }

                            // Direct Theme Selector Button in Top Bar
                            IconButton(
                                onClick = { showThemeDialog = true },
                                modifier = Modifier.testTag("top_bar_theme_button")
                            ) {
                                Icon(imageVector = Icons.Default.ColorLens, contentDescription = "Themes")
                            }

                            // Direct Key Manager Button in Top Bar
                            IconButton(
                                onClick = { showQuickKeyDialog = true },
                                modifier = Modifier.testTag("top_bar_keys_button")
                            ) {
                                Icon(imageVector = Icons.Default.Key, contentDescription = "API Keys")
                            }

                            IconButton(
                                onClick = { viewModel.createNewSession() },
                                modifier = Modifier.testTag("top_bar_new_chat_button")
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = "New chat")
                            }
                            IconButton(
                                onClick = { showSettingsDialog = true },
                                modifier = Modifier.testTag("top_bar_settings_button")
                            ) {
                                Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )

                    // Missing Key Warning Banner with 1-tap Paste shortcut
                    if (selectedProvider.requiresApiKey && !isCurrentKeyConfigured) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showQuickKeyDialog = true }
                                .testTag("missing_key_top_banner")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Key,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${selectedProvider.displayName} key required • Tap to Paste Key 📋",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            },
            bottomBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    if (currentSection == AppSection.CHAT) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            // Quick Web Search Grounding toggle chip & Active Model
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FilterChip(
                                    selected = isWebSearchEnabled,
                                    onClick = { viewModel.toggleWebSearch() },
                                    label = {
                                        Text(
                                            if (isWebSearchEnabled) "🌐 Web Search: ON" else "🌐 Search Web",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isWebSearchEnabled) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    ),
                                    modifier = Modifier.testTag("toggle_web_search_button")
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.clickable { showProviderSheet = true }
                                ) {
                                    Text(
                                        text = "${selectedProvider.displayName} • ${selectedModel.name}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                    )
                                }
                            }

                            // Pending Attachments Strip
                            AnimatedVisibility(
                                visible = pendingAttachments.isNotEmpty(),
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 8.dp)
                                ) {
                                    items(pendingAttachments) { att ->
                                        AttachmentBadge(
                                            attachment = att,
                                            onRemove = { viewModel.removePendingAttachment(att.id) }
                                        )
                                    }
                                }
                            }

                            // Input Text Field & Action Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Bottom
                            ) {
                                // Attachment Plus Button with Dropdown
                                Box {
                                    IconButton(
                                        onClick = { showAttachmentMenu = true },
                                        modifier = Modifier
                                            .padding(bottom = 4.dp)
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                            .testTag("attach_file_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AttachFile,
                                            contentDescription = "Attach file",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    DropdownMenu(
                                        expanded = showAttachmentMenu,
                                        onDismissRequest = { showAttachmentMenu = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Upload Images / Photos") },
                                            leadingIcon = { Icon(Icons.Default.Image, contentDescription = null) },
                                            onClick = {
                                                showAttachmentMenu = false
                                                photoPickerLauncher.launch(
                                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                )
                                            },
                                            modifier = Modifier.testTag("pick_images_option")
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Upload Document / Code / PDF") },
                                            leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) },
                                            onClick = {
                                                showAttachmentMenu = false
                                                documentPickerLauncher.launch(arrayOf("*/*"))
                                            },
                                            modifier = Modifier.testTag("pick_documents_option")
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // Text input field
                                OutlinedTextField(
                                    value = inputText,
                                    onValueChange = { inputText = it },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("chat_input_field"),
                                    placeholder = {
                                        Text(
                                            text = if (pendingAttachments.isEmpty()) "Ask anything or upload a file..." else "Add a message or query...",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    },
                                    shape = RoundedCornerShape(24.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    maxLines = 5
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                // Send or Stop Button
                                if (isGenerating) {
                                    IconButton(
                                        onClick = { viewModel.stopGeneration() },
                                        modifier = Modifier
                                            .padding(bottom = 4.dp)
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.errorContainer)
                                            .testTag("stop_generation_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Stop,
                                            contentDescription = "Stop generating",
                                            tint = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                } else {
                                    val canSend = inputText.isNotBlank() || pendingAttachments.isNotEmpty()
                                    IconButton(
                                        onClick = {
                                            val text = inputText
                                            inputText = ""
                                            viewModel.sendMessage(text)
                                        },
                                        enabled = canSend,
                                        modifier = Modifier
                                            .padding(bottom = 4.dp)
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (canSend) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                            )
                                            .testTag("send_message_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Send,
                                            contentDescription = "Send message",
                                            tint = if (canSend) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Global Navigation Bar across all sections
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        tonalElevation = 6.dp,
                        modifier = Modifier.testTag("global_bottom_navigation")
                    ) {
                        NavigationBarItem(
                            selected = currentSection == AppSection.CHAT,
                            onClick = { viewModel.navigateToSection(AppSection.CHAT) },
                            icon = { Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Chat") },
                            label = { Text("Chat") },
                            modifier = Modifier.testTag("nav_item_chat")
                        )
                        NavigationBarItem(
                            selected = currentSection == AppSection.STUDIO,
                            onClick = { viewModel.navigateToSection(AppSection.STUDIO) },
                            icon = { Icon(Icons.Default.Palette, contentDescription = "Studio") },
                            label = { Text("Studio") },
                            modifier = Modifier.testTag("nav_item_studio")
                        )
                        NavigationBarItem(
                            selected = currentSection == AppSection.MAPS,
                            onClick = { viewModel.navigateToSection(AppSection.MAPS) },
                            icon = { Icon(Icons.Default.Explore, contentDescription = "Maps") },
                            label = { Text("Maps") },
                            modifier = Modifier.testTag("nav_item_maps")
                        )
                        NavigationBarItem(
                            selected = currentSection == AppSection.YOUTUBE,
                            onClick = { viewModel.navigateToSection(AppSection.YOUTUBE) },
                            icon = { Icon(Icons.Default.VideoLibrary, contentDescription = "YouTube") },
                            label = { Text("YouTube") },
                            modifier = Modifier.testTag("nav_item_youtube")
                        )
                        val isHubSelected = currentSection == AppSection.INTEL_HUB ||
                                currentSection == AppSection.PERSONA ||
                                currentSection == AppSection.LEARN ||
                                currentSection == AppSection.NEWS ||
                                currentSection == AppSection.SEARCH ||
                                currentSection == AppSection.READ_ALOUD
                        NavigationBarItem(
                            selected = isHubSelected,
                            onClick = { viewModel.navigateToSection(AppSection.INTEL_HUB) },
                            icon = { Icon(Icons.Default.Psychology, contentDescription = "Hub") },
                            label = { Text("Hub") },
                            modifier = Modifier.testTag("nav_item_hub")
                        )
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when (currentSection) {
                    AppSection.CHAT -> {
                        if (messages.isEmpty()) {
                            // Empty State Screen
                            EmptyChatGreeting(
                                selectedModelName = selectedModel.name,
                                providerName = selectedProvider.displayName,
                                onSuggestionClick = { prompt ->
                                    viewModel.sendMessage(prompt)
                                },
                                onUploadClick = {
                                    documentPickerLauncher.launch(arrayOf("*/*"))
                                }
                            )
                        } else {
                            // Chat Messages LazyColumn
                            LazyColumn(
                                state = listState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("messages_list"),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(messages, key = { it.id }) { msg ->
                                    val attList = remember(msg.attachmentsJson) {
                                        viewModel.repository.parseAttachments(msg.attachmentsJson)
                                    }
                                    val isSpeaking = speakingMessageId == msg.id

                                    ChatMessageItem(
                                        message = msg,
                                        attachments = attList,
                                        isSpeaking = isSpeaking,
                                        onSpeakToggle = { viewModel.toggleSpeech(msg.id, msg.content) },
                                        onRegenerate = { viewModel.regenerateMessage(msg) },
                                        onOpenSettings = { showQuickKeyDialog = true },
                                        onOpenArtifact = { art -> viewModel.selectArtifact(art) }
                                    )
                                }

                                if (isGenerating) {
                                    item {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 20.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(18.dp),
                                                strokeWidth = 2.dp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = "${selectedProvider.displayName} is thinking...",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    AppSection.STUDIO -> {
                        StudioScreen(
                            onSendPromptToChat = { prompt ->
                                viewModel.sendMessage(prompt)
                                viewModel.navigateToSection(AppSection.CHAT)
                            }
                        )
                    }

                    AppSection.MAPS -> {
                        MapExplorerScreen(
                            onSendLocationToChat = { prompt ->
                                viewModel.sendMessage(prompt)
                                viewModel.navigateToSection(AppSection.CHAT)
                            }
                        )
                    }

                    AppSection.YOUTUBE -> {
                        YouTubeScreen(
                            preferencesManager = viewModel.preferencesManager,
                            onOpenSettings = { showQuickKeyDialog = true },
                            onSummarizeVideoInChat = { prompt ->
                                viewModel.sendMessage(prompt)
                                viewModel.navigateToSection(AppSection.CHAT)
                            }
                        )
                    }

                    AppSection.INTEL_HUB, AppSection.PERSONA, AppSection.LEARN, AppSection.NEWS, AppSection.SEARCH, AppSection.READ_ALOUD -> {
                        val initialTab = when (currentSection) {
                            AppSection.PERSONA -> 0
                            AppSection.LEARN -> 1
                            AppSection.NEWS -> 2
                            AppSection.SEARCH -> 3
                            AppSection.READ_ALOUD -> 4
                            else -> 0
                        }
                        IntelHubScreen(
                            initialTab = initialTab,
                            preferencesManager = viewModel.preferencesManager,
                            onSendPromptToChat = { prompt ->
                                viewModel.sendMessage(prompt)
                                viewModel.navigateToSection(AppSection.CHAT)
                            }
                        )
                    }
                }
            }
        }
    }

    // Provider & Model Selector Bottom Sheet
    if (showProviderSheet) {
        ProviderSelectorSheet(
            sheetState = sheetState,
            selectedProvider = selectedProvider,
            selectedModel = selectedModel,
            isKeyConfigured = { viewModel.isKeyConfigured(it) },
            getKeyForProvider = { viewModel.getApiKeyForProvider(it) },
            onUpdateKey = { prov, key -> viewModel.updateApiKey(prov, key) },
            onModelSelected = { prov, mod ->
                viewModel.setProviderAndModel(prov, mod)
            },
            onOpenSettings = { showSettingsDialog = true },
            onDismiss = { showProviderSheet = false }
        )
    }

    // Settings & API Keys Dialog
    if (showSettingsDialog) {
        SettingsDialog(
            preferencesManager = viewModel.preferencesManager,
            onOpenThemePicker = {
                showSettingsDialog = false
                showThemeDialog = true
            },
            onOpenUsageLimits = {
                showSettingsDialog = false
                showUsageLimitsSheet = true
            },
            onDismiss = { showSettingsDialog = false },
            onClearAllChats = { viewModel.clearAllChats() }
        )
    }

    // Quick Key Setup & Backup Dialog
    if (showQuickKeyDialog) {
        QuickKeyDialog(
            preferencesManager = viewModel.preferencesManager,
            initialTargetProvider = selectedProvider,
            onKeyUpdated = { /* viewModel state automatically triggers recomposition */ },
            onDismiss = { showQuickKeyDialog = false }
        )
    }

    // Themes & Styling Dialog
    if (showThemeDialog) {
        ThemeSelectorDialog(
            currentTheme = currentTheme,
            currentThemeMode = currentThemeMode,
            onThemeSelected = { viewModel.setTheme(it) },
            onThemeModeSelected = { viewModel.setThemeMode(it) },
            onDismiss = { showThemeDialog = false }
        )
    }

    // Usage & Rate Limits Sheet
    if (showUsageLimitsSheet) {
        val limitsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        UsageLimitsSheet(
            sheetState = limitsSheetState,
            usageStats = usageStats,
            onSetDailyLimit = { viewModel.setDailyLimit(it) },
            onResetTodayStats = { viewModel.resetTodayUsage() },
            onDismiss = { showUsageLimitsSheet = false }
        )
    }

    // Artifacts Menu Bottom Sheet
    if (showArtifactsSheet) {
        val artifactsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ArtifactsSheet(
            sheetState = artifactsSheetState,
            currentSessionArtifacts = currentSessionArtifacts,
            allArtifacts = allArtifacts,
            onSelectArtifact = { art ->
                showArtifactsSheet = false
                viewModel.selectArtifact(art)
            },
            onPromptSuggestion = { suggestion ->
                showArtifactsSheet = false
                viewModel.sendMessage(suggestion)
            },
            onDismiss = { showArtifactsSheet = false }
        )
    }

    // Full Artifact Viewer & Interactive Live Preview Dialog
    selectedArtifact?.let { artifact ->
        ArtifactViewerDialog(
            artifact = artifact,
            onDismiss = { viewModel.selectArtifact(null) }
        )
    }
}

@Composable
fun EmptyChatGreeting(
    selectedModelName: String,
    providerName: String,
    onSuggestionClick: (String) -> Unit,
    onUploadClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(68.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Welcome to OmniChat",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Powered by $providerName • $selectedModelName",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "Quick suggestions to start:",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        val suggestions = listOf(
            "📄 Summarize an uploaded document or code file",
            "💻 Write a modern Jetpack Compose component in Kotlin",
            "🧠 Explain how large language models work simply",
            "💡 Give me 5 creative ideas for an Android application"
        )

        for (suggestion in suggestions) {
            Card(
                onClick = {
                    if (suggestion.startsWith("📄")) {
                        onUploadClick()
                    } else {
                        onSuggestionClick(suggestion.substring(2).trim())
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            ) {
                Text(
                    text = suggestion,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(14.dp)
                )
            }
        }
    }
}
