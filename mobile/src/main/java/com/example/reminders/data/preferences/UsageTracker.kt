package com.example.reminders.data.preferences

import android.content.Context
import android.util.Log
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
        private const val TAG = "UsageTracker"
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
                Log.i(TAG, "Formatting count reset for new day, starting at 1")
            } else {
                val newCount = (prefs[FORMATTING_COUNT] ?: 0) + 1
                prefs[FORMATTING_COUNT] = newCount
                Log.d(TAG, "Formatting count incremented to $newCount")
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
        val allowed = count < 1
        Log.d(TAG, "isFormattingAllowed: count=$count, allowed=$allowed")
        return allowed
    }

    suspend fun reset() {
        Log.i(TAG, "Usage tracker reset")
        context.usageDataStore.edit { it.clear() }
    }
}
