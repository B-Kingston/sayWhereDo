package com.example.reminders.wear.sync

import kotlinx.serialization.Serializable

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
    val geofencingDevice: String = "watch",
    val createdBy: String = "mobile",
    val lastModifiedBy: String = "mobile",
    val updatedAt: Long
)

@Serializable
data class DeletedReminderDto(
    val id: String,
    val originalTitle: String,
    val deletedAt: Long,
    val deletedBy: String,
    val originalUpdatedAt: Long
)

@Serializable
data class SyncStateDto(
    val activeReminders: List<ReminderDto>,
    val tombstones: List<DeletedReminderDto>,
    val deviceId: String
)
