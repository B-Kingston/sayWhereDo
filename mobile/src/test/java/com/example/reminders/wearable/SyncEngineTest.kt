package com.example.reminders.wearable

import com.example.reminders.data.model.DeletedReminder
import com.example.reminders.data.model.Reminder
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Instant

class SyncEngineTest {

    private val engine = SyncEngine()

    private fun testReminder(
        id: String = "test-${java.util.UUID.randomUUID()}",
        title: String = "Test Reminder",
        updatedAt: Instant = Instant.now(),
        sourceTranscript: String = "test transcript",
        createdAt: Instant = Instant.now()
    ) = Reminder(
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
        deletedBy: String = "mobile"
    ) = DeletedReminder(
        id = id,
        originalTitle = originalTitle,
        originalUpdatedAt = originalUpdatedAt,
        deletedAt = deletedAt,
        deletedBy = deletedBy
    )

    @Test
    fun `reconcile with empty remote and local returns empty result`() {
        val result = engine.reconcile(
            localActive = emptyList(),
            localTombstones = emptyList(),
            remoteActive = emptyList(),
            remoteTombstones = emptyList(),
            localDeviceId = "mobile"
        )

        assertThat(result.remindersToInsert).isEmpty()
        assertThat(result.remindersToUpdate).isEmpty()
        assertThat(result.reminderIdsToDelete).isEmpty()
        assertThat(result.tombstonesToInsert).isEmpty()
        assertThat(result.tombstoneIdsToRemove).isEmpty()
    }

    @Test
    fun `reconcile with new remote reminder inserts locally`() {
        val remote = testReminder(id = "remote-1", title = "New from watch")

        val result = engine.reconcile(
            localActive = emptyList(),
            localTombstones = emptyList(),
            remoteActive = listOf(remote),
            remoteTombstones = emptyList(),
            localDeviceId = "mobile"
        )

        assertThat(result.remindersToInsert).containsExactly(remote)
        assertThat(result.remindersToUpdate).isEmpty()
        assertThat(result.reminderIdsToDelete).isEmpty()
    }

    @Test
    fun `reconcile with remote newer than local updates local`() {
        val now = Instant.now()
        val local = testReminder(id = "shared-1", updatedAt = now.minusSeconds(60))
        val remote = testReminder(id = "shared-1", updatedAt = now, title = "Updated on watch")

        val result = engine.reconcile(
            localActive = listOf(local),
            localTombstones = emptyList(),
            remoteActive = listOf(remote),
            remoteTombstones = emptyList(),
            localDeviceId = "mobile"
        )

        assertThat(result.remindersToUpdate).containsExactly(remote)
        assertThat(result.remindersToInsert).isEmpty()
        assertThat(result.reminderIdsToDelete).isEmpty()
    }

    @Test
    fun `reconcile with local newer than remote keeps local`() {
        val now = Instant.now()
        val local = testReminder(id = "shared-1", updatedAt = now, title = "Newer on phone")
        val remote = testReminder(id = "shared-1", updatedAt = now.minusSeconds(60), title = "Older on watch")

        val result = engine.reconcile(
            localActive = listOf(local),
            localTombstones = emptyList(),
            remoteActive = listOf(remote),
            remoteTombstones = emptyList(),
            localDeviceId = "mobile"
        )

        assertThat(result.remindersToUpdate).isEmpty()
        assertThat(result.remindersToInsert).isEmpty()
    }

    @Test
    fun `reconcile with equal timestamps remote wins`() {
        val now = Instant.now()
        val local = testReminder(id = "shared-1", updatedAt = now, title = "Local version")
        val remote = testReminder(id = "shared-1", updatedAt = now, title = "Remote version")

        val result = engine.reconcile(
            localActive = listOf(local),
            localTombstones = emptyList(),
            remoteActive = listOf(remote),
            remoteTombstones = emptyList(),
            localDeviceId = "mobile"
        )

        assertThat(result.remindersToUpdate).containsExactly(remote)
    }

