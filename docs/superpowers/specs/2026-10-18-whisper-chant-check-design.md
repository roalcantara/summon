# On-device Whisper chant check: design

- **Date:** 2026-10-18
- **Status:** Approved in brainstorming; pending implementation plan
- **Evidence:** ADR-0026 (corpus and two real voices)
- **Replaces:** the vowel judge and chunks of ADR-0022; amends ADR-0018, ADR-0024, ADR-0025

## 1. Goal

Seal a chant line when the player has actually said it, using Whisper base with the language set to
Latin, running entirely on the phone. Honest players are never told they were wrong; nonsense never
matches, and the 60 s timer does the rest.

Measured basis (ADR-0026): at a best-stretch letter distance of 0.60, 19 of 20 honest real lines
(two voices) pass and no line of ordinary nonsense does.

## 2. Decisions

| # | Decision |
|---|---|
| D1 | **Listen until found.** After each pause, the audio since the line began is transcribed; the line seals as soon as it appears. There is no rejection state. |
| D2 | **Runtime: whisper.cpp**, built from source in the app module through CMake and a small JNI bridge. |
| D3 | **Model bundled:** `ggml-base-q5_1.bin` (≈ 57 MB) in the app's assets; works offline from the first ritual. |
| D4 | **Lenient fallback** on devices where Whisper fails to load or is too slow: a line seals when all its syllables have lit (the rule before ADR-0018). |
| D5 | **Remove** the vowel aligner, chunks and `ChantTake` (ADR-0022); seals are whole-line again. |

## 3. Architecture

```
mic PCM ──► VoiceAnalyzer ──► RitualEngine (pure)            karaoke lighting, timer, seals
   │                              ▲
   ▼                              │ RitualInput.heard: LineHeard?
LineListener (ViewModel scope) ───┘
   │ PCM since the line began; triggers on pauses
   ▼
Transcriber ──► WhisperTranscriber (JNI → whisper.cpp)
   │
   ▼
LineMatcher (pure Kotlin) → distance ≤ LINE_MATCH_MAX → found
```

### 3.1 Units

| Unit | Location | Responsibility |
|---|---|---|
| whisper.cpp | `app/src/main/cpp/whisper.cpp` (git submodule, pinned release tag) | Inference library |
| `chant_whisper.cpp` | `app/src/main/cpp/` | JNI: `init(modelPath): Long`, `transcribe(handle, pcm: FloatArray, threads: Int): Array<Segment>` (text + no-speech probability), `free(handle)`. Language `la`, no prompt, greedy decoding, temperature 0, `audio_ctx` sized to the input. |
| CMake | `app/src/main/cpp/CMakeLists.txt` | Builds arm64-v8a and armeabi-v7a with 16 KB page alignment (`-Wl,-z,max-page-size=16384`). |
| `Transcriber` | `ritual/` | Interface: `suspend fun transcribe(pcm: FloatArray): List<TranscriptSegment>`; fakes in tests. |
| `WhisperTranscriber` | `platform/` | Copies the model from assets to private storage on first use, loads it once per process, runs one transcription at a time on a background dispatcher. |
| `DeviceCheck` | `platform/` | Once per install (result cached): model load plus one timed 5 s transcription; fails → lenient mode. |
| `LineMatcher` | `ritual/` | Pure: normalisation, loop collapse, best-stretch distance, word coverage, found/not found. |
| `LineListener` | `ritual/` (used by `RitualViewModel`) | Buffers 16 kHz PCM for the current line, decides when to check, runs checks through `Transcriber` and `LineMatcher`, emits `LineHeard` and word coverage. |
| `RitualEngine` | `ritual/` | Seals a line on `LineHeard` (Whisper mode) or on all syllables lit (lenient mode). |

### 3.2 Engine and state changes

- `RitualInput` gains `heard: LineHeard?` (`phrase`, `coveredWords: Set<Int>`, `found: Boolean`).
- `RitualState` loses `chunks`, `chunk`, `take`; gains `mode: CheckMode` (`Whisper` or `Lenient`, fixed per ritual) and `coveredWords: Set<Int>` for the current line.
- Effects: `Judged`, `ChunkPassed` and `Rejected` are removed; `LineChecked(phrase, distance, found)` is added for feedback and logs; `Sealed`, `Cracked`, `Drift`, `Resumed` stay.
- In Whisper mode lighting all syllables does nothing special; lighting continues past the last syllable without wrapping (it caps at the syllable count).
- `ChantVerdict.Kind.Rejected` is removed.

## 4. Checks and matching

### 4.1 When a check runs
- After a pause ≥ `CHECK_PAUSE_MS` (600 ms), when ≥ `CHECK_MIN_VOICED_MS` (800 ms) of new voiced audio arrived since the last check.
- After `CHECK_CONTINUOUS_MS` (6 s) of continuous voicing.
- At most one check runs; triggers during a check coalesce into one follow-up check.
- Window: audio since the line began, capped to the last `CHECK_WINDOW_MS` (25 s).
- A check whose result arrives after the line already changed (sealed, cracked) is discarded.

