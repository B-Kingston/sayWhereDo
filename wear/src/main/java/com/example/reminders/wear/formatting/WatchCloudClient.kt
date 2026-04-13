package com.example.reminders.wear.formatting

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
 * Minimal OpenAI-compatible HTTP client for the watch module.
 *
 * Sends chat completion requests to the configured cloud API and
 * returns the assistant's text response. Supports optional Bearer
 * token authentication and 429 retry with exponential backoff.
 *
 * @param baseUrl      API base URL (e.g. "https://api.groq.com/openai/v1").
 * @param defaultModel Default model identifier.
 */
class WatchCloudClient(
    private val baseUrl: String,
    private val defaultModel: String
) {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    /**
     * Sends a chat completion request and returns the assistant's text.
     *
     * @param apiKey       API key for authentication. Blank values skip the
     *                     Authorization header.
     * @param systemPrompt The system instruction guiding the model's behavior.
     * @param userMessage  The user's input text.
     * @return The assistant's text content from the first choice.
     * @throws WatchCloudException On network errors, non-2xx responses,
     *                             or parsing failures.
     */
    suspend fun chatCompletion(
        apiKey: String,
        systemPrompt: String,
        userMessage: String
    ): String {
        val request = buildRequest(apiKey, systemPrompt, userMessage)

        repeat(MAX_RETRIES) { attempt ->
            val response = withContext(Dispatchers.IO) {
                httpClient.newCall(request).execute()
            }

            response.use {
                when {
                    it.isSuccessful -> {
                        val body = it.body?.string()
                            ?: throw WatchCloudException("Empty response body")
                        return extractContent(body)
                    }
                    it.code == HTTP_TOO_MANY_REQUESTS && attempt < MAX_RETRIES - 1 -> {
                        val delayMs = INITIAL_RETRY_DELAY_MS * (1 shl attempt)
                        Log.w(TAG, "Rate limited, retrying in ${delayMs}ms")
                        delay(delayMs)
                    }
                    else -> {
                        val errorBody = it.body?.string() ?: "Unknown error"
                        throw WatchCloudException("HTTP ${it.code}: $errorBody")
                    }
                }
            }
        }

        throw WatchCloudException("Max retries exceeded after rate limiting")
    }

    /**
     * Builds the OkHttp [Request] with the OpenAI chat completions body.
     */
    private fun buildRequest(
        apiKey: String,
        systemPrompt: String,
        userMessage: String
    ): Request {
        val url = "$baseUrl/chat/completions"

        val requestBody = buildJsonObject {
            put("model", JsonPrimitive(defaultModel))
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
        return json["choices"]
            ?.jsonArray?.firstOrNull()
            ?.jsonObject?.get("message")
            ?.jsonObject?.get("content")
            ?.jsonPrimitive?.content
            ?: throw WatchCloudException("Could not extract content from response")
    }

    companion object {
        private const val TAG = "WatchCloudClient"
        private const val MAX_RETRIES = 3
        private const val INITIAL_RETRY_DELAY_MS = 1000L
        private const val CONNECT_TIMEOUT_SECONDS = 10L
        private const val READ_TIMEOUT_SECONDS = 30L
        private const val HTTP_TOO_MANY_REQUESTS = 429
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}

/**
 * Thrown when the watch cloud API returns a non-recoverable error
 * or the response cannot be parsed.
 */
class WatchCloudException(message: String, cause: Throwable? = null) : Exception(message, cause)
