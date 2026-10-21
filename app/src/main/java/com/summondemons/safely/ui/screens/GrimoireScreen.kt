package com.summondemons.safely.ui.screens

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.summondemons.safely.R
import com.summondemons.safely.data.Demon
import com.summondemons.safely.data.DemonCatalog
import com.summondemons.safely.platform.GameSounds
import com.summondemons.safely.platform.LocalGameSounds
import com.summondemons.safely.progress.DemonRecord
import com.summondemons.safely.progress.Progress
import com.summondemons.safely.progress.ProgressRules
import com.summondemons.safely.progress.Reveal
import com.summondemons.safely.ui.GameUiState
import com.summondemons.safely.ui.components.BadgeKey
import com.summondemons.safely.ui.components.BadgeRow
import com.summondemons.safely.ui.components.MenuButton
import com.summondemons.safely.ui.components.TitlePlaque
import com.summondemons.safely.ui.components.CUTOUT_ASPECT
import com.summondemons.safely.ui.components.DemonCutout
import com.summondemons.safely.ui.components.DossierCarousel
import com.summondemons.safely.ui.components.FloorCircle
import com.summondemons.safely.ui.components.InfoBand
import com.summondemons.safely.ui.components.QuoteSlot
import com.summondemons.safely.ui.components.QuoteSlotState
import com.summondemons.safely.ui.components.RibbonBanner
import com.summondemons.safely.ui.components.RotatingSunburstBackground
import com.summondemons.safely.ui.components.SECRET_PAGE
import com.summondemons.safely.ui.components.StageBackground
import com.summondemons.safely.ui.fx.Jitter
import com.summondemons.safely.ui.fx.LocalReduceMotion
import com.summondemons.safely.ui.fx.ParticleField
import com.summondemons.safely.ui.fx.ParticleStyle
import com.summondemons.safely.ui.fx.SharedDemonArt
import com.summondemons.safely.ui.fx.rememberFrameTimeMs
import com.summondemons.safely.ui.fx.rememberSigil
import com.summondemons.safely.ui.theme.SummonDemonsSafelyTheme
import com.summondemons.safely.ui.theme.demonColors
import com.summondemons.safely.ui.theme.spacing
import com.summondemons.safely.ui.theme.stage
import com.summondemons.safely.ui.theme.textRoles
import kotlinx.coroutines.delay
import java.time.ZonedDateTime
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/** Share of the area below the avatar rail taken by the card; raise for more text, lower for bigger art. */
private const val CARD_HEIGHT_FRACTION = 0.5f
private const val BADGE_INFO_MS = 5_000L
private const val AVATAR_BURN_CONSUME_MS = 2_200L
private const val STAGE_CROSSFADE_MS = 500
private const val DIM_BOUND = 0.35f
private const val DIM_SEALED = 0.55f
private const val EMBER_ALPHA = 0.67f
private const val EMBERS = 20
private const val PARTY_EMBERS = 50
private const val SMOKE = 8
private const val SUNBURST_ALPHA = 0.2f
private const val FLOOR_WIDTH = 0.92f
private const val FLOOR_ASPECT = 4f
private const val SCENE_HEIGHT = 0.94f
private const val SCENE_BRIGHTNESS = 0.35f
private const val SCENE_SHADE_START = 0.55f
private const val SCENE_SHADE_ALPHA = 0.7f
private const val FLOAT_PERIOD_S = 4f
private const val PARTY_POP_FROM = 0.85f
private const val CARD_ALPHA = 0.95f
private const val METAL_RING_ALPHA = 0.45f
private const val STAGE_ARROW_PRESSED = 0.92f
private val StageArrowWidth = 56.dp
private val StageArrowHeight = 75.dp
private val RailHeight = 72.dp
private val FloatAmplitude = 6.dp
private val SceneBlur = 3.dp
private val StageSwipe = 60.dp
private val CarouselMinHeight = 88.dp
private val CarouselTopPadding = 12.dp
private val MedallionSize = 48.dp
private val BannerDrop = 30.dp
private val NameSize = 26.sp
private val NumberSize = 18.sp
private val TitleSize = 12.sp

