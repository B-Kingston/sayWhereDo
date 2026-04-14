package com.example.reminders.wear.presentation.theme

import androidx.compose.runtime.Composable
import com.example.reminders.wear.ui.theme.RemindersWearTheme

/**
 * Backward-compatible theme wrapper.
 *
 * Delegates to the canonical [RemindersWearTheme] in the ui.theme
 * package. All new code should import [RemindersWearTheme] directly.
 */
@Composable
fun RemindersTheme(
    content: @Composable () -> Unit
) {
    RemindersWearTheme(content = content)
}
