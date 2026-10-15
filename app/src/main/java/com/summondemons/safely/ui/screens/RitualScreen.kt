package com.summondemons.safely.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.summondemons.safely.BuildConfig
import com.summondemons.safely.R
import com.summondemons.safely.data.Demon
import com.summondemons.safely.platform.RitualFeedback
import com.summondemons.safely.ritual.RitualPhase
import com.summondemons.safely.ritual.RitualUiState
import com.summondemons.safely.ritual.RitualViewModel
import com.summondemons.safely.ritual.TurnDirection
import com.summondemons.safely.ritual.headingLabel
import com.summondemons.safely.ritual.turnDirection
import com.summondemons.safely.ui.components.CompassArc
import com.summondemons.safely.ui.components.CompassMode
import com.summondemons.safely.ui.components.DebugControls
import com.summondemons.safely.ui.components.DemonCutout
import com.summondemons.safely.ui.components.KaraokeLine
import com.summondemons.safely.ui.components.PrimaryActionButton
import com.summondemons.safely.ui.fx.InkedSigil
import com.summondemons.safely.ui.fx.LocalReduceMotion
import com.summondemons.safely.ui.fx.ParticleField
import com.summondemons.safely.ui.fx.ParticleStyle
import com.summondemons.safely.ui.fx.SharedDemonArt
import com.summondemons.safely.ui.fx.rememberFrameTimeMs
import com.summondemons.safely.ui.fx.segment
import com.summondemons.safely.ui.theme.demonColors
import com.summondemons.safely.ui.theme.spacing
import kotlin.math.abs
import kotlin.math.sin

private const val SIGIL_WIDTH_FRACTION = 0.86f
private const val SIGIL_IN_ARC_FRACTION = 0.84f
private const val SIGIL_MAX_HEIGHT_FRACTION = 0.45f
private const val GHOST_ALPHA = 0.12f
private const val CUTOUT_ASPECT = 896f / 1200f
private const val CUTOUT_WIDTH_FRACTION = 0.8f
private const val RING_ANIM_MS = 700
private const val FLICKER_MIN = 0.45f
private const val FLICKER_RATE = 0.02f // radians per ms
private const val COMPLETED_ALPHA = 0.85f
private const val UPCOMING_ALPHA = 0.6f
private const val EMBER_ALPHA = 0.5f
private const val CLIMAX_SCRIM_ALPHA = 0.85f
private const val CLIMAX_PARTICLES = 80
private const val FLASH_PEAK = 0.65f
private const val FLASH_HALF_WIDTH = 0.06f
private const val FLASH_ACCENT_ALPHA = 0.6f
private const val RISE_START_SCALE = 0.4f
private val RiseDistance = 48.dp

/** Phases in which the sigil's rings are inked (the ritual is or was locked this attempt). */
private val SEALED_PHASES = setOf(RitualPhase.Chanting, RitualPhase.Wavering, RitualPhase.Climax, RitualPhase.Done)

@Composable
fun RitualScreen(
    viewModel: RitualViewModel,
    onSummoned: () -> Unit,
    onAbort: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = LocalActivity.current
    RitualFeedback(viewModel.effects, state.demon.ambientBed)

    // Ask once; re-check on every resume (e.g. after returning from Settings).
    var requested by rememberSaveable { mutableStateOf(false) }
    var permanentlyDenied by rememberSaveable { mutableStateOf(false) }
    var deniedBefore by rememberSaveable { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        viewModel.onMicPermission(granted)
        // A first denial or a dismissed dialog also reports "no rationale"; only a repeat
        // denial without rationale means "don't ask again".
        permanentlyDenied = !granted && deniedBefore && activity != null &&
            !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.RECORD_AUDIO)
        if (!granted) deniedBefore = true
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        when {
            granted -> viewModel.onMicPermission(true)
            !requested -> {
                requested = true
                launcher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    val done = state.ritual.phase == RitualPhase.Done
    LaunchedEffect(done) {
        if (done) onSummoned()
    }

    RitualContent(
        state = state,
        permanentlyDenied = permanentlyDenied,
        onRetryPermission = { launcher.launch(Manifest.permission.RECORD_AUDIO) },
        onOpenSettings = {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)),
            )
        },
        onSimulateHeadingChange = viewModel::setSimulateHeading,
        onHoldToChantChange = viewModel::setHoldToChant,
        onAbort = onAbort,
        modifier = modifier,
    )
}

