# V2 Features: Input Method UI, BYOK, and Local LLM Implementation Plan

> **For Hermes:** Use subagent-driven-development skill to implement this plan task-by-task.

**Goal:** Add method selection for note input on both Wear OS and mobile, support BYOK for any external AI model with structured output (including Gemini as just another provider), implement optional on-device local LLM inference for formatting, and enable standalone watch formatting via direct cloud API calls.

**Architecture:** Three parallel workstreams — (A) Input method UI on Wear + Mobile, (B) BYOK cloud model support + Watch standalone formatting, (C) Local LLM on-device inference with hardware checker. All formatting flows through a unified `FormattingProvider` interface. Provider credentials sync from phone to watch via Wearable Data Layer.

**Tech Stack:** Jetpack Compose (Material 3 for mobile, Wear Compose Material 3 for watch), OkHttp for BYOK API calls, MediaPipe LLM Inference API for on-device models, Android ActivityManager for hardware detection.

---

## Current State Summary

### What Exists
- **Wear OS:** `VoiceRecordScreen` launches `ACTION_RECOGNIZE_SPEECH` with a mic button, has a "Type reminder" text button that navigates to `KeyboardInputScreen`. No method chooser — mic is the primary, keyboard is a fallback. Formatting is deferred to phone via Data Layer.
- **Mobile:** `ReminderListScreen` has a single mic FAB that navigates to `TranscriptionScreen`. No keyboard input option on the list screen. `TranscriptionScreen` is voice-only.
- **Formatting:** `FormattingProvider` interface with `GeminiFormattingProvider` (hardcoded to Gemini 2.5 Flash Lite) and `RawFallbackProvider`. `FormattingPrompt` builds a system prompt. `GeminiApiClient` is Gemini-specific HTTP client.
- **Settings:** `UserPreferences` stores `apiKey` and `formattingProvider`. Settings screen shows only Gemini API key input. No BYOK options.
- **AppContainer:** Directly instantiates `GeminiFormattingProvider` and wires it into `PipelineOrchestrator`. No provider selection logic.

### What's Missing
- No input method chooser UI on either platform
- No keyboard shortcut from mobile list screen
- No audio streaming from watch to phone for phone-side transcription
- No BYOK support (Gemini, OpenAI, Groq, Together, Ollama, etc.)
- No watch standalone formatting (watch sends to phone for everything)
- No privacy information for users choosing between cloud/local
- No local model support
- No hardware capability detection
- No model download/install flow
- No formatting provider selection in settings

---

## Privacy Model

Users choose between two fundamentally different approaches. Each has trade-offs they should understand:

### Cloud API (BYOK)
- **How it works:** Your voice transcript is sent to a third-party AI provider's servers for processing. The provider returns structured reminder data.
- **Data sent:** The raw transcript text (what you said). No other personal data.
- **Who sees it:** The AI provider you selected (e.g., Google for Gemini, OpenAI, Groq, Together AI). Their privacy policies apply. Your API key authenticates the request.
- **Stored where:** Only on your device after formatting. The AI provider may retain data per their terms.
- **Best for:** Best formatting quality, works on any device, no storage/battery impact.
- **Recommendation:** For maximum privacy, choose a provider with a strong privacy policy, or use a self-hosted endpoint (Ollama, vLLM, LM Studio on your own hardware).

### On-Device Model (Local)
- **How it works:** A small AI model runs entirely on your phone. No data leaves your device.
- **Data sent:** Nothing. Everything stays on your phone.
- **Who sees it:** Nobody. Fully private.
- **Stored where:** The model file (~1.4 GB) is stored on your phone's internal storage. Your reminders stay on-device.
- **Best for:** Maximum privacy, works offline, no API costs.
- **Trade-offs:** Formatting quality is lower than cloud models (especially for complex reminders). Requires a capable device (8GB+ RAM recommended). Uses ~1.5 GB of storage. Slower inference (1-5 seconds vs <1 second cloud). Higher battery usage during formatting.
- **Recommendation:** Best privacy choice. Quality is still good for simple reminders. Try it — if formatting quality isn't sufficient, switch to cloud.

### Provider-Specific Privacy Notes
| Provider | Data Retention | Notes |
|----------|---------------|-------|
| Gemini (Google) | 30 days by default, configurable | Free tier available |
| OpenAI | 30 days, opt-out available | No free tier |
| Groq | Not used for training | Fast inference, free tier |
| Together AI | 30 days by default | Open-source model options |
| Self-hosted (Ollama/vLLM) | Your server, your rules | Full control, requires setup |

These notes should be displayed in the settings UI when selecting a provider.

---

## Workstream A: Input Method UI

### Phase A1: Wear OS Input Method Chooser

#### Task A1.1: Create InputMethodSelector composable for Wear OS

**Objective:** Build a compact Wear Compose component that shows keyboard icon + mic icon with dropdown chevron. Tapping mic opens an animated picker offering "On-watch voice", "Stream to phone", and if the watch has its own internet + configured API: "Cloud format on watch".

**Files:**
- Create: `wear/src/main/java/com/example/reminders/wear/ui/component/InputMethodSelector.kt`
- Create: `wear/src/main/java/com/example/reminders/wear/ui/component/InputMethod.kt`
- Modify: `wear/src/main/res/values/strings.xml` — add string resources
- Test: `wear/src/test/java/com/example/reminders/wear/ui/component/InputMethodSelectorTest.kt`

**Step 1: Add string resources**
```xml
<string name="input_method_keyboard">Keyboard</string>
<string name="input_method_voice_on_watch">Voice on watch</string>
<string name="input_method_voice_stream_phone">Stream to phone</string>
<string name="input_method_cloud_format">Cloud format on watch</string>
<string name="input_method_select">Choose input method</string>
<string name="input_method_no_api_configured">No cloud model configured</string>
```

