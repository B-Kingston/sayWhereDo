package com.example.reminders.geofence

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.reminders.data.model.LocationReminderState
import com.example.reminders.data.repository.ReminderRepository
import com.example.reminders.di.RemindersApplication
import kotlinx.coroutines.flow.first

/**
 * WorkManager worker that re-registers all active geofences after a device reboot.
 *
 * Android clears all geofences when the device restarts. This worker is
 * triggered by the [GeofenceBootReceiver] on [Intent.ACTION_BOOT_COMPLETED].
 * It queries Room for all reminders in the [ACTIVE][LocationReminderState.ACTIVE]
 * location state and re-registers their geofences via [AndroidGeofenceManager].
 */
class GeofenceReregistrationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        Log.i(TAG, "Starting geofence re-registration")

        val container = (applicationContext as RemindersApplication).container
        val repository = container.reminderRepository
        val geofenceManager = container.geofenceManager

        // Rebuild the in-memory registered set from Room DB after process restart
        (geofenceManager as? AndroidGeofenceManager)?.rebuildRegisteredSetFromDb()

        val geofencedReminders = repository.getGeofencedRemindersOnce()

        if (geofencedReminders.isEmpty()) {
            Log.d(TAG, "No geofenced reminders to re-register")
            return Result.success()
        }

        val failedReminders = mutableListOf<Reminder>()

        for (reminder in geofencedReminders) {
            val trigger = reminder.locationTrigger
            if (trigger?.latitude == null || trigger.longitude == null) {
                Log.w(TAG, "Skipping reminder ${reminder.id}: missing coordinates")
                continue
            }

            val result = geofenceManager.registerGeofence(reminder)
            if (result.isSuccess) {
                Log.d(TAG, "Re-registered geofence for reminder ${reminder.id}")
            } else {
                failedReminders.add(reminder)
                Log.e(TAG, "Failed to re-register geofence for ${reminder.id}: ${result.exceptionOrNull()?.message}")
            }
        }

        val successCount = geofencedReminders.size - failedReminders.size
        Log.i(TAG, "Re-registration complete: $successCount success, ${failedReminders.size} failed")

        return if (failedReminders.isEmpty()) {
            Result.success()
        } else {
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "GeofenceReregister"
        const val WORK_NAME = "geofence_reregistration"
    }
}
