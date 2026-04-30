package com.example.reminders.wear.di

import android.app.Application
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
        CoroutineScope(Dispatchers.IO).launch {
            container.watchReminderRepository.cleanExpiredTombstones()
        }
    }

    companion object {
        private const val TAG = "WatchRemindersApp"
    }
}
