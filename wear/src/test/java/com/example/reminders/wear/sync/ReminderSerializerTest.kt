package com.example.reminders.wear.sync

import com.example.reminders.wear.data.WatchReminder
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Instant

class ReminderSerializerTest {

    private fun fullReminder(
        id: String = "test-id",
        title: String = "Buy groceries",
        body: String? = "Milk, eggs, bread",
        triggerTime: Instant? = Instant.ofEpochMilli(1700000000000L),
        recurrence: String? = "daily",
        isCompleted: Boolean = false,
        sourceTranscript: String = "remind me to buy groceries",
        createdAt: Instant = Instant.ofEpochMilli(1699900000000L),
        locationTriggerJson: String? = """{"lat":37.422,"lng":-122.084}""",
        locationState: String? = "ACTIVE",
        formattingProvider: String = "gemini",
        geofencingDevice: String = "phone",
        createdBy: String = "watch",
        lastModifiedBy: String = "phone",
        updatedAt: Instant = Instant.ofEpochMilli(1700001000000L)
    ) = WatchReminder(
        id = id,
        title = title,
        body = body,
        triggerTime = triggerTime,
        recurrence = recurrence,
        isCompleted = isCompleted,
        sourceTranscript = sourceTranscript,
        createdAt = createdAt,
        locationTriggerJson = locationTriggerJson,
        locationState = locationState,
        formattingProvider = formattingProvider,
        geofencingDevice = geofencingDevice,
        createdBy = createdBy,
        lastModifiedBy = lastModifiedBy,
        updatedAt = updatedAt
    )

    @Test
    fun `serialize and deserialize reminder round-trip`() {
        val original = fullReminder()

        val bytes = ReminderSerializer.serialize(original)
        val restored = ReminderSerializer.deserialize(bytes)

        assertThat(restored.id).isEqualTo(original.id)
        assertThat(restored.title).isEqualTo(original.title)
        assertThat(restored.body).isEqualTo(original.body)
        assertThat(restored.triggerTime).isEqualTo(original.triggerTime)
        assertThat(restored.recurrence).isEqualTo(original.recurrence)
        assertThat(restored.isCompleted).isEqualTo(original.isCompleted)
        assertThat(restored.sourceTranscript).isEqualTo(original.sourceTranscript)
        assertThat(restored.createdAt).isEqualTo(original.createdAt)
        assertThat(restored.locationTriggerJson).isEqualTo(original.locationTriggerJson)
        assertThat(restored.locationState).isEqualTo(original.locationState)
        assertThat(restored.formattingProvider).isEqualTo(original.formattingProvider)
        assertThat(restored.geofencingDevice).isEqualTo(original.geofencingDevice)
        assertThat(restored.createdBy).isEqualTo(original.createdBy)
        assertThat(restored.lastModifiedBy).isEqualTo(original.lastModifiedBy)
        assertThat(restored.updatedAt).isEqualTo(original.updatedAt)
    }

    @Test
    fun `serialize and deserialize reminder with nulls`() {
        val original = WatchReminder(
            id = "minimal",
            title = "Simple",
            sourceTranscript = "just a note",
            createdAt = Instant.ofEpochMilli(1000),
            updatedAt = Instant.ofEpochMilli(2000)
        )

        val bytes = ReminderSerializer.serialize(original)
        val restored = ReminderSerializer.deserialize(bytes)

        assertThat(restored.body).isNull()
        assertThat(restored.triggerTime).isNull()
        assertThat(restored.recurrence).isNull()
        assertThat(restored.locationTriggerJson).isNull()
        assertThat(restored.locationState).isNull()
        assertThat(restored.isCompleted).isFalse()
        assertThat(restored.title).isEqualTo("Simple")
    }

    @Test
    fun `serialize and deserialize list round-trip`() {
        val reminders = listOf(
            fullReminder(id = "r-1", title = "First"),
            fullReminder(id = "r-2", title = "Second"),
            fullReminder(id = "r-3", title = "Third")
        )

        val bytes = ReminderSerializer.serializeList(reminders)
        val restored = ReminderSerializer.deserializeList(bytes)

        assertThat(restored).hasSize(3)
        assertThat(restored.map { it.id }).containsExactly("r-1", "r-2", "r-3").inOrder()
        assertThat(restored.map { it.title }).containsExactly("First", "Second", "Third").inOrder()
    }

    @Test
    fun `serialize and deserialize empty list`() {
        val bytes = ReminderSerializer.serializeList(emptyList())
        val restored = ReminderSerializer.deserializeList(bytes)

        assertThat(restored).isEmpty()
    }

