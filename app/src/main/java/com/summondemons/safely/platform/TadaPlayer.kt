package com.summondemons.safely.platform

import android.content.Context
import android.media.MediaPlayer
import com.summondemons.safely.R

object TadaPlayer {
    /** Starts the fanfare; the caller releases the returned player. Null if it can't play. */
    fun play(context: Context): MediaPlayer? = try {
        MediaPlayer.create(context, R.raw.tada)?.apply { start() }
    } catch (e: Exception) {
        null
    }
}
