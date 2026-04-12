package com.example.reminders.wear.data

import kotlinx.coroutines.flow.Flow

class WatchReminderRepository(private val dao: WatchReminderDao) {

    fun getAllReminders(): Flow<List<WatchReminder>> = dao.getAll()

    fun getActiveReminders(): Flow<List<WatchReminder>> = dao.getActive()

    suspend fun getById(id: String): WatchReminder? = dao.getById(id)

    suspend fun insert(reminder: WatchReminder) = dao.insert(reminder)

    suspend fun update(reminder: WatchReminder) = dao.update(reminder)

    suspend fun deleteById(id: String) = dao.deleteById(id)

    suspend fun getAllGeofencedOnce(): List<WatchReminder> = dao.getAllGeofencedOnce()

    suspend fun getUpcomingFrom(fromMillis: Long): Flow<List<WatchReminder>> =
        dao.getUpcomingFrom(fromMillis)

    suspend fun getTimedRemindersOnce(): List<WatchReminder> = dao.getTimedRemindersOnce()
}
