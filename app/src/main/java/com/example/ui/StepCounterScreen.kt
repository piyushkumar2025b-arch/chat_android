package com.example.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.util.AppNotificationManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.sqrt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StepCounterScreen(
    onBack: (() -> Unit)? = null,
    onSendStatsToChat: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("omnichat_pedometer", Context.MODE_PRIVATE) }

    // Saved state
    var dailyGoal by remember { mutableIntStateOf(prefs.getInt("daily_goal", 10_000)) }
    var todaySteps by remember { mutableIntStateOf(prefs.getInt("today_steps", 0)) }
    var initialHardwareStepOffset by remember { mutableFloatStateOf(prefs.getFloat("hardware_step_offset", -1f)) }

    // Runtime state
    var sensorTypeUsed by remember { mutableStateOf("Detecting sensor...") }
    var isSensorListening by remember { mutableStateOf(false) }
    var lastStepTimestamp by remember { mutableLongStateOf(0L) }
    var showGoalDialog by remember { mutableStateOf(false) }
    var tempGoalInput by remember { mutableStateOf(dailyGoal.toString()) }

    // Permission tracking
    var hasActivityPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACTIVITY_RECOGNITION
                ) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    var hasNotificationPermission by remember {
        mutableStateOf(AppNotificationManager.hasNotificationPermission(context))
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val activityGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            perms[Manifest.permission.ACTIVITY_RECOGNITION] ?: hasActivityPermission
        } else true
        hasActivityPermission = activityGranted

        val notifGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms[Manifest.permission.POST_NOTIFICATIONS] ?: hasNotificationPermission
        } else true
        hasNotificationPermission = notifGranted
    }

    // Persist todaySteps
    fun saveSteps(newSteps: Int) {
        todaySteps = newSteps
        prefs.edit().putInt("today_steps", newSteps).apply()
        // Check milestone notification
        if (newSteps >= dailyGoal && prefs.getBoolean("goal_notified_${SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())}", false).not()) {
            AppNotificationManager.sendStepGoalAchieved(context, newSteps, dailyGoal)
            prefs.edit().putBoolean("goal_notified_${SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())}", true).apply()
        }
    }

    // Hardware Sensor Registration
    DisposableEffect(hasActivityPermission) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

        val stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        val stepDetectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
        val accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        var lastAccelMagnitude = 0f
        var accelThreshold = 11.5f // M/S^2 peak threshold for step detection
        var lastPeakTime = 0L

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null) return

                when (event.sensor.type) {
                    Sensor.TYPE_STEP_COUNTER -> {
                        val totalHardwareSteps = event.values[0]
                        if (initialHardwareStepOffset < 0f) {
                            initialHardwareStepOffset = totalHardwareSteps - todaySteps
                            prefs.edit().putFloat("hardware_step_offset", initialHardwareStepOffset).apply()
                        }
                        val calculated = (totalHardwareSteps - initialHardwareStepOffset).toInt().coerceAtLeast(0)
                        if (calculated > todaySteps) {
                            saveSteps(calculated)
                            lastStepTimestamp = System.currentTimeMillis()
                        }
                    }

                    Sensor.TYPE_STEP_DETECTOR -> {
                        if (event.values[0] == 1.0f) {
                            saveSteps(todaySteps + 1)
                            lastStepTimestamp = System.currentTimeMillis()
                        }
                    }

                    Sensor.TYPE_ACCELEROMETER -> {
                        val x = event.values[0]
                        val y = event.values[1]
                        val z = event.values[2]
                        val magnitude = sqrt((x * x + y * y + z * z).toDouble()).toFloat()

                        val now = System.currentTimeMillis()
                        // Peak detection with minimum 260ms cooldown between steps
                        if (magnitude > accelThreshold && lastAccelMagnitude <= accelThreshold && (now - lastPeakTime) > 260) {
                            lastPeakTime = now
                            saveSteps(todaySteps + 1)
                            lastStepTimestamp = now
                        }
                        lastAccelMagnitude = magnitude
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        // Priority 1: Hardware Step Counter, Priority 2: Hardware Step Detector, Priority 3: Real Accelerometer
        if (stepCounterSensor != null) {
            sensorManager.registerListener(listener, stepCounterSensor, SensorManager.SENSOR_DELAY_UI)
            sensorTypeUsed = "Hardware Step Counter Sensor (Dedicated Chip)"
            isSensorListening = true
        } else if (stepDetectorSensor != null) {
            sensorManager.registerListener(listener, stepDetectorSensor, SensorManager.SENSOR_DELAY_UI)
            sensorTypeUsed = "Hardware Step Detector Sensor"
            isSensorListening = true
        } else if (accelerometerSensor != null) {
            sensorManager.registerListener(listener, accelerometerSensor, SensorManager.SENSOR_DELAY_UI)
            sensorTypeUsed = "Hardware Accelerometer (Peak Magnitude Filter)"
            isSensorListening = true
        } else {
            sensorTypeUsed = "No Motion Hardware Detected"
            isSensorListening = false
        }

        onDispose {
            sensorManager.unregisterListener(listener)
            isSensorListening = false
        }
    }

    // Calculated metrics
    val progress = (todaySteps.toFloat() / dailyGoal.toFloat()).coerceIn(0f, 1f)
    val distanceKm = (todaySteps * 0.00078).toFloat() // ~0.78 meters per step
    val distanceMiles = distanceKm * 0.621371f
    val caloriesKcal = (todaySteps * 0.042f).toInt() // ~0.042 kcal per step
    val activeMinutes = (todaySteps / 105).coerceAtLeast(0) // ~105 steps per walking minute

    val animatedProgress by animateFloatAsState(targetValue = progress, label = "progress")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top App Bar
        Surface(
            tonalElevation = 2.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.DirectionsWalk,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Live Step Counter",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isSensorListening) "Sensor Active 🟢" else "Idle",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isSensorListening) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row {
                    // Send test notification button
                    IconButton(
                        onClick = {
                            if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                permissionLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
                            } else {
                                val sent = AppNotificationManager.sendNotification(
                                    context = context,
                                    title = "🏃 Pedometer Notification",
                                    body = "You have recorded $todaySteps steps today! Goal is $dailyGoal steps.",
                                    channelId = AppNotificationManager.CHANNEL_FITNESS
                                )
                                if (sent) {
                                    Toast.makeText(context, "Real notification posted! 🔔", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Please allow notification permission.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.testTag("send_notification_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = "Test Notification",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Reset steps
                    IconButton(
                        onClick = {
                            saveSteps(0)
                            initialHardwareStepOffset = -1f
                            prefs.edit().putFloat("hardware_step_offset", -1f).apply()
                            Toast.makeText(context, "Step counter reset to 0", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset")
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Permission Banner if missing
            if (!hasActivityPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Activity Recognition Needed",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    "Grant motion permission for high-accuracy hardware sensor step tracking.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                            Button(
                                onClick = {
                                    val req = mutableListOf<String>()
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) req.add(Manifest.permission.ACTIVITY_RECOGNITION)
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) req.add(Manifest.permission.POST_NOTIFICATIONS)
                                    permissionLauncher.launch(req.toTypedArray())
                                }
                            ) {
                                Text("Allow")
                            }
                        }
                    }
                }
            }

            // Main Circular Step Counter Card
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Circular Progress with Step Count
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(200.dp)
                        ) {
                            CircularProgressIndicator(
                                progress = { 1f },
                                modifier = Modifier.size(200.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                strokeWidth = 14.dp,
                                trackColor = Color.Transparent
                            )
                            CircularProgressIndicator(
                                progress = { animatedProgress },
                                modifier = Modifier.size(200.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 14.dp,
                                strokeCap = StrokeCap.Round,
                                trackColor = Color.Transparent
                            )
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.DirectionsWalk,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                                Text(
                                    text = String.format(Locale.US, "%,d", todaySteps),
                                    style = MaterialTheme.typography.displayMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Goal: ${String.format(Locale.US, "%,d", dailyGoal)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Percentage & Status
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            val percent = (progress * 100).toInt()
                            Text(
                                text = "$percent% of daily goal",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (percent >= 100) Color(0xFF10B981) else MaterialTheme.colorScheme.primary
                            )
                            if (percent >= 100) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(Icons.Default.EmojiEvents, contentDescription = "Goal reached", tint = Color(0xFFF59E0B))
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalButton(
                                onClick = { showGoalDialog = true }
                            ) {
                                Text("Set Goal")
                            }

                            // Manual Step Test Step simulation for testing without shaking
                            OutlinedButton(
                                onClick = {
                                    saveSteps(todaySteps + 25)
                                    Toast.makeText(context, "+25 Steps added", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Text("+25 Walk")
                            }

                            if (onSendStatsToChat != null) {
                                Button(
                                    onClick = {
                                        val summary = "🏃 Daily Fitness Summary:\n• Steps: $todaySteps / $dailyGoal (${(progress * 100).toInt()}%)\n• Distance: ${String.format(Locale.US, "%.2f", distanceKm)} km (${String.format(Locale.US, "%.2f", distanceMiles)} miles)\n• Calories: $caloriesKcal kcal\n• Active Time: $activeMinutes mins\n• Sensor: $sensorTypeUsed"
                                        onSendStatsToChat(summary)
                                    }
                                ) {
                                    Text("Share to Chat")
                                }
                            }
                        }
                    }
                }
            }

            // Metric Cards Grid (Distance, Calories, Time, Pace)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Distance Card
                    ElevatedCard(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Speed, contentDescription = null, tint = Color(0xFF3B82F6), modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Distance", style = MaterialTheme.typography.labelMedium)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = String.format(Locale.US, "%.2f", distanceKm),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text("km (${String.format(Locale.US, "%.1f", distanceMiles)} mi)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    // Calories Card
                    ElevatedCard(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Burned", style = MaterialTheme.typography.labelMedium)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "$caloriesKcal",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text("kcal burned", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Active Time Card
                    ElevatedCard(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Timer, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Active Time", style = MaterialTheme.typography.labelMedium)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "$activeMinutes",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text("minutes", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    // Equivalent Energy Burned
                    ElevatedCard(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = Color(0xFF8B5CF6), modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Equivalent", style = MaterialTheme.typography.labelMedium)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            val apples = String.format(Locale.US, "%.1f", caloriesKcal / 95f)
                            Text(
                                text = "$apples",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text("🍎 apples burned", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Real Sensor Diagnostic Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            "Device Hardware Telemetry",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Source: $sensorTypeUsed",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Real-time sensor event listener actively monitoring Android SensorManager.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }

    // Goal Configuration Dialog
    if (showGoalDialog) {
        AlertDialog(
            onDismissRequest = { showGoalDialog = false },
            title = { Text("Set Daily Step Goal") },
            text = {
                Column {
                    Text("Select or enter your target daily steps:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = tempGoalInput,
                        onValueChange = { tempGoalInput = it.filter { c -> c.isDigit() } },
                        label = { Text("Steps") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(6000, 10000, 12000, 15000).forEach { preset ->
                            FilledTonalButton(
                                onClick = { tempGoalInput = preset.toString() },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("${preset / 1000}k", fontSize = 12.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = tempGoalInput.toIntOrNull() ?: 10000
                        dailyGoal = parsed.coerceIn(1000, 100000)
                        prefs.edit().putInt("daily_goal", dailyGoal).apply()
                        showGoalDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showGoalDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
