# Chant screen UX: design

- **Date:** 2026-10-18
- **Status:** Approved in conversation; pending implementation plan
- **Evidence:** play-tests after ADR-0028; Stitch rounds 1–2 (`docs/stitch-prompts/chant-screen/`, proposals 01, 02, 03, 08)
- **Amends:** ADR-0019 (chant screen), ADR-0020 (direction in the sigil), ADR-0024 (status column)

## 1. Goal

Make the chant screen teach itself: face first, then chant; see how close you are; see how well
each line went and what the phone heard; retry without being told; see that turning away paused
the chant. Keep the phrase as large as today.

Two arrangements of the same pieces, **split** (today's columns) and **centred** (Stitch 02), sit
behind a debug switch. The session log records which one was on and how large the phrase
actually got. After a device play-test of both, one is kept and the other is deleted (§8).

## 2. Decisions

| # | Decision |
|---|---|
| D1 | **One place per fact.** Direction, lock, pause and progress each appear once; no fact is repeated in words elsewhere. |
| D2 | **Shared pieces, two arrangements.** The pieces in §3 are drawn by the same composables in both arrangements; only their placement differs (§4). |
| D3 | **Live debug switch** (`ChantLayout`: Split, Centred), stored in debug prefs, applied immediately, logged. Release builds use Split until §8 decides. |
| D4 | **Score only when an attempt ends:** on a seal, or on a miss after the whole line was lit. Mid-line checks show nothing but "checking". |
| D5 | **Paused is amber, not red.** Turning away is "wait", not an error (ADR-0020 used red). |
| D6 | **Feedback next to the line:** one fixed-height slot directly above the phrase holds the voice meter, "checking", the score, or "seal cracked". |
| D7 | **No new art.** The House stage backdrop stays (it is already 08's look); the sigil gets 08's glow tile and tilt. |

## 3. The shared pieces

### 3.1 Corner buttons
- **? top-left** (and the dashed **D** next to it in debug builds), **✕ alone top-right**. Same
  `RoundButton` and debug popup as today; only their position changes.

### 3.2 Title: name and seals
- The demon's name (Cinzel, accent) with the four seal marks I–IV beside it (pending, current,
  sealed ✓, cracked). The current mark breathes faster while a check runs (as today).
- This replaces the "2. CHANT" row of `RitualStatusBlock`.

### 3.3 Direction pill (the only place that says the direction)
| State | Pill |
|---|---|
| Seeking | `FACE NORTH →` (or `←`), outline frame |
| Locking / chanting | `FACE NORTH ✓`, green frame |
| Wavering (paused) | `⏸ PAUSED ←` (or `→`), amber frame |
| Done | hidden |

- Arrow from `turnDirection` (existing). This replaces the "1. FACE" row of `RitualStatusBlock`;
  `RitualStatusBlock` is removed.

### 3.4 Sigil (direction, proximity, lock, clock)
Inside a rounded dark tile with a soft accent glow (08):
- **Tilt:** the tile and sigil rotate by the off-course angle, clamped to ±35°
  (`SIGIL_TILT_MAX_DEG`), easing back to upright as the player turns; upright = facing. While
  wavering the tilt returns, in amber.
- **Proximity arc (seeking):** the ring draws an arc from the top (where the phone points) to the
  existing target marker; its length is the off-course angle, so it shrinks as the player gets
  closer. Colour outline → accent within 45°.
- **Lock:** the ring fills green over the 0.6 s hold (existing).
- **Clock:** lines II–IV, the ring drains as the focus clock (existing); amber while wavering.
- Ink progress, flash and mic glow are unchanged.
- Reduce motion: tilt and arc jump to their values without easing.

### 3.5 Feedback slot (directly above the phrase, fixed height)
Exactly one of, newest wins:

| Content | When | How long |
|---|---|---|
| Voice meter (existing 5 bars) | chanting, idle | while nothing else shows |
| `CHECKING…` with the meter pulsing | a Whisper check runs | while it runs |
| `82 % ✓` (green) | the line sealed (Whisper) | `FEEDBACK_HOLD_MS` = 2 500 ms |
| `✓` (green) | the line sealed (lenient) | 2 500 ms |
| `25 % · heard: "gracia tu pi" ↺` (muted, no red) | a miss arrives while every syllable was lit (state before the step) | 2 500 ms |
| `SEAL CRACKED ✕` | a crack (existing) | until the next syllable lights (existing) |
| empty | before the lock; done | — |

- **Score** = `round(100 × (1 − distance))`, clamped 0–100; 40 % or more seals
  (`LINE_MATCH_MAX` = 0.60).
- **Heard text** = the check's raw transcript, lower case, punctuation removed, one line,
  ellipsised at the end. Shown on screen only; not stored in release builds (privacy unchanged).
- Mid-line misses (D4) show nothing: the player is still reading and a partial score would
  discourage.
- TalkBack: the slot is a polite live region (as `VerdictLine` today).

### 3.6 Line box (the phrase)
- The phrase, auto-sized 18–96 sp as today, inside a framed box.
- **Before the lock:** words at the existing dim alpha, a small lock glyph above them, outline
  frame; no karaoke.
- **On lock:** the frame turns accent and flares once (`UNLOCK_FLARE_MS` = 450 ms; reduce motion:
  no flare), the words go to full opacity. This is the "you can chant now" moment.
- Karaoke, current-word scale, green underline for heard words: unchanged.
- **Retry:** unchanged (ADR-0028): after a miss on a fully lit line the words clear; the `↺` in
  the slot is the only cue.
- **Paused:** the words stay as they are (lit syllables kept); the frame turns amber.

## 4. The two arrangements (landscape)

Both keep `safeDrawingPadding` and the House backdrop. Portrait (large screens that ignore the
rotation) keeps today's top band with the new pieces and is not part of the comparison.

### 4.1 Split (today's proportions)
- Left slab, 25 % width: `? D` row, title (name + seals), sigil (fills the slab width, as today),
  direction pill.
- Right, 75 % width: feedback slot, then the line box filling the rest of the height.
- ✕ top-right of the screen.

### 4.2 Centred (Stitch 02)
- Top row: `? D` left, title centred, ✕ right.
- Under it: the direction pill, then the sigil, sized to `SIGIL_CENTRED_FRACTION` = 0.38 of the
  height left under the top row.
- Then the feedback slot, then the line box: full width minus the gutter, taking the remaining
  height.

## 5. Debug switch

- `ChantLayout { Split, Centred }` in `ui/screens/`; read and written through a small
  `DebugPrefs` helper (prefs `debug`, key `chant_layout`; missing = Split).
- A `LayoutChip` in `DebugControls`, beside `CheckModeChip`: "LAYOUT: SPLIT" ↔ "LAYOUT: CENTRED".
  Tapping switches the live screen at once, so the same line can be compared both ways.
- Release builds ignore the pref and use Split.

## 6. Logging (debug builds)

- `SessionHeader` gains `layout: String` (always encoded, like `mode`).
- `SessionTick` gains `ui: UiRecord?`, written only on the tick after it changes:
  `UiRecord(layout, phrase, phraseSp, boxWidthDp, boxHeightDp, sigilDp)`. `phraseSp` is the size
  the auto-sizer settled on (from `TextLayoutResult.layoutInput.style.fontSize`).
- The screen reports it with a `onPhraseLaid(UiRecord)` callback to `RitualViewModel`; a switch
  of layout or line triggers a new record.
- Replay ignores `ui`. `SessionReplayTest` prints, per session, a table of layout × line × sp.

## 7. State and code changes

- `RitualUiState` gains `feedback: LineFeedback?` and `layout: ChantLayout`.
  `LineFeedback(kind: Sealed | Missed | Cracked, score: Int?, heard: String?, untilMs: Long)` is set
  by `RitualViewModel` from the step's effects (`Sealed`, a `LineChecked` miss when
  the state *before* the step had `lit ≥ syllables`, since ADR-0028 clears the line in that step, `Cracked`) and cleared when the ritual clock passes `untilMs`.
