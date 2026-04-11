package com.example.reminders.alarm

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.reminders.di.RemindersApplication

/**
 * WorkManager worker that re-schedules all active time-based alarms
 * after a device reboot.
 *
 * Android clears all [android.app.AlarmManager] alarms when the device
 * restarts. This worker queries Room for all reminders with a future
 * trigger time and re-schedules them via [AndroidAlarmScheduler].
 */
class AlarmReregistrationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        Log.i(TAG, "Starting alarm re-scheduling")

        val container = (applicationContext as RemindersApplication).container
        val alarmScheduler = container.alarmScheduler

        try {
            alarmScheduler.rescheduleAll()
            return Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to re-schedule alarms", e)
            return Result.retry()
        }
    }

    companion object {
        private const val TAG = "AlarmReregister"
        const val WORK_NAME = "alarm_reregistration"
    }
}
