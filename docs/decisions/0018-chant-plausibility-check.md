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
- **Vowel sequence:** classify the vowel (A/E/I/O/U) of each voiced syllable nucleus
  from its formants (LPC), normalised per session, and align it against the expected
  sequence derived from the chant text via `LatinSyllables` (Latin spelling is phonemic;
  AE/OE count as E). Score with a tolerant edit distance.
- **Rhythm:** the phrase arrives in one or two breath groups with roughly
  `LatinSyllables.count` syllables, at a chanting (not conversational) pace.
- A phrase seals only if the combined score clears a **lenient** threshold. Otherwise
  the demon reacts comedically and the phrase stays open.

Rejection compounds across 4 phrases: rejecting unrelated speech 60 % of the time per
phrase lets only ≈ 3 % of nonsense rituals through. Targets: < 5 % false rejects on
real chanting, ≥ 60 % per-phrase rejection of unrelated speech. Thresholds are chosen
from tester clips (correct chants, other-language small talk, humming, mumbling)
captured with a debug-only recorder and replayed as JVM unit-test fixtures. Release
builds never record audio.

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
  measured gains "vowel sounds". No new permission, no network.
- New global tuning in `RitualConfig` (score weights, threshold), not demon data
  (ADR-0006). Record measured values in this ADR.
- PRODUCT.md's Chant row gains the sealing condition once thresholds are measured.
