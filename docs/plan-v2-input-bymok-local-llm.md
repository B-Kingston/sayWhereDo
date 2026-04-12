# V2 Features: Input Method UI, BYOK, and Local LLM Implementation Plan

> **For Hermes:** Use subagent-driven-development skill to implement this plan task-by-task.

**Goal:** Add method selection for note input on both Wear OS and mobile, support BYOK for external AI models, and implement optional on-device local LLM inference for formatting.

**Architecture:** Three parallel workstreams — (A) Input method UI on Wear + Mobile, (B) BYOK external model support, (C) Local LLM on-device inference with hardware checker. Workstreams A and B share a settings/preferences dependency but touch different modules. Workstream C depends on B's abstraction layer.

**Tech Stack:** Jetpack Compose (Material 3 for mobile, Wear Compose Material 3 for watch), OkHttp for BYOK API calls, MediaPipe LLM Inference API for on-device models, Android ActivityManager for hardware detection.

---

## Current State Summary

### What Exists
- **Wear OS:** `VoiceRecordScreen` launches `ACTION_RECOGNIZE_SPEECH` with a mic button, has a "Type reminder" text button that navigates to `KeyboardInputScreen`. No method chooser — mic is the primary, keyboard is a fallback.
- **Mobile:** `ReminderListScreen` has a single mic FAB that navigates to `TranscriptionScreen`. No keyboard input option on the list screen. `TranscriptionScreen` is voice-only.
- **Formatting:** `FormattingProvider` interface with `GeminiFormattingProvider` (hardcoded to Gemini 2.5 Flash Lite) and `RawFallbackProvider`. `FormattingPrompt` builds a system prompt. `GeminiApiClient` is Gemini-specific HTTP client.
- **Settings:** `UserPreferences` stores `apiKey` and `formattingProvider`. Settings screen shows only Gemini API key input. No BYOK options.
- **AppContainer:** Directly instantiates `GeminiFormattingProvider` and wires it into `PipelineOrchestrator`. No provider selection logic.

### What's Missing
- No input method chooser UI on either platform
- No keyboard shortcut from mobile list screen
- No audio streaming from watch to phone for phone-side transcription
- No BYOK support (OpenAI-compatible, custom endpoints)
- No local model support
- No hardware capability detection
- No model download/install flow
- No formatting provider selection in settings

---

## Workstream A: Input Method UI

### Phase A1: Wear OS Input Method Chooser

#### Task A1.1: Create InputMethodSelector composable for Wear OS

**Objective:** Build a compact Wear Compose component that shows keyboard icon + mic icon with dropdown chevron. Tapping mic opens a bottom sheet / scrollable picker offering "On-watch voice" and "Stream to phone".

**Files:**
- Create: `wear/src/main/java/com/example/reminders/wear/ui/component/InputMethodSelector.kt`
- Create: `wear/src/main/res/values/strings.xml` (add new string resources)
- Test: `wear/src/test/java/com/example/reminders/wear/ui/component/InputMethodSelectorTest.kt`

**Step 1: Add string resources**
```xml
<!-- wear/src/main/res/values/strings.xml — add these -->
<string name="input_method_keyboard">Keyboard</string>
<string name="input_method_voice_on_watch">Voice on watch</string>
<string name="input_method_voice_stream_phone">Stream to phone</string>
<string name="input_method_select">Choose input method</string>
```

**Step 2: Create `InputMethod.kt` sealed interface**
```kotlin
// wear/src/main/java/com/example/reminders/wear/ui/component/InputMethod.kt
package com.example.reminders.wear.ui.component

/**
 * Represents the available input methods for creating a reminder on Wear OS.
 */
sealed interface InputMethod {
    /** Opens the on-screen keyboard for text input. */
    data object Keyboard : InputMethod

    /** Uses Wear OS built-in speech recognition. */
    data object VoiceOnWatch : InputMethod

    /** Streams audio to the companion phone for transcription and formatting. */
    data object VoiceStreamToPhone : InputMethod
}
```

**Step 3: Create `InputMethodSelector.kt` composable**
- Row layout: `Icon(Icons.Default.Edit)` | `Icon(Icons.Default.Mic)` + `Icon(Icons.Default.ArrowDropDown)` in a single clickable row
- Tapping mic opens a `LaunchedEffect`-driven state toggle that expands a `Column` with two `Button` items: "Voice on watch" and "Stream to phone"
- Animate expand/collapse with `animateContentSize`
- Material 3 Wear styling throughout
- Tapping keyboard icon directly navigates to keyboard (no expansion)
- Tapping "Voice on watch" triggers the existing `ACTION_RECOGNIZE_SPEECH` flow
- Tapping "Stream to phone" sends a message via `WearDataLayerClient` to start audio streaming on the phone side

