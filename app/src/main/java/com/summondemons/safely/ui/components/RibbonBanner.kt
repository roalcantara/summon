package com.summondemons.safely.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.summondemons.safely.ui.theme.spacing

@Composable
fun RibbonBanner(text: String, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = colors.primaryContainer,
        contentColor = colors.onPrimaryContainer,
        border = BorderStroke(2.dp, colors.primary),
        shadowElevation = 8.dp,
    ) {
        Text(
            text = "★  $text  ★",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = MaterialTheme.spacing.xl, vertical = MaterialTheme.spacing.md),
        )
    }
}
