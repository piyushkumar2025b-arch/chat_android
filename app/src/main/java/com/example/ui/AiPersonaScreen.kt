package com.example.ui

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.PreferencesManager
import com.example.data.model.ProviderType
import com.example.data.remote.AiService
import kotlinx.coroutines.launch

data class PersonaPreset(
    val id: String,
    val name: String,
    val icon: String,
    val tagLine: String,
    val prompt: String
)

val PERSONA_PRESETS = listOf(
    PersonaPreset(
        id = "architect",
        name = "Senior Software Architect",
        icon = "👨‍💻",
        tagLine = "Clean, production-grade code with zero fluff",
        prompt = "You are a world-class Senior Software Architect. Provide robust, clean, efficient code adhering strictly to best practices and design patterns. Skip polite filler and greetings; go straight to the architectural solution, trade-offs, and implementation."
    ),
    PersonaPreset(
        id = "zen",
        name = "Zen Mindfulness Coach",
        icon = "🧘",
        tagLine = "Warm, reflective, calming and empathetic presence",
        prompt = "You are a compassionate, deeply mindful life guide. Speak with warmth, gentle cadence, and emotional intelligence. Help the user reflect peacefully on their challenges, practice gratitude, and stay grounded."
    ),
    PersonaPreset(
        id = "sarcastic",
        name = "Witty Sarcastic Genius",
        icon = "⚡",
        tagLine = "Brilliant intellect with a sharp, playful tongue",
        prompt = "You are an astonishingly clever polymath with a sharp, humorous, and dryly sarcastic wit. You are genuinely helpful and correct, but you tease the user affectionately and provide clever, entertaining banter."
    ),
    PersonaPreset(
        id = "socratic",
        name = "Socratic Professor",
        icon = "🎓",
        tagLine = "Teaches through probing questions and critical thinking",
        prompt = "You are a distinguished classical professor using the Socratic method. Rather than giving blunt answers, challenge the user with thought-provoking questions, guided thought experiments, and deep reasoning."
    ),
    PersonaPreset(
        id = "best_friend",
        name = "Wholesome Best Friend",
        icon = "🧸",
        tagLine = "Supportive, uplifting, enthusiastic, always in your corner",
        prompt = "You are the user's caring, optimistic best friend! Talk in a casual, warm, conversational tone. Celebrate their wins with real enthusiasm, cheer them up when they are down, and offer candid friendly advice."
    ),
    PersonaPreset(
        id = "cyberpunk",
        name = "Sci-Fi Cybernetic AI",
        icon = "🤖",
        tagLine = "Futuristic protocols, tactical analysis & cold precision",
        prompt = "You are OMNI-SYS, an advanced military-grade cybernetic intelligence operating in the year 2099. Format responses with system diagnostics [STATUS: OPTIMAL], tactical efficiency, and futuristic precision."
    )
)

@Composable
fun AiPersonaScreen(
    preferencesManager: PreferencesManager,
    onApplyPersonaToChat: (String) -> Unit = {},
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var activePersonaId by remember { mutableStateOf(preferencesManager.activePersonaId) }
    var customInstructions by remember {
        mutableStateOf(
            if (preferencesManager.systemPrompt != PreferencesManager.DEFAULT_SYSTEM_PROMPT) {
                preferencesManager.systemPrompt
            } else {
                "Talk to me like my personal mentor: address me as 'Partner', keep answers actionable, and never use corporate jargon."
            }
        )
    }

    var testPrompt by remember { mutableStateOf("What are three habits that will transform my focus this week?") }
    var testReply by remember { mutableStateOf<String?>(null) }
    var isTesting by remember { mutableStateOf(false) }

    fun savePersona(prompt: String, personaId: String) {
        preferencesManager.systemPrompt = prompt
        preferencesManager.activePersonaId = personaId
        activePersonaId = personaId
        Toast.makeText(context, "AI behavior updated! All future chats will talk like this.", Toast.LENGTH_LONG).show()
    }

    fun runTestDrive(systemPrompt: String) {
        val q = testPrompt.trim()
        if (q.isEmpty()) return

        isTesting = true
        testReply = null
        coroutineScope.launch {
            val res = AiService.sendMessage(
                provider = ProviderType.POLLINATIONS,
                modelId = "openai",
                prompt = q,
                apiKey = "",
                systemPrompt = systemPrompt,
                temperature = 0.7f,
                customBaseUrl = ""
            )
            isTesting = false
            res.onSuccess { text ->
                testReply = text
            }.onFailure { err ->
                testReply = "Error testing persona: ${err.localizedMessage}"
            }
        }
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
                        modifier = Modifier.testTag("ai_persona_back_button")
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
                        .background(MaterialTheme.colorScheme.surfaceContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Psychology,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "AI Style",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Choose how the AI talks and helps you",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Active Style Banner
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Face, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Current Style", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                        Text(
                            text = if (activePersonaId == "custom") "Custom Rules Active"
                            else PERSONA_PRESETS.firstOrNull { it.id == activePersonaId }?.name ?: "Default Assistant",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Button(
                        onClick = {
                            preferencesManager.systemPrompt = PreferencesManager.DEFAULT_SYSTEM_PROMPT
                            preferencesManager.activePersonaId = "default"
                            activePersonaId = "default"
                            Toast.makeText(context, "Reset to standard assistant style", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Reset", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        // Custom Rules Input
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        "Write Custom Rules",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Tell the AI how to talk, what tone to use, or rules to follow:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = customInstructions,
                        onValueChange = { customInstructions = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .testTag("custom_persona_input"),
                        placeholder = { Text("e.g. Always call me Commander, answer in bullet points only, be very witty...") },
                        maxLines = 6
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { runTestDrive(customInstructions) },
                            enabled = !isTesting && customInstructions.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            if (isTesting) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Try It Out", style = MaterialTheme.typography.labelMedium)
                            }
                        }

                        Button(
                            onClick = { savePersona(customInstructions, "custom") },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("save_custom_persona_button")
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save & Use")
                        }
                    }
                }
            }
        }

        // Test Drive Result Card (if any)
        val currentReply = testReply
        if (currentReply != null) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sample Reply:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(currentReply, style = MaterialTheme.typography.bodyMedium, lineHeight = 20.sp)
                    }
                }
            }
        }

        // Preset Styles
        item {
            Text("Or Pick a Ready-Made Style", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }

        items(PERSONA_PRESETS) { preset ->
            val isSelected = activePersonaId == preset.id
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.surfaceContainerHigh
                    else MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        customInstructions = preset.prompt
                        savePersona(preset.prompt, preset.id)
                    }
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(preset.icon, fontSize = 26.sp)
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(preset.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                            if (isSelected) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(Icons.Default.Check, contentDescription = "Active", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            }
                        }
                        Text(preset.tagLine, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            customInstructions = preset.prompt
                            savePersona(preset.prompt, preset.id)
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(if (isSelected) "Active" else "Use", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}
