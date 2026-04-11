package com.example.reminders.wear.geofence

import android.content.pm.PackageManager

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
        return packageManager.hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS)
    }
}
