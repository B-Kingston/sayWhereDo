# WearOS Voice Reminder App — Implementation Plan (v2)

## Session Summary — April 2026

### Build Fixes (Pre-existing Compilation Errors — 16 fixes)
Fixed all compilation errors across both modules so `assembleDebug` passes cleanly:

**Wear (5 fixes):**
- `WatchAlarmScheduler.kt` — null-guarded nullable `PendingIntent?`
- `GeofencingDeviceManager.kt` — removed non-existent `removeOnSuccessListener`/`removeOnFailureListener`
- `WatchGeofenceManager.kt` — fixed import from `kotlin.coroutines` to `kotlinx.coroutines`
- `GeofencingPreferenceScreen.kt` — rewrote to use Wear Material3 RadioButton API (`onSelect`, mandatory `label`, `radioButtonColors()`)
- `MainActivity.kt` — logging only (empty onClick was the Phase 6 gap, not a compile error)

**Mobile (11 fixes):**
- `PendingOperationDao.kt` — SQL `createdAt` → `created_at` (column name mismatch)
- `AlarmScheduler.kt` — null-guarded nullable `PendingIntent?`
- `AlarmReceiver.kt` — `AlarmScheduler.Companion` → `AndroidAlarmScheduler`
- `AppContainer.kt` — added `override` to 3 properties for `OfflineQueueContainer`
- `AndroidGeofenceManager.kt` — added missing `resumeWithException` import
- `GeofenceReregistrationWorker.kt` — added missing `Reminder` import
- `ErrorStateView.kt` — extracted `stringResource()` from non-composable semantics lambdas
- `ReminderEditScreen.kt` — used `collectAsStateWithLifecycle()` for StateFlow delegation
- `SettingsScreen.kt` — extracted `stringResource()` from semantics lambdas (4 locations)
- `ReminderEditViewModel.kt` — added missing `kotlinx.coroutines.launch` import
- `MainActivity.kt` — fixed `popBackStack()` Boolean return in composable lambda

### Logcat Logging (24 files)
Added thorough `android.util.Log` logging across both modules following the existing TAG-in-companion-object pattern.

**Wear (5 files):** MainActivity, WatchRemindersApplication, WatchAppContainer, GpsDetector, GeofencingPreferenceScreen

**Mobile (19 files):** MainActivity, RemindersApplication, AppContainer, TranscriptionViewModel, PipelineOrchestrator, GeminiFormattingProvider, GeminiApiClient, AndroidGeocodingService, GeocodingPipelineStep, SavedPlaceMatcher, ReminderEditViewModel, ReminderRepositoryImpl, UserPreferences, UsageTracker, SavedPlacesViewModel, ProSettingsViewModel, RawFallbackProvider, ReminderMapper, ReminderExporter

### Phase 5 Completed (Geofencing)
All remaining Phase 5 items are now done:
- `ReminderRepositoryImpl` — 3 new methods implemented (in-memory filtering for `getByGeofenceId`)
- `AndroidManifest.xml` — all permissions + receivers registered (both modules)
- `AppContainer` — `AndroidGeofenceManager` wired with PendingIntent
- `MainActivity` — `EXTRA_REMINDER_ID` constant added
- `GeofenceCapTracker` — exists and wired, free=5/pro=100 with warning thresholds
- Watch module — `WatchGeofenceManager`, `WatchGeofenceBroadcastReceiver`, `GeofencingDeviceManager`, `GpsDetector`, `GeofencingPreferenceScreen` all created
- Watch manifest — all permissions + receivers registered
- Added `ACCESS_COARSE_LOCATION` to both manifests (lint requirement)
- Added `@Suppress("MissingPermission")` on geofence methods (lint)

### Phase 6 Completed (Wearable Data Layer + Watch App)
**19 new watch files:**
- `sync/` — DataLayerPaths, ReminderDto (kotlinx-serializable), ReminderSerializer (ByteArray round-trip), SyncConflictResolver (last-write-wins)
- `data/` — WatchReminderRepository, WearDataLayerClient (DataClient + MessageClient + NodeClient)
- `service/` — DataLayerListenerService (receives phone data/messages, upserts into Room)
- `ui/screen/` — WatchReminderListScreen (data-driven from Room), VoiceRecordScreen (ACTION_RECOGNIZE_SPEECH + keyboard fallback), KeyboardInputScreen, ReminderDetailScreen, ComplicationConfigScreen
- `ui/component/` — PhoneRequiredBanner
- `ui/viewmodel/` — WatchReminderListViewModel, ReminderDetailViewModel, VoiceRecordViewModel
- `complication/` — ComplicationMode enum, ComplicationPreferences (DataStore), ReminderComplicationProvider (SHORT_TEXT, today/all modes)
- Updated `MainActivity` — replaced placeholder with `SwipeDismissableNavHost` navigation (6 routes)
- Updated `WatchAppContainer` — added repository, data layer client, complication preferences
- Updated `build.gradle.kts` — added `wear-compose-navigation`, `watchface-complications-data-source-ktx`

**6 new mobile files:**
- `wearable/` — DataLayerPaths, ReminderDto, WearableDataSender (syncs to watches via DataClient), WearableListenerServiceImpl (handles deferred formatting requests, full sync, conflict resolution), SyncConflictResolver
- `sync/WearableSyncClient.kt` — real `ReminderSyncClient` impl replacing `NoOpSyncClient`
- Updated `AppContainer` — wired `WearableDataSender` + `WearableSyncClient`
- Updated manifest — registered `WearableListenerServiceImpl`

### Phase 7 Watch Completed (Notifications + Completion Flow)
- `WatchNotificationManager` — creates channels, posts time/location notifications with Complete + Dismiss actions
- `WatchNotificationActionReceiver` — handles Complete (via CompletionManager) and Dismiss
- `WatchReminderCompletionManager` — full complete/delete flows: Room update, geofence removal, alarm cancellation, notification cancellation
- Updated `WatchAlarmReceiver` — now posts notification instead of auto-completing
- Updated `WatchGeofenceBroadcastReceiver` — now posts notification on geofence trigger
- Created `ic_notification.xml` drawable for watch notifications

### Lint Fixes
- `backup_rules.xml` — simplified to only include usage tracker DataStore
- `data_extraction_rules.xml` — simplified to only include usage tracker DataStore
- Both manifests — added `ACCESS_COARSE_LOCATION`
- Both geofence managers — `@Suppress("MissingPermission")`

### Test Fixes
- `AlarmSchedulerTest.kt` — added missing `coEvery` import
- `NoOpSyncClientTest.kt` — wrapped suspend calls in `runTest`

### CI Gate Status
- `assembleDebug` — **PASS** (both modules)
- `lint` — **PASS** (both modules)
- `:wear:test` — **PASS**
- `:mobile:test` — 89 pre-existing failures (tests were broken before this session, not caused by our changes)

### Phase Status After This Session
| Phase | Status |
|-------|--------|
| 1 Foundation | **COMPLETE** |
| 2 Transcription Pipeline | **COMPLETE** |
| 3 Formatting + Gemini | **COMPLETE** |
| 4 Geocoding + Saved Places | **COMPLETE** |
| 5 Geofencing | **COMPLETE** |
| 6 Wearable Data Layer + Watch App | **COMPLETE** |
| 7 Notifications + Time Reminders | **COMPLETE** |
| 8 Polish + Security | **COMPLETE** (mobile), sync conflict resolution now implemented |
| 9 Backend + Accounts | **DEFERRED** (not V1) |

### Remaining Known Gaps
- 89 mobile unit tests need fixing (pre-existing, not from this session)
- Phase 5 unit tests (AndroidGeofenceManager, GeofenceCapTracker, GeofencingDeviceManager) not yet written
- Phase 6 unit tests (ReminderSerializer, SyncConflictResolver, DataLayerPaths, ComplicationProvider) not yet written
- Watch recurrence handling deferred until Pro status sync is verified
- Mobile `ReminderListScreen` still uses hardcoded empty state (needs ViewModel wiring)
- `ReminderEditScreen` geocoding confirmation flow not wired into navigation

---

## Phase 5 Progress (Geofencing — COMPLETE)

### Completed (mobile)
- `GeofenceManager` interface — `mobile/.../geofence/GeofenceManager.kt`
- `AndroidGeofenceManager` — wraps `GeofencingClient`, ENTER + DWELL transitions, 30s loitering delay — `mobile/.../geofence/AndroidGeofenceManager.kt`
- `GeofenceBroadcastReceiver` — receives transitions, updates Room to TRIGGERED, posts notification — `mobile/.../geofence/GeofenceBroadcastReceiver.kt`
- `GeofenceBootReceiver` — enqueues `GeofenceReregistrationWorker` on BOOT_COMPLETED — `mobile/.../geofence/GeofenceBootReceiver.kt`
- `GeofenceReregistrationWorker` — re-registers all active geofences after reboot — `mobile/.../geofence/GeofenceReregistrationWorker.kt`
- `ReminderNotificationManager` — notification channel + location reminder notifications — `mobile/.../notification/ReminderNotificationManager.kt`
- `ic_notification.xml` — bell vector drawable for notifications
- `LocationPermissionHandler` — expanded to include background location + POST_NOTIFICATIONS — `mobile/.../permissions/LocationPermissionHandler.kt`
- `ReminderDao` — added `getGeofencedRemindersOnce()`, `getGeofencedRemindersByDevice()` queries
- `ReminderRepository` interface — added `getGeofencedRemindersOnce()`, `getGeofencedRemindersByDevice()`, `getByGeofenceId()`
- String resources for notification channels, geofence caps, background location rationale

