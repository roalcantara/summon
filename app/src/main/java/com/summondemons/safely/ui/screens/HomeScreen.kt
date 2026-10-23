package com.summondemons.safely.ui.screens
import androidx.activity.compose.BackHandler

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.summondemons.safely.R
import com.summondemons.safely.data.Demon
import com.summondemons.safely.data.DemonAccent
import com.summondemons.safely.data.House
import com.summondemons.safely.platform.GameSounds
import com.summondemons.safely.platform.LocalGameSounds
import com.summondemons.safely.ui.components.CompassRose
import com.summondemons.safely.ui.components.CueChip
import com.summondemons.safely.ui.components.CueTone
import com.summondemons.safely.ui.components.MenuButton
import com.summondemons.safely.ui.components.RestlessGrimoire
import com.summondemons.safely.ui.components.RibbonBanner
import com.summondemons.safely.ui.components.StageBackground
import com.summondemons.safely.ui.fx.LocalReduceMotion
import com.summondemons.safely.ui.fx.ParticleField
import com.summondemons.safely.ui.fx.ParticleStyle
import com.summondemons.safely.ui.fx.attentionHalo
import com.summondemons.safely.ui.fx.rememberAttention
import com.summondemons.safely.ui.theme.PillShape
import com.summondemons.safely.ui.theme.Stage
import com.summondemons.safely.ui.theme.SummonDemonsSafelyTheme
import com.summondemons.safely.ui.theme.demonColors
import com.summondemons.safely.ui.theme.spacing
import com.summondemons.safely.ui.theme.textRoles
import com.summondemons.safely.ui.theme.upper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val HOME_DIM = 0.5f
private const val EMBER_ALPHA = 0.53f
private const val EMBER_COUNT = 18
private const val OPEN_MS = 700L
private const val OPEN_REDUCED_MS = 150L
private const val OPEN_CHIME_VOLUME = 0.5f
private const val DAIS_BOTTOM_FRACTION = 0.22f
private const val DAIS_WIDTH_FRACTION = 0.86f
/** The count's distance from the bottom safe edge, as a share of the screen (scales like the dais). */
private const val COUNT_BOTTOM_FRACTION = 0.08f
private const val ROSE_TILT_DEG = 64f
/** In units of density; tune on device so the rose reads as a floor. */
private const val ROSE_CAMERA_DISTANCE = 12f
private const val COURT_BORDER_ALPHA = 0.5f
private const val SCRIM_ALPHA = 0.6f
private const val PANEL_ALPHA = 0.95f
private const val DUST_MS = 800
private const val DUST_ALPHA = 0.33f
private const val DUST_START = 0.6f
private const val DUST_END = 1.6f
private const val BANNER_POP_FROM = 0.9f
private val LogoSize = 96.dp
private val DaisHeight = 260.dp
private val RoseDrop = 90.dp
private val GrimoireWidth = 180.dp
private val GrimoireHeight = 210.dp
/** Empty space between the grimoire art's base and the bottom of its frame. */
private val GrimoireArtInset = 44.dp
private val DustWidth = 256.dp
private val DustHeight = 160.dp

/** Home — "The Restless Grimoire" (ADR-0014). */
@Composable
fun HomeScreen(
    boundCount: Int,
    demonCount: Int,
    courtHouses: Set<House>,
    /** The focus demon's accent while any demon is restless; null otherwise. */
    restlessAccent: DemonAccent?,
    showHint: Boolean,
    escaped: List<Demon>,
    onOpen: () -> Unit,
    onDismissEscape: () -> Unit,
    onMenu: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val spacing = MaterialTheme.spacing
    val reduceMotion = LocalReduceMotion.current
    val sounds = LocalGameSounds.current
    val scope = rememberCoroutineScope()
    var opening by remember { mutableStateOf(false) }
    // If the app left Home during the 700 ms opening, the navigation was dropped: show the book again.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { opening = false }
    val open: () -> Unit = {
        if (!opening) {
            opening = true
            sounds?.play(GameSounds.Cue.Chime4, OPEN_CHIME_VOLUME)
            scope.launch {
                delay(if (reduceMotion) OPEN_REDUCED_MS else OPEN_MS)
                onOpen()
            }
        }
    }
    val lockAccent = restlessAccent?.let { MaterialTheme.demonColors.accent(it) } ?: colors.primary

    BoxWithConstraints(modifier.fillMaxSize().background(colors.background)) {
        StageBackground(Stage.Home, HOME_DIM, Modifier.fillMaxSize())
        ParticleField(colors.primary.copy(alpha = EMBER_ALPHA), ParticleStyle.Embers, Modifier.fillMaxSize(), count = EMBER_COUNT, seed = 3)
        DustPuff(Modifier.align(Alignment.Center))

        Column(
            Modifier.fillMaxWidth().statusBarsPadding().padding(top = spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            Image(
                painter = painterResource(R.drawable.ic_game_logo),
                contentDescription = stringResource(R.string.app_name),
                modifier = Modifier.size(LogoSize).clip(CircleShape),
            )
            CourtChip(courtHouses)
        }

        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = maxHeight * DAIS_BOTTOM_FRACTION)
                .fillMaxWidth(DAIS_WIDTH_FRACTION)
                .height(DaisHeight),
        ) {
            CompassRose(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .offset(y = RoseDrop)
                    .graphicsLayer {
                        rotationX = ROSE_TILT_DEG
                        cameraDistance = ROSE_CAMERA_DISTANCE * density
                    },
            )
            RestlessGrimoire(
                restless = restlessAccent != null,
                accent = lockAccent,
                opening = opening,
                onOpen = open,
                modifier = Modifier.align(Alignment.BottomCenter).size(GrimoireWidth, GrimoireHeight),
            )
        }

        // The hint belongs to the book: 12 dp under the art's base (the art sits GrimoireArtInset above
        // its frame's bottom, room for the hop; the 48 dp target adds ~8 dp above the chip), still while
        // the book bobs, glowing until first used.
        if (showHint) {
            CueChip(
                stringResource(R.string.home_hint),
                CueTone.Neutral,
                Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = maxHeight * (1f - DAIS_BOTTOM_FRACTION) - GrimoireArtInset + spacing.xs)
                    .minimumInteractiveComponentSize()
                    .attentionHalo(rememberAttention(true), lockAccent) // on the chip itself, inside the 48 dp target
                    .clickable(onClick = open),
            )
        }
        // The count on its own line, centred in the band under the rose; it never moves with the hint.
        Text(
            pluralStringResource(R.plurals.home_bound, demonCount, boundCount, demonCount).upper(),
            style = MaterialTheme.textRoles.meta,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = maxHeight * COUNT_BOTTOM_FRACTION),
        )

        MenuButton(onMenu, Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(spacing.sm))
        if (escaped.isNotEmpty()) EscapeBanner(escaped, onDismissEscape)
    }
}

