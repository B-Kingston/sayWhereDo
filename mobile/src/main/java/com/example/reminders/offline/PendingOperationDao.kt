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
}
