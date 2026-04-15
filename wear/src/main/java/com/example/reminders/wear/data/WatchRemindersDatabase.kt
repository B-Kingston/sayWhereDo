package com.example.reminders.wear.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [WatchReminder::class, DeletedReminder::class],
    version = 3,
    exportSchema = true
)
@TypeConverters(WatchConverters::class)
abstract class WatchRemindersDatabase : RoomDatabase() {
    abstract fun watchReminderDao(): WatchReminderDao
    abstract fun deletedReminderDao(): DeletedReminderDao

    companion object {
        /**
         * Migration from v1 to v2: adds the `geofencingDevice` column
         * with a default value of "watch" and the [WatchReminder.recurrence] column.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE watch_reminders ADD COLUMN geofencingDevice TEXT NOT NULL DEFAULT 'watch'"
                )
                db.execSQL("ALTER TABLE watch_reminders ADD COLUMN recurrence TEXT DEFAULT NULL")
            }
        }

        /**
         * Migration from v2 to v3: adds `createdBy` and `lastModifiedBy` columns
         * to [WatchReminder], and creates the `deleted_reminders` tombstone table
         * used for bidirectional sync deletion tracking.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE watch_reminders ADD COLUMN createdBy TEXT NOT NULL DEFAULT 'watch'"
                )
                db.execSQL(
                    "ALTER TABLE watch_reminders ADD COLUMN lastModifiedBy TEXT NOT NULL DEFAULT 'watch'"
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS deleted_reminders (
                        id TEXT PRIMARY KEY NOT NULL,
                        originalTitle TEXT,
                        deletedAt INTEGER,
                        deletedBy TEXT,
                        originalUpdatedAt INTEGER
                    )
                    """.trimIndent()
                )
            }
        }
    }
}
