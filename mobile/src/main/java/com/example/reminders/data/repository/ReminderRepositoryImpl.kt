package com.example.reminders.data.repository

import com.example.reminders.data.local.ReminderDao
import com.example.reminders.data.model.Reminder
import kotlinx.coroutines.flow.Flow

class ReminderRepositoryImpl(
    private val reminderDao: ReminderDao
) : ReminderRepository {

    override fun getAllReminders(): Flow<List<Reminder>> = reminderDao.getAll()

    override fun getActiveReminders(): Flow<List<Reminder>> = reminderDao.getActive()

    override fun getCompletedReminders(): Flow<List<Reminder>> = reminderDao.getCompleted()

    override suspend fun getReminderById(id: String): Reminder? = reminderDao.getById(id)

    override fun observeReminderById(id: String): Flow<Reminder?> = reminderDao.observeById(id)

    override suspend fun insert(reminder: Reminder) = reminderDao.insert(reminder)

    override suspend fun update(reminder: Reminder) = reminderDao.update(reminder)

    override suspend fun delete(reminder: Reminder) = reminderDao.delete(reminder)

    override suspend fun deleteById(id: String) = reminderDao.deleteById(id)

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
        val all = reminderDao.getGeofencedRemindersOnce()
        return all.firstOrNull { reminder ->
            reminder.locationTrigger?.geofenceId == geofenceId
        } ?: reminderDao.getById(geofenceId)
    }

    override suspend fun getTimedRemindersOnce(): List<Reminder> =
        reminderDao.getTimedRemindersOnce()
}
