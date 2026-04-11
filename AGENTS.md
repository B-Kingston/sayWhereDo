# Reminders — WearOS + Mobile App

## Project Overview
Voice-captured reminder app with WearOS watch + Android phone companion. Watch is standalone-capable (own Room DB, voice input, time-based reminders, optional geofencing). Phone provides cloud formatting (Gemini 2.5 Flash Lite LLM), geocoding, and can manage geofences on behalf of watch. Bidirectional sync via Wearable Data Layer.

## Modules
- `mobile/` — All compute: transcription, formatting, geocoding, geofencing, Room DB, notifications.
- `wear/` — WearOS UI, voice capture, own Room DB, standalone geofencing (if GPS present), time-based alarms.
- Watch ↔ phone via **Wearable Data Layer API** (`DataClient` / `MessageClient`).
- Do NOT create new modules without being asked.

## Code Quality
- **Stellar code quality is non-negotiable.** Every file, class, and function should be clean, well-organized, and a joy to read.
- **Comments are required.** Every class and non-trivial function must have a KDoc comment explaining its purpose. Inline comments for any logic that isn't immediately obvious from reading the code.
- Use descriptive names — no abbreviations, no single-letter variables (except trivial lambdas like `it`).
- Functions should be small and do one thing. If a function exceeds ~30 lines, split it.
- Follow Kotlin idioms: use scope functions, extension functions, sealed interfaces, data classes appropriately.
- No magic numbers — extract named constants or `companion object` values.
- Group related code: properties → init → public methods → private methods.
- Consistent formatting: 4-space indent, no trailing whitespace, blank line between logical sections.

## Architecture
- **MVVM**. Coroutines + Flow only (no RxJava).
- **Compose** for both phone and watch UI.
- No business logic in Composables.
- `sealed interface` (not `sealed class`) for UI state.
- All strings in `strings.xml`.
- Manual DI via `AppContainer` — no Hilt/Dagger.
- All dependency versions in `gradle/libs.versions.toml` — never inline.

## Build
```bash
./gradlew assembleDebug             # All modules
./gradlew :mobile:assembleDebug
./gradlew :wear:assembleDebug
```

## Testing (MANDATORY)

