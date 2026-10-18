# Chant check v2: design

- **Date:** 2026-10-18
- **Status:** Approved in brainstorming; pending implementation plan
- **Amends:** ADR-0018 (chant plausibility check), ADR-0010 and ADR-0019 (focus clock), ADR-0019 and ADR-0021 (chant screen status and intro card)
- **Prototype:** Chant Screen Prototype v12.1, https://claude.ai/artifact/9rCV9zSJdqY5mJscEn5EiQ

## 1. Problem

The chant check (ADR-0018) stops nonsense, but it also rejects honest players on long or complex phrases. Replaying the two existing tester clips through the current judge:

| Clip | Phrase | Vowels | Rhythm | Total | Verdict |
|---|---|---|---|---|---|
| honest chant | I | 0.52 | 1.00 | 0.62 | pass (barely) |
| honest chant | II | 0.50 | 0.50 | 0.50 | fail |
| nonsense ×5 | I | 0.40–0.53 | 1.00 | 0.52–0.63 | 4 fail, 1 pass |

The vowel score barely separates honest chanting from nonsense, so no threshold can fix it. Causes:

1. **Scoring follows lit syllables, not spoken ones.** A syllable lights on a loudness onset or after 350 ms of voicing (`RitualEngine`). Voiced consonants between vowels (DAE**M**O**N**IUM, O**M**NIA, SU**M** VOBIS) produce no onset, so two vowels merge into one nucleus whose median formant matches neither; plosives can add extra ones. On long phrases the lighting drifts away from the speech and every later nucleus mixes the wrong frames.
2. **The phrase closes only when the lit count reaches the syllable count**, so a player who fell behind keeps adding sounds that are scored against the last vowels.
3. **Rhythm penalises careful readers:** every silence ≥ 250 ms beyond 1 + syllables/6 costs 0.25, and fast talk halves the score; phrase II failed on rhythm (0.50).
4. **Real pronunciation differs from spelling:** glides (-NIUM said "nyum"), reduced unstressed vowels, nasal endings; each mismatch costs a 0.7 skip.

## 2. Goals and decisions

| # | Decision |
|---|---|
| G1 | **Prefer letting honest players through.** Target ≤ 2 % honest rejects per chunk; accept that ~30–40 % of nonsense chunks pass (about 1–2 % of full nonsense rituals, since every chunk of every line must pass). |
| G2 | Judge by **frame-level forced alignment** against the expected vowels, with a **relative verdict** against shuffled vowel orders. Lighting drives only the karaoke display. |
| G3 | **Long lines are judged in chunks**; a rejection reopens only that chunk. |
| G4 | The **focus clock becomes a focus meter**: no drain while chanting or pausing briefly. |
| G5 | The left panel's status becomes a **two-row Compass-and-Tally block**; the rules move to the intro ("How to chant") card; the verdict line keeps only what needs action. |
| G6 | No pace guide in this pass. |
| G7 | Calibrate with **two voices** now (more later if the harness says the data is insufficient). |
| G8 | Unchanged: no speech recognition, no model, no network, no new permission. The check still measures "vowel sounds and rhythm". |

## 3. Measurement first

### 3.1 Clip protocol
Each of the two voices records, with the debug build's clip recorder:
- 3 honest takes of every phrase of both demons (24 per voice);
- 8 nonsense takes (normal speech in their own language, humming, counting).

File names extend the existing scheme with a voice tag: `{demonId}-{label}-{voice}-{stamp}.wav`, where `label` is `chant` or `nonsense`. Clips stay git-ignored and are recorded only with consent (ADR-0018). The harness also accepts the old two-part names, with the voice reported as `unknown`.

### 3.2 Harness diagnostics (`ChantClipReplayTest`, test code only)
For each judged chunk it prints:
- frames used and skipped;
- the aligned path: expected vowel → frame span → cost;
- the real cost, the median shuffle cost, the shuffle rank and the margin;
- the verdict from the new judge, next to the old judge's score for the same audio.

