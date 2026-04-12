package com.example.reminders.formatting

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Test

/**
 * Comprehensive tests for [FormattingResponseParser].
 *
 * Covers: valid JSON arrays, code fences, bare objects, trailing commas,
 * partial parses, empty responses, location triggers, recurrence fields,
 * missing required fields, null optional fields, and edge cases.
 */
class FormattingResponseParserTest {

    // ── Valid inputs ──────────────────────────────────────────────────

    @Test
    fun `valid JSON array returns Success with reminders`() {
        val json = """[{"title":"Buy milk","body":null,"triggerTime":null,"recurrence":null,"locationTrigger":null}]"""

        val result = FormattingResponseParser.parse(json, "remind me to buy milk")

        assertThat(result).isInstanceOf(FormattingResult.Success::class.java)
        val reminders = (result as FormattingResult.Success).reminders
        assertThat(reminders).hasSize(1)
        assertThat(reminders[0].title).isEqualTo("Buy milk")
    }

    @Test
    fun `multiple reminders are parsed`() {
        val json = """[
            {"title":"Buy milk","body":null,"triggerTime":null,"recurrence":null,"locationTrigger":null},
            {"title":"Call dentist","body":"Schedule appointment","triggerTime":"2026-04-17T09:00:00Z","recurrence":null,"locationTrigger":null}
        ]"""

        val result = FormattingResponseParser.parse(json, "buy milk and call dentist")

        assertThat(result).isInstanceOf(FormattingResult.Success::class.java)
        val reminders = (result as FormattingResult.Success).reminders
        assertThat(reminders).hasSize(2)
        assertThat(reminders[0].title).isEqualTo("Buy milk")
        assertThat(reminders[1].title).isEqualTo("Call dentist")
        assertThat(reminders[1].body).isEqualTo("Schedule appointment")
        assertThat(reminders[1].triggerTime).isNotNull()
    }

    @Test
    fun `reminder with location trigger is parsed`() {
        val json = """[{"title":"Get eggs","body":null,"triggerTime":null,"recurrence":null,"locationTrigger":{"placeLabel":"supermarket","rawAddress":"5th Avenue"}}]"""

        val result = FormattingResponseParser.parse(json, "get eggs at the supermarket")

        val reminder = (result as FormattingResult.Success).reminders[0]
        assertThat(reminder.locationTrigger).isNotNull()
        assertThat(reminder.locationTrigger!!.placeLabel).isEqualTo("supermarket")
        assertThat(reminder.locationTrigger!!.rawAddress).isEqualTo("5th Avenue")
    }

    @Test
    fun `reminder with recurrence is parsed`() {
        val json = """[{"title":"Take out trash","body":null,"triggerTime":null,"recurrence":"weekly","locationTrigger":null}]"""

        val result = FormattingResponseParser.parse(json, "remind me weekly to take out trash")

        val reminder = (result as FormattingResult.Success).reminders[0]
        assertThat(reminder.recurrence).isEqualTo("weekly")
    }

    @Test
    fun `reminder with trigger time is parsed`() {
        val json = """[{"title":"Meeting","body":null,"triggerTime":"2026-04-12T15:00:00Z","recurrence":null,"locationTrigger":null}]"""

        val result = FormattingResponseParser.parse(json, "meeting at 3pm")

        val reminder = (result as FormattingResult.Success).reminders[0]
        assertThat(reminder.triggerTime).isNotNull()
        assertThat(reminder.triggerTime.toString()).contains("2026-04-12")
    }

    @Test
    fun `reminder with body text is parsed`() {
        val json = """[{"title":"Doctor appointment","body":"Remember to bring insurance card","triggerTime":null,"recurrence":null,"locationTrigger":null}]"""

        val result = FormattingResponseParser.parse(json, "doctor appointment bring insurance")

        val reminder = (result as FormattingResult.Success).reminders[0]
        assertThat(reminder.body).isEqualTo("Remember to bring insurance card")
    }

    // ── Code fences ───────────────────────────────────────────────────

    @Test
    fun `code-fenced JSON is parsed correctly`() {
        val json = "```json\n[{\"title\":\"Buy milk\",\"body\":null,\"triggerTime\":null,\"recurrence\":null,\"locationTrigger\":null}]\n```"

        val result = FormattingResponseParser.parse(json, "buy milk")

        assertThat(result).isInstanceOf(FormattingResult.Success::class.java)
        assertThat((result as FormattingResult.Success).reminders[0].title).isEqualTo("Buy milk")
    }

    @Test
    fun `code fence without language tag is parsed correctly`() {
        val json = "```\n[{\"title\":\"Buy milk\",\"body\":null,\"triggerTime\":null,\"recurrence\":null,\"locationTrigger\":null}]\n```"

        val result = FormattingResponseParser.parse(json, "buy milk")

        assertThat(result).isInstanceOf(FormattingResult.Success::class.java)
        assertThat((result as FormattingResult.Success).reminders[0].title).isEqualTo("Buy milk")
    }

    // ── Bare JSON objects ─────────────────────────────────────────────

