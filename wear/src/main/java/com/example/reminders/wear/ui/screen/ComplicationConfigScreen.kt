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
import com.example.reminders.wear.complication.ComplicationMode
import com.example.reminders.wear.complication.ComplicationPreferences
import com.example.reminders.wear.ui.theme.WearSpacing
import kotlinx.coroutines.launch

private const val TAG = "ComplicationConfigScreen"

/**
 * Configuration screen for the watch-face complication.
 *
 * Allows the user to choose between showing only today's reminders
 * or all upcoming reminders in the complication data.
 *
 * @param preferences Manages the complication display-mode preference.
 */
@Composable
fun ComplicationConfigScreen(
    preferences: ComplicationPreferences
) {
    val currentMode by preferences.complicationMode.collectAsState(
        initial = ComplicationMode.TODAY
    )
    val coroutineScope = rememberCoroutineScope()

    val listState = rememberTransformingLazyColumnState()

    ScreenScaffold(scrollState = listState) { contentPadding ->
        TransformingLazyColumn(contentPadding = contentPadding, state = listState) {
            item {
                ListHeader {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            item {
                ComplicationModeOption(
                    label = stringResource(R.string.complication_mode_today),
                    selected = currentMode == ComplicationMode.TODAY,
                    onClick = {
                        if (currentMode != ComplicationMode.TODAY) {
                            Log.i(TAG, "Complication mode changed to TODAY")
                            coroutineScope.launch {
                                preferences.setComplicationMode(ComplicationMode.TODAY)
                            }
                        }
                    }
                )
            }

            item {
                ComplicationModeOption(
                    label = stringResource(R.string.complication_mode_all),
                    selected = currentMode == ComplicationMode.ALL_UPCOMING,
                    onClick = {
                        if (currentMode != ComplicationMode.ALL_UPCOMING) {
                            Log.i(TAG, "Complication mode changed to ALL_UPCOMING")
                            coroutineScope.launch {
                                preferences.setComplicationMode(ComplicationMode.ALL_UPCOMING)
                            }
                        }
                    }
                )
            }
        }
    }
}

/**
 * A single complication mode option rendered as a styled [RadioButton].
 *
 * Uses the theme primary colour for the selected state.
 */
@Composable
private fun ComplicationModeOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    RadioButton(
        selected = selected,
        onSelect = onClick,
        label = { Text(text = label) },
        colors = RadioButtonDefaults.radioButtonColors(),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = WearSpacing.Xs)
    )
}
