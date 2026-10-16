package com.summondemons.safely.platform

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.annotation.RawRes
import androidx.compose.runtime.staticCompositionLocalOf
import com.summondemons.safely.R

/**
 * One-shot sounds outside the Ritual (§5): the Home opening chime and the Grimoire "Tada!".
 * One SoundPool, created once and released with the activity. Main thread only.
 */
class GameSounds(context: Context) {

    enum class Cue(@param:RawRes val res: Int) { Chime4(R.raw.chime_4), Tada(R.raw.tada) }

    private val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_GAME)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()
    private val pool = SoundPool.Builder().setMaxStreams(MAX_STREAMS).setAudioAttributes(attributes).build()
    private val ids: Map<Cue, Int> = Cue.entries.associateWith { pool.load(context.applicationContext, it.res, 1) }
    private var released = false

    fun play(cue: Cue, volume: Float = 1f) {
        if (released) return
        ids[cue]?.let { pool.play(it, volume, volume, 1, 0, 1f) }
    }

    fun release() {
        if (released) return
        released = true
        pool.release()
    }

    private companion object {
        const val MAX_STREAMS = 2
    }
}

/** Provided by AppNavHost for the activity's lifetime; null in previews. */
val LocalGameSounds = staticCompositionLocalOf<GameSounds?> { null }
