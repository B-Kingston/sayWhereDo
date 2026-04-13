package com.example.reminders.wear.ui.component

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Phone
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.example.reminders.wear.R

/**
 * Compact input-method selector for the WearOS reminder creation flow.
 *
 * Displays two primary tappable areas — a keyboard icon on the left and a
 * mic icon with a dropdown chevron on the right. Tapping the mic area
 * expands an animated list of voice-based input options
 * ([InputMethod.VoiceOnWatch], [InputMethod.VoiceStreamToPhone],
 * [InputMethod.CloudFormatOnWatch]).
 *
 * The cloud-format option is only enabled when [hasCloudApiConfigured] is
 * true; otherwise it appears dimmed with a subtitle explaining why.
 *
 * @param hasCloudApiConfigured Whether a cloud LLM API key has been
 *   configured on the watch. Controls the enabled state of the
 *   [InputMethod.CloudFormatOnWatch] option.
 * @param onKeyboardSelected Callback invoked when the keyboard icon is
 *   tapped.
 * @param onInputMethodSelected Callback invoked when a voice-based input
 *   method is selected from the expanded list.
 */
@Composable
fun InputMethodSelector(
    hasCloudApiConfigured: Boolean = false,
    onKeyboardSelected: () -> Unit,
    onInputMethodSelected: (InputMethod) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.animateContentSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = stringResource(R.string.input_method_keyboard),
                modifier = Modifier
                    .size(KEYBOARD_ICON_TOUCH_TARGET)
                    .clickable(
                        role = Role.Button,
                        onClick = onKeyboardSelected
                    ),
                tint = MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier.clickable(
                    role = Role.Button,
                    onClick = { expanded = !expanded }
                ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = stringResource(R.string.input_method_select),
                    modifier = Modifier.size(MIC_ICON_SIZE),
                    tint = MaterialTheme.colorScheme.primary
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    modifier = Modifier.size(CHEVRON_ICON_SIZE),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (expanded) {
            Spacer(modifier = Modifier.height(OPTION_SPACING))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(OPTION_ROW_SPACING)
            ) {
                VoiceOptionRow(
                    icon = Icons.Default.Mic,
                    label = stringResource(R.string.input_method_voice_on_watch),
                    onClick = { onInputMethodSelected(InputMethod.VoiceOnWatch) }
                )

                VoiceOptionRow(
                    icon = Icons.Default.Phone,
                    label = stringResource(R.string.input_method_voice_stream_phone),
                    onClick = { onInputMethodSelected(InputMethod.VoiceStreamToPhone) }
                )

                val cloudAlpha = if (hasCloudApiConfigured) ENABLED_ALPHA else DISABLED_ALPHA
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(cloudAlpha)
                        .clickable(
                            enabled = hasCloudApiConfigured,
                            role = Role.Button,
                            onClick = { onInputMethodSelected(InputMethod.CloudFormatOnWatch) }
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(OPTION_ICON_SIZE),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(OPTION_TEXT_SPACING))
                    Column {
                        Text(
                            text = stringResource(R.string.input_method_cloud_format),
                            style = MaterialTheme.typography.bodySmall
                        )
                        if (!hasCloudApiConfigured) {
                            Text(
                                text = stringResource(R.string.input_method_no_api_configured),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * A single row in the expanded voice-options list, containing an icon and a
 * text label that invokes [onClick] when tapped.
 */
@Composable
private fun VoiceOptionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                role = Role.Button,
                onClick = onClick
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(OPTION_ICON_SIZE),
            tint = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.width(OPTION_TEXT_SPACING))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

private val KEYBOARD_ICON_TOUCH_TARGET = 48.dp
private val MIC_ICON_SIZE = 32.dp
private val CHEVRON_ICON_SIZE = 20.dp
private val OPTION_ICON_SIZE = 24.dp
private val OPTION_SPACING = 8.dp
private val OPTION_ROW_SPACING = 6.dp
private val OPTION_TEXT_SPACING = 8.dp
private const val ENABLED_ALPHA = 1.0f
private const val DISABLED_ALPHA = 0.4f
