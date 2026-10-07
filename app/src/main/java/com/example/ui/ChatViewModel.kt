package com.example.ui

import android.app.Application
import android.net.Uri
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ChatRepository
import com.example.data.local.AppDatabase
import com.example.data.local.PreferencesManager
import com.example.data.local.UsageStats
import com.example.data.local.UsageTracker
import com.example.data.model.AiModel
import com.example.data.model.ArtifactExtractor
import com.example.data.model.ArtifactItem
import com.example.data.model.AttachmentInfo
import com.example.data.model.AvailableModels
import com.example.data.model.ChatMessageEntity
import com.example.data.model.ChatSessionEntity
import com.example.data.model.KnowledgeChunkEntity
import com.example.data.model.KnowledgeDocumentEntity
import com.example.data.model.ProviderType
import com.example.data.model.RagEngineStats
import com.example.data.model.RetrievedChunk
import com.example.data.remote.AiService
import com.example.data.remote.FileUtils
import com.example.data.remote.RagEngine
import com.example.data.remote.WebSearchService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

enum class AppSection(val label: String) {
    CHAT("Chat"),
    KNOWLEDGE("RAG Hub"),
    STUDIO("Studio"),
    MAPS("Maps"),
    YOUTUBE("YouTube"),
    INTEL_HUB("Hub"),
    PERSONA("Persona"),
    LEARN("Learn AI"),
    NEWS("News"),
    SEARCH("Search"),
    READ_ALOUD("Read Aloud")
}

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    val preferencesManager = PreferencesManager(application)
    private val database = AppDatabase.getInstance(application)
    val repository = ChatRepository(database.chatDao())
    val usageTracker = UsageTracker(application, preferencesManager)

    private val _currentSection = MutableStateFlow(AppSection.CHAT)
    val currentSection: StateFlow<AppSection> = _currentSection.asStateFlow()

    fun navigateToSection(section: AppSection) {
        _currentSection.value = section
    }

    private val _isWebSearchEnabled = MutableStateFlow(false)
    val isWebSearchEnabled: StateFlow<Boolean> = _isWebSearchEnabled.asStateFlow()

    fun toggleWebSearch() {
        _isWebSearchEnabled.value = !_isWebSearchEnabled.value
    }

    val ragEngine = RagEngine(database.ragDao())

    val ragDocuments: StateFlow<List<KnowledgeDocumentEntity>> = database.ragDao().getAllDocuments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isRagEnabled = MutableStateFlow(true)
    val isRagEnabled: StateFlow<Boolean> = _isRagEnabled.asStateFlow()

    fun toggleRag() {
        _isRagEnabled.value = !_isRagEnabled.value
    }

    private val _ragStats = MutableStateFlow(RagEngineStats())
    val ragStats: StateFlow<RagEngineStats> = _ragStats.asStateFlow()

    private val _testSearchResults = MutableStateFlow<List<RetrievedChunk>>(emptyList())
    val testSearchResults: StateFlow<List<RetrievedChunk>> = _testSearchResults.asStateFlow()

    private val _isRagProcessing = MutableStateFlow(false)
    val isRagProcessing: StateFlow<Boolean> = _isRagProcessing.asStateFlow()

    fun refreshRagStats() {
        viewModelScope.launch {
            _ragStats.value = ragEngine.getStats(preferencesManager.geminiApiKey)
        }
    }

    fun ingestTextToRag(title: String, content: String, sourceType: String = "MANUAL", onComplete: (Boolean, String) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            _isRagProcessing.value = true
            val res = ragEngine.ingestDocument(
                title = title,
                content = content,
                sourceType = sourceType,
                geminiApiKey = preferencesManager.geminiApiKey
            )
            _isRagProcessing.value = false
            refreshRagStats()
            if (res.isSuccess) {
                onComplete(true, "Document indexed with ${res.getOrNull()?.chunkCount ?: 0} chunks")
            } else {
                onComplete(false, res.exceptionOrNull()?.localizedMessage ?: "Failed to index document")
            }
        }
    }

    fun ingestFileToRag(uri: Uri, onComplete: (Boolean, String) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            _isRagProcessing.value = true
            val res = ragEngine.ingestFile(
                context = getApplication(),
                uri = uri,
                geminiApiKey = preferencesManager.geminiApiKey
            )
            _isRagProcessing.value = false
            refreshRagStats()
            if (res.isSuccess) {
                onComplete(true, "Document '${res.getOrNull()?.title}' indexed (${res.getOrNull()?.chunkCount ?: 0} chunks)")
            } else {
                onComplete(false, res.exceptionOrNull()?.localizedMessage ?: "Failed to ingest file")
            }
        }
    }

    fun deleteRagDocument(docId: String) {
        viewModelScope.launch {
            database.ragDao().deleteDocumentWithChunks(docId)
            refreshRagStats()
        }
    }

    suspend fun getChunksForDocument(docId: String): List<KnowledgeChunkEntity> = withContext(Dispatchers.IO) {
        database.ragDao().getChunksForDoc(docId)
    }

    fun clearEntireRag() {
        viewModelScope.launch {
            database.ragDao().clearEntireKnowledgeBase()
            _testSearchResults.value = emptyList()
            refreshRagStats()
        }
    }

    fun loadStarterKnowledgeBase(onComplete: (Boolean, String) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            _isRagProcessing.value = true
            val res = ragEngine.populateStarterKnowledgeBase(preferencesManager.geminiApiKey)
            _isRagProcessing.value = false
            refreshRagStats()
            if (res.isSuccess) {
                onComplete(true, "Loaded ${res.getOrNull() ?: 0} starter knowledge base documents")
            } else {
                onComplete(false, res.exceptionOrNull()?.localizedMessage ?: "Failed to load starter docs")
            }
        }
    }

    fun testRagQuery(query: String) {
        viewModelScope.launch {
            _isRagProcessing.value = true
            val results = ragEngine.search(query, topK = 5, geminiApiKey = preferencesManager.geminiApiKey)
            _testSearchResults.value = results
            _isRagProcessing.value = false
        }
    }

    val usageStats: StateFlow<UsageStats> = usageTracker.usageStats

    val sessions: StateFlow<List<ChatSessionEntity>> = repository.allSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentSessionId = MutableStateFlow<String?>(null)
    val currentSessionId: StateFlow<String?> = _currentSessionId.asStateFlow()

    private val _currentSession = MutableStateFlow<ChatSessionEntity?>(null)
    val currentSession: StateFlow<ChatSessionEntity?> = _currentSession.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val currentMessages: StateFlow<List<ChatMessageEntity>> = _currentSessionId
        .flatMapLatest { id ->
            if (id != null) repository.getMessagesForSession(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentSessionArtifacts: StateFlow<List<ArtifactItem>> = currentMessages
        .map { list -> list.flatMap { ArtifactExtractor.extractFromMessage(it) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allArtifacts: StateFlow<List<ArtifactItem>> = repository.allMessages
        .map { list -> list.flatMap { ArtifactExtractor.extractFromMessage(it) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedArtifact = MutableStateFlow<ArtifactItem?>(null)
    val selectedArtifact: StateFlow<ArtifactItem?> = _selectedArtifact.asStateFlow()

    fun selectArtifact(artifact: ArtifactItem?) {
        _selectedArtifact.value = artifact
    }

    private val _pendingAttachments = MutableStateFlow<List<AttachmentInfo>>(emptyList())
    val pendingAttachments: StateFlow<List<AttachmentInfo>> = _pendingAttachments.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _continuingMessageId = MutableStateFlow<String?>(null)
    val continuingMessageId: StateFlow<String?> = _continuingMessageId.asStateFlow()

    private val _selectedProvider = MutableStateFlow(
        ProviderType.values().firstOrNull { it.id == preferencesManager.lastProviderId } ?: ProviderType.POLLINATIONS
    )
    val selectedProvider: StateFlow<ProviderType> = _selectedProvider.asStateFlow()

    private val _selectedModel = MutableStateFlow(
        AvailableModels.findModel(preferencesManager.lastModelId, preferencesManager.lastProviderId)
    )
    val selectedModel: StateFlow<AiModel> = _selectedModel.asStateFlow()

    private val _speakingMessageId = MutableStateFlow<String?>(null)
    val speakingMessageId: StateFlow<String?> = _speakingMessageId.asStateFlow()

    private val _selectedTheme = MutableStateFlow(preferencesManager.selectedTheme)
    val selectedTheme: StateFlow<String> = _selectedTheme.asStateFlow()

    private val _themeMode = MutableStateFlow(preferencesManager.themeMode)
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _keysRevision = MutableStateFlow(0)
    val keysRevision: StateFlow<Int> = _keysRevision.asStateFlow()

    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private var activeJob: Job? = null

    fun setTheme(themeId: String) {
        preferencesManager.selectedTheme = themeId
        _selectedTheme.value = themeId
    }

    fun setThemeMode(mode: String) {
        preferencesManager.themeMode = mode
        _themeMode.value = mode
    }

    fun updateApiKey(provider: ProviderType, key: String) {
        when (provider) {
            ProviderType.GEMINI -> preferencesManager.geminiApiKey = key
            ProviderType.GROQ -> preferencesManager.groqApiKey = key
            ProviderType.OPENROUTER -> preferencesManager.openRouterApiKey = key
            ProviderType.CEREBRAS -> preferencesManager.cerebrasApiKey = key
            ProviderType.HUGGINGFACE -> preferencesManager.huggingFaceApiKey = key
            ProviderType.CUSTOM -> preferencesManager.customApiKey = key
            ProviderType.POLLINATIONS -> {}
        }
        _keysRevision.value += 1
    }

    fun updateYouTubeApiKey(key: String) {
        preferencesManager.youtubeApiKey = key
        _keysRevision.value += 1
    }

    fun updateGoogleMapsApiKey(key: String) {
        preferencesManager.googleMapsApiKey = key
        _keysRevision.value += 1
    }

    fun setDailyLimit(limit: Int) {
        usageTracker.setDailyLimit(limit)
    }

    fun resetTodayUsage() {
        usageTracker.resetTodayStats()
    }

    fun exportKeys(): String = preferencesManager.exportKeysJson()

    fun importKeys(json: String): Boolean {
        val success = preferencesManager.importKeysJson(json)
        if (success) {
            _keysRevision.value += 1
        }
        return success
    }

    init {
        // Auto-migrate any deprecated model stored in preferences (e.g. gemini-2.0-flash -> gemini-3.8-flash)
        if (preferencesManager.lastProviderId == ProviderType.GEMINI.id) {
            val mappedModel = AvailableModels.findModel(preferencesManager.lastModelId, ProviderType.GEMINI.id)
            if (mappedModel.id != preferencesManager.lastModelId) {
                preferencesManager.lastModelId = mappedModel.id
                _selectedModel.value = mappedModel
            }
        }

        initTts(application)
        viewModelScope.launch {
            // Query DB once first to prevent empty session flash
            val initialList = repository.getAllSessionsOnce()
            if (initialList.isNotEmpty()) {
                val first = initialList.first()
                _currentSessionId.value = first.id
                _currentSession.value = first
                syncSessionModel(first)
            } else {
                createNewSession()
            }

            // Observe subsequent session changes
            sessions.collect { list ->
                val currentId = _currentSessionId.value
                if (currentId != null) {
                    val matching = list.firstOrNull { it.id == currentId }
                    if (matching != null) {
                        _currentSession.value = matching
                    } else if (list.isNotEmpty()) {
                        val first = list.first()
                        _currentSessionId.value = first.id
                        _currentSession.value = first
                        syncSessionModel(first)
                    }
                }
            }
            // Auto-initialize RAG knowledge base if empty and fetch stats
            launch {
                try {
                    if (database.ragDao().getDocumentCount() == 0) {
                        ragEngine.populateStarterKnowledgeBase(preferencesManager.geminiApiKey)
                    }
                    refreshRagStats()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun syncSessionModel(session: ChatSessionEntity) {
        val prov = ProviderType.values().firstOrNull { it.id == session.providerId } ?: ProviderType.POLLINATIONS
        val mod = AvailableModels.findModel(session.modelId, prov.id)
        _selectedProvider.value = prov
        _selectedModel.value = mod
    }

    fun selectSession(sessionId: String) {
        viewModelScope.launch {
            val session = repository.getSession(sessionId) ?: return@launch
            _currentSessionId.value = session.id
            _currentSession.value = session
            syncSessionModel(session)
            _pendingAttachments.value = emptyList()
            stopSpeech()
        }
    }

    fun createNewSession() {
        viewModelScope.launch {
            stopSpeech()
            val prov = _selectedProvider.value
            val mod = _selectedModel.value
            val session = repository.createNewSession(prov.id, mod.id, "New Chat")
            _currentSessionId.value = session.id
            _currentSession.value = session
            _pendingAttachments.value = emptyList()
        }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            repository.deleteSession(sessionId)
            if (_currentSessionId.value == sessionId) {
                _currentSessionId.value = null
                _currentSession.value = null
            }
        }
    }

    fun clearAllChats() {
        viewModelScope.launch {
            stopSpeech()
            repository.clearAll()
            _currentSessionId.value = null
            _currentSession.value = null
            _pendingAttachments.value = emptyList()
            createNewSession()
        }
    }

    fun setProviderAndModel(provider: ProviderType, model: AiModel) {
        _selectedProvider.value = provider
        _selectedModel.value = model
        preferencesManager.lastProviderId = provider.id
        preferencesManager.lastModelId = model.id

        val currentId = _currentSessionId.value
        if (currentId != null) {
            viewModelScope.launch {
                repository.updateSessionModel(currentId, provider.id, model.id)
            }
        }
    }

    fun isKeyConfigured(provider: ProviderType): Boolean {
        return when (provider) {
            ProviderType.POLLINATIONS -> true
            ProviderType.GEMINI -> preferencesManager.geminiApiKey.isNotBlank()
            ProviderType.GROQ -> preferencesManager.groqApiKey.isNotBlank()
            ProviderType.CEREBRAS -> preferencesManager.cerebrasApiKey.isNotBlank()
            ProviderType.OPENROUTER -> preferencesManager.openRouterApiKey.isNotBlank()
            ProviderType.HUGGINGFACE -> preferencesManager.huggingFaceApiKey.isNotBlank()
            ProviderType.CUSTOM -> true
        }
    }

    fun getApiKeyForProvider(provider: ProviderType): String {
        return when (provider) {
            ProviderType.POLLINATIONS -> ""
            ProviderType.GEMINI -> preferencesManager.geminiApiKey
            ProviderType.GROQ -> preferencesManager.groqApiKey
            ProviderType.CEREBRAS -> preferencesManager.cerebrasApiKey
            ProviderType.OPENROUTER -> preferencesManager.openRouterApiKey
            ProviderType.HUGGINGFACE -> preferencesManager.huggingFaceApiKey
            ProviderType.CUSTOM -> preferencesManager.customApiKey
        }
    }

    fun addAttachments(uris: List<Uri>) {
        viewModelScope.launch {
            val app = getApplication<Application>()
            val processed = uris.mapNotNull { FileUtils.processPickedUri(app, it) }
            _pendingAttachments.value = _pendingAttachments.value + processed
        }
    }

    fun addCachedFileAttachment(file: File) {
        viewModelScope.launch {
            val mime = FileUtils.resolveMimeType(file.name, null, null)
            val isImage = mime.startsWith("image/") || FileUtils.isImageExtension(file.name)
            val snippet = if (!isImage) FileUtils.readFullTextContent(file, 240).take(200) + "..." else null
            val att = AttachmentInfo(
                id = java.util.UUID.randomUUID().toString(),
                name = file.name,
                mimeType = mime,
                sizeBytes = file.length(),
                localUri = file.absolutePath,
                isImage = isImage,
                previewSnippet = snippet
            )
            _pendingAttachments.value = _pendingAttachments.value + att
        }
    }

    fun addSampleAttachment(sampleType: String) {
        viewModelScope.launch {
            val app = getApplication<Application>()
            val sample = FileUtils.createSampleAttachment(app, sampleType)
            _pendingAttachments.value = _pendingAttachments.value + sample
        }
    }

    fun removePendingAttachment(attachmentId: String) {
        _pendingAttachments.value = _pendingAttachments.value.filterNot { it.id == attachmentId }
    }

    fun sendMessage(userText: String, explicitAttachments: List<AttachmentInfo>? = null): Boolean {
        val trimmed = userText.trim()
        val attachments = explicitAttachments ?: _pendingAttachments.value
        if (trimmed.isEmpty() && attachments.isEmpty()) return false

        val sessionId = _currentSessionId.value
        if (sessionId == null) {
            viewModelScope.launch {
                val prov = _selectedProvider.value
                val mod = _selectedModel.value
                val session = repository.createNewSession(prov.id, mod.id, "New Chat")
                _currentSessionId.value = session.id
                _currentSession.value = session
                sendMessage(userText, explicitAttachments)
            }
            return true
        }
        val provider = _selectedProvider.value
        val model = _selectedModel.value

        // Check if provider requires API key and user has not configured it
        if (provider.requiresApiKey && !isKeyConfigured(provider)) {
            viewModelScope.launch {
                // Insert user message
                repository.addMessage(
                    sessionId = sessionId,
                    role = "user",
                    content = trimmed.ifEmpty { "Uploaded ${attachments.size} file(s)" },
                    providerId = provider.id,
                    modelId = model.id,
                    attachments = attachments
                )
                // Insert helpful error guidance
                repository.addMessage(
                    sessionId = sessionId,
                    role = "assistant",
                    content = "⚠️ ${provider.displayName} requires an API key to run. Please tap below to add your free key in Settings, or switch to Pollinations AI (Free) in the model picker!",
                    providerId = provider.id,
                    modelId = model.id,
                    isError = true
                )
                if (explicitAttachments == null) _pendingAttachments.value = emptyList()
            }
            return true
        }

        // Check if daily request budget is reached
        if (usageTracker.isLimitReached()) {
            viewModelScope.launch {
                repository.addMessage(
                    sessionId = sessionId,
                    role = "user",
                    content = trimmed.ifEmpty { "Uploaded ${attachments.size} file(s)" },
                    providerId = provider.id,
                    modelId = model.id,
                    attachments = attachments
                )
                repository.addMessage(
                    sessionId = sessionId,
                    role = "assistant",
                    content = "⚠️ You have reached your daily budget of ${preferencesManager.dailyLimit} requests today. Tap the limit badge in the top bar to increase your limit or switch to Unlimited.",
                    providerId = provider.id,
                    modelId = model.id,
                    isError = true
                )
                if (explicitAttachments == null) _pendingAttachments.value = emptyList()
            }
            return true
        }

        // Check if user uploaded images to a model that does not support vision
        if (attachments.any { it.isImage } && !model.supportsVision && provider != ProviderType.CUSTOM) {
            viewModelScope.launch {
                repository.addMessage(
                    sessionId = sessionId,
                    role = "user",
                    content = trimmed.ifEmpty { "Uploaded ${attachments.size} image(s)" },
                    providerId = provider.id,
                    modelId = model.id,
                    attachments = attachments
                )
                repository.addMessage(
                    sessionId = sessionId,
                    role = "assistant",
                    content = "⚠️ The selected model (${model.name}) does not support vision. Please switch to a vision-capable model (like Google Gemini 2.5 Flash / Pro) in the model selector to analyze images.",
                    providerId = provider.id,
                    modelId = model.id,
                    isError = true
                )
                if (explicitAttachments == null) _pendingAttachments.value = emptyList()
            }
            return true
        }

        activeJob?.cancel()
        activeJob = viewModelScope.launch {
            _isGenerating.value = true
            val displayPrompt = trimmed.ifEmpty { "Analyze the attached file(s) and extract all key information, summary, and details." }

            // Snapshot history BEFORE inserting the new user message (excludes errors & placeholders)
            val historySnapshot = currentMessages.value.filter {
                !it.isError && it.content.isNotBlank() && it.content != "Regenerating response..."
            }

            // Insert user message in database
            repository.addMessage(
                sessionId = sessionId,
                role = "user",
                content = displayPrompt,
                providerId = provider.id,
                modelId = model.id,
                attachments = attachments
            )

            // Update title if first message
            val existing = currentMessages.value
            if (existing.isEmpty() || existing.size <= 1) {
                val titleSnippet = if (displayPrompt.length > 28) displayPrompt.take(28) + "..." else displayPrompt
                repository.updateSessionTitle(sessionId, titleSnippet)
            }

            // Clear pending attachments for next message
            if (explicitAttachments == null) {
                _pendingAttachments.value = emptyList()
            }

            // Call AI Service with optional live web grounding
            val apiKey = getApiKeyForProvider(provider)

            val webContext = if (_isWebSearchEnabled.value && trimmed.isNotBlank()) {
                try {
                    val searchResults = WebSearchService.search(trimmed).getOrNull()
                    if (!searchResults.isNullOrEmpty()) {
                        "\n\n[Live Real-Time Web Search Results & Sources for '$trimmed']:\n" +
                                searchResults.take(5).joinToString("\n") { "• [${it.source}] ${it.title} (${it.pubDate}): ${it.snippet} (Source: ${it.url})" } +
                                "\n\nNote for Assistant: You have live real-time internet access active. Use the above verified live web sources to provide an accurate, up-to-date answer and cite the publisher/sources."
                    } else ""
                } catch (_: Exception) { "" }
            } else ""

            val ragContext = if (_isRagEnabled.value && trimmed.isNotBlank()) {
                try {
                    val geminiKey = preferencesManager.geminiApiKey
                    val retrieved = ragEngine.search(trimmed, topK = 4, geminiApiKey = geminiKey)
                    if (retrieved.isNotEmpty()) {
                        ragEngine.formatRagGroundingPrompt(retrieved)
                    } else ""
                } catch (_: Exception) { "" }
            } else ""

            val promptWithGrounding = displayPrompt + webContext + ragContext
            val effectiveModelId = if (provider == ProviderType.CUSTOM) {
                preferencesManager.customModel.trim().ifEmpty { "gpt-4o-mini" }
            } else {
                model.id
            }

            val result = AiService.sendMessage(
                provider = provider,
                modelId = effectiveModelId,
                prompt = promptWithGrounding,
                attachments = attachments,
                history = historySnapshot,
                apiKey = apiKey,
                systemPrompt = preferencesManager.systemPrompt,
                temperature = preferencesManager.temperature,
                customBaseUrl = preferencesManager.customBaseUrl,
                supportsVision = model.supportsVision || provider == ProviderType.CUSTOM,
                maxTokens = preferencesManager.maxOutputTokens
            )

            result.onSuccess { responseText ->
                repository.addMessage(
                    sessionId = sessionId,
                    role = "assistant",
                    content = responseText,
                    providerId = provider.id,
                    modelId = model.id,
                    isError = false
                )
                val estTokens = (displayPrompt.length + responseText.length) / 4
                usageTracker.recordRequest(provider.id, maxOf(50, estTokens))
            }.onFailure { error ->
                val isCancelled = !isActive || error is kotlinx.coroutines.CancellationException || error.message?.contains("Canceled", ignoreCase = true) == true
                if (isCancelled) {
                    return@onFailure
                }
                repository.addMessage(
                    sessionId = sessionId,
                    role = "assistant",
                    content = "Error from ${provider.displayName}: ${error.localizedMessage ?: "Unknown error"}",
                    providerId = provider.id,
                    modelId = model.id,
                    isError = true
                )
            }

            _isGenerating.value = false
        }
        return true
    }

    fun regenerateMessage(assistantMessage: ChatMessageEntity) {
        val sessionId = _currentSessionId.value ?: return
        val allMsgs = currentMessages.value
        val msgIndex = allMsgs.indexOfFirst { it.id == assistantMessage.id }
        if (msgIndex <= 0) return

        val userMessage = allMsgs[msgIndex - 1]
        val attachments = repository.parseAttachments(userMessage.attachmentsJson)
        val provider = _selectedProvider.value
        val model = _selectedModel.value

        // Pre-flight key and limits checks
        if (provider.requiresApiKey && !isKeyConfigured(provider)) {
            viewModelScope.launch {
                repository.updateMessageContent(
                    assistantMessage.id,
                    "⚠️ ${provider.displayName} requires an API key to run. Please add your key in Settings or switch to Pollinations AI (Free).",
                    isError = true
                )
            }
            return
        }

        if (usageTracker.isLimitReached()) {
            viewModelScope.launch {
                repository.updateMessageContent(
                    assistantMessage.id,
                    "⚠️ Daily budget of ${preferencesManager.dailyLimit} requests reached today.",
                    isError = true
                )
            }
            return
        }

        val originalContent = assistantMessage.content

        activeJob?.cancel()
        activeJob = viewModelScope.launch {
            _isGenerating.value = true
            repository.updateMessageContent(assistantMessage.id, "Regenerating response...")

            val apiKey = getApiKeyForProvider(provider)
            val history = allMsgs.take(msgIndex).filter {
                !it.isError && it.id != assistantMessage.id && it.content != "Regenerating response..."
            }
            val effectiveModelId = if (provider == ProviderType.CUSTOM) {
                preferencesManager.customModel.trim().ifEmpty { "gpt-4o-mini" }
            } else {
                model.id
            }

            val ragContext = if (_isRagEnabled.value && userMessage.content.isNotBlank()) {
                try {
                    val geminiKey = preferencesManager.geminiApiKey
                    val retrieved = ragEngine.search(userMessage.content, topK = 4, geminiApiKey = geminiKey)
                    if (retrieved.isNotEmpty()) {
                        ragEngine.formatRagGroundingPrompt(retrieved)
                    } else ""
                } catch (_: Exception) { "" }
            } else ""

            val regenPrompt = userMessage.content + ragContext

            val result = AiService.sendMessage(
                provider = provider,
                modelId = effectiveModelId,
                prompt = regenPrompt,
                attachments = attachments,
                history = history,
                apiKey = apiKey,
                systemPrompt = preferencesManager.systemPrompt,
                temperature = preferencesManager.temperature,
                customBaseUrl = preferencesManager.customBaseUrl,
                supportsVision = model.supportsVision || provider == ProviderType.CUSTOM,
                maxTokens = preferencesManager.maxOutputTokens
            )

            result.onSuccess { responseText ->
                repository.updateMessageContent(assistantMessage.id, responseText, isError = false)
                val estTokens = (userMessage.content.length + responseText.length) / 4
                usageTracker.recordRequest(provider.id, maxOf(50, estTokens))
            }.onFailure { error ->
                val isCancelled = !isActive || error is kotlinx.coroutines.CancellationException || error is java.util.concurrent.CancellationException || error.message?.contains("Canceled", ignoreCase = true) == true
                repository.updateMessageContent(
                    assistantMessage.id,
                    if (isCancelled) originalContent else "Error: ${error.localizedMessage ?: "Failed to regenerate"}",
                    isError = !isCancelled
                )
            }

            _isGenerating.value = false
        }
    }

    /**
     * Seamlessly continue generation for an assistant reply that stopped prematurely
     * and append the completed continuation directly into the message.
     */
    fun continueMessage(assistantMessage: ChatMessageEntity) {
        val sessionId = _currentSessionId.value ?: return
        val allMsgs = currentMessages.value
        val msgIndex = allMsgs.indexOfFirst { it.id == assistantMessage.id }
        if (msgIndex < 0) return

        val provider = _selectedProvider.value
        val model = _selectedModel.value

        // Pre-flight key and limits checks
        if (provider.requiresApiKey && !isKeyConfigured(provider)) {
            viewModelScope.launch {
                repository.updateMessageContent(
                    assistantMessage.id,
                    "${assistantMessage.content}\n\n⚠️ ${provider.displayName} requires an API key to continue. Please add your key in Settings.",
                    isError = true
                )
            }
            return
        }

        if (usageTracker.isLimitReached()) {
            viewModelScope.launch {
                repository.updateMessageContent(
                    assistantMessage.id,
                    "${assistantMessage.content}\n\n⚠️ Daily budget reached.",
                    isError = true
                )
            }
            return
        }

        val originalContent = assistantMessage.content

        activeJob?.cancel()
        activeJob = viewModelScope.launch {
            _isGenerating.value = true
            _continuingMessageId.value = assistantMessage.id

            try {
                val apiKey = getApiKeyForProvider(provider)
                // History up to and including the current assistant message
                val history = allMsgs.take(msgIndex + 1).filter { !it.isError }

                val effectiveModelId = if (provider == ProviderType.CUSTOM) {
                    preferencesManager.customModel.trim().ifEmpty { "gpt-4o-mini" }
                } else {
                    model.id
                }

                val continueInstruction = "Continue your previous response directly from where you stopped. Do not repeat what you already wrote and do not include conversational filler like 'Sure' or 'Continuing'. Immediately start with the continuation text and complete the answer fully with rich elaboration:"

                val result = AiService.sendMessage(
                    provider = provider,
                    modelId = effectiveModelId,
                    prompt = continueInstruction,
                    attachments = emptyList(),
                    history = history,
                    apiKey = apiKey,
                    systemPrompt = preferencesManager.systemPrompt,
                    temperature = preferencesManager.temperature,
                    customBaseUrl = preferencesManager.customBaseUrl,
                    supportsVision = false,
                    maxTokens = preferencesManager.maxOutputTokens
                )

                result.onSuccess { continuationText ->
                    val cleanedContinuation = continuationText.trim()
                    if (cleanedContinuation.isNotBlank() && cleanedContinuation != "No response content received.") {
                        val current = originalContent.trimEnd()
                        val separator = if (current.endsWith("\n") || current.endsWith("```")) "\n\n" else if (current.endsWith(".")) " " else " "
                        val combinedContent = current + separator + cleanedContinuation
                        repository.updateMessageContent(assistantMessage.id, combinedContent, isError = false)

                        val estTokens = (continueInstruction.length + cleanedContinuation.length) / 4
                        usageTracker.recordRequest(provider.id, maxOf(50, estTokens))
                    }
                }.onFailure { error ->
                    val isCancelled = !isActive || error is kotlinx.coroutines.CancellationException || error is java.util.concurrent.CancellationException || error.message?.contains("Canceled", ignoreCase = true) == true
                    if (!isCancelled) {
                        repository.updateMessageContent(
                            assistantMessage.id,
                            "$originalContent\n\n[Continuation error: ${error.localizedMessage ?: "Failed to continue"}]",
                            isError = false
                        )
                    }
                }
            } finally {
                _isGenerating.value = false
                _continuingMessageId.value = null
            }
        }
    }

    fun stopGeneration() {
        activeJob?.cancel()
        AiService.cancelActiveRequest()
        _isGenerating.value = false
        _continuingMessageId.value = null
    }

    fun onKeysUpdated() {
        _keysRevision.value += 1
    }

    private fun initTts(application: Application) {
        tts = TextToSpeech(application) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                isTtsReady = true
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    override fun onDone(utteranceId: String?) {
                        _speakingMessageId.value = null
                    }
                    override fun onError(utteranceId: String?) {
                        _speakingMessageId.value = null
                    }
                })
            }
        }
    }

    fun toggleSpeech(messageId: String, text: String) {
        if (_speakingMessageId.value == messageId) {
            stopSpeech()
        } else {
            stopSpeech()
            if (isTtsReady) {
                // Strip markdown code blocks and format symbols for clean spoken voice without destroying identifiers
                var spoken = text.replace(Regex("```[\\s\\S]*?```"), "Code snippet omitted.")
                    .replace(Regex("`([^`]+)`"), "$1")
                    .replace(Regex("""\*\*([^*]+)\*\*"""), "$1")
                    .replace(Regex("""\*([^*]+)\*"""), "$1")
                    .replace(Regex("""(?m)^[#>•\-*]\s*"""), "")
                    .replace(Regex("""(?<=\s|^)[_]+|[_]+(?=\s|$)"""), "")

                if (spoken.length > 2000) {
                    spoken = spoken.take(2000) + "... Text truncated for speech."
                }
                _speakingMessageId.value = messageId
                tts?.speak(spoken, TextToSpeech.QUEUE_FLUSH, null, messageId)
            }
        }
    }

    private fun stopSpeech() {
        _speakingMessageId.value = null
        tts?.stop()
    }

    override fun onCleared() {
        super.onCleared()
        activeJob?.cancel()
        tts?.stop()
        tts?.shutdown()
    }
}
