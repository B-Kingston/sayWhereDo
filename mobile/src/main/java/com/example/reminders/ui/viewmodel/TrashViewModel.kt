package com.example.reminders.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.reminders.data.model.DeletedReminder
import com.example.reminders.data.repository.ReminderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/**
 * ViewModel for the Trash screen.
 *
 * Observes soft-deleted reminders (tombstones) from [ReminderRepository] and
 * exposes them as a reactive [StateFlow]. Supports restoring individual
 * reminders and purging expired tombstones beyond the 7-day retention window.
 *
 * @param repository Provides access to deleted reminders and restoration logic.
 */
class TrashViewModel(
    private val repository: ReminderRepository
) : ViewModel() {

    private val _deletedReminders = MutableStateFlow<List<DeletedReminder>>(emptyList())

    /** Reactive list of deleted reminders, most-recently-deleted first. */
    val deletedReminders: StateFlow<List<DeletedReminder>> = _deletedReminders.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getDeletedReminders()
                .catch { error ->
                    Log.e(TAG, "Failed to load deleted reminders", error)
                    _deletedReminders.value = emptyList()
                }
                .collect { reminders ->
                    Log.d(TAG, "Deleted reminders updated: ${reminders.size}")
                    _deletedReminders.value = reminders
                }
        }
    }

    /**
     * Restores a soft-deleted reminder by removing its tombstone, allowing
     * the sync engine to re-insert the reminder on the next sync cycle.
     *
     * @param id The unique identifier of the reminder to restore.
     */
    fun restore(id: String) {
        viewModelScope.launch {
            repository.restoreDeletedReminder(id)
        }
    }

    /**
     * Permanently purges tombstones older than the configured retention
     * window (7 days). Called periodically or on user action.
     */
    fun cleanExpired() {
        viewModelScope.launch {
            repository.cleanExpiredTombstones()
        }
    }

    /** Factory for creating [TrashViewModel] with manual DI. */
    class Factory(
        private val repository: ReminderRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(TrashViewModel::class.java)) {
                "Unknown ViewModel class: ${modelClass.name}"
            }
            return TrashViewModel(repository) as T
        }
    }

    companion object {
        private const val TAG = "TrashVM"
    }
}
