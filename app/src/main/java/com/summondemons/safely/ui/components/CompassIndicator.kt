package com.summondemons.safely.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.summondemons.safely.R
import com.summondemons.safely.ritual.headingLabel
import com.summondemons.safely.ritual.normalizeDegrees
import com.summondemons.safely.ui.theme.demonColors
import kotlin.math.roundToInt

private val DialSize = 56.dp

/** Arrow points from the current heading towards [target]; green when aligned. */
@Composable
fun CompassIndicator(azimuth: Float?, target: Float, aligned: Boolean, modifier: Modifier = Modifier) {
    val status by animateColorAsState(
        if (aligned) MaterialTheme.demonColors.ritualSafe else MaterialTheme.colorScheme.error,
        label = "compassStatus",
    )
    val arrowRotation = if (azimuth == null) 0f else normalizeDegrees(target - azimuth)
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(DialSize)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                .border(2.dp, status, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text("▲", color = status, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.rotate(arrowRotation))
        }
        Text(
            text = azimuth?.let { "${it.roundToInt()}°" } ?: stringResource(R.string.compass_unknown),
            color = status,
            style = MaterialTheme.typography.labelMedium,
        )
        Text(headingLabel(target), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
    }
}
