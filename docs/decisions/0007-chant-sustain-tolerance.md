# 0007 — Chant sustain tolerance and volume threshold

- Date: 2026-10-15
- Status: Superseded by ADR-0008

## Context
PRODUCT.md requires mic volume "sustained above threshold for 2 s" per phrase.
Real chanting dips below any RMS threshold between syllables and words, so a
strict reset-on-silence rule makes phrases nearly impossible to complete. The
draft code decayed the counter instead and used an RMS threshold of ~1800
(16-bit PCM).

## Decision
The ritual ticks every 100 ms. A loud tick adds 100 ms to the sustain counter;
a quiet tick removes 50 ms (half rate, never below 0). At 2 000 ms the phrase
advances and the counter resets. Leaving the demon's heading still resets all
progress (PRODUCT.md). Loud means RMS > 1800 on 16 kHz mono PCM16. These are
global tuning constants in `RitualConfig`, not demon data (ADR-0006 covers
per-demon parameters).

## Consequences
A steady chant completes a phrase in ~2 s; short pauses cost a little time
rather than all of it. The threshold may need tuning per device class; change
`RitualConfig.VOLUME_RMS_THRESHOLD` and note it here.
