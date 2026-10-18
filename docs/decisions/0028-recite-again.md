# 0028 — Reciting a line again

- Date: 2026-10-18
- Status: Accepted. Amends ADR-0027.

## Context
Play-testing ADR-0027 with nonsense on Astaroth's line I: Whisper rejected every attempt (distances
0.64–0.86), but once every syllable had lit the karaoke stopped, nothing on screen changed after a
miss, and line I has no timer. The player could neither see that the attempt failed nor recite the
line again, so the ritual looked frozen.

## Decision
- In Whisper mode a line can always be recited again: the next syllable after the last one
  starts the line over (words clear and light again), with a light tick.
- A check that misses after the whole line was recited clears the line the same way.
- Either way the listener drops the audio heard so far, so the next attempt is judged on its own;
  a check already running still reports, so a late hit still seals.
- Only checks within 0.75 of the line turn words green; babble never does.
- The listening window is 12 s (was 25 s), keeping checks around a second.
- Debug builds: the chant screen's debug panel forces the next ritual's check mode
  (auto → Whisper → lenient); the session log header always records the mode.

## Consequences
No new text and no penalty: retrying is visible because the words clear and light again. Line I
still has no timer.
