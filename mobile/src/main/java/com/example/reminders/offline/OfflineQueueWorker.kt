package com.example.reminders.offline

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * WorkManager worker that processes the offline retry queue when
 * network connectivity is restored.
 *
 * Iterates through all [PendingOperation] entries and attempts to
 * execute each one. Formatting operations re-run the formatting pipeline
 * on the raw transcript; geocoding operations retry the geocoding lookup.
 *
 * Operations that succeed are removed from the queue. Operations that
 * fail increment their retry count and remain in the queue. Exhausted
 * operations (max retries reached) are removed to prevent infinite loops.
 *
 * This worker requires network connectivity (configured via constraints
 * in [OfflineQueueManager.scheduleProcessing]) and will only execute
 * when the device is online.
 *
 * @param context      Application context.
 * @param workerParams WorkManager parameters.
 */
class OfflineQueueWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting offline queue processing")

        val container = obtainContainer()
        val operations = container.offlineQueueManager.getPendingOperations()

        if (operations.isEmpty()) {
            Log.d(TAG, "No pending operations found")
            return Result.success()
        }

        Log.d(TAG, "Processing ${operations.size} pending operations")

        var allSucceeded = true

        for (operation in operations) {
            if (operation.isExhausted) {
                Log.w(TAG, "Removing exhausted operation ${operation.id}")
                container.offlineQueueManager.removeOperation(operation)
                continue
            }

            val success = processOperation(operation, container)

            if (success) {
                container.offlineQueueManager.removeOperation(operation)
                Log.d(TAG, "Successfully processed operation ${operation.id}")
            } else {
                container.offlineQueueManager.incrementRetryCount(operation.id)
                val refreshed = container.offlineQueueManager.getPendingOperations()
                    .firstOrNull { it.id == operation.id }
                if (refreshed != null && refreshed.isExhausted) {
                    Log.w(TAG, "Removing exhausted operation ${operation.id}")
                    container.offlineQueueManager.removeOperation(refreshed)
                } else {
                    allSucceeded = false
                    Log.w(TAG, "Failed to process operation ${operation.id}, will retry later")
                }
            }
        }

        return if (allSucceeded) Result.success() else Result.retry()
    }

    /**
     * Processes a single pending operation.
     *
     * @return true if the operation succeeded, false otherwise.
     */
    private suspend fun processOperation(
        operation: PendingOperation,
        container: OfflineQueueContainer
    ): Boolean {
        return try {
            when (operation.type) {
                OperationType.FORMATTING -> processFormatting(operation, container)
                OperationType.GEOCODING -> processGeocoding(operation, container)
                else -> {
                    Log.w(TAG, "Unknown operation type: ${operation.type}")
                    false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing operation ${operation.id}", e)
            false
        }
    }

    /**
     * Retries a formatting operation by running the transcript through
     * the formatting pipeline again.
     */
    private suspend fun processFormatting(
        operation: PendingOperation,
        container: OfflineQueueContainer
    ): Boolean {
        val transcript = operation.payload
        val result = container.pipelineOrchestrator.processTranscript(transcript)

        return when (result) {
            is com.example.reminders.pipeline.PipelineResult.Success,
            is com.example.reminders.pipeline.PipelineResult.PartialSuccess -> true
            else -> false
        }
    }

    /**
     * Retries a geocoding operation.
     *
     * Note: Full geocoding retry requires the geocoding service and
     * may produce disambiguation results. In the offline queue worker,
     * we attempt the geocode and silently accept the first result
     * or leave it pending if no results are found.
     */
    private suspend fun processGeocoding(
        operation: PendingOperation,
        container: OfflineQueueContainer
    ): Boolean {
        val placeLabel = operation.payload

        val savedPlace = container.savedPlaceMatcher.match(placeLabel)
        if (savedPlace != null) {
            return true
        }

        val geocodingResult = container.geocodingService.geocode(placeLabel)

        return geocodingResult is com.example.reminders.geocoding.GeocodingResult.Resolved
    }

    /**
     * Obtains the [OfflineQueueContainer] from the application.
     *
     * Uses a stub-able interface so the worker can be tested without
     * needing the full AppContainer.
     */
    private fun obtainContainer(): OfflineQueueContainer {
        val app = applicationContext as OfflineQueueProvider
        return app.provideOfflineQueueContainer()
    }

    companion object {
        private const val TAG = "OfflineQueueWorker"
    }
}

/**
 * Interface that the Application class must implement to provide
 * the [OfflineQueueContainer] to the worker. This indirection allows
 * testing by substituting a test container.
 */
interface OfflineQueueProvider {
    fun provideOfflineQueueContainer(): OfflineQueueContainer
}

/**
 * Provides the dependencies needed by [OfflineQueueWorker].
 *
 * Implemented by [com.example.reminders.di.AppContainer] so the
 * worker can access repositories and services without direct
 * coupling to the DI container.
 */
interface OfflineQueueContainer {
    val offlineQueueManager: OfflineQueueManager
    val pipelineOrchestrator: com.example.reminders.pipeline.PipelineOrchestrator
    val savedPlaceMatcher: com.example.reminders.geocoding.SavedPlaceMatcher
    val geocodingService: com.example.reminders.geocoding.GeocodingService
}
