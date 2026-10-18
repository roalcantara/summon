import json, sys
import numpy as np
rows = json.load(open(sys.argv[1]))
def src(r): return "tts" if r["voice"].startswith("tts") else "librispeech" if r["voice"].startswith("ls") else "real"
for kind, ix in (("raw phones", 0), ("broad classes", 1)):
    honest_correct, honest_best, nonsense_best = [], [], []
    for r in rows:
        best = min(v[ix] for v in r["scores"].values())
        if r["label"] == "chant":
            honest_best.append(best)
            key = f"{r['demon']}:{r['seg']}"
            if r["seg"] < 4 and key in r["scores"]: honest_correct.append(r["scores"][key][ix])
        else:
            nonsense_best.append(best)
    p = lambda a: " ".join(f"{q:.2f}" for q in np.percentile(a, [10, 25, 50, 75, 90]))
    print(f"\n== {kind}: normalized edit distance (lower = closer); p10 p25 p50 p75 p90")
    print(f"honest, own line      n={len(honest_correct):3d}  {p(honest_correct)}")
    print(f"honest, best line     n={len(honest_best):3d}  {p(honest_best)}")
    print(f"nonsense, best line   n={len(nonsense_best):3d}  {p(nonsense_best)}")
    print("threshold  honest-pass  nonsense-pass")
    for t in np.arange(0.3, 1.01, 0.05):
        hp = np.mean(np.array(honest_correct) <= t); npass = np.mean(np.array(nonsense_best) <= t)
        print(f"  {t:.2f}      {hp:5.1%}      {npass:5.1%}")
    # nonsense breakdown by source
    for s in ("tts", "librispeech", "real"):
        a = [min(v[ix] for v in r["scores"].values()) for r in rows if r["label"] != "chant" and src(r) == s]
        if a: print(f"  nonsense {s:12s} n={len(a):3d} median {np.median(a):.2f}")
    a = [r["scores"][f"{r['demon']}:{r['seg']}"][ix] for r in rows if r["label"] == "chant" and src(r) == "real" and r["seg"] < 4]
    if a: print(f"  honest real clip segments: {[round(x,2) for x in a]}")
