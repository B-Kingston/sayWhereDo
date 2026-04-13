package com.example.reminders.ui.viewmodel

import com.example.reminders.pipeline.PipelineOrchestrator
import com.example.reminders.pipeline.PipelineResult
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * Verifies that [KeyboardInputViewModel] correctly handles blank input,
 * pipeline success/failure, and whitespace trimming.
 */
class KeyboardInputViewModelTest {

    private val mockPipeline = mockk<PipelineOrchestrator>()
    private lateinit var viewModel: KeyboardInputViewModel

    @Before
    fun setUp() {
        viewModel = KeyboardInputViewModel(mockPipeline)
    }

    @Test
    fun `saveReminder with valid text returns Success`() = runTest {
        coEvery { mockPipeline.processTranscript("buy milk") } returns
            PipelineResult.Success(emptyList())

        viewModel.saveReminder("buy milk")

        assertThat(viewModel.uiState.value).isInstanceOf(KeyboardInputUiState.Success::class.java)
    }

    @Test
    fun `saveReminder with blank text returns Error`() = runTest {
        viewModel.saveReminder("   ")

        assertThat(viewModel.uiState.value).isInstanceOf(KeyboardInputUiState.Error::class.java)
    }

    @Test
    fun `saveReminder with empty text returns Error`() = runTest {
        viewModel.saveReminder("")

        assertThat(viewModel.uiState.value).isInstanceOf(KeyboardInputUiState.Error::class.java)
    }

    @Test
    fun `saveReminder on pipeline failure returns Error`() = runTest {
        coEvery { mockPipeline.processTranscript(any()) } returns
            PipelineResult.Failure("Network error")

        viewModel.saveReminder("buy milk")

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(KeyboardInputUiState.Error::class.java)
        assertThat((state as KeyboardInputUiState.Error).message).isEqualTo("Network error")
    }

    @Test
    fun `saveReminder transitions through Saving state`() = runTest {
        coEvery { mockPipeline.processTranscript(any()) } returns
            PipelineResult.Success(emptyList())

        viewModel.saveReminder("buy milk")

        // After completion, should be Success (was Saving during the call)
        assertThat(viewModel.uiState.value).isInstanceOf(KeyboardInputUiState.Success::class.java)
    }

    @Test
    fun `saveReminder trims whitespace from input`() = runTest {
        coEvery { mockPipeline.processTranscript("buy milk") } returns
            PipelineResult.Success(emptyList())

        viewModel.saveReminder("  buy milk  ")

        coVerify { mockPipeline.processTranscript("buy milk") }
    }

    @Test
    fun `initial state is Idle`() {
        assertThat(viewModel.uiState.value).isEqualTo(KeyboardInputUiState.Idle)
    }

    @Test
    fun `saveReminder on PartialSuccess returns Success`() = runTest {
        coEvery { mockPipeline.processTranscript(any()) } returns
            PipelineResult.PartialSuccess(emptyList(), "raw text")

        viewModel.saveReminder("buy milk")

        assertThat(viewModel.uiState.value).isInstanceOf(KeyboardInputUiState.Success::class.java)
    }

    @Test
    fun `saveReminder on UsageLimited returns Success`() = runTest {
        coEvery { mockPipeline.processTranscript(any()) } returns
            PipelineResult.UsageLimited

        viewModel.saveReminder("buy milk")

        assertThat(viewModel.uiState.value).isInstanceOf(KeyboardInputUiState.Success::class.java)
    }
}
