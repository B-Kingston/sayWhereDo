package com.example.reminders.wear.data

import android.content.Context
import android.util.Log
import com.example.reminders.wear.sync.DataLayerPaths
import com.example.reminders.wear.sync.DeletedReminderDto
import com.example.reminders.wear.sync.ReminderDto
import com.example.reminders.wear.sync.ReminderSerializer
import com.example.reminders.wear.sync.SyncStateDto
import kotlinx.serialization.encodeToString
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.NodeClient
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class WearDataLayerClient(context: Context) {

    private val appContext = context.applicationContext
    private val dataClient: DataClient = Wearable.getDataClient(appContext)
    private val messageClient: MessageClient = Wearable.getMessageClient(appContext)
    private val nodeClient: NodeClient = Wearable.getNodeClient(appContext)
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    private val _isPhoneConnected = MutableStateFlow(false)
    val isPhoneConnected: Flow<Boolean> = _isPhoneConnected.asStateFlow()

    fun startMonitoring() {
        coroutineScope.launch {
            try {
                val capabilityInfo = Wearable.getCapabilityClient(appContext)
                    .getCapability(
                        DataLayerPaths.CAPABILITY_PHONE,
                        com.google.android.gms.wearable.CapabilityClient.FILTER_REACHABLE
                    )
                    .await()
                _isPhoneConnected.value = capabilityInfo.nodes.isNotEmpty()
                Log.i(TAG, "Initial phone connectivity: ${_isPhoneConnected.value}")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to check initial phone connectivity", e)
            }
        }

        Wearable.getCapabilityClient(appContext).addListener(
            { info ->
                val connected = info.nodes.isNotEmpty()
                Log.i(TAG, "Phone connectivity changed: $connected")
                _isPhoneConnected.value = connected
            },
            DataLayerPaths.CAPABILITY_PHONE
        )
    }

    suspend fun sendTranscriptToPhone(transcript: String, reminderId: String) {
        val nodeId = getPhoneNodeId()
        if (nodeId == null) {
            Log.w(TAG, "No phone connected, cannot send transcript")
            return
        }

        try {
            val payload = "$reminderId|$transcript".toByteArray(Charsets.UTF_8)
            messageClient.sendMessage(
                nodeId,
                DataLayerPaths.DEFERRED_FORMATTING_PATH,
                payload
            ).await()
            Log.i(TAG, "Transcript sent to phone for reminder $reminderId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send transcript to phone", e)
        }
    }

    suspend fun syncReminderToPhone(reminder: WatchReminder) {
        try {
            val dto = reminder.toReminderDto()
            val json = kotlinx.serialization.json.Json.encodeToString(
                com.example.reminders.wear.sync.ReminderDto.serializer(), dto
            )
            val path = DataLayerPaths.reminderPath(reminder.id)
            val putRequest = PutDataMapRequest.create(path).apply {
                dataMap.putString(KEY_REMINDER_JSON, json)
                dataMap.putLong(KEY_TIMESTAMP, System.currentTimeMillis())
            }
            dataClient.putDataItem(putRequest.asPutDataRequest().setUrgent()).await()
            Log.i(TAG, "Synced reminder ${reminder.id} to phone, payload=${json.toByteArray(Charsets.UTF_8).size} bytes")
            Log.d(TAG, "Reminder payload: ${json.take(200)}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync reminder ${reminder.id} to phone", e)
        }
    }

    suspend fun sendDeferredFormattingRequests(reminders: List<WatchReminder>) {
        val pendingReminders = reminders.filter {
            it.formattingProvider == FORMATTING_PENDING
        }
        if (pendingReminders.isEmpty()) return

        for (reminder in pendingReminders) {
            sendTranscriptToPhone(reminder.sourceTranscript, reminder.id)
        }
        Log.i(TAG, "Sent ${pendingReminders.size} deferred formatting requests")
    }

    suspend fun requestFullSync() {
        val nodeId = getPhoneNodeId()
        if (nodeId == null) {
            Log.w(TAG, "No phone connected, cannot request full sync")
            return
        }

        try {
            messageClient.sendMessage(
                nodeId,
                DataLayerPaths.FULL_SYNC_PATH,
                ByteArray(0)
            ).await()
            Log.i(TAG, "Full sync requested from phone (path=${DataLayerPaths.FULL_SYNC_PATH})")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to request full sync", e)
        }
    }

    /**
     * Requests the phone to send its current sync state so the watch can
     * reconcile local and remote data.
     */
    suspend fun requestSyncState() {
        val nodeId = getPhoneNodeId()
        if (nodeId == null) {
            Log.w(TAG, "No phone connected, cannot request sync state")
            return
        }

        try {
            messageClient.sendMessage(
                nodeId,
                DataLayerPaths.SYNC_STATE_REQUEST,
                ByteArray(0)
            ).await()
            Log.i(TAG, "Sync state request sent to phone (node=$nodeId)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send sync state request", e)
        }
    }

    /**
     * Sends the watch's full sync state (active reminders plus tombstones)
     * to the phone so it can reconcile both sides of the sync.
     */
    suspend fun sendSyncStateComplete(
        reminders: List<WatchReminder>,
        tombstones: List<DeletedReminder>
    ) {
        val nodeId = getPhoneNodeId()
        if (nodeId == null) {
            Log.w(TAG, "No phone connected, cannot send sync state complete")
            return
        }

        try {
            val dto = SyncStateDto(
                activeReminders = reminders.map { it.toReminderDto() },
                tombstones = tombstones.map { it.toDeletedReminderDto() },
                deviceId = DEVICE_ID
            )
            val payload = ReminderSerializer.serializeSyncState(dto)
            messageClient.sendMessage(
                nodeId,
                DataLayerPaths.SYNC_STATE_COMPLETE,
                payload
            ).await()
            Log.i(
                TAG,
                "Sync state complete sent with ${reminders.size} reminders and ${tombstones.size} tombstones, payload=${payload.size} bytes"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send sync state complete", e)
        }
    }

    /**
     * Sends a single tombstone to the phone so it can mirror the deletion
     * without waiting for a full sync cycle.
     */
    suspend fun sendTombstone(tombstone: DeletedReminder) {
        val nodeId = getPhoneNodeId()
        if (nodeId == null) {
            Log.w(TAG, "No phone connected, cannot send tombstone")
            return
        }

        try {
            val dto = tombstone.toDeletedReminderDto()
            val payload = ReminderSerializer.serializeDeletedReminder(dto)
            messageClient.sendMessage(
                nodeId,
                DataLayerPaths.SYNC_TOMBSTONE,
                payload
            ).await()
            Log.i(TAG, "Tombstone sent for reminder ${tombstone.id}, payload=${payload.size} bytes")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send tombstone for reminder ${tombstone.id}", e)
        }
    }

    suspend fun getPhoneNodeId(): String? {
        return try {
            val capabilityInfo = Wearable.getCapabilityClient(appContext)
                .getCapability(
                    DataLayerPaths.CAPABILITY_PHONE,
                    com.google.android.gms.wearable.CapabilityClient.FILTER_REACHABLE
                )
                .await()
            capabilityInfo.nodes.firstOrNull()?.id
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get phone node ID", e)
            null
        }
    }

    private suspend fun <T> com.google.android.gms.tasks.Task<T>.await(): T =
        suspendCancellableCoroutine { cont ->
            addOnSuccessListener { result -> cont.resume(result) }
            addOnFailureListener { exception -> cont.resumeWithException(exception) }
        }

    companion object {
        private const val TAG = "WearDataLayerClient"
        const val KEY_REMINDER_JSON = "reminder_json"
        private const val KEY_TIMESTAMP = "timestamp"
        private const val FORMATTING_PENDING = "pending"
        private const val DEVICE_ID = "watch"

        private fun WatchReminder.toReminderDto() = ReminderDto(
            id = id,
            title = title,
            body = body,
            triggerTime = triggerTime?.toEpochMilli(),
            recurrence = recurrence,
            isCompleted = isCompleted,
            sourceTranscript = sourceTranscript,
            createdAt = createdAt.toEpochMilli(),
            locationTriggerJson = locationTriggerJson,
            locationState = locationState,
            formattingProvider = formattingProvider,
            geofencingDevice = geofencingDevice,
            updatedAt = updatedAt.toEpochMilli(),
            createdBy = createdBy,
            lastModifiedBy = lastModifiedBy
        )

        private fun DeletedReminder.toDeletedReminderDto() = DeletedReminderDto(
            id = id,
            originalTitle = originalTitle,
            deletedAt = deletedAt.toEpochMilli(),
            deletedBy = deletedBy,
            originalUpdatedAt = originalUpdatedAt.toEpochMilli()
        )
    }
}