### 4.2 Matching (`LineMatcher`)
1. Drop transcript segments with no-speech probability > `NO_SPEECH_MAX` (0.6).
2. Collapse loops: a word sequence of 1–6 words repeated three or more times in a row is kept once.
3. Normalise: letters only, upper case, J→I, V→U.
4. Distance: edit distance between the line's letters and the best-matching stretch of the transcript's letters (free start and end), divided by the line's length.
5. Found: distance ≤ `LINE_MATCH_MAX` (0.60), against the **current** line only.
6. Word coverage: a word of the line is covered when ≥ `WORD_COVER_MIN` (60 %) of its letters align to equal letters in the best stretch. Coverage only grows within a line.

### 4.3 Hallucination guards
- No check when the window has < 0.8 s of voiced audio (Whisper invents text over short noise).
- No-speech filter and loop collapse (4.2 steps 1–2).
- Best-stretch scoring ignores surrounding chatter.

## 5. Ritual rules and screen

- **Seal:** a line seals when found (chime, flare, seal fills); next line starts with a full timer.
- **Timer:** 60 s per line (+25 % in court), drains while the line is open and 3× while wavering; refilled on seal; empty → the last seal cracks (ADR-0010, ADR-0025).
- **Drift hysteresis:** locking needs ±20° (unchanged); once locked, the ritual wavers beyond **25°** and resumes within **20°** (`DRIFT_ENTER_DEG`, `DRIFT_EXIT_DEG`).
- **Seals:** whole-line; part arcs removed. The current seal breathes faster while a check runs.
- **Words:** lit syllables as karaoke; a green line under each covered word; no red or wavy lines, no ✕.
- **Verdict line:** voice meter; "SEAL CRACKED ✕" after a crack.
- **Intro card rules:** "1. Face — Turn until the frame turns green." · "2. Chant — Read the line aloud until it seals." · "Lines — Green: heard." · "Focus — Each line gets a minute; the ring shows what is left."
- **Lenient mode:** identical screen; lines seal when all syllables light; word lines turn green as their syllables light.
- **Debug panel:** last transcript, distance, check duration, mode (and the lenient reason).

## 6. Fallback (`DeviceCheck`)

Runs once per install, in the background before the first ritual, and caches the result:
- model copy and load fail → lenient (reason `load`);
- a 5 s transcription takes more than `DEVICE_MAX_CHECK_MS` (4 s) → lenient (reason `slow`);
- 32-bit-only devices may use armeabi-v7a; they fall under the same timing rule.
A ritual started before the check finishes uses lenient mode. The mode is fixed per ritual and logged.

## 7. Logs, replay and threshold re-check

- Session log (debug) gains one record per check: phrase, window ms, voiced ms, raw transcript, normalised letters, distance, found, check ms, mode.
- `SessionReplayTest` feeds logged `LineHeard` inputs back into the engine, so replays stay exact.
- A harness test re-scores every logged transcript at a range of `LINE_MATCH_MAX` values and prints, per voice tag, how many honest lines would pass and how many would match another line of either demon.
- A desktop re-run of `tools/experiments/whisper_real_lines.py` with greedy decoding (beam 1) confirms the threshold before device tuning.

## 8. Privacy, licences, docs

- Release builds recognise speech on the device only to check the line; transcripts are discarded at once; nothing is stored or sent. Debug logs keep transcripts in private storage, excluded from backups.
- `docs/PRIVACY_POLICY.md` and `docs/STORE_LISTING.md`: replace "no words are recognised" with "speech is recognised on your device to check the chant; it is never stored or sent"; Play Data safety stays "no data collected".
- Licences: whisper.cpp (MIT) and the Whisper weights (MIT) notices ship in `app/src/main/assets/licenses/`.
- ADR-0026 → Accepted; new ADR-0027 records this design (amends ADR-0018, ADR-0022, ADR-0024, ADR-0025). PRODUCT.md (Chant, Focus, Chant screen rows) and DESIGN.md (seals without parts, word lines) are updated.

## 9. Size and performance

- App size: ≈ +57 MB model, ≈ 2–3 MB native code per ABI.
- Memory: ≈ 200 MB while a check runs; the model stays loaded for the process.
- Target: a check of ≤ 10 s audio in ≤ 1.5 s on a Galaxy S24 (SM-S921B). The first implementation task is a device spike measuring load time, check latency and memory there and on the emulator.

## 10. Testing

- `LineMatcherTest`: the real transcripts of ADR-0026 (honest lines found; "Holamio Miku" and other-demon lines not found), normalisation, loop collapse, word coverage.
- `LineListenerTest` with a fake `Transcriber`: pause trigger, 6 s trigger, coalescing, < 0.8 s guard, stale results discarded.
- Engine tests: seal on `LineHeard`; no seal on all-lit in Whisper mode; seal on all-lit in lenient mode; drift hysteresis 25°/20°; timer refill on seal.
- Session log round trip and exact replay with check records.
- Device checklist with both voices.

## 11. Out of scope

- A time-based "say it again" hint (revisit if logs show players getting stuck).
- Downloading the model or Play asset packs.
- Other model sizes (tiny, small).
- iOS.
