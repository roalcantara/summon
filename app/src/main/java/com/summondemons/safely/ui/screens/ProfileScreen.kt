package com.summondemons.safely.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.summondemons.safely.R
import com.summondemons.safely.data.Demon
import com.summondemons.safely.platform.TadaPlayer
import com.summondemons.safely.ui.components.DemonCutout
import com.summondemons.safely.ui.components.PrimaryActionButton
import com.summondemons.safely.ui.components.RibbonBanner
import com.summondemons.safely.ui.components.RotatingSunburstBackground
import com.summondemons.safely.ui.fx.HeatDistorted
import com.summondemons.safely.ui.fx.HeatShimmer
import com.summondemons.safely.ui.fx.LocalReduceMotion
import com.summondemons.safely.ui.fx.SharedDemonArt
import com.summondemons.safely.ui.fx.rememberFrameTimeMs
import com.summondemons.safely.ui.fx.rememberParallax
import com.summondemons.safely.ui.theme.demonColors
import com.summondemons.safely.ui.theme.spacing
import kotlin.math.PI
import kotlin.math.sin

private const val SUNBURST_ALPHA = 0.16f
private const val SCENE_DIM = 0.7f
private const val PANEL_MAX_FRACTION = 0.42f
private const val BOUNCE_FROM = 0.85f
private const val FLOAT_PERIOD_S = 4f
private val SceneBlur = 12.dp
private val FloatAmplitude = 8.dp

@Composable
fun ProfileScreen(
    demon: Demon,
    isNewSummon: Boolean,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    /** Debug builds, view mode only (PRODUCT.md Debug aids). */
    onUnsummon: (() -> Unit)? = null,
) {
    val accent = MaterialTheme.demonColors.accent(demon.accent)
    val colors = MaterialTheme.colorScheme
    val spacing = MaterialTheme.spacing
    val reduceMotion = LocalReduceMotion.current

    if (isNewSummon) {
        val context = LocalContext.current
        // Saved so activity recreation (font scale, locale) doesn't replay the fanfare.
        var tadaPlayed by rememberSaveable { mutableStateOf(false) }
        DisposableEffect(Unit) {
            val player = if (tadaPlayed) null else TadaPlayer.play(context).also { tadaPlayed = true }
            onDispose { player?.release() }
        }
    }
    val bounce = remember { Animatable(if (isNewSummon) BOUNCE_FROM else 1f) }
    LaunchedEffect(Unit) {
        bounce.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
    }
    val parallax by rememberParallax()
    val time by rememberFrameTimeMs()
    val floatPx = with(LocalDensity.current) { FloatAmplitude.toPx() }

    BoxWithConstraints(modifier.fillMaxSize().background(colors.background)) {
        val panelMax = maxHeight * PANEL_MAX_FRACTION
        HeatDistorted(Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(demon.sceneArt),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().blur(SceneBlur),
            )
        }
        Box(Modifier.fillMaxSize().background(colors.surfaceContainerLowest.copy(alpha = SCENE_DIM)))
        RotatingSunburstBackground(accent.copy(alpha = SUNBURST_ALPHA), Modifier.fillMaxSize(), rotating = !reduceMotion)
        HeatShimmer(accent, Modifier.fillMaxSize())

        Column(Modifier.fillMaxSize().safeDrawingPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
            if (isNewSummon) RibbonBanner(stringResource(R.string.profile_banner), Modifier.padding(top = spacing.lg))
            SharedDemonArt(
                demonId = demon.id,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = spacing.xl, vertical = spacing.md)
                    .graphicsLayer {
                        val bob = if (reduceMotion) 0f else sin(time / 1_000f * 2f * PI.toFloat() / FLOAT_PERIOD_S) * floatPx
                        scaleX = bounce.value
                        scaleY = bounce.value
                        translationX = parallax.x
                        translationY = parallax.y + bob
                    },
            ) {
                DemonCutout(demon.cutoutArt, demon.name, accent, Modifier.fillMaxSize())
            }
            Surface(
                shape = MaterialTheme.shapes.large.copy(bottomStart = CornerSize(0.dp), bottomEnd = CornerSize(0.dp)),
                color = colors.surfaceContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    Modifier.padding(spacing.lg),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(spacing.md),
                ) {
                    Column(
                        Modifier.heightIn(max = panelMax).verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(spacing.sm),
                    ) {
                        Text(demon.name, style = MaterialTheme.typography.displaySmall, color = accent)
                        Text(demon.title, style = MaterialTheme.typography.titleLarge, color = colors.primary, textAlign = TextAlign.Center)
                        Text(demon.description, style = MaterialTheme.typography.bodyLarge, color = colors.onSurface, textAlign = TextAlign.Center)
                        Surface(shape = MaterialTheme.shapes.medium, color = colors.surfaceContainerHigh, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                "“${demon.quote}”",
                                style = MaterialTheme.typography.bodyMedium,
                                fontStyle = FontStyle.Italic,
                                color = colors.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(spacing.lg),
                            )
                        }
                    }
                    PrimaryActionButton(
                        text = stringResource(if (isNewSummon) R.string.action_awesome else R.string.action_back),
                        onClick = onDone,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (onUnsummon != null) {
                        OutlinedButton(onClick = onUnsummon, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.action_unsummon))
                        }
                    }
                }
            }
        }
    }
}