    @Test
    fun `serialize and deserialize sync state round-trip`() {
        val state = SyncStateDto(
            activeReminders = listOf(
                ReminderDto(
                    id = "r-1",
                    title = "Buy milk",
                    sourceTranscript = "remind me",
                    createdAt = 1000L,
                    updatedAt = 2000L
                )
            ),
            tombstones = listOf(
                DeletedReminderDto(
                    id = "r-deleted",
                    originalTitle = "Old task",
                    deletedAt = 3000L,
                    deletedBy = "watch",
                    originalUpdatedAt = 2500L
                )
            ),
            deviceId = "phone-1"
        )

        val bytes = ReminderSerializer.serializeSyncState(state)
        val restored = ReminderSerializer.deserializeSyncState(bytes)

        assertThat(restored.deviceId).isEqualTo("phone-1")
        assertThat(restored.activeReminders).hasSize(1)
        assertThat(restored.activeReminders.first().id).isEqualTo("r-1")
        assertThat(restored.tombstones).hasSize(1)
        assertThat(restored.tombstones.first().id).isEqualTo("r-deleted")
    }

    @Test
    fun `serialize and deserialize deleted reminder round-trip`() {
        val dto = DeletedReminderDto(
            id = "del-1",
            originalTitle = "Expired task",
            deletedAt = 5000L,
            deletedBy = "phone",
            originalUpdatedAt = 4000L
        )

        val bytes = ReminderSerializer.serializeDeletedReminder(dto)
        val restored = ReminderSerializer.deserializeDeletedReminder(bytes)

        assertThat(restored.id).isEqualTo("del-1")
        assertThat(restored.originalTitle).isEqualTo("Expired task")
        assertThat(restored.deletedAt).isEqualTo(5000L)
        assertThat(restored.deletedBy).isEqualTo("phone")
        assertThat(restored.originalUpdatedAt).isEqualTo(4000L)
    }

    @Test
    fun `toWatchReminder converts all fields correctly`() {
        val dto = ReminderDto(
            id = "dto-1",
            title = "Task from phone",
            body = "Details here",
            triggerTime = 1700000000000L,
            recurrence = "weekly",
            isCompleted = true,
            sourceTranscript = "do the thing",
            createdAt = 1699900000000L,
            locationTriggerJson = """{"place":"store"}""",
            locationState = "TRIGGERED",
            formattingProvider = "openai",
            geofencingDevice = "watch",
            updatedAt = 1700001000000L,
            createdBy = "phone",
            lastModifiedBy = "phone"
        )

        val entity = ReminderSerializer.toWatchReminder(dto)

        assertThat(entity.id).isEqualTo("dto-1")
        assertThat(entity.title).isEqualTo("Task from phone")
        assertThat(entity.body).isEqualTo("Details here")
        assertThat(entity.triggerTime).isEqualTo(Instant.ofEpochMilli(1700000000000L))
        assertThat(entity.recurrence).isEqualTo("weekly")
        assertThat(entity.isCompleted).isTrue()
        assertThat(entity.sourceTranscript).isEqualTo("do the thing")
        assertThat(entity.createdAt).isEqualTo(Instant.ofEpochMilli(1699900000000L))
        assertThat(entity.locationTriggerJson).isEqualTo("""{"place":"store"}""")
        assertThat(entity.locationState).isEqualTo("TRIGGERED")
        assertThat(entity.formattingProvider).isEqualTo("openai")
        assertThat(entity.geofencingDevice).isEqualTo("watch")
        assertThat(entity.updatedAt).isEqualTo(Instant.ofEpochMilli(1700001000000L))
        assertThat(entity.createdBy).isEqualTo("phone")
        assertThat(entity.lastModifiedBy).isEqualTo("phone")
    }

    @Test
    fun `toDeletedReminder converts all fields correctly`() {
        val dto = DeletedReminderDto(
            id = "del-1",
            originalTitle = "Old reminder",
            deletedAt = 1700002000000L,
            deletedBy = "watch",
            originalUpdatedAt = 1700001000000L
        )

        val entity = ReminderSerializer.toDeletedReminder(dto)

        assertThat(entity.id).isEqualTo("del-1")
        assertThat(entity.originalTitle).isEqualTo("Old reminder")
        assertThat(entity.deletedAt).isEqualTo(Instant.ofEpochMilli(1700002000000L))
        assertThat(entity.deletedBy).isEqualTo("watch")
        assertThat(entity.originalUpdatedAt).isEqualTo(Instant.ofEpochMilli(1700001000000L))
    }
}
