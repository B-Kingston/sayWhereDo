package com.example.reminders.formatting

import com.example.reminders.data.model.LocationTrigger
import com.example.reminders.data.model.ParsedReminder
import com.example.reminders.data.model.Reminder
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Verifies that [RawFallbackProvider] correctly saves the raw transcript
 * as a single unformatted [ParsedReminder].
 */
class RawFallbackProviderTest {

    private val provider = RawFallbackProvider()

    @Test
    fun `returns Success with single reminder whose title is the transcript`() =
        kotlinx.coroutines.test.runTest {
            val transcript = "remind me to buy milk tomorrow at 3pm"
            val result = provider.format(transcript)

            assertThat(result).isInstanceOf(FormattingResult.Success::class.java)
            val reminders = (result as FormattingResult.Success).reminders
            assertThat(reminders).hasSize(1)
            assertThat(reminders[0].title).isEqualTo(transcript)
        }

    @Test
    fun `reminder has null body`() = kotlinx.coroutines.test.runTest {
        val result = provider.format("some text") as FormattingResult.Success

        assertThat(reminders(result)[0].body).isNull()
    }

    @Test
    fun `reminder has null triggerTime`() = kotlinx.coroutines.test.runTest {
        val result = provider.format("some text") as FormattingResult.Success

        assertThat(reminders(result)[0].triggerTime).isNull()
    }

    @Test
    fun `reminder has null recurrence`() = kotlinx.coroutines.test.runTest {
        val result = provider.format("some text") as FormattingResult.Success

        assertThat(reminders(result)[0].recurrence).isNull()
    }

    @Test
    fun `reminder has null locationTrigger`() = kotlinx.coroutines.test.runTest {
        val result = provider.format("some text") as FormattingResult.Success

        assertThat(reminders(result)[0].locationTrigger).isNull()
    }

    @Test
    fun `preserves empty transcript`() = kotlinx.coroutines.test.runTest {
        val result = provider.format("") as FormattingResult.Success

        assertThat(reminders(result)).hasSize(1)
        assertThat(reminders(result)[0].title).isEmpty()
    }

    @Test
    fun `preserves special characters in transcript`() = kotlinx.coroutines.test.runTest {
        val transcript = "buy 3 eggs, milk & bread for the party at 100%!"
        val result = provider.format(transcript) as FormattingResult.Success

        assertThat(reminders(result)[0].title).isEqualTo(transcript)
    }

    private fun reminders(result: FormattingResult.Success): List<ParsedReminder> =
        result.reminders
}
