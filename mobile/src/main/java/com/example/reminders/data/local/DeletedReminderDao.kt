package com.example.reminders.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.reminders.data.model.DeletedReminder
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * Data-access object for the `deleted_reminders` tombstone table.
 *
 * Supports the sync reconciliation layer (checking whether an incoming reminder
 * was previously deleted) and the Trash / Restore UI (listing and permanently
 * purging soft-deleted items).
 */
@Dao
interface DeletedReminderDao {

    /**
     * Inserts a tombstone row. Uses `REPLACE` so that a duplicate insert
     * (e.g. from a retry) silently overwrites the previous entry.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: DeletedReminder)

    /**
     * Returns the tombstone for the given [id], or `null` if the reminder
     * has no corresponding deletion record.
     */
    @Query("SELECT * FROM deleted_reminders WHERE id = :id")
    suspend fun getById(id: String): DeletedReminder?

    /**
     * Observes all tombstones, ordered most-recently-deleted first.
     * Drives the Trash screen UI.
     */
    @Query("SELECT * FROM deleted_reminders ORDER BY deletedAt DESC")
    fun getAll(): Flow<List<DeletedReminder>>

    /**
     * Permanently removes the tombstone for [id].
     * Called after a reminder is restored or when performing final cleanup.
     */
    @Query("DELETE FROM deleted_reminders WHERE id = :id")
    suspend fun deleteById(id: String)

    /**
     * Permanently removes all tombstones older than [cutoff].
     * Returns the number of rows deleted so the caller can log or assert.
     */
    @Query("DELETE FROM deleted_reminders WHERE deletedAt < :cutoff")
    suspend fun deleteOlderThan(cutoff: Instant): Int

    /**
     * Returns `true` when a tombstone exists for [id], meaning the reminder
     * was previously deleted and has not yet been restored or purged.
     * Used by the sync engine to skip re-insertion of deleted items.
     */
    @Query("SELECT EXISTS(SELECT 1 FROM deleted_reminders WHERE id = :id)")
    suspend fun exists(id: String): Boolean
}
