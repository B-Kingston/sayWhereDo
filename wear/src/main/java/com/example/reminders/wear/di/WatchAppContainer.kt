package com.example.reminders.wear.di

import android.content.Context
import androidx.room.Room
import com.example.reminders.wear.alarm.AndroidWatchAlarmScheduler
import com.example.reminders.wear.alarm.WatchAlarmScheduler
import com.example.reminders.wear.data.WatchRemindersDatabase

class WatchAppContainer(context: Context) {

    val database: WatchRemindersDatabase = Room.databaseBuilder(
        context,
        WatchRemindersDatabase::class.java,
        "watch-reminders-db"
    )
        .addMigrations(WatchRemindersDatabase.MIGRATION_1_2)
        .build()

    val watchReminderDao = database.watchReminderDao()

    /**
     * Schedules and cancels time-based reminder alarms on the watch.
     */
    val watchAlarmScheduler: WatchAlarmScheduler = AndroidWatchAlarmScheduler(
        context = context,
        watchReminderDao = watchReminderDao
    )
}
