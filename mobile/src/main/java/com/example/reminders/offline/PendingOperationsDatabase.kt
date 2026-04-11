package com.example.reminders.offline

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Lightweight Room database for the offline retry queue.
 *
 * Uses a separate database file from the main [RemindersDatabase]
 * to avoid schema migration conflicts with parallel development
 * on the primary database.
 *
 * Only contains the [PendingOperation] entity for tracking deferred
 * formatting and geocoding operations.
 */
@Database(
    entities = [PendingOperation::class],
    version = 1,
    exportSchema = true
)
abstract class PendingOperationsDatabase : RoomDatabase() {
    abstract fun pendingOperationDao(): PendingOperationDao
}
