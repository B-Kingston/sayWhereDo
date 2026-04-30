package com.example.reminders.wear.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.reminders.wear.alarm.WatchAlarmScheduler
import com.example.reminders.wear.data.WatchReminder
import com.example.reminders.wear.data.WatchReminderRepository
import com.example.reminders.wear.data.WearDataLayerClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID

sealed interface VoiceRecordUiState {
    data object Idle : VoiceRecordUiState
    data object Recording : VoiceRecordUiState
    data class Success(val text: String) : VoiceRecordUiState
    data class Error(val message: String) : VoiceRecordUiState
}

class VoiceRecordViewModel(
    private val repository: WatchReminderRepository,
    private val alarmScheduler: WatchAlarmScheduler,
    private val wearDataLayerClient: WearDataLayerClient
) : ViewModel() {

    private val _uiState = MutableStateFlow<VoiceRecordUiState>(VoiceRecordUiState.Idle)
    val uiState: StateFlow<VoiceRecordUiState> = _uiState.asStateFlow()

    fun setRecording() {
        _uiState.value = VoiceRecordUiState.Recording
        Log.d(TAG, "State -> Recording")
    }

    fun onVoiceResult(text: String) {
        if (text.isBlank()) {
            _uiState.value = VoiceRecordUiState.Error("Empty transcription")
            return
        }
        Log.d(TAG, "Voice result: ${text.take(80)}")
        viewModelScope.launch {
            saveAndSend(text)
        }
    }

    fun onKeyboardInput(text: String) {
        if (text.isBlank()) {
            _uiState.value = VoiceRecordUiState.Error("Empty input")
            return
        }
        Log.d(TAG, "Keyboard input: ${text.take(80)}")
        viewModelScope.launch {
            saveAndSend(text)
        }
    }

    private suspend fun saveAndSend(text: String) {
        val reminder = saveRawReminderLocally(text)
        wearDataLayerClient.syncReminderToPhone(reminder)
        trySendForFormatting(reminder.id, text)
        _uiState.value = VoiceRecordUiState.Success(text)
    }

    private suspend fun saveRawReminderLocally(text: String): WatchReminder {
        val now = Instant.now()
        val reminder = WatchReminder(
            id = UUID.randomUUID().toString(),
            title = text.take(MAX_TITLE_LENGTH).trim(),
            sourceTranscript = text,
            formattingProvider = FORMATTING_PENDING,
            createdAt = now,
            updatedAt = now
        )
        repository.insert(reminder)
        Log.i(TAG, "Saved raw reminder locally: ${reminder.id}")
        return reminder
    }

    private suspend fun trySendForFormatting(reminderId: String, text: String) {
        wearDataLayerClient.sendTranscriptToPhone(text, reminderId)
        Log.i(TAG, "Deferred formatting request completed for $reminderId")
    }

    fun reset() {
        _uiState.value = VoiceRecordUiState.Idle
    }

    class Factory(
        private val repository: WatchReminderRepository,
        private val alarmScheduler: WatchAlarmScheduler,
        private val wearDataLayerClient: WearDataLayerClient
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(VoiceRecordViewModel::class.java))
            return VoiceRecordViewModel(repository, alarmScheduler, wearDataLayerClient) as T
        }
    }

    companion object {
        private const val TAG = "VoiceRecordVM"
        private const val MAX_TITLE_LENGTH = 120
        private const val FORMATTING_PENDING = "pending"
    }
}
