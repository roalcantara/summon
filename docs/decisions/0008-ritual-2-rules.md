# 0008 — Ritual 2.0 rules

- Date: 2026-10-15
- Status: Accepted (supersedes ADR-0007); the grace period and "silence pauses" are superseded by ADR-0010

## Context
Play-testing the first port showed that the ritual felt arbitrary. Any noise above an
RMS threshold for 2 s completed a phrase, regardless of what was said. Drifting off
the heading silently wiped all progress, and nothing on screen reacted to the voice.

## Decision
The ritual is a pure state machine ticked every 20 ms (`RitualConfig.FRAME_MS`):
Seeking → Locking → Chanting ⇄ Wavering → Climax → Done.
- **Lock:** staying within the demon's heading tolerance (±20°) for 600 ms locks
  the ritual. Leaving earlier returns to Seeking.
- **Syllables:** each phrase needs as many lit syllables as `LatinSyllables.count`
  gives (diphthongs AE/OE/AU/EI/EU are one nucleus; "QU" is a consonant). A syllable
  lights on a voiced onset at least 120 ms after the previous lit syllable, or after
  350 ms of continuous voicing. Unvoiced frames pause; nothing decays.
- **Voiced:** RMS > 1 200 (PCM16) and a normalized autocorrelation peak ≥ 0.5 for
  70–400 Hz over the last 40 ms. **Onset:** a voiced frame ≥ 1.8 × the minimum RMS of
  the previous 100 ms while the detector is armed; it re-arms after an unvoiced frame
  or a dip below 0.6 × the peak since the last onset.
- **Grace:** leaving tolerance while chanting enters Wavering. Returning within 1.5 s
  loses nothing; otherwise only the current phrase's lit syllables are lost and the
  player must lock again.
- **Climax:** after phrase 4, input is ignored for 2.5 s, then the demon is summoned.
All numbers are global tuning in `RitualConfig`, not demon data (ADR-0006).

## Consequences
Claps, taps and wind no longer count, and slurred or held chanting still progresses
(slower). Word recognition stays a non-goal. Thresholds may need per-device tuning;
change `RitualConfig` and note it here.
