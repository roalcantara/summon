package com.summondemons.safely.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
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
import kotlinx.coroutines.delay

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
private const val DOUBT_ALPHA = 0.7f
private const val LINE_FRACTION = 0.06f
private const val PENDING_LINE_SCALE = 0.7f
private const val WAVE_AMPLITUDE = 0.08f
private const val BADGE_FRACTION = 0.22f
private const val BLINK_HALF_MS = 300
private const val BLINK_MIN = 0.55f
private const val HINT_MS = 2_500L
private const val HINT_SCRIM_ALPHA = 0.88f
private const val BACKDROP_DIM = 0.55f
private const val WORDS_SCRIM_ALPHA = 0.85f
private const val METER_TWEEN_MS = 120
private const val METER_FLOOR = 0.22f
private const val SYLLABLE_DOT = "·"
private val METER_HEIGHTS = listOf(0.45f, 0.8f, 1f, 0.7f, 0.4f)
private val NameSize = 24.sp
private val DirectionSize = 18.sp
private val VerdictSize = 22.sp
private val BandSigil = 96.dp
private val MeterWidth = 26.dp
private val MeterHeight = 22.dp
private val MinPhraseSize = 18.sp
private val MaxPhraseSize = 96.sp
private val PhraseStep = 2.sp

/**
 * The chant screen (ADR-0019): from the lock, the demon's name, sigil (progress, direction,
 * focus clock) and direction line on a slab at the left; the phrase, as large as fits, and
 * one verdict line at the right. Portrait screens get the slab as a top band. The House
 * stage fills the screen edge to edge; the content keeps clear of the system bars.
 */
