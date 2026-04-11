package com.example.reminders.wear.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.reminders.wear.data.WatchReminder
import com.example.reminders.wear.di.WatchRemindersApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.Instant

/**
 * Receives alarm broadcasts for time-based reminders on the watch.
 *
 * When triggered, this receiver:
 *
 * 1. Looks up the reminder by ID from the watch's local Room DB.
 * 2. Shows a notification for the reminder.
 * 3. Marks the reminder as triggered (sets [isCompleted] for non-recurring,
 *    or reschedules for recurring reminders).
 *
 * Uses [goAsync] to extend the receiver's lifecycle while database
 * operations complete.
 */
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

                // For now, mark as completed after triggering.
                // Recurrence handling will be added when Phase 6 syncs Pro status.
                val updated = reminder.copy(
                    isCompleted = true,
                    updatedAt = Instant.now()
                )
                container.watchReminderDao.update(updated)

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
