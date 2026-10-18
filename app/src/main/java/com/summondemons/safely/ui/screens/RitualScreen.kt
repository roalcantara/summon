package com.summondemons.safely.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.summondemons.safely.ritual.RitualConfig.LOCK_HOLD_MS
import com.summondemons.safely.ritual.RitualEffect
import com.summondemons.safely.ritual.RitualPhase
import com.summondemons.safely.ritual.RitualUiState
import com.summondemons.safely.ritual.RitualViewModel
import com.summondemons.safely.ritual.headingLabel
import com.summondemons.safely.ui.components.AltarRing
import com.summondemons.safely.ui.components.ChantTablet
import com.summondemons.safely.ui.components.CueChip
import com.summondemons.safely.ui.components.DebugControls
import com.summondemons.safely.ui.components.CUTOUT_ASPECT
import com.summondemons.safely.ui.components.DemonCutout
import com.summondemons.safely.ui.components.FocusTone
import com.summondemons.safely.ui.components.PrimaryActionButton
import com.summondemons.safely.ui.components.ROMAN_NUMERALS
import com.summondemons.safely.ui.fx.InkedSigil
import com.summondemons.safely.ui.fx.LocalReduceMotion
import com.summondemons.safely.ui.fx.ParticleField
import com.summondemons.safely.ui.fx.ParticleStyle
import com.summondemons.safely.ui.fx.SharedDemonArt
import com.summondemons.safely.ui.fx.SigilFlash
import com.summondemons.safely.ui.fx.segment
import com.summondemons.safely.ui.theme.demonColors
import com.summondemons.safely.ui.theme.spacing
import com.summondemons.safely.ui.theme.textRoles
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

private const val SIGIL_WIDTH_FRACTION = 0.86f
private const val SIGIL_IN_RING_FRACTION = 0.84f
private const val SIGIL_MAX_HEIGHT_FRACTION = 0.42f
private const val GHOST_ALPHA = 0.12f
private const val CUTOUT_WIDTH_FRACTION = 0.8f
private const val RING_ANIM_MS = 700
private const val HEARTBEAT_HALF_MS = 150 // 300 ms heartbeat while wavering
private const val WAVER_MIN_ALPHA = 0.45f
private const val SEAL_FLARE_MS = 700
private const val CRACK_MS = 1_500
private const val REJECT_MS = 1_500
private const val INTRO_MS = 500
private const val SEAL_FLASH_MIX = 0.5f
private const val EMBER_ALPHA = 0.5f
private const val CLIMAX_SCRIM_ALPHA = 0.85f
private const val CLIMAX_PARTICLES = 80
private const val FLASH_PEAK = 0.65f
private const val FLASH_HALF_WIDTH = 0.06f
private const val FLASH_ACCENT_ALPHA = 0.6f
private const val RISE_START_SCALE = 0.4f
private val RiseDistance = 48.dp
private val NameSize = 20.sp

/** Phases in which the sigil's rings are inked (the ritual is or was locked this attempt). */
private val SEALED_PHASES = setOf(RitualPhase.Chanting, RitualPhase.Wavering, RitualPhase.Climax, RitualPhase.Done)

/** Before the lock the portrait altar stays; from the lock the chant screen takes over (ADR-0019). */
private val PRE_LOCK_PHASES = setOf(RitualPhase.Seeking, RitualPhase.Locking)

