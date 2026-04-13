package com.example.reminders.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Generic HTTP client for any OpenAI-compatible chat completions API.
 *
 * Supports OpenAI, Gemini (via OpenAI-compatible endpoint), Groq, Together AI,
 * Fireworks, self-hosted Ollama/vLLM, and any provider that exposes
 * `/v1/chat/completions`.
 *
 * Handles request construction, Bearer token authentication (optional for
 * unauthenticated endpoints like Ollama), exponential-backoff retries on
 * rate-limit responses (HTTP 429), and response parsing.
 *
 * @param baseUrl      The API base URL including the version path
 *                     (e.g. `https://api.openai.com/v1`).
 * @param defaultModel The model identifier to use when no override is provided.
 */
class OpenAiCompatibleClient(
    private val baseUrl: String,
    private val defaultModel: String
) {

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    /**
     * Sends a chat completion request and returns the assistant's text response.
     *
     * @param apiKey       API key for authentication. Blank values skip the
     *                     Authorization header (for Ollama and similar).
     * @param systemPrompt The system instruction guiding the model's behavior.
     * @param userMessage  The user's input text.
     * @param model        Model identifier override. Defaults to [defaultModel].
     * @param temperature  Sampling temperature (0.0–2.0). Defaults to [DEFAULT_TEMPERATURE].
     * @return The assistant's text content from the first choice.
     * @throws OpenAiCompatibleException On network errors, non-2xx responses,
     *                                   or parsing failures.
     */
    suspend fun chatCompletion(
        apiKey: String,
        systemPrompt: String,
        userMessage: String,
        model: String = defaultModel,
        temperature: Float = DEFAULT_TEMPERATURE
    ): String {
        val request = buildRequest(apiKey, systemPrompt, userMessage, model, temperature)
        Log.d(TAG, "Sending chat completion request (model=$model, transcript: ${userMessage.take(60)})")

        repeat(MAX_RETRIES) { attempt ->
            val response = try {
                withContext(Dispatchers.IO) {
                    httpClient.newCall(request).execute()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Network request failed: ${e.message}", e)
                throw OpenAiCompatibleException("Network request failed: ${e.message}", e)
            }

            response.use {
                when {
                    it.isSuccessful -> {
                        val body = it.body?.string()
                            ?: throw OpenAiCompatibleException("Empty response body")
                        Log.d(TAG, "Chat completion response received (${body.length} chars)")
                        return extractContent(body)
                    }
                    it.code == HTTP_TOO_MANY_REQUESTS && attempt < MAX_RETRIES - 1 -> {
                        val delayMs = INITIAL_RETRY_DELAY_MS * (1 shl attempt)
                        Log.w(TAG, "Rate limited (429), retrying in ${delayMs}ms (attempt ${attempt + 1}/$MAX_RETRIES)")
                        delay(delayMs)
                    }
                    else -> {
                        val errorBody = it.body?.string() ?: "Unknown error"
                        Log.e(TAG, "API error HTTP ${it.code}: ${errorBody.take(200)}")
                        throw OpenAiCompatibleException("HTTP ${it.code}: $errorBody")
                    }
                }
            }
        }

        throw OpenAiCompatibleException("Max retries ($MAX_RETRIES) exceeded after rate limiting")
    }

    /**
     * Builds the OkHttp [Request] with the OpenAI chat completions body.
     *
     * Includes the Bearer Authorization header only when [apiKey] is non-blank.
     */
    private fun buildRequest(
        apiKey: String,
        systemPrompt: String,
        userMessage: String,
        model: String,
        temperature: Float
    ): Request {
        val url = "$baseUrl/chat/completions"

        val requestBody = buildJsonObject {
            put("model", JsonPrimitive(model))
            put("messages", buildJsonArray {
                add(buildJsonObject {
                    put("role", JsonPrimitive("system"))
                    put("content", JsonPrimitive(systemPrompt))
                })
                add(buildJsonObject {
                    put("role", JsonPrimitive("user"))
                    put("content", JsonPrimitive(userMessage))
                })
            })
            put("temperature", JsonPrimitive(temperature))
        }.toString()

        val requestBuilder = Request.Builder()
            .url(url)
            .post(requestBody.toRequestBody(JSON_MEDIA_TYPE))

        if (apiKey.isNotBlank()) {
            requestBuilder.header("Authorization", "Bearer $apiKey")
        }

        return requestBuilder.build()
    }

    /**
     * Extracts the assistant's text content from the OpenAI-format JSON response.
     *
     * Navigates `choices[0].message.content` and returns the string value.
     */
    private fun extractContent(responseBody: String): String {
        val json = kotlinx.serialization.json.Json.parseToJsonElement(responseBody).jsonObject
        val choices = json["choices"]
            ?.jsonArray
            ?: throw OpenAiCompatibleException("Missing 'choices' in response")

        if (choices.isEmpty()) {
            throw OpenAiCompatibleException("Empty 'choices' array in response")
        }

        val content = choices[0]
            .jsonObject["message"]
            ?.jsonObject?.get("content")
            ?.jsonPrimitive?.content
            ?: throw OpenAiCompatibleException("Could not extract content from response")

        return content
    }

    companion object {
        private const val TAG = "OpenAiCompatible"
        private const val MAX_RETRIES = 3
        private const val INITIAL_RETRY_DELAY_MS = 1000L
        private const val DEFAULT_TEMPERATURE = 0.7f
        private const val CONNECT_TIMEOUT_SECONDS = 10L
        private const val READ_TIMEOUT_SECONDS = 30L
        private const val HTTP_TOO_MANY_REQUESTS = 429
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}

/**
 * Thrown when an OpenAI-compatible API returns a non-recoverable error
 * or the response cannot be parsed.
 */
class OpenAiCompatibleException(message: String, cause: Throwable? = null) : Exception(message, cause)
