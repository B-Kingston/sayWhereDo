package com.example.reminders.wear.sync

import com.example.reminders.wear.data.DeletedReminder
import com.example.reminders.wear.data.WatchReminder
import android.util.Log

/**
 * Result of a sync reconciliation pass.
 *
 * @property remindersToInsert  Remote reminders absent from the local device that
 *                               should be inserted into Room.
 * @property remindersToUpdate  Remote reminders whose `updatedAt` is newer than the
 *                               local copy and should overwrite it.
 * @property reminderIdsToDelete IDs of local active reminders that were tombstoned
 *                                on the remote peer and must be removed.
 * @property tombstonesToInsert  Remote tombstones that are new to this device and
 *                                must be persisted so the deletion is not lost.
 */
data class SyncResult(
    val remindersToInsert: List<WatchReminder>,
    val remindersToUpdate: List<WatchReminder>,
    val reminderIdsToDelete: List<String>,
    val tombstonesToInsert: List<DeletedReminder>,
    val tombstoneIdsToRemove: List<String>
)

/**
 * Stateless engine that reconciles local and remote reminder state during a
 * Wearable Data Layer sync cycle.
 *
 * The algorithm walks every remote active reminder and remote tombstone, comparing
 * them against the local snapshot to produce a [SyncResult] that describes the
 * exact mutations the caller should apply to the local database.
 *
 * ### Reconciliation rules
 *
 * | Remote state     | Local state     | Decision                                                              |
 * |------------------|-----------------|-----------------------------------------------------------------------|
 * | active           | missing         | Insert remote reminder.                                               |
 * | active           | active          | Compare `updatedAt`; newer wins. On tie, [SyncConflictResolver] picks.|
 * | active           | tombstoned      | If `tombstone.originalUpdatedAt > remote.updatedAt` the deletion wins;|
 * |                  |                 | otherwise the remote update supersedes and the reminder is re-inserted.|
 * | tombstone        | active          | If `tombstone.originalUpdatedAt >= local.updatedAt`, honour deletion. |
 * | tombstone        | absent/known    | Persist the tombstone if it is not already known locally.             |
 */
object SyncEngine {

    /**
     * Compares local and remote snapshots and produces a [SyncResult] describing
     * the mutations that should be applied to the local database.
     *
     * @param localActive       Currently active reminders stored on this device.
     * @param localTombstones   Tombstones previously recorded on this device.
     * @param remoteActive      Active reminders sent by the remote peer.
     * @param remoteTombstones  Tombstones sent by the remote peer.
     * @param localDeviceId     Stable identifier of this device, forwarded to
     *                          [SyncConflictResolver] for deterministic tie-breaking.
     * @return A [SyncResult] containing every insert, update, and delete to apply.
     */
    fun reconcile(
        localActive: List<WatchReminder>,
        localTombstones: List<DeletedReminder>,
        remoteActive: List<WatchReminder>,
        remoteTombstones: List<DeletedReminder>,
        localDeviceId: String
    ): SyncResult {
        val localById = localActive.associateBy { it.id }
        val localTombstonesById = localTombstones.associateBy { it.id }

        val toInsert = mutableListOf<WatchReminder>()
        val toUpdate = mutableListOf<WatchReminder>()
        val toDelete = mutableListOf<String>()
        val tombstonesToAdd = mutableListOf<DeletedReminder>()
        val tombstonesToRemove = mutableListOf<String>()

        resolveRemoteActive(
            remoteActive = remoteActive,
            localById = localById,
            localTombstonesById = localTombstonesById,
            localDeviceId = localDeviceId,
            toInsert = toInsert,
            toUpdate = toUpdate,
            tombstonesToRemove = tombstonesToRemove
        )

        resolveRemoteTombstones(
            remoteTombstones = remoteTombstones,
            localById = localById,
            localTombstonesById = localTombstonesById,
            toDelete = toDelete,
            tombstonesToAdd = tombstonesToAdd
        )

        Log.d(
            TAG,
            "Reconciliation: ${toInsert.size} inserts, ${toUpdate.size} updates, " +
                "${toDelete.size} deletes, ${tombstonesToAdd.size} tombstones"
        )

        return SyncResult(
            remindersToInsert = toInsert.toList(),
            remindersToUpdate = toUpdate.toList(),
            reminderIdsToDelete = toDelete.toList(),
            tombstonesToInsert = tombstonesToAdd.toList(),
            tombstoneIdsToRemove = tombstonesToRemove.toList()
        )
    }

