package com.example.reminders.wear.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [WatchReminder::class],
    version = 2,
    exportSchema = true
)
@TypeConverters(WatchConverters::class)
abstract class WatchRemindersDatabase : RoomDatabase() {
    abstract fun watchReminderDao(): WatchReminderDao

    companion object {
        /**
         * Migration from v1 to v2: adds the [WatchReminder.recurrence] column.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE watch_reminders ADD COLUMN recurrence TEXT DEFAULT NULL")
            }
        }
    }
}