    @Test
    fun `reconcile local tombstone newer than remote edit deletes remote`() {
        val now = Instant.now()
        val tombstone = testTombstone(
            id = "shared-1",
            originalUpdatedAt = now
        )
        val remote = testReminder(
            id = "shared-1",
            updatedAt = now.minusSeconds(60),
            title = "Edited on watch"
        )

        val result = engine.reconcile(
            localActive = emptyList(),
            localTombstones = listOf(tombstone),
            remoteActive = listOf(remote),
            remoteTombstones = emptyList(),
            localDeviceId = "mobile"
        )

        assertThat(result.reminderIdsToDelete).containsExactly("shared-1")
        assertThat(result.remindersToInsert).isEmpty()
        assertThat(result.tombstoneIdsToRemove).isEmpty()
    }

    @Test
    fun `reconcile remote edit newer than local tombstone restores`() {
        val now = Instant.now()
        val tombstone = testTombstone(
            id = "shared-1",
            originalUpdatedAt = now.minusSeconds(60)
        )
        val remote = testReminder(
            id = "shared-1",
            updatedAt = now,
            title = "Edited on watch"
        )

        val result = engine.reconcile(
            localActive = emptyList(),
            localTombstones = listOf(tombstone),
            remoteActive = listOf(remote),
            remoteTombstones = emptyList(),
            localDeviceId = "mobile"
        )

        assertThat(result.remindersToInsert).containsExactly(remote)
        assertThat(result.tombstoneIdsToRemove).containsExactly("shared-1")
        assertThat(result.reminderIdsToDelete).isEmpty()
    }

    @Test
    fun `reconcile remote tombstone newer than local active deletes locally`() {
        val now = Instant.now()
        val local = testReminder(id = "shared-1", updatedAt = now.minusSeconds(60))
        val remoteTomb = testTombstone(id = "shared-1", originalUpdatedAt = now)

        val result = engine.reconcile(
            localActive = listOf(local),
            localTombstones = emptyList(),
            remoteActive = emptyList(),
            remoteTombstones = listOf(remoteTomb),
            localDeviceId = "mobile"
        )

        assertThat(result.reminderIdsToDelete).containsExactly("shared-1")
        assertThat(result.tombstonesToInsert).containsExactly(remoteTomb)
    }

    @Test
    fun `reconcile remote tombstone older than local active keeps local`() {
        val now = Instant.now()
        val local = testReminder(id = "shared-1", updatedAt = now)
        val remoteTomb = testTombstone(id = "shared-1", originalUpdatedAt = now.minusSeconds(60))

        val result = engine.reconcile(
            localActive = listOf(local),
            localTombstones = emptyList(),
            remoteActive = emptyList(),
            remoteTombstones = listOf(remoteTomb),
            localDeviceId = "mobile"
        )

        assertThat(result.reminderIdsToDelete).isEmpty()
        assertThat(result.tombstonesToInsert).isEmpty()
    }

    @Test
    fun `reconcile remote tombstone for unknown reminder stores tombstone`() {
        val remoteTomb = testTombstone(id = "unknown-1")

        val result = engine.reconcile(
            localActive = emptyList(),
            localTombstones = emptyList(),
            remoteActive = emptyList(),
            remoteTombstones = listOf(remoteTomb),
            localDeviceId = "mobile"
        )

        assertThat(result.tombstonesToInsert).containsExactly(remoteTomb)
    }

    @Test
    fun `reconcile remote tombstone for already tombstoned reminder does not duplicate`() {
        val localTomb = testTombstone(id = "shared-1")
        val remoteTomb = testTombstone(id = "shared-1", deletedBy = "watch")

        val result = engine.reconcile(
            localActive = emptyList(),
            localTombstones = listOf(localTomb),
            remoteActive = emptyList(),
            remoteTombstones = listOf(remoteTomb),
            localDeviceId = "mobile"
        )

        assertThat(result.tombstonesToInsert).isEmpty()
    }

    @Test
    fun `reconcile completed reminder syncs as update`() {
        val now = Instant.now()
        val local = testReminder(id = "shared-1", updatedAt = now.minusSeconds(60), isCompleted = false)
        val remote = testReminder(id = "shared-1", updatedAt = now, isCompleted = true)

        val result = engine.reconcile(
            localActive = listOf(local),
            localTombstones = emptyList(),
            remoteActive = listOf(remote),
            remoteTombstones = emptyList(),
            localDeviceId = "mobile"
        )

        assertThat(result.remindersToUpdate).containsExactly(remote)
        assertThat(result.reminderIdsToDelete).isEmpty()
    }

