package com.example.reminders.data.preferences

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "user_preferences")

class UserPreferences(private val context: Context) {

    private companion object {
        private const val TAG = "UserPreferences"
        val API_KEY = stringPreferencesKey("api_key")
        val FORMATTING_PROVIDER = stringPreferencesKey("formatting_provider")
        val GEOFENCING_DEVICE = stringPreferencesKey("geofencing_device")
        val AI_PROVIDER_ID = stringPreferencesKey("ai_provider_id")
        val AI_BASE_URL = stringPreferencesKey("ai_base_url")
        val AI_MODEL_NAME = stringPreferencesKey("ai_model_name")
        val FORMATTING_BACKEND = stringPreferencesKey("formatting_backend")
        val LOCAL_MODEL_ID = stringPreferencesKey("local_model_id")
    }

    val apiKey: Flow<String?> = context.dataStore.data.map { it[API_KEY] }
    val formattingProvider: Flow<String> = context.dataStore.data.map { it[FORMATTING_PROVIDER] ?: "gemini" }
    val geofencingDevice: Flow<String> = context.dataStore.data.map { it[GEOFENCING_DEVICE] ?: "auto" }

    val aiProviderId: Flow<String> = context.dataStore.data.map { it[AI_PROVIDER_ID] ?: "gemini" }
    val aiBaseUrl: Flow<String> = context.dataStore.data.map { it[AI_BASE_URL] ?: "" }
    val aiModelName: Flow<String> = context.dataStore.data.map { it[AI_MODEL_NAME] ?: "" }
    val formattingBackend: Flow<String> = context.dataStore.data.map { it[FORMATTING_BACKEND] ?: "cloud" }
    val localModelId: Flow<String?> = context.dataStore.data.map { it[LOCAL_MODEL_ID] }

    suspend fun setApiKey(key: String?) {
        Log.d(TAG, "setApiKey: ${if (key != null) "***updated***" else "cleared"}")
        context.dataStore.edit { prefs ->
            if (key == null) prefs.remove(API_KEY) else prefs[API_KEY] = key
        }
    }

    suspend fun setFormattingProvider(provider: String) {
        Log.d(TAG, "setFormattingProvider: $provider")
        context.dataStore.edit { it[FORMATTING_PROVIDER] = provider }
    }

    suspend fun setGeofencingDevice(device: String) {
        Log.d(TAG, "setGeofencingDevice: $device")
        context.dataStore.edit { it[GEOFENCING_DEVICE] = device }
    }

    suspend fun setAiProviderId(id: String) {
        Log.d(TAG, "setAiProviderId: $id")
        context.dataStore.edit { it[AI_PROVIDER_ID] = id }
    }

    suspend fun setAiBaseUrl(url: String) {
        Log.d(TAG, "setAiBaseUrl: ${url.take(30)}")
        context.dataStore.edit { it[AI_BASE_URL] = url }
    }

    suspend fun setAiModelName(name: String) {
        Log.d(TAG, "setAiModelName: $name")
        context.dataStore.edit { it[AI_MODEL_NAME] = name }
    }

    suspend fun setFormattingBackend(backend: String) {
        Log.d(TAG, "setFormattingBackend: $backend")
        context.dataStore.edit { it[FORMATTING_BACKEND] = backend }
    }

    suspend fun setLocalModelId(id: String?) {
        Log.d(TAG, "setLocalModelId: ${id ?: "cleared"}")
        context.dataStore.edit { prefs ->
            if (id == null) prefs.remove(LOCAL_MODEL_ID) else prefs[LOCAL_MODEL_ID] = id
        }
    }
}
