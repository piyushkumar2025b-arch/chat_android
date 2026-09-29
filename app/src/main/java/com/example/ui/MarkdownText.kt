package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.commonmark.ext.autolink.AutolinkExtension
import org.commonmark.ext.gfm.tables.TableCell
import org.commonmark.ext.gfm.tables.TableHead
import org.commonmark.ext.gfm.tables.TableRow
import org.commonmark.ext.gfm.tables.TablesExtension
import org.commonmark.node.Block
import org.commonmark.node.BlockQuote
import org.commonmark.node.BulletList
import org.commonmark.node.Code
import org.commonmark.node.Document
import org.commonmark.node.Emphasis
import org.commonmark.node.FencedCodeBlock
import org.commonmark.node.HardLineBreak
import org.commonmark.node.Heading
import org.commonmark.node.IndentedCodeBlock
import org.commonmark.node.Link
import org.commonmark.node.ListItem
import org.commonmark.node.Node
import org.commonmark.node.OrderedList
import org.commonmark.node.Paragraph
import org.commonmark.node.SoftLineBreak
import org.commonmark.node.StrongEmphasis
import org.commonmark.node.Text
import org.commonmark.node.ThematicBreak
import org.commonmark.parser.Parser

/**
 * Shared CommonMark parser configured with standard extensions (GFM Tables, Autolink).
 */
private val markdownParser: Parser by lazy {
    Parser.builder()
        .extensions(
            listOf(
                TablesExtension.create(),
                AutolinkExtension.create()
            )
        )
        .build()
}

/**
 * High-performance, fully native Jetpack Compose Markdown renderer.
 * Formats AI responses including code blocks with copy action, bold/italic text,
 * ordered & bullet lists, blockquotes, tables, and links.
 */
@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    style: TextStyle = MaterialTheme.typography.bodyMedium
) {
    if (text.isBlank()) return

    val parsedDocument = remember(text) {
        markdownParser.parse(text) as Document
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        var child: Node? = parsedDocument.firstChild
        var blockIndex = 0
        while (child != null) {
            RenderMarkdownBlock(
                node = child,
                textColor = textColor,
                baseStyle = style,
                modifier = Modifier.testTag("md_block_$blockIndex")
            )
            blockIndex++
            child = child.next
        }
    }
}

@Composable
private fun RenderMarkdownBlock(
    node: Node,
    textColor: Color,
    baseStyle: TextStyle,
    modifier: Modifier = Modifier
) {
    when (node) {
        is Paragraph -> {
            val inlineText = rememberInlineContent(node.firstChild, textColor)
            if (inlineText.isNotEmpty()) {
                Text(
                    text = inlineText,
                    style = baseStyle.copy(lineHeight = 22.sp),
                    color = textColor,
                    modifier = modifier
                )
            }
        }

        is Heading -> {
            val headingText = rememberInlineContent(node.firstChild, textColor)
            val (headingStyle, topSpace) = when (node.level) {
                1 -> MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    lineHeight = 26.sp
                ) to 8.dp
                2 -> MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    lineHeight = 24.sp
                ) to 6.dp
                3 -> MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    lineHeight = 22.sp
                ) to 4.dp
                else -> MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    lineHeight = 20.sp
                ) to 2.dp
            }

            Column(modifier = modifier.padding(top = topSpace)) {
                Text(
                    text = headingText,
                    style = headingStyle,
                    color = textColor
                )
                if (node.level <= 2) {
                    Spacer(modifier = Modifier.height(4.dp))
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        thickness = 1.dp
                    )
                }
            }
        }

        is FencedCodeBlock -> {
            val lang = node.info?.trim()?.split("\\s+".toRegex())?.firstOrNull() ?: ""
            MarkdownCodeBlock(
                language = if (lang.isBlank()) "code" else lang,
                code = node.literal?.trimEnd() ?: "",
                modifier = modifier
            )
        }

        is IndentedCodeBlock -> {
            MarkdownCodeBlock(
                language = "code",
                code = node.literal?.trimEnd() ?: "",
                modifier = modifier
            )
        }

        is BulletList -> {
            Column(
                modifier = modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                var item: Node? = node.firstChild
                while (item != null) {
                    if (item is ListItem) {
                        RenderBulletListItem(item, textColor, baseStyle)
                    }
                    item = item.next
                }
            }
        }

        is OrderedList -> {
            @Suppress("DEPRECATION")
            val startNum = node.startNumber
            Column(
                modifier = modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                var item: Node? = node.firstChild
                var index = 0
                while (item != null) {
                    if (item is ListItem) {
                        RenderOrderedListItem(
                            item = item,
                            number = startNum + index,
                            textColor = textColor,
                            baseStyle = baseStyle
                        )
                        index++
                    }
                    item = item.next
                }
            }
        }

        is BlockQuote -> {
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.4f))
                    .padding(vertical = 4.dp)
            ) {
                // Left accent bar
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .padding(start = 2.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(2.dp)
                        )
                )

                Spacer(modifier = Modifier.width(10.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 4.dp, horizontal = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    var childBlock: Node? = node.firstChild
                    while (childBlock != null) {
                        RenderMarkdownBlock(
                            node = childBlock,
                            textColor = textColor.copy(alpha = 0.9f),
                            baseStyle = baseStyle.copy(fontStyle = FontStyle.Italic)
                        )
                        childBlock = childBlock.next
                    }
                }
            }
        }

        is ThematicBreak -> {
            HorizontalDivider(
                modifier = modifier.padding(vertical = 6.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                thickness = 1.dp
            )
        }

        is org.commonmark.ext.gfm.tables.TableBlock -> {
            RenderMarkdownTable(node, textColor, baseStyle, modifier)
        }

        else -> {
            // Fallback for custom or unknown block nodes: render children
            var childBlock: Node? = node.firstChild
            while (childBlock != null) {
                RenderMarkdownBlock(childBlock, textColor, baseStyle)
                childBlock = childBlock.next
            }
        }
    }
}

