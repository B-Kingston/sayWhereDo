package com.example.reminders.ui.screen

/**
 * Represents the UI state of the transcription screen.
 *
 * The state machine progresses as follows:
 * ```
 * Idle → Listening → Processing → Result
 *                          ↘ Error
 * ```
 * Any state can transition back to [Idle] via the reset action.
 */
sealed interface TranscriptionUiState {

    /** Initial state — no recognition session is active. */
    data object Idle : TranscriptionUiState

    /** The engine is actively listening for speech input. */
    data object Listening : TranscriptionUiState

    /** Speech has ended and the engine is producing final results. */
    data object Processing : TranscriptionUiState

    /** Transcription completed successfully. */
    data class Result(val text: String) : TranscriptionUiState

    /** An error occurred during transcription. */
    data class Error(val message: String) : TranscriptionUiState
}
