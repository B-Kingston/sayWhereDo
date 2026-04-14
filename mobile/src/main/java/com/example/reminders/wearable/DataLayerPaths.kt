package com.example.reminders.wearable

object DataLayerPaths {
    const val REMINDERS_PREFIX = "/reminders"
    const val REMINDER_PATH = "/reminders/{id}"
    const val PRO_STATUS_PATH = "/pro-status"
    const val DEFERRED_FORMATTING_PATH = "/deferred-formatting"
    const val FULL_SYNC_PATH = "/full-sync"
    const val SYNC_STATE_REQUEST = "/sync/state-request"
    const val SYNC_STATE_RESPONSE = "/sync/state-response"
    const val SYNC_STATE_COMPLETE = "/sync/state-complete"
    const val SYNC_TOMBSTONE = "/sync/tombstone"
    const val CAPABILITY_PHONE = "phone_app"
    const val CAPABILITY_WATCH = "watch_app"

    fun reminderPath(id: String) = "/reminders/$id"
}
