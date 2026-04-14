package com.example.reminders.ui.screen

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.reminders.R
import com.example.reminders.data.model.DeletedReminder
import com.example.reminders.ui.theme.Spacing
import com.example.reminders.ui.theme.UiConstants

/**
 * Screen displaying soft-deleted reminders (trash).
 *
 * Shows a list of deleted reminders with their original title and relative
 * deletion time. Each item has a restore button. Displays an empty state
 * when no deleted reminders exist.
 *
 * @param deletedReminders  The current list of deleted reminders.
 * @param onRestore         Callback to restore a reminder by its ID.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(
    deletedReminders: List<DeletedReminder>,
    onRestore: (String) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.trash_screen_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            if (deletedReminders.isEmpty()) {
                TrashEmptyState()
            } else {
                TrashListContent(
                    deletedReminders = deletedReminders,
                    onRestore = onRestore
                )
            }
        }
    }
}

/**
 * Empty-state view shown when there are no deleted reminders.
 *
 * Displays a trash icon and descriptive text to indicate the trash is empty.
 */
@Composable
private fun TrashEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.lg),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Outlined.DeleteOutline,
            contentDescription = null,
            modifier = Modifier.size(UiConstants.EMPTY_STATE_ICON_SIZE_DP.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(Spacing.md))

        Text(
            text = stringResource(R.string.no_deleted_reminders),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Displays the list of deleted reminders inside a [LazyColumn].
 *
 * Each item shows the original title, relative time since deletion, and a
 * restore button. A bottom spacer prevents the last item from being
 * obscured by system navigation.
 *
 * @param deletedReminders The non-empty list of deleted reminders to render.
 * @param onRestore        Callback to restore a reminder by its ID.
 */
@Composable
private fun TrashListContent(
    deletedReminders: List<DeletedReminder>,
    onRestore: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            horizontal = Spacing.md,
            vertical = Spacing.sm
        ),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        items(
            items = deletedReminders,
            key = { reminder -> reminder.id }
        ) { reminder ->
            TrashItem(
                reminder = reminder,
                onRestore = { onRestore(reminder.id) }
            )
        }

        item {
            Spacer(
                modifier = Modifier.height(
                    UiConstants.REMINDER_LIST_BOTTOM_PADDING_DP.dp
                )
            )
        }
    }
}

/**
 * A single row representing a deleted reminder.
 *
 * Shows the original title, a relative timestamp ("2 hours ago"), and an
 * outlined restore button.
 *
 * @param reminder   The deleted reminder to display.
 * @param onRestore  Callback invoked when the restore button is pressed.
 */
@Composable
private fun TrashItem(
    reminder: DeletedReminder,
    onRestore: () -> Unit
) {
    val context = LocalContext.current
    val relativeTime = remember(reminder.deletedAt) {
        DateUtils.getRelativeTimeSpanString(
            context,
            reminder.deletedAt.toEpochMilli()
        ).toString()
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = UiConstants.REMINDER_CARD_ELEVATION_DP.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                Text(
                    text = reminder.originalTitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = relativeTime,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            OutlinedButton(onClick = onRestore) {
                Text(stringResource(R.string.restore))
            }
        }
    }
}

// ── Previews ──────────────────────────────────────────────────────────

/** Preview of the empty trash state. */
@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun TrashEmptyPreview() {
    com.example.reminders.ui.theme.RemindersTheme {
        TrashScreen(
            deletedReminders = emptyList(),
            onRestore = {}
        )
    }
}

/** Preview of the trash list with sample items. */
@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun TrashListPreview() {
    com.example.reminders.ui.theme.RemindersTheme {
        TrashScreen(
            deletedReminders = listOf(
                DeletedReminder(
                    id = "1",
                    originalTitle = "Buy groceries",
                    deletedAt = java.time.Instant.now().minusSeconds(3600),
                    deletedBy = "mobile",
                    originalUpdatedAt = java.time.Instant.now()
                ),
                DeletedReminder(
                    id = "2",
                    originalTitle = "Call dentist",
                    deletedAt = java.time.Instant.now().minusSeconds(86400),
                    deletedBy = "watch",
                    originalUpdatedAt = java.time.Instant.now()
                )
            ),
            onRestore = {}
        )
    }
}
