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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.data.model.ProviderType

@Composable
fun QuickKeyDialog(
    preferencesManager: PreferencesManager,
    initialTargetProvider: ProviderType? = null,
    onKeyUpdated: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    var geminiKey by remember { mutableStateOf(preferencesManager.geminiApiKey) }
    var groqKey by remember { mutableStateOf(preferencesManager.groqApiKey) }
    var openRouterKey by remember { mutableStateOf(preferencesManager.openRouterApiKey) }
    var customKey by remember { mutableStateOf(preferencesManager.customApiKey) }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
            .testTag("quick_key_dialog"),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Key,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("API Keys & Backup", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        text = "Enter, copy, or paste your API keys",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // One-click Backup / Restore Banner to "keep keys"
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Keep & Backup Keys",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Copy all configured keys as a safe JSON backup, or import a previously copied key bundle with 1 tap.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
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
                                modifier = Modifier.weight(1f).testTag("export_keys_backup_button")
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
                                            onKeyUpdated()
                                            Toast.makeText(context, "Keys restored from backup successfully!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Clipboard did not contain a valid keys JSON backup", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f).testTag("import_keys_backup_button")
                            ) {
                                Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Restore Keys", fontSize = 12.sp)
                            }
                        }
                    }
                }

                HorizontalDivider()

                // Google Gemini Key
                ApiKeyInputField(
                    title = "Google Gemini API Key",
                    key = geminiKey,
                    onKeyChange = { geminiKey = it },
                    placeholder = "AIzaSy...",
                    helpUrl = "https://aistudio.google.com/app/apikey",
                    testTagPrefix = "quick_gemini_key"
                )

                HorizontalDivider()

                // Groq API Key
                ApiKeyInputField(
                    title = "Groq Cloud API Key",
                    key = groqKey,
                    onKeyChange = { groqKey = it },
                    placeholder = "gsk_...",
                    helpUrl = "https://console.groq.com/keys",
                    testTagPrefix = "quick_groq_key"
                )

                HorizontalDivider()

                // OpenRouter API Key
                ApiKeyInputField(
                    title = "OpenRouter API Key",
                    key = openRouterKey,
                    onKeyChange = { openRouterKey = it },
                    placeholder = "sk-or-...",
                    helpUrl = "https://openrouter.ai/keys",
                    testTagPrefix = "quick_openrouter_key"
                )

                HorizontalDivider()

                // Security Note
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Security Warning: Keys are stored locally on this device. Do not share your APK publicly.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    preferencesManager.geminiApiKey = geminiKey
                    preferencesManager.groqApiKey = groqKey
                    preferencesManager.openRouterApiKey = openRouterKey
                    preferencesManager.customApiKey = customKey
                    onKeyUpdated()
                    Toast.makeText(context, "API Keys saved!", Toast.LENGTH_SHORT).show()
                    onDismiss()
                },
                modifier = Modifier.testTag("save_quick_keys_button")
            ) {
                Text("Save Keys")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
