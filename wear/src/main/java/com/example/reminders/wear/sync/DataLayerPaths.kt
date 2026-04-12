package com.example.reminders.wear.sync

object DataLayerPaths {
    const val REMINDERS_PREFIX = "/reminders"
    const val REMINDER_PATH = "/reminders/{id}"
    const val PRO_STATUS_PATH = "/pro-status"
    const val DEFERRED_FORMATTING_PATH = "/deferred-formatting"
    const val CAPABILITY_PHONE = "phone_app"

    fun reminderPath(id: String) = "/reminders/$id"
}
