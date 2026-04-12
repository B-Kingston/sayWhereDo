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
import androidx.compose.ui.unit.dp
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
import kotlinx.coroutines.launch

private const val TAG = "ComplicationConfigScreen"

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
            .padding(vertical = 4.dp)
    )
}
