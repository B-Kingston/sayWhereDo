package com.example.reminders.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.reminders.billing.BillingManager
import com.example.reminders.data.export.ImportResult
import com.example.reminders.data.export.ReminderExporter
import com.example.reminders.data.model.Reminder
import com.example.reminders.data.model.SavedPlace
import com.example.reminders.data.repository.ReminderRepository
import com.example.reminders.data.repository.SavedPlaceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * ViewModel for the Pro section of the Settings screen.
 *
 * Manages Pro status observation, purchase flow triggering,
 * export/import operations, and purchase restoration. Exposes
 * a [ProSettingsUiState] that the UI observes for reactive updates.
 *
 * @param billingManager      Provides Pro status and purchase flow access.
 * @param reminderRepository  Source of reminders for export operations.
 * @param savedPlaceRepository Source of saved places for export operations.
 * @param reminderExporter    Handles JSON serialisation and deserialisation.
 */
class ProSettingsViewModel(
    private val billingManager: BillingManager,
    private val reminderRepository: ReminderRepository,
    private val savedPlaceRepository: SavedPlaceRepository,
    private val reminderExporter: ReminderExporter
) : ViewModel() {

    companion object {
        private const val TAG = "ProSettingsViewModel"
    }

    /** The user's current Pro status, observed from BillingManager. */
    val isPro: StateFlow<Boolean> = billingManager.isPro

    private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
    val exportState: StateFlow<ExportState> = _exportState.asStateFlow()

    private val _importState = MutableStateFlow<ImportState>(ImportState.Idle)
    val importState: StateFlow<ImportState> = _importState.asStateFlow()

    private val _restoreState = MutableStateFlow<RestoreState>(RestoreState.Idle)
    val restoreState: StateFlow<RestoreState> = _restoreState.asStateFlow()

    /**
     * Exports all reminders and saved places to a JSON string.
     *
     * The caller is responsible for persisting the result (e.g. via
     * the system file picker or share sheet).
     */
    suspend fun exportReminders(): String? {
        _exportState.value = ExportState.Exporting
        Log.d(TAG, "Starting export")

        return try {
            val reminders = reminderRepository.getActiveReminders().first()
            val completedReminders = reminderRepository.getCompletedReminders().first()
            val allReminders = reminders + completedReminders
            val savedPlaces = savedPlaceRepository.getAll().first()

            if (allReminders.isEmpty() && savedPlaces.isEmpty()) {
                Log.d(TAG, "No data to export")
                _exportState.value = ExportState.NoData
                return null
            }

            val json = reminderExporter.exportToJson(allReminders, savedPlaces)
            Log.i(TAG, "Exported ${allReminders.size} reminder(s), ${savedPlaces.size} saved place(s)")
            _exportState.value = ExportState.Success(
                reminderCount = allReminders.size,
                savedPlaceCount = savedPlaces.size
            )
            json
        } catch (e: Exception) {
            Log.e(TAG, "Export failed", e)
            _exportState.value = ExportState.Error(e.message ?: "Export failed")
            null
        }
    }

    /**
     * Imports reminders and saved places from a JSON string.
     *
     * Duplicates (same ID) are skipped rather than overwritten.
     */
    suspend fun importReminders(jsonText: String) {
        _importState.value = ImportState.Importing
        Log.d(TAG, "Starting import (${jsonText.length} chars)")

        when (val result = reminderExporter.parseImport(jsonText)) {
            is ImportResult.Success -> {
                var newReminders = 0
                var newSavedPlaces = 0
                var skippedDuplicates = 0

                for (reminder in result.reminders) {
                    val existing = reminderRepository.getReminderById(reminder.id)
                    if (existing != null) {
                        skippedDuplicates++
                    } else {
                        reminderRepository.insert(reminder)
                        newReminders++
                    }
                }

                for (place in result.savedPlaces) {
                    val existing = savedPlaceRepository.getById(place.id)
                    if (existing != null) {
                        skippedDuplicates++
                    } else {
                        savedPlaceRepository.insert(place)
                        newSavedPlaces++
                    }
                }

                Log.i(TAG, "Import complete: $newReminders reminder(s), $newSavedPlaces place(s), $skippedDuplicates skipped")
                _importState.value = ImportState.Success(
                    reminderCount = newReminders,
                    savedPlaceCount = newSavedPlaces,
                    skippedCount = skippedDuplicates
                )
            }

            is ImportResult.ParseError -> {
                Log.e(TAG, "Import parse error: ${result.message}")
                _importState.value = ImportState.Error(result.message)
            }
        }
    }

    /**
     * Restores previous purchases by re-querying the billing client.
     *
     * Observes [billingManager.isPro] reactively so the restore state
     * is updated once the async billing query completes.
     */
    fun restorePurchases() {
        _restoreState.value = RestoreState.Restoring
        Log.d(TAG, "Restoring purchases")
        billingManager.restorePurchases()

        viewModelScope.launch {
            val wasPro = billingManager.isPro.drop(1).first()
            Log.i(TAG, "Restore result: isPro=$wasPro")
            _restoreState.value = if (wasPro) {
                RestoreState.Success
            } else {
                RestoreState.NoPurchase
            }
        }
    }

    /**
     * Resets export/import/restore states back to idle.
     */
    fun resetStates() {
        _exportState.value = ExportState.Idle
        _importState.value = ImportState.Idle
        _restoreState.value = RestoreState.Idle
    }
}

/**
 * UI state for the export operation.
 */
sealed interface ExportState {
    data object Idle : ExportState
    data object Exporting : ExportState
    data class Success(val reminderCount: Int, val savedPlaceCount: Int) : ExportState
    data object NoData : ExportState
    data class Error(val message: String) : ExportState
}

/**
 * UI state for the import operation.
 */
sealed interface ImportState {
    data object Idle : ImportState
    data object Importing : ImportState
    data class Success(
        val reminderCount: Int,
        val savedPlaceCount: Int,
        val skippedCount: Int
    ) : ImportState
    data class Error(val message: String) : ImportState
}

/**
 * UI state for the restore purchases operation.
 */
sealed interface RestoreState {
    data object Idle : RestoreState
    data object Restoring : RestoreState
    data object Success : RestoreState
    data object NoPurchase : RestoreState
}

/**
 * Factory for creating [ProSettingsViewModel] with required dependencies.
 */
class ProSettingsViewModelFactory(
    private val billingManager: BillingManager,
    private val reminderRepository: ReminderRepository,
    private val savedPlaceRepository: SavedPlaceRepository,
    private val reminderExporter: ReminderExporter
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ProSettingsViewModel(
            billingManager,
            reminderRepository,
            savedPlaceRepository,
            reminderExporter
        ) as T
    }
}
