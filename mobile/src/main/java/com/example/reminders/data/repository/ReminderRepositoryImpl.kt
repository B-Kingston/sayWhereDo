package com.example.reminders.data.repository

import android.util.Log
import com.example.reminders.data.local.DeletedReminderDao
import com.example.reminders.data.local.ReminderDao
import com.example.reminders.data.model.DeletedReminder
import com.example.reminders.data.model.Reminder
import kotlinx.coroutines.flow.Flow
import java.time.Instant

class ReminderRepositoryImpl(
    private val reminderDao: ReminderDao,
    private val deletedReminderDao: DeletedReminderDao
) : ReminderRepository {

    override fun getAllReminders(): Flow<List<Reminder>> = reminderDao.getAll()

    override fun getActiveReminders(): Flow<List<Reminder>> = reminderDao.getActive()

    override fun getCompletedReminders(): Flow<List<Reminder>> = reminderDao.getCompleted()

    override suspend fun getReminderById(id: String): Reminder? {
        val reminder = reminderDao.getById(id)
        Log.d(TAG, "getReminderById($id): ${if (reminder != null) "found" else "not found"}")
        return reminder
    }

    override fun observeReminderById(id: String): Flow<Reminder?> = reminderDao.observeById(id)

    override suspend fun insert(reminder: Reminder) {
        Log.d(TAG, "insert: ${reminder.id} — ${reminder.title.take(50)}")
        reminderDao.insert(reminder)
    }

    override suspend fun update(reminder: Reminder) {
        Log.d(TAG, "update: ${reminder.id}")
        reminderDao.update(reminder)
    }

    override suspend fun delete(reminder: Reminder) {
        Log.d(TAG, "delete: ${reminder.id}")
        reminderDao.delete(reminder)
    }

    override suspend fun deleteById(id: String) {
        Log.d(TAG, "deleteById: $id")
        reminderDao.deleteById(id)
    }

    override suspend fun getActiveGeofenceCount(): Int = reminderDao.getActiveGeofenceCount()

    override suspend fun getGeofencedRemindersOnce(): List<Reminder> =
        reminderDao.getGeofencedRemindersOnce()

    override suspend fun getGeofencedRemindersByDevice(device: String): List<Reminder> =
        reminderDao.getGeofencedRemindersByDevice(device)

    /**
     * Finds a reminder whose [LocationTrigger.geofenceId] matches, or whose
     * [Reminder.id] matches, the given [geofenceId].
     *
     * Since Room cannot query inside the JSON `locationTrigger` column natively,
     * we load all geofenced reminders and filter in memory. The dataset is
     * small (capped at 100 per device), making this acceptable.
     */
    override suspend fun getByGeofenceId(geofenceId: String): Reminder? {
        Log.d(TAG, "getByGeofenceId: $geofenceId")
        val all = reminderDao.getGeofencedRemindersOnce()
        return all.firstOrNull { reminder ->
            reminder.locationTrigger?.geofenceId == geofenceId
        } ?: reminderDao.getById(geofenceId)
    }

    override suspend fun getTimedRemindersOnce(): List<Reminder> =
        reminderDao.getTimedRemindersOnce()

    override fun getDeletedReminders(): Flow<List<DeletedReminder>> =
        deletedReminderDao.getAll()

    override suspend fun restoreDeletedReminder(id: String) {
        Log.d(TAG, "restoreDeletedReminder: $id")
        deletedReminderDao.deleteById(id)
    }

    /**
     * Soft-deletes a reminder by moving it into the tombstone table and
     * removing it from the active reminders table.
     */
    override suspend fun moveReminderToTombstone(reminderId: String, deletedBy: String) {
        Log.d(TAG, "moveReminderToTombstone: $reminderId (by $deletedBy)")
        val reminder = reminderDao.getById(reminderId)
        if (reminder == null) {
            Log.w(TAG, "moveReminderToTombstone: reminder $reminderId not found — skipping")
            return
        }
        val tombstone = DeletedReminder(
            id = reminder.id,
            originalTitle = reminder.title,
            deletedAt = Instant.now(),
            deletedBy = deletedBy,
            originalUpdatedAt = reminder.updatedAt
        )
        deletedReminderDao.insert(tombstone)
        reminderDao.deleteById(reminderId)
        Log.d(TAG, "moveReminderToTombstone: reminder $reminderId moved to tombstone")
    }

    /**
     * Permanently removes tombstones older than the configured retention window.
     * The DeletedReminder KDoc specifies a 7-day retention period.
     */
    override suspend fun cleanExpiredTombstones() {
        val cutoff = Instant.now().minus(TOMBSTONE_RETENTION)
        val purged = deletedReminderDao.deleteOlderThan(cutoff)
        Log.d(TAG, "cleanExpiredTombstones: purged $purged tombstones older than $cutoff")
    }

    override suspend fun reminderExistsInTrash(id: String): Boolean =
        deletedReminderDao.exists(id)

    override suspend fun insertTombstone(tombstone: DeletedReminder) {
        Log.d(TAG, "insertTombstone: ${tombstone.id}")
        deletedReminderDao.insert(tombstone)
    }

    companion object {
        private const val TAG = "ReminderRepository"

        /** Tombstone retention period — matches the 7-day window described in DeletedReminder docs. */
        private val TOMBSTONE_RETENTION: java.time.Duration = java.time.Duration.ofDays(7)
    }
}
