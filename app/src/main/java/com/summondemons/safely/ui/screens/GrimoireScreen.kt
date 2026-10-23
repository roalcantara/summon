package com.summondemons.safely.ui.screens

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.summondemons.safely.R
import com.summondemons.safely.data.Demon
import com.summondemons.safely.data.DemonCatalog
import com.summondemons.safely.platform.GameSounds
import com.summondemons.safely.platform.Haptics
import com.summondemons.safely.platform.LocalGameSounds
import com.summondemons.safely.progress.Progress
import com.summondemons.safely.progress.ProgressRules
import com.summondemons.safely.progress.Reveal
import com.summondemons.safely.ui.GameUiState
import com.summondemons.safely.ui.components.BadgeKey
import com.summondemons.safely.ui.components.BadgeRow
import com.summondemons.safely.ui.components.CUTOUT_ASPECT
import com.summondemons.safely.ui.components.CardSide
import com.summondemons.safely.ui.components.DemonCard
import com.summondemons.safely.ui.components.DemonCutout
import com.summondemons.safely.ui.components.FloorCircle
import com.summondemons.safely.ui.components.MenuButton
import com.summondemons.safely.ui.components.PsstBubble
import com.summondemons.safely.ui.components.PsstBurst
import com.summondemons.safely.ui.components.QuoteSlot
import com.summondemons.safely.ui.components.QuoteSlotState
import com.summondemons.safely.ui.components.RibbonBanner
import com.summondemons.safely.ui.components.RotatingSunburstBackground
import com.summondemons.safely.ui.components.StageBackground
import com.summondemons.safely.ui.components.TitlePlaque
import com.summondemons.safely.ui.components.focusRing
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
import java.time.ZonedDateTime
import kotlin.math.PI
import kotlin.math.sin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Share of the area below the avatar rail taken by the card; raise for more text, lower for bigger art. */
private const val CARD_HEIGHT_FRACTION = 0.36f
private const val BADGE_INFO_MS = 5_000L
private const val AVATAR_BURN_CONSUME_MS = 2_200L
private const val PAGE_COUNT = 10_000
private const val PAGE_MIDDLE = PAGE_COUNT / 2
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
private const val STAGE_ARROW_PRESSED = 0.92f
private val StageArrowWidth = 56.dp
private val StageArrowHeight = 75.dp
private val RailHeight = 72.dp
private val FloatAmplitude = 6.dp
private val SceneBlur = 3.dp
private val BannerDrop = 30.dp
/** The reply floats this long after the burst (ADR-0036, amended). */
private const val REPLY_MS = 10_000L
private const val SQUASH_MS = 220
private const val SQUASH_X = 0.05f
private const val SQUASH_Y = 0.07f
/** Peak flash per tap: faint, stronger, a summon-like burst. */
private val TAP_FLASH = floatArrayOf(0.22f, 0.4f, 0.95f)
private const val TAP_FLASH_MS = 260
private const val BURST_FLASH_MS = 700
/** The third tap shakes first, then bursts. */
private const val BURST_DELAY_MS = 260L
private const val BURST_POP_IN_MS = 120
private const val BURST_FADE_MS = 400
private const val BURST_FROM = 0.3f

