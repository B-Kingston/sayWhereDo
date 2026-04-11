package com.example.reminders.transcription

/**
 * Represents the available speech transcription backends.
 *
 * Currently only [AndroidBuiltIn] is implemented. [FutoWhisper] is a
 * placeholder for a future on-device Whisper model integration (BYOM).
 */
sealed interface TranscriptionBackend {

    /** Android's built-in [android.speech.SpeechRecognizer]. */
    data object AndroidBuiltIn : TranscriptionBackend

    /** Placeholder for FUTO Whisper on-device model. Not yet implemented. */
    data object FutoWhisper : TranscriptionBackend
}
