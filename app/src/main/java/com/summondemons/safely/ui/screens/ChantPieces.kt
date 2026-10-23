package com.summondemons.safely.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import androidx.compose.foundation.text.modifiers.TextAutoSizeLayoutScope
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.util.lerp
import com.summondemons.safely.R
import com.summondemons.safely.ritual.RitualUiState
import com.summondemons.safely.ritual.TurnDirection
import com.summondemons.safely.ui.components.headingLabel
import com.summondemons.safely.ui.components.ROMAN_NUMERALS
import com.summondemons.safely.ui.fx.InkedSigil
import com.summondemons.safely.ui.fx.LocalReduceMotion
import com.summondemons.safely.ui.fx.SigilFlash
import com.summondemons.safely.ui.theme.demonColors
import com.summondemons.safely.ui.theme.fontScaleCapped
import com.summondemons.safely.ui.theme.spacing
import com.summondemons.safely.ui.theme.textRoles
import com.summondemons.safely.ui.theme.upper
import kotlin.math.abs

// --- Sizes and tuning (ADR-0019, ADR-0029) ---
private val NameSize = 22.sp
private val SealSize = 28.dp
private val NameMinSize = 10.sp
private val NameRule = 1.dp
private const val NAME_RULE_ALPHA = 0.35f
private val TrackLink = 20.dp
private val TrackLinkWidth = 2.dp
private val SealNumeralSize = 10.sp
private const val SEAL_STROKE_FRACTION = 0.065f
private val PillTextSize = 15.sp
private val PillBorder = 1.5.dp
private val SlotHeight = 32.dp
private val SlotTextSize = 16.sp
private val ChipBorder = 1.dp
private const val CHIP_ALPHA = 0.85f
private val MeterWidth = 26.dp
private val MeterHeight = 20.dp
private val MicSize = 16.dp
private val MinPhraseSize = 18.sp
private val MaxPhraseSize = 96.sp
private val PhraseStep = 2.sp
private val LockSize = 16.dp
private val ChevronWidth = 14.dp
private val ChevronHeight = 64.dp
private val ChevronStroke = 3.dp
private val RoundButtonSize = 36.dp
private val RoundButtonBorder = 1.5.dp
private val RoundButtonText = 16.sp
private val Dash = 4.dp
private val DashGap = 3.dp
private val DebugPanelWidth = 300.dp
private val DebugPanelBorder = 1.dp
private const val ROUND_BUTTON_ALPHA = 0.8f
private const val SIGIL_IN_RING = 0.8f
/** The glow behind the sigil: at rest, added while the lock fills, added with the voice, and the unlock flare. */
private const val GLOW_BASE = 0.16f
private const val GLOW_LOCK = 0.3f
private const val GLOW_VOICE = 0.2f
private const val GLOW_FLARE = 0.45f
private const val GLOW_RADIUS = 0.62f
private const val GHOST_ALPHA = 0.12f
private const val MARKER_FRACTION = 0.045f
private const val MARKER_HALF_WIDTH = 0.6f
private const val RING_STROKE_FRACTION = 0.035f
/** Within this many degrees the proximity arc turns from outline to the accent. */
private const val PROXIMITY_NEAR_DEG = 45f
private const val CLOCK_TWEEN_MS = 120
private const val TILT_TWEEN_MS = 250
private const val UNLOCK_FLARE_MS = 600
private const val LINE_FLARE_ALPHA = 0.22f
/** The chanting halo behind the phrase at full breath (ADR-0033). */
private const val LINE_HALO_ALPHA = 0.18f
/** Wavering: the phrase dims while the voice is ignored (ADR-0010, ADR-0033). */
private const val DRIFT_LINE_ALPHA = 0.45f
/** The sigil's extra glow at full breath while it needs the player (ADR-0033). */
private const val GLOW_ATTENTION = 0.35f
private val PillMinSize = 8.sp
/** The closed line brightens from here to [GATED_ALPHA_LOCKED] as the lock fills. */
private const val GATED_ALPHA = 0.4f
private const val GATED_ALPHA_LOCKED = 0.85f
/** The current word's underline is this many times the heard/pending thickness (ADR-0033 amended). */
private const val CURRENT_LINE_SCALE = 2f
/** The soft glow behind the current word, at the chanting halo's strength. */
private const val CURRENT_WORD_GLOW_ALPHA = 0.18f
/** The glow reaches this far past the word's centre, in line heights, stretched to the word's width. */
private const val WORD_GLOW_SPREAD = 0.8f
private const val DIM_ALPHA = 0.55f
private const val LINE_FRACTION = 0.06f
private const val PENDING_LINE_SCALE = 0.7f
private const val BLINK_HALF_MS = 300
private const val BLINK_MIN = 0.55f
private const val BREATH_MIN = 0.55f
private const val BREATH_HALF_MS = 900
private const val FLARE_SCALE = 1.18f
private const val FLARE_MS = 450
private const val METER_TWEEN_MS = 120
private const val METER_FLOOR = 0.22f
private const val CHECKING_HALF_MS = 250
private const val CHECKING_LEVEL = 0.6f
private const val CHEVRON_CYCLE_MS = 900
private const val CHEVRON_MIN_ALPHA = 0.25f
private val METER_HEIGHTS = listOf(0.45f, 0.8f, 1f, 0.7f, 0.4f)

