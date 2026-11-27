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
import androidx.core.content.ContextCompat
import com.example.util.AppNotificationManager
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StepCounterScreen(
    onBack: () -> Unit,
    onSendStatsToChat: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("omnichat_steps", Context.MODE_PRIVATE) }
    var currentSteps by remember { mutableIntStateOf(prefs.getInt("steps_count", 6842)) }
    var goalNotified by remember { mutableStateOf(prefs.getBoolean("goal_notified", false)) }
    val stepGoal = 10000

    val sensorManager = remember {
        context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    }
    val stepSensor = remember(sensorManager) {
        sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    }
    var hasActivityPermission by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACTIVITY_RECOGNITION
                ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var liveSensorEnabled by remember {
        mutableStateOf(prefs.getBoolean("live_sensor_enabled", true))
    }
    var lastHardwareTotal by remember { mutableIntStateOf(-1) }

    fun recordSteps(newSteps: Int, syncBaseline: Boolean = true) {
        val clamped = newSteps.coerceAtLeast(0)
        currentSteps = clamped
        val edit = prefs.edit().putInt("steps_count", clamped)
        if (syncBaseline && lastHardwareTotal >= 0) {
            edit.putInt("sensor_baseline", lastHardwareTotal - clamped)
        }
        edit.apply()
        if (clamped >= stepGoal && !goalNotified) {
            goalNotified = true
            prefs.edit().putBoolean("goal_notified", true).apply()
            AppNotificationManager.sendStepGoalAchieved(context, clamped, stepGoal)
        }
    }

    val activityPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasActivityPermission = granted
        if (granted) {
            liveSensorEnabled = true
            prefs.edit().putBoolean("live_sensor_enabled", true).apply()
            Toast.makeText(context, "Live phone step sensor enabled", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Activity permission denied — manual logging remains active", Toast.LENGTH_SHORT).show()
        }
    }

    DisposableEffect(sensorManager, stepSensor, hasActivityPermission, liveSensorEnabled) {
        val activeSensor = stepSensor
        if (sensorManager != null && activeSensor != null && hasActivityPermission && liveSensorEnabled) {
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent?) {
                    val totalSinceBoot = event?.values?.firstOrNull()?.toInt() ?: return
                    if (totalSinceBoot < 0) return
                    lastHardwareTotal = totalSinceBoot
                    var baseline = prefs.getInt("sensor_baseline", Int.MIN_VALUE)
                    if (baseline == Int.MIN_VALUE || totalSinceBoot < baseline) {
                        baseline = totalSinceBoot - currentSteps
                        prefs.edit().putInt("sensor_baseline", baseline).apply()
                    }
                    val computed = (totalSinceBoot - baseline).coerceAtLeast(0)
                    if (computed != currentSteps) {
                        recordSteps(computed, syncBaseline = false)
                    }
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
            }
            sensorManager.registerListener(listener, activeSensor, SensorManager.SENSOR_DELAY_UI)
            onDispose {
                sensorManager.unregisterListener(listener)
            }
        } else {
            onDispose { }
        }
    }

    val progress = (currentSteps.toFloat() / stepGoal).coerceIn(0f, 1f)
    val distanceKm = "%.2f".format(currentSteps * 0.00078)
    val caloriesBurned = (currentSteps * 0.04).toInt()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Step Counter", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("step_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            onSendStatsToChat("My Daily Fitness Stats:\n- Steps: $currentSteps / $stepGoal (${(progress * 100).toInt()}% of goal)\n- Distance: $distanceKm km\n- Calories Burned: $caloriesBurned kcal\n\nPlease give me coaching feedback and hydration advice.")
                            Toast.makeText(context, "Stats sent to chat!", Toast.LENGTH_SHORT).show()
                            onBack()
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send to Chat", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Circular Progress Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(180.dp)) {
                        CircularProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxSize(),
                            strokeWidth = 14.dp,
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.AutoMirrored.Filled.DirectionsRun,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(34.dp)
                            )
                            Text(
                                text = "$currentSteps",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "of $stepGoal steps",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Timeline, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text("$distanceKm km", fontWeight = FontWeight.SemiBold)
                            Text("Distance", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text("$caloriesBurned kcal", fontWeight = FontWeight.SemiBold)
                            Text("Burned", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Quick Actions
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Add Steps", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = {
                                recordSteps(currentSteps + 250)
                                Toast.makeText(context, "+250 steps added", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f).testTag("add_250_steps_button")
                        ) {
                            Text("+250 Steps")
                        }
                        Button(
                            onClick = {
                                recordSteps(currentSteps + 1000)
                                Toast.makeText(context, "+1,000 steps added", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f).testTag("add_1000_steps_button")
                        ) {
                            Text("+1,000 Steps")
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            goalNotified = false
                            prefs.edit().putBoolean("goal_notified", false).apply()
                            recordSteps(0)
                        },
                        modifier = Modifier.fillMaxWidth().testTag("reset_steps_button")
                    ) {
                        Text("Reset Counter")
                    }

                    if (stepSensor != null) {
                        if (!hasActivityPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            OutlinedButton(
                                onClick = {
                                    activityPermissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
                                },
                                modifier = Modifier.fillMaxWidth().testTag("enable_step_sensor_button")
                            ) {
                                Text("Enable Live Phone Step Sensor")
                            }
                        } else {
                            Text(
                                text = if (liveSensorEnabled) "Live phone step sensor active" else "Phone step sensor paused",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}
