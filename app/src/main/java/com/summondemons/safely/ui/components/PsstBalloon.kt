package com.summondemons.safely.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.summondemons.safely.ui.fx.attentionGlow
import com.summondemons.safely.ui.fx.rememberAttention
import com.summondemons.safely.ui.theme.demonColors
import com.summondemons.safely.ui.theme.fontScaleCapped
import com.summondemons.safely.ui.theme.spacing
import com.summondemons.safely.ui.theme.textRoles

private val BalloonMaxWidth = 128.dp
private val Tail = 12.dp
private val Rim = 2.dp
private val TextSize = 13.sp
private val TextMinSize = 8.sp

/**
 * The "Psst" balloon (ADR-0036): [text] in a violet balloon with a gold rim and a tail at its
 * bottom start (toward the demon), breathing with the attention glow and a faint green halo.
 */
@Composable
fun PsstBalloon(text: String, replying: Boolean, onTap: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.demonColors
    val gold = MaterialTheme.colorScheme.primary
    val breath = rememberAttention(active = !replying)
    val size = TextSize.fontScaleCapped()
    Box(
        modifier
            .widthIn(max = BalloonMaxWidth)
            .attentionGlow(breath, colors.psstHalo)
            .attentionGlow(breath, colors.psstViolet)
            .drawBehind {
                val tail = Tail.toPx()
                val body = Size(this.size.width, this.size.height - tail)
                val radius = body.height / 2f
                val path = Path().apply {
                    addRoundRect(androidx.compose.ui.geometry.RoundRect(0f, 0f, body.width, body.height, radius, radius))
                    moveTo(radius * 0.6f, body.height - 1f)
                    lineTo(radius * 0.2f, this@drawBehind.size.height)
                    lineTo(radius * 1.4f, body.height - 1f)
                    close()
                }
                drawPath(path, Brush.radialGradient(listOf(colors.psstViolet, colors.psstVioletDeep), Offset(body.width * 0.4f, body.height * 0.35f), body.width))
                drawPath(path, gold, style = Stroke(Rim.toPx()))
            }
            .clickable(role = Role.Button, onClick = onTap)
            .semantics { if (replying) liveRegion = LiveRegionMode.Polite }
            .padding(start = MaterialTheme.spacing.md, end = MaterialTheme.spacing.md, top = MaterialTheme.spacing.sm, bottom = MaterialTheme.spacing.sm + Tail),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = MaterialTheme.textRoles.name.copy(fontSize = size, textAlign = TextAlign.Center),
            color = colors.inkHighlight,
            maxLines = 3,
            autoSize = TextAutoSize.StepBased(minFontSize = TextMinSize, maxFontSize = size),
        )
    }
}
