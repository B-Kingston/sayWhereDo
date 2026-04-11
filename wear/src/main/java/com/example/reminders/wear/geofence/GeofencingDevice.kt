package com.example.reminders.wear.geofence

/**
 * Determines which device manages geofence registration.
 *
 * [AUTO] is the default — the system switches between phone and watch
 * based on phone connectivity. The user can override this to force
 * one device or the other.
 */
enum class GeofencingDevice {
    /** Phone when connected, watch when disconnected. */
    AUTO,

    /** Always use phone (watch without GPS, or user choice). */
    PHONE_ONLY,

    /** Always use watch (requires GPS hardware). */
    WATCH_ONLY
}