// --- Corner buttons (ADR-0021; ADR-0029 §3.1) ---

/** "?" (the intro card) and, in debug builds, "D" (the debug panel); the panel floats under them. */
@Composable
internal fun CornerButtons(onHelp: (() -> Unit)?, debug: (@Composable () -> Unit)?) {
    if (onHelp == null && debug == null) return
    var debugOpen by rememberSaveable { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme
    val spacing = MaterialTheme.spacing
    val gap = with(LocalDensity.current) { spacing.xs.roundToPx() }
    Box {
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
            if (onHelp != null) RoundButton("?", stringResource(R.string.chant_help), onHelp)
            if (debug != null) {
                RoundButton("D", stringResource(R.string.debug_controls), { debugOpen = !debugOpen }, dashed = true, selected = debugOpen)
            }
        }
        if (debug != null && debugOpen) {
            Popup(
                popupPositionProvider = remember(gap) { BelowAnchor(gap) },
                onDismissRequest = { debugOpen = false },
                properties = PopupProperties(focusable = true),
            ) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = colors.surfaceContainerLowest,
                    border = BorderStroke(DebugPanelBorder, colors.outlineVariant),
                    modifier = Modifier.width(DebugPanelWidth),
                ) {
                    Box(Modifier.padding(spacing.sm)) { debug() }
                }
            }
        }
    }
}

/** A small round button: solid outline, or dashed for debug only; filled while [selected]. */
@Composable
private fun RoundButton(label: String, description: String, onClick: () -> Unit, dashed: Boolean = false, selected: Boolean = false) {
    val colors = MaterialTheme.colorScheme
    val fill = if (selected) colors.primary else colors.surfaceContainerHighest.copy(alpha = ROUND_BUTTON_ALPHA)
    val edge = if (selected) colors.primary else colors.outline
    val ink = when {
        selected -> colors.onPrimary
        dashed -> colors.outline
        else -> colors.onSurface
    }
    IconButton(onClick = onClick, modifier = Modifier.semantics { contentDescription = description }) {
        Box(
            Modifier.size(RoundButtonSize).drawBehind {
                val width = RoundButtonBorder.toPx()
                val dashes = if (dashed) PathEffect.dashPathEffect(floatArrayOf(Dash.toPx(), DashGap.toPx())) else null
                drawCircle(fill)
                drawCircle(edge, radius = (size.minDimension - width) / 2f, style = Stroke(width, pathEffect = dashes))
            },
            contentAlignment = Alignment.Center,
        ) {
            Text(label, style = MaterialTheme.textRoles.ritualCue.copy(fontSize = RoundButtonText), color = ink)
        }
    }
}

/** Puts the debug panel under the buttons, left edges aligned, kept inside the window. */
private class BelowAnchor(private val gap: Int) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset = IntOffset(
        anchorBounds.left.coerceIn(0, maxOf(0, windowSize.width - popupContentSize.width)),
        (anchorBounds.bottom + gap).coerceIn(0, maxOf(0, windowSize.height - popupContentSize.height)),
    )
}

// --- Title: name and seals (ADR-0029 §3.2) ---

