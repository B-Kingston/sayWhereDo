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

/**
 * Manages notification channels and reminder notifications.
 *
 * Creates the required notification channels on init and provides
 * methods to show location-triggered reminder notifications.
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

        // Use the hash code of the reminder ID as the notification ID
        @Suppress("MissingPermission")
        notificationManager.notify(reminderId.hashCode(), notification)
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

            val notificationManagerSystem =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManagerSystem.createNotificationChannel(locationChannel)
        }
    }

    companion object {
        /** Channel ID for location-triggered reminder notifications. */
        const val CHANNEL_LOCATION_REMINDERS = "location_reminders"
    }
}
