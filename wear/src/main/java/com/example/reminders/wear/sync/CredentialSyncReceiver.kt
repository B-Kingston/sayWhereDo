package com.example.reminders.wear.sync

import android.util.Log
import com.example.reminders.wear.data.WearUserPreferences
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService

/**
 * Wearable Data Layer listener that receives cloud AI credentials
 * synced from the companion phone.
 *
 * When credential data items arrive at [CREDENTIAL_SYNC_PATH], this
 * service extracts the API key, base URL, model name, and provider
 * name, then stores them in [WearUserPreferences] for use by the
 * watch's standalone cloud formatting feature.
 */
class CredentialSyncReceiver : WearableListenerService() {

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        Log.d(TAG, "onDataChanged: ${dataEvents.count} event(s)")

        dataEvents.forEach { event ->
            if (event.type == DataEvent.TYPE_CHANGED &&
                event.dataItem.uri.path == CREDENTIAL_SYNC_PATH
            ) {
                val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                val apiKey = dataMap.getString(KEY_API_KEY, "")
                val baseUrl = dataMap.getString(KEY_BASE_URL, "")
                val modelName = dataMap.getString(KEY_MODEL_NAME, "")
                val providerName = dataMap.getString(KEY_PROVIDER_NAME, "")

                Log.d(TAG, "Received credentials (provider=$providerName)")

                val prefs = WearUserPreferences(this)
                kotlinx.coroutines.runBlocking {
                    prefs.updateCredentials(
                        apiKey = apiKey,
                        baseUrl = baseUrl,
                        modelName = modelName,
                        providerName = providerName
                    )
                }
            }
        }
    }

    companion object {
        private const val TAG = "CredentialSyncReceiver"
        private const val CREDENTIAL_SYNC_PATH = "/sync/credentials"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_BASE_URL = "base_url"
        private const val KEY_MODEL_NAME = "model_name"
        private const val KEY_PROVIDER_NAME = "provider_name"
    }
}
