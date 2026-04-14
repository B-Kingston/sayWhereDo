package com.example.reminders.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.reminders.alarm.ReminderCompletionManager
import com.example.reminders.data.repository.ReminderRepository
import com.example.reminders.ui.screen.ReminderListUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/**
 * ViewModel for the main reminder list screen.
 *
 * Observes active (non-completed) reminders from [ReminderRepository] and
 * exposes them as [ReminderListUiState]. Also handles complete and delete
 * actions via [ReminderCompletionManager] to ensure proper cleanup of
 * geofences, alarms, and sync state.
 *
 * @param repository           Provides the reactive [Flow] of active reminders.
 * @param completionManager    Handles reminder completion/deletion with resource cleanup.
 */
class ReminderListViewModel(
    private val repository: ReminderRepository,
    private val completionManager: ReminderCompletionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<ReminderListUiState>(ReminderListUiState.Loading)
    val uiState: StateFlow<ReminderListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getActiveReminders()
                .catch { error ->
                    Log.e(TAG, "Failed to load reminders", error)
                    _uiState.value = ReminderListUiState.Error(
                        error.message ?: "Failed to load reminders"
                    )
                }
                .collect { reminders ->
                    Log.d(TAG, "Active reminders updated: ${reminders.size}")
                    _uiState.value = ReminderListUiState.Success(reminders)
                }
        }
    }

    /**
     * Marks a reminder as completed and cleans up associated resources.
     */
    fun completeReminder(reminderId: String) {
        viewModelScope.launch {
            completionManager.completeReminder(reminderId)
        }
    }

    /**
     * Permanently deletes a reminder and cleans up associated resources.
     */
    fun deleteReminder(reminderId: String) {
        viewModelScope.launch {
            completionManager.deleteReminder(reminderId)
        }
    }

    /** Factory for creating [ReminderListViewModel] with dependencies. */
    class Factory(
        private val repository: ReminderRepository,
        private val completionManager: ReminderCompletionManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(ReminderListViewModel::class.java)) {
                "Unknown ViewModel class: ${modelClass.name}"
            }
            return ReminderListViewModel(repository, completionManager) as T
        }
    }

    companion object {
        private const val TAG = "ReminderListVM"
    }
}
