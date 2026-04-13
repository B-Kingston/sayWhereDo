# WearOS Voice Reminder App — Implementation Plan

## Context

Greenfield WearOS + mobile companion app for voice-captured reminders. Watch is standalone-capable (own Room DB, voice capture, time-based reminders, optional geofencing). Phone provides cloud formatting (BYOK LLM), geocoding, and can manage geofences on behalf of watch. Bidirectional sync via Wearable Data Layer.

**Key decisions:**
- minSdk **33** (both modules)
- Formatting: BYOK cloud API (Gemini, OpenAI, Groq, etc.) or on-device MediaPipe LLM
- Three formatting backends: Cloud (BYOK), Local (MediaPipe on-device), Raw fallback
- Limited recurrence: daily/weekly/monthly only (no RRULE library)
- Watch has own Room DB, functions standalone with reduced features
- `android.location.Geocoder` for geocoding (no HERE SDK)
- Manual DI via `AppContainer` — no Hilt/Dagger
- MVVM + Compose for both modules

---

## Completed Work

### Phases 1–4: Foundation, Transcription, Formatting, Geocoding
- Room DB + models (both modules), Compose themes, manual DI (`AppContainer`)
- `SpeechRecognitionManager`, `TranscriptionViewModel`, `TranscriptionScreen`
- `GeminiFormattingProvider`, `RawFallbackProvider`, `FormattingPrompt`, `PipelineOrchestrator`
- `AndroidGeocodingService`, `SavedPlaceMatcher`, geocoding disambiguation
- `BillingManager`, `UsageTracker`, `UserPreferences`
- All committed in `5662949`

### Phase 5: Geofencing
- `AndroidGeofenceManager` (shared impl, ENTER + DWELL, 30s loitering)
- `GeofenceBroadcastReceiver`, `GeofenceBootReceiver`, `GeofenceReregistrationWorker`
- `ReminderNotificationManager`, `LocationPermissionHandler`
- Watch: `WatchGeofenceManager`, `GeofencingDeviceManager`, `GpsDetector`, `GeofencingPreferenceScreen`
- Cap tracking: free=5, pro=100
- All remaining items + build fixes committed across `69924d7`–`f0e5074`

### Phase 6: Wearable Data Layer + Watch App
- 19 watch files: sync (`DataLayerPaths`, `ReminderDto`, `SyncConflictResolver`), data layer, `DataLayerListenerService`
- Watch UI: `WatchReminderListScreen`, `VoiceRecordScreen`, `KeyboardInputScreen`, `ReminderDetailScreen`, `ComplicationConfigScreen`
- ViewModels: `WatchReminderListViewModel`, `ReminderDetailViewModel`, `VoiceRecordViewModel`
- Complication: `ReminderComplicationProvider` (SHORT_TEXT, today/all modes)
- 6 mobile files: `WearableDataSender`, `WearableListenerServiceImpl`, `WearableSyncClient`
- `SwipeDismissableNavHost` navigation (6 routes)
- All committed in `f0e5074`

### Phase 7: Notifications + Time Reminders
- `WatchNotificationManager`, `WatchNotificationActionReceiver`, `WatchReminderCompletionManager`
- Mobile: `AlarmScheduler`, `AlarmReceiver`, `RecurrenceHelper`, `NotificationActionReceiver`, `ReminderEditScreen`
- All committed in `2a56cf1`

### Phase 8: Polish + Security
- ProGuard/R8 rules, backup rules, offline queue (WorkManager)
- Pro UI: `ProBadge`, `ProPaywallScreen`, `ErrorStateView`, `StatusIndicator`
- Export/import reminders, accessibility improvements, Settings > Pro section
- All committed in `6e54707`

