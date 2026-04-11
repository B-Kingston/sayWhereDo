package com.example.reminders.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.reminders.data.model.LocationReminderState
import com.example.reminders.data.model.LocationTrigger
import com.example.reminders.data.model.Reminder
import com.example.reminders.data.preferences.UsageTracker
import com.example.reminders.data.repository.ReminderRepository
import com.example.reminders.di.RemindersApplication
import com.example.reminders.notification.ReminderNotificationManager
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Receives geofence transition events from Play Services.
 *
 * When the user enters a geofenced area (with 30s loitering delay),
 * this receiver:
 *
 * 1. Identifies the triggered reminder(s) by geofence request ID.
 * 2. Updates each reminder's [LocationReminderState] to [TRIGGERED][LocationReminderState.TRIGGERED].
 * 3. Posts a notification via [ReminderNotificationManager].
 *
 * Uses [goAsync] to extend the receiver's lifecycle while database
 * operations complete.
 */
class GeofenceBroadcastReceiver : BroadcastReceiver() {

    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent == null) {
            Log.e(TAG, "Null geofencing event")
            return
        }

        if (geofencingEvent.hasError()) {
            Log.e(TAG, "Geofencing error: ${geofencingEvent.errorCode}")
            return
        }

        val transition = geofencingEvent.geofenceTransition
        if (transition != Geofence.GEOFENCE_TRANSITION_ENTER &&
            transition != Geofence.GEOFENCE_TRANSITION_DWELL
        ) {
            Log.d(TAG, "Ignoring transition type: $transition")
            return
        }

        val triggeredIds = geofencingEvent.triggeringGeofences
            ?.map { it.requestId }
            ?: emptyList()

        if (triggeredIds.isEmpty()) {
            Log.d(TAG, "No triggering geofence IDs")
            return
        }

        // goAsync allows extending the receiver's lifecycle for async work
        val pendingResult = goAsync()

        val container = (context.applicationContext as RemindersApplication).container
        val notificationManager = ReminderNotificationManager(context)

        coroutineScope.launch {
            try {
                for (geofenceId in triggeredIds) {
                    handleTriggeredGeofence(
                        container.reminderRepository,
                        notificationManager,
                        geofenceId
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling geofence transition", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    /**
     * Looks up the reminder associated with [geofenceId] and triggers it.
     *
     * The reminder is located by searching all reminders whose
     * [LocationTrigger.geofenceId] or [Reminder.id] matches the geofence ID.
     */
    private suspend fun handleTriggeredGeofence(
        repository: ReminderRepository,
        notificationManager: ReminderNotificationManager,
        geofenceId: String
    ) {
        // Try to find by geofence ID in locationTrigger JSON, fallback to reminder ID
        val reminder = repository.getByGeofenceId(geofenceId)
            ?: repository.getReminderById(geofenceId)
            ?: run {
                Log.w(TAG, "No reminder found for geofence ID: $geofenceId")
                return
            }

        if (reminder.isCompleted) {
            Log.d(TAG, "Reminder ${reminder.id} is already completed, skipping")
            return
        }

        val updated = reminder.copy(
            locationState = LocationReminderState.TRIGGERED,
            updatedAt = java.time.Instant.now()
        )
        repository.update(updated)

        val locationLabel = reminder.locationTrigger?.placeLabel ?: "Location"
        notificationManager.showLocationReminderNotification(
            reminderId = reminder.id,
            title = reminder.title,
            body = "You've arrived at $locationLabel",
            placeLabel = locationLabel
        )

        Log.i(TAG, "Triggered location reminder: ${reminder.title}")
    }

    companion object {
        private const val TAG = "GeofenceReceiver"
    }
}
