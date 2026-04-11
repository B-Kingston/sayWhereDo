package com.example.reminders.geocoding

/**
 * Abstraction over geocoding backends.
 *
 * The pipeline step ([GeocodingPipelineStep]) depends on this interface
 * so the concrete implementation (Android Geocoder, a remote API, etc.)
 * can be swapped without touching pipeline logic.
 */
interface GeocodingService {

    /**
     * Resolves [query] to geographic coordinates.
     *
     * @return [GeocodingResult.Resolved] when exactly one confident result is found,
     *         [GeocodingResult.Ambiguous] when multiple candidates are returned,
     *         [GeocodingResult.NotFound] when the query matches nothing, or
     *         [GeocodingResult.Error] on failure.
     */
    suspend fun geocode(query: String): GeocodingResult
}