### V2: Input Methods, BYOK, Local LLM
- **S1**: `FormattingResponseParser` shared utility (`d87ce1e`)
- **A1.1–A1.2**: `InputMethod` sealed interface, `InputMethodSelector` composable, refactored `VoiceRecordScreen` (`6e3035a`, `5a6582a`, `262e1a6`)
- **A1.3**: Audio streaming **stubs** only — `AudioRecorder` throws, `WearableAudioStreamer` returns null (`2c0d2ef`)
- **A2.1–A2.2**: `MicMethod` sealed interface, `AddNoteFab`, `KeyboardInputScreen` + ViewModel (`9157f10`, `2a5cd34`, `ea92c2c`, `eeba0a7`)
- **B1.1–B1.2**: `OpenAiCompatibleClient`, `CloudFormattingProvider` (`69b0491`, `1ff48c3`)
- **B2.1–B2.3**: `AiProvider` presets, unified AI settings UI, wired into `AppContainer` + `PipelineOrchestrator` (`4b4dbb8`, `ef29317`, `b356c93`)
- **B3.1–B3.3**: OkHttp in wear, credential sync (phone→watch), watch standalone cloud formatting (`27c3a0f`, `871c51b`, `6dd33c2`)
- **C1.1–C1.2**: `DeviceCapabilityChecker`, `ModelInfo`, local model settings section (`0d870ab`, `95a4343`)
- **C2.1–C2.5**: MediaPipe dependency, `LocalModelManager`, `LocalFormattingProvider`, wired into pipeline with memory management, model-specific prompt tuning (`2b5e076`, `299ad10`, `fc6e0a0`, `80aec1d`, `38d7b39`)

### Build Fixes + Logging
- 16 compilation fixes across both modules (see git log `f0e5074`–`ecd427f`)
- 24 files with thorough `android.util.Log` logging
- All test failures fixed (89/136 → 0) in `73e6fb9`
- Regex + offline speech fixes in `b57fa40`

---

## Remaining Work

### 1. Audio Streaming (Watch → Phone Transcription) — STUBS ONLY

Files exist but are non-functional stubs. Need full implementation:
- `wear/.../audio/AudioRecorder.kt` — replace `throw UnsupportedOperationException` with real `AudioRecord` PCM capture at 16kHz mono 16-bit, chunked into 32KB payloads
- `wear/.../audio/WearableAudioStreamer.kt` — use `ChannelClient.openChannel()` + `sendFile()` (NOT `MessageClient` which has ~100KB limit)
- Create: `wear/.../ui/screen/StreamToPhoneScreen.kt` — recording indicator, cancel button, status
- Create: `wear/.../ui/viewmodel/StreamToPhoneViewModel.kt`
- Create: `mobile/.../wearable/AudioStreamReceiver.kt` — receives PCM via Channel API, transcribes on phone, sends result back
- Add audio channel paths to `DataLayerPaths.kt`:
  ```
  /audio-stream/start, /audio-stream/data, /audio-stream/end, /audio-stream/result
  ```

**Pitfalls:**
- `MessageClient` has ~100KB limit. Must use `ChannelClient` for audio.
- `AudioRecord` requires `RECORD_AUDIO` permission on watch.
- PCM at 16kHz/16-bit/mono = ~32KB/sec. Chunk into 32KB payloads.

### 2. Update AGENTS.md — NOT DONE

Current AGENTS.md is outdated. Must update:
- Remove "Do not add on-device LLM deps (MediaPipe, llama.cpp)"
- Relax "Wear module never makes network calls" → permitted for user-initiated cloud formatting
- Document three formatting backends: Cloud (BYOK), Local (on-device), Raw fallback
- Document credential sync flow from phone to watch
- Update pipeline diagram: `TRANSCRIPTION → FORMATTING (cloud/local/raw) → GEOCODING → GEOFENCE → ROOM`

### 3. Missing Tests

**Phase 5 tests (none written):**
- `AndroidGeofenceManagerTest` — mocked `GeofencingClient`, correct transitions/loitering/caps
- `GeofenceCapTrackerTest` — free=5/pro=100 thresholds, warnings at 4/90
- `GeofencingDeviceManagerTest` — auto-switch state transitions, idempotency
- `GeofenceReregistrationWorkerTest` — boot → re-register cycle

