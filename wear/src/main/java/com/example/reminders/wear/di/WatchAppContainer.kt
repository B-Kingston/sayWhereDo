package com.example.reminders.wear.di

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.room.Room
import com.example.reminders.wear.alarm.AndroidWatchAlarmScheduler
import com.example.reminders.wear.alarm.WatchAlarmScheduler
import com.example.reminders.wear.data.WatchRemindersDatabase
import com.example.reminders.wear.geofence.GeofencingDeviceManager
import com.example.reminders.wear.geofence.GpsDetector
import com.example.reminders.wear.geofence.WatchGeofenceBroadcastReceiver
import com.example.reminders.wear.geofence.WatchGeofenceManager
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

    val watchReminderDao = database.watchReminderDao()

    val gpsDetector = GpsDetector(applicationContext.packageManager)

    /**
     * Manages geofence registration on the watch via the Play Services GeofencingClient.
     */
    val watchGeofenceManager = WatchGeofenceManager(
        geofencingClient = LocationServices.getGeofencingClient(applicationContext)
    )

    /**
     * PendingIntent fired by Play Services when a geofence transition occurs on the watch.
     */
    val geofencePendingIntent: PendingIntent by lazy {
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
    )

    /**
     * Schedules and cancels time-based reminder alarms on the watch.
     */
    val watchAlarmScheduler: WatchAlarmScheduler = AndroidWatchAlarmScheduler(
        context = context,
        watchReminderDao = watchReminderDao
    )

    companion object {
        private const val GEOFENCE_PENDING_INTENT_REQUEST_CODE = 0
    }
}
