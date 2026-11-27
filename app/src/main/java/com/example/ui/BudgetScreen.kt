package com.example.ui

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.ExpenseEntity
import com.example.data.local.PreferencesManager
import com.example.data.local.ToolkitDao
import com.example.data.remote.AiService
import com.example.data.remote.MediaSaver
import com.google.firebase.ai.type.Schema
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val BUDGET_CATEGORIES = listOf("Food", "Transport", "Shopping", "Bills", "Fun", "Health", "Income", "Other")

private fun categoryColor(category: String): Color = when (category) {
    "Food" -> Color(0xFFF59E0B)
    "Transport" -> Color(0xFF3B82F6)
    "Shopping" -> Color(0xFFEC4899)
    "Bills" -> Color(0xFF8B5CF6)
    "Fun" -> Color(0xFF10B981)
    "Health" -> Color(0xFFEF4444)
    "Income" -> Color(0xFF059669)
    else -> Color(0xFF64748B)
}

private val EXPENSE_PARSE_SCHEMA = Schema.obj(
    properties = mapOf(
        "title" to Schema.string(description = "Short item description"),
        "amount" to Schema.double(description = "Positive monetary amount"),
        "category" to Schema.enumeration(
            values = BUDGET_CATEGORIES,
            description = "Closest spending or income category"
        ),
        "isIncome" to Schema.boolean(description = "True if money received/earned, false if spent")
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    toolkitDao: ToolkitDao,
    preferencesManager: PreferencesManager,
    onBack: () -> Unit,
    onSendToChat: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currency = remember { NumberFormat.getCurrencyInstance(Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("MMM d • h:mm a", Locale.getDefault()) }

    val entries by toolkitDao.observeExpenses().collectAsState(initial = emptyList())
    var monthlyLimit by remember { mutableFloatStateOf(preferencesManager.monthlyBudgetLimit) }
    var filterTab by remember { mutableStateOf("All") }

    var smartAddText by remember { mutableStateOf("") }
    var isParsingAi by remember { mutableStateOf(false) }
    var aiError by remember { mutableStateOf<String?>(null) }

    var showManualDialog by remember { mutableStateOf(false) }
    var editingExpense by remember { mutableStateOf<ExpenseEntity?>(null) }
    var manualTitle by remember { mutableStateOf("") }
    var manualAmount by remember { mutableStateOf("") }
    var manualCategory by remember { mutableStateOf("Food") }
    var manualIsIncome by remember { mutableStateOf(false) }

    var showLimitDialog by remember { mutableStateOf(false) }
    var limitInput by remember { mutableStateOf(monthlyLimit.toInt().toString()) }

    val totalSpent = remember(entries) { entries.filter { !it.isIncome }.sumOf { it.amount } }
    val totalIncome = remember(entries) { entries.filter { it.isIncome }.sumOf { it.amount } }
    val remaining = monthlyLimit - totalSpent
    val progress = (totalSpent / monthlyLimit.toDouble().coerceAtLeast(1.0)).toFloat().coerceIn(0f, 1f)

    val visibleEntries = remember(entries, filterTab) {
        when (filterTab) {
            "Expenses" -> entries.filter { !it.isIncome }
            "Income" -> entries.filter { it.isIncome }
            else -> entries
        }
    }

    val categoryTotals = remember(entries) {
        entries.filter { !it.isIncome }
            .groupBy { it.category }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
            .entries
            .sortedByDescending { it.value }
    }

    fun smartParseWithAi() {
        val cleaned = smartAddText.trim()
        if (cleaned.isBlank()) {
            Toast.makeText(context, "Type something like: Lunch 14.50", Toast.LENGTH_SHORT).show()
            return
        }
        aiError = null
        isParsingAi = true
        scope.launch {
            AiService.generateStructuredJson(
                taskPrompt = "Extract transaction details from: \"$cleaned\"",
                responseSchema = EXPENSE_PARSE_SCHEMA,
                systemInstruction = "Extract title, numeric amount, category, and whether it is income or expense.",
                modelId = AiService.FAST_GEMINI_MODEL,
                temperature = 0.1f
            ).onSuccess { rawJson ->
                try {
                    val obj = JSONObject(rawJson)
                    val title = obj.optString("title", cleaned).ifBlank { cleaned }
                    val amount = obj.optDouble("amount", 0.0).coerceAtLeast(0.01)
                    val category = obj.optString("category", "Other")
                    val isIncome = obj.optBoolean("isIncome", category == "Income")
                    withContext(Dispatchers.IO) {
                        toolkitDao.upsertExpense(
                            ExpenseEntity(
                                title = title,
                                amount = amount,
                                category = if (category in BUDGET_CATEGORIES) category else "Other",
                                isIncome = isIncome
                            )
                        )
                    }
                    smartAddText = ""
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Added $title (${currency.format(amount)})", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    aiError = "Could not parse entry: ${e.localizedMessage ?: "format error"}"
                }
                isParsingAi = false
            }.onFailure { err ->
                aiError = err.localizedMessage ?: "Could not parse with AI."
                isParsingAi = false
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Budget & Expenses", fontWeight = FontWeight.SemiBold) },
                    navigationIcon = {
                        IconButton(onClick = onBack, modifier = Modifier.testTag("budget_back")) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        if (entries.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        val csvHeader = "Title,Category,Type,Amount,Date\n"
                                        val rows = entries.joinToString("\n") { item ->
                                            val safeTitle = item.title.replace("\"", "\"\"")
                                            val kind = if (item.isIncome) "Income" else "Expense"
                                            val dateStr = dateFormat.format(Date(item.timestamp))
                                            "\"$safeTitle\",\"${item.category}\",$kind,${item.amount},\"$dateStr\""
                                        }
                                        val res = MediaSaver.saveDocumentToDownloads(
                                            context,
                                            "omnichat_budget_${System.currentTimeMillis() / 1000}.csv",
                                            csvHeader + rows,
                                            "text/csv"
                                        )
                                        withContext(Dispatchers.Main) {
                                            res.onSuccess {
                                                Toast.makeText(context, "Saved CSV to Downloads/OmniChat", Toast.LENGTH_SHORT).show()
                                            }.onFailure { err ->
                                                Toast.makeText(context, err.localizedMessage ?: "Export failed", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.testTag("budget_export_csv_button")
                            ) {
                                Icon(Icons.Default.Download, contentDescription = "Export CSV")
                            }
                        }
                        IconButton(
                            onClick = {
                                limitInput = monthlyLimit.toInt().toString()
                                showLimitDialog = true
                            },
                            modifier = Modifier.testTag("budget_edit_limit_button")
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit budget goal")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingExpense = null
                    manualTitle = ""
                    manualAmount = ""
                    manualCategory = "Food"
                    manualIsIncome = false
                    showManualDialog = true
                },
                modifier = Modifier.testTag("budget_add_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add entry")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("budget_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Budget overview card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Spent This Month", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    currency.format(totalSpent),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Budget Goal", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    currency.format(monthlyLimit),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        LinearProgressIndicator(
                            progress = { progress },
                            color = if (remaining < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (remaining >= 0) "${currency.format(remaining)} left" else "${currency.format(-remaining)} over budget",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (remaining < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Income: ${currency.format(totalIncome)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF059669),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // AI Quick Add
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text("AI Quick Entry", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = smartAddText,
                                onValueChange = { smartAddText = it },
                                placeholder = { Text("e.g. Coffee 6.50 or Uber 18") },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("budget_smart_input")
                            )
                            Button(
                                onClick = { smartParseWithAi() },
                                enabled = !isParsingAi && smartAddText.isNotBlank(),
                                modifier = Modifier.testTag("budget_smart_add_button")
                            ) {
                                if (isParsingAi) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                } else {
                                    Text("Log")
                                }
                            }
                        }
                        if (!aiError.isNullOrBlank()) {
                            Text(
                                text = aiError.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                Triple("Coffee", 5.0, "Food"),
                                Triple("Lunch", 15.0, "Food"),
                                Triple("Groceries", 45.0, "Shopping"),
                                Triple("Ride", 18.0, "Transport")
                            ).forEach { (label, amt, cat) ->
                                AssistChip(
                                    onClick = {
                                        scope.launch {
                                            withContext(Dispatchers.IO) {
                                                toolkitDao.upsertExpense(
                                                    ExpenseEntity(title = label, amount = amt, category = cat)
                                                )
                                            }
                                            withContext(Dispatchers.Main) {
                                                Toast.makeText(context, "Logged $label (${currency.format(amt)})", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    label = { Text("$label ${currency.format(amt)}", maxLines = 1) },
                                    modifier = Modifier.testTag("budget_quick_$label")
                                )
                            }
                        }
                    }
                }
            }

            // Visual Spending Breakdown
            if (categoryTotals.isNotEmpty() && totalSpent > 0.0) {
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("Spending Breakdown", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Canvas(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(14.dp)
                                    .clip(RoundedCornerShape(7.dp))
                            ) {
                                var startX = 0f
                                categoryTotals.forEach { (cat, sum) ->
                                    val sliceW = ((sum / totalSpent) * size.width).toFloat()
                                    drawRect(
                                        color = categoryColor(cat),
                                        topLeft = Offset(startX, 0f),
                                        size = Size(sliceW, size.height)
                                    )
                                    startX += sliceW
                                }
                            }
                            categoryTotals.forEach { (cat, sum) ->
                                val pct = ((sum / totalSpent) * 100).toInt()
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(categoryColor(cat))
                                        )
                                        Text(cat, style = MaterialTheme.typography.bodySmall)
                                    }
                                    Text(
                                        "${currency.format(sum)} ($pct%)",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                            OutlinedButton(
                                onClick = {
                                    val summaryLines = categoryTotals.joinToString("\n") { (cat, sum) ->
                                        "- $cat: ${currency.format(sum)}"
                                    }
                                    onSendToChat(
                                        "Here is my monthly spending (${currency.format(totalSpent)} of ${currency.format(monthlyLimit)} budget):\n$summaryLines\n\nGive me 3 practical money-saving tips."
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("budget_ai_tips_button")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Ask AI for Savings Tips")
                            }
                        }
                    }
                }
            }

            // Filter row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("All", "Expenses", "Income").forEach { tab ->
                        FilterChip(
                            selected = filterTab == tab,
                            onClick = { filterTab = tab },
                            label = { Text(tab) },
                            modifier = Modifier.testTag("budget_filter_$tab")
                        )
                    }
                }
            }

            if (visibleEntries.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Savings, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                            Text("No transactions yet", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(
                                "Type a quick expense above or tap + to log one manually.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(visibleEntries, key = { it.id }) { item ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .clickable {
                                editingExpense = item
                                manualTitle = item.title
                                manualAmount = item.amount.toString()
                                manualCategory = item.category
                                manualIsIncome = item.isIncome
                                showManualDialog = true
                            }
                            .testTag("budget_entry_${item.id}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(categoryColor(item.category))
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "${item.category} · ${dateFormat.format(Date(item.timestamp))}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = (if (item.isIncome) "+" else "-") + currency.format(item.amount),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (item.isIncome) Color(0xFF059669) else MaterialTheme.colorScheme.onSurface
                            )
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        withContext(Dispatchers.IO) {
                                            toolkitDao.deleteExpenseById(item.id)
                                        }
                                    }
                                },
                                modifier = Modifier.testTag("budget_delete_${item.id}")
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete transaction")
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(64.dp)) }
        }
    }

    if (showManualDialog) {
        AlertDialog(
            onDismissRequest = {
                showManualDialog = false
                editingExpense = null
            },
            title = { Text(if (editingExpense == null) "Add Transaction" else "Edit Transaction") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = !manualIsIncome,
                            onClick = { manualIsIncome = false },
                            label = { Text("Expense") }
                        )
                        FilterChip(
                            selected = manualIsIncome,
                            onClick = {
                                manualIsIncome = true
                                manualCategory = "Income"
                            },
                            label = { Text("Income") }
                        )
                    }
                    OutlinedTextField(
                        value = manualTitle,
                        onValueChange = { manualTitle = it },
                        label = { Text("Description") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("budget_manual_title")
                    )
                    OutlinedTextField(
                        value = manualAmount,
                        onValueChange = { manualAmount = it },
                        label = { Text("Amount") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("budget_manual_amount")
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        BUDGET_CATEGORIES.forEach { cat ->
                            FilterChip(
                                selected = manualCategory == cat,
                                onClick = { manualCategory = cat },
                                label = { Text(cat) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val title = manualTitle.trim()
                        val amount = manualAmount.trim().toDoubleOrNull()
                        if (title.isNotBlank() && amount != null && amount > 0.0) {
                            val existing = editingExpense
                            scope.launch {
                                withContext(Dispatchers.IO) {
                                    toolkitDao.upsertExpense(
                                        ExpenseEntity(
                                            id = existing?.id ?: java.util.UUID.randomUUID().toString(),
                                            title = title,
                                            amount = amount,
                                            category = manualCategory,
                                            isIncome = manualIsIncome,
                                            timestamp = existing?.timestamp ?: System.currentTimeMillis()
                                        )
                                    )
                                }
                            }
                            editingExpense = null
                            showManualDialog = false
                        } else {
                            Toast.makeText(context, "Enter a valid title and positive amount", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.testTag("budget_manual_save")
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showLimitDialog) {
        AlertDialog(
            onDismissRequest = { showLimitDialog = false },
            title = { Text("Monthly Budget Goal") },
            text = {
                OutlinedTextField(
                    value = limitInput,
                    onValueChange = { limitInput = it },
                    label = { Text("Monthly limit") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val parsed = limitInput.trim().toFloatOrNull()
                        if (parsed != null && parsed >= 10f) {
                            monthlyLimit = parsed
                            preferencesManager.monthlyBudgetLimit = parsed
                            showLimitDialog = false
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLimitDialog = false }) { Text("Cancel") }
            }
        )
    }
}
