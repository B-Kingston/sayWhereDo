package com.example.reminders.ui.screen

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.reminders.R
import com.example.reminders.ui.component.RecordButton
import com.example.reminders.ui.viewmodel.TranscriptionViewModel

/**
 * Full-screen composable for the voice transcription flow.
 *
 * Handles:
 * - [Manifest.permission.RECORD_AUDIO] permission request via the Activity
 *   Result API
 * - Display of recognition state (idle, listening, processing, result, error)
 * - Partial transcription text while listening
 * - Retry on error
 *
 * @param viewModel The [TranscriptionViewModel] driving the UI state.
 * @param onBack    Callback invoked when the user presses the back button.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranscriptionScreen(
    viewModel: TranscriptionViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val partialText by viewModel.partialText.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val hasRecordAudioPermission = remember {
        android.content.pm.PackageManager.PERMISSION_GRANTED ==
            context.checkCallingOrSelfPermission(Manifest.permission.RECORD_AUDIO)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.startListening()
        }
        // If denied, the UI stays in Idle/Error and the user can retry.
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.new_reminder)) },
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = statusTextForState(uiState),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    color = when (uiState) {
                        is TranscriptionUiState.Error -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )

                Spacer(Modifier.height(32.dp))

                RecordButton(
                    uiState = uiState,
                    onClick = {
                        when (uiState) {
                            is TranscriptionUiState.Listening,
                            is TranscriptionUiState.Processing -> viewModel.stopListening()

                            is TranscriptionUiState.Result,
                            is TranscriptionUiState.Error -> {
                                viewModel.reset()
                                requestPermissionOrStart(
                                    hasPermission = hasRecordAudioPermission,
                                    requestPermission = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                                    startListening = { viewModel.startListening() }
                                )
                            }

                            is TranscriptionUiState.Idle -> {
                                requestPermissionOrStart(
                                    hasPermission = hasRecordAudioPermission,
                                    requestPermission = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                                    startListening = { viewModel.startListening() }
                                )
                            }
                        }
                    }
                )

                Spacer(Modifier.height(32.dp))

                val currentState = uiState
                val displayText = when (currentState) {
                    is TranscriptionUiState.Listening -> partialText.ifBlank { "" }
                    is TranscriptionUiState.Result -> currentState.text
                    else -> ""
                }

                if (displayText.isNotBlank()) {
                    Text(
                        text = displayText,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }
}

/**
 * Returns a human-readable status message for the given [TranscriptionUiState].
 */
@Composable
private fun statusTextForState(state: TranscriptionUiState): String = when (state) {
    is TranscriptionUiState.Idle -> stringResource(R.string.tap_to_record)
    is TranscriptionUiState.Listening -> stringResource(R.string.listening)
    is TranscriptionUiState.Processing -> stringResource(R.string.processing)
    is TranscriptionUiState.Result -> stringResource(R.string.transcription_complete)
    is TranscriptionUiState.Error -> state.message
}

/**
 * Checks microphone permission and either requests it or starts listening.
 */
private fun requestPermissionOrStart(
    hasPermission: Boolean,
    requestPermission: () -> Unit,
    startListening: () -> Unit
) {
    if (hasPermission) {
        startListening()
    } else {
        requestPermission()
    }
}
