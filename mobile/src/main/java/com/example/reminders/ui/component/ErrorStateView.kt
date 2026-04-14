package com.example.reminders.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.reminders.R
import com.example.reminders.ui.theme.Spacing
import com.example.reminders.ui.theme.UiConstants

/**
 * A reusable error state view with optional retry and "save raw" buttons.
 *
 * Displays a large illustration icon, error message, and action buttons
 * based on the error type. Used across the app for formatting failures,
 * network errors, geocoding failures, and other recoverable error states.
 *
 * Uses design-system colours and spacing tokens.
 *
 * @param message        The human-readable error message to display.
 * @param onRetry        Optional callback for retrying the failed operation.
 *                        When null, the retry button is hidden.
 * @param onSaveRaw      Optional callback for saving the raw transcript
 *                        as an unformatted reminder. When null, the
 *                        "save raw" button is hidden.
 * @param modifier       Optional modifier for layout customisation.
 */
@Composable
fun ErrorStateView(
    message: String,
    onRetry: (() -> Unit)? = null,
    onSaveRaw: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.CloudOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
            modifier = Modifier.size(UiConstants.ERROR_ICON_SIZE_DP.dp)
        )

        Spacer(Modifier.height(Spacing.md))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(Spacing.md))

        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onRetry != null) {
                val retryContentDescription = stringResource(R.string.content_desc_retry_button)
                OutlinedButton(
                    onClick = onRetry,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.semantics {
                        contentDescription = retryContentDescription
                    }
                ) {
                    Text(stringResource(R.string.error_retry))
                }
            }

            if (onSaveRaw != null) {
                val saveRawContentDescription =
                    stringResource(R.string.content_desc_save_raw_button)
                TextButton(
                    onClick = onSaveRaw,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.semantics {
                        contentDescription = saveRawContentDescription
                    }
                ) {
                    Text(stringResource(R.string.error_save_raw))
                }
            }
        }
    }
}

// ── Previews ──────────────────────────────────────────────────────────

/** Preview of the error state view. */
@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun ErrorStateViewPreview() {
    com.example.reminders.ui.theme.RemindersTheme {
        ErrorStateView(
            message = "Something went wrong",
            onRetry = {},
            onSaveRaw = {}
        )
    }
}
