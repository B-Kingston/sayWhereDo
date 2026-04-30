package com.example.reminders.wear.sync

import com.example.reminders.wear.data.WatchReminder
import android.util.Log

object SyncConflictResolver {

    fun resolve(local: WatchReminder, remote: WatchReminder): WatchReminder {
        if (local.updatedAt == remote.updatedAt) {
            val winner = if (local.id >= remote.id) local else remote
            Log.d(TAG, "Tie-break for ${local.id}: local=${local.id} vs remote=${remote.id} → ${if (winner === local) "local" else "remote"}")
            return winner
        }
        val winner = if (local.updatedAt.isAfter(remote.updatedAt)) local else remote
        Log.d(TAG, "Conflict for ${local.id}: local=${local.updatedAt} vs remote=${remote.updatedAt} → ${if (winner === local) "local" else "remote"}")
        return winner
    }

    fun mergeLists(local: List<WatchReminder>, remote: List<WatchReminder>): List<WatchReminder> {
        val localById = local.associateBy { it.id }
        val remoteById = remote.associateBy { it.id }
        val allIds = localById.keys + remoteById.keys

        return allIds.map { id ->
            val localReminder = localById[id]
            val remoteReminder = remoteById[id]
            when {
                localReminder == null -> remoteReminder!!
                remoteReminder == null -> localReminder
                else -> resolve(localReminder, remoteReminder)
            }
        }
    }

    private const val TAG = "SyncConflictResolver"
}
