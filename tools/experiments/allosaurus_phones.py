"""Desktop experiment: does a universal phone recognizer separate honest Latin chants from nonsense?"""
import json, re, sys, tempfile, wave
from pathlib import Path
import numpy as np
import soundfile as sf
from allosaurus.app import read_recognizer
import piper.phonemize_espeak as pe

clips_dir = Path(sys.argv[1]); catalog = Path(sys.argv[2]); out_json = Path(sys.argv[3])
RATE = 16_000

def demons():
    text = catalog.read_text()
    out = {}
    for m in re.finditer(r'id = "(\w+)"', text):
        b = re.search(r"chantPhrases = listOf\((.*?)\)", text[m.end():], re.S)
        if b: out[m.group(1)] = re.findall(r'"([^"]+)"', b.group(1))[:4]
    return out

VOW = {**{c: "A" for c in "aɑæɐ"}, **{c: "E" for c in "eɛəɘœøɜ"}, **{c: "I" for c in "iɪyʏɨ"},
       **{c: "O" for c in "oɔɒɤ"}, **{c: "U" for c in "uʊɯʉ"}}
CLS = {**{c: "P" for c in "pbtdkgqʔcɟʈɖ"}, **{c: "F" for c in "fvszʃʒθðhxɣçʝχʁβɸ"},
       **{c: "N" for c in "mnŋɲɱɳ"}, **{c: "L" for c in "lrɾɹʀɫɭʎ"}, **{c: "J" for c in "jwɥ"}}
SKIP = set("ˈˌːʰʲʷ̃ ,.?!-")

def phones_raw(seq):
    return [p for p in seq if p and p not in SKIP and not all(ch in SKIP for ch in p)]

def broad(seq):
    out = []
    for p in seq:
        base = next((ch for ch in p if ch in VOW or ch in CLS), None)
        if base: out.append(VOW.get(base) or CLS[base])
    return out

def lev(a, b):
    prev = list(range(len(b) + 1))
    for i, x in enumerate(a, 1):
        cur = [i]
        for j, y in enumerate(b, 1):
            cur.append(min(prev[j] + 1, cur[j - 1] + 1, prev[j - 1] + (x != y)))
        prev = cur
    return prev[-1]

def segments(y):
    f = RATE // 50
    rms = np.array([np.sqrt(np.mean(y[i:i + f] ** 2)) for i in range(0, len(y) - f, f)])
    thr = 0.08 * np.percentile(rms, 95)
    voiced = rms > thr
    segs, start, quiet = [], None, 0
    for i, v in enumerate(voiced):
        if v:
            if start is None: start = i
            quiet = 0
        elif start is not None:
            quiet += 1
            if quiet >= 35:  # 0.7 s
                segs.append((start, i - quiet + 1)); start = None; quiet = 0
    if start is not None: segs.append((start, len(voiced)))
    return [y[a * f:b * f] for a, b in segs if (b - a) >= 20]  # ≥ 0.4 s

lines = demons()
expected = {}
E = pe.EspeakPhonemizer()
for d, ls in lines.items():
    for k, l in enumerate(ls):
        ph = [p for sent in E.phonemize("la", l.lower()) for p in sent]
        expected[(d, k)] = phones_raw(ph)

model = read_recognizer()
rows = []
files = sorted(p for p in clips_dir.rglob("*.wav"))
for n, path in enumerate(files):
    name = path.stem.split("-")
    demon, label = name[0], name[1]
    voice = name[2] if len(name) >= 4 else "unknown"
    y, rate = sf.read(path, dtype="float32")
    if y.ndim > 1: y = y.mean(1)
    for si, seg in enumerate(segments(y)):
        with tempfile.NamedTemporaryFile(suffix=".wav") as tmp:
            sf.write(tmp.name, seg, rate, subtype="PCM_16")
            heard = model.recognize(tmp.name, "ipa").split()
        hr, hb = phones_raw(heard), broad(heard)
        scores = {}
        for key, exp in expected.items():
            eb = broad(exp)
            scores[f"{key[0]}:{key[1]}"] = (lev(hr, exp) / max(1, len(exp)), lev(hb, eb) / max(1, len(eb)))
        rows.append(dict(clip=path.name, demon=demon, label=label, voice=voice, seg=si, heard=" ".join(heard), scores=scores))
    print(f"{n + 1}/{len(files)} {path.name}", flush=True)
out_json.write_text(json.dumps(rows))