/** The Grimoire (ADR-0016): avatar rail, a stage over a static demon card, and the new-summon party. */
@Composable
fun GrimoireScreen(
    state: GameUiState,
    onBack: () -> Unit,
    onSelect: (String) -> Unit,
    onSummon: (String) -> Unit,
    onVisit: (String) -> Unit,
    onDismissCelebration: () -> Unit,
    onConsumeReveal: (String, Reveal) -> Unit,
    onSecretShown: () -> Unit,
    onMenu: () -> Unit,
    modifier: Modifier = Modifier,
    demons: List<Demon> = DemonCatalog.all,
) {
    val demon = demons.firstOrNull { it.id == state.selectedId } ?: demons.first()
    val record = state.record(demon.id)
    val bound = record.bound
    val party = state.celebration?.takeIf { it.demonId == demon.id }
    val accent = MaterialTheme.demonColors.accent(demon.accent)
    val colors = MaterialTheme.colorScheme
    val spacing = MaterialTheme.spacing
    val reduceMotion = LocalReduceMotion.current
    val sounds = LocalGameSounds.current
    val density = LocalDensity.current

    // Captured when this demon is first shown: the visit clears restlessness, but he still sulks for this viewing.
    // Saved, so a configuration change doesn't re-read the already-visited record.
    val restless = rememberSaveable(demon.id, state.today) { ProgressRules.isRestless(record, state.today) }
    // Ink flags are read once per viewing and consumed at once; the saved values keep the animation alive.
    val inkCodex = rememberSaveable(demon.id) { record.revealed && record.revealPending.codex }
    val inkBattle = rememberSaveable(demon.id) { record.revealed && record.revealPending.battle }
    var page by remember(demon.id) { mutableIntStateOf(0) }
    var badge by remember(demon.id) { mutableStateOf<BadgeKey?>(null) }
    var badgeTaps by remember { mutableIntStateOf(0) }
    var shake by remember { mutableIntStateOf(0) }
    var badgeRowBounds by remember { mutableStateOf(Rect.Zero) }
    var rootOrigin by remember { mutableStateOf(Offset.Zero) }
    var tadaFor by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(demon.id, bound, state.today) { if (bound) onVisit(demon.id) }
    LaunchedEffect(demon.id) {
        if (inkCodex) onConsumeReveal(demon.id, Reveal.Codex)
        if (inkBattle) onConsumeReveal(demon.id, Reveal.Battle)
    }
    LaunchedEffect(demon.id, bound, record.revealPending.avatar) {
        if (bound && record.revealPending.avatar) {
            delay(AVATAR_BURN_CONSUME_MS)
            onConsumeReveal(demon.id, Reveal.Avatar)
        }
    }
    LaunchedEffect(state.openSecret) {
        if (state.openSecret) {
            page = SECRET_PAGE
            onSecretShown()
        }
    }
    LaunchedEffect(badge, badgeTaps) {
        if (badge != null) {
            delay(BADGE_INFO_MS)
            badge = null
        }
    }
    // Saved, so recreating the activity doesn't replay the fanfare.
    val partyKey = party?.let { "${it.demonId}@${state.today}" }
    LaunchedEffect(partyKey) {
        if (partyKey != null && tadaFor != partyKey) {
            sounds?.play(GameSounds.Cue.Tada)
            tadaFor = partyKey
        }
    }
    BackHandler(enabled = party != null) { onDismissCelebration() }

    val primary: () -> Unit = {
        when {
            party != null -> onDismissCelebration()
            bound -> Unit
            !state.canSummonToday -> shake++
            else -> onSummon(demon.id)
        }
    }
    val index = demons.indexOf(demon)
    val step: (Int) -> Unit = { delta -> onSelect(demons[(index + delta + demons.size) % demons.size].id) }

    Box(
        modifier
            .fillMaxSize()
            .background(colors.background)
            .onGloballyPositioned { rootOrigin = it.boundsInRoot().topLeft }
            // Observe presses without consuming them: a tap outside the badge row clears the
            // badge and still does its own action (e.g. summon).
            .pointerInput(demon.id) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (event.type == PointerEventType.Press && event.changes.none { badgeRowBounds.contains(it.position + rootOrigin) }) {
                            badge = null
                        }
                    }
                }
            },
    ) {
        Crossfade(targetState = demon.house.stage(), animationSpec = tween(STAGE_CROSSFADE_MS), label = "stage") { stage ->
            StageBackground(stage, dim = if (bound) DIM_BOUND else DIM_SEALED, modifier = Modifier.fillMaxSize())
        }
        if (party != null) RotatingSunburstBackground(accent.copy(alpha = SUNBURST_ALPHA), Modifier.fillMaxSize(), rotating = !reduceMotion)
        ParticleField(
            accent.copy(alpha = EMBER_ALPHA), ParticleStyle.Embers, Modifier.fillMaxSize(),
            count = if (party != null) PARTY_EMBERS else EMBERS, seed = demon.id.hashCode(),
        )
        if (!bound) {
            ParticleField(colors.onSurfaceVariant, ParticleStyle.Smoke, Modifier.fillMaxSize(), count = SMOKE, seed = demon.id.hashCode() + 1)
        }

        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            // Like system back (BackHandler above), the rail's back arrow dismisses the celebration first.
            GrimoireHeader({ if (party != null) onDismissCelebration() else onBack() }, onMenu)
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                var fixedPx by remember { mutableIntStateOf(0) }
                val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                // Fixed at the default font scale; grows only if header + band + badges + 88 dp don't fit.
                val minimum = with(density) { fixedPx.toDp() } + spacing.lg + spacing.md + CarouselTopPadding + CarouselMinHeight + navBottom
                val cardHeight = maxOf(maxHeight * CARD_HEIGHT_FRACTION, minimum)
                Column(Modifier.fillMaxSize()) {
                    Box(Modifier.weight(1f).fillMaxWidth()) {
                        Stage(
                            demon = demon,
                            bound = bound,
                            restless = restless,
                            party = party != null,
                            canSummon = state.canSummonToday,
                            quote = stringResource(if (restless) demon.restlessQuote else demon.quote),
                            hours = state.hoursToNextSummon,
                            shake = shake,
                            onPrimary = primary,
                            onSwipe = step,
                            modifier = Modifier.fillMaxSize(),
                        )
                        // Arrows make the stage swipe visible (RC, TODO r62v); hidden while celebrating, like the swipe.
                        if (party == null && demons.size > 1) {
                            StageArrow(next = false, stringResource(R.string.grimoire_previous), Modifier.align(Alignment.CenterStart)) { step(-1) }
                            StageArrow(next = true, stringResource(R.string.grimoire_next), Modifier.align(Alignment.CenterEnd)) { step(1) }
                        }
                    }
                    DemonCard(
                        demons = demons,
                        demon = demon,
                        record = record,
                        badge = badge,
                        onBadge = {
                            badge = it
                            badgeTaps++
                        },
                        page = page,
                        onPage = { page = it },
                        inkCodex = inkCodex,
                        inkBattle = inkBattle,
                        onFixedHeight = { fixedPx = it },
                        onBadgeRowBounds = { badgeRowBounds = it },
                        modifier = Modifier.fillMaxWidth().height(cardHeight),
                    )
                }
                if (party != null) PartyBanner(Modifier.align(Alignment.TopCenter).padding(top = spacing.sm))
            }
        }
    }
}

