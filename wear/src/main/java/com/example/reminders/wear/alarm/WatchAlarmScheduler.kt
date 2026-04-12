package com.example.reminders.wear.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.reminders.wear.data.WatchReminder
import com.example.reminders.wear.data.WatchReminderDao
import java.time.Instant

/**
 * Schedules and cancels time-based reminder alarms on the watch.
 *
 * Uses [AlarmManager.setExactAndAllowWhileIdle] for precise delivery.
 * The watch declares [android.Manifest.permission.USE_EXACT_ALARM]
 * which is auto-granted for alarm/reminder apps on API 33+.
 *
 * This scheduler operates entirely on the watch's local Room DB,
 * enabling standalone time-based reminders without phone connectivity.
 */
interface WatchAlarmScheduler {

    /**
     * Schedules an exact alarm for [reminder.triggerTime].
     *
     * No-op if the reminder has no trigger time or is completed.
     */
    fun scheduleAlarm(reminder: WatchReminder)

    /**
     * Cancels a previously scheduled alarm for the given [reminderId].
     */
    fun cancelAlarm(reminderId: String)

    /**
     * Re-schedules alarms for all active timed reminders.
     * Called after device reboot to restore cleared alarms.
     */
    suspend fun rescheduleAll()
}

/**
 * Android [AlarmManager]-backed implementation of [WatchAlarmScheduler].
 */
class AndroidWatchAlarmScheduler(
    private val context: Context,
    private val watchReminderDao: WatchReminderDao
) : WatchAlarmScheduler {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    override fun scheduleAlarm(reminder: WatchReminder) {
        val triggerTime = reminder.triggerTime
        if (triggerTime == null || reminder.isCompleted) {
            Log.d(TAG, "Skipping alarm for reminder ${reminder.id}: no trigger time or completed")
            return
        }

        val triggerMillis = triggerTime.toEpochMilli()

        if (triggerMillis <= System.currentTimeMillis()) {
            Log.d(TAG, "Skipping alarm for reminder ${reminder.id}: trigger time is in the past")
            return
        }

        val pendingIntent = createAlarmPendingIntent(reminder.id)
        if (pendingIntent == null) {
            Log.e(TAG, "Failed to create PendingIntent for reminder ${reminder.id}")
            return
        }

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
        Log.i(TAG, "Starting watch alarm re-scheduling")

        val now = System.currentTimeMillis()
        val reminders = watchReminderDao.getTimedRemindersOnce()
        var scheduledCount = 0

        for (reminder in reminders) {
            val triggerTime = reminder.triggerTime
            if (triggerTime != null && triggerTime.toEpochMilli() > now) {
                scheduleAlarm(reminder)
                scheduledCount++
            }
        }

        Log.i(TAG, "Re-scheduled $scheduledCount watch alarms")
    }

    private fun createAlarmPendingIntent(
        reminderId: String,
        isCreate: Boolean = true
    ): PendingIntent? {
        val intent = Intent(context, WatchAlarmReceiver::class.java).apply {
            action = ACTION_TRIGGER_WATCH_REMINDER
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
        private const val TAG = "WatchAlarmScheduler"
        const val ACTION_TRIGGER_WATCH_REMINDER = "com.example.reminders.wear.action.TRIGGER_REMINDER"
        const val EXTRA_REMINDER_ID = "reminder_id"
    }
}
