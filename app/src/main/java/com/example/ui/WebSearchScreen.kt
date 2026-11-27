package com.example.ui

import android.widget.Toast
import com.example.util.openExternalUrl
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.PreferencesManager
import com.example.data.remote.AiService
import com.example.data.remote.SearchCategory
import com.example.data.remote.WebSearchResult
import com.example.data.remote.WebSearchService
import com.example.util.showMainToast
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebSearchScreen(
    preferencesManager: PreferencesManager? = null,
    onBack: () -> Unit,
    onSendResultsToChat: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val resolvedPrefs = remember(preferencesManager) { preferencesManager ?: PreferencesManager(context) }
    val coroutineScope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("Latest Android Jetpack Compose") }
    var selectedCategory by remember { mutableStateOf(SearchCategory.ALL) }
    var isSearching by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }
    var hasSearched by remember { mutableStateOf(false) }
    val searchResults = remember { mutableStateListOf<WebSearchResult>() }
    var aiAnswer by remember { mutableStateOf<String?>(null) }
    var isAiAnswering by remember { mutableStateOf(false) }

    fun executeSearch(categoryOverride: SearchCategory = selectedCategory) {
        if (searchQuery.isBlank()) return
        isSearching = true
        searchError = null
        aiAnswer = null
        coroutineScope.launch {
            WebSearchService.search(searchQuery, categoryOverride)
                .onSuccess { results ->
                    searchResults.clear()
                    searchResults.addAll(results)
                    hasSearched = true
                }
                .onFailure { err ->
                    searchError = err.localizedMessage ?: "Search request failed."
                    hasSearched = true
                }
            isSearching = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Web Search", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("web_search_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (searchResults.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                val summary = searchResults.take(8).joinToString("\n\n") {
                                    "Title: ${it.title}\nSource: ${it.source}\nURL: ${it.url}\nSnippet: ${it.snippet}"
                                }
                                onSendResultsToChat("Web Research Findings for \"$searchQuery\":\n\n$summary\n\nPlease synthesize and verify these sources.")
                                Toast.makeText(context, "Research sent to AI Chat!", Toast.LENGTH_SHORT).show()
                                onBack()
                            }
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = "Send to Chat", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search web topic...") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { executeSearch() })
                )
                Button(
                    onClick = { executeSearch() },
                    enabled = !isSearching && searchQuery.isNotBlank(),
                    modifier = Modifier.testTag("execute_web_search")
                ) {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SearchCategory.entries.forEach { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = {
                            selectedCategory = category
                            if (searchQuery.isNotBlank() && hasSearched) {
                                executeSearch(category)
                            }
                        },
                        label = { Text("${category.icon} ${category.label}") }
                    )
                }
            }

            when {
                isSearching -> {
                    Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                searchError != null && searchResults.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(searchError ?: "Search failed.", color = MaterialTheme.colorScheme.error)
                            Button(onClick = { executeSearch() }) { Text("Retry") }
                        }
                    }
                }
                searchResults.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (hasSearched) "No results found. Try another keyword or source filter."
                            else "Enter a search term and tap Search to query live web sources.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> {
                    FilledTonalButton(
                        onClick = {
                            coroutineScope.launch {
                                isAiAnswering = true
                                val contextText = searchResults.take(6).joinToString("\n\n") {
                                    "Title: ${it.title}\nSource: ${it.source}\nSnippet: ${it.snippet}"
                                }
                                val res = AiService.quickAssist(
                                    taskPrompt = "Answer \"$searchQuery\" simply and clearly using only these search results. Mention the source names when helpful:\n\n$contextText",
                                    systemInstruction = "Give a short, plain-language answer based on the search results.",
                                    customGeminiApiKey = resolvedPrefs.geminiApiKey
                                )
                                isAiAnswering = false
                                res.onSuccess { aiAnswer = it }
                                    .onFailure { err ->
                                        showMainToast(
                                            context,
                                            err.localizedMessage ?: "AI Answer failed"
                                        )
                                    }
                            }
                        },
                        enabled = !isAiAnswering,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("web_search_ai_answer_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isAiAnswering) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Summarizing Results…")
                        } else {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Get AI Quick Answer")
                        }
                    }

                    val currentAiAnswer = aiAnswer
                    if (currentAiAnswer != null) {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("AI Answer", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    IconButton(onClick = { aiAnswer = null }, modifier = Modifier.size(28.dp)) {
                                        Icon(Icons.Default.Close, contentDescription = "Close AI answer", modifier = Modifier.size(16.dp))
                                    }
                                }
                                Text(currentAiAnswer, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(searchResults) { item ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = item.url.isNotBlank()) {
                                        openExternalUrl(context, item.url, "Unable to open URL")
                                    },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${item.source} • ${item.category}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                        if (item.url.isNotBlank()) {
                                            Icon(
                                                Icons.Default.OpenInBrowser,
                                                contentDescription = "Open source",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = item.snippet,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 4,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (item.url.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = item.url,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.outline,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
