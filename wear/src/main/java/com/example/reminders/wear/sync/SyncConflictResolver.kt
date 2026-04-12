package com.example.reminders.wear.sync

import com.example.reminders.wear.data.WatchReminder
import android.util.Log

object SyncConflictResolver {

    fun resolve(local: WatchReminder, remote: WatchReminder): WatchReminder {
        if (local.updatedAt == remote.updatedAt) {
            val winner = if (local.id >= remote.id) local else remote
            Log.d(TAG, "Tie-break for ${local.id}: using lexicographic ID comparison")
            return winner
        }
        return if (local.updatedAt.isAfter(remote.updatedAt)) local else remote
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
