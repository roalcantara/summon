package com.summondemons.safely.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextAlign
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
import com.summondemons.safely.ui.components.StageBackground
import com.summondemons.safely.ui.fx.InkedSigil
import com.summondemons.safely.ui.fx.LocalReduceMotion
import com.summondemons.safely.ui.fx.SigilFlash
import com.summondemons.safely.ui.theme.Stage
import com.summondemons.safely.ui.theme.demonColors
import com.summondemons.safely.ui.theme.spacing
import com.summondemons.safely.ui.theme.stage
import com.summondemons.safely.ui.theme.textRoles
import com.summondemons.safely.ui.theme.upper

private const val SLAB_WEIGHT = 0.25f
private const val SLAB_ALPHA = 0.7f
private const val SIGIL_IN_RING = 0.84f
/** The sigil before it inks, as the portrait altar showed it (ADR-0020). */
private const val GHOST_ALPHA = 0.12f
/** Direction marker: its length as a fraction of the sigil's side, and its half-width as a fraction of its length. */
private const val MARKER_FRACTION = 0.045f
private const val MARKER_HALF_WIDTH = 0.6f
private const val RING_STROKE_FRACTION = 0.035f
private const val CLOCK_TWEEN_MS = 120
private const val CURRENT_WORD_SCALE = 1.15f
private const val DIM_ALPHA = 0.55f
private const val LINE_FRACTION = 0.06f
private const val PENDING_LINE_SCALE = 0.7f
private const val BLINK_HALF_MS = 300
private const val BLINK_MIN = 0.55f
private const val BACKDROP_DIM = 0.55f
private const val WORDS_SCRIM_ALPHA = 0.85f
private const val METER_TWEEN_MS = 120
private const val METER_FLOOR = 0.22f
private const val ROUND_BUTTON_ALPHA = 0.8f
private val METER_HEIGHTS = listOf(0.45f, 0.8f, 1f, 0.7f, 0.4f)
private val NameSize = 24.sp
private val VerdictSize = 22.sp
private val BandSigil = 96.dp
private val MeterWidth = 26.dp
private val MeterHeight = 22.dp
private val MinPhraseSize = 18.sp
private val MaxPhraseSize = 96.sp
private val PhraseStep = 2.sp
private val RoundButtonSize = 36.dp
private val RoundButtonBorder = 1.5.dp
private val RoundButtonText = 16.sp
private val Dash = 4.dp
private val DashGap = 3.dp
private val DebugPanelWidth = 300.dp
private val DebugPanelBorder = 1.dp

/**
 * The chant screen (ADR-0019, ADR-0020): for the whole Ritual, the demon's name, sigil
 * (progress, direction, focus clock) and direction line on a slab at the left; the phrase, as large as fits, and
 * one verdict line at the right. Portrait screens get the slab as a top band. The House
 * stage fills the screen edge to edge; the content keeps clear of the system bars.
 */
