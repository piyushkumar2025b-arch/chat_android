package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.PreferencesManager

@Composable
fun SettingsDialog(
    preferencesManager: PreferencesManager,
    onOpenThemePicker: () -> Unit,
    onDismiss: () -> Unit,
    onClearAllChats: () -> Unit
) {
    val context = LocalContext.current

    var geminiKey by remember { mutableStateOf(preferencesManager.geminiApiKey) }
    var groqKey by remember { mutableStateOf(preferencesManager.groqApiKey) }
    var openRouterKey by remember { mutableStateOf(preferencesManager.openRouterApiKey) }
    var customUrl by remember { mutableStateOf(preferencesManager.customBaseUrl) }
    var customKey by remember { mutableStateOf(preferencesManager.customApiKey) }
    var customModel by remember { mutableStateOf(preferencesManager.customModel) }
    var systemPrompt by remember { mutableStateOf(preferencesManager.systemPrompt) }
    var temperature by remember { mutableFloatStateOf(preferencesManager.temperature) }

    var showConfirmClearDialog by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
            .testTag("settings_dialog"),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Settings & Customization", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Themes & Appearance Action Button
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ColorLens,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Color Themes & Modes",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "6 themes • AMOLED • Dark / Light",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }

                        Button(
                            onClick = onOpenThemePicker,
                            modifier = Modifier.testTag("open_theme_picker_button")
                        ) {
                            Text("Change Theme", fontSize = 12.sp)
                        }
                    }
                }

                // Backup & Restore Keys Banner
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Keep & Backup Keys",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Copy all keys as a JSON backup to keep them safe, or paste a backup to restore.",
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
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("OmniChat Keys Backup", backupJson))
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
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = clipboard.primaryClip
                                    if (clip != null && clip.itemCount > 0) {
                                        val pastedText = clip.getItemAt(0).text?.toString().orEmpty()
                                        if (preferencesManager.importKeysJson(pastedText)) {
                                            geminiKey = preferencesManager.geminiApiKey
                                            groqKey = preferencesManager.groqApiKey
                                            openRouterKey = preferencesManager.openRouterApiKey
                                            customKey = preferencesManager.customApiKey
                                            Toast.makeText(context, "Keys restored from backup successfully!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Invalid backup format in clipboard", Toast.LENGTH_SHORT).show()
                                        }
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
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(20.dp).padding(top = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Security Warning: Keys configured here or injected via Secrets are stored securely on this device for testing. Do not share your APK publicly with unauthorized individuals.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }

                HorizontalDivider(color = DividerDefaults.color.copy(alpha = 0.4f))

                // Gemini API Key with Copy / Paste
                ApiKeyInputField(
                    title = "Google Gemini API Key",
                    key = geminiKey,
                    onKeyChange = { geminiKey = it },
                    placeholder = "AIzaSy...",
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

                // Custom Endpoint Options
                Text("Custom OpenAI Endpoint (Optional)", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = customUrl,
                    onValueChange = { customUrl = it },
                    label = { Text("Base URL") },
                    placeholder = { Text("http://10.0.2.2:11434/v1/chat/completions") },
                    modifier = Modifier.fillMaxWidth().testTag("custom_url_input"),
                    singleLine = true
                )
                ApiKeyInputField(
                    title = "Custom Endpoint API Key (Optional)",
                    key = customKey,
                    onKeyChange = { customKey = it },
                    placeholder = "Bearer token or key...",
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

                // Temperature Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Creativity / Temperature", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                        Text(String.format("%.2f", temperature), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = temperature,
                        onValueChange = { temperature = it },
                        valueRange = 0.0f..1.0f,
                        modifier = Modifier.fillMaxWidth().testTag("temperature_slider")
                    )
                }

                // System Prompt
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("System Instructions", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                    OutlinedTextField(
                        value = systemPrompt,
                        onValueChange = { systemPrompt = it },
                        modifier = Modifier.fillMaxWidth().testTag("system_prompt_input"),
                        maxLines = 4,
                        minLines = 2
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
                    preferencesManager.customBaseUrl = customUrl
                    preferencesManager.customApiKey = customKey
                    preferencesManager.customModel = customModel
                    preferencesManager.systemPrompt = systemPrompt
                    preferencesManager.temperature = temperature
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
