package com.example.reminders.ui.component

/**
 * Represents the available microphone-based transcription methods
 * on the mobile app.
 *
 * Each variant corresponds to a different transcription backend that
 * processes the user's voice input into text.
 */
sealed interface MicMethod {

    /** Uses Android's built-in speech recognition. */
    data object AndroidBuiltIn : MicMethod

    /**
     * Uses a cloud-based AI provider for transcription.
     *
     * @property providerName The display name of the configured provider.
     */
    data class CloudProvider(val providerName: String) : MicMethod

    /** Uses an on-device local LLM for transcription. */
    data object LocalModel : MicMethod
}
