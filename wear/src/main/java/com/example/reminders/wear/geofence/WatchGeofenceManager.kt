package com.example.reminders.wear.geofence

import com.example.reminders.wear.data.WatchReminder
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.Collections
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCancellableCoroutine

/**
 * Manages geofence registration on the watch via the Play Services [GeofencingClient].
 *
 * Each registered geofence uses [Geofence.GEOFENCE_TRANSITION_ENTER] and
 * [Geofence.GEOFENCE_TRANSITION_DWELL] with a 30-second loitering delay
 * to reduce false positives.
 */
class WatchGeofenceManager(
    private val geofencingClient: GeofencingClient
) {

    private val registeredGeofenceIds: MutableSet<String> =
        Collections.synchronizedSet(mutableSetOf())

    /**
     * Registers a geofence for the given [WatchReminder] that has location data.
     *
     * @param geofencePendingIntent The pending intent to fire on transition.
     * @return The geofence request ID on success, or a failure result.
     */
    suspend fun registerGeofence(
        reminder: WatchReminder,
        geofencePendingIntent: android.app.PendingIntent
    ): Result<String> {
        val trigger = reminder.locationTriggerJson?.let { parseLocationTrigger(it) }
        if (trigger == null || trigger.latitude == null || trigger.longitude == null) {
            return Result.failure(GeofenceException("Reminder has no valid location data"))
        }

        val geofenceId = trigger.geofenceId ?: reminder.id

        val geofence = Geofence.Builder()
            .setRequestId(geofenceId)
            .setCircularRegion(trigger.latitude, trigger.longitude, trigger.radiusMetres.toFloat())
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(
                Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_DWELL
            )
            .setLoiteringDelay(LOITERING_DELAY_MS)
            .build()

        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        return try {
            addGeofence(request, geofencePendingIntent)
            registeredGeofenceIds.add(geofenceId)
            Result.success(geofenceId)
        } catch (e: Exception) {
            Result.failure(GeofenceException(translateException(e)))
        }
    }

    /**
     * Removes the geofence with the given ID.
     */
    suspend fun removeGeofence(geofenceId: String): Result<Unit> {
        return try {
            removeGeofences(listOf(geofenceId))
            registeredGeofenceIds.remove(geofenceId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(GeofenceException(translateException(e)))
        }
    }

    /**
     * Removes all geofences with the given IDs.
     */
    suspend fun removeAllGeofences(geofenceIds: List<String>): Result<Unit> {
        return try {
            removeGeofences(geofenceIds)
            registeredGeofenceIds.removeAll(geofenceIds.toSet())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(GeofenceException(translateException(e)))
        }
    }

    /**
     * Returns the number of currently tracked active geofences.
     */
    fun getActiveGeofenceCount(): Int = registeredGeofenceIds.size

    /**
     * Tracks an externally-registered geofence (e.g. re-registered after boot).
     */
    fun markRegistered(geofenceId: String) {
        registeredGeofenceIds.add(geofenceId)
    }

    private suspend fun addGeofence(
        request: GeofencingRequest,
        pendingIntent: android.app.PendingIntent
    ) = suspendCancellableCoroutine<Unit> { cont ->
        geofencingClient.addGeofences(request, pendingIntent)
            .addOnSuccessListener { cont.resume(Unit) }
            .addOnFailureListener { cont.resumeWithException(it) }
    }

    private suspend fun removeGeofences(ids: List<String>) =
        suspendCancellableCoroutine<Unit> { cont ->
            geofencingClient.removeGeofences(ids)
                .addOnSuccessListener { cont.resume(Unit) }
                .addOnFailureListener { cont.resumeWithException(it) }
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

    private fun parseLocationTrigger(json: String): WatchLocationTrigger? {
        return try {
            Json.decodeFromString<WatchLocationTrigger>(json)
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        /** 30-second loitering delay before triggering DWELL transition. */
        const val LOITERING_DELAY_MS = 30_000
    }
}

/**
 * Lightweight data class for deserializing location trigger JSON stored in Room.
 * Kept separate from Room entities to avoid the KSP2 @Serializable/@Embedded bug.
 */
@Serializable
data class WatchLocationTrigger(
    val placeLabel: String,
    val rawAddress: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val radiusMetres: Int = 150,
    val triggerOnEnter: Boolean = true,
    val triggerOnExit: Boolean = false,
    val geofenceId: String? = null
)

/**
 * Thrown when a geofence operation fails on the watch.
 */
class GeofenceException(message: String) : Exception(message)
