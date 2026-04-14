package com.example.reminders.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Shape system for the Reminders app.
 *
 * Uses modern, generous corner radii on an 8 dp grid, with a full
 * pill shape (50 % corner size) used for buttons and chips.
 */

/** App-wide Material 3 shape tokens. */
val RemindersShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

/** Pill-shaped corner — use for FABs, buttons, chips. */
val PillShape = RoundedCornerShape(50)
