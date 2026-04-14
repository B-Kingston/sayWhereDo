package com.example.reminders.wear.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AppCard
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.example.reminders.wear.R
import com.example.reminders.wear.ui.theme.WearConstants
import com.example.reminders.wear.ui.theme.WearSpacing
import com.example.reminders.wear.ui.viewmodel.ReminderDetailViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Detail screen for a single reminder.
 *
 * Displays the reminder title, optional body, trigger time, and
 * location information inside themed cards. Provides "Complete" and
 * "Delete" action buttons.
 */
@Composable
fun ReminderDetailScreen(
    viewModel: ReminderDetailViewModel,
    onNavigateBack: () -> Unit
) {
    val reminder by viewModel.reminder.collectAsStateWithLifecycle()

    val listState = rememberTransformingLazyColumnState()

    ScreenScaffold(scrollState = listState) { contentPadding ->
        TransformingLazyColumn(
            contentPadding = contentPadding,
            state = listState,
            verticalArrangement = Arrangement.spacedBy(WearConstants.ItemSpacing)
        ) {
            val current = reminder
            if (current != null) {
                item {
                    ListHeader {
                        Text(
                            text = current.title.ifBlank {
                                stringResource(R.string.no_title)
                            },
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = WearConstants.MaxDetailTitleLines,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (!current.body.isNullOrBlank()) {
                    item {
                        BodyCard(body = current.body)
                    }
                }

                current.triggerTime?.let { triggerTime ->
                    item {
                        TriggerTimeCard(triggerTime = triggerTime)
                    }
                }

                current.locationTriggerJson?.let { json ->
                    val label = extractLocationLabel(json)
                    if (label.isNotBlank()) {
                        item {
                            LocationCard(locationLabel = label)
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(WearConstants.SectionSpacing))
                }

                if (!current.isCompleted) {
                    item {
                        CompleteButton(onClick = { viewModel.completeReminder() })
                    }
                }

                item {
                    DeleteButton(
                        onClick = {
                            viewModel.deleteReminder { onNavigateBack() }
                        }
                    )
                }
            } else {
                item {
                    Text(
                        text = stringResource(R.string.loading),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

/**
 * Card displaying the reminder body text.
 */
@Composable
private fun BodyCard(body: String) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Card showing the trigger time with a schedule icon.
 */
@Composable
private fun TriggerTimeCard(triggerTime: Instant) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Icon(
            imageVector = Icons.Default.Schedule,
            contentDescription = null,
            modifier = Modifier.padding(end = WearSpacing.Sm),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = formatTriggerTime(triggerTime),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Card showing the location label with a location icon.
 */
@Composable
private fun LocationCard(locationLabel: String) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = null,
            modifier = Modifier.padding(end = WearSpacing.Sm),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = locationLabel,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Primary "Complete" action button styled with the teal accent.
 */
@Composable
private fun CompleteButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        shape = ButtonDefaults.shape
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null
        )
        Text(stringResource(R.string.complete))
    }
}

/**
 * Destructive "Delete" button styled in the error colour.
 */
@Composable
private fun DeleteButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        ),
        shape = ButtonDefaults.shape
    ) {
        Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = null
        )
        Text(stringResource(R.string.delete))
    }
}

/**
 * Formats an [Instant] into a human-readable date/time string.
 */
private fun formatTriggerTime(instant: Instant): String {
    val formatter = DateTimeFormatter.ofPattern("MMM d, h:mm a")
        .withZone(ZoneId.systemDefault())
    return formatter.format(instant)
}

/**
 * Extracts the `placeLabel` field from a location JSON string.
 * Returns an empty string if parsing fails.
 */
private fun extractLocationLabel(json: String): String {
    return try {
        val obj = kotlinx.serialization.json.Json.decodeFromString<
            kotlinx.serialization.json.JsonObject
        >(json)
        obj["placeLabel"]?.toString()?.trim('"') ?: ""
    } catch (_: Exception) {
        ""
    }
}
