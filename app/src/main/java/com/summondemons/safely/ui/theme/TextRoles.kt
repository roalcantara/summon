package com.summondemons.safely.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import java.util.Locale

/** DESIGN.md "Text roles" (ADR-0014). `cue` and `meta` text is upper-cased with [upper]. */
@Immutable
data class TextRoles(
    val cue: TextStyle,
    val incantation: TextStyle,
    val meta: TextStyle,
    /** Cinzel Bold; the font size is set per use. */
    val name: TextStyle,
    val body: TextStyle,
    /** Chant screen lines (ADR-0019): Inter Bold, upper case; 18 sp direction, 22 sp verdict, set per use. */
    val ritualCue: TextStyle,
    /** Chant screen phrase (ADR-0019): Cinzel Bold, auto-sized at the call site. */
    val incantationLarge: TextStyle,
)

internal val AppTextRoles = TextRoles(
    cue = AppTypography.labelMedium.copy(letterSpacing = 0.08.em),
    incantation = TextStyle(
        fontFamily = Display,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.02.em,
    ),
    meta = AppTypography.labelSmall.copy(letterSpacing = 0.14.em, color = Palette.outline),
    name = TextStyle(fontFamily = Display, fontWeight = FontWeight.Bold),
    body = AppTypography.bodyMedium,
    ritualCue = AppTypography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.05.em),
    incantationLarge = TextStyle(
        fontFamily = Display,
        fontWeight = FontWeight.Bold,
        lineHeight = 1.3.em,
        letterSpacing = 0.02.em,
    ),
)

val LocalTextRoles = staticCompositionLocalOf { AppTextRoles }

val MaterialTheme.textRoles: TextRoles
    @Composable @ReadOnlyComposable get() = LocalTextRoles.current

/** Compose has no text-transform: the `cue`, `meta` and `ritualCue` roles upper-case at the call site. */
fun String.upper(): String = uppercase(Locale.ROOT)
