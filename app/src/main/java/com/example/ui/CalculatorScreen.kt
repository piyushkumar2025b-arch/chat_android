package com.example.ui

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Transform
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(
    onBack: () -> Unit,
    onSendResultToChat: (String) -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Standard Calculator, 1 = Unit Converter

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Omni Calculator & Converter", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("calc_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Calculator") },
                    icon = { Icon(Icons.Default.Calculate, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Unit Converter") },
                    icon = { Icon(Icons.Default.Transform, contentDescription = null) }
                )
            }

            if (selectedTab == 0) {
                StandardCalculatorView(onSendResultToChat = onSendResultToChat)
            } else {
                UnitConverterView()
            }
        }
    }
}

@Composable
private fun StandardCalculatorView(onSendResultToChat: (String) -> Unit) {
    var expression by remember { mutableStateOf("") }
    var resultText by remember { mutableStateOf("0") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Display Screen
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = expression.ifEmpty { "0" },
                    fontSize = 24.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontFamily = FontFamily.Monospace,
                    maxLines = 2
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = resultText,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1
                )
            }
        }

        // Action: Send to chat
        if (resultText != "0" && resultText != "Error") {
            OutlinedButton(
                onClick = { onSendResultToChat("Calculator Result: $expression = $resultText") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Text("Insert Result into AI Chat")
            }
        }

        // Keypad Grid - includes Backspace (Fixing BUG-06)
        val buttons = listOf(
            "C", "(", ")", "⌫",
            "7", "8", "9", "÷",
            "4", "5", "6", "×",
            "1", "2", "3", "-",
            "0", ".", "=", "+"
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(buttons) { btn ->
                val isOp = btn in listOf("÷", "×", "-", "+", "=")
                val isSpecial = btn in listOf("C", "⌫", "(", ")")
                val bgColor = when {
                    btn == "=" -> MaterialTheme.colorScheme.primary
                    isOp -> MaterialTheme.colorScheme.primaryContainer
                    isSpecial -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
                    else -> MaterialTheme.colorScheme.surfaceContainerHighest
                }
                val textColor = when {
                    btn == "=" -> MaterialTheme.colorScheme.onPrimary
                    isOp -> MaterialTheme.colorScheme.onPrimaryContainer
                    isSpecial -> MaterialTheme.colorScheme.onErrorContainer
                    else -> MaterialTheme.colorScheme.onSurface
                }

                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(bgColor)
                        .clickable {
                            when (btn) {
                                "C" -> {
                                    expression = ""
                                    resultText = "0"
                                }
                                "⌫" -> {
                                    // FIX BUG-06: Single character backspace
                                    if (expression.isNotEmpty()) {
                                        expression = expression.dropLast(1)
                                        resultText = if (expression.isEmpty()) "0" else evalQuick(expression)
                                    }
                                }
                                "=" -> {
                                    resultText = evalQuick(expression)
                                }
                                else -> {
                                    expression += btn
                                    resultText = evalQuick(expression)
                                }
                            }
                        }
                        .testTag("calc_key_$btn"),
                    contentAlignment = Alignment.Center
                ) {
                    if (btn == "⌫") {
                        Icon(
                            Icons.AutoMirrored.Filled.Backspace,
                            contentDescription = "Backspace",
                            tint = textColor,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Text(
                            text = btn,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textColor
                        )
                    }
                }
            }
        }
    }
}

