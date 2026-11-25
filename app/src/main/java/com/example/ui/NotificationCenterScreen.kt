package com.example.ui

import android.Manifest
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddAlert
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.AppNotificationManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationCenterScreen(
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("omnichat_notifs_prefs", Context.MODE_PRIVATE) }

    var isNotificationAllowed by remember {
        mutableStateOf(AppNotificationManager.hasNotificationPermission(context))
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        isNotificationAllowed = granted
        if (granted) {
            AppNotificationManager.initChannels(context)
            Toast.makeText(context, "Notifications enabled! 🔔", Toast.LENGTH_SHORT).show()
        }
    }

    var stepAlertsEnabled by remember { mutableStateOf(prefs.getBoolean("step_alerts", true)) }
    var aiTipsEnabled by remember { mutableStateOf(prefs.getBoolean("ai_tips", true)) }
    var chatRemindersEnabled by remember { mutableStateOf(prefs.getBoolean("chat_reminders", true)) }

    var customTitle by remember { mutableStateOf("OmniChat Task Reminder") }
    var customBody by remember { mutableStateOf("Remember to check your AI workspace and review chat responses!") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Top App Bar
        Surface(
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onBack != null) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("notifications_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = "Notifications",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (isNotificationAllowed) "System alerts are on" else "Permission needed to show alerts",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isNotificationAllowed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .testTag("notifications_list"),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Permission Card if disabled
            if (!isNotificationAllowed && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Notifications Turned Off",
                                    fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    "Allow notifications to receive step goals, AI tips, and reminders.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            ) {
                                Text("Enable")
                            }
                        }
                    }
                }
            }

            // Quick Test Alerts
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Test Notifications",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Send a real notification now to check how alerts look on your device:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            FilledTonalButton(
                                onClick = {
                                    if (!AppNotificationManager.isChannelEnabled(context, AppNotificationManager.CHANNEL_REMINDERS)) {
                                        Toast.makeText(context, "Turn on Task & Chat Reminders below first.", Toast.LENGTH_SHORT).show()
                                        return@FilledTonalButton
                                    }
                                    val success = AppNotificationManager.sendNotification(
                                        context = context,
                                        title = "OmniChat Reminder",
                                        body = "Your workspace is ready. Tap to continue chatting.",
                                        channelId = AppNotificationManager.CHANNEL_REMINDERS
                                    )
                                    if (success) Toast.makeText(context, "Notification sent!", Toast.LENGTH_SHORT).show()
                                    else if (!isNotificationAllowed && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                },
                                modifier = Modifier.weight(1f).testTag("send_test_notif_btn")
                            ) {
                                Text("Quick Alert")
                            }

                            FilledTonalButton(
                                onClick = {
                                    if (!AppNotificationManager.isChannelEnabled(context, AppNotificationManager.CHANNEL_FITNESS)) {
                                        Toast.makeText(context, "Turn on Step Goal Alerts below first.", Toast.LENGTH_SHORT).show()
                                        return@FilledTonalButton
                                    }
                                    val success = AppNotificationManager.sendStepGoalAchieved(
                                        context = context,
                                        steps = 10_240,
                                        goal = 10_000
                                    )
                                    if (success) Toast.makeText(context, "Step goal alert sent!", Toast.LENGTH_SHORT).show()
                                    else if (!isNotificationAllowed && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Step Goal")
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        FilledTonalButton(
                            onClick = {
                                if (!AppNotificationManager.isChannelEnabled(context, AppNotificationManager.CHANNEL_AI_TIPS)) {
                                    Toast.makeText(context, "Turn on AI Tips below first.", Toast.LENGTH_SHORT).show()
                                    return@FilledTonalButton
                                }
                                val success = AppNotificationManager.sendAiTip(
                                    context = context,
                                    topic = "My Docs",
                                    tip = "Add your PDFs or notes in My Docs to get answers grounded in your own files."
                                )
                                if (success) Toast.makeText(context, "AI tip notification sent!", Toast.LENGTH_SHORT).show()
                                else if (!isNotificationAllowed && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Lightbulb, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Send AI Tip")
                        }
                    }
                }
            }

            // Custom Notification Creator
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AddAlert, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Send Custom Reminder",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = customTitle,
                            onValueChange = { customTitle = it },
                            label = { Text("Title") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = customBody,
                            onValueChange = { customBody = it },
                            label = { Text("Message") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                if (customTitle.isBlank() || customBody.isBlank()) {
                                    Toast.makeText(context, "Title and message cannot be empty", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (!AppNotificationManager.isChannelEnabled(context, AppNotificationManager.CHANNEL_REMINDERS)) {
                                    Toast.makeText(context, "Turn on Task & Chat Reminders below first.", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                val sent = AppNotificationManager.sendNotification(
                                    context = context,
                                    title = customTitle.trim(),
                                    body = customBody.trim(),
                                    channelId = AppNotificationManager.CHANNEL_REMINDERS
                                )
                                if (sent) {
                                    Toast.makeText(context, "Reminder notification sent!", Toast.LENGTH_SHORT).show()
                                } else {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        Toast.makeText(context, "Notifications are turned off in device settings.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("dispatch_custom_notif_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Send Notification")
                        }
                    }
                }
            }

            // Notification Channels & Toggles
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Alert Types",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Step Alerts
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.DirectionsWalk, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Step Goal Alerts", fontWeight = FontWeight.SemiBold)
                                    Text("Notify when reaching 10,000 steps", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Switch(
                                checked = stepAlertsEnabled,
                                onCheckedChange = {
                                    stepAlertsEnabled = it
                                    prefs.edit().putBoolean("step_alerts", it).apply()
                                },
                                modifier = Modifier.testTag("switch_step_alerts")
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                        // AI Tips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("AI Tips & Summaries", fontWeight = FontWeight.SemiBold)
                                    Text("Helpful tips and short digests", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Switch(
                                checked = aiTipsEnabled,
                                onCheckedChange = {
                                    aiTipsEnabled = it
                                    prefs.edit().putBoolean("ai_tips", it).apply()
                                },
                                modifier = Modifier.testTag("switch_ai_tips")
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                        // Chat Reminders
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Task & Chat Reminders", fontWeight = FontWeight.SemiBold)
                                    Text("Custom and scheduled reminders", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Switch(
                                checked = chatRemindersEnabled,
                                onCheckedChange = {
                                    chatRemindersEnabled = it
                                    prefs.edit().putBoolean("chat_reminders", it).apply()
                                },
                                modifier = Modifier.testTag("switch_chat_reminders")
                            )
                        }
                    }
                }
            }
        }
    }
}