@Composable
fun ChantScreen(
    state: RitualUiState,
    flash: SigilFlash?,
    showClose: Boolean,
    onClose: () -> Unit,
    /** Debug controls, shown under the verdict (debug builds only). */
    debug: (@Composable () -> Unit)?,
    modifier: Modifier = Modifier,
    /** Reopens the intro card ("?"); null hides the button. */
    onHelp: (() -> Unit)? = null,
    /** False right after the intro card, which already asked to turn sideways. */
    showTurnHint: Boolean = true,
) {
    val demon = state.demon
    val accent = MaterialTheme.demonColors.accent(demon.accent)
    val screen = remember(state.ritual, state.azimuth) {
        chantScreenState(state.ritual, demon.chantPhrases, state.azimuth, demon.ritualHeading)
    }
    var hint by rememberSaveable { mutableStateOf(showTurnHint) }
    LaunchedEffect(Unit) {
        delay(HINT_MS)
        hint = false
    }
    LaunchedEffect(state.ritual.lit) { if (state.ritual.lit > 0) hint = false }

    BoxWithConstraints(modifier) {
        val landscape = maxWidth > maxHeight
        val spacing = MaterialTheme.spacing
        ChantBackdrop(demon.house.stage(), landscape, Modifier.fillMaxSize())
        if (landscape) {
            Row(Modifier.fillMaxSize().safeDrawingPadding().padding(spacing.lg), horizontalArrangement = Arrangement.spacedBy(spacing.lg)) {
                Slab(state, screen, accent, flash, vertical = true, Modifier.fillMaxHeight().weight(SLAB_WEIGHT))
                Words(state, screen, accent, debug, Modifier.fillMaxHeight().weight(1f - SLAB_WEIGHT))
            }
        } else {
            Column(Modifier.fillMaxSize().safeDrawingPadding().padding(spacing.lg), verticalArrangement = Arrangement.spacedBy(spacing.lg)) {
                Slab(state, screen, accent, flash, vertical = false, Modifier.fillMaxWidth())
                Words(state, screen, accent, debug, Modifier.fillMaxWidth().weight(1f))
            }
        }
        Row(Modifier.align(Alignment.TopEnd).safeDrawingPadding(), verticalAlignment = Alignment.CenterVertically) {
            if (onHelp != null) {
                val helpLabel = stringResource(R.string.chant_help)
                IconButton(onClick = onHelp, modifier = Modifier.semantics { contentDescription = helpLabel }) {
                    Text(
                        "?",
                        style = MaterialTheme.textRoles.ritualCue.copy(fontSize = DirectionSize),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (showClose) {
                IconButton(onClick = onClose) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = stringResource(R.string.action_abort),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        if (hint) TurnSidewaysHint(accent, Modifier.fillMaxSize())
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
    val lineColor = when {
        screen.facing -> safe
        screen.drifting -> colors.error
        else -> colors.onSurface
    }
    Surface(modifier, shape = MaterialTheme.shapes.large, color = colors.surfaceContainerLow.copy(alpha = SLAB_ALPHA)) {
        if (vertical) {
            Column(
                Modifier.padding(spacing.md),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(spacing.sm, Alignment.CenterVertically),
            ) {
                Text(state.demon.name, style = MaterialTheme.textRoles.name.copy(fontSize = NameSize), color = accent, maxLines = 1)
                ChantSigil(
                    state, screen, accent, ringColor, flash,
                    Modifier.fillMaxWidth().weight(1f, fill = false).aspectRatio(1f, matchHeightConstraintsFirst = true),
                )
                DirectionCue(screen, state.demon.ritualHeading, lineColor, Modifier.padding(top = spacing.sm))
            }
        } else {
            Row(
                Modifier.padding(spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.md),
            ) {
                ChantSigil(state, screen, accent, ringColor, flash, Modifier.size(BandSigil))
                DirectionCue(screen, state.demon.ritualHeading, lineColor)
            }
        }
    }
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
        screen.clockSeconds?.let { seconds ->
            Text(
                if (screen.clockHeld) stringResource(R.string.chant_clock_paused).upper() else stringResource(R.string.chant_clock_seconds, seconds),
                style = MaterialTheme.textRoles.meta,
                color = colors.onSurfaceVariant,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

/** "FACE EAST ✓" in green; "← FACE EAST" neutral while seeking, red and blinking while drifting (ADR-0020). */
@Composable
private fun DirectionCue(screen: ChantScreenState, target: Float, color: Color, modifier: Modifier = Modifier) {
    val reduceMotion = LocalReduceMotion.current
    val blink = if (screen.drifting && !reduceMotion) {
        rememberInfiniteTransition(label = "direction").animateFloat(
            1f, BLINK_MIN, infiniteRepeatable(tween(BLINK_HALF_MS), RepeatMode.Reverse), label = "directionBlink",
        ).value
    } else {
        1f
    }
    val direction = headingLabel(target)
    val description = when {
        screen.facing -> stringResource(R.string.chant_a11y_facing, direction)
        screen.turn == TurnDirection.Right -> stringResource(R.string.chant_a11y_turn_right, direction)
        else -> stringResource(R.string.chant_a11y_turn_left, direction)
    }
    val style = MaterialTheme.textRoles.ritualCue.copy(fontSize = DirectionSize)
    Row(
        modifier.graphicsLayer { alpha = blink }.semantics(mergeDescendants = true) { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs),
    ) {
        if (!screen.facing) Text(if (screen.turn == TurnDirection.Right) "→" else "←", style = style, color = color)
        Text(stringResource(R.string.chant_face, direction).upper(), style = style, color = color, maxLines = 1)
        if (screen.facing) Text("✓", style = style, color = color)
    }
}

@Composable
private fun Words(
    state: RitualUiState,
    screen: ChantScreenState,
    accent: Color,
    debug: (@Composable () -> Unit)?,
    modifier: Modifier,
) {
    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
    ) {
        Phrase(screen.words, accent, Modifier.fillMaxWidth().weight(1f))
        VerdictLine(screen, accent, state.micLevel)
        debug?.invoke()
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
                if (si > 0) withStyle(SpanStyle(color = colors.outlineVariant)) { append(SYLLABLE_DOT) }
                val color = when {
                    word.look == WordLook.Rejected -> colors.error
                    word.look == WordLook.Heard || word.look == WordLook.Doubtful -> accent
                    si < word.lit -> accent
                    else -> dim
                }
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
            WordLook.Doubtful -> colors.primary.copy(alpha = DOUBT_ALPHA)
            WordLook.Rejected -> colors.error
        }
    }
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    BasicText(
        text = text,
        style = MaterialTheme.textRoles.incantationLarge.copy(textAlign = TextAlign.Center),
        autoSize = TextAutoSize.StepBased(minFontSize = MinPhraseSize, maxFontSize = MaxPhraseSize, stepSize = PhraseStep),
        onTextLayout = { layout = it },
        modifier = modifier.wrapContentHeight(Alignment.CenterVertically).drawBehind {
            layout?.let { drawWordLines(it, ranges, words, lineColors, colors.onError) }
        },
    )
}

private fun DrawScope.drawWordLines(
    layout: TextLayoutResult,
    ranges: List<IntRange>,
    words: List<ChantWord>,
    lineColors: List<Color>,
    badgeInk: Color,
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
        when (words[i].look) {
            WordLook.Rejected -> {
                drawWave(first.left, last.right, y, height * WAVE_AMPLITUDE, color, thickness)
                val r = height * BADGE_FRACTION
                val c = Offset(last.right + r * 0.4f, first.top + r * 0.6f)
                drawCircle(color, r, c)
                val d = r * 0.45f
                drawLine(badgeInk, Offset(c.x - d, c.y - d), Offset(c.x + d, c.y + d), thickness, StrokeCap.Round)
                drawLine(badgeInk, Offset(c.x - d, c.y + d), Offset(c.x + d, c.y - d), thickness, StrokeCap.Round)
            }
            WordLook.Pending -> drawLine(color, Offset(first.left, y), Offset(last.right, y), thickness * PENDING_LINE_SCALE, StrokeCap.Round)
            else -> drawLine(color, Offset(first.left, y), Offset(last.right, y), thickness, StrokeCap.Round)
        }
    }
}

private fun DrawScope.drawWave(from: Float, to: Float, y: Float, amplitude: Float, color: Color, width: Float) {
    val step = amplitude * 4f
    val path = Path().apply {
        moveTo(from, y)
        var x = from
        var up = true
        while (x < to) {
            val next = minOf(x + step, to)
            quadraticTo((x + next) / 2f, if (up) y - amplitude * 2f else y + amplitude * 2f, next, y)
            x = next
            up = !up
        }
    }
    drawPath(path, color, style = Stroke(width, cap = StrokeCap.Round))
}

/** "CHANT ALOUD · I of IV" and its siblings, with the voice meter while listening. */
@Composable
private fun VerdictLine(screen: ChantScreenState, accent: Color, micLevel: Float) {
    val colors = MaterialTheme.colorScheme
    val safe = MaterialTheme.demonColors.ritualSafe
    val color = when (screen.verdict) {
        VerdictLine.Sealed, VerdictLine.HoldSteady -> safe
        VerdictLine.ChantAgain, VerdictLine.SealCracked -> colors.error
        VerdictLine.ChantAloud, VerdictLine.Paused, VerdictLine.FindDirection -> colors.outline
    }
    val label = stringResource(
        when (screen.verdict) {
            VerdictLine.FindDirection -> R.string.verdict_find_direction
            VerdictLine.HoldSteady -> R.string.verdict_hold_steady
            VerdictLine.ChantAloud -> R.string.verdict_chant_aloud
            VerdictLine.Paused -> R.string.verdict_paused
            VerdictLine.Sealed -> R.string.verdict_sealed
            VerdictLine.ChantAgain -> R.string.verdict_chant_again
            VerdictLine.SealCracked -> R.string.verdict_seal_cracked
        },
    ).upper()
    val of = stringResource(R.string.verdict_of).upper()
    val style = MaterialTheme.textRoles.ritualCue.copy(fontSize = VerdictSize)
    val nameFamily = MaterialTheme.textRoles.name.fontFamily
    val count = buildAnnotatedString {
        withStyle(SpanStyle(fontFamily = nameFamily, color = accent)) { append(ROMAN_NUMERALS[screen.numeral]) }
        withStyle(SpanStyle(color = colors.outline, fontSize = 0.7.em)) { append(" $of ") }
        withStyle(SpanStyle(fontFamily = nameFamily, color = accent)) { append(ROMAN_NUMERALS.last()) }
    }
    Row(
        Modifier.semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
    ) {
        if (screen.verdict == VerdictLine.ChantAloud || screen.verdict == VerdictLine.Paused) {
            val listening = screen.verdict == VerdictLine.ChantAloud
            VoiceMeter(if (listening) micLevel else 0f, if (listening) accent else colors.outline, Modifier.size(MeterWidth, MeterHeight))
        }
        Text(label, style = style, color = color, maxLines = 1)
        when (screen.verdict) {
            VerdictLine.Sealed -> Text("✓", style = style, color = color)
            VerdictLine.ChantAgain, VerdictLine.SealCracked -> Text("✕", style = style, color = color)
            else -> Unit
        }
        Text(count, style = style, maxLines = 1)
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

/** Shown for a moment after the lock: turn sideways, keep the microphone clear. */
@Composable
private fun TurnSidewaysHint(accent: Color, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier.background(colors.surfaceContainerLowest.copy(alpha = HINT_SCRIM_ALPHA)).padding(MaterialTheme.spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md, Alignment.CenterVertically),
    ) {
        Text(
            stringResource(R.string.chant_turn_sideways),
            style = MaterialTheme.textRoles.name.copy(fontSize = NameSize, textAlign = TextAlign.Center),
            color = accent,
        )
        Text(
            stringResource(R.string.chant_keep_mic_clear),
            style = MaterialTheme.textRoles.body.copy(textAlign = TextAlign.Center),
            color = colors.onSurfaceVariant,
            modifier = Modifier.widthIn(max = 420.dp),
        )
    }
}
