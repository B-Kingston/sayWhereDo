package com.example.reminders.formatting

import android.util.Log
import com.example.reminders.network.GeminiApiClient
import com.example.reminders.network.GeminiApiException

/**
 * [FormattingProvider] that uses Google Gemini via the [GeminiApiClient]
 * to parse voice transcripts into structured reminders.
 *
 * Delegates JSON parsing to [FormattingResponseParser], which is shared
 * across all formatting providers (Gemini, OpenAI-compatible, local).
 *
 * @param apiClient      The HTTP client used to call the Gemini API.
 * @param apiKeyProvider Suspend function that returns the current API key.
 *                       Allows the key to be refreshed at call time.
 */
class GeminiFormattingProvider(
    private val apiClient: GeminiApiClient,
    private val apiKeyProvider: suspend () -> String
) : FormattingProvider {

    /**
     * Sends [transcript] to the Gemini API and parses the JSON response
     * into [ParsedReminder] objects via [FormattingResponseParser].
     *
     * Returns [FormattingResult.Failure] if the API call fails or the
     * response cannot be parsed at all, [FormattingResult.PartialSuccess]
     * if some reminders parse but others do not, and
     * [FormattingResult.Success] when all reminders are extracted cleanly.
     */
    override suspend fun format(transcript: String): FormattingResult {
        val apiKey = try {
            apiKeyProvider()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to retrieve API key", e)
            return FormattingResult.Failure("No API key configured")
        }

        if (apiKey.isBlank()) {
            Log.w(TAG, "API key is blank — returning failure")
            return FormattingResult.Failure("No API key configured")
        }

        val prompt = FormattingPrompt.build()

        return try {
            Log.d(TAG, "Sending formatting request for transcript: ${transcript.take(80)}")
            val jsonText = apiClient.generateContent(apiKey, prompt, transcript)
            Log.d(TAG, "Received formatting response (${jsonText.length} chars)")
            FormattingResponseParser.parse(jsonText, transcript)
        } catch (e: GeminiApiException) {
            Log.e(TAG, "Gemini API error: ${e.message}", e)
            FormattingResult.Failure(e.message ?: "Unknown API error")
        } catch (e: Exception) {
            Log.e(TAG, "Formatting failed: ${e.message}", e)
            FormattingResult.Failure("Formatting failed: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "GeminiFormatting"
    }
}
