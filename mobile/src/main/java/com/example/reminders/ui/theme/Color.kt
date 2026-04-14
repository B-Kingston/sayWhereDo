package com.example.reminders.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Teal-based colour palette for the Reminders app.
 *
 * Seed colour: **#006D5B** (rich professional teal).
 * Provides full M3 light/dark fallback schemes and semantic status colours
 * used throughout the UI for success, warning, and error states.
 */

// ────────────────────────────────────────────────────────────────────────
// Seed
// ────────────────────────────────────────────────────────────────────────

/** The seed colour from which the M3 palette is derived. */
val Seed = Color(0xFF006D5B)

// ────────────────────────────────────────────────────────────────────────
// Light colour scheme (fallback when dynamic colour is unavailable)
// ────────────────────────────────────────────────────────────────────────

/** Primary teal for light theme. */
val LightPrimary = Color(0xFF006B5B)
/** On-primary text/icon colour for light theme. */
val LightOnPrimary = Color(0xFFFFFFFF)
/** Primary container (tinted surface) for light theme. */
val LightPrimaryContainer = Color(0xFF78F8DE)
/** On-primary-container text colour for light theme. */
val LightOnPrimaryContainer = Color(0xFF00201B)

/** Secondary accent for light theme. */
val LightSecondary = Color(0xFF4B635C)
val LightOnSecondary = Color(0xFFFFFFFF)
val LightSecondaryContainer = Color(0xFFCDE8DF)
val LightOnSecondaryContainer = Color(0xFF07201A)

/** Tertiary accent for light theme. */
val LightTertiary = Color(0xFF426278)
val LightOnTertiary = Color(0xFFFFFFFF)
val LightTertiaryContainer = Color(0xFFC7E7FF)
val LightOnTertiaryContainer = Color(0xFF001E2F)

val LightError = Color(0xFFBA1A1A)
val LightOnError = Color(0xFFFFFFFF)
val LightErrorContainer = Color(0xFFFFDAD6)
val LightOnErrorContainer = Color(0xFF410002)

val LightBackground = Color(0xFFF5FBF7)
val LightOnBackground = Color(0xFF171D1B)
val LightSurface = Color(0xFFF5FBF7)
val LightOnSurface = Color(0xFF171D1B)

val LightSurfaceVariant = Color(0xFFDBE5DF)
val LightOnSurfaceVariant = Color(0xFF3F4945)

val LightOutline = Color(0xFF6F7975)
val LightOutlineVariant = Color(0xFFBFC9C4)

val LightInverseSurface = Color(0xFF2B3230)
val LightInverseOnSurface = Color(0xFFECF2EE)
val LightInversePrimary = Color(0xFF5CDBC2)

val LightSurfaceDim = Color(0xFFD5DBD8)
val LightSurfaceBright = Color(0xFFF5FBF7)
val LightSurfaceContainerLowest = Color(0xFFFFFFFF)
val LightSurfaceContainerLow = Color(0xFFEFF5F2)
val LightSurfaceContainer = Color(0xFFE9EFEC)
val LightSurfaceContainerHigh = Color(0xFFE3EAE6)
val LightSurfaceContainerHighest = Color(0xFFDEE4E1)

// ────────────────────────────────────────────────────────────────────────
// Dark colour scheme (fallback when dynamic colour is unavailable)
// ────────────────────────────────────────────────────────────────────────

val DarkPrimary = Color(0xFF5CDBC2)
val DarkOnPrimary = Color(0xFF003730)
val DarkPrimaryContainer = Color(0xFF005044)
val DarkOnPrimaryContainer = Color(0xFF78F8DE)

val DarkSecondary = Color(0xFFB1CCC3)
val DarkOnSecondary = Color(0xFF1D352E)
val DarkSecondaryContainer = Color(0xFF344B44)
val DarkOnSecondaryContainer = Color(0xFFCDE8DF)

val DarkTertiary = Color(0xFFA9CBE2)
val DarkOnTertiary = Color(0xFF0E3446)
val DarkTertiaryContainer = Color(0xFF294A5E)
val DarkOnTertiaryContainer = Color(0xFFC7E7FF)

val DarkError = Color(0xFFFFB4AB)
val DarkOnError = Color(0xFF690005)
val DarkErrorContainer = Color(0xFF93000A)
val DarkOnErrorContainer = Color(0xFFFFDAD6)

val DarkBackground = Color(0xFF0E1513)
val DarkOnBackground = Color(0xFFDEE4E1)
val DarkSurface = Color(0xFF0E1513)
val DarkOnSurface = Color(0xFFDEE4E1)

val DarkSurfaceVariant = Color(0xFF3F4945)
val DarkOnSurfaceVariant = Color(0xFFBFC9C4)

val DarkOutline = Color(0xFF899390)
val DarkOutlineVariant = Color(0xFF3F4945)

val DarkInverseSurface = Color(0xFFDEE4E1)
val DarkInverseOnSurface = Color(0xFF2B3230)
val DarkInversePrimary = Color(0xFF006B5B)

val DarkSurfaceDim = Color(0xFF0E1513)
val DarkSurfaceBright = Color(0xFF343B38)
val DarkSurfaceContainerLowest = Color(0xFF090F0E)
val DarkSurfaceContainerLow = Color(0xFF171D1B)
val DarkSurfaceContainer = Color(0xFF1B211F)
val DarkSurfaceContainerHigh = Color(0xFF252B29)
val DarkSurfaceContainerHighest = Color(0xFF303634)

// ────────────────────────────────────────────────────────────────────────
// Semantic status colours
// ────────────────────────────────────────────────────────────────────────

/** Success — used for positive status indicators (green). */
val StatusSuccess = Color(0xFF2E7D32)

/** Warning — used for degraded/pending status indicators (amber). */
val StatusWarning = Color(0xFFF57F17)

/** Error — used for failure/offline status indicators (red). */
val StatusError = Color(0xFFC62828)
