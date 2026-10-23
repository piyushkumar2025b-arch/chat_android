package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ChatRepository
import com.example.data.local.AppDatabase
import com.example.data.local.AppLockManager
import com.example.data.local.CalendarEventEntity
import com.example.data.local.FocusSessionEntity
import com.example.data.local.HabitCheckInEntity
import com.example.data.local.HabitEntity
import com.example.data.local.MemoryEntity
import com.example.data.local.NoteEntity
import com.example.data.local.PreferencesManager
import com.example.data.local.PromptTemplateEntity
import com.example.data.local.UsageStats
import com.example.data.local.UsageTracker
import com.example.data.local.VaultItemEntity
import com.example.data.model.AiModel
import com.example.data.model.AttachmentInfo
import com.example.data.model.AvailableModels
import com.example.data.model.ChatMessageEntity
import com.example.data.model.ChatSearchGroup
import com.example.data.model.ChatSessionEntity
import com.example.data.model.KnowledgeChunkEntity
import com.example.data.model.KnowledgeDocumentEntity
import com.example.data.model.ProviderType
import com.example.data.model.RagEngineStats
import com.example.data.model.RetrievedChunk
import com.example.data.remote.AiService
import com.example.data.remote.ApiRequestException
import com.example.data.remote.AssistantContextBuilder
import com.example.data.remote.BackupManager
import com.example.data.remote.ConversationExporter
import com.example.data.remote.FileUtils
import com.example.data.remote.InternalKnowledgeSources
import com.example.data.remote.MediaSaver
import com.example.data.remote.MissingApiKeyException
import com.example.data.remote.RagEngine
import com.example.data.seed.StarterPrompts
import com.example.model.AppSection
import com.example.model.ChatAttachment
import com.example.model.ChatMessage
import com.example.model.MessageRole
import com.example.model.MessageStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application
    private val db = AppDatabase.getDatabase(application)
    val productivityDao = db.productivityDao()
    val chatDao = db.chatDao()
    val ragDao = db.ragDao()
    val memoryDao = db.memoryDao()
    val promptDao = db.promptDao()
    val focusDao = db.focusDao()
    val ragEngine = RagEngine(ragDao)
    val backupManager = BackupManager(db)
    val appLockManager = AppLockManager(application)

    val preferencesManager = PreferencesManager(application)
    private val chatRepository = ChatRepository(chatDao)
    private val usageTracker = UsageTracker(application, preferencesManager)

    val usageStats: StateFlow<UsageStats> = usageTracker.usageStats

    val notes: StateFlow<List<NoteEntity>> = productivityDao.getAllNotes()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val events: StateFlow<List<CalendarEventEntity>> = productivityDao.getAllEvents()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val vaultItems: StateFlow<List<VaultItemEntity>> = productivityDao.getAllVaultItems()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val memories: StateFlow<List<MemoryEntity>> = memoryDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val promptTemplates: StateFlow<List<PromptTemplateEntity>> = promptDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val habits: StateFlow<List<HabitEntity>> = focusDao.observeHabits()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val habitCheckIns: StateFlow<List<HabitCheckInEntity>> = focusDao.observeAllCheckIns()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val focusSessions: StateFlow<List<FocusSessionEntity>> = focusDao.observeFocusSessions()
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

    private val _activeProvider = MutableStateFlow(initialProvider())
    val activeProvider: StateFlow<ProviderType> = _activeProvider.asStateFlow()

    private val _activeModel = MutableStateFlow(resolveModel(_activeProvider.value))
    val activeModel: StateFlow<AiModel> = _activeModel.asStateFlow()

    private val _apiKeys = MutableStateFlow(loadApiKeys())
    val apiKeys: StateFlow<Map<ProviderType, String>> = _apiKeys.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _sessions = MutableStateFlow<List<ChatSessionEntity>>(emptyList())
    val sessions: StateFlow<List<ChatSessionEntity>> = _sessions.asStateFlow()

    private val _activeSessionId = MutableStateFlow<String?>(null)
    val activeSessionId: StateFlow<String?> = _activeSessionId.asStateFlow()

    private val _pendingAttachments = MutableStateFlow<List<ChatAttachment>>(emptyList())
    val pendingAttachments: StateFlow<List<ChatAttachment>> = _pendingAttachments.asStateFlow()

    private val _themeId = MutableStateFlow(preferencesManager.selectedTheme)
    val themeId: StateFlow<String> = _themeId.asStateFlow()

    private val _themeMode = MutableStateFlow(preferencesManager.themeMode)
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _memoryEnabled = MutableStateFlow(preferencesManager.memoryEnabled)
    val memoryEnabled: StateFlow<Boolean> = _memoryEnabled.asStateFlow()

    private val _isAppUnlocked = MutableStateFlow(!appLockManager.isEnabled())
    val isAppUnlocked: StateFlow<Boolean> = _isAppUnlocked.asStateFlow()

    private val _isAppLockEnabled = MutableStateFlow(appLockManager.isEnabled())
    val isAppLockEnabled: StateFlow<Boolean> = _isAppLockEnabled.asStateFlow()

    private val _composerDraft = MutableStateFlow<String?>(null)
    val composerDraft: StateFlow<String?> = _composerDraft.asStateFlow()

    private val _focusMessageId = MutableStateFlow<String?>(null)
    val focusMessageId: StateFlow<String?> = _focusMessageId.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _isSessionReady = MutableStateFlow(false)
    val isSessionReady: StateFlow<Boolean> = _isSessionReady.asStateFlow()

    private val _isPreparingAttachment = MutableStateFlow(false)
    val isPreparingAttachment: StateFlow<Boolean> = _isPreparingAttachment.asStateFlow()

    private var generationJob: Job? = null

    // Studio Handoff States
    private val _studioCodeHandoff = MutableStateFlow<Triple<String, String, String>?>(null)
    val studioCodeHandoff: StateFlow<Triple<String, String, String>?> = _studioCodeHandoff.asStateFlow()

    private val _studioMarkdownHandoff = MutableStateFlow<Pair<String, String>?>(null)
    val studioMarkdownHandoff: StateFlow<Pair<String, String>?> = _studioMarkdownHandoff.asStateFlow()

    init {
        refreshRagStats()
        viewModelScope.launch { initialiseConversation() }
        viewModelScope.launch {
            runCatching {
                if (promptDao.count() == 0) {
                    promptDao.insertAll(StarterPrompts.toEntities())
                }
            }
        }
    }

    private fun initialProvider(): ProviderType {
        val savedId = preferencesManager.lastProviderId
        return ProviderType.entries.firstOrNull { it.id == savedId } ?: ProviderType.POLLINATIONS
    }

    private fun resolveModel(provider: ProviderType, preferredId: String? = null): AiModel {
        val preferred = preferredId ?: preferencesManager.lastModelId
        return AvailableModels.models.firstOrNull { it.provider == provider && it.id == preferred }
            ?: AvailableModels.models.firstOrNull { it.provider == provider }
            ?: AvailableModels.defaultModel
    }

    private fun loadApiKeys(): Map<ProviderType, String> =
        ProviderType.entries.associateWith { preferencesManager.getApiKey(it) }

    private suspend fun initialiseConversation() {
        try {
            var allSessions = chatRepository.getAllSessionsOnce()
            if (allSessions.isEmpty()) {
                chatRepository.createNewSession(
                    providerId = _activeProvider.value.id,
                    modelId = _activeModel.value.id
                )
                allSessions = chatRepository.getAllSessionsOnce()
            }
            _sessions.value = allSessions
            allSessions.firstOrNull()?.let { activateSession(it) }
        } finally {
            _isSessionReady.value = true
        }
    }

    private suspend fun activateSession(session: ChatSessionEntity) {
        val provider = ProviderType.entries.firstOrNull { it.id == session.providerId }
            ?: _activeProvider.value
        val model = resolveModel(provider, session.modelId)
        _activeProvider.value = provider
        _activeModel.value = model
        preferencesManager.lastProviderId = provider.id
        preferencesManager.lastModelId = model.id
        _activeSessionId.value = session.id
        val entities = withContext(Dispatchers.IO) {
            chatDao.getMessagesListForSession(session.id)
        }
        _messages.value = entities.map { it.toChatMessage() }
    }

    private suspend fun refreshSessionsList() {
        _sessions.value = withContext(Dispatchers.IO) { chatRepository.getAllSessionsOnce() }
    }

    fun navigateToSection(section: AppSection) {
        _selectedSection.value = section
    }

    fun setProvider(provider: ProviderType) {
        if (provider == _activeProvider.value) return
        _activeProvider.value = provider
        _activeModel.value = resolveModel(provider)
        preferencesManager.lastProviderId = provider.id
        preferencesManager.lastModelId = _activeModel.value.id
        persistActiveSessionModel()
    }

    fun setModel(provider: ProviderType, model: AiModel) {
        _activeProvider.value = provider
        _activeModel.value = model
        preferencesManager.lastProviderId = provider.id
        preferencesManager.lastModelId = model.id
        persistActiveSessionModel()
    }

    private fun persistActiveSessionModel() {
        val sessionId = _activeSessionId.value ?: return
        val providerId = _activeProvider.value.id
        val modelId = _activeModel.value.id
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                chatDao.updateSessionModel(sessionId, providerId, modelId)
            }
            refreshSessionsList()
        }
    }

    fun isKeyConfigured(provider: ProviderType): Boolean =
        !provider.requiresApiKey || _apiKeys.value[provider].orEmpty().isNotBlank()

    fun keyForProvider(provider: ProviderType): String =
        _apiKeys.value[provider] ?: preferencesManager.getApiKey(provider)

    fun setApiKey(provider: ProviderType, key: String) {
        preferencesManager.setApiKey(provider, key.trim())
        _apiKeys.value = loadApiKeys()
    }

    fun refreshFromPreferences() {
        _apiKeys.value = loadApiKeys()
    }

    fun setTheme(themeId: String) {
        preferencesManager.selectedTheme = themeId
        _themeId.value = themeId
    }

    fun setThemeMode(mode: String) {
        preferencesManager.themeMode = mode
        _themeMode.value = mode
    }

    fun setDailyLimit(limit: Int) {
        usageTracker.setDailyLimit(limit)
    }

    fun resetUsageStats() {
        usageTracker.resetTodayStats()
    }

    fun setMemoryEnabled(enabled: Boolean) {
        preferencesManager.memoryEnabled = enabled
        _memoryEnabled.value = enabled
    }

    // --- App lock ---

    fun unlockApp(pin: String): Boolean {
        val success = appLockManager.verify(pin)
        if (success) {
            _isAppUnlocked.value = true
            appLockManager.clearBackgroundTimestamp()
        }
        return success
    }

    fun lockApp() {
        if (appLockManager.isEnabled()) {
            _isAppUnlocked.value = false
        }
    }

    fun onAppLockConfigured(enabled: Boolean) {
        _isAppLockEnabled.value = enabled
        _isAppUnlocked.value = true
        if (!enabled) appLockManager.clearBackgroundTimestamp()
    }

    fun onAppBackgrounded() {
        if (!appLockManager.isEnabled()) return
        appLockManager.markBackgrounded()
        if (appLockManager.timeoutMillis == AppLockManager.IMMEDIATE) {
            _isAppUnlocked.value = false
        }
    }

    fun onAppForegrounded() {
        _isAppLockEnabled.value = appLockManager.isEnabled()
        if (appLockManager.isEnabled() && appLockManager.shouldRelock()) {
            _isAppUnlocked.value = false
        }
    }

    fun addAttachment(attachment: ChatAttachment) {
        _pendingAttachments.value = _pendingAttachments.value + attachment
    }

    fun removeAttachment(id: String) {
        _pendingAttachments.value = _pendingAttachments.value.filter { it.id != id }
    }

    fun attachUri(uri: Uri, onResult: (Boolean, String) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            _isPreparingAttachment.value = true
            try {
                val info = withContext(Dispatchers.IO) { FileUtils.processPickedUri(app, uri) }
                if (info == null) {
                    onResult(false, "Could not read that file.")
                    return@launch
                }
                val content = withContext(Dispatchers.IO) { readAttachmentText(info) }
                addAttachment(
                    ChatAttachment(
                        id = info.id,
                        name = info.name,
                        mimeType = info.mimeType,
                        sizeBytes = info.sizeBytes,
                        content = content,
                        localUri = info.localUri
                    )
                )
                onResult(true, "Attached ${info.name}")
            } catch (e: Exception) {
                onResult(false, "Could not attach file: ${e.localizedMessage}")
            } finally {
                _isPreparingAttachment.value = false
            }
        }
    }

    fun attachCachedFile(file: File, onResult: (Boolean, String) -> Unit = { _, _ -> }) {
        if (!file.exists()) {
            onResult(false, "That file is no longer available.")
            return
        }
        viewModelScope.launch {
            _isPreparingAttachment.value = true
            try {
                val mime = FileUtils.resolveMimeType(file.name, null, null)
                val isImage = mime.startsWith("image/")
                val content = withContext(Dispatchers.IO) {
                    if (isImage) "[Image attachment]" else runCatching {
                        FileUtils.readFullTextContent(file, 60_000)
                    }.getOrDefault("")
                }
                addAttachment(
                    ChatAttachment(
                        name = file.name,
                        mimeType = mime,
                        sizeBytes = file.length(),
                        content = content,
                        localUri = file.absolutePath
                    )
                )
                onResult(true, "Attached ${file.name}")
            } catch (e: Exception) {
                onResult(false, "Could not attach file: ${e.localizedMessage}")
            } finally {
                _isPreparingAttachment.value = false
            }
        }
    }

    fun addSampleAttachment(sampleType: String, onResult: (Boolean, String) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            _isPreparingAttachment.value = true
            try {
                val info = withContext(Dispatchers.IO) {
                    FileUtils.createSampleAttachment(app, sampleType)
                }
                val content = withContext(Dispatchers.IO) { readAttachmentText(info) }
                addAttachment(
                    ChatAttachment(
                        id = info.id,
                        name = info.name,
                        mimeType = info.mimeType,
                        sizeBytes = info.sizeBytes,
                        content = content,
                        localUri = info.localUri
                    )
                )
                onResult(true, "Attached ${info.name}")
            } catch (e: Exception) {
                onResult(false, "Could not create sample: ${e.localizedMessage}")
            } finally {
                _isPreparingAttachment.value = false
            }
        }
    }

    private suspend fun readAttachmentText(info: AttachmentInfo): String {
        if (info.isImage) return "[Image attachment]"
        val file = File(info.localUri)
        val text = if (file.exists()) {
            runCatching { FileUtils.readFullTextContent(file, 60_000) }.getOrDefault("")
        } else ""
        return text.ifBlank { info.previewSnippet.orEmpty() }.ifBlank { "[Attached file: ${info.name}]" }
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
            val hasGeminiKey = preferencesManager.geminiApiKey.isNotBlank()
            _ragStats.value = RagEngineStats(
                totalDocuments = docCount,
                totalChunks = chunkCount,
                isGeminiEmbeddingAvailable = hasGeminiKey,
                isHybridSearchActive = true,
                activeEmbeddingModel = if (hasGeminiKey) {
                    "gemini-embedding-2-preview + local hybrid"
                } else {
                    "Local TF-IDF hybrid (offline)"
                }
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
                ragEngine.retrieveRelevantChunks(
                    query,
                    topK = 5,
                    geminiApiKey = preferencesManager.geminiApiKey
                )
            }
            _testSearchResults.value = results
            _isRagProcessing.value = false
        }
    }

    fun ingestFileToRag(uri: Uri, callback: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _isRagProcessing.value = true
            try {
                val attInfo = FileUtils.processPickedUri(app, uri)
                if (attInfo != null) {
                    val file = File(attInfo.localUri)
                    val text = if (file.exists()) FileUtils.readFullTextContent(file) else (attInfo.previewSnippet ?: "")
                    val fileName = attInfo.name
                    if (text.isNotBlank()) {
                        ragEngine.ingestDocument(
                            title = fileName,
                            content = text,
                            sourceType = "FILE",
                            geminiApiKey = preferencesManager.geminiApiKey
                        )
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
                ragEngine.ingestDocument(
                    title = title,
                    content = text,
                    sourceType = sourceType,
                    geminiApiKey = preferencesManager.geminiApiKey
                )
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
                        sourceType = "NOTES",
                        geminiApiKey = preferencesManager.geminiApiKey
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
                        sourceType = "STARTER",
                        geminiApiKey = preferencesManager.geminiApiKey
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
                        sourceType = source.category,
                        geminiApiKey = preferencesManager.geminiApiKey
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

    // --- AI Memory ---

    fun addMemory(content: String, category: String = "General") {
        val cleaned = content.trim()
        if (cleaned.isBlank()) return
        viewModelScope.launch {
            memoryDao.insert(MemoryEntity(content = cleaned, category = category))
        }
    }

    fun upsertMemory(memory: MemoryEntity) {
        val cleaned = memory.content.trim()
        if (cleaned.isBlank()) return
        viewModelScope.launch {
            memoryDao.insert(
                memory.copy(content = cleaned, updatedAt = System.currentTimeMillis())
            )
        }
    }

    fun deleteMemory(memory: MemoryEntity) {
        viewModelScope.launch { memoryDao.delete(memory) }
    }

    fun clearMemories() {
        viewModelScope.launch { memoryDao.clearAll() }
    }

    // --- Prompt Library ---

    fun upsertPrompt(prompt: PromptTemplateEntity) {
        if (prompt.title.isBlank() || prompt.content.isBlank()) return
        viewModelScope.launch {
            promptDao.insert(
                prompt.copy(
                    title = prompt.title.trim(),
                    content = prompt.content.trim(),
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun deletePrompt(prompt: PromptTemplateEntity) {
        viewModelScope.launch { promptDao.delete(prompt) }
    }

    fun togglePromptFavorite(prompt: PromptTemplateEntity) {
        viewModelScope.launch {
            promptDao.update(
                prompt.copy(
                    isFavorite = !prompt.isFavorite,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun usePrompt(prompt: PromptTemplateEntity, resolvedPrompt: String? = null) {
        viewModelScope.launch { promptDao.recordUsage(prompt.id) }
        queuePromptInChat(resolvedPrompt ?: prompt.content)
    }

    fun queuePromptInChat(prompt: String) {
        if (prompt.isBlank()) return
        _composerDraft.value = prompt
        _selectedSection.value = AppSection.CHAT
    }

    fun clearComposerDraft() {
        _composerDraft.value = null
    }

    // --- Habits & Focus ---

    fun addHabit(name: String, emoji: String = "✅", targetPerWeek: Int = 7) {
        val cleaned = name.trim()
        if (cleaned.isBlank()) return
        viewModelScope.launch {
            focusDao.upsertHabit(
                HabitEntity(
                    name = cleaned,
                    emoji = emoji,
                    targetPerWeek = targetPerWeek.coerceIn(1, 7)
                )
            )
        }
    }

    fun upsertHabit(habit: HabitEntity) {
        if (habit.name.isBlank()) return
        viewModelScope.launch {
            focusDao.upsertHabit(habit.copy(name = habit.name.trim()))
        }
    }

    fun deleteHabit(habit: HabitEntity) {
        viewModelScope.launch { focusDao.deleteHabitWithCheckIns(habit.id) }
    }

    fun toggleHabitCheckIn(habitId: String, epochDay: Long) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                if (focusDao.getCheckIn(habitId, epochDay) != null) {
                    focusDao.deleteCheckIn(habitId, epochDay)
                } else {
                    focusDao.insertCheckIn(
                        HabitCheckInEntity(habitId = habitId, epochDay = epochDay)
                    )
                }
            }
        }
    }

    fun recordFocusSession(
        label: String,
        minutes: Int,
        mode: String,
        wasCompleted: Boolean = true
    ) {
        viewModelScope.launch {
            focusDao.insertFocusSession(
                FocusSessionEntity(
                    label = label.ifBlank { "Focus" },
                    mode = mode,
                    minutes = minutes.coerceAtLeast(1),
                    wasCompleted = wasCompleted
                )
            )
        }
    }

    fun deleteFocusSession(sessionId: String) {
        viewModelScope.launch { focusDao.deleteFocusSession(sessionId) }
    }

    // Chat sessions
    fun newSession() {
        stopGeneration()
        viewModelScope.launch {
            val session = chatRepository.createNewSession(
                providerId = _activeProvider.value.id,
                modelId = _activeModel.value.id
            )
            _messages.value = emptyList()
            _activeSessionId.value = session.id
            refreshSessionsList()
        }
    }

    fun selectSession(sessionId: String) {
        if (sessionId == _activeSessionId.value) return
        stopGeneration()
        viewModelScope.launch {
            val session = withContext(Dispatchers.IO) { chatDao.getSessionById(sessionId) } ?: return@launch
            activateSession(session)
        }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            val wasActive = _activeSessionId.value == sessionId
            if (wasActive) stopGeneration()
            chatRepository.deleteSession(sessionId)
            var remaining = chatRepository.getAllSessionsOnce()
            if (remaining.isEmpty()) {
                chatRepository.createNewSession(
                    providerId = _activeProvider.value.id,
                    modelId = _activeModel.value.id
                )
                remaining = chatRepository.getAllSessionsOnce()
            }
            _sessions.value = remaining
            if (wasActive) {
                remaining.firstOrNull()?.let { activateSession(it) }
            }
        }
    }

    fun renameSession(sessionId: String, title: String) {
        val trimmed = title.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            withContext(Dispatchers.IO) { chatRepository.updateSessionTitle(sessionId, trimmed) }
            refreshSessionsList()
        }
    }

    fun clearChat() {
        stopGeneration()
        val sessionId = _activeSessionId.value ?: return
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                chatDao.deleteMessagesForSession(sessionId)
                chatDao.updateSessionTitle(sessionId, "New Chat")
            }
            _messages.value = emptyList()
            refreshSessionsList()
        }
    }

    fun clearAllChats() {
        stopGeneration()
        viewModelScope.launch {
            chatRepository.clearAll()
            val session = chatRepository.createNewSession(
                providerId = _activeProvider.value.id,
                modelId = _activeModel.value.id
            )
            _messages.value = emptyList()
            _activeSessionId.value = session.id
            refreshSessionsList()
        }
    }

    // --- Search, export & message tools ---

    suspend fun searchChats(query: String): List<ChatSearchGroup> = withContext(Dispatchers.IO) {
        val cleaned = query.trim()
        if (cleaned.isBlank()) return@withContext emptyList()
        val allSessions = chatDao.getAllSessionsOnce().associateBy { it.id }
        val groups = LinkedHashMap<String, ChatSearchGroup>()

        chatDao.searchSessions(cleaned).forEach { session ->
            groups[session.id] = ChatSearchGroup(
                sessionId = session.id,
                sessionTitle = session.title,
                sessionUpdatedAt = session.updatedAt,
                titleMatched = true,
                hits = emptyList()
            )
        }
        chatDao.searchMessages(cleaned).forEach { message ->
            val session = allSessions[message.sessionId] ?: return@forEach
            val existing = groups[message.sessionId]
            groups[message.sessionId] = if (existing == null) {
                ChatSearchGroup(
                    sessionId = session.id,
                    sessionTitle = session.title,
                    sessionUpdatedAt = session.updatedAt,
                    titleMatched = false,
                    hits = listOf(message)
                )
            } else {
                existing.copy(hits = existing.hits + message)
            }
        }
        groups.values.sortedByDescending { it.latestHitAt }
    }

    fun openSearchHit(sessionId: String, messageId: String? = null) {
        if (sessionId != _activeSessionId.value) {
            selectSession(sessionId)
        }
        _focusMessageId.value = messageId
        _selectedSection.value = AppSection.CHAT
    }

    fun clearMessageFocus() {
        _focusMessageId.value = null
    }

    fun exportConversation(
        sessionId: String,
        format: ConversationExporter.Format,
        callback: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val session = withContext(Dispatchers.IO) { chatDao.getSessionById(sessionId) }
                if (session == null) {
                    callback(false, "That chat no longer exists.")
                    return@launch
                }
                val messages = withContext(Dispatchers.IO) {
                    chatDao.getMessagesListForSession(sessionId)
                }
                if (messages.isEmpty()) {
                    callback(false, "There are no messages to export yet.")
                    return@launch
                }
                val content = ConversationExporter.exportSession(session, messages, format)
                val fileName = ConversationExporter.fileName(session.title, format)
                MediaSaver.saveDocumentToDownloads(app, fileName, content, format.mimeType)
                    .onSuccess { callback(true, "Saved $fileName to Downloads/OmniChat") }
                    .onFailure {
                        callback(false, "Export failed: ${it.localizedMessage ?: "unknown error"}")
                    }
            } catch (e: Exception) {
                callback(false, "Export failed: ${e.localizedMessage ?: "unknown error"}")
            }
        }
    }

    fun exportAllConversations(
        format: ConversationExporter.Format,
        callback: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val allSessions = withContext(Dispatchers.IO) {
                    chatRepository.getAllSessionsOnce()
                }
                if (allSessions.isEmpty()) {
                    callback(false, "There are no conversations to export.")
                    return@launch
                }
                val messagesBySession = withContext(Dispatchers.IO) {
                    allSessions.associate { session ->
                        session.id to chatDao.getMessagesListForSession(session.id)
                    }
                }
                val content = ConversationExporter.exportAll(allSessions, messagesBySession, format)
                val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                val fileName = "omnichat_all_chats_$stamp.${format.extension}"
                MediaSaver.saveDocumentToDownloads(app, fileName, content, format.mimeType)
                    .onSuccess { callback(true, "Saved $fileName to Downloads/OmniChat") }
                    .onFailure {
                        callback(false, "Export failed: ${it.localizedMessage ?: "unknown error"}")
                    }
            } catch (e: Exception) {
                callback(false, "Export failed: ${e.localizedMessage ?: "unknown error"}")
            }
        }
    }

    fun currentConversationShareText(): String? {
        val session = _sessions.value.firstOrNull { it.id == _activeSessionId.value } ?: return null
        val visible = _messages.value.filter { it.status != MessageStatus.STREAMING }
        if (visible.isEmpty()) return null
        return ConversationExporter.shareText(
            title = session.title,
            modelId = _activeModel.value.id,
            messages = visible
        )
    }

    fun editUserMessageAndRegenerate(messageId: String, newContent: String) {
        val cleaned = newContent.trim()
        if (cleaned.isBlank() || _isGenerating.value) return
        val sessionId = _activeSessionId.value ?: return
        val snapshot = _messages.value
        val index = snapshot.indexOfFirst { it.id == messageId }
        if (index < 0) return
        val target = snapshot[index]
        if (target.role != MessageRole.USER) return
        val history = snapshot.take(index).filter { it.status == MessageStatus.COMPLETE }
        _isGenerating.value = true
        generationJob = viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    chatDao.updateMessageContent(messageId, cleaned)
                    chatDao.deleteMessagesAfter(sessionId, messageId)
                    chatDao.updateSessionTimestamp(sessionId)
                }
                val edited = target.copy(content = cleaned)
                _messages.value = snapshot.take(index) + edited
                streamAssistantReply(sessionId, edited, history)
            } finally {
                _isGenerating.value = false
                generationJob = null
                refreshSessionsList()
            }
        }
    }

    fun deleteMessage(messageId: String) {
        if (_isGenerating.value) return
        val sessionId = _activeSessionId.value ?: return
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                chatDao.deleteMessage(messageId)
                chatDao.updateSessionTimestamp(sessionId)
            }
            _messages.value = _messages.value.filterNot { it.id == messageId }
        }
    }

    fun sendMessage(userText: String) {
        if (_isGenerating.value || !_isSessionReady.value) return
        if (userText.isBlank() && _pendingAttachments.value.isEmpty()) return
        val sessionId = _activeSessionId.value ?: return

        val currentAttachments = _pendingAttachments.value
        _pendingAttachments.value = emptyList()
        val priorHistory = _messages.value.filter {
            it.status == MessageStatus.COMPLETE && it.role != MessageRole.SYSTEM
        }

        val userMessage = ChatMessage(
            role = MessageRole.USER,
            content = userText.ifBlank { "[Sent ${currentAttachments.size} attachment(s)]" },
            attachments = currentAttachments
        )

        _messages.value = _messages.value + userMessage
        _isGenerating.value = true
        generationJob = viewModelScope.launch {
            try {
                val preparedAttachments = withContext(Dispatchers.IO) {
                    currentAttachments.map { prepareAttachment(it) }
                }
                val preparedUserMessage = userMessage.copy(attachments = preparedAttachments)
                _messages.value = _messages.value.map {
                    if (it.id == userMessage.id) preparedUserMessage else it
                }
                insertMessage(preparedUserMessage, sessionId)
                maybeAutoTitle(sessionId, preparedUserMessage)

                if (usageTracker.isLimitReached()) {
                    val limit = preferencesManager.dailyLimit
                    appendErrorMessage(
                        sessionId,
                        "Daily request limit reached ($limit requests). Open Settings → Daily Request Budget to raise the limit or reset today's usage."
                    )
                    return@launch
                }

                val provider = _activeProvider.value
                if (provider.requiresApiKey && keyForProvider(provider).isBlank()) {
                    appendErrorMessage(
                        sessionId,
                        "${provider.displayName} requires an API key. Tap Configure Key or open Settings to add one."
                    )
                    return@launch
                }

                streamAssistantReply(sessionId, preparedUserMessage, priorHistory)
            } finally {
                _isGenerating.value = false
                generationJob = null
                refreshSessionsList()
            }
        }
    }

    fun stopGeneration() {
        generationJob?.cancel()
    }

    fun regenerateLastResponse() {
        if (_isGenerating.value) return
        val sessionId = _activeSessionId.value ?: return
        val snapshot = _messages.value
        val lastMessage = snapshot.lastOrNull() ?: return
        if (lastMessage.role != MessageRole.ASSISTANT || lastMessage.status == MessageStatus.STREAMING) return

        val userIndex = (snapshot.lastIndex - 1 downTo 0).firstOrNull { snapshot[it].role == MessageRole.USER } ?: return
        val userMessage = snapshot[userIndex]
        val history = snapshot.take(userIndex).filter { it.status == MessageStatus.COMPLETE }
        _messages.value = snapshot.dropLast(1)
        _isGenerating.value = true
        generationJob = viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { chatDao.deleteMessage(lastMessage.id) }
                streamAssistantReply(sessionId, userMessage, history)
            } finally {
                _isGenerating.value = false
                generationJob = null
                refreshSessionsList()
            }
        }
    }

    private suspend fun streamAssistantReply(
        sessionId: String,
        userMessage: ChatMessage,
        priorHistory: List<ChatMessage>
    ) {
        val provider = _activeProvider.value
        val model = _activeModel.value
        val assistantId = UUID.randomUUID().toString()
        val pendingMessage = ChatMessage(
            id = assistantId,
            role = MessageRole.ASSISTANT,
            content = "",
            status = MessageStatus.STREAMING,
            modelName = model.id
        )
        _messages.value = _messages.value + pendingMessage

        val contextKnowledge = if (_isRagEnabled.value) {
            runCatching {
                ragEngine.retrieveRelevantChunks(
                    query = userMessage.content,
                    topK = 3,
                    geminiApiKey = preferencesManager.geminiApiKey
                ).joinToString("\n---\n") { it.chunk.content }
            }.getOrDefault("")
        } else ""

        val attachmentSnippets = userMessage.attachments
            .filter { it.content.isNotBlank() && !it.mimeType.startsWith("image/") }
            .joinToString("\n\n") { "Attachment [${it.name}]:\n${it.content}" }
        val prompt = if (attachmentSnippets.isBlank()) {
            userMessage.content
        } else {
            "${userMessage.content}\n\n$attachmentSnippets"
        }

        val memoryList = if (preferencesManager.memoryEnabled) {
            withContext(Dispatchers.IO) { memoryDao.getAllOnce() }
        } else {
            emptyList()
        }
        val composedSystemPrompt = AssistantContextBuilder.composeSystemPrompt(
            basePrompt = preferencesManager.systemPrompt,
            memories = memoryList,
            memoryEnabled = preferencesManager.memoryEnabled
        )
        val requestHistory = AssistantContextBuilder.trimHistory(
            history = priorHistory,
            maxTokens = preferencesManager.contextBudgetTokens
        )

        val builder = StringBuilder()
        var lastUiUpdate = 0L
        try {
            AiService.streamResponse(
                provider = provider,
                modelId = model.id,
                apiKey = keyForProvider(provider),
                history = requestHistory,
                userPrompt = prompt,
                contextDoc = contextKnowledge,
                systemPrompt = composedSystemPrompt,
                temperature = preferencesManager.temperature,
                maxTokens = preferencesManager.maxOutputTokens,
                customBaseUrl = preferencesManager.customBaseUrl
            ).collect { token ->
                builder.append(token)
                val now = SystemClock.elapsedRealtime()
                if (now - lastUiUpdate >= 60 || builder.length < 160) {
                    updateStreamingMessage(assistantId, builder.toString())
                    lastUiUpdate = now
                }
            }
            val finalText = builder.toString().trim().ifBlank {
                "_(The provider returned an empty response.)_"
            }
            completeMessage(
                messageId = assistantId,
                text = finalText,
                status = MessageStatus.COMPLETE,
                modelId = model.id,
                sessionId = sessionId
            )
            usageTracker.recordRequest(provider.id, estimateTokens(prompt, finalText))
        } catch (e: CancellationException) {
            withContext(NonCancellable) {
                val partial = builder.toString().trim().ifBlank { "Generation stopped." }
                completeMessage(
                    messageId = assistantId,
                    text = partial,
                    status = MessageStatus.COMPLETE,
                    modelId = model.id,
                    sessionId = sessionId
                )
            }
            throw e
        } catch (e: Exception) {
            withContext(NonCancellable) {
                completeMessage(
                    messageId = assistantId,
                    text = userFacingError(e, provider),
                    status = MessageStatus.ERROR,
                    modelId = model.id,
                    sessionId = sessionId
                )
            }
        }
    }

    private fun updateStreamingMessage(messageId: String, text: String) {
        _messages.value = _messages.value.map { message ->
            if (message.id == messageId) message.copy(content = text) else message
        }
    }

    private suspend fun completeMessage(
        messageId: String,
        text: String,
        status: MessageStatus,
        modelId: String,
        sessionId: String
    ) {
        _messages.value = _messages.value.map { message ->
            if (message.id == messageId) {
                message.copy(content = text, status = status, modelName = modelId)
            } else message
        }
        val completed = _messages.value.firstOrNull { it.id == messageId }
            ?: ChatMessage(
                id = messageId,
                role = MessageRole.ASSISTANT,
                content = text,
                status = status,
                modelName = modelId
            )
        insertMessage(completed, sessionId)
    }

    private suspend fun appendErrorMessage(sessionId: String, text: String) {
        val message = ChatMessage(
            role = MessageRole.ASSISTANT,
            content = text,
            status = MessageStatus.ERROR,
            modelName = _activeModel.value.id
        )
        _messages.value = _messages.value + message
        insertMessage(message, sessionId)
    }

    private suspend fun maybeAutoTitle(sessionId: String, userMessage: ChatMessage) {
        val session = withContext(Dispatchers.IO) { chatDao.getSessionById(sessionId) } ?: return
        if (session.title.isNotBlank() && session.title != "New Chat") return
        val cleaned = userMessage.content.trim().replace(Regex("\\s+"), " ")
        val base = cleaned.ifBlank {
            userMessage.attachments.firstOrNull()?.name ?: "New Chat"
        }
        val title = if (base.length > 48) base.take(48).trimEnd() + "…" else base
        withContext(Dispatchers.IO) { chatRepository.updateSessionTitle(sessionId, title) }
        refreshSessionsList()
    }

    private suspend fun insertMessage(message: ChatMessage, sessionId: String) {
        withContext(Dispatchers.IO) {
            chatDao.insertMessage(message.toEntity(sessionId))
            chatDao.updateSessionTimestamp(sessionId)
        }
    }

    private fun prepareAttachment(attachment: ChatAttachment): ChatAttachment {
        if (attachment.localUri.isNotBlank() || attachment.bitmap == null) return attachment
        return try {
            val directory = File(app.filesDir, "chat_attachments").apply { mkdirs() }
            val file = File(directory, "img_${attachment.id.take(8)}_${System.currentTimeMillis()}.png")
            file.outputStream().use { output ->
                attachment.bitmap.compress(Bitmap.CompressFormat.PNG, 92, output)
            }
            attachment.copy(localUri = file.absolutePath)
        } catch (_: Exception) {
            attachment
        }
    }

    private fun estimateTokens(prompt: String, response: String): Int =
        ((prompt.length + response.length) / 4).coerceAtLeast(1)

    private fun userFacingError(error: Exception, provider: ProviderType): String {
        return when (error) {
            is MissingApiKeyException ->
                "${provider.displayName} requires an API key. Tap Configure Key or open Settings."
            is ApiRequestException -> buildString {
                append("${provider.displayName} rejected the request (HTTP ${error.statusCode}).\n\n")
                when (error.statusCode) {
                    401, 403 -> append("The API key may be invalid or lack access to this model. Update it in Settings.\n\n")
                    429 -> append("Rate limit reached. Try another provider or wait a moment.\n\n")
                    404 -> append("The selected model may not be available on this provider. Choose another model.\n\n")
                }
                append(error.apiMessage.take(600))
            }
            else -> "Could not get a reply from ${provider.displayName}.\n\n" +
                    "${error.message ?: "Network error"}\n\nCheck your connection and tap Retry."
        }
    }

    // --- Backup & restore ---

    fun createBackup(callback: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val (json, report) = backupManager.exportJson()
                val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                val fileName = "omnichat_backup_$stamp.json"
                MediaSaver.saveDocumentToDownloads(app, fileName, json, "application/json")
                    .onSuccess {
                        callback(
                            true,
                            "Backup saved: $fileName\n${report.summary()}"
                        )
                    }
                    .onFailure {
                        callback(false, "Backup failed: ${it.localizedMessage ?: "unknown error"}")
                    }
            } catch (e: Exception) {
                callback(false, "Backup failed: ${e.localizedMessage ?: "unknown error"}")
            }
        }
    }

    fun restoreBackup(json: String, callback: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val report = backupManager.importJson(json)
                reloadAfterRestore()
                callback(true, report.summary())
            } catch (e: Exception) {
                callback(false, e.message ?: "Restore failed.")
            }
        }
    }

    private suspend fun reloadAfterRestore() {
        val allSessions = withContext(Dispatchers.IO) { chatRepository.getAllSessionsOnce() }
        _sessions.value = allSessions
        val activeId = _activeSessionId.value
        if (allSessions.none { it.id == activeId }) {
            val first = allSessions.firstOrNull()
            if (first != null) {
                activateSession(first)
            } else {
                _messages.value = emptyList()
            }
        } else if (activeId != null) {
            _messages.value = withContext(Dispatchers.IO) {
                chatDao.getMessagesListForSession(activeId)
            }.map { it.toChatMessage() }
        }
        refreshRagStats()
    }

    private fun ChatMessage.toEntity(sessionId: String): ChatMessageEntity = ChatMessageEntity(
        id = id,
        sessionId = sessionId,
        role = when (role) {
            MessageRole.USER -> "user"
            MessageRole.SYSTEM -> "system"
            else -> "assistant"
        },
        content = content,
        timestamp = timestamp,
        providerId = _activeProvider.value.id,
        modelId = modelName.ifBlank { _activeModel.value.id },
        attachmentsJson = chatRepository.serializeAttachments(
            attachments.map { it.toAttachmentInfo() }
        ),
        isError = status == MessageStatus.ERROR
    )

    private fun ChatMessageEntity.toChatMessage(): ChatMessage = ChatMessage(
        id = id,
        role = when (role) {
            "user" -> MessageRole.USER
            "system" -> MessageRole.SYSTEM
            else -> MessageRole.ASSISTANT
        },
        content = content,
        timestamp = timestamp,
        modelName = modelId,
        attachments = chatRepository.parseAttachments(attachmentsJson).map { info ->
            ChatAttachment(
                id = info.id,
                name = info.name,
                mimeType = info.mimeType,
                sizeBytes = info.sizeBytes,
                content = info.content,
                localUri = info.localUri
            )
        },
        status = if (isError) MessageStatus.ERROR else MessageStatus.COMPLETE
    )

    private fun ChatAttachment.toAttachmentInfo(): AttachmentInfo = AttachmentInfo(
        id = id,
        name = name,
        mimeType = mimeType,
        sizeBytes = sizeBytes,
        localUri = localUri,
        isImage = mimeType.startsWith("image/"),
        previewSnippet = if (!mimeType.startsWith("image/")) content.take(300) else null,
        content = content
    )
}
