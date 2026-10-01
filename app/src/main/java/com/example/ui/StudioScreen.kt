package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioScreen(
    onSendPromptToChat: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedStudioTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        Triple("Image Gen", Icons.Default.Image, 0),
        Triple("Video Motion", Icons.Default.Videocam, 1),
        Triple("Sound & Audio", Icons.Default.GraphicEq, 2)
    )

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        TabRow(
            selectedTabIndex = selectedStudioTab,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
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

        Spacer(modifier = Modifier.height(4.dp))

        Box(modifier = Modifier.weight(1f)) {
            when (selectedStudioTab) {
                0 -> ImageStudioScreen(onSendPromptToChat = onSendPromptToChat)
                1 -> VideoStudioScreen(onSendPromptToChat = onSendPromptToChat)
                2 -> SoundStudioScreen()
            }
        }
    }
}
