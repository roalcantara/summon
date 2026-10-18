# 0018 — Chant plausibility check (vowels + rhythm)

- Date: 2026-10-18
- Status: Accepted (direction); thresholds pending measurement. Amends ADR-0008.

## Context
Play-testing with real people showed that any voiced speech completes a phrase: one
tester summoned a demon by repeating "Olá meu amigo, como está hoje?". Once players
learn the words don't matter, the ritual becomes a joke. The goal is **not** to validate
each word, but to reject input that clearly doesn't match the phrase while staying
lenient with honest chanters (accents, children, cheap mics).

Constraints: no network access and no word recognition (PRODUCT.md, ADR-0017,
docs/PRIVACY_POLICY.md); Latin has no off-the-shelf speech models; the mic delivers
16 kHz mono PCM on most devices (`AndroidPcmSource`).

## Decision
Add an on-device **plausibility score** per phrase, built on the existing
`VoiceAnalyzer` frames, with no model and no new dependency:
- **Vowel sequence:** each voiced frame gets F1/F2 by linear prediction (LPC order 10 on
  the mic decimated to ~8 kHz). A syllable's vowel colour is the median F1/F2 of its
  frames, from the frame that lit it until the next syllable lights. The expected vowel
  per syllable comes from the chant text via `LatinSyllables` (Latin spelling is
  phonemic; AE/OE/EI/EU count as E, AU as O). Heard and expected sequences are aligned
  with a tolerant edit distance: a substitution costs the log-formant distance to the
  expected vowel's reference formants (capped at 1), a skipped syllable 0.7, an
  unmeasured one 0.5. Reference formants are scaled by the best of six vocal-tract
  factors (0.9–1.45) per phrase, so men, women and children are judged alike.
- **Rhythm:** at most 1 + syllables / 6 breaths (silences ≥ 250 ms) per phrase, −0.25
  per extra one; halved when syllables came under 170 ms apart on average (talk, not
  chanting).
- **Verdict:** total = 0.8 × vowels + 0.2 × rhythm. When the last syllable lights, its
  vowel is still heard until voicing stops or 300 ms pass, then the phrase is judged. A
  total ≥ 0.6 seals it (`Judged` then `Sealed`). Below that the demon scoffs (`Judged`
  then `Rejected`: a rotating cue line, the warning haptic, a red flash of that phrase's
  sigil strokes) and the phrase reopens with no syllable lit.
- A rejection is **not a crack**: flawless is kept and the focus clock just keeps
  draining, so on phrases II–IV repeated nonsense ends in a crack anyway.
- The check is on only when the ritual starts with `judged = true` (the app always
  does; most engine tests don't). Debug "Hold to Chant" sounds the expected vowels.
- All numbers are global tuning in `RitualConfig` (ADR-0006), starting from the
  simulated defaults below until tester clips calibrate them.

Rejection compounds across 4 phrases: rejecting unrelated speech 60 % of the time per
phrase lets only ≈ 3 % of nonsense rituals through. Targets: < 5 % false rejects on
real chanting, ≥ 60 % per-phrase rejection of unrelated speech.

**Starting values (simulation, 2026-10-18).** Synthetic syllables with log-normal
formant noise (σ 0.08–0.22), 8 % unmeasured and 8 % garbage nuclei, random missed or
doubled syllables. At a 0.6 pass score honest chants passed a phrase 90–99.8 % of the
time and Portuguese-like vowel sequences 3–12 %. Synthetic voices (male, female, 44.1
kHz) chanting the right vowels seal; repeating "Olá meu amigo, como está hoje?" and
holding one vowel do not.

**Calibration.** Debug builds have a clip recorder (WAV files in the app's private
storage, excluded from backups) and a JVM harness replays the clips through the real
pipeline and prints pass rates per threshold. Clips are people's voices: recorded only
with consent, never committed (git-ignored). Release builds get a no-op recorder from
their own source set, so they contain no recording code. Measured values replace the
starting values here and in `RitualConfig`.

## Alternatives
- **sherpa-onnx keyword spotting — deferred.** Offline, Apache-2.0, streaming, with a
  Kotlin API; could spot one anchor word per phrase. Deferred because its small KWS
  model is English-only (Latin fit unproven; non-English keywords need a re-trained
  model), it adds ~10–20 MB of native code and model per ABI, and it needs a 16 KB
  page-size check and 44.1 kHz resampling. Revisit if the vowel + rhythm check misses
  its targets: first run the desktop `sherpa-onnx` Python package with Latin keywords
  over the same tester clips before any Android work.
- **Vosk with a phrase grammar, whisper.cpp:** offline but 40–75 MB and more than this
  goal needs. Rejected.
- **Android `SpeechRecognizer`:** no Latin locale; on-device only from API 31 (minSdk is
  24); the online path sends audio to Google. Rejected.
- **Picovoice:** free tier ended 2026-06-30. Rejected.
- **Jev (TypeSafe AI) via API:** text-only input, so it cannot judge audio without a
  transcript the app doesn't have; comparing vowel strings is a deterministic edit
  distance that needs no model. It would also add network access, an API key in the
  APK and a third-party processor to the privacy policy. Rejected for this use.

## Consequences
- "No speech or words are recognised" stays true; the privacy policy's list of what is
  measured gains "vowel sounds", and the store listing says the app checks that vowels
  and rhythm sound like the chant. No new permission, no network, Data safety stays
  "no data collected". These texts change in the same release as the code, because they
  must describe the shipped app.
- Known limits, accepted: a phrase dominated by one vowel (phrase I of Asmodeus is half
  E) can be passed by holding that vowel, which is close enough to chanting; phrase I
  has no clock, so endless nonsense eventually passes by chance (≈ 1 try in 10–30);
  voices above ~280 Hz are measured less reliably, which the leniency absorbs.
- Sealing is no longer instant on the last syllable (ADR-0008): it lands when the last
  vowel ends, at most 300 ms later.
- Plan: `docs/superpowers/plans/2026-10-18-chant-check.md`.
