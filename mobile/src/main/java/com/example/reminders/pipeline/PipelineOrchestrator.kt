package com.example.reminders.pipeline

import android.util.Log
import com.example.reminders.billing.BillingManager
import com.example.reminders.data.model.ParsedReminder
import com.example.reminders.data.preferences.UsageTracker
import com.example.reminders.data.preferences.UserPreferences
import com.example.reminders.data.repository.ReminderRepository
import com.example.reminders.formatting.FormattingProvider
import com.example.reminders.formatting.FormattingResult
import com.example.reminders.formatting.RawFallbackProvider
import com.example.reminders.formatting.ReminderMapper
import kotlinx.coroutines.flow.first

/**
 * Orchestrates the full voice-transcript-to-storage pipeline.
 *
 * Chains the following stages:
 * 1. **Usage gating** — checks [UsageTracker.isFormattingAllowed] before
 *    calling the cloud formatting provider.
 * 2. **Provider selection** — uses the cloud [FormattingProvider] when an
 *    API key is configured; falls back to [RawFallbackProvider] otherwise.
 * 3. **Room persistence** — maps parsed reminders to entities and inserts
 *    them via [ReminderRepository].
 *
 * Raw fallback: the user's original transcript is *always* preserved as a
 * reminder, even when formatting fails or is rate-limited.
 *
 * @param formattingProvider The cloud-based provider (e.g. Gemini).
 * @param rawFallbackProvider Saves raw transcript when formatting is unavailable.
 * @param reminderRepository Persists [com.example.reminders.data.model.Reminder] entities.
 * @param usageTracker       Tracks daily formatting usage for free-tier limits.
 * @param billingManager     Exposes the user's Pro purchase status.
 * @param userPreferences    Provides the stored API key and provider preference.
 */
class PipelineOrchestrator(
    private val formattingProvider: FormattingProvider,
    private val rawFallbackProvider: RawFallbackProvider,
    private val reminderRepository: ReminderRepository,
    private val usageTracker: UsageTracker,
    private val billingManager: BillingManager,
    private val userPreferences: UserPreferences
) {

    /**
     * Processes a voice [transcript] through the formatting pipeline and
     * persists the resulting reminders.
     *
     * @param transcript The raw text from the speech recognition engine.
     * @return A [PipelineResult] indicating the outcome.
     */
    suspend fun processTranscript(transcript: String): PipelineResult {
        Log.i(TAG, "Pipeline started for transcript: ${transcript.take(80)}")
        val isPro = billingManager.isPro.value
        val hasApiKey = userPreferences.apiKey.first() != null

        if (!usageTracker.isFormattingAllowed(isPro, hasApiKey)) {
            Log.w(TAG, "Usage limited — saving raw fallback (isPro=$isPro, hasApiKey=$hasApiKey)")
            return saveRawFallbackAndReturn(transcript, PipelineResult.UsageLimited)
        }

        if (!hasApiKey) {
            Log.i(TAG, "No API key configured — saving raw fallback")
            return saveRawFallbackAndReturn(transcript) { reminders ->
                PipelineResult.Success(reminders)
            }
        }

        Log.d(TAG, "Calling cloud formatting provider")
        return when (val result = formattingProvider.format(transcript)) {
            is FormattingResult.Success -> {
                Log.i(TAG, "Cloud formatting succeeded: ${result.reminders.size} reminder(s)")
                usageTracker.incrementFormattingCount()
                val reminders = ReminderMapper.mapToReminders(
                    result.reminders, transcript, FORMATTING_PROVIDER_CLOUD
                )
                reminders.forEach { reminderRepository.insert(it) }
                PipelineResult.Success(reminders)
            }

            is FormattingResult.PartialSuccess -> {
                Log.w(TAG, "Cloud formatting partial success: ${result.reminders.size} valid, raw fallback kept")
                usageTracker.incrementFormattingCount()
                val validReminders = ReminderMapper.mapToReminders(
                    result.reminders, transcript, FORMATTING_PROVIDER_CLOUD
                )
                val rawReminder = ReminderMapper.mapToReminders(
                    listOf(ParsedReminder(title = result.rawFallback)),
                    transcript, FORMATTING_PROVIDER_NONE
                )
                (validReminders + rawReminder).forEach { reminderRepository.insert(it) }
                PipelineResult.PartialSuccess(validReminders, result.rawFallback)
            }

            is FormattingResult.Failure -> {
                Log.e(TAG, "Cloud formatting failed: ${result.error}")
                saveRawFallbackAndReturn(transcript) {
                    PipelineResult.Failure(result.error)
                }
            }

            is FormattingResult.UsageLimited -> {
                Log.w(TAG, "Cloud formatting returned usage limited")
                saveRawFallbackAndReturn(transcript, PipelineResult.UsageLimited)
            }
        }
    }

    /**
     * Saves the raw transcript as an unformatted reminder and returns [result].
     *
     * Ensures the user's speech is never discarded, even on errors.
     */
    private suspend fun saveRawFallbackAndReturn(
        transcript: String,
        result: PipelineResult
    ): PipelineResult {
        val rawReminders = rawFallbackProvider.format(transcript)
        val reminders = ReminderMapper.mapToReminders(
            (rawReminders as FormattingResult.Success).reminders,
            transcript, FORMATTING_PROVIDER_NONE
        )
        reminders.forEach { reminderRepository.insert(it) }
        return result
    }

    /**
     * Overload that accepts a lambda to construct the result after saving.
     */
    private suspend fun saveRawFallbackAndReturn(
        transcript: String,
        resultProvider: (List<com.example.reminders.data.model.Reminder>) -> PipelineResult
    ): PipelineResult {
        val rawReminders = rawFallbackProvider.format(transcript)
        val reminders = ReminderMapper.mapToReminders(
            (rawReminders as FormattingResult.Success).reminders,
            transcript, FORMATTING_PROVIDER_NONE
        )
        reminders.forEach { reminderRepository.insert(it) }
        return resultProvider(reminders)
    }

    companion object {
        private const val TAG = "PipelineOrchestrator"
        private const val FORMATTING_PROVIDER_CLOUD = "cloud"
        private const val FORMATTING_PROVIDER_NONE = "none"
    }
}
