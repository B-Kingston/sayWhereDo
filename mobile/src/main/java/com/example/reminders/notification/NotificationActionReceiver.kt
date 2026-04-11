package com.example.reminders.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.reminders.alarm.AndroidAlarmScheduler
import com.example.reminders.alarm.computeNextOccurrence
import com.example.reminders.alarm.RecurrencePattern
import com.example.reminders.data.model.LocationReminderState
import com.example.reminders.di.RemindersApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.Instant

/**
 * Handles notification action button presses for time-based reminders.
 *
 * **Supported actions:**
 * - [ACTION_COMPLETE] — Marks the reminder as completed (all users)
 * - [ACTION_SNOOZE_5MIN] — Reschedules the alarm 5 minutes from now (Pro only)
 * - [ACTION_SNOOZE_15MIN] — Reschedules the alarm 15 minutes from now (Pro only)
 * - [ACTION_SNOOZE_1HR] — Reschedules the alarm 1 hour from now (Pro only)
 * - [ACTION_DISMISS] — Cancels the notification without state change (all users)
 *
 * Snooze actions are only included in notifications for Pro users, so
 * receiving a snooze action from a free user should not occur. This
 * receiver still defensively checks Pro status before processing snooze.
 */
class NotificationActionReceiver : BroadcastReceiver() {

    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getStringExtra(EXTRA_REMINDER_ID)
        if (reminderId == null) {
            Log.e(TAG, "Missing reminder ID in action intent")
            return
        }

        val notificationManager = ReminderNotificationManager(context)
        notificationManager.cancelNotification(reminderId)

        val pendingResult = goAsync()

        coroutineScope.launch {
            try {
                val container = (context.applicationContext as RemindersApplication).container

                when (intent.action) {
                    ACTION_COMPLETE -> handleComplete(container, reminderId)
                    ACTION_SNOOZE_5MIN -> handleSnooze(container, reminderId, SNOOZE_5_MINUTES_MS)
                    ACTION_SNOOZE_15MIN -> handleSnooze(container, reminderId, SNOOZE_15_MINUTES_MS)
                    ACTION_SNOOZE_1HR -> handleSnooze(container, reminderId, SNOOZE_1_HOUR_MS)
                    ACTION_DISMISS -> Log.d(TAG, "Reminder $reminderId dismissed")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling notification action for $reminderId", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    /**
     * Marks the reminder as completed in Room, then runs the full
     * completion flow (geofence removal, alarm cancellation, sync).
     */
    private suspend fun handleComplete(
        container: com.example.reminders.di.AppContainer,
        reminderId: String
    ) {
        container.reminderCompletionManager.completeReminder(reminderId)
        Log.i(TAG, "Completed reminder $reminderId via notification action")
    }

    /**
     * Reschedules the alarm for [snoozeDurationMs] milliseconds from now.
     *
     * Only processes snooze for Pro users — defensively re-checks
     * Pro status even though snooze actions should not appear in
     * free-user notifications.
     */
    private suspend fun handleSnooze(
        container: com.example.reminders.di.AppContainer,
        reminderId: String,
        snoozeDurationMs: Long
    ) {
        val isPro = container.billingManager.isPro.value
        if (!isPro) {
            Log.w(TAG, "Ignoring snooze action for free user on reminder $reminderId")
            return
        }

        val reminder = container.reminderRepository.getReminderById(reminderId)
        if (reminder == null || reminder.isCompleted) {
            Log.d(TAG, "Cannot snooze: reminder $reminderId not found or completed")
            return
        }

        val snoozedTime = Instant.now().plusMillis(snoozeDurationMs)
        val updated = reminder.copy(
            triggerTime = snoozedTime,
            updatedAt = Instant.now()
        )

        container.reminderRepository.update(updated)
        container.alarmScheduler.scheduleAlarm(updated)

        Log.i(TAG, "Snoozed reminder $reminderId until $snoozedTime")
    }

    companion object {
        const val ACTION_COMPLETE = "com.example.reminders.action.COMPLETE"
        const val ACTION_SNOOZE_5MIN = "com.example.reminders.action.SNOOZE_5MIN"
        const val ACTION_SNOOZE_15MIN = "com.example.reminders.action.SNOOZE_15MIN"
        const val ACTION_SNOOZE_1HR = "com.example.reminders.action.SNOOZE_1HR"
        const val ACTION_DISMISS = "com.example.reminders.action.DISMISS"
        const val EXTRA_REMINDER_ID = "reminder_id"

        private const val SNOOZE_5_MINUTES_MS = 5 * 60 * 1_000L
        private const val SNOOZE_15_MINUTES_MS = 15 * 60 * 1_000L
        private const val SNOOZE_1_HOUR_MS = 60 * 60 * 1_000L

        private const val TAG = "NotificationAction"
    }
}
