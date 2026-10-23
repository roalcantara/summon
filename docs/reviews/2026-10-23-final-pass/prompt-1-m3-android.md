# Final pass 1 of 3: Material 3 compliance and Android platform

You are an independent reviewer. You have no prior context on this project. Review it fresh and report gaps against best practices. **Do not change any file.**

## The project

**Summon Demons Safely**: a small native Android game in Kotlin and Jetpack Compose (package `com.summondemons.safely`, min SDK 24, target SDK 37). The player summons chibi Goetic demons by facing a compass direction and chanting a Latin phrase into the microphone. It has a dark-fantasy look and a comedic tone. Release is imminent.

Repository root: `/Users/roalcantara/Work/mydemon/mydemon` (branch `fix/pre-release-ui`, HEAD `121d38d`).

Read these first, in this order (higher wins when they disagree):
1. `PRODUCT.md`: rules and scope
2. `DESIGN.md`: tokens, typography, components, assets
3. `docs/decisions/*.md`: ADRs. In particular 0001 (dark theme only), 0017 (release; §7 large screens, §8 required hardware), 0019/0020/0029 (chant screen), 0021 (intro card), 0031 (i18n, 5 languages), 0033 (attention glow and large-text rules)
4. `docs/reviews/2026-10-22-pre-release-ui-review.md`: the previous review and what it fixed

Code to review: `app/src/main/java/com/summondemons/safely/` (mainly `ui/`, `ui/theme/`, `ui/screens/`, `ui/components/`, `ui/fx/`, `navigation/`, `MainActivity.kt`), `app/src/main/AndroidManifest.xml`, `app/src/main/res/values*/`.

Screenshots (S24, landscape chant screens and portrait elsewhere, font scales 1.0/1.3/2.0): `build/ui-matrix/s24-axis/`. **They predate four later changes:** the intro's rule spacing, the Grimoire's demon pager, the Home hint position and the launcher icon. Judge those four from the code only.

## Deliberate choices: do not report these as gaps

- A **custom dark-fantasy theme** on Material 3 plumbing. It is not meant to look like stock Material: no dynamic color, no light theme (ADR-0001). Check that the theme is used consistently, not that it resembles the M3 baseline.
- Phone layouts only. The app is declared a game (`android:appCategory="game"`) so its orientation locks hold on tablets. Portrait everywhere except the chant screen, which is landscape. No tablet layout for v1.
- Words inside fixed-size art or HUD slots grow at most 1.3× with the font scale (`fontScaleCapped`), then shrink to fit. Body text scales fully (ADR-0033).
- One "attention glow" at a time marks what to act on next (ADR-0033).
- No bottom navigation: a linear Home → Grimoire → Ritual flow.

## Your lens

Load and follow these skills (read each `SKILL.md` and the references it points to):
- `~/.agents/skills/material-3`: use its **"MD3 Compliance Audit"** section as your procedure and report format
- `~/.agents/skills/material-design-3`: note its own rule: do not push Material visuals onto a non-Material design. Use it for **behaviour**: semantics, interaction states, accessibility, adaptive behaviour
- `~/.agents/skills/mobile-android-design`

Cover at least:
1. **Theme discipline:** raw `Color(…)`, `.dp`/`.sp` literals or typography outside `ui/theme/` that should be tokens. Distinguish real gaps from local layout constants.
2. **Components and interaction states:** pressed, focused and disabled states; ripple or indication where it's missing; Material components used correctly.
3. **Accessibility:** contrast of text on its real backgrounds, 48 dp touch targets, content descriptions, roles, heading and live-region semantics, TalkBack reading order, behaviour at font scale 2.0 and the largest display size, reduce motion.
4. **Platform:** edge-to-edge and insets (including display cutout in landscape), predictive and system back, process death and state restoration, configuration changes, permissions UX (microphone), splash screen.
5. **Compose quality:** state hoisting, unnecessary recomposition (animated values read in composition), stability, `remember` and `rememberSaveable` use, previews.
6. **i18n:** hard-coded player text, layout robustness for longer translations.

## Rules

- **Read-only.** Do not edit, create, delete, commit or format files. Do not run `installDebug`, `adb` or anything touching a device. Reading files, `grep` and `git log`/`git show` are fine.
- **Verify every finding in the code.** Quote the file and line. If you can't point to the line, don't report it.
- No speculation about runtime behaviour you can't infer from the code. If something needs a device to confirm, list it under "Needs device check".

## Output

Write your report to `docs/reviews/2026-10-23-final-pass/report-1-m3-android.md` (the only file you may create), in this format:

```markdown
# Report 1: M3 compliance and Android platform

## Summary
<3–5 lines: overall state, biggest risks>

## Findings
| # | Severity | Area | File:line | Rule broken (skill › section) | Problem | Fix | Effort |
|---|---|---|---|---|---|---|---|
Severity: Blocker (must fix before release) / Should-fix / Nice-to-have. Effort: S (<30 min) / M (<2 h) / L.

## Deliberately different, not a gap
<things the skills recommend that this app rightly does differently, with the ADR>

## Needs device check
<items you could not confirm from code>

## Checked and fine
<areas you reviewed with no findings>
```
