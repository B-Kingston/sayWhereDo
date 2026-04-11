package com.example.reminders.geofence

import android.app.PendingIntent
import android.content.Context
import com.example.reminders.data.model.LocationTrigger
import com.example.reminders.data.model.Reminder
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Concrete [GeofenceManager] that wraps the Play Services [GeofencingClient].
 *
 * Each registered geofence uses [Geofence.GEOFENCE_TRANSITION_ENTER] and
 * [Geofence.GEOFENCE_TRANSITION_DWELL] with a 30-second loitering delay
 * to reduce false positives.
 *
 * @param context  Application or activity context.
 * @param geofencePendingIntent The [PendingIntent] that the system will fire
 *                               when a geofence transition occurs.
 */
class AndroidGeofenceManager(
    private val context: Context,
    private val geofencePendingIntent: PendingIntent
) : GeofenceManager {

    private val geofencingClient: GeofencingClient =
        LocationServices.getGeofencingClient(context)

    private val registeredGeofenceIds = mutableSetOf<String>()

    override suspend fun registerGeofence(reminder: Reminder): Result<String> {
        val trigger = reminder.locationTrigger
        requireNotNull(trigger) { "Reminder must have a LocationTrigger" }
        requireNotNull(trigger.latitude) { "Latitude must be resolved" }
        requireNotNull(trigger.longitude) { "Longitude must be resolved" }

        val geofenceId = trigger.geofenceId ?: reminder.id

        val geofence = Geofence.Builder()
            .setRequestId(geofenceId)
            .setCircularRegion(
                trigger.latitude,
                trigger.longitude,
                trigger.radiusMetres.toFloat()
            )
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(
                Geofence.GEOFENCE_TRANSITION_ENTER or
                    Geofence.GEOFENCE_TRANSITION_DWELL
            )
            .setLoiteringDelay(LOITERING_DELAY_MS)
            .build()

        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        return try {
            addGeofence(request)
            registeredGeofenceIds.add(geofenceId)
            Result.success(geofenceId)
        } catch (e: Exception) {
            val errorMessage = translateException(e)
            Result.failure(GeofenceException(errorMessage))
        }
    }

    override suspend fun removeGeofence(geofenceId: String): Result<Unit> {
        return try {
            removeGeofences(listOf(geofenceId))
            registeredGeofenceIds.remove(geofenceId)
            Result.success(Unit)
        } catch (e: Exception) {
            val errorMessage = translateException(e)
            Result.failure(GeofenceException(errorMessage))
        }
    }

    override suspend fun removeAllGeofences(geofenceIds: List<String>): Result<Unit> {
        return try {
            removeGeofences(geofenceIds)
            registeredGeofenceIds.removeAll(geofenceIds.toSet())
            Result.success(Unit)
        } catch (e: Exception) {
            val errorMessage = translateException(e)
            Result.failure(GeofenceException(errorMessage))
        }
    }

    override suspend fun getActiveGeofenceCount(): Int = registeredGeofenceIds.size

    /**
     * Tracks an externally-registered geofence (e.g. re-registered after boot).
     */
    fun markRegistered(geofenceId: String) {
        registeredGeofenceIds.add(geofenceId)
    }

    private suspend fun addGeofence(request: GeofencingRequest) =
        suspendCancellableCoroutine<Unit> { cont ->
            geofencingClient.addGeofences(request, geofencePendingIntent)
                .run { addOnSuccessListener { cont.resume(Unit) } }
                .run { addOnFailureListener { cont.cancel(it) } }
        }

    private suspend fun removeGeofences(ids: List<String>) =
        suspendCancellableCoroutine<Unit> { cont ->
            geofencingClient.removeGeofences(ids)
                .run { addOnSuccessListener { cont.resume(Unit) } }
                .run { addOnFailureListener { cont.cancel(it) } }
        }

    private fun translateException(e: Exception): String {
        return if (e is ApiException) {
            when (e.statusCode) {
                GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE ->
                    "Geofencing is not available. Ensure location is turned on."
                GeofenceStatusCodes.GEOFENCE_TOO_MANY_GEOFENCES ->
                    "Too many active geofences."
                GeofenceStatusCodes.GEOFENCE_TOO_MANY_PENDING_INTENTS ->
                    "Too many pending geofence intents."
                else -> "Geofence error: ${e.message}"
            }
        } else {
            "Geofence error: ${e.message}"
        }
    }

    companion object {
        /** 30-second loitering delay before triggering DWELL transition. */
        const val LOITERING_DELAY_MS = 30_000
    }
}

/**
 * Thrown when a geofence operation fails.
 */
class GeofenceException(message: String) : Exception(message)
