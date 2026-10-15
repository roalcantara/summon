package com.summondemons.safely.ui.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import com.summondemons.safely.ui.theme.PillShape
import com.summondemons.safely.ui.theme.spacing

private val MinTouchHeight = 48.dp

/** Debug builds only (caller checks BuildConfig.DEBUG). */
@Composable
fun DebugControls(
    simulateHeading: Boolean,
    onSimulateHeadingChange: (Boolean) -> Unit,
    onHoldToChantChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val onHold by rememberUpdatedState(onHoldToChantChange)
    var holding by remember { mutableStateOf(false) }
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilterChip(
            selected = simulateHeading,
            onClick = { onSimulateHeadingChange(!simulateHeading) },
            label = { Text(stringResource(R.string.debug_simulate_heading)) },
            modifier = Modifier.weight(1f),
        )
        Surface(
            shape = PillShape,
            color = if (holding) colors.secondaryContainer else colors.surfaceContainerHigh,
            contentColor = if (holding) colors.onSecondaryContainer else colors.onSurfaceVariant,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = MinTouchHeight)
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
            Box(contentAlignment = Alignment.Center) {
                Text(
                    stringResource(if (holding) R.string.debug_chanting else R.string.debug_hold_to_chant),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}
