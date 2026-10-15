package com.example.summondemonssafely.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ==========================================
// 1. Color Palette: Dark Tactical RPG & Occult Magic
// ==========================================
val ObsidianSurface = Color(0xFF0B1326)
val ObsidianSurfaceContainer = Color(0xFF171F33)
val ObsidianSurfaceHigh = Color(0xFF222A3D)
val ObsidianSurfaceLowest = Color(0xFF060E20)
val OnSurfaceLight = Color(0xFFDAE2FD)
val OnSurfaceVariant = Color(0xFFD8C3AD)

// Imperial Amber / Gold (Primary & Asmodeus Accent)
val AmberGoldPrimary = Color(0xFFFFC174)
val AmberGoldContainer = Color(0xFFF59E0B)
val OnAmberGold = Color(0xFF472A00)

// Arcane Cyan / Mana Channel
val ArcaneCyanSecondary = Color(0xFF7BD0FF)
val ArcaneCyanContainer = Color(0xFF00A6E0)

// Infernal Nether Magenta (Astaroth Theme)
val InfernalMagentaTertiary = Color(0xFFFCB4FF)
val InfernalMagentaContainer = Color(0xFFF487FF)
val OnMagenta = Color(0xFF580065)

// Accent Demon Specific Tokens
val AsmodeusYellow = Color(0xFFFFD54F)
val AstarothPurple = Color(0xFFE040FB)
val RitualSafeGreen = Color(0xFF4ADE80)

private val DarkColorScheme = darkColorScheme(
    primary = AmberGoldPrimary,
    onPrimary = OnAmberGold,
    primaryContainer = AmberGoldContainer,
    secondary = ArcaneCyanSecondary,
    secondaryContainer = ArcaneCyanContainer,
    tertiary = InfernalMagentaTertiary,
    tertiaryContainer = InfernalMagentaContainer,
    background = ObsidianSurface,
    surface = ObsidianSurface,
    surfaceContainer = ObsidianSurfaceContainer,
    surfaceContainerHigh = ObsidianSurfaceHigh,
    surfaceContainerLowest = ObsidianSurfaceLowest,
    onSurface = OnSurfaceLight,
    onSurfaceVariant = OnSurfaceVariant
)

// ==========================================
// 2. Shapes: Friendly, Bubbly & Tactile
// ==========================================
val Shapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

// ==========================================
// 3. Typography: Grand Serif Headers & Sharp Stats
// ==========================================
val Typography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.5.sp,
        color = AmberGoldPrimary
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.25.sp,
        color = AmberGoldPrimary
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        color = OnSurfaceLight
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = OnSurfaceVariant
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 1.sp
    )
)

@Composable
fun SummonDemonsSafelyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        shapes = Shapes,
        typography = Typography,
        content = content
    )
}