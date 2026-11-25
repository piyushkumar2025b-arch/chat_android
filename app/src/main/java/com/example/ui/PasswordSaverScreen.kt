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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ProductivityDao
import com.example.data.local.VaultItemEntity
import com.example.data.local.VaultSecurityManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordSaverScreen(
    productivityDao: ProductivityDao,
    onBack: () -> Unit,
    onApplyApiKey: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val vaultSecurity = remember { VaultSecurityManager.getInstance(context) }

    var isUnlocked by remember { mutableStateOf(false) }
    var enteredPin by remember { mutableStateOf("") }
    var hasCustomPin by remember { mutableStateOf(vaultSecurity.isMasterPinSet()) }

    val vaultItems by productivityDao.getAllVaultItems().collectAsState(initial = emptyList())
    var showAddDialog by remember { mutableStateOf(false) }
    var showChangePinDialog by remember { mutableStateOf(false) }

    fun verifyEnteredPin(pin: String): Boolean {
        return if (hasCustomPin) {
            vaultSecurity.verifyMasterPin(pin)
        } else {
            pin == "1234"
        }
    }

    fun attemptUnlock(pin: String) {
        if (verifyEnteredPin(pin)) {
            isUnlocked = true
            enteredPin = ""
        } else {
            val hint = if (hasCustomPin) "Wrong PIN" else "Wrong PIN (default: 1234)"
            Toast.makeText(context, hint, Toast.LENGTH_SHORT).show()
            enteredPin = ""
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isUnlocked) "Password & Key Vault" else "Vault Locked",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("vault_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isUnlocked) {
                        IconButton(
                            onClick = { showChangePinDialog = true },
                            modifier = Modifier.testTag("vault_change_pin_button")
                        ) {
                            Icon(Icons.Default.Pin, contentDescription = "Change PIN")
                        }
                        IconButton(onClick = { isUnlocked = false; enteredPin = "" }) {
                            Icon(Icons.Default.Lock, contentDescription = "Lock Vault")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            if (isUnlocked) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.testTag("vault_add_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Password or Key")
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (!isUnlocked) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Private Vault",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (hasCustomPin) {
                            "Enter your 4-digit PIN to view saved passwords and keys."
                        } else {
                            "Enter PIN to unlock (Default: 1234). You can change it inside."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
                    )

                    // PIN Dots Display
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(bottom = 24.dp)
                    ) {
                        repeat(4) { idx ->
                            val filled = idx < enteredPin.length
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (filled) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outlineVariant
                                    )
                            )
                        }
                    }

                    // Keypad for PIN
                    val keypadDigits = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "Clear", "0", "Unlock")
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        keypadDigits.chunked(3).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                row.forEach { digit ->
                                    Box(
                                        modifier = Modifier
                                            .size(68.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when (digit) {
                                                    "Unlock" -> MaterialTheme.colorScheme.primary
                                                    "Clear" -> MaterialTheme.colorScheme.errorContainer
                                                    else -> MaterialTheme.colorScheme.surfaceContainerHigh
                                                }
                                            )
                                            .clickable {
                                                when (digit) {
                                                    "Clear" -> enteredPin = ""
                                                    "Unlock" -> attemptUnlock(enteredPin)
                                                    else -> {
                                                        if (enteredPin.length < 4) {
                                                            enteredPin += digit
                                                            if (enteredPin.length == 4) {
                                                                attemptUnlock(enteredPin)
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                            .testTag("vault_pin_key_$digit"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = digit,
                                            fontSize = if (digit.length > 1) 14.sp else 20.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = when (digit) {
                                                "Unlock" -> MaterialTheme.colorScheme.onPrimary
                                                "Clear" -> MaterialTheme.colorScheme.onErrorContainer
                                                else -> MaterialTheme.colorScheme.onSurface
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    OutlinedButton(onClick = onBack) {
                        Text("Go Back")
                    }
                }
            } else {
                // Vault Unlocked Content
                if (vaultItems.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Key,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(52.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Nothing Saved Yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Save API keys, passwords, or private notes safely encrypted on this phone.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(vaultItems, key = { it.id }) { item ->
                            var isRevealed by remember { mutableStateOf(false) }
                            val decryptedSecret = remember(item.secretValue) {
                                vaultSecurity.decrypt(item.secretValue)
                            }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.title,
                                                fontWeight = FontWeight.SemiBold,
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                            if (item.username.isNotBlank()) {
                                                Text(
                                                    text = item.username,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        Row {
                                            IconButton(onClick = { isRevealed = !isRevealed }) {
                                                Icon(
                                                    if (isRevealed) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                    contentDescription = "Show or hide"
                                                )
                                            }
                                            IconButton(onClick = {
                                                vaultSecurity.copyPasswordToClipboard(decryptedSecret)
                                                Toast.makeText(
                                                    context,
                                                    "Copied safely (clears in 30s)",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }) {
                                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                                            }
                                            IconButton(onClick = {
                                                coroutineScope.launch { productivityDao.deleteVaultItem(item) }
                                            }) {
                                                Icon(
                                                    Icons.Default.Delete,
                                                    contentDescription = "Delete",
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.surfaceContainer)
                                            .padding(horizontal = 12.dp, vertical = 10.dp)
                                    ) {
                                        Text(
                                            text = if (isRevealed) decryptedSecret else "••••••••••••••••",
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 14.sp
                                        )
                                    }

                                    if (item.category == "API Key" || item.title.contains("Key", ignoreCase = true)) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        OutlinedButton(
                                            onClick = {
                                                onApplyApiKey(decryptedSecret)
                                                Toast.makeText(
                                                    context,
                                                    "Key applied to active AI provider",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Use as Active API Key")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var title by remember { mutableStateOf("") }
        var username by remember { mutableStateOf("") }
        var secret by remember { mutableStateOf("") }
        var category by remember { mutableStateOf("API Key") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Save New Entry") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Name (e.g. Groq Key, Email)") },
                        modifier = Modifier.fillMaxWidth().testTag("vault_title_input"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Username / Note (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = secret,
                        onValueChange = { secret = it },
                        label = { Text("Password or API Key") },
                        modifier = Modifier.fillMaxWidth().testTag("vault_secret_input"),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("API Key", "Password", "Note").forEach { cat ->
                            FilterChip(
                                selected = category == cat,
                                onClick = { category = cat },
                                label = { Text(cat, fontSize = 12.sp) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.isNotBlank() && secret.isNotBlank()) {
                            val encrypted = vaultSecurity.encrypt(secret.trim())
                            coroutineScope.launch {
                                productivityDao.insertVaultItem(
                                    VaultItemEntity(
                                        title = title.trim(),
                                        username = username.trim(),
                                        secretValue = encrypted,
                                        category = category
                                    )
                                )
                                showAddDialog = false
                            }
                        }
                    },
                    modifier = Modifier.testTag("vault_save_button")
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showChangePinDialog) {
        var newPin by remember { mutableStateOf("") }
        var confirmPin by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showChangePinDialog = false },
            title = { Text("Change Vault PIN") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Pick a 4-digit PIN to lock your vault.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = newPin,
                        onValueChange = { if (it.length <= 4 && it.all { ch -> ch.isDigit() }) newPin = it },
                        label = { Text("New 4-digit PIN") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = confirmPin,
                        onValueChange = { if (it.length <= 4 && it.all { ch -> ch.isDigit() }) confirmPin = it },
                        label = { Text("Confirm PIN") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (hasCustomPin) {
                        TextButton(
                            onClick = {
                                vaultSecurity.disableMasterPin()
                                hasCustomPin = false
                                showChangePinDialog = false
                                Toast.makeText(context, "PIN reset to default (1234)", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text("Reset to default PIN (1234)")
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        when {
                            newPin.length != 4 -> Toast.makeText(context, "PIN must be 4 digits", Toast.LENGTH_SHORT).show()
                            newPin != confirmPin -> Toast.makeText(context, "PINs do not match", Toast.LENGTH_SHORT).show()
                            vaultSecurity.setMasterPin(newPin) -> {
                                hasCustomPin = true
                                showChangePinDialog = false
                                Toast.makeText(context, "Vault PIN updated", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                ) {
                    Text("Save PIN")
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangePinDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
