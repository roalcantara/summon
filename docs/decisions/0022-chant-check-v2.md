# 0022 — Chant check v2: alignment and shuffle baseline

- Date: 2026-10-18
- Status: Accepted. Amends ADR-0018 (the verdict and rhythm parts; recording, privacy and alternatives stand). Superseded by ADR-0027.

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

## Tuning
The first device build used the starting values above, which were checked only against
synthetic vowels; they were **not** calibrated on real voices (an earlier version of this ADR
said otherwise). Tuning now runs on the generated corpus and is confirmed on logged real
sessions (ADR-0025); the measured values and rates are recorded here when that is done.

## Consequences
"No speech or words are recognised" stays true. Held vowels and reversed chants no
longer pass. Plan: `docs/superpowers/plans/2026-10-18-chant-check-v2.md`.
