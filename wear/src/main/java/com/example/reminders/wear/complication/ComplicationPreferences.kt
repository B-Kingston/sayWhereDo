package com.example.reminders.wear.complication

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ComplicationPreferences(private val context: Context) {

    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
        name = "complication_prefs"
    )

    val complicationMode: Flow<ComplicationMode> = context.dataStore.data.map { prefs ->
        val name = prefs[COMPLICATION_MODE_KEY] ?: ComplicationMode.TODAY.name
        runCatching { ComplicationMode.valueOf(name) }.getOrDefault(ComplicationMode.TODAY)
    }

    suspend fun setComplicationMode(mode: ComplicationMode) {
        context.dataStore.edit { prefs ->
            prefs[COMPLICATION_MODE_KEY] = mode.name
        }
    }

    companion object {
        private val COMPLICATION_MODE_KEY = stringPreferencesKey("complication_mode")
    }
}
