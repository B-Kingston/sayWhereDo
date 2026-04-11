package com.example.reminders.network

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
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
 * Low-level HTTP client for the Gemini generative language API.
 *
 * Handles request construction, exponential-backoff retries on rate-limit
 * responses (HTTP 429), response parsing, and text cleaning (stripping
 * markdown code fences, wrapping single objects in arrays, removing
 * trailing commas).
 *
 * @param baseUrl The API base URL. Defaults to the Google generative language endpoint.
 *                 Overridden in tests to point at a [okhttp3.mockwebserver.MockWebServer].
 */
class GeminiApiClient(
    private val baseUrl: String = DEFAULT_BASE_URL
) {

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    /**
     * Sends a generate-content request to the Gemini API and returns the
     * cleaned JSON text extracted from the response.
     *
     * @param apiKey           The user's Gemini API key.
     * @param systemInstruction The system prompt that instructs the model how to format.
     * @param userText         The raw voice transcript to be formatted.
     * @return The cleaned JSON string representing a list of reminder objects.
     * @throws GeminiApiException On network errors, non-2xx responses, or parsing failures.
     */
    suspend fun generateContent(
        apiKey: String,
        systemInstruction: String,
        userText: String
    ): String {
        val request = buildRequest(apiKey, systemInstruction, userText)

        repeat(MAX_RETRIES) { attempt ->
            val response = try {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    httpClient.newCall(request).execute()
                }
            } catch (e: Exception) {
                throw GeminiApiException("Network request failed: ${e.message}", e)
            }

            response.use {
                when {
                    it.isSuccessful -> {
                        val body = it.body?.string()
                            ?: throw GeminiApiException("Empty response body")
                        return extractAndCleanText(body)
                    }
                    it.code == HTTP_TOO_MANY_REQUESTS && attempt < MAX_RETRIES - 1 -> {
                        val delayMs = INITIAL_RETRY_DELAY_MS * (1 shl attempt)
                        kotlinx.coroutines.delay(delayMs)
                    }
                    else -> {
                        val errorBody = it.body?.string() ?: "Unknown error"
                        throw GeminiApiException("HTTP ${it.code}: $errorBody")
                    }
                }
            }
        }

        throw GeminiApiException("Max retries ($MAX_RETRIES) exceeded after rate limiting")
    }

    /**
     * Builds the OkHttp [Request] with the Gemini API request body.
     */
    private fun buildRequest(
        apiKey: String,
        systemInstruction: String,
        userText: String
    ): Request {
        val url = "$baseUrl/v1beta/models/$MODEL_ID:generateContent?key=$apiKey"

        val requestBody = buildJsonObject {
            put("systemInstruction", buildJsonObject {
                put("parts", buildJsonArray {
                    add(buildJsonObject { put("text", JsonPrimitive(systemInstruction)) })
                })
             })
             put("contents", buildJsonArray {
                 add(buildJsonObject {
                     put("role", JsonPrimitive("user"))
                     put("parts", buildJsonArray {
                         add(buildJsonObject { put("text", JsonPrimitive(userText)) })
                    })
                })
            })
        }.toString()

        return Request.Builder()
            .url(url)
            .post(requestBody.toRequestBody(JSON_MEDIA_TYPE))
            .build()
    }

    /**
     * Extracts the generated text from the Gemini response JSON and
     * cleans it so downstream consumers receive valid JSON.
     */
    private fun extractAndCleanText(responseBody: String): String {
        val json = kotlinx.serialization.json.Json.parseToJsonElement(responseBody).jsonObject
        val text = json["candidates"]
            ?.jsonArray?.firstOrNull()
            ?.jsonObject?.get("content")
            ?.jsonObject?.get("parts")
            ?.jsonArray?.firstOrNull()
            ?.jsonObject?.get("text")
            ?.jsonPrimitive?.content
            ?: throw GeminiApiException("Could not extract text from response")

        return cleanJsonText(text)
    }

    /**
     * Strips markdown code fences, wraps bare JSON objects in an array,
     * and removes trailing commas so the output is valid JSON.
     */
    internal fun cleanJsonText(text: String): String {
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

    companion object {
        const val DEFAULT_BASE_URL = "https://generativelanguage.googleapis.com"
        private const val MODEL_ID = "gemini-2.5-flash-lite"
        private const val CONNECT_TIMEOUT_SECONDS = 10L
        private const val READ_TIMEOUT_SECONDS = 30L
        private const val MAX_RETRIES = 3
        private const val INITIAL_RETRY_DELAY_MS = 1000L
        private const val HTTP_TOO_MANY_REQUESTS = 429

        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        private val TRAILING_COMMA_BEFORE_CLOSE_BRACKET = Regex(""",\s*]""")
        private val TRAILING_COMMA_BEFORE_CLOSE_BRACE = Regex(""",\s*}""")
    }
}

/**
 * Thrown when the Gemini API returns a non-recoverable error or the
 * response cannot be parsed.
 */
class GeminiApiException(message: String, cause: Throwable? = null) : Exception(message, cause)
