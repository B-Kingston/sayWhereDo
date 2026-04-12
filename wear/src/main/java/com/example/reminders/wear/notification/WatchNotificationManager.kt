package com.example.reminders.wear.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.reminders.wear.R
import com.example.reminders.wear.presentation.MainActivity

class WatchNotificationManager(private val context: Context) {

    private val notificationManager = NotificationManagerCompat.from(context)

    init {
        createNotificationChannels()
    }

    fun showTimeReminderNotification(reminderId: String, title: String, body: String?) {
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_REMINDER_ID, reminderId)
        }

        val contentPendingIntent = PendingIntent.getActivity(
            context,
            reminderId.hashCode(),
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val completeAction = createActionPendingIntent(
            context, reminderId, WatchNotificationActionReceiver.ACTION_COMPLETE
        )
        val dismissAction = createActionPendingIntent(
            context, reminderId, WatchNotificationActionReceiver.ACTION_DISMISS
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_TIME_REMINDERS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body ?: title)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(body ?: title)
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
            .addAction(
                R.drawable.ic_notification,
                context.getString(R.string.notification_action_dismiss),
                dismissAction
            )
            .build()

        @Suppress("MissingPermission")
        notificationManager.notify(reminderId.hashCode(), notification)
    }

    fun showLocationReminderNotification(
        reminderId: String,
        title: String,
        body: String?,
        placeLabel: String
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_REMINDER_ID, reminderId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            reminderId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val completeAction = createActionPendingIntent(
            context, reminderId, WatchNotificationActionReceiver.ACTION_COMPLETE
        )
        val dismissAction = createActionPendingIntent(
            context, reminderId, WatchNotificationActionReceiver.ACTION_DISMISS
        )

        val displayBody = if (!body.isNullOrBlank()) {
            context.getString(R.string.location_notification_body, placeLabel, body)
        } else {
            context.getString(R.string.location_notification_title_only, placeLabel)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_LOCATION_REMINDERS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(displayBody)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(displayBody)
                    .setBigContentTitle(title)
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(
                R.drawable.ic_notification,
                context.getString(R.string.notification_action_complete),
                completeAction
            )
            .addAction(
                R.drawable.ic_notification,
                context.getString(R.string.notification_action_dismiss),
                dismissAction
            )
            .build()

        @Suppress("MissingPermission")
        notificationManager.notify(reminderId.hashCode(), notification)
    }

    fun cancelNotification(reminderId: String) {
        notificationManager.cancel(reminderId.hashCode())
    }

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

            val systemManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            systemManager.createNotificationChannel(locationChannel)
            systemManager.createNotificationChannel(timeChannel)
        }
    }

    private fun createActionPendingIntent(
        context: Context,
        reminderId: String,
        action: String
    ): PendingIntent {
        val intent = Intent(context, WatchNotificationActionReceiver::class.java).apply {
            this.action = action
            putExtra(WatchNotificationActionReceiver.EXTRA_REMINDER_ID, reminderId)
        }

        return PendingIntent.getBroadcast(
            context,
            reminderId.hashCode() + action.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        const val CHANNEL_LOCATION_REMINDERS = "location_reminders"
        const val CHANNEL_TIME_REMINDERS = "time_reminders"
        const val EXTRA_REMINDER_ID = "reminder_id"
    }
}
