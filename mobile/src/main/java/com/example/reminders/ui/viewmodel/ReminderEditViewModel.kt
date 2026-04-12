package com.example.reminders.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.reminders.alarm.AlarmScheduler
import com.example.reminders.alarm.RecurrencePattern
import com.example.reminders.billing.BillingManager
import com.example.reminders.data.model.LocationTrigger
import com.example.reminders.data.model.Reminder
import com.example.reminders.data.repository.ReminderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * UI state for the reminder edit screen.
 */
sealed interface ReminderEditUiState {

    /** The reminder is loading from Room. */
    data object Loading : ReminderEditUiState

    /** The reminder data is ready for editing. */
    data class Ready(
        val title: String,
        val body: String?,
        val triggerDate: LocalDate?,
        val triggerTime: LocalTime?,
        val recurrence: RecurrencePattern?,
        val radiusMetres: Int,
        val hasLocationTrigger: Boolean,
        val isPro: Boolean,
        val isSaving: Boolean = false,
        val saveError: String? = null
    ) : ReminderEditUiState

    /** The reminder has been saved successfully. */
    data object Saved : ReminderEditUiState

    /** The reminder was not found. */
    data object NotFound : ReminderEditUiState
}

/**
 * ViewModel for editing a reminder's time, recurrence, and geofence radius.
 *
 * Pro-gated features:
 * - **Recurrence**: Free users see "Upgrade to set recurrence" CTA.
 * - **Custom radius**: Free users see a fixed 150m radius with the slider disabled.
 */
class ReminderEditViewModel(
    private val reminderRepository: ReminderRepository,
    private val alarmScheduler: AlarmScheduler,
    private val billingManager: BillingManager,
    private val reminderId: String
) : ViewModel() {

    companion object {
        private const val TAG = "ReminderEditViewModel"
    }

    private val _uiState = MutableStateFlow<ReminderEditUiState>(ReminderEditUiState.Loading)
    val uiState: StateFlow<ReminderEditUiState> = _uiState.asStateFlow()

    private var cachedReminder: Reminder? = null

    init {
        loadReminder()
    }

    /**
     * Loads the reminder from Room and populates the edit state.
     */
    private suspend fun loadReminderInternal() {
        Log.d(TAG, "Loading reminder: $reminderId")
        val reminder = reminderRepository.getReminderById(reminderId)
        if (reminder == null) {
            Log.w(TAG, "Reminder not found: $reminderId")
            _uiState.value = ReminderEditUiState.NotFound
            return
        }

        cachedReminder = reminder

        val isPro = billingManager.isPro.value

        // Decompose Instant into date + time for the pickers
        val triggerDate = reminder.triggerTime?.atZone(ZoneId.systemDefault())?.toLocalDate()
        val triggerTime = reminder.triggerTime?.atZone(ZoneId.systemDefault())?.toLocalTime()

        val recurrence = reminder.recurrence?.let { RecurrencePattern.fromString(it) }
        val radius = reminder.locationTrigger?.radiusMetres ?: ReminderEditViewModelFactory.DEFAULT_RADIUS_METRES

        _uiState.value = ReminderEditUiState.Ready(
            title = reminder.title,
            body = reminder.body,
            triggerDate = triggerDate,
            triggerTime = triggerTime,
            recurrence = recurrence,
            radiusMetres = radius,
            hasLocationTrigger = reminder.locationTrigger != null,
            isPro = isPro
        )
    }

    private fun loadReminder() {
        viewModelScope.launch {
            loadReminderInternal()
        }
    }

    /**
     * Updates the selected trigger date.
     */
    fun onDateSelected(date: LocalDate?) {
        val current = _uiState.value as? ReminderEditUiState.Ready ?: return
        _uiState.value = current.copy(triggerDate = date, saveError = null)
    }

    /**
     * Updates the selected trigger time.
     */
    fun onTimeSelected(time: LocalTime?) {
        val current = _uiState.value as? ReminderEditUiState.Ready ?: return
        _uiState.value = current.copy(triggerTime = time, saveError = null)
    }

    /**
     * Updates the recurrence pattern.
     *
     * No-op for free users — Pro gating is enforced at the UI layer.
     */
    fun onRecurrenceSelected(pattern: RecurrencePattern?) {
        val current = _uiState.value as? ReminderEditUiState.Ready ?: return
        if (!current.isPro) return
        _uiState.value = current.copy(recurrence = pattern, saveError = null)
    }

    /**
     * Updates the geofence radius in metres.
     *
     * No-op for free users — radius is locked at 150m.
     */
    fun onRadiusChanged(radiusMetres: Int) {
        val current = _uiState.value as? ReminderEditUiState.Ready ?: return
        if (!current.isPro) return
        _uiState.value = current.copy(radiusMetres = radiusMetres, saveError = null)
    }

    /**
     * Saves the edited reminder fields to Room and reschedules the alarm.
     */
    fun save() {
        val current = _uiState.value as? ReminderEditUiState.Ready ?: return
        val reminder = cachedReminder ?: return

        Log.d(TAG, "Saving reminder: ${reminder.id}")
        viewModelScope.launch {
            _uiState.value = current.copy(isSaving = true)

            try {
                val triggerInstant = combineDateAndTime(current.triggerDate, current.triggerTime)

                val updatedLocationTrigger = if (reminder.locationTrigger != null) {
                    reminder.locationTrigger.copy(
                        radiusMetres = if (current.isPro) current.radiusMetres else ReminderEditViewModelFactory.DEFAULT_RADIUS_METRES
                    )
                } else {
                    null
                }

                val updated = reminder.copy(
                    triggerTime = triggerInstant,
                    recurrence = current.recurrence?.name,
                    locationTrigger = updatedLocationTrigger,
                    updatedAt = Instant.now()
                )

                reminderRepository.update(updated)

                alarmScheduler.cancelAlarm(reminder.id)
                if (triggerInstant != null) {
                    alarmScheduler.scheduleAlarm(updated)
                }

                Log.i(TAG, "Reminder saved: ${reminder.id}")
                _uiState.value = ReminderEditUiState.Saved
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save reminder: ${reminder.id}", e)
                _uiState.value = current.copy(isSaving = false, saveError = e.message)
            }
        }
    }

    /**
     * Combines a [LocalDate] and [LocalTime] into an [Instant].
     * Returns `null` if either component is null.
     */
    private fun combineDateAndTime(date: LocalDate?, time: LocalTime?): Instant? {
        if (date == null || time == null) return null
        return date.atTime(time).atZone(ZoneId.systemDefault()).toInstant()
    }
}

/**
 * Factory for creating [ReminderEditViewModel] with required dependencies.
 */
class ReminderEditViewModelFactory(
    private val reminderRepository: ReminderRepository,
    private val alarmScheduler: AlarmScheduler,
    private val billingManager: BillingManager,
    private val reminderId: String
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        ReminderEditViewModel(
            reminderRepository = reminderRepository,
            alarmScheduler = alarmScheduler,
            billingManager = billingManager,
            reminderId = reminderId
        ) as T

    companion object {
        /** Default geofence radius for free users, in metres. */
        const val DEFAULT_RADIUS_METRES = 150
    }
}
