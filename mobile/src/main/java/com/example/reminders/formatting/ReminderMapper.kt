package com.example.reminders.formatting

import android.util.Log
import com.example.reminders.data.model.ParsedReminder
import com.example.reminders.data.model.Reminder
import java.util.UUID

/**
 * Converts [ParsedReminder] objects (produced by the formatting pipeline)
 * into [Reminder] entities ready for Room persistence.
 *
 * Generates stable UUIDs and populates metadata fields that are not
 * available at the formatting stage (source transcript, provider name, etc.).
 */
object ReminderMapper {

    /**
     * Maps a list of [ParsedReminder]s to [Reminder] entities.
     *
     * @param parsedReminders     The reminders returned by a [FormattingProvider].
     * @param sourceTranscript    The original voice transcript text.
     * @param formattingProvider  Identifier of the provider used ("cloud" or "none").
     * @return A list of [Reminder] entities with generated IDs and metadata.
     */
    fun mapToReminders(
        parsedReminders: List<ParsedReminder>,
        sourceTranscript: String,
        formattingProvider: String
    ): List<Reminder> {
        Log.d(TAG, "Mapping ${parsedReminders.size} parsed reminder(s) (provider=$formattingProvider)")
        return parsedReminders.map { parsed ->
            Reminder(
                id = UUID.randomUUID().toString(),
                title = parsed.title,
                body = parsed.body,
                triggerTime = parsed.triggerTime,
                recurrence = parsed.recurrence,
                locationTrigger = parsed.locationTrigger,
                sourceTranscript = sourceTranscript,
                formattingProvider = formattingProvider,
                geofencingDevice = "phone"
            )
        }
    }

    private const val TAG = "ReminderMapper"
}
