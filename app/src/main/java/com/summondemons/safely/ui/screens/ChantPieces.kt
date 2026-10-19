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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
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
import com.summondemons.safely.R
import com.summondemons.safely.ritual.RitualUiState
import com.summondemons.safely.ritual.TurnDirection
import com.summondemons.safely.ritual.headingLabel
import com.summondemons.safely.ui.components.ROMAN_NUMERALS
import com.summondemons.safely.ui.fx.InkedSigil
import com.summondemons.safely.ui.fx.LocalReduceMotion
import com.summondemons.safely.ui.fx.SigilFlash
import com.summondemons.safely.ui.theme.demonColors
import com.summondemons.safely.ui.theme.spacing
import com.summondemons.safely.ui.theme.textRoles
import com.summondemons.safely.ui.theme.upper
import kotlin.math.abs

// --- Sizes and tuning (ADR-0019, ADR-0029) ---
private val NameSize = 24.sp
private val SealSize = 26.dp
private val SealNumeralSize = 11.sp
private const val SEAL_STROKE_FRACTION = 0.065f
private val PillTextSize = 15.sp
private val PillBorder = 1.5.dp
private val SlotHeight = 32.dp
private val SlotTextSize = 18.sp
private val MeterWidth = 26.dp
private val MeterHeight = 22.dp
private val MinPhraseSize = 18.sp
private val MaxPhraseSize = 96.sp
private val PhraseStep = 2.sp
private val FrameWidth = 2.dp
private val FlareExtra = 4.dp
private val LockSize = 20.dp
private val RoundButtonSize = 36.dp
private val RoundButtonBorder = 1.5.dp
private val RoundButtonText = 16.sp
private val Dash = 4.dp
private val DashGap = 3.dp
private val DebugPanelWidth = 300.dp
private val DebugPanelBorder = 1.dp
private const val ROUND_BUTTON_ALPHA = 0.8f
private const val TILE_IN_RING = 0.62f
private const val SIGIL_IN_TILE = 0.92f
private const val TILE_ALPHA = 0.85f
private const val TILE_CORNER = 0.18f
private const val GLOW_ALPHA = 0.28f
private const val GHOST_ALPHA = 0.12f
private const val MARKER_FRACTION = 0.045f
private const val MARKER_HALF_WIDTH = 0.6f
private const val RING_STROKE_FRACTION = 0.035f
/** Within this many degrees the proximity arc turns from outline to the accent. */
private const val PROXIMITY_NEAR_DEG = 45f
private const val CLOCK_TWEEN_MS = 120
private const val TILT_TWEEN_MS = 250
private const val UNLOCK_FLARE_MS = 450
private const val FLARE_FILL_ALPHA = 0.18f
private const val GATED_ALPHA = 0.7f
private const val CURRENT_WORD_SCALE = 1.15f
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

/** The demon's name with the four seal marks; [stacked] puts the marks under the name (narrow slab). */
@Composable
internal fun ChantTitle(name: String, seals: List<SealLook>, checking: Boolean, paused: Boolean, accent: Color, stacked: Boolean) {
    val spacing = MaterialTheme.spacing
    val description = stringResource(R.string.chant_a11y_seals, seals.count { it == SealLook.Sealed })
    val nameText: @Composable () -> Unit = {
        Text(name, style = MaterialTheme.textRoles.name.copy(fontSize = NameSize), color = accent, maxLines = 1)
    }
    val marks: @Composable () -> Unit = {
        Row(
            Modifier.semantics(mergeDescendants = true) { contentDescription = description },
            horizontalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
            seals.forEachIndexed { i, look -> SealMark(ROMAN_NUMERALS[i], look, accent, paused, checking, Modifier.size(SealSize)) }
        }
    }
    if (stacked) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
            nameText()
            marks()
        }
    } else {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
            nameText()
            marks()
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
        ).value
    } else {
        1f
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
            alpha = breath
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
        Text(numeral, style = MaterialTheme.textRoles.name.copy(fontSize = SealNumeralSize), color = numeralColor, maxLines = 1)
    }
}

// --- Direction pill (ADR-0029 §3.3) ---

@Composable
internal fun DirectionPillView(pill: DirectionPill, target: Float) {
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
        ).value
    } else {
        1f
    }
    Text(
        label,
        style = MaterialTheme.textRoles.ritualCue.copy(fontSize = PillTextSize, letterSpacing = 0.12.em),
        color = color,
        maxLines = 1,
        modifier = Modifier
            .graphicsLayer { alpha = blink }
            .border(PillBorder, color, CircleShape)
            .padding(horizontal = MaterialTheme.spacing.md, vertical = MaterialTheme.spacing.xs)
            .semantics { contentDescription = description },
    )
}

