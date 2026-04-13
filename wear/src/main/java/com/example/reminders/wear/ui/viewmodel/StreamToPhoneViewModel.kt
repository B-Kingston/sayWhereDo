package com.example.reminders.wear.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages the state of the watch-to-phone audio streaming flow.
 *
 * Tracks the recording lifecycle from idle through recording, streaming,
 * and final success or error states.
 */
class StreamToPhoneViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<StreamToPhoneUiState>(StreamToPhoneUiState.Idle)
    val uiState: StateFlow<StreamToPhoneUiState> = _uiState.asStateFlow()

    /** Starts the recording and streaming process. */
    fun startStreaming() {
        Log.d(TAG, "startStreaming called")
        _uiState.value = StreamToPhoneUiState.Recording
    }

    /** Cancels the in-progress streaming. */
    fun cancel() {
        Log.d(TAG, "cancel called")
        _uiState.value = StreamToPhoneUiState.Idle
    }

    /** Resets from an error or success state back to idle. */
    fun reset() {
        _uiState.value = StreamToPhoneUiState.Idle
    }

    companion object {
        private const val TAG = "StreamToPhoneVM"
    }
}

/**
 * Represents the UI state of the stream-to-phone screen.
 */
sealed interface StreamToPhoneUiState {
    /** No recording in progress. */
    data object Idle : StreamToPhoneUiState

    /** Audio is being captured from the microphone. */
    data object Recording : StreamToPhoneUiState

    /** Audio chunks are being streamed to the phone. */
    data object Streaming : StreamToPhoneUiState

    /** Streaming completed and transcription was received. */
    data class Success(val transcription: String) : StreamToPhoneUiState

    /** An error occurred during streaming. */
    data class Error(val message: String) : StreamToPhoneUiState
}
