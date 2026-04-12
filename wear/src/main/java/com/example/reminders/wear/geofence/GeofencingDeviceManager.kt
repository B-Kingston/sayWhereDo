package com.example.reminders.wear.geofence

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.reminders.wear.data.WatchReminder
import com.example.reminders.wear.data.WatchReminderDao
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.NodeClient
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Manages the geofencing device preference and auto-switch logic.
 *
 * When the preference is [GeofencingDevice.AUTO], this manager monitors
 * phone connectivity and migrates geofences between watch and phone:
 *
 * - **Phone disconnects**: All active geofences are registered on the watch.
 * - **Phone reconnects**: Geofences are handed back to the phone.
 *
 * The device preference is persisted in DataStore.
 */
class GeofencingDeviceManager(
    private val context: Context,
    private val watchReminderDao: WatchReminderDao,
    private val watchGeofenceManager: WatchGeofenceManager,
    private val geofencePendingIntent: android.app.PendingIntent
) {

    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val nodeClient: NodeClient = Wearable.getNodeClient(context)
    private val capabilityClient: CapabilityClient = Wearable.getCapabilityClient(context)

    private val Context.geofenceDataStore: DataStore<Preferences> by preferencesDataStore(
        name = "geofencing_prefs"
    )

    /**
     * Flow of the current [GeofencingDevice] preference.
     */
    val devicePreference: Flow<GeofencingDevice> =
        context.geofenceDataStore.data.map { prefs ->
            val name = prefs[GEOFENCING_DEVICE_KEY] ?: GeofencingDevice.AUTO.name
            runCatching { GeofencingDevice.valueOf(name) }.getOrDefault(GeofencingDevice.AUTO)
        }

    /**
     * Updates the geofencing device preference.
     */
    suspend fun setDevicePreference(device: GeofencingDevice) {
        context.geofenceDataStore.edit { prefs ->
            prefs[GEOFENCING_DEVICE_KEY] = device.name
        }
    }

    /**
     * Starts monitoring phone connectivity for auto-switch behavior.
     *
     * Should be called once during application startup.
     */
    fun startMonitoring() {
        capabilityClient.addListener(
            { onPhoneConnectivityChanged() },
            PHONE_CAPABILITY
        )

        // Check initial state
        coroutineScope.launch {
            val preference = devicePreference.first()
            if (preference == GeofencingDevice.AUTO) {
                val phoneConnected = isPhoneConnected()
                if (!phoneConnected) {
                    migrateGeofencesToWatch()
                }
            }
        }
    }

    /**
     * Returns true if the phone is currently connected via the Wearable Data Layer.
     */
    private suspend fun isPhoneConnected(): Boolean {
        return try {
            val capabilityInfo = capabilityClient
                .getCapability(PHONE_CAPABILITY, CapabilityClient.FILTER_REACHABLE)
                .await()
            capabilityInfo.nodes.isNotEmpty()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to check phone connectivity", e)
            false
        }
    }

    private fun onPhoneConnectivityChanged() {
        coroutineScope.launch {
            val preference = devicePreference.first()
            if (preference != GeofencingDevice.AUTO) return@launch

            val phoneConnected = isPhoneConnected()
            if (phoneConnected) {
                Log.i(TAG, "Phone reconnected, migrating geofences back to phone")
                removeAllFromWatch()
            } else {
                Log.i(TAG, "Phone disconnected, migrating geofences to watch")
                migrateGeofencesToWatch()
            }
        }
    }

    /**
     * Registers all active geofence reminders on the watch.
     *
     * Called when the phone disconnects and preference is AUTO or WATCH_ONLY.
     * This operation is idempotent — re-running produces the same result.
     */
    private suspend fun migrateGeofencesToWatch() {
        val reminders = getGeofencedReminders()
        if (reminders.isEmpty()) return

        var successCount = 0
        var failureCount = 0

        for (reminder in reminders) {
            val result = watchGeofenceManager.registerGeofence(reminder, geofencePendingIntent)
            if (result.isSuccess) {
                successCount++
                Log.d(TAG, "Migrated geofence to watch for ${reminder.id}")
            } else {
                failureCount++
                Log.e(TAG, "Failed to migrate geofence for ${reminder.id}: ${result.exceptionOrNull()?.message}")
            }
        }

        Log.i(TAG, "Migration to watch: $successCount success, $failureCount failed")
    }

    /**
     * Removes all geofences from the watch (called when phone takes over).
     */
    suspend fun removeAllFromWatch() {
        val reminders = getGeofencedReminders()
        val geofenceIds = reminders.mapNotNull { reminder ->
            reminder.locationTriggerJson?.let { json ->
                parseLocationTrigger(json)?.geofenceId ?: reminder.id
            }
        }

        if (geofenceIds.isNotEmpty()) {
            watchGeofenceManager.removeAllGeofences(geofenceIds)
        }
    }

    /**
     * Queries the local Room DB for all reminders that have an active geofence.
     */
    private suspend fun getGeofencedReminders(): List<WatchReminder> {
        return watchReminderDao.getAllGeofencedOnce()
    }

    private fun parseLocationTrigger(json: String): WatchLocationTrigger? {
        return try {
            Json.decodeFromString<WatchLocationTrigger>(json)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Awaits a Google Play Services Task using coroutines.
     *
     * Properly removes listeners on cancellation to prevent leaks.
     */
    private suspend fun <T> com.google.android.gms.tasks.Task<T>.await(): T =
        kotlinx.coroutines.suspendCancellableCoroutine { cont ->
            val task = this
            val successListener = com.google.android.gms.tasks.OnSuccessListener<T> {
                cont.resume(it)
            }
            val failureListener = com.google.android.gms.tasks.OnFailureListener {
                cont.resumeWithException(it)
            }
            task.addOnSuccessListener(successListener)
            task.addOnFailureListener(failureListener)

            cont.invokeOnCancellation {
                task.removeOnSuccessListener(successListener)
                task.removeOnFailureListener(failureListener)
            }
        }

    companion object {
        private const val TAG = "GeofencingDeviceMgr"
        private val GEOFENCING_DEVICE_KEY = stringPreferencesKey("geofencing_device")
        private const val PHONE_CAPABILITY = "geofence_phone"
    }
}
