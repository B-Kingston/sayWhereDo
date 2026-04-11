package com.example.reminders.formatting

import com.example.reminders.network.GeminiApiClient
import com.example.reminders.network.GeminiApiException
import com.google.common.truth.Truth.assertThat
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Verifies that [GeminiFormattingProvider] correctly parses Gemini API
 * responses, handles malformed JSON, code fences, empty responses,
 * and rate-limit errors.
 */
class GeminiFormattingProviderTest {

    private val server = MockWebServer()
    private lateinit var apiClient: GeminiApiClient
    private lateinit var provider: GeminiFormattingProvider

    @Before
    fun setUp() {
        server.start()
        apiClient = GeminiApiClient(baseUrl = server.url("").toString())
        provider = GeminiFormattingProvider(
            apiClient = apiClient,
            apiKeyProvider = { TEST_API_KEY }
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `valid JSON array returns Success with reminders`() = runTest {
        server.enqueue(geminiResponse("""[{"title":"Buy milk","body":null,"triggerTime":"2026-04-12T15:00:00Z","recurrence":null,"locationTrigger":null}]"""))

        val result = provider.format("remind me to buy milk tomorrow at 3pm")

        assertThat(result).isInstanceOf(FormattingResult.Success::class.java)
        val reminders = (result as FormattingResult.Success).reminders
        assertThat(reminders).hasSize(1)
        assertThat(reminders[0].title).isEqualTo("Buy milk")
        assertThat(reminders[0].triggerTime).isNotNull()
    }

    @Test
    fun `code-fenced JSON is parsed correctly`() = runTest {
        server.enqueue(geminiResponse("```json\n[{\"title\":\"Buy milk\",\"body\":null,\"triggerTime\":null,\"recurrence\":null,\"locationTrigger\":null}]\n```"))

        val result = provider.format("buy milk")

        assertThat(result).isInstanceOf(FormattingResult.Success::class.java)
        val reminders = (result as FormattingResult.Success).reminders
        assertThat(reminders).hasSize(1)
        assertThat(reminders[0].title).isEqualTo("Buy milk")
    }

    @Test
    fun `single JSON object is wrapped in array`() = runTest {
        server.enqueue(geminiResponse("""{"title":"Buy milk","body":null,"triggerTime":null,"recurrence":null,"locationTrigger":null}"""))

        val result = provider.format("buy milk")

        assertThat(result).isInstanceOf(FormattingResult.Success::class.java)
        val reminders = (result as FormattingResult.Success).reminders
        assertThat(reminders).hasSize(1)
    }

    @Test
    fun `multiple reminders are parsed`() = runTest {
        server.enqueue(geminiResponse("""[{"title":"Buy milk","body":null,"triggerTime":null,"recurrence":null,"locationTrigger":null},{"title":"Call dentist","body":"Schedule appointment","triggerTime":"2026-04-17T09:00:00Z","recurrence":null,"locationTrigger":null}]"""))

        val result = provider.format("buy milk and call the dentist")

        assertThat(result).isInstanceOf(FormattingResult.Success::class.java)
        val reminders = (result as FormattingResult.Success).reminders
        assertThat(reminders).hasSize(2)
        assertThat(reminders[0].title).isEqualTo("Buy milk")
        assertThat(reminders[1].title).isEqualTo("Call dentist")
        assertThat(reminders[1].body).isEqualTo("Schedule appointment")
    }

    @Test
    fun `reminder with location trigger is parsed`() = runTest {
        server.enqueue(geminiResponse("""[{"title":"Get eggs","body":null,"triggerTime":null,"recurrence":null,"locationTrigger":{"placeLabel":"supermarket","rawAddress":"5th Avenue"}}]"""))

        val result = provider.format("get eggs at the supermarket on 5th avenue")

        assertThat(result).isInstanceOf(FormattingResult.Success::class.java)
        val reminder = (result as FormattingResult.Success).reminders[0]
        assertThat(reminder.locationTrigger).isNotNull()
        assertThat(reminder.locationTrigger!!.placeLabel).isEqualTo("supermarket")
        assertThat(reminder.locationTrigger!!.rawAddress).isEqualTo("5th Avenue")
    }

    @Test
    fun `reminder with recurrence is parsed`() = runTest {
        server.enqueue(geminiResponse("""[{"title":"Take out trash","body":null,"triggerTime":null,"recurrence":"weekly","locationTrigger":null}]"""))

        val result = provider.format("remind me every week to take out trash")

        assertThat(result).isInstanceOf(FormattingResult.Success::class.java)
        val reminder = (result as FormattingResult.Success).reminders[0]
        assertThat(reminder.recurrence).isEqualTo("weekly")
    }

    @Test
    fun `malformed JSON returns Failure`() = runTest {
        server.enqueue(geminiResponse("this is not json at all"))

        val result = provider.format("buy milk")

        assertThat(result).isInstanceOf(FormattingResult.Failure::class.java)
        assertThat((result as FormattingResult.Failure).error).isNotNull()
    }

    @Test
    fun `empty response returns Failure`() = runTest {
        server.enqueue(geminiResponse(""))

        val result = provider.format("buy milk")

        assertThat(result).isInstanceOf(FormattingResult.Failure::class.java)
    }

    @Test
    fun `HTTP 429 after retries returns Failure`() = runTest {
        // Enqueue 3 rate-limit responses (exceeds max retries)
        repeat(3) {
            server.enqueue(MockResponse().setResponseCode(429).setBody("Rate limited"))
        }

        val result = provider.format("buy milk")

        assertThat(result).isInstanceOf(FormattingResult.Failure::class.java)
        assertThat((result as FormattingResult.Failure).error).contains("429")
    }

    @Test
    fun `HTTP 500 returns Failure`() = runTest {
        server.enqueue(MockResponse().setResponseCode(500).setBody("Internal server error"))

        val result = provider.format("buy milk")

        assertThat(result).isInstanceOf(FormattingResult.Failure::class.java)
        assertThat((result as FormattingResult.Failure).error).contains("500")
    }

    @Test
    fun `partial parse returns PartialSuccess`() = runTest {
        server.enqueue(geminiResponse("""[{"title":"Buy milk","body":null,"triggerTime":null,"recurrence":null,"locationTrigger":null},{"body":null,"triggerTime":null,"recurrence":null,"locationTrigger":null}]"""))

        val result = provider.format("buy milk and do something")

        assertThat(result).isInstanceOf(FormattingResult.PartialSuccess::class.java)
        val partial = result as FormattingResult.PartialSuccess
        assertThat(partial.reminders).hasSize(1)
        assertThat(partial.reminders[0].title).isEqualTo("Buy milk")
        assertThat(partial.rawFallback).isEqualTo("buy milk and do something")
    }

    @Test
    fun `blank API key returns Failure`() = runTest {
        val blankKeyProvider = GeminiFormattingProvider(
            apiClient = apiClient,
            apiKeyProvider = { "   " }
        )

        val result = blankKeyProvider.format("buy milk")

        assertThat(result).isInstanceOf(FormattingResult.Failure::class.java)
        assertThat((result as FormattingResult.Failure).error).contains("API key")
    }

    @Test
    fun `API key provider throwing returns Failure`() = runTest {
        val throwingProvider = GeminiFormattingProvider(
            apiClient = apiClient,
            apiKeyProvider = { throw IllegalStateException("No key") }
        )

        val result = throwingProvider.format("buy milk")

        assertThat(result).isInstanceOf(FormattingResult.Failure::class.java)
    }

    @Test
    fun `JSON with trailing commas is parsed`() = runTest {
        server.enqueue(geminiResponse("""[{"title":"Buy milk","body":null,"triggerTime":null,"recurrence":null,"locationTrigger":null,}]"""))

        val result = provider.format("buy milk")

        assertThat(result).isInstanceOf(FormattingResult.Success::class.java)
    }

    companion object {
        private const val TEST_API_KEY = "test-api-key-12345"

        /**
         * Builds a mock Gemini API response wrapping [jsonText] as the
         * generated content text.
         */
        private fun geminiResponse(jsonText: String): MockResponse {
            val escaped = jsonText.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
            val body = """{"candidates":[{"content":{"parts":[{"text":"$escaped"}]}}]}"""
            return MockResponse()
                .setResponseCode(200)
                .setBody(body)
                .setHeader("Content-Type", "application/json")
        }
    }
}
