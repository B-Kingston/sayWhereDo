package com.example.reminders.wear.ui.screen

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.example.reminders.wear.R
import com.example.reminders.wear.ui.theme.WearConstants
import com.example.reminders.wear.ui.theme.WearSpacing
import com.example.reminders.wear.ui.viewmodel.StreamToPhoneUiState
import com.example.reminders.wear.ui.viewmodel.StreamToPhoneViewModel

/**
 * Screen displayed while audio is being streamed from the watch to the
 * companion phone for transcription.
 *
 * Shows a pulsing recording indicator, status text, and a cancel button.
 */
@Composable
fun StreamToPhoneScreen(
    viewModel: StreamToPhoneViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (uiState) {
        is StreamToPhoneUiState.Success -> onNavigateBack()
        is StreamToPhoneUiState.Error -> {
            viewModel.reset()
            onNavigateBack()
        }
        else -> Unit
    }

    ScreenScaffold {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                PulsingIndicator(isRecording = uiState is StreamToPhoneUiState.Recording)

                Spacer(modifier = Modifier.height(WearConstants.StatusSpacing))

                Text(
                    text = statusText(uiState),
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(WearConstants.CancelButtonSpacing))

                Button(
                    onClick = {
                        viewModel.cancel()
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    shape = ButtonDefaults.shape
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = stringResource(R.string.stream_cancel_description),
                        modifier = Modifier.size(WearConstants.CancelIconSize)
                    )
                }
            }
        }
    }
}

/**
 * A microphone icon that pulses between translucent and opaque while
 * [isRecording] is true, giving visual feedback that audio capture is
 * active. Uses [animateFloatAsState] for a smooth, hardware-accelerated
 * transition.
 */
@Composable
private fun PulsingIndicator(isRecording: Boolean) {
    val targetAlpha = if (isRecording) {
        WearConstants.MaxPulseAlpha
    } else {
        WearConstants.MinPulseAlpha
    }

    val iconAlpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(
            durationMillis = WearConstants.PulseDurationMs,
            easing = LinearEasing
        ),
        label = "pulseAlpha"
    )

    Icon(
        imageVector = Icons.Default.Mic,
        contentDescription = stringResource(R.string.stream_recording_indicator),
        modifier = Modifier.size(WearConstants.PulseIconSize),
        tint = MaterialTheme.colorScheme.primary.copy(alpha = iconAlpha)
    )
}

/**
 * Returns a human-readable status label for the given [state].
 */
private fun statusText(state: StreamToPhoneUiState): String {
    return when (state) {
        is StreamToPhoneUiState.Idle -> "Ready"
        is StreamToPhoneUiState.Recording -> "Recording..."
        is StreamToPhoneUiState.Streaming -> "Streaming..."
        is StreamToPhoneUiState.Success -> "Done"
        is StreamToPhoneUiState.Error -> state.message
    }
}
