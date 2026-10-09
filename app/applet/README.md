# 🚀 OmniChat — The All-in-One Intelligent AI & Productivity Studio

<p align="center">
  <strong>Next-Generation Multi-Provider AI Assistant, Offline Productivity Hub, Media Studios & Knowledge Engines for Android</strong>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android%2014%2B%20(API%2024--36)-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android" />
  <img src="https://img.shields.io/badge/Language-Kotlin%202.0-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin" />
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose%20%2B%20M3-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white" alt="Compose" />
  <img src="https://img.shields.io/badge/Database-Room%20(SQLite%20KSP)-00599C?style=for-the-badge&logo=sqlite&logoColor=white" alt="Room" />
  <img src="https://img.shields.io/badge/Architecture-MVVM%20%2B%20Clean-00C853?style=for-the-badge" alt="MVVM" />
</p>

---

## 🌟 The Vision: What Was Built & Why It's Useful

### 💡 The Problem with Modern Mobile Apps
Modern mobile workflows are notoriously fragmented. A user typically switches between **5 to 10 separate apps**:
1. An AI chatbot for questions and coding help.
2. A password manager for account credentials.
3. A notes app for jotting down thoughts and checklists.
4. A calculator for standard and scientific arithmetic.
5. A calendar for scheduling events and managing tasks.
6. A photo editor for filters, cropping, and annotations.
7. A markdown reader to inspect `.md` technical documentation.
8. A code playground to test snippets or preview HTML pages.
9. A music player to listen to audio, adjust equalizer bands, and visualize beats.
10. A news app and web search engine for global updates.

Each standalone app requires its own setup, often demands paid subscriptions, bombards users with ads, clutters device storage, and **none of them talk to each other**.

### 🎯 The Solution: OmniChat Unified Intelligent Ecosystem
**OmniChat** bridges this entire spectrum into a **single, unified, privacy-first Android powerhouse**. It is not just an AI chat interface—it is a **complete multi-tool operating environment** where every native tool is seamlessly integrated with intelligent AI capabilities:
- **Math Problem?** Solve it directly in the **Smart Calculator**, then tap **"AI Step-by-Step"** for full mathematical proofs and derivations.
- **Daily Agenda?** Schedule events in the **Interactive Calendar**, then tap **"AI Plan Day"** to optimize your agenda, buffer times, and task priorities.
- **Credential Storage?** Save accounts in the **Encrypted Password Vault**, generate cryptographic passwords, and run an **"AI Security Audit"** against NIST standards.
- **Jotting Ideas?** Take notes in the **Notes Hub**, format checklists, and tap **"AI Polish"** or **"AI Summarize"** to transform rough drafts into professional documents.
- **Web & Code Development?** Upload `.html` or `.js` in the **Code Studio**, preview it live in an interactive sandbox with console logs, and run code with simulated AI diagnostics.
- **Audio & Visual Creation?** Tweak tunes with a 3-band equalizer and 3 beat visualizers in the **Music Studio**, and retouch images with filters and drawing pens in the **Photo Editor**.
- **Private Data & Research?** Search **16 free knowledge engines** (Wikipedia, PubMed NCBI, DuckDuckGo, DEV.to, Internet Archive) and ingest local documents into a private on-device **RAG Vector Hub**.

---

## 🛠️ Complete Technical Stack

