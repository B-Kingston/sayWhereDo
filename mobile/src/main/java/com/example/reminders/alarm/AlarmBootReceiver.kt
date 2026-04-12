package com.example.reminders.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

/**
 * Listens for [Intent.ACTION_BOOT_COMPLETED] and enqueues the
 * [AlarmReregistrationWorker] to restore time-based alarms that
 * were cleared by the device restart.
 *
 * This is a separate receiver from [com.example.reminders.geofence.GeofenceBootReceiver]
 * to maintain clean separation between alarm and geofence concerns.
 * Both receivers are triggered by the same system broadcast.
 */
class AlarmBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val workRequest = OneTimeWorkRequestBuilder<AlarmReregistrationWorker>()
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                AlarmReregistrationWorker.WORK_NAME,
                ExistingWorkPolicy.KEEP,
                workRequest
            )
    }
}
