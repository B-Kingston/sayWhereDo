package com.example.reminders.wear.ui.screen

import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.ScreenScaffold
import com.example.reminders.wear.R
import com.example.reminders.wear.ui.component.InputMethod
import com.example.reminders.wear.ui.component.InputMethodSelector
import com.example.reminders.wear.ui.viewmodel.VoiceRecordUiState
import com.example.reminders.wear.ui.viewmodel.VoiceRecordViewModel

/**
 * Screen shown when the user initiates a new reminder from the watch.
 *
 * Provides an [InputMethodSelector] that lets the user choose between
 * keyboard input, on-watch voice recognition, streaming audio to the
 * phone, or cloud-based formatting (when configured).
 */
@Composable
fun VoiceRecordScreen(
    viewModel: VoiceRecordViewModel,
    onNavigateToKeyboard: () -> Unit,
    onNavigateToStreamToPhone: () -> Unit,
    onNavigateToCloudFormat: () -> Unit,
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
        InputMethodSelector(
            hasCloudApiConfigured = false,
            onKeyboardSelected = onNavigateToKeyboard,
            onInputMethodSelected = { method ->
                when (method) {
                    InputMethod.Keyboard -> onNavigateToKeyboard()
                    InputMethod.VoiceOnWatch -> {
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
                    }
                    InputMethod.VoiceStreamToPhone -> onNavigateToStreamToPhone()
                    InputMethod.CloudFormatOnWatch -> onNavigateToCloudFormat()
                }
            },
            modifier = Modifier.fillMaxSize().wrapContentHeight(Alignment.CenterVertically)
        )
    }
}
