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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.reminders.R
import com.example.reminders.ui.component.ProBadge
import com.example.reminders.ui.viewmodel.ExportState
import com.example.reminders.ui.viewmodel.ImportState
import com.example.reminders.ui.viewmodel.ProSettingsViewModel
import com.example.reminders.ui.viewmodel.RestoreState
import kotlinx.coroutines.launch

/**
 * Settings screen where the user can configure their Gemini API key,
 * view Pro status, upgrade, restore purchases, and export/import data.
 *
 * The API key is stored via [com.example.reminders.data.preferences.UserPreferences]
 * and is never logged or displayed in plain text by default.
 *
 * @param currentApiKey  The currently stored API key, or null if not set.
 * @param proViewModel   ViewModel managing Pro status, export/import, and billing.
 * @param onSaveApiKey   Callback invoked when the user saves a new API key.
 * @param onBack         Callback invoked when the user presses the back button.
 * @param onUpgrade      Callback that launches the Play Billing purchase flow.
 * @param onExport       Callback that receives the exported JSON for sharing/saving.
 * @param onImport       Callback that triggers the file picker for import.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentApiKey: String?,
    proViewModel: ProSettingsViewModel,
    onSaveApiKey: (String) -> Unit,
    onBack: () -> Unit,
    onUpgrade: () -> Unit,
    onExport: (String) -> Unit,
    onImport: () -> Unit
) {
    var apiKeyInput by rememberSaveable { mutableStateOf(currentApiKey ?: "") }
    var isKeyVisible by rememberSaveable { mutableStateOf(false) }
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
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
                .imePadding()
        ) {
            Spacer(Modifier.height(16.dp))

            // ---------- Formatting section ----------
            Text(
                text = stringResource(R.string.settings_api_key_section_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            OutlinedTextField(
                value = apiKeyInput,
                onValueChange = { apiKeyInput = it },
                label = { Text(stringResource(R.string.settings_api_key_label)) },
                placeholder = { Text(stringResource(R.string.settings_api_key_placeholder)) },
                singleLine = true,
                visualTransformation = if (isKeyVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                trailingIcon = {
                    IconButton(onClick = { isKeyVisible = !isKeyVisible }) {
                        Icon(
                            imageVector = if (isKeyVisible) {
                                Icons.Filled.VisibilityOff
                            } else {
                                Icons.Filled.Visibility
                            },
                            contentDescription = if (isKeyVisible) {
                                stringResource(R.string.settings_hide_api_key)
                            } else {
                                stringResource(R.string.settings_show_api_key)
                            }
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { onSaveApiKey(apiKeyInput.trim()) }
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.settings_api_key_help),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            HorizontalDivider()

            Spacer(Modifier.height(16.dp))

            // ---------- Pro section ----------
            ProSettingsSection(
                isPro = isPro,
                restoreState = restoreState,
                onUpgrade = onUpgrade,
                onRestore = { proViewModel.restorePurchases() }
            )

            Spacer(Modifier.height(16.dp))

            HorizontalDivider()

            Spacer(Modifier.height(16.dp))

            // ---------- Feature comparison table ----------
            FeatureComparisonTable()

            Spacer(Modifier.height(16.dp))

            HorizontalDivider()

            Spacer(Modifier.height(16.dp))

            // ---------- Export / Import section ----------
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

            Spacer(Modifier.height(32.dp))
        }
    }
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

    Text(
        text = stringResource(R.string.settings_pro_section_title),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(bottom = 12.dp)
    )

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
        Spacer(Modifier.width(16.dp))
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

    Spacer(Modifier.height(12.dp))

    if (!isPro) {
        Button(
            onClick = onUpgrade,
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = stringResource(R.string.content_desc_upgrade_button)
                }
        ) {
            Text(stringResource(R.string.settings_pro_upgrade_cta))
        }

        Spacer(Modifier.height(8.dp))
    }

    OutlinedButton(
        onClick = onRestore,
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = stringResource(R.string.content_desc_restore_button)
            }
    ) {
        Text(stringResource(R.string.settings_pro_restore))
    }

    when (restoreState) {
        is RestoreState.Success -> {
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.settings_pro_restore_success),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodySmall
            )
        }
        is RestoreState.NoPurchase -> {
            Spacer(Modifier.height(4.dp))
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
 * Feature comparison table showing Free vs Pro tiers.
 */