/**
 * Back arrow, the title plaque and the menu gear (RC, TODO TKJp; replaces the avatar rail,
 * whose symbols players could not read; proposal docs/superpowers/specs/2026-10-21-grimoire-header-proposal.md).
 */
@Composable
private fun GrimoireHeader(onBack: () -> Unit, onMenu: () -> Unit) {
    val spacing = MaterialTheme.spacing
    val back = stringResource(R.string.grimoire_back)
    Box(Modifier.fillMaxWidth().height(RailHeight).padding(horizontal = spacing.sm)) {
        IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart).semantics { contentDescription = back }) {
            Text("←", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.clearAndSetSemantics { })
        }
        TitlePlaque(stringResource(R.string.grimoire_title), Modifier.align(Alignment.Center))
        MenuButton(onMenu, Modifier.align(Alignment.CenterEnd))
    }
}

/** Previous / next demon on the stage edge: the gilded arrow art, mirrored for "next"; shrinks while pressed. */
@Composable
private fun StageArrow(next: Boolean, label: String, modifier: Modifier, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale = if (pressed && !LocalReduceMotion.current) STAGE_ARROW_PRESSED else 1f
    Image(
        painter = painterResource(R.drawable.grimoire_arrow_large),
        contentDescription = label,
        modifier = modifier
            .padding(horizontal = MaterialTheme.spacing.xs)
            .size(StageArrowWidth, StageArrowHeight)
            .graphicsLayer {
                scaleX = if (next) -scale else scale
                scaleY = scale
            }
            .clickable(interaction, indication = null, role = Role.Button, onClick = onClick),
    )
}

