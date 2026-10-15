#!/usr/bin/env python3
"""Generate the placeholder "Tada!" fanfare as a 16-bit mono WAV.

Original synthesized work, dedicated to the public domain (CC0).
Usage: python3 -I tools/gen_tada.py app/src/main/res/raw/tada.wav
"""
import math
import struct
import sys
import wave

RATE = 22050
TOTAL_S = 1.2
C5, E5, G5, C6 = 523.25, 659.25, 783.99, 1046.50


def render():
    n = int(RATE * TOTAL_S)
    buf = [0.0] * n

    def add(freq, start, dur, amp):
        s0 = int(start * RATE)
        for i in range(int(dur * RATE)):
            j = s0 + i
            if j >= n:
                break
            t = i / RATE
            env = min(1.0, t / 0.01) * math.exp(-3.0 * t / dur)
            wave_ = (math.sin(2 * math.pi * freq * t)
                     + 0.35 * math.sin(4 * math.pi * freq * t)
                     + 0.15 * math.sin(6 * math.pi * freq * t))
            buf[j] += amp * env * wave_

    for k, f in enumerate((C5, E5, G5)):  # rising arpeggio
        add(f, 0.09 * k, 0.16, 0.5)
    for f in (C5, E5, G5, C6):  # final chord
        add(f, 0.30, 0.90, 0.35)

    fade = int(0.15 * RATE)
    for i in range(fade):
        buf[n - fade + i] *= (fade - i) / fade
    peak = max(abs(x) for x in buf) or 1.0
    return [int(32767 * 0.8 * x / peak) for x in buf]


def main(path):
    samples = render()
    with wave.open(path, "wb") as out:
        out.setnchannels(1)
        out.setsampwidth(2)
        out.setframerate(RATE)
        out.writeframes(struct.pack("<%dh" % len(samples), *samples))


if __name__ == "__main__":
    main(sys.argv[1])
