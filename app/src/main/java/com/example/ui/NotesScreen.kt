package com.example.ui

import android.widget.Toast
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.NoteEntity
import com.example.data.local.PreferencesManager
import com.example.data.local.ProductivityDao
import com.example.data.remote.AiService
import com.example.util.showMainToast
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    productivityDao: ProductivityDao,
    preferencesManager: PreferencesManager? = null,
    onBack: () -> Unit,
    onSendNoteToChat: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val resolvedPrefs = remember(preferencesManager) { preferencesManager ?: PreferencesManager(context) }
    val coroutineScope = rememberCoroutineScope()
    val allNotes by productivityDao.getAllNotes().collectAsState(initial = emptyList())

    var searchQuery by remember { mutableStateOf("") }
    var selectedTag by remember { mutableStateOf<String?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    var editingNote by remember { mutableStateOf<NoteEntity?>(null) }
    var allNotesSummary by remember { mutableStateOf<String?>(null) }
    var isSummarizingAll by remember { mutableStateOf(false) }

    val availableTags = remember(allNotes) {
        allNotes.flatMap { it.tags.split(",") }
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }

    val filteredNotes = remember(allNotes, searchQuery, selectedTag) {
        allNotes.filter { note ->
            val matchesTag = selectedTag == null || note.tags.split(",")
                .any { it.trim().equals(selectedTag, ignoreCase = true) }
            val matchesSearch = searchQuery.isBlank() ||
                note.title.contains(searchQuery, ignoreCase = true) ||
                note.content.contains(searchQuery, ignoreCase = true) ||
                note.tags.contains(searchQuery, ignoreCase = true)
            matchesTag && matchesSearch
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Productivity Notes", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("notes_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingNote = null
                    showDialog = true
                },
                modifier = Modifier.testTag("notes_add_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Note")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search notes or tags...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            if (availableTags.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedTag == null,
                        onClick = { selectedTag = null },
                        label = { Text("All") },
                        modifier = Modifier.testTag("notes_tag_filter_All")
                    )
                    availableTags.forEach { tag ->
                        FilterChip(
                            selected = selectedTag == tag,
                            onClick = { selectedTag = if (selectedTag == tag) null else tag },
                            label = { Text("#$tag") },
                            modifier = Modifier.testTag("notes_tag_filter_$tag")
                        )
                    }
                }
            }

            if (allNotes.isNotEmpty()) {
                FilledTonalButton(
                    onClick = {
                        coroutineScope.launch {
                            isSummarizingAll = true
                            val notesDump = allNotes.take(15).joinToString("\n\n") { n ->
                                "Title: ${n.title}\nContent: ${n.content}"
                            }
                            val res = AiService.quickAssist(
                                taskPrompt = "Summarize the key themes, tasks, and ideas across these notes in plain bullet points:\n\n$notesDump",
                                systemInstruction = "You write clear, concise plain-language note summaries.",
                                customGeminiApiKey = resolvedPrefs.geminiApiKey
                            )
                            isSummarizingAll = false
                            res.onSuccess { summary ->
                                allNotesSummary = summary
                            }.onFailure { err ->
                                showMainToast(
                                    context,
                                    err.localizedMessage ?: "Could not summarize notes right now"
                                )
                            }
                        }
                    },
                    enabled = !isSummarizingAll,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("notes_summarize_all_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isSummarizingAll) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Summarizing Notes…")
                    } else {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("AI Summary of All Notes")
                    }
                }
            }

            val summaryText = allNotesSummary
            if (summaryText != null) {
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
                            Text("AI Notes Overview", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            IconButton(onClick = { allNotesSummary = null }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Dismiss summary", modifier = Modifier.size(16.dp))
                            }
                        }
                        Text(summaryText, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            if (filteredNotes.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (searchQuery.isBlank() && selectedTag == null) "No notes yet. Tap + to create one!" else "No matching notes found.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("notes_list"),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredNotes, key = { it.id }) { note ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
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
                                        text = note.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Row {
                                        IconButton(onClick = {
                                            onSendNoteToChat("Note Content [${note.title}]:\n${note.content}\n\nPlease summarize and expand on these ideas.")
                                            Toast.makeText(context, "Note sent to AI Chat!", Toast.LENGTH_SHORT).show()
                                            onBack()
                                        }) {
                                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send to Chat", tint = MaterialTheme.colorScheme.primary)
                                        }
                                        IconButton(onClick = {
                                            editingNote = note
                                            showDialog = true
                                        }) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                                        }
                                        IconButton(onClick = {
                                            coroutineScope.launch { productivityDao.deleteNote(note) }
                                            Toast.makeText(context, "Note deleted", Toast.LENGTH_SHORT).show()
                                        }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }

                                if (note.tags.isNotBlank()) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        note.tags.split(",").forEach { tag ->
                                            if (tag.isNotBlank()) {
                                                val cleanTag = tag.trim()
                                                SuggestionChip(
                                                    onClick = { searchQuery = cleanTag },
                                                    label = { Text("#$cleanTag") }
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = note.content,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        val isEditing = editingNote != null
        var title by remember { mutableStateOf(editingNote?.title ?: "") }
        var content by remember { mutableStateOf(editingNote?.content ?: "") }
        var tags by remember { mutableStateOf(editingNote?.tags ?: "") }
        var isAiHelping by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(if (isEditing) "Edit Note" else "Create Note") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = tags,
                        onValueChange = { tags = it },
                        label = { Text("Tags (comma-separated, e.g. ai, study, ideas)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        label = { Text("Note Content") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 6
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (content.isBlank()) {
                                    Toast.makeText(context, "Write a note first", Toast.LENGTH_SHORT).show()
                                    return@OutlinedButton
                                }
                                coroutineScope.launch {
                                    isAiHelping = true
                                    val res = AiService.quickAssist(
                                        taskPrompt = "Fix spelling, improve clarity, and organize this note cleanly without changing its meaning:\n\n$content",
                                        systemInstruction = "You polish notes into clear, easy-to-read text. Return only the updated note text.",
                                        customGeminiApiKey = resolvedPrefs.geminiApiKey
                                    )
                                    isAiHelping = false
                                    res.onSuccess { content = it }
                                        .onFailure { err ->
                                            showMainToast(context, err.localizedMessage ?: "AI Polish failed")
                                        }
                                }
                            },
                            enabled = !isAiHelping && content.isNotBlank(),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("note_ai_polish_button")
                        ) {
                            if (isAiHelping) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("AI Polish", maxLines = 1)
                        }
                        OutlinedButton(
                            onClick = {
                                if (content.isBlank()) {
                                    Toast.makeText(context, "Write a note first", Toast.LENGTH_SHORT).show()
                                    return@OutlinedButton
                                }
                                coroutineScope.launch {
                                    isAiHelping = true
                                    val res = AiService.quickAssist(
                                        taskPrompt = "Summarize this note into 3–5 clear bullet points:\n\n$content",
                                        systemInstruction = "Return only a short, clean bullet summary.",
                                        customGeminiApiKey = resolvedPrefs.geminiApiKey
                                    )
                                    isAiHelping = false
                                    res.onSuccess { content = it }
                                        .onFailure { err ->
                                            showMainToast(context, err.localizedMessage ?: "AI Summary failed")
                                        }
                                }
                            },
                            enabled = !isAiHelping && content.isNotBlank(),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("note_ai_summary_button")
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("AI Summary", maxLines = 1)
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            if (content.isBlank()) {
                                Toast.makeText(context, "Write a note first", Toast.LENGTH_SHORT).show()
                                return@OutlinedButton
                            }
                            coroutineScope.launch {
                                isAiHelping = true
                                val res = AiService.quickAssist(
                                    taskPrompt = "Extract clear, actionable checklist items from this note formatted as `- [ ] Task` lines:\n\n$content",
                                    systemInstruction = "Return only a crisp `- [ ]` markdown checklist.",
                                    customGeminiApiKey = resolvedPrefs.geminiApiKey
                                )
                                isAiHelping = false
                                res.onSuccess { content = it }
                                    .onFailure { err ->
                                        showMainToast(context, err.localizedMessage ?: "AI Action List failed")
                                    }
                            }
                        },
                        enabled = !isAiHelping && content.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("note_ai_checklist_button")
                    ) {
                        Icon(Icons.Default.Checklist, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Turn Into AI Action List", maxLines = 1)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            coroutineScope.launch {
                                val entity = NoteEntity(
                                    id = editingNote?.id ?: UUID.randomUUID().toString(),
                                    title = title,
                                    content = content,
                                    tags = tags,
                                    updatedAt = System.currentTimeMillis()
                                )
                                productivityDao.insertNote(entity)
                                showMainToast(context, if (isEditing) "Note updated" else "Note saved")
                                showDialog = false
                            }
                        }
                    }
                ) {
                    Text(if (isEditing) "Update" else "Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cancel") }
            }
        )
    }
}
