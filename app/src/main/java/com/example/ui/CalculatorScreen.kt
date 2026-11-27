package com.example.ui

import java.util.Locale
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Transform
import androidx.compose.material3.AssistChip
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
import androidx.compose.ui.text.input.KeyboardType
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
import androidx.compose.ui.text.style.TextOverflow
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
                title = { Text("Calculator & Units", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("calc_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Calculator", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    icon = { Icon(Icons.Default.Calculate, contentDescription = null) },
                    modifier = Modifier.testTag("calc_tab_standard")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Units", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    icon = { Icon(Icons.Default.Transform, contentDescription = null) },
                    modifier = Modifier.testTag("calc_tab_units")
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Tip & Split", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    icon = { Icon(Icons.Default.Receipt, contentDescription = null) },
                    modifier = Modifier.testTag("calc_tab_tip")
                )
            }

            when (selectedTab) {
                0 -> StandardCalculatorView(onSendResultToChat = onSendResultToChat)
                1 -> UnitConverterView()
                else -> TipAndSplitView(onSendResultToChat = onSendResultToChat)
            }
        }
    }
}

@Composable
private fun StandardCalculatorView(onSendResultToChat: (String) -> Unit) {
    var expression by remember { mutableStateOf("") }
    var resultText by remember { mutableStateOf("0") }
    var historyTape by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 520.dp)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Display Screen
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = expression.ifEmpty { "0" },
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = resultText,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                )
            }
        }

        if (historyTape.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                historyTape.forEachIndexed { idx, (expr, res) ->
                    AssistChip(
                        onClick = {
                            expression = res
                            resultText = res
                        },
                        label = { Text("$expr = $res", maxLines = 1) },
                        modifier = Modifier.testTag("calc_history_chip_$idx")
                    )
                }
            }
        }

        // Action: Send to chat
        if (resultText != "0" && resultText != "Error" && resultText != "Cannot divide by 0") {
            OutlinedButton(
                onClick = { onSendResultToChat("Calculator Result: $expression = $resultText") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Insert Result into AI Chat", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }

        // Keypad Grid - responsive 5x4 layout so all 20 keys fit without scrolling
        val rows = listOf(
            listOf("C", "(", ")", "⌫"),
            listOf("7", "8", "9", "÷"),
            listOf("4", "5", "6", "×"),
            listOf("1", "2", "3", "-"),
            listOf("0", ".", "=", "+")
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            rows.forEach { rowButtons ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    rowButtons.forEach { btn ->
                        val isOp = btn in listOf("÷", "×", "-", "+")
                        val isSpecial = btn in listOf("C", "⌫", "(", ")")
                        val bgColor = when {
                            btn == "=" -> MaterialTheme.colorScheme.primary
                            isOp -> MaterialTheme.colorScheme.surfaceContainerHigh
                            isSpecial -> MaterialTheme.colorScheme.surfaceContainerLow
                            else -> MaterialTheme.colorScheme.surface
                        }
                        val textColor = when {
                            btn == "=" -> MaterialTheme.colorScheme.onPrimary
                            isOp -> MaterialTheme.colorScheme.primary
                            btn == "C" -> MaterialTheme.colorScheme.error
                            isSpecial -> MaterialTheme.colorScheme.onSurfaceVariant
                            else -> MaterialTheme.colorScheme.onSurface
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .heightIn(max = 64.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .background(bgColor)
                                .then(
                                    if (btn != "=") Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(22.dp))
                                    else Modifier
                                )
                                .clickable {
                                    when (btn) {
                                        "C" -> {
                                            expression = ""
                                            resultText = "0"
                                        }
                                        "⌫" -> {
                                            if (expression.isNotEmpty()) {
                                                expression = expression.dropLast(1)
                                                resultText = if (expression.isEmpty()) "0" else evalQuick(expression)
                                            }
                                        }
                                        "=" -> {
                                            val evaluated = evalQuick(expression)
                                            resultText = evaluated
                                            if (expression.isNotBlank() && evaluated != "Error" && evaluated != "Cannot divide by 0" && evaluated != expression) {
                                                val entry = expression to evaluated
                                                if (historyTape.firstOrNull() != entry) {
                                                    historyTape = (listOf(entry) + historyTape).take(10)
                                                }
                                            }
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
                                    modifier = Modifier.size(22.dp)
                                )
                            } else {
                                Text(
                                    text = btn,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = textColor
                                )
                            }
                        }
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
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Category Chips (horizontally scrollable so Temperature never wraps/clips)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            categories.forEach { cat ->
                FilterChip(
                    selected = category == cat,
                    onClick = {
                        category = cat
                    },
                    label = { Text(cat, maxLines = 1) }
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
                    Text(
                        text = "From: $fromUnit",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
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

            // Swap Button
            IconButton(
                onClick = {
                    val temp = fromUnit
                    fromUnit = toUnit
                    toUnit = temp
                },
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
            ) {
                Icon(
                    Icons.Default.SwapHoriz,
                    contentDescription = "Swap Units",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            // To Unit Dropdown
            Box(modifier = Modifier.weight(1f)) {
                OutlinedButton(
                    onClick = { expandedTo = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "To: $toUnit",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
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
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Result",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "%.4f %s".format(outputNum, toUnit),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$inputNum $fromUnit = %.4f $toUnit".format(outputNum),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TipAndSplitView(onSendResultToChat: (String) -> Unit) {
    var billInput by remember { mutableStateOf("100") }
    var tipPercent by remember { mutableIntStateOf(15) }
    var splitCount by remember { mutableIntStateOf(2) }

    val bill = (billInput.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)
    val tipAmount = bill * (tipPercent / 100.0)
    val totalAmount = bill + tipAmount
    val perPerson = totalAmount / splitCount.coerceAtLeast(1)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedTextField(
            value = billInput,
            onValueChange = { billInput = it },
            label = { Text("Bill Amount ($)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("tip_bill_input")
        )

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Tip Percentage", style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(10, 15, 18, 20, 25).forEach { pct ->
                    FilterChip(
                        selected = tipPercent == pct,
                        onClick = { tipPercent = pct },
                        label = { Text("$pct%") },
                        modifier = Modifier.testTag("tip_percent_$pct")
                    )
                }
            }
        }

        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Split Between", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = { if (splitCount > 1) splitCount-- },
                        enabled = splitCount > 1,
                        modifier = Modifier.testTag("tip_split_minus")
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Fewer people")
                    }
                    Text(
                        text = "$splitCount ${if (splitCount == 1) "person" else "people"}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = { if (splitCount < 20) splitCount++ },
                        enabled = splitCount < 20,
                        modifier = Modifier.testTag("tip_split_plus")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "More people")
                    }
                }
            }
        }

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("tip_summary_card")
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Tip ($tipPercent%)", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("$%.2f".format(Locale.US, tipAmount), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total Bill", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("$%.2f".format(Locale.US, totalAmount), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Per Person", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "$%.2f".format(Locale.US, perPerson),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.testTag("tip_per_person_value")
                    )
                }
            }
        }

        OutlinedButton(
            onClick = {
                val summary = "Bill Split Summary: $%.2f bill + %d%% tip ($%.2f) = $%.2f total (%d people -> $%.2f each)".format(
                    Locale.US, bill, tipPercent, tipAmount, totalAmount, splitCount, perPerson
                )
                onSendResultToChat(summary)
            },
            enabled = bill > 0.0,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("tip_send_to_chat")
        ) {
            Text("Send Split Summary to AI Chat", maxLines = 1, overflow = TextOverflow.Ellipsis)
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

fun evaluateMathExpression(expr: String, isRad: Boolean = true): String {
    try {
        val sanitized = expr
            .replace("×", "*")
            .replace("÷", "/")
            .replace("π", Math.PI.toString())
            .trim()

        if (sanitized.isBlank()) return "0"

        class Parser(val s: String) {
            var pos = 0
            val len = s.length

            fun peek(): Char {
                skipWhitespace()
                return if (pos < len) s[pos] else '\u0000'
            }

            fun nextChar(): Char {
                skipWhitespace()
                return if (pos < len) s[pos++] else '\u0000'
            }

            fun skipWhitespace() {
                while (pos < len && s[pos].isWhitespace()) pos++
            }

            fun parse(): Double {
                val res = parseExpression()
                skipWhitespace()
                if (pos < len) throw IllegalArgumentException("Unexpected: ${s[pos]}")
                return res
            }

            fun parseExpression(): Double {
                var v = parseTerm()
                while (true) {
                    val c = peek()
                    if (c == '+' || c == '-') {
                        nextChar()
                        val right = parseTerm()
                        v = if (c == '+') v + right else v - right
                    } else {
                        break
                    }
                }
                return v
            }

            fun parseTerm(): Double {
                var v = parsePower()
                while (true) {
                    val c = peek()
                    if (c == '*' || c == '/') {
                        nextChar()
                        val right = parsePower()
                        if (c == '/') {
                            if (right == 0.0) throw ArithmeticException("Cannot divide by 0")
                            v /= right
                        } else {
                            v *= right
                        }
                    } else {
                        break
                    }
                }
                return v
            }

            fun parsePower(): Double {
                val left = parseFactor()
                if (peek() == '^') {
                    nextChar()
                    val right = parsePower()
                    return Math.pow(left, right)
                }
                return left
            }

            fun parseFactor(): Double {
                val c = peek()
                if (c == '+') {
                    nextChar()
                    return parseFactor()
                }
                if (c == '-') {
                    nextChar()
                    return -parseFactor()
                }
                if (c == '(') {
                    nextChar()
                    val v = parseExpression()
                    if (peek() == ')') nextChar() else throw IllegalArgumentException("Missing )")
                    return v
                }
                if (c.isLetter()) {
                    val start = pos
                    while (pos < len && s[pos].isLetter()) pos++
                    val fn = s.substring(start, pos).lowercase()
                    if (peek() != '(') throw IllegalArgumentException("Expected ( after $fn")
                    nextChar()
                    val arg = parseExpression()
                    if (peek() == ')') nextChar() else throw IllegalArgumentException("Missing )")
                    return when (fn) {
                        "sqrt" -> {
                            if (arg < 0) throw ArithmeticException("Domain error")
                            Math.sqrt(arg)
                        }
                        "sin" -> {
                            val rad = if (isRad) arg else Math.toRadians(arg)
                            val v = Math.sin(rad)
                            if (Math.abs(v - Math.round(v)) < 1e-10) Math.round(v).toDouble() else v
                        }
                        "cos" -> {
                            val rad = if (isRad) arg else Math.toRadians(arg)
                            val v = Math.cos(rad)
                            if (Math.abs(v - Math.round(v)) < 1e-10) Math.round(v).toDouble() else v
                        }
                        "tan" -> {
                            val rad = if (isRad) arg else Math.toRadians(arg)
                            Math.tan(rad)
                        }
                        "log", "log10" -> Math.log10(arg)
                        "ln" -> Math.log(arg)
                        "abs" -> Math.abs(arg)
                        else -> throw IllegalArgumentException("Unknown func: $fn")
                    }
                }

                val start = pos
                while (pos < len && (s[pos].isDigit() || s[pos] == '.')) pos++
                if (start == pos) throw IllegalArgumentException("Expected number at $pos")
                return s.substring(start, pos).toDouble()
            }
        }

        val result = Parser(sanitized).parse()
        if (result.isInfinite() || result.isNaN()) {
            return "Cannot divide by 0"
        }
        val rounded = Math.round(result * 1000000.0) / 1000000.0
        return if (rounded % 1.0 == 0.0) {
            rounded.toLong().toString()
        } else {
            "%.6f".format(Locale.US, rounded).trimEnd('0').trimEnd('.')
        }
    } catch (e: ArithmeticException) {
        return if (e.message?.contains("divide by 0", ignoreCase = true) == true) {
            "Cannot divide by 0"
        } else {
            "Error"
        }
    } catch (_: Exception) {
        return "Error"
    }
}

private fun evalQuick(expr: String): String {
    val res = evaluateMathExpression(expr, isRad = true)
    return if (res == "Error") expr else res
}
