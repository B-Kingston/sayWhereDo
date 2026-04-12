package com.example.reminders.geocoding

import android.location.Geocoder
import android.util.Log
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.IOException
import kotlin.coroutines.resume

/**
 * Geocoding implementation backed by the platform [Geocoder].
 *
 * Uses the asynchronous listener-based API available on API 33+
 * (our minSdk) so no blocking I/O occurs on the calling coroutine.
 */
class AndroidGeocodingService(
    private val geocoder: Geocoder
) : GeocodingService {

    companion object {
        private const val TAG = "AndroidGeocoding"
        /** Maximum number of candidate addresses to request. */
        private const val MAX_RESULTS = 5
    }

    /**
     * Resolves [query] using the platform [Geocoder].
     *
     * Falls through to [GeocodingResult.Error] when the Geocoder is
     * absent on the device, or when a service-level [IOException] occurs
     * (e.g. "Service not available").
     */
    override suspend fun geocode(query: String): GeocodingResult {
        Log.d(TAG, "Geocoding query: $query")
        if (!Geocoder.isPresent()) {
            Log.e(TAG, "Geocoder is not available on this device")
            return GeocodingResult.Error("Geocoder is not available on this device")
        }

        return suspendCancellableCoroutine { continuation ->
            geocoder.getFromLocationName(
                query,
                MAX_RESULTS,
                object : Geocoder.GeocodeListener {
                    override fun onGeocode(addresses: MutableList<android.location.Address>) {
                        val result = addresses.toGeocodingResult()
                        when (result) {
                            is GeocodingResult.Resolved ->
                                Log.i(TAG, "Geocoding resolved: ${result.displayAddress}")
                            is GeocodingResult.Ambiguous ->
                                Log.i(TAG, "Geocoding ambiguous: ${result.candidates.size} candidates")
                            is GeocodingResult.NotFound ->
                                Log.w(TAG, "Geocoding not found for: $query")
                            is GeocodingResult.Error ->
                                Log.e(TAG, "Geocoding error: ${result.message}")
                        }
                        continuation.resume(result)
                    }

                    override fun onError(errorMessage: String?) {
                        Log.e(TAG, "Geocoder onError: $errorMessage")
                        continuation.resume(
                            GeocodingResult.Error(errorMessage ?: "Geocoding failed")
                        )
                    }
                }
            )
        }
    }

    /**
     * Converts a list of platform [Address] objects into the
     * appropriate [GeocodingResult] variant.
     */
    private fun MutableList<android.location.Address>.toGeocodingResult(): GeocodingResult {
        if (isEmpty()) return GeocodingResult.NotFound

        if (size == 1) {
            val address = first()
            return GeocodingResult.Resolved(
                latitude = address.latitude,
                longitude = address.longitude,
                displayAddress = address.getAddressLine(0)
                    ?: "${address.latitude}, ${address.longitude}"
            )
        }

        return GeocodingResult.Ambiguous(
            candidates = map { address ->
                GeocodingCandidate(
                    latitude = address.latitude,
                    longitude = address.longitude,
                    displayAddress = address.getAddressLine(0)
                        ?: "${address.latitude}, ${address.longitude}"
                )
            }
        )
    }
}
