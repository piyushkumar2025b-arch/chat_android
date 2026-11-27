package com.example.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SwapHoriz
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.FlashcardDeckEntity
import com.example.data.local.FlashcardEntity
import com.example.data.local.ProductivityDao
import com.example.data.local.ToolkitDao
import com.example.data.remote.AiService
import com.example.util.showMainToast
import com.google.firebase.ai.type.Schema
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

private val FLASHCARD_SCHEMA = Schema.obj(
    properties = mapOf(
        "deckTitle" to Schema.string(description = "Short friendly title for the flashcard deck"),
        "cards" to Schema.array(
            items = Schema.obj(
                properties = mapOf(
                    "question" to Schema.string(description = "Clear concise question or prompt on the front of the card"),
                    "answer" to Schema.string(description = "Clear accurate answer on the back of the card")
                )
            ),
            description = "List of generated flashcards"
        )
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashcardsScreen(
    toolkitDao: ToolkitDao,
    productivityDao: ProductivityDao,
    onBack: () -> Unit,
    onSendToChat: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val decks by toolkitDao.observeDecks().collectAsState(initial = emptyList())
    val allCards by toolkitDao.observeAllCards().collectAsState(initial = emptyList())
    val notes by productivityDao.getAllNotes().collectAsState(initial = emptyList())

    var activeDeckId by remember { mutableStateOf<String?>(null) }
    var isQuizMode by remember { mutableStateOf(false) }

    // AI Generator states
    var topicInput by remember { mutableStateOf("") }
    var cardCount by remember { mutableIntStateOf(6) }
    var isGenerating by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Manual dialogs
    var showCreateDeckDialog by remember { mutableStateOf(false) }
    var newDeckTitle by remember { mutableStateOf("") }
    var showAddCardDialog by remember { mutableStateOf(false) }
    var newQuestion by remember { mutableStateOf("") }
    var newAnswer by remember { mutableStateOf("") }

    // Study state
    var studyIndex by remember { mutableIntStateOf(0) }
    var isFlipped by remember { mutableStateOf(false) }
    var unmasteredOnly by remember { mutableStateOf(false) }
    var swapFaces by remember { mutableStateOf(false) }
    var shuffleSeed by remember { mutableIntStateOf(0) }

    // Quiz state
    var quizIndex by remember { mutableIntStateOf(0) }
    var quizScore by remember { mutableIntStateOf(0) }
    var selectedChoice by remember { mutableStateOf<String?>(null) }

    val activeDeck = decks.firstOrNull { it.id == activeDeckId }
    val deckCards = remember(allCards, activeDeckId) {
        allCards.filter { it.deckId == activeDeckId }
    }
    val studyCards = remember(deckCards, unmasteredOnly, shuffleSeed) {
        val base = if (unmasteredOnly) deckCards.filter { !it.mastered } else deckCards
        if (shuffleSeed == 0) base else base.shuffled(kotlin.random.Random(shuffleSeed))
    }

    BackHandler(enabled = activeDeck != null) {
        if (isQuizMode) {
            isQuizMode = false
        } else {
            activeDeckId = null
            isFlipped = false
            studyIndex = 0
        }
    }

    fun generateAiDeck(sourceTopic: String, sourceNoteContent: String = "") {
        val cleaned = sourceTopic.trim()
        if (cleaned.isBlank()) {
            Toast.makeText(context, "Enter a study topic first", Toast.LENGTH_SHORT).show()
            return
        }
        errorMessage = null
        isGenerating = true
        scope.launch {
            val prompt = buildString {
                append("Create $cardCount clear, helpful study flashcards about: \"$cleaned\".")
                if (sourceNoteContent.isNotBlank()) {
                    append("\nBase them on these notes:\n${sourceNoteContent.take(2500)}")
                }
                append("\nKeep each question and answer crisp and easy to study.")
            }
            AiService.generateStructuredJson(
                taskPrompt = prompt,
                responseSchema = FLASHCARD_SCHEMA,
                systemInstruction = "Create accurate, easy-to-remember flashcards with short clear questions and answers.",
                temperature = 0.4f
            ).onSuccess { rawJson ->
                try {
                    val json = JSONObject(rawJson)
                    val title = json.optString("deckTitle", cleaned).ifBlank { cleaned }
                    val arr = json.optJSONArray("cards")
                    val generatedCards = buildList {
                        if (arr != null) {
                            for (i in 0 until arr.length()) {
                                val obj = arr.optJSONObject(i) ?: continue
                                val q = obj.optString("question").trim()
                                val a = obj.optString("answer").trim()
                                if (q.isNotBlank() && a.isNotBlank()) {
                                    add(q to a)
                                }
                            }
                        }
                    }
                    if (generatedCards.isEmpty()) {
                        errorMessage = "Could not create cards for that topic. Try rephrasing."
                    } else {
                        val deck = FlashcardDeckEntity(title = title, topic = cleaned)
                        val entities = generatedCards.mapIndexed { idx, (q, a) ->
                            FlashcardEntity(
                                deckId = deck.id,
                                question = q,
                                answer = a,
                                createdAt = System.currentTimeMillis() + idx
                            )
                        }
                        withContext(Dispatchers.IO) {
                            toolkitDao.upsertDeck(deck)
                            toolkitDao.insertCards(entities)
                        }
                        topicInput = ""
                        studyIndex = 0
                        isFlipped = false
                        isQuizMode = false
                        activeDeckId = deck.id
                        showMainToast(context, "Created ${entities.size} flashcards")
                    }
                } catch (e: Exception) {
                    errorMessage = "Could not read generated flashcards: ${e.localizedMessage ?: "format error"}"
                }
                isGenerating = false
            }.onFailure { err ->
                errorMessage = err.localizedMessage ?: "Failed to generate flashcards."
                isGenerating = false
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = when {
                                activeDeck == null -> "Flashcards & Quiz"
                                isQuizMode -> "Quiz: ${activeDeck.title}"
                                else -> activeDeck.title
                            },
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                when {
                                    isQuizMode -> isQuizMode = false
                                    activeDeck != null -> {
                                        activeDeckId = null
                                        isFlipped = false
                                        studyIndex = 0
                                    }
                                    else -> onBack()
                                }
                            },
                            modifier = Modifier.testTag("flashcards_back")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        if (activeDeck != null && deckCards.size >= 2) {
                            IconButton(
                                onClick = {
                                    isQuizMode = !isQuizMode
                                    quizIndex = 0
                                    quizScore = 0
                                    selectedChoice = null
                                },
                                modifier = Modifier.testTag("flashcards_toggle_quiz")
                            ) {
                                Icon(
                                    if (isQuizMode) Icons.Default.Flip else Icons.Default.Quiz,
                                    contentDescription = if (isQuizMode) "Study Cards" else "Quick Quiz"
                                )
                            }
                        }
                        if (activeDeck != null) {
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        withContext(Dispatchers.IO) {
                                            toolkitDao.resetDeckMastery(activeDeck.id)
                                        }
                                        studyIndex = 0
                                        isFlipped = false
                                        showMainToast(context, "Deck progress reset")
                                    }
                                },
                                modifier = Modifier.testTag("flashcards_reset_progress")
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Reset progress")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        },
        floatingActionButton = {
            if (!isQuizMode) {
                FloatingActionButton(
                    onClick = {
                        if (activeDeck == null) {
                            newDeckTitle = ""
                            showCreateDeckDialog = true
                        } else {
                            newQuestion = ""
                            newAnswer = ""
                            showAddCardDialog = true
                        }
                    },
                    modifier = Modifier.testTag("flashcards_fab")
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = if (activeDeck == null) "New Deck" else "Add Card"
                    )
                }
            }
        }
    ) { innerPadding ->
        if (activeDeck == null) {
            // Deck List + AI Generator
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
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
                                Text("AI Deck Maker", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            }
                            OutlinedTextField(
                                value = topicInput,
                                onValueChange = { topicInput = it },
                                label = { Text("Topic or subject") },
                                placeholder = { Text("e.g. Spanish travel phrases, Cell biology…") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("flashcards_topic_input")
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                listOf(5, 8, 12).forEach { count ->
                                    FilterChip(
                                        selected = cardCount == count,
                                        onClick = { cardCount = count },
                                        label = { Text("$count cards") }
                                    )
                                }
                            }
                            Button(
                                onClick = { generateAiDeck(topicInput) },
                                enabled = !isGenerating && topicInput.isNotBlank(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("flashcards_generate_button")
                            ) {
                                if (isGenerating) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Creating cards…")
                                } else {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Generate Flashcards")
                                }
                            }

                            if (notes.isNotEmpty()) {
                                Text(
                                    "Or turn a saved note into flashcards:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    notes.take(5).forEach { note ->
                                        AssistChip(
                                            onClick = { generateAiDeck(note.title, note.content) },
                                            label = { Text(note.title, maxLines = 1) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (!errorMessage.isNullOrBlank()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.errorContainer
                        ) {
                            Text(
                                text = errorMessage.orEmpty(),
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }

                item {
                    Text(
                        text = "Your Decks (${decks.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                if (decks.isEmpty()) {
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
                                Icon(
                                    Icons.Default.School,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(36.dp)
                                )
                                Text("No decks yet", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "Create a deck above with AI or tap + to add cards yourself.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(decks, key = { it.id }) { deck ->
                        val cardsInDeck = allCards.filter { it.deckId == deck.id }
                        val masteredCount = cardsInDeck.count { it.mastered }
                        val progress = if (cardsInDeck.isEmpty()) 0f else masteredCount.toFloat() / cardsInDeck.size.toFloat()
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    activeDeckId = deck.id
                                    studyIndex = 0
                                    isFlipped = false
                                    isQuizMode = false
                                }
                                .testTag("flashcard_deck_${deck.id}")
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(deck.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                        Text(
                                            "${cardsInDeck.size} cards · $masteredCount mastered",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            scope.launch {
                                                withContext(Dispatchers.IO) {
                                                    toolkitDao.deleteDeckWithCards(deck.id)
                                                }
                                            }
                                        },
                                        modifier = Modifier.testTag("flashcard_delete_deck_${deck.id}")
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete deck")
                                    }
                                }
                                if (cardsInDeck.isNotEmpty()) {
                                    LinearProgressIndicator(
                                        progress = { progress },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(64.dp)) }
            }
        } else if (isQuizMode && deckCards.size >= 2) {
            // Multiple-choice quiz mode
            val safeIndex = quizIndex.coerceIn(0, deckCards.lastIndex)
            val currentCard = deckCards[safeIndex]
            val choices = remember(currentCard.id, deckCards.size) {
                val distractors = deckCards
                    .filter { it.id != currentCard.id }
                    .map { it.answer }
                    .distinct()
                    .shuffled()
                    .take(3)
                (listOf(currentCard.answer) + distractors).shuffled()
            }
            val isFinished = quizIndex >= deckCards.size

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (isFinished) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                            Text("Quiz Complete!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Text(
                                "You got $quizScore out of ${deckCards.size} right",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        quizIndex = 0
                                        quizScore = 0
                                        selectedChoice = null
                                    }
                                ) {
                                    Text("Try Again")
                                }
                                Button(onClick = { isQuizMode = false }) {
                                    Text("Back to Cards")
                                }
                            }
                        }
                    }
                } else {
                    LinearProgressIndicator(
                        progress = { (safeIndex + 1).toFloat() / deckCards.size.toFloat() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                    )
                    Text(
                        "Question ${safeIndex + 1} of ${deckCards.size} · Score: $quizScore",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = currentCard.question,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(18.dp)
                        )
                    }

                    choices.forEachIndexed { idx, choice ->
                        val answered = selectedChoice != null
                        val isCorrect = choice == currentCard.answer
                        val isChosen = choice == selectedChoice
                        val container = when {
                            !answered -> MaterialTheme.colorScheme.surface
                            isCorrect -> MaterialTheme.colorScheme.primaryContainer
                            isChosen -> MaterialTheme.colorScheme.errorContainer
                            else -> MaterialTheme.colorScheme.surface
                        }
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = container),
                            border = BorderStroke(
                                1.dp,
                                if (answered && isCorrect) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !answered) {
                                    selectedChoice = choice
                                    if (isCorrect) {
                                        quizScore++
                                    }
                                    scope.launch {
                                        withContext(Dispatchers.IO) {
                                            toolkitDao.updateCardMastery(currentCard.id, isCorrect)
                                        }
                                    }
                                }
                                .testTag("quiz_choice_$idx")
                        ) {
                            Text(
                                text = choice,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }

                    if (selectedChoice != null) {
                        Button(
                            onClick = {
                                selectedChoice = null
                                quizIndex++
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (safeIndex + 1 >= deckCards.size) "See Score" else "Next Question")
                        }
                    }
                }
            }
        } else {
            // Study Deck & Card List
            val masteredCount = deckCards.count { it.mastered }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (deckCards.isEmpty()) {
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
                                Text("This deck has no cards yet", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "Tap the + button below to add your first flashcard.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = unmasteredOnly,
                                onClick = {
                                    unmasteredOnly = !unmasteredOnly
                                    studyIndex = 0
                                    isFlipped = false
                                },
                                label = { Text("Need Practice") },
                                modifier = Modifier.testTag("flashcards_filter_practice")
                            )
                            FilterChip(
                                selected = swapFaces,
                                onClick = {
                                    swapFaces = !swapFaces
                                    isFlipped = false
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
                                },
                                label = { Text("Swap Q↔A") },
                                modifier = Modifier.testTag("flashcards_swap_qa")
                            )
                            AssistChip(
                                onClick = {
                                    shuffleSeed++
                                    studyIndex = 0
                                    isFlipped = false
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Shuffle, contentDescription = null, modifier = Modifier.size(16.dp))
                                },
                                label = { Text("Shuffle") },
                                modifier = Modifier.testTag("flashcards_shuffle")
                            )
                        }
                    }

                    if (studyCards.isEmpty()) {
                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Text(
                                        "All cards in this deck are mastered!",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        textAlign = TextAlign.Center
                                    )
                                    Button(onClick = { unmasteredOnly = false }) {
                                        Text("Show All Cards")
                                    }
                                }
                            }
                        }
                    } else {
                        val safeIndex = studyIndex.coerceIn(0, studyCards.lastIndex)
                        val activeCard = studyCards[safeIndex]

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Card ${safeIndex + 1} of ${studyCards.size}",
                                    style = MaterialTheme.typography.labelLarge
                                )
                                Text(
                                    "$masteredCount/${deckCards.size} mastered",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { masteredCount.toFloat() / deckCards.size.toFloat() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                            )
                        }

                        item {
                            val rotation by animateFloatAsState(
                                targetValue = if (isFlipped) 180f else 0f,
                                animationSpec = tween(durationMillis = 320),
                                label = "cardFlip"
                            )
                            val showingBack = rotation > 90f
                            val showingAnswer = if (swapFaces) !showingBack else showingBack
                            Card(
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (showingBack) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                    else MaterialTheme.colorScheme.surface
                                ),
                                border = BorderStroke(
                                    1.5.dp,
                                    if (showingBack) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp)
                                    .graphicsLayer {
                                        rotationY = rotation
                                        cameraDistance = 12f * density
                                    }
                                    .clickable { isFlipped = !isFlipped }
                                    .testTag("flashcard_flip_surface")
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(20.dp)
                                        .graphicsLayer {
                                            if (showingBack) rotationY = 180f
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text(
                                            text = if (showingAnswer) "ANSWER" else "QUESTION",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = if (showingAnswer) activeCard.answer else activeCard.question,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            textAlign = TextAlign.Center
                                        )
                                        Text(
                                            text = "Tap card to flip",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        scope.launch {
                                            withContext(Dispatchers.IO) {
                                                toolkitDao.updateCardMastery(activeCard.id, false)
                                            }
                                        }
                                        isFlipped = false
                                        studyIndex = (safeIndex + 1) % studyCards.size.coerceAtLeast(1)
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("flashcard_review_again_button")
                                ) {
                                    Text("Review Again")
                                }
                                Button(
                                    onClick = {
                                        scope.launch {
                                            withContext(Dispatchers.IO) {
                                                toolkitDao.updateCardMastery(activeCard.id, true)
                                            }
                                        }
                                        isFlipped = false
                                        studyIndex = (safeIndex + 1) % studyCards.size.coerceAtLeast(1)
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("flashcard_got_it_button")
                                ) {
                                    Text("Got It!")
                                }
                            }
                        }

                        item {
                            OutlinedButton(
                                onClick = {
                                    onSendToChat("Quiz me and explain this flashcard deeper:\nQ: ${activeCard.question}\nA: ${activeCard.answer}")
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Ask AI Tutor About This Card")
                            }
                        }
                    }

                    item {
                        Text(
                            text = "All Cards in Deck (${deckCards.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }

                    items(deckCards, key = { it.id }) { card ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(card.question, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                    Text(card.answer, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            withContext(Dispatchers.IO) {
                                                toolkitDao.deleteCardById(card.id)
                                            }
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete card")
                                }
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(64.dp)) }
            }
        }
    }

    if (showCreateDeckDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDeckDialog = false },
            title = { Text("New Flashcard Deck") },
            text = {
                OutlinedTextField(
                    value = newDeckTitle,
                    onValueChange = { newDeckTitle = it },
                    label = { Text("Deck title") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("flashcards_new_deck_input")
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val cleaned = newDeckTitle.trim()
                        if (cleaned.isNotBlank()) {
                            val deck = FlashcardDeckEntity(title = cleaned, topic = cleaned)
                            scope.launch {
                                withContext(Dispatchers.IO) {
                                    toolkitDao.upsertDeck(deck)
                                }
                                activeDeckId = deck.id
                            }
                            showCreateDeckDialog = false
                        }
                    },
                    modifier = Modifier.testTag("flashcards_save_deck_button")
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDeckDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showAddCardDialog && activeDeck != null) {
        AlertDialog(
            onDismissRequest = { showAddCardDialog = false },
            title = { Text("Add Card") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newQuestion,
                        onValueChange = { newQuestion = it },
                        label = { Text("Question (Front)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("flashcards_new_question_input")
                    )
                    OutlinedTextField(
                        value = newAnswer,
                        onValueChange = { newAnswer = it },
                        label = { Text("Answer (Back)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("flashcards_new_answer_input")
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val q = newQuestion.trim()
                        val a = newAnswer.trim()
                        if (q.isNotBlank() && a.isNotBlank()) {
                            scope.launch {
                                withContext(Dispatchers.IO) {
                                    toolkitDao.upsertCard(
                                        FlashcardEntity(
                                            deckId = activeDeck.id,
                                            question = q,
                                            answer = a
                                        )
                                    )
                                }
                            }
                            showAddCardDialog = false
                        }
                    },
                    modifier = Modifier.testTag("flashcards_save_card_button")
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCardDialog = false }) { Text("Cancel") }
            }
        )
    }
}
