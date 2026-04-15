package com.example.reminders.wear.data

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Unit tests for [WatchReminderRepository].
 *
 * Both DAOs are mocked with MockK so these tests run on the JVM without
 * any Android framework or Room database dependency.
 */
class WatchReminderRepositoryTest {

    private val reminderDao: WatchReminderDao = mockk(relaxed = true)
    private val deletedDao: DeletedReminderDao = mockk(relaxed = true)
    private lateinit var repository: WatchReminderRepository

    @Before
    fun setUp() {
        repository = WatchReminderRepository(reminderDao, deletedDao)
    }

    // ── Existing methods still delegate correctly ────────────────

    @Test
    fun `getAllReminders delegates to reminderDao`() {
        val expected = flowOf(listOf(buildWatchReminder("r1")))
        every { reminderDao.getAll() } returns expected

        val result = repository.getAllReminders()
        assertThat(result).isSameInstanceAs(expected)
    }

    @Test
    fun `insert delegates to reminderDao`() = runTest {
        val reminder = buildWatchReminder("r1")
        repository.insert(reminder)
        coVerify { reminderDao.insert(reminder) }
    }

    @Test
    fun `deleteById delegates to reminderDao`() = runTest {
        repository.deleteById("r1")
        coVerify { reminderDao.deleteById("r1") }
    }

    // ── getDeletedReminders ─────────────────────────────────────

    @Test
    fun `getDeletedReminders returns flow from deletedDao`() = runTest {
        val tombstones = listOf(buildDeletedReminder("t1"), buildDeletedReminder("t2"))
        every { deletedDao.getAll() } returns flowOf(tombstones)

        repository.getDeletedReminders().test {
            assertThat(awaitItem()).containsExactlyElementsIn(tombstones).inOrder()
            awaitComplete()
        }
    }

    // ── restoreDeletedReminder ──────────────────────────────────

    @Test
    fun `restoreDeletedReminder calls deletedDao deleteById`() = runTest {
        repository.restoreDeletedReminder("t1")
        coVerify { deletedDao.deleteById("t1") }
    }

    // ── moveReminderToTombstone ─────────────────────────────────

    @Test
    fun `moveReminderToTombstone creates tombstone and deletes reminder`() = runTest {
        val reminder = buildWatchReminder("r1")
        coEvery { reminderDao.getById("r1") } returns reminder
        coEvery { deletedDao.insert(any()) } just Runs
        coEvery { reminderDao.deleteById("r1") } just Runs

        repository.moveReminderToTombstone("r1", "watch")

        coVerify {
            deletedDao.insert(match {
                it.id == "r1" &&
                    it.originalTitle == reminder.title &&
                    it.deletedBy == "watch" &&
                    it.originalUpdatedAt == reminder.updatedAt
            })
        }
        coVerify { reminderDao.deleteById("r1") }
    }

    @Test
    fun `moveReminderToTombstone is no-op when reminder does not exist`() = runTest {
        coEvery { reminderDao.getById("missing") } returns null

        repository.moveReminderToTombstone("missing", "watch")

        coVerify(exactly = 0) { deletedDao.insert(any()) }
        coVerify(exactly = 0) { reminderDao.deleteById(any()) }
    }

    // ── cleanExpiredTombstones ──────────────────────────────────

    @Test
    fun `cleanExpiredTombstones delegates to deletedDao and returns count`() = runTest {
        coEvery { deletedDao.deleteOlderThan(any()) } returns 5

        val removed = repository.cleanExpiredTombstones()
        assertThat(removed).isEqualTo(5)
    }

    @Test
    fun `cleanExpiredTombstones passes cutoff based on 30 day expiry`() = runTest {
        coEvery { deletedDao.deleteOlderThan(any()) } returns 0

        repository.cleanExpiredTombstones()

        coVerify {
            deletedDao.deleteOlderThan(match { cutoff ->
                val expectedMax = Instant.now().minus(30, ChronoUnit.DAYS)
                !cutoff.isAfter(expectedMax) && !cutoff.isBefore(expectedMax.minusSeconds(5))
            })
        }
    }

    // ── reminderExistsInTrash ───────────────────────────────────

    @Test
    fun `reminderExistsInTrash returns true when tombstone exists`() = runTest {
        coEvery { deletedDao.exists("r1") } returns true
        assertThat(repository.reminderExistsInTrash("r1")).isTrue()
    }

    @Test
    fun `reminderExistsInTrash returns false when tombstone absent`() = runTest {
        coEvery { deletedDao.exists("r1") } returns false
        assertThat(repository.reminderExistsInTrash("r1")).isFalse()
    }

    // ── Helpers ──────────────────────────────────────────────────

    private fun buildWatchReminder(id: String) = WatchReminder(
        id = id,
        title = "Test reminder $id",
        sourceTranscript = "remind me to test",
        createdAt = Instant.parse("2025-01-15T10:00:00Z"),
        updatedAt = Instant.parse("2025-01-15T10:00:00Z")
    )

    private fun buildDeletedReminder(id: String) = DeletedReminder(
        id = id,
        originalTitle = "Deleted reminder $id",
        deletedAt = Instant.parse("2025-01-20T12:00:00Z"),
        deletedBy = "watch",
        originalUpdatedAt = Instant.parse("2025-01-15T10:00:00Z")
    )
}