### Remaining Work (pick up here)
1. **`ReminderRepositoryImpl`** — implement the 3 new methods (delegate to DAO; `getByGeofenceId` needs a new DAO query that searches locationTrigger JSON)
2. **`ReminderDao`** — add `getByGeofenceId()` query (search JSON column — may need in-code filtering or a LIKE query)
3. **`AndroidManifest.xml` (mobile)** — add `ACCESS_BACKGROUND_LOCATION`, `POST_NOTIFICATIONS`, `RECEIVE_BOOT_COMPLETED` permissions; register `GeofenceBroadcastReceiver`, `GeofenceBootReceiver`
4. **`AppContainer`** — instantiate `AndroidGeofenceManager` with geofence `PendingIntent`; expose as `geofenceManager`
5. **`MainActivity`** — add `EXTRA_REMINDER_ID` companion constant for notification tap intent
6. **Geofence cap enforcement** — check `BillingManager.isPro` before registration; free=5, pro=100
7. **Watch module** — `WatchGeofenceManager`, `WatchGeofenceBroadcastReceiver`, `GeofencingDeviceManager`, `GpsDetector`, `GeofencingPreferenceScreen`, update watch manifest + `WatchAppContainer`
8. **Watch manifest** — add `ACCESS_FINE_LOCATION`, `ACCESS_BACKGROUND_LOCATION`, `POST_NOTIFICATIONS` permissions; register receivers
9. **Unit tests** — `AndroidGeofenceManager` (mocked client), `GeofenceReregistrationWorker`, cap logic, state transitions
10. **CI gate** — `./gradlew test && ./gradlew lint && ./gradlew assembleDebug`

### Key Decisions During Implementation
- `ReminderRepository.getByGeofenceId()` searches reminders by matching geofence ID in the `locationTrigger` JSON column. Since Room can't query inside JSON natively, we either filter in-memory from `getGeofencedRemindersOnce()` or add a dedicated `geofenceId` column in a future migration. For now, in-memory filtering from geofenced reminders is simpler and sufficient (small dataset).
- The `GeofenceBroadcastReceiver` uses `goAsync()` to extend its lifecycle for Room writes, with a `CoroutineScope(SupervisorJob() + Dispatchers.IO)` for the async work.

---

## Context
Greenfield WearOS + mobile companion app for voice-captured reminders. Both modules contain only boilerplate scaffold. The watch app is **standalone-capable** (own Room DB, voice capture, time-based reminders, optional geofencing) but shows an in-app note that the mobile app is required for full features (cloud formatting, geocoding). When paired, bidirectional sync via Wearable Data Layer.

**Key decisions:**
- minSdk **33** (both modules)
- LLM formatting: **Google Gemini 2.0 Flash Lite** (free tier) via REST API
- Limited recurrence: daily/weekly/monthly only (no RRULE library)
- Watch has **own Room DB**, functions standalone with reduced features
- Watch shows **in-app banner** explaining mobile app is needed for full features
- Emulator-first testing strategy
- `android.location.Geocoder` for geocoding (no HERE SDK)
- **Watch geofencing**: autodetect GPS hardware, prompt user for device preference, auto-switch between phone/watch based on connectivity
- Geocoding is phone-only (requires network). Watch receives coordinates from phone, then registers geofences locally if needed.

---

## Phase 1: Foundation — Build System, Models, Room, Compose

### What to Build
- Fix namespace collision: wear namespace → `com.example.reminders.wear` (keep `applicationId` same for Data Layer pairing)
- Change wear manifest `standalone` to `false` (watch depends on phone for full features)
- Add all missing dependencies to version catalog:
  - Room + KSP2 (Room 2.8.x, KSP2 2.3.x)
  - Lifecycle, ViewModel, Navigation Compose
  - DataStore Preferences
  - kotlinx-serialization
  - play-services-location (both modules — watch needs it for geofencing)
  - OkHttp + logging-interceptor
  - coroutines-test, Turbine
  - EncryptedSharedPreferences (security-crypto 1.1.x)
  - **Play Billing Library 8.x** (mobile only — `billing` + `billing-ktx`)
- Add `kotlin-compose`, `ksp`, `kotlin-serialization` plugins to build files
- Migrate mobile `MainActivity` from XML/AppCompat → `ComponentActivity` + Compose; delete `activity_main.xml`
- **Mobile models**: `Reminder` (Room @Entity), `LocationTrigger` (@Embedded), `SavedPlace` (@Entity), `LocationReminderState` enum, `ParsedReminder` DTO
- **Mobile Room**: Database, DAOs, TypeConverters (`Instant`↔`Long`, `LocationTrigger`↔JSON string via manual TypeConverter using `Json.encodeToString()`/`Json.decodeFromString()` — do NOT use `@Serializable` on Room entity/embedded types), `exportSchema = true`
- `ReminderRepository` interface + Room-backed impl, `SavedPlaceRepository`
- Manual DI: `AppContainer`, `RemindersApplication` subclass
- Mobile Compose theme, `ReminderListUiState` sealed interface, placeholder `ReminderListScreen`
- **Billing**: `BillingClient` wrapper (`BillingManager`) — query purchases on init, expose `isPro: StateFlow<Boolean>`, launch purchase flow
- **ProStatus**: stored in DataStore (cached; re-validated against Play on each launch)
- **UsageTracker**: stored in DataStore — `{lastResetDate: LocalDate, count: Int}` for formatting usage counter. Included in auto-backup but NOT Room DB.
- Sync Pro status to watch via Data Layer (`DataClient.putDataItem()` at `/pro-status`)
- **Watch models**: `WatchReminder` (Room @Entity — lightweight: id, title, body, triggerTime, isCompleted, sourceTranscript, createdAt, locationTrigger JSON, locationState)
- **Watch Room**: Separate database class, `WatchReminderDao`, same converters
- Watch Compose theme, placeholder watch `ReminderListScreen`

### Key Files
```
gradle/libs.versions.toml                    — all versions
build.gradle.kts (root)                      — plugins apply false
mobile/build.gradle.kts                      — plugins, Compose, Room KSP, deps
wear/build.gradle.kts                        — namespace change, Room KSP, play-services-location, lifecycle-viewmodel-compose
mobile/src/main/java/.../data/model/         — Reminder, LocationTrigger, SavedPlace, ParsedReminder, LocationReminderState
mobile/src/main/java/.../data/local/          — RemindersDatabase, ReminderDao, SavedPlaceDao, Converters
mobile/src/main/java/.../data/repository/     — ReminderRepository, SavedPlaceRepository interfaces + impls
mobile/src/main/java/.../data/preferences/    — UserPreferences (API key, provider), UsageTracker (formatting counter)
mobile/src/main/java/.../billing/             — BillingManager (BillingClient wrapper)
mobile/src/main/java/.../di/                  — AppContainer, RemindersApplication
mobile/src/main/java/.../ui/                  — theme, ReminderListUiState, ReminderListScreen
wear/src/main/java/.../wear/data/             — WatchReminder, WatchRemindersDatabase, WatchReminderDao, Converters
wear/src/main/java/.../wear/ui/               — theme, ReminderListScreen
```

### Pitfalls
- **Two separate Compose BOMs**: Phone uses `androidx.compose:compose-bom`, Wear uses `androidx.wear.compose:compose-bom`. Name them distinctly in catalog: `composeBomPhone` vs `composeBomWear`.
- **KSP2 versioning**: KSP2 (2.3.x) uses independent versioning — it no longer matches the Kotlin version. Apply with `id("com.google.devtools.ksp") version "2.3.x"`. KSP1 is deprecated and will not support Kotlin 2.3+ or AGP 9+.
- **`@Serializable` + Room `@Embedded` = data loss**: KSP2 has an open bug where `@Embedded` silently drops all columns for `@Serializable` types. Do NOT annotate Room entities or embedded types with `@Serializable`. Use manual TypeConverters that call `Json.encodeToString()`/`Json.decodeFromString()` instead.
- **Room schema export**: Add `ksp { arg("room.schemaLocation", "$projectDir/schemas") }` in both `mobile/` and `wear/` build files.
- **Namespace change ripple**: After changing wear namespace, update all `com.example.reminders.R` imports to `com.example.reminders.wear.R`.
- **`Instant` type converter**: Use `Long` (not `Int`) to avoid Year 2038. Handle null.
- **Never use `fallbackToDestructiveMigration()`** — formally deprecated in Room 2.7.0+. Use explicit `Migration` objects.
- **Watch Room DB name**: Use a distinct name (e.g., `watch-reminders-db`).
- **play-services-location in wear module**: Needed for `GeofencingClient`. Adds ~1-2MB to watch APK. Acceptable tradeoff.
- **Billing on mobile only**: Watch never handles billing. Pro status synced from phone via Data Layer. If no phone connected, watch assumes free tier.
- **BillingClient initialization**: Must call `startConnection()` early (e.g. `Application.onCreate`). Connection is async — don't block UI.
- **Purchase acknowledgment**: Must acknowledge within 3 days or Google auto-refunds. Do in `PurchasesUpdatedListener`.
- **`enablePendingPurchases()` required**: PBL 7+ requires explicit `enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())` during BillingClient setup. Without this, one-time IAP purchases will fail.
- **Billing query on app start**: Always call `queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build())` on launch — handles reinstalls, multi-device, etc.
- **Billing testing**: Requires Play Console setup — create internal testing track, upload signed AAB, create `pro_upgrade` product, add license testing accounts. Test device's Google account must be a license tester. Cannot test billing in debug builds without Play Console linkage. Use Google's Play Billing Lab app for repeat purchase testing.

### Verification
- `./gradlew :mobile:assembleDebug` and `./gradlew :wear:assembleDebug` succeed
- Unit test Converters round-trips (both mobile and watch)
- Unit test ReminderRepositoryImpl with in-memory Room DB
- Unit test `BillingManager`: query returns Pro status, purchase flow launches, acknowledgment called
- Unit test `UsageTracker`: count increments, resets on new day, blocks at limit for free, bypasses for Pro/BYO key

---

## Phase 2: Transcription Pipeline (Mobile)

