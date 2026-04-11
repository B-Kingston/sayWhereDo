package com.example.reminders.alarm

import com.example.reminders.data.model.LocationReminderState
import com.example.reminders.data.model.LocationTrigger
import com.example.reminders.data.model.Reminder
import com.example.reminders.data.repository.ReminderRepository
import com.example.reminders.geofence.GeofenceManager
import com.example.reminders.sync.NoOpSyncClient
import com.example.reminders.sync.ReminderSyncClient
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.Instant

class ReminderCompletionManagerTest {

    private val reminderRepository = mockk<ReminderRepository>(relaxed = true)
    private val geofenceManager = mockk<GeofenceManager>(relaxed = true)
    private val alarmScheduler = mockk<AlarmScheduler>(relaxed = true)
    private val syncClient = mockk<ReminderSyncClient>(relaxed = true)

    private lateinit var completionManager: ReminderCompletionManager

    @Before
    fun setUp() {
        completionManager = ReminderCompletionManager(
            reminderRepository = reminderRepository,
            geofenceManager = geofenceManager,
            alarmScheduler = alarmScheduler,
            syncClient = syncClient
        )
    }

    private fun buildReminder(
        id: String = "test-1",
        isCompleted: Boolean = false,
        locationTrigger: LocationTrigger? = null,
        locationState: LocationReminderState? = null,
        triggerTime: Instant? = null
    ) = Reminder(
        id = id,
        title = "Test Reminder",
        body = null,
        triggerTime = triggerTime,
        recurrence = null,
        locationTrigger = locationTrigger,
        locationState = locationState,
        sourceTranscript = "test transcript"
    )

    // --- Completion flow ---

    @Test
    fun `completeReminder marks reminder completed in Room`() = runTest {
        val reminder = buildReminder()
        coEvery { reminderRepository.getReminderById("test-1") } returns reminder

        completionManager.completeReminder("test-1")

        coVerify {
            reminderRepository.update(match {
                it.id == "test-1" && it.isCompleted
            })
        }
    }

    @Test
    fun `completeReminder cancels alarm`() = runTest {
        val triggerTime = Instant.now().plusSeconds(3600)
        val reminder = buildReminder(triggerTime = triggerTime)
        coEvery { reminderRepository.getReminderById("test-1") } returns reminder

        completionManager.completeReminder("test-1")

        coVerify { alarmScheduler.cancelAlarm("test-1") }
    }

    @Test
    fun `completeReminder removes geofence for location reminder`() = runTest {
        val locationTrigger = LocationTrigger(
            placeLabel = "Home",
            latitude = 37.7749,
            longitude = -122.4194,
            geofenceId = "geo-1"
        )
        val reminder = buildReminder(
            locationTrigger = locationTrigger,
            locationState = LocationReminderState.ACTIVE
        )
        coEvery { reminderRepository.getReminderById("test-1") } returns reminder

        completionManager.completeReminder("test-1")

        coVerify { geofenceManager.removeGeofence("geo-1") }
    }

    @Test
    fun `completeReminder sets locationState to COMPLETED`() = runTest {
        val reminder = buildReminder(
            locationState = LocationReminderState.TRIGGERED,
            locationTrigger = LocationTrigger(placeLabel = "Home")
        )
        coEvery { reminderRepository.getReminderById("test-1") } returns reminder

        completionManager.completeReminder("test-1")

        coVerify {
            reminderRepository.update(match {
                it.locationState == LocationReminderState.COMPLETED
            })
        }
    }

    @Test
    fun `completeReminder syncs update`() = runTest {
        val reminder = buildReminder()
        coEvery { reminderRepository.getReminderById("test-1") } returns reminder

        completionManager.completeReminder("test-1")

        coVerify { syncClient.syncReminderUpdate("test-1") }
    }

    @Test
    fun `completeReminder returns failure when reminder not found`() = runTest {
        coEvery { reminderRepository.getReminderById("missing") } returns null

        val result = completionManager.completeReminder("missing")

        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `completeReminder is idempotent when already completed`() = runTest {
        val reminder = buildReminder(isCompleted = true)
        coEvery { reminderRepository.getReminderById("test-1") } returns reminder

        val result = completionManager.completeReminder("test-1")

        assertThat(result.isSuccess).isTrue()
        // Should NOT call update again
        coVerify(exactly = 0) { reminderRepository.update(any()) }
    }

    @Test
    fun `completeReminder skips geofence removal when no location trigger`() = runTest {
        val reminder = buildReminder()
        coEvery { reminderRepository.getReminderById("test-1") } returns reminder

        completionManager.completeReminder("test-1")

        coVerify(exactly = 0) { geofenceManager.removeGeofence(any()) }
    }

    // --- Deletion flow ---

    @Test
    fun `deleteReminder deletes from Room`() = runTest {
        val reminder = buildReminder()
        coEvery { reminderRepository.getReminderById("test-1") } returns reminder

        completionManager.deleteReminder("test-1")

        coVerify { reminderRepository.deleteById("test-1") }
    }

    @Test
    fun `deleteReminder cancels alarm`() = runTest {
        val reminder = buildReminder(triggerTime = Instant.now().plusSeconds(3600))
        coEvery { reminderRepository.getReminderById("test-1") } returns reminder

        completionManager.deleteReminder("test-1")

        coVerify { alarmScheduler.cancelAlarm("test-1") }
    }

    @Test
    fun `deleteReminder removes geofence`() = runTest {
        val reminder = buildReminder(
            locationTrigger = LocationTrigger(
                placeLabel = "Work",
                geofenceId = "geo-2"
            ),
            locationState = LocationReminderState.ACTIVE
        )
        coEvery { reminderRepository.getReminderById("test-1") } returns reminder

        completionManager.deleteReminder("test-1")

        coVerify { geofenceManager.removeGeofence("geo-2") }
    }

    @Test
    fun `deleteReminder syncs deletion`() = runTest {
        val reminder = buildReminder()
        coEvery { reminderRepository.getReminderById("test-1") } returns reminder

        completionManager.deleteReminder("test-1")

        coVerify { syncClient.syncReminderDeletion("test-1") }
    }

    @Test
    fun `deleteReminder returns failure when reminder not found`() = runTest {
        coEvery { reminderRepository.getReminderById("missing") } returns null

        val result = completionManager.deleteReminder("missing")

        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `completeReminder continues when geofence removal fails`() = runTest {
        val reminder = buildReminder(
            locationTrigger = LocationTrigger(
                placeLabel = "Home",
                geofenceId = "geo-fail"
            ),
            locationState = LocationReminderState.ACTIVE
        )
        coEvery { reminderRepository.getReminderById("test-1") } returns reminder
        coEvery { geofenceManager.removeGeofence("geo-fail") } returns Result.failure(
            RuntimeException("Geofence error")
        )

        val result = completionManager.completeReminder("test-1")

        // Should still succeed — geofence failure is not fatal
        assertThat(result.isSuccess).isTrue()
        coVerify { reminderRepository.update(any()) }
    }

    // --- NoOpSyncClient ---

    @Test
    fun `NoOpSyncClient syncReminderUpdate does not throw`() = runTest {
        val client = NoOpSyncClient()
        // Should complete without exception
        client.syncReminderUpdate("any-id")
    }

    @Test
    fun `NoOpSyncClient syncReminderDeletion does not throw`() = runTest {
        val client = NoOpSyncClient()
        client.syncReminderDeletion("any-id")
    }
}
