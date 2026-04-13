package com.example.reminders.wear.ui.screen

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import com.example.reminders.wear.R
import com.example.reminders.wear.data.WatchReminder
import com.example.reminders.wear.ui.component.PhoneRequiredBanner
import com.example.reminders.wear.ui.viewmodel.WatchReminderListViewModel

@Composable
fun WatchReminderListScreen(
    viewModel: WatchReminderListViewModel,
    isPhoneConnected: Boolean,
    onNavigateToVoiceRecord: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val reminders by viewModel.reminders.collectAsStateWithLifecycle()

    val listState = rememberTransformingLazyColumnState()

    AppScaffold {
        ScreenScaffold(
            scrollState = listState,
            edgeButton = {
                EdgeButton(
                    onClick = onNavigateToVoiceRecord,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Text(stringResource(R.string.add))
                }
            }
        ) { contentPadding ->
            TransformingLazyColumn(contentPadding = contentPadding, state = listState) {
                item {
                    ListHeader {
                        Text(text = stringResource(R.string.app_name))
                    }
                }

                item {
                    Button(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.settings_title),
                            modifier = Modifier.size(ICON_SIZE.dp)
                        )
                    }
                }

                if (!isPhoneConnected) {
                    item {
                        PhoneRequiredBanner()
                    }
                }

                if (reminders.isEmpty()) {
                    item {
                        Button(
                            onClick = onNavigateToVoiceRecord,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.no_reminders_yet))
                        }
                    }
                } else {
                    items(
                        count = reminders.size,
                        key = { index -> reminders[index].id }
                    ) { index ->
                        val reminder = reminders[index]
                        ReminderListChip(
                            reminder = reminder,
                            onClick = { onNavigateToDetail(reminder.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReminderListChip(
    reminder: WatchReminder,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = ButtonDefaults.shape
    ) {
        Text(
            text = reminder.title.ifBlank { stringResource(R.string.no_title) },
            style = MaterialTheme.typography.bodyMedium,
            maxLines = MAX_LINES
        )
    }
}

private const val MAX_LINES = 2
private const val ICON_SIZE = 24
