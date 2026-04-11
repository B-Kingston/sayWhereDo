package com.example.reminders.transcription

import kotlinx.coroutines.flow.StateFlow

/**
 * Internal state emitted by the speech recognition engine.
 *
 * The [TranscriptionViewModel] maps these raw engine events into
 * [com.example.reminders.ui.screen.TranscriptionUiState] for the UI layer.
 */
sealed interface RecognitionState {

    /** No recognition session is active. */
    data object Idle : RecognitionState

    /** The engine is listening for speech input. */
    data object Listening : RecognitionState

    /** The engine has produced partial (in-progress) transcription text. */
    data class PartialResult(val text: String) : RecognitionState

    /** The user has stopped speaking and the engine is finalising results. */
    data object Processing : RecognitionState

    /** Recognition completed successfully with the given [text]. */
    data class FinalResult(val text: String) : RecognitionState

    /** Recognition failed with the given platform [code] and human-readable [message]. */
    data class Error(val code: Int, val message: String) : RecognitionState
}

/**
 * Abstraction over the platform speech recognition engine.
 *
 * Implemented by [AndroidSpeechRecognitionManager] for production and
 * mocked in unit tests to verify [TranscriptionViewModel] behaviour.
 */
interface SpeechRecognitionManager {

    /** Hot flow of the current recognition state. */
    val recognitionState: StateFlow<RecognitionState>

    /**
     * Starts a new recognition session.
     *
     * If a session is already in progress it is stopped first.
     * The [recognitionState] flow will transition to [RecognitionState.Listening].
     */
    fun startListening()

    /**
     * Stops the current recognition session, requesting final results.
     * Ignored if no session is active.
     */
    fun stopListening()

    /** Releases all resources held by the recognition engine. */
    fun destroy()
}
