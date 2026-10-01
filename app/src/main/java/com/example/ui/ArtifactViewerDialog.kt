package com.example.ui

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.ArtifactItem
import com.example.data.model.ArtifactType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtifactViewerDialog(
    artifact: ArtifactItem,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isPreviewable = artifact.type == ArtifactType.HTML_WEB || artifact.type == ArtifactType.SVG
    var selectedTab by remember { mutableIntStateOf(if (isPreviewable) 1 else 0) } // 0 = Code, 1 = Preview

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
                .testTag("artifact_viewer_dialog"),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = artifact.type.getIcon(),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = artifact.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = artifact.type.displayName,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "•",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                text = "${artifact.lineCount} lines",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "•",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                text = "${artifact.charCount} chars",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Copy Action
                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText(artifact.title, artifact.content))
                            Toast.makeText(context, "Copied artifact to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.testTag("copy_artifact_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy code",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Share Action
                    IconButton(
                        onClick = {
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TITLE, artifact.title)
                                putExtra(Intent.EXTRA_TEXT, artifact.content)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Artifact"))
                        },
                        modifier = Modifier.testTag("share_artifact_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Close Dialog
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_artifact_viewer_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // If previewable (HTML or SVG), show Tab Row
                if (isPreviewable) {
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("Live Preview") },
                            icon = { Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            modifier = Modifier.testTag("tab_live_preview")
                        )
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Source Code") },
                            icon = { Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            modifier = Modifier.testTag("tab_source_code")
                        )
                    }
                }

                HorizontalDivider()

                // Content View
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (isPreviewable && selectedTab == 1) {
                        // Live Web / SVG Preview
                        LiveWebPreview(
                            content = artifact.content,
                            isSvg = artifact.type == ArtifactType.SVG
                        )
                    } else {
                        // Code View with line numbers
                        CodeContentView(
                            code = artifact.content,
                            language = artifact.language
                        )
                    }
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun LiveWebPreview(content: String, isSvg: Boolean) {
    val htmlToLoad = remember(content, isSvg) {
        if (isSvg) {
            """
            <!DOCTYPE html>
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    body {
                        margin: 0;
                        padding: 16px;
                        display: flex;
                        justify-content: center;
                        align-items: center;
                        min-height: 90vh;
                        background: #f8f9fa;
                        font-family: sans-serif;
                    }
                    svg {
                        max-width: 100%;
                        height: auto;
                        box-shadow: 0 4px 12px rgba(0,0,0,0.1);
                        border-radius: 8px;
                        background: #ffffff;
                    }
                </style>
            </head>
            <body>
                $content
            </body>
            </html>
            """.trimIndent()
        } else {
            content
        }
    }

    AndroidView(
        modifier = Modifier
            .fillMaxSize()
            .testTag("artifact_webview_preview"),
        factory = { ctx ->
            WebView(ctx).apply {
                // Software layer avoids MESA rendernode lookup in container environments
                setLayerType(View.LAYER_TYPE_SOFTWARE, null)
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                settings.builtInZoomControls = true
                settings.displayZoomControls = false
                webViewClient = WebViewClient()
                loadDataWithBaseURL(null, htmlToLoad, "text/html", "UTF-8", null)
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL(null, htmlToLoad, "text/html", "UTF-8", null)
        },
        onRelease = { webView ->
            try {
                webView.stopLoading()
                webView.loadUrl("about:blank")
                webView.destroy()
            } catch (_: Exception) {}
        }
    )
}

@Composable
private fun CodeContentView(code: String, language: String) {
    val lines = remember(code) { code.lines() }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF1E1E2E) // Dark slate editor background
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .horizontalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Line numbers column
            Column(
                modifier = Modifier.padding(end = 16.dp),
                horizontalAlignment = Alignment.End
            ) {
                for (i in lines.indices) {
                    Text(
                        text = "${i + 1}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        color = Color(0xFF6C7086),
                        textAlign = TextAlign.End
                    )
                }
            }

            // Code text
            Column {
                for (line in lines) {
                    Text(
                        text = if (line.isEmpty()) " " else line,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        color = Color(0xFFCDD6F4)
                    )
                }
            }
        }
    }
}
