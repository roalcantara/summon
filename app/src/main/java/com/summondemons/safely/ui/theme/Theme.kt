package com.summondemons.safely.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.summondemons.safely.ui.fx.LocalReduceMotion
import com.summondemons.safely.ui.fx.rememberReduceMotion

private val DarkColors = darkColorScheme(
    primary = Palette.primary,
    onPrimary = Palette.onPrimary,
    primaryContainer = Palette.primaryContainer,
    onPrimaryContainer = Palette.onPrimaryContainer,
    inversePrimary = Palette.inversePrimary,
    secondary = Palette.secondary,
    onSecondary = Palette.onSecondary,
    secondaryContainer = Palette.secondaryContainer,
    onSecondaryContainer = Palette.onSecondaryContainer,
    tertiary = Palette.tertiary,
    onTertiary = Palette.onTertiary,
    tertiaryContainer = Palette.tertiaryContainer,
    onTertiaryContainer = Palette.onTertiaryContainer,
    background = Palette.background,
    onBackground = Palette.onBackground,
    surface = Palette.surface,
    onSurface = Palette.onSurface,
    surfaceVariant = Palette.surfaceVariant,
    onSurfaceVariant = Palette.onSurfaceVariant,
    surfaceTint = Palette.surfaceTint,
    inverseSurface = Palette.inverseSurface,
    inverseOnSurface = Palette.inverseOnSurface,
    error = Palette.error,
    onError = Palette.onError,
    errorContainer = Palette.errorContainer,
    onErrorContainer = Palette.onErrorContainer,
    outline = Palette.outline,
    outlineVariant = Palette.outlineVariant,
    surfaceBright = Palette.surfaceBright,
    surfaceDim = Palette.surfaceDim,
    surfaceContainer = Palette.surfaceContainer,
    surfaceContainerHigh = Palette.surfaceContainerHigh,
    surfaceContainerHighest = Palette.surfaceContainerHighest,
    surfaceContainerLow = Palette.surfaceContainerLow,
    surfaceContainerLowest = Palette.surfaceContainerLowest,
)

/** Dark only (ADR-0001). */
@Composable
fun SummonDemonsSafelyTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalDemonColors provides DefaultDemonColors,
        LocalSpacing provides Spacing(),
        LocalTextRoles provides AppTextRoles,
        LocalReduceMotion provides rememberReduceMotion(),
    ) {
        MaterialTheme(
            colorScheme = DarkColors,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}
