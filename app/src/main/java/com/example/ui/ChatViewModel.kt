package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.CalendarEventEntity
import com.example.data.local.NoteEntity
import com.example.data.local.VaultItemEntity
import com.example.data.remote.AiService
import com.example.model.AiProvider
import com.example.model.AppSection
import com.example.model.ChatAttachment
import com.example.model.ChatMessage
import com.example.model.MessageRole
import com.example.model.MessageStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    val productivityDao = db.productivityDao()

    val notes: StateFlow<List<NoteEntity>> = productivityDao.getAllNotes()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val events: StateFlow<List<CalendarEventEntity>> = productivityDao.getAllEvents()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val vaultItems: StateFlow<List<VaultItemEntity>> = productivityDao.getAllVaultItems()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

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

    private val _ragKnowledge = MutableStateFlow<List<String>>(
        listOf(
            "OmniChat Knowledge: Built using modern Jetpack Compose with Material Design 3.",
            "Security Policy: Passwords & secrets stored in Room SQLite with local device encryption."
        )
    )
    val ragKnowledge: StateFlow<List<String>> = _ragKnowledge.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    // Studio Handoff States (Fixing BUG-05)
    private val _studioCodeHandoff = MutableStateFlow<Triple<String, String, String>?>(null)
    val studioCodeHandoff: StateFlow<Triple<String, String, String>?> = _studioCodeHandoff.asStateFlow()

    private val _studioMarkdownHandoff = MutableStateFlow<Pair<String, String>?>(null)
    val studioMarkdownHandoff: StateFlow<Pair<String, String>?> = _studioMarkdownHandoff.asStateFlow()

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

    fun addKnowledgeDoc(text: String) {
        if (text.isNotBlank()) {
            _ragKnowledge.value = _ragKnowledge.value + text.trim()
        }
    }

    // Fix for BUG-05: Properly pass code/markdown into studios
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

        val assistantMessageId = java.util.UUID.randomUUID().toString()
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
            val contextKnowledge = _ragKnowledge.value.joinToString("\n---\n")

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
