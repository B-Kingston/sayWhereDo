package com.example.reminders.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.reminders.billing.BillingManager
import com.example.reminders.data.repository.SavedPlaceRepository
import com.example.reminders.geocoding.GeocodingService

/**
 * Factory that supplies dependencies to [SavedPlacesViewModel].
 *
 * Required because the dependencies come from [com.example.reminders.di.AppContainer]
 * and cannot be provided by the default no-arg ViewModel constructor.
 */
class SavedPlacesViewModelFactory(
    private val savedPlaceRepository: SavedPlaceRepository,
    private val geocodingService: GeocodingService,
    private val billingManager: BillingManager
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(SavedPlacesViewModel::class.java)) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        return SavedPlacesViewModel(
            savedPlaceRepository,
            geocodingService,
            billingManager
        ) as T
    }
}
