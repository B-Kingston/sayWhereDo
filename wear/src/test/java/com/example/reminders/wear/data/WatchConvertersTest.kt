package com.example.reminders.wear.data

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Instant

class WatchConvertersTest {

    private val converters = WatchConverters()

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
    fun `instant round trips with large value`() {
        val instant = Instant.ofEpochMilli(Long.MAX_VALUE / 2)
        val millis = converters.fromInstant(instant)
        assertThat(converters.toInstant(millis)).isEqualTo(instant)
    }
}
