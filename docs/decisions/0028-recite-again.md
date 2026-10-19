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
- Past the last syllable, the line starts over only after a pause of at least 1 s
  (`RECITE_AGAIN_PAUSE_MS`). Play-tests showed the karaoke counting extra onsets and restarting
  0.14–0.5 s after the previous syllable, clearing the words mid-reading and dropping the audio
  of the player's real attempt.
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

Known MVP limit: a line can seal from its first half (honest start, nonsense ending). An
"ending must be heard" rule was tested on the 2026-10-18 sessions and rejected: Whisper often
garbles the last word of complete honest readings ("Gratia, sit, sit, kura").
