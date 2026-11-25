package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
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
    onSendImageToChat: (android.graphics.Bitmap) -> Unit = { _ -> onSendPromptToChat("[Image Edited from Studio]") },
    modifier: Modifier = Modifier
) {
    var selectedStudioTab by remember { mutableIntStateOf(initialTab) }
    val tabs = listOf(
        Triple("Images", Icons.Default.Image, 0),
        Triple("Draw", Icons.Default.Tune, 1),
        Triple("Music", Icons.Default.GraphicEq, 2),
        Triple("Code", Icons.Default.Code, 3),
        Triple("Writing", Icons.Default.Description, 4),
        Triple("Video", Icons.Default.Videocam, 5),
        Triple("Player", Icons.Default.GraphicEq, 6),
        Triple("Sounds", Icons.AutoMirrored.Filled.VolumeUp, 7)
    )

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        ScrollableTabRow(
            selectedTabIndex = selectedStudioTab,
            edgePadding = 12.dp,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("studio_tab_row"),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            tabs.forEach { (title, icon, index) ->
                Tab(
                    selected = selectedStudioTab == index,
                    onClick = { selectedStudioTab = index },
                    text = { Text(title, maxLines = 1, fontWeight = if (selectedStudioTab == index) FontWeight.SemiBold else FontWeight.Normal) },
                    icon = { Icon(icon, contentDescription = title) },
                    modifier = Modifier.testTag("studio_tab_$index")
                )
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        BackHandler(enabled = selectedStudioTab != 0) {
            selectedStudioTab = 0
        }

        Box(modifier = Modifier.weight(1f)) {
            when (selectedStudioTab) {
                0 -> ImageStudioScreen(onSendPromptToChat = onSendPromptToChat)
                1 -> ImageEditorScreen(
                    onBack = { selectedStudioTab = 0 },
                    onSendImageToChat = onSendImageToChat,
                    showTopBar = false
                )
                2 -> MusicStudioScreen()
                3 -> CodeStudioScreen(
                    onBack = { selectedStudioTab = 0 },
                    onSendCodeToChat = { code, name -> onSendPromptToChat("Code from Studio ($name):\n$code") },
                    showTopBar = false
                )
                4 -> MarkdownStudioScreen(
                    onBack = { selectedStudioTab = 0 },
                    onSendMarkdownToChat = onSendPromptToChat,
                    showTopBar = false
                )
                5 -> VideoStudioScreen(onSendPromptToChat = onSendPromptToChat)
                6 -> UniversalMediaPlayerScreen(
                    onBack = { selectedStudioTab = 0 },
                    onSendMediaToChat = onSendPromptToChat,
                    showTopBar = false
                )
                7 -> SoundStudioScreen()
            }
        }
    }
}
