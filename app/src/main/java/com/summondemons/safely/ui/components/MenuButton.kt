package com.summondemons.safely.ui.components

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.summondemons.safely.R

/** Gear at the top right of Home and the Grimoire: opens Settings (and, in debug builds, Debug). */
@Composable
fun MenuButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(
            painterResource(R.drawable.ic_settings),
            contentDescription = stringResource(R.string.settings_title),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
