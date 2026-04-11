package com.example.reminders.wear.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [WatchReminder::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(WatchConverters::class)
abstract class WatchRemindersDatabase : RoomDatabase() {
    abstract fun watchReminderDao(): WatchReminderDao
}
