package com.example.reminders.data.repository

import com.example.reminders.data.local.ReminderDao
import com.example.reminders.data.model.LocationReminderState
import com.example.reminders.data.model.LocationTrigger
import com.example.reminders.data.model.Reminder
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ReminderRepositoryImplTest {

    private val dao = mockk<ReminderDao>(relaxed = true)
    private val repo = ReminderRepositoryImpl(dao)

    private val testReminder = Reminder(
        id = "test-1",
        title = "Buy milk",
        sourceTranscript = "remind me to buy milk",
        locationTrigger = LocationTrigger(placeLabel = "Store", latitude = 40.0, longitude = -74.0),
        locationState = LocationReminderState.ACTIVE
    )

    @Test
    fun `insert delegates to dao`() = runTest {
        repo.insert(testReminder)
        coVerify { dao.insert(testReminder) }
    }

    @Test
    fun `getReminderById delegates to dao`() = runTest {
        coEvery { dao.getById("test-1") } returns testReminder
        assertThat(repo.getReminderById("test-1")).isEqualTo(testReminder)
    }

    @Test
    fun `getReminderById returns null when not found`() = runTest {
        coEvery { dao.getById("missing") } returns null
        assertThat(repo.getReminderById("missing")).isNull()
    }

    @Test
    fun `update delegates to dao`() = runTest {
        repo.update(testReminder)
        coVerify { dao.update(testReminder) }
    }

    @Test
    fun `delete delegates to dao`() = runTest {
        repo.delete(testReminder)
        coVerify { dao.delete(testReminder) }
    }

    @Test
    fun `deleteById delegates to dao`() = runTest {
        repo.deleteById("test-1")
        coVerify { dao.deleteById("test-1") }
    }

    @Test
    fun `getActiveGeofenceCount delegates to dao`() = runTest {
        coEvery { dao.getActiveGeofenceCount() } returns 3
        assertThat(repo.getActiveGeofenceCount()).isEqualTo(3)
    }
}
