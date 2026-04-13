package com.example.reminders.wear.di

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.room.Room
import com.example.reminders.wear.alarm.AndroidWatchAlarmScheduler
import com.example.reminders.wear.alarm.WatchAlarmScheduler
import com.example.reminders.wear.alarm.WatchReminderCompletionManager
import com.example.reminders.wear.complication.ComplicationPreferences
import com.example.reminders.wear.data.WatchReminderRepository
import com.example.reminders.wear.data.WatchRemindersDatabase
import com.example.reminders.wear.data.WearDataLayerClient
import com.example.reminders.wear.data.WearUserPreferences
import com.example.reminders.wear.formatting.WatchFormattingManager
import com.example.reminders.wear.geofence.GeofencingDeviceManager
import com.example.reminders.wear.geofence.GpsDetector
import com.example.reminders.wear.geofence.WatchGeofenceBroadcastReceiver
import com.example.reminders.wear.geofence.WatchGeofenceManager
import com.example.reminders.wear.notification.WatchNotificationManager
import com.google.android.gms.location.LocationServices

/**
 * Manual dependency injection container for the watch module.
 *
 * Provides all watch-specific services: Room database, geofence manager,
 * GPS detector, and device manager for auto-switch logic.
 */
class WatchAppContainer(context: Context) {

    private val applicationContext = context.applicationContext

    val database: WatchRemindersDatabase = Room.databaseBuilder(
        applicationContext,
        WatchRemindersDatabase::class.java,
        "watch-reminders-db"
    )
        .addMigrations(WatchRemindersDatabase.MIGRATION_1_2)
        .build()
        .also { Log.i(TAG, "Room database created") }

    val watchReminderDao = database.watchReminderDao()
        .also { Log.d(TAG, "Reminder DAO acquired") }

    val watchReminderRepository = WatchReminderRepository(watchReminderDao)
        .also { Log.d(TAG, "WatchReminderRepository created") }

    val wearDataLayerClient = WearDataLayerClient(applicationContext)
        .also { Log.d(TAG, "WearDataLayerClient created") }

    val complicationPreferences = ComplicationPreferences(applicationContext)
        .also { Log.d(TAG, "ComplicationPreferences created") }

    val wearUserPreferences = WearUserPreferences(applicationContext)
        .also { Log.d(TAG, "WearUserPreferences created") }

    val gpsDetector = GpsDetector(applicationContext.packageManager)
        .also { Log.d(TAG, "GpsDetector created") }

    /**
     * Manages geofence registration on the watch via the Play Services GeofencingClient.
     */
    val watchGeofenceManager = WatchGeofenceManager(
        geofencingClient = LocationServices.getGeofencingClient(applicationContext)
    ).also { Log.d(TAG, "WatchGeofenceManager created") }

    /**
     * PendingIntent fired by Play Services when a geofence transition occurs on the watch.
     */
    val geofencePendingIntent: PendingIntent by lazy {
        Log.d(TAG, "Creating geofence PendingIntent")
        val intent = Intent(applicationContext, WatchGeofenceBroadcastReceiver::class.java)
        PendingIntent.getBroadcast(
            applicationContext,
            GEOFENCE_PENDING_INTENT_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    /**
     * Manages geofencing device preference and auto-switch logic.
     */
    val geofencingDeviceManager = GeofencingDeviceManager(
        context = applicationContext,
        watchReminderDao = watchReminderDao,
        watchGeofenceManager = watchGeofenceManager,
        geofencePendingIntent = geofencePendingIntent
    ).also { Log.d(TAG, "GeofencingDeviceManager created") }

    /**
     * Schedules and cancels time-based reminder alarms on the watch.
     */
    val watchAlarmScheduler: WatchAlarmScheduler = AndroidWatchAlarmScheduler(
        context = context,
        watchReminderDao = watchReminderDao
    ).also { Log.d(TAG, "WatchAlarmScheduler created") }

    val watchNotificationManager = WatchNotificationManager(applicationContext)
        .also { Log.d(TAG, "WatchNotificationManager created") }

    val watchFormattingManager = WatchFormattingManager(wearUserPreferences, applicationContext)
        .also { Log.d(TAG, "WatchFormattingManager created") }

    val watchReminderCompletionManager = WatchReminderCompletionManager(
        watchReminderDao = watchReminderDao,
        watchGeofenceManager = watchGeofenceManager,
        geofencePendingIntent = geofencePendingIntent,
        alarmScheduler = watchAlarmScheduler,
        notificationManager = watchNotificationManager
    ).also { Log.d(TAG, "WatchReminderCompletionManager created") }

    companion object {
        private const val TAG = "WatchAppContainer"
        private const val GEOFENCE_PENDING_INTENT_REQUEST_CODE = 0
    }
}