### Commands
```bash
./gradlew test                      # All unit tests (both modules)
./gradlew :mobile:test              # Mobile unit tests only
./gradlew :wear:test                # Wear unit tests only
./gradlew connectedAndroidTest      # All instrumented tests (requires emulator)
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

## Pipeline
```
TRANSCRIPTION → FORMATTING → GEOCODING → GEOFENCE REGISTRATION → ROOM STORAGE
```
Each stage is a separate abstraction. Never conflate them. Formatting and geocoding are phone-only.

## Key Models
```kotlin
data class Reminder(
    val id: String,                    // UUID
    val title: String,
    val body: String?,
    val triggerTime: Instant?,
    val recurrence: String?,           // "daily" | "weekly" | "monthly" | null
    val locationTrigger: LocationTrigger?,
    val sourceTranscript: String,
    val formattingProvider: String,    // "cloud" | "none"
    val geofencingDevice: String       // "phone" | "watch"
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

## Geofencing Device Preferences
```kotlin
enum class GeofencingDevice {
    AUTO,       // phone when connected, watch when disconnected
    PHONE_ONLY, // always phone
    WATCH_ONLY  // always watch (requires GPS hardware)
}
```
Auto-switch: phone disconnect → geofences migrate to watch. Phone reconnect → migrate back to phone to save battery.

## Formatting Provider
- Default: **Gemini 2.5 Flash Lite** (free, no credit card, dynamic per-project rate limits — check AI Studio)
- API key stored in `EncryptedSharedPreferences`, user enters in Settings
- No key → raw transcript saved as single unformatted reminder (never discard user speech)
- `FormattingProvider` interface enables swapping providers with one class change

## Critical Rules
- Do not add on-device LLM deps (MediaPipe, llama.cpp) — local formatting is future work.
- Do not implement BYOM transcription until AndroidBuiltIn works end-to-end.
- Never request `ACCESS_BACKGROUND_LOCATION` at the same time as fine location.
- Never silently pick first geocoding result — always show disambiguation UI.
- Always remove geofences when a reminder is deleted or completed.
- Never hardcode API keys. Use BuildConfig or user-supplied preferences.
- Wear module never makes network calls.
- `setLoiteringDelay(30_000)` on all geofences. Use `GEOFENCE_TRANSITION_DWELL` alongside `ENTER`.
- All Wear Compose UI supports round and square screens. Tap targets ≥ 48dp.
- All dependency versions in `gradle/libs.versions.toml` — never inline.
- Never use `fallbackToDestructiveMigration()` in Room — formally deprecated in Room 2.7.0+. Use explicit `Migration` objects.
- minSdk 33 for both modules.
- **Do NOT use `@Serializable` on Room entities or `@Embedded` types** — KSP2 has an open bug where `@Embedded` silently drops all columns for `@Serializable` types. Use manual TypeConverters with `Json.encodeToString()`/`Json.decodeFromString()`.
- Handle `GEOFENCE_NOT_AVAILABLE` (ApiException code 1000) on watch — occurs when location is turned off or GPS absent.
- `ACTION_RECOGNIZE_SPEECH` does NOT require `RECORD_AUDIO` permission (system activity handles mic), but availability varies by OEM watch (unreliable on Samsung Galaxy Watch 4/5). Always check `resolveActivity()` and provide keyboard fallback.

## Monetization (V1 — No Backend)

### Billing
- One-time "Pro Upgrade" IAP via Play Billing Library 8.x (`pro_upgrade` product)
- No Google Sign-In required — `BillingClient` uses device Google Play account
- Phone handles all billing; Pro status synced to watch via Data Layer
- Check Pro status on app start: `BillingClient.queryPurchasesAsync()`
- Store cached Pro status in DataStore; re-validate against Play on each launch
- Must call `enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())` during BillingClient setup

### Free Tier Limits
| Feature | Free | Pro |
|---|---|---|
| Time-based reminders | Unlimited | Unlimited |
| Voice capture + raw text | Unlimited | Unlimited |
| Watch ↔ Phone sync | Yes | Yes |
| LLM formatting (BYO key) | 1/day | Unlimited |
| Active location reminders | 5 | 100 |
| Saved Places | 2 | Unlimited |
| Recurring reminders | No | Yes |
| Snooze on notifications | No | Yes |
| Custom geofence radius | No (150m fixed) | Yes |
| Export/import | No | Yes |

### Usage Counter
- `UsageTracker` in DataStore: `{lastResetDate: LocalDate, count: Int}`
- Free: 1 formatting call per day (BYO API key bypasses counter entirely)
- Counter file included in Android Auto-Backup; Room DB excluded
- Known limitation: counter resets on clear data/uninstall — acceptable for V1

### Enforcement Rules
- **Formatting**: check `UsageTracker` before Gemini call. Free + count ≥ 1 → `UsageLimited` → save raw fallback + show paywall
- **Saved Places**: block creation if free + count ≥ 2. Show upgrade CTA
- **Geofences**: block registration if free + active ≥ 5 (Pro cap = 100). Show upgrade CTA
- **Recurrence**: option greyed out with Pro badge for free users
- **Snooze**: excluded from notification actions entirely for free users (not hidden, excluded)
- **Radius**: slider disabled, fixed 150m for free users
- **Export**: button disabled with Pro badge for free users
- **Downgrade (preserve-and-freeze)**: if purchase revoked, preserve existing Pro data (recurring reminders keep firing, geofences beyond cap 5 stay registered). User cannot create NEW Pro features. Show banner.

### V2 (Deferred)
- Backend via Firebase (Auth, Firestore, Cloud Functions)
- **Cloud Functions require Blaze plan** (credit card needed) — 2M free invocations/month on Blaze
- Firestore free tier (Spark): 50K reads/day, 20K writes/day, 1GB storage
- Firebase Auth (Google Sign-In) is free with unlimited users
- Proxied Gemini API (no user API key needed)
- Subscription model (monthly/annual) replacing one-time IAP
- Server-side purchase validation + server-tracked usage counters
- See Phase 9 in implementation plan for full details
