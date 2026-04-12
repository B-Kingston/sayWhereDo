package com.example.reminders.data.export

import android.util.Log
import com.example.reminders.data.model.Reminder
import com.example.reminders.data.model.SavedPlace
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Handles JSON export and import of reminders and saved places.
 *
 * Exported format is a flat JSON object containing arrays of reminders
 * and saved places. The format is forward-compatible: unknown fields
 * are ignored during import, so newer export files can be imported by
 * older app versions without errors.
 *
 * Import behaviour:
 * - Duplicate IDs are skipped (not overwritten).
 * - All imported reminders are marked with [Reminder.formattingProvider]
 *   set to "imported" to distinguish them from user-created reminders.
 *
 * This is a Pro-only feature. The caller is responsible for checking
 * [com.example.reminders.billing.BillingManager.isPro] before invoking
 * export or import methods.
 */
class ReminderExporter {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    /**
     * Serialises the given reminders and saved places to a JSON string.
     *
     * @param reminders   All reminders to include in the export.
     * @param savedPlaces All saved places to include in the export.
     * @return A pretty-printed JSON string representing the export bundle.
     */
    fun exportToJson(
        reminders: List<Reminder>,
        savedPlaces: List<SavedPlace>
    ): String {
        Log.d(TAG, "Exporting ${reminders.size} reminder(s), ${savedPlaces.size} saved place(s)")
        val bundle = ExportBundle(
            version = EXPORT_FORMAT_VERSION,
            reminders = reminders.map { it.toExportReminder() },
            savedPlaces = savedPlaces.map { it.toExportSavedPlace() },
            exportedAt = System.currentTimeMillis()
        )
        val json = json.encodeToString(ExportBundle.serializer(), bundle)
        Log.i(TAG, "Export complete: ${json.length} chars")
        return json
    }

    /**
     * Parses a JSON export string and returns the contained data.
     *
     * @param jsonText The JSON string produced by [exportToJson].
     * @return An [ImportResult] containing the parsed data and stats.
     */
    fun parseImport(jsonText: String): ImportResult {
        Log.d(TAG, "Parsing import (${jsonText.length} chars)")
        return try {
            val bundle = json.decodeFromString(ExportBundle.serializer(), jsonText)
            Log.i(TAG, "Import parsed: ${bundle.reminders.size} reminder(s), ${bundle.savedPlaces.size} saved place(s)")
            ImportResult.Success(
                reminders = bundle.reminders.map { it.toReminder() },
                savedPlaces = bundle.savedPlaces.map { it.toSavedPlace() }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Import parse failed: ${e.message}", e)
            ImportResult.ParseError(e.message ?: "Failed to parse import file")
        }
    }

    companion object {
        private const val TAG = "ReminderExporter"
        const val EXPORT_FORMAT_VERSION = 1
    }
}

/**
 * Result of parsing an import file.
 */
sealed interface ImportResult {

    /**
     * The import file was parsed successfully.
     *
     * @property reminders   Reminders parsed from the file.
     * @property savedPlaces Saved places parsed from the file.
     */
    data class Success(
        val reminders: List<Reminder>,
        val savedPlaces: List<SavedPlace>
    ) : ImportResult

    /**
     * The import file could not be parsed.
     *
     * @property message A human-readable error description.
     */
    data class ParseError(val message: String) : ImportResult
}

/**
 * Top-level container for the export JSON format.
 */
@Serializable
data class ExportBundle(
    val version: Int,
    val reminders: List<ExportReminder>,
    val savedPlaces: List<ExportSavedPlace>,
    val exportedAt: Long
)

/**
 * Serialisable representation of a [Reminder] for export/import.
 *
 * Uses epoch millis for [Instant] fields and JSON strings for
 * complex types (LocationTrigger) to avoid KSP2 serialization bugs
 * with Room @Embedded types.
 */
@Serializable
data class ExportReminder(
    val id: String,
    val title: String,
    val body: String? = null,
    val triggerTimeEpochMillis: Long? = null,
    val recurrence: String? = null,
    val locationTriggerJson: String? = null,
    val locationState: String? = null,
    val sourceTranscript: String = "",
    val formattingProvider: String = "none",
    val isCompleted: Boolean = false,
    val createdAtEpochMillis: Long = 0L,
    val updatedAtEpochMillis: Long = 0L
)

/**
 * Serialisable representation of a [SavedPlace] for export/import.
 */
@Serializable
data class ExportSavedPlace(
    val id: String,
    val label: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val defaultRadiusMetres: Int = 150
)
