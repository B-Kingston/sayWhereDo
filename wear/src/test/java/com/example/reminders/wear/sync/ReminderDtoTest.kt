package com.example.reminders.wear.sync

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import org.junit.Test

class ReminderDtoTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `reminder DTO has correct default values`() {
        val dto = ReminderDto(
            id = "test-1",
            title = "Test",
            sourceTranscript = "transcript",
            createdAt = 1000L,
            updatedAt = 2000L
        )

        assertThat(dto.geofencingDevice).isEqualTo("watch")
        assertThat(dto.createdBy).isEqualTo("watch")
        assertThat(dto.lastModifiedBy).isEqualTo("watch")
        assertThat(dto.formattingProvider).isEqualTo("none")
        assertThat(dto.isCompleted).isFalse()
        assertThat(dto.body).isNull()
        assertThat(dto.triggerTime).isNull()
        assertThat(dto.recurrence).isNull()
        assertThat(dto.locationTriggerJson).isNull()
        assertThat(dto.locationState).isNull()
    }

    @Test
    fun `reminder DTO serializes to expected JSON structure`() {
        val dto = ReminderDto(
            id = "abc-123",
            title = "Buy milk",
            body = "Whole milk",
            triggerTime = 1700000000000L,
            recurrence = "daily",
            isCompleted = true,
            sourceTranscript = "remind me to buy milk",
            createdAt = 1699900000000L,
            locationTriggerJson = """{"lat":0.0}""",
            locationState = "ACTIVE",
            updatedAt = 1700001000000L,
            createdBy = "phone",
            lastModifiedBy = "phone"
        )

        val jsonString = json.encodeToString(ReminderDto.serializer(), dto)

        assertThat(jsonString).contains("\"id\":\"abc-123\"")
        assertThat(jsonString).contains("\"title\":\"Buy milk\"")
        assertThat(jsonString).contains("\"body\":\"Whole milk\"")
        assertThat(jsonString).contains("\"triggerTime\":1700000000000")
        assertThat(jsonString).contains("\"recurrence\":\"daily\"")
        assertThat(jsonString).contains("\"isCompleted\":true")
        assertThat(jsonString).contains("\"sourceTranscript\":\"remind me to buy milk\"")
        assertThat(jsonString).contains("\"createdAt\":1699900000000")
        assertThat(jsonString).contains("\"updatedAt\":1700001000000")
        assertThat(jsonString).contains("\"locationState\":\"ACTIVE\"")
        assertThat(jsonString).contains("\"createdBy\":\"phone\"")
    }

    @Test
    fun `reminder DTO deserializes from JSON`() {
        val jsonString = """{
            "id":"xyz-789",
            "title":"Walk dog",
            "body":"Morning walk",
            "triggerTime":1700000000000,
            "recurrence":"weekly",
            "isCompleted":true,
            "sourceTranscript":"walk the dog",
            "createdAt":1699900000000,
            "locationTriggerJson":"{\"place\":\"park\"}",
            "locationState":"ACTIVE",
            "formattingProvider":"gemini",
            "geofencingDevice":"phone",
            "updatedAt":1700001000000,
            "createdBy":"phone",
            "lastModifiedBy":"watch"
        }"""

        val dto = json.decodeFromString(ReminderDto.serializer(), jsonString)

        assertThat(dto.id).isEqualTo("xyz-789")
        assertThat(dto.title).isEqualTo("Walk dog")
        assertThat(dto.body).isEqualTo("Morning walk")
        assertThat(dto.triggerTime).isEqualTo(1700000000000L)
        assertThat(dto.recurrence).isEqualTo("weekly")
        assertThat(dto.isCompleted).isTrue()
        assertThat(dto.sourceTranscript).isEqualTo("walk the dog")
        assertThat(dto.createdAt).isEqualTo(1699900000000L)
        assertThat(dto.locationTriggerJson).isEqualTo("{\"place\":\"park\"}")
        assertThat(dto.locationState).isEqualTo("ACTIVE")
        assertThat(dto.formattingProvider).isEqualTo("gemini")
        assertThat(dto.geofencingDevice).isEqualTo("phone")
        assertThat(dto.updatedAt).isEqualTo(1700001000000L)
        assertThat(dto.createdBy).isEqualTo("phone")
        assertThat(dto.lastModifiedBy).isEqualTo("watch")
    }

    @Test
    fun `deleted reminder DTO round-trip`() {
        val dto = DeletedReminderDto(
            id = "del-1",
            originalTitle = "Expired task",
            deletedAt = 5000L,
            deletedBy = "watch",
            originalUpdatedAt = 4000L
        )

        val jsonString = json.encodeToString(DeletedReminderDto.serializer(), dto)
        val restored = json.decodeFromString(DeletedReminderDto.serializer(), jsonString)

        assertThat(restored).isEqualTo(dto)
    }

    @Test
    fun `sync state DTO round-trip`() {
        val state = SyncStateDto(
            activeReminders = listOf(
                ReminderDto(
                    id = "r-1",
                    title = "First",
                    sourceTranscript = "one",
                    createdAt = 1000L,
                    updatedAt = 2000L
                ),
                ReminderDto(
                    id = "r-2",
                    title = "Second",
                    sourceTranscript = "two",
                    createdAt = 3000L,
                    updatedAt = 4000L
                )
            ),
            tombstones = listOf(
                DeletedReminderDto(
                    id = "d-1",
                    originalTitle = "Gone",
                    deletedAt = 5000L,
                    deletedBy = "phone",
                    originalUpdatedAt = 4500L
                )
            ),
            deviceId = "watch-42"
        )

        val jsonString = json.encodeToString(SyncStateDto.serializer(), state)
        val restored = json.decodeFromString(SyncStateDto.serializer(), jsonString)

        assertThat(restored.deviceId).isEqualTo("watch-42")
        assertThat(restored.activeReminders).hasSize(2)
        assertThat(restored.activeReminders.first().id).isEqualTo("r-1")
        assertThat(restored.activeReminders.last().id).isEqualTo("r-2")
        assertThat(restored.tombstones).hasSize(1)
        assertThat(restored.tombstones.first().id).isEqualTo("d-1")
    }

    @Test
    fun `reminder DTO ignores unknown keys`() {
        val jsonString = """{
            "id":"safe-1",
            "title":"Works",
            "sourceTranscript":"ok",
            "createdAt":1000,
            "updatedAt":2000,
            "futureField":"should be ignored",
            "anotherUnknown":42
        }"""

        val dto = json.decodeFromString(ReminderDto.serializer(), jsonString)

        assertThat(dto.id).isEqualTo("safe-1")
        assertThat(dto.title).isEqualTo("Works")
    }
}