/** The Grimoire (ADR-0016): a stage over the demon card, a tarot card whose back is the dossier (ADR-0016 amended, spec §2). */
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
    onTeaserShown: (String) -> Unit = {},
    onTeaserTap: (String, Boolean, Int) -> Unit = { _, _, _ -> },
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
    // Ink plays where it can be seen: a pending reveal inks in when the back shows, then is consumed.
    // Read from the live record (a value saved before a ritual would be stale on return); the
    // latch keeps the ink animation alive for this viewing once the reveal is consumed.
    val pendingCodex = record.revealed && record.revealPending.codex
    val pendingBattle = record.revealed && record.revealPending.battle
    var inkedCodex by rememberSaveable(demon.id) { mutableStateOf(false) }
    var inkedBattle by rememberSaveable(demon.id) { mutableStateOf(false) }
    val inkCodex = inkedCodex || pendingCodex
    val inkBattle = inkedBattle || pendingBattle
    // The card's face (spec §2): kept when switching demon; back turns it over before leaving.
    var side by rememberSaveable { mutableStateOf(CardSide.Front) }
    var scrollToSecret by remember(demon.id) { mutableStateOf(false) }
    LaunchedEffect(demon.id, side, pendingCodex, pendingBattle) {
        if (side != CardSide.Back) return@LaunchedEffect
        if (pendingCodex) {
            inkedCodex = true
            onConsumeReveal(demon.id, Reveal.Codex)
        }
        if (pendingBattle) {
            inkedBattle = true
            onConsumeReveal(demon.id, Reveal.Battle)
        }
    }
    // The Secret is scrolled to once; later flips keep the reader's place.
    LaunchedEffect(side) { if (side == CardSide.Front) scrollToSecret = false }
    // Awesome! (or back) ends the celebration and turns the card to the newly revealed dossier.
    // Keyed on the celebration itself, so selecting another demon is not mistaken for its end.
    val celebrating = state.celebration != null
    var wasCelebrating by remember { mutableStateOf(celebrating) }
    LaunchedEffect(celebrating) {
        if (wasCelebrating && !celebrating) side = CardSide.Back
        wasCelebrating = celebrating
    }
    var badge by rememberSaveable(demon.id) { mutableStateOf<BadgeKey?>(null) }
    var badgeTaps by remember { mutableIntStateOf(0) }
    var shake by remember { mutableIntStateOf(0) }
    var badgeRowBounds by remember { mutableStateOf(Rect.Zero) }
    var rootOrigin by remember { mutableStateOf(Offset.Zero) }
    var tadaFor by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(demon.id, bound, state.today) { if (bound) onVisit(demon.id) }
    LaunchedEffect(demon.id, bound, record.revealPending.avatar) {
        if (bound && record.revealPending.avatar) {
            delay(AVATAR_BURN_CONSUME_MS)
            onConsumeReveal(demon.id, Reveal.Avatar)
        }
    }
    LaunchedEffect(state.openSecret) {
        if (state.openSecret) {
            side = CardSide.Back
            scrollToSecret = true
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
    BackHandler(enabled = party == null && side == CardSide.Back) { side = CardSide.Front }

    val primary: () -> Unit = {
        when {
            party != null -> onDismissCelebration()
            bound -> Unit
            !state.canSummonToday -> shake++
            else -> onSummon(demon.id)
        }
    }
    val index = demons.indexOf(demon)
    val count = demons.size
    // Peers in one set (M3 lateral): each demon is a page that follows the finger and snaps; the
    // pager owns the swipe. A large page count with index % count wraps both ways.
    val pager = rememberPagerState(initialPage = PAGE_MIDDLE - PAGE_MIDDLE % count + index) { PAGE_COUNT }
    val scope = rememberCoroutineScope()
    val currentId by rememberUpdatedState(demon.id)
    val select by rememberUpdatedState(onSelect)
    // A settled page is the selection (saved by the view model).
    LaunchedEffect(pager) {
        snapshotFlow { pager.settledPage }.collect { page ->
            val id = demons[page.mod(count)].id
            if (id != currentId) select(id)
        }
    }
    // A selection made elsewhere (celebration, restored state) moves the pager without animation.
    LaunchedEffect(demon.id) {
        val page = pager.currentPage
        if (demons[page.mod(count)].id != demon.id && !pager.isScrollInProgress) {
            pager.scrollToPage(page - page.mod(count) + index)
        }
    }
    val step: (Int) -> Unit = { delta ->
        scope.launch {
            val target = pager.currentPage + delta
            if (reduceMotion) pager.scrollToPage(target) else pager.animateScrollToPage(target)
        }
    }
    // The card keeps the front's natural height (header + band + badges) for both faces, never below the fraction.
    var fixedPx by remember { mutableIntStateOf(0) }
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val minimumCard = with(density) { fixedPx.toDp() } + spacing.lg + spacing.md + navBottom
    val cardHeightIn: (Dp) -> Dp = { area -> maxOf(area * CARD_HEIGHT_FRACTION, minimumCard) }

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
        HorizontalPager(
            state = pager,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = party == null && count > 1, // no swiping during the celebration
            key = { it },
        ) { page ->
            // Each page draws its own demon from his record; only the selected one takes input.
            val d = demons[page.mod(count)]
            val current = d.id == demon.id
            val r = state.record(d.id)
            val pageAccent = MaterialTheme.demonColors.accent(d.accent)
            val pageParty = current && party != null
            val sulks = if (current) restless else ProgressRules.isRestless(r, state.today)
            val teaser = current && party == null && ProgressRules.teaserEligible(r, state.today)
            if (teaser) LaunchedEffect(d.id, state.today) { onTeaserShown(d.id) }
            Box(Modifier.fillMaxSize()) {
                StageBackground(d.house.stage(), dim = if (r.bound) DIM_BOUND else DIM_SEALED, modifier = Modifier.fillMaxSize())
                if (pageParty) RotatingSunburstBackground(pageAccent.copy(alpha = SUNBURST_ALPHA), Modifier.fillMaxSize(), rotating = !reduceMotion)
                ParticleField(
                    pageAccent.copy(alpha = EMBER_ALPHA), ParticleStyle.Embers, Modifier.fillMaxSize(),
                    count = if (pageParty) PARTY_EMBERS else EMBERS, seed = d.id.hashCode(),
                )
                if (!r.bound) {
                    ParticleField(colors.onSurfaceVariant, ParticleStyle.Smoke, Modifier.fillMaxSize(), count = SMOKE, seed = d.id.hashCode() + 1)
                }
                Column(Modifier.fillMaxSize().statusBarsPadding()) {
                    Spacer(Modifier.height(RailHeight)) // the header is drawn once, above the pages
                    BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                        val cardHeight = cardHeightIn(maxHeight)
                        Column(Modifier.fillMaxSize()) {
                            Stage(
                                demon = d,
                                bound = r.bound,
                                restless = sulks,
                                party = pageParty,
                                teaser = teaser,
                                onTeaserTap = { onDemon, step -> onTeaserTap(d.id, onDemon, step) },
                                canSummon = state.canSummonToday,
                                quote = stringResource(if (sulks) d.restlessQuote else d.quote),
                                hours = state.hoursToNextSummon,
                                shake = if (current) shake else 0,
                                onPrimary = if (current) primary else ({}),
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                            )
                            DemonCard(
                                demons = demons,
                                demon = d,
                                record = r,
                                side = side,
                                onFlip = { if (current) side = if (side == CardSide.Front) CardSide.Back else CardSide.Front },
                                glowFlip = ProgressRules.dossierUnread(r),
                                badge = if (current) badge else null,
                                onBadge = {
                                    if (current) {
                                        badge = it
                                        badgeTaps++
                                    }
                                },
                                inkCodex = current && inkCodex,
                                inkBattle = current && inkBattle,
                                scrollToSecret = current && scrollToSecret,
                                onFixedHeight = { if (current) fixedPx = it },
                                onBadgeRowBounds = { if (current) badgeRowBounds = it },
                                modifier = Modifier.fillMaxWidth().height(cardHeight),
                            )
                        }
                    }
                }
            }
        }

        // Fixed above the pages: the header, the arrows over the stage, the celebration banner.
        Column(Modifier.fillMaxSize().statusBarsPadding().semantics { isTraversalGroup = true; traversalIndex = -1f }) {
            // Like system back (the BackHandlers above): dismiss the celebration, else turn the card to its front, else leave.
            GrimoireHeader(
                {
                    when {
                        party != null -> onDismissCelebration()
                        side == CardSide.Back -> side = CardSide.Front
                        else -> onBack()
                    }
                },
                onMenu,
            )
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                val cardHeight = cardHeightIn(maxHeight)
                Column(Modifier.fillMaxSize()) {
                    Box(Modifier.weight(1f).fillMaxWidth()) {
                        // Arrows make the swipe visible (RC, TODO r62v); hidden while celebrating, like the swipe.
                        if (party == null && count > 1) {
                            StageArrow(next = false, stringResource(R.string.grimoire_previous), Modifier.align(Alignment.CenterStart)) { step(-1) }
                            StageArrow(next = true, stringResource(R.string.grimoire_next), Modifier.align(Alignment.CenterEnd)) { step(1) }
                        }
                    }
                    Spacer(Modifier.height(cardHeight))
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
            Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
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
            .focusRing(interaction, MaterialTheme.shapes.medium)
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
    teaser: Boolean,
    onTeaserTap: (onDemon: Boolean, step: Int) -> Unit,
    quote: String,
    hours: Int,
    shake: Int,
    onPrimary: () -> Unit,
    modifier: Modifier,
) {
    val accent = MaterialTheme.demonColors.accent(demon.accent)
    val spacing = MaterialTheme.spacing
    val reduceMotion = LocalReduceMotion.current
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
    // "Psst" (ADR-0036, amended): three taps, each stronger; the third bursts into the reply.
    val context = LocalContext.current
    val sounds = LocalGameSounds.current
    val scope = rememberCoroutineScope()
    var taps by remember(demon.id) { mutableIntStateOf(0) }
    var bursting by remember(demon.id) { mutableStateOf(false) }
    val squash = remember { Animatable(0f) }
    val flash = remember { Animatable(0f) }
    val tapTeaser: (Boolean) -> Unit = tap@{ onDemon ->
        if (taps >= ProgressRules.TEASER_TAPS || bursting) return@tap
        val step = ++taps
        when (step) {
            1 -> Haptics.tick(context)
            2 -> Haptics.clunk(context)
            else -> Haptics.success(context)
        }
        if (!reduceMotion) {
            scope.launch { squash.snapTo(1f); squash.animateTo(0f, tween(SQUASH_MS)) }
            scope.launch { flash.snapTo(TAP_FLASH[step - 1]); flash.animateTo(0f, tween(if (step < 3) TAP_FLASH_MS else BURST_FLASH_MS)) }
        }
        if (step < ProgressRules.TEASER_TAPS) {
            onTeaserTap(onDemon, step)
        } else {
            scope.launch {
                if (!reduceMotion) delay(BURST_DELAY_MS)
                onTeaserTap(onDemon, step) // persists the open: the bubble leaves until tomorrow
                sounds?.playLaughIfPresent()
                bursting = true
                delay(REPLY_MS)
                bursting = false
                taps = 0
            }
        }
    }
    val floatY: () -> Float = { if (reduceMotion) 0f else sin(time / 1_000f * 2f * PI.toFloat() / FLOAT_PERIOD_S) * floatPx }
    Column(
        modifier
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
                if (bursting) {
                    RotatingSunburstBackground(accent.copy(alpha = SUNBURST_ALPHA), Modifier.matchParentSize().clipToBounds(), rotating = !reduceMotion)
                }
                SharedDemonArt(
                    demonId = demon.id,
                    modifier = Modifier.fillMaxSize().graphicsLayer {
                        translationY = floatY()
                        transformOrigin = TransformOrigin(0.5f, 1f)
                        scaleX = pop.value * (1f + SQUASH_X * squash.value)
                        scaleY = pop.value * (1f - SQUASH_Y * squash.value)
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
                                    enabled = !bound || party || teaser,
                                    // A button only while it acts; a bound portrait is not a disabled control.
                                    role = if (!bound || party || teaser) Role.Button else null,
                                    onClick = { if (teaser) tapTeaser(true) else onPrimary() },
                                )
                                .semantics { contentDescription = label },
                            silhouette = if (bound) 0f else 1f,
                            alignment = Alignment.BottomCenter,
                        )
                    }
                }
                if (teaser && taps < ProgressRules.TEASER_TAPS || bursting) {
                    Box(Modifier.matchParentSize().graphicsLayer { translationY = floatY() }) {
                        if (!bursting) {
                            PsstBubble(
                                text = stringResource(R.string.psst_invite),
                                taps = taps,
                                shake = taps,
                                accent = accent,
                                onTap = { tapTeaser(false) },
                                modifier = Modifier.align(Alignment.TopEnd).padding(top = spacing.sm),
                            )
                        }
                        // Qualified: the Column's scoped overload would shadow it here.
                        androidx.compose.animation.AnimatedVisibility(
                            visible = bursting,
                            enter = if (reduceMotion) fadeIn(tween(BURST_FADE_MS)) else fadeIn(tween(BURST_POP_IN_MS)) + scaleIn(spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium), initialScale = BURST_FROM),
                            exit = fadeOut(tween(BURST_FADE_MS)),
                            modifier = Modifier.align(Alignment.TopCenter),
                        ) {
                            PsstBurst(stringResource(R.string.psst_reply))
                        }
                    }
                }
                if (flash.value > 0f) {
                    val core = MaterialTheme.demonColors.inkHighlight
                    Box(
                        Modifier.matchParentSize().drawBehind {
                            drawRect(Brush.radialGradient(listOf(core, accent, Color.Transparent), radius = size.maxDimension * 0.7f), alpha = flash.value)
                        },
                    )
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