/** The demon's name between two thin accent rules, sized to the stack's width (spec 2026-10-22 polish §2). */
@Composable
internal fun ChantNamePlate(name: String, accent: Color) {
    val spacing = MaterialTheme.spacing
    val maxSize = NameSize.fontScaleCapped()
    Box(
        Modifier
            .fillMaxWidth()
            .drawBehind {
                val rule = NameRule.toPx()
                val color = accent.copy(alpha = NAME_RULE_ALPHA)
                drawLine(color, Offset(0f, rule / 2f), Offset(size.width, rule / 2f), rule)
                drawLine(color, Offset(0f, size.height - rule / 2f), Offset(size.width, size.height - rule / 2f), rule)
            }
            .padding(vertical = spacing.xs),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            name.uppercase(),
            style = MaterialTheme.textRoles.name.copy(fontSize = maxSize, letterSpacing = 0.12.em, textAlign = TextAlign.Center),
            color = accent,
            maxLines = 1,
            softWrap = false,
            autoSize = TextAutoSize.StepBased(minFontSize = NameMinSize, maxFontSize = maxSize),
        )
    }
}

/**
 * The four lines as steps I–IV joined by a line that fills as they seal (spec 2026-10-22 polish §3);
 * each step is a [SealMark] (current breathes, sealed flares, cracked red, paused amber).
 */
@Composable
internal fun SealTrack(seals: List<SealLook>, accent: Color, paused: Boolean, checking: Boolean) {
    val sealed = seals.count { it == SealLook.Sealed }
    val description = pluralStringResource(R.plurals.chant_a11y_seals, sealed, sealed)
    val pending = MaterialTheme.colorScheme.outlineVariant
    Column(
        Modifier.semantics(mergeDescendants = true) { contentDescription = description },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        seals.forEachIndexed { i, look ->
            if (i > 0) {
                val filled = seals[i - 1] == SealLook.Sealed
                Box(Modifier.size(TrackLinkWidth, TrackLink).background(if (filled) accent else pending))
            }
            SealMark(ROMAN_NUMERALS[i], look, accent, paused, checking, Modifier.size(SealSize))
        }
    }
}


/** A numeral in a ring: filled when sealed, error when cracked, amber when current and paused; the current one breathes. */
@Composable
private fun SealMark(numeral: String, look: SealLook, accent: Color, paused: Boolean, checking: Boolean, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    val reduceMotion = LocalReduceMotion.current
    val current = look == SealLook.Current
    val breath = if (current && !paused && !reduceMotion) {
        rememberInfiniteTransition(label = "seal").animateFloat(
            1f, BREATH_MIN, infiniteRepeatable(tween(if (checking) BREATH_HALF_MS / 3 else BREATH_HALF_MS), RepeatMode.Reverse), label = "sealBreath",
        )
    } else {
        null
    }
    val pulse = remember { Animatable(1f) }
    LaunchedEffect(look) {
        if (look == SealLook.Sealed && !reduceMotion) {
            pulse.snapTo(FLARE_SCALE)
            pulse.animateTo(1f, tween(FLARE_MS))
        }
    }
    val ring = when {
        look == SealLook.Cracked -> colors.error
        current && paused -> colors.primary
        look == SealLook.Pending -> colors.outlineVariant
        else -> accent
    }
    val numeralColor = when {
        look == SealLook.Sealed -> colors.surfaceContainerLowest
        look == SealLook.Cracked -> colors.error
        current && paused -> colors.primary
        look == SealLook.Pending -> colors.outline
        else -> accent
    }
    Box(
        modifier.graphicsLayer {
            alpha = breath?.value ?: 1f
            scaleX = pulse.value
            scaleY = pulse.value
        },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.matchParentSize()) {
            val stroke = size.minDimension * SEAL_STROKE_FRACTION
            val radius = size.minDimension / 2f - stroke
            if (look == SealLook.Sealed) drawCircle(accent, radius = radius)
            drawCircle(ring, radius = radius, style = Stroke(stroke))
        }
        Text(numeral, style = MaterialTheme.textRoles.name.copy(fontSize = SealNumeralSize.fontScaleCapped()), color = numeralColor, maxLines = 1)
    }
}

// --- Direction pill (ADR-0029 §3.3) ---

