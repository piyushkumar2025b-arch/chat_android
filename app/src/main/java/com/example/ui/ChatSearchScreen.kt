package com.example.ui

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ManageSearch
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChatMessageEntity
import com.example.data.model.ChatSearchGroup
import kotlinx.coroutines.delay

private fun snippetAround(text: String, query: String, radius: Int = 90): String {
    val normalized = text.replace('\n', ' ')
    if (query.isBlank()) return normalized.take(radius * 2)
    val index = normalized.indexOf(query, ignoreCase = true)
    if (index < 0) return normalized.take(radius * 2)
    val start = (index - radius).coerceAtLeast(0)
    val end = (index + query.length + radius).coerceAtMost(normalized.length)
    val prefix = if (start > 0) "…" else ""
    val suffix = if (end < normalized.length) "…" else ""
    return prefix + normalized.substring(start, end) + suffix
}

private fun highlightMatches(text: String, query: String, highlight: Color): AnnotatedString {
    if (query.isBlank()) return AnnotatedString(text)
    return buildAnnotatedString {
        append(text)
        var index = text.indexOf(query, ignoreCase = true)
        while (index >= 0) {
            addStyle(
                SpanStyle(
                    background = highlight,
                    fontWeight = FontWeight.SemiBold
                ),
                index,
                index + query.length
            )
            index = text.indexOf(query, index + query.length, ignoreCase = true)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatSearchScreen(
    viewModel: ChatViewModel,
    onBack: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<ChatSearchGroup>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var hasSearched by remember { mutableStateOf(false) }
    var titlesOnly by remember { mutableStateOf(false) }

    LaunchedEffect(query) {
        if (query.isBlank()) {
            results = emptyList()
            hasSearched = false
            isSearching = false
            return@LaunchedEffect
        }
        delay(250)
        isSearching = true
        results = viewModel.searchChats(query)
        isSearching = false
        hasSearched = true
    }

    val visibleGroups = remember(results, titlesOnly) {
        if (titlesOnly) results.filter { it.titleMatched } else results
    }
    val matchCount = visibleGroups.sumOf { group ->
        group.hits.size + if (group.titleMatched) 1 else 0
    }
    val highlightColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Search Chats", fontWeight = FontWeight.Bold)
                        Text(
                            if (hasSearched) "$matchCount match${if (matchCount == 1) "" else "es"} in ${visibleGroups.size} conversation${if (visibleGroups.size == 1) "" else "s"}"
                            else "Find messages and session titles instantly",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("search_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search words, phrases, titles…") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("chat_search_input")
            )

            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = !titlesOnly,
                    onClick = { titlesOnly = false },
                    label = { Text("Everything", fontSize = 12.sp) }
                )
                FilterChip(
                    selected = titlesOnly,
                    onClick = { titlesOnly = true },
                    label = { Text("Titles only", fontSize = 12.sp) }
                )
                if (isSearching) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                }
            }

            when {
                query.isBlank() -> {
                    SearchEmptyState(
                        icon = Icons.Default.ManageSearch,
                        title = "Search across every conversation",
                        subtitle = "Try a keyword, a decision you made, a code snippet, or part of a chat title."
                    )
                }
                hasSearched && visibleGroups.isEmpty() && !isSearching -> {
                    SearchEmptyState(
                        icon = Icons.Default.Search,
                        title = "No matches for “${query.trim()}”",
                        subtitle = "Try a shorter keyword, check spelling, or switch off Titles only."
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(visibleGroups, key = { it.sessionId }) { group ->
                            SearchGroupCard(
                                group = group,
                                query = query.trim(),
                                highlightColor = highlightColor,
                                onOpenSession = {
                                    viewModel.openSearchHit(group.sessionId, null)
                                    onBack()
                                },
                                onOpenMessage = { messageId ->
                                    viewModel.openSearchHit(group.sessionId, messageId)
                                    onBack()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchEmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(30.dp)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }
}

@Composable
private fun SearchGroupCard(
    group: ChatSearchGroup,
    query: String,
    highlightColor: Color,
    onOpenSession: () -> Unit,
    onOpenMessage: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(vertical = 12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenSession)
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        group.sessionTitle,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "${group.hits.size} message match${if (group.hits.size == 1) "" else "es"} · " +
                            DateUtils.getRelativeTimeSpanString(
                                group.sessionUpdatedAt,
                                System.currentTimeMillis(),
                                DateUtils.MINUTE_IN_MILLIS
                            ).toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (group.titleMatched) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            "Title match",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            if (group.hits.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                group.hits.take(6).forEach { hit ->
                    SearchHitRow(
                        hit = hit,
                        query = query,
                        highlightColor = highlightColor,
                        onClick = { onOpenMessage(hit.id) }
                    )
                }
                if (group.hits.size > 6) {
                    Text(
                        "+${group.hits.size - 6} more matches — open the chat to see them",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchHitRow(
    hit: ChatMessageEntity,
    query: String,
    highlightColor: Color,
    onClick: () -> Unit
) {
    val isUser = hit.role == "user"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(
                    if (isUser) MaterialTheme.colorScheme.secondaryContainer
                    else MaterialTheme.colorScheme.primaryContainer
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (isUser) Icons.Default.Person else Icons.Default.SmartToy,
                contentDescription = null,
                modifier = Modifier.size(15.dp),
                tint = if (isUser) MaterialTheme.colorScheme.onSecondaryContainer
                else MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                highlightMatches(snippetAround(hit.content, query), query, highlightColor),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                DateUtils.getRelativeTimeSpanString(
                    hit.timestamp,
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS
                ).toString(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
