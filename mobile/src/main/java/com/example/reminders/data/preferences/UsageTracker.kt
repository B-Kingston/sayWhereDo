package com.example.reminders.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

private val Context.usageDataStore by preferencesDataStore(name = "usage_tracker")

class UsageTracker(private val context: Context) {

    private companion object {
        val LAST_RESET_DATE = stringPreferencesKey("last_reset_date")
        val FORMATTING_COUNT = intPreferencesKey("formatting_count")
    }

    private val today: String get() = LocalDate.now().toString()

    val formattingCount: Flow<Int> = context.usageDataStore.data.map { prefs ->
        val lastReset = prefs[LAST_RESET_DATE]
        if (lastReset != today) 0 else prefs[FORMATTING_COUNT] ?: 0
    }

    suspend fun incrementFormattingCount() {
        context.usageDataStore.edit { prefs ->
            val lastReset = prefs[LAST_RESET_DATE]
            if (lastReset != today) {
                prefs[LAST_RESET_DATE] = today
                prefs[FORMATTING_COUNT] = 1
            } else {
                prefs[FORMATTING_COUNT] = (prefs[FORMATTING_COUNT] ?: 0) + 1
            }
        }
    }

    suspend fun isFormattingAllowed(isPro: Boolean, hasApiKey: Boolean): Boolean {
        if (isPro || hasApiKey) return true
        var count = 0
        context.usageDataStore.data.map { prefs ->
            val lastReset = prefs[LAST_RESET_DATE]
            count = if (lastReset != today) 0 else (prefs[FORMATTING_COUNT] ?: 0)
        }
        return count < 1
    }

    suspend fun reset() {
        context.usageDataStore.edit { it.clear() }
    }
}
