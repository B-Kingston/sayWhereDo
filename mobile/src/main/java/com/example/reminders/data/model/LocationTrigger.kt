package com.example.reminders.data.model

import kotlinx.serialization.Serializable

@Serializable
data class LocationTrigger(
    val placeLabel: String,
    val rawAddress: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val radiusMetres: Int = 150,
    val triggerOnEnter: Boolean = true,
    val triggerOnExit: Boolean = false,
    val geofenceId: String? = null
)
