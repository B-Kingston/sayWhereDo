package com.example.reminders.wear.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.reminders.wear.di.WatchRemindersApplication
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.time.Instant

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
                    handleTriggeredGeofence(container, geofenceId)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling geofence transition", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun handleTriggeredGeofence(
        container: com.example.reminders.wear.di.WatchAppContainer,
        geofenceId: String
    ) {
        val dao = container.watchReminderDao
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

        val placeLabel = try {
            reminder.locationTriggerJson?.let {
                Json.decodeFromString<WatchLocationTrigger>(it).placeLabel
            }
        } catch (_: Exception) {
            null
        } ?: geofenceId

        container.watchNotificationManager.showLocationReminderNotification(
            reminderId = reminder.id,
            title = reminder.title,
            body = reminder.body,
            placeLabel = placeLabel
        )

        Log.i(TAG, "Triggered location reminder on watch: ${reminder.title}")
    }

    companion object {
        private const val TAG = "WatchGeofenceReceiver"
    }
}