**Step 2: Create `InputMethod.kt` sealed interface**
```kotlin
package com.example.reminders.wear.ui.component

/**
 * Represents the available input methods for creating a reminder on Wear OS.
 */
sealed interface InputMethod {
    /** Opens the on-screen keyboard for text input. */
    data object Keyboard : InputMethod

    /** Uses Wear OS built-in speech recognition, defers formatting to phone. */
    data object VoiceOnWatch : InputMethod

    /** Streams audio to companion phone for phone-side transcription. */
    data object VoiceStreamToPhone : InputMethod

    /**
     * Uses watch's own speech recognition AND calls the cloud API directly
     * from the watch for formatting. Requires internet (Wi-Fi/LTE) and
     * a configured API key synced from phone.
     */
    data object CloudFormatOnWatch : InputMethod
}
```

**Step 3: Create `InputMethodSelector.kt` composable**
- Row layout: `Icon(Icons.Default.Edit)` | `Icon(Icons.Default.Mic)` + `Icon(Icons.Default.ArrowDropDown)` in a single clickable row
- Tapping keyboard icon directly navigates to keyboard (no expansion needed)
- Tapping mic toggles an expand/collapse `Column` animated with `animateContentSize()`:
  - "Voice on watch" — always shown
  - "Stream to phone" — always shown
  - "Cloud format on watch" — shown only if `hasCloudApiConfigured` is true and watch has internet; dimmed/hidden otherwise with subtitle "No API configured" or "No internet"
- Material 3 Wear styling throughout
- Each option shows an icon: mic, phone with arrow, cloud

**Step 4: Write tests** — Verify composable renders, dropdown state toggles, conditional visibility of cloud option.

**Step 5: Commit**
```bash
git add wear/src/main/java/com/example/reminders/wear/ui/component/
git commit -m "feat(wear): add InputMethod sealed interface and InputMethodSelector composable"
```

#### Task A1.2: Refactor Wear VoiceRecordScreen to use InputMethodSelector

**Objective:** Replace the current mic-button + text-button layout with the new `InputMethodSelector`.

**Files:**
- Modify: `wear/src/main/java/com/example/reminders/wear/ui/screen/VoiceRecordScreen.kt`
- Modify: `wear/src/main/java/com/example/reminders/wear/presentation/MainActivity.kt` — add new routes
- Test: existing VoiceRecord tests still pass

**Step 1:** Replace the `Column` containing the mic `Button` and text `Button` with `InputMethodSelector`. Wire callbacks:
- `Keyboard` → navigate to `ROUTE_KEYBOARD_INPUT`
- `VoiceOnWatch` → existing `voiceLauncher.launch(intent)` logic (same as current)
- `VoiceStreamToPhone` → navigate to `ROUTE_STREAM_TO_PHONE`
- `CloudFormatOnWatch` → launch voice recognition, then on result call local cloud formatter (from Workstream B)

**Step 2:** Add new routes to `MainActivity.kt`:
```kotlin
private const val ROUTE_STREAM_TO_PHONE = "stream-to-phone"
private const val ROUTE_CLOUD_FORMAT = "cloud-format"
```

**Step 3:** Run tests: `./gradlew :wear:test`

**Step 4:** Commit
```bash
git commit -m "feat(wear): refactor VoiceRecordScreen to use InputMethodSelector"
```

#### Task A1.3: Implement Watch → Phone Audio Streaming for Transcription

