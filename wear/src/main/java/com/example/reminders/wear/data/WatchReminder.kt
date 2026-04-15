package com.example.reminders.wear.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * Room entity representing a reminder stored on the watch.
 *
 * This is a lightweight version of the mobile Reminder entity. Location
 * data is stored as a JSON string since Room cannot natively handle
 * complex embedded types without triggering the KSP2 @Serializable bug.
 */
@Entity(tableName = "watch_reminders")
data class WatchReminder(
    @PrimaryKey
    val id: String,
    val title: String,
    val body: String? = null,
    val triggerTime: Instant? = null,
    val recurrence: String? = null,
    val isCompleted: Boolean = false,
    val sourceTranscript: String,
    val createdAt: Instant = Instant.now(),
    val locationTriggerJson: String? = null,
    val locationState: String? = null,
    val formattingProvider: String = "none",
    @ColumnInfo(defaultValue = "watch")
    val geofencingDevice: String = "watch",
    @ColumnInfo(defaultValue = "watch")
    val createdBy: String = "watch",
    @ColumnInfo(defaultValue = "watch")
    val lastModifiedBy: String = "watch",
    val updatedAt: Instant = Instant.now()
)
