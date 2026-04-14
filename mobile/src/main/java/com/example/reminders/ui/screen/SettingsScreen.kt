package com.example.reminders.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.reminders.R
import com.example.reminders.data.preferences.UserPreferences
import com.example.reminders.ui.component.AiModelSettingsSection
import com.example.reminders.ui.component.ProBadge
import com.example.reminders.ui.component.StatusIndicator
import com.example.reminders.ui.component.StatusLevel
import com.example.reminders.ui.theme.Spacing
import com.example.reminders.ui.theme.UiConstants
import com.example.reminders.ui.viewmodel.ExportState
import com.example.reminders.ui.viewmodel.ImportState
import com.example.reminders.ui.viewmodel.ProSettingsViewModel
import com.example.reminders.ui.viewmodel.RestoreState
import kotlinx.coroutines.launch

/**
 * Settings screen where the user can configure AI formatting, view Pro status,
 * upgrade, restore purchases, and export/import data.
 *
 * The API key and AI provider settings are managed via
 * [com.example.reminders.data.preferences.UserPreferences] and are never logged
 * or displayed in plain text by default.
 *
 * @param currentApiKey  The currently stored API key, or null if not set.
 * @param userPreferences The [UserPreferences] instance for reading/writing AI settings.
 * @param proViewModel   ViewModel managing Pro status, export/import, and billing.
 * @param onSaveApiKey   Callback invoked when the user saves a new API key.
 * @param onBack         Callback invoked when the user presses the back button.
 * @param onUpgrade      Callback that launches the Play Billing purchase flow.
 * @param onExport       Callback that receives the exported JSON for sharing/saving.
 * @param onImport       Callback that triggers the file picker for import.
 * @param isWatchConnected Whether a WearOS watch is currently connected.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentApiKey: String?,
    userPreferences: UserPreferences,
    proViewModel: ProSettingsViewModel,
    onSaveApiKey: (String) -> Unit,
    onBack: () -> Unit,
    onUpgrade: () -> Unit,
    onExport: (String) -> Unit,
    onImport: () -> Unit,
    isWatchConnected: Boolean = false
) {
    val scope = rememberCoroutineScope()

    val isPro by proViewModel.isPro.collectAsStateWithLifecycle()
    val exportState by proViewModel.exportState.collectAsStateWithLifecycle()
    val importState by proViewModel.importState.collectAsStateWithLifecycle()
    val restoreState by proViewModel.restoreState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = Spacing.md)
                    .verticalScroll(rememberScrollState())
                    .imePadding()
            ) {
                Spacer(Modifier.height(Spacing.md))

                // ── Section: AI Formatting ────────────────────────────
                SettingsSectionCard {
                    SectionHeader(title = stringResource(R.string.settings_api_key_section_title))
                    AiModelSettingsSection(
                        userPreferences = userPreferences,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(Modifier.height(Spacing.md))

                // ── Section: Pro ──────────────────────────────────────
                SettingsSectionCard {
                    ProSettingsSection(
                        isPro = isPro,
                        restoreState = restoreState,
                        onUpgrade = onUpgrade,
                        onRestore = { proViewModel.restorePurchases() }
                    )
                }

                Spacer(Modifier.height(Spacing.md))

                // ── Section: Watch ────────────────────────────────────
                SettingsSectionCard {
                    SectionHeader(title = stringResource(R.string.settings_watch_section_title))
                    WatchConnectivitySection(isWatchConnected = isWatchConnected)
                }

                Spacer(Modifier.height(Spacing.md))

                // ── Section: Feature Comparison ───────────────────────
                SettingsSectionCard {
                    SectionHeader(
                        title = stringResource(R.string.settings_pro_feature_comparison)
                    )
                    FeatureComparisonTable()
                }

                Spacer(Modifier.height(Spacing.md))

                // ── Section: Data ─────────────────────────────────────
                SettingsSectionCard {
                    DataSection(
                        isPro = isPro,
                        exportState = exportState,
                        importState = importState,
                        onExport = {
                            scope.launch {
                                val json = proViewModel.exportReminders()
                                if (json != null) {
                                    onExport(json)
                                }
                            }
                        },
                        onImport = onImport,
                        onUpgrade = onUpgrade
                    )
                }

                Spacer(Modifier.height(Spacing.xl))
            }
        }
    }
}

/**
 * A card that groups a settings section for visual depth.
 *
 * Uses [MaterialTheme.colorScheme.surfaceContainer] for a subtle
 * elevated appearance against the `surfaceContainerLow` background.
 *
 * @param content The section content.
 */
@Composable
private fun SettingsSectionCard(
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(Spacing.md)
        ) {
            content()
        }
    }
}

