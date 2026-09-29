package com.example.data.model

enum class ProviderType(
    val id: String,
    val displayName: String,
    val requiresApiKey: Boolean,
    val defaultBaseUrl: String,
    val apiKeyUrl: String,
    val description: String
) {
    POLLINATIONS(
        id = "pollinations",
        displayName = "Pollinations AI (Free)",
        requiresApiKey = false,
        defaultBaseUrl = "https://text.pollinations.ai/openai/chat/completions",
        apiKeyUrl = "",
        description = "100% Free • No API Key required • Instant access"
    ),
    GEMINI(
        id = "gemini",
        displayName = "Google Gemini",
        requiresApiKey = true,
        defaultBaseUrl = "https://generativelanguage.googleapis.com/v1beta/",
        apiKeyUrl = "https://aistudio.google.com/app/apikey",
        description = "Google's flagship multimodal models (Flash, Pro)"
    ),
    GROQ(
        id = "groq",
        displayName = "Groq Cloud",
        requiresApiKey = true,
        defaultBaseUrl = "https://api.groq.com/openai/v1/chat/completions",
        apiKeyUrl = "https://console.groq.com/keys",
        description = "Lightning-fast inference with free tier (Llama 3.3, Gemma)"
    ),
    OPENROUTER(
        id = "openrouter",
        displayName = "OpenRouter",
        requiresApiKey = true,
        defaultBaseUrl = "https://openrouter.ai/api/v1/chat/completions",
        apiKeyUrl = "https://openrouter.ai/keys",
        description = "Unified access to DeepSeek, Mistral, Llama & free models"
    ),
    CUSTOM(
        id = "custom",
        displayName = "Custom OpenAI Endpoint",
        requiresApiKey = false,
        defaultBaseUrl = "",
        apiKeyUrl = "",
        description = "Any OpenAI-compatible API (Ollama, Together, vLLM)"
    )
}

data class AiModel(
    val id: String,
    val name: String,
    val provider: ProviderType,
    val description: String,
    val supportsVision: Boolean = false,
    val isFree: Boolean = false,
    val tag: String = ""
)

object AvailableModels {
    val models: List<AiModel> = listOf(
        // Pollinations (Free - No Key)
        AiModel(
            id = "openai",
            name = "GPT-4o Mini (Free)",
            provider = ProviderType.POLLINATIONS,
            description = "Fast, balanced general reasoning",
            supportsVision = true,
            isFree = true,
            tag = "🆓 Free • No Key"
        ),
        AiModel(
            id = "mistral",
            name = "Mistral Small (Free)",
            provider = ProviderType.POLLINATIONS,
            description = "High quality European open weights model",
            supportsVision = false,
            isFree = true,
            tag = "🆓 Free • No Key"
        ),
        AiModel(
            id = "qwen-coder",
            name = "Qwen 2.5 Coder (Free)",
            provider = ProviderType.POLLINATIONS,
            description = "Specialized for coding and technical queries",
            supportsVision = false,
            isFree = true,
            tag = "💻 Code Specialist"
        ),
        AiModel(
            id = "deepseek-r1",
            name = "DeepSeek R1 (Free)",
            provider = ProviderType.POLLINATIONS,
            description = "Deep reasoning and chain of thought",
            supportsVision = false,
            isFree = true,
            tag = "🧠 Reasoning"
        ),

        // Google Gemini
        AiModel(
            id = "gemini-3.5-flash",
            name = "Gemini 3.5 Flash",
            provider = ProviderType.GEMINI,
            description = "Next-gen multimodal, fast and versatile",
            supportsVision = true,
            isFree = false,
            tag = "⚡ Recommended"
        ),
        AiModel(
            id = "gemini-3.1-pro-preview",
            name = "Gemini 3.1 Pro",
            provider = ProviderType.GEMINI,
            description = "State of the art reasoning & STEM queries",
            supportsVision = true,
            isFree = false,
            tag = "🧠 Pro Reasoning"
        ),
        AiModel(
            id = "gemini-2.5-flash-image",
            name = "Gemini 2.5 Flash Image",
            provider = ProviderType.GEMINI,
            description = "Optimized for visual understanding and image analysis",
            supportsVision = true,
            isFree = false,
            tag = "🖼️ Vision Expert"
        ),

        // Groq
        AiModel(
            id = "llama-3.3-70b-versatile",
            name = "Llama 3.3 70B (Groq)",
            provider = ProviderType.GROQ,
            description = "Top open source model running at 300+ tokens/sec",
            supportsVision = false,
            isFree = true,
            tag = "⚡ Blazing Fast"
        ),
        AiModel(
            id = "llama-3.1-8b-instant",
            name = "Llama 3.1 8B Instant (Groq)",
            provider = ProviderType.GROQ,
            description = "Ultra low latency response for quick queries",
            supportsVision = false,
            isFree = true,
            tag = "🚀 Instant"
        ),
        AiModel(
            id = "gemma2-9b-it",
            name = "Gemma 2 9B (Groq)",
            provider = ProviderType.GROQ,
            description = "Google's lightweight instruction-tuned model",
            supportsVision = false,
            isFree = true,
            tag = "🎯 Accurate"
        ),

        // OpenRouter
        AiModel(
            id = "meta-llama/llama-3.2-3b-instruct:free",
            name = "Llama 3.2 3B (Free)",
            provider = ProviderType.OPENROUTER,
            description = "Compact lightweight assistant via OpenRouter",
            supportsVision = false,
            isFree = true,
            tag = "🆓 OpenRouter Free"
        ),
        AiModel(
            id = "google/gemini-2.0-flash-exp:free",
            name = "Gemini 2.0 Flash Exp (Free)",
            provider = ProviderType.OPENROUTER,
            description = "Gemini experimental route via OpenRouter",
            supportsVision = true,
            isFree = true,
            tag = "🆓 Multimodal"
        ),
        AiModel(
            id = "deepseek/deepseek-r1:free",
            name = "DeepSeek R1 (Free)",
            provider = ProviderType.OPENROUTER,
            description = "OpenRouter free tier reasoning powerhouse",
            supportsVision = false,
            isFree = true,
            tag = "🧠 Reasoning"
        ),
        AiModel(
            id = "qwen/qwen-2.5-coder-32b-instruct:free",
            name = "Qwen 2.5 Coder 32B (Free)",
            provider = ProviderType.OPENROUTER,
            description = "Premier programming assistance via OpenRouter",
            supportsVision = false,
            isFree = true,
            tag = "💻 Code"
        ),

        // Custom Endpoint
        AiModel(
            id = "custom-model",
            name = "Custom Model",
            provider = ProviderType.CUSTOM,
            description = "Model specified in your custom endpoint configuration",
            supportsVision = true,
            isFree = false,
            tag = "⚙️ Custom"
        )
    )

    val defaultModel: AiModel = models.first { it.id == "openai" } // Pollinations default, works out of box!

    fun findModel(modelId: String, providerId: String): AiModel {
        return models.firstOrNull { it.id == modelId && it.provider.id == providerId }
            ?: models.firstOrNull { it.provider.id == providerId }
            ?: defaultModel
    }
}
