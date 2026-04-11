package com.example.reminders.wear.presentation.theme

import androidx.compose.runtime.Composable
import androidx.wear.compose.material3.MaterialTheme

@Composable
fun RemindersTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        content = content
    )
}
