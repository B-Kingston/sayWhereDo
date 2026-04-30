package com.example.reminders.di

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.location.Geocoder
import android.util.Log
import androidx.room.Room
import com.example.reminders.MainActivity
import com.example.reminders.alarm.AlarmScheduler
import com.example.reminders.alarm.AndroidAlarmScheduler
import com.example.reminders.alarm.ReminderCompletionManager
import com.example.reminders.billing.BillingManager
import com.example.reminders.data.local.RemindersDatabase
import com.example.reminders.data.preferences.UsageTracker
import com.example.reminders.data.preferences.UserPreferences
import com.example.reminders.data.repository.ReminderRepository
import com.example.reminders.data.repository.ReminderRepositoryImpl
import com.example.reminders.data.repository.SavedPlaceRepository
import com.example.reminders.data.repository.SavedPlaceRepositoryImpl
import com.example.reminders.formatting.AiProviderPresets
import com.example.reminders.formatting.CloudFormattingProvider
import com.example.reminders.formatting.GeminiFormattingProvider
import com.example.reminders.formatting.RawFallbackProvider
import com.example.reminders.geocoding.AndroidGeocodingService
import com.example.reminders.geocoding.GeocodingService
import com.example.reminders.geocoding.SavedPlaceMatcher
import com.example.reminders.geofence.AndroidGeofenceManager
import com.example.reminders.geofence.GeofenceBroadcastReceiver
import com.example.reminders.geofence.GeofenceCapTracker
import com.example.reminders.geofence.GeofenceManager
import com.example.reminders.ml.AvailableModels
import com.example.reminders.ml.LocalFormattingProvider
import com.example.reminders.ml.LocalModelManager
import com.example.reminders.ml.MediaPipeInferenceWrapper
import com.example.reminders.network.GeminiApiClient
import com.example.reminders.network.OpenAiCompatibleClient
import com.example.reminders.offline.OfflineQueueContainer
import com.example.reminders.offline.OfflineQueueManager
import com.example.reminders.offline.PendingOperationDao
import com.example.reminders.offline.PendingOperationsDatabase
import com.example.reminders.pipeline.PipelineOrchestrator
import com.example.reminders.sync.ReminderSyncClient
import com.example.reminders.sync.WearableSyncClient
import com.example.reminders.wearable.SyncEngine
import com.example.reminders.wearable.WearableDataSender
import com.example.reminders.wearable.WatchConnectivityMonitor
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * Manual dependency injection container for the mobile module.
 *
 * Instantiates all services, repositories, and managers in the correct
 * order. Provides lazy initialisation for heavyweight objects (Room
 * databases, BillingClient).
 *
 * Implements [OfflineQueueContainer] so that the WorkManager offline
 * queue worker can access the dependencies it needs without static
 * singletons or a DI framework.
 */
class AppContainer(context: Context) : OfflineQueueContainer {

    private val database: RemindersDatabase = Room.databaseBuilder(
        context,
        RemindersDatabase::class.java,
        "reminders-db"
    )
        .addMigrations(RemindersDatabase.MIGRATION_1_2)
        .build()

    val reminderRepository: ReminderRepository = ReminderRepositoryImpl(
        reminderDao = database.reminderDao(),
        deletedReminderDao = database.deletedReminderDao()
    )

    val savedPlaceRepository: SavedPlaceRepository = SavedPlaceRepositoryImpl(
        database.savedPlaceDao()
    )

    override val geocodingService: GeocodingService = AndroidGeocodingService(Geocoder(context))

    override val savedPlaceMatcher = SavedPlaceMatcher(savedPlaceRepository)

    val userPreferences = UserPreferences(context)
    val usageTracker = UsageTracker(context)

    val billingManager = try {
        BillingManager(context).also {
            Log.i(TAG, "BillingManager created successfully")
        }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to create BillingManager — Play Services may be unavailable", e)
        throw e
    }

    val geminiApiClient = GeminiApiClient()

    val geminiFormattingProvider = GeminiFormattingProvider(
        apiClient = geminiApiClient,
        apiKeyProvider = { userPreferences.apiKey.first() ?: "" }
    )