@Composable
private fun RitualContent(
    state: RitualUiState,
    permanentlyDenied: Boolean,
    onRetryPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    onSimulateHeadingChange: (Boolean) -> Unit,
    onHoldToChantChange: (Boolean) -> Unit,
    onAbort: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val demon = state.demon
    val ritual = state.ritual
    val accent = MaterialTheme.demonColors.accent(demon.accent)
    val colors = MaterialTheme.colorScheme
    val spacing = MaterialTheme.spacing
    val reduceMotion = LocalReduceMotion.current
    val inClimax = ritual.phase == RitualPhase.Climax || ritual.phase == RitualPhase.Done
    val chromeAlpha = 1f - segment(ritual.climaxFraction, 0f, 0.3f)
    val ringProgress by animateFloatAsState(
        if (ritual.phase in SEALED_PHASES) 1f else 0f,
        tween(RING_ANIM_MS),
        label = "sigilRing",
    )
    val wavering = ritual.phase == RitualPhase.Wavering
    // The frame clock only runs while the sigil actually flickers; it is read in the layer, not in composition.
    val frameTime = if (wavering && !reduceMotion) rememberFrameTimeMs() else null

    BoxWithConstraints(modifier.fillMaxSize().background(colors.background)) {
        val sigilSide = minOf(maxWidth * SIGIL_WIDTH_FRACTION, maxHeight * SIGIL_MAX_HEIGHT_FRACTION)
        ParticleField(accent.copy(alpha = EMBER_ALPHA), ParticleStyle.Embers, Modifier.fillMaxSize())
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(spacing.lg)
                .graphicsLayer { alpha = chromeAlpha },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    demon.name.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = accent,
                    modifier = Modifier.weight(1f),
                )
                if (!inClimax) {
                    IconButton(onClick = onAbort) {
                        Icon(
                            painter = painterResource(R.drawable.ic_close),
                            contentDescription = stringResource(R.string.action_abort),
                            tint = colors.onSurfaceVariant,
                        )
                    }
                }
            }

            Box(Modifier.size(sigilSide), contentAlignment = Alignment.Center) {
                CompassArc(
                    azimuth = state.azimuth,
                    target = demon.ritualHeading,
                    mode = compassMode(ritual.phase),
                    graceFraction = ritual.graceFraction,
                    modifier = Modifier.fillMaxSize(),
                )
                InkedSigil(
                    sigil = demon.sigil,
                    inkProgress = ritual.inkProgress,
                    accent = accent,
                    rimText = demon.name.uppercase(),
                    modifier = Modifier.fillMaxSize(SIGIL_IN_ARC_FRACTION).graphicsLayer {
                        if (wavering) alpha = flickerAlpha(frameTime)
                    },
                    ringProgress = ringProgress,
                    micLevel = state.micLevel,
                    ghostAlpha = GHOST_ALPHA,
                )
            }

            Text(
                phaseHint(state),
                style = MaterialTheme.typography.titleLarge,
                color = when (ritual.phase) {
                    RitualPhase.Wavering -> colors.error
                    RitualPhase.Chanting -> accent
                    else -> colors.onSurface
                },
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )

            ChantStack(
                phrases = demon.chantPhrases,
                phrase = ritual.phrase,
                lit = ritual.lit,
                chanting = ritual.phase == RitualPhase.Chanting,
                accent = accent,
                modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()),
            )

            if (!inClimax && state.micGranted == false) {
                ErrorPanel(stringResource(R.string.mic_rationale)) {
                    PrimaryActionButton(
                        text = stringResource(if (permanentlyDenied) R.string.action_open_settings else R.string.action_retry),
                        onClick = if (permanentlyDenied) onOpenSettings else onRetryPermission,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            if (!inClimax && state.micGranted == true && state.micUnavailable) ErrorPanel(stringResource(R.string.mic_unavailable))
            if (!inClimax && state.headingUnavailable && !state.simulateHeading) ErrorPanel(stringResource(R.string.heading_unavailable))

            if (BuildConfig.DEBUG && !inClimax) {
                DebugControls(
                    simulateHeading = state.simulateHeading,
                    onSimulateHeadingChange = onSimulateHeadingChange,
                    onHoldToChantChange = onHoldToChantChange,
                )
            }
        }
        if (inClimax) ClimaxOverlay(demon, accent, ritual.climaxFraction, reduceMotion)
    }
}

private fun compassMode(phase: RitualPhase): CompassMode = when (phase) {
    RitualPhase.Seeking, RitualPhase.Locking -> CompassMode.Seeking
    RitualPhase.Wavering -> CompassMode.Wavering
    RitualPhase.Chanting, RitualPhase.Climax, RitualPhase.Done -> CompassMode.Locked
}

/** Sigil alpha multiplier while Wavering: a nervous flicker, or a steady dim under reduced motion (null clock). */
private fun flickerAlpha(frameTime: State<Long>?): Float {
    val time = frameTime?.value ?: return FLICKER_MIN
    return FLICKER_MIN + (1f - FLICKER_MIN) * abs(sin(time * FLICKER_RATE))
}

