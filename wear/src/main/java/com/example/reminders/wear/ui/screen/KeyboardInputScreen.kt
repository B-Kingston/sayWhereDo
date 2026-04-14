package com.example.reminders.wear.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.example.reminders.wear.R
import com.example.reminders.wear.ui.theme.WearConstants
import com.example.reminders.wear.ui.theme.WearSpacing
import com.example.reminders.wear.ui.viewmodel.VoiceRecordViewModel

/**
 * Screen for typing a reminder using the on-watch keyboard or IME.
 *
 * Uses a [BasicTextField] styled with theme colours instead of the
 * phone-compose TextField — the wear module must not import
 * `androidx.compose.material3`. The save button is disabled while
 * the input is blank.
 */
@Composable
fun KeyboardInputScreen(
    viewModel: VoiceRecordViewModel,
    onNavigateBack: () -> Unit
) {
    var text by rememberSaveable { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    fun submit() {
        if (text.isNotBlank()) {
            focusManager.clearFocus()
            viewModel.onKeyboardInput(text.trim())
            onNavigateBack()
        }
    }

    ScreenScaffold {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = WearConstants.ScreenPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            StyledInputField(
                text = text,
                onTextChange = { text = it },
                onDone = { submit() }
            )

            Spacer(modifier = Modifier.height(WearSpacing.Lg))

            Button(
                onClick = { submit() },
                modifier = Modifier.fillMaxWidth(WearConstants.ButtonWidthFraction),
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

/**
 * A text input field styled for the wear dark theme.
 *
 * Draws a rounded, semi-transparent background so the field is
 * visually distinct from the surrounding surface.
 */
@Composable
private fun StyledInputField(
    text: String,
    onTextChange: (String) -> Unit,
    onDone: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(WearSpacing.Md))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        BasicTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(WearSpacing.Md),
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurface
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onDone() }),
            singleLine = false,
            maxLines = WearConstants.MaxBodyLines,
            decorationBox = { innerTextField ->
                if (text.isEmpty()) {
                    Text(
                        text = stringResource(R.string.enter_reminder_text),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                innerTextField()
            }
        )
    }
}
