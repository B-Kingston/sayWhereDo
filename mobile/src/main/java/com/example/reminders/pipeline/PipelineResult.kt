package com.example.reminders.pipeline

import com.example.reminders.data.model.Reminder

/**
 * Result of the full transcription-to-storage pipeline, as returned by
 * [PipelineOrchestrator.processTranscript].
 *
 * Each variant indicates a different outcome that the UI layer may
 * present to the user.
 */
sealed interface PipelineResult {

    /**
     * All reminders were formatted and stored successfully.
     *
     * @property reminders The persisted [Reminder] entities.
     */
    data class Success(val reminders: List<Reminder>) : PipelineResult

    /**
     * Some reminders were stored, but formatting partially failed.
     * A raw fallback reminder was also saved to preserve the user's speech.
     *
     * @property reminders   The reminders that were successfully formatted and stored.
     * @property rawFallback The original transcript text that was saved as a fallback.
     */
    data class PartialSuccess(
        val reminders: List<Reminder>,
        val rawFallback: String
    ) : PipelineResult

    /**
     * Formatting failed entirely. A raw fallback reminder was saved.
     *
     * @property error A human-readable description of the failure.
     */
    data class Failure(val error: String) : PipelineResult

    /** The user exceeded their free-tier formatting limit. A raw fallback was saved. */
    data object UsageLimited : PipelineResult
}
