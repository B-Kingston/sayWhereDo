package com.example.reminders.alarm

import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Supported recurrence patterns for reminders.
 *
 * Each pattern computes the next occurrence from a given trigger time.
 * Recurrence is a **Pro-only** feature — free users see the option
 * greyed out with a Pro badge in the edit screen.
 */
enum class RecurrencePattern {
    DAILY,
    WEEKLY,
    MONTHLY;

    companion object {
        /**
         * Parses a recurrence pattern from its string representation.
         * Returns `null` if the value does not match any pattern.
         */
        fun fromString(value: String): RecurrencePattern? =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
    }
}

/**
 * Computes the next occurrence for the given [pattern] starting
 * from [triggerTime].
 *
 * Handles edge cases:
 * - Month-end overflow (e.g. Jan 31 → Feb 28)
 * - Leap year (Feb 29 → Feb 28 in non-leap years)
 * - DST boundaries (clock shifts are absorbed by [ZoneId.systemDefault])
 *
 * @param triggerTime The current trigger time.
 * @param pattern     The recurrence pattern to apply.
 * @return The next trigger time, or `null` if pattern is `null`.
 */
fun computeNextOccurrence(
    triggerTime: Instant,
    pattern: RecurrencePattern
): Instant {
    val zoned = triggerTime.atZone(ZoneId.systemDefault())

    return when (pattern) {
        RecurrencePattern.DAILY -> zoned.plusDays(1).toInstant()

        RecurrencePattern.WEEKLY -> zoned.plusWeeks(1).toInstant()

        RecurrencePattern.MONTHLY -> advanceMonth(zoned).toInstant()
    }
}

/**
 * Advances the date by one month, clamping the day-of-month when the
 * target month has fewer days than the current one.
 *
 * Examples:
 * - Jan 31 → Feb 28 (or 29 in a leap year)
 * - May 31 → Jun 30
 * - Dec 31 → Jan 31
 */
private fun advanceMonth(zonedDateTime: ZonedDateTime): ZonedDateTime {
    val next = zonedDateTime.plusMonths(1)

    // If the day-of-month shifted, the target month is shorter — clamp
    if (next.dayOfMonth != zonedDateTime.dayOfMonth) {
        val lastDay = next.month.length(
            next.toLocalDate().isLeapYear
        )
        return next.withDayOfMonth(lastDay)
    }

    return next
}
