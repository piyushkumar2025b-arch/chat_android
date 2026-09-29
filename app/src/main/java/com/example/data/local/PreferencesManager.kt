package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("omnichat_prefs", Context.MODE_PRIVATE)

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

        const val DEFAULT_SYSTEM_PROMPT =
            "You are a friendly, highly intelligent AI assistant. Provide helpful, accurate, well-formatted, and concise answers with markdown and code snippets when needed."
    }

    var dailyLimit: Int
        get() = prefs.getInt(KEY_DAILY_LIMIT, 50)
        set(value) = prefs.edit().putInt(KEY_DAILY_LIMIT, value).apply()

    var huggingFaceApiKey: String
        get() = prefs.getString(KEY_HUGGINGFACE_API_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_HUGGINGFACE_API_KEY, value.trim()).apply()

    var selectedTheme: String
        get() = prefs.getString(KEY_SELECTED_THEME, "indigo") ?: "indigo"
        set(value) = prefs.edit().putString(KEY_SELECTED_THEME, value).apply()

    var themeMode: String
        get() = prefs.getString(KEY_THEME_MODE, "system") ?: "system"
        set(value) = prefs.edit().putString(KEY_THEME_MODE, value).apply()

    var cerebrasApiKey: String
        get() = prefs.getString(KEY_CEREBRAS_API_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_CEREBRAS_API_KEY, value.trim()).apply()

    fun exportKeysJson(): String {
        val obj = org.json.JSONObject().apply {
            put("gemini", geminiApiKey)
            put("groq", groqApiKey)
            put("openrouter", openRouterApiKey)
            put("huggingface", huggingFaceApiKey)
            put("cerebras", cerebrasApiKey)
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
            val saved = prefs.getString(KEY_GEMINI_API_KEY, "") ?: ""
            if (saved.isNotBlank()) return saved
            return try {
                val buildConfigKey = BuildConfig.GEMINI_API_KEY
                if (buildConfigKey != "MY_GEMINI_API_KEY") buildConfigKey else ""
            } catch (e: Exception) {
                ""
            }
        }
        set(value) = prefs.edit().putString(KEY_GEMINI_API_KEY, value.trim()).apply()

    var groqApiKey: String
        get() {
            val saved = prefs.getString(KEY_GROQ_API_KEY, "") ?: ""
            if (saved.isNotBlank()) return saved
            return try {
                val buildConfigKey = BuildConfig.GROQ_API_KEY
                if (buildConfigKey != "MY_GROQ_API_KEY") buildConfigKey else ""
            } catch (e: Exception) {
                ""
            }
        }
        set(value) = prefs.edit().putString(KEY_GROQ_API_KEY, value.trim()).apply()

    var openRouterApiKey: String
        get() {
            val saved = prefs.getString(KEY_OPENROUTER_API_KEY, "") ?: ""
            if (saved.isNotBlank()) return saved
            return try {
                val buildConfigKey = BuildConfig.OPENROUTER_API_KEY
                if (buildConfigKey != "MY_OPENROUTER_API_KEY") buildConfigKey else ""
            } catch (e: Exception) {
                ""
            }
        }
        set(value) = prefs.edit().putString(KEY_OPENROUTER_API_KEY, value.trim()).apply()

    var customBaseUrl: String
        get() = prefs.getString(KEY_CUSTOM_BASE_URL, "https://api.openai.com/v1/chat/completions") ?: ""
        set(value) = prefs.edit().putString(KEY_CUSTOM_BASE_URL, value.trim()).apply()

    var customApiKey: String
        get() = prefs.getString(KEY_CUSTOM_API_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_CUSTOM_API_KEY, value.trim()).apply()

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
        get() = prefs.getString(KEY_LAST_MODEL, "openai") ?: "openai"
        set(value) = prefs.edit().putString(KEY_LAST_MODEL, value).apply()
}
