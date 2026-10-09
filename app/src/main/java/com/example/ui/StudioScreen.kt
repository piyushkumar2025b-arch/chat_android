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
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Videocam
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
fun StudioScreen(
    initialTab: Int = 0,
    preferencesManager: PreferencesManager? = null,
    onSendPromptToChat: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedStudioTab by remember { mutableIntStateOf(initialTab) }
    val tabs = listOf(
        Triple("Image Gen", Icons.Default.Image, 0),
        Triple("Photo Editor", Icons.Default.Tune, 1),
        Triple("Music & Beats", Icons.Default.GraphicEq, 2),
        Triple("Code & HTML", Icons.Default.Code, 3),
        Triple("Markdown Hub", Icons.Default.Description, 4),
        Triple("Video Motion", Icons.Default.Videocam, 5)
    )

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        ScrollableTabRow(
            selectedTabIndex = selectedStudioTab,
            edgePadding = 12.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clip(RoundedCornerShape(12.dp))
                .testTag("studio_tab_row"),
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            tabs.forEach { (title, icon, index) ->
                Tab(
                    selected = selectedStudioTab == index,
                    onClick = { selectedStudioTab = index },
                    text = { Text(title, fontWeight = if (selectedStudioTab == index) FontWeight.Bold else FontWeight.Normal) },
                    icon = { Icon(icon, contentDescription = title) },
                    modifier = Modifier.testTag("studio_tab_$index")
                )
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        Box(modifier = Modifier.weight(1f)) {
            when (selectedStudioTab) {
                0 -> ImageStudioScreen(onSendPromptToChat = onSendPromptToChat)
                1 -> ImageEditorScreen(onSendImageToChat = onSendPromptToChat)
                2 -> MusicStudioScreen()
                3 -> CodeStudioScreen(preferencesManager = preferencesManager, onSendToChat = onSendPromptToChat)
                4 -> MarkdownStudioScreen(onSendToChat = onSendPromptToChat)
                5 -> VideoStudioScreen(onSendPromptToChat = onSendPromptToChat)
            }
        }
    }
}
