package com.example.reminders.wear.wearable

import android.content.Context
import android.util.Log
import com.example.reminders.data.preferences.UserPreferences
import com.example.reminders.formatting.AiProviderPresets
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * Syncs cloud AI credentials from the phone to the watch via the
 * Wearable Data Layer.
 *
 * Observes [UserPreferences] for changes to the AI provider configuration
 * (provider ID, API key, base URL, model name) and sends updated
 * credentials to the watch whenever they change. Uses debouncing via
 * [distinctUntilChanged] to avoid rapid syncs during typing.
 *
 * @param context       Application context for accessing [DataClient].
 * @param userPreferences Phone-side preferences to observe.
 * @param coroutineScope Scope for launching sync coroutines.
 */
class CredentialSyncSender(
    private val context: Context,
    private val userPreferences: UserPreferences,
    private val coroutineScope: CoroutineScope
) {

    private val dataClient: DataClient by lazy {
        Wearable.getDataClient(context)
    }

    /**
     * Starts observing preference changes and syncing credentials
     * to connected watches.
     */
    fun startSyncing() {
        Log.d(TAG, "Starting credential sync observation")
        coroutineScope.launch(Dispatchers.IO) {
            combine(
                userPreferences.aiProviderId,
                userPreferences.apiKey,
                userPreferences.aiBaseUrl,
                userPreferences.aiModelName
            ) { providerId, apiKey, baseUrl, modelName ->
                SyncData(
                    providerId = providerId,
                    apiKey = apiKey ?: "",
                    baseUrl = baseUrl,
                    modelName = modelName
                )
            }
                .distinctUntilChanged()
                .collect { syncData -> syncCredentials(syncData) }
        }
    }

    private fun syncCredentials(data: SyncData) {
        Log.d(TAG, "Syncing credentials to watch (provider=${data.providerId})")

        val provider = AiProviderPresets.getById(data.providerId)
        val resolvedBaseUrl = data.baseUrl.ifBlank { provider?.baseUrl ?: "" }
        val resolvedModel = data.modelName.ifBlank { provider?.defaultModel ?: "" }
        val providerName = provider?.name ?: data.providerId

        try {
            val request = PutDataMapRequest.create(CREDENTIAL_SYNC_PATH).apply {
                dataMap.putString(KEY_API_KEY, data.apiKey)
                dataMap.putString(KEY_BASE_URL, resolvedBaseUrl)
                dataMap.putString(KEY_MODEL_NAME, resolvedModel)
                dataMap.putString(KEY_PROVIDER_NAME, providerName)
            }
                .asPutDataRequest()
                .setUrgent()

            dataClient.putDataItem(request)
            Log.d(TAG, "Credentials synced successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync credentials: ${e.message}", e)
        }
    }

    private data class SyncData(
        val providerId: String,
        val apiKey: String,
        val baseUrl: String,
        val modelName: String
    )

    companion object {
        private const val TAG = "CredentialSyncSender"
        private const val CREDENTIAL_SYNC_PATH = "/sync/credentials"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_BASE_URL = "base_url"
        private const val KEY_MODEL_NAME = "model_name"
        private const val KEY_PROVIDER_NAME = "provider_name"
    }
}
