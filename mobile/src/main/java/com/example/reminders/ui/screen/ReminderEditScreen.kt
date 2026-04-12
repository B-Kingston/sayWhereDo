package com.example.reminders.ui.screen

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.reminders.R
import com.example.reminders.alarm.RecurrencePattern
import com.example.reminders.ui.viewmodel.ReminderEditUiState
import com.example.reminders.ui.viewmodel.ReminderEditViewModel
import com.example.reminders.ui.viewmodel.ReminderEditViewModelFactory
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Screen for editing a reminder's time, recurrence, and geofence radius.
 *
 * Pro-gated features:
 * - **Recurrence**: Free users see "Upgrade to set recurrence" CTA.
 * - **Custom radius**: Free users see a fixed 150m radius with the slider disabled.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderEditScreen(
    viewModel: ReminderEditViewModel,
    onBack: () -> Unit,
    onUpgradeToPro: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.edit_reminder_title)) },
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
        when (val state = uiState) {
            is ReminderEditUiState.Loading -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = stringResource(R.string.loading_reminder))
                }
            }

            is ReminderEditUiState.NotFound -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = stringResource(R.string.reminder_not_found))
                }
            }

            is ReminderEditUiState.Saved -> onBack()

            is ReminderEditUiState.Ready -> {
                ReadyContent(
                    state = state,
                    onDateSelected = viewModel::onDateSelected,
                    onTimeSelected = viewModel::onTimeSelected,
                    onRecurrenceSelected = viewModel::onRecurrenceSelected,
                    onRadiusChanged = viewModel::onRadiusChanged,
                    onSave = viewModel::save,
                    onUpgradeToPro = onUpgradeToPro
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReadyContent(
    state: ReminderEditUiState.Ready,
    onDateSelected: (LocalDate?) -> Unit,
    onTimeSelected: (LocalTime?) -> Unit,
    onRecurrenceSelected: (RecurrencePattern?) -> Unit,
    onRadiusChanged: (Int) -> Unit,
    onSave: () -> Unit,
    onUpgradeToPro: () -> Unit
) {
    val context = LocalContext.current
    val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
    val timeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Title (read-only display)
        OutlinedTextField(
            value = state.title,
            onValueChange = {},
            label = { Text(text = stringResource(R.string.reminder_title_label)) },
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            enabled = false
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Trigger Date picker
        var showDatePicker by remember { mutableStateOf(false) }
        OutlinedTextField(
            value = state.triggerDate?.format(dateFormatter) ?: "",
            onValueChange = {},
            label = { Text(text = stringResource(R.string.trigger_date_label)) },
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            interactionSource = remember { mutableStateOf(
                androidx.compose.foundation.interaction.MutableInteractionSource()
            ) }.value,
            trailingIcon = {
                if (state.triggerDate != null) {
                    IconButton(onClick = { onDateSelected(null) }) {
                        Text("✕")
                    }
                }
            }
        )
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedButton(
            onClick = { showDatePicker = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(R.string.pick_date))
        }

        if (showDatePicker && state.triggerDate != null) {
            DatePickerDialog(
                context,
                { _, year, month, dayOfMonth ->
                    onDateSelected(LocalDate.of(year, month + 1, dayOfMonth))
                    showDatePicker = false
                },
                state.triggerDate.year,
                state.triggerDate.monthValue - 1,
                state.triggerDate.dayOfMonth
            ).apply {
                setOnCancelListener { showDatePicker = false }
                show()
            }
        } else if (showDatePicker) {
            val today = LocalDate.now()
            DatePickerDialog(
                context,
                { _, year, month, dayOfMonth ->
                    onDateSelected(LocalDate.of(year, month + 1, dayOfMonth))
                    showDatePicker = false
                },
                today.year,
                today.monthValue - 1,
                today.dayOfMonth
            ).apply {
                setOnCancelListener { showDatePicker = false }
                show()
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Trigger Time picker
        var showTimePicker by remember { mutableStateOf(false) }
        OutlinedTextField(
            value = state.triggerTime?.format(timeFormatter) ?: "",
            onValueChange = {},
            label = { Text(text = stringResource(R.string.trigger_time_label)) },
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            trailingIcon = {
                if (state.triggerTime != null) {
                    IconButton(onClick = { onTimeSelected(null) }) {
                        Text("✕")
                    }
                }
            }
        )
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedButton(
            onClick = { showTimePicker = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(R.string.pick_time))
        }

        if (showTimePicker) {
            val currentTime = state.triggerTime ?: LocalTime.now()
            TimePickerDialog(
                context,
                { _, hourOfDay, minute ->
                    onTimeSelected(LocalTime.of(hourOfDay, minute))
                    showTimePicker = false
                },
                currentTime.hour,
                currentTime.minute,
                false
            ).apply {
                setOnCancelListener { showTimePicker = false }
                show()
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Recurrence (Pro-gated)
        Text(
            text = stringResource(R.string.recurrence_label),
            style = MaterialTheme.typography.bodyLarge
        )

        if (state.isPro) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = state.recurrence == null,
                    onClick = { onRecurrenceSelected(null) },
                    label = { Text(stringResource(R.string.recurrence_none)) }
                )
                FilterChip(
                    selected = state.recurrence == RecurrencePattern.DAILY,
                    onClick = { onRecurrenceSelected(RecurrencePattern.DAILY) },
                    label = { Text(stringResource(R.string.recurrence_daily)) }
                )
                FilterChip(
                    selected = state.recurrence == RecurrencePattern.WEEKLY,
                    onClick = { onRecurrenceSelected(RecurrencePattern.WEEKLY) },
                    label = { Text(stringResource(R.string.recurrence_weekly)) }
                )
                FilterChip(
                    selected = state.recurrence == RecurrencePattern.MONTHLY,
                    onClick = { onRecurrenceSelected(RecurrencePattern.MONTHLY) },
                    label = { Text(stringResource(R.string.recurrence_monthly)) }
                )
            }
        } else {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.recurrence_pro_only),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedButton(onClick = onUpgradeToPro) {
                Text(text = stringResource(R.string.upgrade_to_pro))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Geofence radius (only shown for location reminders, Pro-gated)
        if (state.hasLocationTrigger) {
            Text(
                text = stringResource(R.string.radius_label),
                style = MaterialTheme.typography.bodyLarge
            )

            val sliderEnabled = state.isPro

            Slider(
                value = state.radiusMetres.toFloat(),
                onValueChange = { if (sliderEnabled) onRadiusChanged(it.toInt()) },
                enabled = sliderEnabled,
                valueRange = MIN_RADIUS_METRES.toFloat()..MAX_RADIUS_METRES.toFloat(),
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = if (state.isPro) {
                    stringResource(R.string.radius_value, state.radiusMetres)
                } else {
                    stringResource(R.string.radius_fixed, ReminderEditViewModelFactory.DEFAULT_RADIUS_METRES)
                },
                style = MaterialTheme.typography.bodyMedium
            )

            if (!state.isPro) {
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedButton(onClick = onUpgradeToPro) {
                    Text(text = stringResource(R.string.upgrade_to_pro))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Error message
        if (state.saveError != null) {
            Text(
                text = state.saveError,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Save button
        Button(
            onClick = onSave,
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isSaving
        ) {
            Text(
                text = if (state.isSaving) {
                    stringResource(R.string.saving)
                } else {
                    stringResource(R.string.save)
                }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

private const val MIN_RADIUS_METRES = 50
private const val MAX_RADIUS_METRES = 1000