@Composable
private fun phaseHint(state: RitualUiState): String {
    val direction = headingLabel(state.demon.ritualHeading)
    return when (state.ritual.phase) {
        RitualPhase.Seeking -> when (turnDirection(state.azimuth, state.demon.ritualHeading)) {
            TurnDirection.Left -> stringResource(R.string.hint_turn_left, direction)
            TurnDirection.Right -> stringResource(R.string.hint_turn_right, direction)
            null -> stringResource(R.string.hint_face, direction)
        }
        RitualPhase.Locking -> stringResource(R.string.hint_hold_steady)
        RitualPhase.Chanting -> if (state.micGranted == false && !state.holdToChant) {
            stringResource(R.string.hint_mic_needed)
        } else {
            stringResource(R.string.hint_chant)
        }
        RitualPhase.Wavering -> stringResource(R.string.hint_face_again, direction)
        RitualPhase.Climax, RitualPhase.Done -> stringResource(R.string.hint_summoning)
    }
}

@Composable
private fun ChantStack(
    phrases: List<String>,
    phrase: Int,
    lit: Int,
    chanting: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val currentLine = remember { BringIntoViewRequester() }
    LaunchedEffect(phrase) { currentLine.bringIntoView() }
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
    ) {
        phrases.forEachIndexed { index, line ->
            when {
                index < phrase -> Text(
                    line,
                    style = MaterialTheme.typography.bodyMedium,
                    color = accent.copy(alpha = COMPLETED_ALPHA),
                    textAlign = TextAlign.Center,
                )
                index == phrase -> KaraokeLine(
                    text = line,
                    lit = lit,
                    accent = accent,
                    dim = if (chanting) colors.onSurface else colors.outline,
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.bringIntoViewRequester(currentLine),
                )
                else -> Text(
                    line,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.outline.copy(alpha = UPCOMING_ALPHA),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun ErrorPanel(message: String, action: (@Composable () -> Unit)? = null) {
    val colors = MaterialTheme.colorScheme
    Surface(shape = MaterialTheme.shapes.medium, color = colors.errorContainer, contentColor = colors.onErrorContainer) {
        Column(Modifier.padding(MaterialTheme.spacing.lg), verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md)) {
            Text(message, style = MaterialTheme.typography.bodyMedium)
            action?.invoke()
        }
    }
}

/** Climax staging by [fraction]: dim, converge, flash, then the demon rises out of the sigil. */
@Composable
private fun ClimaxOverlay(demon: Demon, accent: Color, fraction: Float, reduceMotion: Boolean) {
    val colors = MaterialTheme.colorScheme
    val dim = segment(fraction, 0f, 0.3f)
    val converge = segment(fraction, 0f, 0.6f)
    val flash = if (reduceMotion) 0f else 1f - (abs(fraction - FLASH_PEAK) / FLASH_HALF_WIDTH).coerceIn(0f, 1f)
    val rise = if (reduceMotion) fraction else segment(fraction, 0.7f, 1f)
    val riseDistance = with(LocalDensity.current) { RiseDistance.toPx() }
    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) {} // swallow touches so nothing behind the overlay is tappable
            .background(colors.surfaceContainerLowest.copy(alpha = CLIMAX_SCRIM_ALPHA * dim)),
        contentAlignment = Alignment.Center,
    ) {
        ParticleField(accent, ParticleStyle.Embers, Modifier.fillMaxSize(), count = CLIMAX_PARTICLES, converge = converge, seed = 29)
        if (flash > 0f) {
            Canvas(Modifier.fillMaxSize()) {
                drawRect(
                    Brush.radialGradient(
                        listOf(colors.onBackground.copy(alpha = flash), accent.copy(alpha = flash * FLASH_ACCENT_ALPHA), Color.Transparent),
                    ),
                )
            }
        }
        SharedDemonArt(
            demonId = demon.id,
            modifier = Modifier
                .fillMaxWidth(CUTOUT_WIDTH_FRACTION)
                .aspectRatio(CUTOUT_ASPECT)
                .graphicsLayer {
                    alpha = rise
                    // Reduced motion: the cut-out only fades in.
                    val scale = if (reduceMotion) 1f else lerp(RISE_START_SCALE, 1f, rise)
                    scaleX = scale
                    scaleY = scale
                    translationY = if (reduceMotion) 0f else (1f - rise) * riseDistance
                },
        ) {
            DemonCutout(demon.cutoutArt, demon.name, accent, Modifier.fillMaxSize(), silhouette = 1f - rise)
        }
    }
}
