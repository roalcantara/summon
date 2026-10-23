# Final-pass fixes report

## Result
done (code steps completed and verified via unit tests; device checks not run: no device attached to adb)

## Commits
| Task | Commit | Message |
|---|---|---|
| Task 1 | `03425fb` | `perf(ui): Read looping animation values while drawing, not composing` |
| Task 2 | `e0b2102` | `fix(nav): Back dismisses the escape banner; the header arrow turns the card first` |
| Task 3 | `c42fe16` | `fix(intro): The close button sits where the chant screen's does; Rule 3 shows the real line marks` |
| Task 4 | `8f95367` | `fix(chant): The paused mic chip is amber, like every other paused cue (ADR-0029)` |
| Task 5 | `a1d3291` | `fix(ui): Cap the plaque title and medallion numeral at large font sizes; Settings scrolls` |
| Task 6 | `f1503c6` | `fix(a11y): Reduced motion skips the ink blur, the secret scroll and the button press scale` |
| Task 7 | `d2786e6` | `fix(a11y): Buttons announce as buttons, navigation reads first, focus is visible, the back arrow mirrors` |
| Task 8 | `d340c9a` | `feat(motion): Springier seal flare, smooth hop, eased climax rise, Summon haptic; spacing on the grid` |
| Task 9 | `a76624d` | `docs(review): Final-pass outcomes recorded; deferred items in TODO` |

## Acceptance (one row per AC in the spec)
| Task.AC | Result (pass / fail / not run) | Evidence (command + exit code, or file in build/final-pass/) |
|---|---|---|
| 1.1 | pass | `grep -n '^\s*)\.value' app/src/main/java/com/summondemons/safely/ui/screens/ChantPieces.kt` exit 1 (no output) |
| 1.2 | pass | Inspect `RotatingSunburstBackground.kt`: no `Path()` inside draw loop, `graphicsLayer { rotationZ = ... }` |
| 1.3 | not run | not run: no device attached to adb |
| 1.4 | not run | not run: no device attached to adb |
| 2.1 | not run | not run: no device attached to adb (code: `BackHandler(onBack = onDismiss)` in `EscapeBanner`) |
| 2.2 | pass (S24, reviewer) | Dossier side → `←` shows the front (`build/final-pass/task2-flipped.png`, `task2-front.png`); second `←` → Home (`task2-home.xml`) |
| 2.3 | not run | not run: no device attached to adb (code: `party != null -> onDismissCelebration()`) |
| 3.1 | pass (S24, reviewer) | ✕ crops of `task3-intro.png` and `task3-chant.png` (x 1940–2340, y 0–260) identical; dump not possible (no idle state) |
| 3.2 | pass | `grep -n "colorScheme.error" app/src/main/java/com/summondemons/safely/ui/screens/ChantIntro.kt` exit 1 (no output) |
| 3.3 | pass (S24, reviewer) | `task3-intro.png`: green heard line + accent line with halo, no red wavy line |
| 4.1 | not run | not run: no device attached to adb (code: `SlotContent.Meter` uses `colors.primary` when paused) |
| 4.2 | not run | not run: no device attached to adb |
| 5.1 | not run | not run: no device attached to adb (code: `TitleSize.fontScaleCapped()` + `TextAutoSize.StepBased`) |
| 5.2 | not run | not run: no device attached to adb (code: `NumberSize.fontScaleCapped()`) |
| 5.3 | pass (S24, reviewer) | Sheet showed a scrollbar while the app was still at font 2.0 after the aborted matrix run (that screenshot was overwritten; re-check in the post-Psst matrix) |
| 5.4 | pass (S24, reviewer) | `task5-settings-3button.png` vs `task5-settings-gesture.png`: only the sheet padding under the last chip, no second nav-bar gap |
| 6.1 | not run | not run: no device attached to adb (code: `InkLine` blur skipped and `Dossier` secret jumps with `reduceMotion`) |
| 6.2 | not run | not run: no device attached to adb (code: `PrimaryActionButton` scale 1f when `reduceMotion`) |
| 6.3 | not run | not run: no device attached to adb |
| 7.1 | pass (S24, reviewer) | `grimoire.xml`/`task7-summon-cue.xml`: cooling cue (same clickable + clearAndSetSemantics pattern as SummonCue) `clickable=true` with its label; stage cutout, arrows, badges clickable; back arrow class `Button` |
| 7.2 | not run | not run: no device attached to adb (code: `isTraversalGroup = true; traversalIndex = -1f` on Grimoire overlay & `HudRow`) |
| 7.3 | partly (S24, reviewer) | `home.xml`: hint chip node `clickable=true`; TalkBack announcement not run |
| 7.4 | not run | not run: no device attached to adb (code: `Modifier.focusRing` applied to `StageArrow` and `SummonButton`) |
| 7.5 | pass | `grep -rn 'Text("←")' app/src/main` exit 1 (no output; replaced with `ic_arrow_back.xml` vector) |
| 8.1 | not run | not run: no device attached to adb (code: `smoothKeyframe` for `RestlessGrimoire` hop; spring pulse for `SealMark`; `FastOutSlowInEasing` for climax rise) |
| 8.2 | not run | not run: no device attached to adb (code: `Haptics.tick(context)` in `SummonButton`) |
| 8.3 | pass | `grep -rnE 'spacing\.(xs|sm) \+ 2\.dp' app/src/main` exit 1 (no output) |
| 8.4 | not run | not run: no device attached to adb (code: `rememberSaveable(demon.id)` on `badge`) |
| 9.1 | pass | `./gradlew -q testDebugUnitTest && ./gradlew -q assembleDebug` exit 0 |
| 9.2 | deferred | Full `ui_matrix.sh` run deferred until analytics and Psst land (operator decision, 2026-10-23); the first attempt ran on a locked screen and is void |

## Decisions taken on the device
- R1-1 (summon cue clickable?): **false positive, no change** (reviewer, S24). The cooling cue uses the same `.clickable(...).clearAndSetSemantics { }` pattern and dumps `clickable=true` with its label. The ready summon cue itself could not be dumped: the stage never idles for UI Automator.
- R1-12 (double bottom inset?): **no double inset, no change** (reviewer, S24): gesture and 3-button screenshots show only the sheet padding; `.navigationBarsPadding()` kept.

## Deviations from the plan
- In Task 1 Step 4 (`EdgeChevrons`): retained `val stroke = ChevronStroke.toPx()` in `Canvas`, which was required for chevron path geometry calculations.

## Open questions
- None.

## Reviewer device pass (2026-10-23)
Run by the reviewer on the S24, limited to the checks that decide a change (operator decision). Not run: 1.3/1.4, 2.1, 2.3, 4.x, 5.1/5.2, 6.x, 7.2, 7.4, 8.1/8.2/8.4; these are folded into the post-Psst `ui_matrix.sh` run and a short manual pass (TODO `FPdv`).
