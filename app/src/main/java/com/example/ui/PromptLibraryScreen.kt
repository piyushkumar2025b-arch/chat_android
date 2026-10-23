package com.example.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.PromptTemplateEntity

private val PLACEHOLDER_REGEX = Regex("\\{\\{\\s*([^{}]+?)\\s*}}")

private fun placeholderKeys(content: String): List<String> =
    PLACEHOLDER_REGEX.findAll(content)
        .map { it.groupValues[1] }
        .distinct()
        .toList()

private fun resolvePrompt(content: String, values: Map<String, String>): String =
    PLACEHOLDER_REGEX.replace(content) { match ->
        val key = match.groupValues[1]
        values[key]?.takeIf { it.isNotBlank() } ?: match.value
    }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PromptLibraryScreen(
    viewModel: ChatViewModel,
    onBack: () -> Unit
) {
    val prompts by viewModel.promptTemplates.collectAsState()

    var query by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var favoritesOnly by remember { mutableStateOf(false) }
    var editorTarget by remember { mutableStateOf<PromptTemplateEntity?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<PromptTemplateEntity?>(null) }
    var fillTarget by remember { mutableStateOf<PromptTemplateEntity?>(null) }

    val categories = listOf("All") + prompts.map { it.category.ifBlank { "General" } }.distinct().sorted()
    val filtered = prompts.filter { prompt ->
        val matchesQuery = query.isBlank() ||
            prompt.title.contains(query, ignoreCase = true) ||
            prompt.content.contains(query, ignoreCase = true) ||
            prompt.category.contains(query, ignoreCase = true)
        val matchesCategory = selectedCategory == "All" || prompt.category == selectedCategory
        val matchesFavorite = !favoritesOnly || prompt.isFavorite
        matchesQuery && matchesCategory && matchesFavorite
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Prompt Library", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    editorTarget = null
                    showEditor = true
                },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New prompt") }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Bookmarks,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(30.dp)
                        )
                        Spacer(Modifier.size(12.dp))
                        Column {
                            Text(
                                "${prompts.size} reusable prompts",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                "Tap Use to send a template to the composer. Placeholders like {{topic}} are filled in first.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.82f)
                            )
                        }
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search prompts…") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    categories.forEach { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category },
                            label = { Text(category) }
                        )
                    }
                    FilterChip(
                        selected = favoritesOnly,
                        onClick = { favoritesOnly = !favoritesOnly },
                        label = { Text("Favorites") },
                        leadingIcon = {
                            Icon(
                                if (favoritesOnly) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
            }

            item {
                Text(
                    text = "${filtered.size} shown",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (filtered.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Bookmarks,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(42.dp)
                            )
                            Spacer(Modifier.height(10.dp))
                            Text(
                                "No prompts match this filter.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            items(filtered, key = { it.id }) { prompt ->
                PromptCard(
                    prompt = prompt,
                    onUse = {
                        val keys = placeholderKeys(prompt.content)
                        if (keys.isEmpty()) {
                            viewModel.usePrompt(prompt)
                        } else {
                            fillTarget = prompt
                        }
                    },
                    onToggleFavorite = { viewModel.togglePromptFavorite(prompt) },
                    onEdit = {
                        editorTarget = prompt
                        showEditor = true
                    },
                    onDelete = { deleteTarget = prompt }
                )
            }

            item {
                Text(
                    "Unused starter prompts can be edited or deleted like any other template.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
    }

    if (showEditor) {
        PromptEditorDialog(
            initial = editorTarget,
            onDismiss = { showEditor = false },
            onSave = { title, content, category, favorite ->
                val existing = editorTarget
                if (existing == null) {
                    viewModel.upsertPrompt(
                        PromptTemplateEntity(
                            title = title,
                            content = content,
                            category = category,
                            isFavorite = favorite
                        )
                    )
                } else {
                    viewModel.upsertPrompt(
                        existing.copy(
                            title = title,
                            content = content,
                            category = category,
                            isFavorite = favorite
                        )
                    )
                }
                showEditor = false
            }
        )
    }

    fillTarget?.let { prompt ->
        PromptVariablesDialog(
            prompt = prompt,
            onDismiss = { fillTarget = null },
            onResolved = { resolved ->
                viewModel.usePrompt(prompt, resolved)
                fillTarget = null
            }
        )
    }

    deleteTarget?.let { prompt ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete this prompt?") },
            text = { Text("\"${prompt.title}\" will be removed from your library.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deletePrompt(prompt)
                        deleteTarget = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun PromptCard(
    prompt: PromptTemplateEntity,
    onUse: () -> Unit,
    onToggleFavorite: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = prompt.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AssistChip(
                            onClick = {},
                            label = { Text(prompt.category.ifBlank { "General" }) }
                        )
                        val variables = placeholderKeys(prompt.content).size
                        if (variables > 0) {
                            Spacer(Modifier.size(6.dp))
                            Text(
                                "$variables variable${if (variables == 1) "" else "s"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                IconButton(onClick = onToggleFavorite, modifier = Modifier.size(36.dp)) {
                    Icon(
                        if (prompt.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = if (prompt.isFavorite) "Remove favorite" else "Favorite",
                        tint = if (prompt.isFavorite) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Text(
                text = prompt.content,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (prompt.usageCount == 0) "Not used yet" else "Used ${prompt.usageCount}×",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onEdit, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit prompt", modifier = Modifier.size(17.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete prompt",
                        modifier = Modifier.size(17.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
                TextButton(onClick = onUse) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(4.dp))
                    Text("Use")
                }
            }
        }
    }
}

@Composable
private fun PromptEditorDialog(
    initial: PromptTemplateEntity?,
    onDismiss: () -> Unit,
    onSave: (title: String, content: String, category: String, favorite: Boolean) -> Unit
) {
    var title by remember { mutableStateOf(initial?.title.orEmpty()) }
    var content by remember { mutableStateOf(initial?.content.orEmpty()) }
    var category by remember { mutableStateOf(initial?.category ?: "General") }
    var favorite by remember { mutableStateOf(initial?.isFavorite ?: false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "New prompt" else "Edit prompt") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(80) },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it.take(30) },
                    label = { Text("Category") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Prompt") },
                    placeholder = { Text("Include {{variables}} the library should ask about.") },
                    minLines = 4,
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = favorite, onCheckedChange = { favorite = it })
                    Spacer(Modifier.size(8.dp))
                    Text("Mark as favorite", style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(title.trim(), content.trim(), category.trim().ifBlank { "General" }, favorite) },
                enabled = title.isNotBlank() && content.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun PromptVariablesDialog(
    prompt: PromptTemplateEntity,
    onDismiss: () -> Unit,
    onResolved: (String) -> Unit
) {
    val keys = remember(prompt.id) { placeholderKeys(prompt.content) }
    val values = remember(prompt.id) { mutableStateMapOf<String, String>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Fill in the variables") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    prompt.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                keys.forEach { key ->
                    OutlinedTextField(
                        value = values[key].orEmpty(),
                        onValueChange = { values[key] = it },
                        label = { Text(key.replaceFirstChar { it.uppercase() }) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Text(
                    "Blank fields keep their {{placeholder}} so you can edit in the composer.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(onClick = { onResolved(resolvePrompt(prompt.content, values)) }) {
                Text("Use in chat")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
