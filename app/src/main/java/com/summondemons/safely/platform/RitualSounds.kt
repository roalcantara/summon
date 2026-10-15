package com.summondemons.safely.platform

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Build
import androidx.annotation.RawRes
import com.summondemons.safely.R
import kotlinx.coroutines.delay

/** One-shot cues (SoundPool) plus the demon's looping ambient bed (MediaPlayer). Main thread only. */
class RitualSounds(context: Context, @RawRes bed: Int) {

    enum class Cue(@param:RawRes val res: Int) {
        Chime1(R.raw.chime_1), Chime2(R.raw.chime_2), Chime3(R.raw.chime_3), Chime4(R.raw.chime_4),
        Lock(R.raw.lock), Riser(R.raw.riser), Boom(R.raw.boom);

        companion object {
            fun chime(phrase: Int): Cue = listOf(Chime1, Chime2, Chime3, Chime4)[phrase.coerceIn(0, 3)]
        }
    }

    private val appContext = context.applicationContext
    private val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_GAME)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()
    private val pool = SoundPool.Builder().setMaxStreams(MAX_STREAMS).setAudioAttributes(attributes).build()
    private val ids: Map<Cue, Int> = Cue.entries.associateWith { pool.load(appContext, it.res, 1) }
    private val bedPlayer: MediaPlayer? = try {
        MediaPlayer.create(appContext, bed)?.apply {
            isLooping = true
            setVolume(0f, 0f)
        }
    } catch (e: Exception) {
        null
    }
    private var focusRequest: AudioFocusRequest? = null
    private var focusLost = false
    private var bedVolume = 0f
    private var released = false
    private val audioManager: AudioManager? = appContext.getSystemService(AudioManager::class.java)
    private val focusListener = AudioManager.OnAudioFocusChangeListener { change ->
        if (released) return@OnAudioFocusChangeListener
        when (change) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                bedPlayer?.takeIf { it.isPlaying }?.pause()
                abandonFocus() // also clears focusLost so a later fade re-requests focus
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                focusLost = true
                bedPlayer?.takeIf { it.isPlaying }?.pause()
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                focusLost = false
                if (bedVolume > 0f) bedPlayer?.start()
            }
        }
    }
    fun play(cue: Cue) {
        if (released) return
        ids[cue]?.let { pool.play(it, 1f, 1f, 1, 0, 1f) }
    }

    /** Fades the bed to [to] (0..1); starts it when rising from silence, pauses it at 0. Cancellable. */
    suspend fun fadeBed(to: Float, durationMs: Long) {
        val player = bedPlayer ?: return
        if (released) return
        if (to > 0f && !player.isPlaying && !focusLost && requestFocus()) player.start()
        val from = bedVolume
        val steps = (durationMs / FADE_STEP_MS).coerceAtLeast(1)
        for (i in 1..steps) {
            if (released) return
            bedVolume = from + (to - from) * i / steps
            player.setVolume(bedVolume, bedVolume)
            delay(FADE_STEP_MS)
        }
        if (to == 0f && player.isPlaying) player.pause()
    }

    fun stopBed() {
        if (released) return
        bedVolume = 0f
        bedPlayer?.takeIf { it.isPlaying }?.pause()
        abandonFocus()
    }

    fun release() {
        if (released) return
        stopBed()
        released = true
        pool.release()
        bedPlayer?.release()
    }

    private fun requestFocus(): Boolean {
        val manager = audioManager ?: return true
        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(attributes)
                .setOnAudioFocusChangeListener(focusListener)
                .build()
            focusRequest = request
            manager.requestAudioFocus(request)
        } else {
            @Suppress("DEPRECATION")
            manager.requestAudioFocus(focusListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
        }
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    private fun abandonFocus() {
        focusLost = false
        val manager = audioManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let { manager.abandonAudioFocusRequest(it) }
            focusRequest = null
        } else {
            @Suppress("DEPRECATION")
            manager.abandonAudioFocus(focusListener)
        }
    }

    private companion object {
        const val MAX_STREAMS = 4
        const val FADE_STEP_MS = 50L
    }
}
