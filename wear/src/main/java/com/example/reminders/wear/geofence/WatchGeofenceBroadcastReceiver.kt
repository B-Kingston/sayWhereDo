package com.example.reminders.wear.geofence

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.reminders.wear.data.WatchReminder
import com.example.reminders.wear.data.WatchReminderDao
import com.example.reminders.wear.di.WatchRemindersApplication
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.Instant

/**
 * Receives geofence transition events on the watch.
 *
 * When the user enters a geofenced area (with 30s loitering delay),
 * this receiver:
 *
 * 1. Identifies the triggered reminder by geofence request ID.
 * 2. Updates the reminder's locationState to "TRIGGERED".
 * 3. Posts a local notification.
 *
 * Uses [goAsync] to extend the receiver's lifecycle while database
 * operations complete.
 */
class WatchGeofenceBroadcastReceiver : BroadcastReceiver() {

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

        val pendingResult = goAsync()
        val container = (context.applicationContext as WatchRemindersApplication).container

        coroutineScope.launch {
            try {
                for (geofenceId in triggeredIds) {
                    handleTriggeredGeofence(container.watchReminderDao, geofenceId)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling geofence transition", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    /**
     * Looks up the reminder associated with [geofenceId] and marks it as triggered.
     *
     * The geofence request ID may differ from the reminder's primary key when
     * a custom geofenceId is set in the LocationTrigger. Falls back to a JSON
     * search via [WatchReminderDao.findByGeofenceId] if the primary key lookup fails.
     */
    private suspend fun handleTriggeredGeofence(dao: WatchReminderDao, geofenceId: String) {
        val reminder = dao.getById(geofenceId)
            ?: dao.findByGeofenceId(geofenceId)
            ?: run {
                Log.w(TAG, "No reminder found for geofence ID: $geofenceId")
                return
            }

        if (reminder.isCompleted) {
            Log.d(TAG, "Reminder ${reminder.id} is already completed, skipping")
            return
        }

        val updated = reminder.copy(
            locationState = "TRIGGERED",
            updatedAt = Instant.now()
        )
        dao.update(updated)

        Log.i(TAG, "Triggered location reminder on watch: ${reminder.title}")
    }

    companion object {
        private const val TAG = "WatchGeofenceReceiver"
    }
}
