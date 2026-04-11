package com.example.reminders.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.reminders.data.model.Reminder
import com.example.reminders.data.model.SavedPlace

@Database(
    entities = [Reminder::class, SavedPlace::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class RemindersDatabase : RoomDatabase() {
    abstract fun reminderDao(): ReminderDao
    abstract fun savedPlaceDao(): SavedPlaceDao
}
