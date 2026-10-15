package com.summondemons.safely.ui.fx

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/** Provided by AppNavHost around the NavHost. */
@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedTransitionScope = staticCompositionLocalOf<SharedTransitionScope?> { null }

/** Provided by AppNavHost inside each destination. */
val LocalNavAnimatedVisibilityScope = staticCompositionLocalOf<AnimatedVisibilityScope?> { null }

/**
 * Wraps a demon's cut-out so it glides between Grimoire, Ritual (climax) and Profile.
 * Without the scopes (previews) it is a plain Box.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedDemonArt(demonId: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val shared = LocalSharedTransitionScope.current
    val visibility = LocalNavAnimatedVisibilityScope.current
    val sharedModifier = if (shared != null && visibility != null) {
        with(shared) { Modifier.sharedElement(rememberSharedContentState("demon-art-$demonId"), visibility) }
    } else {
        Modifier
    }
    Box(modifier.then(sharedModifier), contentAlignment = Alignment.Center) { content() }
}
