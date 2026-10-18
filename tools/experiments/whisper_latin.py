"""Desktop experiment: Whisper (language forced to Latin) transcribes each line; compare letters with the expected text."""
import json, re, sys
from pathlib import Path
import numpy as np
import soundfile as sf
from faster_whisper import WhisperModel

clips_dir, catalog, size, out_json = Path(sys.argv[1]), Path(sys.argv[2]), sys.argv[3], Path(sys.argv[4])
RATE = 16_000

def demons():
    text = catalog.read_text(); out = {}
    for m in re.finditer(r'id = "(\w+)"', text):
        b = re.search(r"chantPhrases = listOf\((.*?)\)", text[m.end():], re.S)
        if b: out[m.group(1)] = re.findall(r'"([^"]+)"', b.group(1))[:4]
    return out

def letters(s):
    s = s.upper().replace("J", "I").replace("V", "U")
    return re.sub(r"[^A-Z]", "", s)

def lev(a, b):
    prev = list(range(len(b) + 1))
    for i, x in enumerate(a, 1):
        cur = [i]
        for j, y in enumerate(b, 1):
            cur.append(min(prev[j] + 1, cur[j - 1] + 1, prev[j - 1] + (x != y)))
        prev = cur
    return prev[-1]

def segments(y):  # identical to the Allosaurus experiment
    f = RATE // 50
    rms = np.array([np.sqrt(np.mean(y[i:i + f] ** 2)) for i in range(0, len(y) - f, f)])
    thr = 0.08 * np.percentile(rms, 95); voiced = rms > thr
    segs, start, quiet = [], None, 0
    for i, v in enumerate(voiced):
        if v:
            if start is None: start = i
            quiet = 0
        elif start is not None:
            quiet += 1
            if quiet >= 35: segs.append((start, i - quiet + 1)); start = None; quiet = 0
    if start is not None: segs.append((start, len(voiced)))
    return [y[a * f:b * f] for a, b in segs if (b - a) >= 20]

expected = {f"{d}:{k}": letters(l) for d, ls in demons().items() for k, l in enumerate(ls)}
model = WhisperModel(size, device="cpu", compute_type="int8")
rows = []
files = sorted(clips_dir.rglob("*.wav"))
for n, path in enumerate(files):
    name = path.stem.split("-"); demon, label = name[0], name[1]
    voice = name[2] if len(name) >= 4 else "unknown"
    y, rate = sf.read(path, dtype="float32")
    if y.ndim > 1: y = y.mean(1)
    if rate != RATE: raise SystemExit(f"{path}: expected 16 kHz")
    for si, seg in enumerate(segments(y)):
        parts, _ = model.transcribe(seg, language="la", beam_size=5, temperature=0.0, condition_on_previous_text=False, vad_filter=False)
        text = " ".join(p.text for p in parts).strip()
        heard = letters(text)
        scores = {k: lev(heard, e) / max(1, len(e)) for k, e in expected.items()}
        rows.append(dict(clip=path.name, demon=demon, label=label, voice=voice, seg=si, text=text, scores=scores))
    print(f"{n + 1}/{len(files)} {path.name}", flush=True)
out_json.write_text(json.dumps(rows))
