# 🌐 OmniChat: The Universal On-Device AI Operating Environment & Personal Intelligence Suite

> **A native, zero-compromise Android AI workstation and daily executive productivity suite built with 100% Kotlin, Jetpack Compose, Material Design 3, local vector RAG, zero-trust cryptographic vaults, and multi-provider intelligence routing.**

---

## 🎯 What I Tried to Build & Why It's Useful

### The Vision
In today's mobile landscape, interacting with artificial intelligence is profoundly fragmented. Most mobile "AI" apps are sluggish web-view wrappers or locked into a single proprietary cloud API. Users who need to brainstorm, draft documents, search real-time news, organize encrypted credentials, track daily events, solve scientific equations, edit photos, and write code are forced to bounce between 8 to 10 distinct, tracking-heavy applications. Furthermore, sensitive user documents and passwords are often uploaded to third-party cloud servers without transparency.

**OmniChat was designed to dismantle this fragmentation.** 
I set out to build an all-in-one, privacy-first **Personal Intelligence Operating Environment** on Android. OmniChat gives users desktop-grade creative and analytical power directly in their pocket:

1. **Freedom of Intelligence**: The app doesn't lock you into one model. It intelligently routes between world-class frontier models—Google Gemini 2.5/3.5, Groq LLaMA 3.3, OpenRouter (Claude 3.7, DeepSeek V3/R1), Cerebras (hyper-speed 8,000+ tok/s), and Pollinations free tier—with zero downtime.
2. **Private On-Device Knowledge (RAG Engine)**: Users can ingest their own private PDFs, Word documents, text notes, and codebases into an offline vector database. The app indexes them into embeddings and utilizes hybrid semantic vector + BM25 keyword retrieval to ground AI answers with verifiable citations.
3. **Zero-Trust Daily Productivity**: Real-world work requires daily execution tools. OmniChat natively integrates a master-protected **Password Saver Vault**, a categorized **Notes Hub**, a dual-mode **Scientific & Standard Calculator**, and a full **Calendar & Agenda Scheduler**, all backed by local SQLite Room storage that never leaks data off the device.
4. **Complete Creative & Technical Studios**: Beyond chat, users have access to an instant **Photo & Image Studio**, a live-rendering **Code & Web Sandbox**, a split-view **Markdown Hub**, a synthesized **Music & Beat Studio** with real-time audio visualizers, and an **AI Video Motion Studio**.
5. **Real-Time Live Web Grounding**: Live search and news telemetry queries 15+ real-time academic, encyclopedic, and journalistic databases (Wikipedia, arXiv, PubMed, DuckDuckGo, Stack Overflow, GitHub, and global RSS feeds) with real publisher citations.

---

## 🛠️ Complete Technology Stack

| Layer | Technology / Library | Version | Purpose & Rationale |
| :--- | :--- | :--- | :--- |
| **Language** | Kotlin | `2.0.21` | Modern, concise, type-safe development with coroutines & flow |
| **Target Framework** | Android SDK | Compile/Target `35`, Min `24` | Modern Android 15 ready with backward compatibility to Android 7.0 |
| **UI Toolkit** | Jetpack Compose | Material 3 (`1.3.1`) | Declarative, dynamic reactive UI with fluid animations & M3 tokens |
| **Architecture** | MVVM + Clean Architecture | Android Jetpack | ViewModel, StateFlow, Coroutines, Repository pattern, and unidirectional data flow |
| **Database & Persistence** | Android Room Database | `2.6.1` + KSP | Local SQLite WAL persistence, reactive Flow queries, destructive migration |
| **Preferences & Storage** | AndroidX DataStore / SharedPreferences | AndroidX Core | Lightweight, persistent settings, theme toggles, and user preferences |
| **Networking & HTTP** | OkHttp3 | `4.12.0` | High-performance HTTP client, connection pooling, and multi-part handling |
| **XML & RSS Parsing** | XmlPullParserFactory | Android Runtime | Fast, memory-efficient streaming RSS and Atom feed deserialization |
| **Image Loading** | Coil Compose | `2.7.0` | Asynchronous image decoding, memory caching, and disk caching |
| **Vector Engine & RAG** | Local TF-IDF + Gemini Embeddings | `text-embedding-004` | 768-d semantic dense vectors + 128-d local term hashing with Reciprocal Rank Fusion |
| **Cryptography & Security** | Java Cryptography Architecture (JCA) | AES-256-GCM / PBKDF2 | Authenticated encryption, SecureRandom salt generation, and zero-knowledge vaults |
| **Media & Scoped Storage** | Android MediaStore API | Native API | Zero-permission Scoped Storage for Android 10+ (`Pictures/`, `Download/`, `Music/`, `Movies/`) |
| **Voice & Speech** | Android Text-To-Speech (TTS) | `android.speech.tts` | Local on-device synthesized vocalization for articles and assistant replies |
| **Web Rendering** | Android WebKit WebView | Android Platform | Live embedded execution sandbox for HTML5, CSS3, and JavaScript code snippets |

