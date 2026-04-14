package com.example.reminders.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.reminders.R
import com.example.reminders.ui.theme.Spacing
import com.example.reminders.ui.theme.UiConstants

/**
 * A reusable full-screen paywall card shown when a free user attempts
 * a Pro-gated feature.
 *
 * Displays the feature name, a bullet list of Pro benefits, and two
 * CTAs: "Upgrade to Pro" (launches billing flow) and "Restore Purchases".
 * An optional hint about BYO API key is shown when the paywall was
 * triggered by formatting usage limits.
 *
 * Uses design-system colours, shapes, and spacing tokens for a polished
 * visual hierarchy.
 *
 * @param featureName    The human-readable name of the gated feature
 *                        (e.g. "Cloud Formatting", "Location Reminders").
 * @param onUpgrade      Callback that launches the Play Billing purchase flow.
 * @param onRestore      Callback that triggers purchase restoration.
 * @param onDismiss      Callback to close the paywall without action.
 * @param showApiKeyHint Whether to show the BYO API key alternative message.
 * @param modifier       Optional modifier for layout customisation.
 */
@Composable
fun ProPaywallScreen(
    featureName: String,
    onUpgrade: () -> Unit,
    onRestore: () -> Unit,
    onDismiss: () -> Unit,
    showApiKeyHint: Boolean = false,
    modifier: Modifier = Modifier
) {
    val upgradeContentDescription = stringResource(R.string.content_desc_upgrade_button)
    val restoreContentDescription = stringResource(R.string.content_desc_restore_button)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(Spacing.md),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.lg)
        ) {
            // ── Hero icon ─────────────────────────────────────────
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(UiConstants.PAYWALL_ICON_SIZE_DP.dp)
            )

            Spacer(Modifier.height(Spacing.sm))

            // ── Title ─────────────────────────────────────────────
            Text(
                text = stringResource(R.string.paywall_title, featureName),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold
                )
            )

            Spacer(Modifier.height(Spacing.md))

            // ── Benefits list ─────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                BenefitRow(stringResource(R.string.paywall_benefit_formatting))
                BenefitRow(stringResource(R.string.paywall_benefit_geofences))
                BenefitRow(stringResource(R.string.paywall_benefit_saved_places))
                BenefitRow(stringResource(R.string.paywall_benefit_recurrence))
                BenefitRow(stringResource(R.string.paywall_benefit_snooze))
                BenefitRow(stringResource(R.string.paywall_benefit_custom_radius))
                BenefitRow(stringResource(R.string.paywall_benefit_export))
            }

            Spacer(Modifier.height(Spacing.lg))

            // ── Upgrade CTA ───────────────────────────────────────
            OutlinedButton(
                onClick = onUpgrade,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = upgradeContentDescription },
                shape = MaterialTheme.shapes.large
            ) {
                Text(
                    text = stringResource(R.string.paywall_upgrade_button),
                    style = MaterialTheme.typography.labelLarge
                )
            }

            Spacer(Modifier.height(Spacing.xs))

            // ── Restore ───────────────────────────────────────────
            TextButton(
                onClick = onRestore,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = restoreContentDescription }
            ) {
                Text(stringResource(R.string.paywall_restore_button))
            }

            Spacer(Modifier.height(Spacing.xs))

            // ── Dismiss ───────────────────────────────────────────
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.dismiss))
            }

            // ── BYO key hint ──────────────────────────────────────
            if (showApiKeyHint) {
                Spacer(Modifier.height(Spacing.sm))

                Text(
                    text = stringResource(R.string.paywall_byo_key_alternative),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * A single benefit row with a checkmark icon.
 */
@Composable
private fun BenefitRow(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(UiConstants.PAYWALL_CHECK_ICON_SIZE_DP.dp)
        )
        Spacer(Modifier.width(Spacing.sm))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

// ── Previews ──────────────────────────────────────────────────────────

/** Preview of the Pro paywall. */
@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun ProPaywallPreview() {
    com.example.reminders.ui.theme.RemindersTheme {
        ProPaywallScreen(
            featureName = "Cloud Formatting",
            onUpgrade = {},
            onRestore = {},
            onDismiss = {},
            showApiKeyHint = true
        )
    }
}
