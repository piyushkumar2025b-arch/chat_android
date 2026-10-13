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
        ),

        InternalKnowledgeSourceItem(
            id = "quantum_computing",
            title = "Quantum Computing, Qubits & Quantum Algorithms",
            fileName = "Quantum Computing, Qubits & Quantum Algorithms.md",
            category = "Quantum Science",
            iconEmoji = "⚛️",
            summary = "Qubit superposition, Bloch sphere, quantum entanglement, Hadamard/CNOT gates, Shor's and Grover's algorithms.",
            content = """
# Quantum Computing Principles & Algorithmic Foundations
Quantum computation leverages the fundamental postulates of quantum mechanics to solve select computational problems exponentially faster than classical computers:

1. Qubit State & Superposition:
   Unlike a classical binary bit (0 or 1), a quantum bit exists in a continuous linear superposition:
   |ψ⟩ = α|0⟩ + β|1⟩
   where α, β ∈ ℂ are probability amplitudes satisfying the normalization constraint |α|² + |β|² = 1.
   Upon measurement in the computational basis, the state collapses to |0⟩ with probability |α|² and |1⟩ with probability |β|².
   Geometrically, single-qubit pure states are mapped onto the surface of the three-dimensional Bloch Sphere:
   |ψ⟩ = cos(θ/2)|0⟩ + e^(iφ)sin(θ/2)|1⟩

2. Quantum Entanglement & Bell States:
   Non-separable multi-qubit states where the quantum state of each particle cannot be described independently of the others:
   |Φ⁺⟩ = (|00⟩ + |11⟩) / √2
   Measurement on one entangled qubit instantaneously determines the state of the coupled qubit regardless of spatial separation (EPR paradox, verified by Bell's inequalities).

3. Fundamental Quantum Logic Gates:
   - Pauli-X (NOT): Flips |0⟩ ↔ |1⟩ (corresponds to π rotation around X-axis).
   - Pauli-Z (Phase Flip): Maps |1⟩ to -|1⟩, leaving |0⟩ invariant.
   - Hadamard Gate (H): Creates equal superposition: H|0⟩ = (|0⟩ + |1⟩)/√2 and H|1⟩ = (|0⟩ - |1⟩)/√2.
   - Controlled-NOT (CNOT): 2-qubit entangling gate flipping the target qubit if and only if the control qubit is |1⟩.
   - Toffoli (CCNOT): Universal classical reversible gate.

4. Groundbreaking Quantum Algorithms:
   - Shor's Algorithm (1994): Factors integers in polynomial time O((log N)³) via Quantum Phase Estimation and Quantum Fourier Transform (QFT), threatening standard RSA and Elliptic Curve Cryptography.
   - Grover's Algorithm (1996): Searches an unstructured database of N items with quadratic speedup O(√N), utilizing amplitude amplification and phase inversion.
   - Variational Quantum Eigensolver (VQE): Hybrid classical-quantum algorithm designed for NISQ (Noisy Intermediate-Scale Quantum) devices to simulate molecular electronic ground states in quantum chemistry.
            """.trimIndent()
        ),

        InternalKnowledgeSourceItem(
            id = "distributed_systems",
            title = "Distributed Systems, Consensus Protocols & Cloud Architecture",
            fileName = "Distributed Systems, Consensus Protocols & Cloud Architecture.md",
            category = "Cloud & Backend",
            iconEmoji = "🌐",
            summary = "CAP Theorem, PACELC, Raft & Paxos consensus, Vector Clocks, Eventual Consistency, and Gossip protocols.",
            content = """
# Distributed Systems Engineering & Scalability Reference
Building robust, fault-tolerant systems across networked, asynchronous machines:

1. The CAP Theorem (Brewer, 2000):
   In any asynchronous network prone to network partitions (P), a distributed data store can guarantee at most two of three properties:
   - Consistency (C): Every read receives the most recent write or an error.
   - Availability (A): Every non-failing node returns a non-error response for every request.
   - Partition Tolerance (P): The system continues operating despite arbitrary dropped or delayed messages between nodes.
   Since physical networks inevitably partition (fiber cuts, router reboots), modern distributed architectures must choose between CP (e.g., Raft, Spanner, Zookeeper) and AP (e.g., Cassandra, DynamoDB, CouchDB).

2. PACELC Theorem (Abadi, 2012):
   Extends CAP: If there is a Partition (P), choose between Availability (A) and Consistency (C); Else (E), choose between Latency (L) and Consistency (C).

3. Consensus Protocols (Raft & Multi-Paxos):
   - Leader Election: Randomized election timers ensure a single leader is elected via quorum majority (N/2 + 1 votes).
   - Log Replication: The leader accepts client write requests, appends log entries, and commits once a quorum of followers acknowledges replication.
   - Safety Invariant: If a leader has committed a log entry at a given index, no other node will ever commit a different entry for that index.

4. Logical Clocks & Causal Ordering:
   - Lamport Timestamps: Assigns scalar numbers to events such that if A happens-before B (A → B), then L(A) < L(B).
   - Vector Clocks: Array of integer clocks across all N processes, allowing detection of concurrent events and conflict resolution in decentralized multi-master systems.

5. Gossip Protocols & Failure Detection:
   - Nodes periodically exchange state summaries with random peers, achieving exponential epidemiological spread of membership metadata.
   - Phi Accrual Failure Detectors measure inter-heartbeat latency variance to output a dynamic suspicion probability rather than a brittle binary timeout.
            """.trimIndent()
        ),

        InternalKnowledgeSourceItem(
            id = "data_structures_algorithms",
            title = "Advanced Data Structures, Graph Algorithms & Big-O Optimization",
            fileName = "Advanced Data Structures, Graph Algorithms & Big-O Optimization.md",
            category = "Computer Science",
            iconEmoji = "🌲",
            summary = "Asymptotic analysis, Trie, Red-Black Trees, Segment Trees, Dijkstra, A* search, and Dynamic Programming.",
            content = """
# Advanced Algorithms & Data Structures Reference Manual
Computational efficiency and structural optimization guidelines:

1. Asymptotic Complexity Hierarchy:
   O(1) Constant < O(log N) Logarithmic < O(√N) Sub-linear < O(N) Linear < O(N log N) Linearithmic < O(N²) Quadratic < O(2^N) Exponential < O(N!) Factorial.

2. Advanced Tree & Range Query Structures:
   - Red-Black Tree: Self-balancing Binary Search Tree ensuring O(log N) worst-case search, insertion, and deletion via color invariants and tree rotations.
   - Trie (Prefix Tree): O(L) string retrieval and autocomplete, where L is key length, independent of total dataset size N.
   - Segment Tree & Fenwick Tree (Binary Indexed Tree): Enables range sum/min/max queries and point updates in O(log N) time with O(N) memory overhead.
   - Disjoint Set Union (DSU / Union-Find): Employs Path Compression and Union by Rank to achieve nearly constant amortized inverse Ackermann complexity O(α(N)).

3. Essential Graph Algorithms:
   - Dijkstra's Shortest Path: O((V + E) log V) on non-negative weighted graphs using a min-heap priority queue.
   - A* Heuristic Search: f(n) = g(n) + h(n), where g(n) is exact cost from start and h(n) is an admissible heuristic estimate to the goal, drastically reducing explored states.
   - Bellman-Ford: O(V * E) detects negative weight cycles and computes single-source shortest paths.
   - Tarjan's Strongly Connected Components (SCC): O(V + E) single-pass DFS identifying directed cycles via discovery low-link values.

4. Dynamic Programming (DP) Paradigms:
   - Optimal Substructure: Solution to problem can be constructed from optimal solutions to its subproblems.
   - Overlapping Subproblems: Space-time tradeoff via memoization (top-down) or tabulation (bottom-up).
   - Classical canonical patterns: 0/1 Knapsack, Longest Common Subsequence (LCS), Matrix Chain Multiplication, Edit Distance (Levenshtein), and Bitmask DP for TSP.
            """.trimIndent()
        ),

        InternalKnowledgeSourceItem(
            id = "linux_os_internals",
            title = "Linux Operating System Architecture, Kernel & POSIX Internals",
            fileName = "Linux Operating System Architecture, Kernel & POSIX Internals.md",
            category = "Operating Systems",
            iconEmoji = "🐧",
            summary = "Virtual memory, MMU page tables, system calls, epoll I/O multiplexing, cgroups, and process namespaces.",
            content = """
# Linux Kernel Architecture & Systems Programming Specification
Deep technical overview of Linux kernel subsystems and low-level runtime execution:

1. Process & Memory Management:
   - Virtual Memory Subsystem: 4KB standard pages mapped through multi-level hierarchical Page Tables (PGD, P4D, PUD, PMD, PTE) managed by the hardware Memory Management Unit (MMU).
   - Copy-on-Write (CoW): fork() clones parent page tables with write-protect flags; physical memory duplication occurs only when a process modifies a page, enabling instant process spawning.
   - Completely Fair Scheduler (CFS): Schedules runnable tasks using red-black trees ordered by virtual runtime (vruntime), guaranteeing fair CPU time shares based on nice values.

2. High-Performance I/O Multiplexing:
   - select() & poll(): O(N) scaling; re-scans entire file descriptor array on every event.
   - epoll (Event Poll): O(1) event readiness notification. Utilizes kernel red-black tree to store monitored descriptors and an in-kernel ready-list populated by hardware interrupt callbacks.
   - Edge-Triggered (EPOLLET) vs. Level-Triggered (EPOLLIN): Edge-triggered delivers events only when state transitions occur, requiring non-blocking sockets and drain-to-EAGAIN loops.
   - io_uring: Asynchronous ring buffers (Submission Queue & Completion Queue) shared between user space and kernel space, eliminating system call overhead entirely via SQPOLL kernel threads.

3. Linux Containers (LXC / Docker Primitives):
   - Namespaces: Isolate system resources per process tree: PID (processes), NET (interfaces & routing), MNT (mount points), IPC (message queues), UTS (hostnames), and USER (UID mappings).
   - Control Groups (cgroups v2): Enforce strict resource quotas, throttling, and accounting for CPU, memory, blkio, and pids.
   - Seccomp-BPF: Restricts available system calls for sandboxing untrusted executable workloads.
            """.trimIndent()
        ),

        InternalKnowledgeSourceItem(
            id = "modern_web_networking",
            title = "Modern Internet Networking Protocols: HTTP/3, QUIC & WebSockets",
            fileName = "Modern Internet Networking Protocols: HTTP/3, QUIC & WebSockets.md",
            category = "Networking",
            iconEmoji = "⚡",
            summary = "HTTP/1.1 vs HTTP/2 multiplexing, QUIC over UDP, 0-RTT TLS 1.3 handshake, and WebSockets.",
            content = """
# Modern Networking Architecture & Transport Layer Protocols
Evolution of web protocols powering real-time low-latency mobile and cloud applications:

1. HTTP/1.1 vs HTTP/2 vs HTTP/3:
   - HTTP/1.1: Plaintext protocol with Head-of-Line (HoL) blocking; requires multiple TCP connections (domain sharding) for concurrent resource downloads.
   - HTTP/2: Binary framing layer; multiplexes concurrent requests and responses over a single TCP connection. However, single TCP packet loss stalls all concurrent streams (TCP-level HoL blocking).
   - HTTP/3 (QUIC): Replaces TCP with UDP. Independent multiplexed byte streams ensure packet loss on stream A never impacts transmission of stream B.

2. QUIC Protocol Architecture:
   - Integrated TLS 1.3 Encryption: Eliminates redundant TCP 3-way handshake + TLS handshake round trips.
   - 0-RTT Connection Resumption: Returning clients can transmit encrypted application data in the very first flight.
   - Connection Migration: QUIC connections use a 64-bit Connection ID instead of IP:Port tuple, enabling seamless roaming between Wi-Fi and 5G cellular without dropped downloads or TCP reconnects.

3. Real-Time Streaming (WebSockets & WebRTC):
   - WebSockets (RFC 6455): Full-duplex bidirectional persistent connection established via HTTP/1.1 Upgrade header, with minimal 2–10 byte framing overhead per message.
   - Server-Sent Events (SSE): Unidirectional text/event-stream over standard HTTP, ideal for LLM token streaming with automatic reconnection handling.
   - WebRTC: Peer-to-peer real-time media exchange utilizing SRTP (Secure Real-time Transport Protocol) and SCTP for data channels negotiated via ICE/STUN/TURN.
            """.trimIndent()
        ),

        InternalKnowledgeSourceItem(
            id = "ai_agents_reasoning",
            title = "Autonomous AI Agents, Tool Use & Cognitive Reasoning Frameworks",
            fileName = "Autonomous AI Agents, Tool Use & Cognitive Reasoning Frameworks.md",
            category = "Artificial Intelligence",
            iconEmoji = "🦾",
            summary = "ReAct (Reasoning + Acting) loop, Chain-of-Thought, Tool/Function Calling, Tree of Thoughts, and Self-Reflection.",
            content = """
# Cognitive AI Agent Architecture & Multi-Step Reasoning
Operational paradigms for constructing reliable, autonomous LLM agents equipped with tools:

1. The ReAct Framework (Yao et al., 2022):
   Interleaves reasoning traces and task-specific actions in an iterative execution loop:
   - Thought: Deconstruct current state, evaluate sub-goals, and formulate next action.
   - Action: Emit structured tool invocation (e.g., searchWeb(query), calculate(expr), runCode(script)).
   - Observation: Ingest tool execution output into conversational working memory.
   - Repeat until goal verification produces Final Answer.

2. Structured Tool / Function Calling:
   - Schema Definition: Models receive JSON-Schema definitions specifying function signatures, parameter types, enums, and required fields.
   - Grammar-Constrained Decoding: Ensures model token generation adheres strictly to syntactic JSON grammar, eliminating parsing errors.
   - Grounding & Anti-Hallucination: Tool execution outputs serve as ground-truth anchor context, preventing fictitious assertions.

3. Advanced Reasoning Strategies:
   - Chain-of-Thought (CoT): Elicits step-by-step intermediate tokens before outputting conclusions, utilizing more forward-pass compute on complex problems.
   - Self-Consistency (Wang et al.): Samples multiple reasoning paths at high temperature and selects the majority consensus answer.
   - Tree of Thoughts (ToT): Explores deliberate reasoning trees using BFS/DFS with heuristic self-evaluations and backtracking.
   - Reflexion & Self-Correction: Agents critique their own past tool failures in retrospective memory to correct errors in subsequent attempts.
            """.trimIndent()
        ),

        InternalKnowledgeSourceItem(
            id = "quantum_advanced_mechanics",
            title = "Quantum Advanced Algorithms, QFT & Variational Optimization",
            fileName = "Quantum Advanced Algorithms, QFT & Variational Optimization.md",
            category = "Quantum Physics",
            iconEmoji = "⚛️",
            summary = "Superposition states, entanglement, Bloch sphere, Shor's & Grover's algorithms, and NISQ error mitigation.",
            content = """
# Quantum Computing Foundations & Algorithm Specifications
Quantum computation leverages fundamental quantum mechanical phenomena to solve specialized computational problems exponentially faster than classical Turing machines.

1. Qubit Mechanics & State Representation:
   - Superposition: Pure state |ψ⟩ = α|0⟩ + β|1⟩ where α, β ∈ ℂ and |α|² + |β|² = 1.
   - Bloch Sphere: Geometric representation parameterized by polar angle θ and azimuthal angle φ: |ψ⟩ = cos(θ/2)|0⟩ + e^(iφ)sin(θ/2)|1⟩.
   - Entanglement: Composite states that cannot be factored into tensor products of single qubit states, exemplified by Bell states: |Φ+⟩ = (|00⟩ + |11⟩)/√2.

2. Quantum Logic Gates:
   - Single-Qubit: Pauli-X (NOT), Pauli-Z (phase flip), Hadamard H = (|0⟩+|1⟩)/√2 (creates equal superposition), and Phase Gate S.
   - Multi-Qubit Entangling: CNOT (Controlled-NOT), CZ (Controlled-Z), and Toffoli (CCNOT).
   - Unitary Invariance: All quantum logic gates correspond to unitary operators (U†U = I), ensuring quantum state evolution is strictly reversible.

3. Benchmark Quantum Algorithms:
   - Deutsch-Jozsa: Determines if a black-box boolean function is constant or balanced in a single evaluation (O(1) vs classical O(2^(n-1))).
   - Grover's Search Algorithm: Quadratic speedup for searching unsorted databases of N items in O(√N) iterations using amplitude amplification.
   - Shor's Algorithm: Polynomial time prime factorization of an integer N in O((log N)³) utilizing Quantum Fourier Transform (QFT) to compute modular order.
   - Quantum Approximate Optimization Algorithm (QAOA): Hybrid quantum-classical variational heuristic for combinatorial graph optimization on noisy intermediate-scale quantum (NISQ) processors.
            """.trimIndent()
        ),

        InternalKnowledgeSourceItem(
            id = "cloud_kubernetes",
            title = "Cloud Native Architecture, Kubernetes & Distributed Systems",
            fileName = "Cloud Native Architecture, Kubernetes & Distributed Systems.md",
            category = "Cloud Infrastructure",
            iconEmoji = "☁️",
            summary = "Kubernetes control plane, Pod scheduling, Service Mesh (mTLS), Raft consensus, and CAP theorem trade-offs.",
            content = """
# Cloud Native Architecture & Distributed Systems Engineering
Architecture patterns for building resilient, elastic, and container-orchestrated microservices:

1. Kubernetes (K8s) Control Plane & Architecture:
   - kube-apiserver: Stateless REST gateway validating and configuring Pods, Deployments, and CRDs.
   - etcd: Distributed consistent key-value store using the Raft consensus algorithm for cluster state synchronization.
   - kube-scheduler: Evaluates resource requests/limits, node affinity/anti-affinity, taints, and tolerations to assign Pods to worker nodes.
   - kube-controller-manager: Continuous reconciliation loop enforcing desired cluster state vs observed state.

2. Networking & Service Mesh:
   - Container Network Interface (CNI): Flat IP-per-Pod network topology (Calico, Cilium with eBPF datapaths).
   - Kubernetes Services: ClusterIP, NodePort, and LoadBalancer abstractions backed by kube-proxy iptables or IPVS routing.
   - Service Mesh (Istio / Linkerd): Envoy sidecar proxies injecting mutual TLS (mTLS), distributed tracing (OpenTelemetry), circuit breakers, and canary routing without modifying application binaries.

3. Distributed Systems Consistency & Reliability:
   - CAP Theorem: A distributed data store can guarantee at most two of Consistency, Availability, and Partition Tolerance.
   - PACELC Theorem: In partitioned systems choose Availability or Consistency; else choose Latency or Consistency.
   - Saga Pattern: Coordinates distributed transactions across microservices using compensating transactions to recover from intermediate failures.
            """.trimIndent()
        ),

        InternalKnowledgeSourceItem(
            id = "cyber_security_zero_trust",
            title = "Cybersecurity, Cryptography & Zero-Trust Defense",
            fileName = "Cybersecurity, Cryptography & Zero-Trust Defense.md",
            category = "Security",
            iconEmoji = "🛡️",
            summary = "AES-GCM-256, RSA vs ECC curves, Zero-Trust Architecture (ZTA), OWASP Top 10 mitigation, and OAuth2/OIDC flows.",
            content = """
# Cybersecurity Principles, Cryptography & Defense-in-Depth
Comprehensive security architecture for protecting cloud workloads and mobile client applications:

1. Modern Cryptographic Primitives:
   - Symmetric Ciphers: AES-256-GCM (Galois/Counter Mode) and ChaCha20-Poly1305 providing authenticated encryption with associated data (AEAD).
   - Asymmetric Cryptography: Elliptic Curve Cryptography (Ed25519 for digital signatures, X25519 for Diffie-Hellman key exchange) providing superior 128-bit security with much smaller key lengths than RSA-2048/4096.
   - Key Derivation Functions (KDF): Argon2id, PBKDF2, and HKDF for resilient password storage against GPU/ASIC brute-force cracking.

2. Zero-Trust Architecture (NIST SP 800-207):
   - Never Trust, Always Verify: Perimeter security is assumed breached; every network request requires explicit cryptographic authentication, least-privilege authorization, and risk inspection.
   - Continuous Posture Evaluation: Device health, location, certificate validation, and behavioral telemetry inspected before granting access tokens.
   - Micro-Segmentation: Restricts lateral network movement across workload zones through granular firewalling and software-defined perimeters.

3. Application Security & OWASP Top 10 Defense:
   - Injection Prevention: Mandatory parameterized queries and prepared statements for SQL/NoSQL databases.
   - Cryptographic Storage: Android EncryptedSharedPreferences, Keystore hardware-backed KeyStore (StrongBox / TEE) for biometric-bound keys.
   - Cross-Site Request Forgery (CSRF) & XSS: SameSite cookie flags, Content Security Policy (CSP), and strict input sanitization.
            """.trimIndent()
        ),

        InternalKnowledgeSourceItem(
            id = "space_astrophysics",
            title = "Orbital Mechanics, Astrophysics & Deep Space Propulsion",
            fileName = "Orbital Mechanics, Astrophysics & Deep Space Propulsion.md",
            category = "Space Science",
            iconEmoji = "🚀",
            summary = "Tsiolkovsky rocket equation, Keplerian orbital elements, Hohmann transfers, Lagrange points, and stellar nucleosynthesis.",
            content = """
# Aerospace Engineering & Astrophysics Principles
Physics foundations governing orbital trajectories, celestial mechanics, and deep space exploration:

1. Rocket Propulsion & Orbital Dynamics:
   - Tsiolkovsky Rocket Equation: Δv = v_e * ln(m_0 / m_f) = I_sp * g_0 * ln(m_0 / m_f), relating achievable velocity change to propellant mass fraction and specific impulse (I_sp).
   - Kepler's Laws:
     1. Planetary orbits are ellipses with the Sun at one focus.
     2. A line segment joining a planet and the Sun sweeps out equal areas during equal intervals of time.
     3. The square of the orbital period is proportional to the cube of the semi-major axis (T² ∝ a³).
   - Orbital Transfers: Hohmann transfer orbit calculates minimum energy delta-v required to transit between two circular coplanar orbits.

2. Gravitational Equilibrium & Lagrange Points:
   - Three-body gravitational equilibria (L1, L2, L3, L4, L5) where gravitational forces and orbital centrifugal forces balance.
   - Halo and Lissajous Orbits: Stable trajectories at Sun-Earth L2 home to deep-space observatories like the James Webb Space Telescope (JWST).

3. Stellar Evolution & Astrophysics:
   - Hydrostatic Equilibrium: Inward gravitational collapse balanced against outward radiation pressure generated by core nuclear fusion.
   - Stellar Nucleosynthesis: Proton-proton chain and CNO cycle fusing hydrogen into helium, progressing through carbon, neon, oxygen, and silicon burning until reaching the iron peak (Fe-56).
   - Final Evolutionary States: White dwarfs supported by electron degeneracy pressure (Chandrasekhar limit ~1.44 M☉), neutron stars supported by neutron degeneracy pressure (Tolman-Oppenheimer-Volkoff limit ~2.1 M☉), and black holes.
            """.trimIndent()
        ),

        InternalKnowledgeSourceItem(
            id = "clean_energy_batteries",
            title = "Clean Energy, Battery Chemistry & Grid Storage Systems",
            fileName = "Clean Energy, Battery Chemistry & Grid Storage Systems.md",
            category = "Sustainable Tech",
            iconEmoji = "🔋",
            summary = "Lithium-ion vs LFP cathode chemistry, solid-state electrolytes, photovoltaic bandgap, and grid frequency regulation.",
            content = """
# Clean Energy Technologies & Electrochemical Energy Storage
Engineering analysis of sustainable generation, electrochemical storage, and grid-scale energy transition:

1. Electrochemical Cell Chemistry:
   - Lithium-Ion (NMC / LFP): Intercalation of lithium ions into layered transition-metal oxide cathodes (LiNi_xMn_yCo_zO_2) or olivine lithium iron phosphate (LiFePO_4) during charge/discharge cycles.
   - Energy Density vs Cycle Life: NMC achieves high volumetric density (~250-300 Wh/kg), while LFP offers superior thermal stability, cobalt-free chemistry, and extended cycle life (3000-5000+ cycles).
   - Solid-State Batteries: Replacement of flammable liquid carbonate electrolytes with ceramic or polymer solid electrolytes, enabling lithium metal anodes for >400 Wh/kg specific energy and rapid charging.

2. Photovoltaic Solar Generation:
   - Semiconductor P-N Junction: Photons with energy exceeding the semiconductor bandgap (E_g ≈ 1.1 eV for Silicon) liberate electron-hole pairs, producing electric current through the built-in electric field.
   - Shockley-Queisser Limit: Maximum theoretical thermodynamic efficiency of single-junction solar cells (~33.7%) constrained by unabsorbed sub-bandgap photons and thermalization losses.
   - Perovskite Tandem Cells: Stacking wide-bandgap perovskites atop silicon absorbs complementary solar spectra, surpassing 30%+ practical module efficiency.

3. Grid Integration & Energy Storage Systems (BESS):
   - Frequency Response & Inertia: Grid-forming inverters (GFM) emulate synchronous machine rotational inertia to stabilize 50Hz/60Hz grid frequency during sudden generation or load disturbances.
   - Utility-Scale Storage: Flow batteries (Vanadium redox) and pumped hydroelectric storage for long-duration energy storage (8-24+ hours).
            """.trimIndent()
        ),

        InternalKnowledgeSourceItem(
            id = "algorithms_graph_theory",
            title = "Graph Theory, Dynamic Programming & Advanced Algorithms",
            fileName = "Graph Theory, Dynamic Programming & Advanced Algorithms.md",
            category = "Computer Science",
            iconEmoji = "📊",
            summary = "Dijkstra & A* pathfinding, Tarjan SCC, Maximum Flow (Ford-Fulkerson), Knapsack DP, and Segment Trees.",
            content = """
# Advanced Algorithms, Graph Theory & Computational Complexity
Algorithmic paradigms for high-efficiency computational processing and combinatorial problem solving:

1. Shortest Path & Network Algorithms:
   - Dijkstra's Algorithm: Greedy single-source shortest path on non-negative weighted graphs in O((V + E) log V) utilizing binary/fibonacci heaps.
   - A* Search: Heuristic-guided pathfinding where f(n) = g(n) + h(n); admissible and consistent heuristics guarantee optimal shortest paths while pruning search space.
   - Tarjan's Strongly Connected Components (SCC): Computes maximal strongly connected subgraphs in linear O(V + E) using depth-first search (DFS) with low-link values.
   - Network Flow: Ford-Fulkerson and Edmonds-Karp algorithms computing maximum flow matching minimum cut capacity in O(V * E²).

2. Dynamic Programming Paradigms:
   - Optimal Substructure & Overlapping Subproblems: State transitions memoized top-down or tabulated bottom-up.
   - 0/1 Knapsack & Unbounded Knapsack: Maximizing utility within capacity constraints; state dp[i][w] = max(dp[i-1][w], dp[i-1][w - weight[i]] + value[i]).
   - Longest Common Subsequence (LCS) and Edit Distance (Levenshtein): String alignment and diff algorithms foundational to version control and bioinformatic sequence matching.

3. Advanced Spatial & Range Query Data Structures:
   - Segment Trees & Fenwick Trees (Binary Indexed Tree): Range sum/minimum queries and point updates in O(log N) time with lazy propagation for range updates.
   - Disjoint Set Union (DSU / Union-Find): Near-constant time amortized O(α(N)) set connectivity operations with path compression and union by rank.
            """.trimIndent()
        ),

        InternalKnowledgeSourceItem(
            id = "synthetic_biology_genomics",
            title = "Synthetic Biology, CRISPR Gene Editing & Genomics",
            fileName = "Synthetic Biology, CRISPR Gene Editing & Genomics.md",
            category = "Biotechnology",
            iconEmoji = "🧬",
            summary = "CRISPR-Cas9 endonuclease mechanics, base editing, prime editing, NGS genomic sequencing and computational biology.",
            content = """
# Synthetic Biology, CRISPR Gene Editing & Molecular Genomics
Core principles of modern molecular biotechnology and genomic engineering:

1. CRISPR-Cas Gene Editing Systems:
   - Cas9 Endonuclease: Directed by single guide RNA (sgRNA) targeting ~20nt sequence adjacent to Protospacer Adjacent Motif (PAM, 5'-NGG-3').
   - Double-Strand Breaks (DSB): Repaired via Non-Homologous End Joining (NHEJ, causing gene knockout indels) or Homology-Directed Repair (HDR, precise sequence insertion using donor template).
   - Base Editing & Prime Editing: Deaminase-fused Cas nickases enabling C->T or A->G transitions without DSBs; prime editing couples reverse transcriptase to pegRNA for arbitrary insertions and deletions.

2. Next-Generation Sequencing (NGS) & Bio-computation:
   - High-Throughput Sequencing: Illumina reversible dye-terminator SBS, Pacific Biosciences long-read SMRT sequencing, and Oxford Nanopore strand translocation.
   - Computational Alignment: Burrows-Wheeler Aligner (BWA), De Bruijn graph de novo assembly, and Variant Call Format (VCF) analysis.
   - Structural Biology: AlphaFold 3 and ESMFold predicting atomic coordinates and ligand interactions from primary amino acid sequences.
            """.trimIndent()
        ),

        InternalKnowledgeSourceItem(
            id = "cryptography_blockchain",
            title = "Modern Cryptography, Zero-Knowledge Proofs & zk-SNARKs",
            fileName = "Modern Cryptography, Zero-Knowledge Proofs & zk-SNARKs.md",
            category = "Cryptography",
            iconEmoji = "🔐",
            summary = "Post-quantum lattice cryptography, elliptic curve pairings, zero-knowledge proofs (zk-SNARKs/STARKs), and verifiable computation.",
            content = """
# Modern Cryptography, Zero-Knowledge Proofs & Verifiable Computation
Mathematical foundations of state-of-the-art cryptographic protocols:

1. Zero-Knowledge Proofs (ZKP):
   - Fundamental Properties: Completeness (honest verifier convinced by honest prover), Soundness (malicious prover cannot convince verifier of falsehood with non-negligible probability), Zero-Knowledge (verifier learns nothing beyond truth of statement).
   - zk-SNARKs (Succinct Non-Interactive Arguments of Knowledge): Arithmetic circuit compilation (R1CS, QAP), elliptic curve pairings (BLS12-381), and sub-millisecond verification.
   - zk-STARKs: Scalable and transparent ZKPs relying strictly on collision-resistant hash functions (FRI protocol) with no trusted setup and post-quantum resistance.

2. Post-Quantum Cryptography (NIST Standardized):
   - Lattice-Based Cryptography: Module Learning With Errors (M-LWE) and Shortest Vector Problem (SVP); standardized in ML-KEM (Kyber) and ML-DSA (Dilithium).
   - State-Machine Replication: Byzantine Fault Tolerant (BFT) consensus, verifiable random functions (VRF), and Merkle Patricia Tries.
            """.trimIndent()
        )
    )
}