**Phase 6 tests (only `WatchConvertersTest` exists):**
- `ReminderSerializerTest` — ByteArray round-trip
- `SyncConflictResolverTest` — last-write-wins
- `DataLayerPathsTest` — path construction/parsing
- `ReminderComplicationProviderTest` — count modes, filtering

**V2 tests missing:**
- `AudioRecorderTest`, `AudioStreamReceiverTest`
- `CredentialSyncSenderTest`, `CredentialSyncReceiverTest`
- `WatchFormattingManagerTest`
- `DeviceCapabilityCheckerTest` (exists but verify coverage)
- `LocalModelManagerTest` (exists but verify coverage)

### 4. Minor Gaps
- Mobile `ReminderListScreen` may still use hardcoded empty state (needs ViewModel wiring verification)
- `ReminderEditScreen` geocoding confirmation flow not wired into navigation
- Watch recurrence handling deferred until Pro status sync is verified
- Phone-side `AudioStreamReceiver` does not exist yet (part of audio streaming task)

---

## Phase 9: Backend, Accounts & Cloud Features (DEFERRED — V2+)

Not part of V1. All V1 features work without a backend.

### Architecture
- **Auth**: Firebase Auth via Credential Manager + Google Sign-In (free, unlimited users)
- **Cloud DB**: Cloud Firestore (Spark free tier: 50K reads/day, 20K writes/day)
- **Serverless**: Firebase Cloud Functions (requires Blaze plan, 2M free invocations/month)
- **No dedicated server** — entirely serverless

### What to Build

#### 9.1: Google Sign-In
- Dependencies: `credentials`, `credentials-play-services-auth`, `googleid`, `firebase-auth`
- `AuthManager` wrapping Credential Manager → Firebase Auth
- Sign-in screen on phone, identity sync to watch via Data Layer

#### 9.2: Cloud Backup & Cross-Device Sync
- Firestore collections: `users/{uid}/reminders`, `users/{uid}/savedPlaces`, `users/{uid}/preferences`
- `CloudSyncManager`: bidirectional Room ↔ Firestore, conflict resolution via `lastUpdatedAt`
- Watch still uses Data Layer for real-time; Firestore is cloud mirror

#### 9.3: Proxied Gemini API (Serverless)
- Firebase Cloud Function `formatReminder(transcript, uid)` — validates auth, checks server-side usage counter, calls Gemini with function's own key
- `ServerFormattingProvider` — calls Cloud Function instead of direct Gemini
- User no longer needs to enter API key. BYO key option remains.

#### 9.4: Server-Side Purchase Validation
- Cloud Function `validatePurchase()` calls Play Developer API
- Pro status stored in Firestore — eliminates rooted-device spoofing

#### 9.5: Subscription Migration
- Replace one-time `pro_upgrade` with monthly/annual subscription + 7-day trial
- Grandfather existing one-time purchasers

### Migration Path V1 → V2
1. Add Firebase (google-services.json, plugin)
2. Sign-in screen → merge local Room with Firestore
3. Swap `CloudFormattingProvider` → `ServerFormattingProvider`
4. Swap `BillingManager` → `SubscriptionManager`
5. Add `CloudSyncManager` alongside Data Layer sync

### V2 Dependencies (DO NOT add in V1)
```toml
credentials = "1.5.0"
googleid = "1.1.1"
firebaseAuth = "23.2.1"
firebaseBom = "33.x.x"
```

---

## Architecture Reference

### Pipeline
```
TRANSCRIPTION → FORMATTING (cloud/local/raw) → GEOCODING → GEOFENCE REGISTRATION → ROOM STORAGE
```
Each stage is a separate abstraction. Geocoding is phone-only. Formatting has three backends.

### Modules
- `mobile/` — All compute: transcription, formatting (cloud+local), geocoding, geofencing, Room DB, notifications, billing
- `wear/` — WearOS UI, voice capture, own Room DB, standalone geofencing, optional direct cloud formatting
- Watch ↔ phone via **Wearable Data Layer API** (`DataClient` / `MessageClient` / `ChannelClient`)

