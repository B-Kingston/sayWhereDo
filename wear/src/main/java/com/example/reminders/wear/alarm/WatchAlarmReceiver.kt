package com.example.reminders.wear.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.reminders.wear.di.WatchRemindersApplication
import com.example.reminders.wear.notification.WatchNotificationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class WatchAlarmReceiver : BroadcastReceiver() {

    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != AndroidWatchAlarmScheduler.ACTION_TRIGGER_WATCH_REMINDER) return

        val reminderId = intent.getStringExtra(AndroidWatchAlarmScheduler.EXTRA_REMINDER_ID)
        if (reminderId == null) {
            Log.e(TAG, "Missing reminder ID in alarm intent")
            return
        }

        val pendingResult = goAsync()

        coroutineScope.launch {
            try {
                val container = (context.applicationContext as WatchRemindersApplication).container
                val reminder = container.watchReminderDao.getById(reminderId)

                if (reminder == null) {
                    Log.w(TAG, "No reminder found for alarm: $reminderId")
                    return@launch
                }

                if (reminder.isCompleted) {
                    Log.d(TAG, "Reminder $reminderId already completed, skipping")
                    return@launch
                }

                container.watchNotificationManager.showTimeReminderNotification(
                    reminderId = reminder.id,
                    title = reminder.title,
                    body = reminder.body
                )

                Log.i(TAG, "Triggered watch reminder: ${reminder.title}")
            } catch (e: Exception) {
                Log.e(TAG, "Error handling alarm for $reminderId", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "WatchAlarmReceiver"
    }
}
