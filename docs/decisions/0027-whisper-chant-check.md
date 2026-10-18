# 0027 — On-device Whisper chant check

- Date: 2026-10-18
- Status: Proposed

## Spike

Measured on Android (arm64-v8a, 2 threads):

- Model load: 3137 ms
- Native heap: +678 MB (peak ~699 MB)
- 3s noise: 21.8 s (warmup)
- 5s noise shrink=true: 6.6 s (vs 17.2 s shrink=false)
- 10s noise shrink=true: 7.1 s (vs 10.5 s shrink=false)
- spike.wav (88.54s): transcribed recognisably in Latin with `shrink=true` ("Da emuniung residente ostende mihi facim, toam, potincham..."). Text is identical between `shrink=true` and `shrink=false`.

Decision: Go. `shrinkContext = true` is kept; device check enables lenient mode on slow devices (emulator / low-end) and Whisper on fast hardware.