A summary per voice and per phrase gives honest pass rates and nonsense reject rates. An **"enough data?"** line reports *insufficient* when any phrase has fewer than 4 honest takes or only one voice passes it.

### 3.3 Gate
Tuning values (§4) are set from this report. The release waits until **both voices are at ≤ 2 % honest rejects per chunk on all 8 phrases**. The nonsense reject rate is reported, not gated.

## 4. The judge: `ChantAligner`

Replaces `ChantJudge`'s nucleus scoring. A pure object in `ritual/`, unit-tested on the JVM.

### 4.1 Input
Every 20 ms frame of the chunk, from its first voiced frame until voicing ends after its last lit syllable (the existing ≤ 300 ms closing rule): an F1/F2 pair, or null when unvoiced or without stable formants. `ChantTake` keeps this frame list for the current chunk instead of per-syllable medians.

### 4.2 Expected template
Built from the chunk's text with `LatinSyllables`: one **slot** per syllable vowel, each with
- an **accepted set**: the Latin vowel (AE/OE/EI/EU → E, AU → O, as today), plus a neutral vowel **"uh"** (reference F1 500 Hz, F2 1500 Hz) when the slot is the unstressed final syllable of a word of two or more syllables;
- an **optional** flag for glides (I or U directly before another vowel in the same word, e.g. DAEMON**I**UM, GRAT**I**A) and for the second vowel of a diphthong.