**Objective:** When user selects "Stream to phone", record audio on the watch and send it via Wearable Data Layer Channel API to the phone, which transcribes it using the phone's speech recognizer.

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
const val AUDIO_STREAM_START_PATH = "/audio-stream/start"
const val AUDIO_STREAM_DATA_PATH = "/audio-stream/data"
const val AUDIO_STREAM_END_PATH = "/audio-stream/end"
const val AUDIO_STREAM_RESULT_PATH = "/audio-stream/result"
```

**Step 2:** Implement `AudioRecorder.kt` using `AudioRecord` (not MediaRecorder, for PCM streaming). Record at 16kHz mono 16-bit PCM. Chunk into 32KB payloads.

**Step 3:** Implement `WearableAudioStreamer.kt` using `ChannelClient` for bidirectional streaming. Use `ChannelClient.openChannel()` and `ChannelClient.sendFile()` for large payloads — do NOT use `MessageClient.sendMessage()` which has a ~100KB limit.

**Step 4:** Implement `StreamToPhoneScreen.kt` with recording indicator, waveform visualization, cancel button, and status text.

**Step 5:** Implement phone-side `AudioStreamReceiver.kt` — receives PCM chunks via Channel API, writes to temp file, feeds to `AndroidSpeechRecognitionManager` for transcription, then sends result back to watch.

**Step 6:** Write unit tests for audio chunking, stream state management.

**Step 7:** Commit
```bash
git commit -m "feat(wear+mobile): implement watch-to-phone audio streaming for transcription"
```

### Phase A2: Mobile Input Method UI

#### Task A2.1: Create mobile AddNoteFab with method picker

**Objective:** Replace the single mic FAB on `ReminderListScreen` with a split FAB — keyboard button (left) + mic button with dropdown chevron (right). The mic dropdown offers available transcription methods.

**Files:**
- Create: `mobile/src/main/java/com/example/reminders/ui/component/AddNoteFab.kt`
- Create: `mobile/src/main/java/com/example/reminders/ui/component/MicMethodPicker.kt`
- Modify: `mobile/src/main/java/com/example/reminders/ui/screen/ReminderListScreen.kt`
- Test: `mobile/src/test/java/com/example/reminders/ui/component/AddNoteFabTest.kt`

**Step 1:** Create `MicMethod.kt` sealed interface:
```kotlin
sealed interface MicMethod {
    data object AndroidBuiltIn : MicMethod
    data class CloudProvider(val providerName: String) : MicMethod
    data object LocalModel : MicMethod
}
```

**Step 2:** Create `AddNoteFab.kt` — a `Row` with two `SmallFloatingActionButton`s:
- Left: keyboard icon (`Icons.Default.Edit`) — navigates to keyboard input screen
- Right: mic icon + `Icons.Default.ArrowDropDown` — toggles the method picker dropdown

**Step 3:** Create `MicMethodPicker.kt` — a `DropdownMenu` anchored to the mic FAB:
- "Android speech recognition" (always shown)
- "Cloud: [provider name]" — shown if a BYOK provider is configured (from Workstream B)
- "Local: [model name]" — shown if a local model is downloaded (from Workstream C)
- Each item has an icon and label
- Material 3 styled with fluid animation

**Step 4:** Update `ReminderListScreen` to use `AddNoteFab` instead of the single mic FAB. Add `onKeyboardInput` and `onMicMethodSelected` callbacks.

**Step 5:** Write tests for picker state and callback behavior.

**Step 6:** Commit
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

**Step 3:** Wire into `MainActivity.kt` navigation with route `ROUTE_KEYBOARD_INPUT`.

**Step 4:** Write tests for the ViewModel (happy path, empty input, pipeline error).

**Step 5:** Commit
```bash
git commit -m "feat(mobile): add KeyboardInputScreen for typed reminder entry"
```

---

## Workstream B: BYOK Cloud Model Support + Watch Standalone Formatting

### Phase B1: Unified Cloud API Client

#### Task B1.1: Create OpenAI-compatible API client

**Objective:** Build a generic HTTP client that can call any OpenAI-compatible API endpoint (base URL + API key + model name). This supports OpenAI, Gemini (via OpenAI-compatible endpoint), Groq, Together AI, Ollama, LM Studio, vLLM, Cloudflare Workers AI, and any provider exposing `/v1/chat/completions`.

**Files:**
- Create: `mobile/src/main/java/com/example/reminders/network/OpenAiCompatibleClient.kt`
- Create: `mobile/src/test/java/com/example/reminders/network/OpenAiCompatibleClientTest.kt`

**Step 1:** Implement `OpenAiCompatibleClient`:
```kotlin
/**
 * Generic HTTP client for any OpenAI-compatible chat completions API.
 *
 * Supports: OpenAI, Gemini (via OpenAI-compatible endpoint), Groq, Together AI,
 * Fireworks, Ollama, LM Studio, vLLM, and any provider exposing /v1/chat/completions.
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
- Sends `POST /chat/completions` with `model`, `messages`, `temperature`
- Parses `choices[0].message.content`
- Retry on 429 (same pattern as existing Gemini client)
- Handles empty API key gracefully (for self-hosted endpoints like Ollama that don't require auth)

**Step 2:** Write tests using `MockWebServer` — verify request format, response parsing, error handling, retry logic, empty API key.

**Step 3:** Commit
```bash
git commit -m "feat(network): add OpenAI-compatible API client for BYOK support"
```

#### Task B1.2: Create CloudFormattingProvider

**Objective:** Implement `FormattingProvider` that uses the `OpenAiCompatibleClient`. This replaces `GeminiFormattingProvider` as the universal cloud provider.

**Files:**
- Create: `mobile/src/main/java/com/example/reminders/formatting/CloudFormattingProvider.kt`
- Test: `mobile/src/test/java/com/example/reminders/formatting/CloudFormattingProviderTest.kt`

**Step 1:** Implement:
```kotlin
/**
 * [FormattingProvider] that uses any OpenAI-compatible cloud API.
 *
 * Works with: Gemini (via OpenAI-compatible endpoint), OpenAI, Groq,
 * Together AI, Fireworks, self-hosted Ollama/vLLM, etc.
 *
 * @param apiClient       The OpenAI-compatible HTTP client.
 * @param apiKeyProvider  Suspend function that returns the current API key.
 *                        Returns empty string for unauthenticated endpoints.
 */
class CloudFormattingProvider(
    private val apiClient: OpenAiCompatibleClient,
    private val apiKeyProvider: suspend () -> String
) : FormattingProvider {
    override suspend fun format(transcript: String): FormattingResult {
        val apiKey = apiKeyProvider()
        val prompt = FormattingPrompt.build()
        val jsonText = apiClient.chatCompletion(
            apiKey = apiKey,
            systemPrompt = prompt,
            userMessage = transcript
        )
        return FormattingResponseParser.parse(jsonText, transcript)
    }
}
```

**Step 2:** Reuse `FormattingPrompt.build()` — it's model-agnostic already.

**Step 3:** Deprecate `GeminiFormattingProvider` but keep it as a thin wrapper around `CloudFormattingProvider` for backward compatibility during migration.

**Step 4:** Write tests with MockWebServer.

**Step 5:** Commit
```bash
git commit -m "feat(formatting): add CloudFormattingProvider as universal cloud backend"
```

### Phase B2: Provider Presets & Settings UI

#### Task B2.1: Define provider presets

**Objective:** Create a catalog of pre-configured AI providers so users can select from a list instead of manually entering URLs.

**Files:**
- Create: `mobile/src/main/java/com/example/reminders/formatting/AiProvider.kt`

**Step 1:** Define presets:
```kotlin
/**
 * A pre-configured AI provider preset with all connection details.
 */
data class AiProvider(
    val id: String,
    val name: String,
    val baseUrl: String,
    val defaultModel: String,
    val requiresApiKey: Boolean,
    val privacyNote: String,
    val dataRetention: String,
    val hasFreeTier: Boolean,
    val iconRes: Int  // Material icon name
)

object AiProviderPresets {
    val GEMINI = AiProvider(
        id = "gemini",
        name = "Gemini",
        baseUrl = "https://generativelanguage.googleapis.com/v1beta/openai",
        defaultModel = "gemini-2.5-flash-lite",
        requiresApiKey = true,
        privacyNote = "Google's AI service. Data may be used to improve products.",
        dataRetention = "30 days, configurable in Google AI Studio",
        hasFreeTier = true
    )