@Composable
internal fun DirectionPillView(pill: DirectionPill, target: Float, modifier: Modifier = Modifier) {
    if (pill.look == PillLook.Hidden) return
    val colors = MaterialTheme.colorScheme
    val reduceMotion = LocalReduceMotion.current
    val direction = headingLabel(target)
    val color = when (pill.look) {
        PillLook.Facing -> MaterialTheme.demonColors.ritualSafe
        PillLook.Paused -> colors.primary
        PillLook.Seeking, PillLook.Hidden -> colors.outline
    }
    val arrow = when (pill.turn) {
        TurnDirection.Right -> "→"
        TurnDirection.Left -> "←"
        null -> ""
    }
    val label = when (pill.look) {
        PillLook.Paused -> "${stringResource(R.string.chant_paused).upper()} $arrow"
        PillLook.Facing -> "${stringResource(R.string.chant_face, direction).upper()} ✓"
        PillLook.Seeking, PillLook.Hidden -> "${stringResource(R.string.chant_face, direction).upper()} $arrow"
    }.trim()
    val description = when {
        pill.look == PillLook.Facing -> stringResource(R.string.chant_a11y_facing, direction)
        pill.look == PillLook.Paused -> stringResource(R.string.chant_a11y_paused, direction)
        pill.turn == TurnDirection.Right -> stringResource(R.string.chant_a11y_turn_right, direction)
        else -> stringResource(R.string.chant_a11y_turn_left, direction)
    }
    val blink = if (pill.look == PillLook.Paused && !reduceMotion) {
        rememberInfiniteTransition(label = "pill").animateFloat(
            1f, BLINK_MIN, infiniteRepeatable(tween(BLINK_HALF_MS), RepeatMode.Reverse), label = "pillBlink",
        )
    } else {
        null
    }
    Text(
        label,
        style = MaterialTheme.textRoles.ritualCue.copy(fontSize = PillTextSize.fontScaleCapped(), letterSpacing = 0.12.em, textAlign = TextAlign.Center),
        color = color,
        maxLines = 1,
        softWrap = false,
        autoSize = TextAutoSize.StepBased(minFontSize = PillMinSize, maxFontSize = PillTextSize.fontScaleCapped()),
        modifier = modifier
            .graphicsLayer { alpha = blink?.value ?: 1f }
            .background(colors.surfaceContainerLowest.copy(alpha = CHIP_ALPHA), CircleShape)
            .border(PillBorder, color, CircleShape)
            .padding(horizontal = MaterialTheme.spacing.md, vertical = MaterialTheme.spacing.xs)
            .semantics { contentDescription = description },
    )
}

// --- Edge chevrons (ADR-0029) ---

/**
 * Three chevrons on the screen edge the player should turn toward, lit in a wave: wordless and
 * seen from the corner of the eye while reading. Amber while paused. Still with reduced motion.
 */
@Composable
internal fun EdgeChevrons(turn: TurnDirection, paused: Boolean, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val color = if (paused) colors.primary else colors.onSurfaceVariant
    val phase = if (LocalReduceMotion.current) {
        null
    } else {
        rememberInfiniteTransition(label = "chevrons").animateFloat(
            0f, 1f, infiniteRepeatable(tween(CHEVRON_CYCLE_MS, easing = LinearEasing)), label = "chevronPhase",
        )
    }
    val right = turn == TurnDirection.Right
    Canvas(modifier.size(ChevronWidth, ChevronHeight)) {
        val stroke = ChevronStroke.toPx()
        val p = phase?.value ?: -1f
        val count = 3
        val each = size.height / count
        val depth = size.width - stroke
        repeat(count) { i ->
            // The wave runs in the turning direction: top to bottom for right, bottom to top for left.
            val k = if (right) i else count - 1 - i
            val alpha = if (p < 0f) 1f else {
                val d = ((p * count - k) % count + count) % count
                lerp(1f, CHEVRON_MIN_ALPHA, (d / count * 2f).coerceAtMost(1f))
            }
            val top = i * each + stroke
            val mid = top + (each - 2 * stroke) / 2f
            val bottom = top + each - 2 * stroke
            val tipX = if (right) stroke / 2 + depth else stroke / 2
            val baseX = if (right) stroke / 2 else stroke / 2 + depth
            drawPath(
                Path().apply {
                    moveTo(baseX, top)
                    lineTo(tipX, mid)
                    lineTo(baseX, bottom)
                },
                color.copy(alpha = alpha),
                style = Stroke(stroke, cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round),
            )
        }
    }
}

