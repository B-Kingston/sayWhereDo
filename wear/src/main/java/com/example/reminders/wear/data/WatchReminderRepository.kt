package com.example.reminders.wear.data

import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Central repository for all watch-side reminder data access.
 *
 * Wraps both the active-reminder DAO ([WatchReminderDao]) and the
 * tombstone DAO ([DeletedReminderDao]) so that callers never touch
 * Room directly. Every tombstone operation (soft-delete, restore,
 * existence check, expiry cleanup) flows through here.
 *
 * @param reminderDao   DAO for the `watch_reminders` table.
 * @param deletedDao    DAO for the `deleted_reminders` tombstone table.
 */
class WatchReminderRepository(
    private val reminderDao: WatchReminderDao,
    private val deletedDao: DeletedReminderDao
) {

    // ── Active reminders ────────────────────────────────────────

    fun getAllReminders(): Flow<List<WatchReminder>> = reminderDao.getAll()

    fun getActiveReminders(): Flow<List<WatchReminder>> = reminderDao.getActive()

    suspend fun getById(id: String): WatchReminder? = reminderDao.getById(id)

    suspend fun insert(reminder: WatchReminder) = reminderDao.insert(reminder)

    suspend fun update(reminder: WatchReminder) = reminderDao.update(reminder)

    suspend fun deleteById(id: String) = reminderDao.deleteById(id)

    suspend fun getAllGeofencedOnce(): List<WatchReminder> = reminderDao.getAllGeofencedOnce()

    suspend fun getUpcomingFrom(fromMillis: Long): Flow<List<WatchReminder>> =
        reminderDao.getUpcomingFrom(fromMillis)

    suspend fun getTimedRemindersOnce(): List<WatchReminder> = reminderDao.getTimedRemindersOnce()

    // ── Tombstone / trash operations ────────────────────────────

    /**
     * Emits the current list of soft-deleted reminders (trash), ordered
     * newest-first. The UI can display these in a "recently deleted" screen.
     */
    fun getDeletedReminders(): Flow<List<DeletedReminder>> = deletedDao.getAll()

    /**
     * Restores a reminder from the trash by removing its tombstone record.
     * The caller is responsible for re-inserting the [WatchReminder] row
     * before or after calling this method.
     */
    suspend fun restoreDeletedReminder(id: String) {
        deletedDao.deleteById(id)
    }

    /**
     * Moves a reminder into the trash (soft-delete).
     *
     * 1. Looks up the active [WatchReminder] by [reminderId].
     * 2. Creates a [DeletedReminder] tombstone capturing the original title,
     *    the [deletedBy] source, and timestamps.
     * 3. Persists the tombstone and removes the active row.
     *
     * If the reminder does not exist this is a no-op.
     */
    suspend fun moveReminderToTombstone(reminderId: String, deletedBy: String) {
        val reminder = reminderDao.getById(reminderId) ?: return
        val tombstone = DeletedReminder(
            id = reminder.id,
            originalTitle = reminder.title,
            deletedAt = Instant.now(),
            deletedBy = deletedBy,
            originalUpdatedAt = reminder.updatedAt
        )
        deletedDao.insert(tombstone)
        reminderDao.deleteById(reminderId)
    }

    /**
     * Purges tombstone records that are older than [TOMBSTONE_EXPIRY_DAYS].
     * Run this periodically (e.g. on app start or via a WorkManager task)
     * to keep the tombstone table from growing unbounded.
     *
     * @return the number of tombstones removed.
     */
    suspend fun cleanExpiredTombstones(): Int {
        val cutoff = Instant.now().minus(TOMBSTONE_EXPIRY_DAYS, ChronoUnit.DAYS)
        return deletedDao.deleteOlderThan(cutoff)
    }

    /**
     * Persists a remote tombstone directly during sync reconciliation.
     * Unlike [moveReminderToTombstone], this does not look up or delete an
     * active reminder row — it simply records that the remote peer deleted
     * the reminder so that future sync cycles honour the deletion.
     */
    suspend fun insertTombstone(tombstone: DeletedReminder) {
        deletedDao.insert(tombstone)
    }

    /**
     * Quick check whether a reminder id exists in the trash.
     * Used during inbound sync to skip reminders the user already deleted.
     */
    suspend fun reminderExistsInTrash(id: String): Boolean = deletedDao.exists(id)

    companion object {
        /** Number of days a tombstone is kept before automatic purge. */
        private const val TOMBSTONE_EXPIRY_DAYS = 30L
    }
}
