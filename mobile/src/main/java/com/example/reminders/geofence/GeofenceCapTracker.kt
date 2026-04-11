package com.example.reminders.geofence

import kotlinx.coroutines.flow.StateFlow

/**
 * Enforces geofence registration limits based on the user's Pro status.
 *
 * Free users are capped at [FREE_CAP] active geofences; Pro users at [PRO_CAP].
 * A warning threshold is provided so the UI can alert the user before
 * hitting the hard limit.
 */
class GeofenceCapTracker(
    private val isProFlow: StateFlow<Boolean>
) {

    /**
     * Represents the result of a cap check.
     */
    sealed interface CapCheckResult {

        /** Registration is allowed. [remaining] slots are left after this one. */
        data class Allowed(val remaining: Int) : CapCheckResult

        /** Approaching the cap — warn the user. [remaining] slots left. */
        data class Warning(val remaining: Int) : CapCheckResult

        /** Hard cap reached — registration is blocked. */
        data object Blocked : CapCheckResult
    }

    /**
     * Checks whether a new geofence can be registered given the [currentCount]
     * of active geofences and the user's Pro status.
     */
    fun checkCap(currentCount: Int): CapCheckResult {
        val isPro = isProFlow.value
        val cap = if (isPro) PRO_CAP else FREE_CAP
        val warnThreshold = if (isPro) PRO_WARN_THRESHOLD else FREE_WARN_THRESHOLD

        if (currentCount >= cap) {
            return CapCheckResult.Blocked
        }

        val remaining = cap - currentCount - 1

        return if (currentCount >= warnThreshold) {
            CapCheckResult.Warning(remaining)
        } else {
            CapCheckResult.Allowed(remaining)
        }
    }

    /**
     * Returns the maximum number of geofences the current user can have active.
     */
    fun getMaxCap(): Int = if (isProFlow.value) PRO_CAP else FREE_CAP

    companion object {
        /** Maximum active geofences for free users. */
        const val FREE_CAP = 5

        /** Maximum active geofences for Pro users. */
        const val PRO_CAP = 100

        /** Warn when free user has this many active geofences. */
        const val FREE_WARN_THRESHOLD = 4

        /** Warn when Pro user has this many active geofences. */
        const val PRO_WARN_THRESHOLD = 90
    }
}