@Composable
private fun Stage(
    demon: Demon,
    bound: Boolean,
    restless: Boolean,
    party: Boolean,
    canSummon: Boolean,
    quote: String,
    hours: Int,
    shake: Int,
    onPrimary: () -> Unit,
    onSwipe: (Int) -> Unit,
    modifier: Modifier,
) {
    val accent = MaterialTheme.demonColors.accent(demon.accent)
    val spacing = MaterialTheme.spacing
    val reduceMotion = LocalReduceMotion.current
    val swipePx = with(LocalDensity.current) { StageSwipe.toPx() }
    val currentSwipe by rememberUpdatedState(onSwipe)
    val time by if (reduceMotion) remember { mutableLongStateOf(0L) } else rememberFrameTimeMs()
    val floatPx = with(LocalDensity.current) { FloatAmplitude.toPx() }
    val pop = remember(demon.id, party) { Animatable(if (party && !reduceMotion) PARTY_POP_FROM else 1f) }
    LaunchedEffect(pop) { pop.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow)) }
    val slot = when {
        party -> QuoteSlotState.Celebration
        bound -> QuoteSlotState.Bound
        canSummon -> QuoteSlotState.Ready
        else -> QuoteSlotState.Cooling
    }
    val label = when (slot) {
        QuoteSlotState.Celebration -> stringResource(R.string.action_awesome)
        QuoteSlotState.Bound -> demon.name
        QuoteSlotState.Ready -> stringResource(R.string.cd_summon, demon.name)
        QuoteSlotState.Cooling -> pluralStringResource(R.plurals.cd_cooling, hours, hours)
    }
    Column(
        modifier
            .pointerInput(party) {
                if (party) return@pointerInput // no swiping during the celebration
                var total = 0f
                detectHorizontalDragGestures(
                    onDragStart = { total = 0f },
                    onDragEnd = { if (abs(total) > swipePx) currentSwipe(if (total < 0f) 1 else -1) },
                ) { _, amount -> total += amount }
            }
            .padding(horizontal = spacing.xl)
            .padding(bottom = spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
            Box(Modifier.aspectRatio(CUTOUT_ASPECT, matchHeightConstraintsFirst = true)) {
                if (bound) SceneBackdrop(demon.sceneArt, Modifier.align(Alignment.TopCenter))
                FloorCircle(
                    accent = accent,
                    pulse = !party && !bound && canSummon,
                    sigil = rememberSigil(demon.sigil),
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(FLOOR_WIDTH).aspectRatio(FLOOR_ASPECT),
                )
                SharedDemonArt(
                    demonId = demon.id,
                    modifier = Modifier.fillMaxSize().graphicsLayer {
                        translationY = if (reduceMotion) 0f else sin(time / 1_000f * 2f * PI.toFloat() / FLOAT_PERIOD_S) * floatPx
                        scaleX = pop.value
                        scaleY = pop.value
                    },
                ) {
                    Jitter(enabled = restless && bound, modifier = Modifier.fillMaxSize()) {
                        DemonCutout(
                            art = demon.cutoutArt,
                            contentDescription = null,
                            accent = accent,
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    enabled = !bound || party,
                                    onClick = onPrimary,
                                )
                                .semantics { contentDescription = label },
                            silhouette = if (bound) 0f else 1f,
                            alignment = Alignment.BottomCenter,
                        )
                    }
                }
            }
        }
        QuoteSlot(slot, demon, quote, hours, shake, onTap = onPrimary, modifier = Modifier.padding(top = spacing.md))
    }
}

/** The demon's own scene, dimmed and softened, framing the cut-out. */
@Composable
private fun SceneBackdrop(@DrawableRes art: Int, modifier: Modifier) {
    val shade = MaterialTheme.colorScheme.surfaceContainerLowest
    val dim = remember { ColorMatrix().apply { setToScale(SCENE_BRIGHTNESS, SCENE_BRIGHTNESS, SCENE_BRIGHTNESS, 1f) } }
    Box(modifier.fillMaxWidth().fillMaxHeight(SCENE_HEIGHT).clip(MaterialTheme.shapes.medium)) {
        Image(
            painter = painterResource(art),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            colorFilter = ColorFilter.colorMatrix(dim),
            modifier = Modifier.fillMaxSize().blur(SceneBlur), // API 31+; plain dimmed art below
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(SCENE_SHADE_START to Color.Transparent, 1f to shade.copy(alpha = SCENE_SHADE_ALPHA)),
            ),
        )
    }
}

