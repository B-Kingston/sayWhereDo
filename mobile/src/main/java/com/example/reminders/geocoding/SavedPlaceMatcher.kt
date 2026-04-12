package com.example.reminders.geocoding

import android.util.Log
import com.example.reminders.data.model.SavedPlace
import com.example.reminders.data.repository.SavedPlaceRepository

/**
 * Attempts to match a user-provided place label against previously saved
 * places before falling back to network geocoding.
 *
 * Matching is case-insensitive and whitespace-trimmed so that
 * "Home", " home ", and "HOME" all resolve to the same entry.
 * This should always be called **before** [GeocodingService.geocode]
 * so that pre-resolved coordinates are preferred.
 */
class SavedPlaceMatcher(
    private val savedPlaceRepository: SavedPlaceRepository
) {

    /**
     * Looks up [label] in the saved-places table.
     *
     * @return The matching [SavedPlace] with coordinates, or `null` when
     *         no saved place matches the normalised label.
     */
    suspend fun match(label: String): SavedPlace? {
        val normalisedLabel = label.trim()
        val result = savedPlaceRepository.findByLabel(normalisedLabel)
        if (result != null) {
            Log.d(TAG, "Matched saved place: $normalisedLabel -> ${result.address}")
        } else {
            Log.d(TAG, "No saved place match for: $normalisedLabel")
        }
        return result
    }

    companion object {
        private const val TAG = "SavedPlaceMatcher"
    }
}
