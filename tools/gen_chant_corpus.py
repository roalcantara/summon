#!/usr/bin/env python3
"""Generates the chant-check calibration corpus (ADR-0025).

Honest chants: Piper TTS voices (Italian, Spanish, Portuguese, English, German, French) read
every demon's four lines, at several speeds, phrase by phrase or word by word like a careful
reader. Nonsense: the same voices reading ordinary sentences, counting and "la la la", plus real
people from mini LibriSpeech (OpenSLR 31, CC BY 4.0). Every take gets a random vocal-tract
shift (down to child-like), room reverb, background noise and a phone-microphone band-pass.

Output: app/src/test/chant-clips/generated/{demon}-{chant|nonsense}-{voice}-{variant}.wav
(mono 16 kHz PCM16, git-ignored), read by ChantClipReplayTest. Downloads go to tools/.cache/.

Run:
  uv run --no-project --python 3.12 --with piper-tts --with numpy --with scipy --with soundfile \\
      python -I tools/gen_chant_corpus.py
"""
import argparse
import random
import re
import tarfile
import urllib.request
import wave
from pathlib import Path

import numpy as np
import soundfile as sf
from piper import PiperVoice, SynthesisConfig
from scipy import signal

ROOT = Path(__file__).resolve().parent.parent
CACHE = ROOT / "tools" / ".cache" / "corpus"
OUT = ROOT / "app" / "src" / "test" / "chant-clips" / "generated"
CATALOG = ROOT / "app" / "src" / "main" / "java" / "com" / "summondemons" / "safely" / "data" / "DemonCatalog.kt"
RATE = 16_000

HF = "https://huggingface.co/rhasspy/piper-voices/resolve/main"
VOICES = {
    "paola": "it/it_IT/paola/medium/it_IT-paola-medium",
    "riccardo": "it/it_IT/riccardo/x_low/it_IT-riccardo-x_low",
    "davefx": "es/es_ES/davefx/medium/es_ES-davefx-medium",
    "faber": "pt/pt_BR/faber/medium/pt_BR-faber-medium",
    "lessac": "en/en_US/lessac/medium/en_US-lessac-medium",
    "alba": "en/en_GB/alba/medium/en_GB-alba-medium",
    "thorsten": "de/de_DE/thorsten/medium/de_DE-thorsten-medium",
    "siwis": "fr/fr_FR/siwis/medium/fr_FR-siwis-medium",
}
NONSENSE_TEXT = {
    "faber": ["Olá meu amigo, como está hoje?", "Eu fui ao mercado comprar pão e leite.", "Um, dois, três, quatro, cinco, seis, sete, oito."],
    "paola": ["Buongiorno, oggi il tempo è bellissimo e andiamo al mare.", "Uno, due, tre, quattro, cinque, sei, sette, otto."],
    "riccardo": ["La la la la la la la la la la."],
    "davefx": ["Hola, ¿qué tal estás? Vamos a la playa esta tarde.", "Lo que importa es que lleguemos a tiempo."],
    "lessac": ["Hello there, what are we having for dinner tonight?", "One two three four five six seven eight nine ten."],
    "alba": ["I really don't know what I'm supposed to say into this phone."],
    "thorsten": ["Guten Morgen, wie geht es dir heute? Ich habe keine Zeit."],
    "siwis": ["Bonjour, je ne sais pas ce que je dois dire maintenant."],
}
LIBRISPEECH = "https://www.openslr.org/resources/31/dev-clean-2.tar.gz"


def demons() -> dict:
    """{demon id: [four chant lines]}, read from DemonCatalog.kt so the corpus follows the data."""
    text = CATALOG.read_text(encoding="utf-8")
    out = {}
    for m in re.finditer(r'id = "(\w+)"', text):
        rest = text[m.end():]
        block = re.search(r"chantPhrases = listOf\((.*?)\)", rest, re.S)
        if block:
            out[m.group(1)] = re.findall(r'"([^"]+)"', block.group(1))[:4]
    if not out:
        raise SystemExit(f"no chant phrases found in {CATALOG}")
    return out


def fetch(url: str, dest: Path) -> Path:
    if not dest.exists():
        dest.parent.mkdir(parents=True, exist_ok=True)
        print(f"downloading {url}")
        tmp = dest.with_suffix(dest.suffix + ".part")
        urllib.request.urlretrieve(url, tmp)
        tmp.rename(dest)
    return dest


def voice(tag: str) -> PiperVoice:
    base = VOICES[tag]
    model = fetch(f"{HF}/{base}.onnx", CACHE / "voices" / f"{tag}.onnx")
    fetch(f"{HF}/{base}.onnx.json", CACHE / "voices" / f"{tag}.onnx.json")
    return PiperVoice.load(str(model))


def speak(v: PiperVoice, text: str, length: float, rng: random.Random) -> tuple:
    config = SynthesisConfig(length_scale=length, noise_scale=rng.uniform(0.5, 0.8), noise_w_scale=rng.uniform(0.6, 0.9))
    chunks = list(v.synthesize(text, syn_config=config))
    audio = np.concatenate([c.audio_float_array for c in chunks]) if chunks else np.zeros(1, dtype=np.float32)
    return audio.astype(np.float32), chunks[0].sample_rate if chunks else 22_050


