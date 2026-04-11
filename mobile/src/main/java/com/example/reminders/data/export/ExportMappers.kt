package com.example.reminders.data.export

import com.example.reminders.data.model.LocationReminderState
import com.example.reminders.data.model.LocationTrigger
import com.example.reminders.data.model.Reminder
import com.example.reminders.data.model.SavedPlace
import kotlinx.serialization.json.Json
import java.time.Instant

/**
 * Extension functions for converting between domain models and
 * their export representations.
 *
 * These mappings are intentionally placed in a separate file from
 * the data classes to keep the domain model free of export concerns.
 */

private val json = Json { ignoreUnknownKeys = true }

/**
 * Converts a [Reminder] to its export representation.
 */
fun Reminder.toExportReminder(): ExportReminder = ExportReminder(
    id = id,
    title = title,
    body = body,
    triggerTimeEpochMillis = triggerTime?.toEpochMilli(),
    recurrence = recurrence,
    locationTriggerJson = locationTrigger?.let {
        json.encodeToString(LocationTrigger.serializer(), it)
    },
    locationState = locationState?.name,
    sourceTranscript = sourceTranscript,
    formattingProvider = formattingProvider,
    isCompleted = isCompleted,
    createdAtEpochMillis = createdAt.toEpochMilli(),
    updatedAtEpochMillis = updatedAt.toEpochMilli()
)

/**
 * Converts an [ExportReminder] back to a domain [Reminder].
 *
 * Imported reminders have their [formattingProvider] set to
 * "imported" to distinguish them from user-created data.
 */
fun ExportReminder.toReminder(): Reminder = Reminder(
    id = id,
    title = title,
    body = body,
    triggerTime = triggerTimeEpochMillis?.let { Instant.ofEpochMilli(it) },
    recurrence = recurrence,
    locationTrigger = locationTriggerJson?.let {
        json.decodeFromString<LocationTrigger>(it)
    },
    locationState = locationState?.let {
        runCatching { LocationReminderState.valueOf(it) }.getOrNull()
    },
    sourceTranscript = sourceTranscript,
    formattingProvider = IMPORTED_PROVIDER,
    isCompleted = isCompleted,
    createdAt = Instant.ofEpochMilli(createdAtEpochMillis),
    updatedAt = Instant.ofEpochMilli(updatedAtEpochMillis)
)

/**
 * Converts a [SavedPlace] to its export representation.
 */
fun SavedPlace.toExportSavedPlace(): ExportSavedPlace = ExportSavedPlace(
    id = id,
    label = label,
    address = address,
    latitude = latitude,
    longitude = longitude,
    defaultRadiusMetres = defaultRadiusMetres
)

/**
 * Converts an [ExportSavedPlace] back to a domain [SavedPlace].
 */
fun ExportSavedPlace.toSavedPlace(): SavedPlace = SavedPlace(
    id = id,
    label = label,
    address = address,
    latitude = latitude,
    longitude = longitude,
    defaultRadiusMetres = defaultRadiusMetres
)

private const val IMPORTED_PROVIDER = "imported"
