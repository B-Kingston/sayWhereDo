package com.example.reminders.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.reminders.data.model.Reminder
import com.example.reminders.data.repository.ReminderRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.Instant

class AlarmSchedulerTest {

    private val context = mockk<Context>(relaxed = true)
    private val alarmManager = mockk<AlarmManager>(relaxed = true)
    private val reminderRepository = mockk<ReminderRepository>(relaxed = true)
    private lateinit var scheduler: AndroidAlarmScheduler

    @Before
    fun setUp() {
        every { context.getSystemService(Context.ALARM_SERVICE) } returns alarmManager
        every { context.packageName } returns "com.example.reminders"

        scheduler = AndroidAlarmScheduler(
            context = context,
            reminderRepository = reminderRepository
        )
    }

    private fun buildReminder(
        id: String = "test-1",
        triggerTime: Instant? = Instant.now().plusSeconds(3600),
        isCompleted: Boolean = false
    ) = Reminder(
        id = id,
        title = "Test",
        body = null,
        triggerTime = triggerTime,
        recurrence = null,
        locationTrigger = null,
        locationState = null,
        sourceTranscript = "test"
    )

    @Test
    fun `scheduleAlarm sets exact alarm with RTC_WAKEUP`() {
        val triggerTime = Instant.now().plusSeconds(3600)
        val reminder = buildReminder(triggerTime = triggerTime)

        scheduler.scheduleAlarm(reminder)

        verify {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime.toEpochMilli(),
                any<PendingIntent>()
            )
        }
    }

    @Test
    fun `scheduleAlarm skips when triggerTime is null`() {
        val reminder = buildReminder(triggerTime = null)

        scheduler.scheduleAlarm(reminder)

        verify(exactly = 0) {
            alarmManager.setExactAndAllowWhileIdle(any(), any(), any())
        }
    }

    @Test
    fun `scheduleAlarm skips when reminder is completed`() {
        val reminder = buildReminder(isCompleted = true)

        scheduler.scheduleAlarm(reminder)

        verify(exactly = 0) {
            alarmManager.setExactAndAllowWhileIdle(any(), any(), any())
        }
    }

    @Test
    fun `cancelAlarm cancels pendingIntent`() {
        val pendingIntent = mockk<PendingIntent>(relaxed = true)
        every {
            PendingIntent.getBroadcast(
                context,
                "test-1".hashCode(),
                any<Intent>(),
                any<Int>()
            )
        } returns pendingIntent

        scheduler.cancelAlarm("test-1")

        verify { alarmManager.cancel(pendingIntent) }
    }

    @Test
    fun `rescheduleAll schedules alarms for all timed reminders`() = runTest {
        val futureTime = Instant.now().plusSeconds(3600)
        val reminders = listOf(
            buildReminder(id = "r1", triggerTime = futureTime),
            buildReminder(id = "r2", triggerTime = futureTime.plusSeconds(1800))
        )
        coEvery { reminderRepository.getTimedRemindersOnce() } returns reminders

        scheduler.rescheduleAll()

        verify(exactly = 2) {
            alarmManager.setExactAndAllowWhileIdle(any(), any(), any<PendingIntent>())
        }
    }

    @Test
    fun `rescheduleAll skips reminders with past trigger times`() = runTest {
        val pastTime = Instant.now().minusSeconds(3600)
        val reminders = listOf(
            buildReminder(id = "r1", triggerTime = pastTime)
        )
        coEvery { reminderRepository.getTimedRemindersOnce() } returns reminders

        scheduler.rescheduleAll()

        verify(exactly = 0) {
            alarmManager.setExactAndAllowWhileIdle(any(), any(), any())
        }
    }

    @Test
    fun `rescheduleAll handles empty list gracefully`() = runTest {
        coEvery { reminderRepository.getTimedRemindersOnce() } returns emptyList()

        scheduler.rescheduleAll()

        verify(exactly = 0) {
            alarmManager.setExactAndAllowWhileIdle(any(), any(), any())
        }
    }

    @Test
    fun `scheduleAlarm skips when trigger time is in the past`() {
        val pastTime = Instant.now().minusSeconds(3600)
        val reminder = buildReminder(triggerTime = pastTime)

        scheduler.scheduleAlarm(reminder)

        verify(exactly = 0) {
            alarmManager.setExactAndAllowWhileIdle(any(), any(), any())
        }
    }
}
