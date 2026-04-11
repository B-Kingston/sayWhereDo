package com.example.reminders.data.model

import java.time.Instant

data class ParsedReminder(
    val title: String,
    val body: String? = null,
    val triggerTime: Instant? = null,
    val recurrence: String? = null,
    val locationTrigger: LocationTrigger? = null
)
