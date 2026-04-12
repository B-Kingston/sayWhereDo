package com.example.reminders.wear.alarm

import android.util.Log
import com.example.reminders.wear.data.WatchReminderDao
import com.example.reminders.wear.geofence.WatchGeofenceManager
import com.example.reminders.wear.geofence.WatchLocationTrigger
import com.example.reminders.wear.notification.WatchNotificationManager
import kotlinx.serialization.json.Json
import java.time.Instant

class WatchReminderCompletionManager(
    private val watchReminderDao: WatchReminderDao,
    private val watchGeofenceManager: WatchGeofenceManager,
    private val geofencePendingIntent: android.app.PendingIntent,
    private val alarmScheduler: WatchAlarmScheduler,
    private val notificationManager: WatchNotificationManager
) {

    suspend fun completeReminder(reminderId: String): Result<Unit> {
        val reminder = watchReminderDao.getById(reminderId)
            ?: return Result.failure(IllegalArgumentException("Reminder not found: $reminderId"))

        if (reminder.isCompleted) {
            Log.d(TAG, "Reminder $reminderId already completed")
            return Result.success(Unit)
        }

        cleanupGeofence(reminder)
        alarmScheduler.cancelAlarm(reminder.id)
        notificationManager.cancelNotification(reminder.id)

        val completed = reminder.copy(
            isCompleted = true,
            locationState = if (reminder.locationState != null) "COMPLETED" else reminder.locationState,
            updatedAt = Instant.now()
        )
        watchReminderDao.update(completed)

        Log.i(TAG, "Completed reminder $reminderId")
        return Result.success(Unit)
    }

    suspend fun deleteReminder(reminderId: String): Result<Unit> {
        val reminder = watchReminderDao.getById(reminderId)
            ?: return Result.failure(IllegalArgumentException("Reminder not found: $reminderId"))

        cleanupGeofence(reminder)
        alarmScheduler.cancelAlarm(reminder.id)
        notificationManager.cancelNotification(reminder.id)

        watchReminderDao.deleteById(reminderId)

        Log.i(TAG, "Deleted reminder $reminderId")
        return Result.success(Unit)
    }

    private suspend fun cleanupGeofence(reminder: com.example.reminders.wear.data.WatchReminder) {
        val trigger = reminder.locationTriggerJson?.let { parseLocationTrigger(it) } ?: return
        val geofenceId = trigger.geofenceId ?: reminder.id

        val result = watchGeofenceManager.removeGeofence(geofenceId)
        if (result.isFailure) {
            Log.e(
                TAG,
                "Failed to remove geofence $geofenceId for reminder ${reminder.id}",
                result.exceptionOrNull()
            )
        }
    }

    private fun parseLocationTrigger(json: String): WatchLocationTrigger? {
        return try {
            Json.decodeFromString<WatchLocationTrigger>(json)
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        private const val TAG = "WatchCompletionManager"
    }
}
