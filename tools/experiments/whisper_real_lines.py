import sys
from pathlib import Path
import soundfile as sf
sys.argv=[sys.argv[0], sys.argv[1], sys.argv[2], "base", "/dev/null"]
exec(open("whisper_experiment.py").read().split("expected = ")[0])
from faster_whisper import WhisperModel
model=WhisperModel("base", device="cpu", compute_type="int8"); lines=demons()
def tr(a):
    parts,_=model.transcribe(a, language="la", beam_size=5, temperature=0.0, condition_on_previous_text=False, vad_filter=False)
    return " ".join(p.text for p in parts).strip()
def local(exp, heard):
    """Edit distance of [exp] against the best-matching stretch of [heard] (free start and end), / len(exp)."""
    prev=[0]*(len(heard)+1)
    for i,x in enumerate(exp,1):
        cur=[i]
        for j,y in enumerate(heard,1): cur.append(min(prev[j]+1, cur[j-1]+1, prev[j-1]+(x!=y)))
        prev=cur
    return min(prev)/len(exp)
for path in sorted(Path(sys.argv[1]).glob("*.wav")):
    demon,label=path.stem.split("-")[:2]
    y,_=sf.read(path, dtype="float32")
    heard=letters(" ".join(tr(s) for s in segments(y) if len(s)>=0.8*16000))
    print(f"{path.name:42s} {label:8s} " + "  ".join(f"line{k+1} {local(letters(l), heard):.2f}" for k,l in enumerate(lines[demon])))
    # nonsense must also fail against the other demon's lines
    other=[d for d in lines if d!=demon][0]
    print(f"{'':42s} {'vs '+other:8s} " + "  ".join(f"line{k+1} {local(letters(l), heard):.2f}" for k,l in enumerate(lines[other])))
