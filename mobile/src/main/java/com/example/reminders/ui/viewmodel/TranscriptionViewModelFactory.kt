package com.example.reminders.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.reminders.transcription.SpeechRecognitionManager

/**
 * Factory that supplies a [SpeechRecognitionManager] instance to
 * [TranscriptionViewModel].
 *
 * Required because the manager is created at the Activity level (it needs
 * an Activity context for [android.speech.SpeechRecognizer]) and cannot
 * be provided by the default no-arg ViewModel constructor.
 */
class TranscriptionViewModelFactory(
    private val speechRecognitionManager: SpeechRecognitionManager
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(TranscriptionViewModel::class.java)) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        return TranscriptionViewModel(speechRecognitionManager) as T
    }
}
