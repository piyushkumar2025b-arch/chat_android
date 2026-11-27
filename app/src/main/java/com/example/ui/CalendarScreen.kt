package com.example.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.CalendarEventEntity
import com.example.data.local.ProductivityDao
import com.example.data.remote.AiService
import com.google.firebase.ai.type.Schema
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

private val CALENDAR_EVENT_SCHEMA = Schema.obj(
    properties = mapOf(
        "title" to Schema.string(description = "Short event title"),
        "location" to Schema.string(description = "Optional location or meeting link, empty string if none"),
        "priority" to Schema.enumeration(listOf("Low", "Normal", "High"), description = "Urgency of the event"),
        "daysFromToday" to Schema.integer(description = "Number of days from today (0 for today, 1 for tomorrow, etc.)"),
        "hour" to Schema.integer(description = "Hour of day in 24h format (0 to 23), default 9"),
        "minute" to Schema.integer(description = "Minute of hour (0 to 59), default 0")
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    productivityDao: ProductivityDao,
    onBack: () -> Unit,
    onSendScheduleToChat: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val events by productivityDao.getAllEvents().collectAsState(initial = emptyList())

    var showEditorDialog by remember { mutableStateOf(false) }
    var editingEvent by remember { mutableStateOf<CalendarEventEntity?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var priorityFilter by remember { mutableStateOf("All") }
    var smartInput by remember { mutableStateOf("") }
    var isParsingEvent by remember { mutableStateOf(false) }

    val filteredEvents = remember(events, searchQuery, priorityFilter) {
        events.filter { ev ->
            val matchesPriority = priorityFilter == "All" || ev.priority.equals(priorityFilter, ignoreCase = true)
            val matchesSearch = searchQuery.isBlank() ||
                ev.title.contains(searchQuery, ignoreCase = true) ||
                ev.location.contains(searchQuery, ignoreCase = true) ||
                ev.description.contains(searchQuery, ignoreCase = true)
            matchesPriority && matchesSearch
        }
    }

    val dateFormat = remember { SimpleDateFormat("EEE, MMM d, yyyy • h:mm a", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calendar", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("calendar_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (events.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                val summary = events.joinToString("\n") {
                                    "- ${it.title} at ${dateFormat.format(Date(it.timestamp))} (${it.location}) [Priority: ${it.priority}]"
                                }
                                onSendScheduleToChat("Here is my upcoming schedule:\n$summary\n\nPlease summarize my priorities and suggest preparation tips.")
                                Toast.makeText(context, "Schedule sent to chat!", Toast.LENGTH_SHORT).show()
                                onBack()
                            },
                            modifier = Modifier.testTag("calendar_ai_brief_button")
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = "Ask AI about schedule", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingEvent = null
                    showEditorDialog = true
                },
                modifier = Modifier.testTag("calendar_add_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Event")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("calendar_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                // Header summary card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "${events.size} Scheduled Events",
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Tap AI icon above to summarize or prep",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {
                // AI Quick Event Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = smartInput,
                        onValueChange = { smartInput = it },
                        placeholder = { Text("Quick AI add: Dentist tomorrow 3pm") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("calendar_smart_input")
                    )
                    Button(
                        onClick = {
                            val raw = smartInput.trim()
                            if (raw.isBlank()) return@Button
                            isParsingEvent = true
                            coroutineScope.launch {
                                AiService.generateStructuredJson(
                                    taskPrompt = "Parse this calendar event request: \"$raw\"",
                                    responseSchema = CALENDAR_EVENT_SCHEMA,
                                    systemInstruction = "Extract calendar event fields cleanly.",
                                    temperature = 0.1f
                                ).onSuccess { jsonStr ->
                                    runCatching {
                                        val obj = JSONObject(jsonStr)
                                        val title = obj.optString("title", raw).ifBlank { raw }
                                        val loc = obj.optString("location", "")
                                        val prio = obj.optString("priority", "Normal").let {
                                            if (it in listOf("Low", "Normal", "High")) it else "Normal"
                                        }
                                        val daysOffset = obj.optInt("daysFromToday", 0).coerceIn(0, 365)
                                        val hour = obj.optInt("hour", 9).coerceIn(0, 23)
                                        val minute = obj.optInt("minute", 0).coerceIn(0, 59)
                                        val cal = Calendar.getInstance().apply {
                                            add(Calendar.DAY_OF_YEAR, daysOffset)
                                            set(Calendar.HOUR_OF_DAY, hour)
                                            set(Calendar.MINUTE, minute)
                                            set(Calendar.SECOND, 0)
                                        }
                                        withContext(Dispatchers.IO) {
                                            productivityDao.insertEvent(
                                                CalendarEventEntity(
                                                    id = UUID.randomUUID().toString(),
                                                    title = title,
                                                    description = raw,
                                                    location = loc,
                                                    priority = prio,
                                                    timestamp = cal.timeInMillis
                                                )
                                            )
                                        }
                                        smartInput = ""
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, "Added \"$title\"", Toast.LENGTH_SHORT).show()
                                        }
                                    }.onFailure {
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, "Could not parse event details", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    isParsingEvent = false
                                }.onFailure { err ->
                                    isParsingEvent = false
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, err.localizedMessage ?: "AI add failed", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                        enabled = !isParsingEvent && smartInput.isNotBlank(),
                        modifier = Modifier.testTag("calendar_smart_add_button")
                    ) {
                        if (isParsingEvent) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            if (events.isNotEmpty()) {
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search events or locations…") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear search")
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("calendar_search_input")
                    )
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("All", "High", "Normal", "Low").forEach { opt ->
                            FilterChip(
                                selected = priorityFilter == opt,
                                onClick = { priorityFilter = opt },
                                label = { Text(opt) },
                                modifier = Modifier.testTag("calendar_filter_$opt")
                            )
                        }
                    }
                }
            }

            if (filteredEvents.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (events.isEmpty()) "No events scheduled" else "No matching events",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (events.isEmpty()) "Tap + below to add a new meeting, task, or deadline." else "Try clearing your search or priority filter.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            } else {
                items(filteredEvents, key = { it.id }) { event ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = event.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (event.priority.isNotBlank() && event.priority != "Normal") {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (event.priority == "High") MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceContainerHigh
                                        ) {
                                            Text(
                                                text = "${event.priority} Priority",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (event.priority == "High") MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Row {
                                    IconButton(onClick = {
                                        editingEvent = event
                                        showEditorDialog = true
                                    }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit Event")
                                    }
                                    IconButton(onClick = {
                                        coroutineScope.launch { productivityDao.deleteEvent(event) }
                                        Toast.makeText(context, "Event deleted", Toast.LENGTH_SHORT).show()
                                    }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }

                            Text(
                                text = dateFormat.format(Date(event.timestamp)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )

                            if (event.location.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = event.location,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (event.description.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = event.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(64.dp)) }
        }
    }

    if (showEditorDialog) {
        val isEditing = editingEvent != null
        var title by remember { mutableStateOf(editingEvent?.title ?: "") }
        var description by remember { mutableStateOf(editingEvent?.description ?: "") }
        var location by remember { mutableStateOf(editingEvent?.location ?: "") }
        var priority by remember { mutableStateOf(editingEvent?.priority ?: "Normal") }
        var selectedTimestamp by remember { mutableLongStateOf(editingEvent?.timestamp ?: System.currentTimeMillis()) }

        val dateOnlyFormat = remember { SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault()) }
        val timeOnlyFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }

        AlertDialog(
            onDismissRequest = { showEditorDialog = false },
            title = { Text(if (isEditing) "Edit Event" else "New Event") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Event Title") },
                        modifier = Modifier.fillMaxWidth().testTag("calendar_title_input"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("Location or Link") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val cal = Calendar.getInstance().apply { timeInMillis = selectedTimestamp }
                                DatePickerDialog(
                                    context,
                                    { _, year, month, day ->
                                        val updated = Calendar.getInstance().apply {
                                            timeInMillis = selectedTimestamp
                                            set(Calendar.YEAR, year)
                                            set(Calendar.MONTH, month)
                                            set(Calendar.DAY_OF_MONTH, day)
                                        }
                                        selectedTimestamp = updated.timeInMillis
                                    },
                                    cal.get(Calendar.YEAR),
                                    cal.get(Calendar.MONTH),
                                    cal.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            },
                            modifier = Modifier.weight(1f).testTag("calendar_pick_date_button")
                        ) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(dateOnlyFormat.format(Date(selectedTimestamp)), fontSize = 12.sp, maxLines = 1)
                        }

                        OutlinedButton(
                            onClick = {
                                val cal = Calendar.getInstance().apply { timeInMillis = selectedTimestamp }
                                TimePickerDialog(
                                    context,
                                    { _, hour, minute ->
                                        val updated = Calendar.getInstance().apply {
                                            timeInMillis = selectedTimestamp
                                            set(Calendar.HOUR_OF_DAY, hour)
                                            set(Calendar.MINUTE, minute)
                                        }
                                        selectedTimestamp = updated.timeInMillis
                                    },
                                    cal.get(Calendar.HOUR_OF_DAY),
                                    cal.get(Calendar.MINUTE),
                                    false
                                ).show()
                            },
                            modifier = Modifier.testTag("calendar_pick_time_button")
                        ) {
                            Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(timeOnlyFormat.format(Date(selectedTimestamp)), fontSize = 12.sp, maxLines = 1)
                        }
                    }

                    Text("Priority", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Low", "Normal", "High").forEach { option ->
                            FilterChip(
                                selected = priority == option,
                                onClick = { priority = option },
                                label = { Text(option) },
                                modifier = Modifier.testTag("calendar_priority_$option")
                            )
                        }
                    }

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Notes") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            coroutineScope.launch {
                                val eventToSave = CalendarEventEntity(
                                    id = editingEvent?.id ?: UUID.randomUUID().toString(),
                                    title = title.trim(),
                                    description = description.trim(),
                                    location = location.trim(),
                                    priority = priority,
                                    timestamp = selectedTimestamp
                                )
                                productivityDao.insertEvent(eventToSave)

                                val toastMessage = if (isEditing) "Event updated!" else "Event saved!"
                                android.os.Handler(android.os.Looper.getMainLooper()).post {
                                    Toast.makeText(context, toastMessage, Toast.LENGTH_SHORT).show()
                                }
                                showEditorDialog = false
                            }
                        }
                    },
                    modifier = Modifier.testTag("calendar_save_button")
                ) {
                    Text(if (isEditing) "Update" else "Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditorDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
