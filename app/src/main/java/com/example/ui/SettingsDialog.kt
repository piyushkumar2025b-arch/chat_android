package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.AppLockManager
import com.example.data.local.PreferencesManager
import kotlin.math.roundToInt

@Composable
fun SettingsDialog(
    preferencesManager: PreferencesManager,
    onOpenThemePicker: () -> Unit,
    onOpenUsageLimits: () -> Unit,
    onDismiss: () -> Unit,
    onClearAllChats: () -> Unit,
    onKeyUpdated: (() -> Unit)? = null,
    appLockManager: AppLockManager? = null,
    onAppLockChanged: (Boolean) -> Unit = {},
    onLockNow: () -> Unit = {},
    onOpenBackupRestore: () -> Unit = {}
) {
    val context = LocalContext.current

    var geminiKey by remember { mutableStateOf(preferencesManager.geminiApiKey) }
    var groqKey by remember { mutableStateOf(preferencesManager.groqApiKey) }
    var openRouterKey by remember { mutableStateOf(preferencesManager.openRouterApiKey) }
    var cerebrasKey by remember { mutableStateOf(preferencesManager.cerebrasApiKey) }
    var huggingFaceKey by remember { mutableStateOf(preferencesManager.huggingFaceApiKey) }
    var youtubeKey by remember { mutableStateOf(preferencesManager.youtubeApiKey) }
    var googleMapsKey by remember { mutableStateOf(preferencesManager.googleMapsApiKey) }
    var customUrl by remember { mutableStateOf(preferencesManager.customBaseUrl) }
    var customKey by remember { mutableStateOf(preferencesManager.customApiKey) }
    var customModel by remember { mutableStateOf(preferencesManager.customModel) }
    var systemPrompt by remember { mutableStateOf(preferencesManager.systemPrompt) }
    var temperature by remember { mutableFloatStateOf(preferencesManager.temperature) }
    var maxTokens by remember { mutableStateOf(preferencesManager.maxOutputTokens) }
    var contextBudget by remember { mutableStateOf(preferencesManager.contextBudgetTokens) }

    var showConfirmClearDialog by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .widthIn(max = 560.dp)
            .padding(16.dp)
            .fillMaxWidth()
            .imePadding()
            .testTag("settings_dialog"),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Appearance & Themes
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ColorLens,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Appearance",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "5 colors • Light, Dark, or Black",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = onOpenThemePicker,
                            modifier = Modifier.testTag("open_theme_picker_button")
                        ) {
                            Text("Themes", fontSize = 12.sp, maxLines = 1)
                        }
                    }
                }

                // Daily Limit Card
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Daily Limit",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (preferencesManager.dailyLimit <= 0) "No daily limit" else "${preferencesManager.dailyLimit} messages per day",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = onOpenUsageLimits,
                            modifier = Modifier.testTag("open_usage_limits_button")
                        ) {
                            Text("Set Limit", fontSize = 12.sp, maxLines = 1)
                        }
                    }
                }

                // App Lock & Backup shortcuts
                appLockManager?.let { manager ->
                    AppLockSettingsSection(
                        appLockManager = manager,
                        onAppLockChanged = onAppLockChanged,
                        onLockNow = onLockNow,
                        onOpenBackupRestore = onOpenBackupRestore
                    )
                }

                // Backup & Restore Keys Banner
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Backup Saved Keys",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Copy your keys to move them safely, or paste a saved backup.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val backupJson = preferencesManager.exportKeysJson()
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    if (clipboard == null) {
                                        Toast.makeText(context, "Clipboard service unavailable", Toast.LENGTH_SHORT).show()
                                        return@OutlinedButton
                                    }
                                    val clipData = ClipData.newPlainText("OmniChat Keys Backup", backupJson)
                                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                        clipData.description.extras = android.os.PersistableBundle().apply {
                                            putBoolean(android.content.ClipDescription.EXTRA_IS_SENSITIVE, true)
                                        }
                                    }
                                    clipboard.setPrimaryClip(clipData)
                                    Toast.makeText(context, "All keys copied to clipboard for backup!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f).testTag("settings_export_keys_button")
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Backup Keys", fontSize = 12.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    val clip = clipboard?.primaryClip
                                    if (clip == null || clip.itemCount == 0) {
                                        Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT).show()
                                        return@OutlinedButton
                                    }
                                    val pastedText = clip.getItemAt(0).text?.toString().orEmpty()
                                    if (preferencesManager.importKeysJson(pastedText)) {
                                        geminiKey = preferencesManager.geminiApiKey
                                        groqKey = preferencesManager.groqApiKey
                                        openRouterKey = preferencesManager.openRouterApiKey
                                        cerebrasKey = preferencesManager.cerebrasApiKey
                                        huggingFaceKey = preferencesManager.huggingFaceApiKey
                                        youtubeKey = preferencesManager.youtubeApiKey
                                        googleMapsKey = preferencesManager.googleMapsApiKey
                                        customUrl = preferencesManager.customBaseUrl
                                        customKey = preferencesManager.customApiKey
                                        customModel = preferencesManager.customModel
                                        onKeyUpdated?.invoke()
                                        Toast.makeText(context, "Keys restored from backup successfully!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Invalid backup format in clipboard", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.weight(1f).testTag("settings_import_keys_button")
                            ) {
                                Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Restore Keys", fontSize = 12.sp)
                            }
                        }
                    }
                }

                // Security Notice Banner
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp).padding(top = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Your keys stay private on this device. Do not share exported key backups with anyone else.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(color = DividerDefaults.color.copy(alpha = 0.4f))

                // Gemini API Key (Optional custom override; Built-in Gemini works without a key)
                Text(
                    text = "Built-in Google Gemini works right away with no key. Add a key below only if you prefer using your own.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                ApiKeyInputField(
                    title = "Custom Gemini Key (Optional)",
                    key = geminiKey,
                    onKeyChange = { geminiKey = it },
                    placeholder = "Leave blank to use Built-in Gemini",
                    helpUrl = "https://aistudio.google.com/app/apikey",
                    testTagPrefix = "gemini_key"
                )

                HorizontalDivider(color = DividerDefaults.color.copy(alpha = 0.4f))

                // Groq API Key with Copy / Paste
                ApiKeyInputField(
                    title = "Groq Cloud API Key",
                    key = groqKey,
                    onKeyChange = { groqKey = it },
                    placeholder = "gsk_...",
                    helpUrl = "https://console.groq.com/keys",
                    testTagPrefix = "groq_key"
                )

                HorizontalDivider(color = DividerDefaults.color.copy(alpha = 0.4f))

                // OpenRouter API Key with Copy / Paste
                ApiKeyInputField(
                    title = "OpenRouter API Key",
                    key = openRouterKey,
                    onKeyChange = { openRouterKey = it },
                    placeholder = "sk-or-...",
                    helpUrl = "https://openrouter.ai/keys",
                    testTagPrefix = "openrouter_key"
                )

                HorizontalDivider(color = DividerDefaults.color.copy(alpha = 0.4f))

                // Cerebras Cloud Key (1M Free Tokens/day)
                ApiKeyInputField(
                    title = "Cerebras Cloud Free Key (1M Tokens/Day)",
                    key = cerebrasKey,
                    onKeyChange = { cerebrasKey = it },
                    placeholder = "csk-...",
                    helpUrl = "https://cloud.cerebras.ai/",
                    testTagPrefix = "cerebras_key"
                )

                HorizontalDivider(color = DividerDefaults.color.copy(alpha = 0.4f))

                // Hugging Face Token with Copy / Paste
                ApiKeyInputField(
                    title = "Hugging Face Free Token",
                    key = huggingFaceKey,
                    onKeyChange = { huggingFaceKey = it },
                    placeholder = "hf_...",
                    helpUrl = "https://huggingface.co/settings/tokens",
                    testTagPrefix = "huggingface_key"
                )

                HorizontalDivider(color = DividerDefaults.color.copy(alpha = 0.4f))

                // YouTube Data API v3 Key
                ApiKeyInputField(
                    title = "YouTube Data API v3 Key",
                    key = youtubeKey,
                    onKeyChange = { youtubeKey = it },
                    placeholder = "AIzaSy...",
                    helpUrl = "https://console.cloud.google.com/apis/credentials",
                    testTagPrefix = "youtube_key"
                )

                HorizontalDivider(color = DividerDefaults.color.copy(alpha = 0.4f))

                // Google Maps API Key
                ApiKeyInputField(
                    title = "Google Maps API Key",
                    key = googleMapsKey,
                    onKeyChange = { googleMapsKey = it },
                    placeholder = "AIzaSy...",
                    helpUrl = "https://console.cloud.google.com/google/maps-apis/credentials",
                    testTagPrefix = "google_maps_key"
                )

                HorizontalDivider(color = DividerDefaults.color.copy(alpha = 0.4f))

                // Custom Endpoint Options
                Text("Custom AI Server (Optional)", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = customUrl,
                    onValueChange = { customUrl = it },
                    label = { Text("Server Address (URL)") },
                    placeholder = { Text("http://10.0.2.2:11434/v1/chat/completions") },
                    modifier = Modifier.fillMaxWidth().testTag("custom_url_input"),
                    singleLine = true
                )
                ApiKeyInputField(
                    title = "Server Key (Optional)",
                    key = customKey,
                    onKeyChange = { customKey = it },
                    placeholder = "Paste key or leave blank...",
                    testTagPrefix = "custom_key"
                )
                OutlinedTextField(
                    value = customModel,
                    onValueChange = { customModel = it },
                    label = { Text("Model Name") },
                    placeholder = { Text("llama3 or gpt-4o") },
                    modifier = Modifier.fillMaxWidth().testTag("custom_model_input"),
                    singleLine = true
                )

                HorizontalDivider(color = DividerDefaults.color.copy(alpha = 0.4f))

                // Creativity Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Creativity", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                        Text(String.format(java.util.Locale.US, "%.2f", temperature), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = temperature,
                        onValueChange = { temperature = it },
                        valueRange = 0.0f..1.0f,
                        modifier = Modifier.fillMaxWidth().testTag("temperature_slider")
                    )
                }

                // Reply Length Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Reply Length", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                        Text("$maxTokens words/parts", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Slider(
                        value = maxTokens.toFloat(),
                        onValueChange = { maxTokens = (Math.round(it / 1024f) * 1024).toInt() },
                        valueRange = 2048f..16384f,
                        steps = 13,
                        modifier = Modifier.fillMaxWidth().testTag("max_tokens_slider")
                    )
                    Text(
                        "Higher limits allow longer, more detailed replies without stopping early.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Custom Rules
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Custom AI Rules", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                    OutlinedTextField(
                        value = systemPrompt,
                        onValueChange = { systemPrompt = it },
                        modifier = Modifier.fillMaxWidth().testTag("system_prompt_input"),
                        maxLines = 4,
                        minLines = 2
                    )
                }

                // Chat Memory Size
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Chat Memory Size", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "~${contextBudget / 1000}k",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = contextBudget.toFloat(),
                        onValueChange = {
                            contextBudget = ((it / 1000f).roundToInt() * 1000).coerceIn(1_000, 32_000)
                        },
                        valueRange = 1_000f..32_000f,
                        steps = 30,
                        modifier = Modifier.fillMaxWidth().testTag("context_budget_slider")
                    )
                    Text(
                        "Controls how much of the earlier chat the AI keeps in mind while replying quickly.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Start
                    )
                }

                // Danger zone: Clear all chat history
                OutlinedButton(
                    onClick = { showConfirmClearDialog = true },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth().testTag("clear_history_button")
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Clear All Chat History")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    preferencesManager.geminiApiKey = geminiKey
                    preferencesManager.groqApiKey = groqKey
                    preferencesManager.openRouterApiKey = openRouterKey
                    preferencesManager.cerebrasApiKey = cerebrasKey
                    preferencesManager.huggingFaceApiKey = huggingFaceKey
                    preferencesManager.youtubeApiKey = youtubeKey
                    preferencesManager.googleMapsApiKey = googleMapsKey
                    preferencesManager.customBaseUrl = customUrl
                    preferencesManager.customApiKey = customKey
                    preferencesManager.customModel = customModel
                    preferencesManager.systemPrompt = systemPrompt
                    preferencesManager.temperature = temperature
                    preferencesManager.maxOutputTokens = maxTokens
                    preferencesManager.contextBudgetTokens = contextBudget
                    onKeyUpdated?.invoke()
                    Toast.makeText(context, "Settings saved!", Toast.LENGTH_SHORT).show()
                    onDismiss()
                },
                modifier = Modifier.testTag("save_settings_button")
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    if (showConfirmClearDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmClearDialog = false },
            title = { Text("Clear All Chats?") },
            text = { Text("This will permanently delete all chat sessions and messages from your device.") },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmClearDialog = false
                        onClearAllChats()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmClearDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
