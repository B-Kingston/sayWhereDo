package com.example.reminders.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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

/**
 * A small "PRO" chip badge displayed next to Pro-gated features.
 *
 * Used throughout the UI — settings lists, edit screens, and feature
 * rows — to visually indicate that a feature requires the Pro upgrade.
 * Meets minimum contrast ratios and TalkBack accessibility requirements.
 *
 * @param modifier Optional modifier for layout customisation.
 */
@Composable
fun ProBadge(
    modifier: Modifier = Modifier
) {
    val contentDescription = stringResource(R.string.pro_badge_content_description)
    val shape = RoundedCornerShape(BADGE_CORNER_RADIUS)

    Text(
        text = stringResource(R.string.pro_badge_label),
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold
        ),
        color = MaterialTheme.colorScheme.onPrimary,
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.primary,
                shape = shape
            )
            .padding(
                horizontal = HORIZONTAL_PADDING,
                vertical = VERTICAL_PADDING
            )
            .semantics {
                this.contentDescription = contentDescription
            }
    )
}

private val HORIZONTAL_PADDING = 6.dp
private val VERTICAL_PADDING = 2.dp
private val BADGE_CORNER_RADIUS = 4.dp
