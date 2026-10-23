# Final-pass review fixes

- Date: 2026-10-23
- Status: Approved (brainstorming; scope "fix confirmed + cheap taste")
- Input: `docs/reviews/2026-10-23-final-pass/report-1-m3-android.md` (R1), `report-2-motion.md` (R2),
  `report-3-ux-visual.md` (R3); finding `R1-3` is row 3 of report 1's Findings table.
- Plan: `docs/superpowers/plans/2026-10-23-final-pass-fixes.md`

## Why
Three independent reviewers (M3/Android, motion, UX) audited the app before release. Their findings
overlap and some overstate: every claim below was checked against the code at `1bfb17c` before
triage. No finding is a true release blocker; the five "Blockers" are either per-frame recomposition
(real, but a performance cost, not a broken feature) or a false positive (R1-1).

## Verification notes (what the code actually shows)
- **R1-1 (QuoteSlot loses its click action): likely false.** `clearAndSetSemantics` clears the
  semantics of *descendant* layout nodes; the `clickable` on the same node keeps its `OnClick`. Proven
  or disproven by `uiautomator dump` in task 7, fixed only if the node is not clickable.
- **R1-2 (predictive back flag): moot.** `targetSdk = 37`; from API 36 targets predictive back is on
  by default and `enableOnBackInvokedCallback` is not needed.
- **R2-7 (off-screen pager pages animate): false.** `HorizontalPager` composes only visible pages
  (`beyondViewportPageCount` is the default 0); the sunburst draws only on the party page.
- **R1-3..6 = R2-1..4**, **R1-17 = R2-9**, **R1-14 = R3-9**: duplicates, fixed once.
- **R3-5** is a real conflict with ADR-0029 §5 ("Paused is amber, not red"): the paused mic chip uses
  `colors.outline`.
- **R3-4**: `LinesIcon` draws a red wavy `colorScheme.error` line; the chant screen has no such state.
- The repo has no Compose UI tests (no Robolectric). Acceptance is checked by `uiautomator dump`
  semantics, `tools/ui_matrix.sh` screenshots, grep, and the existing unit tests.

## Triage

| Finding | Disposition | Reason |
|---|---|---|
| R1-1 | Verify (task 7) | Likely false positive; prove with a dump |
| R1-2 | Won't fix | Moot at targetSdk 37 |
| R1-3, R1-4, R1-5, R1-6 / R2-1..R2-4 | Fix, task 1 | Confirmed: `.value` of an infinite transition read in composition |
| R2-5 | Fix, task 1 | Confirmed: `angle` read in composition; `Path()` allocated per beam per frame |
| R2-14 | Fix (derivedStateOf part), task 1; spec part deferred | Face switch recomposes per frame; spring retune is taste |
| R3-1 | Fix, task 2 | No `BackHandler` for the escape banner: Back exits the app |
| R3-2 | Fix, task 2 | Header `←` ignores `CardSide.Back`, unlike system Back (line 248) |
| R3-3 | Fix, task 3 | Intro ✕ lacks the 16 dp the chant HUD ✕ has |
| R3-4 | Fix, task 3 | Icon shows an error state the game does not have |
| R3-5 | Fix, task 4 | Violates ADR-0029 §5 |
| R3-6, R3-7 | Fix, task 5 | Fixed-size art slots without `fontScaleCapped()` (ADR-0033) |
| R3-8 | Fix, task 5 | Settings sheet cannot scroll at 2.0× |
| R1-12 | Fix, task 5 | Possible double bottom inset in Settings and Debug sheets |
| R2-8, R2-13, R1-17 / R2-9 | Fix, task 6 | Reduce motion not honoured |
| R1-7, R1-18 | Fix, task 7 | TalkBack reads content before the navigation HUD |
| R1-8 | Fix, task 7 | Home hint chip is a live region and has no button role |
| R1-9 | Fix, task 7 | No focus indication on `StageArrow` and the intro Summon for keyboard/D-pad |
| R1-10 | Fix, task 7 | `DemonCutout` has no button role |
| R1-11 | Fix, task 7 | Text `←` does not mirror in RTL |
| R2-10, R2-11, R2-12 | Fix, task 8 | Cheap expressive-motion wins at the peak moments |
| R2-18 | Fix, task 8 | Summon gets a light haptic, like the heading lock |
| R1-14 / R3-9 | Fix, task 8 | Off-grid 6 dp / 10 dp spacing |
| R1-16 | Fix, task 8 | Unused import |
| R1-19 | Fix, task 8 | Open badge resets on recreation |
| R3-10 | Fix, task 8 | Selected badge header too light at 10 sp |
| R2-6 | Deferred | Screen-transition motion is a design decision (needs an ADR); M effort |
| R1-13 / R3-11 | Deferred | Typography token consolidation: M effort, no player-visible change |
| R1-15 | Deferred | Font-scale previews: ui_matrix already covers font scales |
| R2-15, R2-16, R2-17 | Deferred | Feel tuning; judge on device after v1 |
| R2-7 | Won't fix | False: off-screen pages are not composed |

