package com.example.reminders.sync

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

class NoOpSyncClientTest {

    @Test
    fun `syncReminderUpdate completes without error`() = runTest {
        val client = NoOpSyncClient()
        val result = runCatching { client.syncReminderUpdate("test-id") }
        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `syncReminderDeletion completes without error`() = runTest {
        val client = NoOpSyncClient()
        val result = runCatching { client.syncReminderDeletion("test-id") }
        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `syncReminderUpdate handles empty id gracefully`() = runTest {
        val client = NoOpSyncClient()
        val result = runCatching { client.syncReminderUpdate("") }
        assertThat(result.isSuccess).isTrue()
    }
}
