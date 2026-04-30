package com.example.reminders.wearable

import com.example.reminders.data.model.DeletedReminder
import com.example.reminders.data.model.Reminder
import com.example.reminders.data.repository.ReminderRepository
import com.example.reminders.sync.WearableSyncClient
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Instant

class WearableSyncClientTest {

    private val dataSender = mockk<WearableDataSender>(relaxed = true)
    private val repository = mockk<ReminderRepository>()
    private val client = WearableSyncClient(dataSender, repository)

    private fun testReminder(
        id: String = "test-1",
        title: String = "Test Reminder",
        updatedAt: Instant = Instant.now()
    ) = Reminder(
        id = id,
        title = title,
        updatedAt = updatedAt,
        sourceTranscript = "transcript",
        createdAt = Instant.now()
    )

    @Test
    fun `syncReminderUpdate calls dataSender with correct reminder`() = runTest {
        val reminder = testReminder()
        coEvery { repository.getReminderById("test-1") } returns reminder

        client.syncReminderUpdate("test-1")

        coVerify(exactly = 1) { dataSender.syncReminderToWatch(reminder) }
    }

    @Test
    fun `syncReminderUpdate with null reminder does not call dataSender`() = runTest {
        coEvery { repository.getReminderById("missing") } returns null

        client.syncReminderUpdate("missing")

        coVerify(exactly = 0) { dataSender.syncReminderToWatch(any()) }
    }

    @Test
    fun `syncReminderDeletion creates tombstone and sends to watches`() = runTest {
        val reminder = testReminder(id = "del-1", title = "To Delete")
        coEvery { repository.getReminderById("del-1") } returns reminder
        coEvery { repository.moveReminderToTombstone(any(), any()) } returns Unit
        coEvery { dataSender.getConnectedWatchNodes() } returns emptyList()

        client.syncReminderDeletion("del-1")

        coVerify(exactly = 1) { repository.moveReminderToTombstone("del-1", "mobile") }
    }

    @Test
    fun `syncReminderDeletion with null reminder does nothing`() = runTest {
        coEvery { repository.getReminderById("missing") } returns null

        client.syncReminderDeletion("missing")

        coVerify(exactly = 0) { repository.moveReminderToTombstone(any(), any()) }
        coVerify(exactly = 0) { dataSender.sendTombstoneToWatch(any(), any()) }
    }
}