Deferred items go to `TODO.md` as one entry ("Final-pass deferred motion/typography items") pointing
here.

## Fix design and acceptance criteria

Every task: one Conventional Commit; `./gradlew -q testDebugUnitTest` exits 0 after it; no new player
strings unless all five languages are added and `TranslationResourcesTest` passes; nothing under
`reference/` changes; theme tokens only (no new raw colours). Device checks run on the S24 (`s24-en`
profile); if a `ui_matrix.sh` run is interrupted, restore with `adb shell wm density 420`.

### Task 1: animated values read in the draw phase
Files: `ui/screens/ChantPieces.kt` (`SealMark`, `DirectionPillView`, `EdgeChevrons`, `CheckingMeter`),
`ui/components/RotatingSunburstBackground.kt`, `ui/components/FlipCard.kt`.
- Keep each infinite `animateFloat` as `State<Float>` (no trailing `.value`); read `.value` only inside
  `graphicsLayer { }`, `drawBehind`/`Canvas` draw lambdas, or pass the `State` (or a `() -> Float`) to
  the child that draws (`VoiceMeter` for the checking level).
- Reduce motion keeps working: the reduced branch supplies a constant (e.g. `remember { mutableFloatStateOf(1f) }`)
  instead of a raw `Float`.
- Sunburst: rotation through `Modifier.graphicsLayer { rotationZ = angle.value }` on the Canvas; beam
  paths built once per size (`remember`/`drawWithCache`), not per frame.
- FlipCard: the front/back switch reads `derivedStateOf { angle <= 90f }`.

**AC**
1. `grep -n '^\s*)\.value' app/src/main/java/com/summondemons/safely/ui/screens/ChantPieces.kt` returns
   nothing (at `1bfb17c` it matches lines 330, 404, 439, 655: an `animateFloat(...)` closed and read in
   composition).
2. `RotatingSunburstBackground` has no `Path()` call inside the draw loop and no composition-scope read
   of the angle.
3. ui_matrix screenshots `chant-seeking`, `chant-paused`, `chant-checking`, `grimoire-party` (or the
   nearest captured states) show the same visuals as before (side-by-side listed in the report).
4. With reduce motion on, the four ChantPieces elements are static, and the sunburst does not rotate.

### Task 2: Back is consistent
Files: `ui/screens/HomeScreen.kt`, `ui/screens/GrimoireScreen.kt`.
- `EscapeBanner` registers `BackHandler(onBack = onDismiss)`.
- The header `←` callback: celebration → dismiss; else card back side → flip to front; else `onBack()`
  (same order as the two `BackHandler`s at lines 247–248). Update the comment at line 379.

