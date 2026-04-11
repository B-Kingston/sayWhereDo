package com.example.reminders.di

import android.app.Application
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
        container.billingManager.startConnection()
    }

    override fun provideOfflineQueueContainer() = container
}