/** A phrase that just sealed or cracked; [id] keeps repeats distinct. */
private data class Mark(val phrase: Int, val id: Long)

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
    RitualFeedback(viewModel.effects, state.demon.ambientBed, state.ritual.phase)
    // The focus clock only runs while the screen is started (§2).
    LifecycleEventEffect(Lifecycle.Event.ON_START) { viewModel.setActive(true) }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.setActive(false) }

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

    // Seal flares and the crack cue come from the lossy effects flow: UI only, nothing durable.
    var flare by remember { mutableStateOf<Mark?>(null) }
    var cracked by remember { mutableStateOf<Mark?>(null) }
    var rejected by remember { mutableStateOf<Mark?>(null) }
    var flash by remember { mutableStateOf<SigilFlash?>(null) }
    val accent = MaterialTheme.demonColors.accent(state.demon.accent)
    val sealColor = lerp(accent, MaterialTheme.demonColors.inkHighlight, SEAL_FLASH_MIX)
    val crackColor = MaterialTheme.colorScheme.error
    LaunchedEffect(viewModel) {
        var serial = 0L
        viewModel.effects.collect { effect ->
            when (effect) {
                is RitualEffect.Sealed -> {
                    val mark = Mark(effect.phrase, ++serial)
                    flare = mark
                    flash = SigilFlash(effect.phrase + 1, sealColor, SEAL_FLARE_MS, mark.id)
                    launch {
                        delay(SEAL_FLARE_MS.toLong())
                        if (flare == mark) flare = null
                    }
                }
                is RitualEffect.Cracked -> {
                    val mark = Mark(effect.phrase, ++serial)
                    cracked = mark
                    flash = SigilFlash(effect.phrase + 1, crackColor, CRACK_MS, mark.id)
                    launch {
                        delay(CRACK_MS.toLong())
                        if (cracked == mark) cracked = null
                    }
                }
                is RitualEffect.Rejected -> {
                    val mark = Mark(effect.phrase, ++serial)
                    rejected = mark
                    flash = SigilFlash(effect.phrase + 1, crackColor, REJECT_MS, mark.id)
                    launch {
                        delay(REJECT_MS.toLong())
                        if (rejected == mark) rejected = null
                    }
                }
                else -> Unit
            }
        }
    }

    RitualContent(
        state = state,
        flare = flare?.phrase,
        cracked = cracked?.phrase,
        rejected = rejected != null,
        flash = flash,
        permanentlyDenied = permanentlyDenied,
        onRetryPermission = { launcher.launch(Manifest.permission.RECORD_AUDIO) },
        onOpenSettings = {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)),
            )
        },
        onSimulateHeadingChange = viewModel::setSimulateHeading,
        onHoldToChantChange = viewModel::setHoldToChant,
        onCycleClipLabel = viewModel::cycleClipLabel,
        onDismissIntro = viewModel::dismissIntro,
        onShowIntro = viewModel::openIntro,
        onAbort = onAbort,
        modifier = modifier,
    )
}

