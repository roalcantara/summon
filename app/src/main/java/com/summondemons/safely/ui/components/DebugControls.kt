package com.summondemons.safely.ui.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.summondemons.safely.R
import com.summondemons.safely.ritual.ClipLabel
import com.summondemons.safely.ritual.CheckReport
import com.summondemons.safely.ui.theme.PillShape
import com.summondemons.safely.ui.theme.spacing
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import com.summondemons.safely.platform.DeviceCheck
import com.summondemons.safely.ritual.CheckMode

/** As tall as a chip, so "Hold to Chant" lines up with its neighbours. */
private val ChipHeight = 32.dp

/**
 * The chant screen's debug panel, as small chips (ADR-0021). Debug builds only (caller
 * checks BuildConfig.DEBUG); shown in the floating panel under "D", never in the layout.
 */
@Composable
fun DebugControls(
    simulateHeading: Boolean,
    onSimulateHeadingChange: (Boolean) -> Unit,
    onHoldToChantChange: (Boolean) -> Unit,
    /** The latest Whisper check (ADR-0027). */
    lastCheck: CheckReport?,
    /** Why the ritual runs in lenient mode; empty in Whisper mode. */
    modeReason: String,
    /** Clip recorder label; null = not recording (ADR-0018). */
    clipLabel: ClipLabel?,
    onCycleClipLabel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val small = MaterialTheme.typography.labelSmall
    Column(modifier, verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs)) {
        Text(stringResource(R.string.debug_controls), style = small, color = colors.outline)
        Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs), verticalAlignment = Alignment.CenterVertically) {
            FilterChip(
                selected = simulateHeading,
                onClick = { onSimulateHeadingChange(!simulateHeading) },
                label = { Text(stringResource(R.string.debug_simulate_heading), style = small) },
            )
            HoldToChantChip(onHoldToChantChange)
        }
        CheckModeChip(small)
        FilterChip(
            selected = clipLabel != null,
            onClick = onCycleClipLabel,
            label = {
                Text(
                    stringResource(
                        when (clipLabel) {
                            null -> R.string.debug_clip_off
                            ClipLabel.Chant -> R.string.debug_clip_chant
                            ClipLabel.Nonsense -> R.string.debug_clip_nonsense
                        },
                    ),
                    style = small,
                )
            },
        )
        Text(
            when {
                modeReason.isNotEmpty() -> stringResource(R.string.debug_chant_mode, modeReason)
                lastCheck == null -> stringResource(R.string.debug_chant_score_none)
                else -> stringResource(
                    R.string.debug_chant_score,
                    ROMAN_NUMERALS[lastCheck.heard.phrase],
                    lastCheck.heard.distance,
                    if (lastCheck.heard.found) "✓" else "✗",
                    lastCheck.checkMs,
                    lastCheck.transcript.take(40),
                )
            },
            style = small,
            color = colors.onSurfaceVariant,
        )
    }
}

/** Sounds the expected vowels while pressed (ADR-0018). */
@Composable
private fun HoldToChantChip(onHoldToChantChange: (Boolean) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val onHold by rememberUpdatedState(onHoldToChantChange)
    var holding by remember { mutableStateOf(false) }
    Surface(
        shape = PillShape,
        color = if (holding) colors.secondaryContainer else colors.surfaceContainerHigh,
        contentColor = if (holding) colors.onSecondaryContainer else colors.onSurfaceVariant,
        modifier = Modifier
            .heightIn(min = ChipHeight)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        holding = true
                        onHold(true)
                        try {
                            awaitRelease()
                        } finally {
                            holding = false
                            onHold(false)
                        }
                    },
                )
            },
    ) {
        Box(Modifier.padding(horizontal = MaterialTheme.spacing.md), contentAlignment = Alignment.Center) {
            Text(
                stringResource(if (holding) R.string.debug_chanting else R.string.debug_hold_to_chant),
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

/** Opens the debug sheet from Home and Grimoire. Debug builds only (caller checks BuildConfig.DEBUG). */
@Composable
fun DebugChip(onClick: () -> Unit, modifier: Modifier = Modifier) {
    AssistChip(onClick = onClick, label = { Text(stringResource(R.string.debug_chip)) }, modifier = modifier)
}

/** Debug: force the next ritual's check mode, auto → Whisper → lenient → auto (ADR-0028). */
@Composable
private fun CheckModeChip(style: TextStyle) {
    val context = LocalContext.current
    var forced by remember { mutableStateOf(DeviceCheck.forced(context)) }
    FilterChip(
        selected = forced != null,
        onClick = {
            val next = when (forced) {
                null -> CheckMode.Whisper
                CheckMode.Whisper -> CheckMode.Lenient
                CheckMode.Lenient -> null
            }
            DeviceCheck.setForced(context, next)
            forced = next
        },
        label = {
            Text(
                stringResource(
                    when (forced) {
                        null -> R.string.debug_check_auto
                        CheckMode.Whisper -> R.string.debug_check_whisper
                        CheckMode.Lenient -> R.string.debug_check_lenient
                    },
                ),
                style = style,
            )
        },
    )
}

