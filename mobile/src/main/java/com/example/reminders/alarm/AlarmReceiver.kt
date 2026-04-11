package com.example.reminders.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.reminders.data.model.Reminder
import com.example.reminders.di.RemindersApplication
import com.example.reminders.notification.ReminderNotificationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.Instant

/**
 * Receives alarm broadcasts for time-based reminders.
 *
 * When triggered, this receiver:
 *
 * 1. Looks up the reminder by ID from Room.
 * 2. Posts a notification with Complete / Snooze / Dismiss actions.
 * 3. If the reminder has a recurrence pattern, computes the next
 *    occurrence, updates Room, and schedules the next alarm.
 *
 * Uses [goAsync] to extend the receiver's lifecycle while async
 * database operations complete.
 */
class AlarmReceiver : BroadcastReceiver() {

    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != AlarmScheduler.Companion.ACTION_TRIGGER_REMINDER) return

        val reminderId = intent.getStringExtra(AlarmScheduler.Companion.EXTRA_REMINDER_ID)
        if (reminderId == null) {
            Log.e(TAG, "Missing reminder ID in alarm intent")
            return
        }

        val pendingResult = goAsync()

        coroutineScope.launch {
            try {
                val container = (context.applicationContext as RemindersApplication).container
                val reminder = container.reminderRepository.getReminderById(reminderId)

                if (reminder == null) {
                    Log.w(TAG, "No reminder found for alarm: $reminderId")
                    return@launch
                }

                if (reminder.isCompleted) {
                    Log.d(TAG, "Reminder $reminderId already completed, skipping")
                    return@launch
                }

                postNotification(context, reminder, container.billingManager.isPro.value)

                handleRecurrence(container, reminder)
            } catch (e: Exception) {
                Log.e(TAG, "Error handling alarm for $reminderId", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    /**
     * Posts a high-priority notification for the triggered reminder.
     *
     * Action buttons are conditional on Pro status:
     * - **All users**: Complete, Dismiss
     * - **Pro only**: Snooze 5 min, Snooze 15 min, Snooze 1 hr
     */
    private suspend fun postNotification(
        context: Context,
        reminder: Reminder,
        isPro: Boolean
    ) {
        val notificationManager = ReminderNotificationManager(context)
        notificationManager.showTimeReminderNotification(
            reminderId = reminder.id,
            title = reminder.title,
            body = reminder.body ?: reminder.sourceTranscript,
            isPro = isPro
        )
        Log.i(TAG, "Posted notification for reminder: ${reminder.title}")
    }

    /**
     * If the reminder has a recurrence pattern, computes the next trigger
     * time, updates Room, and schedules the follow-up alarm.
     *
     * Recurrence is a Pro feature — the recurrence field is only set for
     * Pro users, so no additional Pro check is needed here.
     */
    private suspend fun handleRecurrence(
        container: com.example.reminders.di.AppContainer,
        reminder: Reminder
    ) {
        val recurrence = reminder.recurrence ?: return
        val pattern = RecurrencePattern.fromString(recurrence) ?: return
        val currentTriggerTime = reminder.triggerTime ?: return

        val nextTime = computeNextOccurrence(currentTriggerTime, pattern)
        val updated = reminder.copy(
            triggerTime = nextTime,
            updatedAt = Instant.now()
        )

        container.reminderRepository.update(updated)
        container.alarmScheduler.scheduleAlarm(updated)

        Log.i(TAG, "Scheduled next recurrence for ${reminder.id} at $nextTime")
    }

    companion object {
        private const val TAG = "AlarmReceiver"
    }
}
