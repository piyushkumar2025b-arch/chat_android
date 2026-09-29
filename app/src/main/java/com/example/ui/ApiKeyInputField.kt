package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ApiKeyInputField(
    title: String,
    key: String,
    onKeyChange: (String) -> Unit,
    placeholder: String,
    helpUrl: String = "",
    testTagPrefix: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    // Default to visible so user can clearly see what they are entering
    var isVisible by remember { mutableStateOf(true) }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Key,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = if (key.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (helpUrl.isNotBlank()) {
                TextButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(helpUrl))
                        context.startActivity(intent)
                    },
                    modifier = Modifier.testTag("${testTagPrefix}_help_link")
                ) {
                    Text("Get Free Key", fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }

        OutlinedTextField(
            value = key,
            onValueChange = onKeyChange,
            modifier = Modifier.fillMaxWidth().testTag("${testTagPrefix}_input"),
            placeholder = { Text(placeholder) },
            singleLine = true,
            visualTransformation = if (isVisible) VisualTransformation.None else PasswordVisualTransformation(),
            // Use KeyboardType.Text so Android OS does NOT disable clipboard, suggestions, mic, or keyboard features!
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                autoCorrectEnabled = false,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { focusManager.clearFocus() }
            ),
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (key.isNotEmpty()) {
                        IconButton(
                            onClick = { onKeyChange("") },
                            modifier = Modifier.size(32.dp).testTag("${testTagPrefix}_clear_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear key",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    IconButton(
                        onClick = { isVisible = !isVisible },
                        modifier = Modifier.size(32.dp).testTag("${testTagPrefix}_visibility_toggle")
                    ) {
                        Icon(
                            imageVector = if (isVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (isVisible) "Hide key" else "Show key",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        )

        // Copy / Paste / Status action row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Paste from clipboard button
                AssistChip(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = clipboard.primaryClip
                        if (clip != null && clip.itemCount > 0) {
                            val pasted = clip.getItemAt(0).text?.toString()?.trim().orEmpty()
                            if (pasted.isNotEmpty()) {
                                onKeyChange(pasted)
                                Toast.makeText(context, "Pasted key into $title", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, "Nothing to paste", Toast.LENGTH_SHORT).show()
                        }
                    },
                    label = { Text("Paste", fontSize = 11.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(14.dp))
                    },
                    modifier = Modifier.testTag("${testTagPrefix}_paste_button")
                )

                // Copy to clipboard to keep / backup key
                if (key.isNotBlank()) {
                    AssistChip(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText(title, key))
                            Toast.makeText(context, "$title copied to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        label = { Text("Copy", fontSize = 11.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                        },
                        modifier = Modifier.testTag("${testTagPrefix}_copy_button")
                    )
                }
            }

            // Status chip
            if (key.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Active",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                Text(
                    text = "Key required",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                )
            }
        }
    }
}