@Composable
private fun CourtChip(houses: Set<House>) {
    val colors = MaterialTheme.colorScheme
    val inCourt = houses.isNotEmpty()
    val color = if (inCourt) MaterialTheme.demonColors.houseSol else colors.outline
    val join = stringResource(R.string.home_court_join)
    val text = if (inCourt) {
        stringResource(R.string.home_court, House.entries.filter { it in houses }.joinToString(join) { it.planet })
    } else {
        stringResource(R.string.home_out_of_office)
    }
    Surface(
        shape = PillShape,
        color = Color.Transparent,
        border = BorderStroke(1.dp, if (inCourt) color.copy(alpha = COURT_BORDER_ALPHA) else colors.outlineVariant),
    ) {
        Text(
            text.upper(),
            style = MaterialTheme.textRoles.meta,
            color = color,
            modifier = Modifier.padding(horizontal = MaterialTheme.spacing.md, vertical = MaterialTheme.spacing.xs),
        )
    }
}

@Composable
private fun BoxScope.EscapeBanner(escaped: List<Demon>, onDismiss: () -> Unit) {
    BackHandler(onBack = onDismiss)
    val colors = MaterialTheme.colorScheme
    val spacing = MaterialTheme.spacing
    val reduceMotion = LocalReduceMotion.current
    val pop = remember { Animatable(if (reduceMotion) 1f else BANNER_POP_FROM) }
    LaunchedEffect(Unit) { pop.animateTo(1f, spring(Spring.DampingRatioMediumBouncy)) }
    val dismiss = stringResource(R.string.escape_dismiss)
    Box(
        Modifier
            .matchParentSize()
            .background(colors.surfaceContainerLowest.copy(alpha = SCRIM_ALPHA))
            .clickable(onClickLabel = dismiss, onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.padding(horizontal = spacing.xl).graphicsLayer {
                scaleX = pop.value
                scaleY = pop.value
            },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            RibbonBanner(stringResource(R.string.escape_banner))
            Surface(shape = MaterialTheme.shapes.medium, color = colors.surfaceContainerHigh.copy(alpha = PANEL_ALPHA)) {
                Column(
                    Modifier.padding(horizontal = spacing.lg, vertical = spacing.sm),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        if (escaped.size > 1) stringResource(R.string.escape_all) else stringResource(R.string.escape_one, escaped.first().name),
                        style = MaterialTheme.textRoles.body,
                        color = colors.onSurface,
                        textAlign = TextAlign.Center,
                    )
                    Text(dismiss.upper(), style = MaterialTheme.textRoles.meta, modifier = Modifier.padding(top = spacing.xs))
                }
            }
        }
    }
}

/** The book "slams" shut when coming back Home: a dust puff (skipped under reduced motion). */
@Composable
private fun DustPuff(modifier: Modifier) {
    if (LocalReduceMotion.current) return
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) { progress.animateTo(1f, tween(DUST_MS)) }
    Canvas(modifier.size(DustWidth, DustHeight)) {
        val p = progress.value
        if (p >= 1f) return@Canvas
        scale(DUST_START + (DUST_END - DUST_START) * p) {
            drawOval(Brush.radialGradient(listOf(color.copy(alpha = DUST_ALPHA), Color.Transparent)), alpha = 1f - p)
        }
    }
}

@Preview
@Composable
private fun HomeScreenPreview() {
    SummonDemonsSafelyTheme {
        HomeScreen(
            boundCount = 1,
            demonCount = 2,
            courtHouses = setOf(House.Sol),
            restlessAccent = DemonAccent.Asmodeus,
            showHint = true,
            escaped = emptyList(),
            onOpen = {},
            onDismissEscape = {},
            onMenu = {},
        )
    }
}
