package com.example.reminders.wear.service

import android.net.Uri
import android.util.Log
import com.example.reminders.wear.data.WatchReminder
import com.example.reminders.wear.di.WatchRemindersApplication
import com.example.reminders.wear.sync.DataLayerPaths
import com.example.reminders.wear.sync.ReminderSerializer
import com.example.reminders.wear.sync.SyncConflictResolver
import com.example.reminders.wear.sync.SyncEngine
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class DataLayerListenerService : WearableListenerService() {

    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        Log.d(TAG, "onDataChanged: ${dataEvents.count} events")

        for (event in dataEvents) {
            val path = event.dataItem.uri.path ?: continue
            Log.d(TAG, "Data event path: $path, type: ${event.type}")

            when {
                path.startsWith(DataLayerPaths.REMINDERS_PREFIX) -> {
                    when (event.type) {
                        DataEvent.TYPE_CHANGED -> {
                            val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                            val bytes = dataMap.getByteArray(KEY_REMINDER_DATA)
                            if (bytes != null) {
                                coroutineScope.launch {
                                    val remote = ReminderSerializer.deserialize(bytes)
                                    handleSingleReminderUpdate(remote)
                                }
                            }
                        }
                        DataEvent.TYPE_DELETED -> {
                            val id = path.removePrefix(DataLayerPaths.REMINDERS_PREFIX + "/")
                            if (id.isNotBlank()) {
                                coroutineScope.launch {
                                    val container = (applicationContext as WatchRemindersApplication).container
                                    container.watchReminderRepository.moveReminderToTombstone(
                                        reminderId = id,
                                        deletedBy = "phone"
                                    )
                                    Log.i(TAG, "Tombstoned deleted reminder from phone: $id")
                                }
                            }
                        }
                    }
                }
                path == DataLayerPaths.PRO_STATUS_PATH -> {
                    Log.d(TAG, "Pro status update received")
                }
            }
        }
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        Log.d(TAG, "onMessageReceived: ${messageEvent.path}")

        when (messageEvent.path) {
            "/reminders-sync" -> {
                coroutineScope.launch {
                    val reminders = ReminderSerializer.deserializeList(messageEvent.data)
                    handleFullSync(reminders)
                }
            }
            "/reminder-update" -> {
                coroutineScope.launch {
                    val reminder = ReminderSerializer.deserialize(messageEvent.data)
                    handleSingleReminderUpdate(reminder)
                }
            }
            DataLayerPaths.SYNC_STATE_RESPONSE -> {
                coroutineScope.launch {
                    handleSyncStateResponse(messageEvent.data)
                }
            }
            DataLayerPaths.SYNC_TOMBSTONE -> {
                coroutineScope.launch {
                    handleTombstone(messageEvent.data)
                }
            }
        }
    }

    private suspend fun handleSingleReminderUpdate(remote: WatchReminder) {
        val container = (applicationContext as WatchRemindersApplication).container
        val repository = container.watchReminderRepository

        val local = repository.getById(remote.id)
        if (local == null) {
            repository.insert(remote)
            Log.i(TAG, "Inserted new reminder from phone: ${remote.id}")
        } else {
            val resolved = SyncConflictResolver.resolve(local, remote)
            repository.insert(resolved)
            Log.i(TAG, "Upserted reminder from phone: ${remote.id}")
        }
    }

    private suspend fun handleFullSync(reminders: List<WatchReminder>) {
        val container = (applicationContext as WatchRemindersApplication).container
        val repository = container.watchReminderRepository

        val localReminders = repository.getActiveReminders().first()
        val merged = SyncConflictResolver.mergeLists(localReminders, reminders)

        for (reminder in merged) {
            repository.insert(reminder)
        }
        Log.i(TAG, "Full sync complete: ${merged.size} reminders merged")
    }

    /**
     * Handles an inbound [SyncStateDto] from the phone by reconciling remote
     * state against the local database, applying all inserts / updates /
     * deletes, persisting remote tombstones, and then sending the watch's
     * updated state back to the phone via [sendSyncStateComplete].
     */
    private suspend fun handleSyncStateResponse(data: ByteArray) {
        val container = (applicationContext as WatchRemindersApplication).container
        val repository = container.watchReminderRepository

        val remoteState = ReminderSerializer.deserializeSyncState(data)

        val localActive = repository.getActiveReminders().first()
        val localTombstones = repository.getDeletedReminders().first()

        val remoteActive = remoteState.activeReminders.map { ReminderSerializer.toWatchReminder(it) }
        val remoteTombstones = remoteState.tombstones.map { ReminderSerializer.toDeletedReminder(it) }

        val result = SyncEngine.reconcile(
            localActive = localActive,
            localTombstones = localTombstones,
            remoteActive = remoteActive,
            remoteTombstones = remoteTombstones,
            localDeviceId = DEVICE_ID
        )

        for (reminder in result.remindersToInsert) {
            repository.insert(reminder)
        }
        for (reminder in result.remindersToUpdate) {
            repository.insert(reminder)
        }
        for (id in result.reminderIdsToDelete) {
            repository.deleteById(id)
        }
        for (tombstone in result.tombstonesToInsert) {
            repository.insertTombstone(tombstone)
        }

        val updatedActive = repository.getActiveReminders().first()
        val updatedTombstones = repository.getDeletedReminders().first()
        container.wearDataLayerClient.sendSyncStateComplete(updatedActive, updatedTombstones)

        Log.i(
            TAG,
            "Sync reconciliation complete: ${result.remindersToInsert.size} inserted, " +
                "${result.remindersToUpdate.size} updated, ${result.reminderIdsToDelete.size} deleted, " +
                "${result.tombstonesToInsert.size} tombstones added"
        )
    }

    /**
     * Handles a single tombstone notification from the phone. If the reminder
     * still exists locally as an active reminder it is moved to the tombstone
     * table so the deletion is not lost during future sync cycles.
     */
    private suspend fun handleTombstone(data: ByteArray) {
        val container = (applicationContext as WatchRemindersApplication).container
        val repository = container.watchReminderRepository

        val dto = ReminderSerializer.deserializeDeletedReminder(data)
        val localReminder = repository.getById(dto.id)
        if (localReminder != null) {
            repository.moveReminderToTombstone(dto.id, dto.deletedBy)
            Log.i(TAG, "Tombstoned reminder from phone: ${dto.id}")
        }
    }

    companion object {
        private const val TAG = "DataLayerListener"
        private const val KEY_REMINDER_DATA = "reminder_data"
        private const val DEVICE_ID = "watch"
    }
}
