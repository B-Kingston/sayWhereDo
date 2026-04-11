package com.example.reminders.ui.viewmodel

import com.example.reminders.transcription.RecognitionState
import com.example.reminders.transcription.SpeechRecognitionManager
import com.example.reminders.ui.screen.TranscriptionUiState
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TranscriptionViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private val recognitionState = MutableStateFlow<RecognitionState>(RecognitionState.Idle)
    private val mockManager = mockk<SpeechRecognitionManager>(relaxed = true)

    private lateinit var viewModel: TranscriptionViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { mockManager.recognitionState } returns recognitionState
        viewModel = TranscriptionViewModel(mockManager)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is Idle`() {
        assertThat(viewModel.uiState.value).isEqualTo(TranscriptionUiState.Idle)
    }

    @Test
    fun `initial partial text is empty`() {
        assertThat(viewModel.partialText.value).isEmpty()
    }

    @Test
    fun `startListening delegates to manager`() {
        viewModel.startListening()
        verify { mockManager.startListening() }
    }

    @Test
    fun `stopListening delegates to manager`() {
        viewModel.stopListening()
        verify { mockManager.stopListening() }
    }

    @Test
    fun `Listening state from manager maps to Listening UI state`() {
        recognitionState.value = RecognitionState.Listening
        assertThat(viewModel.uiState.value).isEqualTo(TranscriptionUiState.Listening)
    }

    @Test
    fun `PartialResult updates partial text and keeps Listening state`() {
        recognitionState.value = RecognitionState.Listening
        recognitionState.value = RecognitionState.PartialResult("hello")

        assertThat(viewModel.uiState.value).isEqualTo(TranscriptionUiState.Listening)
        assertThat(viewModel.partialText.value).isEqualTo("hello")
    }

    @Test
    fun `PartialResult replaces previous partial text`() {
        recognitionState.value = RecognitionState.PartialResult("hello")
        recognitionState.value = RecognitionState.PartialResult("hello world")

        assertThat(viewModel.partialText.value).isEqualTo("hello world")
    }

    @Test
    fun `Processing state from manager maps to Processing UI state`() {
        recognitionState.value = RecognitionState.Processing
        assertThat(viewModel.uiState.value).isEqualTo(TranscriptionUiState.Processing)
    }

    @Test
    fun `Processing clears partial text`() {
        recognitionState.value = RecognitionState.PartialResult("hello")
        recognitionState.value = RecognitionState.Processing
        assertThat(viewModel.partialText.value).isEmpty()
    }

    @Test
    fun `FinalResult maps to Result UI state`() {
        recognitionState.value = RecognitionState.FinalResult("buy milk tomorrow")
        assertThat(viewModel.uiState.value).isEqualTo(TranscriptionUiState.Result("buy milk tomorrow"))
    }

    @Test
    fun `FinalResult clears partial text`() {
        recognitionState.value = RecognitionState.PartialResult("buy")
        recognitionState.value = RecognitionState.FinalResult("buy milk tomorrow")
        assertThat(viewModel.partialText.value).isEmpty()
    }

    @Test
    fun `Error from manager maps to Error UI state`() {
        recognitionState.value = RecognitionState.Error(7, "No speech detected")
        assertThat(viewModel.uiState.value).isEqualTo(TranscriptionUiState.Error("No speech detected"))
    }

    @Test
    fun `Error clears partial text`() {
        recognitionState.value = RecognitionState.PartialResult("hel")
        recognitionState.value = RecognitionState.Error(7, "No speech detected")
        assertThat(viewModel.partialText.value).isEmpty()
    }

    @Test
    fun `reset returns to Idle`() {
        recognitionState.value = RecognitionState.FinalResult("test")
        viewModel.reset()
        assertThat(viewModel.uiState.value).isEqualTo(TranscriptionUiState.Idle)
    }

    @Test
    fun `reset clears partial text`() {
        recognitionState.value = RecognitionState.PartialResult("hello")
        viewModel.reset()
        assertThat(viewModel.partialText.value).isEmpty()
    }

    @Test
    fun `reset after error allows new recording`() {
        recognitionState.value = RecognitionState.Error(7, "No speech detected")
        viewModel.reset()
        assertThat(viewModel.uiState.value).isEqualTo(TranscriptionUiState.Idle)

        // Simulate starting a new session
        recognitionState.value = RecognitionState.Listening
        assertThat(viewModel.uiState.value).isEqualTo(TranscriptionUiState.Listening)
    }

    @Test
    fun `full happy path transitions`() {
        // Start listening
        recognitionState.value = RecognitionState.Listening
        assertThat(viewModel.uiState.value).isEqualTo(TranscriptionUiState.Listening)

        // Partial result
        recognitionState.value = RecognitionState.PartialResult("remind me to")
        assertThat(viewModel.uiState.value).isEqualTo(TranscriptionUiState.Listening)
        assertThat(viewModel.partialText.value).isEqualTo("remind me to")

        // More partial results (replace, not append)
        recognitionState.value = RecognitionState.PartialResult("remind me to buy milk")
        assertThat(viewModel.partialText.value).isEqualTo("remind me to buy milk")

        // Processing
        recognitionState.value = RecognitionState.Processing
        assertThat(viewModel.uiState.value).isEqualTo(TranscriptionUiState.Processing)

        // Final result
        recognitionState.value = RecognitionState.FinalResult("remind me to buy milk")
        assertThat(viewModel.uiState.value).isEqualTo(TranscriptionUiState.Result("remind me to buy milk"))
        assertThat(viewModel.partialText.value).isEmpty()
    }
}
