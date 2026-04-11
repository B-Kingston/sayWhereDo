package com.example.reminders.data.model

enum class LocationReminderState {
    PENDING_GEOCODING,
    NEEDS_CONFIRMATION,
    ACTIVE,
    TRIGGERED,
    COMPLETED
}
