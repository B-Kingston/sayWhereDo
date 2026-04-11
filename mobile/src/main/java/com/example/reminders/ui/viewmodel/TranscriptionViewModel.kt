package com.example.reminders.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.reminders.transcription.RecognitionState
import com.example.reminders.transcription.SpeechRecognitionManager
import com.example.reminders.ui.screen.TranscriptionUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel that bridges the [SpeechRecognitionManager] engine events
 * into [TranscriptionUiState] for consumption by the Compose UI.
 *
 * The ViewModel owns the observation lifecycle; the
 * [SpeechRecognitionManager] is created at the Activity level (it needs
 * an Activity context for [android.speech.SpeechRecognizer]) and passed
 * in via the factory.
 *
 * @param speechRecognitionManager The speech recognition backend to observe.
 */
class TranscriptionViewModel(
    private val speechRecognitionManager: SpeechRecognitionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<TranscriptionUiState>(TranscriptionUiState.Idle)
    val uiState: StateFlow<TranscriptionUiState> = _uiState.asStateFlow()

    /**
     * In-progress transcription text produced during the listening phase.
     * Cleared when a final result or error is received.
     */
    private val _partialText = MutableStateFlow("")
    val partialText: StateFlow<String> = _partialText.asStateFlow()

    init {
        viewModelScope.launch {
            speechRecognitionManager.recognitionState.collect { state ->
                _uiState.value = when (state) {
                    is RecognitionState.Idle -> TranscriptionUiState.Idle
                    is RecognitionState.Listening -> TranscriptionUiState.Listening
                    is RecognitionState.PartialResult -> {
                        _partialText.value = state.text
                        TranscriptionUiState.Listening
                    }
                    is RecognitionState.Processing -> {
                        _partialText.value = ""
                        TranscriptionUiState.Processing
                    }
                    is RecognitionState.FinalResult -> {
                        _partialText.value = ""
                        TranscriptionUiState.Result(state.text)
                    }
                    is RecognitionState.Error -> {
                        _partialText.value = ""
                        TranscriptionUiState.Error(state.message)
                    }
                }
            }
        }
    }

    /**
     * Starts a new recognition session.
     * If the manager is unavailable the error will flow through
     * [RecognitionState] and be mapped to [TranscriptionUiState.Error].
     */
    fun startListening() {
        speechRecognitionManager.startListening()
    }

    /** Stops the current recognition session, requesting final results. */
    fun stopListening() {
        speechRecognitionManager.stopListening()
    }

    /** Resets to [TranscriptionUiState.Idle], discarding any result or error. */
    fun reset() {
        _uiState.value = TranscriptionUiState.Idle
        _partialText.value = ""
    }

    override fun onCleared() {
        super.onCleared()
        speechRecognitionManager.destroy()
    }
}