    /**
     * Cloud formatting provider configured from the user's AI provider
     * preferences (base URL, model, API key). Falls back to the preset
     * defaults when the user has not customised a field.
     */
    val cloudFormattingProvider: CloudFormattingProvider by lazy {
        val providerId = runBlocking { userPreferences.aiProviderId.first() }
        val provider = AiProviderPresets.getById(providerId)
        val baseUrl = runBlocking { userPreferences.aiBaseUrl.first() }
            .ifBlank { provider?.baseUrl ?: "" }
        val model = runBlocking { userPreferences.aiModelName.first() }
            .ifBlank { provider?.defaultModel ?: "" }
        val client = OpenAiCompatibleClient(baseUrl, model)
        CloudFormattingProvider(
            apiClient = client,
            apiKeyProvider = { userPreferences.apiKey.first() ?: "" }
        )
    }

    /**
     * Manages downloading and storing on-device LLM model files.
     */
    val localModelManager = LocalModelManager(context)

    /**
     * On-device formatting provider using MediaPipe LLM inference.
     *
     * Lazily initialises the model based on the user's selected model
     * preference, defaulting to [AvailableModels.GEMMA_2_2B_Q4].
     */
    val localFormattingProvider: LocalFormattingProvider by lazy {
        val modelId = runBlocking { userPreferences.localModelId.first() }
            ?: AvailableModels.GEMMA_2_2B_Q4.id
        val modelInfo = AvailableModels.getById(modelId)
            ?: AvailableModels.GEMMA_2_2B_Q4
        LocalFormattingProvider(
            modelManager = localModelManager,
            modelInfo = modelInfo,
            inferenceFactory = { modelFile ->
                MediaPipeInferenceWrapper(modelFile.absolutePath)
            }
        )
    }

    val rawFallbackProvider = RawFallbackProvider()

    /**
     * PendingIntent fired by Play Services when a geofence transition occurs.
     * Uses [GeofenceBroadcastReceiver] to process the event.
     */
    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        PendingIntent.getBroadcast(
            context,
            GEOFENCE_PENDING_INTENT_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    val geofenceManager: GeofenceManager = AndroidGeofenceManager(
        context = context,
        geofencePendingIntent = geofencePendingIntent,
        reminderRepository = reminderRepository
    )

    val geofenceCapTracker = GeofenceCapTracker(billingManager.isPro)

    /**
     * Schedules and cancels time-based reminder alarms via AlarmManager.
     */
    val alarmScheduler: AlarmScheduler = AndroidAlarmScheduler(
        context = context,
        reminderRepository = reminderRepository
    )

    val wearableDataSender = WearableDataSender(context)

    /**
     * Monitors connectivity to a paired WearOS watch via the CapabilityClient.
     * Provides a reactive [Flow] that emits `true` when at least one watch
     * node is reachable.
     */
    val watchConnectivityMonitor = WatchConnectivityMonitor(context)

    /**
     * Reconciles local and remote reminder state using the tombstone-based
     * bidirectional sync algorithm. Stateless — safe to share across callers.
     */
    val syncEngine = SyncEngine()

    val syncClient: ReminderSyncClient = WearableSyncClient(
        wearableDataSender = wearableDataSender,
        reminderRepository = reminderRepository
    )

    /**
     * Orchestrates completion and deletion flows with full resource cleanup.
     */
    val reminderCompletionManager = ReminderCompletionManager(
        reminderRepository = reminderRepository,
        geofenceManager = geofenceManager,
        alarmScheduler = alarmScheduler,
        syncClient = syncClient
    )

    override val pipelineOrchestrator = PipelineOrchestrator(
        formattingProviderFactory = {
            val backend = userPreferences.formattingBackend.first()
            if (backend == "local") {
                localFormattingProvider
            } else {
                cloudFormattingProvider
            }
        },
        rawFallbackProvider = rawFallbackProvider,
        reminderRepository = reminderRepository,
        usageTracker = usageTracker,
        billingManager = billingManager,
        userPreferences = userPreferences,
        syncClient = syncClient
    )

    private val pendingOperationsDatabase: PendingOperationsDatabase = Room.databaseBuilder(
        context,
        PendingOperationsDatabase::class.java,
        "pending-operations-db"
    ).build()

    private val pendingOperationDao: PendingOperationDao =
        pendingOperationsDatabase.pendingOperationDao()

    override val offlineQueueManager = OfflineQueueManager(
        context = context,
        dao = pendingOperationDao
    )

    init {
        Log.i(TAG, "AppContainer initialised — all dependencies created")
    }

    companion object {
        private const val TAG = "AppContainer"
        private const val GEOFENCE_PENDING_INTENT_REQUEST_CODE = 0
    }
}
