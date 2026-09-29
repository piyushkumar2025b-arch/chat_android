package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

sealed class MarkdownBlock {
    data class TextBlock(val content: String) : MarkdownBlock()
    data class CodeBlock(val language: String, val code: String) : MarkdownBlock()
}

@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.onSurface
) {
    val context = LocalContext.current
    val blocks = remember(text) { parseMarkdownBlocks(text) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (block in blocks) {
            when (block) {
                is MarkdownBlock.TextBlock -> {
                    RenderFormattedText(block.content, textColor = textColor)
                }
                is MarkdownBlock.CodeBlock -> {
                    RenderCodeBlock(
                        language = block.language,
                        code = block.code,
                        onCopy = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Code", block.code))
                            Toast.makeText(context, "Code copied to clipboard", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }
}

private fun parseMarkdownBlocks(text: String): List<MarkdownBlock> {
    val blocks = mutableListOf<MarkdownBlock>()
    val lines = text.split("\n")
    var inCodeBlock = false
    var codeLang = ""
    val codeBuilder = StringBuilder()
    val textBuilder = StringBuilder()

    for (line in lines) {
        val trimmed = line.trim()
        if (trimmed.startsWith("```")) {
            if (inCodeBlock) {
                // End code block
                blocks.add(MarkdownBlock.CodeBlock(codeLang, codeBuilder.toString().trimEnd()))
                codeBuilder.clear()
                codeLang = ""
                inCodeBlock = false
            } else {
                // Flush preceding text
                if (textBuilder.isNotEmpty()) {
                    blocks.add(MarkdownBlock.TextBlock(textBuilder.toString().trimEnd()))
                    textBuilder.clear()
                }
                inCodeBlock = true
                codeLang = trimmed.removePrefix("```").trim()
                if (codeLang.isEmpty()) codeLang = "code"
            }
        } else {
            if (inCodeBlock) {
                codeBuilder.append(line).append("\n")
            } else {
                textBuilder.append(line).append("\n")
            }
        }
    }

    if (inCodeBlock && codeBuilder.isNotEmpty()) {
        blocks.add(MarkdownBlock.CodeBlock(codeLang, codeBuilder.toString().trimEnd()))
    } else if (textBuilder.isNotEmpty()) {
        blocks.add(MarkdownBlock.TextBlock(textBuilder.toString().trimEnd()))
    }

    return if (blocks.isEmpty()) listOf(MarkdownBlock.TextBlock(text)) else blocks
}

@Composable
private fun RenderCodeBlock(
    language: String,
    code: String,
    onCopy: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .testTag("code_block_$language"),
        color = Color(0xFF1E1E2E), // Modern dark slate code background
        contentColor = Color(0xFFCDD6F4)
    ) {
        Column {
            // Header bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF181825))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = language.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFA6ADC8),
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = onCopy,
                    modifier = Modifier.size(28.dp).testTag("copy_code_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy code",
                        tint = Color(0xFFA6ADC8),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Code content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(12.dp)
            ) {
                Text(
                    text = code,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = Color(0xFFF5E0DC)
                )
            }
        }
    }
}

@Composable
private fun RenderFormattedText(content: String, textColor: Color) {
    val annotatedString = remember(content) {
        buildAnnotatedString {
            val lines = content.split("\n")
            for ((index, line) in lines.withIndex()) {
                val trimmed = line.trimStart()

                when {
                    trimmed.startsWith("### ") -> {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp, color = textColor)) {
                            append(trimmed.removePrefix("### "))
                        }
                    }
                    trimmed.startsWith("## ") -> {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 18.sp, color = textColor)) {
                            append(trimmed.removePrefix("## "))
                        }
                    }
                    trimmed.startsWith("# ") -> {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp, color = textColor)) {
                            append(trimmed.removePrefix("# "))
                        }
                    }
                    trimmed.startsWith("> ") -> {
                        withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = textColor.copy(alpha = 0.8f))) {
                            append("┃ " + trimmed.removePrefix("> "))
                        }
                    }
                    trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                        append(" • ")
                        appendFormattedInline(trimmed.substring(2), textColor)
                    }
                    trimmed.matches(Regex("^\\d+\\.\\s.*")) -> {
                        val numPart = trimmed.substringBefore(". ") + ". "
                        val textPart = trimmed.substringAfter(". ")
                        append(" $numPart")
                        appendFormattedInline(textPart, textColor)
                    }
                    else -> {
                        appendFormattedInline(line, textColor)
                    }
                }

                if (index < lines.size - 1) {
                    append("\n")
                }
            }
        }
    }

    Text(
        text = annotatedString,
        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
        color = textColor
    )
}

private fun androidx.compose.ui.text.AnnotatedString.Builder.appendFormattedInline(
    text: String,
    textColor: Color
) {
    var i = 0
    while (i < text.length) {
        if (text.startsWith("**", i)) {
            val next = text.indexOf("**", i + 2)
            if (next != -1) {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = textColor)) {
                    append(text.substring(i + 2, next))
                }
                i = next + 2
                continue
            }
        } else if (text[i] == '`') {
            val next = text.indexOf('`', i + 1)
            if (next != -1) {
                withStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        background = Color(0x22888888),
                        fontSize = 13.sp,
                        color = textColor
                    )
                ) {
                    append(" ${text.substring(i + 1, next)} ")
                }
                i = next + 1
                continue
            }
        } else if (text[i] == '*' && i + 1 < text.length && text[i + 1] != '*') {
            val next = text.indexOf('*', i + 1)
            if (next != -1) {
                withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = textColor)) {
                    append(text.substring(i + 1, next))
                }
                i = next + 1
                continue
            }
        }
        append(text[i])
        i++
    }
}
