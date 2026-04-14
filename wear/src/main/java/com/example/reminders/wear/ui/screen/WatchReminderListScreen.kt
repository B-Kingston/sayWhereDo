package com.example.reminders.wear.ui.screen

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Settings
import com.example.reminders.wear.R
import com.example.reminders.wear.ui.component.PhoneRequiredBanner
import com.example.reminders.wear.ui.component.SwipeableWatchReminderCard
import com.example.reminders.wear.ui.theme.WearConstants
import com.example.reminders.wear.ui.viewmodel.WatchReminderListViewModel

/**
 * Main reminder list screen on the watch.
 *
 * Displays all pending reminders as tappable cards, a settings shortcut,
 * and an "Add" EdgeButton. When no reminders exist a friendly empty
 * state with an icon and hint text is shown. If the phone is
 * disconnected, a [PhoneRequiredBanner] is displayed.
 */
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
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
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
                    SettingsCard(onClick = onNavigateToSettings)
                }

                if (!isPhoneConnected) {
                    item {
                        PhoneRequiredBanner()
                    }
                }

                if (reminders.isEmpty()) {
                    item {
                        EmptyReminderState(onClick = onNavigateToVoiceRecord)
                    }
                } else {
                    items(
                        count = reminders.size,
                        key = { index -> reminders[index].id }
                    ) { index ->
                        val reminder = reminders[index]
                        SwipeableWatchReminderCard(
                            reminder = reminder,
                            onComplete = { viewModel.completeReminder(reminder) },
                            onDelete = { viewModel.deleteReminder(reminder.id) },
                            onClick = { onNavigateToDetail(reminder.id) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Settings shortcut card with a gear icon.
 *
 * Rendered as a subtle card to visually separate it from reminder items.
 */
@Composable
private fun SettingsCard(onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = Icons.Default.Settings,
            contentDescription = stringResource(R.string.settings_title),
            modifier = Modifier.size(WearConstants.ListIconSize),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Friendly empty state shown when there are no reminders.
 *
 * Displays a prominent icon and a hint encouraging the user to add
 * their first reminder. The entire card is tappable and routes to the
 * voice/keyboard input screen.
 */
@Composable
private fun EmptyReminderState(onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = Icons.Default.AddCircle,
            contentDescription = null,
            modifier = Modifier.size(WearConstants.ListIconSize),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = stringResource(R.string.no_reminders_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
