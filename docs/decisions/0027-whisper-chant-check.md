# 0027 — On-device Whisper chant check

- Date: 2026-10-18
- Status: Accepted. Replaces the vowel judge and chunks of ADR-0022; amends ADR-0018 (no words recognised → on-device recognition), ADR-0024 (no part arcs, no rejection state) and ADR-0025 (copy, timer refill on seal only).

## Decision
- Whisper base (q5_1, MIT) runs on the phone through whisper.cpp v1.9.4 (MIT), language Latin, no prompt, greedy decoding, encoder context sized to the audio.
- **Listen until found:** after each pause (≥ 0.6 s after ≥ 0.8 s of new voice) or 6 s of continuous voice, the audio since the line began (≤ 25 s) is transcribed; the line seals when its letters are within 0.60 of the best-matching stretch of the transcript (J→I, V→U; repeats collapsed; likely-silence segments dropped). No rejection state.
- Words turn green as checks cover them; the current seal pulses faster while a check runs.
- Drift hysteresis: lock at ±20°, waver beyond 25°, resume within 20°.
- Devices that fail to load the model or need more than 4 s for a 5 s check use the lenient rule (a line seals when every syllable lit), decided once per install.
- The model ships in the app (≈ 57 MB); `tools/fetch_whisper_model.sh` downloads it for builds.

## Consequences
Speech is recognised on the device; transcripts are discarded at once in release builds and kept
only in debug session logs. Half a long line can pass (plan note 4); `TranscriptRescoreTest`
re-checks the threshold on logged sessions.

## Spike

Measured on Galaxy S24 (SM-S921B, arm64-v8a, 4 threads):

- Model load: 106 ms
- Native heap: +678 MB (peak ~698 MB)
- 3s noise shrink=true: 440 ms (vs 1993 ms shrink=false)
- 5s noise shrink=true: 565 ms (vs 2015 ms shrink=false)
- 10s noise shrink=true: 954 ms (vs 2078 ms shrink=false)

Measured on emulator (arm64-v8a, 2 threads):

- Model load: 3137 ms
- Native heap: +678 MB (peak ~699 MB)
- 3s noise: 21.8 s (warmup)
- 5s noise shrink=true: 6.6 s (vs 17.2 s shrink=false)
- 10s noise shrink=true: 7.1 s (vs 10.5 s shrink=false)
- spike.wav (88.54s): transcribed recognisably in Latin with `shrink=true` ("Da emuniung residente ostende mihi facim, toam, potincham..."). Text is identical between `shrink=true` and `shrink=false`.

Decision: Go. `shrinkContext = true` is kept; device check enables lenient mode on slow devices (emulator / low-end) and Whisper on fast hardware (Galaxy S24 completes a 10 s check in under 1.0 s).

## Fix: device check timing (2026-10-18)
The first device check timed the model copy out of the APK, the model load and whisper.cpp's
first-run warm-up together with the 5 s transcription. On a fresh install of the Galaxy S24 that
took 7.4 s, so the phone was marked "slow" and every ritual ran in lenient mode (nonsense sealed
lines by lighting syllables). The check now loads and warms up untimed; the cache key moved to
`v2` so earlier verdicts are re-checked. Result on the S24: Whisper mode, 396 ms for 5 s.

Desktop Whisper on the nonsense clip of that session heard gibberish and rejected lines I–III
(0.61–0.71), but line IV scored 0.56: short lines against long babble are the weak spot to watch
in the threshold re-check (`TranscriptRescoreTest`).
