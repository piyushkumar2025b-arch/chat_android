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
import com.example.data.model.AiModel
import com.example.data.model.AttachmentInfo
import com.example.data.model.AvailableModels
import com.example.data.model.ChatMessageEntity
import com.example.data.model.ChatSessionEntity
import com.example.data.model.ProviderType
import com.example.data.remote.AiService
import com.example.data.remote.FileUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    val preferencesManager = PreferencesManager(application)
    private val database = AppDatabase.getInstance(application)
    val repository = ChatRepository(database.chatDao())

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

    private val _pendingAttachments = MutableStateFlow<List<AttachmentInfo>>(emptyList())
    val pendingAttachments: StateFlow<List<AttachmentInfo>> = _pendingAttachments.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

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
            ProviderType.CUSTOM -> preferencesManager.customApiKey = key
            ProviderType.POLLINATIONS -> {}
        }
        _keysRevision.value += 1
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
        initTts(application)
        viewModelScope.launch {
            // Observe sessions and pick the most recent one or create one
            sessions.collect { list ->
                if (_currentSessionId.value == null) {
                    if (list.isNotEmpty()) {
                        val first = list.first()
                        _currentSessionId.value = first.id
                        _currentSession.value = first
                        syncSessionModel(first)
                    } else {
                        createNewSession()
                    }
                } else {
                    val matching = list.firstOrNull { it.id == _currentSessionId.value }
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
            ProviderType.OPENROUTER -> preferencesManager.openRouterApiKey.isNotBlank()
            ProviderType.CUSTOM -> true
        }
    }

    fun getApiKeyForProvider(provider: ProviderType): String {
        return when (provider) {
            ProviderType.POLLINATIONS -> ""
            ProviderType.GEMINI -> preferencesManager.geminiApiKey
            ProviderType.GROQ -> preferencesManager.groqApiKey
            ProviderType.OPENROUTER -> preferencesManager.openRouterApiKey
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

    fun removePendingAttachment(attachmentId: String) {
        _pendingAttachments.value = _pendingAttachments.value.filterNot { it.id == attachmentId }
    }

    fun sendMessage(userText: String) {
        val trimmed = userText.trim()
        val attachments = _pendingAttachments.value
        if (trimmed.isEmpty() && attachments.isEmpty()) return

        val sessionId = _currentSessionId.value ?: return
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
                _pendingAttachments.value = emptyList()
            }
            return
        }

        activeJob?.cancel()
        activeJob = viewModelScope.launch {
            _isGenerating.value = true
            val displayPrompt = trimmed.ifEmpty { "Analyze the attached file(s)" }

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
            _pendingAttachments.value = emptyList()

            // Call AI Service
            val apiKey = getApiKeyForProvider(provider)
            val history = currentMessages.value

            val result = AiService.sendMessage(
                provider = provider,
                modelId = model.id,
                prompt = displayPrompt,
                attachments = attachments,
                history = history,
                apiKey = apiKey,
                systemPrompt = preferencesManager.systemPrompt,
                temperature = preferencesManager.temperature,
                customBaseUrl = preferencesManager.customBaseUrl
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
            }.onFailure { error ->
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

        activeJob?.cancel()
        activeJob = viewModelScope.launch {
            _isGenerating.value = true
            repository.updateMessageContent(assistantMessage.id, "Regenerating response...")

            val apiKey = getApiKeyForProvider(provider)
            val history = allMsgs.take(msgIndex - 1)

            val result = AiService.sendMessage(
                provider = provider,
                modelId = model.id,
                prompt = userMessage.content,
                attachments = attachments,
                history = history,
                apiKey = apiKey,
                systemPrompt = preferencesManager.systemPrompt,
                temperature = preferencesManager.temperature,
                customBaseUrl = preferencesManager.customBaseUrl
            )

            result.onSuccess { responseText ->
                repository.updateMessageContent(assistantMessage.id, responseText, isError = false)
            }.onFailure { error ->
                repository.updateMessageContent(
                    assistantMessage.id,
                    "Error: ${error.localizedMessage ?: "Failed to regenerate"}",
                    isError = true
                )
            }

            _isGenerating.value = false
        }
    }

    fun stopGeneration() {
        activeJob?.cancel()
        _isGenerating.value = false
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
                // Strip markdown code blocks and symbols for clean spoken voice
                val cleanText = text.replace(Regex("```[\\s\\S]*?```"), "Code snippet omitted.")
                    .replace(Regex("[*#_`>]"), "")
                    .take(2000) // safety limit for spoken text
                _speakingMessageId.value = messageId
                tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, messageId)
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
