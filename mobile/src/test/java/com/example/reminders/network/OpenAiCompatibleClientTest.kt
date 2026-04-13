package com.example.reminders.network

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Verifies that [OpenAiCompatibleClient] correctly constructs requests,
 * handles authentication, parses responses, retries on 429, and reports
 * errors for malformed or failed responses.
 */
class OpenAiCompatibleClientTest {

    private val server = MockWebServer()
    private lateinit var client: OpenAiCompatibleClient

    @Before
    fun setUp() {
        server.start()
        client = OpenAiCompatibleClient(
            baseUrl = server.url("v1").toString().trimEnd('/'),
            defaultModel = "test-model"
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `valid response returns content`() = runTest {
        server.enqueue(openAiResponse("result text"))

        val result = client.chatCompletion("key-123", "system prompt", "user message")

        assertThat(result).isEqualTo("result text")
    }

    @Test
    fun `request has correct format`() = runTest {
        server.enqueue(openAiResponse("ok"))

        client.chatCompletion("key-123", "system prompt", "user message")

        val request = server.takeRequest()
        val body = request.body.readUtf8()

        assertThat(request.path).isEqualTo("/v1/chat/completions")
        assertThat(body).contains("\"model\":\"test-model\"")
        assertThat(body).contains("\"system\"")
        assertThat(body).contains("\"system prompt\"")
        assertThat(body).contains("\"user\"")
        assertThat(body).contains("\"user message\"")
        assertThat(body).contains("\"temperature\":0.7")
    }

    @Test
    fun `authorization header present when key provided`() = runTest {
        server.enqueue(openAiResponse("ok"))

        client.chatCompletion("my-secret-key", "system", "user")

        val request = server.takeRequest()
        assertThat(request.getHeader("Authorization")).isEqualTo("Bearer my-secret-key")
    }

    @Test
    fun `no authorization header when key is blank`() = runTest {
        server.enqueue(openAiResponse("ok"))

        client.chatCompletion("", "system", "user")

        val request = server.takeRequest()
        assertThat(request.getHeader("Authorization")).isNull()
    }

    @Test
    fun `429 retry succeeds`() = runTest {
        server.enqueue(MockResponse().setResponseCode(429).setBody("Rate limited"))
        server.enqueue(openAiResponse("retried result"))

        val result = client.chatCompletion("key", "system", "user")

        assertThat(result).isEqualTo("retried result")
        assertThat(server.requestCount).isEqualTo(2)
    }

    @Test
    fun `429 max retries exceeded throws exception`() = runTest {
        repeat(3) {
            server.enqueue(MockResponse().setResponseCode(429).setBody("Rate limited"))
        }

        val exception = runCatching {
            client.chatCompletion("key", "system", "user")
        }.exceptionOrNull()

        assertThat(exception).isInstanceOf(OpenAiCompatibleException::class.java)
        assertThat(exception!!.message).contains("429")
    }

    @Test
    fun `500 error throws exception`() = runTest {
        server.enqueue(MockResponse().setResponseCode(500).setBody("Internal server error"))

        val exception = runCatching {
            client.chatCompletion("key", "system", "user")
        }.exceptionOrNull()

        assertThat(exception).isInstanceOf(OpenAiCompatibleException::class.java)
        assertThat(exception!!.message).contains("500")
    }

    @Test
    fun `empty choices throws exception`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"choices":[]}""")
                .setHeader("Content-Type", "application/json")
        )

        val exception = runCatching {
            client.chatCompletion("key", "system", "user")
        }.exceptionOrNull()

        assertThat(exception).isInstanceOf(OpenAiCompatibleException::class.java)
        assertThat(exception!!.message).contains("Empty")
    }

    @Test
    fun `custom model override appears in request`() = runTest {
        server.enqueue(openAiResponse("ok"))

        client.chatCompletion("key", "system", "user", model = "custom-llama-3")

        val request = server.takeRequest()
        val body = request.body.readUtf8()
        assertThat(body).contains("\"model\":\"custom-llama-3\"")
    }

    @Test
    fun `temperature override appears in request`() = runTest {
        server.enqueue(openAiResponse("ok"))

        client.chatCompletion("key", "system", "user", temperature = 0.3f)

        val request = server.takeRequest()
        val body = request.body.readUtf8()
        assertThat(body).contains("\"temperature\":0.3")
    }

    companion object {
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
