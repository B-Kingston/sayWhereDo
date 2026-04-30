package com.example.reminders.wear.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.reminders.wear.alarm.WatchAlarmScheduler
import com.example.reminders.wear.data.DeletedReminder
import com.example.reminders.wear.data.WatchReminder
import com.example.reminders.wear.data.WatchReminderRepository
import com.example.reminders.wear.data.WearDataLayerClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant

class ReminderDetailViewModel(
    private val repository: WatchReminderRepository,
    private val alarmScheduler: WatchAlarmScheduler,
    private val reminderId: String,
    private val wearDataLayerClient: WearDataLayerClient
) : ViewModel() {

    private val _reminder = MutableStateFlow<WatchReminder?>(null)
    val reminder: StateFlow<WatchReminder?> = _reminder.asStateFlow()

    init {
        viewModelScope.launch {
            val result = repository.getById(reminderId)
            _reminder.value = result
            Log.d(TAG, "Loaded reminder: $reminderId")
        }
    }

    fun completeReminder() {
        viewModelScope.launch {
            val current = _reminder.value ?: return@launch
            val updated = current.copy(
                isCompleted = true,
                updatedAt = Instant.now()
            )
            repository.update(updated)
            alarmScheduler.cancelAlarm(current.id)
            wearDataLayerClient.syncReminderToPhone(updated)
            _reminder.value = updated
            Log.i(TAG, "Completed reminder: ${current.id}")
        }
    }

    fun deleteReminder(onDeleted: () -> Unit) {
        viewModelScope.launch {
            val current = _reminder.value ?: return@launch
            alarmScheduler.cancelAlarm(current.id)
            repository.moveReminderToTombstone(current.id, "watch")
            val tombstone = DeletedReminder(
                id = current.id,
                originalTitle = current.title,
                deletedAt = Instant.now(),
                deletedBy = "watch",
                originalUpdatedAt = current.updatedAt
            )
            wearDataLayerClient.sendTombstone(tombstone)
            Log.i(TAG, "Deleted reminder: ${current.id}")
            onDeleted()
        }
    }

    class Factory(
        private val repository: WatchReminderRepository,
        private val alarmScheduler: WatchAlarmScheduler,
        private val reminderId: String,
        private val wearDataLayerClient: WearDataLayerClient
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(ReminderDetailViewModel::class.java))
            return ReminderDetailViewModel(repository, alarmScheduler, reminderId, wearDataLayerClient) as T
        }
    }

    companion object {
        private const val TAG = "ReminderDetailVM"
    }
}
