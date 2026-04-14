package com.example.reminders.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.reminders.R
import com.example.reminders.ui.theme.UiConstants

/**
 * A small "PRO" chip badge displayed next to Pro-gated features.
 *
 * Used throughout the UI — settings lists, edit screens, and feature
 * rows — to visually indicate that a feature requires the Pro upgrade.
 * Meets minimum contrast ratios and TalkBack accessibility requirements.
 *
 * Uses theme colours and shapes from the design system.
 *
 * @param modifier Optional modifier for layout customisation.
 */
@Composable
fun ProBadge(
    modifier: Modifier = Modifier
) {
    val contentDescription = stringResource(R.string.pro_badge_content_description)

    Text(
        text = stringResource(R.string.pro_badge_label),
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold
        ),
        color = MaterialTheme.colorScheme.onPrimary,
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.primary,
                shape = MaterialTheme.shapes.extraSmall
            )
            .padding(
                horizontal = UiConstants.PRO_BADGE_HORIZONTAL_PADDING_DP.dp,
                vertical = UiConstants.PRO_BADGE_VERTICAL_PADDING_DP.dp
            )
            .semantics {
                this.contentDescription = contentDescription
            }
    )
}

// ── Previews ──────────────────────────────────────────────────────────

/** Preview of the ProBadge. */
@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun ProBadgePreview() {
    com.example.reminders.ui.theme.RemindersTheme {
        ProBadge()
    }
}
