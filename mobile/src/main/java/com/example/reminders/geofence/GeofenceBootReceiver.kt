package com.example.reminders.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

/**
 * Listens for [Intent.ACTION_BOOT_COMPLETED] and enqueues the
 * [GeofenceReregistrationWorker] to restore geofences that were
 * cleared by the device restart.
 */
class GeofenceBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val workRequest = OneTimeWorkRequestBuilder<GeofenceReregistrationWorker>()
            .build()

        WorkManager.getInstance(context)
            .enqueue(workRequest)
    }
}
