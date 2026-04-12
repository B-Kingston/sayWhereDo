package com.example.reminders.formatting

import android.util.Log
import com.example.reminders.data.model.LocationTrigger
import com.example.reminders.data.model.ParsedReminder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Shared utility for parsing LLM-generated JSON responses into [ParsedReminder]s.
 *
 * All formatting providers (Gemini, OpenAI-compatible, local) produce the same
 * JSON schema, so parsing logic is centralized here. The parser is tolerant of
 * common LLM output issues: markdown code fences, bare JSON objects, trailing
 * commas, and partial array failures.
 */
object FormattingResponseParser {

    private val jsonParser = Json { ignoreUnknownKeys = true }

    /**
     * Parses raw JSON text from any LLM into a [FormattingResult].
     *
     * Handles:
     * - Valid JSON arrays of reminder objects → [FormattingResult.Success]
     * - Arrays where some elements fail to parse → [FormattingResult.PartialSuccess]
     * - Bare JSON objects (not wrapped in array) → auto-wrapped and parsed
     * - Markdown code fences (```json ... ```) → stripped before parsing
     * - Trailing commas → cleaned before parsing
     * - Completely invalid JSON → [FormattingResult.Failure]
     *
     * @param jsonText       The raw text returned by the LLM.
     * @param rawTranscript  The original user transcript (used as fallback).
     * @return A [FormattingResult] with parsed reminders or an error.
     */
    fun parse(jsonText: String, rawTranscript: String): FormattingResult {
        val cleaned = cleanJsonText(jsonText)
        val parsedReminders = mutableListOf<ParsedReminder>()
        var hasFailures = false

        try {
            val jsonArray = jsonParser.parseToJsonElement(cleaned).jsonArray

            for (element in jsonArray) {
                try {
                    val obj = element.jsonObject
                    parsedReminders.add(parseSingleReminder(obj))
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse individual reminder element: ${e.message}")
                    hasFailures = true
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse JSON response: ${e.message}")
            return FormattingResult.Failure("Failed to parse formatting response")
        }

        return when {
            parsedReminders.isEmpty() -> {
                Log.w(TAG, "No reminders could be parsed from response")
                FormattingResult.Failure("No reminders could be parsed from response")
            }
            hasFailures -> {
                Log.w(TAG, "Partial parse: ${parsedReminders.size} reminder(s) parsed, some failed")
                FormattingResult.PartialSuccess(
                    reminders = parsedReminders,
                    rawFallback = rawTranscript
                )
            }
            else -> {
                Log.i(TAG, "Successfully parsed ${parsedReminders.size} reminder(s)")
                FormattingResult.Success(parsedReminders)
            }
        }
    }

    /**
     * Parses a single [JsonObject] into a [ParsedReminder].
     *
     * Expected schema:
     * ```json
     * {
     *   "title": string (required),
     *   "body": string or null,
     *   "triggerTime": ISO 8601 string or null,
     *   "recurrence": "daily" | "weekly" | "monthly" | null,
     *   "locationTrigger": { "placeLabel": string, "rawAddress": string or null } | null
     * }
     * ```
     *
     * @param obj The JSON object representing a single reminder.
     * @return A fully-populated [ParsedReminder].
     * @throws IllegalArgumentException If required fields are missing.
     */
    fun parseSingleReminder(obj: JsonObject): ParsedReminder {
        return ParsedReminder(
            title = obj.getRequiredString("title"),
            body = obj.getOptionalString("body"),
            triggerTime = obj.getOptionalString("triggerTime")?.let { java.time.Instant.parse(it) },
            recurrence = obj.getOptionalString("recurrence"),
            locationTrigger = (obj["locationTrigger"] as? JsonObject)?.let { locObj ->
                LocationTrigger(
                    placeLabel = locObj.getRequiredString("placeLabel"),
                    rawAddress = locObj.getOptionalString("rawAddress")
                )
            }
        )
    }

    /**
     * Cleans raw LLM output text into valid JSON.
     *
     * Handles common LLM formatting issues:
     * - Strips markdown code fences (` ```json ... ``` ` or ` ``` ... ``` `)
     * - Wraps bare JSON objects in an array
     * - Removes trailing commas before `]` or `}`
     *
     * @param text The raw text returned by the LLM.
     * @return Cleaned JSON string ready for parsing.
     */
    fun cleanJsonText(text: String): String {
        var cleaned = text.trim()

        // Strip markdown code fences: ```json ... ``` or ``` ... ```
        if (cleaned.startsWith("```")) {
            val firstNewline = cleaned.indexOf('\n')
            if (firstNewline != -1) {
                cleaned = cleaned.substring(firstNewline + 1)
            }
            if (cleaned.endsWith("```")) {
                cleaned = cleaned.dropLast(3)
            }
            cleaned = cleaned.trim()
        }

        // Wrap a bare JSON object in an array
        if (cleaned.startsWith("{") && !cleaned.startsWith("[")) {
            cleaned = "[$cleaned]"
        }

        // Remove trailing commas before ] or }
        cleaned = cleaned.replace(TRAILING_COMMA_BEFORE_CLOSE_BRACKET, "]")
        cleaned = cleaned.replace(TRAILING_COMMA_BEFORE_CLOSE_BRACE, "}")

        return cleaned
    }

    /**
     * Extracts a required string field from a [JsonObject].
     *
     * @throws IllegalArgumentException If the field is missing or null.
     */
    internal fun JsonObject.getRequiredString(key: String): String {
        val element = this[key]
            ?: throw IllegalArgumentException("Missing required field: $key")
        if (element is JsonNull) {
            throw IllegalArgumentException("Required field is null: $key")
        }
        return element.jsonPrimitive.content
    }

    /**
     * Extracts an optional string field from a [JsonObject].
     *
     * Returns null if the field is missing or explicitly null.
     */
    internal fun JsonObject.getOptionalString(key: String): String? {
        val element = this[key] ?: return null
        if (element is JsonNull) return null
        return element.jsonPrimitive.content
    }

    private const val TAG = "FormattingParser"
    private val TRAILING_COMMA_BEFORE_CLOSE_BRACKET = Regex(""",\s*\]""")
    private val TRAILING_COMMA_BEFORE_CLOSE_BRACE = Regex(""",\s*\}""")
}
