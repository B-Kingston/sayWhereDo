package com.example.reminders.wearable

import android.net.Uri
import android.util.Log
import com.example.reminders.data.model.LocationReminderState
import com.example.reminders.data.model.LocationTrigger
import com.example.reminders.data.model.Reminder
import com.example.reminders.di.RemindersApplication
import com.example.reminders.pipeline.PipelineResult
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.time.Instant

class WearableListenerServiceImpl : WearableListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json = Json { ignoreUnknownKeys = true }

    private val container by lazy {
        (applicationContext as RemindersApplication).container
    }

    private val dataSender by lazy { container.wearableDataSender }
    private val repository by lazy { container.reminderRepository }
    private val pipeline by lazy { container.pipelineOrchestrator }
    private val conflictResolver = SyncConflictResolver()

    override fun onMessageReceived(event: MessageEvent) {
        val path = event.path
        Log.i(TAG, "Message received: path=$path from=${event.sourceNodeId}")

        when {
            path == DataLayerPaths.DEFERRED_FORMATTING_PATH -> {
                handleDeferredFormatting(event)
            }

            path == DataLayerPaths.FULL_SYNC_PATH -> {
                handleFullSync(event)
            }

            else -> {
                Log.w(TAG, "Unknown message path: $path")
            }
        }
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        for (event in dataEvents) {
            val uri = event.dataItem.uri
            val path = uri.path ?: continue

            if (!path.startsWith(DataLayerPaths.REMINDERS_PREFIX)) continue

            when (event.type) {
                DataEvent.TYPE_CHANGED -> handleReminderUpdate(event)
                DataEvent.TYPE_DELETED -> handleReminderDelete(event)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    private fun handleDeferredFormatting(event: MessageEvent) {
        val transcript = event.data.decodeToString()
        val sourceNodeId = event.sourceNodeId

        Log.i(TAG, "Deferred formatting request from $sourceNodeId: ${transcript.take(60)}")

        scope.launch {
            try {
                val result = pipeline.processTranscript(transcript)

                when (result) {
                    is PipelineResult.Success -> {
                        for (reminder in result.reminders) {
                            dataSender.sendFormattedReminder(reminder, sourceNodeId)
                        }
                    }

                    is PipelineResult.PartialSuccess -> {
                        for (reminder in result.reminders) {
                            dataSender.sendFormattedReminder(reminder, sourceNodeId)
                        }
                    }

                    is PipelineResult.Failure -> {
                        Log.e(TAG, "Formatting failed: ${result.error}")
                    }

                    is PipelineResult.UsageLimited -> {
                        Log.w(TAG, "Formatting usage limited for watch request")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing deferred formatting", e)
            }
        }
    }

    private fun handleFullSync(event: MessageEvent) {
        val sourceNodeId = event.sourceNodeId
        Log.i(TAG, "Full sync requested by $sourceNodeId")

        scope.launch {
            try {
                val reminders = repository.getActiveReminders().first()
                dataSender.sendAllRemindersToNode(reminders, sourceNodeId)

                val isPro = container.billingManager.isPro.value
                dataSender.syncProStatus(isPro)

                Log.i(TAG, "Full sync completed: ${reminders.size} reminders sent")
            } catch (e: Exception) {
                Log.e(TAG, "Error during full sync", e)
            }
        }
    }

    private fun handleReminderUpdate(event: DataEvent) {
        val uri = event.dataItem.uri
        val reminderId = uri.lastPathSegment ?: return

        Log.d(TAG, "Reminder update from watch: $reminderId")

        scope.launch {
            try {
                val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                val dtoJson = dataMap.getString(WearableDataSender.KEY_REMINDER_JSON)
                    ?: return@launch

                val dto = json.decodeFromString<ReminderDto>(dtoJson)
                val remoteReminder = dtoToReminder(dto)

                val localReminder = repository.getReminderById(reminderId)
                if (localReminder == null) {
                    repository.insert(remoteReminder)
                    Log.i(TAG, "Inserted new reminder from watch: $reminderId")
                } else {
                    val resolved = conflictResolver.resolve(localReminder, remoteReminder)
                    repository.update(resolved)
                    Log.i(TAG, "Resolved and updated reminder from watch: $reminderId")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling reminder update from watch", e)
            }
        }
    }

    private fun handleReminderDelete(event: DataEvent) {
        val reminderId = event.dataItem.uri.lastPathSegment ?: return

        Log.d(TAG, "Reminder deletion from watch: $reminderId")

        scope.launch {
            try {
                val existing = repository.getReminderById(reminderId)
                if (existing != null) {
                    repository.deleteById(reminderId)
                    Log.i(TAG, "Deleted reminder from watch: $reminderId")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling reminder deletion from watch", e)
            }
        }
    }

    private fun dtoToReminder(dto: ReminderDto): Reminder {
        val locationTrigger = dto.locationTriggerJson?.let {
            json.decodeFromString<LocationTrigger>(it)
        }

        val locationState = dto.locationState?.let {
            runCatching { LocationReminderState.valueOf(it) }.getOrNull()
        }

        return Reminder(
            id = dto.id,
            title = dto.title,
            body = dto.body,
            triggerTime = dto.triggerTime?.let { Instant.ofEpochMilli(it) },
            recurrence = dto.recurrence,
            locationTrigger = locationTrigger,
            locationState = locationState,
            sourceTranscript = dto.sourceTranscript,
            formattingProvider = dto.formattingProvider,
            geofencingDevice = dto.geofencingDevice,
            isCompleted = dto.isCompleted,
            createdAt = Instant.ofEpochMilli(dto.createdAt),
            updatedAt = Instant.ofEpochMilli(dto.updatedAt)
        )
    }

    companion object {
        private const val TAG = "WearableListenerSvc"
    }
}
