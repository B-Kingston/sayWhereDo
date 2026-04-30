package com.example.reminders.data.repository

import com.example.reminders.data.model.DeletedReminder
import com.example.reminders.data.model.Reminder
import kotlinx.coroutines.flow.Flow

interface ReminderRepository {
    fun getAllReminders(): Flow<List<Reminder>>
    fun getActiveReminders(): Flow<List<Reminder>>
    fun getCompletedReminders(): Flow<List<Reminder>>
    suspend fun getReminderById(id: String): Reminder?
    fun observeReminderById(id: String): Flow<Reminder?>
    suspend fun insert(reminder: Reminder)
    suspend fun update(reminder: Reminder)
    suspend fun delete(reminder: Reminder)
    suspend fun deleteById(id: String)
    suspend fun getActiveGeofenceCount(): Int
    suspend fun getGeofencedRemindersOnce(): List<Reminder>
    suspend fun getGeofencedRemindersByDevice(device: String): List<Reminder>
    suspend fun getByGeofenceId(geofenceId: String): Reminder?
    suspend fun getTimedRemindersOnce(): List<Reminder>

    /** Observes all soft-deleted reminders (tombstones), most-recently-deleted first. */
    fun getDeletedReminders(): Flow<List<DeletedReminder>>

    /** Removes the tombstone for [id], allowing the reminder to be re-synced. */
    suspend fun restoreDeletedReminder(id: String)

    /**
     * Moves the reminder identified by [reminderId] into the tombstone table
     * and deletes it from the active reminders table.
     *
     * @param deletedBy device identifier that performed the deletion (e.g. "mobile" or "watch").
     */
    suspend fun moveReminderToTombstone(reminderId: String, deletedBy: String)

    /** Permanently purges tombstones older than the configured retention window (7 days). */
    suspend fun cleanExpiredTombstones()

    /** Returns `true` when a tombstone exists for [id] (reminder is in the trash). */
    suspend fun reminderExistsInTrash(id: String): Boolean

    /** Inserts a tombstone directly (e.g. received from a peer device during sync). */
    suspend fun insertTombstone(tombstone: DeletedReminder)
}
