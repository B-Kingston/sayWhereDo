package com.example.reminders.wear.di

import android.app.Application

class WatchRemindersApplication : Application() {

    val container: WatchAppContainer by lazy { WatchAppContainer(this) }
}
