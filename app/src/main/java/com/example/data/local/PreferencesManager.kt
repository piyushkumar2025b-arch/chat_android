package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig
import com.example.data.model.ProviderType

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("omnichat_prefs", Context.MODE_PRIVATE)

    private val vaultSecurity = VaultSecurityManager.getInstance(context)

    private fun getSecureString(key: String): String {
        val raw = prefs.getString(key, "") ?: ""
        if (raw.isBlank()) return ""
        return vaultSecurity.decrypt(raw)
    }

    private fun putSecureString(key: String, value: String) {
        val trimmed = value.trim()
        val toSave = if (trimmed.isNotBlank()) vaultSecurity.encrypt(trimmed) else ""
        prefs.edit().putString(key, toSave).apply()
    }

    companion object {
        private const val KEY_GEMINI_API_KEY = "gemini_api_key"
        private const val KEY_GROQ_API_KEY = "groq_api_key"
        private const val KEY_OPENROUTER_API_KEY = "openrouter_api_key"
        private const val KEY_CUSTOM_BASE_URL = "custom_base_url"
        private const val KEY_CUSTOM_API_KEY = "custom_api_key"
        private const val KEY_CUSTOM_MODEL = "custom_model"
        private const val KEY_SYSTEM_PROMPT = "system_prompt"
        private const val KEY_TEMPERATURE = "temperature"
        private const val KEY_LAST_PROVIDER = "last_provider"
        private const val KEY_LAST_MODEL = "last_model"
        private const val KEY_SELECTED_THEME = "selected_theme"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_DAILY_LIMIT = "daily_limit"
        private const val KEY_HUGGINGFACE_API_KEY = "huggingface_api_key"
        private const val KEY_CEREBRAS_API_KEY = "cerebras_api_key"
        private const val KEY_YOUTUBE_API_KEY = "youtube_api_key"
        private const val KEY_GOOGLE_MAPS_API_KEY = "google_maps_api_key"
        private const val KEY_ACTIVE_PERSONA_ID = "active_persona_id"
        private const val KEY_CUSTOM_PERSONA_INSTRUCTIONS = "custom_persona_instructions"
        private const val KEY_MAX_OUTPUT_TOKENS = "max_output_tokens"
        private const val KEY_MEMORY_ENABLED = "assistant_memory_enabled"
        private const val KEY_CONTEXT_BUDGET = "context_budget_tokens"

        const val DEFAULT_SYSTEM_PROMPT =
            "You are a friendly, highly intelligent AI assistant. Provide helpful, accurate, well-formatted, and concise answers with markdown and code snippets when needed."
    }

    var dailyLimit: Int
        get() = prefs.getInt(KEY_DAILY_LIMIT, 50)
        set(value) = prefs.edit().putInt(KEY_DAILY_LIMIT, value).apply()

    var huggingFaceApiKey: String
        get() = getSecureString(KEY_HUGGINGFACE_API_KEY)
        set(value) = putSecureString(KEY_HUGGINGFACE_API_KEY, value)

    var selectedTheme: String
        get() {
            val saved = prefs.getString(KEY_SELECTED_THEME, "default") ?: "default"
            return if (saved == "indigo") "default" else saved
        }
        set(value) = prefs.edit().putString(KEY_SELECTED_THEME, value).apply()

    var themeMode: String
        get() = prefs.getString(KEY_THEME_MODE, "system") ?: "system"
        set(value) = prefs.edit().putString(KEY_THEME_MODE, value).apply()

    var cerebrasApiKey: String
        get() = getSecureString(KEY_CEREBRAS_API_KEY)
        set(value) = putSecureString(KEY_CEREBRAS_API_KEY, value)

    var youtubeApiKey: String
        get() = getSecureString(KEY_YOUTUBE_API_KEY)
        set(value) = putSecureString(KEY_YOUTUBE_API_KEY, value)

    var googleMapsApiKey: String
        get() {
            val saved = getSecureString(KEY_GOOGLE_MAPS_API_KEY)
            if (saved.isNotBlank()) return saved
            return try {
                val field = BuildConfig::class.java.getField("MAPS_API_KEY")
                val buildConfigKey = field.get(null) as? String ?: ""
                if (buildConfigKey != "MY_MAPS_API_KEY") buildConfigKey else ""
            } catch (e: Exception) {
                ""
            }
        }
        set(value) = putSecureString(KEY_GOOGLE_MAPS_API_KEY, value)

    var activePersonaId: String
        get() = prefs.getString(KEY_ACTIVE_PERSONA_ID, "default") ?: "default"
        set(value) = prefs.edit().putString(KEY_ACTIVE_PERSONA_ID, value).apply()

    var customPersonaInstructions: String
        get() = prefs.getString(KEY_CUSTOM_PERSONA_INSTRUCTIONS, "") ?: ""
        set(value) = prefs.edit().putString(KEY_CUSTOM_PERSONA_INSTRUCTIONS, value).apply()

    var maxOutputTokens: Int
        get() = prefs.getInt(KEY_MAX_OUTPUT_TOKENS, 8192)
        set(value) = prefs.edit().putInt(KEY_MAX_OUTPUT_TOKENS, value).apply()

    var memoryEnabled: Boolean
        get() = prefs.getBoolean(KEY_MEMORY_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_MEMORY_ENABLED, value).apply()

    var contextBudgetTokens: Int
        get() = prefs.getInt(KEY_CONTEXT_BUDGET, 6000)
        set(value) = prefs.edit().putInt(KEY_CONTEXT_BUDGET, value.coerceIn(1_000, 32_000)).apply()

    fun exportKeysJson(): String {
        val obj = org.json.JSONObject().apply {
            put("_security_notice", "OmniChat Confidential Credentials. Treat as sensitive secrets.")
            put("_exported_at", System.currentTimeMillis())
            put("gemini", geminiApiKey)
            put("groq", groqApiKey)
            put("openrouter", openRouterApiKey)
            put("huggingface", huggingFaceApiKey)
            put("cerebras", cerebrasApiKey)
            put("youtube", youtubeApiKey)
            put("googleMaps", googleMapsApiKey)
            put("customUrl", customBaseUrl)
            put("customKey", customApiKey)
            put("customModel", customModel)
        }
        return obj.toString(2)
    }

    fun importKeysJson(jsonStr: String): Boolean {
        return try {
            val obj = org.json.JSONObject(jsonStr)
            if (obj.has("gemini")) geminiApiKey = obj.optString("gemini", "")
            if (obj.has("groq")) groqApiKey = obj.optString("groq", "")
            if (obj.has("openrouter")) openRouterApiKey = obj.optString("openrouter", "")
            if (obj.has("huggingface")) huggingFaceApiKey = obj.optString("huggingface", "")
            if (obj.has("cerebras")) cerebrasApiKey = obj.optString("cerebras", "")
            if (obj.has("youtube")) youtubeApiKey = obj.optString("youtube", "")
            if (obj.has("googleMaps")) googleMapsApiKey = obj.optString("googleMaps", "")
            if (obj.has("customUrl")) customBaseUrl = obj.optString("customUrl", "")
            if (obj.has("customKey")) customApiKey = obj.optString("customKey", "")
            if (obj.has("customModel")) customModel = obj.optString("customModel", "")
            true
        } catch (e: Exception) {
            false
        }
    }

    var geminiApiKey: String
        get() {
            val saved = getSecureString(KEY_GEMINI_API_KEY)
            if (saved.isNotBlank()) return saved
            return try {
                val field = BuildConfig::class.java.getField("GEMINI_API_KEY")
                val buildConfigKey = field.get(null) as? String ?: ""
                if (buildConfigKey != "MY_GEMINI_API_KEY") buildConfigKey else ""
            } catch (e: Exception) {
                ""
            }
        }
        set(value) = putSecureString(KEY_GEMINI_API_KEY, value)

    var groqApiKey: String
        get() {
            val saved = getSecureString(KEY_GROQ_API_KEY)
            if (saved.isNotBlank()) return saved
            return try {
                val field = BuildConfig::class.java.getField("GROQ_API_KEY")
                val buildConfigKey = field.get(null) as? String ?: ""
                if (buildConfigKey != "MY_GROQ_API_KEY") buildConfigKey else ""
            } catch (e: Exception) {
                ""
            }
        }
        set(value) = putSecureString(KEY_GROQ_API_KEY, value)

    var openRouterApiKey: String
        get() {
            val saved = getSecureString(KEY_OPENROUTER_API_KEY)
            if (saved.isNotBlank()) return saved
            return try {
                val field = BuildConfig::class.java.getField("OPENROUTER_API_KEY")
                val buildConfigKey = field.get(null) as? String ?: ""
                if (buildConfigKey != "MY_OPENROUTER_API_KEY") buildConfigKey else ""
            } catch (e: Exception) {
                ""
            }
        }
        set(value) = putSecureString(KEY_OPENROUTER_API_KEY, value)

    var customBaseUrl: String
        get() = prefs.getString(KEY_CUSTOM_BASE_URL, "https://api.openai.com/v1/chat/completions") ?: ""
        set(value) = prefs.edit().putString(KEY_CUSTOM_BASE_URL, value.trim()).apply()

    var customApiKey: String
        get() = getSecureString(KEY_CUSTOM_API_KEY)
        set(value) = putSecureString(KEY_CUSTOM_API_KEY, value)

    var customModel: String
        get() = prefs.getString(KEY_CUSTOM_MODEL, "gpt-4o-mini") ?: "gpt-4o-mini"
        set(value) = prefs.edit().putString(KEY_CUSTOM_MODEL, value.trim()).apply()

    var systemPrompt: String
        get() = prefs.getString(KEY_SYSTEM_PROMPT, DEFAULT_SYSTEM_PROMPT) ?: DEFAULT_SYSTEM_PROMPT
        set(value) = prefs.edit().putString(KEY_SYSTEM_PROMPT, value).apply()

    var temperature: Float
        get() = prefs.getFloat(KEY_TEMPERATURE, 0.7f)
        set(value) = prefs.edit().putFloat(KEY_TEMPERATURE, value).apply()

    var lastProviderId: String
        get() = prefs.getString(KEY_LAST_PROVIDER, "pollinations") ?: "pollinations"
        set(value) = prefs.edit().putString(KEY_LAST_PROVIDER, value).apply()

    var lastModelId: String
        get() = prefs.getString(KEY_LAST_MODEL, "openai-fast") ?: "openai-fast"
        set(value) = prefs.edit().putString(KEY_LAST_MODEL, value).apply()

    fun getApiKey(provider: ProviderType): String = when (provider) {
        ProviderType.GEMINI -> geminiApiKey
        ProviderType.GROQ -> groqApiKey
        ProviderType.OPENROUTER -> openRouterApiKey
        ProviderType.CEREBRAS -> cerebrasApiKey
        ProviderType.HUGGINGFACE -> huggingFaceApiKey
        ProviderType.CUSTOM -> customApiKey
        ProviderType.POLLINATIONS -> ""
    }

    fun setApiKey(provider: ProviderType, key: String) {
        when (provider) {
            ProviderType.GEMINI -> geminiApiKey = key
            ProviderType.GROQ -> groqApiKey = key
            ProviderType.OPENROUTER -> openRouterApiKey = key
            ProviderType.CEREBRAS -> cerebrasApiKey = key
            ProviderType.HUGGINGFACE -> huggingFaceApiKey = key
            ProviderType.CUSTOM -> customApiKey = key
            ProviderType.POLLINATIONS -> Unit
        }
    }

    fun isKeyConfigured(provider: ProviderType): Boolean {
        if (!provider.requiresApiKey) return true
        return getApiKey(provider).isNotBlank()
    }
}
