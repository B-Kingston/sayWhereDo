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
}
