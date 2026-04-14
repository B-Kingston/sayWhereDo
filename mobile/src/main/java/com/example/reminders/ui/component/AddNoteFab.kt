package com.example.reminders.ui.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.reminders.R
import com.example.reminders.ui.theme.Spacing
import com.example.reminders.ui.theme.UiConstants

/**
 * A dual-FAB row that lets the user choose between keyboard input and
 * voice transcription.
 *
 * The keyboard FAB immediately invokes [onKeyboardSelected]. The mic FAB
 * toggles a dropdown menu ([MicMethodPicker]) with available voice input
 * methods. When the user selects a method the menu calls
 * [onMicMethodSelected] and dismisses itself.
 *
 * @param onKeyboardSelected  Called when the keyboard (edit) FAB is tapped.
 * @param onMicMethodSelected Called with the chosen [MicMethod].
 * @param hasCloudProvider    Whether a cloud transcription provider is configured.
 * @param hasLocalModel       Whether a local on-device model is available.
 * @param cloudProviderName   Display name for the cloud provider (ignored unless [hasCloudProvider]).
 * @param localModelName      Display name for the local model (ignored unless [hasLocalModel]).
 * @param modifier            Optional layout modifier.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddNoteFab(
    onKeyboardSelected: () -> Unit,
    onMicMethodSelected: (MicMethod) -> Unit,
    hasCloudProvider: Boolean = false,
    hasLocalModel: Boolean = false,
    cloudProviderName: String = "",
    localModelName: String = "",
    modifier: Modifier = Modifier
) {
    var micMenuExpanded by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        SmallFloatingActionButton(
            onClick = onKeyboardSelected,
            shape = MaterialTheme.shapes.large,
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        ) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = stringResource(R.string.input_method_keyboard)
            )
        }

        Spacer(modifier = Modifier.width(UiConstants.FAB_SPACING_DP.dp))

        SmallFloatingActionButton(
            onClick = { micMenuExpanded = !micMenuExpanded },
            shape = MaterialTheme.shapes.large,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = stringResource(R.string.input_method_mic)
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null
            )
        }

        MicMethodPicker(
            expanded = micMenuExpanded,
            onDismissRequest = { micMenuExpanded = false },
            onMicMethodSelected = { method ->
                micMenuExpanded = false
                onMicMethodSelected(method)
            },
            hasCloudProvider = hasCloudProvider,
            hasLocalModel = hasLocalModel,
            cloudProviderName = cloudProviderName,
            localModelName = localModelName
        )
    }
}

/**
 * Dropdown menu listing the available voice input methods.
 *
 * Always shows the Android built-in option. Conditionally shows a cloud
 * provider option and a local model option based on configuration.
 *
 * @param expanded           Whether the menu is currently visible.
 * @param onDismissRequest   Called when the menu should close.
 * @param onMicMethodSelected Called with the user's chosen [MicMethod].
 * @param hasCloudProvider   Whether to show the cloud provider option.
 * @param hasLocalModel      Whether to show the local model option.
 * @param cloudProviderName  Display name for the cloud provider.
 * @param localModelName     Display name for the local model.
 */
@Composable
private fun MicMethodPicker(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    onMicMethodSelected: (MicMethod) -> Unit,
    hasCloudProvider: Boolean,
    hasLocalModel: Boolean,
    cloudProviderName: String,
    localModelName: String
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        shape = MaterialTheme.shapes.medium
    ) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.input_method_android_builtin)) },
            onClick = { onMicMethodSelected(MicMethod.AndroidBuiltIn) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = null
                )
            }
        )

        if (hasCloudProvider) {
            DropdownMenuItem(
                text = {
                    Text(stringResource(R.string.input_method_cloud_provider, cloudProviderName))
                },
                onClick = { onMicMethodSelected(MicMethod.CloudProvider(cloudProviderName)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = null
                    )
                }
            )
        }

        if (hasLocalModel) {
            DropdownMenuItem(
                text = {
                    Text(stringResource(R.string.input_method_local_model, localModelName))
                },
                onClick = { onMicMethodSelected(MicMethod.LocalModel) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = null
                    )
                }
            )
        }
    }
}

// ── Previews ──────────────────────────────────────────────────────────

/** Preview of the AddNoteFab. */
@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun AddNoteFabPreview() {
    com.example.reminders.ui.theme.RemindersTheme {
        AddNoteFab(
            onKeyboardSelected = {},
            onMicMethodSelected = {},
            hasCloudProvider = false,
            hasLocalModel = false
        )
    }
}
