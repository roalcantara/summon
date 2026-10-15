package com.summondemons.safely.platform

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.summondemons.safely.ritual.RitualConfig
import com.summondemons.safely.ritual.VolumeSource
import com.summondemons.safely.ritual.rms
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive

/**
 * Mono 16 kHz PCM16 mic, one RMS value per [RitualConfig.TICK_MS] window.
 * Only collected after RECORD_AUDIO is granted (RitualViewModel). If the recorder
 * can't start, the flow just completes and the ritual treats it as silence.
 */
class AndroidVolumeSource : VolumeSource {

    @SuppressLint("MissingPermission") // Collected only once RECORD_AUDIO is granted.
    override fun rmsLevels(): Flow<Double> = flow {
        val minBuffer = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL, ENCODING)
        if (minBuffer <= 0) return@flow
        val window = (SAMPLE_RATE * RitualConfig.TICK_MS / 1_000).toInt()
        val recorder = try {
            AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, CHANNEL, ENCODING, maxOf(minBuffer, window * 2))
        } catch (e: SecurityException) {
            return@flow
        }
        try {
            if (recorder.state != AudioRecord.STATE_INITIALIZED) return@flow
            recorder.startRecording()
            val buffer = ShortArray(window)
            while (currentCoroutineContext().isActive) {
                val read = recorder.read(buffer, 0, window) // blocks ~TICK_MS
                if (read <= 0) break
                emit(rms(buffer, read))
            }
        } finally {
            recorder.release()
        }
    }.flowOn(Dispatchers.IO)

    private companion object {
        const val SAMPLE_RATE = 16_000
        const val CHANNEL = AudioFormat.CHANNEL_IN_MONO
        const val ENCODING = AudioFormat.ENCODING_PCM_16BIT
    }
}