@Composable
fun ChantScreen(
    state: RitualUiState,
    flash: SigilFlash?,
    showClose: Boolean,
    onClose: () -> Unit,
    /** Debug controls, in a floating panel the "D" button opens (debug builds only; ADR-0021). */
    debug: (@Composable () -> Unit)?,
    modifier: Modifier = Modifier,
    /** Reopens the intro card ("?" above the demon's name); null hides the button. */
    onHelp: (() -> Unit)? = null,
    /** Shown instead of the phrase when the ritual can't hear or can't sense direction (ADR-0020). */
    blocker: (@Composable () -> Unit)? = null,
) {
    val demon = state.demon
    val accent = MaterialTheme.demonColors.accent(demon.accent)
    val screen = remember(state.ritual, state.azimuth, state.checking) {
        chantScreenState(state.ritual, demon.chantPhrases, state.azimuth, demon.ritualHeading, checking = state.checking)
    }

    BoxWithConstraints(modifier) {
        val landscape = maxWidth > maxHeight
        val spacing = MaterialTheme.spacing
        ChantBackdrop(demon.house.stage(), landscape, Modifier.fillMaxSize())
        if (landscape) {
            Row(Modifier.fillMaxSize().safeDrawingPadding().padding(spacing.lg), horizontalArrangement = Arrangement.spacedBy(spacing.lg)) {
                Slab(state, screen, accent, flash, onHelp, debug, vertical = true, Modifier.fillMaxHeight().weight(SLAB_WEIGHT))
                Words(state, screen, accent, blocker, Modifier.fillMaxHeight().weight(1f - SLAB_WEIGHT))
            }
        } else {
            Column(Modifier.fillMaxSize().safeDrawingPadding().padding(spacing.lg), verticalArrangement = Arrangement.spacedBy(spacing.lg)) {
                Slab(state, screen, accent, flash, onHelp, debug, vertical = false, Modifier.fillMaxWidth())
                Words(state, screen, accent, blocker, Modifier.fillMaxWidth().weight(1f))
            }
        }
        if (showClose) {
            IconButton(onClick = onClose, modifier = Modifier.align(Alignment.TopEnd).safeDrawingPadding()) {
                Icon(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = stringResource(R.string.action_abort),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** The House stage, darkened most behind the words so the phrase keeps its contrast (also behind the intro card). */
@Composable
internal fun ChantBackdrop(stage: Stage, landscape: Boolean, modifier: Modifier) {
    val scrim = MaterialTheme.colorScheme.surfaceContainerLowest
    Box(modifier) {
        StageBackground(stage, dim = BACKDROP_DIM, modifier = Modifier.fillMaxSize())
        Canvas(Modifier.fillMaxSize()) {
            val stops = arrayOf(
                0f to scrim.copy(alpha = BACKDROP_DIM),
                0.3f to scrim.copy(alpha = WORDS_SCRIM_ALPHA),
                1f to scrim.copy(alpha = WORDS_SCRIM_ALPHA),
            )
            drawRect(if (landscape) Brush.horizontalGradient(*stops) else Brush.verticalGradient(*stops))
        }
    }
}

@Composable
private fun Slab(
    state: RitualUiState,
    screen: ChantScreenState,
    accent: Color,
    flash: SigilFlash?,
    onHelp: (() -> Unit)?,
    debug: (@Composable () -> Unit)?,
    vertical: Boolean,
    modifier: Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val spacing = MaterialTheme.spacing
    val safe = MaterialTheme.demonColors.ritualSafe
    // Red only once drifting after the lock; before it, finding the direction is not an error (ADR-0020).
    val ringColor = when {
        screen.facing -> safe
        screen.drifting -> colors.error
        else -> colors.outline
    }
    Surface(modifier, shape = MaterialTheme.shapes.large, color = colors.surfaceContainerLow.copy(alpha = SLAB_ALPHA)) {
        if (vertical) {
            Column(
                Modifier.padding(spacing.md),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(spacing.sm, Alignment.CenterVertically),
            ) {
                SlabButtons(onHelp, debug)
                Text(state.demon.name, style = MaterialTheme.textRoles.name.copy(fontSize = NameSize), color = accent, maxLines = 1)
                ChantSigil(
                    state, screen, accent, ringColor, flash,
                    Modifier.fillMaxWidth().weight(1f, fill = false).aspectRatio(1f, matchHeightConstraintsFirst = true),
                )
                RitualStatusBlock(screen.status, state.demon.ritualHeading, accent, Modifier.padding(top = spacing.sm))
            }
        } else {
            Row(
                Modifier.padding(spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.md),
            ) {
                SlabButtons(onHelp, debug)
                ChantSigil(state, screen, accent, ringColor, flash, Modifier.size(BandSigil))
                RitualStatusBlock(screen.status, state.demon.ritualHeading, accent, Modifier.weight(1f))
            }
        }
    }
}

/**
 * "?" (the intro card) and, in debug builds, "D" (the debug panel), centred above the
 * demon's name (ADR-0021). The panel floats under them, so opening it moves nothing; a tap
 * outside or back closes it.
 */
@Composable
private fun SlabButtons(onHelp: (() -> Unit)?, debug: (@Composable () -> Unit)?) {
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

/** A small round button on the slab: solid outline, or dashed for debug only; filled while [selected]. */
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

/**
 * The sigil inside a ring that guides to the direction and keeps time (ADR-0019, ADR-0020):
 * a marker points at the demon relative to the phone; the ring fills with the lock hold,
 * then drains as the focus clock; its colour says facing / seeking / drifting.
 */
@Composable
private fun ChantSigil(
    state: RitualUiState,
    screen: ChantScreenState,
    accent: Color,
    ringColor: Color,
    flash: SigilFlash?,
    modifier: Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val track = colors.surfaceContainerHighest
    val sweep by animateFloatAsState(screen.clockFraction, tween(CLOCK_TWEEN_MS, easing = LinearEasing), label = "clock")
    val ring by animateColorAsState(ringColor, label = "ring")
    val marker by animateColorAsState(if (screen.facing) MaterialTheme.demonColors.ritualSafe else accent, label = "marker")
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = size.minDimension * RING_STROKE_FRACTION
            val side = size.minDimension - stroke
            val topLeft = Offset((size.width - side) / 2f, (size.height - side) / 2f)
            drawArc(track, 0f, 360f, false, topLeft, Size(side, side), style = Stroke(stroke))
            val arc = when (screen.ring) {
                RingMode.Seeking -> 0f
                RingMode.Locking -> screen.lockFraction
                RingMode.Clock -> sweep
            }
            if (arc > 0f) drawArc(ring, -90f, 360f * arc, false, topLeft, Size(side, side), style = Stroke(stroke, cap = StrokeCap.Round))
            // Between the ring and the sigil, pointing outwards at the demon (0° = top).
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
            modifier = Modifier.fillMaxSize(SIGIL_IN_RING),
            micLevel = state.micLevel,
            ghostAlpha = GHOST_ALPHA,
            flash = flash,
        )
    }
}

@Composable
private fun Words(
    state: RitualUiState,
    screen: ChantScreenState,
    accent: Color,
    blocker: (@Composable () -> Unit)?,
    modifier: Modifier,
) {
    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
    ) {
        if (blocker != null) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) { blocker() }
        } else {
            Phrase(screen.words, accent, Modifier.fillMaxWidth().weight(1f))
            VerdictLine(screen, accent, state.micLevel)
        }
    }
}

/** The phrase as one auto-sized text; a line under each word is drawn from the layout. */
@Composable
private fun Phrase(words: List<ChantWord>, accent: Color, modifier: Modifier) {
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
        onTextLayout = { layout = it },
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
        val color = lineColors[i]
        val width = if (words[i].look == WordLook.Pending) thickness * PENDING_LINE_SCALE else thickness
        drawLine(color, Offset(first.left, y), Offset(last.right, y), width, StrokeCap.Round)
    }
}


/** The voice meter while chanting, or the one message that needs action (ADR-0024). */
@Composable
private fun VerdictLine(screen: ChantScreenState, accent: Color, micLevel: Float) {
    val colors = MaterialTheme.colorScheme
    val style = MaterialTheme.textRoles.ritualCue.copy(fontSize = VerdictSize)
    Row(
        Modifier.semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
    ) {
        when (screen.verdict) {
            VerdictLine.None -> Unit
            VerdictLine.Listening -> VoiceMeter(
                if (screen.drifting) 0f else micLevel,
                if (screen.drifting) colors.outline else accent,
                Modifier.size(MeterWidth, MeterHeight),
            )
            VerdictLine.SealCracked -> {
                Text(stringResource(R.string.verdict_seal_cracked).upper(), style = style, color = colors.error, maxLines = 1)
                Text("✕", style = style, color = colors.error)
            }
        }
    }
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