    val OPENAI = AiProvider(
        id = "openai",
        name = "OpenAI",
        baseUrl = "https://api.openai.com/v1",
        defaultModel = "gpt-4o-mini",
        requiresApiKey = true,
        privacyNote = "Data may be used for safety and model improvement. Opt out available.",
        dataRetention = "30 days, opt-out available",
        hasFreeTier = false
    )

    val GROQ = AiProvider(
        id = "groq",
        name = "Groq",
        baseUrl = "https://api.groq.com/openai/v1",
        defaultModel = "llama-3.1-8b-instant",
        requiresApiKey = true,
        privacyNote = "Data not used for training. Fast inference.",
        dataRetention = "Not retained",
        hasFreeTier = true
    )

    val TOGETHER = AiProvider(
        id = "together",
        name = "Together AI",
        baseUrl = "https://api.together.xyz/v1",
        defaultModel = "meta-llama/Meta-Llama-3.1-8B-Instruct-Turbo",
        requiresApiKey = true,
        privacyNote = "Open-source model options available.",
        dataRetention = "30 days by default",
        hasFreeTier = true
    )

    val OLLAMA_LOCAL = AiProvider(
        id = "ollama-local",
        name = "Ollama (local)",
        baseUrl = "http://localhost:11434/v1",
        defaultModel = "gemma2:2b",
        requiresApiKey = false,
        privacyNote = "Runs on your own hardware. Full data control.",
        dataRetention = "Your server, your rules",
        hasFreeTier = true
    )

    val CUSTOM = AiProvider(
        id = "custom",
        name = "Custom endpoint",
        baseUrl = "",
        defaultModel = "",
        requiresApiKey = true,
        privacyNote = "Review the privacy policy of your chosen provider.",
        dataRetention = "Varies by provider",
        hasFreeTier = false
    )

    val ALL = listOf(GEMINI, OPENAI, GROQ, TOGETHER, OLLAMA_LOCAL, CUSTOM)
}
```

**Step 2:** Commit
```bash
git commit -m "feat(formatting): define AI provider presets with privacy metadata"
```

#### Task B2.2: Add unified BYOK settings UI

**Objective:** Replace the Gemini-specific API key field with a unified "AI Model" settings section where users pick a provider, see privacy info, and configure credentials.

**Files:**
- Create: `mobile/src/main/java/com/example/reminders/ui/component/AiModelSettingsSection.kt`
- Modify: `mobile/src/main/java/com/example/reminders/ui/screen/SettingsScreen.kt` — replace Gemini section with unified AI section
- Modify: `mobile/src/main/java/com/example/reminders/data/preferences/UserPreferences.kt` — add BYOK prefs
- Modify: `mobile/src/main/res/values/strings.xml` — add strings
- Test: `mobile/src/test/java/com/example/reminders/data/preferences/UserPreferencesTest.kt`

**Step 1:** Add preferences to `UserPreferences.kt`:
```kotlin
// Replace single apiKey with structured BYOK settings
val aiProviderId: Flow<String> = context.dataStore.data.map { it[AI_PROVIDER_ID] ?: "" }
val aiApiKey: Flow<String> = context.dataStore.data.map { it[AI_API_KEY] ?: "" }
val aiBaseUrl: Flow<String> = context.dataStore.data.map { it[AI_BASE_URL] ?: "" }
val aiModelName: Flow<String> = context.dataStore.data.map { it[AI_MODEL_NAME] ?: "" }
val formattingBackend: Flow<String> = context.dataStore.data.map {
    it[FORMATTING_BACKEND] ?: "cloud"  // "cloud" or "local"
}