@Composable
private fun DemonCard(
    demons: List<Demon>,
    demon: Demon,
    record: DemonRecord,
    badge: BadgeKey?,
    onBadge: (BadgeKey?) -> Unit,
    page: Int,
    onPage: (Int) -> Unit,
    inkCodex: Boolean,
    inkBattle: Boolean,
    onFixedHeight: (Int) -> Unit,
    onBadgeRowBounds: (Rect) -> Unit,
    modifier: Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val spacing = MaterialTheme.spacing
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large.copy(bottomStart = CornerSize(0.dp), bottomEnd = CornerSize(0.dp)),
        color = colors.surfaceContainerLow.copy(alpha = CARD_ALPHA),
        border = BorderStroke(1.dp, colors.outlineVariant),
    ) {
        Column(Modifier.navigationBarsPadding().padding(top = spacing.lg, bottom = spacing.md)) {
            Column(Modifier.onSizeChanged { onFixedHeight(it.height) }) {
                CardHeader(demon, record.bound, Modifier.padding(horizontal = spacing.lg))
                InfoBand(demons, demon, badge, Modifier.padding(horizontal = spacing.lg).padding(top = spacing.lg))
                BadgeRow(
                    demon = demon,
                    selected = badge,
                    onSelect = onBadge,
                    modifier = Modifier
                        .padding(horizontal = spacing.lg)
                        .padding(top = spacing.lg)
                        .onGloballyPositioned { onBadgeRowBounds(it.boundsInRoot()) },
                )
            }
            DossierCarousel(
                demon = demon,
                record = record,
                inkCodex = inkCodex,
                inkBattle = inkBattle,
                page = page,
                onPage = onPage,
                modifier = Modifier.weight(1f).fillMaxWidth().padding(top = CarouselTopPadding).padding(horizontal = spacing.xs),
            )
        }
    }
}

/** Tarot-style header: number medallion in the House metal, name, title. */
@Composable
private fun CardHeader(demon: Demon, bound: Boolean, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    val metal = MaterialTheme.demonColors.metal(demon.house)
    val accent = MaterialTheme.demonColors.accent(demon.accent)
    val roles = MaterialTheme.textRoles
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(MedallionSize)
                .background(colors.surfaceContainerLow, CircleShape)
                .clip(CircleShape)
                .background(metal.copy(alpha = METAL_RING_ALPHA), CircleShape)
                .padding(1.dp)
                .background(colors.surfaceContainerLow, CircleShape)
                .padding(2.dp)
                .background(metal, CircleShape)
                .padding(1.5.dp)
                .background(colors.surfaceContainerLow, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(demon.number.toString(), style = roles.name.copy(fontSize = NumberSize), color = metal)
        }
        Column(Modifier.weight(1f)) {
            Text(
                demon.name,
                style = roles.name.copy(fontSize = NameSize),
                color = if (bound) accent else colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                stringResource(demon.title),
                style = roles.body.copy(fontSize = TitleSize),
                color = colors.outline,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** "New Demon Received!" drops in at the top of the stage. */
@Composable
private fun PartyBanner(modifier: Modifier) {
    val reduceMotion = LocalReduceMotion.current
    val dropPx = with(LocalDensity.current) { BannerDrop.toPx() }
    val drop = remember { Animatable(if (reduceMotion) 0f else -dropPx) }
    LaunchedEffect(Unit) { drop.animateTo(0f, spring(Spring.DampingRatioMediumBouncy)) }
    RibbonBanner(
        stringResource(R.string.celebration_banner),
        modifier.graphicsLayer {
            translationY = drop.value
            alpha = 1f - drop.value / -dropPx * 0.9f
        },
    )
}

@Preview
@Composable
private fun GrimoireScreenPreview() {
    SummonDemonsSafelyTheme {
        GrimoireScreen(
            state = GameUiState(
                loaded = true,
                progress = Progress(),
                now = ZonedDateTime.now(),
                selectedId = DemonCatalog.all.first().id,
            ),
            onBack = {},
            onSelect = {},
            onSummon = {},
            onVisit = {},
            onDismissCelebration = {},
            onConsumeReveal = { _, _ -> },
            onSecretShown = {},
            onMenu = {},
        )
    }
}
