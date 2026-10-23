# Chant screen: one axis, no top bar

- Date: 2026-10-23
- Status: Approved (brainstorming, visual companion: `chant-final.html`)
- Branch: `fix/pre-release-ui`
- Amends: `2026-10-22-chant-screen-polish-design.md` §2, §4, §5 (placement); ADR-0029

## Problem
The 48 dp top bar holds only "?" (and "D") and ✕, yet takes a full row from both columns; the ✕
adds its own 16 dp inset on top of the screen's; the left stack is pinned to the edges, and the
sigil and the phrase sit at different heights (≈ 75 dp apart on the S24).

## Design (S24 landscape numbers: content box 820 × 334 dp)

1. **No top bar.** The right column's first 48 dp is the **HUD row**: "?" (and "D" in debug) at
   its start, the feedback slot centred, ✕ at its end. The slot is kept clear of the buttons by a
   symmetric side inset of 104 dp (two 48 dp targets + 8 dp), so it stays centred on the column;
   long chips ellipsize as today.
2. **One edge inset.** Everything sits 16 dp (`lg`) inside the safe area; ✕ no longer adds its
   own padding.
3. **One axis.** The content box's vertical middle. The sigil, the I–IV track and the phrase are
   centred on it.
4. **Left stack** (`SigilStack`): name plate, sigil, pill as one group, centred in the column,
   all as wide as the sigil.
   - Gaps inside the group: 16 dp (`lg`), equal.
   - Air above and below the group: at least 24 dp (`xl`), equal.
   - Sigil diameter = `min(columnWidth, height − 2·24 − 2·16 − nameHeight − pillHeight)`; a
     circle always (≈ 185 dp on the S24). The sigil is off the axis by (name − pill) / 2
     (≈ 3.5 dp), below visible.
   - At large font sizes the name plate and pill grow and the sigil shrinks; the spacing does not.
5. **Track** centred on the axis (unchanged component).
6. **Phrase** auto-sized in the space below the HUD row and its 24 dp gap (532 × 262 dp on the
   S24), then placed with its centre on the axis; when it is too tall for that, it sits as close
   to the axis as the space allows (`y = clamp(axis − h/2, top, bottom − h)`). Error panels
   (mic, heading) take the same space, centred.
7. **Portrait** (large screens only): HUD row on top, then the band (stack + track), then the
   phrase; unchanged otherwise.

## Validation
- `SigilStackTest` covers the new diameter and group placement; a pure `phraseTop` helper is
  unit-tested for the clamp.
- S24 at font scale 1.0 and 2.0 (`tools/ui_matrix.sh`): no top bar; ✕ 16 dp from the safe
  edge; equal 16 / 24 spacing in the stack; sigil, track and a two-row phrase on one line;
  nothing clipped.

## Docs
ADR-0029 amendment; DESIGN.md chant screen contract.
