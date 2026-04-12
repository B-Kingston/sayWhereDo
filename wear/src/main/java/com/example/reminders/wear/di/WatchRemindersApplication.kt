package com.example.reminders.wear.di

import android.app.Application
import android.util.Log

class WatchRemindersApplication : Application() {

    val container: WatchAppContainer by lazy {
        Log.i(TAG, "Initializing WatchAppContainer")
        WatchAppContainer(this).also {
            Log.i(TAG, "WatchAppContainer initialized")
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "onCreate called")
    }

    companion object {
        private const val TAG = "WatchRemindersApp"
    }
}
