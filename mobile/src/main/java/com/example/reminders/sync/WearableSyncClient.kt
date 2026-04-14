package com.example.reminders.sync

import android.util.Log
import com.example.reminders.data.model.DeletedReminder
import com.example.reminders.data.repository.ReminderRepository
import com.example.reminders.wearable.WearableDataSender
import java.time.Instant

/**
 * [ReminderSyncClient] that propagates reminder state changes to connected
 * WearOS devices via the Wearable Data Layer.
 *
 * Update events (including completions) are sent as full-reminder syncs so
 * the watch can mirror the current state.  Deletions are tombstoned locally
 * via [ReminderRepository.moveReminderToTombstone] and the tombstone is
 * forwarded to every connected watch so it can reconcile its local database.
 */
class WearableSyncClient(
    private val wearableDataSender: WearableDataSender,
    private val reminderRepository: ReminderRepository
) : ReminderSyncClient {

    /**
     * Syncs the current state of a reminder to all connected watches.
     * Completions are treated as updates — the watch receives the full
     * reminder so it can display or archive it, rather than silently
     * deleting it.
     */
    override suspend fun syncReminderUpdate(reminderId: String) {
        val reminder = reminderRepository.getReminderById(reminderId)
        if (reminder == null) {
            Log.w(TAG, "Cannot sync update — reminder $reminderId not found")
            return
        }

        wearableDataSender.syncReminderToWatch(reminder)
    }

    /**
     * Soft-deletes a reminder by moving it to the tombstone table via
     * [ReminderRepository.moveReminderToTombstone], then forwards the
     * tombstone to every connected watch so it can remove its local copy
     * and avoid re-inserting the reminder during future syncs.
     */
    override suspend fun syncReminderDeletion(reminderId: String) {
        val reminder = reminderRepository.getReminderById(reminderId)
        if (reminder == null) {
            Log.w(TAG, "Cannot sync deletion — reminder $reminderId not found")
            return
        }

        reminderRepository.moveReminderToTombstone(reminderId, DELETED_BY_MOBILE)

        val tombstone = DeletedReminder(
            id = reminder.id,
            originalTitle = reminder.title,
            deletedAt = Instant.now(),
            deletedBy = DELETED_BY_MOBILE,
            originalUpdatedAt = reminder.updatedAt
        )

        val nodes = wearableDataSender.getConnectedWatchNodes()
        for (node in nodes) {
            wearableDataSender.sendTombstoneToWatch(tombstone, node.id)
        }
    }

    companion object {
        private const val TAG = "WearableSyncClient"
        private const val DELETED_BY_MOBILE = "mobile"
    }
}