suspend fun setAiProviderId(id: String)
suspend fun setAiApiKey(key: String)
suspend fun setAiBaseUrl(url: String)
suspend fun setAiModelName(name: String)
suspend fun setFormattingBackend(backend: String)  // "cloud" or "local"
```

**Step 2:** Create `AiModelSettingsSection.kt`:
- **Section title:** "AI Formatting Model"
- **Backend selector:** Radio buttons — "Cloud API" / "On-device model"
- **When "Cloud API" selected:**
  - Provider picker (horizontal scrollable chips or dropdown): Gemini, OpenAI, Groq, Together, Ollama, Custom
  - Selecting a preset auto-fills base URL and default model
  - API key field (hidden for Ollama)
  - Model name field (editable, pre-filled from preset default)
  - Privacy info card: expandable `Card` showing provider's `privacyNote`, `dataRetention`, and `hasFreeTier`
  - "Test connection" button — sends a sample formatting request and shows success/failure
- **When "On-device model" selected:**
  - Device capability indicator (green/yellow/red circle)
  - Model download/management (from Workstream C)
  - Privacy card: "Fully private — no data leaves your device"

**Step 3:** Wire into `SettingsScreen.kt`, replacing the current "Formatting section" (Gemini API key).

**Step 4:** Migrate existing Gemini API key to the new preferences format on first launch (backward compat).

**Step 5:** Commit
```bash
git commit -m "feat(settings): add unified AI model settings with privacy info and provider presets"
```

#### Task B2.3: Wire provider selection into AppContainer

**Objective:** Make `AppContainer` create the correct `FormattingProvider` based on user settings.

**Files:**
- Modify: `mobile/src/main/java/com/example/reminders/di/AppContainer.kt`
- Modify: `mobile/src/main/java/com/example/reminders/pipeline/PipelineOrchestrator.kt`
- Test: update existing pipeline tests

**Step 1:** Add lazy cloud provider to `AppContainer`:
```kotlin
val cloudFormattingProvider: CloudFormattingProvider by lazy {
    val prefs = runBlocking {
        Triple(
            userPreferences.aiBaseUrl.first(),
            userPreferences.aiModelName.first(),
            userPreferences.aiApiKey.first()
        )
    }
    val baseUrl = prefs.first.ifBlank {
        AiProviderPresets.ALL.find { it.id == runBlocking { userPreferences.aiProviderId.first() } }?.baseUrl ?: ""
    }
    val model = prefs.second.ifBlank {
        AiProviderPresets.ALL.find { it.id == runBlocking { userPreferences.aiProviderId.first() } }?.defaultModel ?: ""
    }
    val client = OpenAiCompatibleClient(baseUrl, model)
    CloudFormattingProvider(
        apiClient = client,
        apiKeyProvider = { userPreferences.aiApiKey.first() ?: "" }
    )
}
```

**Step 2:** Create `FormattingProviderFactory`:
```kotlin
suspend fun createFormattingProvider(): FormattingProvider {
    return when (userPreferences.formattingBackend.first()) {
        "local" -> localFormattingProvider  // Workstream C
        else -> cloudFormattingProvider     // Default
    }
}
```

**Step 3:** Update `PipelineOrchestrator` to resolve the provider lazily at call time (not at construction time) so settings changes take effect immediately.

**Step 4:** Update existing tests and add new tests for provider selection.

**Step 5:** Commit
```bash
git commit -m "feat(di): wire unified provider selection into AppContainer and PipelineOrchestrator"
```

### Phase B3: Watch Standalone Cloud Formatting

#### Task B3.1: Add OkHttp to wear module

**Objective:** Enable the watch module to make direct HTTP calls for cloud formatting.

**Files:**
- Modify: `wear/build.gradle.kts` — add OkHttp dependency
- Modify: `gradle/libs.versions.toml` — no change needed (OkHttp already defined)

**Note:** The AGENTS.md rule "Wear module never makes network calls" is being intentionally relaxed here. The watch will only make network calls for cloud formatting when the user explicitly chooses the "Cloud format on watch" input method. This is an opt-in feature, not default behavior.

**Step 1:** Add to `wear/build.gradle.kts`:
```kotlin
implementation(libs.okhttp)
```

**Step 2:** Build: `./gradlew :wear:assembleDebug`

**Step 3:** Commit
```bash
git commit -m "feat(wear): add OkHttp dependency for standalone cloud formatting"
```

#### Task B3.2: Sync BYOK credentials from phone to watch

**Objective:** When the user configures a cloud AI provider on the phone, sync the credentials to the watch via Wearable Data Layer so the watch can format reminders independently.

**Files:**
- Create: `mobile/src/main/java/com/example/reminders/wearable/CredentialSyncSender.kt`
- Create: `wear/src/main/java/com/example/reminders/wear/sync/CredentialSyncReceiver.kt`
- Modify: `wear/src/main/java/com/example/reminders/wear/data/WearUserPreferences.kt` — new file for watch-side preferences
- Modify: `wear/src/main/java/com/example/reminders/wear/sync/DataLayerPaths.kt` — add credential paths
- Modify: `wear/src/main/java/com/example/reminders/wear/di/WatchAppContainer.kt` — create cloud formatting provider
- Test: `mobile/src/test/java/com/example/reminders/wearable/CredentialSyncSenderTest.kt`
- Test: `wear/src/test/java/com/example/reminders/wear/sync/CredentialSyncReceiverTest.kt`

**Step 1:** Add credential sync paths to `DataLayerPaths.kt`:
```kotlin
const val CREDENTIAL_SYNC_PATH = "/sync/credentials"
const val CREDENTIAL_SYNC_PROVIDER_PATH = "/sync/credentials/provider"
const val CREDENTIAL_SYNC_MODEL_PATH = "/sync/credentials/model"
```

**Step 2:** Create `CredentialSyncSender.kt` on mobile:
- Observes `UserPreferences` changes (provider ID, API key, base URL, model name)
- On change, encrypts API key using `EncryptedSharedPreferences` or `MasterKey`
- Sends encrypted credentials to watch via `DataClient.putDataItem()`
- Debounce rapid changes (user typing API key)

**Step 3:** Create `CredentialSyncReceiver.kt` on watch:
- Listens for credential data items via `DataClient`
- Decrypts and stores in `WearUserPreferences` (DataStore on watch)
- Emits updates to UI so the InputMethodSelector knows whether cloud is available

**Step 4:** Create `WearUserPreferences.kt`:
```kotlin
class WearUserPreferences(private val context: Context) {
    val hasCloudCredentials: Flow<Boolean>
    val cloudProviderName: Flow<String>
    val isWatchOnline: Flow<Boolean>  // checks ConnectivityManager for Wi-Fi/LTE
}
```

**Step 5:** Wire into `WatchAppContainer.kt` to create `CloudFormattingProvider` on the watch when credentials are available.

**Step 6:** Write tests for credential encryption/decryption, sync triggers, preference reading.

**Step 7:** Commit
```bash
git commit -m "feat(wear+mobile): sync BYOK credentials from phone to watch for standalone formatting"
```

#### Task B3.3: Implement watch-side cloud formatting flow

**Objective:** When user selects "Cloud format on watch", the watch captures speech locally, then calls the cloud API directly from the watch for formatting.

**Files:**
- Create: `wear/src/main/java/com/example/reminders/wear/formatting/WatchFormattingManager.kt`
- Modify: `wear/src/main/java/com/example/reminders/wear/ui/screen/VoiceRecordScreen.kt` — handle cloud format flow
- Modify: `wear/src/main/java/com/example/reminders/wear/ui/viewmodel/VoiceRecordViewModel.kt` — add cloud formatting path
- Test: `wear/src/test/java/com/example/reminders/wear/formatting/WatchFormattingManagerTest.kt`

**Step 1:** Create `WatchFormattingManager.kt`:
```kotlin
/**
 * Manages cloud formatting on the watch.
 *
 * Uses the same OpenAI-compatible client as the phone module.
 * Credentials are synced from phone via Wearable Data Layer.
 */
