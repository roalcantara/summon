#!/usr/bin/env python3
"""Generate the ritual sound set as 16-bit mono WAVs (Ritual 2.0 spec, section 6).

Original synthesized work, dedicated to the public domain (CC0).
Usage: python3 -I tools/gen_audio.py app/src/main/res/raw
"""
import math
import os
import random
import struct
import sys
import wave

RATE = 22050
TAU = 2 * math.pi
BED_SECONDS = 8.0  # every bed frequency is a multiple of 1/8 Hz, so the loop is seamless


def silence(seconds):
    return [0.0] * int(seconds * RATE)


def mix(buf, other, start=0.0, gain=1.0):
    s0 = int(start * RATE)
    for i, x in enumerate(other):
        j = s0 + i
        if j >= len(buf):
            break
        buf[j] += gain * x
    return buf


def lowpass(samples, cutoff):
    """One-pole low-pass; `cutoff` is Hz or a function of time in seconds."""
    y = 0.0
    out = []
    for i, x in enumerate(samples):
        fc = cutoff(i / RATE) if callable(cutoff) else cutoff
        y += (1 - math.exp(-TAU * fc / RATE)) * (x - y)
        out.append(y)
    return out


def tone(freq, seconds, partials, attack=0.01, decay=None, vibrato=0.0):
    out = []
    phase = 0.0
    for i in range(int(seconds * RATE)):
        t = i / RATE
        env = min(1.0, t / attack)
        if decay:
            env *= math.exp(-t / decay)
        phase += TAU * freq * (1 + vibrato * math.sin(TAU * 5.5 * t)) / RATE
        out.append(env * sum(a * math.sin(k * phase) for k, a in partials))
    return out


def bell(freq, seconds=1.6):
    parts = ((1.0, 1.0, 1.2), (2.76, 0.5, 0.6), (5.40, 0.25, 0.35), (8.93, 0.12, 0.2))
    out = []
    for i in range(int(seconds * RATE)):
        t = i / RATE
        s = sum(a * math.exp(-t / d) * math.sin(TAU * freq * r * t) for r, a, d in parts)
        out.append(s * min(1.0, t / 0.004))
    return out


def bed_asmodeus():
    """Warm low drone (A1 + fifth) with slow tremolo and faint fire crackle."""
    n = int(BED_SECONDS * RATE)
    drone = []
    for i in range(n):
        t = i / RATE
        trem = 0.8 + 0.2 * math.sin(TAU * 0.25 * t)
        drone.append(trem * (math.sin(TAU * 55 * t) + 0.6 * math.sin(TAU * 110 * t)
                             + 0.5 * math.sin(TAU * 82.5 * t) + 0.2 * math.sin(TAU * 165 * t)
                             + 0.1 * math.sin(TAU * 220 * t)))
    rng = random.Random(32)
    crackle = [0.0] * n
    tail = int(0.06 * RATE)  # keep the loop seam clean
    for _ in range(int(BED_SECONDS * 14)):
        s = rng.randrange(0, n - tail)
        for k in range(int(0.004 * RATE)):
            crackle[s + k] += rng.uniform(-1, 1) * math.exp(-k / (0.001 * RATE))
    crackle = lowpass(crackle, 3000.0)
    return [d + 0.6 * c for d, c in zip(drone, crackle)]


def bed_astaroth():
    """Eerie detuned minor pad (A, C, E) with a slow high shimmer."""
    voices = ((110.0, 1.0), (110.25, 0.8), (130.875, 0.7), (131.125, 0.6), (164.875, 0.5), (165.0, 0.4))
    out = []
    for i in range(int(BED_SECONDS * RATE)):
        t = i / RATE
        pad = sum(a * (math.sin(TAU * f * t) + 0.3 * math.sin(TAU * 2 * f * t) + 0.1 * math.sin(TAU * 3 * f * t))
                  for f, a in voices)
        lfo = 0.5 + 0.5 * math.sin(TAU * 0.125 * t)
        shimmer = lfo * (0.25 * math.sin(TAU * 880 * t) + 0.15 * math.sin(TAU * 1046.625 * t))
        out.append(pad + shimmer)
    return out


