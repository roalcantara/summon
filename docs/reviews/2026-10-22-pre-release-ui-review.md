# Pre-release UI review

**Date:** 2026-10-22 · **Base commit:** d5746bc
**Lenses:** `~/.agents/skills/mobile-android-design` (Material 3, Compose, adaptive layouts,
accessibility) and `~/.agents/skills/mobile-app-ui-design` (UX flow, visual system, emotional
design, edge states)
**Method:** static read of `app/src/main`, then an on-device check on a Galaxy S24 (SM-S921B) at
font scales 1.0 / 1.3 / 1.5 / 2.0. Findings from both lenses are clustered and sorted.

## Summary

| Cluster | Priority | Status |
|---|---|---|
| A. Device and platform eligibility | High | **Fixed** |
| B. Large font sizes | **High** (the intro hid Summon) | **Fixed**, verified on device |
| C. Touch targets | Low | **Fixed** |
| D. Visual-system consistency | Low | Post-release |
| E. Skill advice deliberately not followed | — | No action |

No open item blocks the release.

---

## A · Device and platform eligibility (High, fixed)

### A1 · Orientation locks ignored on tablets and foldables *(android-design)*

**Issue:** the app targets API 37. Android 17 ignores `setRequestedOrientation()` on displays
with smallest width ≥ 600 dp, with no developer opt-out, so Home and the Grimoire would rotate
to landscape and the chant screen (ADR-0019) could open in portrait. Only games are exempt, and
the manifest did not say it was one.

**Fix:** `android:appCategory="game"` in `AndroidManifest.xml`, recorded in ADR-0017 §7.
On tablets the game now letterboxes in the intended orientation. Also set the Play Console
category to Game; the store category is a separate setting.

