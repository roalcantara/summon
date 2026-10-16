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
| `docs/PRIVACY_POLICY.md`, `docs/STORE_LISTING.md` | Privacy policy and Play Store listing drafts (release prep: `docs/superpowers/plans/play-store-readiness.md`) |
| `reference/prompts/` | The two original Stitch prompts, verbatim |
| `reference/stitch/` | Stitch mockups, character art, logo, sigil |
| `reference/src/` | Draft Compose code (input for the port; does not compile as-is) |
| `tools/` | Small generators (`gen_audio.py` for `res/raw/*.wav`, `sigil_to_raw.py` for sigils) |
| `art/sigils/` | Stroke-only sigil sources (SVG), converted by `tools/sigil_to_raw.py` |

## Commands

```bash
./gradlew assembleDebug        # build
./gradlew testDebugUnitTest    # unit tests
./gradlew installDebug         # install on a connected device/emulator
```

The interactive web prototype for the screen-system pass lives in
`roalcantara/mydemon`, branch `claude/cool-rubin-pylwkn` (not part of this repo, see
ADR-0004 and its 2026-10-16 amendment):

```bash
git clone -b claude/cool-rubin-pylwkn https://github.com/roalcantara/mydemon ../mydemon
cd ../mydemon && npm install && npm run dev   # http://localhost:3000
```

## Current state

- `app/` is the Compose port (package `com.summondemons.safely`): Home → Grimoire →
  Ritual, one summon per local day, restless/escaped demons, a static demon card, the
  focus-clock ritual, persistent progress (Preferences DataStore). Design:
  `docs/superpowers/specs/2026-10-16-screen-system-design.md` (builds on
  `2026-10-15-ritual-2-design.md`).
- Packages: `data/` (demons, Houses, Callings), `progress/` (pure progress rules, JSON
  model, court hours, `ProgressStore`), `time/` (`GameClock`, debug settings), `ritual/`,
  `platform/` (Android implementations), `ui/`, `navigation/`.
- Open items: the "LOVIS" question in ADR-0005; per-device tuning of the voice
  thresholds (ADR-0008); Stitch art for the DESIGN.md Gaps.

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
