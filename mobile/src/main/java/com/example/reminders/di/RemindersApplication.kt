package com.example.reminders.di

import android.app.Application
import android.util.Log
import com.example.reminders.offline.OfflineQueueProvider

/**
 * Application subclass that initialises the manual DI container
 * and starts the billing connection on launch.
 *
 * Implements [OfflineQueueProvider] so that [com.example.reminders.offline.OfflineQueueWorker]
 * can obtain its dependencies without static accessors or Hilt.
 */
class RemindersApplication : Application(), OfflineQueueProvider {

    val container: AppContainer by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "Application onCreate")
        container.billingManager.startConnection()
        Log.i(TAG, "Application onCreate complete")
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= TRIM_MEMORY_RUNNING_LOW) {
            container.localFormattingProvider.close()
        }
    }

    override fun provideOfflineQueueContainer() = container

    companion object {
        private const val TAG = "RemindersApplication"
        private const val TRIM_MEMORY_RUNNING_LOW = 10
    }
}
