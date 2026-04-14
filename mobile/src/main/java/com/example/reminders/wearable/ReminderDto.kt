package com.example.reminders.wearable

import com.example.reminders.data.model.DeletedReminder
import com.example.reminders.data.model.LocationReminderState
import com.example.reminders.data.model.LocationTrigger
import com.example.reminders.data.model.Reminder
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class ReminderDto(
    val id: String,
    val title: String,
    val body: String? = null,
    val triggerTime: Long? = null,
    val recurrence: String? = null,
    val isCompleted: Boolean = false,
    val sourceTranscript: String,
    val createdAt: Long,
    val locationTriggerJson: String? = null,
    val locationState: String? = null,
    val formattingProvider: String = "none",
    val geofencingDevice: String = "phone",
    val createdBy: String = "mobile",
    val lastModifiedBy: String = "mobile",
    val updatedAt: Long
) {
    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun fromReminder(reminder: Reminder): ReminderDto = ReminderDto(
            id = reminder.id,
            title = reminder.title,
            body = reminder.body,
            triggerTime = reminder.triggerTime?.toEpochMilli(),
            recurrence = reminder.recurrence,
            isCompleted = reminder.isCompleted,
            sourceTranscript = reminder.sourceTranscript,
            createdAt = reminder.createdAt.toEpochMilli(),
            locationTriggerJson = reminder.locationTrigger?.let { json.encodeToString(it) },
            locationState = reminder.locationState?.name,
            formattingProvider = reminder.formattingProvider,
            geofencingDevice = reminder.geofencingDevice,
            createdBy = reminder.createdBy,
            lastModifiedBy = reminder.lastModifiedBy,
            updatedAt = reminder.updatedAt.toEpochMilli()
        )
    }
}

@Serializable
data class DeletedReminderDto(
    val id: String,
    val originalTitle: String,
    val deletedAt: Long,
    val deletedBy: String,
    val originalUpdatedAt: Long
) {
    companion object {
        /**
         * Converts a [DeletedReminder] entity into its wire-format DTO,
         * converting [Instant] timestamps to epoch milliseconds.
         */
        fun fromDeletedReminder(tombstone: DeletedReminder): DeletedReminderDto =
            DeletedReminderDto(
                id = tombstone.id,
                originalTitle = tombstone.originalTitle,
                deletedAt = tombstone.deletedAt.toEpochMilli(),
                deletedBy = tombstone.deletedBy,
                originalUpdatedAt = tombstone.originalUpdatedAt.toEpochMilli()
            )
    }
}

@Serializable
data class SyncStateDto(
    val activeReminders: List<ReminderDto>,
    val tombstones: List<DeletedReminderDto>,
    val deviceId: String
)
