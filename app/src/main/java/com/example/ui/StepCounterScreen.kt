package com.example.ui

import android.content.Context
import android.widget.Toast
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

    fun recordSteps(newSteps: Int) {
        currentSteps = newSteps
        prefs.edit().putInt("steps_count", newSteps).apply()
        if (newSteps >= stepGoal && !goalNotified) {
            goalNotified = true
            prefs.edit().putBoolean("goal_notified", true).apply()
            AppNotificationManager.sendStepGoalAchieved(context, newSteps, stepGoal)
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
                }
            }
        }
    }
}
