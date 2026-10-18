# 0022 — Chant check v2: alignment and shuffle baseline

- Date: 2026-10-18
- Status: Accepted. Amends ADR-0018 (the verdict and rhythm parts; recording, privacy and alternatives stand).

## Context
The ADR-0018 check judged one median formant per lit syllable. Lighting follows loudness
onsets, which voiced consonants (M, N, L) suppress, so on long phrases the nuclei drifted
away from the speech. On the tester clips honest chanting (vowel score 0.50–0.52) and
nonsense (0.40–0.53) were barely apart, and careful readers lost points for breathing.

## Decision
- **Template:** one slot per syllable vowel (`ChantTemplate`); a glide (I or U before
  another vowel in the word) is optional; the unstressed last syllable of a longer word
  also accepts a neutral "uh".
- **Alignment:** every voiced frame's F1/F2 is aligned with the slots by dynamic
  programming (`ChantAligner`): a frame joins a slot or is skipped; a slot needs ≥ 3 frames
  to be heard. Best of the six vocal-tract scales. Lighting drives only the karaoke.
- **Relative verdict:** the same frames are aligned with 24 seeded shuffles of whole slots;
  a chunk passes when its cost is ≤ the ceiling, it beats ≥ 70 % of the shuffles (ties
  count half), and its median heard slot is ≥ 70 ms (talk veto). With fewer than 8 valid
  shuffles only the ceiling applies.
- **Chunks:** lines over 8 syllables are judged in word groups (`ChantChunks`); a
  rejection reopens only its chunk.
- **Rhythm** is no longer scored; pauses never count.

## Tuning (from Task 8 of the plan)
Final calibrated parameters in `RitualConfig`:
- `ALIGN_MIN_SLOT_FRAMES = 3`
- `ALIGN_FRAME_SKIP_COST = 0.6`
- `ALIGN_OPTIONAL_SKIP_COST = 0.15`
- `ALIGN_SKIPPED_WEIGHT = 0.6`
- `ALIGN_MAX_COST = 0.8`
- `BASELINE_SHUFFLES = 24`, `BASELINE_MIN_SHUFFLES = 8`
- `BASELINE_MIN_DIFFERENT = 0.3`
- `BASELINE_PASS_RANK = 0.7`
- `TALK_MAX_SLOT_MS = 70L`
- `CHUNK_MAX_SYLLABLES = 8`, `CHUNK_MIN_SYLLABLES = 3`

## Consequences
"No speech or words are recognised" stays true. Held vowels and reversed chants no
longer pass. Plan: `docs/superpowers/plans/2026-10-18-chant-check-v2.md`.