| Tier | Technologies & Libraries | Key Responsibilities |
| :--- | :--- | :--- |
| **Language & Runtime** | **Kotlin 2.0+**, Java 11, Android SDK 24–36 | Modern, type-safe, expressive Android codebase with strict null safety |
| **UI Framework** | **Jetpack Compose**, Compose BOM, Material Design 3 (M3) | Declarative reactive UI, edge-to-edge window insets, fluid 60/120 FPS animations |
| **Icons & Symbols** | `androidx.compose.material.icons.extended`, `automirrored` | Comprehensive Material Symbols library with full directional mirroring |
| **State & Architecture** | **MVVM (Model-View-ViewModel)**, `ViewModel`, Coroutines, `StateFlow`, `collectAsStateWithLifecycle` | Unidirectional Data Flow (UDF), lifecycle-aware reactive UI states |
| **Local Persistence** | **AndroidX Room (KSP)**, SQLite, `PreferencesManager` | Reactive `Flow<List<T>>` database for chats, sessions, passwords, notes, calendar events, RAG vector chunks |
| **AI Models & Engines** | **Multi-Provider Hub**: Google Gemini 2.5/Flash, Groq, OpenRouter, Ollama, Custom REST | Streaming responses, multimodal vision reasoning, system prompt injection |
| **Audio & DSP** | Native Android `MediaPlayer`, `PlaybackParams`, `AudioSynthesizer` | Custom song playback, real-time speed (0.5x–2.0x), pitch tuning, procedural audio waveforms |
| **Graphics & Vision** | Android `Bitmap`, `ColorMatrix`, `Matrix`, Jetpack Compose `Canvas` | Hardware-accelerated image filters, freehand brush strokes, 32-band beat spectrums |
| **Web & Sandbox** | Android `WebView`, `WebChromeClient`, `WebViewClient` | Sandboxed HTML5/CSS3/JavaScript in-app execution with `console.log` capture |
| **Vector RAG Engine** | On-Device Custom Cosine Similarity Embedding Engine | Semantic chunking, cosine distance retrieval, zero-cloud private grounding |
| **Networking & RSS** | OkHttp, HttpURLConnection, XML DOM/SAX Parsers | Zero-API-key news streams (BBC, NYT, Guardian, CNBC, Al Jazeera, TechCrunch, NASA) |
| **Security & Privacy** | AES/Key Store patterns, `SecureRandom`, Zero Broad Storage Permissions | Google Play compliance via Android Photo Picker (`PickVisualMedia`) and Document Picker |
| **Testing** | **Robolectric**, JUnit 4, Kotlinx Coroutines Test | Fast JVM-based testing for Critical User Journeys (CUJs) and reactive Flows |

---

## 📋 Comprehensive Point-by-Point Feature Breakdown

### 🤖 1. Multi-Provider AI Chat Engine
- **Multi-Provider Architecture**: Seamlessly switch between Google Gemini, Groq, OpenRouter, and custom endpoints without losing conversation context.
- **Multimodal File & Vision Attachments**: Attach photos, screenshots, code files, and documents for vision analysis.
- **Artifacts Extraction Engine**: Automatically detects code, markdown, diagrams, and HTML in AI replies and extracts them into dedicated, launchable artifacts.
- **Session Management**: Create, rename, search, and delete multiple independent conversation threads with persistent Room storage.
- **Read Aloud TTS**: One-tap text-to-speech speaker button on any AI message with adjustable pitch and playback speed.
- **Regenerate & Continue**: Expand responses or regenerate answers with alternative temperatures.

---

### 🔒 2. Password Saver & Secure Vault (`PasswordSaverScreen`)
- **Encrypted Local Credentials**: Securely stores Service Names, Usernames/Emails, Passwords, Categories, Website URLs, and Security Notes.
- **Random Strong Password Generator**:
  - Precision length slider from 8 to 32 characters.
  - Toggles for Uppercase (A-Z), Numbers (0-9), and Special Symbols (`!@#$%^&*`).
  - One-tap insertion into the active account form.
- **Dynamic Password Strength Meter**:
  - Live algorithmic entropy scoring (Weak, Fair, Moderate, Strong, Very Strong) with color-coded progress indicators.
- **Visibility Toggle & Instant Clipboard Copy**:
  - Masked password field (`••••••••••••`) with one-tap eye reveal.
  - Dedicated copy buttons for password and username with toast feedback.
- **Category Organization**: Filter by *Logins, Finance, Social, Work, Entertainment, Other*.
- **Live Search**: Instant keyword filtering across service names, usernames, and notes.
- **AI Security Audit**: One-click prompt that forwards credential metadata to AI for modern NIST guideline analysis.

---

### 📝 3. Notes Hub & Journal (`NotesScreen`)
- **Reactive Room Persistence**: Fast on-device database tracking note creation and edit timestamps.
- **6-Color Accent Tagging**: Visual color tags (Blue, Emerald, Amber, Pink, Violet, Cyan) for instant visual scanning.
- **Pin to Top**: Keep critical notes and active journals pinned above the list.
- **Category Folders**: Organize thoughts into *Personal, Work, Ideas, Study, and To-Do*.
- **Quick-Insert Formatting Toolbar**:
  - Insert checklist task items (`- [ ] `) with one tap.
  - Insert bullet points (`• `).
- **Word & Character Counting**: Live word counter to track composition progress.
- **AI Writing Assistant**:
  - **AI Polish**: Structures unstructured thoughts into clean bullet points.
  - **AI Summarize**: Extracts core takeaways and action items.

---

