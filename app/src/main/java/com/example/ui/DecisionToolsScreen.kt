package com.example.ui

import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.remote.AiService
import com.google.firebase.ai.type.Schema
import kotlinx.coroutines.launch
import org.json.JSONObject
import kotlin.random.Random

private val WHEEL_COLORS = listOf(
    Color(0xFF6366F1),
    Color(0xFF10B981),
    Color(0xFFF59E0B),
    Color(0xFFEC4899),
    Color(0xFF3B82F6),
    Color(0xFF8B5CF6),
    Color(0xFFEF4444),
    Color(0xFF14B8A6)
)

private val SUGGEST_OPTIONS_SCHEMA = Schema.obj(
    properties = mapOf(
        "options" to Schema.array(
            items = Schema.string(description = "A concise short option label (1-4 words)"),
            description = "Between 4 and 6 fun, practical options"
        )
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DecisionToolsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableIntStateOf(0) }

    // Spin Wheel State
    val options = remember {
        mutableStateListOf("Pizza", "Sushi", "Tacos", "Burgers", "Salad", "Pasta")
    }
    var newOptionText by remember { mutableStateOf("") }
    var aiDilemmaText by remember { mutableStateOf("") }
    var isSuggesting by remember { mutableStateOf(false) }
    var isSpinning by remember { mutableStateOf(false) }
    var wheelResult by remember { mutableStateOf<String?>(null) }
    val wheelRotation = remember { Animatable(0f) }

    // Dice & Coin State
    var die1 by remember { mutableIntStateOf(6) }
    var die2 by remember { mutableIntStateOf(4) }
    var useTwoDice by remember { mutableStateOf(true) }
    var coinResult by remember { mutableStateOf("Heads") }
    var coinFlipCount by remember { mutableIntStateOf(0) }

    fun spinWheel() {
        if (options.size < 2 || isSpinning) return
        scope.launch {
            isSpinning = true
            wheelResult = null
            val extraSpins = 360f * (4 + Random.nextInt(3)) + Random.nextFloat() * 360f
            val target = wheelRotation.value + extraSpins
            wheelRotation.animateTo(
                targetValue = target,
                animationSpec = tween(durationMillis = 2200, easing = FastOutSlowInEasing)
            )
            // Pointer is at top (-90 deg / 270 deg). Determine which slice landed at 270 deg.
            val normalized = ((270f - (target % 360f)) % 360f + 360f) % 360f
            val sliceSweep = 360f / options.size.toFloat()
            val winnerIdx = (normalized / sliceSweep).toInt().coerceIn(0, options.lastIndex)
            wheelResult = options[winnerIdx]
            isSpinning = false
        }
    }

    fun suggestOptionsWithAi() {
        val query = aiDilemmaText.trim()
        if (query.isBlank()) {
            Toast.makeText(context, "Describe what you are deciding on", Toast.LENGTH_SHORT).show()
            return
        }
        isSuggesting = true
        scope.launch {
            AiService.generateStructuredJson(
                taskPrompt = "Suggest 6 short, fun, practical options for: \"$query\"",
                responseSchema = SUGGEST_OPTIONS_SCHEMA,
                systemInstruction = "Return 4 to 6 short concise options (1-4 words each).",
                modelId = AiService.FAST_GEMINI_MODEL,
                temperature = 0.7f
            ).onSuccess { raw ->
                try {
                    val arr = JSONObject(raw).optJSONArray("options")
                    val list = buildList {
                        if (arr != null) {
                            for (i in 0 until arr.length()) {
                                val v = arr.optString(i).trim()
                                if (v.isNotBlank()) add(v)
                            }
                        }
                    }
                    if (list.size >= 2) {
                        options.clear()
                        options.addAll(list.take(8))
                        wheelResult = null
                        aiDilemmaText = ""
                    }
                } catch (_: Exception) {
                    Toast.makeText(context, "Could not load suggestions", Toast.LENGTH_SHORT).show()
                }
                isSuggesting = false
            }.onFailure { err ->
                Toast.makeText(context, err.localizedMessage ?: "AI suggestion failed", Toast.LENGTH_SHORT).show()
                isSuggesting = false
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Spin & Decide", fontWeight = FontWeight.SemiBold) },
                    navigationIcon = {
                        IconButton(onClick = onBack, modifier = Modifier.testTag("decision_back")) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
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
                    text = { Text("Choice Wheel") },
                    modifier = Modifier.testTag("decision_tab_wheel")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Dice & Coin") },
                    modifier = Modifier.testTag("decision_tab_dice")
                )
            }

            if (selectedTab == 0) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Spinning Wheel Visual
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                val pointerColor = MaterialTheme.colorScheme.onSurface
                                Box(
                                    modifier = Modifier.size(220.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        val diameter = minOf(size.width, size.height) - 24f
                                        val topLeft = Offset(
                                            (size.width - diameter) / 2f,
                                            (size.height - diameter) / 2f + 10f
                                        )
                                        val sliceCount = options.size.coerceAtLeast(1)
                                        val sweep = 360f / sliceCount.toFloat()

                                        rotate(wheelRotation.value, pivot = Offset(size.width / 2f, size.height / 2f + 10f)) {
                                            for (i in 0 until sliceCount) {
                                                drawArc(
                                                    color = WHEEL_COLORS[i % WHEEL_COLORS.size],
                                                    startAngle = i * sweep,
                                                    sweepAngle = sweep,
                                                    useCenter = true,
                                                    topLeft = topLeft,
                                                    size = Size(diameter, diameter)
                                                )
                                            }
                                            drawCircle(
                                                color = Color.White,
                                                radius = 18f,
                                                center = Offset(size.width / 2f, size.height / 2f + 10f)
                                            )
                                        }

                                        // Top indicator triangle
                                        val cx = size.width / 2f
                                        val pointerPath = Path().apply {
                                            moveTo(cx - 14f, 0f)
                                            lineTo(cx + 14f, 0f)
                                            lineTo(cx, 24f)
                                            close()
                                        }
                                        drawPath(pointerPath, color = pointerColor)
                                    }
                                }

                                if (wheelResult != null) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier.testTag("decision_wheel_result")
                                    ) {
                                        Text(
                                            text = "Winner: $wheelResult",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                        )
                                    }
                                }

                                Button(
                                    onClick = { spinWheel() },
                                    enabled = options.size >= 2 && !isSpinning,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("decision_spin_button")
                                ) {
                                    Text(if (isSpinning) "Spinning…" else "Spin the Wheel")
                                }
                            }
                        }
                    }

                    // Quick presets
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AssistChip(
                                onClick = {
                                    options.clear()
                                    options.addAll(listOf("Pizza", "Sushi", "Tacos", "Burgers", "Thai", "Pasta"))
                                    wheelResult = null
                                },
                                label = { Text("What to Eat") }
                            )
                            AssistChip(
                                onClick = {
                                    options.clear()
                                    options.addAll(listOf("Movie Night", "Board Games", "Walk Outside", "Read a Book", "Cook Together"))
                                    wheelResult = null
                                },
                                label = { Text("Weekend Plan") }
                            )
                            AssistChip(
                                onClick = {
                                    options.clear()
                                    options.addAll(listOf("Yes", "No", "Wait & See", "Definitely!"))
                                    wheelResult = null
                                },
                                label = { Text("Yes / No") }
                            )
                        }
                    }

                    // Options Editor + AI Suggest
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
                                Text("Choices (${options.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    options.forEachIndexed { index, option ->
                                        val swatch = WHEEL_COLORS[index % WHEEL_COLORS.size]
                                        InputChip(
                                            selected = wheelResult == option,
                                            onClick = {
                                                if (options.size > 2) options.removeAt(index)
                                            },
                                            label = { Text(option) },
                                            leadingIcon = {
                                                Canvas(modifier = Modifier.size(10.dp)) {
                                                    drawCircle(color = swatch)
                                                }
                                            },
                                            trailingIcon = {
                                                if (options.size > 2) {
                                                    Icon(Icons.Default.Close, contentDescription = "Remove $option", modifier = Modifier.size(14.dp))
                                                }
                                            }
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = newOptionText,
                                        onValueChange = { newOptionText = it },
                                        placeholder = { Text("Add a choice…") },
                                        singleLine = true,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("decision_add_option_input")
                                    )
                                    IconButton(
                                        onClick = {
                                            val cleaned = newOptionText.trim()
                                            if (cleaned.isNotBlank()) {
                                                options.add(cleaned)
                                                newOptionText = ""
                                            }
                                        },
                                        modifier = Modifier.testTag("decision_add_option_button")
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Add option")
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = aiDilemmaText,
                                        onValueChange = { aiDilemmaText = it },
                                        placeholder = { Text("Ask AI for ideas (e.g. Friday night)…") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedButton(
                                        onClick = { suggestOptionsWithAi() },
                                        enabled = !isSuggesting && aiDilemmaText.isNotBlank(),
                                        modifier = Modifier.testTag("decision_ai_suggest_button")
                                    ) {
                                        if (isSuggesting) {
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                        } else {
                                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Fill")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            } else {
                // Dice & Coin Tab
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("Dice Roller", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(selected = !useTwoDice, onClick = { useTwoDice = false }, label = { Text("1 Die") })
                                FilterChip(selected = useTwoDice, onClick = { useTwoDice = true }, label = { Text("2 Dice") })
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                DieBox(value = die1)
                                if (useTwoDice) {
                                    DieBox(value = die2)
                                }
                            }
                            Text(
                                text = if (useTwoDice) "Total: ${die1 + die2}" else "Rolled: $die1",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Button(
                                onClick = {
                                    die1 = Random.nextInt(1, 7)
                                    die2 = Random.nextInt(1, 7)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("decision_roll_dice_button")
                            ) {
                                Icon(Icons.Default.Casino, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Roll Dice")
                            }
                        }
                    }

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("Coin Flip", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(84.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = coinResult,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                            if (coinFlipCount > 0) {
                                Text(
                                    "Flips this session: $coinFlipCount",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            OutlinedButton(
                                onClick = {
                                    coinResult = if (Random.nextBoolean()) "Heads" else "Tails"
                                    coinFlipCount++
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("decision_flip_coin_button")
                            ) {
                                Text("Flip Coin")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DieBox(value: Int) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.size(64.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
