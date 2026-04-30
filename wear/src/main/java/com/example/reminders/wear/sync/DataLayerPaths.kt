package com.example.reminders.wear.sync

object DataLayerPaths {
    const val REMINDERS_PREFIX = "/reminders"
    const val REMINDER_PATH = "/reminders/{id}"
    const val PRO_STATUS_PATH = "/pro-status"
    const val DEFERRED_FORMATTING_PATH = "/deferred-formatting"
    const val FULL_SYNC_PATH = "/full-sync"
    const val CREDENTIAL_SYNC_PATH = "/sync/credentials"
    const val CREDENTIAL_SYNC_PROVIDER_PATH = "/sync/credentials/provider"
    const val CREDENTIAL_SYNC_MODEL_PATH = "/sync/credentials/model"
    const val SYNC_STATE_REQUEST = "/sync/state-request"
    const val SYNC_STATE_RESPONSE = "/sync/state-response"
    const val SYNC_STATE_COMPLETE = "/sync/state-complete"
    const val SYNC_TOMBSTONE = "/sync/tombstone"
    const val CAPABILITY_PHONE = "phone_app"
    const val CAPABILITY_WATCH = "watch_app"

    const val AUDIO_STREAM_START_PATH = "/audio-stream/start"
    const val AUDIO_STREAM_DATA_PATH = "/audio-stream/data"
    const val AUDIO_STREAM_END_PATH = "/audio-stream/end"
    const val AUDIO_STREAM_RESULT_PATH = "/audio-stream/result"

    fun reminderPath(id: String) = "/reminders/$id"
}