### 🧮 4. Smart Calculator & Unit Converter (`CalculatorScreen`)
- **Dual Standard & Scientific Modes**:
  - **Standard**: Complete arithmetic (`+`, `−`, `×`, `÷`, `%`), parentheses `( )`, sign toggle `±`, clear `AC`, backspace `⌫`.
  - **Scientific**: Trigonometry (`sin`, `cos`, `tan`), Logarithms (`log`, `ln`), Square root (`√`), Powers (`xʸ`), Constants (`π`, `e`).
  - **Angle Mode**: One-tap toggle between Radians (`RAD`) and Degrees (`DEG`).
- **Live Expression Evaluation**: Real-time evaluation display beneath your expression as you type.
- **Interactive Calculation History Tape**:
  - Maintains a chronological log of expressions and solutions.
  - Tap any history entry to reload the expression or copy the solution.
- **Multi-Unit Converter**:
  - **Length**: Meters, Feet, Kilometers, Miles.
  - **Weight**: Kilograms, Pounds.
  - **Temperature**: Celsius, Fahrenheit.
  - **Speed**: km/h, mph.
- **AI Step-by-Step Solver**: Sends equations to OmniChat for comprehensive algebraic and calculus derivations.

---

### 📅 5. Calendar & Schedule Planner (`CalendarScreen`)
- **Interactive Monthly Grid**:
  - Smooth month-to-month navigation with `< Previous` and `Next >` headers.
  - **"Today" Shortcut**: Instantly returns to the current date.
  - Current day highlighted with dynamic primary border and container colors.
- **Event Dots Indicators**: Visual indicator dots on calendar grid cells that contain scheduled events.
- **Daily Schedule Timeline**:
  - Chronological list of events for the selected date sorted by start time.
  - Event start and end time displays.
  - Task completion checkboxes with animated strike-through styling.
- **Event Scheduling Dialog**:
  - Event title, start time, end time, category (*Meeting, Task, Birthday, Reminder, Personal*), color marker, and description.
- **AI Daily Planner**: One-tap **"AI Plan Day"** button that sends your daily agenda to OmniChat to schedule breaks, buffer times, and productivity optimizations.

---

### 🎨 6. Photo & Image Editor (`ImageEditorScreen`)
- **Zero-Permission Photo Picker**: Implements Android's modern `ActivityResultContracts.PickVisualMedia` with zero storage permission requests.
- **Instant Color Filters & Presets**: Vibrant, Black & White, Vintage Sepia, Cyber Cool, Golden Warmth, and Negative/Invert.
- **Precision Adjustment Sliders**:
  - Brightness (-100 to +100).
  - Contrast (0.5x to 2.0x).
  - Saturation (0x to 2.0x).
- **Transform & Crop**:
  - 90° clockwise rotation.
  - Horizontal flipping.
  - Aspect ratio presets: 1:1 Square, 4:3, 16:9 Landscape, 9:16 Portrait.
- **Freehand Pen & Text Overlay**:
  - Smooth touch drawing canvas with color palette selection and brush stroke slider.
  - Undo stroke stack.
  - Customizable text watermark overlay with color and scale controls.
- **Gallery Export & AI Vision**:
  - Save processed images to local storage via `MediaSaver`.
  - Send edited images into AI Chat for vision reasoning.

---

### 📄 7. Markdown Hub & Live Viewer (`MarkdownStudioScreen`)
- **Import Local `.md` Files**: Open and render any Markdown document from your device.
- **Three Viewing Modes**:
  - **Rendered Preview**: Beautifully styles `# Headings`, `**bold**`, `*italics*`, `> blockquotes`, `- [ ] tasks`, tables, and code blocks.
  - **Raw Monospace Editor**: Clean text editor with an interactive Markdown toolbar (Bold, Italic, Code, Block, List, Quote, Table, Link).
  - **Live Split View**: Side-by-side editing on top with real-time rendered preview below.
- **Live Document Metrics**: Word count, character count, and estimated reading time.
- **Built-in Starter Templates**: Instant templates for READMEs, Project Specs, Task Lists, and Prompt Guides.
- **Export & Share**: Save edited files to Downloads or send to AI Chat for proofreading.

---

### 💻 8. Code Studio & In-Site HTML Sandbox (`CodeStudioScreen`)
- **Upload Any Code File**: Open `.html`, `.js`, `.py`, `.kt`, `.java`, `.cpp`, `.ts`, `.css`, or `.json`.
- **In-Site Interactive HTML/JS Webview Sandbox**:
  - Sandboxed `WebView` executing client-side HTML, CSS, and JavaScript.
  - Full touch interactivity and dynamic animations.
  - **Console Message Interception**: Live `console.log` capture and display.
- **AI Execution Terminal**:
  - AI code runner simulating execution for Python, Kotlin, C++, and JavaScript.
  - Displays stdout, trace logs, return codes, and execution duration.