class WatchFormattingManager(
    private val preferences: WearUserPreferences,
    private val context: Context
) {
    /** Check if standalone cloud formatting is available. */
    suspend fun isAvailable(): Boolean {
        return preferences.hasCloudCredentials.first() && isNetworkAvailable()
    }

    /** Format a transcript using the cloud API directly from the watch. */
    suspend fun format(transcript: String): FormattingResult {
        // Load credentials from WearUserPreferences
        // Create OpenAiCompatibleClient (or reuse cached instance)
        // Call API
        // Parse response
    }

    private fun isNetworkAvailable(): Boolean {
        // Check ConnectivityManager for Wi-Fi or LTE
    }
}
```

**Step 2:** Update `VoiceRecordViewModel` to have a `onVoiceResultCloudFormat(text: String)` path that calls `WatchFormattingManager.format()` instead of sending to phone.

**Step 3:** Update `VoiceRecordScreen` — when `CloudFormatOnWatch` is selected, launch voice recognition, and on result call the cloud formatting path instead of sending transcript to phone.

**Step 4:** The formatted result updates the reminder in-place (same as the deferred formatting flow but synchronous).

**Step 5:** Write tests for formatting manager, network check, credential loading.

**Step 6:** Commit
```bash
git commit -m "feat(wear): implement standalone cloud formatting on watch with direct API calls"
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
data class DeviceCapabilities(
    val totalRamMb: Long,
    val availableStorageMb: Long,
    val cpuCores: Int,
    val hasNpu: Boolean,
    val supportedAbis: List<String>,
    val androidVersion: Int,
    val isRecommended: Boolean,   // Green light — supports 2B-4B quantized models
    val isMinimum: Boolean        // Yellow light — supports 2B quantized, may be slow
)

class DeviceCapabilityChecker(private val context: Context) {
    fun check(): DeviceCapabilities
}
```

**Thresholds:**
- **Recommended (green circle):** ≥8GB RAM, ≥4GB free storage, arm64-v8a, Android 12+
  - Supports: Gemma 2 2B Q4 (~1.4 GB), Phi-3 mini 3.8B Q4 (~2.2 GB)
- **Minimum (yellow circle):** ≥6GB RAM, ≥2GB free storage, arm64-v8a
  - Supports: Gemma 2 2B Q4 (slower, ~3-5 sec inference)
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
- Green/yellow/red circle indicator at the top with device capability text (e.g., "Your device supports on-device AI" / "Limited support — may be slow")
- Privacy card: "Fully private — no data leaves your device. Formatting quality is lower than cloud models."
- Model selector dropdown (Gemma 2 2B Q4, Phi-3 Mini Q4, etc.)
- "Download model" button — shows model size prominently (e.g., "~1.4 GB"), warns if not on Wi-Fi
- Download progress indicator with percentage
- "Delete model" button when downloaded (frees ~1.4 GB)
- "Learn more" expandable text explaining trade-offs

**Step 2:** Wire into SettingsScreen as the alternative to the "Cloud API" backend selector.

**Step 3:** Commit
```bash
git commit -m "feat(settings): add local model section with device capability indicator"
```

### Phase C2: Local Model Runtime

#### Task C2.1: Add MediaPipe LLM Inference dependency

**Objective:** Add the MediaPipe LLM Inference API library to the project.

**Research findings:**
- **MediaPipe LLM Inference API** (`com.google.mediapipe:tasks-genai`) — Google's official on-device LLM runtime for Android
- Supports Gemma 2, Gemma 2.5, Phi-3 via `.task` files (NOT GGUF)
- Uses optimized delegates: GPU (OpenGL), NPU (via NNAPI delegate), CPU fallback
- Structured output via options configuration
- Production-ready and maintained by Google

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

#### Task C2.2: Create LocalModelManager for download and lifecycle

**Objective:** Manage local model download, storage, and lifecycle.

**Files:**
- Create: `mobile/src/main/java/com/example/reminders/ml/LocalModelManager.kt`
- Create: `mobile/src/main/java/com/example/reminders/ml/ModelInfo.kt`
- Create: `mobile/src/test/java/com/example/reminders/ml/LocalModelManagerTest.kt`

**Step 1:** Define `ModelInfo.kt`:
```kotlin
data class ModelInfo(
    val id: String,           // e.g., "gemma2-2b-q4"
    val name: String,         // e.g., "Gemma 2 2B (Quantized)"
    val downloadUrl: String,  // URL to download the .task file
    val fileSizeBytes: Long,  // Expected file size
    val fileSizeDisplay: String, // e.g., "~1.4 GB"
    val minRamMb: Long,
    val isRecommended: Boolean
)

object AvailableModels {
    val GEMMA_2_2B_Q4 = ModelInfo(
        id = "gemma2-2b-q4",
        name = "Gemma 2 2B (Q4)",
        downloadUrl = "https://storage.googleapis.com/mediapipe-models/gemma2/text_classification/gemma2-2b-it-q4/float32/1/gemma2-2b-it-q4.task",
        fileSizeBytes = 1_500_000_000L,
        fileSizeDisplay = "~1.4 GB",
        minRamMb = 6000,
        isRecommended = true
    )
    // Add more as available
    val ALL = listOf(GEMMA_2_2B_Q4)
}
```

**Step 2:** Implement `LocalModelManager.kt`:
```kotlin
class LocalModelManager(private val context: Context) {
    val downloadProgress: Flow<Float?>
    val downloadedModelId: Flow<String?>

