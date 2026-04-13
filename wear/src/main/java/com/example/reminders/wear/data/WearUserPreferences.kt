package com.example.reminders.wear.data

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * DataStore-backed preferences for the Wear OS module.
 *
 * Stores cloud AI credentials synced from the phone via the Wearable
 * Data Layer. Used by [WatchFormattingManager] to determine whether
 * standalone cloud formatting is available.
 *
 * TODO: Encrypt the API key using Android Keystore + EncryptedSharedPreferences.
 */
private val Context.wearDataStore by preferencesDataStore(name = "wear_user_preferences")

class WearUserPreferences(private val context: Context) {

    private companion object {
        private const val TAG = "WearUserPreferences"
        val CLOUD_API_KEY = stringPreferencesKey("cloud_api_key")
        val CLOUD_BASE_URL = stringPreferencesKey("cloud_base_url")
        val CLOUD_MODEL_NAME = stringPreferencesKey("cloud_model_name")
        val CLOUD_PROVIDER_NAME = stringPreferencesKey("cloud_provider_name")
    }

    /** Whether cloud credentials have been synced from the phone. */
    val hasCloudCredentials: Flow<Boolean> = context.wearDataStore.data.map { prefs ->
        !prefs[CLOUD_API_KEY].isNullOrBlank()
    }

    /** Display name of the configured cloud provider. */
    val cloudProviderName: Flow<String> = context.wearDataStore.data.map {
        it[CLOUD_PROVIDER_NAME] ?: ""
    }

    /** The synced API key for cloud formatting. */
    val cloudApiKey: Flow<String> = context.wearDataStore.data.map {
        it[CLOUD_API_KEY] ?: ""
    }

    /** The synced base URL for the cloud API. */
    val cloudBaseUrl: Flow<String> = context.wearDataStore.data.map {
        it[CLOUD_BASE_URL] ?: ""
    }

    /** The synced model name for the cloud API. */
    val cloudModelName: Flow<String> = context.wearDataStore.data.map {
        it[CLOUD_MODEL_NAME] ?: ""
    }

    /**
     * Updates the stored cloud credentials from a Data Layer sync event.
     */
    suspend fun updateCredentials(
        apiKey: String,
        baseUrl: String,
        modelName: String,
        providerName: String
    ) {
        Log.d(TAG, "Updating cloud credentials (provider=$providerName)")
        context.wearDataStore.edit { prefs ->
            prefs[CLOUD_API_KEY] = apiKey
            prefs[CLOUD_BASE_URL] = baseUrl
            prefs[CLOUD_MODEL_NAME] = modelName
            prefs[CLOUD_PROVIDER_NAME] = providerName
        }
    }

    /** Clears all cloud credentials. */
    suspend fun clearCredentials() {
        Log.d(TAG, "Clearing cloud credentials")
        context.wearDataStore.edit { prefs ->
            prefs.remove(CLOUD_API_KEY)
            prefs.remove(CLOUD_BASE_URL)
            prefs.remove(CLOUD_MODEL_NAME)
            prefs.remove(CLOUD_PROVIDER_NAME)
        }
    }
}
