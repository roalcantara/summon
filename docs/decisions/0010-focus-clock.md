# 0010 — Focus clock

- Date: 2026-10-16
- Status: Accepted (supersedes the drift grace of ADR-0008)

## Context
Ritual 2.0 (ADR-0008) had no real failure mode: silence paused for free, and drifting
off the heading for 1.5 s threw the player back to Seeking and wiped the current
phrase. Play-testing the web prototype showed that a draining clock gives tension
without ever wiping a whole attempt.

## Decision
- **Budget:** phrases II–IV (index *i* ≥ 1) each get
  `round((3 000 + 500 × syllables[i]) × courtMultiplier)` ms of focus. Phrase I has
  no clock (budget 0).
- **Seal:** lighting every syllable of a phrase seals it (`Sealed(phrase)`); the next
  phrase starts with a full budget. The 4th seal starts the climax and reports
  `ClimaxStart(flawless)`.
- **Drain:** every 20 ms tick while Chanting or Wavering on phrases II–IV: 1 × 20 ms on
  heading (**also in silence**), 3 × 20 ms off heading.
- **Crack:** when focus reaches 0 the last sealed phrase reopens (`Cracked(phrase)`,
  `phrase -= 1`, `lit = 0`) with its own full budget (0 on phrase I), and the attempt
  is no longer flawless.
- **No return to Seeking:** locking happens once per attempt. Leaving the tolerance
  (or an unknown heading) while chanting enters Wavering (`Drift`): voice is ignored and
  the clock drains 3×. Facing the demon again resumes chanting (`Resumed`) with nothing
  lost and no re-lock. On phrase I, Wavering simply waits.
- Effects: `PhraseComplete` → `Sealed`, `DriftWarning` → `Drift`; `PhraseLost` is removed.
- Tuning lives in `RitualConfig`: `FOCUS_BASE_MS = 3 000`, `FOCUS_PER_SYLLABLE_MS = 500`,
  `FOCUS_DRIFT_MULTIPLIER = 3`, `COURT_BONUS = 0.25`, `FOCUS_LOW_FRACTION = 0.25`.
  `DRIFT_GRACE_MS` is removed.

## Consequences
Supersedes ADR-0008's grace period and its "silence pauses without penalty" for
phrases II–IV. The worst case is losing one sealed phrase at a time. The clock pauses
while the Ritual screen is stopped, and after a restart ticks are held until the first
heading reading arrives, so a return from the background never counts as drifting.
