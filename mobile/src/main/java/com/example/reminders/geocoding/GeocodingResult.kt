package com.example.reminders.geocoding

/**
 * Represents the outcome of a geocoding request.
 *
 * The sealed interface ensures exhaustive handling: every call-site must
 * account for all four possible outcomes — a single resolved address,
 * multiple ambiguous candidates, no results, or an error condition.
 */
sealed interface GeocodingResult {

    /** A single, high-confidence address was resolved. */
    data class Resolved(
        val latitude: Double,
        val longitude: Double,
        val displayAddress: String
    ) : GeocodingResult

    /** Multiple candidates were found; user disambiguation is required. */
    data class Ambiguous(
        val candidates: List<GeocodingCandidate>
    ) : GeocodingResult

    /** The geocoder returned zero results for the query. */
    data object NotFound : GeocodingResult

    /** An error occurred (geocoder unavailable, network failure, etc.). */
    data class Error(val message: String) : GeocodingResult
}

/**
 * A single candidate produced by the geocoder when multiple addresses
 * match the query. Displayed in the disambiguation UI.
 */
data class GeocodingCandidate(
    val latitude: Double,
    val longitude: Double,
    val displayAddress: String
)
