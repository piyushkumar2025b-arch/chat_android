package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.DecimalFormat
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

data class CalculationHistory(
    val expression: String,
    val result: String,
    val timestamp: Long = System.currentTimeMillis()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(
    onBack: (() -> Unit)? = null,
    onSendToChat: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // 0 = Calculator, 1 = Unit Converter, 2 = History Log
    var selectedScreenTab by remember { mutableIntStateOf(0) }
    var isScientificMode by remember { mutableStateOf(false) }
    var isRadMode by remember { mutableStateOf(true) }

    var expression by remember { mutableStateOf("") }
    val history = remember { mutableStateListOf<CalculationHistory>() }

    // Live preview of expression calculation
    val liveResult by remember(expression) {
        derivedStateOf {
            if (expression.isBlank()) ""
            else evaluateMathExpression(expression, isRadMode)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
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
                            imageVector = Icons.Default.Calculate,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Smart Calculator",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isScientificMode) "Scientific Mode (${if (isRadMode) "RAD" else "DEG"})" else "Standard Arithmetic",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (selectedScreenTab == 0) {
                        FilterChip(
                            selected = isScientificMode,
                            onClick = { isScientificMode = !isScientificMode },
                            label = { Text("Sci", fontSize = 12.sp) },
                            leadingIcon = { Icon(Icons.Default.Science, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        )
                    }
                }
            }
        }

        // Top Navigation Tabs: Calculator, Unit Converter, History
        TabRow(
            selectedTabIndex = selectedScreenTab,
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = selectedScreenTab == 0,
                onClick = { selectedScreenTab = 0 },
                text = { Text("Calculator") },
                icon = { Icon(Icons.Default.Calculate, contentDescription = null) }
            )
            Tab(
                selected = selectedScreenTab == 1,
                onClick = { selectedScreenTab = 1 },
                text = { Text("Converter") },
                icon = { Icon(Icons.Default.SwapHoriz, contentDescription = null) }
            )
            Tab(
                selected = selectedScreenTab == 2,
                onClick = { selectedScreenTab = 2 },
                text = { Text("History (${history.size})") },
                icon = { Icon(Icons.Default.History, contentDescription = null) }
            )
        }

        when (selectedScreenTab) {
            0 -> {
                // Calculator Display & Keypad
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // Display Area Card
                    ElevatedCard(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.7f)
                            .padding(bottom = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Top Row: Copy & AI Solver
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (isScientificMode) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            modifier = Modifier.clickable { isRadMode = !isRadMode }
                                        ) {
                                            Text(
                                                text = if (isRadMode) "RAD" else "DEG",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    if (liveResult.isNotBlank() && !liveResult.startsWith("Error")) {
                                        IconButton(
                                            onClick = {
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                val clip = ClipData.newPlainText("Result", liveResult)
                                                clipboard.setPrimaryClip(clip)
                                                Toast.makeText(context, "Copied $liveResult", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy Result", modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }

                                if (expression.isNotBlank() && onSendToChat != null) {
                                    TextButton(
                                        onClick = {
                                            onSendToChat("Please solve and explain this math problem step-by-step: $expression")
                                        },
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("AI Step-by-Step", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }

                            // Math Expression
                            Text(
                                text = if (expression.isEmpty()) "0" else expression,
                                style = MaterialTheme.typography.headlineMedium,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Normal,
                                textAlign = TextAlign.End,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("calc_expression_text")
                            )

                            // Live Result
                            Text(
                                text = if (liveResult.isBlank()) "" else "= $liveResult",
                                style = MaterialTheme.typography.headlineLarge,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = if (liveResult.startsWith("Error")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.End,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("calc_result_text")
                            )
                        }
                    }

                    // Keypad Grid
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1.3f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Scientific Keys if toggled
                        if (isScientificMode) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                SciButton(text = "sin", onClick = { expression += "sin(" }, modifier = Modifier.weight(1f))
                                SciButton(text = "cos", onClick = { expression += "cos(" }, modifier = Modifier.weight(1f))
                                SciButton(text = "tan", onClick = { expression += "tan(" }, modifier = Modifier.weight(1f))
                                SciButton(text = "ln", onClick = { expression += "ln(" }, modifier = Modifier.weight(1f))
                                SciButton(text = "log", onClick = { expression += "log(" }, modifier = Modifier.weight(1f))
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                SciButton(text = "√", onClick = { expression += "sqrt(" }, modifier = Modifier.weight(1f))
                                SciButton(text = "^", onClick = { expression += "^" }, modifier = Modifier.weight(1f))
                                SciButton(text = "π", onClick = { expression += "π" }, modifier = Modifier.weight(1f))
                                SciButton(text = "e", onClick = { expression += "e" }, modifier = Modifier.weight(1f))
                                SciButton(text = "%", onClick = { expression += "%" }, modifier = Modifier.weight(1f))
                            }
                        }

                        // Row 1: AC, ( , ) , ÷
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            CalcButton(
                                text = "AC",
                                isAction = true,
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                                onClick = { expression = "" },
                                modifier = Modifier.weight(1f)
                            )
                            CalcButton(
                                text = "(",
                                isAction = true,
                                onClick = { expression += "(" },
                                modifier = Modifier.weight(1f)
                            )
                            CalcButton(
                                text = ")",
                                isAction = true,
                                onClick = { expression += ")" },
                                modifier = Modifier.weight(1f)
                            )
                            CalcButton(
                                text = "÷",
                                isOperator = true,
                                onClick = { expression += " ÷ " },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Row 2: 7, 8, 9, ×
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            CalcButton(text = "7", onClick = { expression += "7" }, modifier = Modifier.weight(1f))
                            CalcButton(text = "8", onClick = { expression += "8" }, modifier = Modifier.weight(1f))
                            CalcButton(text = "9", onClick = { expression += "9" }, modifier = Modifier.weight(1f))
                            CalcButton(
                                text = "×",
                                isOperator = true,
                                onClick = { expression += " × " },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Row 3: 4, 5, 6, -
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            CalcButton(text = "4", onClick = { expression += "4" }, modifier = Modifier.weight(1f))
                            CalcButton(text = "5", onClick = { expression += "5" }, modifier = Modifier.weight(1f))
                            CalcButton(text = "6", onClick = { expression += "6" }, modifier = Modifier.weight(1f))
                            CalcButton(
                                text = "−",
                                isOperator = true,
                                onClick = { expression += " − " },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Row 4: 1, 2, 3, +
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            CalcButton(text = "1", onClick = { expression += "1" }, modifier = Modifier.weight(1f))
                            CalcButton(text = "2", onClick = { expression += "2" }, modifier = Modifier.weight(1f))
                            CalcButton(text = "3", onClick = { expression += "3" }, modifier = Modifier.weight(1f))
                            CalcButton(
                                text = "+",
                                isOperator = true,
                                onClick = { expression += " + " },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Row 5: 0, . , ⌫ , =
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            CalcButton(text = "0", onClick = { expression += "0" }, modifier = Modifier.weight(1f))
                            CalcButton(text = ".", onClick = { expression += "." }, modifier = Modifier.weight(1f))
                            CalcButton(
                                text = "⌫",
                                isAction = true,
                                onClick = {
                                    if (expression.isNotEmpty()) {
                                        expression = if (expression.endsWith(" ")) {
                                            expression.dropLast(3)
                                        } else {
                                            expression.dropLast(1)
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                            CalcButton(
                                text = "=",
                                isEquals = true,
                                onClick = {
                                    if (expression.isNotBlank() && liveResult.isNotBlank() && !liveResult.startsWith("Error")) {
                                        history.add(0, CalculationHistory(expression, liveResult))
                                        expression = liveResult
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            1 -> {
                // Unit Converter Sub-screen
                UnitConverterTab()
            }

            2 -> {
                // History Tape Sub-screen
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Calculation Tape", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        if (history.isNotEmpty()) {
                            TextButton(onClick = { history.clear() }) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Clear")
                            }
                        }
                    }

                    if (history.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No calculation history yet.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(history) { item ->
                                ElevatedCard(
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            expression = item.expression
                                            selectedScreenTab = 0
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.expression,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontFamily = FontFamily.Monospace,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = "= ${item.result}",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                val clip = ClipData.newPlainText("Result", item.result)
                                                clipboard.setPrimaryClip(clip)
                                                Toast.makeText(context, "Copied ${item.result}", Toast.LENGTH_SHORT).show()
                                            }
                                        ) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
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
}

@Composable
private fun CalcButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isOperator: Boolean = false,
    isAction: Boolean = false,
    isEquals: Boolean = false,
    containerColor: Color? = null,
    contentColor: Color? = null
) {
    val defaultContainer = when {
        isEquals -> MaterialTheme.colorScheme.primary
        isOperator -> MaterialTheme.colorScheme.secondaryContainer
        isAction -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.surface
    }

    val defaultContent = when {
        isEquals -> MaterialTheme.colorScheme.onPrimary
        isOperator -> MaterialTheme.colorScheme.onSecondaryContainer
        isAction -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onSurface
    }

    Button(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor ?: defaultContainer,
            contentColor = contentColor ?: defaultContent
        ),
        modifier = modifier
            .height(56.dp)
            .testTag("calc_btn_$text")
    ) {
        Text(
            text = text,
            fontSize = if (text.length > 2) 16.sp else 22.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun SciButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilledTonalButton(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f),
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
        ),
        modifier = modifier.height(38.dp)
    ) {
        Text(text = text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnitConverterTab() {
    var category by remember { mutableStateOf("Length") }
    var inputValue by remember { mutableStateOf("1") }
    val categories = listOf("Length", "Weight", "Temperature", "Speed")

    var fromUnit by remember { mutableStateOf("Meters") }
    var toUnit by remember { mutableStateOf("Feet") }

    val convertedValue = remember(category, inputValue, fromUnit, toUnit) {
        convertUnits(inputValue, category, fromUnit, toUnit)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(categories) { cat ->
                FilterChip(
                    selected = category == cat,
                    onClick = {
                        category = cat
                        when (cat) {
                            "Length" -> { fromUnit = "Meters"; toUnit = "Feet" }
                            "Weight" -> { fromUnit = "Kilograms"; toUnit = "Pounds" }
                            "Temperature" -> { fromUnit = "Celsius"; toUnit = "Fahrenheit" }
                            "Speed" -> { fromUnit = "km/h"; toUnit = "mph" }
                        }
                    },
                    label = { Text(cat) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = inputValue,
            onValueChange = { inputValue = it },
            label = { Text("Value to Convert") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        ElevatedCard(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "From: $fromUnit", style = MaterialTheme.typography.titleSmall)
                Text(
                    text = "$inputValue $fromUnit",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                Text(text = "Equals: $toUnit", style = MaterialTheme.typography.titleSmall)
                Text(
                    text = "$convertedValue $toUnit",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

private fun convertUnits(inputStr: String, category: String, from: String, to: String): String {
    val value = inputStr.toDoubleOrNull() ?: return "0"
    val df = DecimalFormat("#.####")
    return when (category) {
        "Length" -> {
            val meters = when (from) {
                "Meters" -> value
                "Feet" -> value * 0.3048
                "Kilometers" -> value * 1000.0
                "Miles" -> value * 1609.34
                else -> value
            }
            val result = when (to) {
                "Meters" -> meters
                "Feet" -> meters / 0.3048
                "Kilometers" -> meters / 1000.0
                "Miles" -> meters / 1609.34
                else -> meters
            }
            df.format(result)
        }
        "Weight" -> {
            val kg = when (from) {
                "Kilograms" -> value
                "Pounds" -> value * 0.453592
                else -> value
            }
            val result = when (to) {
                "Kilograms" -> kg
                "Pounds" -> kg / 0.453592
                else -> kg
            }
            df.format(result)
        }
        "Temperature" -> {
            val celsius = when (from) {
                "Celsius" -> value
                "Fahrenheit" -> (value - 32.0) * (5.0 / 9.0)
                else -> value
            }
            val result = when (to) {
                "Celsius" -> celsius
                "Fahrenheit" -> (celsius * (9.0 / 5.0)) + 32.0
                else -> celsius
            }
            df.format(result)
        }
        "Speed" -> {
            val kmh = if (from == "km/h") value else value * 1.60934
            val result = if (to == "km/h") kmh else kmh / 1.60934
            df.format(result)
        }
        else -> df.format(value)
    }
}

private fun evaluateMathExpression(rawExpr: String, isRad: Boolean): String {
    try {
        var clean = rawExpr
            .replace("×", "*")
            .replace("÷", "/")
            .replace("−", "-")
            .replace("π", Math.PI.toString())
            .replace("e", Math.E.toString())

        // Handle functions like sin, cos, tan, ln, log, sqrt
        clean = evaluateFunctions(clean, isRad)

        val result = simpleEvaluate(clean)
        val df = DecimalFormat("#.########")
        return df.format(result)
    } catch (_: Exception) {
        return "Error"
    }
}

private fun evaluateFunctions(expr: String, isRad: Boolean): String {
    var s = expr
    val regex = Regex("(sin|cos|tan|ln|log|sqrt)\\(([^()]+)\\)")
    var match = regex.find(s)
    while (match != null) {
        val func = match.groupValues[1]
        val arg = simpleEvaluate(match.groupValues[2])
        val evaluated = when (func) {
            "sin" -> sin(if (isRad) arg else Math.toRadians(arg))
            "cos" -> cos(if (isRad) arg else Math.toRadians(arg))
            "tan" -> tan(if (isRad) arg else Math.toRadians(arg))
            "ln" -> ln(arg)
            "log" -> log10(arg)
            "sqrt" -> sqrt(arg)
            else -> arg
        }
        s = s.substring(0, match.range.first) + evaluated.toString() + s.substring(match.range.last + 1)
        match = regex.find(s)
    }
    return s
}

private fun simpleEvaluate(expr: String): Double {
    val tokens = expr.replace(" ", "")
    var cur = 0.0
    var pendingOp = '+'
    var i = 0
    val numBuffer = StringBuilder()

    fun applyOp(op: Char, v: Double) {
        when (op) {
            '+' -> cur += v
            '-' -> cur -= v
            '*' -> cur *= v
            '/' -> cur = if (v != 0.0) cur / v else 0.0
            '^' -> cur = Math.pow(cur, v)
            '%' -> cur = cur * (v / 100.0)
        }
    }

    while (i < tokens.length) {
        val c = tokens[i]
        if (c.isDigit() || c == '.' || (c == '-' && numBuffer.isEmpty() && cur == 0.0)) {
            numBuffer.append(c)
        } else if (c in listOf('+', '-', '*', '/', '^', '%')) {
            if (numBuffer.isNotEmpty()) {
                val num = numBuffer.toString().toDoubleOrNull() ?: 0.0
                numBuffer.clear()
                applyOp(pendingOp, num)
            }
            pendingOp = c
        }
        i++
    }

    if (numBuffer.isNotEmpty()) {
        val num = numBuffer.toString().toDoubleOrNull() ?: 0.0
        applyOp(pendingOp, num)
    }

    return cur
}
