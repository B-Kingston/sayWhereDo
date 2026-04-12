package com.example.reminders.wear.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

/**
 * Listens for [Intent.ACTION_BOOT_COMPLETED] and enqueues the
 * [WatchAlarmReregistrationWorker] to restore time-based alarms
 * that were cleared by the device restart.
 *
 * Uses [ExistingWorkPolicy.KEEP] to prevent duplicate workers when
 * multiple BOOT_COMPLETED broadcasts are received.
 */
class WatchAlarmBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val workRequest = OneTimeWorkRequestBuilder<WatchAlarmReregistrationWorker>()
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                UNIQUE_WORK_NAME,
                ExistingWorkPolicy.KEEP,
                workRequest
            )
    }

    companion object {
        private const val UNIQUE_WORK_NAME = "watch_alarm_reregistration"
    }
}
