package com.example.reminders.formatting

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Verifies that [FormattingPrompt] produces a well-structured system
 * prompt containing the expected schema, examples, and current date.
 */
class FormattingPromptTest {

    @Test
    fun `prompt contains JSON schema fields`() {
        val prompt = FormattingPrompt.build()

        assertThat(prompt).contains("title")
        assertThat(prompt).contains("body")
        assertThat(prompt).contains("triggerTime")
        assertThat(prompt).contains("recurrence")
        assertThat(prompt).contains("locationTrigger")
        assertThat(prompt).contains("placeLabel")
        assertThat(prompt).contains("rawAddress")
    }

    @Test
    fun `prompt contains JSON array instruction`() {
        val prompt = FormattingPrompt.build()

        assertThat(prompt).contains("JSON array")
        assertThat(prompt).contains("[")
        assertThat(prompt).contains("]")
    }

    @Test
    fun `prompt contains ISO 8601 reference`() {
        val prompt = FormattingPrompt.build()

        assertThat(prompt).contains("ISO 8601")
    }

    @Test
    fun `prompt contains recurrence options`() {
        val prompt = FormattingPrompt.build()

        assertThat(prompt).contains("daily")
        assertThat(prompt).contains("weekly")
        assertThat(prompt).contains("monthly")
    }

    @Test
    fun `prompt contains examples`() {
        val prompt = FormattingPrompt.build()

        assertThat(prompt).contains("Examples")
        assertThat(prompt).contains("Buy milk")
        assertThat(prompt).contains("Water the plants")
        assertThat(prompt).contains("Take out the trash")
    }

    @Test
    fun `prompt includes the provided current date`() {
        val testDate = java.time.LocalDate.of(2026, 6, 15)
        val prompt = FormattingPrompt.build(testDate)

        assertThat(prompt).contains("2026-06-15")
    }

    @Test
    fun `prompt instructs to return only JSON`() {
        val prompt = FormattingPrompt.build()

        assertThat(prompt).contains("ONLY the JSON array")
    }
}
