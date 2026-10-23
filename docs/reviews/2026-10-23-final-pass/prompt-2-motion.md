# Final pass 2 of 3: Motion

You are an independent reviewer. You have no prior context on this project. Review it fresh and report gaps against best practices. **Do not change any file.**

## The project

**Summon Demons Safely**: a small native Android game in Kotlin and Jetpack Compose (package `com.summondemons.safely`). The player summons chibi Goetic demons by facing a compass direction and chanting a Latin phrase into the microphone. It has a dark-fantasy look and a comedic tone. Release is imminent.

Repository root: `/Users/roalcantara/Work/mydemon/mydemon` (branch `fix/pre-release-ui`, HEAD `121d38d`).

Read these first, in this order (higher wins when they disagree):
1. `PRODUCT.md`: rules and scope
2. `DESIGN.md`: tokens, components, motion notes
3. `docs/decisions/*.md`: ADRs. In particular 0010 (focus clock), 0016 (Grimoire card and demons as pages), 0019/0020/0029 (chant screen), 0033 (attention glow: one thing glows at a time)

Code to review: everything that animates in `app/src/main/java/com/summondemons/safely/ui/`. Start with:
- `ui/fx/`: `AttentionGlow.kt`, `ReduceMotion.kt`, `InkedSigil.kt`, `Jitter.kt`, `ParticleField.kt`, `HeatShimmer.kt`, `GyroParallax.kt`, `SharedDemonArt.kt`
- `ui/components/`: `FlipCard.kt`, `Dossier.kt` (ink-in), `QuoteSlot.kt`, `RestlessGrimoire.kt`, `RotatingSunburstBackground.kt`, `CueChip.kt`
- `ui/screens/`: `GrimoireScreen.kt` (demon pager, celebration), `HomeScreen.kt`, `ChantPieces.kt` (sigil, seals, phrase, pill, chevrons), `ChantScreen.kt`, `RitualScreen.kt` (climax), `ChantIntro.kt`
- `navigation/AppNavHost.kt` (screen transitions, shared element)

## Deliberate choices: do not report these as gaps

- A custom dark-fantasy theme, not stock Material visuals. Motion should feel game-like and expressive, within M3 motion principles.
- **One attention cue at a time** (ADR-0033): a breathing glow (1.6 s; 0.8 s while wavering; steady with reduce motion). It marks the dossier button while lore is unread, Summon until the first summon, the sigil while seeking, and the phrase while chanting.
- Demons are pages in a `HorizontalPager` that follows the finger (M3 lateral pattern); the arrows animate it.

## Your lens

Load and follow `~/.agents/skills/material-design-3-motion` (its examples are CSS/JS; apply the **principles** to Compose: springs vs tweens, durations, easing, choreography, purposeful motion). Use `~/.agents/skills/mobile-app-ui-design` for the emotional side (peak and end moments, feedback loops).

Cover at least:
1. **Inventory:** every animation, with file:line, its type (spring or tween), duration or stiffness/damping, easing, trigger and purpose.
2. **Consistency:** similar motions use similar specs; durations sit in a coherent scale; no magic numbers that clash.
3. **Purpose:** each motion explains a change, gives feedback or adds delight at the right moment; nothing moves without a reason. Nothing competes with the one attention cue.
4. **Transitions:** Grimoire pager, card flip, Home → Grimoire → Ritual navigation, the shared demon art, intro → chant, the climax.
5. **Feedback:** seal flare, crack, lock fill, celebration. Proportional to the event? Haptics and sound alignment where the code shows them.
6. **Reduce motion:** every animation respects `LocalReduceMotion`; infinite animations stop or turn steady.
7. **Performance:** animated values read during composition instead of in draw or layout (recomposition every frame); infinite transitions running off-screen; heavy work per frame.

## Rules

- **Read-only.** Do not edit, create, delete, commit or format files. Do not run `installDebug`, `adb` or anything touching a device. Reading files, `grep` and `git log`/`git show` are fine.
- **Verify every finding in the code.** Quote the file and line. If you can't point to the line, don't report it.
- If something needs a device to judge (how a motion feels), list it under "Needs device check" with what to look for.

## Output

Write your report to `docs/reviews/2026-10-23-final-pass/report-2-motion.md` (the only file you may create), in this format:

```markdown
# Report 2: Motion

## Summary
<3–5 lines>

## Motion inventory
| Where (file:line) | What | Spec (type, duration/spring, easing) | Purpose | Reduce motion? |
|---|---|---|---|---|

## Findings
| # | Severity | File:line | Rule broken (skill › section) | Problem | Fix | Effort |
|---|---|---|---|---|---|---|
Severity: Blocker (must fix before release) / Should-fix / Nice-to-have. Effort: S (<30 min) / M (<2 h) / L.

## Deliberately different, not a gap

## Needs device check

## Checked and fine
```
