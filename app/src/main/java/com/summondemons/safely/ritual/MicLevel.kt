package com.summondemons.safely.ritual

import com.summondemons.safely.ritual.RitualConfig.FRAME_MS
import com.summondemons.safely.ritual.RitualConfig.MIC_LEVEL_ATTACK_MS
import com.summondemons.safely.ritual.RitualConfig.MIC_LEVEL_CEILING
import com.summondemons.safely.ritual.RitualConfig.MIC_LEVEL_RELEASE_MS

/** Next smoothed mic level in [0, 1] after one frame of [rms]. Drives glow only. */
fun smoothMicLevel(current: Float, rms: Double): Float {
    val target = (rms / MIC_LEVEL_CEILING).toFloat().coerceIn(0f, 1f)
    val timeConstant = if (target > current) MIC_LEVEL_ATTACK_MS else MIC_LEVEL_RELEASE_MS
    return current + (target - current) * (FRAME_MS / timeConstant).coerceAtMost(1f)
}