- `ChantScreenState` loses `status` and `verdict`; gains `pill: DirectionPill`, `seals`,
  `slot: SlotContent`, `tiltDegrees`, `gated: Boolean` (before the lock). All pure, from
  `RitualState`, azimuth, `checking`, `feedback`.
- `ChantScreen.kt` is split: pieces in `ChantPieces.kt`, the two arrangements in
  `ChantArrangements.kt`; `RitualStatusBlock.kt` is deleted.
- Strings: `chant_face_dir`, `chant_paused`, `chant_checking`, `chant_heard`, `debug_layout_split`,
  `debug_layout_centred`. "SEAL CRACKED" reused.
- Intro card text unchanged ("1. Face … 2. Chant …").

## 8. Device comparison and decision

1. On the S24, both demons, all four lines, each line read once per arrangement at ~30 cm, with
   glasses.
2. Pull the logs (`tools/pull_sessions.sh`), print the size table.
3. The user picks. An ADR-0029 amendment records the numbers and the choice; the losing
   arrangement, the switch and the `layout` fields are deleted in the same change (phrase size
   logging stays).

## 9. Docs

- ADR-0029 "Chant screen UX": D1–D7, the comparison plan; amends ADR-0019, ADR-0020, ADR-0024.
- PRODUCT.md "Chant screen" row and DESIGN.md chant screen contract rewritten for §3 (after the
  decision, the arrangement too).

## 10. Testing

- `ChantScreenStateTest`: pill per phase; tilt clamp and sign; gated before the lock only; slot
  priority (checking over meter, feedback over checking, crack over all); no slot content before
  the lock.
- `RitualViewModel` feedback rules (pure helper `feedbackFor(step, previous, nowMs)`): seal →
  score; mid-line miss → nothing; end-of-line miss → score + heard; expiry at 2 500 ms; lenient
  seal → no score.
- Score mapping: distance 0.60 → 40, 0.18 → 82, values outside 0–1 clamped.
- Session log round trip with `layout` and `ui`.
- Build, lint, all unit tests.

## 11. Out of scope

- Phonetic guides, translations, stanza names, degree read-outs, "mic active" labels (all seen in
  Stitch 08; none needed).
- New stage art, sigil redraws.
- Half-line seals (ADR-0028 known limit).
- Portrait comparison.
