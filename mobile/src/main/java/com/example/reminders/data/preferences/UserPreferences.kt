package com.example.reminders.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "user_preferences")

class UserPreferences(private val context: Context) {

    private companion object {
        val API_KEY = stringPreferencesKey("api_key")
        val FORMATTING_PROVIDER = stringPreferencesKey("formatting_provider")
        val GEOFENCING_DEVICE = stringPreferencesKey("geofencing_device")
    }

    val apiKey: Flow<String?> = context.dataStore.data.map { it[API_KEY] }
    val formattingProvider: Flow<String> = context.dataStore.data.map { it[FORMATTING_PROVIDER] ?: "gemini" }
    val geofencingDevice: Flow<String> = context.dataStore.data.map { it[GEOFENCING_DEVICE] ?: "auto" }

    suspend fun setApiKey(key: String?) {
        context.dataStore.edit { prefs ->
            if (key == null) prefs.remove(API_KEY) else prefs[API_KEY] = key
        }
    }

    suspend fun setFormattingProvider(provider: String) {
        context.dataStore.edit { it[FORMATTING_PROVIDER] = provider }
    }

    suspend fun setGeofencingDevice(device: String) {
        context.dataStore.edit { it[GEOFENCING_DEVICE] = device }
    }
}
