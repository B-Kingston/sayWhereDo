package com.example.reminders.wear.ui.screen

import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.example.reminders.wear.R
import com.example.reminders.wear.ui.viewmodel.VoiceRecordUiState
import com.example.reminders.wear.ui.viewmodel.VoiceRecordViewModel

@Composable
fun VoiceRecordScreen(
    viewModel: VoiceRecordViewModel,
    onNavigateToKeyboard: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val voiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val text = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (text != null) {
                viewModel.onVoiceResult(text)
                onNavigateBack()
            }
        } else {
            viewModel.reset()
        }
    }

    when (uiState) {
        is VoiceRecordUiState.Success -> onNavigateBack()
        is VoiceRecordUiState.Error -> {
            Toast.makeText(context, (uiState as VoiceRecordUiState.Error).message, Toast.LENGTH_SHORT).show()
            viewModel.reset()
        }
        else -> Unit
    }

    ScreenScaffold {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Button(
                onClick = {
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(
                            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                        )
                    }
                    val resolved = context.packageManager.resolveActivity(intent, 0)
                    if (resolved != null) {
                        viewModel.setRecording()
                        voiceLauncher.launch(intent)
                    } else {
                        Toast.makeText(
                            context,
                            context.getString(R.string.speech_not_available),
                            Toast.LENGTH_SHORT
                        ).show()
                        onNavigateToKeyboard()
                    }
                },
                modifier = Modifier.size(VOICE_BUTTON_SIZE),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = ButtonDefaults.shape
            ) {
                Icon(
                    imageVector = Icons.Filled.Mic,
                    contentDescription = stringResource(R.string.voice_record),
                    modifier = Modifier.size(ICON_SIZE)
                )
            }

            Spacer(modifier = Modifier.height(SPACE_BETWEEN))

            Button(
                onClick = onNavigateToKeyboard,
                modifier = Modifier.fillMaxWidth(FRACTION_WIDTH),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ),
                shape = ButtonDefaults.shape
            ) {
                Text(
                    text = stringResource(R.string.type_reminder),
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

private val VOICE_BUTTON_SIZE = 72.dp
private val ICON_SIZE = 32.dp
private val SPACE_BETWEEN = 16.dp
private val FRACTION_WIDTH = 0.85f
