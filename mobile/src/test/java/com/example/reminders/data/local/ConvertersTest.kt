package com.example.reminders.data.local

import com.example.reminders.data.model.LocationReminderState
import com.example.reminders.data.model.LocationTrigger
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Instant

class ConvertersTest {

    private val converters = Converters()

    @Test
    fun `fromInstant null returns null`() {
        assertThat(converters.fromInstant(null)).isNull()
    }

    @Test
    fun `toInstant null returns null`() {
        assertThat(converters.toInstant(null)).isNull()
    }

    @Test
    fun `instant round trips correctly`() {
        val instant = Instant.ofEpochMilli(1700000000000L)
        val millis = converters.fromInstant(instant)
        assertThat(millis).isEqualTo(1700000000000L)
        assertThat(converters.toInstant(millis)).isEqualTo(instant)
    }

    @Test
    fun `locationTrigger null returns null`() {
        assertThat(converters.fromLocationTrigger(null)).isNull()
        assertThat(converters.toLocationTrigger(null)).isNull()
    }

    @Test
    fun `locationTrigger round trips correctly`() {
        val trigger = LocationTrigger(
            placeLabel = "Home",
            rawAddress = "123 Main St",
            latitude = 40.7128,
            longitude = -74.0060,
            radiusMetres = 200,
            triggerOnEnter = true,
            triggerOnExit = false,
            geofenceId = "geo-123"
        )
        val json = converters.fromLocationTrigger(trigger)
        assertThat(json).isNotNull()
        val restored = converters.toLocationTrigger(json)
        assertThat(restored).isEqualTo(trigger)
    }

    @Test
    fun `locationTrigger with nulls round trips correctly`() {
        val trigger = LocationTrigger(
            placeLabel = "Work",
            rawAddress = null,
            latitude = null,
            longitude = null
        )
        val json = converters.fromLocationTrigger(trigger)
        val restored = converters.toLocationTrigger(json)
        assertThat(restored).isEqualTo(trigger)
    }

    @Test
    fun `locationReminderState null returns null`() {
        assertThat(converters.fromLocationReminderState(null)).isNull()
        assertThat(converters.toLocationReminderState(null)).isNull()
    }

    @Test
    fun `locationReminderState round trips all values`() {
        for (state in LocationReminderState.entries) {
            val name = converters.fromLocationReminderState(state)
            assertThat(name).isEqualTo(state.name)
            assertThat(converters.toLocationReminderState(name)).isEqualTo(state)
        }
    }
}
