package com.example.reminders.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.reminders.R
import com.example.reminders.ml.AvailableModels
import com.example.reminders.ml.CapabilityLevel
import com.example.reminders.ml.ModelInfo
import com.example.reminders.ui.theme.Spacing

/**
 * Settings section for managing the on-device LLM model.
 *
 * Displays a device capability indicator (green/yellow/red circle),
 * a model selector dropdown, download/delete buttons, and a privacy card.
 * All wrapped in a card for visual depth.
 *
 * @param capabilityLevel  The device's capability assessment.
 * @param downloadedModelId The ID of the currently downloaded model, or null.
 * @param downloadProgress Current download progress (0..1), or null if not downloading.
 * @param onDownloadModel  Callback to download the selected model.
 * @param onDeleteModel    Callback to delete the downloaded model.
 * @param modifier         Optional modifier for layout customisation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalModelSettingsSection(
    capabilityLevel: CapabilityLevel,
    downloadedModelId: String?,
    downloadProgress: Float?,
    onDownloadModel: (ModelInfo) -> Unit,
    onDeleteModel: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            CapabilityIndicator(capabilityLevel = capabilityLevel)

            Spacer(modifier = Modifier.height(Spacing.sm))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                )
            ) {
                Text(
                    text = stringResource(R.string.ai_model_settings_local_privacy),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(Spacing.sm)
                )
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            var selectedModel by remember {
                mutableStateOf(AvailableModels.GEMMA_2_2B_Q4)
            }
            var expanded by remember { mutableStateOf(false) }

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = selectedModel.name,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.local_model_select)) },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    shape = MaterialTheme.shapes.medium
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    shape = MaterialTheme.shapes.medium
                ) {
                    AvailableModels.ALL.forEach { model ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    stringResource(
                                        R.string.local_model_name_with_size,
                                        model.name,
                                        model.fileSizeDisplay
                                    )
                                )
                            },
                            onClick = {
                                selectedModel = model
                                expanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            if (downloadProgress != null) {
                LinearProgressIndicator(
                    progress = { downloadProgress },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                )
                Spacer(modifier = Modifier.height(Spacing.sm))
                Text(
                    text = stringResource(
                        R.string.local_model_downloading,
                        (downloadProgress * PERCENTAGE_MULTIPLIER).toInt()
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else if (downloadedModelId != null) {
                Text(
                    text = stringResource(R.string.local_model_downloaded),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(Spacing.sm))
                OutlinedButton(
                    onClick = { onDeleteModel(downloadedModelId) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large
                ) {
                    Text(stringResource(R.string.local_model_delete))
                }
            } else {
                Button(
                    onClick = { onDownloadModel(selectedModel) },
                    enabled = capabilityLevel != CapabilityLevel.NOT_SUPPORTED,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large
                ) {
                    Text(
                        stringResource(
                            R.string.local_model_download,
                            selectedModel.fileSizeDisplay
                        )
                    )
                }
            }
        }
    }
}

/**
 * Shows a coloured circle and description label indicating the device's
 * capability level for on-device LLM inference.
 */
@Composable
private fun CapabilityIndicator(capabilityLevel: CapabilityLevel) {
    val indicatorColor = when (capabilityLevel) {
        CapabilityLevel.RECOMMENDED -> MaterialTheme.colorScheme.primary
        CapabilityLevel.MINIMUM -> MaterialTheme.colorScheme.tertiary
        CapabilityLevel.NOT_SUPPORTED -> MaterialTheme.colorScheme.error
    }

    val descriptionText = when (capabilityLevel) {
        CapabilityLevel.RECOMMENDED ->
            stringResource(R.string.ai_model_settings_capability_recommended)
        CapabilityLevel.MINIMUM ->
            stringResource(R.string.ai_model_settings_capability_minimum)
        CapabilityLevel.NOT_SUPPORTED ->
            stringResource(R.string.ai_model_settings_capability_unsupported)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(INDICATOR_SIZE)
                .clip(CircleShape)
                .background(indicatorColor)
        )
        Spacer(modifier = Modifier.width(Spacing.sm))
        Text(
            text = descriptionText,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

private val INDICATOR_SIZE = 16.dp
private const val PERCENTAGE_MULTIPLIER = 100

// ── Previews ──────────────────────────────────────────────────────────

/** Preview of the local model settings section. */
@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun LocalModelSettingsPreview() {
    com.example.reminders.ui.theme.RemindersTheme {
        Text("Local model settings — requires ModelInfo")
    }
}
