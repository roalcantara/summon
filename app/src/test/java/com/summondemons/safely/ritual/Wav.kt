package com.summondemons.safely.ritual

import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** A mono PCM16 WAV file. */
class Wav(val rate: Int, val samples: ShortArray) {
    companion object {
        /** Reads the `fmt ` and `data` chunks; rejects anything but mono 16-bit PCM. */
        fun read(file: File): Wav {
            val raw = file.readBytes()
            require(raw.size >= 12 && String(raw, 0, 4, Charsets.US_ASCII) == "RIFF") { "$file: not a WAV file" }
            val bytes = ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN)
            bytes.position(12)
            var rate = 0
            while (bytes.remaining() >= 8) {
                val id = ByteArray(4).also { bytes.get(it) }.toString(Charsets.US_ASCII)
                val size = bytes.int
                when (id) {
                    "fmt " -> {
                        val format = bytes.short.toInt()
                        val channels = bytes.short.toInt()
                        rate = bytes.int
                        bytes.position(bytes.position() + 6)
                        val bits = bytes.short.toInt()
                        require(format == 1 && channels == 1 && bits == 16) { "$file: need mono 16-bit PCM" }
                        bytes.position(bytes.position() + size - 16)
                    }
                    "data" -> {
                        val n = minOf(size, bytes.remaining()) / 2
                        return Wav(rate, ShortArray(n) { bytes.short })
                    }
                    else -> bytes.position(bytes.position() + size)
                }
            }
            error("$file: no data chunk")
        }
    }
}
