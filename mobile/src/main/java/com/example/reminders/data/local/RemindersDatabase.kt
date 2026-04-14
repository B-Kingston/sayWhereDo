package com.example.reminders.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.reminders.data.model.DeletedReminder
import com.example.reminders.data.model.Reminder
import com.example.reminders.data.model.SavedPlace

@Database(
    entities = [Reminder::class, SavedPlace::class, DeletedReminder::class],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class RemindersDatabase : RoomDatabase() {
    abstract fun reminderDao(): ReminderDao
    abstract fun savedPlaceDao(): SavedPlaceDao
    abstract fun deletedReminderDao(): DeletedReminderDao

    companion object {
        /**
         * Migration from v1 → v2:
         *  - adds `createdBy` and `lastModifiedBy` columns to `reminders`
         *  - creates the `deleted_reminders` tombstone table
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE reminders ADD COLUMN createdBy TEXT NOT NULL DEFAULT 'mobile'"
                )
                db.execSQL(
                    "ALTER TABLE reminders ADD COLUMN lastModifiedBy TEXT NOT NULL DEFAULT 'mobile'"
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS deleted_reminders (
                        id TEXT NOT NULL PRIMARY KEY,
                        originalTitle TEXT NOT NULL,
                        deletedAt INTEGER NOT NULL,
                        deletedBy TEXT NOT NULL,
                        originalUpdatedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }
    }
}
