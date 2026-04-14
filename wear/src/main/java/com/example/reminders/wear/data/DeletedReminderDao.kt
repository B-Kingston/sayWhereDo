package com.example.reminders.wear.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * Data-access object for the [DeletedReminder] tombstone table.
 *
 * Each method corresponds to a sync-related operation: inserting a new
 * tombstone when the user deletes a reminder, checking whether a reminder
 * has already been deleted (preventing re-insertion during sync), and
 * purging acknowledged tombstones that are older than a given cutoff.
 */
@Dao
interface DeletedReminderDao {

    /**
     * Persists a new tombstone record. Replaces any existing row with the same
     * [DeletedReminder.id] so that a repeated deletion is idempotent.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: DeletedReminder)

    /**
     * Returns the tombstone for the given reminder id, or `null` if the
     * reminder has not been locally deleted.
     */
    @Query("SELECT * FROM deleted_reminders WHERE id = :id")
    suspend fun getById(id: String): DeletedReminder?

    /**
     * Emits the full list of tombstones, ordered by deletion time
     * (newest first). The phone consumes this flow during sync to discover
     * which reminders it should also delete.
     */
    @Query("SELECT * FROM deleted_reminders ORDER BY deletedAt DESC")
    fun getAll(): Flow<List<DeletedReminder>>

    /**
     * Removes a single tombstone after the phone has acknowledged the
     * deletion during sync.
     */
    @Query("DELETE FROM deleted_reminders WHERE id = :id")
    suspend fun deleteById(id: String)

    /**
     * Bulk-purges tombstones older than [cutoff]. Returns the number of
     * rows removed so the caller can log or verify the cleanup.
     */
    @Query("DELETE FROM deleted_reminders WHERE deletedAt < :cutoff")
    suspend fun deleteOlderThan(cutoff: Instant): Int

    /**
     * Quick existence check used during inbound sync to decide whether a
     * reminder pulled from the phone should be silently skipped because the
     * user already deleted it on the watch.
     */
    @Query("SELECT EXISTS(SELECT 1 FROM deleted_reminders WHERE id = :id)")
    suspend fun exists(id: String): Boolean
}
