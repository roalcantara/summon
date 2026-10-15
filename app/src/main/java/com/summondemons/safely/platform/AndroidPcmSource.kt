package com.summondemons.safely.platform

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.summondemons.safely.ritual.PcmFrame
import com.summondemons.safely.ritual.PcmSource
import com.summondemons.safely.ritual.RitualConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive

/**
 * Mono PCM16 mic in [RitualConfig.FRAME_MS] frames. Tries 16 kHz, then 44.1 kHz (the
 * only rate every device must support). Only collected after RECORD_AUDIO is granted
 * (RitualViewModel). If no recorder starts, or reading fails, the flow just completes;
 * RitualViewModel then reports the mic as unavailable.
 */
class AndroidPcmSource : PcmSource {

    override fun frames(): Flow<PcmFrame> = flow {
        val recorder = openRecorder() ?: return@flow
        try {
            recorder.startRecording()
            if (recorder.recordingState != AudioRecord.RECORDSTATE_RECORDING) return@flow
            val rate = recorder.sampleRate
            val size = frameSize(rate)
            val buffer = ShortArray(size)
            while (currentCoroutineContext().isActive) {
                val read = recorder.read(buffer, 0, size) // blocks ~FRAME_MS
                if (read <= 0) break
                emit(PcmFrame(buffer.copyOf(read), read, rate))
            }
        } finally {
            recorder.release()
        }
    }.flowOn(Dispatchers.IO)

    @SuppressLint("MissingPermission") // Collected only once RECORD_AUDIO is granted.
    private fun openRecorder(): AudioRecord? {
        for (rate in SAMPLE_RATES) {
            val minBuffer = AudioRecord.getMinBufferSize(rate, CHANNEL, ENCODING)
            if (minBuffer <= 0) continue
            val recorder = try {
                AudioRecord(MediaRecorder.AudioSource.MIC, rate, CHANNEL, ENCODING, maxOf(minBuffer, frameSize(rate) * 4))
            } catch (e: SecurityException) {
                return null
            } catch (e: IllegalArgumentException) {
                continue
            }
            if (recorder.state == AudioRecord.STATE_INITIALIZED) return recorder
            recorder.release()
        }
        return null
    }

    private fun frameSize(sampleRate: Int): Int = (sampleRate * RitualConfig.FRAME_MS / 1_000).toInt()

    private companion object {
        val SAMPLE_RATES = listOf(16_000, 44_100)
        const val CHANNEL = AudioFormat.CHANNEL_IN_MONO
        const val ENCODING = AudioFormat.ENCODING_PCM_16BIT
    }
}