@Composable
private fun RenderBulletListItem(
    item: ListItem,
    textColor: Color,
    baseStyle: TextStyle
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "•",
            style = baseStyle.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                lineHeight = 22.sp
            ),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(18.dp)
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            var childBlock: Node? = item.firstChild
            while (childBlock != null) {
                RenderMarkdownBlock(childBlock, textColor, baseStyle)
                childBlock = childBlock.next
            }
        }
    }
}

@Composable
private fun RenderOrderedListItem(
    item: ListItem,
    number: Int,
    textColor: Color,
    baseStyle: TextStyle
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "$number.",
            style = baseStyle.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                lineHeight = 22.sp
            ),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.widthIn(min = 22.dp).padding(end = 4.dp)
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            var childBlock: Node? = item.firstChild
            while (childBlock != null) {
                RenderMarkdownBlock(childBlock, textColor, baseStyle)
                childBlock = childBlock.next
            }
        }
    }
}

@Composable
private fun RenderMarkdownTable(
    tableBlock: org.commonmark.ext.gfm.tables.TableBlock,
    textColor: Color,
    baseStyle: TextStyle,
    modifier: Modifier = Modifier
) {
    val headers = mutableListOf<AnnotatedString>()
    val rows = mutableListOf<List<AnnotatedString>>()

    var section: Node? = tableBlock.firstChild
    while (section != null) {
        when (section) {
            is TableHead -> {
                var rowNode: Node? = section.firstChild
                while (rowNode != null) {
                    if (rowNode is TableRow) {
                        var cellNode: Node? = rowNode.firstChild
                        while (cellNode != null) {
                            if (cellNode is TableCell) {
                                headers.add(buildInlineAnnotatedString(cellNode.firstChild, textColor))
                            }
                            cellNode = cellNode.next
                        }
                    }
                    rowNode = rowNode.next
                }
            }
            else -> {
                // TableBody
                var rowNode: Node? = section.firstChild
                while (rowNode != null) {
                    if (rowNode is TableRow) {
                        val rowCells = mutableListOf<AnnotatedString>()
                        var cellNode: Node? = rowNode.firstChild
                        while (cellNode != null) {
                            if (cellNode is TableCell) {
                                rowCells.add(buildInlineAnnotatedString(cellNode.firstChild, textColor))
                            }
                            cellNode = cellNode.next
                        }
                        if (rowCells.isNotEmpty()) {
                            rows.add(rowCells)
                        }
                    }
                    rowNode = rowNode.next
                }
            }
        }
        section = section.next
    }

    if (headers.isEmpty() && rows.isEmpty()) return

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            // Header Row
            if (headers.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    for (header in headers) {
                        Text(
                            text = header,
                            style = baseStyle.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.widthIn(min = 80.dp)
                        )
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            }

            // Data Rows
            for ((rowIndex, rowData) in rows.withIndex()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (rowIndex % 2 == 1) MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.3f)
                            else Color.Transparent,
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    for (cell in rowData) {
                        Text(
                            text = cell,
                            style = baseStyle.copy(fontSize = 13.sp),
                            color = textColor,
                            modifier = Modifier.widthIn(min = 80.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Modern code block with programming language label, syntax background,
 * one-tap copy button with toast feedback and checkmark animation.
 */
@Composable
fun MarkdownCodeBlock(
    language: String,
    code: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isCopied by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val displayLang = remember(language) {
        val cleaned = language.trim().uppercase()
        when {
            cleaned.isBlank() -> "CODE"
            cleaned == "KOTLIN" || cleaned == "KT" -> "KOTLIN"
            cleaned == "PYTHON" || cleaned == "PY" -> "PYTHON"
            cleaned == "JAVASCRIPT" || cleaned == "JS" -> "JAVASCRIPT"
            cleaned == "TYPESCRIPT" || cleaned == "TS" -> "TYPESCRIPT"
            cleaned == "JSON" -> "JSON"
            cleaned == "XML" -> "XML"
            cleaned == "HTML" -> "HTML"
            cleaned == "CSS" -> "CSS"
            cleaned == "BASH" || cleaned == "SH" || cleaned == "SHELL" -> "BASH"
            cleaned == "SQL" -> "SQL"
            cleaned == "RUST" || cleaned == "RS" -> "RUST"
            cleaned == "CPP" || cleaned == "C++" -> "C++"
            cleaned == "JAVA" -> "JAVA"
            cleaned == "GO" || cleaned == "GOLANG" -> "GO"
            cleaned == "DART" -> "DART"
            cleaned == "SWIFT" -> "SWIFT"
            else -> cleaned
        }
    }

    // Modern dark slate editor color scheme
    val codeBackgroundColor = Color(0xFF1E1E2E)
    val headerBackgroundColor = Color(0xFF181825)
    val codeTextColor = Color(0xFFCDD6F4)
    val headerTextColor = Color(0xFFA6ADC8)
    val borderColor = Color(0xFF313244)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .testTag("code_block_$displayLang"),
        color = codeBackgroundColor,
        contentColor = codeTextColor
    ) {
        Column {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(headerBackgroundColor)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (displayLang in listOf("BASH", "SHELL", "SH")) Icons.Default.Terminal else Icons.Default.Code,
                        contentDescription = null,
                        tint = headerTextColor,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = displayLang,
                        style = MaterialTheme.typography.labelSmall,
                        color = headerTextColor,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }

                // Copy Code Button with Animated Feedback
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Code", code))
                            Toast.makeText(context, "Copied $displayLang code to clipboard", Toast.LENGTH_SHORT).show()
                            isCopied = true
                            coroutineScope.launch {
                                delay(2000)
                                isCopied = false
                            }
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .testTag("copy_code_button"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AnimatedContent(
                        targetState = isCopied,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "copy_icon"
                    ) { copied ->
                        if (copied) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Copied",
                                    tint = Color(0xFFA6E3A1),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Copied!",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFA6E3A1),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy code",
                                    tint = headerTextColor,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Copy",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = headerTextColor
                                )
                            }
                        }
                    }
                }
            }

            // Code Content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Text(
                    text = code,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    color = codeTextColor
                )
            }
        }
    }
}