### Formatting Providers
| Provider | Class | Backend | Notes |
|----------|-------|---------|-------|
| Cloud BYOK | `CloudFormattingProvider` | Phone | Any OpenAI-compatible API (Gemini, OpenAI, Groq, Together, Ollama, etc.) |
| Local on-device | `LocalFormattingProvider` | Phone | MediaPipe LLM Inference API, Gemma 2 2B Q4 |
| Watch standalone | `WatchFormattingManager` | Watch | Direct cloud API call from watch (credentials synced from phone) |
| Raw fallback | `RawFallbackProvider` | Both | Saves raw transcript as unformatted reminder |

### AI Provider Presets
| Provider | Base URL | Default Model | Free Tier |
|----------|----------|---------------|-----------|
| Gemini | `generativelanguage.googleapis.com/v1beta/openai` | gemini-2.5-flash-lite | Yes |
| OpenAI | `api.openai.com/v1` | gpt-4o-mini | No |
| Groq | `api.groq.com/openai/v1` | llama-3.1-8b-instant | Yes |
| Together AI | `api.together.xyz/v1` | Meta-Llama-3.1-8B-Instruct-Turbo | Yes |
| Ollama (local) | `localhost:11434/v1` | gemma2:2b | Yes (self-hosted) |
| Custom | (user-entered) | (user-entered) | Varies |

### Monetization: Free vs Pro

| Feature | Free | Pro |
|---------|------|-----|
| Time-based reminders | Unlimited | Unlimited |
| Voice capture + raw text | Unlimited | Unlimited |
| Watch ↔ Phone sync | Yes | Yes |
| LLM formatting (BYO key) | 1/day | Unlimited |
| LLM formatting (no key) | Unlimited (raw) | Unlimited |
| Active geofences | 5 | 100 |
| Saved Places | 2 | Unlimited |
| Recurring reminders | No | Yes |
| Snooze actions | No | Yes |
| Custom geofence radius | No (150m fixed) | Yes |
| Export/import | No | Yes |

One-time "Pro Upgrade" IAP (`pro_upgrade`). No sign-in required. Billing via `BillingClient`. Pro status synced to watch via Data Layer.

### Pro Enforcement Points
| Gate | Enforcement |
|------|-------------|
| `PipelineOrchestrator` | `UsageTracker` + Pro status before formatting. Free + count ≥ 1 → paywall |
| `SavedPlacesScreen` | Block creation if free + count ≥ 2 |
| `GeofenceManager` | Block registration if free + active ≥ 5 |
| `ReminderEditScreen` | Grey out recurrence, disable radius slider if free |
| `NotificationActionReceiver` | Snooze actions hidden on free |
| Settings > Export | Button disabled with Pro badge |

---

## UX Failure Matrix

| Stage | Failure | User Sees | Recovery |
|-------|---------|-----------|----------|
| Transcription | SpeechRecognizer error | "Could not recognize speech. Try again?" | Retry button |
| Formatting | Network/API error | "Could not parse reminder. Saved as note." | Raw reminder saved |
| Formatting | No API key | "Set up cloud formatting in Settings." | Raw reminder saved, settings link |
| Formatting | Usage limit (free) | "Daily limit reached. Upgrade or add your own key." | Raw reminder saved, paywall CTA |
| Geocoding | No results | "Could not find [place]. Search manually?" | Manual search |
| Geocoding | No internet | "Could not look up location. Will retry." | PENDING_GEOCODING, WorkManager |
| Geocoding | Ambiguous | "Multiple matches. Which one?" | Disambiguation screen |
| Geofence | Cap reached (free: 5) | "Location reminder limit reached." | Upgrade CTA |
| Geofence | Cap reached (Pro: 100) | "Too many. Complete some first." | Link to active list |
| Geofence | Permission denied | "Background location needed." | Settings link |
| Geofence | Watch GPS unavailable | "Location reminders will run on phone." | Auto PHONE_ONLY |
| Geofence | GEOFENCE_NOT_AVAILABLE (1000) | "Turn on location on your watch." | Settings link |
| Data Layer | Phone unreachable | "Waiting for phone..." + banner | Auto-retry, local save |
| Watch standalone | No speech recognizer | "Type your reminder" | Keyboard fallback |

