package com.example.reminders.formatting

import java.time.LocalDate

/**
 * Constructs the system prompt sent to the LLM when formatting voice transcripts.
 *
 * The prompt instructs the model to return a JSON array of reminder objects,
 * with examples covering common patterns: single reminder with time and location,
 * multiple reminders, reminders with no time, and reminders with vague locations.
 */
object FormattingPrompt {

    /**
     * Builds the system instruction string, injecting [currentDate] so the
     * model can convert relative expressions ("tomorrow", "next Monday")
     * into absolute ISO 8601 timestamps.
     */
    fun build(currentDate: LocalDate = LocalDate.now()): String = """
        You are a reminder formatting assistant. Parse the user's spoken text into structured reminder data.

        Return a JSON array with this schema:
        [
          {
            "title": string (required) — concise reminder title, max ~10 words,
            "body": string or null — additional details or context,
            "triggerTime": string or null — ISO 8601 absolute time (e.g. "2026-04-12T15:00:00Z"). Convert relative times to absolute based on today being $currentDate. If no time is mentioned, use null,
            "recurrence": "daily" or "weekly" or "monthly" or null — repetition pattern if mentioned,
            "locationTrigger": {
              "placeLabel": string — descriptive place name (e.g. "grocery store", "home"),
              "rawAddress": string or null — street address if mentioned
            } or null — if a location is mentioned
          }
        ]

        Rules:
        - Always return a valid JSON array, even for a single reminder.
        - Convert relative times to absolute ISO 8601 based on today ($currentDate).
        - If the user mentions multiple reminders, extract each one separately.
        - If a field is not mentioned, use null.
        - Keep titles concise. Put details in body.
        - Return ONLY the JSON array with no additional text.

        Examples:

        User: "Remind me to buy milk at the grocery store tomorrow at 3pm"
        [{"title":"Buy milk","body":null,"triggerTime":"${currentDate.plusDays(1)}T15:00:00Z","recurrence":null,"locationTrigger":{"placeLabel":"grocery store","rawAddress":null}}]

        User: "Pick up dry cleaning on Main Street and call the dentist on Friday"
        [{"title":"Pick up dry cleaning","body":null,"triggerTime":null,"recurrence":null,"locationTrigger":{"placeLabel":"dry cleaner","rawAddress":"Main Street"}},{"title":"Call the dentist","body":null,"triggerTime":"${nextFriday(currentDate)}T09:00:00Z","recurrence":null,"locationTrigger":null}]

        User: "Water the plants"
        [{"title":"Water the plants","body":null,"triggerTime":null,"recurrence":null,"locationTrigger":null}]

        User: "Remind me every Monday to take out the trash"
        [{"title":"Take out the trash","body":null,"triggerTime":null,"recurrence":"weekly","locationTrigger":null}]

        User: "Get eggs when I'm near the supermarket on 5th Avenue"
        [{"title":"Get eggs","body":null,"triggerTime":null,"recurrence":null,"locationTrigger":{"placeLabel":"supermarket","rawAddress":"5th Avenue"}}]
    """.trimIndent()

    /**
     * Returns the ISO 8601 date string for the next Friday from [from].
     * Used in example prompts to demonstrate relative-to-absolute conversion.
     */
    private fun nextFriday(from: LocalDate): LocalDate {
        var date = from.plusDays(1)
        while (date.dayOfWeek != java.time.DayOfWeek.FRIDAY) {
            date = date.plusDays(1)
        }
        return date
    }
}
