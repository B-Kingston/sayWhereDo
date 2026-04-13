package com.example.reminders.pipeline

import com.example.reminders.billing.BillingManager
import com.example.reminders.data.model.ParsedReminder
import com.example.reminders.data.model.Reminder
import com.example.reminders.data.preferences.UsageTracker
import com.example.reminders.data.preferences.UserPreferences
import com.example.reminders.data.repository.ReminderRepository
import com.example.reminders.formatting.FormattingProvider
import com.example.reminders.formatting.FormattingResult
import com.example.reminders.formatting.RawFallbackProvider
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * Verifies that [PipelineOrchestrator] correctly chains usage gating,
 * formatting, fallback handling, and Room storage.
 */
class PipelineOrchestratorTest {

    private val mockFormattingProvider = mockk<FormattingProvider>()
    private val rawFallbackProvider = RawFallbackProvider()
    private val reminderRepository = mockk<ReminderRepository>(relaxed = true)
    private val usageTracker = mockk<UsageTracker>(relaxed = true)
    private val billingManager = mockk<BillingManager>()
    private val userPreferences = mockk<UserPreferences>()

    private lateinit var orchestrator: PipelineOrchestrator

    @Before
    fun setUp() {
        orchestrator = PipelineOrchestrator(
            formattingProviderFactory = { mockFormattingProvider },
            rawFallbackProvider = rawFallbackProvider,
            reminderRepository = reminderRepository,
            usageTracker = usageTracker,
            billingManager = billingManager,
            userPreferences = userPreferences
        )
    }

    @Test
    fun `successful formatting inserts reminders and returns Success`() = runTest {
        every { billingManager.isPro } returns MutableStateFlow(false)
        every { userPreferences.apiKey } returns flowOf("test-key")
        coEvery { usageTracker.isFormattingAllowed(false, true) } returns true

        val parsedReminders = listOf(
            ParsedReminder(title = "Buy milk", triggerTime = null)
        )
        coEvery { mockFormattingProvider.format("buy milk") } returns
            FormattingResult.Success(parsedReminders)

        val result = orchestrator.processTranscript("buy milk")

        assertThat(result).isInstanceOf(PipelineResult.Success::class.java)
        val successResult = result as PipelineResult.Success
        assertThat(successResult.reminders).hasSize(1)
        assertThat(successResult.reminders[0].title).isEqualTo("Buy milk")
        assertThat(successResult.reminders[0].formattingProvider).isEqualTo("cloud")
        assertThat(successResult.reminders[0].sourceTranscript).isEqualTo("buy milk")

        coVerify { usageTracker.incrementFormattingCount() }
        coVerify { reminderRepository.insert(any()) }
    }

    @Test
    fun `formatting failure saves raw fallback and returns Failure`() = runTest {
        every { billingManager.isPro } returns MutableStateFlow(false)
        every { userPreferences.apiKey } returns flowOf("test-key")
        coEvery { usageTracker.isFormattingAllowed(false, true) } returns true
        coEvery { mockFormattingProvider.format("buy milk") } returns
            FormattingResult.Failure("Network error")

        val result = orchestrator.processTranscript("buy milk")

        assertThat(result).isInstanceOf(PipelineResult.Failure::class.java)
        assertThat((result as PipelineResult.Failure).error).isEqualTo("Network error")
        coVerify { reminderRepository.insert(match { it.title == "buy milk" && it.formattingProvider == "none" }) }
    }

    @Test
    fun `partial success saves valid reminders and raw fallback`() = runTest {
        every { billingManager.isPro } returns MutableStateFlow(false)
        every { userPreferences.apiKey } returns flowOf("test-key")
        coEvery { usageTracker.isFormattingAllowed(false, true) } returns true

        coEvery { mockFormattingProvider.format("buy milk and something") } returns
            FormattingResult.PartialSuccess(
                reminders = listOf(ParsedReminder(title = "Buy milk")),
                rawFallback = "buy milk and something"
            )

        val result = orchestrator.processTranscript("buy milk and something")

        assertThat(result).isInstanceOf(PipelineResult.PartialSuccess::class.java)
        val partial = result as PipelineResult.PartialSuccess
        assertThat(partial.reminders).hasSize(1)
        assertThat(partial.reminders[0].title).isEqualTo("Buy milk")
        assertThat(partial.rawFallback).isEqualTo("buy milk and something")

        // Should have saved 2 reminders: the valid one + raw fallback
        coVerify(exactly = 2) { reminderRepository.insert(any()) }
        coVerify { usageTracker.incrementFormattingCount() }
    }

