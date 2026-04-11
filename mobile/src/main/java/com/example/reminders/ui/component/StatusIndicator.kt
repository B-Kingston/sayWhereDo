package com.example.reminders.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * A small status indicator showing a coloured dot and a label.
 *
 * Used to display pipeline state (e.g. "Offline", "Pending",
 * "Synced") in the UI. The dot colour reflects the status level:
 * - [StatusLevel.OK] — green (healthy)
 * - [StatusLevel.WARNING] — amber (degraded)
 * - [StatusLevel.ERROR] — red (offline/error)
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
        StatusLevel.OK -> StatusGreen
        StatusLevel.WARNING -> StatusAmber
        StatusLevel.ERROR -> StatusRed
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .semantics { contentDescription = label }
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .size(DOT_SIZE)
                .background(color = dotColor, shape = CircleShape)
        )

        androidx.compose.foundation.layout.Spacer(
            modifier = Modifier.size(DOT_LABEL_SPACING)
        )

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
    OK,
    WARNING,
    ERROR
}

private val DOT_SIZE = 8.dp
private val DOT_LABEL_SPACING = 6.dp

private val StatusGreen = androidx.compose.ui.graphics.Color(0xFF4CAF50)
private val StatusAmber = androidx.compose.ui.graphics.Color(0xFFFFC107)
private val StatusRed = androidx.compose.ui.graphics.Color(0xFFF44336)
