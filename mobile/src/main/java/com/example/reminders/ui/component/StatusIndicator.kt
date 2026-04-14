package com.example.reminders.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.example.reminders.ui.theme.StatusError
import com.example.reminders.ui.theme.StatusSuccess
import com.example.reminders.ui.theme.StatusWarning
import com.example.reminders.ui.theme.UiConstants

/**
 * A small status indicator showing a coloured dot and a label.
 *
 * Used to display pipeline state (e.g. "Offline", "Pending",
 * "Synced") in the UI. The dot colour reflects the status level:
 * - [StatusLevel.OK] — green (healthy)
 * - [StatusLevel.WARNING] — amber (degraded)
 * - [StatusLevel.ERROR] — red (offline/error)
 *
 * Uses semantic status colours from [com.example.reminders.ui.theme.Color].
 *
 * @param label    Human-readable status text.
 * @param level    The severity level that drives the dot colour.
 * @param modifier Optional modifier for layout customisation.
 */
@Composable
fun StatusIndicator(
    label: String,
    level: StatusLevel,
    modifier: Modifier = Modifier
) {
    val dotColor = when (level) {
        StatusLevel.OK -> StatusSuccess
        StatusLevel.WARNING -> StatusWarning
        StatusLevel.ERROR -> StatusError
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clearAndSetSemantics { contentDescription = label }
    ) {
        Box(
            modifier = Modifier
                .size(UiConstants.STATUS_DOT_SIZE_DP.dp)
                .background(color = dotColor, shape = CircleShape)
        )

        Spacer(modifier = Modifier.size(UiConstants.STATUS_DOT_LABEL_SPACING_DP.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

/**
 * Severity levels for [StatusIndicator].
 */
enum class StatusLevel {
    /** Healthy / connected state — green. */
    OK,

    /** Degraded / pending state — amber. */
    WARNING,

    /** Error / offline state — red. */
    ERROR
}

// ── Previews ──────────────────────────────────────────────────────────

/** Preview of all status indicator levels. */
@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun StatusIndicatorPreview() {
    com.example.reminders.ui.theme.RemindersTheme {
        androidx.compose.foundation.layout.Column(
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
        ) {
            StatusIndicator(label = "Connected", level = StatusLevel.OK)
            StatusIndicator(label = "Syncing…", level = StatusLevel.WARNING)
            StatusIndicator(label = "Offline", level = StatusLevel.ERROR)
        }
    }
}
