package com.example.reminders.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.reminders.billing.BillingManager
import com.example.reminders.data.model.SavedPlace
import com.example.reminders.data.repository.SavedPlaceRepository
import com.example.reminders.geocoding.GeocodingResult
import com.example.reminders.geocoding.GeocodingService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * ViewModel driving the Saved Places screen.
 *
 * Responsibilities:
 * - Observes the list of saved places via [SavedPlaceRepository]
 * - Enforces the free-tier cap (2 saved places)
 * - Geocodes the address when adding a new place
 * - Delegates Pro status checks to [BillingManager]
 */
class SavedPlacesViewModel(
    private val savedPlaceRepository: SavedPlaceRepository,
    private val geocodingService: GeocodingService,
    private val billingManager: BillingManager
) : ViewModel() {

    /** All saved places, ordered alphabetically by label. */
    val savedPlaces = savedPlaceRepository.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Whether the current user has Pro. */
    val isPro: StateFlow<Boolean> = billingManager.isPro

    private val _addPlaceState = MutableStateFlow<AddPlaceState>(AddPlaceState.Idle)
    val addPlaceState: StateFlow<AddPlaceState> = _addPlaceState.asStateFlow()

    /** Maximum number of saved places a free user may create. */
    private val freeTierCap = FREE_TIER_SAVED_PLACES_CAP

    /**
     * Attempts to add a new saved place.
     *
     * Enforces the free-tier cap before performing geocoding.
     * On success, the place is persisted to the repository.
     */
    fun addPlace(label: String, address: String) {
        viewModelScope.launch {
            if (!billingManager.isPro.value) {
                val currentCount = savedPlaceRepository.count()
                if (currentCount >= freeTierCap) {
                    _addPlaceState.value = AddPlaceState.CapReached
                    return@launch
                }
            }

            _addPlaceState.value = AddPlaceState.Geocoding

            when (val result = geocodingService.geocode(address)) {
                is GeocodingResult.Resolved -> {
                    val place = SavedPlace(
                        id = UUID.randomUUID().toString(),
                        label = label.trim(),
                        address = result.displayAddress,
                        latitude = result.latitude,
                        longitude = result.longitude
                    )
                    savedPlaceRepository.insert(place)
                    _addPlaceState.value = AddPlaceState.Success
                }

                is GeocodingResult.Ambiguous -> {
                    _addPlaceState.value = AddPlaceState.AmbiguousAddress(
                        label = label.trim(),
                        candidates = result.candidates
                    )
                }

                is GeocodingResult.NotFound -> {
                    _addPlaceState.value = AddPlaceState.Error(
                        "No location found for \"$address\""
                    )
                }

                is GeocodingResult.Error -> {
                    _addPlaceState.value = AddPlaceState.Error(result.message)
                }
            }
        }
    }

    /**
     * Resolves an ambiguous address by using the user-selected candidate.
     */
    fun confirmCandidate(label: String, candidate: com.example.reminders.geocoding.GeocodingCandidate) {
        viewModelScope.launch {
            val place = SavedPlace(
                id = UUID.randomUUID().toString(),
                label = label,
                address = candidate.displayAddress,
                latitude = candidate.latitude,
                longitude = candidate.longitude
            )
            savedPlaceRepository.insert(place)
            _addPlaceState.value = AddPlaceState.Success
        }
    }

    /** Deletes a saved place from the repository. */
    fun deletePlace(place: SavedPlace) {
        viewModelScope.launch {
            savedPlaceRepository.delete(place)
        }
    }

    /** Resets the add-place dialog state. */
    fun resetAddPlaceState() {
        _addPlaceState.value = AddPlaceState.Idle
    }

    companion object {
        /** Maximum saved places allowed on the free tier. */
        const val FREE_TIER_SAVED_PLACES_CAP = 2
    }
}

/**
 * State for the add-place dialog flow.
 */
sealed interface AddPlaceState {
    data object Idle : AddPlaceState
    data object Geocoding : AddPlaceState
    data object Success : AddPlaceState
    data object CapReached : AddPlaceState
    data class AmbiguousAddress(
        val label: String,
        val candidates: List<com.example.reminders.geocoding.GeocodingCandidate>
    ) : AddPlaceState
    data class Error(val message: String) : AddPlaceState
}
