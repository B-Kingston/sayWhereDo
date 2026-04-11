package com.example.reminders.wear.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchReminderDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reminder: WatchReminder)

    @Update
    suspend fun update(reminder: WatchReminder)

    @Delete
    suspend fun delete(reminder: WatchReminder)

    @Query("DELETE FROM watch_reminders WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM watch_reminders WHERE id = :id")
    suspend fun getById(id: String): WatchReminder?

    @Query("SELECT * FROM watch_reminders ORDER BY createdAt DESC")
    fun getAll(): Flow<List<WatchReminder>>

    @Query("SELECT * FROM watch_reminders WHERE isCompleted = 0 ORDER BY createdAt DESC")
    fun getActive(): Flow<List<WatchReminder>>

    @Query("SELECT * FROM watch_reminders WHERE triggerTime BETWEEN :fromMillis AND :toMillis AND isCompleted = 0")
    fun getUpcoming(fromMillis: Long, toMillis: Long): Flow<List<WatchReminder>>

    @Query("SELECT * FROM watch_reminders WHERE locationTriggerJson IS NOT NULL AND locationState IN ('ACTIVE', 'TRIGGERED')")
    suspend fun getAllGeofencedOnce(): List<WatchReminder>

    @Query("SELECT COUNT(*) FROM watch_reminders WHERE isCompleted = 0 AND locationState IN ('ACTIVE', 'TRIGGERED')")
    suspend fun getActiveGeofenceCount(): Int

    @Query("SELECT * FROM watch_reminders WHERE triggerTime IS NOT NULL AND isCompleted = 0 AND triggerTime >= :fromMillis")
    fun getUpcomingFrom(fromMillis: Long): Flow<List<WatchReminder>>

    @Query("SELECT * FROM watch_reminders WHERE triggerTime IS NOT NULL AND isCompleted = 0")
    suspend fun getTimedRemindersOnce(): List<WatchReminder>
}