---

## 🌟 Point-Wise Granular Feature Breakdown

### 🧠 1. Multi-Provider AI Reasoning Engine
- **Universal Multi-Provider Switcher**: Switch seamlessly between **Google Gemini** (Gemini 2.5 Flash, Gemini Pro), **Groq** (LLaMA 3.3 70B Versatile, LLaMA 3.1 8B), **OpenRouter** (Claude 3.7 Sonnet, DeepSeek V3, DeepSeek R1), **Cerebras** (Ultra-low latency inference delivering 8,000+ tokens/second), and **Pollinations** (Free tier zero-API-key fallback).
- **Latency & Provider Health Telemetry**: Live ping and status diagnostics showing response latencies, active models, token metrics, and token usage estimators.
- **Context Preservation & Multi-Session History**: Organize conversations into distinct sessions with custom session names, session search, message pinning, and session deletion.
- **Smart System Prompting & Personas**: Pre-tuned persona configurations including Senior Software Architect, Research Scientist, Socratic Tutor, Creative Storyteller, and Custom System Instructions.
- **Token Estimator & Usage Limits Sheet**: Real-time counter calculating prompt tokens, response tokens, and daily rate limit projections.

### 🌐 2. Live Web Grounding & Real-Time Intelligence
- **Multi-Source Web Search Engine**: Ground queries with live information across 15+ real-time search backends:
  - **Encyclopedias**: Wikipedia, Wikibooks, Wikiquote, Wiktionary, Wikiversity, Wikivoyage.
  - **Academic & Scientific Repositories**: arXiv, PubMed, OpenAlex, CrossRef, NASA Archives.
  - **Developer Knowledge**: Stack Overflow API, GitHub Public Repositories, NPM Registry, DEV.to.
  - **Live Search & News**: DuckDuckGo Instant Answers, Bing News, Hacker News (Algolia API).
- **Anti-Hallucination Citations**: The AI cites exact publisher names, article links, and publication dates in the chat response.
- **Live Search Sandbox**: Interactive search screen allowing users to query any topic, preview snippets with colored source badges, and dispatch the research directly into the chat prompt with one tap.

### 📚 3. Local RAG Knowledge Hub & Vector Retrieval
- **Universal Document Ingestion**: Ingest and index private files into the local Room database, including **PDF**, **DOCX**, **TXT**, **Markdown (.md)**, **JSON**, and code files (**Kotlin, Python, Java, JS, HTML, C++**).
- **Overlapping Semantic Sentence Chunking**: Documents are parsed into 500-character windows with a 100-character sliding overlap to preserve conversational context across chunk boundaries.
- **Hybrid Reciprocal Rank Fusion**:
  - **Dense Semantic Vectors**: 768-dimensional float embeddings generated via Google Gemini `text-embedding-004`.
  - **Offline Fallback Vectors**: Deterministic 128-dimensional local TF-IDF and bigram hashing for completely offline semantic search without requiring network access.
  - **Lexical Keyword Scoring (BM25)**: Evaluates exact term frequencies to prevent semantic drift.
  - **Weighted Fusion**: Final score = `(0.65 × VectorCosineScore) + (0.35 × KeywordBM25Score)`.
- **12 Curated Built-In Internal Knowledge Sources**:
  1. *OmniChat AI Core Architecture & Capabilities* (`.md`)
  2. *Android Jetpack Compose & Clean Architecture Specs* (`.txt`)
  3. *RAG & Semantic Retrieval Mathematics* (`.md`)
  4. *Transformers, Attention Mechanisms & Neural Network Architecture* (`.md`)
  5. *Cybersecurity, Vault Encryption & Zero-Trust Architecture* (`.md`)
  6. *Scientific Constants, Mathematics Formulas & Physics Reference* (`.md`)
  7. *Computer Science Algorithms & Complexity Analysis* (`.md`)
  8. *Prompt Engineering, In-Context Learning & Agentic Workflows* (`.md`)
  9. *Audio Synthesis, Waveforms & Frequency Spectrum Acoustics* (`.md`)
  10. *Markdown Specification & Technical Documentation Reference* (`.md`)
  11. *Global Geography, History & Civilization Chronology* (`.md`)
  12. *Personal Productivity, Time Blocking & Executive Function Systems* (`.md`)