// --- Sigil (ADR-0020; ADR-0029 §3.4) ---

/**
 * The ring (proximity arc while seeking, lock hold, then focus clock; amber while paused) with the
 * marker pointing at the demon relative to the phone, around a glowing tile that tilts toward him.
 * Only the tile turns: the ring and marker stay in the phone's frame.
 */
@Composable
internal fun ChantSigil(state: RitualUiState, screen: ChantScreenState, accent: Color, flash: SigilFlash?, modifier: Modifier) {
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
    val track = colors.surfaceContainerHighest
    val tile = colors.surfaceContainerLowest
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
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
        Box(
            Modifier
                .fillMaxSize(TILE_IN_RING)
                .graphicsLayer { rotationZ = tilt }
                .drawBehind {
                    drawCircle(Brush.radialGradient(listOf(accent.copy(alpha = GLOW_ALPHA), Color.Transparent)), radius = size.minDimension * 0.75f)
                    drawRoundRect(tile.copy(alpha = TILE_ALPHA), cornerRadius = CornerRadius(size.minDimension * TILE_CORNER))
                },
            contentAlignment = Alignment.Center,
        ) {
            InkedSigil(
                sigil = state.demon.sigil,
                inkProgress = state.ritual.inkProgress,
                accent = accent,
                rimText = state.demon.name.uppercase(),
                modifier = Modifier.fillMaxSize(SIGIL_IN_TILE),
                micLevel = state.micLevel,
                ghostAlpha = GHOST_ALPHA,
                flash = flash,
            )
        }
    }
}

// --- Feedback slot (ADR-0029 §3.5) ---

/** One fixed-height line above the phrase: meter, checking, score, or seal cracked. */
@Composable
internal fun FeedbackSlot(slot: SlotContent, accent: Color, micLevel: Float, paused: Boolean) {
    val colors = MaterialTheme.colorScheme
    val safe = MaterialTheme.demonColors.ritualSafe
    val style = MaterialTheme.textRoles.ritualCue.copy(fontSize = SlotTextSize)
    Row(
        Modifier.fillMaxWidth().height(SlotHeight).semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm, Alignment.CenterHorizontally),
    ) {
        when (slot) {
            SlotContent.Empty -> Unit
            SlotContent.Meter -> VoiceMeter(if (paused) 0f else micLevel, if (paused) colors.outline else accent, Modifier.size(MeterWidth, MeterHeight))
            SlotContent.Checking -> {
                CheckingMeter(accent)
                Text(stringResource(R.string.chant_checking).upper(), style = style, color = colors.onSurfaceVariant, maxLines = 1)
            }
            is SlotContent.Sealed -> Text(slot.score?.let { "$it % ✓" } ?: "✓", style = style, color = safe, maxLines = 1)
            is SlotContent.Missed -> {
                Text("${slot.score} %", style = style, color = colors.onSurface, maxLines = 1)
                if (slot.heard != null) {
                    Text(
                        stringResource(R.string.chant_heard, slot.heard),
                        style = style.copy(fontStyle = FontStyle.Italic),
                        color = colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                }
                Text("↺", style = style, color = colors.onSurfaceVariant)
            }
            SlotContent.Cracked -> {
                Text(stringResource(R.string.verdict_seal_cracked).upper(), style = style, color = colors.error, maxLines = 1)
                Text("✕", style = style, color = colors.error)
            }
        }
    }
}

/** The meter pulsing on its own while the phone checks what was said. */
@Composable
private fun CheckingMeter(color: Color) {
    val level = if (LocalReduceMotion.current) {
        CHECKING_LEVEL
    } else {
        rememberInfiniteTransition(label = "checking").animateFloat(
            METER_FLOOR, 1f, infiniteRepeatable(tween(CHECKING_HALF_MS), RepeatMode.Reverse), label = "checkingLevel",
        ).value
    }
    VoiceMeter(level, color, Modifier.size(MeterWidth, MeterHeight))
}

/** Five ember bars that follow the mic level. Shows that the mic hears you; decides nothing. */
@Composable
fun VoiceMeter(level: Float, color: Color, modifier: Modifier = Modifier) {
    val animated by animateFloatAsState(level.coerceIn(0f, 1f), tween(METER_TWEEN_MS), label = "meter")
    Canvas(modifier) {
        val bar = size.width / (METER_HEIGHTS.size * 2 - 1)
        METER_HEIGHTS.forEachIndexed { i, h ->
            val height = size.height * (METER_FLOOR + animated * h * (1f - METER_FLOOR))
            drawRoundRect(
                color,
                topLeft = Offset(i * 2 * bar, (size.height - height) / 2f),
                size = Size(bar, height),
                cornerRadius = CornerRadius(bar / 2f),
            )
        }
    }
}