**Step 4: Write tests** — Verify that the composable renders all three methods, that the dropdown state toggles correctly.

**Step 5: Commit**
```bash
git add wear/src/main/java/com/example/reminders/wear/ui/component/
git commit -m "feat(wear): add InputMethod sealed interface and InputMethodSelector composable"
```

#### Task A1.2: Refactor Wear VoiceRecordScreen to use InputMethodSelector

**Objective:** Replace the current mic-button + text-button layout with the new `InputMethodSelector`.

**Files:**
- Modify: `wear/src/main/java/com/example/reminders/wear/ui/screen/VoiceRecordScreen.kt`
- Modify: `wear/src/main/java/com/example/reminders/wear/presentation/MainActivity.kt` (add new route for stream-to-phone)
- Test: existing VoiceRecord tests still pass

**Step 1:** Replace the `Column` containing the mic `Button` and text `Button` with `InputMethodSelector`. Wire callbacks:
- `Keyboard` → navigate to `ROUTE_KEYBOARD_INPUT`
- `VoiceOnWatch` → existing `voiceLauncher.launch(intent)` logic
- `VoiceStreamToPhone` → navigate to new `ROUTE_STREAM_TO_PHONE` (stub for now)

**Step 2:** Add `ROUTE_STREAM_TO_PHONE = "stream-to-phone"` to `MainActivity.kt` companion object with a placeholder composable.

**Step 3:** Run tests: `./gradlew :wear:test`

**Step 4:** Commit
```bash
git commit -m "feat(wear): refactor VoiceRecordScreen to use InputMethodSelector with method picker"
```

#### Task A1.3: Implement Watch → Phone Audio Streaming for Transcription

**Objective:** When user selects "Stream to phone", record audio on the watch and send it via Wearable Data Layer Channel API (for large payloads) to the phone, which transcribes it using the phone's speech recognizer.

**Files:**
- Create: `wear/src/main/java/com/example/reminders/wear/audio/AudioRecorder.kt` — records PCM audio on watch
- Create: `wear/src/main/java/com/example/reminders/wear/audio/WearableAudioStreamer.kt` — streams via Channel API
- Create: `wear/src/main/java/com/example/reminders/wear/ui/screen/StreamToPhoneScreen.kt` — UI showing recording state
- Create: `wear/src/main/java/com/example/reminders/wear/ui/viewmodel/StreamToPhoneViewModel.kt`
- Create: `mobile/src/main/java/com/example/reminders/wearable/AudioStreamReceiver.kt` — receives and transcribes on phone
- Modify: `wear/src/main/java/com/example/reminders/wear/sync/DataLayerPaths.kt` — add audio channel paths
- Modify: `mobile/src/main/java/com/example/reminders/wearable/WearableListenerServiceImpl.kt` — handle audio messages
- Test: `wear/src/test/java/com/example/reminders/wear/audio/AudioRecorderTest.kt`
- Test: `mobile/src/test/java/com/example/reminders/wearable/AudioStreamReceiverTest.kt`

**Step 1:** Define audio channel paths in `DataLayerPaths.kt`:
```kotlin
const val AUDIO_STREAM_PATH = "/audio-stream"
const val AUDIO_STREAM_START_PATH = "/audio-stream/start"
const val AUDIO_STREAM_END_PATH = "/audio-stream/end"
const val AUDIO_STREAM_RESULT_PATH = "/audio-stream/result"
```

**Step 2:** Implement `AudioRecorder.kt` using `AudioRecord` (not MediaRecorder, for PCM streaming). Record at 16kHz mono 16-bit PCM. Chunk into 32KB payloads.

**Step 3:** Implement `WearableAudioStreamer.kt` using `ChannelClient` for bidirectional streaming.

**Step 4:** Implement `StreamToPhoneScreen.kt` with recording indicator, cancel button, and status text.

**Step 5:** Implement phone-side `AudioStreamReceiver.kt` — receives PCM chunks, writes to temp file, feeds to `AndroidSpeechRecognitionManager` or runs Whisper if available.

**Step 6:** Write unit tests for audio chunking, stream state management.

**Step 7:** Commit
```bash
git commit -m "feat(wear+mobile): implement watch-to-phone audio streaming for transcription"
```

### Phase A2: Mobile Input Method UI

#### Task A2.1: Create mobile AddNoteFab with method picker

**Objective:** Replace the single mic FAB on `ReminderListScreen` with a split FAB — keyboard button (left) + mic button with dropdown chevron (right). The mic dropdown offers "Built-in Android" and "Local models" (if available).

