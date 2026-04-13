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
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.RadioButton
import androidx.wear.compose.material3.RadioButtonDefaults
import androidx.wear.compose.material3.Text
import com.example.reminders.wear.R
import com.example.reminders.wear.geofence.GeofencingDevice
import com.example.reminders.wear.geofence.GeofencingDeviceManager
import kotlinx.coroutines.launch

/**
 * Settings screen for the WearOS watch.
 *
 * Displays the phone connectivity status (green/red dot) and the
 * geofencing device preference (Auto / Phone only / Watch only).
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

    TransformingLazyColumn {
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
            PhoneConnectivityRow(isPhoneConnected = isPhoneConnected)
        }

        // ---------- Geofencing preference ----------
        item {
            Spacer(Modifier.height(12.dp))
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

/**
 * Displays a green or red dot alongside text indicating whether the
 * companion phone is currently connected.
 */
@Composable
private fun PhoneConnectivityRow(
    isPhoneConnected: Boolean
) {
    val label = if (isPhoneConnected) {
        stringResource(R.string.settings_phone_connected)
    } else {
        stringResource(R.string.settings_phone_not_connected)
    }

    val dotColor = if (isPhoneConnected) {
        ConnectedGreen
    } else {
        DisconnectedRed
    }

    val semanticsDescription = if (isPhoneConnected) {
        stringResource(R.string.settings_phone_connected)
    } else {
        stringResource(R.string.settings_phone_not_connected)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = semanticsDescription }
    ) {
        Box(
            modifier = Modifier
                .size(DotSize)
                .background(color = dotColor, shape = CircleShape)
        )

        Spacer(Modifier.size(DotLabelSpacing))

        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * A single geofencing device option rendered as a [RadioButton].
 *
 * Duplicated here (instead of reusing [GeofencingPreferenceScreen]'s
 * private composable) so the settings screen is self-contained.
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
        colors = RadioButtonDefaults.radioButtonColors(),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    )
}

private val DotSize = 10.dp
private val DotLabelSpacing = 8.dp

/** Material Design green for connected state. */
private val ConnectedGreen = androidx.compose.ui.graphics.Color(0xFF4CAF50)

/** Material Design red for disconnected state. */
private val DisconnectedRed = androidx.compose.ui.graphics.Color(0xFFF44336)
