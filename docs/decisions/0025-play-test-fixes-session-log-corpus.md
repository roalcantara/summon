# 0025 — Play-test fixes, session log and calibration corpus

- Date: 2026-10-18
- Status: Accepted. Replaces the focus meter of ADR-0023; amends ADR-0022 (tuning) and ADR-0024 (intro card copy). Amended by ADR-0027 (judge, copy).

## Context
The first device build of chant check v2 (ADR-0022–0024) was hard to play: the intro card's
long rules pushed **Summon** off screen, the turn arrow flipped right after the lock, syllable
dots made the words hard to read, honest chanting was often rejected, and the 8 s focus meter
made progress almost impossible. The judge had never been tuned on real voices: ADR-0022's
"calibrated" values were the starting guesses from synthetic vowels.

## Decision
- **Timer:** each part of lines II–IV has 60 s (+25 % in court). It drains as before (3× while
  facing away), **refills whenever a part passes**, and a rejection costs nothing. Empty → the
  last seal cracks, as before.
- **Heading:** after the display rotation changes, the last heading is held for 1.5 s
  (`RotationGrace`), so turning the phone to landscape at the lock can't flip the arrow.
- **Words** are shown whole, without syllable dots; lit syllables still colour inside the word.
- **Intro rules** are one short line each, so **Summon** stays on screen.
- **Session log (debug builds only):** every ritual is written as JSON lines to the app's
  private `files/sessions/` (excluded from backups): a header, then one line per 20 ms tick with
  the heading, the analysed voice frame (level, voicing, onset, F1/F2 — no audio), the state, the
  effects and each verdict with its aligned path, plus notes (intro closed, clip toggled, simulate
  heading). `tools/pull_sessions.sh` pulls logs and clips; `SessionReplayTest` replays them
  through today's engine and prints each verdict as it was on the phone and as it would be now.
  Release builds get a no-op log from their own source set.
- **Calibration corpus:** `tools/gen_chant_corpus.py` renders honest rituals with 8 Piper TTS
  voices (it, es, pt, en, de, fr; several speeds, word-by-word "careful" takes) and nonsense
  (the same voices reading ordinary sentences, counting, "la la la", plus mini LibriSpeech
  speakers), each with a random vocal-tract shift (down to child-like), reverb, noise and a
  phone band-pass. The clips go to the git-ignored `app/src/test/chant-clips/generated/`.
  Tuning uses this corpus; **real logged sessions confirm it**, because TTS reads too cleanly
  to reproduce hesitations and accents.

## Consequences
No more time pressure while reading; repeated nonsense no longer cracks seals by itself (the
chant check still rejects it). Play-test reports become log files instead of descriptions.

## Finding (2026-10-18, generated corpus)
On 271 judged chunks (8 TTS voices with augmentation, plus TTS nonsense), the vowel-alignment
judge of ADR-0022 passes honest chunks and nonsense at about the same rate (≈ 0.36–0.45 each)
with the starting values, without the talk veto, and with per-speaker (Lobanov) vowel
normalisation. Mean alignment cost is 0.46 for honest chants and 0.50 for nonsense; mean rank
against shuffles 0.61 and 0.55. Formants are in plausible ranges and only 3 % of frames are
unmeasured, so the measurement works but carries too little information: vowel colour from a
phone microphone cannot tell the right Latin from other speech. Tuning thresholds cannot fix
this. Options: drop the vowel check, or replace it with a phone recogniser (see the decision
that follows this ADR).
