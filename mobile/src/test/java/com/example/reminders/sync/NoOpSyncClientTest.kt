package com.example.reminders.sync

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class NoOpSyncClientTest {

    @Test
    fun `syncReminderUpdate completes without error`() {
        val client = NoOpSyncClient()
        // Should not throw
        val result = runCatching { client.syncReminderUpdate("test-id") }
        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `syncReminderDeletion completes without error`() {
        val client = NoOpSyncClient()
        val result = runCatching { client.syncReminderDeletion("test-id") }
        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `syncReminderUpdate handles null id gracefully`() {
        val client = NoOpSyncClient()
        val result = runCatching { client.syncReminderUpdate("") }
        assertThat(result.isSuccess).isTrue()
    }
}