---

## Dependency Summary

| Library | Version | Module | Purpose |
|---------|---------|--------|---------|
| Room + KSP2 | 2.8.x / 2.3.x | both | Local database |
| Compose BOM (phone) | latest stable | mobile | Phone UI |
| Compose BOM (wear) | 2024.09.00 | wear | Watch UI |
| Wear Compose Navigation | 1.5.x | wear | Watch navigation |
| kotlinx-serialization | 1.7.x | both | JSON parsing (manual TypeConverters, NOT @Serializable on Room entities) |
| OkHttp | 4.12.x | both | HTTP client (mobile: Gemini/BYOK, wear: standalone cloud) |
| security-crypto | 1.1.x | mobile | Encrypted API key storage |
| play-services-location | 21.3.x | both | Geofencing |
| WorkManager | 2.10.x | both | Boot re-registration, offline retry |
| DataStore Preferences | 1.1.x | both | User preferences, Pro status, usage counter |
| Play Billing Library | 8.x | mobile | One-time Pro Upgrade IAP |
| MediaPipe tasks-genai | 0.10.14 | mobile | On-device LLM inference |
| Turbine | 1.2.x | both (test) | Flow testing |
| coroutines-test | 1.9.x | both (test) | Coroutine testing |
| MockK | 1.13.13 | both (test) | Mocking |
| Truth | 1.4.4 | both (test) | Assertions |
| MockWebServer | 4.12.0 | mobile (test) | HTTP mocking |

---

## Testing

### Commands
```bash
./gradlew test                        # All unit tests
./gradlew :mobile:test                # Mobile unit tests only
./gradlew :wear:test                  # Wear unit tests only
./gradlew connectedAndroidTest        # All instrumented tests (requires emulator)
./gradlew lint                        # Static analysis
```

### CI Gate
```bash
./gradlew test && ./gradlew lint && ./gradlew assembleDebug
```

### Test Directory Structure
```
mobile/src/test/java/com/example/reminders/
├── data/local/ConvertersTest.kt
├── data/repository/ReminderRepositoryImplTest.kt, SavedPlaceRepositoryImplTest.kt
├── ui/viewmodel/TranscriptionViewModelTest.kt, KeyboardInputViewModelTest.kt
├── formatting/GeminiFormattingProviderTest.kt, CloudFormattingProviderTest.kt,
│   RawFallbackProviderTest.kt, FormattingResponseParserTest.kt, FormattingPromptTest.kt
├── pipeline/PipelineOrchestratorTest.kt
├── geocoding/SavedPlaceMatcherTest.kt, GeocodingPipelineStepTest.kt
├── alarm/AlarmSchedulerTest.kt, RecurrenceHelperTest.kt, ReminderCompletionManagerTest.kt
├── network/OpenAiCompatibleClientTest.kt
├── ml/DeviceCapabilityCheckerTest.kt, LocalModelManagerTest.kt, LocalFormattingProviderTest.kt
└── sync/NoOpSyncClientTest.kt

wear/src/test/java/com/example/reminders/wear/
└── data/WatchConvertersTest.kt
```

### Test Libraries
- **MockK** — mocking: `every { }`, `coEvery { }`, `verify { }`
- **Turbine** — Flow testing: `flow.test { awaitItem() }`
- **kotlinx-coroutines-test** — `runTest { }`, `TestScope`, `TestDispatcher`
- **Truth** — `assertThat(x).isEqualTo(y)`
- **MockWebServer** — HTTP mocking for API tests
- **Room in-memory DB** — `Room.inMemoryDatabaseBuilder()` for DAO/repository tests

### Key Test Patterns

