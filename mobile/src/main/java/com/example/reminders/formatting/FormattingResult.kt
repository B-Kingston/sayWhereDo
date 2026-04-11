package com.example.reminders.formatting

import com.example.reminders.data.model.ParsedReminder

/**
 * Result of a formatting operation performed by a [FormattingProvider].
 *
 * Encapsulates the possible outcomes when attempting to format a raw
 * voice transcript into structured [ParsedReminder] objects.
 */
sealed interface FormattingResult {

    /**
     * Formatting succeeded and produced one or more valid reminders.
     *
     * @property reminders The fully-parsed reminders extracted from the transcript.
     */
    data class Success(val reminders: List<ParsedReminder>) : FormattingResult

    /**
     * Some reminders were parsed successfully, but others could not be extracted.
     *
     * The caller should persist the valid [reminders] and save [rawFallback]
     * as an unformatted reminder so the user's speech is never lost.
     *
     * @property reminders    The reminders that were successfully parsed.
     * @property rawFallback  The original transcript text to save as a fallback.
     */
    data class PartialSuccess(
        val reminders: List<ParsedReminder>,
        val rawFallback: String
    ) : FormattingResult

    /**
     * Formatting failed entirely (network error, malformed response, etc.).
     *
     * @property error A human-readable description of the failure.
     */
    data class Failure(val error: String) : FormattingResult

    /** Formatting was blocked because the user exceeded their free-tier usage limit. */
    data object UsageLimited : FormattingResult
}
