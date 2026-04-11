package com.example.reminders.wear.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.reminders.wear.di.WatchRemindersApplication
import kotlinx.serialization.json.Json

/**
 * Listens for [Intent.ACTION_BOOT_COMPLETED] on the watch and enqueues the
 * [WatchGeofenceReregistrationWorker] to restore geofences cleared by reboot.
 */
class WatchGeofenceBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val workRequest = OneTimeWorkRequestBuilder<WatchGeofenceReregistrationWorker>()
            .build()

        WorkManager.getInstance(context).enqueue(workRequest)
    }
}

/**
 * WorkManager worker that re-registers all active geofences on the watch
 * after a device reboot.
 *
 * Queries Room for all reminders with active geofences and re-registers
 * them via [WatchGeofenceManager].
 */
class WatchGeofenceReregistrationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        Log.i(TAG, "Starting watch geofence re-registration")

        val container = (applicationContext as WatchRemindersApplication).container
        val dao = container.watchReminderDao
        val geofenceManager = container.watchGeofenceManager
        val pendingIntent = container.geofencePendingIntent

        val geofencedReminders = dao.getAllGeofencedOnce()
        if (geofencedReminders.isEmpty()) {
            Log.d(TAG, "No geofenced reminders to re-register")
            return Result.success()
        }

        var successCount = 0
        var failureCount = 0

        for (reminder in geofencedReminders) {
            val result = geofenceManager.registerGeofence(reminder, pendingIntent)
            if (result.isSuccess) {
                successCount++
                Log.d(TAG, "Re-registered geofence for ${reminder.id}")
            } else {
                failureCount++
                Log.e(TAG, "Failed to re-register for ${reminder.id}: ${result.exceptionOrNull()?.message}")
            }
        }

        Log.i(TAG, "Re-registration: $successCount success, $failureCount failed")
        return if (failureCount == 0) Result.success() else Result.retry()
    }

    companion object {
        private const val TAG = "WatchGeofenceReregister"
    }
}
