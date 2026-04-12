package com.example.reminders.geocoding

import android.util.Log
import com.example.reminders.data.model.LocationTrigger
import com.example.reminders.data.model.ParsedReminder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * State machine that drives the geocoding stage of the reminder pipeline.
 *
 * Given a [ParsedReminder] whose [LocationTrigger] contains a
 * [placeLabel][LocationTrigger.placeLabel] but no coordinates, this step:
 *
 * 1. Checks [SavedPlaceMatcher] for a pre-resolved saved place.
 * 2. Falls back to [GeocodingService] when no saved place matches.
 * 3. Emits [GeocodingStepState.AwaitingConfirmation] when multiple
 *    candidates are returned, pausing for user disambiguation.
 * 4. On confirmation, updates the [LocationTrigger] with coordinates.
 *
 * The step is designed as a **Flow-based state machine** rather than a
 * single blocking suspend function so the UI can observe transitions
 * reactively and the pipeline can be paused for user input.
 */
class GeocodingPipelineStep(
    private val savedPlaceMatcher: SavedPlaceMatcher,
    private val geocodingService: GeocodingService
) {

    companion object {
        private const val TAG = "GeocodingPipelineStep"
    }

    private val _state = MutableStateFlow<GeocodingStepState>(GeocodingStepState.Idle)
    val state = _state.asStateFlow()

    /**
     * Begins the geocoding process for the given [reminder].
     *
     * If the reminder has no [LocationTrigger] or coordinates are already
     * present, the step completes immediately with [GeocodingStepState.Resolved].
     */
    suspend fun process(reminder: ParsedReminder) {
        Log.d(TAG, "Processing reminder: ${reminder.title.take(50)}")
        val trigger = reminder.locationTrigger
        if (trigger == null) {
            Log.d(TAG, "No location trigger — resolved immediately")
            _state.value = GeocodingStepState.Resolved(reminder)
            return
        }

        if (trigger.latitude != null && trigger.longitude != null) {
            Log.d(TAG, "Coordinates already present — resolved immediately")
            _state.value = GeocodingStepState.Resolved(reminder)
            return
        }

        val placeLabel = trigger.placeLabel
        if (placeLabel.isBlank()) {
            Log.w(TAG, "Place label is empty — failing")
            _state.value = GeocodingStepState.Failed("Place label is empty")
            return
        }

        _state.value = GeocodingStepState.NeedsGeocoding(placeLabel)
        Log.d(TAG, "Checking saved places for: $placeLabel")
        val savedMatch = savedPlaceMatcher.match(placeLabel)
        if (savedMatch != null) {
            val resolved = reminder.withCoordinates(
                latitude = savedMatch.latitude,
                longitude = savedMatch.longitude,
                address = savedMatch.address
            )
            Log.i(TAG, "Matched saved place: ${savedMatch.label}")
            _state.value = GeocodingStepState.Matched(resolved, savedMatch.label)
            _state.value = GeocodingStepState.Resolved(resolved)
            return
        }

        Log.d(TAG, "No saved place match — calling geocoding service for: $placeLabel")
        _state.value = GeocodingStepState.NeedsGeocoding(placeLabel)
        val result = geocodingService.geocode(placeLabel)

        when (result) {
            is GeocodingResult.Resolved -> {
                val resolved = reminder.withCoordinates(
                    latitude = result.latitude,
                    longitude = result.longitude,
                    address = result.displayAddress
                )
                Log.i(TAG, "Geocoding resolved: ${result.displayAddress}")
                _state.value = GeocodingStepState.Resolved(resolved)
            }

            is GeocodingResult.Ambiguous -> {
                Log.i(TAG, "Geocoding ambiguous: ${result.candidates.size} candidates — awaiting confirmation")
                _state.value = GeocodingStepState.AwaitingConfirmation(
                    reminder = reminder,
                    candidates = result.candidates
                )
            }

            is GeocodingResult.NotFound -> {
                Log.w(TAG, "No location found for: $placeLabel")
                _state.value = GeocodingStepState.Failed(
                    "No location found for \"$placeLabel\""
                )
            }

            is GeocodingResult.Error -> {
                Log.e(TAG, "Geocoding error: ${result.message}")
                _state.value = GeocodingStepState.Failed(result.message)
            }
        }
    }

    /**
     * Resolves an [AwaitingConfirmation][GeocodingStepState.AwaitingConfirmation]
     * state by applying the user-selected candidate's coordinates.
     *
     * Must only be called when [state] is
     * [AwaitingConfirmation][GeocodingStepState.AwaitingConfirmation].
     *
     * @param candidate The disambiguated candidate the user chose.
     */
    fun confirmCandidate(candidate: GeocodingCandidate) {
        Log.d(TAG, "Confirming candidate: ${candidate.displayAddress}")
        val currentState = _state.value
        check(currentState is GeocodingStepState.AwaitingConfirmation) {
            "confirmCandidate must only be called in AwaitingConfirmation state, " +
                "but current state is ${currentState::class.simpleName}"
        }

        val resolved = currentState.reminder.withCoordinates(
            latitude = candidate.latitude,
            longitude = candidate.longitude,
            address = candidate.displayAddress
        )
        _state.value = GeocodingStepState.Resolved(resolved)
    }

    /** Resets the step to [GeocodingStepState.Idle]. */
    fun reset() {
        Log.d(TAG, "Reset to Idle")
        _state.value = GeocodingStepState.Idle
    }

    /**
     * Copies the reminder with updated coordinates in its [LocationTrigger].
     */
    private fun ParsedReminder.withCoordinates(
        latitude: Double,
        longitude: Double,
        address: String
    ): ParsedReminder {
        val trigger = locationTrigger ?: return this
        return copy(
            locationTrigger = trigger.copy(
                latitude = latitude,
                longitude = longitude,
                rawAddress = address
            )
        )
    }
}

/**
 * States emitted by [GeocodingPipelineStep].
 */
sealed interface GeocodingStepState {

    /** No geocoding in progress. */
    data object Idle : GeocodingStepState

    /** A saved place matched and coordinates were applied. */
    data class Matched(
        val reminder: ParsedReminder,
        val savedPlaceLabel: String
    ) : GeocodingStepState

    /** The step needs to call the geocoder (no saved place matched). */
    data class NeedsGeocoding(val placeLabel: String) : GeocodingStepState

    /** Multiple candidates found — user must pick one. */
    data class AwaitingConfirmation(
        val reminder: ParsedReminder,
        val candidates: List<GeocodingCandidate>
    ) : GeocodingStepState

    /** Coordinates resolved (from any source). */
    data class Resolved(val reminder: ParsedReminder) : GeocodingStepState

    /** Geocoding failed with an error message. */
    data class Failed(val message: String) : GeocodingStepState
}
