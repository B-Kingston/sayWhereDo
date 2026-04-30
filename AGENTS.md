# Reminders — WearOS + Mobile App

## Project Overview

Voice-captured reminder app with WearOS watch + Android phone companion. Watch is standalone-capable (own Room DB, voice input, time-based reminders, optional geofencing). Phone provides cloud formatting (Gemini 2.5 Flash Lite LLM or OpenAI-compatible providers), geocoding, and can manage geofences on behalf of watch. Tombstone-based bidirectional sync via Wearable Data Layer.

**Reference docs (committed in repo)**:
- `docs/implementation-plan.md` — full phase specs, key files, pitfalls, verification steps, monetization, V2 deferred plans
- `docs/sync-architecture-spec.md` — complete sync protocol spec, DTOs, reconciliation rules, data model

---

## File Map

There is no `:common` module — models and DTOs are duplicated across modules. Know which side you're editing.

### Mobile (`mobile/src/main/java/com/example/reminders/`)

```
├── MainActivity.kt                          # Single-activity host, Compose navigation
├── di/
│   ├── AppContainer.kt                      # Manual DI: wires ALL services, repos, providers, DBs
│   └── RemindersApplication.kt              # Application subclass, exposes AppContainer
├── data/
│   ├── model/
│   │   ├── Reminder.kt                      # Room @Entity (table: reminders, schema v2)
│   │   ├── ParsedReminder.kt                # Plain data class: intermediate formatting output
│   │   ├── DeletedReminder.kt               # Room @Entity: soft-delete tombstone table
│   │   ├── SavedPlace.kt                    # Room @Entity: named geocoded locations (phone-only)
│   │   ├── LocationTrigger.kt               # Geofence params (label, lat/lng, radius)
│   │   └── LocationReminderState.kt         # Enum: PENDING_GEOCODING → NEEDS_CONFIRMATION → ACTIVE → TRIGGERED → COMPLETED
│   ├── local/
│   │   ├── RemindersDatabase.kt             # Room DB (v2), Migration_1_2, TypeConverters
│   │   ├── ReminderDao.kt                   # DAO: insert/update/delete/observe, geofenced queries
│   │   ├── DeletedReminderDao.kt            # DAO: tombstone CRUD + exists check + purgeOlderThan
│   │   ├── SavedPlaceDao.kt                 # DAO: saved places CRUD
│   │   └── Converters.kt                    # Instant↔Long, LocationTrigger↔JSON, LocationReminderState↔String
│   ├── repository/
│   │   ├── ReminderRepository.kt            # Interface + ReminderRepositoryImpl
│   │   └── SavedPlaceRepository.kt          # Interface + SavedPlaceRepositoryImpl
│   ├── preferences/
│   │   ├── UserPreferences.kt               # Encrypted DataStore: API key, provider, base URL, model
│   │   └── UsageTracker.kt                  # DataStore: daily formatting count, free-tier gating
│   └── export/
│       ├── ReminderExporter.kt              # Export reminders to JSON
│       └── ExportMappers.kt                 # Reminder entities → export DTOs
├── transcription/
│   ├── SpeechRecognitionManager.kt          # Interface: start/stop, returns Flow<TranscriptionUiState>
│   ├── AndroidSpeechRecognitionManager.kt   # Android SpeechRecognizer with offline retry
│   └── TranscriptionBackend.kt             # Sealed interface: AndroidBuiltIn | FutoWhisper (stub)
├── formatting/
│   ├── FormattingProvider.kt               # Interface: suspend format(transcript) → FormattingResult
│   ├── FormattingResult.kt                 # Sealed interface: Success | PartialSuccess | Failure | UsageLimited
│   ├── FormattingPrompt.kt                 # System prompt builder (full + local model variant)
│   ├── FormattingResponseParser.kt         # Shared JSON parser: strips fences, trailing commas, partial parse
│   ├── GeminiFormattingProvider.kt         # Uses GeminiApiClient + FormattingResponseParser
│   ├── CloudFormattingProvider.kt          # Uses OpenAiCompatibleClient (/v1/chat/completions)
│   ├── RawFallbackProvider.kt              # Returns raw transcript as ParsedReminder (no LLM)
│   ├── ReminderMapper.kt                   # ParsedReminder → Room Reminder entity with UUID
│   └── AiProvider.kt                       # Provider presets: Gemini, OpenAI, Groq, Together, Ollama, Custom
├── pipeline/
│   ├── PipelineOrchestrator.kt             # Chains: usage gate → provider → format → persist
│   └── PipelineResult.kt                   # Sealed interface: Success | PartialSuccess | Failure | UsageLimited
├── network/
│   ├── GeminiApiClient.kt                  # OkHttp, Gemini native API, exponential backoff on 429
│   └── OpenAiCompatibleClient.kt           # Generic /v1/chat/completions, Bearer auth, backoff on 429
├── ml/                                      # On-device LLM (future — do not add new deps here)
│   ├── DeviceCapabilityChecker.kt
│   ├── LocalModelManager.kt
│   ├── MediaPipeInferenceWrapper.kt
│   ├── LocalFormattingProvider.kt
│   └── ...
├── geocoding/
│   ├── GeocodingService.kt                 # Interface: geocode(placeLabel) → GeocodingResult
│   ├── GeocodingResult.kt                  # Sealed interface: Resolved | Ambiguous | NotFound | Error
│   ├── AndroidGeocodingService.kt          # android.location.Geocoder wrapper
│   ├── GeocodingPipelineStep.kt            # Orchestrates geocode → check SavedPlace → disambiguate
│   └── SavedPlaceMatcher.kt               # Matches LLM place labels to saved places
├── geofence/
│   ├── GeofenceManager.kt                  # Interface: register/remove geofence
│   ├── AndroidGeofenceManager.kt           # GeofencingClient wrapper, ENTER+DWELL, 30s loitering
│   ├── GeofenceCapTracker.kt              # Enforces free=5 / pro=100 geofence caps
│   ├── GeofenceBroadcastReceiver.kt        # Geofence transitions → notifications
│   ├── GeofenceBootReceiver.kt            # Re-registers geofences after boot
│   └── GeofenceReregistrationWorker.kt     # WorkManager periodic re-registration
├── alarm/
│   ├── AlarmScheduler.kt                   # Interface: schedule/cancel time-based alarms
│   ├── AlarmReceiver.kt                    # BroadcastReceiver: fires notification at alarm time
│   ├── RecurrenceHelper.kt                # Calculates next trigger for daily/weekly/monthly
│   ├── ReminderCompletionManager.kt        # Orchestrates complete + cleanup (geofence, alarm, sync)
│   └── ...
├── notification/
│   ├── ReminderNotificationManager.kt      # Channels and notification posting
│   └── NotificationActionReceiver.kt       # Actions: complete, snooze, dismiss
├── permissions/
│   └── LocationPermissionHandler.kt        # Composable helper for location permission flow
├── sync/
│   ├── ReminderSyncClient.kt              # Interface + NoOpSyncClient stub
│   └── WearableSyncClient.kt              # Impl: syncs via WearableDataSender, tombstones deletions
├── wearable/                               # Data Layer communication (phone side)
│   ├── DataLayerPaths.kt                  # Path constants: /reminders/{id}, /sync/*, /pro-status, etc.
│   ├── ReminderDto.kt                     # @Serializable DTOs: ReminderDto, DeletedReminderDto, SyncStateDto
│   ├── SyncEngine.kt                      # Tombstone-based bidirectional reconciliation algorithm
│   ├── SyncConflictResolver.kt            # Last-write-wins (by updatedAt)
│   ├── WearableDataSender.kt             # DataClient/MessageClient: sends to watch
│   ├── WearableListenerServiceImpl.kt    # WearableListenerService: receives from watch, runs pipeline
│   ├── WatchConnectivityMonitor.kt        # CapabilityClient: Flow<Boolean> for watch connectivity
│   └── CredentialSyncSender.kt           # Observes UserPreferences → syncs AI creds to watch
├── billing/
│   └── BillingManager.kt                  # Play Billing Library, isPro StateFlow
├── offline/
│   ├── PendingOperation.kt                # Room entity for offline queue
│   ├── OfflineQueueManager.kt             # Manages pending operations
│   └── OfflineQueueWorker.kt              # WorkManager retry worker
└── ui/
    ├── screen/                            # TranscriptionScreen, ReminderListScreen, EditScreen, SettingsScreen, etc.
    ├── viewmodel/                         # One ViewModel per screen
    ├── component/                         # Reusable Compose components
    ├── theme/                             # Material 3 theming (Color, Type, Shape, Theme, Spacing)
    └── accessibility/                     # Compose modifiers for accessibility
```

