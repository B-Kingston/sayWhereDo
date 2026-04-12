package com.example.reminders.alarm

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

class RecurrenceHelperTest {

    @Test
    fun `computeNextOccurrence daily advances by one day`() {
        val base = Instant.parse("2026-04-11T09:00:00Z")

        val next = computeNextOccurrence(base, RecurrencePattern.DAILY)

        assertThat(next).isEqualTo(Instant.parse("2026-04-12T09:00:00Z"))
    }

    @Test
    fun `computeNextOccurrence weekly advances by seven days`() {
        val base = Instant.parse("2026-04-11T14:30:00Z")

        val next = computeNextOccurrence(base, RecurrencePattern.WEEKLY)

        assertThat(next).isEqualTo(Instant.parse("2026-04-18T14:30:00Z"))
    }

    @Test
    fun `computeNextOccurrence monthly advances by one month`() {
        val base = Instant.parse("2026-01-15T10:00:00Z")

        val next = computeNextOccurrence(base, RecurrencePattern.MONTHLY)

        assertThat(next).isEqualTo(Instant.parse("2026-02-15T10:00:00Z"))
    }

    @Test
    fun `computeNextOccurrence monthly january31 clamps to february28`() {
        val base = Instant.parse("2026-01-31T10:00:00Z")

        val next = computeNextOccurrence(base, RecurrencePattern.MONTHLY)

        // 2026 is not a leap year — Feb has 28 days
        assertThat(next).isEqualTo(Instant.parse("2026-02-28T10:00:00Z"))
    }

    @Test
    fun `computeNextOccurrence monthly january31 leapYear clamps to february29`() {
        // 2024 is a leap year
        val base = Instant.parse("2024-01-31T10:00:00Z")

        val next = computeNextOccurrence(base, RecurrencePattern.MONTHLY)

        assertThat(next).isEqualTo(Instant.parse("2024-02-29T10:00:00Z"))
    }

    @Test
    fun `computeNextOccurrence monthly may31 clamps to june30`() {
        val base = Instant.parse("2026-05-31T10:00:00Z")

        val next = computeNextOccurrence(base, RecurrencePattern.MONTHLY)

        assertThat(next).isEqualTo(Instant.parse("2026-06-30T10:00:00Z"))
    }

    @Test
    fun `computeNextOccurrence monthly december31 wraps to january31`() {
        val base = Instant.parse("2026-12-31T10:00:00Z")

        val next = computeNextOccurrence(base, RecurrencePattern.MONTHLY)

        assertThat(next).isEqualTo(Instant.parse("2027-01-31T10:00:00Z"))
    }

    @Test
    fun `computeNextOccurrence preserves time of day`() {
        val base = Instant.parse("2026-04-11T23:59:59Z")

        val next = computeNextOccurrence(base, RecurrencePattern.DAILY)

        assertThat(next).isEqualTo(Instant.parse("2026-04-12T23:59:59Z"))
    }

    @Test
    fun `computeNextOccurrence handles DST boundary`() {
        // US DST spring forward: 2026-03-08 at 2am -> 3am
        // Using UTC avoids local TZ issues in test, but the computation
        // should still produce the correct wall-clock time
        val base = Instant.parse("2026-03-08T07:00:00Z") // 2am EST -> 3am EDT

        val next = computeNextOccurrence(base, RecurrencePattern.DAILY)

        // Next day at same UTC instant (wall clock may differ by DST offset)
        assertThat(next).isEqualTo(Instant.parse("2026-03-09T07:00:00Z"))
    }

    @Test
    fun `RecurrencePattern fromString parses valid values`() {
        assertThat(RecurrencePattern.fromString("DAILY")).isEqualTo(RecurrencePattern.DAILY)
        assertThat(RecurrencePattern.fromString("WEEKLY")).isEqualTo(RecurrencePattern.WEEKLY)
        assertThat(RecurrencePattern.fromString("MONTHLY")).isEqualTo(RecurrencePattern.MONTHLY)
    }

    @Test
    fun `RecurrencePattern fromString is case insensitive`() {
        assertThat(RecurrencePattern.fromString("daily")).isEqualTo(RecurrencePattern.DAILY)
        assertThat(RecurrencePattern.fromString("weekly")).isEqualTo(RecurrencePattern.WEEKLY)
        assertThat(RecurrencePattern.fromString("monthly")).isEqualTo(RecurrencePattern.MONTHLY)
    }

    @Test
    fun `RecurrencePattern fromString returns null for invalid value`() {
        assertThat(RecurrencePattern.fromString("YEARLY")).isNull()
        assertThat(RecurrencePattern.fromString("")).isNull()
        assertThat(RecurrencePattern.fromString("invalid")).isNull()
    }

    @Test
    fun `computeNextOccurrence monthly feb29 leap year advances to march29`() {
        val base = Instant.parse("2024-02-29T10:00:00Z")

        val next = computeNextOccurrence(base, RecurrencePattern.MONTHLY)

        // March has 31 days, so day 29 fits — no clamping needed
        assertThat(next).isEqualTo(Instant.parse("2024-03-29T10:00:00Z"))
    }

    @Test
    fun `computeNextOccurrence monthly oct31 clamps to nov30`() {
        val base = Instant.parse("2026-10-31T10:00:00Z")

        val next = computeNextOccurrence(base, RecurrencePattern.MONTHLY)

        assertThat(next).isEqualTo(Instant.parse("2026-11-30T10:00:00Z"))
    }
}
