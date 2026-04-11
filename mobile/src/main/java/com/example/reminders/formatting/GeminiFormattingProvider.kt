package com.example.reminders.formatting

import com.example.reminders.data.model.LocationTrigger
import com.example.reminders.data.model.ParsedReminder
import com.example.reminders.network.GeminiApiClient
import com.example.reminders.network.GeminiApiException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.Instant

/**
 * [FormattingProvider] that uses Google Gemini 2.5 Flash Lite via the
 * [GeminiApiClient] to parse voice transcripts into structured reminders.
 *
 * Handles malformed JSON responses gracefully: valid reminders are
 * extracted as a [FormattingResult.PartialSuccess] while the raw transcript
 * is preserved as a fallback.
 *
 * @param apiClient      The HTTP client used to call the Gemini API.
 * @param apiKeyProvider Suspend function that returns the current API key.
 *                       Allows the key to be refreshed at call time.
 */
class GeminiFormattingProvider(
    private val apiClient: GeminiApiClient,
    private val apiKeyProvider: suspend () -> String
) : FormattingProvider {

    private val jsonParser = Json { ignoreUnknownKeys = true }

    /**
     * Sends [transcript] to the Gemini API and parses the JSON response
     * into [ParsedReminder] objects.
     *
     * Returns [FormattingResult.Failure] if the API call fails or the
     * response cannot be parsed at all, [FormattingResult.PartialSuccess]
     * if some reminders parse but others do not, and
     * [FormattingResult.Success] when all reminders are extracted cleanly.
     */
    override suspend fun format(transcript: String): FormattingResult {
        val apiKey = try {
            apiKeyProvider()
        } catch (_: Exception) {
            return FormattingResult.Failure("No API key configured")
        }

        if (apiKey.isBlank()) {
            return FormattingResult.Failure("No API key configured")
        }

        val prompt = FormattingPrompt.build()

        return try {
            val jsonText = apiClient.generateContent(apiKey, prompt, transcript)
            parseReminders(jsonText, transcript)
        } catch (e: GeminiApiException) {
            FormattingResult.Failure(e.message ?: "Unknown API error")
        } catch (e: Exception) {
            FormattingResult.Failure("Formatting failed: ${e.message}")
        }
    }

    /**
     * Attempts to parse the JSON text into a list of [ParsedReminder]s.
     *
     * If the overall JSON is invalid, returns [FormattingResult.Failure].
     * If some elements in the array fail to parse, returns
     * [FormattingResult.PartialSuccess] with the valid ones plus the
     * raw transcript as a fallback.
     */
    private fun parseReminders(
        jsonText: String,
        rawTranscript: String
    ): FormattingResult {
        val parsedReminders = mutableListOf<ParsedReminder>()
        var hasFailures = false

        try {
            val jsonArray = jsonParser.parseToJsonElement(jsonText).jsonArray

            for (element in jsonArray) {
                try {
                    val obj = element.jsonObject
                    parsedReminders.add(parseSingleReminder(obj))
                } catch (_: Exception) {
                    hasFailures = true
                }
            }
        } catch (_: Exception) {
            return FormattingResult.Failure("Failed to parse formatting response")
        }

        return when {
            parsedReminders.isEmpty() -> FormattingResult.Failure(
                "No reminders could be parsed from response"
            )
            hasFailures -> FormattingResult.PartialSuccess(
                reminders = parsedReminders,
                rawFallback = rawTranscript
            )
            else -> FormattingResult.Success(parsedReminders)
        }
    }

    /**
     * Parses a single [JsonObject] into a [ParsedReminder].
     *
     * @throws IllegalArgumentException If required fields are missing.
     */
    private fun parseSingleReminder(obj: JsonObject): ParsedReminder {
        return ParsedReminder(
            title = obj.getRequiredString("title"),
            body = obj.getOptionalString("body"),
            triggerTime = obj.getOptionalString("triggerTime")?.let { Instant.parse(it) },
            recurrence = obj.getOptionalString("recurrence"),
            locationTrigger = (obj["locationTrigger"] as? JsonObject)?.let { locObj ->
                LocationTrigger(
                    placeLabel = locObj.getRequiredString("placeLabel"),
                    rawAddress = locObj.getOptionalString("rawAddress")
                )
            }
        )
    }

    private fun JsonObject.getRequiredString(key: String): String {
        val element = this[key]
            ?: throw IllegalArgumentException("Missing required field: $key")
        if (element is JsonNull) {
            throw IllegalArgumentException("Required field is null: $key")
        }
        return element.jsonPrimitive.content
    }

    private fun JsonObject.getOptionalString(key: String): String? {
        val element = this[key] ?: return null
        if (element is JsonNull) return null
        return element.jsonPrimitive.content
    }
}
