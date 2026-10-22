# Chant screen polish: stable phrase, left stack, vertical progress

- Date: 2026-10-22
- Status: Approved (brainstorming session, visual companion; mockup `chant-top-v4.html`, option 1)
- Branch: `fix/pre-release-ui`
- Amends: ADR-0029 (chant screen layout), ADR-0033 (current word emphasis)
- Builds on: `2026-10-22-pre-release-screens-design.md` (text rules §1.1, attention glow §1.2)

## Problem (play-test, S24)

1. **The phrase jumps while chanting.** The current word is drawn at 1.15 em
   (`CURRENT_WORD_SCALE`); every time the current word advances, the line's width changes, the
   auto-sizer refits it and the size and line breaks move ("TUI GRATIA LOVIS" →
   "TUI GRATIA / LOVIS").
2. **The top looks truncated and unorganised.** The silent voice meter's five bars collapse to
   "•••••", which reads as truncated text; the name and seals are centred over the whole screen
   while the meter and phrase are centred over the right column; the 22 dp seals read as
   decoration.

## Design

### 1. Stable phrase
- The current word keeps the phrase's font size. It is marked by colour only: the accent
  colour (lit syllables as today), a thicker underline (2× the pending line), and a soft glow
  behind it (accent, same alpha as the chanting halo, drawn from the word's layout bounds).
- `CURRENT_WORD_SCALE` and its `SpanStyle(fontSize = …)` are removed, so only colours change
  between words; the auto-sized phrase size is the same for the whole line.
- Two-row lines break balanced from the start: the phrase style uses
  `lineBreak = LineBreak.Heading`. `WholeWordAutoSize` keeps rejecting sizes that split a word.

### 2. Left column: name plate, sigil, direction pill
- One vertical stack, centred in the left column (30 %), all three as wide as the sigil:
  - **Name plate:** the demon's name (upper case, `name` role, accent colour, letter-spaced)
    between two thin accent rules (alpha 0.35), sized to fit the stack width, at most
    `NameSize` (22 sp) capped with `fontScaleCapped()`; one line, never wraps.
  - **Sigil:** takes all the height left between the plate and the pill; square; its diameter
    sets the stack width.
  - **Direction pill:** stretched to the stack width; text centred; the one-line rule from the
    pre-release spec (cap 1.3×, auto-size down) unchanged.
- Gaps: `spacing.md` between the three.

### 3. Vertical progress track, between the columns
- A column of four seal nodes (I–IV) joined by a vertical line; the segment above a sealed node
  is filled in the accent colour, the rest is `outlineVariant`.
- Node looks are today's `SealMark` looks (pending, current breathing, sealed filled + flare,
  cracked error, current-and-paused amber); node size grows from 22 dp to 28 dp; numerals use
  `fontScaleCapped()`.
- The whole track keeps today's merged semantics (`chant_a11y_seals`).
- The track is vertically centred on the sigil.

### 4. Top row
- "?" and "D" (debug) top left, ✕ top right. The centred title (`ChantTitle`) is removed.

### 5. Mic chip (the feedback slot's idle state)
- `SlotContent.Meter` draws a chip instead of bare bars: mic icon (`ic_mic`, 16 dp), the live
  level bars (today's `VoiceMeter`), and a label: **LISTENING** (`chant_listening`), or
  **PAUSED** (`chant_paused`, existing) while wavering. Chip style = the slot's other chips
  (`Chip`), edge `outlineVariant`; label text capped at 1.3× like the slot.
- All other slot states (Locked, Checking, Sealed, Missed, Cracked) are unchanged.
- **Placement:** both columns are top-aligned: the slot (chip) starts level with the name
  plate, so the two columns begin on one line; the phrase is centred in the rest of the right
  column, at least `spacing.xl` below the slot.
- New string `chant_listening`: en "Listening", es "Oyendo", pt-BR "Ouvindo", it "Ascolto",
  fr "J’écoute".

### 6. Portrait arrangement (large screens only)
- Top row as §4; a band with the stack (§2, fixed band size) and the vertical track beside it;
  then the slot and the phrase, as today.

## Validation
- Unit tests stay green; `ChantScreenStateTest` unchanged (no state changes).
- S24, font scale 1.0 and 2.0, both demons: the phrase never changes size or line breaks while
  the current word advances; name, sigil and pill share one width; the track fills as lines
  seal; the chip reads LISTENING / PAUSED, never "•••••"; nothing clipped (spec §5.3 of the
  pre-release spec).
- `tools/ui_matrix.sh` chant shots (seeking, chanting) on the S24 and the small-phone emulator.

## Docs
- ADR-0029: amendment (layout: left stack, vertical track, top row, mic chip).
- ADR-0033: amendment (current word marked by colour, underline and glow, never by size).
- DESIGN.md: chant screen component contracts; PRODUCT.md chant row if it mentions the title.

## Out of scope
New art; tablet-specific layout; changing the slot's other states.
