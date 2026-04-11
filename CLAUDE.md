# Reminders — WearOS App

## Modules
- `wear/` — WearOS UI, voice capture, reminder display. No network, no model compute.
- `mobile/` — All compute, transcription, API calls, geofencing. Companion to watch.
- Watch ↔ phone via **Wearable Data Layer API** (`DataClient` / `ChannelClient`).
- Do NOT create new modules without being asked.

## Build
```bash
./gradlew assembleDebug
./gradlew :wear:assembleDebug
./gradlew :mobile:assembleDebug
./gradlew test
./gradlew lint
```
All dependency versions in `gradle/libs.versions.toml` — never inline.

## Architecture
- MVVM. Coroutines + Flow only (no RxJava).
- Compose for Wear OS (`wear`), Compose (`mobile`).
- No business logic in Composables.
- `sealed interface` over `sealed class` for UI state.
- All strings in `strings.xml`.

## Pipeline (in order)
```
TRANSCRIPTION → FORMATTING → GEOCODING → GEOFENCE REGISTRATION → ROOM STORAGE
```
Each stage is a separate abstraction. Never conflate them.

## Transcription (`mobile`, offline)
- Default: Android `SpeechRecognizer` + `EXTRA_PREFER_OFFLINE = true` (API 31+)
- BYOM: user-supplied Futo Whisper `.bin` file (from keyboard.futo.org/voice-input-models)
- `sealed interface TranscriptionBackend { object AndroidBuiltIn; data class FutoWhisper(val modelPath: String) }`
- Fallback to AndroidBuiltIn if BYOM model missing. Warn user if online recognition is used.
- **Do not implement BYOM until AndroidBuiltIn path works end-to-end.**

## Formatting (`mobile`, cloud)
- Cloud LLM (user supplies API key — never hardcode). Returns structured JSON reminder list.
- `interface FormattingProvider { suspend fun format(transcript: String): List<ParsedReminder> }`
- No key configured → save raw transcript as single unformatted reminder. Never silently discard.
- Future: `LocalLlmFormattingProvider` — do NOT add MediaPipe/llama.cpp dependencies yet.
- Prompt returns JSON array: `{ title, triggerTime: ISO8601|null, recurrence|null, locationTrigger: { placeLabel, triggerOnEnter, triggerOnExit }|null }`

## Location Reminders & Geofencing (`mobile`)
- Reminders can trigger on entering/leaving a place ("when I get home", "at the doctors").
- **Saved Places** checked first before any geocoding API call.
- **Geocoding:** HERE SDK for Android (Explore Edition). Key in BuildConfig, never hardcoded.
  - Ambiguous results → `NEEDS_CONFIRMATION` state, surface to user. Never silently pick first result.
- **Geofences:** Android `GeofencingClient` (play-services-location).
  - Radius options: 50m / 100m / 150m (default) / 300m / 500m / 1km. Set per-reminder.
  - Always set `setLoiteringDelay(30_000)` to avoid drive-by false triggers.
  - Android cap: 100 active geofences. Warn user if limit approached.
  - On reminder delete/complete: always call `removeGeofences()`. Never leave orphans.
- **Permissions:** `ACCESS_FINE_LOCATION` first, then `ACCESS_BACKGROUND_LOCATION` separately (API 29+). Never request both together.
- Background location denied → disable all geofence reminders, inform user clearly.

## Key Models
```kotlin
data class Reminder(
    val id: String,                    // UUID
    val title: String,
    val body: String?,
    val triggerTime: Instant?,
    val recurrence: String?,
    val locationTrigger: LocationTrigger?,
    val sourceTranscript: String,
    val formattingProvider: String     // "cloud" | "local" | "none"
)

data class LocationTrigger(
    val placeLabel: String,
    val rawAddress: String?,
    val latitude: Double?,
    val longitude: Double?,
    val radiusMetres: Int = 150,
    val triggerOnEnter: Boolean = true,
    val triggerOnExit: Boolean = false,
    val geofenceId: String?
)

data class SavedPlace(
    val id: String,
    val label: String,                 // case-insensitive match e.g. "home"
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val defaultRadiusMetres: Int = 150
)
```
`ReminderParser` always returns `List<Reminder>` — never a single object.

## Location Reminder States
`PENDING_GEOCODING → NEEDS_CONFIRMATION → ACTIVE → TRIGGERED → COMPLETED`

## WearOS Rules
- All Wear Compose UI supports round and square screens. Use `ScalingLazyColumn`.
- Persistent screens need ambient mode (minimal layout, no animation, reduced colour).
- Tap targets ≥ 48dp.
- `wear` never makes network calls or runs model compute.

## Lessons Learned
- Do not create new Gradle modules without being asked.
- Do not add on-device LLM deps (MediaPipe, llama.cpp) — local formatting is future work.
- Do not implement BYOM until AndroidBuiltIn transcription works end-to-end.
- Never request `ACCESS_BACKGROUND_LOCATION` at the same time as fine location.
- Never silently pick first geocoding result — always show disambiguation UI.
- Always remove geofences when a reminder is deleted or completed.
- Never hardcode API keys (HERE, cloud LLM). Always use BuildConfig or user-supplied preferences.
