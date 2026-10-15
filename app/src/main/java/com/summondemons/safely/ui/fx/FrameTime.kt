package com.summondemons.safely.ui.fx

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.runtime.withFrameMillis

/** Milliseconds since first composition, updated every frame. Read it in draw lambdas to avoid recomposition. */
@Composable
fun rememberFrameTimeMs(): State<Long> = produceState(0L) {
    val start = withFrameMillis { it }
    while (true) {
        withFrameMillis { value = it - start }
    }
}
