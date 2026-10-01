package com.example.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class AiTopic(
    val title: String,
    val category: String,
    val summary: String,
    val keyPoints: List<String>,
    val promptForAi: String
)

data class ExternalResource(
    val title: String,
    val provider: String,
    val description: String,
    val url: String,
    val tag: String
)

val AI_TOPICS = listOf(
    AiTopic(
        title = "Transformers & LLM Fundamentals",
        category = "Architecture",
        summary = "How modern language models process and generate human language through self-attention mechanisms.",
        keyPoints = listOf(
            "Tokens: Words and sub-words converted into numerical vectors",
            "Self-Attention: Calculates how each word relates to every other word",
            "Context Window: Total memory size of past tokens the model can remember",
            "Temperature: Controls randomness and creativity (0.0 = deterministic, 1.0 = creative)"
        ),
        promptForAi = "Explain how Transformer neural networks and self-attention work using an intuitive, real-world metaphor. Break down tokens, embeddings, and context windows."
    ),
    AiTopic(
        title = "Prompt Engineering Masterclass",
        category = "Prompting",
        summary = "Techniques to elicit the most accurate, structured, and creative responses from any AI model.",
        keyPoints = listOf(
            "Few-Shot: Giving 2-3 examples before asking for the task",
            "Chain-of-Thought (CoT): Asking the model to 'think step by step'",
            "ReAct: Combining reasoning and actions for complex problem solving",
            "Role & Persona Framing: Setting domain expertise and constraints upfront"
        ),
        promptForAi = "Give me a practical masterclass in Prompt Engineering. Teach me Chain-of-Thought, Few-Shot prompting, and how to write system prompts with examples."
    ),
    AiTopic(
        title = "AI Agents & Autonomous Tool Calling",
        category = "Agents",
        summary = "Systems that reason, plan, and call external tools (Web Search, APIs, Python, Files) autonomously.",
        keyPoints = listOf(
            "Function Calling: Generating structured JSON arguments for APIs",
            "RAG (Retrieval-Augmented Generation): Injecting private documents into prompts",
            "Model Context Protocol (MCP): Standardized connection to external data sources",
            "Feedback Loops: Self-correcting and iterating until goal completion"
        ),
        promptForAi = "Explain how Autonomous AI Agents and Function Calling work. How does an LLM decide when to call a tool or search the web?"
    ),
    AiTopic(
        title = "Multimodal Vision & Generative Media",
        category = "Vision & Media",
        summary = "How models perceive images, generate art, and analyze video through joint cross-modal embeddings.",
        keyPoints = listOf(
            "Vision Tokens: Converting image patches into tokens compatible with text",
            "Diffusion Models: Generating images by reversing gradual Gaussian noise",
            "Flux & SDXL: Modern flow matching and latent diffusion architectures",
            "Multimodal Reasoning: Answering complex spatial and OCR questions"
        ),
        promptForAi = "How do Multimodal LLMs and Diffusion models like Flux work under the hood? Explain how AI understands and generates images."
    ),
    AiTopic(
        title = "Open-Source vs Proprietary Ecosystem",
        category = "Models",
        summary = "The state of modern open-weight models compared to commercial cloud APIs.",
        keyPoints = listOf(
            "Open Weights: Meta Llama 3.3, DeepSeek R1, Qwen 2.5 available to run anywhere",
            "Proprietary APIs: Google Gemini 2.5, OpenAI GPT-4o, Anthropic Claude 3.5",
            "LPUs & Hardware Acceleration: Groq LPU fast inference vs GPU clusters",
            "Quantization: Running 70B models locally on phones and laptops using 4-bit weights"
        ),
        promptForAi = "Compare the top open-weight models (Llama 3.3, DeepSeek R1, Qwen 2.5) with proprietary models. What are the pros, cons, and future directions?"
    )
)

val EXTERNAL_RESOURCES = listOf(
    ExternalResource(
        title = "DeepLearning.AI Short Courses",
        provider = "Andrew Ng",
        description = "Free 1-hour courses on LangChain, Prompt Engineering, Agents, and RAG.",
        url = "https://www.deeplearning.ai/short-courses/",
        tag = "Interactive"
    ),
    ExternalResource(
        title = "Hugging Face NLP & LLM Course",
        provider = "Hugging Face",
        description = "Complete open-source guide to Transformers, datasets, fine-tuning, and tokenizers.",
        url = "https://huggingface.co/learn/nlp-course",
        tag = "Comprehensive"
    ),
    ExternalResource(
        title = "Neural Networks: Zero to Hero",
        provider = "Andrej Karpathy",
        description = "Legendary video series building GPT from scratch in pure Python.",
        url = "https://karpathy.ai/zero-to-hero.html",
        tag = "Deep Dive"
    ),
    ExternalResource(
        title = "Google AI Studio Prompt Gallery",
        provider = "Google Cloud",
        description = "Explore interactive Gemini prompt patterns, multimodal recipes, and structured outputs.",
        url = "https://aistudio.google.com/gallery",
        tag = "Hands-On"
    ),
    ExternalResource(
        title = "Anthropic Prompt Engineering Tutorial",
        provider = "Anthropic",
        description = "Interactive workbook on mastering system prompts, few-shot prompts, and XML formatting.",
        url = "https://docs.anthropic.com/en/docs/build-with-claude/prompt-engineering/overview",
        tag = "Best Practices"
    )
)

@Composable
fun AiLearningScreen(
    onSendTopicToChat: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val categories = listOf("All", "Architecture", "Prompting", "Agents", "Vision & Media", "Models")
    var selectedCategory by remember { mutableStateOf("All") }

    val filteredTopics = remember(selectedCategory) {
        if (selectedCategory == "All") AI_TOPICS
        else AI_TOPICS.filter { it.category == selectedCategory }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.School,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        "AI Learning Academy & Hub",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Master Generative AI, Prompt Engineering & Agents • Interactive",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Category Filter Chips
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat) }
                    )
                }
            }
        }

        // Topics Cards
        items(filteredTopics) { topic ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        ) {
                            Text(
                                topic.category,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(topic.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(topic.summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Spacer(modifier = Modifier.height(10.dp))

                    // Key Points
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        topic.keyPoints.forEach { pt ->
                            Row(verticalAlignment = Alignment.Top) {
                                Text("• ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                Text(pt, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { onSendTopicToChat(topic.promptForAi) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Explain with AI", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        // Top Free AI Learning Resources
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Curated Free Courses & Guides", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }

        items(EXTERNAL_RESOURCES) { res ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(res.url))
                            context.startActivity(intent)
                        } catch (_: Exception) {}
                    }
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(res.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(res.tag, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                            }
                        }
                        Text("By ${res.provider}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(res.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(res.url))
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        }
                    ) {
                        Icon(Icons.Default.OpenInBrowser, contentDescription = "Open Resource")
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}
