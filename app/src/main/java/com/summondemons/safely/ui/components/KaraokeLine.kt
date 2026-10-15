package com.summondemons.safely.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import com.summondemons.safely.ritual.LatinSyllables

private const val LIT_GLOW_ALPHA = 0.6f
private const val LIT_GLOW_BLUR = 8f
private const val NEWEST_GLOW_BLUR = 18f

/** The current chant phrase, lit syllable by syllable in [accent]; the newest syllable glows brightest. */
@Composable
fun KaraokeLine(text: String, lit: Int, accent: Color, dim: Color, style: TextStyle, modifier: Modifier = Modifier) {
    val spans = remember(text) { LatinSyllables.spans(text) }
    val annotated = remember(text, lit, accent, dim) {
        val newest = spans.getOrNull(lit - 1)
        val litEnd = newest?.end ?: 0
        buildAnnotatedString {
            append(text)
            addStyle(SpanStyle(color = dim), 0, text.length)
            if (litEnd > 0) {
                addStyle(SpanStyle(color = accent, shadow = Shadow(accent.copy(alpha = LIT_GLOW_ALPHA), blurRadius = LIT_GLOW_BLUR)), 0, litEnd)
            }
            if (newest != null) {
                addStyle(SpanStyle(shadow = Shadow(accent, blurRadius = NEWEST_GLOW_BLUR)), newest.start, newest.end)
            }
        }
    }
    Text(annotated, style = style, textAlign = TextAlign.Center, modifier = modifier)
}