    @Test
    fun `usage limited saves raw fallback and returns UsageLimited`() = runTest {
        every { billingManager.isPro } returns MutableStateFlow(false)
        every { userPreferences.apiKey } returns flowOf("test-key")
        coEvery { usageTracker.isFormattingAllowed(false, true) } returns false

        val result = orchestrator.processTranscript("buy milk")

        assertThat(result).isInstanceOf(PipelineResult.UsageLimited::class.java)
        coVerify { reminderRepository.insert(match { it.title == "buy milk" && it.formattingProvider == "none" }) }
    }

    @Test
    fun `no API key saves raw fallback and returns Success`() = runTest {
        every { billingManager.isPro } returns MutableStateFlow(false)
        every { userPreferences.apiKey } returns flowOf(null)
        coEvery { usageTracker.isFormattingAllowed(false, false) } returns true

        val result = orchestrator.processTranscript("buy milk")

        assertThat(result).isInstanceOf(PipelineResult.Success::class.java)
        val successResult = result as PipelineResult.Success
        assertThat(successResult.reminders).hasSize(1)
        assertThat(successResult.reminders[0].title).isEqualTo("buy milk")
        assertThat(successResult.reminders[0].formattingProvider).isEqualTo("none")
    }

    @Test
    fun `Pro user with API key is always allowed`() = runTest {
        every { billingManager.isPro } returns MutableStateFlow(true)
        every { userPreferences.apiKey } returns flowOf("test-key")
        coEvery { usageTracker.isFormattingAllowed(true, true) } returns true

        coEvery { mockFormattingProvider.format("buy milk") } returns
            FormattingResult.Success(listOf(ParsedReminder(title = "Buy milk")))

        val result = orchestrator.processTranscript("buy milk")

        assertThat(result).isInstanceOf(PipelineResult.Success::class.java)
        coVerify { usageTracker.incrementFormattingCount() }
    }

    @Test
    fun `multiple reminders from formatting are all inserted`() = runTest {
        every { billingManager.isPro } returns MutableStateFlow(false)
        every { userPreferences.apiKey } returns flowOf("test-key")
        coEvery { usageTracker.isFormattingAllowed(false, true) } returns true

        val parsedReminders = listOf(
            ParsedReminder(title = "Buy milk"),
            ParsedReminder(title = "Call dentist")
        )
        coEvery { mockFormattingProvider.format("buy milk and call dentist") } returns
            FormattingResult.Success(parsedReminders)

        val result = orchestrator.processTranscript("buy milk and call dentist")

        assertThat(result).isInstanceOf(PipelineResult.Success::class.java)
        assertThat((result as PipelineResult.Success).reminders).hasSize(2)
        coVerify(exactly = 2) { reminderRepository.insert(any()) }
    }

    @Test
    fun `generated reminders have unique IDs`() = runTest {
        every { billingManager.isPro } returns MutableStateFlow(false)
        every { userPreferences.apiKey } returns flowOf("test-key")
        coEvery { usageTracker.isFormattingAllowed(false, true) } returns true

        val parsedReminders = listOf(
            ParsedReminder(title = "Buy milk"),
            ParsedReminder(title = "Call dentist")
        )
        coEvery { mockFormattingProvider.format("buy milk and call dentist") } returns
            FormattingResult.Success(parsedReminders)

        val result = orchestrator.processTranscript("buy milk and call dentist") as PipelineResult.Success
        val ids = result.reminders.map { it.id }

        assertThat(ids.toSet()).hasSize(ids.size)
    }
}
