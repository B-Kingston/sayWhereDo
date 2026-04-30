package com.example.reminders.wearable

import com.example.reminders.data.model.Reminder
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Instant

class SyncConflictResolverTest {

    private val resolver = SyncConflictResolver()

    private fun testReminder(
        id: String = "test-1",
        title: String = "Test",
        updatedAt: Instant = Instant.now()
    ) = Reminder(
        id = id,
        title = title,
        updatedAt = updatedAt,
        sourceTranscript = "transcript",
        createdAt = Instant.now()
    )

    @Test
    fun `resolve with remote newer returns remote`() {
        val now = Instant.now()
        val local = testReminder(updatedAt = now.minusSeconds(60))
        val remote = testReminder(updatedAt = now, title = "Remote wins")

        val result = resolver.resolve(local, remote)

        assertThat(result).isEqualTo(remote)
    }

    @Test
    fun `resolve with local newer returns local`() {
        val now = Instant.now()
        val local = testReminder(updatedAt = now, title = "Local wins")
        val remote = testReminder(updatedAt = now.minusSeconds(60))

        val result = resolver.resolve(local, remote)

        assertThat(result).isEqualTo(local)
    }

    @Test
    fun `resolve with equal timestamps returns remote`() {
        val now = Instant.now()
        val local = testReminder(updatedAt = now, title = "Local version")
        val remote = testReminder(updatedAt = now, title = "Remote version")

        val result = resolver.resolve(local, remote)

        assertThat(result).isEqualTo(remote)
    }
}