**AC**
1. Banner shown (force an escape via the debug panel/clock) → system Back dismisses it; the app stays
   on Home.
2. Grimoire, card flipped to the dossier → tap `←` → card shows the front; tap `←` again → Home.
3. During the celebration, `←` still dismisses the celebration first.

### Task 3: Chant intro alignment and Rule 3 icon
File: `ui/screens/ChantIntro.kt`.
- The ✕ `IconButton` gets `.padding(MaterialTheme.spacing.lg)` after `safeDrawingPadding()`, matching
  `HudRow`'s inset, so it does not move when the intro lifts.
- `LinesIcon` draws what the chant line really shows: a short `ritualSafe` line (heard) and an
  accent line with a soft halo (current). No `colorScheme.error`, no wavy path. Pass `accent` in as
  `SealsIcon` does.

**AC**
1. `uiautomator dump` on the intro and on the chant screen: the ✕ node bounds differ by ≤ 2 dp
   (≤ 6 px at 420 dpi) on each edge.
2. `grep -n "colorScheme.error" ui/screens/ChantIntro.kt` returns nothing.
3. Screenshot `intro` shows the new icon; it reads as "green heard, accent current".

### Task 4: Paused is amber
File: `ui/screens/ChantPieces.kt` (`FeedbackSlot`, `SlotContent.Meter`).
- When `paused`: chip border, mic tint, meter colour and "PAUSED" text use `colors.primary`. Listening
  is unchanged.

**AC**
1. Screenshot `chant-paused`: the slot chip is amber like the pill and chevrons.
2. Listening state screenshot unchanged.

### Task 5: Large text and sheets
Files: `ui/components/TitlePlaque.kt`, `ui/components/DemonCard.kt`, `ui/components/SettingsSheet.kt`,
the debug sheet (wherever `DebugSheet` lives).
- `TitlePlaque`: `TitleSize.fontScaleCapped()` plus `TextAutoSize.StepBased` down to a minimum, one line.
- Medallion number: `NumberSize.fontScaleCapped()`.
- `SettingsSheet` content `verticalScroll(rememberScrollState())`.
- Remove the inner `navigationBarsPadding()` in both sheets *only if* `ModalBottomSheet`'s default
  insets already pad the bottom (check with 3-button navigation); otherwise keep it and record why.

**AC**
1. ui_matrix at font 2.0 in en, es, pt-BR, it, fr: the Grimoire plaque text stays inside the plaque and
   does not overlap the `←` or menu touch targets (dump bounds do not intersect).
2. At font 2.0 the medallion numeral keeps a visible gap to the rim.
3. At font 2.0 the Settings sheet scrolls to its last item (debug build: the debug button).
4. With 3-button navigation, neither sheet has extra empty space under its last item (screenshot).

### Task 6: Reduce motion gaps
Files: `ui/components/Dossier.kt`, `ui/components/PrimaryActionButton.kt`.
- `InkLine`: under `LocalReduceMotion`, no blur; snap to 1 or a ≤ 150 ms alpha fade.
- `scrollToSecret`: `scrollTo` under reduce motion, `animateScrollTo` otherwise.
- `PrimaryActionButton`: scale target `0.96f` only when pressed and not reduce motion.

**AC**
1. Reduce motion on, flawless summon: the Secret page appears without blur and the dossier jumps
   (no smooth scroll).
2. Reduce motion on: pressing a primary button does not scale it.
3. Reduce motion off: all three behave as before.

### Task 7: Accessibility semantics
Files: `ui/components/QuoteSlot.kt`, `ui/components/CueChip.kt`, `ui/screens/HomeScreen.kt`,
`ui/screens/GrimoireScreen.kt`, `ui/screens/ChantIntro.kt`, `ui/screens/ChantArrangements.kt`.
- R1-1: dump first. Only if the summon cue is not `clickable="true"`, move the action into
  `clearAndSetSemantics { onClick { onTap(); true } }`.
