package com.summondemons.safely.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import com.summondemons.safely.R
import com.summondemons.safely.ritual.RitualUiState
import com.summondemons.safely.ritual.TurnDirection
import com.summondemons.safely.ritual.UiRecord
import com.summondemons.safely.ui.components.StageBackground
import com.summondemons.safely.ui.fx.SigilFlash
import com.summondemons.safely.ui.fx.rememberAttention
import com.summondemons.safely.ui.theme.Stage
import com.summondemons.safely.ui.theme.demonColors
import com.summondemons.safely.ui.theme.spacing
import com.summondemons.safely.ui.theme.stage
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

private const val BACKDROP_DIM = 0.55f
private const val WORDS_SCRIM_ALPHA = 0.85f
/** The auto-sizer settles within a frame or two; wait this long before logging its size. */
private const val PHRASE_SETTLE_MS = 300L

/**
 * The chant screen (ADR-0019, ADR-0029): the pieces of ChantPieces.kt in the split arrangement
 * (ChantArrangements.kt) over the House stage. ✕ sits alone top-right; chevrons on the edge
 * point the way to turn while seeking or paused.
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
    /** Reopens the intro card ("?"); null hides the button. */
    onHelp: (() -> Unit)? = null,
    /** Shown instead of the line when the ritual can't hear or can't sense direction (ADR-0020). */
    blocker: (@Composable () -> Unit)? = null,
    /** The phrase size the screen settles on, for the debug log (ADR-0029). */
    onPhraseLaid: (UiRecord) -> Unit = {},
) {
    val demon = state.demon
    val accent = MaterialTheme.demonColors.accent(demon.accent)
    val screen = remember(state.ritual, state.azimuth, state.checking, state.feedback) {
        chantScreenState(state.ritual, demon.chantPhrases, state.azimuth, demon.ritualHeading, state.checking, state.feedback)
    }
    var phraseSp by remember { mutableFloatStateOf(0f) }
    var box by remember { mutableStateOf(IntSize.Zero) }
    var sigilPx by remember { mutableIntStateOf(0) }

    BoxWithConstraints(modifier) {
        val landscape = maxWidth > maxHeight
        ChantBackdrop(demon.house.stage(), landscape, Modifier.fillMaxSize())
        // One thing glows at a time (ADR-0033); nothing while an error panel replaces the line.
        val attention = if (blocker != null) Attention.None else screen.attention
        val sigilGlow = rememberAttention(attention == Attention.Sigil || attention == Attention.SigilAlarm, fast = attention == Attention.SigilAlarm)
        val phraseGlow = rememberAttention(attention == Attention.Phrase)
        val parts = ChantParts(
            buttons = { CornerButtons(onHelp, debug) },
            title = { ChantTitle(demon.name, screen.seals, screen.checking, screen.drifting, accent) },
            pill = { DirectionPillView(screen.pill, demon.ritualHeading) },
            sigil = { m -> ChantSigil(state, screen, accent, flash, sigilGlow, m.onSizeChanged { sigilPx = it.width }) },
            slot = { if (blocker == null) FeedbackSlot(screen.slot, accent, state.micLevel, screen.drifting) },
            line = { m ->
                if (blocker != null) {
                    Box(m, contentAlignment = Alignment.Center) { blocker() }
                } else {
                    ChantLine(screen.words, screen.gated, screen.drifting, screen.lockFraction, accent, phraseGlow, onPhraseSize = { phraseSp = it }, modifier = m.onSizeChanged { box = it })
                }
            },
        )
        val content = Modifier.fillMaxSize().safeDrawingPadding().padding(MaterialTheme.spacing.lg)
        if (landscape) SplitArrangement(parts, content) else PortraitArrangement(parts, content)
        val turn = screen.pill.turn
        if (turn != null && blocker == null) {
            EdgeChevrons(
                turn,
                paused = screen.drifting,
                modifier = Modifier
                    .align(if (turn == TurnDirection.Right) Alignment.CenterEnd else Alignment.CenterStart)
                    .safeDrawingPadding()
                    .padding(horizontal = MaterialTheme.spacing.xs),
            )
        }
        if (showClose) {
            IconButton(onClick = onClose, modifier = Modifier.align(Alignment.TopEnd).safeDrawingPadding().padding(MaterialTheme.spacing.lg)) {
                Icon(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = stringResource(R.string.action_abort),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    // Logged once the size settles; any change restarts the wait (ADR-0029).
    val density = LocalDensity.current
    val phrase = state.ritual.phrase
    LaunchedEffect(phrase, phraseSp, box, sigilPx, blocker == null) {
        if (phraseSp <= 0f || blocker != null) return@LaunchedEffect
        delay(PHRASE_SETTLE_MS)
        with(density) {
            onPhraseLaid(
                UiRecord(
                    phrase = phrase,
                    phraseSp = phraseSp,
                    boxWidthDp = box.width.toDp().value.roundToInt(),
                    boxHeightDp = box.height.toDp().value.roundToInt(),
                    sigilDp = sigilPx.toDp().value.roundToInt(),
                ),
            )
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
