package com.example.reminders.wear.di

import android.content.Context
import androidx.room.Room
import com.example.reminders.wear.data.WatchRemindersDatabase

class WatchAppContainer(context: Context) {

    val database: WatchRemindersDatabase = Room.databaseBuilder(
        context,
        WatchRemindersDatabase::class.java,
        "watch-reminders-db"
    ).build()

    val watchReminderDao = database.watchReminderDao()
}