### 4.3 Alignment
Dynamic time warping over frames × slots, evaluated at each of the existing six vocal-tract scales (`VOWEL_SCALES`), keeping the best:
- a frame either **joins** the current slot (cost = log-formant distance to the slot's nearest accepted vowel, capped at 1, as in `ChantJudge.distance`) or is **skipped** as a consonant or transition (`ALIGN_FRAME_SKIP_COST`);
- a slot counts as heard only with at least `ALIGN_MIN_SLOT_FRAMES` (3 frames = 60 ms); otherwise it is skipped at `CHANT_GAP_COST` (0.7), or `ALIGN_OPTIONAL_SKIP_COST` (0.15) if optional;
- **chunk cost** = mean over slots of each slot's mean frame cost (so a long-held vowel does not dominate) + `ALIGN_SKIPPED_WEIGHT` × share of frames skipped.

Cost: about 20 slots × 400 frames × 6 scales × 25 runs per chunk, once per chunk.

### 4.4 Relative verdict
- Generate **24 shuffles** of the chunk's slot vowels, using a PRNG seeded from the chunk text (deterministic and testable). Discard shuffles that differ from the real order in fewer than 30 % of slots.
- Align the same frames to each surviving shuffle.
- **Pass** when the real cost beats at least `BASELINE_PASS_RANK` (starting at 70 %) of the shuffles **and** stays under `ALIGN_MAX_COST` (starting at 0.8).
- **Low-diversity fallback:** with fewer than 8 surviving shuffles (a chunk dominated by one vowel), only the `ALIGN_MAX_COST` ceiling applies.

### 4.5 Rhythm
Rhythm leaves the score and becomes a **veto for obvious talk**: a chunk is rejected only when the median duration of its heard slots is under `TALK_MAX_SLOT_MS` (70 ms). Pauses at word boundaries never count. `CHANT_PAUSE_PENALTY`, `CHANT_SYLLABLES_PER_PAUSE`, `CHANT_FAST_INTERVAL_MS` and `CHANT_RHYTHM_WEIGHT` are removed.

### 4.6 Word hints
ADR-0019's heard/doubtful marks come from the alignment path: a word is doubtful when the mean cost of its slots is ≥ `WORD_DOUBT_DISTANCE`. Still only a hint, never the verdict.

All starting values live in `RitualConfig` and are replaced by measured ones (§3.3).

## 5. Chunks

- A phrase of ≤ 8 syllables is one chunk. A longer one splits at word boundaries into chunks of at most 8 syllables, as even as possible, never leaving a chunk under 3 syllables. Chunks are computed from the text (demon data stays data, ADR-0006).
- Resulting splits, pinned by unit tests:

| Demon | Phrase | Chunks (syllables) |
|---|---|---|
| Asmodeus | I | DAEMONIUM RESIDENTE (8) |
| Asmodeus | II | OSTENDE MIHI FACIEM (8) · TUAM POTENTIAM (6) · VENIT AD ME (4) |
| Asmodeus | III | DONA MIHI PER FOEDUS (7) · QUOD EST IRE AD PRAESENS (7) · SUM VOBIS (3) |
| Asmodeus | IV | UT VOTA OMNIA (6) · OPUS EST (3) |
| Astaroth | I–IV | one chunk each (6–8) |

- **Flow:** a chunk is judged when its last syllable lights and the vowel ends. The player does not need to stop between chunks.
  - **Pass:** the chunk's word lines turn green, a light tick haptic plays and the current seal's ring gains a segment. When the last chunk passes, the phrase **seals** as today (chime, sigil flare).
  - **Reject:** "CHANT AGAIN ✕", the red wavy lines on that chunk's doubtful words, the warning haptic, and −25 % focus. The chunk reopens at its first word; earlier chunks stay passed.
- Sigil ink keeps today's formula; a rejection rolls the lit count back to the chunk's start.

## 6. Focus meter (replaces the focus clock)

| Rule | Value |
|---|---|
| Lines with a meter | II–IV (line I has none, as today) |
| Budget | flat 8 s of lost focus per line, full at the start of each line and refilled on each seal |
| While voicing, or a pause under 2 s | no drain |
| Silence beyond 2 s | drains 1× |
| Facing away (wavering) | drains 3× |
| Rejected chunk | −25 % at once |
| Empty | **crack**, as today: the last seal reopens and the summon is no longer flawless |
| Verdict hold (ADR-0019) | unchanged: up to 3 s after a verdict, or until the next syllable |

The ring around the sigil shows the meter; the seconds label and the "PAUSED" label are removed. `FOCUS_BASE_MS` and `FOCUS_PER_SYLLABLE_MS` are replaced by `FOCUS_BUDGET_MS` (8 000), `FOCUS_IDLE_GRACE_MS` (2 000) and `FOCUS_REJECT_COST` (0.25); `FOCUS_DRIFT_MULTIPLIER` stays.

## 7. Chant screen changes

### 7.1 Status block (left panel, under the sigil)
One framed block with two rows, replacing the direction line:

| Row | Content |
|---|---|
| 1 | "1. FACE EAST" (the demon's direction) and, at the right end, → / ← while turning or ✓ when aligned |
| 2 | "2. CHANT" and four round seals I–IV |

- **Both labels** use one fixed style and colour: Inter ExtraBold, upper case, letter-spacing 0.12 em, `outline` colour. Each label stays on one line.
- **Only the numbers, the arrow and the frame carry state:**

| State | Frame | "1." and arrow | "2." |
|---|---|---|---|
| Turning, before the lock | `outline-variant` | yellow (`primary`), arrow | dim (`outline-variant`) |
| Aligned and chanting | `ritual-safe` | green ✓ | yellow (`primary`) |
| Drifting after the lock | error red; row 1 blinks | red, arrow back | dim |
| All four sealed | demon accent with glow | green ✓ | green |

- **Seals:** a numeral disc inside a ring split into one arc per chunk.
  - pending: dim outline;
  - current: accent outline, slow breathing; arcs light as chunks pass;
  - sealed: filled accent with glow, flare on sealing;
  - cracked: red shake and fade on the seal that reopens;
  - drifting: all seals dimmed, the current one outlined in red;
  - all sealed: all four flare.
- The seal row is the only progress display besides the sigil's ink. "I of IV" is removed everywhere.

### 7.2 Verdict line (right panel, under the words)
Keeps only the voice meter and the messages that need action: **"CHANT AGAIN ✕"** after a rejected chunk and **"SEAL CRACKED ✕"** after a crack. "CHANT ALOUD", "PAUSED" and "SEALED ✓" are removed: the seals and the frame show them.

### 7.3 Intro card ("How to chant")
Carries the rules, so the chant screen needs no explanatory text:

1. **1. Face:** Turn until the frame turns green; the arrow shows which way. Turning away pauses the chant.
2. **2. Chant:** Read each line aloud. A finished line lights its seal. Long lines are checked in parts, shown on the ring around its seal.
3. **Lines:** Green under a word: heard. Red: say that part again.
4. **Focus:** The ring around the sigil is your focus. Long silences, turning away and parts you must repeat drain it. If it empties, your last sealed line cracks.

### 7.4 Debug panel
"Last score" shows: chunk n/k, chunk cost, shuffle rank, verdict.

## 8. Engine changes

- `ChantTake`: holds the current chunk's frames (F1/F2 or null per frame) plus the existing timing fields needed for closing.
- `ChantAligner` (new): template, alignment, shuffle baseline, talk veto, word marks.
- `ChantJudge`: keeps `expectedVowels` and `distance` as shared helpers; nucleus scoring is removed from the app and kept only in test code for the harness's side-by-side comparison.
- `RitualState`: gains `chunk`. Effects: new `ChunkPassed(phrase, chunk)`; `Rejected` gains `chunk`; `Judged` carries the aligner's result.
- Focus meter rules in `RitualEngine` (§6).

## 9. Testing

- **`ChantAligner`**, using `SyntheticVoice` frames:
  - pass: honest takes with merged syllables, split syllables, short glides, "uh" endings, slow takes with word pauses; male, female and child vocal-tract scales;
  - fail: Portuguese-like vowel sequences, counting, one vowel held throughout;
  - shuffles are deterministic for a given text; the low-diversity fallback applies below 8 shuffles; the talk veto fires when every heard slot is at the 60 ms minimum, and not at 100 ms.
- **Chunking:** the table in §5, exactly.
- **Engine:** a rejection reopens only its chunk; the phrase seals after its last chunk; focus does not drain while voicing or in a pause under 2 s, drains after 2 s idle, 3× while drifting, −25 % per rejection, cracks at 0, refills on seal.
- **Clip harness:** §3.2 and the gate in §3.3.

## 10. Docs

- **ADR-0022** Chant check v2: alignment, shuffle baseline, leniency, talk veto. Amends ADR-0018.
- **ADR-0023** Focus meter. Amends ADR-0010 and ADR-0019.
- **ADR-0024** Status column, intro-card rules, reduced verdict line. Amends ADR-0019 and ADR-0021; links prototype v12.1.
- **PRODUCT.md:** the Chant, Focus clock (renamed Focus meter), Sigil and Chant screen rows.
- **DESIGN.md:** a contract for the status block and its seals; the `ritualCue` role notes the step-label style.
- **Privacy policy and store listing:** re-read in the same release; they must still say that only vowel sounds and rhythm are measured and nothing is recognised.
- Commit `docs/stitch-prompts/2026-10-18-ritual-status-block.md`, the prompt that produced the chosen layout.

## 11. Rollout

One implementation plan in four phases:
1. Harness diagnostics; record clips from both voices. No app change.
2. `ChantAligner` and the shuffle baseline replace the nucleus judge; the harness compares old and new. Tune §4 values from the report.
3. Chunks and the focus meter in the engine.
4. UI: status block, intro-card rules, verdict line, debug score line.

The release waits for the gate in §3.3.

## 12. Out of scope

- Pace guide (visual cursor or listen demo).
- Keyword spotting (sherpa-onnx, ADR-0018 alternatives); revisit only if this design misses the gate.
- More than two calibration voices, unless the harness reports the data as insufficient.
- The "LOVIS" question (ADR-0005).
