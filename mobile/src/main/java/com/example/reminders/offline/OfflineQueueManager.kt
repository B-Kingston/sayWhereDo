package com.example.reminders.offline

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.UUID

/**
 * Manages the offline retry queue for formatting and geocoding operations.
 *
 * When network calls fail due to connectivity issues, the caller enqueues
 * a [PendingOperation] via this manager. A WorkManager worker
 * ([OfflineQueueWorker]) processes the queue when connectivity is restored.
 *
 * Usage:
 * ```
 * // Enqueue a failed formatting operation
 * offlineQueueManager.enqueueFormatting(transcript, reminderId)
 *
 * // Enqueue a failed geocoding operation
 * offlineQueueManager.enqueueGeocoding(reminderId, placeLabel)
 * ```
 *
 * @param context  Application context for WorkManager access.
 * @param dao      DAO for the pending operations table.
 */
class OfflineQueueManager(
    private val context: Context,
    private val dao: PendingOperationDao
) {

    /**
     * Enqueues a formatting operation for later retry.
     *
     * @param transcript The raw voice transcript that needs formatting.
     * @param reminderId The ID of the raw fallback reminder already saved.
     */
    suspend fun enqueueFormatting(transcript: String, reminderId: String) {
        val operation = PendingOperation(
            id = UUID.randomUUID().toString(),
            type = OperationType.FORMATTING,
            payload = transcript,
            reminderId = reminderId
        )
        dao.insert(operation)
        scheduleProcessing()
    }

    /**
     * Enqueues a geocoding operation for later retry.
     *
     * @param reminderId The ID of the reminder that needs geocoding.
     * @param placeLabel The location text to geocode.
     */
    suspend fun enqueueGeocoding(reminderId: String, placeLabel: String) {
        val operation = PendingOperation(
            id = UUID.randomUUID().toString(),
            type = OperationType.GEOCODING,
            payload = placeLabel,
            reminderId = reminderId
        )
        dao.insert(operation)
        scheduleProcessing()
    }

    /**
     * Returns all pending operations in the queue.
     */
    suspend fun getPendingOperations(): List<PendingOperation> = dao.getAll()

    /**
     * Returns the number of pending operations in the queue.
     */
    suspend fun getPendingCount(): Int = dao.count()

    /**
     * Removes a completed operation from the queue.
     */
    suspend fun removeOperation(operation: PendingOperation) {
        dao.delete(operation)
    }

    /**
     * Removes an operation by ID.
     */
    suspend fun removeOperationById(id: String) {
        dao.deleteById(id)
    }

    /**
     * Clears all pending operations.
     */
    suspend fun clearAll() {
        dao.deleteAll()
    }

    /**
     * Schedules the [OfflineQueueWorker] to process the queue when
     * connectivity is restored. Uses a unique work name so that
     * multiple enqueue calls collapse into a single execution.
     */
    private fun scheduleProcessing() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<OfflineQueueWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                UNIQUE_WORK_NAME,
                ExistingWorkPolicy.KEEP,
                workRequest
            )
    }

    companion object {
        const val UNIQUE_WORK_NAME = "offline_queue_processing"
    }
}
