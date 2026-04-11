package com.example.reminders.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.reminders.data.model.Reminder
import com.example.reminders.data.repository.ReminderRepository
import com.example.reminders.di.RemindersApplication

/**
 * Schedules and cancels time-based reminder alarms via [AlarmManager].
 *
 * Uses [AlarmManager.setExactAndAllowWhileIdle] for precise delivery.
 * The app declares [android.Manifest.permission.USE_EXACT_ALARM] which
 * is auto-granted for reminder/alarm apps on API 33+.
 */
interface AlarmScheduler {

    /**
     * Schedules an exact alarm for [reminder.triggerTime].
     *
     * No-op if the reminder has no trigger time or is already completed.
     */
    fun scheduleAlarm(reminder: Reminder)

    /**
     * Cancels a previously scheduled alarm for the given [reminderId].
     *
     * Safe to call even if no alarm exists — silently ignores missing alarms.
     */
    fun cancelAlarm(reminderId: String)

    /**
     * Re-schedules alarms for all active timed reminders.
     *
     * Called after a device reboot to restore alarms cleared by the restart.
     */
    suspend fun rescheduleAll()
}

/**
 * Android [AlarmManager]-backed implementation of [AlarmScheduler].
 */
class AndroidAlarmScheduler(
    private val context: Context,
    private val reminderRepository: ReminderRepository
) : AlarmScheduler {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    override fun scheduleAlarm(reminder: Reminder) {
        val triggerTime = reminder.triggerTime
        if (triggerTime == null || reminder.isCompleted) {
            Log.d(TAG, "Skipping alarm for reminder ${reminder.id}: no trigger time or completed")
            return
        }

        val triggerMillis = triggerTime.toEpochMilli()

        // Don't schedule in the past
        if (triggerMillis <= System.currentTimeMillis()) {
            Log.d(TAG, "Skipping alarm for reminder ${reminder.id}: trigger time is in the past")
            return
        }

        val pendingIntent = createAlarmPendingIntent(reminder.id)

        // Check if exact alarms are permitted (API 31+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.w(TAG, "Exact alarms not permitted, falling back to inexact for ${reminder.id}")
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerMillis,
                    pendingIntent
                )
                return
            }
        }

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerMillis,
            pendingIntent
        )

        Log.i(TAG, "Scheduled alarm for reminder ${reminder.id} at $triggerTime")
    }

    override fun cancelAlarm(reminderId: String) {
        val pendingIntent = createAlarmPendingIntent(reminderId, isCreate = false)
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d(TAG, "Cancelled alarm for reminder $reminderId")
        }
    }

    override suspend fun rescheduleAll() {
        Log.i(TAG, "Starting alarm re-scheduling")
        val timedReminders = reminderRepository.getTimedRemindersOnce()

        if (timedReminders.isEmpty()) {
            Log.d(TAG, "No timed reminders to re-schedule")
            return
        }

        var scheduledCount = 0
        for (reminder in timedReminders) {
            if (reminder.triggerTime != null &&
                reminder.triggerTime.toEpochMilli() > System.currentTimeMillis()
            ) {
                scheduleAlarm(reminder)
                scheduledCount++
            }
        }

        Log.i(TAG, "Re-scheduled $scheduledCount alarms")
    }

    /**
     * Creates a [PendingIntent] targeting [AlarmReceiver] for the given [reminderId].
     *
     * @param isCreate When `false`, uses [PendingIntent.FLAG_NO_CREATE] to retrieve
     *                 an existing PendingIntent without creating a new one.
     */
    private fun createAlarmPendingIntent(
        reminderId: String,
        isCreate: Boolean = true
    ): PendingIntent? {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_TRIGGER_REMINDER
            putExtra(EXTRA_REMINDER_ID, reminderId)
        }

        val flags = if (isCreate) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        }

        return PendingIntent.getBroadcast(
            context,
            reminderId.hashCode(),
            intent,
            flags
        )
    }

    companion object {
        private const val TAG = "AlarmScheduler"
        const val ACTION_TRIGGER_REMINDER = "com.example.reminders.action.TRIGGER_REMINDER"
        const val EXTRA_REMINDER_ID = "reminder_id"
    }
}
