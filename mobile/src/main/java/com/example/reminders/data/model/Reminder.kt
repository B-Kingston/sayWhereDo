package com.example.reminders.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey
    val id: String,
    val title: String,
    val body: String? = null,
    val triggerTime: Instant? = null,
    val recurrence: String? = null,
    @ColumnInfo(defaultValue = "NULL")
    val locationTrigger: LocationTrigger? = null,
    val locationState: LocationReminderState? = null,
    val sourceTranscript: String,
    val formattingProvider: String = "none",
    val geofencingDevice: String = "phone",
    val isCompleted: Boolean = false,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
)
