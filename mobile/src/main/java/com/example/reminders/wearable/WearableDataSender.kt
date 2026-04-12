package com.example.reminders.wearable

import android.net.Uri
import android.util.Log
import com.example.reminders.data.model.Reminder
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.NodeClient
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream

class WearableDataSender(context: android.content.Context) {

    private val dataClient: DataClient = Wearable.getDataClient(context)
    private val messageClient: MessageClient = Wearable.getMessageClient(context)
    private val nodeClient: NodeClient = Wearable.getNodeClient(context)

    suspend fun syncReminderToWatch(reminder: Reminder) {
        val nodes = getConnectedWatchNodes()
        if (nodes.isEmpty()) {
            Log.d(TAG, "No connected watches — skipping sync for ${reminder.id}")
            return
        }

        val dto = ReminderDto.fromReminder(reminder)
        val json = Json.encodeToString(dto)
        val compressed = compress(json.toByteArray(Charsets.UTF_8))

        val putRequest = PutDataMapRequest.create(DataLayerPaths.reminderPath(reminder.id)).apply {
            dataMap.putByteArray(KEY_REMINDER_DATA, compressed)
            dataMap.putString(KEY_REMINDER_JSON, json)
        }

        putRequest.setUrgent()
        withContext(Dispatchers.IO) {
            Tasks.await(dataClient.putDataItem(putRequest.asPutDataRequest()))
        }

        Log.i(TAG, "Synced reminder ${reminder.id} to ${nodes.size} watch(es)")
    }

    suspend fun deleteReminderFromWatch(reminderId: String) {
        val nodes = getConnectedWatchNodes()
        if (nodes.isEmpty()) {
            Log.d(TAG, "No connected watches — skipping deletion for $reminderId")
            return
        }

        val uri = Uri.Builder()
            .scheme("wear")
            .path(DataLayerPaths.reminderPath(reminderId))
            .build()

        withContext(Dispatchers.IO) {
            Tasks.await(dataClient.deleteDataItems(uri))
        }

        Log.i(TAG, "Deleted reminder $reminderId from ${nodes.size} watch(es)")
    }

    suspend fun syncProStatus(isPro: Boolean) {
        val nodes = getConnectedWatchNodes()
        if (nodes.isEmpty()) {
            Log.d(TAG, "No connected watches — skipping Pro status sync")
            return
        }

        val putRequest = PutDataMapRequest.create(DataLayerPaths.PRO_STATUS_PATH).apply {
            dataMap.putBoolean(KEY_IS_PRO, isPro)
        }

        putRequest.setUrgent()
        withContext(Dispatchers.IO) {
            Tasks.await(dataClient.putDataItem(putRequest.asPutDataRequest()))
        }

        Log.i(TAG, "Synced Pro status (isPro=$isPro) to ${nodes.size} watch(es)")
    }

    suspend fun sendFormattedReminder(reminder: Reminder, targetNodeId: String) {
        val dto = ReminderDto.fromReminder(reminder)
        val json = Json.encodeToString(dto)

        withContext(Dispatchers.IO) {
            Tasks.await(
                messageClient.sendMessage(
                    targetNodeId,
                    DataLayerPaths.DEFERRED_FORMATTING_PATH,
                    json.toByteArray(Charsets.UTF_8)
                )
            )
        }

        Log.i(TAG, "Sent formatted reminder ${reminder.id} to node $targetNodeId")
    }

    suspend fun sendAllRemindersToNode(
        reminders: List<Reminder>,
        targetNodeId: String
    ) {
        for (reminder in reminders) {
            syncReminderToWatch(reminder)
        }
        Log.i(TAG, "Full sync: sent ${reminders.size} reminder(s) to node $targetNodeId")
    }

    suspend fun getConnectedWatchNodes(): List<Node> = withContext(Dispatchers.IO) {
        try {
            Tasks.await(nodeClient.connectedNodes)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to query connected nodes", e)
            emptyList()
        }
    }

    private fun compress(data: ByteArray): ByteArray {
        val bos = ByteArrayOutputStream()
        GZIPOutputStream(bos).use { it.write(data) }
        return bos.toByteArray()
    }

    companion object {
        private const val TAG = "WearableDataSender"
        const val KEY_REMINDER_DATA = "reminder_data_gzip"
        const val KEY_REMINDER_JSON = "reminder_json"
        const val KEY_IS_PRO = "is_pro"
    }
}