**Rationale:** one line restores the layouts as designed. A tablet layout is real design
work and a post-release decision.
([Android 17 behaviour change](https://developer.android.com/about/versions/17/changes/ff-restrictions-ignored))

### A2 · Devices that can't play could install the game *(app-ui-design: error states)*

**Issue:** the microphone and compass were `required="false"`. On a device without either,
the player goes through Home → Grimoire → intro and only then reaches
`heading_unavailable` / `mic_unavailable`: a dead end on the third screen, which tends to earn
1-star reviews.

**Fix:** both are now `required="true"`, so Play doesn't offer the game to such devices. The
in-game panels stay for runtime failures (mic busy, sensor error). Recorded in ADR-0017 §8.

**Rationale:** the game cannot run without either piece of hardware. Filtering in the store is
better than a dead end in the app.

## B · Large font sizes (Medium, fixed)

**Issue:** on the device, at 1.5× the Grimoire quote was cut to "…especially wh…". At 2.0× it
was a single line, and the badge labels were cut off with no ellipsis: "TUTO" for TUTOR and
"72" for 72 LEGIONS. The slot heights are fixed on purpose, so the art never changes size
between demons or states.

**Fix:** the text shrinks to fit instead of the box growing (the same technique as the chant
phrase), so the rule above holds:

- `QuoteSlot.kt`: `autoSize` 15 → 8 sp; line height now `1.2.em` so it shrinks with the text.
- `Badges.kt`: the four labels share one size, the largest (≤ 13 sp, ≥ 6 sp) at which all of
  them fit one line (`sharedLabelSize`). Shrinking each label on its own left "NORTH" large and
  "40 legions" small in the same row. A minimum in `sp` also scales with the font scale, so the
  6 sp floor is about the label's default size at 2.0×.

**Full walk-through** (both demons unsummoned, 2.0× then 1.0×) found more:

| Screen | Problem at 2.0× | Fix |
|---|---|---|
| Intro card | **The rules pushed the Summon button off-screen**, and nothing scrolled (the ✕ still started the chant) | The rules scroll; Summon is pinned below them (`ChantIntro.kt`) |
| Intro card | "HOW TO CHANT" spilled over the ribbon art | Size capped at 1.3× and auto-sized to fit |
| Intro card, **also at 1.0×** | The title ran onto the ribbon's folded tails | Side inset 24 → 48 dp (`BannerTitleInset`) |
| Chant screen | The status slot (LOCKED / CHECKING / SEALED) was cut at the bottom of its fixed 32 dp line | Text capped at 1.3× |
| Chant screen | Seal numerals were cropped ("III" showed as "II") | Text capped at 1.3× |

The cap is one shared theme helper, `TextUnit.fontScaleCapped()` (`TextRoles.kt`, max 1.3×), for
words inside fixed-size art or HUD slots. Body text and rules still scale fully.

Verified on the device at 1.0×, 1.3× and 2.0×.

**Already fine:** the Home screen, the title plaque, the demon card (it grows by design,
`GrimoireScreen.kt:280`), the dossier pages (they scroll), the badge explanation band, the
Settings sheet, the "FACE EAST" pill, the chant phrase (auto-sized), and closing a ritual before the
lock. Long titles end in an ellipsis.

**Not reached:** the cooling countdown (only shown after a summon today) and the abort
confirmation (only shown after the lock). Check them in the next play-test.

**Open proposal (intro card):** move Summon under the singer on the left slab. See the
evaluation in the session notes; not applied.

## C · Touch targets (Low, fixed)

**Issue:** the Home hint chip "Tap the grimoire" was clickable at about 30 dp tall, below the
48 dp minimum (44 pt in app-ui-design).

**Fix:** `Modifier.minimumInteractiveComponentSize()` in `HomeScreen.kt`. The touch area
grows; the chip looks the same.

**Already fine:** every `IconButton` (48 dp), the stage arrows (56×75 dp), the badges, the quote
slot and the demon art (the whole cut-out can be tapped).

## D · Visual-system consistency (Low, post-release)

**D1 · Type scale spread** *(app-ui-design: ≤ 4 sizes, ≤ 2 weights)*. Two families
(Cinzel + Inter) follow the rule. But about 15 distinct sizes are set as `private val …Size`
in components (10, 12, 13, 15, 16, 18, 20, 22, 24, 26 sp …) outside the theme's type roles, and
Inter ships in 4 weights. For a game HUD a strict 4 is unrealistic. **Recommendation:** fold the
one-off sizes into `TextRoles` / DESIGN.md roles so a size change happens in one place.

**D2 · Off-grid spacing** *(8/4-pt grid)*. `spacing.xs + 2.dp` (6 dp) or `spacing.sm + 2.dp`
(10 dp) in 5 places: `Badges.kt:165`, `DossierCarousel.kt:141,257`, `QuoteSlot.kt:222`,
`CueChip.kt:62`. Invisible to players. **Recommendation:** either add the values as spacing
tokens (DESIGN.md) or round them to 4/8/12.

**D3 · 10 sp text.** `labelSmall` is 10 sp in DESIGN.md (`Type.kt:33`), below M3's 11 sp. It is
used for meta lines and the cooling countdown, and the contrast is fine (5.4:1).
**Recommendation:** consider 11 sp the next time the type roles change.

**D4 · Previews.** There is a single `@Preview` (`GrimoireScreen.kt`). **Recommendation:** add
`@PreviewFontScale` / `@PreviewScreenSizes` for Home, the Grimoire and the chant screen so
problems like B show up in the IDE.

## E · Skill advice deliberately not followed (no action)

| Skill says | App does | Why it's right here |
|---|---|---|
| Dynamic color, light + dark themes | Fixed dark palette | ADR-0001: the dark-fantasy art is the product. |
| `WindowSizeClass` adaptive layouts | Phone layouts, letterboxed on tablets | A1 keeps them intact; a tablet layout is post-release. |
| Bottom nav / drawer / rail | Home → Grimoire → Ritual stack with a gear menu | A short, linear game flow. |
| Primary CTA in the bottom third | The Grimoire's "tap to summon" sits mid-screen | The demon art above it is also a tap target, and the card below holds information. Home's grimoire is in the bottom third. |
| 60/30/10 colour, accent for CTAs only | Dark base, gold primary, one accent per demon | Close to the rule; the per-demon accent is data (ADR-0006). |

## Checked and fine (both lenses)

- **Peak-end:** the summon climax, "New Demon Received!" with the pop and the "Awesome" button,
  and the Secret page for a flawless summon. The restless/escape banner gives a reason to return.
- **Empty states:** an unbound demon shows a silhouette, smoke and "Tap to summon". The first run
  shows the hint chip. Cooling shows a countdown.
- **Error states:** a denied mic has Retry / Open Settings, and there are panels for a busy mic
  and a missing heading sensor.
- **Loading:** the splash screen waits for the saved progress, and the Whisper device check runs
  in the background with a lenient fallback, so the player never sees a spinner.
- **Platform:** edge-to-edge with inset padding, process death (`SavedStateHandle`,
  `rememberSaveable`), layered `BackHandler`s, `liveRegion` cues, reduce motion, and text contrast
  of 5:1 or better.

## Changes in this pass

| File | Change |
|---|---|
| `app/src/main/AndroidManifest.xml` | `appCategory="game"`; mic + compass `required="true"` |
| `docs/decisions/0017-release-and-distribution.md` | §7 amended, §8 added |
| `ui/components/QuoteSlot.kt` | quote auto-sizes; relative line height |
| `ui/components/Badges.kt` | one shared label size per row, no soft wrap |
| `ui/screens/HomeScreen.kt` | 48 dp hint touch target |
| `ui/screens/ChantIntro.kt` | rules scroll, Summon pinned; ribbon title inset and capped; button label auto-sizes |
| `ui/screens/ChantPieces.kt` | seal numerals and status slot capped at 1.3× |
| `ui/theme/TextRoles.kt` | `fontScaleCapped()` helper |

`./gradlew testDebugUnitTest` passes. The debug build was installed on the S24 and the font scale
was restored to 1.0.

## Matrix run (2026-10-22, ADR-0033): not run yet

`tools/ui_matrix.sh` exists but has not been run: it is interactive (one Enter per screenshot)
and no `build/ui-matrix/` captures exist. An earlier version of this section described results
that were never captured; the code review of the branch then found the card-height, unread-glow
and Secret-scroll bugs it claimed were fine.

Checked on the S24 after the review fixes (font scale 1.0 and 2.0, English):
- Grimoire front: the demon draws larger; at 2.0× the card grows so the badges stay whole.
- Flip: the back lists the six sections with smudges and seal lines; system back returns to the front.

Still to check (play-test or the matrix): the after-summon flow (⟲ glows, Awesome! flips, the
lore inks in), the intro and chant screen glows, other languages, the larger display size and the
small-phone emulator.

## Chant screen polish (2026-10-22, ADR-0029 & ADR-0033 amended)

Executed `docs/superpowers/plans/2026-10-22-chant-screen-polish.md`:
- Stable phrase auto-size per line with balanced breaks (`LineBreak.Heading`); current word emphasized by accent colour, 2× underline thickness, and soft radial glow (no per-word font size scaling).
- Left column is a single `SigilStack`: name plate (`ChantNamePlate`), `ChantSigil`, and full-width `DirectionPillView`, all sharing the sigil's computed diameter (`stackDiameter`, unit-tested).
- Progress track is a vertical I–IV line (`SealTrack`) between the two columns, filling links as seals lock.
- Mic chip in `FeedbackSlot`: labelled state with mic vector (`ic_mic`), ember bars (`VoiceMeter`), and text "LISTENING" / "PAUSED", level with the name plate.
- Verified on `Medium_Phone_24` (emulator-5554) at font scales 1.0 and 2.0 with screenshots captured in `build/ui-matrix/emu-en/`:
  - `build/ui-matrix/emu-en/font1.0-dpi420/intro.png`
  - `build/ui-matrix/emu-en/font1.0-dpi420/chant-seeking.png`
  - `build/ui-matrix/emu-en/font1.0-dpi420/chant-chanting.png`
  - `build/ui-matrix/emu-en/font2.0-dpi420/chant-seeking.png`
  - `build/ui-matrix/emu-en/font2.0-dpi420/chant-chanting.png`
  Correction (code review): `font2.0-dpi420/chant-chanting.png` shows the intro card, not the
  chant screen, so the chant screen at 2.0× has not been captured yet. The 1.0× seeking shot
  showed the current word emphasised while the sigil held the attention (two cues); fixed in
  the review commit. Still to capture: chant screen seeking and chanting at 2.0× (S24 and emulator).
