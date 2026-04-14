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
import androidx.wear.compose.material3.AppCard
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.example.reminders.wear.R
import com.example.reminders.wear.ui.theme.WearConstants

/**
 * Compact input-method selector for the WearOS reminder creation flow.
 *
 * Displays two primary tappable areas — a keyboard icon on the left and
 * a mic icon with a dropdown chevron on the right. Tapping the mic area
 * expands an animated list of voice-based input options rendered as
 * wear cards: [InputMethod.VoiceOnWatch], [InputMethod.VoiceStreamToPhone],
 * [InputMethod.CloudFormatOnWatch].
 *
 * The cloud-format option is only enabled when [hasCloudApiConfigured] is
 * true; otherwise it appears dimmed with a subtitle explaining why.
 *
 * @param hasCloudApiConfigured Whether a cloud LLM API key has been
 *   configured on the watch.
 * @param onKeyboardSelected Callback invoked when the keyboard icon is tapped.
 * @param onInputMethodSelected Callback invoked when a voice-based input
 *   method is selected from the expanded list.
 * @param modifier Optional modifier applied to the root column.
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
                    .size(WearConstants.KeyboardIconTouchTarget)
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
                    modifier = Modifier.size(WearConstants.MicIconSize),
                    tint = MaterialTheme.colorScheme.primary
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    modifier = Modifier.size(WearConstants.ChevronIconSize),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (expanded) {
            Spacer(modifier = Modifier.height(WearConstants.OptionSpacing))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(WearConstants.OptionRowSpacing)
            ) {
                VoiceOptionCard(
                    icon = Icons.Default.Mic,
                    label = stringResource(R.string.input_method_voice_on_watch),
                    onClick = { onInputMethodSelected(InputMethod.VoiceOnWatch) }
                )

                VoiceOptionCard(
                    icon = Icons.Default.Phone,
                    label = stringResource(R.string.input_method_voice_stream_phone),
                    onClick = { onInputMethodSelected(InputMethod.VoiceStreamToPhone) }
                )

                val cloudAlpha = if (hasCloudApiConfigured) {
                    WearConstants.EnabledAlpha
                } else {
                    WearConstants.DisabledAlpha
                }

                AppCard(
                    onClick = { onInputMethodSelected(InputMethod.CloudFormatOnWatch) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(cloudAlpha),
                    enabled = hasCloudApiConfigured
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(WearConstants.OptionIconSize),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(WearConstants.OptionTextSpacing))
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
 * A single option row rendered as an [AppCard] in the expanded
 * voice-options list, containing an icon and a text label.
 *
 * @param icon   The leading icon for the option.
 * @param label  The human-readable option label.
 * @param onClick Callback when the option is tapped.
 */
@Composable
private fun VoiceOptionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    AppCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(WearConstants.OptionIconSize),
            tint = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.width(WearConstants.OptionTextSpacing))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall
        )
    }
}
