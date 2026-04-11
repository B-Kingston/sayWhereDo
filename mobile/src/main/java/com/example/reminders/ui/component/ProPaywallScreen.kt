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

/**
 * A reusable full-screen paywall card shown when a free user attempts
 * a Pro-gated feature.
 *
 * Displays the feature name, a bullet list of Pro benefits, and two
 * CTAs: "Upgrade to Pro" (launches billing flow) and "Restore Purchases".
 * An optional hint about BYO API key is shown when the paywall was
 * triggered by formatting usage limits.
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
            .padding(CARD_PADDING),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(INTERNAL_PADDING)
        ) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(ICON_SIZE)
            )

            Spacer(Modifier.height(TITLE_SPACING))

            Text(
                text = stringResource(R.string.paywall_title, featureName),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold
                )
            )

            Spacer(Modifier.height(BENEFITS_SPACING))

            Column(verticalArrangement = Arrangement.spacedBy(BENEFIT_ROW_SPACING)) {
                BenefitRow(stringResource(R.string.paywall_benefit_formatting))
                BenefitRow(stringResource(R.string.paywall_benefit_geofences))
                BenefitRow(stringResource(R.string.paywall_benefit_saved_places))
                BenefitRow(stringResource(R.string.paywall_benefit_recurrence))
                BenefitRow(stringResource(R.string.paywall_benefit_snooze))
                BenefitRow(stringResource(R.string.paywall_benefit_custom_radius))
                BenefitRow(stringResource(R.string.paywall_benefit_export))
            }

            Spacer(Modifier.height(CTA_SPACING))

            OutlinedButton(
                onClick = onUpgrade,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = upgradeContentDescription }
            ) {
                Text(stringResource(R.string.paywall_upgrade_button))
            }

            Spacer(Modifier.height(BUTTON_SPACING))

            TextButton(
                onClick = onRestore,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = restoreContentDescription }
            ) {
                Text(stringResource(R.string.paywall_restore_button))
            }

            Spacer(Modifier.height(BUTTON_SPACING))

            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.dismiss))
            }

            if (showApiKeyHint) {
                Spacer(Modifier.height(HINT_SPACING))

                Text(
                    text = stringResource(R.string.paywall_byo_key_alternative),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun BenefitRow(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(CHECK_ICON_SIZE)
        )
        Spacer(Modifier.width(BENEFIT_TEXT_SPACING))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

private val CARD_PADDING = 16.dp
private val INTERNAL_PADDING = 24.dp
private val ICON_SIZE = 48.dp
private val TITLE_SPACING = 12.dp
private val BENEFITS_SPACING = 16.dp
private val BENEFIT_ROW_SPACING = 8.dp
private val CTA_SPACING = 24.dp
private val BUTTON_SPACING = 4.dp
private val HINT_SPACING = 12.dp
private val CHECK_ICON_SIZE = 20.dp
private val BENEFIT_TEXT_SPACING = 8.dp
