package com.example.reminders.wearable

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Monitors the connectivity state between the phone and a paired WearOS watch.
 *
 * Uses the Wearable [CapabilityClient] to detect nodes that declare the
 * [DataLayerPaths.CAPABILITY_WATCH] capability. When at least one node is
 * reachable, the watch is considered connected.
 *
 * The [isWatchConnected] [Flow] starts as `false` until the initial
 * capability check completes. Call [startMonitoring] once (typically from
 * the Application or Activity) to begin listening for capability changes.
 */
class WatchConnectivityMonitor(context: Context) {

    private val appContext = context.applicationContext
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    private val _isWatchConnected = MutableStateFlow(false)
    val isWatchConnected: Flow<Boolean> = _isWatchConnected.asStateFlow()

    /**
     * Performs an initial capability check and registers a listener for
     * subsequent changes. Safe to call multiple times — the listener is
     * only registered once.
     */
    fun startMonitoring() {
        // Initial check
        coroutineScope.launch {
            try {
                val capabilityInfo = Wearable.getCapabilityClient(appContext)
                    .getCapability(
                        DataLayerPaths.CAPABILITY_WATCH,
                        CapabilityClient.FILTER_REACHABLE
                    )
                    .await()
                _isWatchConnected.value = capabilityInfo.nodes.isNotEmpty()
                Log.i(TAG, "Initial watch connectivity: ${_isWatchConnected.value}")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to check initial watch connectivity", e)
            }
        }

        // Listen for capability changes
        Wearable.getCapabilityClient(appContext).addListener(
            { capabilityInfo ->
                val connected = capabilityInfo.nodes.isNotEmpty()
                Log.i(TAG, "Watch connectivity changed: $connected")
                _isWatchConnected.value = connected
            },
            DataLayerPaths.CAPABILITY_WATCH
        )

        Log.d(TAG, "Watch connectivity monitoring started")
    }

    private suspend fun <T> com.google.android.gms.tasks.Task<T>.await(): T =
        suspendCancellableCoroutine { cont ->
            addOnSuccessListener { result -> cont.resume(result) }
            addOnFailureListener { exception -> cont.resumeWithException(exception) }
        }

    companion object {
        private const val TAG = "WatchConnectivityMonitor"
    }
}
