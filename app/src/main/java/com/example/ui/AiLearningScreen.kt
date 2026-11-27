package com.example.ui

import com.example.util.openExternalUrl
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.MenuBook
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
        title = "How AI Reads & Writes Text",
        category = "Basics",
        summary = "How AI breaks sentences into small pieces and predicts the best next word.",
        keyPoints = listOf(
            "Tokens: Words and word parts turned into numbers the model can read",
            "Attention: Checks how each word connects to every other word in a sentence",
            "Memory Window: How much of the current chat the AI can see at once",
            "Creativity: Controls whether replies stay safe and factual or more playful"
        ),
        promptForAi = "Explain in simple everyday language how AI language models read text, use tokens and attention, and decide what word to write next."
    ),
    AiTopic(
        title = "How to Write Better Prompts",
        category = "Prompting",
        summary = "Easy ways to ask clearer questions and get much better AI answers.",
        keyPoints = listOf(
            "Show Examples: Give 2 or 3 sample answers before asking for yours",
            "Step-by-Step: Ask the AI to think through a hard problem one step at a time",
            "Set a Role: Tell the AI who to act as, like a teacher or editor",
            "Add Rules: Mention the format, length, or style you want"
        ),
        promptForAi = "Teach me simple rules for writing better AI prompts, with before-and-after examples anyone can use."
    ),
    AiTopic(
        title = "AI Assistants & Tool Use",
        category = "Tools",
        summary = "How AI goes beyond chatting to search the web, read files, and run actions.",
        keyPoints = listOf(
            "Tool Calls: The AI fills in a structured request when it needs live info",
            "My Docs Search: Pulls helpful facts from your saved files before answering",
            "Connected Tools: Links the AI safely to search, maps, and calculators",
            "Checking Work: Reviews its own steps and fixes mistakes before replying"
        ),
        promptForAi = "Explain in plain English how AI assistants decide when to search the web, look up my documents, or use external tools."
    ),
    AiTopic(
        title = "How AI Understands & Makes Images",
        category = "Images",
        summary = "How AI recognizes what is in a photo and creates new artwork from a sentence.",
        keyPoints = listOf(
            "Seeing Images: Breaks a picture into small squares the AI can understand",
            "Image Creation: Starts from random static and gradually shapes a clear picture",
            "Fast Models: Newer image engines create crisp art in just a few steps",
            "Visual Q&A: Reads signs, charts, and objects directly from your photos"
        ),
        promptForAi = "Explain simply how AI can both read photos and generate brand-new images from text descriptions."
    ),
    AiTopic(
        title = "Open vs Cloud AI Models",
        category = "Models",
        summary = "How free open models compare to large cloud AI services.",
        keyPoints = listOf(
            "Open Models: Models like Llama, DeepSeek, and Qwen anyone can run or host",
            "Cloud Models: Services like Gemini, GPT, and Claude with large built-in tools",
            "Fast Hardware: Specialized chips make replies appear almost instantly",
            "Smaller Versions: Compact models can even run right on laptops and phones"
        ),
        promptForAi = "Compare open AI models like Llama and DeepSeek with cloud models like Gemini and Claude in simple terms."
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
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val categories = listOf("All", "Basics", "Prompting", "Tools", "Images", "Models")
    var selectedCategory by remember { mutableStateOf("All") }

    val filteredTopics = remember(selectedCategory) {
        if (selectedCategory == "All") AI_TOPICS
        else AI_TOPICS.filter { it.category == selectedCategory }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (onBack != null) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("ai_learning_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.School,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        "Learn AI",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Simple guides to how AI works & better prompts",
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
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
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
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Text(
                                topic.category,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
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
                Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Free Courses & Guides", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
        }

        items(EXTERNAL_RESOURCES) { res ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        openExternalUrl(context, res.url)
                    }
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(res.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f, fill = false))
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Text(res.tag, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                        Text("By ${res.provider}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(res.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(
                        onClick = {
                            openExternalUrl(context, res.url)
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