### Wear (`wear/src/main/java/com/example/reminders/wear/`)

```
├── presentation/
│   ├── MainActivity.kt                    # Splash + SwipeDismissableNavHost
│   └── theme/Theme.kt                     # Wear-specific theme
├── di/
│   ├── WatchAppContainer.kt               # Manual DI: DB, repos, geofence, alarms, formatting
│   └── WatchRemindersApplication.kt       # Application subclass
├── data/
│   ├── WatchReminder.kt                   # Room @Entity (table: watch_reminders, schema v3)
│   ├── DeletedReminder.kt                 # Tombstone table (identical schema to mobile)
│   ├── WatchReminderDao.kt               # DAO: CRUD + geofenced + timed queries
│   ├── WatchRemindersDatabase.kt         # Room DB (v3), Migration_1_2 + Migration_2_3
│   ├── WatchConverters.kt                # Instant↔Long only (no LocationTrigger converter)
│   ├── WatchReminderRepository.kt        # Repository: CRUD + tombstones
│   ├── WearDataLayerClient.kt            # DataClient/MessageClient: sync to phone, send transcript
│   └── WearUserPreferences.kt            # DataStore: cloud credentials, geofencing preference
├── sync/
│   ├── DataLayerPaths.kt                 # Path constants (mirrors mobile + audio stream paths)
│   ├── ReminderDto.kt                    # @Serializable DTOs (structurally identical to mobile)
│   ├── ReminderSerializer.kt            # ByteArray ↔ WatchReminder/DTO serialization
│   ├── SyncEngine.kt                     # Stateless object: tombstone reconciliation
│   ├── SyncConflictResolver.kt           # Last-write-wins + lexicographic ID tie-break
│   └── CredentialSyncReceiver.kt         # WearableListenerService: receives AI creds from phone
├── formatting/
│   ├── FormattingPrompt.kt               # Shorter system prompt for watch-side cloud formatting
│   ├── FormattingResponseParser.kt       # Shared JSON response parser (same as mobile)
│   ├── WatchCloudClient.kt              # OpenAI-compatible HTTP client for watch
│   └── WatchFormattingManager.kt        # Manages standalone cloud formatting
├── geofence/
│   ├── WatchGeofenceManager.kt           # GeofencingClient wrapper for watch
│   ├── GpsDetector.kt                    # Checks if watch has GPS hardware
│   ├── GeofencingDeviceManager.kt        # Auto-switch: watch geofence ↔ phone geofence
│   └── ...
├── alarm/                                 # Mirror of mobile alarm layer adapted for watch
│   ├── WatchAlarmScheduler.kt
│   ├── WatchReminderCompletionManager.kt
│   └── ...
├── notification/                          # Watch notification channels and posting
├── complication/
│   ├── ReminderComplicationProvider.kt    # SHORT_TEXT complication: today/all counts
│   └── ...
├── service/
│   └── DataLayerListenerService.kt        # WearableListenerService: receives phone data/messages
└── ui/
    ├── screen/                           # WatchReminderListScreen, VoiceRecordScreen, ReminderDetailScreen, etc.
    ├── viewmodel/                        # One ViewModel per screen
    ├── component/                        # SwipeableWatchReminderCard, InputMethodSelector, PhoneRequiredBanner
    └── theme/                            # Wear Material 3 theming
```

