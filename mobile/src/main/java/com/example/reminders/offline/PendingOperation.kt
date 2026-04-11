package com.example.reminders.offline

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a deferred operation that will be retried when
 * connectivity is restored.
 *
 * Used by the offline queue to track pending formatting and
 * geocoding operations that failed due to network unavailability.
 * Each operation stores the raw transcript (for formatting) or
 * reminder data (for geocoding) so it can be replayed later.
 *
 * @property id           Unique identifier for the operation.
 * @property type         The kind of operation (see [OperationType]).
 * @property payload      JSON-encoded data needed to execute the operation.
 * @property reminderId   The ID of the associated reminder, if applicable.
 * @property createdAt    Epoch millis when the operation was enqueued.
 * @property retryCount   Number of times this operation has been attempted.
 * @property maxRetries   Maximum number of retries before giving up.
 */
@Entity(tableName = "pending_operations")
data class PendingOperation(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "type")
    val type: String,
    @ColumnInfo(name = "payload")
    val payload: String,
    @ColumnInfo(name = "reminder_id")
    val reminderId: String? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "retry_count")
    val retryCount: Int = 0,
    @ColumnInfo(name = "max_retries")
    val maxRetries: Int = DEFAULT_MAX_RETRIES
) {
    /**
     * Whether this operation has exhausted its retry budget.
     */
    val isExhausted: Boolean get() = retryCount >= maxRetries

    companion object {
        const val DEFAULT_MAX_RETRIES = 3
    }
}

/**
 * Types of deferred operations that can be queued for retry.
 */
object OperationType {
    const val FORMATTING = "formatting"
    const val GEOCODING = "geocoding"
}
