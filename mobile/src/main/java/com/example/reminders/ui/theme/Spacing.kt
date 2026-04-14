package com.example.reminders.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Centralised spacing tokens for the Reminders app.
 *
 * All tokens sit on an 8 dp grid to maintain consistent rhythm
 * across layouts. Use these instead of hardcoded `dp` values.
 */
object Spacing {

    /** 2 dp — hairline gaps, divider insets. */
    val xxs = 2.dp

    /** 4 dp — tight inner padding, badge insets. */
    val xs = 4.dp

    /** 8 dp — small gaps, icon-text spacing, list item insets. */
    val sm = 8.dp

    /** 16 dp — standard content padding, card inner padding, section gaps. */
    val md = 16.dp

    /** 24 dp — section-to-section spacing, screen edge padding on tablets. */
    val lg = 24.dp

    /** 32 dp — large separations, bottom-of-screen clearance. */
    val xl = 32.dp

    /** 48 dp — major structural spacing, hero-image padding. */
    val xxl = 48.dp
}
