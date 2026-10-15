# AGENTS.md

Entry point for coding agents (and humans) working in this repo.

## What this is

**Summon Demons Safely** — a small native Android game (Kotlin + Jetpack Compose).
The player summons cute chibi Goetic demons by facing each demon's required
direction and chanting its Latin incantation into the microphone. Dark-fantasy RPG
presentation, comedic tone. Full rules: `PRODUCT.md`.

## Source of truth (highest first)

1. `PRODUCT.md` — game rules, demon data, scope and non-goals
2. `DESIGN.md` — tokens, typography, shapes, component contracts, asset catalog
3. `docs/decisions/` — ADRs explaining the decisions above
4. Code in `app/`
5. `reference/` — generated input (prompts, Stitch output, draft code). Never canonical.

If two sources disagree, the higher one wins. Fix the lower one or note it in an ADR.

## Repo map

| Path | Contents |
|---|---|
| `app/` | Android app module |
| `gradle/libs.versions.toml` | Dependency versions |
| `docs/decisions/` | ADRs |
| `docs/superpowers/specs/`, `docs/superpowers/plans/` | Design specs and implementation plans |
| `reference/prompts/` | The two original Stitch prompts, verbatim |
| `reference/stitch/` | Stitch mockups, character art, logo, sigil |
| `reference/src/` | Draft Compose code (input for the port; does not compile as-is) |

## Commands

```bash
./gradlew assembleDebug        # build
./gradlew testDebugUnitTest    # unit tests
./gradlew installDebug         # install on a connected device/emulator
```

The interactive web simulator lives in the sibling repo `../mdweb` (not part of this
repo, see ADR-0004):

```bash
cd ../mdweb && npm install && npm run dev   # http://localhost:3000
```

## Current state

- `app/` is still the Android Studio "Basic Views Activity" template (XML, fragments,
  package `com.example.mydemon`).
- Next step: port `reference/src/` to Compose in `app/`, per a dedicated spec in
  `docs/superpowers/specs/`.

## Conventions

- Kotlin + Jetpack Compose, Modern Android Development guidelines.
- Package / applicationId: `com.summondemons.safely` (ADR-0002).
- Sensor (azimuth) and audio (mic volume) logic sits behind a ViewModel exposing
  `StateFlow`; composables only render state and send events.
- Ritual parameters (heading, chant phrases, accent color) are demon data, never
  constants in UI code (ADR-0006).
- UI reads colors, type, shapes and spacing from the theme only (DESIGN.md).
- Commits: Conventional Commits, e.g. `docs(product): ...`, `feat(ritual): ...`.

## Rules for agents

- Do not edit anything under `reference/`.
- When a product or design decision changes, update `PRODUCT.md` / `DESIGN.md` and
  add an ADR in `docs/decisions/` (next free number).
- Missing art: use the ready-made Stitch prompts in DESIGN.md → "Gaps".