**ViewModel tests:**
```kotlin
class TranscriptionViewModelTest {
    private val mockManager = mockk<SpeechRecognitionManager>()
    private val viewModel = TranscriptionViewModel(mockManager)

    @Test
    fun startListening transitions to Listening state() = runTest { ... }
}
```

**Repository tests (Room in-memory):**
```kotlin
@RunWith(AndroidJUnit4::class)
class ReminderDaoTest {
    private lateinit var db: RemindersDatabase
    @Before fun createDb() { db = Room.inMemoryDatabaseBuilder(context, RemindersDatabase::class.java).build() }
    @After fun closeDb() = db.close()
    @Test fun insert and query by id() = runTest { ... }
}
```

**HTTP mock tests (MockWebServer):**
```kotlin
class GeminiFormattingProviderTest {
    private val server = MockWebServer()
    @After fun tearDown() = server.shutdown()
    @Test fun format valid json returns Success() = runTest {
        server.enqueue(MockResponse().setBody("""{"candidates":[{"content":{"parts":[{"text":"..."}]}}]}"""))
        val result = provider.format("remind me to buy milk")
        assertThat((result as FormattingResult.Success).reminders).hasSize(1)
    }
}
```

**Flow tests (Turbine):**
```kotlin
@Test fun repository emits on insert() = runTest {
    repository.allReminders.test {
        assertThat(awaitItem()).isEmpty()
        repository.insert(testReminder)
        assertThat(awaitItem()).containsExactly(testReminder)
    }
}
```

---

## Critical Rules & Pitfalls

### Build
- All dependency versions in `gradle/libs.versions.toml` — never inline
- **JAVA_HOME** must be set for all gradle commands (see AGENTS.md)
- minSdk 33 for both modules

### Room
- **Never use `fallbackToDestructiveMigration()`** — deprecated in Room 2.7.0+. Use explicit `Migration` objects
- **Do NOT use `@Serializable` on Room entities or `@Embedded` types** — KSP2 bug silently drops columns. Use manual TypeConverters with `Json.encodeToString()`/`Json.decodeFromString()`
- **`Instant` converter**: Use `Long` (not `Int`) to avoid Year 2038

### Compose
- All strings in `strings.xml`
- All Wear Compose UI supports round and square screens. Tap targets ≥ 48dp
- No business logic in Composables
- `sealed interface` (not `sealed class`) for UI state

### Geofencing
- `setLoiteringDelay(30_000)` on all geofences. Use `GEOFENCE_TRANSITION_DWELL` alongside `ENTER`
- Never request `ACCESS_BACKGROUND_LOCATION` at same time as fine location
- Always remove geofences when reminder is deleted or completed
- Never silently pick first geocoding result — always show disambiguation UI
- Handle `GEOFENCE_NOT_AVAILABLE` (ApiException code 1000) on watch

### Speech Recognition
- `ACTION_RECOGNIZE_SPEECH` does NOT require `RECORD_AUDIO` permission, but availability varies by OEM watch. Always check `resolveActivity()` and provide keyboard fallback
- `EXTRA_PREFER_OFFLINE` causes `ERROR_LANGUAGE_UNAVAILABLE` on devices without offline speech packs. Retry without the hint

### Regex
- **Regex in Kotlin raw strings (`"""`)**: Backslashes are literal — escape regex metacharacters with single `\`, not `\\`

### Formatting
- Never hardcode API keys. Use BuildConfig or user-supplied preferences
- LLM JSON may come in markdown fences — strip ```json ... ``` wrappers
- Never discard user speech — formatting failure → save raw transcript

### Wear Module Network
- Wear module should not make network calls by default. Network calls permitted only for user-initiated cloud formatting when user explicitly selects "Cloud format on watch"

---

## minSdk 33 Implications
- `Geocoder` listener-based API available
- `POST_NOTIFICATIONS` permission required and available
- `SpeechRecognizer.isOnDeviceRecognitionAvailable()` available
- WearOS 4+ (API 33) covers all modern watches
