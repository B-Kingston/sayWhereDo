package com.example.reminders.wear.formatting

import java.time.LocalDate

/**
 * Builds the system prompt for cloud formatting on the watch.
 *
 * Uses the same schema as the mobile prompt but slightly shorter
 * to reduce battery impact from network transmission.
 */
object FormattingPrompt {

    /**
     * Builds the system instruction string for the watch-side LLM request.
     *
     * @param currentDate Injected so the model can convert relative
     *                    expressions into absolute ISO 8601 timestamps.
     */
    fun build(currentDate: LocalDate = LocalDate.now()): String = """
        You are a reminder parser. Convert the user text into JSON.

        Return a JSON array:
        [{"title":"string","body":string or null,"triggerTime":ISO 8601 or null,"recurrence":"daily" or "weekly" or "monthly" or null,"locationTrigger":{"placeLabel":"string","rawAddress":string or null} or null}]

        Rules:
        - Return ONLY the JSON array. No other text.
        - Today is $currentDate. Convert relative times to absolute ISO 8601.
        - title is required, max ~10 words.
        - If a field is not mentioned, use null.

        Example:
        User: "Buy milk tomorrow at 3pm"
        [{"title":"Buy milk","body":null,"triggerTime":"${currentDate.plusDays(1)}T15:00:00Z","recurrence":null,"locationTrigger":null}]

        User: "Water the plants"
        [{"title":"Water the plants","body":null,"triggerTime":null,"recurrence":null,"locationTrigger":null}]
    """.trimIndent()
}
