package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.AppLockManager

@Composable
fun AppLockSettingsSection(
    appLockManager: AppLockManager,
    onAppLockChanged: (Boolean) -> Unit,
    onLockNow: () -> Unit,
    onOpenBackupRestore: () -> Unit
) {
    var lockEnabled by remember { mutableStateOf(appLockManager.isEnabled()) }
    var timeout by remember { mutableStateOf(appLockManager.timeoutMillis) }
    var showSetupDialog by remember { mutableStateOf(false) }
    var showDisableDialog by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("App Lock", fontWeight = FontWeight.Bold)
                    Text(
                        if (lockEnabled) "PIN required to open OmniChat" else "Optional 4–8 digit PIN",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = lockEnabled,
                    onCheckedChange = { checked ->
                        if (checked) showSetupDialog = true else showDisableDialog = true
                    },
                    modifier = Modifier.testTag("app_lock_switch")
                )
            }

            if (lockEnabled) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    "Relock after",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    AppLockTimeoutChip(
                        label = "Immediately",
                        selected = timeout == AppLockManager.IMMEDIATE,
                        onClick = {
                            timeout = AppLockManager.IMMEDIATE
                            appLockManager.timeoutMillis = AppLockManager.IMMEDIATE
                        }
                    )
                    AppLockTimeoutChip(
                        label = "30 sec",
                        selected = timeout == AppLockManager.THIRTY_SECONDS,
                        onClick = {
                            timeout = AppLockManager.THIRTY_SECONDS
                            appLockManager.timeoutMillis = AppLockManager.THIRTY_SECONDS
                        }
                    )
                    AppLockTimeoutChip(
                        label = "1 min",
                        selected = timeout == AppLockManager.ONE_MINUTE,
                        onClick = {
                            timeout = AppLockManager.ONE_MINUTE
                            appLockManager.timeoutMillis = AppLockManager.ONE_MINUTE
                        }
                    )
                    AppLockTimeoutChip(
                        label = "5 min",
                        selected = timeout == AppLockManager.FIVE_MINUTES,
                        onClick = {
                            timeout = AppLockManager.FIVE_MINUTES
                            appLockManager.timeoutMillis = AppLockManager.FIVE_MINUTES
                        }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onLockNow,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("lock_app_now_button")
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Lock app now", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.SettingsBackupRestore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Backup & Restore", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text(
                            "Chats, notes, memories & habits",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Button(
                    onClick = onOpenBackupRestore,
                    modifier = Modifier.testTag("open_backup_restore_button")
                ) {
                    Text("Open", fontSize = 12.sp)
                }
            }
        }
    }

    if (showSetupDialog) {
        PinSetupDialog(
            onDismiss = { showSetupDialog = false },
            onConfirm = { pin ->
                val success = appLockManager.setPin(pin)
                if (success) {
                    lockEnabled = true
                    timeout = appLockManager.timeoutMillis
                    showSetupDialog = false
                    onAppLockChanged(true)
                }
                success
            }
        )
    }

    if (showDisableDialog) {
        PinVerifyDialog(
            title = "Disable app lock",
            description = "Enter your current PIN to remove the lock.",
            confirmLabel = "Disable",
            onDismiss = { showDisableDialog = false },
            onVerify = { pin ->
                val ok = appLockManager.verify(pin)
                if (ok) {
                    appLockManager.disable()
                    lockEnabled = false
                    appLockManager.clearBackgroundTimestamp()
                    showDisableDialog = false
                    onAppLockChanged(false)
                }
                ok
            }
        )
    }
}

@Composable
private fun AppLockTimeoutChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, fontSize = 11.sp) }
    )
}

@Composable
private fun PinSetupDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Boolean
) {
    var pin by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set app lock PIN") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Choose a 4–8 digit PIN. It is stored as a salted PBKDF2 hash and never in plain text.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it.filter(Char::isDigit).take(8); error = null },
                    label = { Text("New PIN") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth().testTag("app_lock_new_pin")
                )
                OutlinedTextField(
                    value = confirmation,
                    onValueChange = { confirmation = it.filter(Char::isDigit).take(8); error = null },
                    label = { Text("Confirm PIN") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth().testTag("app_lock_confirm_pin")
                )
                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when {
                        pin.length < 4 -> error = "PIN must be at least 4 digits."
                        pin != confirmation -> error = "PINs do not match."
                        !onConfirm(pin) -> error = "Could not save that PIN."
                    }
                }
            ) {
                Text("Enable lock")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun PinVerifyDialog(
    title: String,
    description: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onVerify: (String) -> Boolean
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(description, style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it.filter(Char::isDigit).take(8); error = null },
                    label = { Text("Current PIN") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth()
                )
                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (!onVerify(pin)) error = "Incorrect PIN. Try again."
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
