package com.summondemons.safely.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.summondemons.safely.R
import com.summondemons.safely.data.Demon
import com.summondemons.safely.ui.fx.Jitter
import com.summondemons.safely.ui.fx.LocalReduceMotion
import com.summondemons.safely.ui.fx.rememberSigil
import com.summondemons.safely.ui.theme.demonColors
import kotlinx.coroutines.delay

private const val VIEWBOX = 100f
private const val SIGIL_STROKE = 3.5f
private const val RING_RADIUS = 46f
private const val RING_STROKE = 2.5f
private const val RING_ALPHA = 0.8f
private const val BURN_DELAY_MS = 400L
private const val BURN_MS = 1_400
private const val BURN_GROW = 0.4f
private val AvatarSize = 40.dp
private val SelectedAvatarSize = 52.dp
private val Ring = 2.dp
private val Glow = 8.dp

/**
 * Avatar rail item (ADR-0016): the demon's sigil in accent strokes while unbound, his portrait
 * once bound. Right after a summon ([burn]) the sigil burns away over the portrait once.
 */
@Composable
fun SigilAvatar(
    demon: Demon,
    bound: Boolean,
    burn: Boolean,
    restless: Boolean,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val accent = MaterialTheme.demonColors.accent(demon.accent)
    val reduceMotion = LocalReduceMotion.current
    val size by animateDpAsState(if (selected) SelectedAvatarSize else AvatarSize, label = "avatarSize")
    val sigil = rememberSigil(demon.sigil)
    val burnProgress = remember(burn) { Animatable(0f) }
    LaunchedEffect(burn) {
        if (burn) {
            delay(BURN_DELAY_MS)
            burnProgress.animateTo(1f, tween(BURN_MS))
        }
    }
    val label = if (bound) demon.name else stringResource(R.string.avatar_sealed, demon.name)
    Jitter(
        enabled = restless && bound,
        modifier = modifier
            .minimumInteractiveComponentSize()
            .selectable(selected = selected, enabled = enabled, role = Role.Tab, onClick = onClick)
            .semantics { contentDescription = label },
    ) {
        Box(
            Modifier
                .size(size)
                .then(if (selected) Modifier.shadow(Glow, CircleShape, ambientColor = accent, spotColor = accent) else Modifier)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(colors.surfaceContainer, colors.surfaceContainerLowest)))
                .border(Ring, if (selected) accent else colors.outlineVariant, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (bound) {
                Image(painterResource(demon.avatarArt), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
            if (!bound || burn) {
                val backdrop = colors.surfaceContainerLowest
                Canvas(
                    Modifier.fillMaxSize().graphicsLayer {
                        val p = if (burn) burnProgress.value else 0f
                        alpha = 1f - p
                        val grow = if (reduceMotion) 1f else 1f + BURN_GROW * p // reduced motion: cross-fade only
                        scaleX = grow
                        scaleY = grow
                    },
                ) {
                    if (burn) drawCircle(backdrop)
                    val unit = this.size.minDimension / VIEWBOX
                    scale(unit, unit, pivot = Offset.Zero) {
                        val stroke = Stroke(SIGIL_STROKE, cap = StrokeCap.Round, join = StrokeJoin.Round)
                        sigil.paths.forEach { drawPath(it, accent, style = stroke) }
                        drawCircle(accent.copy(alpha = RING_ALPHA), RING_RADIUS, Offset(VIEWBOX / 2, VIEWBOX / 2), style = Stroke(RING_STROKE))
                    }
                }
            }
        }
    }
}