- `CueChip`: `liveRegion` becomes an opt-in parameter (default off); Home's hint chip uses
  `clickable(role = Role.Button, onClick = open)`. Other `CueChip` callers that relied on the live
  region pass it explicitly.
- `DemonCutout` clickable gets `role = Role.Button`.
- Grimoire header column and `HudRow` become traversal groups with a lower `traversalIndex` than the
  content, so TalkBack reads Back/title/menu (Grimoire) and ?/D/slot/✕ (chant) first.
- `StageArrow` and the intro `SummonButton`: a visible focus indication (e.g. a `primary` outline drawn
  while focused, via `collectIsFocusedAsState`); press feedback stays as is.
- Header `←`: `Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)` inside the
  `IconButton` that already carries the description; drop the `clearAndSetSemantics` workaround. Add
  `material-icons-core` to the version catalog only if the icon is not already available.

**AC**
1. `adb shell uiautomator dump`: the summon cue node and the demon stage node have `clickable="true"`;
   the stage node's class or role reads as a button in TalkBack.
2. TalkBack on Grimoire: first swipe-right lands on Back, then the title, then menu, before any card
   content. On the chant screen: ?, D, the slot, ✕ before the phrase.
3. TalkBack on Home: the hint chip is announced once on focus as a button, not spontaneously.
4. With a keyboard (`adb shell input keyevent KEYCODE_TAB` repeatedly), a focus outline is visible on
   each stage arrow and on Summon (screenshots).
5. `grep -n 'Text("←")' -r app/src/main` returns nothing.

### Task 8: Cheap polish
- R2-10: `SealMark` flare uses `spring(DampingRatioMediumBouncy, StiffnessMedium)`; reduce motion
  unchanged.
- R2-11: `RestlessGrimoire` hop keyframes use `FastOutSlowInEasing` (or an up-ease-out / down-ease-in
  keyframe spec).
- R2-12: the climax rise fraction passes through `FastOutSlowInEasing.transform(...)`; reduce motion
  path unchanged.
- R2-18: `SummonButton` click performs `HapticFeedbackType.LongPress` (or `Confirm` where available).
- R1-14 / R3-9: replace `spacing.xs + 2.dp` and `spacing.sm + 2.dp` with the nearest token, checking each
  in a screenshot; no new tokens.
- R1-16: remove the unused `sp` import in `GrimoireScreen.kt` (only if truly unused).
- R1-19: the open badge uses `rememberSaveable(demon.id)` (enum name saver).
- R3-10: the selected badge header in `InfoBand` uses a bold weight (or `labelMedium`), same colour.

**AC**
1. On device: seal flare overshoots slightly; the hop and the rise ease (judged on video or by eye,
   noted in the report).
2. Tapping Summon on the intro gives a short haptic.
3. `grep -nE 'spacing\.(xs|sm) \+ 2\.dp' -r app/src/main` returns nothing; affected screenshots show no
   regression.
4. Open a badge, change the language in Settings: the same badge is still open.

### Task 9: Verify and close
- `./gradlew -q testDebugUnitTest` exit 0; `./gradlew assembleDebug` exit 0.
- Full `tools/ui_matrix.sh s24-en en` run; list the screenshot directory.
- Update this spec's triage table with a final column (Fixed / Verified-not-needed / Deferred) and
  commit hashes.
- `DESIGN.md`: update only if a component description changed (the Rule 3 icon; paused slot colour).
- `TODO.md`: one entry for the deferred items.

**AC**
1. The implementer's report lists, per task, the exact commands run with their exit codes and the
   screenshot or dump paths. Claims without evidence count as not done.
2. The device is back at density 420 and font scale 1.0 after the run.

## Out of scope
NavHost transitions, typography token consolidation, previews, spring harmonisation of the sigil ring
(R2-6, R1-13, R3-11, R1-15, R2-15..17). Analytics and Psst follow this work.
