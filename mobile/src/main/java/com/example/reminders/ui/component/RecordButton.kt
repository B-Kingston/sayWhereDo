package com.example.reminders.ui.component

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.reminders.R
import com.example.reminders.ui.theme.UiConstants
import com.example.reminders.ui.screen.TranscriptionUiState

/**
 * A large circular record button that toggles between microphone (idle)
 * and stop (listening) icons.
 *
 * When the state is [TranscriptionUiState.Listening], the button pulses
 * gently to give the user visual feedback that recording is active.
 * Uses a spring-based animation via the expressive motion scheme.
 *
 * @param uiState  Current transcription state — drives the icon and animation.
 * @param onClick  Callback invoked when the button is tapped.
 * @param modifier Optional modifier for layout customisation.
 */
@Composable
fun RecordButton(
    uiState: TranscriptionUiState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isListening = uiState is TranscriptionUiState.Listening
    val isProcessing = uiState is TranscriptionUiState.Processing

    val pulseAnimation = rememberInfiniteTransition(label = "pulse")
    val pulseScale by pulseAnimation.animateFloat(
        initialValue = 1.0f,
        targetValue = UiConstants.PULSE_TARGET_SCALE,
        animationSpec = infiniteRepeatable(
            animation = spring(
                dampingRatio = 0.4f,
                stiffness = 200f
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val containerColor = when {
        isListening -> MaterialTheme.colorScheme.error
        isProcessing -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }

    Box {
        // Pulse ring behind the button when listening
        if (isListening) {
            Box(
                modifier = Modifier
                    .size(UiConstants.RECORD_BUTTON_SIZE_DP.dp)
                    .scale(pulseScale * 1.15f)
                    .background(
                        color = containerColor.copy(alpha = 0.2f),
                        shape = CircleShape
                    )
            )
        }

        FloatingActionButton(
            onClick = onClick,
            modifier = modifier
                .size(UiConstants.RECORD_BUTTON_SIZE_DP.dp)
                .then(if (isListening) Modifier.scale(pulseScale) else Modifier),
            shape = CircleShape,
            containerColor = containerColor,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = if (isListening) 8.dp else 4.dp
            )
        ) {
            Icon(
                imageVector = if (isListening || isProcessing) {
                    Icons.Filled.Stop
                } else {
                    Icons.Filled.Mic
                },
                contentDescription = if (isListening || isProcessing) {
                    stringResource(R.string.stop_recording)
                } else {
                    stringResource(R.string.start_recording)
                },
                modifier = Modifier.size(UiConstants.RECORD_ICON_SIZE_DP.dp)
            )
        }
    }
}

// ── Previews ──────────────────────────────────────────────────────────

/** Preview of the record button in idle state. */
@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun RecordButtonIdlePreview() {
    com.example.reminders.ui.theme.RemindersTheme {
        RecordButton(
            uiState = TranscriptionUiState.Idle,
            onClick = {}
        )
    }
}

/** Preview of the record button in listening state. */
@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun RecordButtonListeningPreview() {
    com.example.reminders.ui.theme.RemindersTheme {
        RecordButton(
            uiState = TranscriptionUiState.Listening,
            onClick = {}
        )
    }
}
