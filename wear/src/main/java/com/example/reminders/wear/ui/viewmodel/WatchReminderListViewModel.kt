package com.example.reminders.wear.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.reminders.wear.alarm.WatchAlarmScheduler
import com.example.reminders.wear.data.WatchReminder
import com.example.reminders.wear.data.WatchReminderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant

class WatchReminderListViewModel(
    private val repository: WatchReminderRepository,
    private val alarmScheduler: WatchAlarmScheduler
) : ViewModel() {

    private val _reminders = MutableStateFlow<List<WatchReminder>>(emptyList())
    val reminders: StateFlow<List<WatchReminder>> = _reminders.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getActiveReminders().collect { reminders ->
                Log.d(TAG, "Active reminders updated: ${reminders.size}")
                _reminders.value = reminders
            }
        }
    }

    fun deleteReminder(reminderId: String) {
        viewModelScope.launch {
            alarmScheduler.cancelAlarm(reminderId)
            repository.deleteById(reminderId)
            Log.i(TAG, "Deleted reminder: $reminderId")
        }
    }

    fun completeReminder(reminder: WatchReminder) {
        viewModelScope.launch {
            val updated = reminder.copy(
                isCompleted = true,
                updatedAt = Instant.now()
            )
            repository.update(updated)
            alarmScheduler.cancelAlarm(reminder.id)
            Log.i(TAG, "Completed reminder: ${reminder.id}")
        }
    }

    class Factory(
        private val repository: WatchReminderRepository,
        private val alarmScheduler: WatchAlarmScheduler
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(WatchReminderListViewModel::class.java))
            return WatchReminderListViewModel(repository, alarmScheduler) as T
        }
    }

    companion object {
        private const val TAG = "WatchReminderListVM"
    }
}
