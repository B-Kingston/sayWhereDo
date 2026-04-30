package com.example.reminders.wear.sync

import com.example.reminders.wear.data.WatchReminder
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Instant

class SyncConflictResolverTest {

    private fun testReminder(
        id: String = "test-${java.util.UUID.randomUUID()}",
        title: String = "Test Reminder",
        updatedAt: Instant = Instant.now()
    ) = WatchReminder(
        id = id,
        title = title,
        updatedAt = updatedAt,
        sourceTranscript = "transcript",
        createdAt = Instant.now()
    )

    @Test
    fun `resolve with remote newer returns remote`() {
        val local = testReminder(id = "r-1", updatedAt = Instant.ofEpochMilli(1000))
        val remote = testReminder(id = "r-1", title = "Remote", updatedAt = Instant.ofEpochMilli(2000))

        val winner = SyncConflictResolver.resolve(local, remote)

        assertThat(winner).isSameInstanceAs(remote)
    }

    @Test
    fun `resolve with local newer returns local`() {
        val local = testReminder(id = "r-1", title = "Local", updatedAt = Instant.ofEpochMilli(2000))
        val remote = testReminder(id = "r-1", updatedAt = Instant.ofEpochMilli(1000))

        val winner = SyncConflictResolver.resolve(local, remote)

        assertThat(winner).isSameInstanceAs(local)
    }

    @Test
    fun `resolve tie with larger id wins`() {
        val timestamp = Instant.ofEpochMilli(1000)
        val local = testReminder(id = "aaa", updatedAt = timestamp)
        val remote = testReminder(id = "zzz", title = "Remote", updatedAt = timestamp)

        val winner = SyncConflictResolver.resolve(local, remote)

        assertThat(winner).isSameInstanceAs(remote)
    }

    @Test
    fun `resolve tie with smaller id loses`() {
        val timestamp = Instant.ofEpochMilli(1000)
        val local = testReminder(id = "zzz", title = "Local", updatedAt = timestamp)
        val remote = testReminder(id = "aaa", updatedAt = timestamp)

        val winner = SyncConflictResolver.resolve(local, remote)

        assertThat(winner).isSameInstanceAs(local)
    }

    @Test
    fun `mergeLists combines unique reminders from both sides`() {
        val timestamp = Instant.ofEpochMilli(1000)
        val a = testReminder(id = "a", title = "A", updatedAt = timestamp)
        val bLocal = testReminder(id = "b", title = "B Local", updatedAt = timestamp)
        val bRemote = testReminder(id = "b", title = "B Remote", updatedAt = Instant.ofEpochMilli(2000))
        val c = testReminder(id = "c", title = "C", updatedAt = timestamp)

        val merged = SyncConflictResolver.mergeLists(
            local = listOf(a, bLocal),
            remote = listOf(bRemote, c)
        )

        assertThat(merged).hasSize(3)
        val byId = merged.associateBy { it.id }
        assertThat(byId["a"]).isEqualTo(a)
        assertThat(byId["b"]).isEqualTo(bRemote)
        assertThat(byId["c"]).isEqualTo(c)
    }

    @Test
    fun `mergeLists with empty local returns all remote`() {
        val remote = listOf(
            testReminder(id = "r-1"),
            testReminder(id = "r-2")
        )

        val merged = SyncConflictResolver.mergeLists(local = emptyList(), remote = remote)

        assertThat(merged).containsExactlyElementsIn(remote)
    }

    @Test
    fun `mergeLists with empty remote returns all local`() {
        val local = listOf(
            testReminder(id = "r-1"),
            testReminder(id = "r-2")
        )

        val merged = SyncConflictResolver.mergeLists(local = local, remote = emptyList())

        assertThat(merged).containsExactlyElementsIn(local)
    }
}
