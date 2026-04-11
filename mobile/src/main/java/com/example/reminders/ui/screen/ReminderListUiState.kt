package com.example.reminders.ui.screen

sealed interface ReminderListUiState {
    data object Loading : ReminderListUiState
    data class Success(val reminders: List<com.example.reminders.data.model.Reminder>) : ReminderListUiState
    data class Error(val message: String) : ReminderListUiState
}
