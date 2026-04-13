package com.example.reminders.wear.ui.component

/**
 * Represents the different ways a user can provide reminder input on WearOS.
 *
 * Each variant corresponds to a distinct capture strategy with different
 * capabilities, latency, and phone-dependency characteristics. The UI uses
 * this to select the appropriate input screen and to communicate which path
 * was taken when handing off to the formatting pipeline.
 */
sealed interface InputMethod {

    /**
     * On-device text input via the WearOS keyboard (or handwriting IME).
     * No phone connection required. The raw text is sent straight to
     * formatting on the phone via the Data Layer.
     */
    data object Keyboard : InputMethod

    /**
     * On-watch voice recognition using Android's built-in
     * [android.speech.RecognizerIntent]. Works without a phone when the
     * watch has a downloaded speech pack, but availability varies by OEM.
     */
    data object VoiceOnWatch : InputMethod

    /**
     * Capture audio on the watch and stream it to the phone for
     * transcription via [android.speech.SpeechRecognizer]. Used as a
     * fallback when on-watch recognition is unavailable.
     */
    data object VoiceStreamToPhone : InputMethod

    /**
     * On-watch voice capture with cloud-based formatting executed directly
     * from the watch (V2 path). Requires network access and a user-supplied
     * API key; bypasses the phone entirely for the formatting step.
     */
    data object CloudFormatOnWatch : InputMethod
}
