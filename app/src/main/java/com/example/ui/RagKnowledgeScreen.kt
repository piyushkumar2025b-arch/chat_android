package com.example.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.KnowledgeChunkEntity
import com.example.data.model.KnowledgeDocumentEntity
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RagKnowledgeScreen(
    viewModel: ChatViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val documents by viewModel.ragDocuments.collectAsState()
    val isRagEnabled by viewModel.isRagEnabled.collectAsState()
    val ragStats by viewModel.ragStats.collectAsState()
    val testSearchResults by viewModel.testSearchResults.collectAsState()
    val isRagProcessing by viewModel.isRagProcessing.collectAsState()

    var testQuery by remember { mutableStateOf("") }
    var showAddNoteDialog by remember { mutableStateOf(false) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var viewingDocumentChunks by remember { mutableStateOf<KnowledgeDocumentEntity?>(null) }
    var loadedChunksForDoc by remember { mutableStateOf<List<KnowledgeChunkEntity>>(emptyList()) }
    var isLoadingDocChunks by remember { mutableStateOf(false) }

    // File picker for document ingestion (PDF, DOCX, TXT, MD, code, JSON)
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.ingestFileToRag(uri) { success, msg ->
                Toast.makeText(context, if (success) "✅ $msg" else "❌ $msg", Toast.LENGTH_LONG).show()
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.refreshRagStats()
    }

    BackHandler {
        onBack()
    }

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("rag_knowledge_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Hub,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                "RAG Knowledge Hub",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Local Vector Store • Semantic Search • Embeddings",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("rag_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Chat")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.refreshRagStats() },
                        modifier = Modifier.testTag("rag_refresh_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh Stats")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // RAG Status & Engine Telemetry Card
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("rag_telemetry_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.FactCheck,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "RAG Grounding in Chat",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Switch(
                                checked = isRagEnabled,
                                onCheckedChange = { viewModel.toggleRag() },
                                modifier = Modifier.testTag("rag_enable_switch")
                            )
                        }

                        Text(
                            text = if (isRagEnabled)
                                "Active: Chat queries will semantically retrieve and ground responses with relevant knowledge base chunks."
                            else
                                "Disabled: Standard chat generation without private document grounding.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(12.dp))

                        // Stats Grid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Documents", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${ragStats.totalDocuments}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                            Column {
                                Text("Indexed Chunks", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${ragStats.totalChunks}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                            }
                            Column {
                                Text("Embedding Engine", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    ragStats.activeEmbeddingModel,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (ragStats.isGeminiEmbeddingAvailable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
                                )
                            }
                        }
                    }
                }
            }

            // Interactive Semantic Retrieval Sandbox / Query Inspector
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("rag_search_sandbox_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Semantic Search Sandbox",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            "Test vector retrieval live against your indexed chunks to inspect similarity scores and citations.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = testQuery,
                            onValueChange = { testQuery = it },
                            placeholder = { Text("Enter a concept or query to test retrieval...") },
                            modifier = Modifier.fillMaxWidth().testTag("rag_test_query_input"),
                            singleLine = true,
                            trailingIcon = {
                                if (testQuery.isNotEmpty()) {
                                    IconButton(onClick = { testQuery = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                                    }
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    if (testQuery.isNotBlank()) {
                                        viewModel.testRagQuery(testQuery)
                                    }
                                },
                                enabled = testQuery.isNotBlank() && !isRagProcessing,
                                modifier = Modifier.weight(1f).testTag("rag_execute_search_button")
                            ) {
                                if (isRagProcessing) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Searching...")
                                } else {
                                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Test RAG Retrieval")
                                }
                            }
                        }

                        // Test Results
                        AnimatedVisibility(visible = testSearchResults.isNotEmpty()) {
                            Column(modifier = Modifier.padding(top = 14.dp)) {
                                Text(
                                    "Top Retrieved Passages (${testSearchResults.size} matches):",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                for (item in testSearchResults) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    "#${item.rank} • ${item.chunk.docTitle}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = MaterialTheme.colorScheme.secondaryContainer
                                                ) {
                                                    val pct = (item.score * 100).toInt()
                                                    Text(
                                                        "$pct% relevance [${item.matchType}]",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                item.chunk.content,
                                                style = MaterialTheme.typography.bodySmall,
                                                maxLines = 4,
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

            // Ingestion Actions Card
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Knowledge Documents (${documents.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilledTonalButton(
                            onClick = {
                                documentPickerLauncher.launch(
                                    arrayOf(
                                        "application/pdf",
                                        "application/msword",
                                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                        "text/plain",
                                        "text/markdown",
                                        "application/json",
                                        "*/*"
                                    )
                                )
                            },
                            modifier = Modifier.testTag("rag_upload_doc_button")
                        ) {
                            Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Upload File")
                        }

                        FilledTonalButton(
                            onClick = { showAddNoteDialog = true },
                            modifier = Modifier.testTag("rag_add_note_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Note")
                        }
                    }
                }
            }

            // Quick Starter Docs & Actions Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            viewModel.loadStarterKnowledgeBase { success, msg ->
                                Toast.makeText(context, if (success) "✅ $msg" else "❌ $msg", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f).testTag("rag_load_starter_button")
                    ) {
                        Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Starter Docs")
                    }

                    if (documents.isNotEmpty()) {
                        OutlinedButton(
                            onClick = { showClearConfirmDialog = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.testTag("rag_clear_all_button")
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Clear All")
                        }
                    }
                }
            }

            // Documents List
            if (documents.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "No documents in Knowledge Base yet",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Upload PDF, Word, Code or add notes to empower the AI with custom retrieval.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    viewModel.loadStarterKnowledgeBase { success, msg ->
                                        Toast.makeText(context, if (success) "✅ $msg" else "❌ $msg", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            ) {
                                Text("Populate Starter Knowledge Base")
                            }
                        }
                    }
                }
            } else {
                items(documents, key = { it.id }) { doc ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewingDocumentChunks = doc
                                isLoadingDocChunks = true
                                coroutineScope.launch {
                                    val chunks = viewModel.ragEngine.search(doc.title, topK = 10, geminiApiKey = "")
                                    loadedChunksForDoc = chunks.map { it.chunk }.filter { it.docId == doc.id }
                                    isLoadingDocChunks = false
                                }
                            }
                            .testTag("rag_doc_card_${doc.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when (doc.sourceType) {
                                        "PDF" -> Icons.Default.Description
                                        "CODE" -> Icons.AutoMirrored.Filled.OpenInNew
                                        "SAMPLE" -> Icons.Default.MenuBook
                                        else -> Icons.Default.FolderOpen
                                    },
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = doc.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = MaterialTheme.colorScheme.surface
                                    ) {
                                        Text(
                                            doc.sourceType,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                    Text(
                                        "• ${doc.chunkCount} chunks",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        "• ${doc.charCount} chars",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            IconButton(
                                onClick = { viewModel.deleteRagDocument(doc.id) },
                                modifier = Modifier.testTag("delete_rag_doc_${doc.id}")
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete document",
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Custom Note Dialog
    if (showAddNoteDialog) {
        var noteTitle by remember { mutableStateOf("") }
        var noteContent by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddNoteDialog = false },
            title = { Text("Add Note to Knowledge Base", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Enter notes, documentation, facts, or instructions. They will be segmented into semantic vectors for RAG retrieval.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = noteTitle,
                        onValueChange = { noteTitle = it },
                        label = { Text("Title / Topic") },
                        placeholder = { Text("e.g., Company Policies 2026") },
                        modifier = Modifier.fillMaxWidth().testTag("add_note_title_input"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = noteContent,
                        onValueChange = { noteContent = it },
                        label = { Text("Document Content") },
                        placeholder = { Text("Paste or type document content...") },
                        modifier = Modifier.fillMaxWidth().height(160.dp).testTag("add_note_content_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (noteContent.isNotBlank()) {
                            val title = noteTitle.ifBlank { "Note_${System.currentTimeMillis()}" }
                            viewModel.ingestTextToRag(title, noteContent, sourceType = "MANUAL") { success, msg ->
                                Toast.makeText(context, if (success) "✅ $msg" else "❌ $msg", Toast.LENGTH_SHORT).show()
                            }
                            showAddNoteDialog = false
                        }
                    },
                    enabled = noteContent.isNotBlank(),
                    modifier = Modifier.testTag("save_note_button")
                ) {
                    Text("Index Document")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddNoteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Clear All Confirmation Dialog
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("Clear Entire Knowledge Base?", fontWeight = FontWeight.Bold) },
            text = {
                Text("This will remove all indexed documents, vectors, and chunks from local storage.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearEntireRag()
                        showClearConfirmDialog = false
                        Toast.makeText(context, "Knowledge base cleared", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // View Document Chunks Sheet
    viewingDocumentChunks?.let { doc ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { viewingDocumentChunks = null },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = doc.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${doc.sourceType} • ${doc.chunkCount} Chunks Total",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (isLoadingDocChunks) {
                    Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().height(350.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(loadedChunksForDoc) { chunk ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        "Chunk #${chunk.chunkIndex + 1} (${chunk.content.length} chars)",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        chunk.content,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { viewingDocumentChunks = null },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close")
                }
            }
        }
    }
}
