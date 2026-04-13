package com.example.reminders.formatting

import android.util.Log
import com.example.reminders.network.OpenAiCompatibleClient
import com.example.reminders.network.OpenAiCompatibleException

/**
 * [FormattingProvider] that uses any OpenAI-compatible cloud API.
 *
 * Works with: Gemini (via OpenAI-compatible endpoint), OpenAI, Groq,
 * Together AI, Fireworks, self-hosted Ollama/vLLM, and any provider
 * exposing `/v1/chat/completions`.
 *
 * Unlike [GeminiFormattingProvider], this provider does not reject blank
 * API keys — some self-hosted endpoints (Ollama) don't require authentication.
 *
 * @param apiClient       The OpenAI-compatible HTTP client.
 * @param apiKeyProvider  Suspend function that returns the current API key.
 *                        Returns empty string for unauthenticated endpoints.
 */
class CloudFormattingProvider(
    private val apiClient: OpenAiCompatibleClient,
    private val apiKeyProvider: suspend () -> String
) : FormattingProvider {

    /**
     * Sends [transcript] to the cloud API and parses the JSON response
     * into [ParsedReminder] objects via [FormattingResponseParser].
     */
    override suspend fun format(transcript: String): FormattingResult {
        val apiKey = try {
            apiKeyProvider()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to retrieve API key", e)
            return FormattingResult.Failure("No API key configured")
        }

        val prompt = FormattingPrompt.build()

        return try {
            Log.d(TAG, "Sending formatting request for transcript: ${transcript.take(80)}")
            val jsonText = apiClient.chatCompletion(apiKey, prompt, transcript)
            Log.d(TAG, "Received formatting response (${jsonText.length} chars)")
            FormattingResponseParser.parse(jsonText, transcript)
        } catch (e: OpenAiCompatibleException) {
            Log.e(TAG, "Cloud API error: ${e.message}", e)
            FormattingResult.Failure(e.message ?: "Unknown API error")
        } catch (e: Exception) {
            Log.e(TAG, "Formatting failed: ${e.message}", e)
            FormattingResult.Failure("Formatting failed: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "CloudFormatting"
    }
}