- **AI Diagnostics & Bug Inspector**:
  - Explains code line-by-line, identifies bugs, and suggests security and performance fixes.
- **Code Actions**: Copy code, download file, or export snippet to AI Chat.

---

### 🎵 9. Music Studio & Beat Visualizer (`MusicStudioScreen`)
- **Custom Song Upload**: Upload `.mp3`, `.wav`, `.ogg`, or `.m4a` files using native Android `MediaPlayer`.
- **Procedural Soundtrack Presets**: Built-in synthwave, lo-fi chill, meditation resonance, and space pad soundscapes.
- **Precision Tune Adjustments**:
  - **Playback Speed**: Adjust speed in real-time from 0.5x to 2.0x.
  - **Pitch Tuning**: Micro-tune pitch shifting.
  - **3-Band Equalizer**: Dedicated Bass, Mid, and Treble sliders (-10 dB to +10 dB).
  - **Volume Boost & Seeking**: Scrub slider with `mm:ss` timestamps, loop toggle, and ±10s skips.
- **3 Dynamic Beat Visualizers**:
  - **32-Band Spectrum Bars**: Frequency bars that bounce and shift color gradients to the rhythm.
  - **Pulsing Beat Radar**: Concentric glowing radar rings synchronized to track BPM.
  - **Waveform Oscilloscope Ribbon**: Smooth animated sine ribbon reacting to volume and audio energy.

---

### 📚 10. Local RAG Knowledge Base (`RagKnowledgeScreen`)
- **On-Device Vector Knowledge Grounding**: Ingest personal PDF, TXT, and Markdown files.
- **Semantic Text Chunking**: Automatic document parsing into vectorized knowledge chunks.
- **Cosine Distance Retrieval**: Local semantic search to retrieve the most relevant chunks before generating AI responses.
- **Zero-Cloud Privacy**: Documents never leave your device for grounding.

---

### 📰 11. Live Global News Feed (`NewsScreen`)
- **Zero API Keys Required**: Real-time RSS feeds parsed from 8 global news agencies:
  - **BBC News** (World, Tech, Business, Science, Entertainment, Health)
  - **The New York Times** (Top Stories, Technology, Science, World)
  - **The Guardian** (World, Tech, Science, Business, Culture, Health)
  - **CNBC** (Markets, Business, Finance, Technology)
  - **Al Jazeera English** (World News)
  - **TechCrunch** (Startups, Venture Capital, Gadgets)
  - **Ars Technica** (Deep Tech, Software, Hardware)
  - **NASA News** (Space Exploration & Discoveries)
- **AI News Summarization**: Tap any article card to generate a bullet-point executive summary.

---

### 🌐 12. Unified Free Search & Knowledge Engines (`WebSearchScreen`)
- **16 Zero-Key Search Providers**:
  - **Wikipedia**: Global encyclopedia summaries.
  - **PubMed / NCBI**: 36M+ biomedical, clinical, and life sciences research publications.
  - **DEV.to Community**: Software engineering articles, tutorials, and guides.
  - **OpenLibrary**: Internet Archive author catalogs and classic literature.
  - **Internet Archive**: Preserved historical books, digital records, and research media.
  - **DuckDuckGo**: Instant web answers and zero-tracking search.
  - **Stack Overflow**: Developer questions and verified solutions.
  - **GitHub**: Open-source repositories and code libraries.
  - **arXiv**: Academic pre-prints in Physics, Math, and Computer Science.
  - Alongside CrossRef, Hacker News, Wikinews, Wikiquote, and Wiktionary.

---

### 🗺️ 13. Free Maps & Travel Guide (`MapExplorerScreen`)
- **OpenStreetMap & Nominatim**: Free global geocoding and location exploration.
- **AI Itinerary Generator**: One-click generation of custom travel itineraries, packing lists, and local recommendations.

---

### 📺 14. YouTube Player & Explorer (`YouTubeScreen`)
- **Sandboxed Video Player**: Search videos and watch directly inside the app without ad tracking.
- **AI Video Summarizer**: Generate bullet-point notes and key takeaways for any video.

---

### 🎓 15. AI Learning Academy (`AiLearningScreen`)
- **Interactive AI Curriculum**: Master LLM architectures, prompt engineering, RAG, agentic workflows, and fine-tuning with hands-on practice.

---

### 🎭 16. AI Persona Creator (`AiPersonaScreen`)
- **Feed AI How to Act**: Configure customized personas (e.g., Code Reviewer, Friendly Tutor, Concise Executive, Creative Writer) with tone sliders for formality, humor, and verbosity.

---