// --- Line box (ADR-0029 §3.6) ---

/**
 * The phrase in a frame: closed (dim, lock glyph, outline) before the lock; on the lock the frame
 * turns the accent and flares once; amber while paused. [onPhraseSize] gets the auto-sized sp.
 */
@Composable
internal fun LineBox(
    words: List<ChantWord>,
    gated: Boolean,
    paused: Boolean,
    accent: Color,
    onPhraseSize: (Float) -> Unit,
    modifier: Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val spacing = MaterialTheme.spacing
    val reduceMotion = LocalReduceMotion.current
    val frame by animateColorAsState(
        when {
            gated -> colors.outlineVariant
            paused -> colors.primary
            else -> accent
        },
        label = "frame",
    )
    val flare = remember { Animatable(0f) }
    var wasGated by remember { mutableStateOf(gated) }
    LaunchedEffect(gated) {
        if (wasGated && !gated && !reduceMotion) {
            flare.snapTo(1f)
            flare.animateTo(0f, tween(UNLOCK_FLARE_MS))
        }
        wasGated = gated
    }
    val shape = MaterialTheme.shapes.large
    Column(
        modifier
            .background(accent.copy(alpha = FLARE_FILL_ALPHA * flare.value), shape)
            .border(FrameWidth + FlareExtra * flare.value, frame, shape)
            .padding(horizontal = spacing.md, vertical = spacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (gated) {
            Icon(
                painterResource(R.drawable.ic_lock),
                contentDescription = stringResource(R.string.chant_a11y_locked),
                tint = colors.outline,
                modifier = Modifier.size(LockSize),
            )
        }
        Phrase(words, accent, onPhraseSize, Modifier.fillMaxWidth().weight(1f).graphicsLayer { alpha = if (gated) GATED_ALPHA else 1f })
    }
}

/** The phrase as one auto-sized text; a line under each word is drawn from the layout. */
@Composable
private fun Phrase(words: List<ChantWord>, accent: Color, onPhraseSize: (Float) -> Unit, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    val safe = MaterialTheme.demonColors.ritualSafe
    val dim = colors.onSurface.copy(alpha = DIM_ALPHA)
    val ranges = ArrayList<IntRange>(words.size)
    val text = buildAnnotatedString {
        words.forEachIndexed { wi, word ->
            if (wi > 0) append(' ')
            val start = length
            val sized = if (word.look == WordLook.Current) pushStyle(SpanStyle(fontSize = CURRENT_WORD_SCALE.em)) else null
            word.syllables.forEachIndexed { si, syllable ->
                val color = if (word.look == WordLook.Heard || si < word.lit) accent else dim
                withStyle(SpanStyle(color = color)) { append(syllable) }
            }
            if (sized != null) pop(sized)
            ranges += start until length
        }
    }
    val lineColors = words.map {
        when (it.look) {
            WordLook.Pending -> colors.outlineVariant
            WordLook.Current -> colors.onSurfaceVariant
            WordLook.Heard -> safe
        }
    }
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    BasicText(
        text = text,
        style = MaterialTheme.textRoles.incantationLarge.copy(textAlign = TextAlign.Center),
        autoSize = TextAutoSize.StepBased(minFontSize = MinPhraseSize, maxFontSize = MaxPhraseSize, stepSize = PhraseStep),
        onTextLayout = {
            layout = it
            onPhraseSize(it.layoutInput.style.fontSize.value)
        },
        modifier = modifier.wrapContentHeight(Alignment.CenterVertically).drawBehind {
            layout?.let { drawWordLines(it, ranges, words, lineColors) }
        },
    )
}

private fun DrawScope.drawWordLines(
    layout: TextLayoutResult,
    ranges: List<IntRange>,
    words: List<ChantWord>,
    lineColors: List<Color>,
) {
    ranges.forEachIndexed { i, range ->
        if (range.isEmpty() || range.last >= layout.layoutInput.text.length) return@forEachIndexed
        if (layout.getLineForOffset(range.first) != layout.getLineForOffset(range.last)) return@forEachIndexed
        val first = layout.getBoundingBox(range.first)
        val last = layout.getBoundingBox(range.last)
        val height = last.bottom - last.top
        val thickness = height * LINE_FRACTION
        val y = last.bottom + thickness
        val width = if (words[i].look == WordLook.Pending) thickness * PENDING_LINE_SCALE else thickness
        drawLine(lineColors[i], Offset(first.left, y), Offset(last.right, y), width, StrokeCap.Round)
    }
}
