package com.example.reminders.wear.complication

import android.app.PendingIntent
import android.content.Intent
import android.util.Log
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import com.example.reminders.wear.di.WatchRemindersApplication
import com.example.reminders.wear.presentation.MainActivity
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class ReminderComplicationProvider : SuspendingComplicationDataSourceService() {

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        Log.d(TAG, "onComplicationRequest: id=${request.complicationInstanceId}, type=${request.complicationType}")

        if (request.complicationType != ComplicationType.SHORT_TEXT) {
            Log.w(TAG, "Unsupported complication type: ${request.complicationType}")
            return null
        }

        val container = (applicationContext as WatchRemindersApplication).container
        val preferences = complicationPreferences ?: ComplicationPreferences(applicationContext).also {
            complicationPreferences = it
        }

        return try {
            val mode = preferences.complicationMode.first()
            val count = getReminderCount(container.watchReminderDao, mode)

            val tapIntent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val pendingIntent = PendingIntent.getActivity(
                applicationContext,
                request.complicationInstanceId,
                tapIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            ShortTextComplicationData.Builder(
                text = PlainComplicationText.Builder(count.toString()).build(),
                contentDescription = PlainComplicationText.Builder(
                    getString(com.example.reminders.wear.R.string.app_name)
                ).build()
            )
                .setTapAction(pendingIntent)
                .build()
                .also { Log.d(TAG, "Updated complication: count=$count, mode=$mode") }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update complication", e)
            null
        }
    }

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        return ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder("3").build(),
            contentDescription = PlainComplicationText.Builder(
                getString(com.example.reminders.wear.R.string.app_name)
            ).build()
        ).build()
    }

    private suspend fun getReminderCount(
        dao: com.example.reminders.wear.data.WatchReminderDao,
        mode: ComplicationMode
    ): Int {
        return when (mode) {
            ComplicationMode.TODAY -> {
                val zone = ZoneId.systemDefault()
                val startOfDay = LocalDate.now(zone)
                    .atStartOfDay(zone)
                    .toInstant()
                    .toEpochMilli()
                val endOfDay = LocalDate.now(zone)
                    .plusDays(1)
                    .atStartOfDay(zone)
                    .toInstant()
                    .toEpochMilli()
                dao.getUpcoming(startOfDay, endOfDay).first().size
            }
            ComplicationMode.ALL_UPCOMING -> {
                val now = Instant.now().toEpochMilli()
                dao.getUpcomingFrom(now).first().size
            }
        }
    }

    companion object {
        private const val TAG = "ReminderComplication"
        private var complicationPreferences: ComplicationPreferences? = null
    }
}
