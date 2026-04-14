package com.example.reminders.wear.ui.theme

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Centralised hard-coded constants for the Wear UI layer.
 *
 * Every magic number that would otherwise appear in screen or component
 * composables lives here so that values are easy to audit and adjust in
 * one place.
 */
object WearConstants {

    // ── Layout ─────────────────────────────────────────────────────────
    /** Standard horizontal screen padding. */
    val ScreenPadding = WearSpacing.Lg

    /** Fraction of screen width used for centered action buttons. */
    const val ButtonWidthFraction = 0.7f

    /** Minimum touch-target size on Wear (48 dp per guidelines). */
    val MinTouchTarget = 48.dp

    // ── List items ─────────────────────────────────────────────────────
    /** Vertical spacing between items in a TransformingLazyColumn. */
    val ItemSpacing = WearSpacing.Xs

    /** Horizontal padding for content inside list items. */
    val HorizontalPadding = WearSpacing.Lg

    /** Spacing between major sections (e.g., header → body → actions). */
    val SectionSpacing = WearSpacing.Xl

    // ── Pulsing indicator (StreamToPhoneScreen) ────────────────────────
    /** Size of the pulsing microphone icon. */
    val PulseIconSize = 48.dp

    /** Vertical space between status text and the indicator. */
    val StatusSpacing = WearSpacing.Xl

    /** Vertical space between status text and the cancel button. */
    val CancelButtonSpacing = WearSpacing.Lg

    /** Size of the stop/cancel icon inside the cancel button. */
    val CancelIconSize = 24.dp

    /** Minimum alpha for the pulsing animation. */
    const val MinPulseAlpha = 0.3f

    /** Maximum alpha for the pulsing animation. */
    const val MaxPulseAlpha = 1.0f

    /** Duration of one pulse cycle in milliseconds. */
    const val PulseDurationMs = 800

    // ── Settings screen ────────────────────────────────────────────────
    /** Size of the connectivity status dot. */
    val DotSize = 10.dp

    /** Space between the dot and its label text. */
    val DotLabelSpacing = WearSpacing.Md

    // ── Input method selector ──────────────────────────────────────────
    /** Size of the keyboard icon touch target. */
    val KeyboardIconTouchTarget = 48.dp

    /** Size of the microphone icon. */
    val MicIconSize = 32.dp

    /** Size of the chevron (dropdown arrow) icon. */
    val ChevronIconSize = 20.dp

    /** Size of icons in expanded option rows. */
    val OptionIconSize = 24.dp

    /** Space between the selector bar and expanded options. */
    val OptionSpacing = WearSpacing.Md

    /** Vertical spacing between expanded option rows. */
    val OptionRowSpacing = WearSpacing.Sm

    /** Space between an option icon and its text. */
    val OptionTextSpacing = WearSpacing.Md

    /** Alpha for enabled option rows. */
    const val EnabledAlpha = 1.0f

    /** Alpha for disabled option rows (e.g., cloud without API key). */
    const val DisabledAlpha = 0.4f

    // ── Text ───────────────────────────────────────────────────────────
    /** Maximum lines for reminder titles in list items. */
    const val MaxTitleLines = 2

    /** Maximum lines for reminder detail body text. */
    const val MaxBodyLines = 4

    /** Maximum lines for reminder detail title. */
    const val MaxDetailTitleLines = 3

    // ── Icons ──────────────────────────────────────────────────────────
    /** Standard icon size for list-item icons (settings gear, etc.). */
    val ListIconSize = 24.dp

    // ── PhoneRequiredBanner ────────────────────────────────────────────
    /** Inner padding of the banner card. */
    val BannerInnerPadding = WearSpacing.Md
}
