package com.example.ui

import android.text.format.DateUtils
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
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
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.FocusSessionEntity
import com.example.data.local.HabitEntity
import com.example.util.StreakCalculator
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.roundToInt

private data class FocusTimerMode(
    val label: String,
    val minutes: Int,
    val description: String
)

private val FOCUS_MODES = listOf(
    FocusTimerMode("Focus", 25, "Classic pomodoro sprint"),
    FocusTimerMode("Deep Work", 50, "Extended concentration block"),
    FocusTimerMode("Break", 5, "Step away and recharge")
)

private val HABIT_EMOJIS = listOf(
    "💧", "📚", "🏃", "🧘", "🥗", "😴", "💻", "✍️", "🎯", "✅"
)

private fun dayWeekdayLabel(epochDay: Long): String {
    val formatter = SimpleDateFormat("EEEEE", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    return formatter.format(Date(epochDay * 86_400_000L))
}

private fun dayOfMonthLabel(epochDay: Long): String {
    val formatter = SimpleDateFormat("d", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    return formatter.format(Date(epochDay * 86_400_000L))
}

private fun formatTimer(seconds: Int): String {
    val safe = seconds.coerceAtLeast(0)
    return String.format(Locale.US, "%02d:%02d", safe / 60, safe % 60)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusHabitsScreen(
    viewModel: ChatViewModel,
    onBack: () -> Unit
) {
    val habits by viewModel.habits.collectAsState()
    val checkIns by viewModel.habitCheckIns.collectAsState()
    val sessions by viewModel.focusSessions.collectAsState()

    val today = remember { StreakCalculator.localEpochDay() }
    val checkedDaysByHabit = remember(checkIns) {
        checkIns.groupBy { it.habitId }.mapValues { entry -> entry.value.map { it.epochDay }.toSet() }
    }

    var selectedModeIndex by remember { mutableIntStateOf(0) }
    var remainingSeconds by remember { mutableIntStateOf(FOCUS_MODES[0].minutes * 60) }
    var isRunning by remember { mutableStateOf(false) }
    var timerKey by remember { mutableIntStateOf(0) }
    var focusLabel by remember { mutableStateOf("") }
    var completionNotice by remember { mutableStateOf<String?>(null) }

    var showHabitEditor by remember { mutableStateOf(false) }
    var editingHabit by remember { mutableStateOf<HabitEntity?>(null) }
    var deleteTarget by remember { mutableStateOf<HabitEntity?>(null) }

    val selectedMode = FOCUS_MODES[selectedModeIndex]
    val totalSeconds = selectedMode.minutes * 60
    val elapsedProgress = if (totalSeconds == 0) 0f else 1f - (remainingSeconds.toFloat() / totalSeconds)

    LaunchedEffect(isRunning, timerKey) {
        if (!isRunning) return@LaunchedEffect
        while (remainingSeconds > 0) {
            delay(1_000L)
            remainingSeconds -= 1
        }
        isRunning = false
        viewModel.recordFocusSession(
            label = focusLabel.ifBlank { selectedMode.label },
            minutes = selectedMode.minutes,
            mode = selectedMode.label
        )
        completionNotice = "${selectedMode.minutes} min ${selectedMode.label.lowercase()} complete. Nice work!"
    }

    val sessionsToday = sessions.filter {
        StreakCalculator.localEpochDay(it.createdAt, TimeZone.getDefault()) == today
    }
    val focusMinutesToday = sessionsToday.filter { it.mode != "Break" }.sumOf { it.minutes }
    val bestStreak = habits.maxOfOrNull { habit ->
        StreakCalculator.currentStreak(
            checkedDaysByHabit[habit.id].orEmpty(),
            today
        )
    } ?: 0

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Focus & Habits", fontWeight = FontWeight.Bold)
                        Text(
                            "Deep work timer, streaks & daily momentum",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("focus_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingHabit = null
                    showHabitEditor = true
                },
                modifier = Modifier.testTag("add_habit_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add habit")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FocusStatCard(
                        label = "Focus today",
                        value = "${focusMinutesToday}m",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    FocusStatCard(
                        label = "Sessions",
                        value = sessionsToday.count { it.mode != "Break" }.toString(),
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f)
                    )
                    FocusStatCard(
                        label = "Best streak",
                        value = "${bestStreak}d",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                TimerCard(
                    modes = FOCUS_MODES,
                    selectedIndex = selectedModeIndex,
                    onSelectMode = { index ->
                        if (index != selectedModeIndex) {
                            isRunning = false
                            selectedModeIndex = index
                            remainingSeconds = FOCUS_MODES[index].minutes * 60
                            timerKey++
                            completionNotice = null
                        }
                    },
                    remainingSeconds = remainingSeconds,
                    progress = elapsedProgress,
                    isRunning = isRunning,
                    focusLabel = focusLabel,
                    onFocusLabelChange = { focusLabel = it },
                    onToggleRunning = {
                        if (remainingSeconds == 0) {
                            remainingSeconds = totalSeconds
                            timerKey++
                        }
                        completionNotice = null
                        isRunning = !isRunning
                    },
                    onReset = {
                        isRunning = false
                        remainingSeconds = totalSeconds
                        timerKey++
                        completionNotice = null
                    },
                    completionNotice = completionNotice
                )
            }

            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "AI focus coach",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Turn today's habits and focus time into a concrete plan with the assistant.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    val habitSummary = habits.joinToString("; ") { habit ->
                                        val checked = checkedDaysByHabit[habit.id].orEmpty()
                                        val streak = StreakCalculator.currentStreak(checked, today)
                                        val week = StreakCalculator.checksInWindow(checked, today)
                                        "${habit.name} (${streak}d streak, $week/${habit.targetPerWeek} this week)"
                                    }.ifBlank { "no habits tracked yet" }
                                    viewModel.queuePromptInChat(
                                        "Help me plan a realistic focus routine for today. " +
                                            "I have focused $focusMinutesToday minutes across " +
                                            "${sessionsToday.count { it.mode != "Break" }} sessions today. " +
                                            "My habits: $habitSummary. " +
                                            "Suggest a schedule with 2-3 priority focus blocks and one small habit win."
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Plan my day", fontSize = 12.sp)
                            }
                            OutlinedButton(
                                onClick = {
                                    val habitSummary = habits.joinToString("; ") { habit ->
                                        val checked = checkedDaysByHabit[habit.id].orEmpty()
                                        val streak = StreakCalculator.currentStreak(checked, today)
                                        "${habit.name} (${streak}d streak, target ${habit.targetPerWeek}/week)"
                                    }.ifBlank { "no habits tracked yet" }
                                    viewModel.queuePromptInChat(
                                        "Review my habit system and suggest improvements. " +
                                            "Focus minutes completed today: $focusMinutesToday. " +
                                            "Habits: $habitSummary. " +
                                            "Give me one habit to keep, one to adjust, and one new keystone habit to try."
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Review habits", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    "Habits",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            if (habits.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("No habits yet", fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Tap + to track a daily habit and start a streak.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(habits, key = { it.id }) { habit ->
                    HabitCard(
                        habit = habit,
                        checkedDays = checkedDaysByHabit[habit.id].orEmpty(),
                        today = today,
                        onToggleToday = { viewModel.toggleHabitCheckIn(habit.id, today) },
                        onToggleDay = { day -> viewModel.toggleHabitCheckIn(habit.id, day) },
                        onEdit = {
                            editingHabit = habit
                            showHabitEditor = true
                        },
                        onDelete = { deleteTarget = habit }
                    )
                }
            }

            item {
                Text(
                    "Recent sessions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            if (sessions.isEmpty()) {
                item {
                    Text(
                        "Finished sessions will appear here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(sessions.take(10), key = { it.id }) { session ->
                    FocusSessionRow(
                        session = session,
                        onDelete = { viewModel.deleteFocusSession(session.id) }
                    )
                }
            }
        }
    }

    if (showHabitEditor) {
        HabitEditorDialog(
            existing = editingHabit,
            onDismiss = {
                showHabitEditor = false
                editingHabit = null
            },
            onSave = { name, emoji, target ->
                val current = editingHabit
                if (current == null) {
                    viewModel.addHabit(name, emoji, target)
                } else {
                    viewModel.upsertHabit(
                        current.copy(
                            name = name,
                            emoji = emoji,
                            targetPerWeek = target.coerceIn(1, 7)
                        )
                    )
                }
                showHabitEditor = false
                editingHabit = null
            }
        )
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete habit?") },
            text = { Text("“${target.name}” and its check-in history will be permanently removed.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteHabit(target)
                        deleteTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun FocusStatCard(
    label: String,
    value: String,
    tint: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = tint.copy(alpha = 0.14f))
    ) {
        Column(modifier = Modifier.padding(vertical = 12.dp, horizontal = 10.dp)) {
            Text(
                value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = tint
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun TimerCard(
    modes: List<FocusTimerMode>,
    selectedIndex: Int,
    onSelectMode: (Int) -> Unit,
    remainingSeconds: Int,
    progress: Float,
    isRunning: Boolean,
    focusLabel: String,
    onFocusLabelChange: (String) -> Unit,
    onToggleRunning: () -> Unit,
    onReset: () -> Unit,
    completionNotice: String?
) {
    val mode = modes[selectedIndex]
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                modes.forEachIndexed { index, item ->
                    FilterChip(
                        selected = index == selectedIndex,
                        onClick = { onSelectMode(index) },
                        label = { Text("${item.label} · ${item.minutes}m", fontSize = 12.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Box(
                modifier = Modifier.size(220.dp),
                contentAlignment = Alignment.Center
            ) {
                val trackColor = MaterialTheme.colorScheme.surfaceVariant
                val accentColor = MaterialTheme.colorScheme.primary
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 16.dp.toPx()
                    val inset = strokeWidth / 2f
                    val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                    drawArc(
                        color = trackColor,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = accentColor,
                        startAngle = -90f,
                        sweepAngle = 360f * progress.coerceIn(0f, 1f),
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Timer,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        formatTimer(remainingSeconds),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        mode.description,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = focusLabel,
                onValueChange = onFocusLabelChange,
                label = { Text("What are you focusing on? (optional)") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("focus_label_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onToggleRunning,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("focus_timer_toggle")
                ) {
                    Icon(
                        if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isRunning) "Pause" else "Start ${mode.minutes}m")
                }
                IconButton(
                    onClick = onReset,
                    modifier = Modifier.testTag("focus_timer_reset")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reset timer")
                }
            }

            completionNotice?.let { notice ->
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        notice,
                        modifier = Modifier.padding(10.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun HabitCard(
    habit: HabitEntity,
    checkedDays: Set<Long>,
    today: Long,
    onToggleToday: () -> Unit,
    onToggleDay: (Long) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val streak = StreakCalculator.currentStreak(checkedDays, today)
    val weekCount = StreakCalculator.checksInWindow(checkedDays, today, 7)
    val lastSeven = StreakCalculator.lastDays(today, 7)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (today in checkedDays) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            }
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(habit.emoji, fontSize = 20.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        habit.name,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            if (streak > 0) "$streak day streak · $weekCount/${habit.targetPerWeek} this week"
                            else "$weekCount/${habit.targetPerWeek} this week",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit habit", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete habit",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                lastSeven.forEach { day ->
                    DayCell(
                        weekday = dayWeekdayLabel(day),
                        dayOfMonth = dayOfMonthLabel(day),
                        checked = day in checkedDays,
                        isToday = day == today,
                        onClick = { if (day == today) onToggleToday() else onToggleDay(day) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    weekday: String,
    dayOfMonth: String,
    checked: Boolean,
    isToday: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 4.dp)
    ) {
        Text(
            weekday,
            style = MaterialTheme.typography.labelSmall,
            color = if (isToday) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(
                    when {
                        checked -> MaterialTheme.colorScheme.primary
                        isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                if (checked) "✓" else dayOfMonth,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = when {
                    checked -> MaterialTheme.colorScheme.onPrimary
                    isToday -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

@Composable
private fun FocusSessionRow(
    session: FocusSessionEntity,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Timer,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(session.label, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "${session.mode} · ${session.minutes}m · " +
                        DateUtils.getRelativeTimeSpanString(
                            session.createdAt,
                            System.currentTimeMillis(),
                            DateUtils.MINUTE_IN_MILLIS
                        ).toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete session",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HabitEditorDialog(
    existing: HabitEntity?,
    onDismiss: () -> Unit,
    onSave: (String, String, Int) -> Unit
) {
    var name by remember { mutableStateOf(existing?.name.orEmpty()) }
    var emoji by remember { mutableStateOf(existing?.emoji ?: HABIT_EMOJIS.first()) }
    var target by remember { mutableStateOf(existing?.targetPerWeek ?: 7) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "New habit" else "Edit habit") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Habit name") },
                    placeholder = { Text("Drink water, Read 20 pages…") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Column {
                    Text("Icon", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        HABIT_EMOJIS.forEach { item ->
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (item == emoji) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .clickable { emoji = item },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(item, fontSize = 18.sp)
                            }
                        }
                    }
                }
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Target per week", style = MaterialTheme.typography.labelMedium)
                        Text(
                            "$target days",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = target.toFloat(),
                        onValueChange = { target = it.roundToInt().coerceIn(1, 7) },
                        valueRange = 1f..7f,
                        steps = 5
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name.trim(), emoji, target) },
                enabled = name.isNotBlank()
            ) {
                Text(if (existing == null) "Add habit" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
