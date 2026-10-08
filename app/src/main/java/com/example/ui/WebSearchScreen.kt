package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.remote.SearchCategory
import com.example.data.remote.WebSearchResult
import com.example.data.remote.WebSearchService
import kotlinx.coroutines.launch

@Composable
fun WebSearchScreen(
    onSendSearchToChat: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(SearchCategory.ALL) }
    var isSearching by remember { mutableStateOf(false) }
    val searchResults = remember { mutableStateListOf<WebSearchResult>() }
    var hasSearched by remember { mutableStateOf(false) }

    fun executeSearch() {
        val trimmed = searchQuery.trim()
        if (trimmed.isEmpty()) return

        isSearching = true
        hasSearched = true
        coroutineScope.launch {
            val result = WebSearchService.search(trimmed, selectedCategory)
            isSearching = false
            result.onSuccess { list ->
                searchResults.clear()
                searchResults.addAll(list)
            }.onFailure { err ->
                searchResults.clear()
                Toast.makeText(context, "Search error: ${err.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Language,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        "Live Multi-Source Web Search",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Wikipedia • arXiv • GitHub • Bing News • StackOverflow • OpenAlex • HackerNews",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Search Bar Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("web_search_query_input"),
                            placeholder = { Text("Search topics, code, papers, news, quotes...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                                    }
                                }
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { executeSearch() }),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = { executeSearch() },
                            enabled = !isSearching && searchQuery.isNotBlank(),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("execute_web_search_button")
                        ) {
                            if (isSearching) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text("Search")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Category filter chips
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(SearchCategory.values()) { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = {
                                    selectedCategory = cat
                                    if (searchQuery.isNotBlank()) executeSearch()
                                },
                                label = {
                                    Text("${cat.icon} ${cat.label}", style = MaterialTheme.typography.labelSmall)
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                modifier = Modifier.testTag("filter_chip_${cat.id}")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Quick topic chips
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val suggestions = when (selectedCategory) {
                            SearchCategory.NEWS -> listOf("Latest AI News", "World Tech Summit", "SpaceX Starship", "Global Economy 2026")
                            SearchCategory.KNOWLEDGE -> listOf("Quantum Computing", "James Webb Space Telescope", "Plate Tectonics", "CRISPR Gene Editing")
                            SearchCategory.SCIENCE -> listOf("Large Language Models", "Superconductivity", "Gravitational Waves", "Neural Architecture")
                            SearchCategory.CODE -> listOf("Kotlin Coroutines", "Jetpack Compose", "PyTorch 2.0", "FastAPI Framework")
                            SearchCategory.BOOKS -> listOf("Artificial Intelligence Books", "Science Fiction Classics", "Dune Frank Herbert", "Philosophy of Mind")
                            SearchCategory.ALL -> listOf("Gemini 1.5 Pro", "Quantum Computing", "Kotlin 2.0", "Latest AI News", "SpaceX Starship")
                        }
                        items(suggestions) { topic ->
                            AssistChip(
                                onClick = {
                                    searchQuery = topic
                                    executeSearch()
                                },
                                label = { Text(topic, style = MaterialTheme.typography.labelSmall) },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "🌐 16 Free Sources: Wikipedia • DuckDuckGo • StackOverflow • GitHub • DEV.to • PubMed • arXiv • OpenAlex • OpenLibrary • Internet Archive • HackerNews • Bing News • CrossRef • Wikiquote • Wikinews • Wiktionary",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }
        }

        if (hasSearched && searchResults.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Found ${searchResults.size} Verified Results",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Button(
                        onClick = {
                            val contextSnippets = searchResults.take(10).joinToString("\n\n") {
                                "• [${it.source}] ${it.title} (${it.pubDate}):\n${it.snippet}\nLink: ${it.url}"
                            }
                            val prompt = "Based on these verified live real-time web sources for '$searchQuery':\n\n$contextSnippets\n\nPlease synthesize a clear, comprehensive, and updated answer with source citations for: $searchQuery"
                            onSendSearchToChat(prompt)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("ask_ai_all_results_button")
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Ask AI with All Results", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        if (hasSearched && !isSearching && searchResults.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No results found for '$searchQuery' in ${selectedCategory.label}. Try 'All Sources' or different keywords.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        items(searchResults) { result ->
            val sourceBadgeColor = getSourceBadgeColor(result.source)

            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_result_card")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = sourceBadgeColor.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "${getSourceEmoji(result.source)} ${result.source.ifEmpty { "Web" }}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = sourceBadgeColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }

                        if (result.pubDate.isNotBlank()) {
                            Text(
                                text = result.pubDate,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = result.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 22.sp
                    )

                    if (result.snippet.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = result.snippet,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 18.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                val prompt = "Based on this verified live real-time web source from ${result.source}:\n\nTitle: ${result.title}\nSnippet: ${result.snippet}\nLink: ${result.url}\n\nPlease analyze, explain, or answer key takeaways from this source for: $searchQuery"
                                onSendSearchToChat(prompt)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("discuss_result_with_ai_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Discuss with AI", style = MaterialTheme.typography.labelSmall)
                        }

                        Row {
                            if (result.snippet.isNotBlank()) {
                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("Search Result", "${result.title}\n${result.snippet}\n${result.url}")
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy text", modifier = Modifier.size(18.dp))
                                }
                            }

                            if (result.url.isNotBlank()) {
                                IconButton(
                                    onClick = {
                                        try {
                                            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(result.url))
                                            context.startActivity(browserIntent)
                                        } catch (_: Exception) {
                                            Toast.makeText(context, "Cannot open URL", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.OpenInBrowser, contentDescription = "Open Source")
                                }
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
private fun getSourceBadgeColor(source: String): Color {
    return when (source.lowercase()) {
        "wikipedia" -> MaterialTheme.colorScheme.primary
        "duckduckgo" -> Color(0xFFD97706) // Amber
        "stack overflow" -> Color(0xFFF97316) // StackOverflow Orange
        "arxiv" -> Color(0xFFDC2626) // Crimson Red
        "github" -> Color(0xFF059669) // Emerald Green
        "dev.to" -> Color(0xFF0F172A) // Dark Slate
        "pubmed" -> Color(0xFF0284C7) // Sky Blue
        "openlibrary" -> Color(0xFFB45309) // Amber Brown
        "internet archive" -> Color(0xFF475569) // Classic Slate
        "hacker news" -> Color(0xFFEA580C) // Orange
        "openalex" -> Color(0xFF2563EB) // Royal Blue
        "crossref" -> Color(0xFF7C3AED) // Purple
        "wikiquote" -> Color(0xFF4F46E5) // Indigo
        "wikinews" -> Color(0xFF0284C7) // Sky Blue
        "wiktionary" -> Color(0xFF0D9488) // Teal
        else -> MaterialTheme.colorScheme.tertiary
    }
}

private fun getSourceEmoji(source: String): String {
    return when (source.lowercase()) {
        "wikipedia" -> "📖"
        "duckduckgo" -> "🦆"
        "stack overflow" -> "💡"
        "arxiv" -> "🔬"
        "github" -> "💻"
        "dev.to" -> "👩‍💻"
        "pubmed" -> "🧬"
        "openlibrary" -> "📚"
        "internet archive" -> "🏛️"
        "hacker news" -> "⚡"
        "openalex" -> "🏛️"
        "crossref" -> "📚"
        "wikiquote" -> "💬"
        "wikinews" -> "📢"
        "wiktionary" -> "📝"
        else -> "📰"
    }
}