@Composable
private fun FeatureComparisonTable() {
    Text(
        text = stringResource(R.string.settings_pro_feature_comparison),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(bottom = 12.dp)
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
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
                freeValue = "Yes",
                proValue = "Yes"
            )

            FeatureRow(
                feature = stringResource(R.string.settings_pro_feature_formatting),
                freeValue = stringResource(R.string.settings_pro_feature_formatting_free),
                proValue = stringResource(R.string.settings_pro_feature_formatting_pro)
            )

            FeatureRow(
                feature = stringResource(R.string.settings_pro_feature_geofences),
                freeValue = "5",
                proValue = "100"
            )

            FeatureRow(
                feature = stringResource(R.string.settings_pro_feature_saved_places),
                freeValue = "2",
                proValue = stringResource(R.string.settings_pro_unlimited)
            )

            FeatureRow(
                feature = stringResource(R.string.settings_pro_feature_recurrence),
                freeValue = "—",
                proValue = "Yes"
            )

            FeatureRow(
                feature = stringResource(R.string.settings_pro_feature_snooze),
                freeValue = "—",
                proValue = "Yes"
            )

            FeatureRow(
                feature = stringResource(R.string.settings_pro_feature_radius),
                freeValue = "150m",
                proValue = stringResource(R.string.settings_pro_unlimited)
            )

            FeatureRow(
                feature = stringResource(R.string.settings_pro_feature_export),
                freeValue = "—",
                proValue = "Yes"
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
            modifier = Modifier.weight(FEATURE_WEIGHT)
        )
        Text(
            text = freeValue,
            style = textStyle,
            modifier = Modifier.weight(VALUE_WEIGHT),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Text(
            text = proValue,
            style = textStyle,
            color = if (!isHeader) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(VALUE_WEIGHT),
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
    Text(
        text = stringResource(R.string.settings_pro_export_section_title),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(bottom = 12.dp)
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Button(
            onClick = if (isPro) onExport else onUpgrade,
            enabled = exportState !is ExportState.Exporting,
            modifier = Modifier
                .weight(1f)
                .semantics {
                    contentDescription = stringResource(R.string.content_desc_export_button)
                }
        ) {
            if (exportState is ExportState.Exporting) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(end = BUTTON_ICON_PADDING),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
            Text(stringResource(R.string.settings_pro_export_button))
        }

        if (!isPro) {
            Spacer(Modifier.width(PRO_BADGE_SPACING))
            ProBadge()
        }
    }

    when (exportState) {
        is ExportState.Success -> {
            Spacer(Modifier.height(4.dp))
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
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.settings_pro_export_no_data),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        }
        is ExportState.Error -> {
            Spacer(Modifier.height(4.dp))
            Text(
                text = exportState.message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
        else -> {}
    }

    Spacer(Modifier.height(8.dp))

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedButton(
            onClick = if (isPro) onImport else onUpgrade,
            modifier = Modifier
                .weight(1f)
                .semantics {
                    contentDescription = stringResource(R.string.content_desc_import_button)
                }
        ) {
            Text(stringResource(R.string.settings_pro_import_button))
        }

        if (!isPro) {
            Spacer(Modifier.width(PRO_BADGE_SPACING))
            ProBadge()
        }
    }

    when (importState) {
        is ImportState.Success -> {
            Spacer(Modifier.height(4.dp))
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
            Spacer(Modifier.height(4.dp))
            Text(
                text = importState.message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
        else -> {}
    }
}

private val TABLE_PADDING = 12.dp
private val ROW_VERTICAL_PADDING = 6.dp
private const val FEATURE_WEIGHT = 2f
private const val VALUE_WEIGHT = 1f
private val PRO_BADGE_SPACING = 8.dp
private val BUTTON_ICON_PADDING = 4.dp