/**
 * A styled section header with medium-emphasis typography.
 *
 * @param title The section title text.
 */
@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = Spacing.sm)
    )
}

/**
 * Displays the current Pro status, upgrade CTA, and restore purchases button.
 */
@Composable
private fun ProSettingsSection(
    isPro: Boolean,
    restoreState: RestoreState,
    onUpgrade: () -> Unit,
    onRestore: () -> Unit
) {
    val statusText = if (isPro) {
        stringResource(R.string.settings_pro_status_pro)
    } else {
        stringResource(R.string.settings_pro_status_free)
    }

    val statusContentDescription = stringResource(
        R.string.content_desc_pro_status_indicator,
        statusText
    )

    SectionHeader(title = stringResource(R.string.settings_pro_section_title))

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = statusContentDescription }
    ) {
        Text(
            text = stringResource(R.string.settings_pro_status_label),
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(Modifier.width(Spacing.md))
        Text(
            text = statusText,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Bold
            ),
            color = if (isPro) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )
    }

    Spacer(Modifier.height(Spacing.sm))

    if (!isPro) {
        val upgradeContentDescription = stringResource(R.string.content_desc_upgrade_button)
        Button(
            onClick = onUpgrade,
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = upgradeContentDescription
                },
            shape = MaterialTheme.shapes.large,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text(stringResource(R.string.settings_pro_upgrade_cta))
        }

        Spacer(Modifier.height(Spacing.sm))
    }

    val restoreContentDescription = stringResource(R.string.content_desc_restore_button)
    OutlinedButton(
        onClick = onRestore,
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = restoreContentDescription
            },
        shape = MaterialTheme.shapes.large
    ) {
        Text(stringResource(R.string.settings_pro_restore))
    }

    when (restoreState) {
        is RestoreState.Success -> {
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = stringResource(R.string.settings_pro_restore_success),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodySmall
            )
        }
        is RestoreState.NoPurchase -> {
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = stringResource(R.string.settings_pro_restore_no_purchase),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        }
        else -> {}
    }
}

/**
 * Displays a coloured dot indicating whether a WearOS watch is connected
 * and syncing with the phone.
 */
@Composable
private fun WatchConnectivitySection(
    isWatchConnected: Boolean
) {
    val statusLabel = if (isWatchConnected) {
        stringResource(R.string.settings_watch_connected)
    } else {
        stringResource(R.string.settings_watch_not_connected)
    }

    val statusLevel = if (isWatchConnected) StatusLevel.OK else StatusLevel.ERROR

    StatusIndicator(
        label = statusLabel,
        level = statusLevel
    )
}

/**
 * Feature comparison table showing Free vs Pro tiers.
 */
@Composable
private fun FeatureComparisonTable() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(modifier = Modifier.padding(TABLE_PADDING)) {
            // Header row
            FeatureRow(
                feature = stringResource(R.string.settings_pro_feature_header),
                freeValue = stringResource(R.string.settings_pro_free_header),
                proValue = stringResource(R.string.settings_pro_pro_header),
                isHeader = true
            )

            HorizontalDivider()

            FeatureRow(
                feature = stringResource(R.string.settings_pro_feature_time_reminders),
                freeValue = stringResource(R.string.settings_pro_unlimited),
                proValue = stringResource(R.string.settings_pro_unlimited)
            )

            FeatureRow(
                feature = stringResource(R.string.settings_pro_feature_voice),
                freeValue = stringResource(R.string.settings_pro_unlimited),
                proValue = stringResource(R.string.settings_pro_unlimited)
            )

            FeatureRow(
                feature = stringResource(R.string.settings_pro_feature_sync),
                freeValue = stringResource(R.string.settings_pro_value_yes),
                proValue = stringResource(R.string.settings_pro_value_yes)
            )

            FeatureRow(
                feature = stringResource(R.string.settings_pro_feature_formatting),
                freeValue = stringResource(R.string.settings_pro_feature_formatting_free),
                proValue = stringResource(R.string.settings_pro_feature_formatting_pro)
            )

            FeatureRow(
                feature = stringResource(R.string.settings_pro_feature_geofences),
                freeValue = stringResource(R.string.settings_pro_value_5),
                proValue = stringResource(R.string.settings_pro_value_100)
            )

            FeatureRow(
                feature = stringResource(R.string.settings_pro_feature_saved_places),
                freeValue = stringResource(R.string.settings_pro_value_2),
                proValue = stringResource(R.string.settings_pro_unlimited)
            )

            FeatureRow(
                feature = stringResource(R.string.settings_pro_feature_recurrence),
                freeValue = stringResource(R.string.settings_pro_value_dash),
                proValue = stringResource(R.string.settings_pro_value_yes)
            )

            FeatureRow(
                feature = stringResource(R.string.settings_pro_feature_snooze),
                freeValue = stringResource(R.string.settings_pro_value_dash),
                proValue = stringResource(R.string.settings_pro_value_yes)
            )

            FeatureRow(
                feature = stringResource(R.string.settings_pro_feature_radius),
                freeValue = stringResource(R.string.settings_pro_value_150m),
                proValue = stringResource(R.string.settings_pro_unlimited)
            )

            FeatureRow(
                feature = stringResource(R.string.settings_pro_feature_export),
                freeValue = stringResource(R.string.settings_pro_value_dash),
                proValue = stringResource(R.string.settings_pro_value_yes)
            )
        }
    }
}