**Files:**
- Create: `mobile/src/main/java/com/example/reminders/ui/component/AddNoteFab.kt`
- Create: `mobile/src/main/java/com/example/reminders/ui/component/MicMethodPicker.kt`
- Modify: `mobile/src/main/java/com/example/reminders/ui/screen/ReminderListScreen.kt`
- Test: `mobile/src/test/java/com/example/reminders/ui/component/AddNoteFabTest.kt`

**Step 1:** Create `MicMethod.kt` sealed interface:
```kotlin
sealed interface MicMethod {
    data object AndroidBuiltIn : MicMethod
    data class LocalModel(val modelId: String, val modelName: String) : MicMethod
}
```

**Step 2:** Create `AddNoteFab.kt` — a `Row` with two `SmallFloatingActionButton`s:
- Left: keyboard icon (`Icons.Default.Edit`) — navigates to keyboard input screen
- Right: mic icon + `Icons.Default.ArrowDropDown` — toggles the method picker

**Step 3:** Create `MicMethodPicker.kt` — a `DropdownMenu` anchored to the mic FAB:
- "Android speech recognition" (always shown)
- "Whisper (local)" — shown only if a local model is installed (from Workstream C state)
- Each item has an icon and label
- Material 3 styled, fluid animation

**Step 4:** Update `ReminderListScreen` to use `AddNoteFab` instead of the single mic FAB. Add `onKeyboardInput` and `onMicMethodSelected` callbacks.