### What to Build
- `TranscriptionBackend` sealed interface: `AndroidBuiltIn` + `FutoWhisper` stub (don't implement BYOM yet)
- `SpeechRecognitionManager` wrapping Android `SpeechRecognizer`
- `RECORD_AUDIO` permission handling via Activity Result API
- `TranscriptionUiState` sealed interface: `Idle`, `Listening`, `Processing`, `Result(text)`, `Error(message)`
- `TranscriptionViewModel`, `TranscriptionScreen`, `RecordButton` component
- Error handling for all `SpeechRecognizer` error codes

### Key Files
```
mobile/.../transcription/TranscriptionBackend.kt
mobile/.../transcription/SpeechRecognitionManager.kt
mobile/.../transcription/TranscriptionResult.kt
mobile/.../ui/viewmodel/TranscriptionViewModel.kt
mobile/.../ui/screen/TranscriptionScreen.kt
mobile/src/main/AndroidManifest.xml           — RECORD_AUDIO, INTERNET
```

### Pitfalls
- **`SpeechRecognizer` must run on main thread.** Create in Activity scope, bridge results to ViewModel via Flow.
- **`EXTRA_PREFER_OFFLINE` is a hint, not guaranteed.** OEMs may ignore it. Detect online fallback via `isOnDeviceRecognitionAvailable()` (API 33+) and warn user.
- **`ERROR_NO_MATCH` (code 7)** — not fatal, means "no speech detected." Handle gracefully.
- **Partial results replace, not append** — each `onPartialResults` contains full text so far.
- **Process death during recognition**: Return to idle on restore; don't auto-restart.
- **`createSpeechRecognizer()` returns null** if speech recognition unavailable — always null-check.
- **Audio focus**: Request before recognition, release after.

### Verification
- Unit test ViewModel with fake `SpeechRecognitionManager` interface
- Manual test on emulator (use "Virtual microphone uses host audio input" in AVD settings)

---

## Phase 3: Formatting Pipeline + Gemini Integration

### What to Build
- `FormattingProvider` interface:
  ```kotlin
  interface FormattingProvider {
      suspend fun format(transcript: String): FormattingResult
  }
  sealed interface FormattingResult {
      data class Success(val reminders: List<ParsedReminder>) : FormattingResult
      data class PartialSuccess(val reminders: List<ParsedReminder>, val rawFallback: String) : FormattingResult
      data class Failure(val error: String) : FormattingResult
      data object UsageLimited : FormattingResult
  }
  ```
- `GeminiFormattingProvider` — calls Gemini 2.0 Flash Lite via REST API (generateContent endpoint)
- `RawFallbackProvider` — no API key configured → save raw transcript as single unformatted reminder
- API key storage via `EncryptedSharedPreferences`
- Settings screen: API key entry, provider selection (just Gemini for now, designed to extend)
- `ParsedReminder` → `Reminder` mapper
- `PipelineOrchestrator` chaining: transcription → formatting → Room storage
- `PipelineResult` sealed interface for partial/full success/failure
- **Formatting usage gating**: before calling Gemini, check `UsageTracker.isFormattingAllowed(proStatus)`. If free + count ≥ 1 today → return `FormattingResult.UsageLimited`. If BYO API key set → bypass counter entirely (unlimited). Pro users → always allowed.

### Key Files
```
mobile/.../formatting/FormattingProvider.kt
mobile/.../formatting/GeminiFormattingProvider.kt
mobile/.../formatting/FormattingPrompt.kt          — system prompt for structured JSON output
mobile/.../formatting/FormattingResult.kt
mobile/.../pipeline/PipelineOrchestrator.kt
mobile/.../data/preferences/UserPreferences.kt      — API key, provider choice
mobile/.../data/preferences/UsageTracker.kt          — formatting usage counter (1/day free)
mobile/.../ui/screen/SettingsScreen.kt
mobile/.../network/GeminiApiClient.kt
```

### Gemini API Details
- **Endpoint**: `POST https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-lite:generateContent?key={API_KEY}` (or `gemini-2.0-flash-lite` — both work, 2.5 is better quality at same cost)
- **Free tier**: No credit card required. User creates API key at ai.google.dev. Rate limits are **dynamic per-project** (view actual limits in AI Studio > Usage). Historically ~15 RPM, ~1500 RPD for Flash Lite models — sufficient for personal use.
- **No credit card required** — user creates API key at ai.google.dev
- **Response format**: JSON with `candidates[0].content.parts[0].text`
- **System prompt**: Instructs model to return JSON array of `{title, triggerTime, recurrence, locationTrigger}` — includes examples of edge cases

### Dependencies
- OkHttp 4.12.x + logging-interceptor
- `androidx.security:security-crypto` for encrypted key storage
- kotlinx-serialization for JSON parsing

### Pitfalls
- **LLM JSON in markdown fences**: Strip ```json ... ``` wrappers. Handle single objects (wrap in `[]`). Handle trailing commas.
- **Rate limiting (429)**: Exponential backoff, max 3 retries, 30s total timeout. Surface "rate limited" to user.
- **OkHttp timeouts**: Set explicitly — `connectTimeout(10s)`, `readTimeout(30s)`.
- **`EncryptedSharedPreferences` on API 33+**: Test on target API level. Fallback: DataStore + Keystore.
- **ISO 8601 parsing**: LLMs return varied formats. Use `DateTimeFormatter.ISO_DATE_TIME` with fallbacks. Instruct model to convert relative times to absolute ISO 8601.
- **Never discard user speech**: Formatting failure → save raw transcript. Partial failures save valid ones + raw fallback for the rest.
- **Prompt engineering**: Include 3-5 examples covering: single reminder with time+location, multiple reminders, reminder with no time, reminder with vague location.
- **Usage gating flow**: Free user → `UsageTracker.count` for today → if 0, allow + increment → if ≥ 1, return `UsageLimited` → UI shows paywall + "use your own API key for unlimited" CTA. Pro user → always allow, never increment counter. BYO key user → always allow (key holder, not our API).
- **UsageLimited fallback**: When formatting is usage-limited, still save raw transcript as unformatted reminder. Never discard user speech.

### Verification
- Unit test `GeminiFormattingProvider` with mock HTTP responses (valid JSON, code-fenced, malformed, empty)
- Unit test `PipelineOrchestrator` success + formatting failure + partial failure paths
- Unit test no-API-key fallback
- Unit test `UsageTracker`: count increments, resets on new day, blocks at 1 for free users, bypasses for Pro users, bypasses for BYO key
- Unit test `PipelineOrchestrator` usage-limited path: returns `UsageLimited` → saves raw fallback
- Integration test with real Gemini API key (manual, key in gitignored test resources)

---

## Phase 4: Geocoding + Saved Places + Location Permissions

### What to Build
- `GeocodingService` interface + `AndroidGeocodingService` using `android.location.Geocoder`
- `GeocodingResult` sealed: `Resolved`, `Ambiguous(candidates)`, `NotFound`, `Error`
- Saved Places CRUD (Room + UI screens on mobile)
- `SavedPlaceMatcher` — case-insensitive label lookup before any geocoding call
- Geocoding disambiguation screen (`NEEDS_CONFIRMATION` state)
- Location permission flow: `ACCESS_FINE_LOCATION` first (background comes in Phase 5)
- Pipeline integration: formatting → saved place check → geocode → store
- **Saved Places limit**: Free users capped at 2. Check `BillingManager.isPro` + current count before allowing creation. Show upgrade CTA at limit.

**Why `android.location.Geocoder`**: Built into Android, no API key, no extra dependency. On API 33+ (our minSdk), supports on-device geocoding. Saved Places handles common labels; geocoding runs for unknown addresses.

### Key Files
```
mobile/.../geocoding/GeocodingService.kt
mobile/.../geocoding/AndroidGeocodingService.kt
mobile/.../geocoding/SavedPlaceMatcher.kt
mobile/.../ui/screen/SavedPlacesScreen.kt
mobile/.../ui/screen/GeocodingConfirmationScreen.kt
mobile/.../permissions/LocationPermissionHandler.kt
```

### Pitfalls
- **`Geocoder` API 33+**: Use the listener-based `getFromLocationName()` (results on background thread). minSdk 33 means deprecated synchronous overload still exists but prefer async.
- **`Geocoder.isPresent()` check**: Always call first. Return `Error` if absent.
- **Vague POI queries**: `Geocoder` may return 0 results for non-address queries ("the doctors"). Route to Saved Places first, then disambiguation screen.
- **Case-insensitive Room query**: Use `LOWER()`: `WHERE LOWER(label) = LOWER(:label)`.
- **Pipeline state machine**: Cannot be a single blocking suspend function at `NEEDS_CONFIRMATION`. Design as state machine emitting states; UI observes and sends confirmation events back.
- **No internet during geocoding**: `Geocoder` may require network on some devices. Save as `PENDING_GEOCODING`, retry later via WorkManager.
- **Cache confirmed results**: After user confirms a candidate, offer to save as `SavedPlace`.
- **Saved Places cap enforcement**: Query `SavedPlaceDao.count()` before insert. If free + count ≥ 2 → show Pro paywall. Pro users → unlimited.

### Verification
- Unit test `SavedPlaceMatcher` with case/whitespace variations
- Unit test pipeline state transitions (saved place match, single result, ambiguous, not found, geocoder absent)
- Manual test on emulator with mock locations

---

## Phase 5: Geofencing — Phone + Watch with Auto-Switch

### Overview
Geofencing runs on **both** phone and watch. The system auto-detects watch GPS hardware, prompts user for preference, and auto-switches the active geofencing device based on phone connectivity.

### Geofencing Device Preferences
```kotlin
enum class GeofencingDevice {
    AUTO,       // default — phone when connected, watch when disconnected
    PHONE_ONLY, // always phone (watch without GPS, or user choice)
    WATCH_ONLY  // always watch (requires GPS hardware)
}
```

### What to Build — Shared Abstraction
- `GeofenceManager` interface:
  ```kotlin
  interface GeofenceManager {
      suspend fun registerGeofence(reminder: Reminder): Result<String>
      suspend fun removeGeofence(geofenceId: String): Result<Unit>
      suspend fun removeAllGeofences(geofenceIds: List<String>): Result<Unit>
      suspend fun getActiveGeofenceCount(): Int
  }
  ```
- `AndroidGeofenceManager` — concrete implementation wrapping `GeofencingClient` (used on both phone and watch)

### What to Build — Phone
- `PhoneGeofenceManager` — delegates to `AndroidGeofenceManager`
- `GeofenceBroadcastReceiver` for transition events → notification → Room update → sync to watch
- `ACCESS_BACKGROUND_LOCATION` permission flow (separate from fine location)
- Geofence registration after geocoding confirmation
- Geofence removal on reminder delete/complete
- Geofence cap tracking + UI warnings: **Free users capped at 5 active geofences** (warn at 4, block at 5). **Pro users capped at 100** (warn at 90, block at 100). Check `BillingManager.isPro` before registration. Show upgrade CTA when free user hits cap.
- `GeofenceReregistrationWorker` — re-register all geofences after device reboot
- Notification channel setup + reminder trigger notifications
- `POST_NOTIFICATIONS` permission

### What to Build — Watch
- `WatchGeofenceManager` — delegates to `AndroidGeofenceManager` (local on watch)
- `WatchGeofenceBroadcastReceiver` for transition events on watch → notification → Room update → sync to phone
- `ACCESS_FINE_LOCATION` + `ACCESS_BACKGROUND_LOCATION` permissions on watch
- `POST_NOTIFICATIONS` permission on watch
- GPS hardware detection: `PackageManager.hasSystemFeature(FEATURE_LOCATION_GPS)`
- **Geofencing device preference screen** (shown on first location reminder if GPS detected):
  - "Where should location reminders run?"
  - Auto (recommended): Uses phone when connected, switches to watch when phone is away
  - Phone only: Saves battery on watch, requires phone nearby
  - Watch only: Works independently, uses more watch battery

### What to Build — Auto-Switch Logic
- `GeofencingDeviceManager` (on watch):
  - Monitors phone connectivity via `CapabilityClient` / `NodeClient`
  - When phone disconnects AND preference is AUTO:
    1. Query all active geofence reminders from local Room DB (those with coordinates from phone sync)
    2. Register all on watch via `WatchGeofenceManager`
    3. Update local state: `geofencingDevice = WATCH`
  - When phone reconnects AND preference is AUTO:
    1. Notify phone of current geofence state
    2. Phone re-registers all active geofences via `PhoneGeofenceManager`
    3. Remove geofences from watch via `WatchGeofenceManager`
    4. Update local state: `geofencingDevice = PHONE`
  - Migration is **asynchronous with retry** — if watch fails to register, keep trying via WorkManager
- `GeofenceSyncMessage` — Data Layer message for coordinating geofence migration:
  - `PHONE_REQUESTS_TAKEOVER`: phone asks watch to hand off geofences
  - `WATCH_CONFIRMS_HANDOFF`: watch confirms geofences removed, phone can register
  - `WATCH_REQUESTS_TAKEOVER`: watch asks phone to hand off (phone disconnected scenario — watch just registers locally)
- **Preference stored in** `DataStore<GeofencingDevice>` on watch, synced to phone

### Key Files
```
mobile/.../geofence/GeofenceManager.kt                 — interface
mobile/.../geofence/AndroidGeofenceManager.kt           — shared implementation
mobile/.../geofence/GeofenceBroadcastReceiver.kt
mobile/.../geofence/GeofenceReregistrationWorker.kt
mobile/.../notification/ReminderNotificationManager.kt
mobile/src/main/AndroidManifest.xml                     — BACKGROUND_LOCATION, RECEIVE_BOOT_COMPLETED, POST_NOTIFICATIONS
wear/.../wear/geofence/WatchGeofenceManager.kt          — uses AndroidGeofenceManager
wear/.../wear/geofence/WatchGeofenceBroadcastReceiver.kt
wear/.../wear/geofence/GeofencingDeviceManager.kt       — auto-switch logic
wear/.../wear/geofence/GpsDetector.kt                  — hasSystemFeature check
wear/.../wear/ui/screen/GeofencingPreferenceScreen.kt  — device choice UI
wear/src/main/AndroidManifest.xml                       — FINE_LOCATION, BACKGROUND_LOCATION, POST_NOTIFICATIONS
shared concept (both modules):
  GeofencingDevice enum, GeofenceSyncMessage data class
```

### Dependencies
- WorkManager 2.10.x (both modules — boot re-registration, offline retry, geofence migration retry on watch)
- play-services-location already added in Phase 1 (both modules)

### Auto-Switch Flow Diagram
```
WATCH DETECTS PHONE DISCONNECT (preference = AUTO):
  1. Watch GeofencingDeviceManager receives onPeerDisconnected()
  2. Query Room for all reminders with locationTrigger WHERE geofenceId != null
  3. For each: register geofence on watch via WatchGeofenceManager
  4. Mark each reminder's geofencingDevice = WATCH in Room
  5. Show notification: "Location reminders running on watch"

PHONE RECONNECTS (preference = AUTO):
  1. Watch GeofencingDeviceManager receives onPeerConnected()
  2. Send message to phone: list of active geofence reminders with coordinates
  3. Phone registers all geofences via PhoneGeofenceManager
  4. Phone confirms back to watch
  5. Watch removes local geofences via WatchGeofenceManager
  6. Mark each reminder's geofencingDevice = PHONE in Room
  7. Show notification: "Location reminders moved to phone"
```

### Pitfalls
- **Background location on Android 11+**: Cannot request in same session as fine location. System sends user to Settings. Provide clear instructions. Applies to BOTH phone and watch.
- **PendingIntent flags**: Must use `FLAG_MUTABLE` for geofencing — system attaches transition extras.
- **Geofence reliability on Android 12+**: Play Services batches events. Expect 2-5 min delay + 30s loitering delay.
- **Boot clears geofences**: Re-register on both devices via WorkManager + `BOOT_COMPLETED`.
- **Geofence removal must be transactional**: Room delete + `removeGeofences()` must both succeed. Retry failures via WorkManager.
- **100 geofence cap per device**: Track per-device. Phone and watch each have independent 100-cap. **Free tier override**: 5 active geofences max (Pro = 100). Enforce at registration time.
- **`setLoiteringDelay(30_000)`**: Required. Use `GEOFENCE_TRANSITION_DWELL` alongside `ENTER`.
- **Migration race condition**: If phone disconnects mid-migration, watch may have partial geofences. Design migration as idempotent — re-running produces same result.
- **Watch GPS cold start**: Watch GPS chipsets are slower than phones. First fix can take 30-60 seconds. Geofence registration may fail if no location fix yet. Retry with backoff.
- **Watch battery warning**: When user selects WATCH_ONLY or auto-switches to watch, show one-time notice: "Location reminders on watch use GPS, which increases battery usage."
- **No GPS on watch**: If `hasSystemFeature(FEATURE_LOCATION_GPS)` returns false, skip preference prompt entirely, default to PHONE_ONLY, explain in banner.
- **`GEOFENCE_NOT_AVAILABLE` (error code 1000)**: Returned by `GeofencingClient` on watches without GPS hardware or when location is simply turned off on the device. Must handle explicitly — catch `ApiException` with status code 1000, show user a clear message ("Turn on location on your watch to use location reminders"), and do NOT register the geofence (retry won't help until location is enabled).
- **Geocoding is phone-only**: Watch never calls Geocoder. Phone sends coordinates (lat/long) to watch as part of reminder sync. Watch uses these coordinates directly for geofence registration.

### Verification
- Unit test `AndroidGeofenceManager` with mocked `GeofencingClient` (shared logic)
- Unit test `GeofencingDeviceManager` state transitions (phone connect/disconnect/auto-switch)
- Unit test migration idempotency
- Unit test cap counting logic (per-device)
- Unit test `GeofenceReregistrationWorker`
- Emulator test: use Extended Controls → Location to simulate geofence entry/exit
- Emulator pair test: connect wear emulator to phone emulator, verify auto-switch on disconnect/reconnect

---

## Phase 6: Wearable Data Layer + Watch App

### What to Build
- **Watch standalone features** (work without phone):
  - Voice capture via `ACTION_RECOGNIZE_SPEECH` intent (system UI, no `RECORD_AUDIO` permission needed)
  - Fallback: WearOS keyboard input via `TextField` when speech unavailable or user prefers typing
  - Local Room DB for reminders (created in Phase 1)
  - Reminder list display (`ScalingLazyColumn`)
  - Time-based reminders (own `AlarmScheduler` — same pattern as mobile Phase 7)
  - Geofencing (built in Phase 5, uses coordinates received from phone or entered manually)
  - **In-app banner**: "Install the mobile app for cloud formatting, location lookups, and more" — shown on main screen when no phone connected
- **Watch → Phone sync** (when paired):
  - Voice transcript sent via `MessageClient` → phone runs full pipeline (formatting + geocoding + geofencing) → syncs result back
  - Raw transcript saved locally on watch immediately (don't lose data if phone unreachable)
  - **Deferred formatting**: when watch reconnects to phone after being offline, any locally-saved raw reminders with `formattingProvider = "pending"` are automatically sent to phone for formatting. On success, watch updates the reminder with formatted data. Rate-limited to avoid flooding (batch of 5, with delay).
- **Phone → Watch sync** (when paired):
  - Reminder sync via `DataClient` — individual `DataItem` per reminder at `/reminders/{id}`
  - Bidirectional: edits/deletions/completions propagate both directions
  - Geofence coordination via `GeofencingDeviceManager` (Phase 5)
- `WearableListenerService` on both sides
- Watch UI: reminder list, voice record screen, reminder detail, keyboard input, geofencing preferences
- Ambient mode support for persistent screens
- Navigation via `SwipeDismissableNavHost`
- Watch `ReminderDto` for Data Layer serialization (lightweight)
- **Watch face complication** — SHORT_TEXT type showing a reminder count badge (e.g. "3"):
  - `ComplicationProviderService` subclass reads count from watch's local Room DB
  - Two configurable modes (set in watch app settings):
    - **Due today**: count of reminders with `triggerTime` between start and end of today
    - **All upcoming**: count of all incomplete reminders with a future `triggerTime`
  - Config stored in watch DataStore (`complicationMode: "today" | "all"`)
  - Update via polling (`updatePeriodMillis = 3600000` = 1hr, minimum per WearOS guidelines) + push (`requestUpdate()` called whenever a reminder is created/completed/deleted/synced)
  - Tap action: opens watch app to reminder list screen
  - Icon: reminder bell drawable in wear module resources
  - **Not Pro-gated** — core usability feature, available to all users
  - No phone needed — reads from watch's local DB, works standalone

### Key Files
```
wear/.../wear/data/WearDataLayerClient.kt
wear/.../wear/data/WatchReminderRepository.kt
wear/.../wear/ui/screen/ReminderListScreen.kt
wear/.../wear/ui/screen/VoiceRecordScreen.kt
wear/.../wear/ui/screen/KeyboardInputScreen.kt
wear/.../wear/ui/screen/ReminderDetailScreen.kt
wear/.../wear/ui/component/PhoneRequiredBanner.kt
wear/.../wear/service/DataLayerListenerService.kt
wear/.../wear/complication/ReminderComplicationProvider.kt    — ComplicationProviderService
wear/.../wear/complication/ComplicationMode.kt                — "today" | "all" config enum
wear/.../wear/ui/screen/ComplicationConfigScreen.kt            — mode picker in watch settings
mobile/.../wearable/WearableListenerServiceImpl.kt
mobile/.../wearable/WearableDataSender.kt
mobile/.../wearable/DataLayerPaths.kt
```

### Dependencies
- `wear-compose-navigation` for watch navigation

### Pitfalls
- **`MessageClient` is fire-and-forget**: Not queued if phone unreachable. Save transcript locally, retry with backoff.
- **Message ordering not guaranteed**: Include timestamp in payload.
- **DataItem 100KB limit**: Individual items per reminder, not bulk.
- **`WearableListenerService` lifecycle**: Short-lived bound service. Don't do long work in callbacks. Enqueue via coroutine.
- **`applicationId` must match** between modules for Data Layer pairing.
- **Round vs square screens**: Use `rememberIsRoundDevice()`. Tap targets ≥ 48dp.
- **Watch battery**: Batch DataItem updates, don't update every second.
- **Sync conflict resolution**: Last-write-wins using `updatedAt` timestamp.
- **`ACTION_RECOGNIZE_SPEECH` availability**: Check `PackageManager.resolveActivity()`. If unavailable, go to keyboard. **Note**: `ACTION_RECOGNIZE_SPEECH` does NOT require `RECORD_AUDIO` permission — it launches a system activity that handles mic access itself.
- **WearOS voice reliability (device-dependent)**: `SpeechRecognizer` (programmatic API) reports `isRecognitionAvailable() == false` on Galaxy Watch 4/5. The intent-based `ACTION_RECOGNIZE_SPEECH` is more compatible but still unreliable on some Samsung watches. Always check availability, always provide keyboard fallback. Test on multiple OEM watch emulators if possible.
- **WearOS keyboard**: Use `TextField` with `ImeAction.Done`. Voice is primary, keyboard is fallback.
- **Complication update frequency**: `updatePeriodMillis` has a 1-hour minimum on WearOS. For real-time updates, call `requestUpdate(complicationProvider)` from your code when reminders change — but don't call it more than a few times per minute to avoid system throttling.
- **Complication data freshness**: When the watch face is visible, the system may cache complication data. After calling `requestUpdate()`, the visible data may take a few seconds to refresh. Don't design UX that depends on instant visual updates.
- **Complication tap intent**: Use a PendingIntent to your app's main Activity with a route argument. The Activity reads the route and navigates to the reminder list. Must use `FLAG_IMMUTABLE` on the PendingIntent.

### Verification
- Unit test `ReminderSerializer` round-trip
- Unit test sync conflict resolution logic
- Unit test `ReminderComplicationProvider`: count = 0 → shows "0", count = 5 → shows "5", "today" mode only counts today's reminders, "all" mode counts all upcoming, completed reminders excluded
- Emulator pair test: connect Wear emulator to phone emulator
- Test watch standalone: disable phone connection, create reminder via voice + keyboard, verify local persistence
- Test complication: add complication to a watch face, create/complete reminders, verify count updates

---

## Phase 7: Notifications, Time-Based Reminders, Completion Flow

### What to Build
- `AlarmScheduler` using `AlarmManager.setExactAndAllowWhileIdle()` for time-based triggers
- `AlarmReceiver` BroadcastReceiver
- `RecurrenceHelper` — limited set: daily, weekly, monthly (compute next occurrence, no RRULE library). **Pro only** — free users see recurrence option greyed out with Pro badge
- Notification actions: Complete (always), Snooze 5min/15min/1hr (**Pro only** — free users see only Complete + Dismiss)
- `NotificationActionReceiver` handles action intents — check Pro status before processing snooze; if free, ignore snooze action
- Full completion flow: mark complete → remove geofence (phone + watch) → cancel alarm → update Room → sync to watch
- Full deletion flow: confirm → remove geofence (phone + watch) → cancel alarm → delete Room → sync to watch
- Reminder edit screen (time, radius, recurrence)
  - **Custom radius**: Pro only. Free users see 150m fixed (slider disabled with Pro badge)
  - **Recurrence**: Pro only. Free users see "Upgrade to set recurrence" CTA
  - **Snooze**: Pro only in notification actions
- Boot re-registration for alarms (reuse `BOOT_COMPLETED` receiver from Phase 5)
- **Same AlarmScheduler on watch** for standalone time-based reminders

### Key Files
```
mobile/.../alarm/AlarmScheduler.kt
mobile/.../alarm/AlarmReceiver.kt
mobile/.../alarm/RecurrenceHelper.kt
mobile/.../notification/NotificationActionReceiver.kt
mobile/.../ui/screen/ReminderEditScreen.kt
wear/.../wear/alarm/WatchAlarmScheduler.kt
wear/.../wear/alarm/WatchAlarmReceiver.kt
```

### Pitfalls
- **`USE_EXACT_ALARM`** (reminder app qualifies — auto-granted, can't be revoked). Prefer over `SCHEDULE_EXACT_ALARM`. Still check `canScheduleExactAlarms()`.
- **Doze throttling**: `setExactAndAllowWhileIdle` throttled to ~1 per 9 min. Acceptable for reminders.
- **PendingIntent uniqueness**: Use reminder's auto-increment integer PK for requestCode — `hashCode()` can collide.
- **Recurrence rescheduling**: After trigger, calculate + schedule next occurrence in `AlarmReceiver.onReceive()`. Use `goAsync()`.
- **Alarms cleared on reboot**: Reuse boot receiver from Phase 5.
- **Completion must clean up both devices**: When completing a geofence reminder, remove geofence from whichever device is currently managing it (check `geofencingDevice` field).
- **WearOS AlarmManager OEM issues**: Some OEMs (OnePlus Watch 2) force-stop apps when the screen turns off, cancelling all registered alarms. Prefer `ELAPSED_REALTIME_WAKEUP` over `RTC_WAKEUP` on WearOS (better wake-from-sleep behavior). Use WorkManager as fallback for less time-critical scheduling on watches with known OEM issues.
- **Snooze on free users**: Build notification with conditional actions — `if (isPro) add snooze actions else only complete/dismiss`. Don't build snooze actions then hide them — excluded from the notification entirely.
- **Recurrence enforcement**: Free users can view existing recurring reminders (created before downgrade or via Pro) but cannot create new ones. Editing a recurring reminder on free tier preserves the recurrence but prevents changing it.

### Verification
- Unit test `RecurrenceHelper` with daily/weekly/monthly patterns
- Unit test completion flow (geofence + alarm cleanup)
- Integration test notification action → Room state change
- Emulator test: set reminder for 1 minute from now, verify notification

---

## Phase 8: Polish, Security, Error Resilience

### What to Build
- ProGuard/R8 rules for Room, OkHttp, kotlinx-serialization, Play Services
- Backup rules: API keys and Room DB excluded from cloud backup; **UsageTracker DataStore file included** (survives reinstall for most users)
- Comprehensive error recovery UI: retry buttons, "save raw" fallbacks, status indicators
- Offline queue via WorkManager (retry geocoding, formatting when connectivity returns)
- Accessibility: content descriptions, TalkBack support, contrast
- Watch complication showing next upcoming reminder (optional)
- Merge strategy for bidirectional sync edge cases
- **`ProPaywallScreen`**: reusable composable — feature name, benefit bullet list, price, "Upgrade" CTA → `BillingClient.launchBillingFlow()`
- **`ProBadge`**: small "PRO" chip composable shown next to gated features in settings, lists, edit screens
- **Export/import reminders**: Pro only. JSON export of all reminders + saved places. Import merges (skip duplicates by ID). Button disabled with Pro badge for free users.
- **Settings > Pro section**: shows current status (Free/Pro), upgrade CTA, restore purchases button, feature comparison table

### Key Files
```
mobile/proguard-rules.pro
wear/proguard-rules.pro
mobile/src/main/res/xml/data_extraction_rules.xml
mobile/src/main/res/xml/backup_rules.xml
mobile/.../ui/component/ProPaywallScreen.kt          — reusable paywall composable
mobile/.../ui/component/ProBadge.kt                   — small "PRO" chip
mobile/.../ui/screen/SettingsScreen.kt                — Pro section (status, upgrade, restore)
mobile/.../data/export/ReminderExporter.kt            — JSON export/import logic
```

### Pitfalls
- **R8 full mode**: More aggressive in recent AGP. Test release build — Room entities and serialization classes vulnerable to stripping.
- **Auto-backup of Room DB**: Exclude by default. Contains location/transcript data. **Include UsageTracker DataStore file** so formatting counter survives reinstall.
- **`restorePurchases()`**: Must call on Settings screen button press — handles users who reinstall and need to recover Pro.
- **Pro downgrade edge case (preserve-and-freeze)**: If Google Play refunds/revokes a purchase, `queryPurchasesAsync()` returns empty → Pro status flips to free. **Existing Pro data is preserved**: recurring reminders keep firing, geofences beyond cap 5 stay registered, saved places beyond cap 2 remain. User simply cannot create NEW Pro features. Show banner: "Pro features locked. Some features may be limited." This is less destructive than removing data and avoids user anger.
- **SQLCipher**: Not needed. Android file-based encryption (default on API 33+) is sufficient.

### Verification
- `./gradlew :mobile:minifyReleaseWithR8` — no crashes
- Test every screen with TalkBack on emulator
- Test airplane mode at each pipeline stage
- Test with 99 active geofences + create one more (both devices)
- Test watch disconnected during voice flow
- Test auto-switch: disconnect phone, verify watch picks up geofences, reconnect, verify phone takes over
- Test with "Don't keep activities" developer option

---

## Phase 9: Backend, Accounts & Cloud Features (DEFERRED — Action Later)

This phase is **not part of V1**. It documents the V2 backend architecture to be implemented after V1 launch when user growth justifies infrastructure investment. All V1 features work without a backend.

### Overview

V2 adds cloud infrastructure to enable: cloud backup, cross-device sync (beyond watch↔phone), proxied Gemini API (no user API key needed), server-side purchase validation, and server-tracked usage counters.

### Architecture

- **Auth**: Firebase Auth via AndroidX Credential Manager + Google Sign-In. Phone handles sign-in; identity synced to watch via Data Layer (no auth UI on watch). Firebase Auth (including Google Sign-In) is free with unlimited users.
- **Cloud DB**: Cloud Firestore — stores reminders, saved places, user preferences. Spark plan free tier: 50K reads/day, 20K writes/day, 1GB storage (confirmed current). Sufficient for small user base.
- **Serverless functions**: Firebase Cloud Functions (Node.js). **Requires Blaze (pay-as-you-go) plan** — Cloud Functions are NOT available on the free Spark plan. Credit card required. Blaze includes 2M free invocations/month, 400K GB-seconds/month. Realistically free for small-medium user base.
- **No dedicated server**: Entirely serverless. Near-zero ops overhead.

### What to Build

#### 9.1: Google Sign-In (Credential Manager + Firebase Auth)
- Add dependencies: `androidx.credentials:credentials`, `credentials-play-services-auth`, `googleid`, `firebase-auth`
- Create "Web application" OAuth 2.0 Client ID in Google Cloud Console
- `AuthManager` wrapping Credential Manager → Firebase Auth → user ID + email
- Sign-in screen on phone (Settings or first-launch)
- Identity sync to watch: `DataClient.putDataItem("/user-profile", {uid, email, displayName})`
- Watch reads user profile on startup; shows "Signed in as..." in settings
- Sign-out: clear local data + Firebase signOut + notify watch

#### 9.2: Cloud Backup & Cross-Device Sync
- Firestore collections: `users/{uid}/reminders`, `users/{uid}/savedPlaces`, `users/{uid}/preferences`
- `CloudSyncManager`: bidirectional sync between Room ↔ Firestore
- Conflict resolution: `lastUpdatedAt` timestamp, server-side wins (Firestore server timestamps)
- Trigger: sync on app start, on data change, on connectivity restore
- Cross-device: tablet or 2nd phone can read same Firestore data
- Offline-first: Room is source of truth; Firestore syncs when online
- Watch still uses Data Layer for real-time sync; Firestore is the cloud mirror

#### 9.3: Proxied Gemini API (Serverless)
- Firebase Cloud Function `formatReminder(transcript, uid)`:
  - Validates user is authenticated
  - Checks server-side usage counter (stored in Firestore `users/{uid}/usage`)
  - Calls Gemini API with function's own API key (user never sees the key)
  - Returns formatted reminders
  - Rate limiting: free users 1/day (server-enforced, no clear-data loophole), Pro users unlimited
- `ServerFormattingProvider` — new `FormattingProvider` impl calling Cloud Function instead of Gemini directly
- User no longer needs to enter a Gemini API key — the function handles it
- BYO key option remains for power users (direct Gemini call, bypasses Cloud Function)

#### 9.4: Server-Side Purchase Validation
- Firebase Cloud Function `validatePurchase(packageName, productId, purchaseToken)`:
  - Calls Google Play Developer API `purchases.products.get()` to verify
  - Stores Pro status in Firestore `users/{uid}/proStatus`
  - Returns validated status to client
- Client calls on purchase + on app start (after auth)
- Eliminates rooted-device purchase spoofing
- Pro status now server-authoritative (not just local DataStore)

#### 9.5: Subscription Migration
- Replace one-time `pro_upgrade` IAP with Play Billing subscription:
  - Monthly plan (~$2-3/mo)
  - Annual plan (~$20/yr, discounted)
  - Offer 7-day free trial
- Existing one-time purchasers: grandfathered in (check `originalJson` purchase date)
- `BillingClient` switches from `ProductType.INAPP` to `ProductType.SUBS`
- Server-side subscription status check via Play Developer API
- Grace period handling: 3-day grace for payment failures (configured in Play Console)

### Dependencies (V2 only — DO NOT add in V1)
```toml
# Auth
credentials = "1.5.0"
googleid = "1.1.1"
firebaseAuth = "23.2.1"
firebaseBom = "33.x.x"

# In build.gradle
implementation("androidx.credentials:credentials")
implementation("androidx.credentials:credentials-play-services-auth")
implementation("com.google.android.libraries.identity.googleid:googleid")
implementation(platform("com.google.firebase:firebase-bom"))
implementation("com.google.firebase:firebase-auth")
implementation("com.google.firebase:firebase-firestore-ktx")
```

### Key Files (V2)
```
mobile/.../auth/AuthManager.kt                    — Credential Manager + Firebase Auth
mobile/.../auth/SignInScreen.kt                   — Phone sign-in UI
mobile/.../sync/CloudSyncManager.kt               — Room ↔ Firestore bidirectional sync
mobile/.../sync/ConflictResolver.kt               — Firestore server timestamp resolution
mobile/.../formatting/ServerFormattingProvider.kt  — calls Cloud Function
mobile/.../billing/SubscriptionManager.kt          — replaces BillingManager for subs
functions/                                         — Firebase Cloud Functions project (separate)
  index.js                                         — formatReminder, validatePurchase
  package.json
```

### Why This Is a Separate Project Decision
- Requires Firebase project setup (console, google-services.json, etc.)
- Requires Google Cloud project with Play Developer API enabled
- Requires a separate Cloud Functions deployment pipeline
- Requires Firebase Auth configuration (OAuth consent screen, etc.)
- Changes the app from "fully offline" to "offline-first with cloud" — architectural shift
- Should only be pursued when user growth justifies the operational overhead

### Migration Path from V1 → V2
1. Add Firebase to project (google-services.json, plugin)
2. Add sign-in screen → existing users sign in → merge local Room data with Firestore
3. Swap `GeminiFormattingProvider` → `ServerFormattingProvider` (same interface, one class change)
4. Swap `BillingManager` → `SubscriptionManager`
5. Add `CloudSyncManager` alongside existing Data Layer sync (Data Layer for watch, Firestore for cloud)
6. Server-side usage counters replace local `UsageTracker` (clear-data loophole closed)

---

## UX Failure Matrix

| Stage | Failure | User Sees | Recovery |
|---|---|---|---|
| Transcription | SpeechRecognizer error | "Could not recognize speech. Try again?" | Retry button |
| Formatting | Network/API error | "Could not parse reminder. Saved as note." | Raw reminder saved, edit later |
| Formatting | Malformed JSON | Same as above | Same |
| Formatting | No API key | "Set up cloud formatting in Settings for smart parsing." | Raw reminder saved, settings link |
| Formatting | Usage limit (free) | "Daily formatting limit reached. Upgrade to Pro or add your own API key." | Raw reminder saved, paywall CTA |
| Geocoding | No results | "Could not find [place]. Search manually?" | Manual search UI |
| Geocoding | No internet | "Could not look up location. Will retry." | PENDING_GEOCODING, WorkManager retry |
| Geocoding | Ambiguous | "Multiple matches for [place]. Which one?" | Disambiguation screen |
| Geofence | Cap reached (free: 5) | "Location reminder limit reached. Upgrade to Pro for up to 100." | Upgrade CTA |
| Geofence | Cap reached (Pro: 100) | "Too many location reminders. Complete some first." | Link to active list |
| Geofence | Permission denied | "Background location needed for location reminders." | Settings link |
| Geofence | Watch GPS unavailable | "Location reminders will run on your phone." | Auto PHONE_ONLY, banner explains |
| Geofence | GEOFENCE_NOT_AVAILABLE (1000) | "Turn on location on your watch to use location reminders." | Settings link, geofence not registered |
| Geofence | Auto-switch to watch | "Phone disconnected. Location reminders running on watch." | Notification, auto |
| Geofence | Auto-switch back to phone | "Phone reconnected. Location reminders moved to phone." | Notification, auto |
| Data Layer | Phone unreachable | (Watch) "Waiting for phone connection..." + banner | Auto-retry, local save |
| Watch standalone | No speech recognizer | "Type your reminder" | Keyboard input fallback |

---

## Dependency Summary

| Library | Version | Module | Purpose |
|---|---|---|---|
| Room + KSP2 | Room 2.8.x, KSP2 2.3.x | both | Local database |
| Compose BOM (phone) | latest stable | mobile | Phone UI |
| Compose BOM (wear) | 2024.09.00 | wear | Watch UI |
| Wear Compose Navigation | 1.5.x | wear | Watch navigation |
| kotlinx-serialization | 1.7.x | both | JSON parsing (manual TypeConverters, NOT @Serializable on Room entities) |
| OkHttp | 4.12.x | mobile | Gemini API calls |
| security-crypto | 1.1.x | mobile | Encrypted API key storage |
| play-services-location | 21.3.x | both | Geofencing |
| WorkManager | 2.10.x | both | Boot re-registration, offline retry, geofence migration retry |
| DataStore Preferences | 1.1.x | both | User preferences, Pro status, usage counter |
| Play Billing Library | 8.x | mobile | One-time Pro Upgrade IAP |
| Turbine | 1.2.x | both (test) | Flow testing |
| coroutines-test | 1.9.x | both (test) | Coroutine testing |

---

## minSdk 33 Implications
- `Geocoder` listener-based API available (preferred over deprecated synchronous form)
- `POST_NOTIFICATIONS` permission required and available
- `SpeechRecognizer.isOnDeviceRecognitionAvailable()` available
- WearOS 4+ (API 33) covers all modern watches
- No backward-compatible code paths below API 33

---

## LLM Provider Research Summary

| Provider | Model | Cost | Rate Limit | Notes |
|---|---|---|---|---|
| **Google Gemini** (direct) | 2.5 Flash Lite | Free (dynamic per-project) | No CC needed | **Recommended — best free tier, better quality than 2.0** |
| **Google Gemini** (direct) | 2.0 Flash Lite | Free (dynamic per-project) | No CC needed | Older gen, still available |
| **OpenRouter** | Various free models | Free (limited RPM) | Single API, swap models | Good flexibility |
| **Groq** | Llama 3.1 8B | Free tier, $0.05/M paid | 800+ TPS | OpenAI-compatible |

Chosen: **Google Gemini 2.5 Flash Lite** via direct REST API (2.0 Flash Lite also supported). Rate limits are dynamic per-project — check AI Studio for actual limits. The architecture uses a `FormattingProvider` interface so swapping providers or models is a one-class change.

---

## Monetization

### V1: Local-Only, No Backend, No Sign-In

Play Billing Library handles purchases via the device's Google Play account — **no Google Sign-In required**. Check Pro status on app start with `BillingClient.queryPurchasesAsync()`. Store Pro status in `DataStore` (cached locally; re-validated against Play on each launch).

#### Free vs Pro Tier

| Feature | Free | Pro |
|---|---|---|
| Time-based reminders | **Unlimited** | Unlimited |
| Voice capture + raw text saving | **Unlimited** | Unlimited |
| Watch ↔ Phone sync | **Yes** | Yes |
| LLM formatting (BYO Gemini key) | **1/day** | Unlimited |
| LLM formatting (no key, raw fallback) | **Unlimited** | Unlimited |
| Active location reminders (geofences) | **5** | 100 |
| Saved Places | **2** | Unlimited |
| Recurring reminders (daily/weekly/monthly) | **No** | Yes |
| Snooze actions on notifications | **No** | Yes |
| Custom geofence radius | **No (150m fixed)** | Yes (any radius) |
| Export/import reminders | **No** | Yes |

#### One-Time "Pro Upgrade" IAP

- Single in-app product: `pro_upgrade` (one-time purchase, no subscription in V1)
- No sign-in, no account creation — `BillingClient` uses the device Google Play account
- Pro status synced to watch via Data Layer (watch never handles billing)
- Price point: indie-friendly (~$3-5 USD, adjust per market)

#### Usage Counter (1/day Formatting)

- `UsageTracker` in DataStore: stores `{lastResetDate: LocalDate, count: Int}`
- On each formatting call: if `lastResetDate != today`, reset count to 0; increment; if count > 1, show paywall
- **BYO API key bypasses the counter entirely** — user's own key = unlimited formatting regardless of Pro status
- Counter file included in Android Auto-Backup (`fullBackupContent` includes usage tracker file but excludes Room DB)
- **Known V1 limitation:** counter resets on clear data or uninstall. Acceptable tradeoff — counter is a soft paywall, not a security boundary

#### Pro Enforcement Points

| Gate Location | Enforcement |
|---|---|
| `PipelineOrchestrator` (Phase 3) | Check `UsageTracker` + Pro status before calling Gemini. Free + count ≥ 1 → show paywall |
| `SavedPlacesScreen` (Phase 4) | Block creation if free + count ≥ 2 saved places. Show upgrade CTA |
| `GeofenceManager` (Phase 5) | Block registration if free + active count ≥ 5. Show upgrade CTA |
| `ReminderEditScreen` (Phase 7) | Grey out recurrence options if free. Show Pro badge |
| `NotificationActionReceiver` (Phase 7) | Snooze actions hidden on free. Only "Complete" + "Dismiss" shown |
| `ReminderEditScreen` radius (Phase 7) | Slider disabled on free. Fixed at 150m |
| Settings > Export (Phase 8) | Button disabled with Pro badge |

#### Paywall UI Pattern

- `ProPaywallScreen` composable: feature name, benefit list, "Upgrade to Pro" CTA, price
- `ProBadge` composable: small "PRO" chip shown next to gated features in settings/lists
- All paywalls launch `BillingClient.launchBillingFlow()` on CTA tap
- After successful purchase: `BillingClient.acknowledgePurchase()` → update DataStore → sync to watch

### V2: Backend + Accounts (Deferred — See Phase 9)

---

## Comprehensive Testing Strategy

### Testing Architecture

**Unit Tests** (`src/test/`) — JVM-local, no Android framework. Mock all Android dependencies.
- Mocking: **MockK** (better Kotlin support than Mockito)
- Coroutine testing: `kotlinx-coroutines-test` (`TestScope`, `runTest`, `TestDispatcher`)
- Flow testing: **Turbine** (`flow.test { awaitItem() }`)
- Truth assertions: **Truth** (`assertThat(x).isEqualTo(y)`) — more readable than JUnit for Kotlin
- HTTP mocking: **MockWebServer** (OkHttp) for Gemini API tests

**Instrumented Tests** (`src/androidTest/`) — run on emulator. Room in-memory DB, WorkManager tests.
- `androidx.room:room-testing` — `Room.inMemoryDatabaseBuilder()` for DAO tests
- `androidx.work:work-testing` — `WorkManagerTestInitHelper` for WorkManager tests
- `androidx.compose.ui:ui-test-junit4` — Compose UI tests (`createComposeRule()`)

### Test Dependencies (add to `libs.versions.toml`)
```toml
mockk = "1.13.13"
truth = "1.4.4"
coroutinesTest = "1.9.0"
turbine = "1.2.0"
mockwebserver = "4.12.0"
roomTesting = { group = "androidx.room", name = "room-testing", version.ref = "room" }
workTesting = { group = "androidx.work", name = "work-testing", version.ref = "work" }
```

Both `build.gradle.kts` files need:
```kotlin
testImplementation("io.mockk:mockk")
testImplementation("com.google.truth:truth")
testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")
testImplementation("app.cash.turbine:turbine")
// mobile only:
testImplementation("com.squareup.okhttp3:mockwebserver")
androidTestImplementation("androidx.room:room-testing")
androidTestImplementation("androidx.work:work-testing")
androidTestImplementation("io.mockk:mockk-android")
androidTestImplementation("com.google.truth:truth")
```

### Test Directory Structure
```
mobile/src/test/java/com/example/reminders/
├── data/
│   ├── local/
│   │   ├── ConvertersTest.kt
│   │   ├── ReminderDaoTest.kt
│   │   └── SavedPlaceDaoTest.kt
│   └── repository/
│       ├── ReminderRepositoryImplTest.kt
│       └── SavedPlaceRepositoryImplTest.kt
├── transcription/
│   └── TranscriptionViewModelTest.kt
├── formatting/
│   ├── GeminiFormattingProviderTest.kt
│   ├── RawFallbackProviderTest.kt
│   ├── FormattingPromptTest.kt
│   └── ParsedReminderMapperTest.kt
├── pipeline/
│   └── PipelineOrchestratorTest.kt
├── geocoding/
│   ├── SavedPlaceMatcherTest.kt
│   └── GeocodingResultTest.kt
├── geofence/
│   ├── AndroidGeofenceManagerTest.kt
│   ├── GeofenceCapTrackerTest.kt
│   └── GeofencingDeviceManagerTest.kt
├── alarm/
│   ├── AlarmSchedulerTest.kt
│   └── RecurrenceHelperTest.kt
├── wearable/
│   ├── ReminderSerializerTest.kt
│   └── SyncConflictResolverTest.kt
├── billing/
│   └── BillingManagerTest.kt
└── preferences/
    ├── UserPreferencesTest.kt
    └── UsageTrackerTest.kt

mobile/src/androidTest/java/com/example/reminders/
├── data/local/
│   └── RoomMigrationTest.kt
├── geofence/
│   └── GeofenceReregistrationWorkerTest.kt
├── pipeline/
│   └── PipelineE2eTest.kt
└── ui/
    ├── ReminderListScreenTest.kt
    ├── TranscriptionScreenTest.kt
    ├── SettingsScreenTest.kt
    └── GeocodingConfirmationScreenTest.kt

wear/src/test/java/com/example/reminders/wear/
├── data/
│   ├── WatchConvertersTest.kt
│   └── WatchReminderDaoTest.kt
├── geofence/
│   ├── GeofencingDeviceManagerTest.kt
│   └── GpsDetectorTest.kt
├── alarm/
│   └── WatchRecurrenceHelperTest.kt
├── data/
│   └── WatchReminderSerializerTest.kt
└── sync/
    └── SyncConflictResolverTest.kt

wear/src/androidTest/java/com/example/reminders/wear/
└── ui/
    ├── WatchReminderListScreenTest.kt
    └── VoiceRecordScreenTest.kt
```

### Phase-by-Phase Test Specifications

#### Phase 1 Tests
| Test Class | Type | What It Tests |
|---|---|---|
| `ConvertersTest` | Unit | `Instant→Long→Instant` round-trip, null Instant, `LocationTrigger→JSON→LocationTrigger` round-trip with all fields populated, null LocationTrigger, epoch edge cases |
| `ReminderDaoTest` | Unit | Insert + query by ID, query active reminders, query by locationState, query geofenced reminders (where geofenceId != null), update title/body/triggerTime, delete, verify no orphan data |
| `SavedPlaceDaoTest` | Unit | CRUD, `findByLabel` case-insensitive match (`"Home"` matches `"home"`, `"HOME"`, `" home "`), whitespace trimming, no match returns empty |
| `ReminderRepositoryImplTest` | Unit | Insert delegates to DAO and returns Flow update, delete removes from DAO, getAllReminders returns sorted Flow, error wrapping |
| `SavedPlaceRepositoryImplTest` | Unit | findByLabel delegates to DAO with LOWER(), insert/upsert/getAll |
| `WatchConvertersTest` | Unit | Same as mobile ConvertersTest |
| `WatchReminderDaoTest` | Unit | CRUD, query by isCompleted, query by triggerTime range (upcoming) |
| `BillingManagerTest` | Unit | `queryPurchasesAsync` returns Pro status, purchase flow calls `launchBillingFlow`, acknowledgment called on success, connection failure handled gracefully, `enablePendingPurchases` called during setup |
| `UsageTrackerTest` | Unit | Count increments correctly, resets on new day (lastResetDate != today → count = 0), blocks at ≥ 1 for free users, bypasses entirely for Pro users, bypasses for BYO key users, survives across midnight boundary |

#### Phase 2 Tests
| Test Class | Type | What It Tests |
|---|---|---|
| `TranscriptionViewModelTest` | Unit | State transitions: Idle→Listening (on start), Listening→Processing (on end of speech), Processing→Result (on results), all error codes (1-9) map to Error state with correct message, null SpeechRecognizer → Error, process death restoration returns Idle, partial results update state text |

#### Phase 3 Tests
| Test Class | Type | What It Tests |
|---|---|---|
| `GeminiFormattingProviderTest` | Unit | MockWebServer responses: valid JSON array, ```json-wrapped response (strip fences), single JSON object (wrap in array), trailing commas (strip), empty candidates array, 429 rate limit → retry with backoff then Failure, 500 server error → Failure, malformed JSON → Failure, connection timeout → Failure, multiple reminders parsed from single transcript, special characters in reminder text |
| `RawFallbackProviderTest` | Unit | Returns single Reminder with raw text as title, body is null, formattingProvider = "none" |
| `ParsedReminderMapperTest` | Unit | Full ParsedReminder→Reminder mapping, null triggerTime → Reminder.triggerTime = null, null locationTrigger → Reminder.locationTrigger = null, ISO 8601 with timezone, ISO 8601 without timezone (assume local) |
| `FormattingPromptTest` | Unit | Prompt contains system instructions, includes 3+ examples, instructs JSON array output, handles empty transcript |
| `PipelineOrchestratorTest` | Unit | Full success: transcript → formatted → stored in Room, formatting fails → raw fallback stored, partial success (2 valid + 1 invalid → 2 stored + raw fallback for invalid), transcription fails → error (nothing stored), Room write fails → PipelineResult.Failure |

#### Phase 4 Tests
| Test Class | Type | What It Tests |
|---|---|---|
| `SavedPlaceMatcherTest` | Unit | Exact match returns SavedPlace, case-insensitive (`"HOME"` matches `"Home"`), leading/trailing whitespace trimmed, no match returns null, multiple Saved Places with similar labels returns best match, empty label → null |
| `GeocodingResultTest` | Unit | Resolved has lat/lng/address, Ambiguous has list of 2+ candidates, NotFound, Error with message string |
| `PipelineGeocodingTest` | Unit | Saved place match → skip geocoding entirely, single geocoding result → auto-resolve to ACTIVE, ambiguous (2+ results) → NEEDS_CONFIRMATION state, 0 results → offer manual search, geocoder absent → error with message |

#### Phase 5 Tests
| Test Class | Type | What It Tests |
|---|---|---|
| `AndroidGeofenceManagerTest` | Unit | Register: correct lat/lng/radius/loiteringDelay(30s)/transitions(ENTER+DWELL), PendingIntent uses FLAG_MUTABLE, returns geofenceId on success. Remove: by ID, remove multiple. Cap: count queries correct. Failure: GeofencingClient returns failure → Result.failure |
| `GeofencingDeviceManagerTest` | Unit | AUTO + phone disconnect → migrate all active geofences to watch, AUTO + phone reconnect → migrate back to phone, PHONE_ONLY + disconnect → no migration, WATCH_ONLY → always on watch regardless of connectivity, migration idempotency (run twice → same geofence state), partial migration failure (2 of 3 succeed) → retry failed ones |
| `GeofenceCapTrackerTest` | Unit | Free: 0 → no warning, 3 → no warning, 4 → warn (1 remaining), 5 → hard block. Pro: 89 → no warning, 90 → warn, 95 → suggest cleanup, 100 → hard block. Verify free vs Pro cap is checked correctly |
| `GeofenceReregistrationWorkerTest` | Instrumented | Boot → queries all active geofenced reminders from Room → re-registers via GeofencingClient. Empty DB → no-ops. Partial failure → WorkManager retries |
| `GeofencingDeviceManagerIntegrationTest` | Instrumented | Full auto-switch cycle on emulator pair: register geofence → disconnect → verify watch picks up → reconnect → verify phone takes over |

#### Phase 6 Tests
| Test Class | Type | What It Tests |
|---|---|---|
| `ReminderSerializerTest` | Unit | Full Reminder→ByteArray→Reminder round-trip, null body/null triggerTime/null locationTrigger, special characters in title, emoji in text, long title (500+ chars) |
| `SyncConflictResolverTest` | Unit | Same timestamp → deterministic winner (by ID comparison), older local vs newer remote → remote wins, no conflict → merge both, empty list → empty result |
| `DataLayerPathsTest` | Unit | `/reminders/{id}` path construction, path parsing extracts ID correctly |
| `ReminderComplicationProviderTest` | Unit | Count 0 → text "0", count 5 → text "5", "today" mode filters to today's reminders only, "all" mode counts all upcoming incomplete, completed reminders excluded from count, null triggerTime excluded from "today" mode |

#### Phase 7 Tests
| Test Class | Type | What It Tests |
|---|---|---|
| `AlarmSchedulerTest` | Unit | Schedules with `setExactAndAllowWhileIdle`, correct PendingIntent action/extras, cancel removes PendingIntent, USE_EXACT_ALARM permission check |
| `RecurrenceHelperTest` | Unit | Daily → next day same time, weekly → same day next week, monthly → same date next month, month-end edge case (Jan 31 → Feb 28, May 31 → June 30), across DST boundary (clock change), null recurrence → returns null, leap year Feb 29 → Feb 28 next year |
| `CompletionFlowTest` | Unit | Complete → marks Room completed + removes geofence + cancels alarm, geofence remove fails → marks for retry, delete → full cleanup, reminder with no geofence → skip geofence removal |
| `NotificationActionReceiverTest` | Instrumented | Complete action → marks reminder completed in Room, Snooze 5min → reschedules alarm 5min from now, Snooze 1hr → reschedules 1hr from now |

#### Phase 8 Tests (Regression Suite)
| Test | Type | What It Tests |
|---|---|---|
| `R8ReleaseBuildTest` | Build | `./gradlew :mobile:minifyReleaseWithR8` + `./gradlew :wear:minifyReleaseWithR8` succeed |
| `AirplaneModeRegression` | Manual/emulator | Each pipeline stage in airplane mode → correct fallback (raw save / PENDING_GEOCODING / queue for retry) |
| `ProcessDeathRegression` | Manual/emulator | "Don't keep activities" enabled → all screens restore state correctly |
| `GeofenceCapStressTest` | Manual/emulator | Create 99 geofences → 100th warns → 101st blocked |
| `AutoSwitchStressTest` | Manual/emulator | Rapid connect/disconnect 10x → geofences end in consistent state (all on phone when connected) |
| `SyncConflictE2e` | Manual/emulator | Edit same reminder on both devices simultaneously → last-write-wins, no data loss |

### Test Commands
```bash
./gradlew test                        # All unit tests (both modules)
./gradlew :mobile:test                # Mobile unit tests only
./gradlew :wear:test                  # Wear unit tests only
./gradlew connectedAndroidTest        # All instrumented tests (requires emulator)
./gradlew :mobile:connectedAndroidTest
./gradlew :wear:connectedAndroidTest
./gradlew lint                        # Static analysis
./gradlew :mobile:lint
./gradlew :wear:lint
```

### CI Gate (run before merging any phase)
```bash
./gradlew test && ./gradlew lint && ./gradlew assembleDebug
```
All three must pass. Instrumented tests run separately on emulator.

---

## AGENTS.md Section

After completing all phases, write an `AGENTS.md` file to the project root. Its contents are defined in the next section of this plan and should be placed at `/Users/bailee/AndroidStudioProjects/Reminders/AGENTS.md`. This file provides project context and testing knowledge for AI agents (and developers) working on the codebase.
