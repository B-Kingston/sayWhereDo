package com.example.reminders.wear.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.reminders.wear.di.WatchRemindersApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class WatchNotificationActionReceiver : BroadcastReceiver() {

    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getStringExtra(EXTRA_REMINDER_ID)
        if (reminderId == null) {
            Log.e(TAG, "Missing reminder ID in action intent")
            return
        }

        val notificationManager = WatchNotificationManager(context)
        notificationManager.cancelNotification(reminderId)

        val pendingResult = goAsync()

        coroutineScope.launch {
            try {
                val container = (context.applicationContext as WatchRemindersApplication).container

                when (intent.action) {
                    ACTION_COMPLETE -> {
                        container.watchReminderCompletionManager.completeReminder(reminderId)
                        Log.i(TAG, "Completed reminder $reminderId via notification action")
                    }
                    ACTION_DISMISS -> {
                        Log.d(TAG, "Reminder $reminderId dismissed")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling notification action for $reminderId", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_COMPLETE = "com.example.reminders.wear.action.COMPLETE"
        const val ACTION_DISMISS = "com.example.reminders.wear.action.DISMISS"
        const val EXTRA_REMINDER_ID = "reminder_id"

        private const val TAG = "WatchNotificationAction"
    }
}
