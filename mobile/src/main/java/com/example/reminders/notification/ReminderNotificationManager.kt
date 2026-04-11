package com.example.reminders.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.reminders.MainActivity
import com.example.reminders.R
import com.example.reminders.alarm.AndroidAlarmScheduler

/**
 * Manages notification channels and reminder notifications.
 *
 * Creates the required notification channels on init and provides
 * methods to show location-triggered and time-triggered reminder
 * notifications with optional action buttons.
 */
class ReminderNotificationManager(private val context: Context) {

    private val notificationManager = NotificationManagerCompat.from(context)

    init {
        createNotificationChannels()
    }

    /**
     * Shows a notification for a location-triggered reminder.
     *
     * @param reminderId  Unique ID for the reminder (used as notification ID).
     * @param title       Reminder title.
     * @param body        Notification body text.
     * @param placeLabel  The location name that triggered the reminder.
     */
    fun showLocationReminderNotification(
        reminderId: String,
        title: String,
        body: String,
        placeLabel: String
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_REMINDER_ID, reminderId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            reminderId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_LOCATION_REMINDERS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(body)
                    .setBigContentTitle(title)
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        @Suppress("MissingPermission")
        notificationManager.notify(reminderId.hashCode(), notification)
    }

    /**
     * Shows a notification for a time-triggered reminder with action buttons.
     *
     * **Action buttons are conditional on Pro status:**
     * - All users: Complete, Dismiss
     * - Pro only: Snooze 5 min, Snooze 15 min, Snooze 1 hr
     *
     * Snooze actions are excluded from the notification entirely for free
     * users — they are not merely hidden.
     *
     * @param reminderId  Unique ID for the reminder (used as notification ID).
     * @param title       Reminder title.
     * @param body        Notification body text.
     * @param isPro       Whether the user has Pro status.
     */
    fun showTimeReminderNotification(
        reminderId: String,
        title: String,
        body: String,
        isPro: Boolean
    ) {
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_REMINDER_ID, reminderId)
        }

        val contentPendingIntent = PendingIntent.getActivity(
            context,
            reminderId.hashCode(),
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val completeAction = createActionPendingIntent(
            context, reminderId, NotificationActionReceiver.ACTION_COMPLETE
        )
        val dismissAction = createActionPendingIntent(
            context, reminderId, NotificationActionReceiver.ACTION_DISMISS
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_TIME_REMINDERS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(body)
                    .setBigContentTitle(title)
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .addAction(
                R.drawable.ic_notification,
                context.getString(R.string.notification_action_complete),
                completeAction
            )

        // Snooze actions are Pro-only — excluded from notification entirely for free users
        if (isPro) {
            val snooze5 = createActionPendingIntent(
                context, reminderId, NotificationActionReceiver.ACTION_SNOOZE_5MIN
            )
            val snooze15 = createActionPendingIntent(
                context, reminderId, NotificationActionReceiver.ACTION_SNOOZE_15MIN
            )
            val snooze1hr = createActionPendingIntent(
                context, reminderId, NotificationActionReceiver.ACTION_SNOOZE_1HR
            )

            builder.addAction(
                R.drawable.ic_notification,
                context.getString(R.string.notification_action_snooze_5min),
                snooze5
            )
            builder.addAction(
                R.drawable.ic_notification,
                context.getString(R.string.notification_action_snooze_15min),
                snooze15
            )
            builder.addAction(
                R.drawable.ic_notification,
                context.getString(R.string.notification_action_snooze_1hr),
                snooze1hr
            )
        }

        builder.addAction(
            R.drawable.ic_notification,
            context.getString(R.string.notification_action_dismiss),
            dismissAction
        )

        @Suppress("MissingPermission")
        notificationManager.notify(reminderId.hashCode(), builder.build())
    }

    /**
     * Cancels a previously shown notification.
     *
     * @param reminderId The reminder ID whose notification should be cancelled.
     */
    fun cancelNotification(reminderId: String) {
        notificationManager.cancel(reminderId.hashCode())
    }

    /**
     * Creates all required notification channels.
     *
     * Called during initialization. Safe to call multiple times —
     * the system ignores channel creation when the channel already exists.
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val locationChannel = NotificationChannel(
                CHANNEL_LOCATION_REMINDERS,
                context.getString(R.string.notification_channel_location_reminders),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notification_channel_location_desc)
                enableVibration(true)
                enableLights(true)
            }

            val timeChannel = NotificationChannel(
                CHANNEL_TIME_REMINDERS,
                context.getString(R.string.notification_channel_time_reminders),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notification_channel_time_desc)
                enableVibration(true)
                enableLights(true)
            }

            val notificationManagerSystem =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManagerSystem.createNotificationChannel(locationChannel)
            notificationManagerSystem.createNotificationChannel(timeChannel)
        }
    }

    /**
     * Creates a [PendingIntent] for a notification action targeting
     * [NotificationActionReceiver].
     */
    private fun createActionPendingIntent(
        context: Context,
        reminderId: String,
        action: String
    ): PendingIntent {
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            this.action = action
            putExtra(NotificationActionReceiver.EXTRA_REMINDER_ID, reminderId)
        }

        return PendingIntent.getBroadcast(
            context,
            reminderId.hashCode() + action.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        /** Channel ID for location-triggered reminder notifications. */
        const val CHANNEL_LOCATION_REMINDERS = "location_reminders"

        /** Channel ID for time-triggered reminder notifications. */
        const val CHANNEL_TIME_REMINDERS = "time_reminders"
    }
}
