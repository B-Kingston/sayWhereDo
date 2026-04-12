package com.example.reminders.wear.data

import android.content.Context
import android.util.Log
import com.example.reminders.wear.sync.DataLayerPaths
import com.example.reminders.wear.sync.ReminderSerializer
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
            val path = DataLayerPaths.reminderPath(reminder.id)
            val putRequest = PutDataMapRequest.create(path).apply {
                dataMap.putByteArray(KEY_REMINDER_DATA, ReminderSerializer.serialize(reminder))
                dataMap.putLong(KEY_TIMESTAMP, System.currentTimeMillis())
            }
            dataClient.putDataItem(putRequest.asPutDataRequest().setUrgent()).await()
            Log.i(TAG, "Synced reminder ${reminder.id} to phone")
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
                "/request-sync",
                ByteArray(0)
            ).await()
            Log.i(TAG, "Full sync requested from phone")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to request full sync", e)
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
        private const val KEY_REMINDER_DATA = "reminder_data"
        private const val KEY_TIMESTAMP = "timestamp"
        private const val FORMATTING_PENDING = "pending"
    }
}
