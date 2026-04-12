package com.example.reminders.wear.sync

import com.example.reminders.wear.data.WatchReminder
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.time.Instant

object ReminderSerializer {

    private val json = Json { ignoreUnknownKeys = true }

    fun serialize(reminder: WatchReminder): ByteArray {
        val dto = reminder.toDto()
        return json.encodeToString(ReminderDto.serializer(), dto).toByteArray(Charsets.UTF_8)
    }

    fun deserialize(bytes: ByteArray): WatchReminder {
        val dto = json.decodeFromString(ReminderDto.serializer(), bytes.toString(Charsets.UTF_8))
        return dto.toEntity()
    }

    fun serializeList(reminders: List<WatchReminder>): ByteArray {
        val dtos = reminders.map { it.toDto() }
        return json.encodeToString(ListSerializer(ReminderDto.serializer()), dtos).toByteArray(Charsets.UTF_8)
    }

    fun deserializeList(bytes: ByteArray): List<WatchReminder> {
        val dtos = json.decodeFromString(ListSerializer(ReminderDto.serializer()), bytes.toString(Charsets.UTF_8))
        return dtos.map { it.toEntity() }
    }

    private fun WatchReminder.toDto() = ReminderDto(
        id = id,
        title = title,
        body = body,
        triggerTime = triggerTime?.toEpochMilli(),
        recurrence = recurrence,
        isCompleted = isCompleted,
        sourceTranscript = sourceTranscript,
        createdAt = createdAt.toEpochMilli(),
        locationTriggerJson = locationTriggerJson,
        locationState = locationState,
        formattingProvider = formattingProvider,
        geofencingDevice = geofencingDevice,
        updatedAt = updatedAt.toEpochMilli()
    )

    private fun ReminderDto.toEntity() = WatchReminder(
        id = id,
        title = title,
        body = body,
        triggerTime = triggerTime?.let { Instant.ofEpochMilli(it) },
        recurrence = recurrence,
        isCompleted = isCompleted,
        sourceTranscript = sourceTranscript,
        createdAt = Instant.ofEpochMilli(createdAt),
        locationTriggerJson = locationTriggerJson,
        locationState = locationState,
        formattingProvider = formattingProvider,
        geofencingDevice = geofencingDevice,
        updatedAt = Instant.ofEpochMilli(updatedAt)
    )
}
