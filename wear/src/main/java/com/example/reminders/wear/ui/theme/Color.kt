package com.example.reminders.wear.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Wear design-system color tokens.
 *
 * Monochrome base (dark grays) with a single teal accent (#00BFA5) that
 * pops on small round and square watch screens. Status colors are
 * limited to connected/disconnected states — keeping the palette minimal
 * is essential for legibility on WearOS.
 */

// ────────────────────────────────────────────────────────────────────────
// Accent (single brand color)
// ────────────────────────────────────────────────────────────────────────

/** Vibrant teal — the sole accent used for primary actions and highlights. */
val TealAccent = Color(0xFF00BFA5)

/** White — used for text and icons sitting on the teal accent. */
val OnTealAccent = Color.White

// ────────────────────────────────────────────────────────────────────────
// Surface hierarchy (dark grays, low → high)
// ────────────────────────────────────────────────────────────────────────

val SurfaceDarkest = Color(0xFF1A1A1A)
val SurfaceDark = Color(0xFF212121)
val SurfaceMedium = Color(0xFF2C2C2C)
val SurfaceLight = Color(0xFF383838)
val SurfaceLightest = Color(0xFF424242)

// ────────────────────────────────────────────────────────────────────────
// Content
// ────────────────────────────────────────────────────────────────────────

val OnSurfaceBright = Color(0xFFF5F5F5)
val OnSurfaceMedium = Color(0xFFBDBDBD)
val OnSurfaceDim = Color(0xFF8A8A8A)

// ────────────────────────────────────────────────────────────────────────
// Outline
// ────────────────────────────────────────────────────────────────────────

val OutlineSubtle = Color(0x1AFFFFFF)
val OutlineMedium = Color(0x33FFFFFF)

// ────────────────────────────────────────────────────────────────────────
// Status
// ────────────────────────────────────────────────────────────────────────

/** Green indicator for phone-connected state. */
val StatusConnected = Color(0xFF4CAF50)

/** Red indicator for phone-disconnected state. */
val StatusDisconnected = Color(0xFFF44336)

// ────────────────────────────────────────────────────────────────────────
// Error
// ────────────────────────────────────────────────────────────────────────

val ErrorRed = Color(0xFFCF6679)
val ErrorRedContainer = Color(0xFF3C1A1F)
val OnErrorRedContainer = Color(0xFFFFB3B3)