- **Internal Sources Browser**: Browse the library of 12 built-in reference sources, read full previews, and ingest them all at once or individually.
- **Semantic Retrieval Sandbox**: Test queries against your local vector database live to inspect rank, match type (`HYBRID`, `SEMANTIC_VECTOR`, `KEYWORD_BM25`), and percentage relevance score.

### 🔒 4. Secure Password Saver Vault
- **Zero-Knowledge Local Storage**: Store account credentials, emails, and passwords safely inside the on-device Room database.
- **Categorized Vault Structure**: Group passwords by categories—*Logins, Work, Finance, Social, Entertainment, and Other*.
- **Password Strength Analyzer**: Real-time evaluation of password entropy (Weak, Moderate, Strong, Fortified) checking length, uppercase, numbers, and symbols.
- **Cryptographic Password Generator**: Generate custom 8 to 32 character passwords with toggles for symbols, digits, and capital letters.
- **Privacy Protections**: Hidden password masks with visibility toggles, one-tap clipboard copy, and website launch shortcuts.

### 📝 5. Notes & Ideas Hub
- **Rich Tagging & Color Palette**: Color-code notes with vibrant M3 palette tags (*Indigo, Emerald, Amber, Rose, Purple, Teal*).
- **Organization & Filtering**: Organize by categories (*Personal, Work, Ideas, Study, Checklist*) and quickly filter by search keywords.
- **Pinned Notes**: Keep critical notes pinned to the top of the hub.
- **Instant AI Grounding**: One-tap "Send to Chat" button to feed note content directly to the AI assistant for summarizing, rewriting, or outlining next steps.

### 🧮 6. Dual-Mode Scientific & Standard Calculator
- **Standard Mode**: Clean arithmetic interface with addition, subtraction, multiplication, division, percentage, sign toggle, and decimals.
- **Scientific Mode**: Advanced mathematics suite featuring trigonometric functions (`sin`, `cos`, `tan`), logarithmic functions (`ln`, `log`), powers (`x^y`, `x²`), square root (`√`), factorial (`n!`), and mathematical constants (`π`, `e`).
- **Calculation History Log**: Every evaluation is logged with timestamp and equation string; tap any past calculation to restore or copy it.
- **AI Integration**: Tap "Explain in Chat" to transfer calculation equations directly into the chat for mathematical proofs, unit analysis, or derivations.

### 📅 7. Interactive Calendar & Agenda Hub
- **Full Month Calendar Grid**: Responsive calendar grid with day-of-week headers, previous/next month navigation, and a quick "Today" jump button.
- **Visual Event Indicators**: Dates with scheduled tasks or meetings display category-colored indicators.
- **Event Scheduling**: Schedule events with start and end times, descriptions, and category tags (*Work, Meeting, Personal, Study, Reminder*).
- **Task Completion Toggles**: Check off completed items directly from the agenda timeline.
- **Chat Consultation**: One-tap "Plan Day with AI" button sends the day's schedule to the assistant for time optimization and task prioritization.

### 🎨 8. Creative Studio: Photo & Image Editor
- **Zero-Permission Photo Selection**: Utilizes Google Play compliant Android Photo Picker (`ActivityResultContracts.PickVisualMedia`).
- **One-Tap Color Filter Presets**: Instant GPU-accelerated canvas filters including *Vibrant, Black & White, Vintage Sepia, Cyber Cool, Golden Warmth, and Negative/Invert*.
- **Precision Image Sliders**: Granular adjustment sliders for *Brightness (-100 to +100), Contrast (0.5x to 2.0x), and Saturation (0.0 to 2.0)*.
- **Transform & Geometry Controls**: 90° clockwise rotation, horizontal flipping, vertical flipping, and preset aspect-ratio cropping (1:1, 4:3, 16:9).
- **Freehand Canvas Drawing Pen**: Draw on images with customizable brush colors and stroke widths.
- **Export to MediaStore**: Save edited images directly to `Pictures/OmniChat/` using scoped storage.