def lock():
    """Low thud with a falling pitch, plus a short high shimmer."""
    out = []
    phase = 0.0
    for i in range(int(0.9 * RATE)):
        t = i / RATE
        phase += TAU * (50 + 70 * math.exp(-t / 0.05)) / RATE
        out.append(math.sin(phase) * math.exp(-t / 0.25) * min(1.0, t / 0.003))
    return mix(out, bell(1760.0, 0.6), start=0.02, gain=0.25)


def riser():
    """~2.2 s noise + pitch sweep that swells into the climax."""
    seconds = 2.2
    n = int(seconds * RATE)
    rng = random.Random(29)
    hiss = lowpass([rng.uniform(-1, 1) for _ in range(n)], lambda t: 300 + 5000 * (t / seconds) ** 2)
    out = []
    phase = 0.0
    for i in range(n):
        p = i / n
        phase += TAU * 200 * (6 ** p) / RATE
        out.append(p * p * (0.5 * math.sin(phase) + 0.25 * math.sin(2 * phase) + 0.9 * hiss[i]))
    return out


def boom():
    """Sub hit with a long tail and a short noise thump."""
    out = []
    phase = 0.0
    for i in range(int(2.0 * RATE)):
        t = i / RATE
        phase += TAU * (40 + 60 * math.exp(-t / 0.08)) / RATE
        out.append(math.sin(phase) * math.exp(-t / 0.7) * min(1.0, t / 0.002))
    rng = random.Random(66)
    thump = lowpass([rng.uniform(-1, 1) * math.exp(-(i / RATE) / 0.03) for i in range(int(0.2 * RATE))], 900.0)
    return mix(out, thump, gain=0.8)


def tada():
    """Brassy rising arpeggio and final chord (C5 E5 G5 C6)."""
    brass = ((1, 1.0), (2, 0.6), (3, 0.45), (4, 0.3), (5, 0.2), (6, 0.12))
    buf = silence(1.6)
    for k, f in enumerate((523.25, 659.25, 783.99)):
        mix(buf, tone(f, 0.18, brass, attack=0.015, decay=0.12), start=0.1 * k, gain=0.5)
    for f in (523.25, 659.25, 783.99, 1046.50):
        mix(buf, tone(f, 1.25, brass, attack=0.03, decay=0.6, vibrato=0.004), start=0.33, gain=0.3)
    fade = int(0.2 * RATE)
    for i in range(fade):
        buf[len(buf) - fade + i] *= (fade - i) / fade
    return buf


# A minor pentatonic, one step higher per phrase.
CHIMES = (440.0, 523.25, 587.33, 659.25)

SOUNDS = {
    "bed_asmodeus": (bed_asmodeus, 0.6),
    "bed_astaroth": (bed_astaroth, 0.6),
    "chime_1": (lambda: bell(CHIMES[0]), 0.7),
    "chime_2": (lambda: bell(CHIMES[1]), 0.7),
    "chime_3": (lambda: bell(CHIMES[2]), 0.7),
    "chime_4": (lambda: bell(CHIMES[3]), 0.7),
    "lock": (lock, 0.85),
    "riser": (riser, 0.7),
    "boom": (boom, 0.9),
    "tada": (tada, 0.8),
}


def write(path, samples, peak):
    top = max(abs(x) for x in samples) or 1.0
    data = [int(32767 * peak * x / top) for x in samples]
    with wave.open(path, "wb") as out:
        out.setnchannels(1)
        out.setsampwidth(2)
        out.setframerate(RATE)
        out.writeframes(struct.pack("<%dh" % len(data), *data))


def main(out_dir):
    for name, (make, peak) in SOUNDS.items():
        write(os.path.join(out_dir, name + ".wav"), make(), peak)
        print(name)


if __name__ == "__main__":
    main(sys.argv[1])
