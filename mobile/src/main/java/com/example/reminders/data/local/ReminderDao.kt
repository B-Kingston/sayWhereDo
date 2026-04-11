package com.example.reminders.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.reminders.data.model.Reminder
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reminder: Reminder)

    @Update
    suspend fun update(reminder: Reminder)

    @Delete
    suspend fun delete(reminder: Reminder)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getById(id: String): Reminder?

    @Query("SELECT * FROM reminders WHERE id = :id")
    fun observeById(id: String): Flow<Reminder?>

    @Query("SELECT * FROM reminders ORDER BY createdAt DESC")
    fun getAll(): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE isCompleted = 0 ORDER BY createdAt DESC")
    fun getActive(): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE isCompleted = 1 ORDER BY updatedAt DESC")
    fun getCompleted(): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE locationState = :state")
    fun getByLocationState(state: String): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE locationTrigger IS NOT NULL AND locationState IN ('ACTIVE', 'TRIGGERED')")
    fun getGeofencedReminders(): Flow<List<Reminder>>

    @Query("SELECT COUNT(*) FROM reminders WHERE isCompleted = 0 AND locationState IN ('ACTIVE', 'TRIGGERED')")
    suspend fun getActiveGeofenceCount(): Int

    @Query("SELECT * FROM reminders WHERE locationTrigger IS NOT NULL AND locationState IN ('ACTIVE', 'TRIGGERED')")
    suspend fun getGeofencedRemindersOnce(): List<Reminder>

    @Query("SELECT * FROM reminders WHERE locationTrigger IS NOT NULL AND locationState IN ('ACTIVE', 'TRIGGERED') AND geofencingDevice = :device")
    suspend fun getGeofencedRemindersByDevice(device: String): List<Reminder>

    @Query("SELECT * FROM reminders WHERE triggerTime IS NOT NULL AND isCompleted = 0")
    suspend fun getTimedRemindersOnce(): List<Reminder>
}