// --- Sigil (ADR-0020; ADR-0029 §3.4) ---

/**
 * The ring (proximity arc while seeking, lock hold, then focus clock; amber while paused) with the
 * marker pointing at the demon relative to the phone, around the sigil, which tilts toward him.
 * Its glow grows as the lock fills, flares once on the lock, then follows the voice.
 */
@Composable
internal fun ChantSigil(state: RitualUiState, screen: ChantScreenState, accent: Color, flash: SigilFlash?, attention: State<Float>, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    val safe = MaterialTheme.demonColors.ritualSafe
    val reduceMotion = LocalReduceMotion.current
    val near = screen.markerDegrees?.let { abs(it) <= PROXIMITY_NEAR_DEG } == true
    val ringTarget = when {
        screen.drifting -> colors.primary
        screen.facing -> safe
        near -> accent
        else -> colors.outline
    }
    val ring by animateColorAsState(ringTarget, label = "ring")
    val marker by animateColorAsState(if (screen.facing) safe else accent, label = "marker")
    val sweep by animateFloatAsState(screen.clockFraction, tween(CLOCK_TWEEN_MS, easing = LinearEasing), label = "clock")
    val tilt by animateFloatAsState(screen.tiltDegrees, if (reduceMotion) snap() else tween(TILT_TWEEN_MS), label = "tilt")
    val flare = unlockFlare(screen.gated)
    val voice = if (screen.gated || screen.drifting) 0f else state.micLevel
    val baseGlow = GLOW_BASE + GLOW_LOCK * screen.lockFraction + GLOW_VOICE * voice + GLOW_FLARE * flare
    val glowColor = if (screen.drifting) colors.primary else accent
    val track = colors.surfaceContainerHighest
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            // The breathing term is read here, while drawing, so it only redraws the canvas.
            val glow = (baseGlow + GLOW_ATTENTION * attention.value).coerceAtMost(1f)
            drawCircle(
                Brush.radialGradient(listOf(glowColor.copy(alpha = glow), Color.Transparent), radius = size.minDimension * GLOW_RADIUS),
                radius = size.minDimension * GLOW_RADIUS,
            )
            val stroke = size.minDimension * RING_STROKE_FRACTION
            val side = size.minDimension - stroke
            val topLeft = Offset((size.width - side) / 2f, (size.height - side) / 2f)
            val arcSize = Size(side, side)
            drawArc(track, 0f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
            val cap = Stroke(stroke, cap = StrokeCap.Round)
            when (screen.ring) {
                // From where the phone points (top) to the demon: shorter as the player gets closer.
                RingMode.Seeking -> screen.markerDegrees?.takeIf { it != 0f }?.let { drawArc(ring, -90f, it, false, topLeft, arcSize, style = cap) }
                RingMode.Locking -> if (screen.lockFraction > 0f) drawArc(ring, -90f, 360f * screen.lockFraction, false, topLeft, arcSize, style = cap)
                RingMode.Clock -> if (sweep > 0f) drawArc(ring, -90f, 360f * sweep, false, topLeft, arcSize, style = cap)
            }
            screen.markerDegrees?.let { degrees ->
                val length = size.minDimension * MARKER_FRACTION
                val tip = center.y - side / 2f + stroke
                rotate(degrees, pivot = center) {
                    drawPath(
                        Path().apply {
                            moveTo(center.x, tip)
                            lineTo(center.x + length * MARKER_HALF_WIDTH, tip + length)
                            lineTo(center.x - length * MARKER_HALF_WIDTH, tip + length)
                            close()
                        },
                        marker,
                    )
                }
            }
        }
        InkedSigil(
            sigil = state.demon.sigil,
            inkProgress = state.ritual.inkProgress,
            accent = accent,
            rimText = state.demon.name.uppercase(),
            modifier = Modifier.fillMaxSize(SIGIL_IN_RING).graphicsLayer { rotationZ = tilt },
            micLevel = state.micLevel,
            ghostAlpha = GHOST_ALPHA,
            flash = flash,
        )
    }
}

