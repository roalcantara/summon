# 0026 — Chant check by Whisper in Latin (proposed)

- Date: 2026-10-18
- Status: Proposed. Would replace the vowel judge of ADR-0018/ADR-0022; needs real-voice confirmation first.

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

## Open points before any Android work
- **Real voices:** no honest real audio exists yet (the one "chant" clip is the Portuguese test
  phrase, mislabelled). Confirm on logged real sessions.
- **Size and speed:** Whisper base adds roughly 60–80 MB (quantised) and needs whisper.cpp or
  sherpa-onnx; judging a 5 s line takes about 1–3 s on a mid-range phone (to be measured).
- **Privacy:** the app would then recognise speech on device. The privacy policy, store listing
  and ADR-0018's "no words are recognised" must change; audio still never leaves the phone.