def silence(seconds: float, rate: int) -> np.ndarray:
    return np.zeros(int(seconds * rate), dtype=np.float32)


def ritual(v: PiperVoice, lines: list, length: float, careful: bool, rng: random.Random) -> tuple:
    """All four lines with breaths between them; careful takes read word by word with pauses."""
    parts, rate = [], 22_050
    for line in lines:
        words = line.lower().split() if careful else [line.lower()]
        for w in words:
            audio, rate = speak(v, w, length, rng)
            parts.append(audio)
            if careful:
                parts.append(silence(rng.uniform(0.12, 0.4), rate))
        parts.append(silence(rng.uniform(1.0, 2.0), rate))
    return np.concatenate([silence(0.5, rate)] + parts), rate


def augment(x: np.ndarray, rate: int, rng: random.Random, tract: float) -> np.ndarray:
    """To 16 kHz with every frequency × [tract]; then reverb, phone band-pass, noise and level."""
    y = signal.resample_poly(x, RATE * 100, int(round(rate * tract * 100))).astype(np.float64)
    rt60 = rng.uniform(0.15, 0.45)
    t = np.arange(int(rt60 * RATE)) / RATE
    ir = np.random.default_rng(rng.randrange(1 << 30)).standard_normal(len(t)) * np.exp(-6.9 * t / rt60)
    wet = signal.fftconvolve(y, ir)[: len(y)]
    wet *= np.std(y) / (np.std(wet) + 1e-9)
    y = y + rng.uniform(0.1, 0.35) * wet
    sos = signal.butter(2, [rng.uniform(120, 250), rng.uniform(5_000, 7_200)], btype="band", fs=RATE, output="sos")
    y = signal.sosfilt(sos, y)
    white = np.random.default_rng(rng.randrange(1 << 30)).standard_normal(len(y))
    pink = signal.lfilter([0.049922035, -0.095993537, 0.050612699, -0.004408786], [1, -2.494956002, 2.017265875, -0.522189400], white)
    snr = rng.uniform(15, 32)
    y = y + pink * np.std(y) / (np.std(pink) + 1e-9) / (10 ** (snr / 20))
    return y / (np.max(np.abs(y)) + 1e-9) * rng.uniform(0.25, 0.8)


def write(path: Path, y: np.ndarray) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    pcm = np.clip(y * 32_767, -32_768, 32_767).astype("<i2")
    with wave.open(str(path), "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(RATE)
        w.writeframes(pcm.tobytes())


def librispeech(limit: int) -> list:
    """Up to [limit] (speaker, audio at 16 kHz) utterances from mini LibriSpeech dev-clean-2."""
    archive = fetch(LIBRISPEECH, CACHE / "dev-clean-2.tar.gz")
    root = CACHE / "librispeech"
    if not root.exists():
        with tarfile.open(archive) as tar:
            tar.extractall(root, filter="data")
    flacs = sorted(root.rglob("*.flac"))
    rng = random.Random(3)
    rng.shuffle(flacs)
    out = []
    for f in flacs[:limit]:
        audio, rate = sf.read(f, dtype="float32")
        out.append((f.name.split("-")[0], audio, rate))
    return out


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--variants", type=int, default=4, help="honest takes per voice and demon")
    parser.add_argument("--librispeech", type=int, default=40, help="real-speech nonsense utterances (0 = none)")
    parser.add_argument("--seed", type=int, default=7)
    args = parser.parse_args()

    rng = random.Random(args.seed)
    lines = demons()
    ids = sorted(lines)
    styles = [(1.0, False), (1.35, True), (1.6, True), (1.15, False), (1.25, True), (0.9, False)]
    tracts = [1.0, 0.92, 1.12, 1.25, 1.35]
    count = 0
    for tag in VOICES:
        v = voice(tag)
        for demon in ids:
            for k in range(args.variants):
                length, careful = styles[k % len(styles)]
                x, rate = ritual(v, lines[demon], length * rng.uniform(0.95, 1.05), careful, rng)
                write(OUT / f"{demon}-chant-tts{tag}-v{k + 1}.wav", augment(x, rate, rng, rng.choice(tracts)))
                count += 1
        for k, text in enumerate(NONSENSE_TEXT.get(tag, [])):
            parts = []
            for _ in range(2):
                audio, rate = speak(v, text, rng.uniform(0.9, 1.2), rng)
                parts += [audio, silence(rng.uniform(0.4, 1.0), rate)]
            x = np.concatenate([silence(0.5, rate)] + parts)
            write(OUT / f"{ids[k % len(ids)]}-nonsense-tts{tag}-n{k + 1}.wav", augment(x, rate, rng, rng.choice(tracts)))
            count += 1
        print(f"{tag}: done")
    if args.librispeech:
        for k, (speaker, audio, rate) in enumerate(librispeech(args.librispeech)):
            write(OUT / f"{ids[k % len(ids)]}-nonsense-ls{speaker}-n{k + 1}.wav", augment(audio, rate, rng, 1.0))
            count += 1
    print(f"wrote {count} clips to {OUT}")


if __name__ == "__main__":
    main()