    @Test
    fun `reconcile multiple reminders handles all correctly`() {
        val now = Instant.now()

        val localActive1 = testReminder(id = "both-newer-local", updatedAt = now, title = "Local wins")
        val localActive2 = testReminder(id = "both-newer-remote", updatedAt = now.minusSeconds(60))
        val localTomb1 = testTombstone(id = "tomb-vs-edit-delete-wins", originalUpdatedAt = now)
        val localTomb2 = testTombstone(id = "tomb-vs-edit-restore-wins", originalUpdatedAt = now.minusSeconds(60))

        val remoteActive1 = testReminder(id = "both-newer-local", updatedAt = now.minusSeconds(30))
        val remoteActive2 = testReminder(id = "both-newer-remote", updatedAt = now, title = "Remote wins")
        val remoteActive3 = testReminder(id = "brand-new-remote", title = "Totally new")
        val remoteActive4 = testReminder(id = "tomb-vs-edit-delete-wins", updatedAt = now.minusSeconds(30))
        val remoteActive5 = testReminder(id = "tomb-vs-edit-restore-wins", updatedAt = now, title = "Remote edit wins")

        val remoteTomb1 = testTombstone(id = "both-newer-local-tomb", originalUpdatedAt = now.minusSeconds(30))
        val remoteTomb2 = testTombstone(id = "active-vs-tomb-del", originalUpdatedAt = now)

        val localActiveForTomb = testReminder(id = "active-vs-tomb-del", updatedAt = now.minusSeconds(60))
        val localActiveKeepsLocal = testReminder(id = "both-newer-local-tomb", updatedAt = now)

        val result = engine.reconcile(
            localActive = listOf(localActive1, localActive2, localActiveForTomb, localActiveKeepsLocal),
            localTombstones = listOf(localTomb1, localTomb2),
            remoteActive = listOf(remoteActive1, remoteActive2, remoteActive3, remoteActive4, remoteActive5),
            remoteTombstones = listOf(remoteTomb1, remoteTomb2),
            localDeviceId = "mobile"
        )

        assertThat(result.remindersToInsert).hasSize(2)
        assertThat(result.remindersToInsert.map { it.id }).containsExactly("brand-new-remote", "tomb-vs-edit-restore-wins")

        assertThat(result.remindersToUpdate).hasSize(1)
        assertThat(result.remindersToUpdate.first().id).isEqualTo("both-newer-remote")

        assertThat(result.tombstoneIdsToRemove).containsExactly("tomb-vs-edit-restore-wins")

        assertThat(result.reminderIdsToDelete).containsExactly("tomb-vs-edit-delete-wins", "active-vs-tomb-del")

        assertThat(result.tombstonesToInsert.map { it.id }).containsExactly("active-vs-tomb-del")
    }

    @Test
    fun `reconcile deduplicates reminder IDs in delete list`() {
        val now = Instant.now()
        val local = testReminder(id = "dup-delete", updatedAt = now.minusSeconds(120))
        val localTomb = testTombstone(id = "dup-delete", originalUpdatedAt = now.minusSeconds(30))
        val remote = testReminder(id = "dup-delete", updatedAt = now.minusSeconds(60), title = "Stale remote edit")
        val remoteTomb = testTombstone(id = "dup-delete", originalUpdatedAt = now)

        val result = engine.reconcile(
            localActive = listOf(local),
            localTombstones = listOf(localTomb),
            remoteActive = listOf(remote),
            remoteTombstones = listOf(remoteTomb),
            localDeviceId = "mobile"
        )

        assertThat(result.reminderIdsToDelete).containsExactly("dup-delete")
    }

    private fun testReminder(
        id: String = "test-${java.util.UUID.randomUUID()}",
        title: String = "Test Reminder",
        updatedAt: Instant = Instant.now(),
        sourceTranscript: String = "test transcript",
        createdAt: Instant = Instant.now(),
        isCompleted: Boolean = false
    ) = Reminder(
        id = id,
        title = title,
        updatedAt = updatedAt,
        sourceTranscript = sourceTranscript,
        createdAt = createdAt,
        isCompleted = isCompleted
    )
}