    fun isModelDownloaded(modelId: String): Boolean
    suspend fun downloadModel(modelInfo: ModelInfo, onProgress: (Float) -> Unit)
    suspend fun deleteModel(modelId: String)
    fun getModelPath(modelId: String): File?
}
```

- Store models in `context.filesDir/models/`
- Download with OkHttp streaming, track progress
- Verify file hash after download (SHA-256)
- Show Wi-Fi recommendation before downloading over cellular

**Step 3:** Write tests for download tracking, file management, hash verification.

**Step 4:** Commit
```bash
git commit -m "feat(ml): add LocalModelManager for model download and lifecycle"
```

#### Task C2.3: Create LocalFormattingProvider

**Objective:** Implement `FormattingProvider` that runs inference using MediaPipe's LLM Inference API.

**Files:**
- Create: `mobile/src/main/java/com/example/reminders/ml/LocalFormattingProvider.kt`
- Test: `mobile/src/test/java/com/example/reminders/ml/LocalFormattingProviderTest.kt`

**Step 1:** Implement:
```kotlin
class LocalFormattingProvider(
    private val modelManager: LocalModelManager,
    private val modelInfo: ModelInfo
) : FormattingProvider {

    @Volatile private var session: LlmInference? = null
    private val mutex = Mutex()  // Serialize concurrent calls

    override suspend fun format(transcript: String): FormattingResult {
        return mutex.withLock {
            val model = ensureModelLoaded()
            val prompt = FormattingPrompt.buildForLocalModel()
            val fullPrompt = "$prompt\n\nUser: $transcript\n\nAssistant:"

            val response = model.generateResponseAsync(fullPrompt)
            FormattingResponseParser.parse(response.textChunk, transcript)
        }
    }

    private suspend fun ensureModelLoaded(): LlmInference {
        session?.let { return it }
        val path = modelManager.getModelPath(modelInfo.id)
            ?: return FormattingResult.Failure("Model not downloaded")
        // Load with GPU delegate if available, CPU fallback
        session = LlmInference.createFromOptions(
            context,
            LlmInferenceOptions.builder()
                .setModelPath(path.absolutePath)
                .setMaxTokens(512)
                .setTemperature(0.1f)
                .build()
        )
        return session!!
    }

    fun close() {
        session?.close()
        session = null
    }
}
```

**Key considerations:**
- Use `Mutex` to serialize concurrent calls (model is not thread-safe)
- Set `maxTokens` to 512 (plenty for reminder JSON)
- Set `temperature` to 0.1 for deterministic output
- Reuse `FormattingPrompt.buildForLocalModel()` — shorter variant for small models
- Unload model on `onTrimMemory()` (listen in Application class)
- Parse output with shared `FormattingResponseParser`

**Step 2:** Write tests with mock `LlmInference`.

**Step 3:** Commit
```bash
git commit -m "feat(ml): add LocalFormattingProvider using MediaPipe LLM Inference API"
```

#### Task C2.4: Wire local model into provider selection

**Objective:** When user selects "On-device model" as formatting backend, use `LocalFormattingProvider`.

**Files:**
- Modify: `mobile/src/main/java/com/example/reminders/di/AppContainer.kt`
- Modify: `mobile/src/main/java/com/example/reminders/di/RemindersApplication.kt` — handle `onTrimMemory()`

**Step 1:** Add lazy `LocalFormattingProvider` to `AppContainer`:
```kotlin
val localModelManager = LocalModelManager(context)

