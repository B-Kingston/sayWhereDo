package com.example.reminders.wear.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AppCard
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.RadioButton
import androidx.wear.compose.material3.RadioButtonDefaults
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.example.reminders.wear.R
import com.example.reminders.wear.geofence.GeofencingDevice
import com.example.reminders.wear.geofence.GeofencingDeviceManager
import com.example.reminders.wear.ui.theme.StatusConnected
import com.example.reminders.wear.ui.theme.StatusDisconnected
import com.example.reminders.wear.ui.theme.WearConstants
import com.example.reminders.wear.ui.theme.WearSpacing
import kotlinx.coroutines.launch

/**
 * Settings screen for the WearOS watch.
 *
 * Displays the phone connectivity status (green/red dot) inside a card
 * and the geofencing device preference as grouped radio buttons.
 * The geofencing options are shown directly on this screen so users
 * can find all watch settings in one place.
 *
 * @param isPhoneConnected Whether the companion phone app is reachable.
 * @param deviceManager    Manages the geofencing device preference.
 * @param hasGps           Whether the watch has GPS hardware.
 */
@Composable
fun WatchSettingsScreen(
    isPhoneConnected: Boolean,
    deviceManager: GeofencingDeviceManager,
    hasGps: Boolean
) {
    val coroutineScope = rememberCoroutineScope()
    val currentPreference by deviceManager.devicePreference.collectAsState(
        initial = GeofencingDevice.AUTO
    )

    val listState = rememberTransformingLazyColumnState()

    ScreenScaffold(scrollState = listState) { contentPadding ->
        TransformingLazyColumn(contentPadding = contentPadding, state = listState) {
            // ---------- Section header ----------
            item {
                ListHeader {
                    Text(
                        text = stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            // ---------- Phone connectivity ----------
            item {
                PhoneConnectivityCard(isPhoneConnected = isPhoneConnected)
            }

            // ---------- Geofencing preference ----------
            item {
                Spacer(Modifier.height(WearSpacing.Lg))
                ListHeader {
                    Text(
                        text = stringResource(R.string.geofencing_preference_title),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            item {
                GeofenceDeviceOption(
                    label = stringResource(R.string.geofencing_auto),
                    description = stringResource(R.string.geofencing_auto_desc),
                    selected = currentPreference == GeofencingDevice.AUTO,
                    onClick = {
                        if (currentPreference != GeofencingDevice.AUTO) {
                            coroutineScope.launch {
                                deviceManager.setDevicePreference(GeofencingDevice.AUTO)
                            }
                        }
                    }
                )
            }

            item {
                GeofenceDeviceOption(
                    label = stringResource(R.string.geofencing_phone_only),
                    description = stringResource(R.string.geofencing_phone_only_desc),
                    selected = currentPreference == GeofencingDevice.PHONE_ONLY,
                    onClick = {
                        if (currentPreference != GeofencingDevice.PHONE_ONLY) {
                            coroutineScope.launch {
                                deviceManager.setDevicePreference(GeofencingDevice.PHONE_ONLY)
                            }
                        }
                    }
                )
            }

            if (hasGps) {
                item {
                    GeofenceDeviceOption(
                        label = stringResource(R.string.geofencing_watch_only),
                        description = stringResource(R.string.geofencing_watch_only_desc),
                        selected = currentPreference == GeofencingDevice.WATCH_ONLY,
                        onClick = {
                            if (currentPreference != GeofencingDevice.WATCH_ONLY) {
                                coroutineScope.launch {
                                    deviceManager.setDevicePreference(GeofencingDevice.WATCH_ONLY)
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

/**
 * Card displaying phone connectivity status with a coloured dot.
 *
 * Uses theme status colours (green for connected, red for disconnected)
 * from the design system.
 */
@Composable
private fun PhoneConnectivityCard(isPhoneConnected: Boolean) {
    val label = if (isPhoneConnected) {
        stringResource(R.string.settings_phone_connected)
    } else {
        stringResource(R.string.settings_phone_not_connected)
    }

    val dotColor = if (isPhoneConnected) StatusConnected else StatusDisconnected

    val semanticsDescription = if (isPhoneConnected) {
        stringResource(R.string.settings_phone_connected)
    } else {
        stringResource(R.string.settings_phone_not_connected)
    }

    AppCard(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = semanticsDescription }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(WearConstants.DotSize)
                    .background(color = dotColor, shape = CircleShape)
            )

            Spacer(Modifier.size(WearConstants.DotLabelSpacing))

            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * A single geofencing device option rendered as a styled [RadioButton].
 *
 * Uses theme colours for the radio button to maintain visual consistency
 * with the teal accent palette.
 */
@Composable
private fun GeofenceDeviceOption(
    label: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    RadioButton(
        selected = selected,
        onSelect = onClick,
        label = { Text(text = label) },
        secondaryLabel = { Text(text = description) },
        colors = RadioButtonDefaults.radioButtonColors(
            selectedColor = MaterialTheme.colorScheme.primary
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = WearSpacing.Xs)
    )
}
