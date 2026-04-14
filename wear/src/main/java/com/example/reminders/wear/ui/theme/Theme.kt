package com.example.reminders.wear.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Typography

/**
 * Theme for the WearOS reminders app.
 *
 * Always renders in dark mode (watches are almost always dark).
 * Uses a monochrome surface hierarchy with a single teal accent for
 * primary actions. All colours come from [Color.kt] design tokens.
 *
 * Every screen and component in the wear module should be wrapped in
 * this theme so that [MaterialTheme.colorScheme] and
 * [MaterialTheme.typography] are available.
 */
@Composable
fun RemindersWearTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = wearColorScheme(),
        typography = wearTypography(),
        content = content
    )
}

/**
 * Builds the [ColorScheme] for the watch dark theme.
 *
 * Monochrome base surfaces (darkest → lightest) with a single teal
 * accent colour for primary interactive elements. Error palette is
 * soft red to remain legible on dark backgrounds.
 */
private fun wearColorScheme(): ColorScheme = ColorScheme(
    primary = TealAccent,
    onPrimary = OnTealAccent,
    primaryContainer = TealAccent.copy(alpha = 0.15f),
    onPrimaryContainer = TealAccent,

    secondary = SurfaceLightest,
    onSecondary = OnSurfaceBright,
    secondaryContainer = SurfaceLight,
    onSecondaryContainer = OnSurfaceMedium,

    tertiary = TealAccent.copy(alpha = 0.6f),
    onTertiary = OnTealAccent,
    tertiaryContainer = TealAccent.copy(alpha = 0.1f),
    onTertiaryContainer = TealAccent,

    error = ErrorRed,
    onError = Color.White,
    errorContainer = ErrorRedContainer,
    onErrorContainer = OnErrorRedContainer,

    background = SurfaceDarkest,
    onBackground = OnSurfaceBright,

    onSurface = OnSurfaceBright,
    onSurfaceVariant = OnSurfaceMedium,

    surfaceContainerLow = SurfaceDark,
    surfaceContainer = SurfaceMedium,
    surfaceContainerHigh = SurfaceLight,

    outline = OutlineMedium,
    outlineVariant = OutlineSubtle
)

/**
 * Maps [WearTypography] tokens to the Wear Material3 [Typography] class.
 *
 * Not every slot in the M3 typography scale is distinct on Wear — some
 * are intentionally aliased to the same token to avoid visual noise.
 * Uses the default constructor and copies over individual text styles
 * because the Wear Typography primary constructor is internal.
 */
private fun wearTypography(): Typography {
    val base = Typography()
    return base.copy(
        displayLarge = WearTypography.Display,
        displayMedium = WearTypography.Display,
        displaySmall = WearTypography.Headline,

        titleLarge = WearTypography.Title,
        titleMedium = WearTypography.Title,
        titleSmall = WearTypography.Body,

        bodyLarge = WearTypography.Body,
        bodyMedium = WearTypography.Body,
        bodySmall = WearTypography.BodySmall,

        labelLarge = WearTypography.Label,
        labelMedium = WearTypography.Label,
        labelSmall = WearTypography.LabelSmall
    )
}
