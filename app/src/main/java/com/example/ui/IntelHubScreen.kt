package com.example.ui

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CalendarMonth
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
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class HubToolItem(
    val id: Int,
    val title: String,
    val subtitle: String,
    val icon: ImageVector
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
    // 0 = Hub Overview, 1 = Notes, 2 = Calendar, 3 = Secrets Vault, 4 = Calculator,
    // 5 = Web Search, 6 = News, 7 = Step Counter, 8 = Media Player, 9 = Read Aloud,
    // 10 = Image Studio, 11 = 2048, 12 = Creative Studio, 13 = Knowledge Base,
    // 14 = AI Persona, 15 = AI Academy, 16 = YouTube Intel, 17 = Map Explorer,
    // 18 = Notification Center, 19 = Focus & Habits, 20 = AI Memory, 21 = Prompt Library
    var selectedHubTab by remember { mutableIntStateOf(0) }

    // CRITICAL FIX FOR BUG-02:
    // When inside any sub-tab (selectedHubTab != 0), hardware/system back returns to Hub Overview (0)
    // instead of prematurely popping out to Chat!
    BackHandler(enabled = selectedHubTab != 0) {
        selectedHubTab = 0
    }

    val tools = listOf(
        HubToolItem(1, "Notes", "Rich markdown notes with tags & SQLite persistence", Icons.AutoMirrored.Filled.Note),
        HubToolItem(2, "Calendar", "Schedule meetings & dispatch AI briefings", Icons.Default.CalendarMonth),
        HubToolItem(3, "Secrets Vault", "PIN-protected encrypted storage for keys & passwords", Icons.Default.Key),
        HubToolItem(4, "Calculator", "Scientific calculator & interactive unit converter", Icons.Default.Calculate),
        HubToolItem(5, "Live Search", "Query DuckDuckGo & forward research to AI", Icons.Default.Search),
        HubToolItem(6, "AI News", "Latest AI, Android, and Silicon intelligence", Icons.Default.Newspaper),
        HubToolItem(7, "Step Counter", "Activity tracking with AI Health Coach advice", Icons.AutoMirrored.Filled.DirectionsRun),
        HubToolItem(8, "Media Player", "Universal audio playback with release safety", Icons.Default.PlayCircle),
        HubToolItem(9, "Read Aloud", "Text-to-speech audio reader for documents", Icons.Default.RecordVoiceOver),
        HubToolItem(10, "Image Studio", "Sketch, annotate, and send image to AI chat", Icons.Default.Image),
        HubToolItem(11, "2048 Game", "Mind puzzle with AI game advisor integration", Icons.Default.SportsEsports),
        HubToolItem(12, "Creative Studio", "Multi-track audio, image, and video generators", Icons.Default.Palette),
        HubToolItem(13, "Knowledge Base", "RAG engine: ingest documents, chunk & semantic search", Icons.AutoMirrored.Filled.MenuBook),
        HubToolItem(14, "AI Persona", "Behavior presets, custom instructions & test drive", Icons.Default.Face),
        HubToolItem(15, "AI Academy", "Curated learning paths with AI topic explainers", Icons.Default.School),
        HubToolItem(16, "YouTube Intel", "Search videos, transcripts & summarize into chat", Icons.Default.OndemandVideo),
        HubToolItem(17, "Map Explorer", "Search places, coordinates & route planning", Icons.Default.Map),
        HubToolItem(18, "Notifications", "Permissions, test alerts & notification history", Icons.Default.Notifications),
        HubToolItem(19, "Focus & Habits", "Pomodoro timer, daily streaks & focus analytics", Icons.Default.Timer),
        HubToolItem(20, "AI Memory", "Long-term facts the assistant remembers across chats", Icons.Default.Psychology),
        HubToolItem(21, "Prompt Library", "Reusable prompt templates with variable placeholders", Icons.Default.Bookmarks)
    )

    when (selectedHubTab) {
        0 -> {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Omni Intel Hub", fontWeight = FontWeight.Bold) },
                        navigationIcon = {
                            IconButton(onClick = onBackToChat, modifier = Modifier.testTag("hub_back_to_chat")) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Chat")
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
                        .padding(16.dp)
                ) {
                    // Hero Banner
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Row(
                            modifier = Modifier.padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Widgets,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = "Productivity & Intel Suite",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "All tools integrate bi-directionally with OmniChat multi-provider AI.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }

                    // Tools Grid
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(tools) { item ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedHubTab = item.id }
                                    .testTag("hub_tool_${item.id}"),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            item.icon,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                    Text(
                                        text = item.title,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        text = item.subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2
                                    )
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
        12 -> StudioScreen(
            preferencesManager = viewModel.preferencesManager,
            onSendPromptToChat = onSendPromptToChat
        )
        13 -> RagKnowledgeScreen(
            viewModel = viewModel,
            onBack = { selectedHubTab = 0 }
        )
        14 -> AiPersonaScreen(
            preferencesManager = viewModel.preferencesManager,
            onApplyPersonaToChat = onSendPromptToChat
        )
        15 -> AiLearningScreen(
            onSendTopicToChat = onSendPromptToChat
        )
        16 -> YouTubeScreen(
            preferencesManager = viewModel.preferencesManager,
            onOpenSettings = onOpenSettings,
            onSummarizeVideoInChat = onSendPromptToChat
        )
        17 -> MapExplorerScreen(
            preferencesManager = viewModel.preferencesManager,
            onOpenSettings = onOpenSettings,
            onSendLocationToChat = onSendPromptToChat
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
