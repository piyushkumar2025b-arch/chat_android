# OmniChat Functionality Bug Report & Remediation Plan

## Executive Summary
A comprehensive audit of the OmniChat workspace revealed several functional defects, navigation traps, UI overflow issues on mobile screens, and disconnected data paths between the Chat interface and secondary tool studios. This document details each bug, root cause analysis, severity, and the remediation implemented.

---

## Identified Bugs

### 1. Dialog Content View Overflow (Mobile Keyboard Clipping)
- **Severity**: High
- **Affected Components**:
  - `PasswordEditorDialog` in `PasswordSaverScreen.kt`
  - `EventEditorDialog` in `CalendarScreen.kt`
  - `NoteEditorDialog` in `NotesScreen.kt`
- **Symptom**: On mobile screens, smaller viewports, or when the on-screen software keyboard appears, the dialog `Column` overflows vertically. The "Save", "Cancel", and secondary action buttons are pushed off-screen and cannot be clicked, trapping the user.
- **Root Cause**: The inner dialog `Column(modifier = Modifier.fillMaxWidth())` was missing `Modifier.verticalScroll(rememberScrollState())`.
- **Fix**: Add `.verticalScroll(rememberScrollState())` to all three dialog content columns.

---

### 2. Sub-Screen Navigation Traps & Missing Back Handlers in Hubs
- **Severity**: High
- **Affected Components**:
  - `IntelHubScreen.kt`
  - `StudioScreen.kt`
  - `PasswordSaverScreen.kt`
  - `CalculatorScreen.kt`
  - `NotesScreen.kt`
  - `CalendarScreen.kt`
  - `StepCounterScreen.kt`
  - `Game2048Screen.kt`
  - `NotificationCenterScreen.kt`
  - `ImageEditorScreen.kt`
- **Symptom**:
  1. Sub-screens embedded in `IntelHubScreen` and `StudioScreen` were called without `onBack`, hiding their top-left back navigation buttons.
  2. If the user had Master PIN enabled on `PasswordSaverScreen`, the lock screen rendered full-screen without an `onBack` button, completely trapping the user.
  3. Pressing the hardware/system back button while on any sub-tab (e.g. Tab 5) immediately exited the entire Hub back to Chat, rather than returning to Tab 0 (Overview).
- **Root Cause**: Missing `BackHandler(enabled = selectedTab != 0) { selectedTab = 0 }` inside `IntelHubScreen` and `StudioScreen`, and missing `onBack = { selectedTab = 0 }` passed into the sub-screen composables.
- **Fix**: Wire `onBack = { selectedHubTab = 0 }` and `onBack = { selectedStudioTab = 0 }` to all child screens, and add `BackHandler` on both Hubs.

---

### 3. Unit Converter Selection Lock & Calculator Backspace Missing
- **Severity**: Medium-High
- **Affected Components**:
  - `CalculatorScreen.kt`
- **Symptom**:
  1. The Unit Converter displayed "Length", "Weight", "Temperature", "Speed", but gave the user **zero controls** to choose `fromUnit` and `toUnit` or swap them (e.g. converting Feet to Meters or Miles to Kilometers was impossible).
  2. The calculator keypad only provided `AC` (All Clear) and lacked a single-character `Backspace` button. Any typographical error in a lengthy mathematical expression forced clearing the entire expression.
- **Root Cause**: Keypad layout omitted `Backspace` despite importing `Icons.AutoMirrored.Filled.Backspace`. Unit converter UI omitted dropdowns/chips for unit selection and swap buttons.
- **Fix**:
  - Add individual unit selection chips / dropdowns for each category (Length, Weight, Temperature, Speed) with expanded units (Kilometers, Miles, Meters, Feet, Centimeters, Inches; Kilograms, Pounds, Grams, Ounces; Celsius, Fahrenheit, Kelvin; km/h, mph, m/s, Knots).
  - Add an animated Swap button (`Icons.Default.SwapHoriz`).
  - Add Backspace button to the calculator keypad with single-character deletion.

---

### 4. Cross-Tool Attachment Data Loss on Navigation
- **Severity**: High
- **Affected Components**:
  - `ChatScreen.kt`
  - `ChatViewModel.kt`
  - `CodeStudioScreen.kt`
  - `MarkdownStudioScreen.kt`
  - `UniversalMediaPlayerScreen.kt`
- **Symptom**: When inspecting an attachment in Chat (e.g., a `.kt` file, `.md` file, or `.mp3`/`.mp4` media) and clicking "Code Studio", "Markdown", or "Media Player", the app navigated to the studio but did not carry over the attachment data. Code Studio displayed generic boilerplate; Markdown Studio displayed default text; Media Player did not queue or play the selected item.
- **Root Cause**: `navigateToSection` only toggled the `AppSection` enum without setting pending state or passing parameters.
- **Fix**:
  - Add `activeCodeSnippet`, `activeMarkdownSnippet`, and `activeMediaItem` to `ChatViewModel`.
  - Provide `openInCodeStudio(code, fileName)`, `openInMarkdown(text)`, and `openInMediaPlayer(uri, name, mime)`.
  - Connect `CodeStudioScreen` (`initialCode`, `initialFileName`), `MarkdownStudioScreen` (`initialContent`), and `UniversalMediaPlayerScreen` (`initialMediaItem`).

---

### 5. Missing AI Chat Connectivity in ImageEditorScreen
- **Severity**: Medium
- **Affected Components**:
  - `ImageEditorScreen.kt`
- **Symptom**: `ImageEditorScreen` defined `onSendImageToChat: ((String) -> Unit)?`, but never displayed a button or invoked the callback.
- **Root Cause**: Action bar omitted the `Icons.Default.Send` action button.
- **Fix**: Add a "Send to AI Chat" action button in the top bar of `ImageEditorScreen` allowing users to prompt AI to analyze their edited image.

---

### 6. YouTube WebView Audio/Video Background Leaks
- **Severity**: Medium
- **Affected Components**:
  - `YouTubeScreen.kt`
- **Symptom**: When navigating away from `YouTubeScreen` while a video was playing inside the embedded WebView, audio continued playing in the background, and the WebView leaked resources.
- **Root Cause**: Lack of `DisposableEffect` to pause, stop, and clean up the WebView instance.
- **Fix**: Add `DisposableEffect` in `InlineYouTubePlayer` calling `onPause()`, `stopLoading()`, and `loadUrl("about:blank")` on dispose.

---

## Verification Strategy
- Run `compile_applet` to confirm clean compilation across all modules.
- Run unit tests to verify mathematical parsing, unit conversion, RAG ingestion, and file inspection helpers.
