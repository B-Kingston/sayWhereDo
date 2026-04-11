package com.example.reminders.geofence

import com.example.reminders.data.model.Reminder

/**
 * Manages geofence registration and removal.
 *
 * Implementations wrap [com.google.android.gms.location.GeofencingClient]
 * to register location-based triggers for reminders.
 */
interface GeofenceManager {

    /**
     * Registers a geofence for the given [reminder].
     *
     * The reminder must have a [com.example.reminders.data.model.LocationTrigger]
     * with non-null latitude, longitude, and a unique geofence ID.
     *
     * @return The geofence request ID on success, or an error message on failure.
     */
    suspend fun registerGeofence(reminder: Reminder): Result<String>

    /**
     * Removes the geofence identified by [geofenceId].
     *
     * @return Success or an error message on failure.
     */
    suspend fun removeGeofence(geofenceId: String): Result<Unit>

    /**
     * Removes all geofences identified by [geofenceIds].
     *
     * @return Success or an error message on failure.
     */
    suspend fun removeAllGeofences(geofenceIds: List<String>): Result<Unit>

    /**
     * Returns the number of currently active geofences managed by this instance.
     */
    suspend fun getActiveGeofenceCount(): Int
}