### 💻 9. Creative Studio: Code & Live Web Sandbox
- **Multi-Language Syntax Editor**: Write and edit HTML, CSS, JavaScript, Kotlin, Python, and SQL code.
- **Embedded WebKit Runner**: Live WebView rendering engine to test HTML5, CSS3, and JavaScript web apps, canvas games, and interactive widgets directly on your device.
- **Syntax Snippets**: Quick boilerplate generation for HTML5 templates, responsive flexbox cards, SVG graphics, and canvas games.
- **Export**: Download code files directly to `Download/OmniChat/`.

### 📄 10. Creative Studio: Markdown Hub
- **Split-View Editor & Preview**: Toggle between Raw Editor, Live Rendered Preview, or Side-by-Side Split View.
- **Formatting Toolbar**: Instant formatting tools for Bold, Italic, Code, Blockquotes, Headings, Bullet Lists, and GFM Tables.
- **Device File Picker**: Open and view any `.md` or `.txt` file stored on your phone.
- **Artifact Export**: Save formatted markdown documents to phone storage.

### 🎵 11. Creative Studio: Music & Beat Synthesizer
- **Synthesizer Presets**: Lo-Fi Chill, Cyberpunk 140, Retro Synthwave, and Deep Ambient.
- **Acoustic Controls**: Live BPM tempo adjustment, master volume slider, and 3-band equalizer (Bass, Mid, Treble).
- **3 Real-Time Canvas Visualizers**:
  1. *32-Band Spectrum Equalizer*: Animated frequency bars reacting to volume and audio bands.
  2. *Pulsing Beat Radar*: Concentric ripple rings pulsing in sync with the song's BPM.
  3. *Waveform Oscilloscope Ribbon*: Fluid animated sine wave tracing audio waveforms.

### 🎬 12. Creative Studio: AI Video Motion Studio
- **Prompt-to-Motion Pipeline**: Generate cinematic animations and motion concepts from descriptive text prompts.
- **Style Controls**: Cinematic 3D, Anime/Manga, Photorealistic, Cyberpunk, and Watercolor styles with framerate controls (24 FPS, 30 FPS, 60 FPS).
- **Export**: Direct video download into `Movies/OmniChat/`.

### 📰 13. Live Global News Terminal
- **Categorized News Hub**: Instant streaming RSS news across 8 global categories:
  - *All News, Tech & AI, World, Science, Business, Entertainment, Health, and Sports*.
- **Top Journalistic Feeds**: BBC, New York Times, The Guardian, Deutsche Welle, Al Jazeera, NPR, Reuters, CNBC, Ars Technica, The Verge, Engadget, Wired, MIT Tech Review, ScienceDaily, Phys.org, Nature, NASA, MarketWatch, and Harvard Health.
- **Search & Filter**: Search news articles by title or keyword.
- **AI Analysis**: One-tap "Summarize in Chat" button sends full article text and source metadata to the AI for key takeaways.

### 🔊 14. Voice & Read-Aloud Studio
- **Native Android TTS**: Integrated Text-To-Speech engine with customizable speech rate, pitch, and voice locales.
- **Audio Playback Controls**: Play, pause, restart, and clear audio playback with real-time word-by-word visual reading highlights.

### 📁 15. Scoped Storage & Artifacts Manager
- **Zero Storage Permission**: Fully compliant with modern Android Scoped Storage rules on Android 10+ (API 29 to 35).
- **MediaStore Persistence**: Exports files cleanly into standard user-accessible directories:
  - Images ➔ `Pictures/OmniChat/`
  - Audio ➔ `Music/OmniChat/`
  - Videos ➔ `Movies/OmniChat/`
  - Documents & Code ➔ `Download/OmniChat/`

---

## 🏗️ Architecture & Project Directory Structure

OmniChat strictly follows the **Android Recommended Architecture (MVVM)**, utilizing unidirectional data flow (`UDF`), reactive `StateFlow` streams, and clean separation between data layers and UI components:

