package com.example.reminders.wear.formatting

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.example.reminders.wear.data.WearUserPreferences
import kotlinx.coroutines.flow.first

/**
 * Manages cloud formatting on the watch.
 *
 * Uses the same OpenAI-compatible client pattern as the phone module.
 * Credentials are synced from the phone via Wearable Data Layer and
 * stored in [WearUserPreferences].
 *
 * @param preferences Watch-side preferences containing synced credentials.
 * @param context     Application context for network connectivity checks.
 */
class WatchFormattingManager(
    private val preferences: WearUserPreferences,
    private val context: Context
) {

    /**
     * Checks whether standalone cloud formatting is available.
     *
     * Returns true only if cloud credentials have been synced from the
     * phone AND the watch has an active network connection (Wi-Fi/LTE).
     */
    suspend fun isAvailable(): Boolean {
        val hasCredentials = preferences.hasCloudCredentials.first()
        val hasNetwork = isNetworkAvailable()
        Log.d(TAG, "isAvailable: hasCredentials=$hasCredentials, hasNetwork=$hasNetwork")
        return hasCredentials && hasNetwork
    }

    /**
     * Formats a transcript using the cloud API directly from the watch.
     *
     * Loads credentials from [WearUserPreferences], creates a
     * [WatchCloudClient], and sends the formatting request.
     *
     * @param transcript The raw text from speech recognition.
     * @return The cleaned JSON string, or null on failure.
     */
    suspend fun format(transcript: String): String? {
        val apiKey = preferences.cloudApiKey.first()
        val baseUrl = preferences.cloudBaseUrl.first()
        val modelName = preferences.cloudModelName.first()

        if (apiKey.isBlank() || baseUrl.isBlank()) {
            Log.w(TAG, "Missing credentials — cannot format")
            return null
        }

        val providerName = preferences.cloudProviderName.first()
        Log.d(TAG, "Formatting via $providerName (model=$modelName)")

        return try {
            val client = WatchCloudClient(baseUrl, modelName)
            val prompt = FormattingPrompt.build()
            val response = client.chatCompletion(apiKey, prompt, transcript)

            FormattingResponseParser.parseResponse(response)
        } catch (e: Exception) {
            Log.e(TAG, "Cloud formatting failed: ${e.message}", e)
            null
        }
    }

    /**
     * Checks whether the watch currently has Wi-Fi or cellular connectivity.
     */
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE)
            as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
    }

    companion object {
        private const val TAG = "WatchFormattingMgr"
    }
}
