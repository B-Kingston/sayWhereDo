package com.example.reminders.data.repository

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
}