```
app/src/main/java/com/example/
├── data/
│   ├── local/
│   │   ├── AppDatabase.kt              # Room Database v3 with KSP schemas
│   │   ├── ProductivityDao.kt          # Reactive CRUD DAO for Passwords, Notes, Calendar
│   │   ├── RagDao.kt                   # Vector chunk & document persistence DAO
│   │   └── PreferencesManager.kt       # API keys, active providers, and theme state
│   ├── model/
│   │   ├── ChatModels.kt               # Messages, sessions, attachments, and providers
│   │   ├── ProductivityEntities.kt     # Room entities for Passwords, Notes, Calendar
│   │   └── RagModels.kt                # Chunks, knowledge documents, and embeddings
│   └── remote/
│       ├── AiRoutingService.kt         # Multi-provider client (Gemini, Groq, OpenRouter, Cerebras)
│       ├── FileUtils.kt                # MediaStore scoped storage writer and file parsers
│       ├── InternalKnowledgeSources.kt # 12 curated built-in reference source packs
│       ├── MediaSaver.kt               # Android Scoped Storage media persistence
│       ├── NewsAndSearchService.kt     # 15+ live web search engines & RSS feed services
│       └── RagEngine.kt                # Hybrid cosine vector search + TF-IDF/BM25 rank fusion
├── ui/
│   ├── theme/                          # Material Design 3 ColorSchemes, Shapes, Typography
│   ├── AiLearningScreen.kt             # AI Concept Academy & tutorials
│   ├── AiPersonaScreen.kt              # Persona configuration & system instructions
│   ├── CalculatorScreen.kt             # Dual-mode Scientific & Standard calculator
│   ├── CalendarScreen.kt               # Interactive calendar grid & daily agenda hub
│   ├── ChatMessageItem.kt              # Markdown-rendered chat bubbles with copy & speak
│   ├── ChatScreen.kt                   # Core navigation shell, drawer, and chat container
│   ├── ChatViewModel.kt                # Central state manager, flows, and coroutine scopes
│   ├── CodeStudioScreen.kt             # Code editor & live WebKit HTML runner
│   ├── ImageEditorScreen.kt            # GPU color filters, crop, rotate, and canvas drawing
│   ├── IntelHubScreen.kt               # Tabbed intelligence hub (News, Search, Persona, Learn)
│   ├── MarkdownStudioScreen.kt         # Markdown editor with split live preview
│   ├── MusicStudioScreen.kt            # Beat synthesizer with 3 real-time canvas visualizers
│   ├── NewsScreen.kt                   # Real-time multi-feed global news terminal
│   ├── NotesScreen.kt                  # Color-tagged, categorized notes & ideas vault
│   ├── PasswordSaverScreen.kt          # Zero-trust password vault with strength meter
│   ├── RagKnowledgeScreen.kt           # RAG document manager, search sandbox & source library
│   ├── ReadAloudScreen.kt              # Text-to-speech voice studio
│   ├── StudioScreen.kt                 # Creative hub container (Image, Code, Music, MD, Video)
│   ├── VideoStudioScreen.kt            # AI Video motion generator
│   └── WebSearchScreen.kt              # Live web grounding research terminal
└── MainActivity.kt                     # Single Activity with enableEdgeToEdge()
```

---

## 🔒 Security, Privacy & Google Play Policy Compliance

- **No Broad Storage Permissions**: The app strictly complies with modern Android and Google Play policies. It requests zero storage permissions (`READ_EXTERNAL_STORAGE` and `WRITE_EXTERNAL_STORAGE` are omitted). File picking uses the zero-permission **Android Photo Picker** (`PickVisualMedia`) and system document picker (`OpenDocument`).
- **On-Device Data Retention**: Passwords, notes, and local knowledge vectors remain strictly inside the local encrypted app sandbox. No sensitive information is shared with analytics or third-party tracking services.
- **Secure Key Management**: API keys for external providers (Gemini, Groq, OpenRouter, Cerebras) are stored locally using Android `EncryptedSharedPreferences` / private application storage and can be injected securely via build environment variables (`BuildConfig`).
- **Input Sanitization & Output Guardrails**: Search results and RSS feeds are stripped of malicious script tags and HTML formatting via custom regex clean utilities prior to display.

---

## 🚀 Getting Started & Build Instructions

### Prerequisites
- Android Studio Ladybug / Meerkat (or newer)
- Android SDK 35 (Android 15)
- JDK 17 / 21
- Gradle 8.9+

### Compiling and Running
1. Clone the repository:
   ```bash
   git clone <repo-url>
   cd omnichat-android
   ```
2. Build the debug APK:
   ```bash
   gradle assembleDebug
   ```
3. Run unit and local JVM tests:
   ```bash
   gradle :app:testDebugUnitTest
   ```
4. Install to a connected device or streaming emulator:
   ```bash
   gradle installDebug
   ```

---

## 📝 License & Attribution
Designed and engineered for high-performance mobile intelligence. Built on modern Android standards with Jetpack Compose, Material Design 3, and Kotlin Coroutines.
