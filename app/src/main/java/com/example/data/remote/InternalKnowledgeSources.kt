package com.example.data.remote

/**
 * High-quality built-in internal knowledge packs that power OmniChat's offline RAG engine.
 * These comprehensive reference documents provide out-of-the-box domain grounding across
 * software architecture, artificial intelligence, security, mathematics, algorithms,
 * music acoustics, markdown formatting, global history, and productivity science.
 */
data class InternalKnowledgeSourceItem(
    val id: String,
    val title: String,
    val fileName: String,
    val category: String,
    val iconEmoji: String,
    val summary: String,
    val content: String
)

object InternalKnowledgeSources {

    val sources: List<InternalKnowledgeSourceItem> = listOf(
        InternalKnowledgeSourceItem(
            id = "omnichat_arch",
            title = "OmniChat AI Core Architecture & Capabilities",
            fileName = "OmniChat AI Core Architecture & Capabilities.md",
            category = "Architecture",
            iconEmoji = "🤖",
            summary = "Multi-provider LLM routing, local RAG vector engine, scoped storage, and media synthesis pipelines.",
            content = """
# OmniChat AI Architecture & Operational Guide
OmniChat is an all-in-one artificial intelligence workbench and personal productivity operating suite featuring:
1. Multi-Provider AI Routing: Supports Gemini 2.5/3.5, Groq LLaMA-3.3, OpenRouter Claude/DeepSeek, Cerebras 8000+ tokens/sec, and Pollinations free tier with automatic latency fallback.
2. Local RAG Engine: Offline and cloud-powered Retrieval-Augmented Generation utilizing vector embeddings, semantic sentence chunking (500 char windows with 100 char overlap), and reciprocal rank fusion.
3. Universal Scoped Storage: Direct file generation, code downloads, camera/audio persistence to native Android MediaStore directories (Pictures/OmniChat, Movies/OmniChat, Music/OmniChat, Download/OmniChat).
4. Media Synthesizers: Integrated text-to-speech (TTS), canvas waveform audio synthesis, image editing filters, and AI video motion generators.
5. Live Internet Grounding: Multi-source web search with real publisher citations via Google News RSS, DuckDuckGo, Wikipedia, arXiv, and PubMed search APIs.
6. Offline Productivity Tools: Integrated Password Vault with AES security, categorized Notes Hub, Dual-Mode Scientific Calculator, and Calendar Scheduler.
            """.trimIndent()
        ),

        InternalKnowledgeSourceItem(
            id = "android_compose",
            title = "Android Jetpack Compose & Clean Architecture Specs",
            fileName = "Android Jetpack Compose & Clean Architecture Specs.txt",
            category = "Mobile Dev",
            iconEmoji = "📱",
            summary = "Edge-to-edge WindowInsets, MVVM pattern, Room KSP persistence, and Material 3 design tokens.",
            content = """
Android Jetpack Compose Guidelines for OmniChat:
- Edge-to-edge layout with full WindowInsets handling and Scaffold components to avoid navigation bar clipping.
- State management through AndroidViewModel and StateFlow with StateFlow.collectAsState() lifecycle binding.
- Clean database layer utilizing Room Database 2.6+ with Room KSP code generation and automated destructive migration.
- Zero-permission MediaStore API integration for Scoped Storage compliance on Android 10+ (API 29+) with legacy storage fallback for Android 9.
- Material 3 design tokens: ColorScheme dynamic theming, AssistChips, FilterChips, ModalBottomSheet, and animated transitions.
- Asynchronous concurrency powered by Kotlin Coroutines, Dispatchers.IO for background network/disk I/O, and Dispatchers.Main for UI mutations.
- Touch target compliance: All interactive elements maintain a minimum size of 48.dp x 48.dp with accessibility contentDescription tags.
            """.trimIndent()
        ),

        InternalKnowledgeSourceItem(
            id = "rag_math",
            title = "RAG & Semantic Retrieval Mathematics",
            fileName = "RAG & Semantic Retrieval Mathematics.md",
            category = "Machine Learning",
            iconEmoji = "📐",
            summary = "Cosine similarity calculations, 768-d embeddings, BM25 lexical scoring, and hybrid rank fusion.",
            content = """
# RAG Technical Specification & Mathematical Formulation
Retrieval-Augmented Generation optimizes language model responses by indexing external domain documents.

Process Flow:
1. Text Ingestion: Raw documents are parsed into UTF-8 text and segmented into overlapping windows (500 characters with 100 character overlap).
2. Dense Embedding Vector: 768-dimensional floating point representation computed via Gemini Text-Embedding-004 or 128-dimensional local TF-IDF vectors for zero-network operation.
3. Similarity Metric: Cosine Similarity computed as:
   CosineSimilarity(A, B) = DotProduct(A, B) / (Magnitude(A) * Magnitude(B))
   where DotProduct(A, B) = Σ(Ai * Bi) from i=1 to n, and Magnitude(V) = √(Σ(Vi²)).
4. Lexical Scoring (BM25):
   Term Frequency (TF) and Inverse Document Frequency (IDF) are evaluated across document tokens to penalize ubiquitous stop words and reward discriminative keywords.
5. Hybrid Rank Fusion:
   Score = (VectorCosineScore * 0.65) + (KeywordBM25Score * 0.35)
   This balances abstract conceptual semantic alignment with exact keyword and proper-noun precision.
            """.trimIndent()
        ),

        InternalKnowledgeSourceItem(
            id = "ai_transformers",
            title = "Transformers, Attention Mechanisms & Neural Network Architecture",
            fileName = "Transformers, Attention Mechanisms & Neural Network Architecture.md",
            category = "Machine Learning",
            iconEmoji = "🧠",
            summary = "Self-attention formula, Multi-Head Attention, RoPE positional encoding, KV-caching, and quantizations.",
            content = """
# Transformer Architecture & Deep Learning Guide
The Transformer neural network architecture (Vaswani et al., 2017) underlies modern Large Language Models (LLMs).

Core Components:
1. Scaled Dot-Product Attention:
   Attention(Q, K, V) = softmax((Q * K^T) / √d_k) * V
   where Q is the Query matrix, K is the Key matrix, V is the Value matrix, and d_k is the dimension of the keys. The scaling factor 1/√d_k prevents dot products from growing excessively large, which would push the softmax into regions with vanishing gradients.
2. Multi-Head Attention:
   Allows the model to jointly attend to information from different representation subspaces at different positions:
   MultiHead(Q, K, V) = Concat(head_1, ..., head_h) * W^O
   where head_i = Attention(Q * W_i^Q, K * W_i^K, V * W_i^V).
3. Rotary Positional Embeddings (RoPE):
   Encodes token positions by rotating the query and key representations in complex vector space, naturally preserving relative distances over long sequence lengths.
4. Key-Value (KV) Cache:
   During autoregressive inference, past key and value vectors are cached in GPU/NPU memory so only the single newest token requires forward-pass projection, drastically reducing computation from O(N²) to O(N).
5. Model Quantization:
   Techniques such as INT8, INT4, AWQ, and GGUF compress 16-bit floating-point weights into integer representations with minimal perplexity degradation, allowing high-performance on-device execution.
            """.trimIndent()
        ),

        InternalKnowledgeSourceItem(
            id = "cyber_security",
            title = "Cybersecurity, Vault Encryption & Zero-Trust Architecture",
            fileName = "Cybersecurity, Vault Encryption & Zero-Trust Architecture.md",
            category = "Security",
            iconEmoji = "🔒",
            summary = "AES-256-GCM authenticated encryption, PBKDF2 key derivation, salt entropy, and zero-knowledge storage.",
            content = """
# Cybersecurity & Password Vault Technical Standard
Securing private credentials and sensitive documents requires defense-in-depth principles:

1. Symmetric Encryption Standard:
   Advanced Encryption Standard (AES) with 256-bit keys in Galois/Counter Mode (AES-256-GCM). GCM provides both confidentiality and cryptographic authenticity (Authenticated Encryption with Associated Data, AEAD), preventing ciphertext tampering.
2. Key Derivation Functions (KDF):
   Raw master passwords must never be stored. PBKDF2-HMAC-SHA256 or Argon2id stretch passwords with at least 100,000 iterations and a unique 128-bit cryptographically secure random salt (java.security.SecureRandom) to resist GPU rainbow-table attacks.
3. Password Entropy Calculation:
   Entropy (bits) = L * log2(R)
   where L is the password length and R is the size of the character pool:
   - Lowercase only: R = 26
   - Alphanumeric: R = 62
   - Printable ASCII (letters, numbers, symbols): R = 94
   A password with 16 characters from a 94-character pool yields 16 * log2(94) ≈ 104.8 bits of entropy, which is computationally infeasible to brute force.
4. Zero-Knowledge Architecture:
   All decryption keys are derived in ephemeral memory on the client device. Decrypted passwords and private notes are never transmitted over the network or saved to plain cloud logs.
            """.trimIndent()
        ),

        InternalKnowledgeSourceItem(
            id = "math_constants",
            title = "Scientific Constants, Mathematics Formulas & Physics Reference",
            fileName = "Scientific Constants, Mathematics Formulas & Physics Reference.md",
            category = "Science & Math",
            iconEmoji = "🔬",
            summary = "Fundamental physical constants, calculus derivatives, trigonometric identities, and SI conversions.",
            content = """
# Mathematics & Scientific Formula Reference Sheet
A handy computational and mathematical reference for scientific calculations and conversions:

1. Fundamental Constants of the Universe:
   - Speed of Light in Vacuum (c): 299,792,458 m/s (exact)
   - Planck's Constant (h): 6.62607015 × 10⁻³⁴ J·s
   - Reduced Planck Constant (ℏ): h / 2π ≈ 1.0545718 × 10⁻³⁴ J·s
   - Gravitational Constant (G): 6.67430 × 10⁻¹¹ m³/(kg·s²)
   - Boltzmann Constant (k_B): 1.380649 × 10⁻²³ J/K
   - Elementary Charge (e): 1.602176634 × 10⁻¹⁹ C
   - Avogadro Constant (N_A): 6.02214076 × 10²³ mol⁻¹
   - Pi (π): 3.141592653589793
   - Euler's Number (e): 2.718281828459045

2. Trigonometric Identities:
   - Pythagorean Identity: sin²(θ) + cos²(θ) = 1
   - Double Angle: sin(2θ) = 2·sin(θ)·cos(θ)
   - Double Angle: cos(2θ) = cos²(θ) - sin²(θ) = 2·cos²(θ) - 1
   - Euler's Formula: e^(iθ) = cos(θ) + i·sin(θ)
   - Euler's Identity: e^(iπ) + 1 = 0

3. Calculus Fundamentals:
   - Derivative of x^n: d/dx(x^n) = n·x^(n-1)
   - Derivative of e^x: d/dx(e^x) = e^x
   - Derivative of ln(x): d/dx(ln(x)) = 1/x (for x > 0)
   - Derivative of sin(x): d/dx(sin(x)) = cos(x)
   - Derivative of cos(x): d/dx(cos(x)) = -sin(x)
   - Fundamental Theorem of Calculus: ∫[a,b] f(x) dx = F(b) - F(a)

4. Standard Unit Conversions:
   - Temperature: T(K) = T(°C) + 273.15; T(°F) = T(°C) × 9/5 + 32
   - Energy: 1 eV = 1.602176634 × 10⁻¹⁹ Joules; 1 cal = 4.184 Joules
   - Pressure: 1 atm = 101,325 Pa = 760 mmHg = 1.01325 bar
            """.trimIndent()
        ),

        InternalKnowledgeSourceItem(
            id = "cs_algorithms",
            title = "Computer Science Algorithms & Complexity Analysis",
            fileName = "Computer Science Algorithms & Complexity Analysis.md",
            category = "Algorithms",
            iconEmoji = "⚡",
            summary = "Big-O notation, sorting complexities, graph traversals, dynamic programming, and data structures.",
            content = """
# Algorithms, Data Structures & Computational Complexity
Essential computer science paradigms and asymptotic performance analysis:

1. Asymptotic Big-O Analysis:
   - Constant Time: O(1) - Hash table lookup, array index access.
   - Logarithmic Time: O(log n) - Binary search, balanced BST search.
   - Linear Time: O(n) - Single loop over elements, sequential search.
   - Linearithmic Time: O(n log n) - MergeSort, HeapSort, optimal comparison sorting.
   - Quadratic Time: O(n²) - BubbleSort, nested loops, pairwise comparisons.
   - Exponential Time: O(2^n) - Recursive Fibonacci, subset generation.
   - Factorial Time: O(n!) - Traveling Salesperson Problem brute-force.

2. Classic Sorting Algorithms:
   - QuickSort: Average O(n log n), worst case O(n²). In-place partitioning.
   - MergeSort: Guaranteed O(n log n) time, requires O(n) auxiliary space. Stable.
   - HeapSort: Guaranteed O(n log n) time, O(1) auxiliary space. In-place heapification.

3. Graph Traversals:
   - Breadth-First Search (BFS): Explores vertex neighbors level by level using a Queue. Finds shortest paths in unweighted graphs. Time: O(V + E).
   - Depth-First Search (DFS): Explores deep branches using a Stack or recursion. Ideal for cycle detection and topological sorting. Time: O(V + E).
   - Dijkstra's Algorithm: Finds single-source shortest paths in graphs with non-negative edge weights using a Priority Queue. Time: O((V + E) log V).

4. Core Data Structures:
   - Hash Table: Average O(1) insertion, deletion, lookup. Handles collisions via chaining or open addressing.
   - Red-Black Tree / AVL Tree: Self-balancing BST guaranteeing O(log n) height.
   - Trie (Prefix Tree): Fast string search and autocomplete with O(L) lookup where L is word length.
            """.trimIndent()
        ),

        InternalKnowledgeSourceItem(
            id = "prompt_engineering",
            title = "Prompt Engineering, In-Context Learning & Agentic Workflows",
            fileName = "Prompt Engineering, In-Context Learning & Agentic Workflows.md",
            category = "AI Systems",
            iconEmoji = "💡",
            summary = "Chain-of-Thought, ReAct agent pattern, Few-Shot prompting, Tool calling, and sampling parameters.",
            content = """
# Prompt Engineering & Agentic Workflow Guide
Systematic techniques for eliciting reliable, grounded reasoning from large language models:

1. In-Context Learning & Prompt Paradigms:
   - Zero-Shot: Prompting the model directly without task demonstrations.
   - Few-Shot (ICL): Providing 2–5 structured input-output exemplars in the context window to anchor style, format, and reasoning pathways.
   - Chain-of-Thought (CoT): Directing the model to "think step by step" prior to arriving at a final conclusion, allowing intermediate token generation to guide subsequent deduction.

2. Agentic Architectures:
   - ReAct Pattern (Reasoning + Acting): Interleaving thought reasoning with external tool actions (Search, Calculate, Run Code) and observing results before generating answers.
   - Self-Refine / Critic Loops: Prompting the agent to critique its preliminary output against a rubric and iterate on improvements.
   - Tool Calling / Function Calling: Defining structured JSON schemas for executable functions so the model reliably outputs valid parameters rather than unstructured text.

3. Hyperparameter Tuning Dynamics:
   - Temperature (0.0 to 1.0+): Controls randomness. Low temperature (0.0–0.2) produces deterministic, factual, and analytical responses. High temperature (0.7–1.0) produces varied, creative output.
   - Top-P (Nucleus Sampling): Considers only the smallest set of tokens whose cumulative probability exceeds P (e.g., 0.95), cutting off the long tail of unlikely tokens.
   - System Instructions: Persistent behavioral framing that sets the persona, constraints, safety guardrails, and citation expectations for the interaction.
            """.trimIndent()
        ),

        InternalKnowledgeSourceItem(
            id = "audio_acoustics",
            title = "Audio Synthesis, Waveforms & Frequency Spectrum Acoustics",
            fileName = "Audio Synthesis, Waveforms & Frequency Spectrum Acoustics.md",
            category = "Audio & Acoustics",
            iconEmoji = "🎵",
            summary = "Nyquist-Shannon sampling, FFT spectrum analysis, synthesis oscillator waves, and musical frequencies.",
            content = """
# Audio Synthesis, Acoustics & Digital Signal Processing
Technical foundations of digital sound synthesis and audio spectrum analysis:

1. Digital Audio Representation:
   - Nyquist-Shannon Sampling Theorem: To accurately reconstruct an analog signal of maximum frequency f_max, the digital sampling rate f_s must satisfy f_s > 2·f_max. For human hearing (20 Hz – 20,000 Hz), CD-quality 44,100 Hz comfortably prevents aliasing distortion.
   - Bit Depth: 16-bit PCM offers 96 dB of dynamic range (6.02 dB per bit); 24-bit PCM offers 144 dB.

2. Fundamental Oscillator Waveforms:
   - Sine Wave: Pure single frequency without harmonic overtones. Smooth and clean.
   - Square Wave: Contains only odd harmonics (1f, 3f, 5f...) with amplitudes inversely proportional to frequency (1/n). Hollow, retro synth sound.
   - Sawtooth Wave: Contains all harmonics (even and odd) with amplitudes inversely proportional to frequency (1/n). Bright, buzzy, and cutting.
   - Triangle Wave: Contains only odd harmonics with amplitudes falling off at 1/n². Softer than square, warmer than sine.

3. Fast Fourier Transform (FFT):
   Decomposes a discrete time-domain audio signal into its constituent frequency spectrum bins. Powers real-time visualizers such as 32-band equalizers, audio radars, and oscilloscope ribbons.

4. Equal Temperament Tuning:
   Standard Western tuning divides the octave into 12 semitones where each semitone has a frequency ratio of 2^(1/12) ≈ 1.059463.
   Standard Concert Pitch: A4 = 440.00 Hz.
   - Middle C (C4) ≈ 261.63 Hz
   - C5 (Octave above) ≈ 523.25 Hz (exact 2:1 frequency ratio).
            """.trimIndent()
        ),

        InternalKnowledgeSourceItem(
            id = "markdown_spec",
            title = "Markdown Specification & Technical Documentation Reference",
            fileName = "Markdown Specification & Technical Documentation Reference.md",
            category = "Documentation",
            iconEmoji = "📄",
            summary = "CommonMark, GFM tables, code blocks with syntax tags, blockquotes, and LaTeX formatting.",
            content = """
# Markdown & Technical Documentation Specification
Standard formatting syntax for technical documentation, notes, and rich previews:

1. Headers & Hierarchy:
   - Level 1: # Title (Document title, unique per file)
   - Level 2: ## Major Section (Core architecture, components)
   - Level 3: ### Sub-section (Detailed subsections)
   - Level 4-6: #### Minor headings

2. Text Formatting:
   - Bold: **bold text** or __bold text__
   - Italic: *italic text* or _italic text_
   - Strikethrough: ~~strikethrough~~
   - Inline Code: `variableName` or `fun execute()`
   - Blockquotes: > Important operational constraint or note

3. Tables (GitHub Flavored Markdown):
   | Column Header | Alignment | Type |
   | :--- | :---: | ---: |
   | Left Aligned | Centered | Right Aligned |
   | Data Cell 1 | Active | 100.00 |

4. Code Blocks with Language Highlighting:
   Use triple backticks followed by the language identifier (kotlin, python, json, html, js, css, bash, sql).

5. Task Lists:
   - [x] Completed item with green check
   - [ ] Pending backlog action
            """.trimIndent()
        ),

        InternalKnowledgeSourceItem(
            id = "world_history",
            title = "Global Geography, History & Civilization Chronology",
            fileName = "Global Geography, History & Civilization Chronology.md",
            category = "World Knowledge",
            iconEmoji = "🌍",
            summary = "Milestones from the Agricultural Revolution to the Computing Era, major geographic bodies, and world history.",
            content = """
# Global History, Geography & Human Civilization Reference
Key chronological milestones and geographic reference facts:

1. Major Epochs in Human History:
   - Agricultural Revolution (circa 10,000 BCE): Transition from nomadic hunting-gathering to permanent agrarian settlements in the Fertile Crescent.
   - Classical Antiquity (circa 8th century BCE – 5th century CE): Flourishing of philosophy, democracy, and architecture in Ancient Greece and the Roman Empire; development of the Silk Road.
   - Scientific Revolution (16th – 17th Century): Paradigm shifts driven by Copernicus, Galileo, Kepler, and Newton replacing scholasticism with empirical observation and mathematical modeling.
   - First Industrial Revolution (circa 1760 – 1840): Steam power, mechanized textile manufacturing, iron smelting, and canal transportation.
   - Second Industrial Revolution (circa 1870 – 1914): Electrical power, internal combustion engines, mass steel production, telecommunications (telegraph/telephone).
   - Information Age (mid-20th Century – Present): Invention of the transistor (1947), the microchip, the ARPANET/Internet, personal computing, mobile smartphones, and generative artificial intelligence.

2. Earth Geography & Continents:
   - Continents (by land area): Asia (44.58M km²), Africa (30.37M km²), North America (24.71M km²), South America (17.84M km²), Antarctica (14.20M km²), Europe (10.18M km²), Oceania/Australia (8.60M km²).
   - Oceans: Pacific Ocean (largest & deepest, covering 165.2M km²), Atlantic Ocean (106.5M km²), Indian Ocean (70.56M km²), Southern Ocean (20.33M km²), Arctic Ocean (14.06M km²).
   - Deepest Oceanic Point: Challenger Deep in the Mariana Trench (~10,928 meters).
   - Highest Terrestrial Point: Mount Everest summit (8,848.86 meters above sea level).
            """.trimIndent()
        ),

        InternalKnowledgeSourceItem(
            id = "productivity_science",
            title = "Personal Productivity, Time Blocking & Executive Function Systems",
            fileName = "Personal Productivity, Time Blocking & Executive Function Systems.md",
            category = "Productivity",
            iconEmoji = "⏱️",
            summary = "Eisenhower matrix, Pomodoro cycles, Getting Things Done (GTD), and cognitive habit loops.",
            content = """
# Personal Productivity, Executive Function & Time Science
Frameworks for high-velocity cognitive output, habit maintenance, and calendar management:

1. Eisenhower Decision Matrix:
   - Urgent & Important (Quadrant 1): Do immediately (crises, deadlines, critical system failures).
   - Not Urgent & Important (Quadrant 2): Schedule proactively (strategic planning, skill development, health, architecture).
   - Urgent & Not Important (Quadrant 3): Delegate or automate (interruptions, minor notifications).
   - Not Urgent & Not Important (Quadrant 4): Eliminate (time wasters, doomscrolling).

2. Pomodoro Focus Cycles:
   - 25 minutes of unbroken single-task deep focus.
   - 5-minute cognitive rest interval away from screens.
   - After 4 consecutive cycles, take an extended 20–30 minute restoration break.
   - Minimizes context-switching penalties, which research shows can cost up to 23 minutes per interruption to recover deep flow.

3. The Neurological Habit Loop (Charles Duhigg / James Clear):
   - Cue: Environmental trigger initiating behavioral craving.
   - Craving: Motivational drive behind the action.
   - Response: Frictionless execution of the habit.
   - Reward: Immediate positive reinforcement satisfying the craving.
   - Strategy: Habit Stacking (linking a desired new habit immediately following an established automatic habit).

4. Time Blocking & Calendar Management:
   - Dedicate discrete calendar blocks for specific work categories rather than working from an unranked to-do list.
   - Protect high-energy morning hours for complex analytical tasks (deep work).
   - Consolidate administrative tasks (email, updates) into designated afternoon batch blocks.
            """.trimIndent()
        )
    )
}
