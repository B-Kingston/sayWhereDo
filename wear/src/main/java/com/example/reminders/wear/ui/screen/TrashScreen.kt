package com.example.reminders.wear.ui.screen

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import com.example.reminders.wear.R
import com.example.reminders.wear.data.DeletedReminder
import com.example.reminders.wear.ui.theme.WearConstants
import com.example.reminders.wear.ui.viewmodel.TrashViewModel
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Trash screen for the watch.
 *
 * Displays all soft-deleted reminders from the tombstone table. Tapping
 * a card removes the tombstone and flags the reminder for restoration
 * during the next sync cycle. When the trash is empty, a friendly
 * empty-state card is shown.
 *
 * @param viewModel the [TrashViewModel] providing deleted-reminder data
 *  and restore/clean operations.
 */
@Composable
fun TrashScreen(
    viewModel: TrashViewModel
) {
    val deletedReminders by viewModel.deletedReminders.collectAsStateWithLifecycle()

    val listState = rememberTransformingLazyColumnState()

    AppScaffold {
        ScreenScaffold(scrollState = listState) { contentPadding ->
            TransformingLazyColumn(contentPadding = contentPadding, state = listState) {
                item {
                    ListHeader {
                        Text(text = stringResource(R.string.trash_title))
                    }
                }

                if (deletedReminders.isEmpty()) {
                    item {
                        EmptyTrashState()
                    }
                } else {
                    items(
                        count = deletedReminders.size,
                        key = { index -> deletedReminders[index].id }
                    ) { index ->
                        val deletedReminder = deletedReminders[index]
                        DeletedReminderCard(
                            deletedReminder = deletedReminder,
                            onRestore = { viewModel.restore(deletedReminder.id) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Card representing a single soft-deleted reminder in the trash.
 *
 * Shows the original reminder title and the date it was deleted.
 * Tapping the card triggers restoration.
 *
 * @param deletedReminder the tombstone record to display.
 * @param onRestore       callback invoked when the user taps to restore.
 */
@Composable
private fun DeletedReminderCard(
    deletedReminder: DeletedReminder,
    onRestore: () -> Unit
) {
    Card(
        onClick = onRestore,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = deletedReminder.originalTitle.ifBlank { stringResource(R.string.no_title) },
            style = MaterialTheme.typography.titleSmall,
            maxLines = WearConstants.MaxTitleLines,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = formatDeletedDate(deletedReminder),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Empty state shown when the trash contains no deleted reminders.
 *
 * Displays a subtle icon and a hint message so the user understands
 * the purpose of this screen.
 */
@Composable
private fun EmptyTrashState() {
    Card(
        onClick = {},
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = Icons.Default.DeleteOutline,
            contentDescription = null,
            modifier = Modifier.size(EmptyStateIconSize),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = stringResource(R.string.no_deleted_reminders),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Formats the deletion timestamp into a human-readable relative date.
 */
private fun formatDeletedDate(deletedReminder: DeletedReminder): String {
    val formatter = DateTimeFormatter.ofPattern(DeletedDateFormat)
        .withZone(ZoneId.systemDefault())
    return formatter.format(deletedReminder.deletedAt)
}

private val EmptyStateIconSize = 24.dp
private const val DeletedDateFormat = "MMM d, yyyy"
