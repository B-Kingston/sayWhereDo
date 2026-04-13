package com.example.reminders.formatting

import com.example.reminders.network.OpenAiCompatibleClient
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Verifies that [CloudFormattingProvider] correctly parses OpenAI-compatible
 * API responses, handles malformed JSON, code fences, empty responses,
 * rate-limit errors, and accepts blank API keys.
 */
class CloudFormattingProviderTest {

    private val server = MockWebServer()
    private lateinit var apiClient: OpenAiCompatibleClient
    private lateinit var provider: CloudFormattingProvider

    @Before
    fun setUp() {
        server.start()
        apiClient = OpenAiCompatibleClient(
            baseUrl = server.url("v1").toString().trimEnd('/'),
            defaultModel = "test-model"
        )
        provider = CloudFormattingProvider(
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
        server.enqueue(openAiResponse("""[{"title":"Buy milk","body":null,"triggerTime":"2026-04-12T15:00:00Z","recurrence":null,"locationTrigger":null}]"""))

        val result = provider.format("remind me to buy milk tomorrow at 3pm")

        assertThat(result).isInstanceOf(FormattingResult.Success::class.java)
        val reminders = (result as FormattingResult.Success).reminders
        assertThat(reminders).hasSize(1)
        assertThat(reminders[0].title).isEqualTo("Buy milk")
        assertThat(reminders[0].triggerTime).isNotNull()
    }

    @Test
    fun `code-fenced JSON is parsed correctly`() = runTest {
        server.enqueue(openAiResponse("```json\n[{\"title\":\"Buy milk\",\"body\":null,\"triggerTime\":null,\"recurrence\":null,\"locationTrigger\":null}]\n```"))

        val result = provider.format("buy milk")

        assertThat(result).isInstanceOf(FormattingResult.Success::class.java)
        val reminders = (result as FormattingResult.Success).reminders
        assertThat(reminders).hasSize(1)
        assertThat(reminders[0].title).isEqualTo("Buy milk")
    }

    @Test
    fun `single JSON object is wrapped in array`() = runTest {
        server.enqueue(openAiResponse("""{"title":"Buy milk","body":null,"triggerTime":null,"recurrence":null,"locationTrigger":null}"""))

        val result = provider.format("buy milk")

        assertThat(result).isInstanceOf(FormattingResult.Success::class.java)
        val reminders = (result as FormattingResult.Success).reminders
        assertThat(reminders).hasSize(1)
    }

    @Test
    fun `multiple reminders are parsed`() = runTest {
        server.enqueue(openAiResponse("""[{"title":"Buy milk","body":null,"triggerTime":null,"recurrence":null,"locationTrigger":null},{"title":"Call dentist","body":"Schedule appointment","triggerTime":"2026-04-17T09:00:00Z","recurrence":null,"locationTrigger":null}]"""))

        val result = provider.format("buy milk and call the dentist")

        assertThat(result).isInstanceOf(FormattingResult.Success::class.java)
        val reminders = (result as FormattingResult.Success).reminders
        assertThat(reminders).hasSize(2)
        assertThat(reminders[0].title).isEqualTo("Buy milk")
        assertThat(reminders[1].title).isEqualTo("Call dentist")
        assertThat(reminders[1].body).isEqualTo("Schedule appointment")
    }

    @Test
    fun `malformed JSON returns Failure`() = runTest {
        server.enqueue(openAiResponse("this is not json at all"))

        val result = provider.format("buy milk")

        assertThat(result).isInstanceOf(FormattingResult.Failure::class.java)
        assertThat((result as FormattingResult.Failure).error).isNotNull()
    }

    @Test
    fun `empty response returns Failure`() = runTest {
        server.enqueue(openAiResponse(""))

        val result = provider.format("buy milk")

        assertThat(result).isInstanceOf(FormattingResult.Failure::class.java)
    }

    @Test
    fun `HTTP 500 returns Failure`() = runTest {
        server.enqueue(MockResponse().setResponseCode(500).setBody("Internal server error"))

        val result = provider.format("buy milk")

        assertThat(result).isInstanceOf(FormattingResult.Failure::class.java)
        assertThat((result as FormattingResult.Failure).error).contains("500")
    }

    @Test
    fun `HTTP 429 after retries returns Failure`() = runTest {
        repeat(3) {
            server.enqueue(MockResponse().setResponseCode(429).setBody("Rate limited"))
        }

        val result = provider.format("buy milk")

        assertThat(result).isInstanceOf(FormattingResult.Failure::class.java)
        assertThat((result as FormattingResult.Failure).error).contains("429")
    }

    @Test
    fun `partial parse returns PartialSuccess`() = runTest {
        server.enqueue(openAiResponse("""[{"title":"Buy milk","body":null,"triggerTime":null,"recurrence":null,"locationTrigger":null},{"body":null,"triggerTime":null,"recurrence":null,"locationTrigger":null}]"""))

        val result = provider.format("buy milk and do something")

        assertThat(result).isInstanceOf(FormattingResult.PartialSuccess::class.java)
        val partial = result as FormattingResult.PartialSuccess
        assertThat(partial.reminders).hasSize(1)
        assertThat(partial.reminders[0].title).isEqualTo("Buy milk")
        assertThat(partial.rawFallback).isEqualTo("buy milk and do something")
    }

    @Test
    fun `empty API key is accepted`() = runTest {
        val noKeyProvider = CloudFormattingProvider(
            apiClient = apiClient,
            apiKeyProvider = { "" }
        )
        server.enqueue(openAiResponse("""[{"title":"Buy milk","body":null,"triggerTime":null,"recurrence":null,"locationTrigger":null}]"""))

        val result = noKeyProvider.format("buy milk")

        assertThat(result).isInstanceOf(FormattingResult.Success::class.java)
        val request = server.takeRequest()
        assertThat(request.getHeader("Authorization")).isNull()
    }

    companion object {
        private const val TEST_API_KEY = "test-api-key-12345"

        /**
         * Builds a mock OpenAI chat completions response wrapping [content]
         * as the assistant's message content.
         */
        private fun openAiResponse(content: String): MockResponse {
            val escaped = content.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
            val body = """{"choices":[{"message":{"content":"$escaped"}}]}"""
            return MockResponse()
                .setResponseCode(200)
                .setBody(body)
                .setHeader("Content-Type", "application/json")
        }
    }
}
