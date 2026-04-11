package com.example.reminders.wear.data

import androidx.room.TypeConverter
import java.time.Instant

class WatchConverters {

    @TypeConverter
    fun fromInstant(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    fun toInstant(value: Long?): Instant? = value?.let { Instant.ofEpochMilli(it) }
}