---

## Critical Rules

### Will crash or corrupt data

- **Never use `fallbackToDestructiveMigration()`** in Room — formally deprecated in Room 2.7.0+. Use explicit `Migration` objects.
- **Do NOT use `@Serializable` on Room entities or `@Embedded` types** — KSP2 has an open bug where `@Embedded` silently drops all columns for `@Serializable` types. Use manual TypeConverters with `Json.encodeToString()`/`Json.decodeFromString()`.
- **JAVA_HOME** must be set to `"/Applications/Android Studio.app/Contents/jbr/Contents/Home"` for all gradle commands — no system JDK is installed.
- **Never hardcode API keys.** API key is stored in `UserPreferences` (Encrypted DataStore) and read at call time via `apiKeyProvider: suspend () -> String` lambda. For build-time keys use `BuildConfig` from `local.properties`.
- **Wear module never makes network calls** (except watch-side cloud formatting via `WatchCloudClient` — the only authorized exception).
- **Always remove geofences** when a reminder is deleted or completed.

### Will break sync / lose data

- **Both sides must be updated for sync changes.** If you touch `DataLayerPaths`, `ReminderDto`, `SyncEngine`, or `SyncConflictResolver` in one module, update the other module to match.
- **DTOs are duplicated** — `mobile/.../wearable/ReminderDto.kt` and `wear/.../sync/ReminderDto.kt` must stay structurally identical. There is no shared module.
- **Room schema changes require migrations.** Mobile DB is v2, Watch DB is v3. Write + test a migration before reporting done.
- **`setLoiteringDelay(30_000)`** on all geofences. Use `GEOFENCE_TRANSITION_DWELL` alongside `ENTER`.
- **Completed reminders sync as updates** (not deletions). Only explicit user delete creates a tombstone.

