import json, sys
import numpy as np
for path in sys.argv[1:]:
    rows = json.load(open(path))
    def src(r): return "tts" if r["voice"].startswith("tts") else "librispeech" if r["voice"].startswith("ls") else "real"
    own = [r["scores"][f"{r['demon']}:{r['seg']}"] for r in rows if r["label"] == "chant" and r["seg"] < 4 and f"{r['demon']}:{r['seg']}" in r["scores"]]
    best_h = [min(r["scores"].values()) for r in rows if r["label"] == "chant"]
    best_n = [min(r["scores"].values()) for r in rows if r["label"] != "chant"]
    p = lambda a: " ".join(f"{q:.2f}" for q in np.percentile(a, [10, 25, 50, 75, 90]))
    print(f"\n== {path}: letter edit distance / expected length; p10 p25 p50 p75 p90")
    print(f"honest, own line      n={len(own):3d}  {p(own)}")
    print(f"honest, best line     n={len(best_h):3d}  {p(best_h)}")
    print(f"nonsense, best line   n={len(best_n):3d}  {p(best_n)}")
    print("threshold  honest-pass  nonsense-pass")
    for t in np.arange(0.2, 1.01, 0.05):
        print(f"  {t:.2f}      {np.mean(np.array(own) <= t):5.1%}      {np.mean(np.array(best_n) <= t):5.1%}")
    for s in ("tts", "librispeech", "real"):
        a = [min(r["scores"].values()) for r in rows if r["label"] != "chant" and src(r) == s]
        if a: print(f"  nonsense {s:12s} n={len(a):3d} median {np.median(a):.2f}")
    ex = [r for r in rows if r["label"] == "chant" and r["seg"] < 4][:6] + [r for r in rows if r["label"] != "chant"][:4]
    for r in ex: print(f"  {r['label']:8s} {r['voice']:12s} seg{r['seg']} '{r['text'][:70]}'")
