package com.example.reminders.wear.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * Tombstone entity recording a reminder that was deleted on the watch.
 *
 * During bidirectional sync, the phone needs to know which reminders were
 * removed locally so it can mirror the deletion. This table persists those
 * tombstones until the phone has acknowledged them, preventing deleted
 * reminders from reappearing on the next sync cycle.
 */
@Entity(tableName = "deleted_reminders")
data class DeletedReminder(
    @PrimaryKey
    val id: String,
    val originalTitle: String,
    val deletedAt: Instant,
    val deletedBy: String,
    val originalUpdatedAt: Instant
)