val localFormattingProvider: LocalFormattingProvider by lazy {
    val modelId = runBlocking { userPreferences.localModelId.first() }
    val modelInfo = AvailableModels.getById(modelId) ?: AvailableModels.GEMMA_2_2B_Q4
    LocalFormattingProvider(localModelManager, modelInfo)
}
```

**Step 2:** Update the provider factory to include "local" option.

**Step 3:** Add `onTrimMemory()` listener in `RemindersApplication.kt` to close the local model session when memory pressure is high:
```kotlin
override fun onTrimMemory(level: Int) {
    super.onTrimMemory(level)
    if (level >= TRIM_MEMORY_RUNNING_LOW) {
        container.localFormattingProvider.close()
    }
}
```

**Step 4:** Commit
```bash
git commit -m "feat(di): wire local model provider into formatting pipeline with memory management"
```

### Phase C3: Prompt Engineering for Local Models

#### Task C2.5: Create model-specific prompt tuning

**Objective:** Adapt `FormattingPrompt` for smaller models that need more explicit instructions.

**Files:**
- Modify: `mobile/src/main/java/com/example/reminders/formatting/FormattingPrompt.kt`
- Test: `mobile/src/test/java/com/example/reminders/formatting/FormattingPromptTest.kt`

**Step 1:** Add `buildForLocalModel()` variant:
```kotlin
fun buildForLocalModel(currentDate: LocalDate = LocalDate.now()): String {
    // Shorter prompt optimized for 2B-4B parameter models:
    // - 1-2 examples instead of 5
    // - More explicit schema repetition
    // - "Return ONLY valid JSON" emphasis
    // - No ambiguous instructions
    // - ~800 tokens vs ~1200 tokens for the full prompt
}
```

**Step 2:** Update `LocalFormattingProvider` to use `buildForLocalModel()`.

**Step 3:** Add tests verifying prompt structure.

**Step 4:** Commit
```bash
git commit -m "feat(formatting): add model-specific prompt tuning for local LLMs"
```

---

## Shared Tasks

### Task S1: Extract shared FormattingResponseParser

**Objective:** All formatting providers parse the same JSON response format. Extract shared parsing into a utility.

**Files:**
- Create: `mobile/src/main/java/com/example/reminders/formatting/FormattingResponseParser.kt`
- Modify: `mobile/src/main/java/com/example/reminders/formatting/GeminiFormattingProvider.kt`
- Modify: `mobile/src/main/java/com/example/reminders/formatting/CloudFormattingProvider.kt`
- Modify: `mobile/src/main/java/com/example/reminders/ml/LocalFormattingProvider.kt`
- Modify: `mobile/src/main/java/com/example/reminders/network/GeminiApiClient.kt` — extract `cleanJsonText()`
- Test: `mobile/src/test/java/com/example/reminders/formatting/FormattingResponseParserTest.kt`

**Step 1:** Create `FormattingResponseParser`:
```kotlin
object FormattingResponseParser {
    fun parse(jsonText: String, rawTranscript: String): FormattingResult
    fun parseSingleReminder(obj: JsonObject): ParsedReminder
    fun cleanJsonText(text: String): String
}
```

**Step 2:** Move `parseReminders()` and `parseSingleReminder()` from `GeminiFormattingProvider`, and `cleanJsonText()` from `GeminiApiClient`.

**Step 3:** Update all providers to use the shared parser.

**Step 4:** Write comprehensive tests (valid JSON, malformed, partial, empty, code fences, trailing commas).

**Step 5:** Commit
```bash
git commit -m "refactor(formatting): extract shared FormattingResponseParser utility"
```

### Task S2: Update AGENTS.md

**Objective:** Document the new architecture and relax the no-network rule for wear.

**Files:**
- Modify: `AGENTS.md`

**Step 1:** Update sections:
- Remove "Do not add on-device LLM deps" from Critical Rules (now supported)
- Relax "Wear module never makes network calls" to "Wear module should not make network calls by default. Network calls are permitted only for user-initiated cloud formatting when the user explicitly selects 'Cloud format on watch'."
- Document the three formatting backends: Cloud (BYOK), Local (on-device), Raw fallback
- Document credential sync flow from phone to watch
- Update Pipeline diagram

**Step 2:** Commit
```bash
git commit -m "docs: update AGENTS.md with BYOK, local LLM, and watch cloud formatting architecture"
```

---

## Execution Order

### Pre-requisite (do first on main)
1. **S1** — Extract shared `FormattingResponseParser`

### Parallel Tracks (after S1)

```
Track 1 (A):  Input Method UI          [Branch: feature/input-methods]
Track 2 (B):  BYOK Cloud + Watch Cloud [Branch: feature/byok-cloud]
Track 3 (C):  Local LLM Inference      [Branch: feature/local-llm]
```

### Recommended Agent Assignment

| Agent | Branch | Tasks | Scope |
|-------|--------|-------|-------|
| Agent 1 | `feature/input-methods` | A1.1, A1.2, A1.3, A2.1, A2.2 | Wear + mobile input UI, audio streaming |
| Agent 2 | `feature/byok-cloud` | B1.1, B1.2, B2.1, B2.2, B2.3, B3.1, B3.2, B3.3 | BYOK client, providers, settings, watch cloud |
| Agent 3 | `feature/local-llm` | C1.1, C1.2, C2.1, C2.2, C2.3, C2.4, C2.5 | Hardware check, MediaPipe, download, local provider |

### Merge Order
1. S1 (shared parser) → main
2. A (input methods) → main
3. B (BYOK cloud) → main
4. C (local LLM) → main

After each merge: `./gradlew test && ./gradlew lint && ./gradlew assembleDebug`

---

## Pitfalls

1. **Wear Data Layer payload limits** — `MessageClient.sendMessage()` has ~100KB limit. For audio streaming, use `ChannelClient.openChannel()` and `ChannelClient.sendFile()`. Do NOT send raw audio via `sendMessage`.

2. **MediaPipe model format** — Uses `.task` files, not GGUF. Models must be pre-converted. Google provides Gemma 2 models in this format from their storage bucket.

3. **Memory pressure from local models** — 2B Q4 model uses ~1.5GB RAM. Must unload on `onTrimMemory(RUNNING_LOW)`. Use `Volatile` + `Mutex` pattern.

4. **Watch battery from direct API calls** — HTTP calls on watch drain battery faster than on phone. The "Cloud format on watch" option should clearly indicate it uses more battery. Consider showing battery impact warning.

5. **OpenAI-compatible API differences** — Some providers (Ollama) don't require API keys. Client must handle empty key gracefully. Response format variations exist — parser must be robust.

6. **Model download UX** — 1.4GB downloads over mobile data are bad UX. Show file size, prefer Wi-Fi check, support resume on failure.

7. **FormattingPrompt for small models** — The full prompt was designed for Gemini. 2B local models struggle with long contexts. The `buildForLocalModel()` variant addresses this but must be validated with actual model output.

8. **Thread safety for local model** — MediaPipe `LlmInference` is not thread-safe. Use `Mutex` to serialize concurrent `format()` calls.

9. **Credential sync encryption** — API keys synced to watch must be encrypted. Use Android Keystore + `EncryptedSharedPreferences` on the watch side. The watch has no biometric prompt, so keys are protected by the hardware keystore only.

10. **Gemini OpenAI-compatible endpoint** — Google exposes Gemini via an OpenAI-compatible endpoint at `https://generativelanguage.googleapis.com/v1beta/openai`. This is how Gemini becomes "just another BYOK provider" — same `OpenAiCompatibleClient`, same `CloudFormattingProvider`, just different URL and key format.

---

## Verification

### Per-task
- `./gradlew test` passes
- `./gradlew lint` passes (no new warnings)
- `./gradlew assembleDebug` succeeds

### Integration
- Full CI gate: `./gradlew test && ./gradlew lint && ./gradlew assembleDebug`
- Manual: Test on Wear OS emulator (voice + keyboard + stream + cloud format)
- Manual: Test BYOK with a real endpoint (e.g., Groq free tier)
- Manual: Test credential sync phone → watch
- Manual: Test local model download and formatting on a capable device

### Code Quality (reinforced for all agents)
- All new code has KDoc comments on classes and non-trivial functions
- All new public functions have tests (happy path + error path + edge cases)
- No magic numbers — extract named constants
- Follow existing code style (4-space indent, descriptive names, no abbreviations)
- All strings in `strings.xml`
- Functions ≤30 lines — split if longer
- Properties → init → public methods → private methods ordering
