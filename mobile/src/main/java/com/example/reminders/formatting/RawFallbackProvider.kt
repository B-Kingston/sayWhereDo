package com.example.reminders.formatting

import android.util.Log
import com.example.reminders.data.model.ParsedReminder

/**
 * [FormattingProvider] that performs no LLM-based formatting.
 *
 * Used when the user has not configured an API key, or when cloud formatting
 * fails and the raw transcript must be preserved as a reminder so the user's
 * speech is never discarded.
 */
class RawFallbackProvider : FormattingProvider {

    /**
     * Returns a single [ParsedReminder] whose title is the raw [transcript].
     *
     * All optional fields (body, triggerTime, recurrence, locationTrigger) are null.
     */
    override suspend fun format(transcript: String): FormattingResult {
        Log.d(TAG, "Using raw fallback formatting for: ${transcript.take(80)}")
        val reminder = ParsedReminder(title = transcript)
        return FormattingResult.Success(listOf(reminder))
    }

    companion object {
        private const val TAG = "RawFallbackProvider"
    }
}