/** 1 → 0 over [UNLOCK_FLARE_MS] when [gated] turns false (the lock); 0 otherwise and with reduced motion. */
@Composable
private fun unlockFlare(gated: Boolean): Float {
    val reduceMotion = LocalReduceMotion.current
    val flare = remember { Animatable(0f) }
    var wasGated by remember { mutableStateOf(gated) }
    LaunchedEffect(gated) {
        if (wasGated && !gated && !reduceMotion) {
            flare.snapTo(1f)
            flare.animateTo(0f, tween(UNLOCK_FLARE_MS))
        }
        wasGated = gated
    }
    return flare.value
}

// --- Feedback slot (ADR-0029 §3.5) ---

/** One fixed-height line above the phrase: the bare meter, or a chip (locked, checking, score, seal cracked). */
@Composable
internal fun FeedbackSlot(slot: SlotContent, accent: Color, micLevel: Float, paused: Boolean) {
    val colors = MaterialTheme.colorScheme
    val safe = MaterialTheme.demonColors.ritualSafe
    val style = MaterialTheme.textRoles.ritualCue.copy(fontSize = SlotTextSize.fontScaleCapped(), letterSpacing = 0.1.em)
    Box(
        Modifier.fillMaxWidth().height(SlotHeight).semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        contentAlignment = Alignment.Center,
    ) {
        when (slot) {
            SlotContent.Empty -> Unit
            SlotContent.Meter -> Chip(colors.outlineVariant) {
                // Listening (or paused) as a labelled state, never bare bars that read as "•••••".
                Icon(painterResource(R.drawable.ic_mic), contentDescription = null, tint = if (paused) colors.outline else accent, modifier = Modifier.size(MicSize))
                VoiceMeter(if (paused) 0f else micLevel, if (paused) colors.outline else accent, Modifier.size(MeterWidth, MeterHeight))
                Text(
                    stringResource(if (paused) R.string.chant_paused else R.string.chant_listening).upper(),
                    style = style,
                    color = if (paused) colors.outline else colors.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false,
                    // Visual only: the slot is a live region, and announcing "Listening" after every
                    // line (or "Paused" next to the pill's own description) would only add noise.
                    modifier = Modifier.clearAndSetSemantics { },
                )
            }
            SlotContent.Locked -> Chip(colors.outlineVariant) {
                Icon(painterResource(R.drawable.ic_lock), contentDescription = stringResource(R.string.chant_a11y_locked), tint = colors.outline, modifier = Modifier.size(LockSize))
                Text(stringResource(R.string.chant_locked).upper(), style = style, color = colors.outline, maxLines = 1)
            }
            SlotContent.Checking -> Chip(accent) {
                CheckingMeter(accent)
                Text(stringResource(R.string.chant_checking).upper(), style = style, color = colors.onSurface, maxLines = 1)
            }
            is SlotContent.Sealed -> Chip(safe) {
                if (slot.score != null) {
                    Text(stringResource(R.string.chant_score, slot.score), style = style, color = safe, maxLines = 1)
                    Text("·", style = style, color = colors.outline)
                }
                Text("${stringResource(R.string.chant_sealed).upper()} ✓", style = style, color = safe, maxLines = 1)
            }
            is SlotContent.Missed -> Chip(colors.outline) {
                Text(stringResource(R.string.chant_score, slot.score), style = style, color = colors.onSurface, maxLines = 1)
                if (slot.heard != null) {
                    Text("·", style = style, color = colors.outline)
                    Text(
                        stringResource(R.string.chant_heard, slot.heard),
                        style = style.copy(fontStyle = FontStyle.Italic, letterSpacing = 0.02.em),
                        color = colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                }
                Text("↺", style = style, color = colors.onSurfaceVariant)
            }
            SlotContent.Cracked -> Chip(colors.error) {
                Text(stringResource(R.string.verdict_seal_cracked).upper(), style = style, color = colors.error, maxLines = 1)
                Text("✕", style = style, color = colors.error)
            }
        }
    }
}

/** A rounded dark chip with a thin [edge], holding one row. */
@Composable
private fun Chip(edge: Color, content: @Composable RowScope.() -> Unit) {
    val spacing = MaterialTheme.spacing
    Row(
        Modifier
            .background(MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = CHIP_ALPHA), CircleShape)
            .border(ChipBorder, edge, CircleShape)
            .padding(horizontal = spacing.md, vertical = spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        content = content,
    )
}

/** The meter pulsing on its own while the phone checks what was said. */
@Composable
private fun CheckingMeter(color: Color) {
    val level = if (LocalReduceMotion.current) {
        null
    } else {
        rememberInfiniteTransition(label = "checking").animateFloat(
            METER_FLOOR, 1f, infiniteRepeatable(tween(CHECKING_HALF_MS), RepeatMode.Reverse), label = "checkingLevel",
        )
    }
    MeterBars({ level?.value ?: CHECKING_LEVEL }, color, Modifier.size(MeterWidth, MeterHeight))
}

/** Five ember bars that follow the mic level. Shows that the mic hears you; decides nothing. */
@Composable
fun VoiceMeter(level: Float, color: Color, modifier: Modifier = Modifier) {
    val animated = animateFloatAsState(level.coerceIn(0f, 1f), tween(METER_TWEEN_MS), label = "meter")
    MeterBars({ animated.value }, color, modifier)
}

/** The bars; [level] is read while drawing, so a moving level redraws without recomposing. */
@Composable
private fun MeterBars(level: () -> Float, color: Color, modifier: Modifier) {
    Canvas(modifier) {
        val l = level()
        val bar = size.width / (METER_HEIGHTS.size * 2 - 1)
        METER_HEIGHTS.forEachIndexed { i, h ->
            val height = size.height * (METER_FLOOR + l * h * (1f - METER_FLOOR))
            drawRoundRect(
                color,
                topLeft = Offset(i * 2 * bar, (size.height - height) / 2f),
                size = Size(bar, height),
                cornerRadius = CornerRadius(bar / 2f),
            )
        }
    }
}

// --- The line (ADR-0029 §3.6) ---

/**
 * The phrase floating over the stage, no frame: dim before the lock, brightening as the lock
 * fills, a soft accent flare as it opens, a breathing halo while chanting and dimmed while wavering (ADR-0033).
 */
@Composable
internal fun ChantLine(
    words: List<ChantWord>,
    gated: Boolean,
    drifting: Boolean,
    lockFraction: Float,
    accent: Color,
    halo: State<Float>,
    /** Chanting: the current word is marked (ADR-0033); otherwise the sigil holds the attention. */
    emphasize: Boolean,
    onPhraseSize: (Float) -> Unit,
    modifier: Modifier,
) {
    val flare = unlockFlare(gated)
    val alpha = when {
        gated -> lerp(GATED_ALPHA, GATED_ALPHA_LOCKED, lockFraction)
        drifting -> DRIFT_LINE_ALPHA
        else -> 1f
    }
    Box(
        modifier.drawBehind {
            val strength = LINE_FLARE_ALPHA * flare + LINE_HALO_ALPHA * halo.value
            if (strength > 0f) {
                val radius = size.maxDimension * 0.5f
                drawCircle(Brush.radialGradient(listOf(accent.copy(alpha = strength), Color.Transparent), radius = radius), radius = radius)
            }
        },
        contentAlignment = Alignment.Center,
    ) {
        Phrase(words, accent, emphasize, onPhraseSize, Modifier.fillMaxWidth().graphicsLayer { this.alpha = alpha })
    }
}

/**
 * The phrase as one auto-sized text that never splits a word; a line under each word is drawn
 * from the layout. The current word is marked (accent underline, 2×, soft glow) only when
 * [emphasize]: while chanting, the phrase is the one thing that glows (ADR-0033).
 */
@Composable
private fun Phrase(words: List<ChantWord>, accent: Color, emphasize: Boolean, onPhraseSize: (Float) -> Unit, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    val safe = MaterialTheme.demonColors.ritualSafe
    val dim = colors.onSurface.copy(alpha = DIM_ALPHA)
    val ranges = ArrayList<IntRange>(words.size)
    val text = buildAnnotatedString {
        words.forEachIndexed { wi, word ->
            if (wi > 0) append(' ')
            val start = length
            // Same size for every word: only colours change as the chant moves, so the
            // auto-sized phrase never refits or re-wraps mid-line (spec 2026-10-22 polish §1).
            word.syllables.forEachIndexed { si, syllable ->
                val color = if (word.look == WordLook.Heard || si < word.lit) accent else dim
                withStyle(SpanStyle(color = color)) { append(syllable) }
            }
            ranges += start until length
        }
    }
    val lineColors = words.map {
        when (it.look) {
            WordLook.Pending -> colors.outlineVariant
            WordLook.Current -> if (emphasize) accent else colors.onSurfaceVariant
            WordLook.Heard -> safe
        }
    }
    val currentGlow = if (emphasize) accent.copy(alpha = CURRENT_WORD_GLOW_ALPHA) else null
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    BasicText(
        text = text,
        style = MaterialTheme.textRoles.incantationLarge.copy(textAlign = TextAlign.Center, lineBreak = LineBreak.Heading),
        autoSize = WholeWordAutoSize(MinPhraseSize, MaxPhraseSize, PhraseStep),
        onTextLayout = {
            layout = it
            onPhraseSize(it.layoutInput.style.fontSize.value)
        },
        modifier = modifier.wrapContentHeight(Alignment.CenterVertically).drawBehind {
            layout?.let { drawWordLines(it, ranges, words, lineColors, currentGlow) }
        },
    )
}

/**
 * The largest size from [min] to [max] (in [step]s) at which the text fits its box **and** no word
 * is broken across lines. The stock step-based auto-size checks only the box, which let a long
 * word wrap mid-word ("DAEMONIU / M"; ADR-0029).
 */
private data class WholeWordAutoSize(val min: TextUnit, val max: TextUnit, val step: TextUnit) : TextAutoSize {
    override fun TextAutoSizeLayoutScope.getFontSize(constraints: Constraints, text: AnnotatedString): TextUnit {
        val stepPx = step.toPx()
        fun fits(px: Float): Boolean {
            val result = performLayout(constraints, text, px.toSp())
            return !result.didOverflowWidth && !result.didOverflowHeight && !splitsAWord(text.text) { result.getLineForOffset(it) }
        }
        var lo = min.toPx()
        var hi = max.toPx()
        if (fits(hi)) return hi.toSp()
        while (hi - lo >= stepPx) {
            val mid = (lo + hi) / 2f
            if (fits(mid)) lo = mid else hi = mid
        }
        return lo.toSp()
    }
}

private fun DrawScope.drawWordLines(
    layout: TextLayoutResult,
    ranges: List<IntRange>,
    words: List<ChantWord>,
    lineColors: List<Color>,
    /** Null: the current word is not emphasised (not chanting). */
    currentGlow: Color?,
) {
    ranges.forEachIndexed { i, range ->
        if (range.isEmpty() || range.last >= layout.layoutInput.text.length) return@forEachIndexed
        if (layout.getLineForOffset(range.first) != layout.getLineForOffset(range.last)) return@forEachIndexed
        val first = layout.getBoundingBox(range.first)
        val last = layout.getBoundingBox(range.last)
        val height = last.bottom - last.top
        val thickness = height * LINE_FRACTION
        val look = words[i].look
        val emphasised = look == WordLook.Current && currentGlow != null
        if (emphasised && currentGlow != null) {
            // A soft oval glow behind the word being chanted, sized to the word so it stays on its
            // line (drawn first, so the text sits on it).
            val width = last.right - first.left
            val center = Offset((first.left + last.right) / 2f, (first.top + last.bottom) / 2f)
            val radius = height * WORD_GLOW_SPREAD
            scale(scaleX = (width / height).coerceAtLeast(1f), scaleY = 1f, pivot = center) {
                drawCircle(Brush.radialGradient(listOf(currentGlow, Color.Transparent), center, radius), radius, center)
            }
        }
        val y = last.bottom + thickness
        val width = when {
            look == WordLook.Pending -> thickness * PENDING_LINE_SCALE
            emphasised -> thickness * CURRENT_LINE_SCALE
            else -> thickness
        }
        drawLine(lineColors[i], Offset(first.left, y), Offset(last.right, y), width, StrokeCap.Round)
    }
}

