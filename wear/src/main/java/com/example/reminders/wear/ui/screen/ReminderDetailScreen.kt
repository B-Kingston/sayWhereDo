package com.example.reminders.wear.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.example.reminders.wear.R
import com.example.reminders.wear.data.WatchReminder
import com.example.reminders.wear.ui.viewmodel.ReminderDetailViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

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
            verticalArrangement = Arrangement.spacedBy(ITEM_SPACING)
        ) {
            if (reminder != null) {
                item {
                    ListHeader {
                        Text(
                            text = reminder!!.title.ifBlank { stringResource(R.string.no_title) },
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = MAX_TITLE_LINES,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                reminder!!.body?.let { body ->
                    if (body.isNotBlank()) {
                        item {
                            Text(
                                text = body,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = HORIZONTAL_PADDING)
                            )
                        }
                    }
                }

                reminder!!.triggerTime?.let { triggerTime ->
                    item {
                        Text(
                            text = formatTriggerTime(triggerTime),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = HORIZONTAL_PADDING)
                        )
                    }
                }

                reminder!!.locationTriggerJson?.let {
                    item {
                        Text(
                            text = extractLocationLabel(it),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = HORIZONTAL_PADDING)
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(SECTION_SPACING))
                }

                if (reminder?.isCompleted == false) {
                    item {
                        Button(
                            onClick = { viewModel.completeReminder() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = ButtonDefaults.shape
                        ) {
                            Text(stringResource(R.string.complete))
                        }
                    }
                }

                item {
                    Button(
                        onClick = {
                            viewModel.deleteReminder {
                                onNavigateBack()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        shape = ButtonDefaults.shape
                    ) {
                        Text(stringResource(R.string.delete))
                    }
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

private fun formatTriggerTime(instant: Instant): String {
    val formatter = DateTimeFormatter.ofPattern("MMM d, h:mm a")
        .withZone(ZoneId.systemDefault())
    return formatter.format(instant)
}

private fun extractLocationLabel(json: String): String {
    return try {
        val obj = kotlinx.serialization.json.Json.decodeFromString<
            kotlinx.serialization.json.JsonObject>(json)
        obj["placeLabel"]?.toString()?.trim('"') ?: ""
    } catch (_: Exception) {
        ""
    }
}

private val ITEM_SPACING = 4.dp
private val HORIZONTAL_PADDING = 12.dp
private val SECTION_SPACING = 16.dp
private const val MAX_TITLE_LINES = 3
