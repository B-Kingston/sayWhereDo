package com.example.reminders.offline

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * Data access object for [PendingOperation] entities.
 *
 * Provides CRUD operations for the offline retry queue,
 * allowing the [OfflineQueueManager] to enqueue, query,
 * and remove pending formatting and geocoding operations.
 */
@Dao
interface PendingOperationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(operation: PendingOperation)

    @Query("SELECT * FROM pending_operations ORDER BY createdAt ASC")
    suspend fun getAll(): List<PendingOperation>

    @Query("SELECT * FROM pending_operations WHERE type = :type ORDER BY createdAt ASC")
    suspend fun getByType(type: String): List<PendingOperation>

    @Query("SELECT COUNT(*) FROM pending_operations")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM pending_operations WHERE type = :type")
    suspend fun countByType(type: String): Int

    @Delete
    suspend fun delete(operation: PendingOperation)

    @Query("DELETE FROM pending_operations WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM pending_operations")
    suspend fun deleteAll()

    @Query("UPDATE pending_operations SET retry_count = retry_count + 1 WHERE id = :id")
    suspend fun incrementRetryCount(id: String)

    @Query("SELECT EXISTS(SELECT 1 FROM pending_operations WHERE reminder_id = :reminderId AND type = :type)")
    suspend fun existsByReminderIdAndType(reminderId: String, type: String): Boolean
}