/**
 * A single row in the feature comparison table.
 */
@Composable
private fun FeatureRow(
    feature: String,
    freeValue: String,
    proValue: String,
    isHeader: Boolean = false
) {
    val textStyle = if (isHeader) {
        MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
    } else {
        MaterialTheme.typography.bodyMedium
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = ROW_VERTICAL_PADDING),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = feature,
            style = textStyle,
            modifier = Modifier.weight(UiConstants.FEATURE_TABLE_FEATURE_WEIGHT)
        )
        Text(
            text = freeValue,
            style = textStyle,
            modifier = Modifier.weight(UiConstants.FEATURE_TABLE_VALUE_WEIGHT),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Text(
            text = proValue,
            style = textStyle,
            color = if (!isHeader) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(UiConstants.FEATURE_TABLE_VALUE_WEIGHT),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

/**
 * Export and import section. Pro-only with badge for free users.
 */
@Composable
private fun DataSection(
    isPro: Boolean,
    exportState: ExportState,
    importState: ImportState,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onUpgrade: () -> Unit
) {
    SectionHeader(
        title = stringResource(R.string.settings_pro_export_section_title)
    )

    val exportContentDescription = stringResource(R.string.content_desc_export_button)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Button(
            onClick = if (isPro) onExport else onUpgrade,
            enabled = exportState !is ExportState.Exporting,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier
                .weight(1f)
                .semantics {
                    contentDescription = exportContentDescription
                }
        ) {
            if (exportState is ExportState.Exporting) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(end = UiConstants.BUTTON_ICON_PADDING_DP.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
            Text(stringResource(R.string.settings_pro_export_button))
        }

        if (!isPro) {
            Spacer(Modifier.width(UiConstants.PRO_BADGE_SPACING_DP.dp))
            ProBadge()
        }
    }

    when (exportState) {
        is ExportState.Success -> {
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = stringResource(
                    R.string.settings_pro_export_success,
                    exportState.reminderCount,
                    exportState.savedPlaceCount
                ),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodySmall
            )
        }
        is ExportState.NoData -> {
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = stringResource(R.string.settings_pro_export_no_data),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        }
        is ExportState.Error -> {
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = exportState.message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
        else -> {}
    }

    Spacer(Modifier.height(Spacing.sm))

    val importContentDescription = stringResource(R.string.content_desc_import_button)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedButton(
            onClick = if (isPro) onImport else onUpgrade,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier
                .weight(1f)
                .semantics {
                    contentDescription = importContentDescription
                }
        ) {
            Text(stringResource(R.string.settings_pro_import_button))
        }

        if (!isPro) {
            Spacer(Modifier.width(UiConstants.PRO_BADGE_SPACING_DP.dp))
            ProBadge()
        }
    }

    when (importState) {
        is ImportState.Success -> {
            Spacer(Modifier.height(Spacing.xs))
            val message = if (importState.skippedCount > 0) {
                stringResource(
                    R.string.settings_pro_import_partial,
                    importState.reminderCount,
                    importState.savedPlaceCount,
                    importState.skippedCount
                )
            } else {
                stringResource(
                    R.string.settings_pro_import_success,
                    importState.reminderCount,
                    importState.savedPlaceCount
                )
            }
            Text(
                text = message,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodySmall
            )
        }
        is ImportState.Error -> {
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = importState.message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
        else -> {}
    }
}

/** Padding inside the feature comparison table card. */
private val TABLE_PADDING = 12.dp

/** Vertical padding between feature rows. */
private val ROW_VERTICAL_PADDING = 6.dp

// ── Previews ──────────────────────────────────────────────────────────

/** Preview of the settings screen. */
@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    com.example.reminders.ui.theme.RemindersTheme {
        Text("Settings screen — requires ViewModel")
    }
}