    @Test
    fun `single JSON object is wrapped in array`() {
        val json = """{"title":"Buy milk","body":null,"triggerTime":null,"recurrence":null,"locationTrigger":null}"""

        val result = FormattingResponseParser.parse(json, "buy milk")

        assertThat(result).isInstanceOf(FormattingResult.Success::class.java)
        assertThat((result as FormattingResult.Success).reminders).hasSize(1)
        assertThat((result as FormattingResult.Success).reminders[0].title).isEqualTo("Buy milk")
    }

    @Test
    fun `bare object with code fence is parsed`() {
        val json = "```json\n{\"title\":\"Buy milk\",\"body\":null,\"triggerTime\":null,\"recurrence\":null,\"locationTrigger\":null}\n```"

        val result = FormattingResponseParser.parse(json, "buy milk")

        assertThat(result).isInstanceOf(FormattingResult.Success::class.java)
        assertThat((result as FormattingResult.Success).reminders[0].title).isEqualTo("Buy milk")
    }

    // ── Trailing commas ───────────────────────────────────────────────

    @Test
    fun `JSON with trailing comma before array close is parsed`() {
        val json = """[{"title":"Buy milk","body":null,"triggerTime":null,"recurrence":null,"locationTrigger":null,}]"""

        val result = FormattingResponseParser.parse(json, "buy milk")

        assertThat(result).isInstanceOf(FormattingResult.Success::class.java)
        assertThat((result as FormattingResult.Success).reminders[0].title).isEqualTo("Buy milk")
    }

    @Test
    fun `JSON with trailing comma before object close is parsed`() {
        val json = """{"title":"Buy milk","body":null,"triggerTime":null,"recurrence":null,"locationTrigger":null,}"""

        val result = FormattingResponseParser.parse(json, "buy milk")

        assertThat(result).isInstanceOf(FormattingResult.Success::class.java)
        assertThat((result as FormattingResult.Success).reminders[0].title).isEqualTo("Buy milk")
    }

    @Test
    fun `JSON with trailing comma and whitespace is parsed`() {
        val json = """[{"title":"Buy milk", }]"""

        val result = FormattingResponseParser.parse(json, "buy milk")

        assertThat(result).isInstanceOf(FormattingResult.Success::class.java)
    }

    // ── Partial parse ─────────────────────────────────────────────────

    @Test
    fun `partial parse returns PartialSuccess`() {
        val json = """[{"title":"Buy milk","body":null,"triggerTime":null,"recurrence":null,"locationTrigger":null},{"body":null,"triggerTime":null,"recurrence":null,"locationTrigger":null}]"""

        val result = FormattingResponseParser.parse(json, "buy milk and do something")

        assertThat(result).isInstanceOf(FormattingResult.PartialSuccess::class.java)
        val partial = result as FormattingResult.PartialSuccess
        assertThat(partial.reminders).hasSize(1)
        assertThat(partial.reminders[0].title).isEqualTo("Buy milk")
        assertThat(partial.rawFallback).isEqualTo("buy milk and do something")
    }

    @Test
    fun `all elements failing returns Failure`() {
        val json = """[{"body":null,"triggerTime":null},{"triggerTime":null}]"""

        val result = FormattingResponseParser.parse(json, "something")

        assertThat(result).isInstanceOf(FormattingResult.Failure::class.java)
    }

    // ── Error cases ───────────────────────────────────────────────────

    @Test
    fun `malformed JSON returns Failure`() {
        val result = FormattingResponseParser.parse("this is not json at all", "buy milk")

        assertThat(result).isInstanceOf(FormattingResult.Failure::class.java)
        assertThat((result as FormattingResult.Failure).error).contains("parse")
    }

    @Test
    fun `empty string returns Failure`() {
        val result = FormattingResponseParser.parse("", "buy milk")

        assertThat(result).isInstanceOf(FormattingResult.Failure::class.java)
    }

    @Test
    fun `whitespace only returns Failure`() {
        val result = FormattingResponseParser.parse("   \n\t  ", "buy milk")

        assertThat(result).isInstanceOf(FormattingResult.Failure::class.java)
    }

    @Test
    fun `empty JSON array returns Failure`() {
        val result = FormattingResponseParser.parse("[]", "buy milk")

        assertThat(result).isInstanceOf(FormattingResult.Failure::class.java)
        assertThat((result as FormattingResult.Failure).error).contains("No reminders")
    }

    @Test
    fun `JSON array with only null elements returns Failure`() {
        val result = FormattingResponseParser.parse("[null, null]", "buy milk")

        assertThat(result).isInstanceOf(FormattingResult.Failure::class.java)
    }

    // ── cleanJsonText ─────────────────────────────────────────────────

    @Test
    fun `cleanJsonText strips json code fence`() {
        val input = "```json\n[{\"title\":\"test\"}]\n```"
        val cleaned = FormattingResponseParser.cleanJsonText(input)
        assertThat(cleaned).isEqualTo("""[{"title":"test"}]""")
    }