@Composable
private fun rememberInlineContent(firstInline: Node?, textColor: Color): AnnotatedString {
    val linkColor = MaterialTheme.colorScheme.primary
    val codeBg = MaterialTheme.colorScheme.surfaceContainerHighest
    val codeText = MaterialTheme.colorScheme.onSurfaceVariant

    return remember(firstInline, textColor, linkColor) {
        buildInlineAnnotatedString(
            firstInline = firstInline,
            textColor = textColor,
            linkColor = linkColor,
            codeBgColor = codeBg,
            codeTextColor = codeText
        )
    }
}

private fun buildInlineAnnotatedString(
    firstInline: Node?,
    textColor: Color,
    linkColor: Color = Color(0xFF007AFF),
    codeBgColor: Color = Color(0x1F888888),
    codeTextColor: Color = textColor
): AnnotatedString {
    return buildAnnotatedString {
        var curr = firstInline
        while (curr != null) {
            appendNode(curr, textColor, linkColor, codeBgColor, codeTextColor)
            curr = curr.next
        }
    }
}

private fun AnnotatedString.Builder.appendNode(
    node: Node,
    textColor: Color,
    linkColor: Color,
    codeBgColor: Color,
    codeTextColor: Color
) {
    when (node) {
        is Text -> {
            append(node.literal ?: "")
        }

        is StrongEmphasis -> {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = textColor)) {
                var child = node.firstChild
                while (child != null) {
                    appendNode(child, textColor, linkColor, codeBgColor, codeTextColor)
                    child = child.next
                }
            }
        }

        is Emphasis -> {
            withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = textColor)) {
                var child = node.firstChild
                while (child != null) {
                    appendNode(child, textColor, linkColor, codeBgColor, codeTextColor)
                    child = child.next
                }
            }
        }

        is Code -> {
            // Inline code badge
            withStyle(
                SpanStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.5.sp,
                    background = codeBgColor,
                    color = codeTextColor,
                    fontWeight = FontWeight.Medium
                )
            ) {
                append(" ${node.literal ?: ""} ")
            }
        }

        is Link -> {
            val destination = node.destination ?: ""
            val linkStyles = TextLinkStyles(
                style = SpanStyle(
                    color = linkColor,
                    textDecoration = TextDecoration.Underline,
                    fontWeight = FontWeight.SemiBold
                )
            )
            val linkAnnotation = LinkAnnotation.Url(
                url = destination,
                styles = linkStyles
            )
            pushLink(linkAnnotation)
            var child = node.firstChild
            while (child != null) {
                appendNode(child, textColor, linkColor, codeBgColor, codeTextColor)
                child = child.next
            }
            pop()
        }

        is HardLineBreak -> {
            append("\n")
        }

        is SoftLineBreak -> {
            append(" ")
        }

        else -> {
            var child = node.firstChild
            while (child != null) {
                appendNode(child, textColor, linkColor, codeBgColor, codeTextColor)
                child = child.next
            }
        }
    }
}
