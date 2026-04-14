package com.example.reminders.wear.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.reminders.wear.data.DeletedReminder
import com.example.reminders.wear.data.WatchReminderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel backing the Trash screen on the watch.
 *
 * Observes the tombstone table via [WatchReminderRepository.getDeletedReminders]
 * and exposes the current list of soft-deleted reminders as a [StateFlow].
 * Provides [restore] to remove a tombstone (un-delete) and [cleanExpired]
 * to purge tombstones older than the configured retention window.
 *
 * @param repository the watch-side reminder repository.
 */
class TrashViewModel(
    private val repository: WatchReminderRepository
) : ViewModel() {

    private val _deletedReminders = MutableStateFlow<List<DeletedReminder>>(emptyList())
    val deletedReminders: StateFlow<List<DeletedReminder>> = _deletedReminders.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getDeletedReminders().collect { reminders ->
                Log.d(TAG, "Deleted reminders updated: ${reminders.size}")
                _deletedReminders.value = reminders
            }
        }
    }

    /**
     * Restores a reminder by removing its tombstone record.
     *
     * The actual [com.example.reminders.wear.data.WatchReminder] row will be
     * re-inserted during the next sync cycle. This call simply clears the
     * deletion marker so the reminder is no longer considered deleted.
     *
     * @param id the unique identifier of the reminder to restore.
     */
    fun restore(id: String) {
        viewModelScope.launch {
            repository.restoreDeletedReminder(id)
            Log.i(TAG, "Restored reminder from trash: $id")
        }
    }

    /**
     * Purges tombstone records that have exceeded the configured retention
     * window (30 days by default).
     *
     * @return the number of tombstones removed.
     */
    suspend fun cleanExpired(): Int {
        val removed = repository.cleanExpiredTombstones()
        Log.i(TAG, "Cleaned $removed expired tombstones")
        return removed
    }

    class Factory(
        private val repository: WatchReminderRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(TrashViewModel::class.java))
            return TrashViewModel(repository) as T
        }
    }

    companion object {
        private const val TAG = "TrashViewModel"
    }
}
