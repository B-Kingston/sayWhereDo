package com.example.reminders.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.reminders.R
import com.example.reminders.ui.viewmodel.KeyboardInputUiState
import com.example.reminders.ui.viewmodel.KeyboardInputViewModel
import kotlinx.coroutines.launch

/**
 * Full-screen composable for typing a reminder via the keyboard.
 *
 * Displays a multiline text field and a save button. The entered text
 * is processed through the [PipelineOrchestrator] via [KeyboardInputViewModel].
 * On success the screen navigates back automatically.
 *
 * @param viewModel The ViewModel that owns the pipeline interaction.
 * @param onBack    Called to navigate up after success or when the user taps back.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeyboardInputScreen(
    viewModel: KeyboardInputViewModel,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var inputText by rememberSaveable { mutableStateOf("") }
    val uiState = viewModel.uiState

    LaunchedEffect(uiState) {
        if (uiState.value is KeyboardInputUiState.Success) {
            onBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.keyboard_input_title)) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(CONTENT_PADDING)
                .imePadding()
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                label = { Text(stringResource(R.string.keyboard_input_placeholder)) },
                modifier = Modifier.fillMaxWidth(),
                maxLines = MAX_INPUT_LINES,
                enabled = uiState.value !is KeyboardInputUiState.Saving,
                isError = uiState.value is KeyboardInputUiState.Error,
                supportingText = {
                    val state = uiState.value
                    if (state is KeyboardInputUiState.Error) {
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(SAVE_BUTTON_TOP_SPACING))

            when (val state = uiState.value) {
                is KeyboardInputUiState.Saving -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    Spacer(modifier = Modifier.height(SAVE_BUTTON_TOP_SPACING))
                    Text(
                        text = stringResource(R.string.keyboard_input_saving),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
                else -> {
                    Button(
                        onClick = {
                            scope.launch {
                                viewModel.saveReminder(inputText)
                            }
                        },
                        enabled = inputText.isNotBlank() &&
                            uiState.value !is KeyboardInputUiState.Saving,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.keyboard_input_save))
                    }
                }
            }
        }
    }
}

/** Padding around the main content column. */
private val CONTENT_PADDING = 16.dp

/** Maximum number of visible lines in the input field. */
private const val MAX_INPUT_LINES = 4

/** Vertical spacing between the text field and the save button. */
private val SAVE_BUTTON_TOP_SPACING = 16.dp
