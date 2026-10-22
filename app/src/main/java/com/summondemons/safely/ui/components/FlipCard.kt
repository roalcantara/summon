package com.summondemons.safely.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import com.summondemons.safely.ui.fx.LocalReduceMotion

/** Which face of the demon card shows (spec §2). UI state, saved across configuration changes. */
enum class CardSide { Front, Back }

private const val FLIP_MS = 400
private const val FADE_MS = 150
private const val CAMERA_DISTANCE = 12f

/**
 * A tarot card with two faces (spec §2): it turns about its vertical axis in 400 ms, showing
 * [front] up to 90° and [back] after; a 150 ms cross-fade with reduced motion.
 */
@Composable
fun FlipCard(side: CardSide, modifier: Modifier = Modifier, front: @Composable () -> Unit, back: @Composable () -> Unit) {
    if (LocalReduceMotion.current) {
        Crossfade(side, modifier, animationSpec = tween(FADE_MS), label = "cardFade") { shown ->
            Box(Modifier.fillMaxSize()) { if (shown == CardSide.Front) front() else back() }
        }
        return
    }
    val angle by animateFloatAsState(if (side == CardSide.Front) 0f else 180f, tween(FLIP_MS), label = "cardFlip")
    val density = LocalDensity.current.density
    Box(
        modifier.graphicsLayer {
            rotationY = angle
            cameraDistance = CAMERA_DISTANCE * density
        },
    ) {
        if (angle <= 90f) {
            Box(Modifier.fillMaxSize()) { front() }
        } else {
            // Un-mirror the back face.
            Box(Modifier.fillMaxSize().graphicsLayer { rotationY = 180f }) { back() }
        }
    }
}
