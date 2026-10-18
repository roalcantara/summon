# 0026 — Chant check by Whisper in Latin (proposed)

- Date: 2026-10-18
- Status: Accepted; implemented by ADR-0027.

## Context
On the generated corpus (ADR-0025: 64 honest TTS rituals in 8 voices with augmentation, 13 TTS
and 40 LibriSpeech nonsense takes), three judges were compared per line. Scripts:
`tools/experiments/`.

| Judge (line known, as in the app) | Honest lines passed | Nonsense lines passed |
|---|---|---|
| Vowel alignment (ADR-0022) | ≈ 40 % | ≈ 40 % |
| Allosaurus universal phones vs espeak Latin (broad classes) | 95 % | 70 % |
| Whisper tiny, language Latin, letter distance ≤ 0.75 | 71 % | 0.4 % |
| Whisper base, language Latin, letter distance ≤ 0.75 | 85.5 % | 0.7 % |

Letter distance = edit distance between the transcript's letters (J→I, V→U) and the line's, divided
by the line's length. Whisper runs without a prompt: priming it with the line would make it hear
the line in nonsense too. Most honest failures are English-accented voices that Whisper writes as
English words ("I stand my high face…" for OSTENDE MIHI FACIEM); a second try or a 0.85 threshold
(≈ 12 % nonsense) covers part of it.

## Proposal
Judge each line (or chunk) by transcribing it on device with Whisper base, language Latin, and
comparing letters with the line; keep the chant screen, timer and seals as they are.

## Real voices (2026-10-18, one tester, 3 honest rituals, 2 nonsense clips)
Whisper base, language Latin, scoring each line against the best-matching stretch of everything
heard (free start and end, so repeats and restarts are tolerated; segments under 0.8 s ignored,
because Whisper invents text such as "I'm sorry, I'm sorry…" over short noise):

| Clip | I | II | III | IV |
|---|---|---|---|---|
| Asmodeus, honest | 0.00 | 0.10 | 0.53 | 0.39 |
| Astaroth, honest #1 | 0.14 | 0.15 | 0.33 | 0.11 |
| Astaroth, honest #2 | 0.36 | 0.54 | 0.56 | 0.28 |
| Nonsense vs Asmodeus | 0.72 | 0.72–0.74 | 0.76 | 0.67 |
| Honest audio vs the other demon's lines | 0.56–0.73 | | | |

At about 0.55, 11 of 12 honest lines pass and 0 of 16 wrong lines; the margin is narrow and the
sample is one voice. Script: `tools/experiments/whisper_real_lines.py`. The first real "chant" clip
(2026-10-18 05:41) was the Portuguese test phrase and is now labelled nonsense.

## Second voice (2026-10-18, 2 honest Asmodeus rituals)
| Clip | I | II | III | IV | Same audio vs Astaroth's lines |
|---|---|---|---|---|---|
| Asmodeus 16:18 (room talk and laughter in the take) | 0.28 | 0.56 | 0.64 | 0.56 | 0.54–0.67 |
| Asmodeus 16:19 | 0.22 | 0.21 | 0.49 | 0.33 | 0.43–0.67 |

Both voices together at 0.60: 19 of 20 honest lines pass; ordinary nonsense ("Olá meu amigo")
passes no line (0.67–0.76); Latin from the other demon, a deliberately hard stand-in, passes about
15 % of lines. Whisper works as a lenient but real check; keep best-stretch scoring, free retries
and the 60 s timer. Clips are tagged locally as `v1` (first tester) and `v2` (second).

## Open points before any Android work
- **Real voices:** two voices measured (above); the threshold (≈ 0.60) should be re-checked on logged sessions once the judge runs on device.
- **Size and speed:** Whisper base adds roughly 60–80 MB (quantised) and needs whisper.cpp or
  sherpa-onnx; judging a 5 s line takes about 1–3 s on a mid-range phone (to be measured).
- **Privacy:** the app would then recognise speech on device. The privacy policy, store listing
  and ADR-0018's "no words are recognised" must change; audio still never leaves the phone.