// FIX BUG-06: Interactive Unit Converter with category selector, unit pickers, and bidirectional swap
@Composable
private fun UnitConverterView() {
    var category by remember { mutableStateOf("Speed") }
    var inputValue by remember { mutableStateOf("100") }

    val categories = listOf("Speed", "Length", "Weight", "Temperature")

    val unitOptions = remember(category) {
        when (category) {
            "Speed" -> listOf("km/h", "mph", "m/s", "knots")
            "Length" -> listOf("Meters", "Kilometers", "Miles", "Feet", "Inches")
            "Weight" -> listOf("Kilograms", "Grams", "Pounds", "Ounces")
            "Temperature" -> listOf("Celsius", "Fahrenheit", "Kelvin")
            else -> listOf("Unit A", "Unit B")
        }
    }

    var fromUnit by remember(category) { mutableStateOf(unitOptions.first()) }
    var toUnit by remember(category) { mutableStateOf(unitOptions.getOrElse(1) { unitOptions.first() }) }

    var expandedFrom by remember { mutableStateOf(false) }
    var expandedTo by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Category Chips
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            categories.forEach { cat ->
                FilterChip(
                    selected = category == cat,
                    onClick = {
                        category = cat
                    },
                    label = { Text(cat) }
                )
            }
        }

        // Input value
        OutlinedTextField(
            value = inputValue,
            onValueChange = { inputValue = it },
            label = { Text("Input Value") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // Selectors & Swap Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // From Unit Dropdown
            Box(modifier = Modifier.weight(1f)) {
                OutlinedButton(
                    onClick = { expandedFrom = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("From: $fromUnit")
                }
                DropdownMenu(expanded = expandedFrom, onDismissRequest = { expandedFrom = false }) {
                    unitOptions.forEach { u ->
                        DropdownMenuItem(
                            text = { Text(u) },
                            onClick = {
                                fromUnit = u
                                expandedFrom = false
                            }
                        )
                    }
                }
            }

            // Swap Button (Fixing BUG-06)
            IconButton(
                onClick = {
                    val temp = fromUnit
                    fromUnit = toUnit
                    toUnit = temp
                },
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
            ) {
                Icon(
                    Icons.Default.SwapHoriz,
                    contentDescription = "Swap Units",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            // To Unit Dropdown
            Box(modifier = Modifier.weight(1f)) {
                OutlinedButton(
                    onClick = { expandedTo = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("To: $toUnit")
                }
                DropdownMenu(expanded = expandedTo, onDismissRequest = { expandedTo = false }) {
                    unitOptions.forEach { u ->
                        DropdownMenuItem(
                            text = { Text(u) },
                            onClick = {
                                toUnit = u
                                expandedTo = false
                            }
                        )
                    }
                }
            }
        }

        // Converted Result Display
        val inputNum = inputValue.toDoubleOrNull() ?: 0.0
        val outputNum = computeConversion(category, inputNum, fromUnit, toUnit)

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Converted Result",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "%.4f %s".format(outputNum, toUnit),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$inputNum $fromUnit = %.4f $toUnit".format(outputNum),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

private fun computeConversion(category: String, v: Double, from: String, to: String): Double {
    if (from == to) return v
    return when (category) {
        "Speed" -> {
            // Base: m/s
            val inMs = when (from) {
                "km/h" -> v / 3.6
                "mph" -> v * 0.44704
                "m/s" -> v
                "knots" -> v * 0.514444
                else -> v
            }
            when (to) {
                "km/h" -> inMs * 3.6
                "mph" -> inMs / 0.44704
                "m/s" -> inMs
                "knots" -> inMs / 0.514444
                else -> inMs
            }
        }
        "Length" -> {
            // Base: Meters
            val inM = when (from) {
                "Meters" -> v
                "Kilometers" -> v * 1000.0
                "Miles" -> v * 1609.34
                "Feet" -> v * 0.3048
                "Inches" -> v * 0.0254
                else -> v
            }
            when (to) {
                "Meters" -> inM
                "Kilometers" -> inM / 1000.0
                "Miles" -> inM / 1609.34
                "Feet" -> inM / 0.3048
                "Inches" -> inM / 0.0254
                else -> inM
            }
        }
        "Weight" -> {
            // Base: kg
            val inKg = when (from) {
                "Kilograms" -> v
                "Grams" -> v / 1000.0
                "Pounds" -> v * 0.453592
                "Ounces" -> v * 0.0283495
                else -> v
            }
            when (to) {
                "Kilograms" -> inKg
                "Grams" -> inKg * 1000.0
                "Pounds" -> inKg / 0.453592
                "Ounces" -> inKg / 0.0283495
                else -> inKg
            }
        }
        "Temperature" -> {
            // Base: Celsius
            val inC = when (from) {
                "Celsius" -> v
                "Fahrenheit" -> (v - 32.0) * 5.0 / 9.0
                "Kelvin" -> v - 273.15
                else -> v
            }
            when (to) {
                "Celsius" -> inC
                "Fahrenheit" -> (inC * 9.0 / 5.0) + 32.0
                "Kelvin" -> inC + 273.15
                else -> inC
            }
        }
        else -> v
    }
}

private fun evalQuick(expr: String): String {
    try {
        val sanitized = expr.replace("×", "*").replace("÷", "/")
        if (sanitized.isBlank()) return "0"
        // Simple 2-operand or single operand evaluator
        val ops = listOf("+", "-", "*", "/")
        for (op in ops) {
            val idx = sanitized.lastIndexOf(op)
            if (idx > 0 && idx < sanitized.length - 1) {
                val left = sanitized.substring(0, idx).toDoubleOrNull()
                val right = sanitized.substring(idx + 1).toDoubleOrNull()
                if (left != null && right != null) {
                    val ans = when (op) {
                        "+" -> left + right
                        "-" -> left - right
                        "*" -> left * right
                        "/" -> if (right != 0.0) left / right else Double.NaN
                        else -> 0.0
                    }
                    return if (ans.isNaN()) "Error" else if (ans % 1.0 == 0.0) ans.toLong().toString() else "%.4f".format(ans)
                }
            }
        }
        val single = sanitized.toDoubleOrNull()
        return if (single != null) {
            if (single % 1.0 == 0.0) single.toLong().toString() else "%.4f".format(single)
        } else {
            expr
        }
    } catch (_: Exception) {
        return "Error"
    }
}
