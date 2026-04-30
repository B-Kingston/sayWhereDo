package com.example.reminders.wear.sync

import com.example.reminders.wear.data.DeletedReminder
import com.example.reminders.wear.data.WatchReminder
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Instant

class SyncEngineTest {

    private fun testReminder(
        id: String = "test-${java.util.UUID.randomUUID()}",
        title: String = "Test Reminder",
        updatedAt: Instant = Instant.now(),
        sourceTranscript: String = "test transcript",
        createdAt: Instant = Instant.now()
    ) = WatchReminder(
        id = id,
        title = title,
        updatedAt = updatedAt,
        sourceTranscript = sourceTranscript,
        createdAt = createdAt
    )

    private fun testTombstone(
        id: String = "test-${java.util.UUID.randomUUID()}",
        originalTitle: String = "Deleted Reminder",
        originalUpdatedAt: Instant = Instant.now(),
        deletedAt: Instant = Instant.now(),
        deletedBy: String = "watch"
    ) = DeletedReminder(
        id = id,
        originalTitle = originalTitle,
        originalUpdatedAt = originalUpdatedAt,
        deletedAt = deletedAt,
        deletedBy = deletedBy
    )

    @Test
    fun `reconcile with empty remote and local returns empty result`() {
        val result = SyncEngine.reconcile(
            localActive = emptyList(),
            localTombstones = emptyList(),
            remoteActive = emptyList(),
            remoteTombstones = emptyList(),
            localDeviceId = "watch-1"
        )

        assertThat(result.remindersToInsert).isEmpty()
        assertThat(result.reminderIdsToDelete).isEmpty()
        assertThat(result.tombstoneIdsToRemove).isEmpty()
    }

    @Test
    fun `reconcile with new remote reminder inserts locally`() {
        val remote = testReminder(id = "remote-1", title = "Buy milk")

        val result = SyncEngine.reconcile(
            localActive = emptyList(),
            localTombstones = emptyList(),
            remoteActive = listOf(remote),
            remoteTombstones = emptyList(),
            localDeviceId = "watch-1"
        )

        assertThat(result.remindersToInsert).containsExactly(remote)
        assertThat(result.remindersToUpdate).isEmpty()
        assertThat(result.reminderIdsToDelete).isEmpty()
        assertThat(result.tombstonesToInsert).isEmpty()
    }

    @Test
    fun `reconcile with remote newer than local updates local`() {
        val older = Instant.ofEpochMilli(1000)
        val newer = Instant.ofEpochMilli(2000)

        val local = testReminder(id = "r-1", updatedAt = older)
        val remote = testReminder(id = "r-1", title = "Updated", updatedAt = newer)

        val result = SyncEngine.reconcile(
            localActive = listOf(local),
            localTombstones = emptyList(),
            remoteActive = listOf(remote),
            remoteTombstones = emptyList(),
            localDeviceId = "watch-1"
        )

        assertThat(result.remindersToUpdate).containsExactly(remote)
        assertThat(result.remindersToInsert).isEmpty()
    }

    @Test
    fun `reconcile with local newer than remote keeps local`() {
        val older = Instant.ofEpochMilli(1000)
        val newer = Instant.ofEpochMilli(2000)

        val local = testReminder(id = "r-1", updatedAt = newer)
        val remote = testReminder(id = "r-1", title = "Stale", updatedAt = older)

        val result = SyncEngine.reconcile(
            localActive = listOf(local),
            localTombstones = emptyList(),
            remoteActive = listOf(remote),
            remoteTombstones = emptyList(),
            localDeviceId = "watch-1"
        )

        assertThat(result.remindersToUpdate).isEmpty()
        assertThat(result.remindersToInsert).isEmpty()
    }

    @Test
    fun `reconcile with equal timestamps keeps local version`() {
        val timestamp = Instant.ofEpochMilli(1000)

        val local = testReminder(id = "shared-id", title = "Local", updatedAt = timestamp)
        val remote = testReminder(id = "shared-id", title = "Remote", updatedAt = timestamp)

        val result = SyncEngine.reconcile(
            localActive = listOf(local),
            localTombstones = emptyList(),
            remoteActive = listOf(remote),
            remoteTombstones = emptyList(),
            localDeviceId = "watch-1"
        )

        assertThat(result.remindersToUpdate).isEmpty()
        assertThat(result.remindersToInsert).isEmpty()
    }

