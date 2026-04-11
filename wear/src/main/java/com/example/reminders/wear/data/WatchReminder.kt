package com.example.reminders.wear.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

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
    val updatedAt: Instant = Instant.now()
)
