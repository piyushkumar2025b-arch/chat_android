package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AiModel
import com.example.data.model.AvailableModels
import com.example.data.model.ProviderType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderSelectorSheet(
    sheetState: SheetState,
    selectedProvider: ProviderType,
    selectedModel: AiModel,
    isKeyConfigured: (ProviderType) -> Boolean,
    getKeyForProvider: (ProviderType) -> String,
    onUpdateKey: (ProviderType, String) -> Unit,
    onModelSelected: (ProviderType, AiModel) -> Unit,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var activeFilterProvider by remember { mutableStateOf(selectedProvider) }
    var isInlineEditingKey by remember { mutableStateOf(false) }
    var inlineKeyInput by remember(activeFilterProvider) { mutableStateOf(getKeyForProvider(activeFilterProvider)) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("provider_selector_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Select AI Provider & Model",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Switch models anytime without losing context",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = {
                        onDismiss()
                        onOpenSettings()
                    },
                    modifier = Modifier.testTag("open_settings_from_sheet")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Provider filter tabs
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(ProviderType.values()) { provider ->
                    val isSelected = activeFilterProvider == provider
                    val hasKey = isKeyConfigured(provider)
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            activeFilterProvider = provider
                            isInlineEditingKey = false
                            inlineKeyInput = getKeyForProvider(provider)
                        },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(provider.displayName.substringBefore(" ("))
                                if (provider.requiresApiKey && !hasKey) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.Key,
                                        contentDescription = "Key needed",
                                        modifier = Modifier.size(12.dp),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        },
                        modifier = Modifier.testTag("provider_chip_${provider.id}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Provider description & Inline Key Entry Card
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = activeFilterProvider.displayName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = activeFilterProvider.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // If provider requires key, show quick paste / edit affordances
                        if (activeFilterProvider.requiresApiKey) {
                            val currentKey = getKeyForProvider(activeFilterProvider)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (currentKey.isNotBlank()) {
                                    // Copy key chip
                                    IconButton(
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            clipboard.setPrimaryClip(ClipData.newPlainText("API Key", currentKey))
                                            Toast.makeText(context, "API Key copied!", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(32.dp).testTag("copy_key_chip_${activeFilterProvider.id}")
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy key", modifier = Modifier.size(16.dp))
                                    }
                                }

                                // 1-tap Paste Key from clipboard
                                AssistChip(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = clipboard.primaryClip
                                        if (clip != null && clip.itemCount > 0) {
                                            val pasted = clip.getItemAt(0).text?.toString()?.trim().orEmpty()
                                            if (pasted.isNotEmpty()) {
                                                onUpdateKey(activeFilterProvider, pasted)
                                                inlineKeyInput = pasted
                                                Toast.makeText(context, "Pasted & saved key for ${activeFilterProvider.displayName}!", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "Clipboard empty", Toast.LENGTH_SHORT).show()
                                            }
                                        } else {
                                            Toast.makeText(context, "Nothing to paste", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    label = { Text(if (currentKey.isBlank()) "Paste Key" else "Paste New", fontSize = 11.sp) },
                                    leadingIcon = {
                                        Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(13.dp))
                                    },
                                    modifier = Modifier.testTag("paste_key_chip_${activeFilterProvider.id}")
                                )

                                IconButton(
                                    onClick = { isInlineEditingKey = !isInlineEditingKey },
                                    modifier = Modifier.size(32.dp).testTag("toggle_inline_key_edit")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit key manually",
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }

                    // Expandable inline input field for typing / editing key directly
                    AnimatedVisibility(
                        visible = isInlineEditingKey && activeFilterProvider.requiresApiKey,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(modifier = Modifier.padding(top = 10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = inlineKeyInput,
                                    onValueChange = { inlineKeyInput = it },
                                    label = { Text("Enter API Key") },
                                    modifier = Modifier.weight(1f).testTag("inline_key_field"),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Text,
                                        autoCorrectEnabled = false,
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onDone = {
                                            onUpdateKey(activeFilterProvider, inlineKeyInput.trim())
                                            isInlineEditingKey = false
                                            Toast.makeText(context, "Key updated!", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                TextButton(
                                    onClick = {
                                        onUpdateKey(activeFilterProvider, inlineKeyInput.trim())
                                        isInlineEditingKey = false
                                        Toast.makeText(context, "Key updated!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.testTag("save_inline_key_button")
                                ) {
                                    Text("Save")
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Models list for selected provider
            val filteredModels = AvailableModels.models.filter { it.provider == activeFilterProvider }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            ) {
                items(filteredModels) { model ->
                    val isCurrent = selectedModel.id == model.id && selectedProvider == model.provider

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onModelSelected(model.provider, model)
                                onDismiss()
                            }
                            .testTag("model_item_${model.id}"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCurrent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = model.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (model.tag.isNotBlank()) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer
                                        ) {
                                            Text(
                                                text = model.tag,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (isCurrent) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = model.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (isCurrent) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