    @Test
    fun `reconcile local tombstone with older or equal remote edit restores reminder`() {
        val tombstoneTime = Instant.ofEpochMilli(1000)
        val remoteTime = Instant.ofEpochMilli(2000)

        val tombstone = testTombstone(
            id = "r-1",
            originalUpdatedAt = tombstoneTime
        )
        val remote = testReminder(id = "r-1", updatedAt = remoteTime)

        val result = SyncEngine.reconcile(
            localActive = emptyList(),
            localTombstones = listOf(tombstone),
            remoteActive = listOf(remote),
            remoteTombstones = emptyList(),
            localDeviceId = "watch-1"
        )

        assertThat(result.remindersToInsert).containsExactly(remote)
        assertThat(result.tombstoneIdsToRemove).containsExactly("r-1")
    }

    @Test
    fun `reconcile local tombstone with newer than remote edit honours deletion`() {
        val tombstoneTime = Instant.ofEpochMilli(2000)
        val remoteTime = Instant.ofEpochMilli(1000)

        val tombstone = testTombstone(
            id = "r-1",
            originalUpdatedAt = tombstoneTime
        )
        val remote = testReminder(id = "r-1", updatedAt = remoteTime)

        val result = SyncEngine.reconcile(
            localActive = emptyList(),
            localTombstones = listOf(tombstone),
            remoteActive = listOf(remote),
            remoteTombstones = emptyList(),
            localDeviceId = "watch-1"
        )

        assertThat(result.remindersToInsert).isEmpty()
        assertThat(result.tombstoneIdsToRemove).isEmpty()
    }

    @Test
    fun `reconcile remote tombstone newer than local active deletes locally`() {
        val localTime = Instant.ofEpochMilli(1000)
        val tombstoneTime = Instant.ofEpochMilli(2000)

        val local = testReminder(id = "r-1", updatedAt = localTime)
        val remoteTombstone = testTombstone(id = "r-1", originalUpdatedAt = tombstoneTime)

        val result = SyncEngine.reconcile(
            localActive = listOf(local),
            localTombstones = emptyList(),
            remoteActive = emptyList(),
            remoteTombstones = listOf(remoteTombstone),
            localDeviceId = "watch-1"
        )

        assertThat(result.reminderIdsToDelete).containsExactly("r-1")
        assertThat(result.tombstonesToInsert).containsExactly(remoteTombstone)
    }

    @Test
    fun `reconcile remote tombstone older than local active keeps local`() {
        val localTime = Instant.ofEpochMilli(2000)
        val tombstoneTime = Instant.ofEpochMilli(1000)

        val local = testReminder(id = "r-1", updatedAt = localTime)
        val remoteTombstone = testTombstone(id = "r-1", originalUpdatedAt = tombstoneTime)

        val result = SyncEngine.reconcile(
            localActive = listOf(local),
            localTombstones = emptyList(),
            remoteActive = emptyList(),
            remoteTombstones = listOf(remoteTombstone),
            localDeviceId = "watch-1"
        )

        assertThat(result.reminderIdsToDelete).isEmpty()
        assertThat(result.tombstonesToInsert).containsExactly(remoteTombstone)
    }

    @Test
    fun `reconcile remote tombstone for unknown reminder stores tombstone`() {
        val remoteTombstone = testTombstone(id = "unknown-1")

        val result = SyncEngine.reconcile(
            localActive = emptyList(),
            localTombstones = emptyList(),
            remoteActive = emptyList(),
            remoteTombstones = listOf(remoteTombstone),
            localDeviceId = "watch-1"
        )

        assertThat(result.tombstonesToInsert).containsExactly(remoteTombstone)
        assertThat(result.reminderIdsToDelete).isEmpty()
    }

    @Test
    fun `reconcile remote tombstone for already tombstoned reminder does not duplicate`() {
        val tombstone = testTombstone(id = "r-1")
        val remoteTombstone = testTombstone(id = "r-1", deletedBy = "phone")

        val result = SyncEngine.reconcile(
            localActive = emptyList(),
            localTombstones = listOf(tombstone),
            remoteActive = emptyList(),
            remoteTombstones = listOf(remoteTombstone),
            localDeviceId = "watch-1"
        )

        assertThat(result.tombstonesToInsert).isEmpty()
    }

