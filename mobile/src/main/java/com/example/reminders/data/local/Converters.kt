package com.example.reminders.data.local

import androidx.room.TypeConverter
import com.example.reminders.data.model.LocationReminderState
import com.example.reminders.data.model.LocationTrigger
import kotlinx.serialization.json.Json
import java.time.Instant

class Converters {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromInstant(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    fun toInstant(value: Long?): Instant? = value?.let { Instant.ofEpochMilli(it) }

    @TypeConverter
    fun fromLocationTrigger(value: LocationTrigger?): String? {
        return value?.let { json.encodeToString(LocationTrigger.serializer(), it) }
    }

    @TypeConverter
    fun toLocationTrigger(value: String?): LocationTrigger? {
        return value?.let { json.decodeFromString<LocationTrigger>(it) }
    }

    @TypeConverter
    fun fromLocationReminderState(value: LocationReminderState?): String? = value?.name

    @TypeConverter
    fun toLocationReminderState(value: String?): LocationReminderState? {
        return value?.let { LocationReminderState.valueOf(it) }
    }
}
