package com.example.reminders.sync

/**
 * Client for syncing reminder state changes to connected devices
 * via the Wearable Data Layer.
 *
 * This is a Phase 7 stub interface. The Phase 6 agent will provide
 * the real implementation backed by [DataClient]/[MessageClient].
 * Until then, [NoOpSyncClient] is used which safely discards sync calls.
 */
interface ReminderSyncClient {

    /**
     * Notifies connected devices that a reminder has been updated
     * (completed, edited, or state changed).
     */
    suspend fun syncReminderUpdate(reminderId: String)

    /**
     * Notifies connected devices that a reminder has been deleted.
     */
    suspend fun syncReminderDeletion(reminderId: String)
}

/**
 * No-op implementation used until the Phase 6 Data Layer client
 * is merged. All sync calls are safely discarded.
 */
class NoOpSyncClient : ReminderSyncClient {

    override suspend fun syncReminderUpdate(reminderId: String) {
        // No-op — Phase 6 will replace this
    }

    override suspend fun syncReminderDeletion(reminderId: String) {
        // No-op — Phase 6 will replace this
    }
}