### 🔊 17. Read Aloud Narrator (`ReadAloudScreen`)
- **Text-to-Speech Engine**: Read any text, article, or document aloud with customizable speech rate and voice pitch.

---

## 🏗️ Architecture & Engineering Highlights

```
com.example
├── MainActivity.kt                # Single-activity container with edge-to-edge support
├── data
│   ├── local
│   │   ├── AppDatabase.kt         # Room database (Chats, RAG chunks, Passwords, Notes, Events)
│   │   ├── ChatDao.kt             # Chat sessions and messages DAO
│   │   ├── RagDao.kt              # Document chunks and vector entries DAO
│   │   ├── ProductivityDao.kt     # Passwords, Notes, and Calendar events DAO
│   │   ├── PreferencesManager.kt  # User settings, API keys, and theme toggles
│   │   └── UsageTracker.kt        # Token and quota tracking
│   ├── model
│   │   ├── ChatMessage.kt         # Chat domain entities
│   │   ├── ProductivityEntities.kt# Password, Note, and Calendar Room entities
│   │   ├── RagModels.kt           # Vector and chunk models
│   │   └── AiProvider.kt          # AI provider configurations
│   └── remote
│       ├── AiService.kt           # Multi-provider REST clients
│       ├── NewsAndSearchService.kt# 16 search engines & 8 live RSS news parsers
│       ├── MediaSaver.kt          # Scoped storage media export utility
│       ├── AudioSynthesizer.kt    # Procedural sound synthesis engine
│       └── RagEngine.kt           # On-device vector chunking & cosine similarity
└── ui
    ├── ChatScreen.kt              # Core scaffold, drawer navigation, and chat stream
    ├── ChatViewModel.kt           # Central ViewModel coordinating state and Room DAOs
    ├── PasswordSaverScreen.kt     # Encrypted Password Vault & Generator
    ├── NotesScreen.kt             # Smart Notes Hub with color tags & checklists
    ├── CalculatorScreen.kt        # Standard & Scientific Calculator with converter
    ├── CalendarScreen.kt          # Interactive Monthly Calendar & Event Planner
    ├── ImageEditorScreen.kt       # Photo filter, adjustment, crop & drawing studio
    ├── MarkdownStudioScreen.kt    # Markdown file reader, live editor & split preview
    ├── CodeStudioScreen.kt        # Code runner, diagnostic inspector & HTML sandbox
    ├── MusicStudioScreen.kt       # Audio player, 3-band equalizer & beat visualizer
    ├── IntelHubScreen.kt          # Central scrollable Productivity & Intel tab hub
    ├── StudioScreen.kt            # Creative Multi-Tool Studio tab hub
    ├── NewsScreen.kt              # Real-time news reader
    ├── WebSearchScreen.kt         # Multi-engine search interface
    ├── MapExplorerScreen.kt       # Free Maps & AI itinerary planner
    ├── YouTubeScreen.kt           # Video explorer & summarizer
    ├── RagKnowledgeScreen.kt      # Document vector knowledge store
    ├── AiPersonaScreen.kt         # Persona customization suite
    ├── AiLearningScreen.kt        # AI Academy learning modules
    ├── ReadAloudScreen.kt         # Text-to-speech narration studio
    └── theme                      # Material 3 Color Schemes & Typography
```

---

## 🔒 Security & Google Play Policy Compliance

1. **Zero Broad Storage Permissions**: The app strictly avoids `READ_EXTERNAL_STORAGE` or `MANAGE_EXTERNAL_STORAGE`. All media selection uses the native Android Photo Picker (`ActivityResultContracts.PickVisualMedia`) and system Document Picker.
2. **Local Credential Storage**: All password vault items, notes, and calendar events are stored in a private local SQLite database that never transmits user data to third parties.
3. **No Dynamic Code Loading**: Complies with Google Play safety policies by avoiding external `.dex` or `.so` loading; HTML and JavaScript execution is strictly sandboxed inside an Android `WebView`.
4. **Key Management**: Secrets and API keys are managed safely via `BuildConfig` and AI Studio's environment properties, ensuring zero keys are exposed in git commits.

---

## 🚀 Building & Running

### Requirements
- **Android Studio Ladybug (2024.2+)** or newer
- **JDK 11** or **JDK 17**
- **Android SDK API 36**

### Compile & Test via Command Line
```bash
# Build debug APK
gradle assembleDebug

# Run Robolectric unit tests
gradle :app:testDebugUnitTest
```

---

<p align="center">
  <strong>Built with craftsmanship, clean architecture, and modern Android design principles.</strong>
</p>