    /**
     * Walks every remote active reminder and decides whether it should be inserted
     * or updated locally, respecting tombstone precedence.
     */
    private fun resolveRemoteActive(
        remoteActive: List<WatchReminder>,
        localById: Map<String, WatchReminder>,
        localTombstonesById: Map<String, DeletedReminder>,
        localDeviceId: String,
        toInsert: MutableList<WatchReminder>,
        toUpdate: MutableList<WatchReminder>,
        tombstonesToRemove: MutableList<String>
    ) {
        for (remote in remoteActive) {
            val local = localById[remote.id]
            val localTombstone = localTombstonesById[remote.id]

            when {
                local == null && localTombstone == null -> {
                    Log.d(TAG, "New remote ${remote.id} → insert")
                    toInsert.add(remote)
                }

                local != null -> {
                    Log.d(TAG, "Active conflict ${remote.id}: remote=${remote.updatedAt} vs local=${local.updatedAt}")
                    resolveActiveConflict(local = local, remote = remote, toUpdate = toUpdate)
                }

                localTombstone != null -> {
                    if (localTombstone.originalUpdatedAt <= remote.updatedAt) {
                        Log.d(TAG, "Edit-vs-delete ${remote.id}: tombstone <= remote → restore")
                        toInsert.add(remote)
                        tombstonesToRemove.add(remote.id)
                    } else {
                        Log.d(TAG, "Edit-vs-delete ${remote.id}: tombstone > remote → honour deletion")
                    }
                }
            }
        }
    }

    /**
     * Compares two active versions of the same reminder. The version with the
     * newer `updatedAt` wins. On an exact tie, delegates to
     * [SyncConflictResolver.resolve] for deterministic lexicographic tie-breaking.
     */
    private fun resolveActiveConflict(
        local: WatchReminder,
        remote: WatchReminder,
        toUpdate: MutableList<WatchReminder>
    ) {
        when {
            remote.updatedAt.isAfter(local.updatedAt) -> toUpdate.add(remote)
            remote.updatedAt == local.updatedAt -> {
                val winner = SyncConflictResolver.resolve(local, remote)
                if (winner !== local) {
                    toUpdate.add(remote)
                }
            }
        }
    }

    /**
     * Walks every remote tombstone. If the local device still holds an active copy
     * of the reminder and the tombstone records an `originalUpdatedAt` at least as
     * recent as the local `updatedAt`, the deletion is honoured.
     *
     * Remote tombstones that are not already known locally are also collected so the
     * caller can persist them.
     */
    private fun resolveRemoteTombstones(
        remoteTombstones: List<DeletedReminder>,
        localById: Map<String, WatchReminder>,
        localTombstonesById: Map<String, DeletedReminder>,
        toDelete: MutableList<String>,
        tombstonesToAdd: MutableList<DeletedReminder>
    ) {
        for (remoteTombstone in remoteTombstones) {
            val local = localById[remoteTombstone.id]
            val alreadyKnown = localTombstonesById.containsKey(remoteTombstone.id)

            if (!alreadyKnown) {
                Log.d(TAG, "Remote tombstone ${remoteTombstone.id}: unknown → persist")
                tombstonesToAdd.add(remoteTombstone)
            }

            if (local != null && remoteTombstone.originalUpdatedAt >= local.updatedAt) {
                Log.d(TAG, "Remote tombstone ${remoteTombstone.id}: local active, tombstone >= local → delete")
                toDelete.add(remoteTombstone.id)
            }
        }
    }

    private const val TAG = "SyncEngine"
}
