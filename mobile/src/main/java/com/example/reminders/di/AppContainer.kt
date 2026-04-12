package com.example.reminders.di

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.location.Geocoder
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
import com.example.reminders.formatting.GeminiFormattingProvider
import com.example.reminders.formatting.RawFallbackProvider
import com.example.reminders.geocoding.AndroidGeocodingService
import com.example.reminders.geocoding.GeocodingService
import com.example.reminders.geocoding.SavedPlaceMatcher
import com.example.reminders.geofence.AndroidGeofenceManager
import com.example.reminders.geofence.GeofenceBroadcastReceiver
import com.example.reminders.geofence.GeofenceCapTracker
import com.example.reminders.geofence.GeofenceManager
import com.example.reminders.network.GeminiApiClient
import com.example.reminders.offline.OfflineQueueContainer
import com.example.reminders.offline.OfflineQueueManager
import com.example.reminders.offline.PendingOperationDao
import com.example.reminders.offline.PendingOperationsDatabase
import com.example.reminders.pipeline.PipelineOrchestrator
import com.example.reminders.sync.NoOpSyncClient
import com.example.reminders.sync.ReminderSyncClient
import kotlinx.coroutines.flow.first

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
    ).build()

    val reminderRepository: ReminderRepository = ReminderRepositoryImpl(
        database.reminderDao()
    )

    val savedPlaceRepository: SavedPlaceRepository = SavedPlaceRepositoryImpl(
        database.savedPlaceDao()
    )

    val geocodingService: GeocodingService = AndroidGeocodingService(Geocoder(context))

    val savedPlaceMatcher = SavedPlaceMatcher(savedPlaceRepository)

    val userPreferences = UserPreferences(context)
    val usageTracker = UsageTracker(context)
    val billingManager = BillingManager(context)

    val geminiApiClient = GeminiApiClient()

    val geminiFormattingProvider = GeminiFormattingProvider(
        apiClient = geminiApiClient,
        apiKeyProvider = { userPreferences.apiKey.first() ?: "" }
    )

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

    /**
     * Phase 6 sync client stub — replaced with real Data Layer client when merged.
     */
    val syncClient: ReminderSyncClient = NoOpSyncClient()

    /**
     * Orchestrates completion and deletion flows with full resource cleanup.
     */
    val reminderCompletionManager = ReminderCompletionManager(
        reminderRepository = reminderRepository,
        geofenceManager = geofenceManager,
        alarmScheduler = alarmScheduler,
        syncClient = syncClient
    )

    val pipelineOrchestrator = PipelineOrchestrator(
        formattingProvider = geminiFormattingProvider,
        rawFallbackProvider = rawFallbackProvider,
        reminderRepository = reminderRepository,
        usageTracker = usageTracker,
        billingManager = billingManager,
        userPreferences = userPreferences
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

    companion object {
        private const val GEOFENCE_PENDING_INTENT_REQUEST_CODE = 0
    }
}
