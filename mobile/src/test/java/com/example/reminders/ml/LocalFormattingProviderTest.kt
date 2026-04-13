package com.example.reminders.ml

import com.example.reminders.formatting.FormattingResult
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File

/**
 * Verifies that [LocalFormattingProvider] correctly loads models,
 * delegates to the inference wrapper, and handles errors.
 */
class LocalFormattingProviderTest {

    private val mockModelManager = mockk<LocalModelManager>()
    private val mockInference = mockk<LlmInferenceWrapper>(relaxed = true)
    private lateinit var provider: LocalFormattingProvider

    @Before
    fun setUp() {
        every { mockModelManager.getModelPath(GEMMA_MODEL.id) } returns File("/tmp/test.model")

        provider = LocalFormattingProvider(
            modelManager = mockModelManager,
            modelInfo = GEMMA_MODEL,
            inferenceFactory = { mockInference }
        )
    }

    @After
    fun tearDown() {
        provider.close()
    }

    @Test
    fun `format returns Success with valid JSON response`() = runTest {
        every { mockInference.generateResponse(any()) } returns
            """[{"title":"Buy milk","body":null,"triggerTime":null,"recurrence":null,"locationTrigger":null}]"""

        val result = provider.format("remind me to buy milk")

        assertThat(result).isInstanceOf(FormattingResult.Success::class.java)
        val reminders = (result as FormattingResult.Success).reminders
        assertThat(reminders).hasSize(1)
        assertThat(reminders[0].title).isEqualTo("Buy milk")
    }

    @Test
    fun `format returns Failure when model not downloaded`() = runTest {
        every { mockModelManager.getModelPath(GEMMA_MODEL.id) } returns null

        val result = provider.format("buy milk")

        assertThat(result).isInstanceOf(FormattingResult.Failure::class.java)
    }

    @Test
    fun `format returns Failure on inference error`() = runTest {
        every { mockInference.generateResponse(any()) } throws
            RuntimeException("Inference failed")

        val result = provider.format("buy milk")

        assertThat(result).isInstanceOf(FormattingResult.Failure::class.java)
        assertThat((result as FormattingResult.Failure).error).contains("Inference failed")
    }

    @Test
    fun `format returns Failure on malformed response`() = runTest {
        every { mockInference.generateResponse(any()) } returns "not valid json"

        val result = provider.format("buy milk")

        assertThat(result).isInstanceOf(FormattingResult.Failure::class.java)
    }

    @Test
    fun `close releases inference session`() = runTest {
        every { mockInference.generateResponse(any()) } returns
            """[{"title":"Test","body":null,"triggerTime":null,"recurrence":null,"locationTrigger":null}]"""

        provider.format("initialize session")
        provider.close()
        verify { mockInference.close() }
    }

    @Test
    fun `format includes system prompt in request`() = runTest {
        var capturedPrompt = ""
        every { mockInference.generateResponse(any()) } answers {
            capturedPrompt = args[0] as String
            """[{"title":"Test","body":null,"triggerTime":null,"recurrence":null,"locationTrigger":null}]"""
        }

        provider.format("buy milk")

        assertThat(capturedPrompt).contains("User: buy milk")
        assertThat(capturedPrompt).contains("Assistant:")
    }

    companion object {
        private val GEMMA_MODEL = ModelInfo(
            id = "gemma2-2b-q4",
            name = "Gemma 2 2B (Q4)",
            downloadUrl = "https://example.com/model.task",
            fileSizeBytes = 1_500_000_000L,
            fileSizeDisplay = "~1.4 GB",
            minRamMb = 6000L,
            isRecommended = true
        )
    }
}
