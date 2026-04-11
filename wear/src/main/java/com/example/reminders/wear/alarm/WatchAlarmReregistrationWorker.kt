package com.example.reminders.wear.alarm

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.reminders.wear.di.WatchRemindersApplication

/**
 * WorkManager worker that re-schedules all active time-based alarms
 * on the watch after a device reboot.
 */
class WatchAlarmReregistrationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        Log.i(TAG, "Starting watch alarm re-scheduling")

        val container = (applicationContext as WatchRemindersApplication).container
        val alarmScheduler = container.watchAlarmScheduler

        try {
            alarmScheduler.rescheduleAll()
            return Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to re-schedule watch alarms", e)
            return Result.retry()
        }
    }

    companion object {
        private const val TAG = "WatchAlarmReregister"
        const val WORK_NAME = "watch_alarm_reregistration"
    }
}