    @Test
    fun `cleanJsonText strips plain code fence`() {
        val input = "```\n[{\"title\":\"test\"}]\n```"
        val cleaned = FormattingResponseParser.cleanJsonText(input)
        assertThat(cleaned).isEqualTo("""[{"title":"test"}]""")
    }

    @Test
    fun `cleanJsonText wraps bare object`() {
        val input = """{"title":"test"}"""
        val cleaned = FormattingResponseParser.cleanJsonText(input)
        assertThat(cleaned).isEqualTo("""[{"title":"test"}]""")
    }

    @Test
    fun `cleanJsonText does not double-wrap array`() {
        val input = """[{"title":"test"}]"""
        val cleaned = FormattingResponseParser.cleanJsonText(input)
        assertThat(cleaned).isEqualTo("""[{"title":"test"}]""")
    }

    @Test
    fun `cleanJsonText removes trailing comma before bracket`() {
        val input = """[{"title":"test"},]"""
        val cleaned = FormattingResponseParser.cleanJsonText(input)
        assertThat(cleaned).isEqualTo("""[{"title":"test"}]""")
    }

    @Test
    fun `cleanJsonText removes trailing comma before brace`() {
        val input = """{"title":"test",}"""
        val cleaned = FormattingResponseParser.cleanJsonText(input)
        assertThat(cleaned).isEqualTo("""[{"title":"test"}]""")  // Also wraps in array
    }

    @Test
    fun `cleanJsonText handles combined code fence and bare object`() {
        val input = "```json\n{\"title\":\"test\"}\n```"
        val cleaned = FormattingResponseParser.cleanJsonText(input)
        assertThat(cleaned).isEqualTo("""[{"title":"test"}]""")
    }

    @Test
    fun `cleanJsonText preserves already valid JSON`() {
        val input = """[{"title":"Buy milk","body":null}]"""
        val cleaned = FormattingResponseParser.cleanJsonText(input)
        assertThat(cleaned).isEqualTo(input)
    }

    // ── parseSingleReminder ───────────────────────────────────────────

    @Test
    fun `parseSingleReminder with minimal required fields`() {
        val json = Json.parseToJsonElement(
            """{"title":"Test","body":null,"triggerTime":null,"recurrence":null,"locationTrigger":null}"""
        ).jsonObject

        val reminder = FormattingResponseParser.parseSingleReminder(json)
        assertThat(reminder.title).isEqualTo("Test")
        assertThat(reminder.body).isNull()
        assertThat(reminder.triggerTime).isNull()
        assertThat(reminder.recurrence).isNull()
        assertThat(reminder.locationTrigger).isNull()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `parseSingleReminder throws on missing title`() {
        val json = Json.parseToJsonElement(
            """{"body":null,"triggerTime":null,"recurrence":null,"locationTrigger":null}"""
        ).jsonObject

        FormattingResponseParser.parseSingleReminder(json)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `parseSingleReminder throws on null title`() {
        val json = Json.parseToJsonElement(
            """{"title":null,"body":null,"triggerTime":null,"recurrence":null,"locationTrigger":null}"""
        ).jsonObject

        FormattingResponseParser.parseSingleReminder(json)
    }

    @Test
    fun `parseSingleReminder ignores unknown fields`() {
        val json = Json.parseToJsonElement(
            """{"title":"Test","body":null,"triggerTime":null,"recurrence":null,"locationTrigger":null,"unknownField":"ignored"}"""
        ).jsonObject

        val reminder = FormattingResponseParser.parseSingleReminder(json)
        assertThat(reminder.title).isEqualTo("Test")
    }

    @Test
    fun `parseSingleReminder with location trigger without rawAddress`() {
        val json = Json.parseToJsonElement(
            """{"title":"Test","body":null,"triggerTime":null,"recurrence":null,"locationTrigger":{"placeLabel":"Home"}}"""
        ).jsonObject

        val reminder = FormattingResponseParser.parseSingleReminder(json)
        assertThat(reminder.locationTrigger).isNotNull()
        assertThat(reminder.locationTrigger!!.placeLabel).isEqualTo("Home")
        assertThat(reminder.locationTrigger!!.rawAddress).isNull()
    }

    // ── Edge cases ────────────────────────────────────────────────────

    @Test
    fun `response with leading and trailing whitespace is parsed`() {
        val json = "   \n  [{\"title\":\"Test\",\"body\":null,\"triggerTime\":null,\"recurrence\":null,\"locationTrigger\":null}]  \n  "

        val result = FormattingResponseParser.parse(json, "test")

        assertThat(result).isInstanceOf(FormattingResult.Success::class.java)
    }

    @Test
    fun `response with unicode characters is parsed`() {
        val json = """[{"title":"Buy groceries — milk, bread & eggs 🥛","body":null,"triggerTime":null,"recurrence":null,"locationTrigger":null}]"""

        val result = FormattingResponseParser.parse(json, "buy groceries")

        assertThat(result).isInstanceOf(FormattingResult.Success::class.java)
        assertThat((result as FormattingResult.Success).reminders[0].title).contains("🥛")
    }
}
