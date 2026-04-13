package com.example.reminders.wear.formatting

import android.util.Log
import kotlinx.serialization.json.Json

/**
 * Parses LLM formatting responses on the watch.
 *
 * Handles markdown code fences, trailing commas, and bare JSON objects.
 * Returns the cleaned JSON string for the watch to send to the phone
 * via the Data Layer, or to display locally.
 */
object FormattingResponseParser {

    private val relaxedJson = Json { ignoreUnknownKeys = true }

    /**
     * Cleans and parses the LLM response text into a valid JSON string.
     *
     * @param text Raw response text from the LLM.
     * @return Cleaned JSON string, or null if parsing fails.
     */
    fun parseResponse(text: String): String? {
        val cleaned = cleanJsonText(text)
        return try {
            relaxedJson.parseToJsonElement(cleaned)
            cleaned
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse cleaned JSON: ${e.message}")
            null
        }
    }

    /**
     * Strips markdown code fences, wraps bare JSON objects in arrays,
     * and removes trailing commas so the output is valid JSON.
     */
    fun cleanJsonText(text: String): String {
        var cleaned = text.trim()

        // Strip markdown code fences
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.removePrefix("```json").trimStart()
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.removePrefix("```").trimStart()
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.removeSuffix("```").trimEnd()
        }

        // Remove trailing commas before } or ]
        cleaned = cleaned.replace(Regex(TRAILING_COMMA_PATTERN), "$1")

        // Wrap bare JSON object in array
        if (cleaned.startsWith("{") && !cleaned.startsWith("[")) {
            cleaned = "[$cleaned]"
        }

        return cleaned
    }

    private const val TAG = "WatchFmtParser"
    private const val TRAILING_COMMA_PATTERN = """,\s*([}\]])"""
}
