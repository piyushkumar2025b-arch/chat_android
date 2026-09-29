package com.example.data.model

enum class ProviderType(
    val id: String,
    val displayName: String,
    val requiresApiKey: Boolean,
    val defaultBaseUrl: String,
    val apiKeyUrl: String,
    val description: String,
    val knownLimitInfo: String
) {
    POLLINATIONS(
        id = "pollinations",
        displayName = "Pollinations AI (Free)",
        requiresApiKey = false,
        defaultBaseUrl = "https://text.pollinations.ai/openai/chat/completions",
        apiKeyUrl = "",
        description = "100% Free • No API Key required • Instant anonymous access",
        knownLimitInfo = "Unlimited Fair-Use • No account required"
    ),
    GEMINI(
        id = "gemini",
        displayName = "Google Gemini",
        requiresApiKey = true,
        defaultBaseUrl = "https://generativelanguage.googleapis.com/v1beta/",
        apiKeyUrl = "https://aistudio.google.com/app/apikey",
        description = "Official Google Gemini models (Gemini 2.0 Flash, 1.5 Pro)",
        knownLimitInfo = "Free Tier: 15 RPM • 1,500 Requests/Day"
    ),
    GROQ(
        id = "groq",
        displayName = "Groq Cloud",
        requiresApiKey = true,
        defaultBaseUrl = "https://api.groq.com/openai/v1/chat/completions",
        apiKeyUrl = "https://console.groq.com/keys",
        description = "Blazing-fast LPU inference (Llama 3.3 70B, Gemma 2)",
        knownLimitInfo = "Free Tier: 30 RPM • 14,400 Requests/Day"
    ),
    CEREBRAS(
        id = "cerebras",
        displayName = "Cerebras Cloud (Free)",
        requiresApiKey = true,
        defaultBaseUrl = "https://api.cerebras.ai/v1/chat/completions",
        apiKeyUrl = "https://cloud.cerebras.ai/",
        description = "Fastest AI inference on Earth (2,100 T/s) • 1 Million Free Tokens/Day",
        knownLimitInfo = "Free Tier: 1M Tokens/Day • 30 RPM • 2100 T/s"
    ),
    OPENROUTER(
        id = "openrouter",
        displayName = "OpenRouter",
        requiresApiKey = true,
        defaultBaseUrl = "https://openrouter.ai/api/v1/chat/completions",
        apiKeyUrl = "https://openrouter.ai/keys",
        description = "Unified access to DeepSeek, Llama & Free Auto-Router",
        knownLimitInfo = "Free Models: 200 Requests/Day • 20 RPM"
    ),
    HUGGINGFACE(
        id = "huggingface",
        displayName = "Hugging Face (Free)",
        requiresApiKey = true,
        defaultBaseUrl = "https://router.huggingface.co/hf-inference/v1/chat/completions",
        apiKeyUrl = "https://huggingface.co/settings/tokens",
        description = "Serverless open-weights models (Qwen 72B, Llama 3.1)",
        knownLimitInfo = "Free Token Tier: ~30 RPM Serverless"
    ),
    CUSTOM(
        id = "custom",
        displayName = "Custom OpenAI Endpoint",
        requiresApiKey = false,
        defaultBaseUrl = "",
        apiKeyUrl = "",
        description = "Local Ollama, LM Studio, vLLM, or custom OpenAI API",
        knownLimitInfo = "Determined by your custom host"
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
        // Pollinations (100% Free - No Key) - Verified live
        AiModel(
            id = "openai-fast",
            name = "GPT-OSS 20B (Free)",
            provider = ProviderType.POLLINATIONS,
            description = "High-speed reasoning model running anonymously without an API key",
            supportsVision = false,
            isFree = true,
            tag = "🆓 Free • Verified"
        ),
        AiModel(
            id = "openai",
            name = "OpenAI Fast (Free)",
            provider = ProviderType.POLLINATIONS,
            description = "Fast, versatile general-purpose text assistant",
            supportsVision = false,
            isFree = true,
            tag = "🆓 Free • No Key"
        ),
        AiModel(
            id = "gpt-oss-20b",
            name = "GPT-OSS 20B Reasoning (Free)",
            provider = ProviderType.POLLINATIONS,
            description = "Chain-of-thought reasoning model running on free OVH cloud",
            supportsVision = false,
            isFree = true,
            tag = "🧠 Reasoning"
        ),

        // Google Gemini - Latest 3.x Models
        AiModel(
            id = "gemini-3.8-flash",
            name = "Gemini 3.8 Flash",
            provider = ProviderType.GEMINI,
            description = "Google's latest flagship multimodal model, ultra-fast and precise",
            supportsVision = true,
            isFree = false,
            tag = "⚡ Recommended"
        ),
        AiModel(
            id = "gemini-3.8-pro",
            name = "Gemini 3.8 Pro",
            provider = ProviderType.GEMINI,
            description = "State-of-the-art complex reasoning and multi-turn STEM logic",
            supportsVision = true,
            isFree = false,
            tag = "🧠 Pro Reasoning"
        ),
        AiModel(
            id = "gemini-3.1-flash-lite",
            name = "Gemini 3.1 Flash Lite",
            provider = ProviderType.GEMINI,
            description = "Ultra low latency variant engineered for rapid conversation",
            supportsVision = true,
            isFree = false,
            tag = "🚀 Ultra Fast"
        ),

        // Groq Cloud - Verified live
        AiModel(
            id = "llama-3.3-70b-versatile",
            name = "Llama 3.3 70B (Groq)",
            provider = ProviderType.GROQ,
            description = "Meta's flagship 70B open model running at 300+ tokens/sec",
            supportsVision = false,
            isFree = true,
            tag = "⚡ Blazing Fast"
        ),
        AiModel(
            id = "llama-3.1-8b-instant",
            name = "Llama 3.1 8B Instant (Groq)",
            provider = ProviderType.GROQ,
            description = "Ultra low latency 8B model with 128k context window",
            supportsVision = false,
            isFree = true,
            tag = "🚀 Instant"
        ),
        AiModel(
            id = "gemma2-9b-it",
            name = "Gemma 2 9B (Groq)",
            provider = ProviderType.GROQ,
            description = "Google's accurate instruction-tuned model on Groq LPU",
            supportsVision = false,
            isFree = true,
            tag = "🎯 Accurate"
        ),

        // Cerebras Cloud - 1,000,000 Free Tokens/Day!
        AiModel(
            id = "llama3.1-8b",
            name = "Llama 3.1 8B (Cerebras)",
            provider = ProviderType.CEREBRAS,
            description = "Fastest inference on Earth (2,100 tokens/sec) • 1M Free Tokens/Day",
            supportsVision = false,
            isFree = true,
            tag = "⚡ 2100 T/s"
        ),
        AiModel(
            id = "llama3.1-70b",
            name = "Llama 3.1 70B (Cerebras)",
            provider = ProviderType.CEREBRAS,
            description = "70B reasoning powerhouse on Cerebras Wafer-Scale Engine",
            supportsVision = false,
            isFree = true,
            tag = "🧠 70B Fast"
        ),
        AiModel(
            id = "llama-3.3-70b",
            name = "Llama 3.3 70B (Cerebras)",
            provider = ProviderType.CEREBRAS,
            description = "Meta's latest 70B release running with high throughput",
            supportsVision = false,
            isFree = true,
            tag = "🚀 Latest 70B"
        ),

        // OpenRouter - Verified live
        AiModel(
            id = "openrouter/free",
            name = "Free Models Auto-Router",
            provider = ProviderType.OPENROUTER,
            description = "Automatically routes to the best available free model on OpenRouter",
            supportsVision = true,
            isFree = true,
            tag = "🔄 Auto Free"
        ),
        AiModel(
            id = "meta-llama/llama-3.2-3b-instruct:free",
            name = "Llama 3.2 3B (Free)",
            provider = ProviderType.OPENROUTER,
            description = "Lightweight efficient model accessible with free OpenRouter key",
            supportsVision = false,
            isFree = true,
            tag = "🆓 Free"
        ),
        AiModel(
            id = "deepseek/deepseek-r1:free",
            name = "DeepSeek R1 (Free)",
            provider = ProviderType.OPENROUTER,
            description = "OpenRouter free tier chain-of-thought reasoning powerhouse",
            supportsVision = false,
            isFree = true,
            tag = "🧠 Reasoning"
        ),
        AiModel(
            id = "deepseek/deepseek-chat:free",
            name = "DeepSeek V3 (Free)",
            provider = ProviderType.OPENROUTER,
            description = "671B MoE state of the art chat model on OpenRouter free tier",
            supportsVision = false,
            isFree = true,
            tag = "🧠 671B MoE"
        ),
        AiModel(
            id = "mistralai/mistral-small-3.1-24b-instruct:free",
            name = "Mistral Small 3.1 24B (Free)",
            provider = ProviderType.OPENROUTER,
            description = "High quality 24B European open-weights model via OpenRouter",
            supportsVision = false,
            isFree = true,
            tag = "🆓 Free"
        ),

        // Hugging Face (Free Token)
        AiModel(
            id = "Qwen/Qwen2.5-72B-Instruct",
            name = "Qwen 2.5 72B (HuggingFace)",
            provider = ProviderType.HUGGINGFACE,
            description = "Top benchmark performer hosted on Hugging Face Serverless",
            supportsVision = false,
            isFree = true,
            tag = "🤗 HF Free"
        ),
        AiModel(
            id = "meta-llama/Llama-3.1-8B-Instruct",
            name = "Llama 3.1 8B (HuggingFace)",
            provider = ProviderType.HUGGINGFACE,
            description = "Fast 8B inference on Hugging Face Serverless endpoint",
            supportsVision = false,
            isFree = true,
            tag = "🤗 HF Free"
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

    val defaultModel: AiModel = models.first { it.id == "openai-fast" } // Verified Pollinations default

    fun findModel(modelId: String, providerId: String): AiModel {
        val resolvedModelId = if (providerId == ProviderType.GEMINI.id) {
            when (modelId) {
                "gemini-2.0-flash", "gemini-1.5-flash", "gemini-2.0-flash-exp" -> "gemini-3.8-flash"
                "gemini-2.0-flash-lite" -> "gemini-3.1-flash-lite"
                "gemini-1.5-pro", "gemini-2.0-pro-exp-02-05" -> "gemini-3.8-pro"
                else -> modelId
            }
        } else {
            modelId
        }

        return models.firstOrNull { it.id == resolvedModelId && it.provider.id == providerId }
            ?: models.firstOrNull { it.provider.id == providerId }
            ?: defaultModel
    }
}
