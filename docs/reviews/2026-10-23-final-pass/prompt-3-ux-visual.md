# Final pass 3 of 3: UX and visual design

You are an independent reviewer. You have no prior context on this project. Review it fresh and report gaps against best practices. **Do not change any file.**

## The project

**Summon Demons Safely**: a small native Android game in Kotlin and Jetpack Compose (package `com.summondemons.safely`). The player summons chibi Goetic demons by facing a compass direction and chanting a Latin phrase into the microphone. One summon per day; bound demons get restless and escape when neglected. It has a dark-fantasy look and a comedic tone. Release is imminent.

Repository root: `/Users/roalcantara/Work/mydemon/mydemon` (branch `fix/pre-release-ui`, HEAD `121d38d`).

Read these first, in this order (higher wins when they disagree):
1. `PRODUCT.md`: rules, screens, scope
2. `DESIGN.md`: tokens, typography, spacing, components, assets
3. `docs/decisions/*.md`: ADRs. In particular 0014 (Home), 0016 (Grimoire card), 0021 (intro card), 0029 (chant screen), 0033 (attention glow and large-text rules)
4. `docs/reviews/2026-10-22-pre-release-ui-review.md` and the specs in `docs/superpowers/specs/2026-10-2*.md`: what was already reviewed and decided

Screens and code:
- Home: `ui/screens/HomeScreen.kt`
- Grimoire (demon pages, tarot card with a dossier back, celebration): `ui/screens/GrimoireScreen.kt`, `ui/components/DemonCard.kt`, `Dossier.kt`, `Badges.kt`, `QuoteSlot.kt`
- Intro card: `ui/screens/ChantIntro.kt`
- Chant screen (landscape): `ui/screens/ChantScreen.kt`, `ChantArrangements.kt`, `ChantPieces.kt`, `SigilStack.kt`; ritual states and errors in `RitualScreen.kt`
- Settings: `ui/components/SettingsSheet.kt`
- Strings: `app/src/main/res/values/strings.xml` (English; four translations alongside)
(Paths are under `app/src/main/java/com/summondemons/safely/`.)

Screenshots (S24, font scales 1.0/1.3/2.0, two densities): `build/ui-matrix/s24-axis/`. **They predate four later changes:** the intro's rule spacing, the Grimoire's demon pager, the Home hint position and the launcher icon (`docs/play-store/icon-512.png` is current). Judge those four from the code.

## Deliberate choices: do not report these as gaps

- A dark-only, custom dark-fantasy theme (ADR-0001); per-demon accent colours are demon data (ADR-0006).
- A linear Home → Grimoire → Ritual flow, without bottom navigation.
- Phone layouts only (portrait, chant screen landscape); no tablet layout for v1.
- The intro card shows before every ritual, by design (ADR-0021).
- One attention glow at a time (ADR-0033).
- Fixed-size art and HUD words cap at 1.3× font scale and shrink to fit; body text scales fully.

## Your lens

Load and follow `~/.agents/skills/mobile-app-ui-design` (and its `references/industry-conventions.md`, gaming and emotional design parts). Cross-check spacing and type against `DESIGN.md`.

Cover at least:
1. **Per-screen hierarchy:** what the eye sees first, whether it's the right thing; primary action placement and reach.
2. **Spacing system:** 8/4 dp grid, relationship-based spacing, consistency across screens; one-off values.
3. **Typography and colour:** number of sizes and weights per screen, contrast hierarchy, accent use (60/30/10).
4. **Consistency across screens:** shared components behave and look the same (chips, glows, buttons, headers).
5. **States:** first run, empty (no demon bound), cooling, restless, escaped, celebration, errors (mic denied or unavailable, no compass), loading. Each one guides the player.
6. **Emotional design:** the peak (summon climax, celebration, secret) and the end; feedback for success and failure.
7. **Copy:** clarity and tone of player-facing strings (English), length risks for translations.
8. **Onboarding:** can a first-time player understand how to summon from the screens alone?

## Rules

- **Read-only.** Do not edit, create, delete, commit or format files. Do not run `installDebug`, `adb` or anything touching a device. Reading files, viewing screenshots, `grep` and `git log`/`git show` are fine.
- **Verify every finding** in the code or a screenshot. Quote the file and line, or the screenshot path. Unverifiable impressions go under "Needs device check".

## Output

Write your report to `docs/reviews/2026-10-23-final-pass/report-3-ux-visual.md` (the only file you may create), in this format:

```markdown
# Report 3: UX and visual design

## Summary
<3–5 lines>

## Findings
| # | Severity | Screen | File:line or screenshot | Rule broken (skill › section) | Problem | Fix | Effort |
|---|---|---|---|---|---|---|---|
Severity: Blocker (must fix before release) / Should-fix / Nice-to-have. Effort: S (<30 min) / M (<2 h) / L.

## Deliberately different, not a gap

## Needs device check

## Checked and fine
```
