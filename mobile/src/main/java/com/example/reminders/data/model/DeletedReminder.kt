package com.example.reminders.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * Tombstone entity representing a soft-deleted reminder.
 *
 * When a reminder is deleted, it is moved from the `reminders` table into
 * `deleted_reminders`. Tombstones drive bidirectional sync reconciliation
 * (preventing re-insertion of deleted items) and power the Trash/Restore UI.
 * Rows older than 7 days are eligible for permanent cleanup.
 *
 * @property id              Same identifier as the original [Reminder].
 * @property originalTitle   Title preserved for display in the Trash screen.
 * @property deletedAt       Timestamp when the deletion occurred.
 * @property deletedBy       Device that performed the deletion ("mobile" or "watch").
 * @property originalUpdatedAt The [Reminder.updatedAt] value at the moment of deletion,
 *                           used by the sync engine to resolve edit-vs-delete conflicts.
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
