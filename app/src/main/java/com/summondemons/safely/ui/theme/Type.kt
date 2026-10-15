package com.summondemons.safely.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// DESIGN.md: Cinzel and Inter until bundled in res/font/ → Serif / SansSerif fallbacks.
private val Display = FontFamily.Serif
private val Body = FontFamily.SansSerif
private const val TABULAR_FIGURES = "tnum"

val AppTypography = Typography(
    displaySmall = TextStyle(fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 40.sp),
    headlineLarge = TextStyle(fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 32.sp),
    headlineMedium = TextStyle(fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 28.sp),
    titleLarge = TextStyle(fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 24.sp),
    bodyLarge = TextStyle(fontFamily = Body, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 24.sp, fontFeatureSettings = TABULAR_FIGURES),
    bodyMedium = TextStyle(fontFamily = Body, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp, fontFeatureSettings = TABULAR_FIGURES),
    bodySmall = TextStyle(fontFamily = Body, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp, fontFeatureSettings = TABULAR_FIGURES),
    labelMedium = TextStyle(fontFamily = Body, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 16.sp, fontFeatureSettings = TABULAR_FIGURES),
    labelSmall = TextStyle(fontFamily = Body, fontWeight = FontWeight.Bold, fontSize = 10.sp, lineHeight = 12.sp, fontFeatureSettings = TABULAR_FIGURES),
)
