package com.example.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.PreferencesManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntelHubScreen(
    initialTab: Int = 0,
    preferencesManager: PreferencesManager,
    onSendPromptToChat: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedHubTab by remember { mutableIntStateOf(initialTab) }
    val tabs = listOf(
        Triple("AI Persona", Icons.Default.Psychology, 0),
        Triple("Learn AI", Icons.Default.School, 1),
        Triple("Live News", Icons.Default.Newspaper, 2),
        Triple("Web Search", Icons.Default.Language, 3),
        Triple("Read Aloud", Icons.Default.RecordVoiceOver, 4)
    )

    Column(modifier = modifier.fillMaxSize()) {
        ScrollableTabRow(
            selectedTabIndex = selectedHubTab,
            edgePadding = 16.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clip(RoundedCornerShape(12.dp))
                .testTag("intel_hub_tab_row"),
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            tabs.forEach { (title, icon, index) ->
                Tab(
                    selected = selectedHubTab == index,
                    onClick = { selectedHubTab = index },
                    text = { Text(title, fontWeight = if (selectedHubTab == index) FontWeight.Bold else FontWeight.Normal) },
                    icon = { Icon(icon, contentDescription = title) },
                    modifier = Modifier.testTag("hub_tab_$index")
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Box(modifier = Modifier.weight(1f)) {
            when (selectedHubTab) {
                0 -> AiPersonaScreen(
                    preferencesManager = preferencesManager,
                    onApplyPersonaToChat = onSendPromptToChat
                )
                1 -> AiLearningScreen(onSendTopicToChat = onSendPromptToChat)
                2 -> NewsScreen(onSummarizeArticleInChat = onSendPromptToChat)
                3 -> WebSearchScreen(onSendSearchToChat = onSendPromptToChat)
                4 -> ReadAloudScreen(onSendTextToChat = onSendPromptToChat)
            }
        }
    }
}
