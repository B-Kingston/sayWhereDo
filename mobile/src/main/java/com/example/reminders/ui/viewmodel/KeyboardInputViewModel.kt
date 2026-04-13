package com.example.reminders.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.reminders.pipeline.PipelineOrchestrator
import com.example.reminders.pipeline.PipelineResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel for the keyboard-based reminder input screen.
 *
 * Accepts typed text from the user and processes it through the
 * [PipelineOrchestrator] for formatting, geocoding, and storage.
 *
 * @param pipelineOrchestrator The pipeline that transforms raw text into reminders.
 */
class KeyboardInputViewModel(
    private val pipelineOrchestrator: PipelineOrchestrator
) : ViewModel() {

    private val _uiState = MutableStateFlow<KeyboardInputUiState>(KeyboardInputUiState.Idle)
    val uiState: StateFlow<KeyboardInputUiState> = _uiState.asStateFlow()

    /**
     * Processes the user's [text] through the formatting pipeline and
     * persists the resulting reminders.
     *
     * Blank or empty input is rejected immediately with an error state.
     */
    suspend fun saveReminder(text: String) {
        if (text.isBlank()) {
            _uiState.value = KeyboardInputUiState.Error(ERROR_BLANK_INPUT)
            return
        }

        Log.d(TAG, "Processing keyboard input: ${text.take(LOG_PREVIEW_LENGTH)}")
        _uiState.value = KeyboardInputUiState.Saving

        val result = pipelineOrchestrator.processTranscript(text.trim())
        _uiState.value = when (result) {
            is PipelineResult.Success -> {
                Log.i(TAG, "Keyboard input saved: ${result.reminders.size} reminder(s)")
                KeyboardInputUiState.Success
            }
            is PipelineResult.PartialSuccess -> {
                Log.w(TAG, "Keyboard input partial success")
                KeyboardInputUiState.Success
            }
            is PipelineResult.Failure -> {
                Log.e(TAG, "Keyboard input failed: ${result.error}")
                KeyboardInputUiState.Error(result.error)
            }
            is PipelineResult.UsageLimited -> {
                Log.w(TAG, "Keyboard input usage limited")
                KeyboardInputUiState.Success
            }
        }
    }

    /** Factory for creating [KeyboardInputViewModel] with dependencies. */
    class Factory(
        private val pipelineOrchestrator: PipelineOrchestrator
    ) : androidx.lifecycle.ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(KeyboardInputViewModel::class.java)) {
                "Unknown ViewModel class: ${modelClass.name}"
            }
            return KeyboardInputViewModel(pipelineOrchestrator) as T
        }
    }

    companion object {
        private const val TAG = "KeyboardInputVM"
        private const val LOG_PREVIEW_LENGTH = 80
        private const val ERROR_BLANK_INPUT = "Please enter some text"
    }
}

/**
 * UI state for the keyboard input screen.
 */
sealed interface KeyboardInputUiState {

    /** Initial state, ready for input. */
    data object Idle : KeyboardInputUiState

    /** Processing the user's text through the pipeline. */
    data object Saving : KeyboardInputUiState

    /** Successfully saved one or more reminders. */
    data object Success : KeyboardInputUiState

    /** An error occurred during processing. */
    data class Error(val message: String) : KeyboardInputUiState
}
