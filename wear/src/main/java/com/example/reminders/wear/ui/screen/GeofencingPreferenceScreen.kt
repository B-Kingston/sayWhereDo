package com.example.reminders.wear.ui.screen

import android.util.Log
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.RadioButton
import androidx.wear.compose.material3.RadioButtonDefaults
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.example.reminders.wear.R
import com.example.reminders.wear.geofence.GeofencingDevice
import com.example.reminders.wear.geofence.GeofencingDeviceManager
import com.example.reminders.wear.ui.theme.WearSpacing
import kotlinx.coroutines.launch

private const val TAG = "GeofencePrefScreen"

/**
 * Standalone screen for selecting which device should manage geofences.
 *
 * Shown on first location-reminder creation when GPS hardware is
 * detected. The user can choose between AUTO (recommended),
 * PHONE_ONLY, or WATCH_ONLY. Options are grouped inside a scrollable
 * list with consistent card-style radio buttons.
 *
 * @param deviceManager Manages the geofencing device preference.
 * @param hasGps        Whether the watch has GPS hardware.
 */
@Composable
fun GeofencingPreferenceScreen(
    deviceManager: GeofencingDeviceManager,
    hasGps: Boolean
) {
    val currentPreference by deviceManager.devicePreference.collectAsState(
        initial = GeofencingDevice.AUTO
    )
    val coroutineScope = rememberCoroutineScope()

    val listState = rememberTransformingLazyColumnState()

    ScreenScaffold(scrollState = listState) { contentPadding ->
        TransformingLazyColumn(contentPadding = contentPadding, state = listState) {
            item {
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
                            Log.i(TAG, "Geofencing preference changed to AUTO")
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
                            Log.i(TAG, "Geofencing preference changed to PHONE_ONLY")
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
                                Log.i(TAG, "Geofencing preference changed to WATCH_ONLY")
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
 * A single geofencing device option rendered as a styled [RadioButton].
 *
 * Uses theme primary colour for the selected state to maintain
 * visual consistency across the app.
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = WearSpacing.Xs)
    )
}
