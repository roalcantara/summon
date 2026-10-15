package com.summondemons.safely.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.summondemons.safely.BuildConfig
import com.summondemons.safely.R
import com.summondemons.safely.platform.Haptics
import com.summondemons.safely.ritual.RitualUiState
import com.summondemons.safely.ritual.RitualViewModel
import com.summondemons.safely.ritual.headingLabel
import com.summondemons.safely.ui.components.ButtonTone
import com.summondemons.safely.ui.components.CompassIndicator
import com.summondemons.safely.ui.components.DebugControls
import com.summondemons.safely.ui.components.PrimaryActionButton
import com.summondemons.safely.ui.components.SigilView
import com.summondemons.safely.ui.theme.demonColors
import com.summondemons.safely.ui.theme.spacing
import kotlin.math.roundToInt

private val SigilSize = 220.dp
private const val CURRENT_PHRASE_ALPHA = 0.6f

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

    LaunchedEffect(state.completed) {
        if (state.completed) {
            Haptics.success(context)
            onSummoned()
        }
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
    val accent = MaterialTheme.demonColors.accent(demon.accent)
    val colors = MaterialTheme.colorScheme
    val spacing = MaterialTheme.spacing

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = colors.surfaceContainerHigh,
            border = BorderStroke(1.dp, accent.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(Modifier.padding(spacing.md), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.ritual_label), style = MaterialTheme.typography.labelMedium, color = accent)
                    Text(demon.name, style = MaterialTheme.typography.headlineLarge, color = colors.onSurface)
                    Text(
                        stringResource(R.string.ritual_instruction, headingLabel(demon.ritualHeading)),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                }
                CompassIndicator(azimuth = state.azimuth, target = demon.ritualHeading, aligned = state.aligned)
            }
        }

        SigilView(sigil = demon.sigil, opacity = state.progress.sigilOpacity, accent = accent, modifier = Modifier.size(SigilSize))

        Surface(shape = MaterialTheme.shapes.medium, color = colors.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
            Column(
                Modifier.padding(spacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(spacing.xs),
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.chant_label), style = MaterialTheme.typography.labelMedium, color = accent)
                    Text(
                        stringResource(R.string.chant_resonance, (state.progress.sigilOpacity * 100).roundToInt()),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.primary,
                    )
                }
                demon.chantPhrases.forEachIndexed { index, phrase ->
                    val reached = index < state.progress.phraseIndex
                    val current = index == state.progress.phraseIndex
                    val phraseColor: Color = when {
                        reached -> accent
                        current && state.aligned -> accent.copy(alpha = CURRENT_PHRASE_ALPHA)
                        else -> colors.outline
                    }
                    Text(
                        phrase,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = if (current) FontWeight.Bold else FontWeight.Normal,
                        color = phraseColor,
                        textAlign = TextAlign.Center,
                    )
                }
                LinearProgressIndicator(
                    progress = { state.progress.sustainFraction },
                    color = accent,
                    trackColor = colors.surfaceContainerLowest,
                    modifier = Modifier.fillMaxWidth().padding(top = spacing.sm),
                )
            }
        }

        if (state.micGranted == false) {
            Surface(shape = MaterialTheme.shapes.medium, color = colors.errorContainer, contentColor = colors.onErrorContainer) {
                Column(Modifier.padding(spacing.lg), verticalArrangement = Arrangement.spacedBy(spacing.md)) {
                    Text(stringResource(R.string.mic_rationale), style = MaterialTheme.typography.bodyMedium)
                    PrimaryActionButton(
                        text = stringResource(if (permanentlyDenied) R.string.action_open_settings else R.string.action_retry),
                        onClick = if (permanentlyDenied) onOpenSettings else onRetryPermission,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        if (state.micGranted == true && state.micUnavailable) {
            Surface(shape = MaterialTheme.shapes.medium, color = colors.errorContainer, contentColor = colors.onErrorContainer) {
                Text(
                    stringResource(R.string.mic_unavailable),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(spacing.lg),
                )
            }
        }

        if (BuildConfig.DEBUG) {
            DebugControls(
                simulateHeading = state.simulateHeading,
                onSimulateHeadingChange = onSimulateHeadingChange,
                onHoldToChantChange = onHoldToChantChange,
            )
        }

        PrimaryActionButton(
            text = stringResource(R.string.action_abort),
            onClick = onAbort,
            tone = ButtonTone.Danger,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
