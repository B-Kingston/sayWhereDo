package com.example.reminders.wear.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.material3.TextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.example.reminders.wear.R
import com.example.reminders.wear.ui.viewmodel.VoiceRecordViewModel

@Composable
fun KeyboardInputScreen(
    viewModel: VoiceRecordViewModel,
    onNavigateBack: () -> Unit
) {
    var text by rememberSaveable { mutableStateOf("") }

    ScreenScaffold {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = SCREEN_PADDING),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            TextField(
                value = text,
                onValueChange = { text = it },
                placeholder = {
                    Text(stringResource(R.string.enter_reminder_text))
                },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (text.isNotBlank()) {
                            viewModel.onKeyboardInput(text.trim())
                            onNavigateBack()
                        }
                    }
                ),
                singleLine = false,
                maxLines = MAX_LINES,
                colors = OutlinedTextFieldDefaults.colors()
            )

            Spacer(modifier = Modifier.height(SPACE_BETWEEN))

            Button(
                onClick = {
                    if (text.isNotBlank()) {
                        viewModel.onKeyboardInput(text.trim())
                        onNavigateBack()
                    }
                },
                modifier = Modifier.fillMaxWidth(BUTTON_WIDTH_FRACTION),
                enabled = text.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = ButtonDefaults.shape
            ) {
                Text(
                    text = stringResource(R.string.save),
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

private val SCREEN_PADDING = 8.dp
private val BUTTON_WIDTH_FRACTION = 0.7f
private val SPACE_BETWEEN = 12.dp
private const val MAX_LINES = 4
