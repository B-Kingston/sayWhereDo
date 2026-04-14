package com.example.reminders.wearable

import com.example.reminders.data.model.DeletedReminder
import com.example.reminders.data.model.Reminder

/**
 * Immutable result of a single sync reconciliation pass.
 *
 * The caller should apply these changes atomically in the following order
 * to avoid constraint violations:
 *
 * 1. Delete rows from the active reminders table whose IDs are in
 *    [reminderIdsToDelete].
 * 2. Insert rows into the `deleted_reminders` table for every entry in
 *    [tombstonesToInsert].
 * 3. Remove rows from the `deleted_reminders` table for every ID in
 *    [tombstoneIdsToRemove] (handles restores where a remote edit wins
 *    over a prior local deletion).
 * 4. Insert rows into the active reminders table for every entry in
 *    [remindersToInsert].
 * 5. Update rows in the active reminders table for every entry in
 *    [remindersToUpdate].
 */
data class SyncResult(
    val remindersToInsert: List<Reminder>,
    val remindersToUpdate: List<Reminder>,
    val reminderIdsToDelete: List<String>,
    val tombstonesToInsert: List<DeletedReminder>,
    val tombstoneIdsToRemove: List<String>
)

/**
 * Reconciles local and remote reminder state, producing the set of
 * changes that should be applied locally.
 *
 * Implements the tombstone-based bidirectional sync algorithm described in
 * the Sync Architecture Spec (sections 3.2–3.3). Rules in summary:
 *
 * | Scenario | Resolution |
 * |----------|------------|
 * | Remote reminder not present locally | Insert locally |
 * | Both sides active, remote is newer | Update local with remote copy |
 * | Both sides active, local is newer | No-op (local wins) |
 * | Local tombstone, remote edit, tombstone newer | Mark remote for deletion |
 * | Local tombstone, remote edit, remote newer | Restore (remove tombstone, insert remote) |
 * | Remote tombstone, local active, tombstone ≥ local | Delete locally |
 * | Remote tombstone, local active, local newer | No-op (local edit wins) |
 * | Remote tombstone, no local trace | Store tombstone for future reference |
 * | Completed reminders | Sync as updates, never deletions |
 *
 * @param localActive      Active reminders on this device.
 * @param localTombstones  Soft-deleted reminders on this device.
 * @param remoteActive     Active reminders received from the peer device.
 * @param remoteTombstones Soft-deleted reminders received from the peer device.
 * @param localDeviceId    Identifier of this device, used for logging and
 *                         filtering (does not overwrite `lastModifiedBy` on
 *                         accepted remote changes).
 */
class SyncEngine {

    /**
     * Runs the full reconciliation algorithm against the supplied state
     * snapshots and returns a [SyncResult] describing what the caller must
     * apply locally.
     */
    fun reconcile(
        localActive: List<Reminder>,
        localTombstones: List<DeletedReminder>,
        remoteActive: List<Reminder>,
        remoteTombstones: List<DeletedReminder>,
        localDeviceId: String
    ): SyncResult {
        val localActiveById = localActive.associateBy { it.id }
        val localTombstonesById = localTombstones.associateBy { it.id }

        val remindersToInsert = mutableListOf<Reminder>()
        val remindersToUpdate = mutableListOf<Reminder>()
        val reminderIdsToDelete = mutableListOf<String>()
        val tombstonesToInsert = mutableListOf<DeletedReminder>()
        val tombstoneIdsToRemove = mutableListOf<String>()

        // ---- Phase 1: Process every remote active reminder ----
        for (remote in remoteActive) {
            val local = localActiveById[remote.id]
            val localTombstone = localTombstonesById[remote.id]

            when {
                local == null && localTombstone == null -> {
                    // Entirely new to this device — insert.
                    remindersToInsert.add(remote)
                }

                local == null && localTombstone != null -> {
                    // Edit-vs-delete conflict: local deleted, remote edited.
                    if (localTombstone.originalUpdatedAt > remote.updatedAt) {
                        // Local deletion is newer — honour it, mark remote for deletion.
                        reminderIdsToDelete.add(remote.id)
                    } else {
                        // Remote edit is newer — restore, remove the local tombstone.
                        remindersToInsert.add(remote)
                        tombstoneIdsToRemove.add(remote.id)
                    }
                }

                local != null -> {
                    // Both sides have the reminder active — newer updatedAt wins.
                    if (!remote.updatedAt.isBefore(local.updatedAt)) {
                        remindersToUpdate.add(remote)
                    }
                }
            }
        }

        // ---- Phase 2: Process every remote tombstone ----
        for (remoteTomb in remoteTombstones) {
            val local = localActiveById[remoteTomb.id]

            if (local != null) {
                // Remote says deleted, local says active.
                if (!remoteTomb.originalUpdatedAt.isBefore(local.updatedAt)) {
                    // Remote deletion is at least as recent — accept deletion locally.
                    reminderIdsToDelete.add(remoteTomb.id)
                    tombstonesToInsert.add(remoteTomb)
                }
                // else: local edit is newer — ignore the stale remote tombstone.
            } else {
                // Reminder is not active locally.
                val alreadyTombstonedLocally = localTombstonesById.containsKey(remoteTomb.id)
                if (!alreadyTombstonedLocally) {
                    // No local record at all — store tombstone for future reference.
                    tombstonesToInsert.add(remoteTomb)
                }
            }
        }

        return SyncResult(
            remindersToInsert = remindersToInsert,
            remindersToUpdate = remindersToUpdate,
            reminderIdsToDelete = reminderIdsToDelete.distinct(),
            tombstonesToInsert = tombstonesToInsert,
            tombstoneIdsToRemove = tombstoneIdsToRemove.distinct()
        )
    }
}
