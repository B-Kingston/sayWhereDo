package com.example.reminders.alarm

import android.util.Log
import com.example.reminders.data.model.LocationReminderState
import com.example.reminders.data.model.Reminder
import com.example.reminders.data.repository.ReminderRepository
import com.example.reminders.geofence.GeofenceManager
import com.example.reminders.sync.ReminderSyncClient
import java.time.Instant

/**
 * Orchestrates the full completion and deletion flows for reminders.
 *
 * Both flows clean up **all** associated resources before modifying
 * Room state:
 *
 * **Completion flow:**
 * 1. Remove geofence (if location-based)
 * 2. Cancel alarm (if time-based)
 * 3. Mark completed in Room
 * 4. Sync to connected devices
 *
 * **Deletion flow:**
 * 1. Remove geofence (if location-based)
 * 2. Cancel alarm (if time-based)
 * 3. Delete from Room
 * 4. Sync to connected devices
 *
 * Cleanup is best-effort: geofence or alarm removal failures are
 * logged but do not prevent the Room update. The reminder is still
 * marked as completed or deleted to avoid leaving it in a stuck state.
 */
class ReminderCompletionManager(
    private val reminderRepository: ReminderRepository,
    private val geofenceManager: GeofenceManager,
    private val alarmScheduler: AlarmScheduler,
    private val syncClient: ReminderSyncClient
) {

    /**
     * Marks the reminder as completed and cleans up all associated resources.
     *
     * @param reminderId The ID of the reminder to complete.
     * @return [Result.success] if the Room update succeeded, or a failure.
     */
    suspend fun completeReminder(reminderId: String): Result<Unit> {
        val reminder = reminderRepository.getReminderById(reminderId)
            ?: return Result.failure(IllegalArgumentException("Reminder not found: $reminderId"))

        if (reminder.isCompleted) {
            Log.d(TAG, "Reminder $reminderId already completed")
            return Result.success(Unit)
        }

        // Clean up resources before updating Room
        cleanupGeofence(reminder)
        alarmScheduler.cancelAlarm(reminder.id)

        val completed = reminder.copy(
            isCompleted = true,
            locationState = if (reminder.locationState != null) {
                LocationReminderState.COMPLETED
            } else {
                reminder.locationState
            },
            updatedAt = Instant.now()
        )

        reminderRepository.update(completed)
        syncClient.syncReminderUpdate(reminderId)

        Log.i(TAG, "Completed reminder $reminderId")
        return Result.success(Unit)
    }

    /**
     * Deletes the reminder and cleans up all associated resources.
     *
     * @param reminderId The ID of the reminder to delete.
     * @return [Result.success] if the Room delete succeeded, or a failure.
     */
    suspend fun deleteReminder(reminderId: String): Result<Unit> {
        val reminder = reminderRepository.getReminderById(reminderId)
            ?: return Result.failure(IllegalArgumentException("Reminder not found: $reminderId"))

        // Clean up resources before deleting from Room
        cleanupGeofence(reminder)
        alarmScheduler.cancelAlarm(reminder.id)

        reminderRepository.deleteById(reminderId)
        syncClient.syncReminderDeletion(reminderId)

        Log.i(TAG, "Deleted reminder $reminderId")
        return Result.success(Unit)
    }

    /**
     * Removes the geofence associated with [reminder], if any.
     *
     * Failures are logged but not propagated — the reminder should
     * still be completable/deletable even if geofence removal fails.
     */
    private suspend fun cleanupGeofence(reminder: Reminder) {
        val geofenceId = reminder.locationTrigger?.geofenceId ?: return

        val result = geofenceManager.removeGeofence(geofenceId)
        if (result.isFailure) {
            Log.e(
                TAG,
                "Failed to remove geofence $geofenceId for reminder ${reminder.id}",
                result.exceptionOrNull()
            )
        }
    }

    companion object {
        private const val TAG = "CompletionManager"
    }
}
