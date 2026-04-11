package com.example.reminders.geocoding

import android.location.Geocoder
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
        if (!Geocoder.isPresent()) {
            return GeocodingResult.Error("Geocoder is not available on this device")
        }

        return suspendCancellableCoroutine { continuation ->
            geocoder.getFromLocationName(
                query,
                MAX_RESULTS,
                object : Geocoder.GeocodeListener {
                    override fun onGeocode(addresses: MutableList<android.location.Address>) {
                        continuation.resume(addresses.toGeocodingResult())
                    }

                    override fun onError(errorMessage: String?) {
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