    @Test
    fun `reconcile multiple scenarios in single pass`() {
        val now = Instant.now()
        val older = Instant.ofEpochMilli(now.toEpochMilli() - 1000)
        val newer = Instant.ofEpochMilli(now.toEpochMilli() + 1000)

        val toInsertRemote = testReminder(id = "new-remote", title = "New from phone", updatedAt = now)
        val localOlder = testReminder(id = "update-me", title = "Old local", updatedAt = older)
        val remoteNewer = testReminder(id = "update-me", title = "New from phone", updatedAt = newer)
        val localToDelete = testReminder(id = "delete-me", title = "I will be deleted", updatedAt = older)
        val deleteTombstone = testTombstone(id = "delete-me", originalUpdatedAt = newer)
        val unknownTombstone = testTombstone(id = "unknown-deleted", originalUpdatedAt = now)

        val result = SyncEngine.reconcile(
            localActive = listOf(localOlder, localToDelete),
            localTombstones = emptyList(),
            remoteActive = listOf(toInsertRemote, remoteNewer),
            remoteTombstones = listOf(deleteTombstone, unknownTombstone),
            localDeviceId = "watch-1"
        )

        assertThat(result.remindersToInsert).containsExactly(toInsertRemote)
        assertThat(result.remindersToUpdate).containsExactly(remoteNewer)
        assertThat(result.reminderIdsToDelete).containsExactly("delete-me")
        assertThat(result.tombstonesToInsert).hasSize(2)
        assertThat(result.tombstonesToInsert).containsExactly(deleteTombstone, unknownTombstone)
    }

    @Test
    fun `reconcile completed reminder syncs as update when remote newer`() {
        val older = Instant.ofEpochMilli(1000)
        val newer = Instant.ofEpochMilli(2000)

        val local = testReminder(id = "r-1", updatedAt = older, title = "Active")
        val remote = testReminder(
            id = "r-1",
            updatedAt = newer,
            title = "Completed",
            sourceTranscript = "done"
        ).copy(isCompleted = true)

        val result = SyncEngine.reconcile(
            localActive = listOf(local),
            localTombstones = emptyList(),
            remoteActive = listOf(remote),
            remoteTombstones = emptyList(),
            localDeviceId = "watch-1"
        )

        assertThat(result.remindersToUpdate).containsExactly(remote)
        assertThat(result.remindersToUpdate.first().isCompleted).isTrue()
    }

    @Test
    fun `reconcile remote edit newer than local tombstone removes tombstone`() {
        val tombstoneTime = Instant.ofEpochMilli(1000)
        val remoteTime = Instant.ofEpochMilli(2000)

        val tombstone = testTombstone(id = "r-1", originalUpdatedAt = tombstoneTime)
        val remote = testReminder(id = "r-1", updatedAt = remoteTime, title = "Edited on phone")

        val result = SyncEngine.reconcile(
            localActive = emptyList(),
            localTombstones = listOf(tombstone),
            remoteActive = listOf(remote),
            remoteTombstones = emptyList(),
            localDeviceId = "watch-1"
        )

        assertThat(result.remindersToInsert).containsExactly(remote)
        assertThat(result.tombstoneIdsToRemove).containsExactly("r-1")
    }

    @Test
    fun `reconcile remote edit older than local tombstone keeps tombstone`() {
        val tombstoneTime = Instant.ofEpochMilli(2000)
        val remoteTime = Instant.ofEpochMilli(1000)

        val tombstone = testTombstone(id = "r-1", originalUpdatedAt = tombstoneTime)
        val remote = testReminder(id = "r-1", updatedAt = remoteTime)

        val result = SyncEngine.reconcile(
            localActive = emptyList(),
            localTombstones = listOf(tombstone),
            remoteActive = listOf(remote),
            remoteTombstones = emptyList(),
            localDeviceId = "watch-1"
        )

        assertThat(result.remindersToInsert).isEmpty()
        assertThat(result.tombstoneIdsToRemove).isEmpty()
    }

    @Test
    fun `reconcile remote edit equal to local tombstone removes tombstone`() {
        val timestamp = Instant.ofEpochMilli(1000)

        val tombstone = testTombstone(id = "r-1", originalUpdatedAt = timestamp)
        val remote = testReminder(id = "r-1", updatedAt = timestamp)

        val result = SyncEngine.reconcile(
            localActive = emptyList(),
            localTombstones = listOf(tombstone),
            remoteActive = listOf(remote),
            remoteTombstones = emptyList(),
            localDeviceId = "watch-1"
        )

        assertThat(result.remindersToInsert).containsExactly(remote)
        assertThat(result.tombstoneIdsToRemove).containsExactly("r-1")
    }
}
