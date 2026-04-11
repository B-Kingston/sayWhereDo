package com.example.reminders.formatting

/**
 * Abstraction over transcript formatting backends.
 *
 * Implementations take a raw voice transcript and attempt to parse it into
 * structured [com.example.reminders.data.model.ParsedReminder] objects.
 * The concrete implementation is selected based on user configuration
 * (e.g. Gemini cloud formatting, or raw fallback when no API key is set).
 */
interface FormattingProvider {

    /**
     * Formats the given [transcript] into structured reminders.
     *
     * @param transcript The raw text produced by the speech recognition engine.
     * @return A [FormattingResult] indicating success, partial success, failure,
     *         or usage-limited status.
     */
    suspend fun format(transcript: String): FormattingResult
}