@Composable
private fun RitualContent(
    state: RitualUiState,
    flare: Int?,
    cracked: Int?,
    rejected: Boolean,
    flash: SigilFlash?,
    permanentlyDenied: Boolean,
    onRetryPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    onSimulateHeadingChange: (Boolean) -> Unit,
    onHoldToChantChange: (Boolean) -> Unit,
    onCycleClipLabel: () -> Unit,
    onDismissIntro: () -> Unit,
    onShowIntro: () -> Unit,
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
    var confirmAbort by rememberSaveable { mutableStateOf(false) }
    // With a seal held, leaving asks first; the ritual keeps ticking behind the question (§3).
    val requestClose = { if (ritual.phrase >= 1 && !inClimax) confirmAbort = true else onAbort() }
    BackHandler(enabled = inClimax) {}
    BackHandler(enabled = !inClimax) { requestClose() }
    val chromeAlpha = 1f - segment(ritual.climaxFraction, 0f, 0.3f)
    val ringProgress by animateFloatAsState(
        if (ritual.phase in SEALED_PHASES) 1f else 0f,
        tween(RING_ANIM_MS),
        label = "sigilRing",
    )
    val wavering = ritual.phase == RitualPhase.Wavering
    val heartbeat = if (wavering && !reduceMotion) rememberHeartbeat() else null
    RequestLandscape(true)
    // The altar's error panels, now in place of the phrase (ADR-0020).
    val blocker: (@Composable () -> Unit)? = when {
        inClimax -> null
        state.micGranted == false -> {
            {
                ErrorPanel(stringResource(R.string.mic_rationale)) {
                    PrimaryActionButton(
                        text = stringResource(if (permanentlyDenied) R.string.action_open_settings else R.string.action_retry),
                        onClick = if (permanentlyDenied) onOpenSettings else onRetryPermission,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        state.micGranted == true && state.micUnavailable -> {
            { ErrorPanel(stringResource(R.string.mic_unavailable)) }
        }
        state.headingUnavailable && !state.simulateHeading -> {
            { ErrorPanel(stringResource(R.string.heading_unavailable)) }
        }
        else -> null
    }

    Box(modifier.fillMaxSize().background(colors.background)) {
        ChantScreen(
            state = state,
            flash = flash,
            showClose = !inClimax,
            onClose = requestClose,
            onHelp = onShowIntro,
            showTurnHint = !state.introOffered,
            blocker = blocker,
            debug = if (BuildConfig.DEBUG && !inClimax) {
                {
                    DebugControls(
                        simulateHeading = state.simulateHeading,
                        onSimulateHeadingChange = onSimulateHeadingChange,
                        onHoldToChantChange = onHoldToChantChange,
                        lastScore = state.lastScore,
                        clipLabel = state.clipLabel,
                        onCycleClipLabel = onCycleClipLabel,
                    )
                }
            } else {
                null
            },
            modifier = Modifier.fillMaxSize().graphicsLayer { alpha = chromeAlpha },
        )
        // The intro card comes down over the chant screen and goes back up to reveal it (ADR-0019).
        AnimatedVisibility(
            visible = state.introVisible && !inClimax,
            enter = if (reduceMotion) fadeIn(tween(INTRO_MS)) else slideInVertically(tween(INTRO_MS)) { -it } + fadeIn(tween(INTRO_MS)),
            exit = if (reduceMotion) fadeOut(tween(INTRO_MS)) else slideOutVertically(tween(INTRO_MS)) { -it } + fadeOut(tween(INTRO_MS)),
        ) {
            ChantIntro(demon = demon, onSummon = onDismissIntro, showClose = true, onClose = requestClose, modifier = Modifier.fillMaxSize())
        }
        if (inClimax) ClimaxOverlay(demon, accent, ritual.climaxFraction, reduceMotion)
    }

    if (confirmAbort && !inClimax) {
        AlertDialog(
            onDismissRequest = { confirmAbort = false },
            title = { Text(stringResource(R.string.abandon_title), style = MaterialTheme.typography.headlineMedium) },
            text = { Text(stringResource(R.string.abandon_body), style = MaterialTheme.textRoles.body) },
            confirmButton = {
                PrimaryActionButton(stringResource(R.string.action_stay), onClick = { confirmAbort = false })
            },
            dismissButton = {
                OutlinedButton(onClick = {
                    confirmAbort = false
                    onAbort()
                }) { Text(stringResource(R.string.action_abandon)) }
            },
            containerColor = colors.surfaceContainer,
        )
    }
}


/** The chant screen is landscape (ADR-0019); portrait comes back when it closes or the Ritual leaves. */
@Composable
private fun RequestLandscape(landscape: Boolean) {
    val activity = LocalActivity.current ?: return
    DisposableEffect(activity, landscape) {
        activity.requestedOrientation =
            if (landscape) ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        onDispose { activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT }
    }
}
@Composable
private fun rememberHeartbeat(): State<Float> = rememberInfiniteTransition(label = "heartbeat").animateFloat(
    1f, WAVER_MIN_ALPHA, infiniteRepeatable(tween(HEARTBEAT_HALF_MS), RepeatMode.Reverse), label = "heartbeatAlpha",
)

@Composable
private fun cueText(cue: RitualCue, demon: Demon): String {
    val direction = headingLabel(demon.ritualHeading)
    return when (cue.kind) {
        CueKind.MicDenied -> stringResource(R.string.cue_mic_denied)
        CueKind.MicUnavailable -> stringResource(R.string.cue_mic_unavailable)
        CueKind.NoHeading -> stringResource(R.string.cue_no_heading)
        CueKind.Cracked -> stringResource(R.string.cue_cracked, ROMAN_NUMERALS[cue.crackedPhrase ?: 0])
        CueKind.Rejected -> stringArrayResource(R.array.cue_rejected).let { it[(cue.rejections - 1).coerceAtLeast(0) % it.size] }
        CueKind.TurnLeft -> stringResource(R.string.hint_turn_left, direction)
        CueKind.TurnRight -> stringResource(R.string.hint_turn_right, direction)
        CueKind.Face -> stringResource(R.string.hint_face, direction)
        CueKind.HoldSteady -> stringResource(R.string.hint_hold_steady)
        CueKind.Chant -> stringResource(R.string.cue_chant)
        CueKind.Wavering -> stringResource(R.string.cue_wavering, direction)
        CueKind.WaveringFocus -> stringResource(R.string.cue_wavering_focus, direction)
        CueKind.Opening -> stringResource(R.string.hint_summoning)
    }
}

@Composable
private fun chantHeaderText(header: ChantHeader): String = when (header) {
    is ChantHeader.SealedCount -> stringResource(
        R.string.chant_sealed_count,
        if (header.count == 0) "0" else ROMAN_NUMERALS[header.count - 1],
    )
    is ChantHeader.Seconds -> stringResource(R.string.chant_seconds, header.seconds)
    ChantHeader.Drifting -> stringResource(R.string.chant_drift)
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
                // Fits both dimensions: the climax plays in landscape too (ADR-0019).
                .fillMaxWidth(CUTOUT_WIDTH_FRACTION)
                .fillMaxHeight(CUTOUT_WIDTH_FRACTION)
                .wrapContentSize()
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
