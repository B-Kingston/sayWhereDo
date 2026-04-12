package com.example.reminders.wear.geofence

import android.content.pm.PackageManager
import android.util.Log

/**
 * Detects whether the watch has GPS hardware available.
 *
 * Watches without GPS cannot run geofences locally and must
 * delegate to the phone. This check uses [PackageManager.hasSystemFeature]
 * with [PackageManager.FEATURE_LOCATION_GPS].
 */
class GpsDetector(private val packageManager: PackageManager) {

    /**
     * Returns `true` if this watch has a GPS chipset.
     * GPS-less watches must default to [GeofencingDevice.PHONE_ONLY].
     */
    fun hasGps(): Boolean {
        val result = packageManager.hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS)
        Log.i(TAG, "GPS hardware available: $result")
        return result
    }

    companion object {
        private const val TAG = "GpsDetector"
    }
}
