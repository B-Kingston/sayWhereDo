package com.example.reminders.wearable

import com.example.reminders.data.model.DeletedReminder
import com.example.reminders.data.model.LocationReminderState
import com.example.reminders.data.model.LocationTrigger
import com.example.reminders.data.model.Reminder
import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Test
import java.time.Instant

class ReminderDtoTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun testReminder(
        id: String = "test-1",
        title: String = "Buy groceries",
        body: String? = "Milk, eggs, bread",
        triggerTime: Instant? = Instant.parse("2026-04-15T10:00:00Z"),
        recurrence: String? = "daily",
        locationTrigger: LocationTrigger? = LocationTrigger(
            placeLabel = "Grocery Store",
            rawAddress = "123 Main St",
            latitude = 37.7749,
            longitude = -122.4194,
            radiusMetres = 150
        ),
        locationState: LocationReminderState? = LocationReminderState.ACTIVE,
        isCompleted: Boolean = false,
        sourceTranscript: String = "remind me to buy groceries",
        formattingProvider: String = "gemini",
        geofencingDevice: String = "phone",
        createdBy: String = "mobile",
        lastModifiedBy: String = "mobile",
        createdAt: Instant = Instant.parse("2026-04-15T08:00:00Z"),
        updatedAt: Instant = Instant.parse("2026-04-15T09:00:00Z")
    ) = Reminder(
        id = id,
        title = title,
        body = body,
        triggerTime = triggerTime,
        recurrence = recurrence,
        locationTrigger = locationTrigger,
        locationState = locationState,
        isCompleted = isCompleted,
        sourceTranscript = sourceTranscript,
        formattingProvider = formattingProvider,
        geofencingDevice = geofencingDevice,
        createdBy = createdBy,
        lastModifiedBy = lastModifiedBy,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    @Test
    fun `fromReminder preserves all fields`() {
        val reminder = testReminder()
        val dto = ReminderDto.fromReminder(reminder)

        assertThat(dto.id).isEqualTo(reminder.id)
        assertThat(dto.title).isEqualTo(reminder.title)
        assertThat(dto.body).isEqualTo(reminder.body)
        assertThat(dto.triggerTime).isEqualTo(reminder.triggerTime?.toEpochMilli())
        assertThat(dto.recurrence).isEqualTo(reminder.recurrence)
        assertThat(dto.isCompleted).isEqualTo(reminder.isCompleted)
        assertThat(dto.sourceTranscript).isEqualTo(reminder.sourceTranscript)
        assertThat(dto.formattingProvider).isEqualTo(reminder.formattingProvider)
        assertThat(dto.geofencingDevice).isEqualTo(reminder.geofencingDevice)
        assertThat(dto.createdBy).isEqualTo(reminder.createdBy)
        assertThat(dto.lastModifiedBy).isEqualTo(reminder.lastModifiedBy)
        assertThat(dto.createdAt).isEqualTo(reminder.createdAt.toEpochMilli())
        assertThat(dto.updatedAt).isEqualTo(reminder.updatedAt.toEpochMilli())
        assertThat(dto.locationState).isEqualTo(reminder.locationState?.name)
    }

    @Test
    fun `fromReminder handles null optional fields`() {
        val reminder = testReminder(
            body = null,
            triggerTime = null,
            recurrence = null,
            locationTrigger = null,
            locationState = null
        )
        val dto = ReminderDto.fromReminder(reminder)

        assertThat(dto.body).isNull()
        assertThat(dto.triggerTime).isNull()
        assertThat(dto.recurrence).isNull()
        assertThat(dto.locationTriggerJson).isNull()
        assertThat(dto.locationState).isNull()
    }

    @Test
    fun `fromReminder converts Instant timestamps to epoch millis`() {
        val createdAt = Instant.parse("2026-04-15T08:00:00Z")
        val updatedAt = Instant.parse("2026-04-15T09:00:00Z")
        val triggerTime = Instant.parse("2026-04-15T10:00:00Z")

        val reminder = testReminder(
            createdAt = createdAt,
            updatedAt = updatedAt,
            triggerTime = triggerTime
        )
        val dto = ReminderDto.fromReminder(reminder)

        assertThat(dto.createdAt).isEqualTo(createdAt.toEpochMilli())
        assertThat(dto.updatedAt).isEqualTo(updatedAt.toEpochMilli())
        assertThat(dto.triggerTime).isEqualTo(triggerTime.toEpochMilli())
    }

    @Test
    fun `reminder DTO serializes and deserializes correctly`() {
        val reminder = testReminder()
        val dto = ReminderDto.fromReminder(reminder)

        val serialized = Json.encodeToString(dto)
        val deserialized = Json.decodeFromString<ReminderDto>(serialized)

        assertThat(deserialized).isEqualTo(dto)
    }

    @Test
    fun `deleted reminder DTO round-trip preserves all fields`() {
        val now = Instant.now()
        val tombstone = DeletedReminder(
            id = "del-1",
            originalTitle = "Deleted Task",
            deletedAt = now,
            deletedBy = "mobile",
            originalUpdatedAt = now.minusSeconds(300)
        )
        val dto = DeletedReminderDto.fromDeletedReminder(tombstone)

        val serialized = Json.encodeToString(dto)
        val deserialized = Json.decodeFromString<DeletedReminderDto>(serialized)

        assertThat(deserialized.id).isEqualTo(tombstone.id)
        assertThat(deserialized.originalTitle).isEqualTo(tombstone.originalTitle)
        assertThat(deserialized.deletedAt).isEqualTo(tombstone.deletedAt.toEpochMilli())
        assertThat(deserialized.deletedBy).isEqualTo(tombstone.deletedBy)
        assertThat(deserialized.originalUpdatedAt).isEqualTo(tombstone.originalUpdatedAt.toEpochMilli())
    }

    @Test
    fun `sync state DTO round-trip preserves all fields`() {
        val now = Instant.now()
        val reminder = testReminder(updatedAt = now, createdAt = now)
        val tombstone = DeletedReminder(
            id = "del-1",
            originalTitle = "Gone",
            deletedAt = now,
            deletedBy = "watch",
            originalUpdatedAt = now.minusSeconds(60)
        )

        val syncState = SyncStateDto(
            activeReminders = listOf(ReminderDto.fromReminder(reminder)),
            tombstones = listOf(DeletedReminderDto.fromDeletedReminder(tombstone)),
            deviceId = "phone-device-1"
        )

        val serialized = Json.encodeToString(syncState)
        val deserialized = Json.decodeFromString<SyncStateDto>(serialized)

        assertThat(deserialized.deviceId).isEqualTo("phone-device-1")
        assertThat(deserialized.activeReminders).hasSize(1)
        assertThat(deserialized.activeReminders.first().id).isEqualTo(reminder.id)
        assertThat(deserialized.tombstones).hasSize(1)
        assertThat(deserialized.tombstones.first().id).isEqualTo(tombstone.id)
    }
}