**Step 5:** Add a keyboard input route to `MainActivity.kt` — reuse or create a `KeyboardInputScreen` for mobile (similar to wear's but Material 3).

**Step 6:** Write tests for picker state and callback behavior.

**Step 7:** Commit
```bash
git commit -m "feat(mobile): add AddNoteFab with keyboard + mic method picker dropdown"
```

#### Task A2.2: Create mobile KeyboardInputScreen

**Objective:** Add a full-screen text input screen for mobile where users can type a reminder directly.

**Files:**
- Create: `mobile/src/main/java/com/example/reminders/ui/screen/KeyboardInputScreen.kt`
- Create: `mobile/src/main/java/com/example/reminders/ui/viewmodel/KeyboardInputViewModel.kt`
- Modify: `mobile/src/main/java/com/example/reminders/MainActivity.kt` — add route
- Modify: `mobile/src/main/res/values/strings.xml` — add strings
- Test: `mobile/src/test/java/com/example/reminders/ui/viewmodel/KeyboardInputViewModelTest.kt`

**Step 1:** Create `KeyboardInputScreen.kt` with Material 3 `OutlinedTextField`, multiline, max 4 lines. "Save" button. Sends text through the `PipelineOrchestrator` (formatting + geocoding + save).

**Step 2:** Create `KeyboardInputViewModel.kt` that wraps `PipelineOrchestrator.processTranscript()`.

**Step 3:** Wire into `MainActivity.kt` navigation.

**Step 4:** Write tests for the ViewModel (happy path, empty input, pipeline error).

**Step 5:** Commit
```bash
git commit -m "feat(mobile): add KeyboardInputScreen for typed reminder entry"
```

---

## Workstream B: BYOK External Model Support

### Phase B1: Abstract Formatting Provider

#### Task B1.1: Create OpenAI-compatible API client

**Objective:** Build a generic HTTP client that can call any OpenAI-compatible API endpoint (base URL + API key + model name). This supports OpenAI, Anthropic (via proxy), Groq, Together AI, Ollama (local), LM Studio, vLLM, etc.

**Files:**
- Create: `mobile/src/main/java/com/example/reminders/network/OpenAiCompatibleClient.kt`
- Create: `mobile/src/test/java/com/example/reminders/network/OpenAiCompatibleClientTest.kt`

**Step 1:** Implement `OpenAiCompatibleClient`:
```kotlin
/**
 * Generic HTTP client for any OpenAI-compatible chat completions API.
 *
 * Supports: OpenAI, Groq, Together AI, Fireworks, Ollama, LM Studio, vLLM,
 * Cloudflare Workers AI, and any provider exposing the /v1/chat/completions endpoint.
 *
 * @param baseUrl      The API base URL (e.g. "https://api.openai.com/v1" or "http://localhost:11434/v1").
 * @param defaultModel Default model name to use if not overridden per-request.
 */
class OpenAiCompatibleClient(
    private val baseUrl: String,
    private val defaultModel: String
) {
    suspend fun chatCompletion(
        apiKey: String,
        systemPrompt: String,
        userMessage: String,
        model: String = defaultModel,
        temperature: Float = DEFAULT_TEMPERATURE
    ): String
}
```

- Uses OkHttp (already in deps)
- Sends `POST /chat/completions` with `model`, `messages: [{role: "system", content: ...}, {role: "user", content: ...}]`, `temperature`
- Parses `choices[0].message.content`
- Applies same JSON cleaning as `GeminiApiClient.cleanJsonText()`
- Retry on 429 (same pattern as Gemini client)
- Extract `cleanJsonText()` into a shared utility since both clients need it

**Step 2:** Write tests using `MockWebServer` — verify request format, response parsing, error handling, retry logic.

**Step 3:** Commit
```bash
git commit -m "feat(network): add OpenAI-compatible API client for BYOK support"
```

#### Task B1.2: Create OpenAiFormattingProvider

**Objective:** Implement `FormattingProvider` that uses the `OpenAiCompatibleClient`.

**Files:**
- Create: `mobile/src/main/java/com/example/reminders/formatting/OpenAiFormattingProvider.kt`
- Test: `mobile/src/test/java/com/example/reminders/formatting/OpenAiFormattingProviderTest.kt`

**Step 1:** Implement:
```kotlin
class OpenAiFormattingProvider(
    private val apiClient: OpenAiCompatibleClient,
    private val apiKeyProvider: suspend () -> String
) : FormattingProvider {
    override suspend fun format(transcript: String): FormattingResult {
        // Same pattern as GeminiFormattingProvider but using OpenAiCompatibleClient
    }
}
```

**Step 2:** Reuse `FormattingPrompt.build()` for the system prompt — it's model-agnostic already.

**Step 3:** Write tests with MockWebServer.

**Step 4:** Commit
```bash
git commit -m "feat(formatting): add OpenAiFormattingProvider for BYOK external models"
```

#### Task B1.3: Add BYOK settings UI

**Objective:** Add a "Custom AI Model" section in Settings where users can configure an OpenAI-compatible endpoint.

**Files:**
- Create: `mobile/src/main/java/com/example/reminders/ui/component/ByokSettingsSection.kt`
- Modify: `mobile/src/main/java/com/example/reminders/ui/screen/SettingsScreen.kt` — add BYOK section
- Modify: `mobile/src/main/java/com/example/reminders/data/preferences/UserPreferences.kt` — add BYOK prefs
- Modify: `mobile/src/main/res/values/strings.xml` — add strings
- Test: `mobile/src/test/java/com/example/reminders/data/preferences/UserPreferencesTest.kt` (if not exists)

**Step 1:** Add preferences to `UserPreferences.kt`:
```kotlin
val byokBaseUrl: Flow<String> = context.dataStore.data.map { it[BYOK_BASE_URL] ?: "" }
val byokApiKey: Flow<String> = context.dataStore.data.map { it[BYOK_API_KEY] ?: "" }
val byokModelName: Flow<String> = context.dataStore.data.map { it[BYOK_MODEL_NAME] ?: "" }
val formattingBackend: Flow<String> = context.dataStore.data.map {
    it[FORMATTING_BACKEND] ?: "gemini"
}

suspend fun setByokBaseUrl(url: String)
suspend fun setByokApiKey(key: String)
suspend fun setByokModelName(name: String)
suspend fun setFormattingBackend(backend: String) // "gemini", "openai_compatible", "local"
```

**Step 2:** Create `ByokSettingsSection.kt`:
- Dropdown/radio to select formatting backend: "Gemini (default)", "Custom API (OpenAI-compatible)", "On-device model"
- When "Custom API" selected, show three fields: Base URL, API Key, Model Name
- "Test connection" button that sends a simple formatting request
- Preset buttons for common providers (Groq, Together, Ollama) that auto-fill URL + default model

**Step 3:** Wire into `SettingsScreen.kt` above the Pro section.

**Step 4:** Commit
```bash
git commit -m "feat(settings): add BYOK configuration section with provider presets"
```

#### Task B1.4: Wire provider selection into AppContainer

**Objective:** Make `AppContainer` create the correct `FormattingProvider` based on user settings.

**Files:**
- Modify: `mobile/src/main/java/com/example/reminders/di/AppContainer.kt`
- Modify: `mobile/src/main/java/com/example/reminders/pipeline/PipelineOrchestrator.kt`
- Test: update existing pipeline tests

**Step 1:** Add lazy providers to `AppContainer`:
```kotlin
val openAiClient: OpenAiCompatibleClient by lazy {
    val baseUrl = runBlocking { userPreferences.byokBaseUrl.first() }
    val model = runBlocking { userPreferences.byokModelName.first() }
    OpenAiCompatibleClient(baseUrl, model)
}

val openAiFormattingProvider: OpenAiFormattingProvider by lazy {
    OpenAiFormattingProvider(
        apiClient = openAiClient,
        apiKeyProvider = { userPreferences.byokApiKey.first() ?: "" }
    )
}
```

**Step 2:** Create a `FormattingProviderFactory` that selects the right provider based on `formattingBackend` preference:
```kotlin
suspend fun createFormattingProvider(): FormattingProvider {
    return when (userPreferences.formattingBackend.first()) {
        "openai_compatible" -> openAiFormattingProvider
        "local" -> localFormattingProvider // Workstream C
        else -> geminiFormattingProvider
    }
}
```

**Step 3:** Update `PipelineOrchestrator` to accept a `FormattingProvider` factory or make it lazy.

**Step 4:** Update existing tests and add new tests for provider selection.

**Step 5:** Commit
```bash
git commit -m "feat(di): wire BYOK provider selection into AppContainer and PipelineOrchestrator"
```

---

## Workstream C: Local LLM On-Device Inference

### Phase C1: Research & Hardware Detection

#### Task C1.1: Create DeviceCapabilityChecker

**Objective:** Detect device hardware capabilities and determine if local LLM inference is feasible.

**Files:**
- Create: `mobile/src/main/java/com/example/reminders/ml/DeviceCapabilityChecker.kt`
- Create: `mobile/src/test/java/com/example/reminders/ml/DeviceCapabilityCheckerTest.kt`

**Step 1:** Implement hardware detection:
```kotlin
/**
 * Checks device hardware capabilities for on-device LLM inference.
 *
 * Evaluates: total RAM, available storage, CPU cores, GPU/NPU presence
 * via Android's [ActivityManager.MemoryInfo], [StatFs], and
 * [android.os.Build] properties.
 */
data class DeviceCapabilities(
    val totalRamMb: Long,
    val availableStorageMb: Long,
    val cpuCores: Int,
    val hasNpu: Boolean,       // Android 12+ Build.SOC_MODEL or NPU detection
    val supportedAbis: List<String>,  // arm64-v8a, x86_64, etc.
    val androidVersion: Int,
    val isRecommended: Boolean,  // Green light for 4B quantized model
    val isMinimum: Boolean       // Yellow light for 2B quantized model
)

class DeviceCapabilityChecker(private val context: Context) {
    fun check(): DeviceCapabilities
}
```

**Thresholds:**
- **Recommended (green circle):** ≥8GB RAM, ≥4GB free storage, arm64-v8a, Android 12+
  - Supports: Gemma 2 2B Q4, Phi-3 mini 3.8B Q4, Qwen2.5-1.5B Q4
- **Minimum (yellow circle):** ≥6GB RAM, ≥2GB free storage, arm64-v8a
  - Supports: Gemma 2 2B Q4 (slower), TinyLlama 1.1B Q4
- **Not supported (red circle):** <6GB RAM or no arm64

**Step 2:** Write tests with mocked `ActivityManager` and `StatFs`.

**Step 3:** Commit
```bash
git commit -m "feat(ml): add DeviceCapabilityChecker for local LLM hardware detection"
```

#### Task C1.2: Add Local Model settings section with capability indicator

**Objective:** Add an "On-Device AI" section in Settings showing the device capability check result and model management options.

**Files:**
- Create: `mobile/src/main/java/com/example/reminders/ui/component/LocalModelSettingsSection.kt`
- Modify: `mobile/src/main/java/com/example/reminders/ui/screen/SettingsScreen.kt`
- Test: `mobile/src/test/java/com/example/reminders/ui/component/LocalModelSettingsSectionTest.kt`

**Step 1:** Create `LocalModelSettingsSection.kt`:
- Green/yellow/red circle indicator at the top with device capability text
- "Download model" button (enabled only if device supports it)
- Model selector dropdown (Gemma 2 2B, Phi-3 Mini, Qwen2.5-1.5B)
- Download progress indicator
- "Delete model" button when downloaded
- Model size display (e.g., "~1.4 GB")
- "Learn more" expandable text explaining what local models do

**Step 2:** Wire into SettingsScreen between BYOK and Pro sections.

**Step 3:** Commit
```bash
git commit -m "feat(settings): add local model section with device capability indicator"
```

### Phase C2: Local Model Runtime

#### Task C1.3: Add MediaPipe LLM Inference dependency

**Objective:** Add the MediaPipe LLM Inference API library to the project.

**Research findings:**
- **MediaPipe LLM Inference API** (`com.google.mediapipe:tasks-genai`) — Google's official on-device LLM runtime for Android
- Supports Gemma 2, Gemma 2.5, Phi-3, and other models via `.tflite` or `.bin` format
- Uses optimized delegates: GPU (OpenGL), NPU (via NNAPI delegate), CPU fallback
- Structured output via options configuration
- This is the recommended approach for production — Google maintains it, it's well-integrated with Android

**Files:**
- Modify: `gradle/libs.versions.toml` — add mediapipe-genai version
- Modify: `mobile/build.gradle.kts` — add dependency

**Step 1:** Add to `libs.versions.toml`:
```toml
mediapipe-genai = "0.10.14"

[libraries]
mediapipe-tasks-genai = { group = "com.google.mediapipe", name = "tasks-genai", version.ref = "mediapipe-genai" }
```

**Step 2:** Add to `mobile/build.gradle.kts` dependencies:
```kotlin
implementation(libs.mediapipe.tasks.genai)
```

**Step 3:** Sync and verify build: `./gradlew :mobile:assembleDebug`

**Step 4:** Commit
```bash
git commit -m "feat(deps): add MediaPipe LLM Inference API dependency"
```

#### Task C2.1: Create LocalModelManager for download and lifecycle

**Objective:** Manage local model download, storage, and lifecycle.

**Files:**
- Create: `mobile/src/main/java/com/example/reminders/ml/LocalModelManager.kt`
- Create: `mobile/src/main/java/com/example/reminders/ml/ModelInfo.kt`
- Create: `mobile/src/test/java/com/example/reminders/ml/LocalModelManagerTest.kt`

**Step 1:** Define `ModelInfo.kt`:
```kotlin
/**
 * Metadata for a downloadable on-device LLM model.
 */
data class ModelInfo(
    val id: String,           // e.g., "gemma2-2b-q4"
    val name: String,         // e.g., "Gemma 2 2B (Quantized)"
    val downloadUrl: String,  // URL to download the model file
    val fileSizeBytes: Long,  // Expected file size
    val minRamMb: Long,       // Minimum RAM requirement
    val isRecommended: Boolean // Whether device checker gives green light
)

object AvailableModels {
    val GEMMA_2_2B_Q4 = ModelInfo(
        id = "gemma2-2b-q4",
        name = "Gemma 2 2B (Q4)",
        downloadUrl = "https://storage.googleapis.com/mediapipe-models/gemma2/text_classification/gemma2-2b-it-q4/float32/1/gemma2-2b-it-q4.task",
        fileSizeBytes = 1_500_000_000L, // ~1.4 GB
        minRamMb = 6000,
        isRecommended = true
    )
    // Add more models as they become available
}
```

**Step 2:** Implement `LocalModelManager.kt`:
```kotlin
class LocalModelManager(private val context: Context) {
    /** Flow emitting download progress (0.0 to 1.0) or null if idle. */
    val downloadProgress: Flow<Float?>

    /** Currently downloaded model ID, or null. */
    val downloadedModelId: Flow<String?>

    /** Check if a specific model is downloaded. */
    fun isModelDownloaded(modelId: String): Boolean

    /** Start downloading a model. Emits progress. */
    suspend fun downloadModel(modelInfo: ModelInfo, onProgress: (Float) -> Unit)

    /** Delete a downloaded model. */
    suspend fun deleteModel(modelId: String)

    /** Get the file path for a downloaded model. */
    fun getModelPath(modelId: String): File?
}
```

- Store models in `context.filesDir/models/`
- Download with OkHttp (already in deps), track progress
- Verify file hash after download (SHA-256)
- Use `DownloadManager` or direct OkHttp streaming

**Step 3:** Write tests for download tracking, file management, hash verification.

**Step 4:** Commit
```bash
git commit -m "feat(ml): add LocalModelManager for model download and lifecycle"
```

#### Task C2.2: Create LocalFormattingProvider

**Objective:** Implement `FormattingProvider` that runs inference using MediaPipe's LLM Inference API.

**Files:**
- Create: `mobile/src/main/java/com/example/reminders/ml/LocalFormattingProvider.kt`
- Test: `mobile/src/test/java/com/example/reminders/ml/LocalFormattingProviderTest.kt`

**Step 1:** Implement:
```kotlin
/**
 * [FormattingProvider] that runs a local LLM on-device using
 * MediaPipe's LLM Inference API.
 *
 * Loads the model from [LocalModelManager], applies the formatting
 * prompt via [FormattingPrompt], and parses the JSON output.
 */
class LocalFormattingProvider(
    private val modelManager: LocalModelManager,
    private val modelInfo: ModelInfo
) : FormattingProvider {

    private var session: LlmInference? = null

    override suspend fun format(transcript: String): FormattingResult {
        val model = ensureModelLoaded()
        val prompt = FormattingPrompt.build()
        val fullPrompt = "$prompt\n\nUser: $transcript\n\nAssistant:"

        val response = withContext(Dispatchers.Default) {
            model.generateResponseAsync(fullPrompt)
        }

        return parseReminders(response.textChunk, transcript)
    }

    private suspend fun ensureModelLoaded(): LlmInference {
        // Load from modelManager.getModelPath() if not already loaded
    }

    fun close() {
        session?.close()
        session = null
    }
}
```

**Key considerations:**
- Use `LlmInference` from MediaPipe with GPU delegate if available, CPU fallback
- Set `maxTokens` to 512 (plenty for reminder JSON output)
- Set `temperature` to 0.1 for deterministic output
- Reuse the same `FormattingPrompt` — it works for any LLM
- Parse output with same JSON extraction logic as other providers
- Model stays loaded in memory for the app lifecycle (close on app destroy)

**Step 2:** Extract shared JSON parsing into a `FormattingResponseParser` utility used by all providers.

**Step 3:** Write tests with a mock `LlmInference`.

**Step 4:** Commit
```bash
git commit -m "feat(ml): add LocalFormattingProvider using MediaPipe LLM Inference API"
```

#### Task C2.3: Wire local model into provider selection

**Objective:** When user selects "On-device model" as formatting backend, use `LocalFormattingProvider`.

**Files:**
- Modify: `mobile/src/main/java/com/example/reminders/di/AppContainer.kt`
- Modify: `mobile/src/main/java/com/example/reminders/ui/component/LocalModelSettingsSection.kt`

**Step 1:** Add lazy `LocalFormattingProvider` to `AppContainer`:
```kotlin
val localModelManager = LocalModelManager(context)

val localFormattingProvider: LocalFormattingProvider by lazy {
    val modelId = runBlocking { userPreferences.localModelId.first() }
    val modelInfo = AvailableModels.getById(modelId) ?: AvailableModels.GEMMA_2_2B_Q4
    LocalFormattingProvider(localModelManager, modelInfo)
}
```

**Step 2:** Update the `FormattingProviderFactory` to include "local" option.

**Step 3:** When user downloads a model in settings, automatically switch to "local" backend if no other is configured.

**Step 4:** Commit
```bash
git commit -m "feat(di): wire local model provider into formatting pipeline"
```

### Phase C3: Prompt Engineering for Local Models

#### Task C2.4: Create model-specific prompt tuning

**Objective:** Adapt `FormattingPrompt` for smaller models that may need more explicit instructions and simpler examples.

**Files:**
- Modify: `mobile/src/main/java/com/example/reminders/formatting/FormattingPrompt.kt`
- Test: `mobile/src/test/java/com/example/reminders/formatting/FormattingPromptTest.kt`

**Step 1:** Add a `buildForLocalModel()` variant:
```kotlin
fun buildForLocalModel(currentDate: LocalDate = LocalDate.now()): String {
    // Shorter prompt, fewer examples, more explicit JSON structure
    // Local models benefit from:
    // - Fewer examples (less context = more room for output)
    // - More explicit schema description
    // - "Think step by step" prefix for complex extractions
    // - No ambiguous instructions
}
```

**Key differences for local 2B-4B models:**
- 1-2 examples instead of 5
- Explicit schema repeated in the instruction
- Shorter system prompt (~800 tokens vs ~1200 tokens)
- Add "Return ONLY valid JSON" emphasis
- Consider chain-of-thought for complex inputs

**Step 2:** Update `LocalFormattingProvider` to use `buildForLocalModel()`.

**Step 3:** Add tests verifying prompt structure and content.

**Step 4:** Commit
```bash
git commit -m "feat(formatting): add model-specific prompt tuning for local LLMs"
```

---

## Shared Tasks

### Task S1: Extract shared JSON response parser

**Objective:** All three formatting providers (Gemini, OpenAI-compatible, Local) parse the same JSON response format. Extract shared parsing into a utility.

**Files:**
- Create: `mobile/src/main/java/com/example/reminders/formatting/FormattingResponseParser.kt`
- Modify: `mobile/src/main/java/com/example/reminders/formatting/GeminiFormattingProvider.kt`
- Modify: `mobile/src/main/java/com/example/reminders/formatting/OpenAiFormattingProvider.kt`
- Modify: `mobile/src/main/java/com/example/reminders/ml/LocalFormattingProvider.kt`
- Test: `mobile/src/test/java/com/example/reminders/formatting/FormattingResponseParserTest.kt`

**Step 1:** Extract `parseReminders()` and `parseSingleReminder()` from `GeminiFormattingProvider` into `FormattingResponseParser`:
```kotlin
object FormattingResponseParser {
    fun parse(jsonText: String, rawTranscript: String): FormattingResult
    fun parseSingleReminder(obj: JsonObject): ParsedReminder
    fun cleanJsonText(text: String): String  // from GeminiApiClient
}
```

**Step 2:** Update all three providers to use the shared parser.

**Step 3:** Move `cleanJsonText` from `GeminiApiClient` to `FormattingResponseParser`.

**Step 4:** Write comprehensive tests for the parser (valid JSON, malformed, partial, empty, code fences, trailing commas).

**Step 5:** Commit
```bash
git commit -m "refactor(formatting): extract shared FormattingResponseParser utility"
```

### Task S2: Update AGENTS.md with new architecture

**Objective:** Document the new formatting provider architecture and local model system.

**Files:**
- Modify: `AGENTS.md`

**Step 1:** Update the Pipeline section to document:
- Three formatting backends: Gemini (default), OpenAI-compatible (BYOK), Local (on-device)
- Provider selection flow via `FormattingProviderFactory`
- Local model download and management
- Audio streaming from watch to phone

**Step 2:** Update Critical Rules to remove "Do not add on-device LLM deps" (now we have them).

**Step 3:** Commit
```bash
git commit -m "docs: update AGENTS.md with BYOK and local LLM architecture"
```

---

## Execution Order

### Parallel Tracks

```
Track 1 (A1): Wear Input Method UI     [Independent]
Track 2 (A2): Mobile Input Method UI   [Independent]
Track 3 (B):  BYOK External Models     [Independent]
Track 4 (C):  Local LLM Inference      [Depends on B1.1-B1.2 for shared parser]
```

### Recommended Agent Assignment

| Agent | Branch | Tasks | Est. Scope |
|-------|--------|-------|-----------|
| Agent 1 | `feature/wear-input-methods` | A1.1, A1.2, A1.3 | 3 tasks |
| Agent 2 | `feature/mobile-input-methods` | A2.1, A2.2, S1 | 3 tasks |
| Agent 3 | `feature/byok-external-models` | B1.1, B1.2, B1.3, B1.4, S1 | 5 tasks |
| Agent 4 | `feature/local-llm` | C1.1, C1.2, C1.3, C2.1, C2.2, C2.3, C2.4 | 7 tasks |

**Note:** Agent 2 and Agent 3 share Task S1. Agent 2 should do it first (simpler scope), or Agent 3 does it and Agent 2 rebases. Alternatively, S1 is done first on main before branching.

### Merge Order

1. S1 (shared parser) → main
2. A1 (Wear input) → main
3. A2 (Mobile input) → main
4. B (BYOK) → main
5. C (Local LLM) → main

After each merge: `./gradlew test && ./gradlew lint && ./gradlew assembleDebug`

---

## Pitfalls

1. **Wear Data Layer payload limits** — `MessageClient.sendMessage()` has a ~100KB limit. For audio streaming, use `ChannelClient` which supports larger streams. Do NOT try to send raw audio via `sendMessage`.

2. **MediaPipe model format** — MediaPipe LLM Inference API uses `.task` files, not GGUF. The model must be in MediaPipe's format. Gemma 2 models are available pre-converted from Google's storage.

3. **Memory pressure from local models** — Loading a 2B Q4 model uses ~1.5GB RAM. Must unload when app goes to background. Listen for `onTrimMemory()` and close the inference session.

4. **Watch speech recognition reliability** — `ACTION_RECOGNIZE_SPEECH` varies by OEM. Samsung Galaxy Watch 4/5 are unreliable. Always check `resolveActivity()` and provide alternatives. The stream-to-phone option is specifically for this case.

5. **OpenAI-compatible API differences** — Some providers (Ollama) don't require an API key. The client should handle empty API key gracefully. Some providers use different response formats — the parser must be robust.

6. **Model download UX** — 1.4GB downloads over mobile data are a bad experience. Show file size prominently, prefer Wi-Fi check, and support resume on failure.

7. **FormattingPrompt compatibility** — The existing prompt was designed for Gemini. Smaller local models may struggle with the full prompt. The `buildForLocalModel()` variant addresses this but must be tested with actual model output.

8. **Thread safety** — `LocalFormattingProvider` loads a model into memory. Multiple concurrent calls must be serialized. Use a `Mutex` or `SingleThreadDispatcher`.

---

## Verification

### Per-task
- `./gradlew test` passes
- `./gradlew lint` passes (no new warnings)
- `./gradlew assembleDebug` succeeds

### Integration
- Full CI gate: `./gradlew test && ./gradlew lint && ./gradlew assembleDebug`
- Manual: Test on Wear OS emulator (voice + keyboard + stream)
- Manual: Test BYOK with a real OpenAI-compatible endpoint (e.g., Groq free tier)
- Manual: Test local model download and formatting on a capable device

### Code Quality
- All new code has KDoc comments
- All new public functions have tests
- No magic numbers
- Follow existing code style (4-space indent, descriptive names)
- All strings in strings.xml
