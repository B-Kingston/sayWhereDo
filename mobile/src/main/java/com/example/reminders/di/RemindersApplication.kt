package com.example.reminders.di

import android.app.Application

class RemindersApplication : Application() {

    val container: AppContainer by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        container.billingManager.startConnection()
    }
}
