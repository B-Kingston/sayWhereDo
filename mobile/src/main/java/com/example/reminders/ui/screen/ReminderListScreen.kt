package com.example.reminders.ui.screen

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.reminders.R
import com.example.reminders.data.model.Reminder
import com.example.reminders.ui.component.AddNoteFab
import com.example.reminders.ui.component.ErrorStateView
import com.example.reminders.ui.theme.Spacing
import com.example.reminders.ui.theme.UiConstants

/**
 * Main screen displaying the user's list of reminders.
 *
 * Shows a loading skeleton while data is being fetched, an empty-state
 * illustration when there are no reminders, the reminder list when
 * available, and an error view on failure.
 *
 * @param uiState          The current [ReminderListUiState].
 * @param onRecordReminder Callback to start voice input.
 * @param onKeyboardInput  Callback to open keyboard input.
 * @param onSettings       Callback to open settings.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderListScreen(
    uiState: ReminderListUiState,
    onRecordReminder: () -> Unit,
    onKeyboardInput: () -> Unit = {},
    onSettings: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = stringResource(R.string.settings)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            AddNoteFab(
                onKeyboardSelected = onKeyboardInput,
                onMicMethodSelected = { onRecordReminder() },
                hasCloudProvider = false,
                hasLocalModel = false
            )
        }
    ) { padding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            when (uiState) {
                is ReminderListUiState.Loading -> LoadingSkeleton()

                is ReminderListUiState.Success -> {
                    if (uiState.reminders.isEmpty()) {
                        EmptyState(onKeyboardInput = onKeyboardInput)
                    } else {
                        ReminderListContent(reminders = uiState.reminders)
                    }
                }

                is ReminderListUiState.Error -> {
                    ErrorStateView(
                        message = uiState.message,
                        modifier = Modifier.padding(Spacing.md)
                    )
                }
            }
        }
    }
}

/**
 * Animated skeleton placeholder shown while reminders are loading.
 *
 * Displays pulsing rectangular shapes that mimic the expected content
 * layout, giving the user an immediate visual response instead of a
 * bare spinner.
 */
@Composable
private fun LoadingSkeleton() {
    val infiniteTransition = rememberInfiniteTransition(label = "skeleton_shimmer")
    val shimmerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(
                durationMillis = UiConstants.PULSE_DURATION_MS
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer_alpha"
    )

    val shimmerColor = MaterialTheme.colorScheme.surfaceContainerHighest

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        repeat(UiConstants.SKELETON_ROW_COUNT) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(UiConstants.SKELETON_ROW_HEIGHT_DP.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(shimmerColor.copy(alpha = shimmerAlpha)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Leading circle
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                )
                Spacer(modifier = Modifier.width(Spacing.md))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(UiConstants.SKELETON_SHIMMER_WIDTH_FRACTION)
                            .height(14.dp)
                            .clip(MaterialTheme.shapes.extraSmall)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.4f)
                            .height(10.dp)
                            .clip(MaterialTheme.shapes.extraSmall)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    )
                }
            }
        }
    }
}

/**
 * Beautiful empty-state view shown when there are no reminders.
 *
 * Features a large illustration icon, descriptive text, and an
 * action button to create the first reminder.
 *
 * @param onKeyboardInput Callback to open keyboard input for a new reminder.
 */
@Composable
private fun EmptyState(onKeyboardInput: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.lg),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Outlined.CheckCircleOutline,
            contentDescription = null,
            modifier = Modifier.size(UiConstants.EMPTY_STATE_ICON_SIZE_DP.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(Spacing.md))

        Text(
            text = stringResource(R.string.no_reminders_yet),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(Spacing.xs))

        Text(
            text = stringResource(R.string.empty_state_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(Spacing.lg))

        Button(
            onClick = onKeyboardInput,
            shape = MaterialTheme.shapes.large,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text(stringResource(R.string.empty_state_action))
        }
    }
}

/**
 * Displays the list of reminders.
 *
 * @param reminders The non-empty list of reminders to render.
 */
@Composable
private fun ReminderListContent(reminders: List<Reminder>) {
    // TODO: Phase 2 — Implement full reminder list with LazyColumn
}

// ── Previews ──────────────────────────────────────────────────────────

/** Preview of the loading state. */
@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun ReminderListLoadingPreview() {
    com.example.reminders.ui.theme.RemindersTheme {
        ReminderListScreen(
            uiState = ReminderListUiState.Loading,
            onRecordReminder = {},
            onKeyboardInput = {},
            onSettings = {}
        )
    }
}

/** Preview of the empty state. */
@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun ReminderListEmptyPreview() {
    com.example.reminders.ui.theme.RemindersTheme {
        ReminderListScreen(
            uiState = ReminderListUiState.Success(emptyList()),
            onRecordReminder = {},
            onKeyboardInput = {},
            onSettings = {}
        )
    }
}

/** Preview of the error state. */
@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun ReminderListErrorPreview() {
    com.example.reminders.ui.theme.RemindersTheme {
        ReminderListScreen(
            uiState = ReminderListUiState.Error("Something went wrong"),
            onRecordReminder = {},
            onKeyboardInput = {},
            onSettings = {}
        )
    }
}
