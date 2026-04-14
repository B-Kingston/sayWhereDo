package com.example.reminders.wear.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Halved 8 dp grid for WearOS.
 *
 * Every spacing value used in the wear module should reference one of
 * these tokens. Keeping the grid tight preserves screen real-estate on
 * small round displays while maintaining consistent visual rhythm.
 */
object WearSpacing {

    /** 2 dp — hairline gaps, divider padding. */
    val Xxs = 2.dp

    /** 4 dp — compact list-item internal spacing. */
    val Xs = 4.dp

    /** 6 dp — tight vertical rhythm between related items. */
    val Sm = 6.dp

    /** 8 dp — the base unit; standard list-item gap, inner card padding. */
    val Md = 8.dp

    /** 12 dp — section dividers, larger card padding. */
    val Lg = 12.dp

    /** 16 dp — screen-level horizontal padding, major section gaps. */
    val Xl = 16.dp

    /** 24 dp — top-level vertical separation. */
    val Xxl = 24.dp
}
