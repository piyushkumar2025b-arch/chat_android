package com.example.ui

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.OndemandVideo
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

data class HubToolItem(
    val id: Int,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val category: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntelHubScreen(
    viewModel: ChatViewModel,
    onBackToChat: () -> Unit,
    onSendPromptToChat: (String) -> Unit = {},
    onSendImageToChat: (Bitmap) -> Unit = {},
    onApplyApiKey: (String) -> Unit = {},
    onOpenSettings: () -> Unit = {}
) {
    var selectedHubTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }

    BackHandler(enabled = selectedHubTab != 0) {
        selectedHubTab = 0
    }

    val categories = remember { listOf("All", "Write", "Search", "Media", "Daily") }

    val tools = remember {
        listOf(
            HubToolItem(1, "Notes", "Write and organize your notes", Icons.AutoMirrored.Filled.Note, "Write"),
            HubToolItem(13, "My Docs", "Save files so AI can answer from them", Icons.AutoMirrored.Filled.MenuBook, "Write"),
            HubToolItem(21, "Saved Prompts", "Quick reusable message templates", Icons.Default.Bookmarks, "Write"),
            HubToolItem(20, "AI Memory", "Facts the AI remembers for you", Icons.Default.Psychology, "Write"),
            HubToolItem(14, "AI Style", "Choose how the AI talks to you", Icons.Default.Face, "Write"),
            HubToolItem(5, "Web Search", "Search the web and ask AI about it", Icons.Default.Search, "Search"),
            HubToolItem(6, "News", "Today's top tech and AI headlines", Icons.Default.Newspaper, "Search"),
            HubToolItem(16, "YouTube", "Find videos and get fast summaries", Icons.Default.OndemandVideo, "Search"),
            HubToolItem(17, "Maps", "Look up places and plan routes", Icons.Default.Map, "Search"),
            HubToolItem(15, "Learn AI", "Short, easy guides on how AI works", Icons.Default.School, "Search"),
            HubToolItem(12, "Create Studio", "Make images, music, code, and video", Icons.Default.Palette, "Media"),
            HubToolItem(10, "Draw & Edit", "Sketch or edit photos for chat", Icons.Default.Image, "Media"),
            HubToolItem(8, "Media Player", "Play audio files and relaxing tracks", Icons.Default.PlayCircle, "Media"),
            HubToolItem(9, "Read Aloud", "Listen to text read out loud", Icons.Default.RecordVoiceOver, "Media"),
            HubToolItem(2, "Calendar", "Plan events and daily schedules", Icons.Default.CalendarMonth, "Daily"),
            HubToolItem(19, "Focus & Habits", "Focus timer and daily habit streaks", Icons.Default.Timer, "Daily"),
            HubToolItem(4, "Calculator", "Math calculator and unit converter", Icons.Default.Calculate, "Daily"),
            HubToolItem(7, "Step Counter", "Track steps and daily activity", Icons.AutoMirrored.Filled.DirectionsRun, "Daily"),
            HubToolItem(3, "Passwords & Keys", "Keep logins and keys locked safely", Icons.Default.Key, "Daily"),
            HubToolItem(18, "Alerts", "Manage reminders and notifications", Icons.Default.Notifications, "Daily"),
            HubToolItem(11, "2048 Game", "Classic number puzzle game", Icons.Default.SportsEsports, "Daily")
        )
    }

    val visibleTools = remember(searchQuery, selectedCategory, tools) {
        val q = searchQuery.trim().lowercase()
        tools.filter { tool ->
            val catOk = selectedCategory == "All" || tool.category == selectedCategory
            val queryOk = q.isEmpty() ||
                tool.title.lowercase().contains(q) ||
                tool.subtitle.lowercase().contains(q)
            catOk && queryOk
        }
    }

    when (selectedHubTab) {
        0 -> {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                topBar = {
                    Column {
                        TopAppBar(
                            title = {
                                Column {
                                    Text("All Tools", fontWeight = FontWeight.SemiBold)
                                    Text(
                                        "Pick a tool to open or send to chat",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            navigationIcon = {
                                IconButton(
                                    onClick = onBackToChat,
                                    modifier = Modifier.testTag("hub_back_to_chat")
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Chat")
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp)
                ) {
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search tools…") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear search")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categories.forEach { category ->
                            FilterChip(
                                selected = selectedCategory == category,
                                onClick = { selectedCategory = category },
                                label = { Text(category, maxLines = 1) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (visibleTools.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No tools match \"$searchQuery\"",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 24.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(visibleTools, key = { it.id }) { item ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedHubTab = item.id }
                                        .testTag("hub_tool_${item.id}"),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                    ),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(MaterialTheme.colorScheme.surfaceContainer),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                item.icon,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text(
                                                text = item.title,
                                                fontWeight = FontWeight.SemiBold,
                                                style = MaterialTheme.typography.titleSmall,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = item.subtitle,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
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
        1 -> NotesScreen(
            productivityDao = viewModel.productivityDao,
            onBack = { selectedHubTab = 0 },
            onSendNoteToChat = onSendPromptToChat
        )
        2 -> CalendarScreen(
            productivityDao = viewModel.productivityDao,
            onBack = { selectedHubTab = 0 },
            onSendScheduleToChat = onSendPromptToChat
        )
        3 -> PasswordSaverScreen(
            productivityDao = viewModel.productivityDao,
            onBack = { selectedHubTab = 0 },
            onApplyApiKey = onApplyApiKey
        )
        4 -> CalculatorScreen(
            onBack = { selectedHubTab = 0 },
            onSendResultToChat = onSendPromptToChat
        )
        5 -> WebSearchScreen(
            onBack = { selectedHubTab = 0 },
            onSendResultsToChat = onSendPromptToChat
        )
        6 -> NewsScreen(
            onBack = { selectedHubTab = 0 },
            onSendNewsToChat = onSendPromptToChat
        )
        7 -> StepCounterScreen(
            onBack = { selectedHubTab = 0 },
            onSendStatsToChat = onSendPromptToChat
        )
        8 -> UniversalMediaPlayerScreen(
            onBack = { selectedHubTab = 0 },
            onSendMediaToChat = onSendPromptToChat
        )
        9 -> ReadAloudScreen(
            onBack = { selectedHubTab = 0 }
        )
        10 -> ImageEditorScreen(
            onBack = { selectedHubTab = 0 },
            onSendImageToChat = onSendImageToChat
        )
        11 -> Game2048Screen(
            onBack = { selectedHubTab = 0 },
            onSendScoreToChat = onSendPromptToChat
        )
        12 -> Scaffold(
            topBar = {
                Column {
                    TopAppBar(
                        title = { Text("Create Studio", fontWeight = FontWeight.SemiBold) },
                        navigationIcon = {
                            IconButton(
                                onClick = { selectedHubTab = 0 },
                                modifier = Modifier.testTag("hub_studio_back")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        ) { innerPadding ->
            StudioScreen(
                preferencesManager = viewModel.preferencesManager,
                onSendPromptToChat = onSendPromptToChat,
                onSendImageToChat = onSendImageToChat,
                modifier = Modifier.padding(innerPadding)
            )
        }
        13 -> RagKnowledgeScreen(
            viewModel = viewModel,
            onBack = { selectedHubTab = 0 }
        )
        14 -> AiPersonaScreen(
            preferencesManager = viewModel.preferencesManager,
            onApplyPersonaToChat = onSendPromptToChat,
            onBack = { selectedHubTab = 0 }
        )
        15 -> AiLearningScreen(
            onSendTopicToChat = onSendPromptToChat,
            onBack = { selectedHubTab = 0 }
        )
        16 -> YouTubeScreen(
            preferencesManager = viewModel.preferencesManager,
            onOpenSettings = onOpenSettings,
            onSummarizeVideoInChat = onSendPromptToChat,
            onBack = { selectedHubTab = 0 }
        )
        17 -> MapExplorerScreen(
            preferencesManager = viewModel.preferencesManager,
            onOpenSettings = onOpenSettings,
            onSendLocationToChat = onSendPromptToChat,
            onBack = { selectedHubTab = 0 }
        )
        18 -> NotificationCenterScreen(
            onBack = { selectedHubTab = 0 }
        )
        19 -> FocusHabitsScreen(
            viewModel = viewModel,
            onBack = { selectedHubTab = 0 }
        )
        20 -> MemoryScreen(
            viewModel = viewModel,
            onBack = { selectedHubTab = 0 }
        )
        21 -> PromptLibraryScreen(
            viewModel = viewModel,
            onBack = { selectedHubTab = 0 }
        )
    }
}