### Will degrade UX or get app rejected

- **Never request `ACCESS_BACKGROUND_LOCATION`** at the same time as fine location.
- **Never silently pick first geocoding result** — always show disambiguation UI (`GeocodingConfirmationScreen`).
- **All Wear Compose UI must support round and square screens.** Tap targets ≥ 48dp.
- **`ACTION_RECOGNIZE_SPEECH`** does NOT require `RECORD_AUDIO` permission, but availability varies by OEM watch (unreliable on Samsung Galaxy Watch 4/5). Always check `resolveActivity()` and provide keyboard fallback.
- **Handle `GEOFENCE_NOT_AVAILABLE`** (ApiException code 1000) on watch — occurs when location is turned off or GPS absent.

### Architecture constraints

- **Do not add on-device LLM deps** (MediaPipe, llama.cpp) — local formatting is future work. The `ml/` package exists but do not expand it.
- **Do not implement BYOM transcription** until AndroidBuiltIn works end-to-end.
- **Do NOT create new modules** without being asked. The two-module structure is fixed.
- **minSdk 33** for both modules.
- **All dependency versions in `gradle/libs.versions.toml`** — never inline a version number.
- **All strings in `strings.xml`** — never hardcode user-visible text.

---

## Lessons Learned

- **Regex in Kotlin raw strings (`"""`)**: Backslashes are literal — escape regex metacharacters with single `\` (e.g., `\}`, `\]`), not `\\`. Android's ICU regex engine treats unescaped `}` as a quantifier error.
- **`EXTRA_PREFER_OFFLINE` causes `ERROR_LANGUAGE_UNAVAILABLE`** on devices without downloaded offline speech packs. Always retry without the hint to fall back to cloud recognition.
- **NEVER `pm clear com.google.android.gms`** — it destroys Wear OS pairing, Google account data, and all GMS state. Use `am force-stop com.google.android.gms` to safely restart Play Services (preserves pairing). If WearableService shows `STUCK WHILE PROCESSING READ`, force-stop GMS on both devices, wait 5s, then relaunch the app.
- **Wear OS emulator Data Layer is unreliable** — `CapabilityClient` often returns empty nodes even when paired. Always verify with `dumpsys activity service com.google.android.gms | grep -E "STUCK|IsConnected"` before assuming app-level bugs. Prefer real devices for Data Layer testing.
- **WearableDataSender and WearDataLayerClient must use identical DataMap keys** — a mismatch in `putString`/`getString` keys causes silent data loss. Define key constants in one place and reference them.

---

## Architecture

- **MVVM**. Coroutines + Flow only (no RxJava).
- **Compose** for both phone and watch UI.
- No business logic in Composables.
- `sealed interface` (not `sealed class`) for UI state and all result types.
- Manual DI via `AppContainer` (mobile) and `WatchAppContainer` (wear) — no Hilt/Dagger.
- **Logging**: plain `android.util.Log` (no Timber). Tag convention: class name or short descriptive name as `companion object { private const val TAG = "..." }`.

### Anti-patterns — do not write code like this

- ❌ **Catching `Exception` in repository methods and returning `null`** — propagate as `Result.Failure(error)` so ViewModel can render error state.
- ❌ **Putting business logic in a Composable** — extract to ViewModel. Composables only observe state and emit events.
- ❌ **Using `sealed class` instead of `sealed interface`** for UI state or result types.
- ❌ **Inlining a dependency version** (e.g., `"androidx.room:room-runtime:2.7.2"`) — always use the version catalog: `libs.room.runtime.get()`.

---

## Sync Protocol

See `docs/sync-architecture-spec.md` for the full spec. Summary:

### Data Layer paths

| Path | Transport | Direction | Purpose |
|------|-----------|-----------|---------|
| `/reminders/{id}` | DataClient | Bidirectional | Individual reminder sync |
| `/pro-status` | DataClient | Phone → Watch | Pro subscription status |
| `/deferred-formatting` | MessageClient | Watch → Phone | Request cloud formatting |
| `/sync/state-request` | MessageClient | Watch → Phone | Request phone's full state |
| `/sync/state-response` | MessageClient | Phone → Watch | Phone sends reminders + tombstones |
| `/sync/state-complete` | MessageClient | Watch → Phone | Watch sends reconciled state |
| `/sync/tombstone` | MessageClient | Bidirectional | Individual tombstone notification |
| `/sync/credentials` | DataClient | Phone → Watch | AI API key + base URL + model |
| `/audio-stream/*` | MessageClient | Bidirectional | Audio streaming (stub, not implemented) |

### Conflict resolution

- **Last-write-wins** by `updatedAt` timestamp.
- **Watch adds lexicographic ID tie-break** when `updatedAt` values are equal (deterministic).
- **Tombstones beat active edits** only when `tombstone.originalUpdatedAt > remote.updatedAt` — otherwise the edit wins (restore).
- **No CRDT.** This is a simple timestamp-based merge.
- **7-day tombstone retention** before permanent purge. Trash/restore UI on both sides.

### Sync state exchange flow

1. Watch → Phone: `SYNC_STATE_REQUEST`
2. Phone → Watch: `SYNC_STATE_RESPONSE` (all active reminders + tombstones + deviceId)
3. Watch runs `SyncEngine.reconcile()`, applies locally
4. Watch → Phone: `SYNC_STATE_COMPLETE` (reconciled watch state)
5. Phone runs reconciliation, applies locally

### What to update when touching sync

If you change any of these, **both modules must be updated together**:
- `DataLayerPaths.kt` — path constants
- `ReminderDto.kt` — wire format DTOs
- `SyncEngine.kt` — reconciliation algorithm
- `SyncConflictResolver.kt` — conflict resolution rules
- `ReminderSerializer.kt` (wear) — serialization logic

---

## Pipeline

Each stage is a separate abstraction with a `sealed interface` result type. Never conflate them.

```
📱 TRANSCRIPTION → 📱 FORMATTING → 📱 GEOCODING → 📱 GEOFENCE REGISTRATION → 💾 ROOM STORAGE
```

- 📱 = phone-only. ⌚ = watch-only. 💾 = both modules have their own DB.
- All stages can fail. Every result type follows: `Success | PartialSuccess | Failure | UsageLimited` (or equivalent).
- **Formatting and geocoding are phone-only** (watch can request phone formatting via `/deferred-formatting`).
- The watch has a standalone `WatchFormattingManager` for cloud formatting when phone is disconnected.

### Result types flowing through the pipeline

```kotlin
FormattingResult  → Success | PartialSuccess | Failure | UsageLimited  // formatting/FormattingResult.kt
PipelineResult    → Success | PartialSuccess | Failure | UsageLimited  // pipeline/PipelineResult.kt
GeocodingResult   → Resolved | Ambiguous | NotFound | Error           // geocoding/GeocodingResult.kt
```

### LLM formatting

- **Prompt template**: `formatting/FormattingPrompt.kt` — single source of truth. Injects current date for relative time resolution.
- **Expected JSON schema from LLM**:
  ```json
  [{"title":"string","body":"string|null","triggerTime":"ISO 8601|null","recurrence":"daily|weekly|monthly|null","locationTrigger":{"placeLabel":"string","rawAddress":"string|null"}|null}]
  ```
- **Response parsing**: `FormattingResponseParser.kt` — strips markdown fences, trailing commas, handles partial JSON.
- **API key**: stored in `UserPreferences` (Encrypted DataStore), never hardcoded.
- **Retry**: 3 retries with exponential backoff (starting 1000ms) on HTTP 429. Timeouts: 10s connect, 30s read.

---

## Code Quality

- **Stellar code quality is non-negotiable.** Every file, class, and function should be clean, well-organized, and a joy to read.
- **KDoc explains *why* and *when to use*, not *what the code does*.** Skip KDoc when the name fully conveys intent (e.g., `fun ReminderRepository.deleteAll()`). Every class should have a brief KDoc explaining its purpose.
- Inline comments for any logic that isn't immediately obvious from reading the code.
- Use descriptive names — no abbreviations, no single-letter variables (except trivial lambdas like `it`).
- **Long functions are a signal to look for missing abstractions, not a hard limit.** If splitting reduces clarity, leave it. As a guideline, functions over ~30 lines should prompt a review.
- Follow Kotlin idioms: use scope functions, extension functions, sealed interfaces, data classes appropriately.
- **Extract named constants when the number has domain meaning** (`GEOFENCE_LOITERING_MS = 30_000`). Inline when self-evident (`list.take(1)`).
- Group related code: properties → init → public methods → private methods.
- Consistent formatting: 4-space indent, no trailing whitespace, blank line between logical sections.

---

## Key Dependency Versions

For quick reasoning about API availability. Authoritative source is `gradle/libs.versions.toml`.

| Dependency | Version |
|---|---|
| AGP | 9.1.1 |
| Kotlin | 2.2.10 |
| KSP | 2.3.2 |
| Compose BOM (phone) | 2026.03.01 |
| Wear Compose Material3 | 1.5.6 |
| Room | 2.7.2 |
| OkHttp | 4.12.0 |
| Lifecycle | 2.9.1 |
| Navigation Compose | 2.9.1 |
| kotlinx-serialization | 1.8.1 |
| Play Services Wearable | 19.0.0 |
| WorkManager | 2.10.2 |

---

## Build

```bash
# JAVA_HOME is required — no system JDK is installed
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"

./gradlew assembleDebug             # All modules
./gradlew :mobile:assembleDebug     # Mobile only
./gradlew :wear:assembleDebug       # Wear only
```

---

## Testing (MANDATORY)

### Commands

```bash
./gradlew test                      # All unit tests (both modules)
./gradlew :mobile:test              # Mobile unit tests only
./gradlew :wear:test                # Wear unit tests only

# Run a single test class
./gradlew :mobile:test --tests "com.example.reminders.formatting.GeminiFormattingProviderTest"

# Run a single test method
./gradlew :mobile:test --tests "com.example.reminders.formatting.GeminiFormattingProviderTest.format valid json returns Success"

./gradlew connectedAndroidTest      # All instrumented tests (requires running emulator)
./gradlew :mobile:connectedAndroidTest
./gradlew :wear:connectedAndroidTest
./gradlew lint                      # Static analysis
```

### CI Gate — run after every change

```bash
./gradlew test && ./gradlew lint && ./gradlew assembleDebug
```

All three must pass before considering work done. If tests fail, fix them before reporting completion. Iterate until green.

### Test Libraries

- **MockK** (`io.mockk:mockk`) — mocking: `every { }`, `coEvery { }`, `verify { }`, `slot { }`
- **Turbine** (`app.cash.turbine:turbine`) — Flow testing: `flow.test { awaitItem() }`
- **kotlinx-coroutines-test** — `runTest { }`, `TestScope`, `TestDispatcher`
- **Truth** (`com.google.truth:truth`) — assertions: `assertThat(x).isEqualTo(y)`
- **MockWebServer** (`com.squareup.okhttp3:mockwebserver`) — HTTP mocking for Gemini API tests
- **Room in-memory DB** — `Room.inMemoryDatabaseBuilder()` for DAO/repository tests
- **Compose UI test** — `createComposeRule()` for screen tests

### Test Location

- **Unit tests**: `src/test/java/...` — JVM, no Android framework
- **Instrumented tests**: `src/androidTest/java/...` — runs on emulator/device

### Test Conventions

- Test classes: `<ClassUnderTest>Test.kt`
- Test methods: descriptive sentences — `format validJson returns Success`
- One test class per production class
- Every bug fix must include a test that would have caught it
- Every new feature must include tests for: happy path, error path, edge cases

### Key Test Patterns

**ViewModel tests:**
```kotlin
class TranscriptionViewModelTest {
    private val mockManager = mockk<SpeechRecognitionManager>()
    private val viewModel = TranscriptionViewModel(mockManager)

    @Test
    fun startListening transitions to Listening state() = runTest {
        // Arrange: mock startListening to emit state changes
        // Act: call viewModel.startListening()
        // Assert: verify state is TranscriptionUiState.Listening
    }
}
```

**Repository tests (Room in-memory):**
```kotlin
@RunWith(AndroidJUnit4::class)
class ReminderDaoTest {
    private lateinit var db: RemindersDatabase
    private lateinit var dao: ReminderDao

    @Before fun createDb() {
        db = Room.inMemoryDatabaseBuilder(context, RemindersDatabase::class.java).build()
        dao = db.reminderDao()
    }
    @After fun closeDb() = db.close()

    @Test fun insert and query by id() = runTest {
        val reminder = Reminder(id = "test-1", ...)
        dao.insert(reminder)
        assertThat(dao.getById("test-1")).isEqualTo(reminder)
    }
}
```

**HTTP mock tests (MockWebServer):**
```kotlin
class GeminiFormattingProviderTest {
    private val server = MockWebServer()
    private val provider = GeminiFormattingProvider(server.url("/").toString(), "test-key")

    @After fun tearDown() = server.shutdown()

    @Test fun format valid json returns Success() = runTest {
        server.enqueue(MockResponse().setBody("""{"candidates":[{"content":{"parts":[{"text":"[{\"title\":\"Buy milk\"}]"}]}}]}"""))
        val result = provider.format("remind me to buy milk")
        assertThat((result as FormattingResult.Success).reminders).hasSize(1)
    }
}
```

**Flow tests (Turbine):**
```kotlin
@Test fun repository emits on insert() = runTest {
    val repository = ReminderRepositoryImpl(dao)
    repository.allReminders.test {
        assertThat(awaitItem()).isEmpty()
        repository.insert(testReminder)
        assertThat(awaitItem()).containsExactly(testReminder)
    }
}
```

---

## Definition of Done

Before reporting a task complete, verify:

- [ ] CI gate green: `./gradlew test && ./gradlew lint && ./gradlew assembleDebug`
- [ ] Unit tests: happy path / error path / edge cases
- [ ] Strings in `strings.xml` (both modules if UI-facing)
- [ ] New deps added to `gradle/libs.versions.toml` (never inline)
- [ ] If touches Room schema → migration written + tested
- [ ] If touches sync → both modules updated (`DataLayerPaths`, `ReminderDto`, `SyncEngine`, `SyncConflictResolver`)
- [ ] If new permission → runtime request flow + rationale UI
- [ ] If new Compose UI → supports round + square screens, tap targets ≥ 48dp

---

## When to Stop and Ask

Surface the change before applying if you're about to:

- Change the sync protocol (paths, DTOs, conflict resolution)
- Modify the Room schema (add/remove columns, change types)
- Change a public DAO method signature
- Add a new dependency
- Create a new module
- Modify the LLM prompt template (affects formatting accuracy for all users)

---

## Glossary

| Term | Meaning |
|------|---------|
| BYOM | Bring Your Own Model — custom on-device transcription. Not yet implemented. |
| AndroidBuiltIn | `SpeechRecognizer` via `ACTION_RECOGNIZE_SPEECH`. The current transcription backend. |
| Data Layer | Google Wearable Data Layer API — `DataClient` (state) + `MessageClient` (events) |
| Tombstone | Soft-delete record in `deleted_reminders` table. Enables bidirectional delete sync. |
| `FormattingProvider` | Interface for LLM-based transcript → structured reminder conversion |
| `PipelineOrchestrator` | Chains all pipeline stages: usage gate → format → persist |
| `SyncEngine` | Tombstone-based reconciliation algorithm. Implemented independently in both modules. |
| `WatchAppContainer` | Manual DI container for the wear module |
| `GeofencingDeviceManager` | Auto-switches geofence management between watch GPS and phone proxy |
