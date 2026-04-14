package com.example.reminders.ui.screen

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import com.example.reminders.ui.theme.Spacing
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
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = Spacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // ── Status text with hierarchy ────────────────────────
                AnimatedContent(
                    targetState = uiState,
                    label = "status_text_transition"
                ) { targetState ->
                    Text(
                        text = statusTextForState(targetState),
                        style = when (targetState) {
                            is TranscriptionUiState.Listening -> MaterialTheme.typography.headlineSmall
                            is TranscriptionUiState.Error -> MaterialTheme.typography.titleMedium
                            else -> MaterialTheme.typography.titleMedium
                        },
                        textAlign = TextAlign.Center,
                        color = when (targetState) {
                            is TranscriptionUiState.Error -> MaterialTheme.colorScheme.error
                            is TranscriptionUiState.Listening -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }

                Spacer(Modifier.height(Spacing.xl))

                // ── Record button ─────────────────────────────────────
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
                                    requestPermission = {
                                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    },
                                    startListening = { viewModel.startListening() }
                                )
                            }

                            is TranscriptionUiState.Idle -> {
                                requestPermissionOrStart(
                                    hasPermission = hasRecordAudioPermission,
                                    requestPermission = {
                                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    },
                                    startListening = { viewModel.startListening() }
                                )
                            }
                        }
                    }
                )

                Spacer(Modifier.height(Spacing.xl))

                // ── Partial / final transcription text ────────────────
                val currentState = uiState
                val displayText = when (currentState) {
                    is TranscriptionUiState.Listening -> partialText.ifBlank { "" }
                    is TranscriptionUiState.Result -> currentState.text
                    else -> ""
                }

                AnimatedVisibility(
                    visible = displayText.isNotBlank(),
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + slideOutVertically()
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        tonalElevation = 2.dp
                    ) {
                        Text(
                            text = displayText,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Spacing.md)
                        )
                    }
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

// ── Previews ──────────────────────────────────────────────────────────

/** Preview of the idle transcription state. */
@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun TranscriptionIdlePreview() {
    com.example.reminders.ui.theme.RemindersTheme {
        Surface {
            Text("Transcription screen — requires ViewModel")
        }
    }
}
