package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.CalendarEventEntity
import com.example.data.local.NoteEntity
import com.example.data.local.VaultItemEntity
import com.example.data.model.KnowledgeChunkEntity
import com.example.data.model.KnowledgeDocumentEntity
import com.example.data.model.RagEngineStats
import com.example.data.model.RetrievedChunk
import com.example.data.remote.AiService
import com.example.data.remote.FileUtils
import com.example.data.remote.InternalKnowledgeSources
import com.example.data.remote.RagEngine
import com.example.model.AiProvider
import com.example.model.AppSection
import com.example.model.ChatAttachment
import com.example.model.ChatMessage
import com.example.model.MessageRole
import com.example.model.MessageStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    val productivityDao = db.productivityDao()
    val chatDao = db.chatDao()
    val ragDao = db.ragDao()
    val ragEngine = RagEngine(ragDao)

    val notes: StateFlow<List<NoteEntity>> = productivityDao.getAllNotes()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val events: StateFlow<List<CalendarEventEntity>> = productivityDao.getAllEvents()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val vaultItems: StateFlow<List<VaultItemEntity>> = productivityDao.getAllVaultItems()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val ragDocuments: StateFlow<List<KnowledgeDocumentEntity>> = ragDao.getAllDocuments()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _isRagEnabled = MutableStateFlow(true)
    val isRagEnabled: StateFlow<Boolean> = _isRagEnabled.asStateFlow()

    private val _ragStats = MutableStateFlow(RagEngineStats())
    val ragStats: StateFlow<RagEngineStats> = _ragStats.asStateFlow()

    private val _testSearchResults = MutableStateFlow<List<RetrievedChunk>>(emptyList())
    val testSearchResults: StateFlow<List<RetrievedChunk>> = _testSearchResults.asStateFlow()

    private val _isRagProcessing = MutableStateFlow(false)
    val isRagProcessing: StateFlow<Boolean> = _isRagProcessing.asStateFlow()

    private val _selectedSection = MutableStateFlow(AppSection.CHAT)
    val selectedSection: StateFlow<AppSection> = _selectedSection.asStateFlow()

    private val _activeProvider = MutableStateFlow(AiProvider.GEMINI)
    val activeProvider: StateFlow<AiProvider> = _activeProvider.asStateFlow()

    private val _apiKeys = MutableStateFlow<Map<AiProvider, String>>(emptyMap())
    val apiKeys: StateFlow<Map<AiProvider, String>> = _apiKeys.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                role = MessageRole.ASSISTANT,
                content = "👋 Welcome to **OmniChat**! I'm your multi-provider AI workstation.\n\n" +
                        "- 🧠 **Multi-Provider Switcher**: Gemini, Groq, OpenRouter, and Free Web AI\n" +
                        "- 🛠️ **Intel Hub**: Productivity notes, schedule, encrypted secrets vault, scientific calculator, live web search, and step counter\n" +
                        "- 💻 **Code & Markdown Studios**: Direct attachment execution and live preview\n" +
                        "- 🎨 **Image Studio & Media**: Edit and send images directly to AI!\n\n" +
                        "How can I assist your workflow today?",
                modelName = "OmniChat System"
            )
        )
    )
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _pendingAttachments = MutableStateFlow<List<ChatAttachment>>(emptyList())
    val pendingAttachments: StateFlow<List<ChatAttachment>> = _pendingAttachments.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    // Studio Handoff States (Fixing BUG-05)
    private val _studioCodeHandoff = MutableStateFlow<Triple<String, String, String>?>(null)
    val studioCodeHandoff: StateFlow<Triple<String, String, String>?> = _studioCodeHandoff.asStateFlow()

    private val _studioMarkdownHandoff = MutableStateFlow<Pair<String, String>?>(null)
    val studioMarkdownHandoff: StateFlow<Pair<String, String>?> = _studioMarkdownHandoff.asStateFlow()

    init {
        refreshRagStats()
    }

    fun navigateToSection(section: AppSection) {
        _selectedSection.value = section
    }

    fun setProvider(provider: AiProvider) {
        _activeProvider.value = provider
    }

    fun setApiKey(provider: AiProvider, key: String) {
        _apiKeys.value = _apiKeys.value.toMutableMap().apply { put(provider, key.trim()) }
    }

    fun addAttachment(attachment: ChatAttachment) {
        _pendingAttachments.value = _pendingAttachments.value + attachment
    }

    fun removeAttachment(id: String) {
        _pendingAttachments.value = _pendingAttachments.value.filter { it.id != id }
    }

    // Fix for BUG-05: Forward code/markdown into studios
    fun openCodeInStudio(code: String, filename: String, language: String) {
        _studioCodeHandoff.value = Triple(code, filename, language)
        _selectedSection.value = AppSection.CODE_STUDIO
    }

    fun openMarkdownInStudio(content: String, filename: String) {
        _studioMarkdownHandoff.value = Pair(content, filename)
        _selectedSection.value = AppSection.MARKDOWN_STUDIO
    }

    fun clearCodeHandoff() {
        _studioCodeHandoff.value = null
    }

    fun clearMarkdownHandoff() {
        _studioMarkdownHandoff.value = null
    }

    // RAG Knowledge Base Methods
    fun refreshRagStats() {
        viewModelScope.launch {
            val docCount = ragDao.getDocumentCount()
            val chunkCount = ragDao.getChunkCount()
            _ragStats.value = RagEngineStats(
                totalDocuments = docCount,
                totalChunks = chunkCount,
                isGeminiEmbeddingAvailable = true,
                isHybridSearchActive = true,
                activeEmbeddingModel = "gemini-embedding-2-preview"
            )
        }
    }

    fun toggleRag(enabled: Boolean = !_isRagEnabled.value) {
        _isRagEnabled.value = enabled
    }

    fun testRagQuery(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch {
            _isRagProcessing.value = true
            val results = withContext(Dispatchers.IO) {
                ragEngine.retrieveRelevantChunks(query, topK = 5)
            }
            _testSearchResults.value = results
            _isRagProcessing.value = false
        }
    }

    fun ingestFileToRag(uri: Uri, callback: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _isRagProcessing.value = true
            try {
                val attInfo = FileUtils.processPickedUri(getApplication(), uri)
                if (attInfo != null) {
                    val file = File(attInfo.localUri)
                    val text = if (file.exists()) FileUtils.readFullTextContent(file) else (attInfo.previewSnippet ?: "")
                    val fileName = attInfo.name
                    if (text.isNotBlank()) {
                        ragEngine.ingestDocument(title = fileName, content = text, sourceType = "FILE")
                        refreshRagStats()
                        callback(true, "Ingested $fileName successfully")
                    } else {
                        callback(false, "Could not extract text from document")
                    }
                } else {
                    callback(false, "Could not process picked file")
                }
            } catch (e: Exception) {
                callback(false, "Error: ${e.localizedMessage}")
            } finally {
                _isRagProcessing.value = false
            }
        }
    }

    fun ingestTextToRag(title: String, text: String, sourceType: String = "MANUAL", callback: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _isRagProcessing.value = true
            try {
                ragEngine.ingestDocument(title = title, content = text, sourceType = sourceType)
                refreshRagStats()
                callback(true, "Ingested note '$title'")
            } catch (e: Exception) {
                callback(false, "Error: ${e.localizedMessage}")
            } finally {
                _isRagProcessing.value = false
            }
        }
    }

    fun ingestAllNotesToRag(callback: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _isRagProcessing.value = true
            var count = 0
            val allNotes = notes.value
            for (note in allNotes) {
                try {
                    ragEngine.ingestDocument(
                        title = note.title,
                        content = "${note.title}\n${note.tags}\n${note.content}",
                        sourceType = "NOTES"
                    )
                    count++
                } catch (_: Exception) {}
            }
            refreshRagStats()
            _isRagProcessing.value = false
            callback(true, "Imported $count notes into RAG knowledge base!")
        }
    }

    fun loadStarterKnowledgeBase(callback: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _isRagProcessing.value = true
            var count = 0
            for (source in InternalKnowledgeSources.sources) {
                try {
                    ragEngine.ingestDocument(
                        title = source.title,
                        content = source.content,
                        sourceType = "STARTER"
                    )
                    count++
                } catch (_: Exception) {}
            }
            refreshRagStats()
            _isRagProcessing.value = false
            callback(true, "Loaded $count starter knowledge packs!")
        }
    }

    fun ingestInternalSource(sourceId: String, callback: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _isRagProcessing.value = true
            try {
                val source = InternalKnowledgeSources.sources.firstOrNull { it.id == sourceId }
                if (source != null) {
                    ragEngine.ingestDocument(
                        title = source.title,
                        content = source.content,
                        sourceType = source.category
                    )
                    refreshRagStats()
                    _isRagProcessing.value = false
                    callback(true, "Ingested ${source.title}")
                } else {
                    _isRagProcessing.value = false
                    callback(false, "Source not found")
                }
            } catch (e: Exception) {
                _isRagProcessing.value = false
                callback(false, "Error: ${e.localizedMessage}")
            }
        }
    }

    suspend fun getChunksForDocument(docId: String): List<KnowledgeChunkEntity> = withContext(Dispatchers.IO) {
        ragDao.getChunksForDoc(docId)
    }

    fun deleteRagDocument(docId: String) {
        viewModelScope.launch {
            ragDao.deleteDocumentWithChunks(docId)
            refreshRagStats()
        }
    }

    fun clearEntireRag() {
        viewModelScope.launch {
            ragDao.clearAllKnowledge()
            _testSearchResults.value = emptyList()
            refreshRagStats()
        }
    }

    fun sendMessage(userText: String) {
        if (userText.isBlank() && _pendingAttachments.value.isEmpty()) return

        val currentAttachments = _pendingAttachments.value
        _pendingAttachments.value = emptyList()

        val userMessage = ChatMessage(
            role = MessageRole.USER,
            content = userText.ifBlank { "[Sent ${currentAttachments.size} attachment(s)]" },
            attachments = currentAttachments
        )

        _messages.value = _messages.value + userMessage
        _isGenerating.value = true

        val assistantMessageId = UUID.randomUUID().toString()
        val provider = _activeProvider.value
        val pendingAssistantMsg = ChatMessage(
            id = assistantMessageId,
            role = MessageRole.ASSISTANT,
            content = "Thinking with ${provider.displayName}...",
            status = MessageStatus.STREAMING,
            modelName = provider.defaultModel
        )
        _messages.value = _messages.value + pendingAssistantMsg

        viewModelScope.launch {
            val apiKey = _apiKeys.value[provider] ?: ""

            // Retrieve contextual knowledge from RAG if enabled
            val contextKnowledge = if (_isRagEnabled.value) {
                try {
                    val chunks = ragEngine.retrieveRelevantChunks(userText, topK = 3)
                    chunks.joinToString("\n---\n") { it.chunk.content }
                } catch (_: Exception) {
                    ""
                }
            } else ""

            // Append attachment snippets if text or code
            val attachmentSnippets = currentAttachments.filter { it.content.isNotBlank() }
                .joinToString("\n\n") { "Attachment [${it.name}]:\n${it.content}" }
            val fullPrompt = if (attachmentSnippets.isNotBlank()) {
                "$userText\n\n$attachmentSnippets"
            } else {
                userText
            }

            val response = AiService.generateResponse(
                provider = provider,
                apiKey = apiKey,
                history = _messages.value,
                userPrompt = fullPrompt,
                contextDoc = contextKnowledge
            )

            _messages.value = _messages.value.map { msg ->
                if (msg.id == assistantMessageId) {
                    msg.copy(
                        content = response,
                        status = MessageStatus.COMPLETE,
                        modelName = provider.defaultModel
                    )
                } else msg
            }
            _isGenerating.value = false
        }
    }

    fun clearChat() {
        _messages.value = emptyList()
    }
}
