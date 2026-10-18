package com.summondemons.safely.platform

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.annotation.RawRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.summondemons.safely.platform.RitualSounds.Cue
import com.summondemons.safely.ritual.RitualEffect
import com.summondemons.safely.ritual.RitualPhase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

private const val BED_VOLUME = 0.7f
private const val BED_DUCKED = 0.2f
private const val FADE_IN_MS = 1_200L
private const val DUCK_MS = 300L
private const val SWELL_MS = 2_000L
private const val FADE_OUT_MS = 1_500L
private const val CRACK_VOLUME = 0.35f

/** Lets the boom and the bed's fade-out ring on after navigating back to the Grimoire. */
private const val RELEASE_DELAY_MS = 2_500L

/** Turns ritual effects into haptics and sounds (§5). Composed by the Ritual screen. */
@Composable
fun RitualFeedback(effects: Flow<RitualEffect>, @RawRes bed: Int, phase: RitualPhase) {
    val context = LocalContext.current
    val sounds = remember(bed) { RitualSounds(context, bed) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val summoned = remember(sounds) { BooleanArray(1) }
    val currentPhase by rememberUpdatedState(phase)
    DisposableEffect(sounds) {
        onDispose {
            // Leaving mid-ritual: silence the bed now; the delayed release only lets cues finish.
            if (!summoned[0]) sounds.stopBed()
            Handler(Looper.getMainLooper()).postDelayed({ sounds.release() }, RELEASE_DELAY_MS)
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { sounds.stopBed() }

    LaunchedEffect(effects, sounds, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            var fade: Job? = null
            fun fadeBed(to: Float, ms: Long) {
                fade?.cancel()
                fade = launch { sounds.fadeBed(to, ms) }
            }
            // The bed follows the phase, not only Locked/Resumed, so it returns after the background (§2).
            when (currentPhase) {
                RitualPhase.Chanting -> fadeBed(BED_VOLUME, FADE_IN_MS)
                RitualPhase.Wavering -> fadeBed(BED_DUCKED, FADE_IN_MS)
                else -> Unit
            }
            try {
                effects.collect { effect ->
                    summoned[0] = effect == RitualEffect.Summoned
                    handle(effect, context, sounds, ::fadeBed)
                }
            } finally {
                fade?.cancel()
            }
        }
    }
}

private fun handle(
    effect: RitualEffect,
    context: Context,
    sounds: RitualSounds,
    fadeBed: (Float, Long) -> Unit,
) {
    when (effect) {
        RitualEffect.ProximityTick -> Haptics.tick(context)
        RitualEffect.Locked -> {
            Haptics.clunk(context)
            sounds.play(Cue.Lock)
            fadeBed(BED_VOLUME, FADE_IN_MS)
        }
        RitualEffect.SyllableLit -> Haptics.syllable(context)
        is RitualEffect.Sealed -> {
            Haptics.chime(context)
            sounds.play(Cue.chime(effect.phrase))
        }
        is RitualEffect.Cracked -> {
            Haptics.crack(context)
            sounds.play(Cue.Boom, CRACK_VOLUME)
        }
        RitualEffect.Drift -> {
            Haptics.warn(context)
            fadeBed(BED_DUCKED, DUCK_MS)
        }
        RitualEffect.Resumed -> fadeBed(BED_VOLUME, DUCK_MS)
        is RitualEffect.LineChecked -> Unit
        is RitualEffect.ClimaxStart -> {
            Haptics.crescendo(context)
            sounds.play(Cue.Riser)
            fadeBed(1f, SWELL_MS)
        }
        RitualEffect.Summoned -> {
            Haptics.success(context)
            sounds.play(Cue.Boom)
            fadeBed(0f, FADE_OUT_MS)
        }
    }
}
