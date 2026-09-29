package com.example.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DataArray
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.ui.graphics.vector.ImageVector

enum class ArtifactType(val displayName: String, val category: String) {
    HTML_WEB("Web Page / HTML", "Web"),
    SVG("SVG Vector Graphic", "Graphics"),
    CODE("Code Snippet", "Code"),
    SHELL("Terminal Script", "Shell"),
    JSON_DATA("Data / Config", "Data"),
    MARKDOWN("Markdown Document", "Docs"),
    OTHER("Text Document", "Text");

    fun getIcon(): ImageVector {
        return when (this) {
            HTML_WEB -> Icons.Default.Language
            SVG -> Icons.Default.Brush
            CODE -> Icons.Default.Code
            SHELL -> Icons.Default.Terminal
            JSON_DATA -> Icons.Default.DataArray
            MARKDOWN -> Icons.AutoMirrored.Filled.Article
            OTHER -> Icons.Default.Description
        }
    }
}

data class ArtifactItem(
    val id: String,
    val messageId: String,
    val sessionId: String,
    val title: String,
    val type: ArtifactType,
    val language: String,
    val content: String,
    val timestamp: Long,
    val lineCount: Int,
    val charCount: Int
)
