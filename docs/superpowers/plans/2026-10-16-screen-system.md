# Screen System + Progress Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Turn the three-screen MVP into a small game with a reason to come back: Home → Grimoire → Ritual, one summon per local day, demons that get restless and escape when neglected, a static demon card that replaces Profile, and a focus clock that cracks seals instead of wiping the ritual.

**Architecture:** Pure Kotlin core (`progress/*` rules and JSON model, `CourtHours`, demon data, the focus-clock `RitualEngine`) is JVM-unit-tested. Time comes from an injected `GameClock`; progress lives in a process-wide Preferences DataStore behind `ProgressStore`. An activity-scoped `GameViewModel` owns progress, selection, the celebration and the debug actions, and exposes one `StateFlow<GameUiState>`. `RitualViewModel` gains an `inCourt` provider, a `commitSummon` callback (called inside the tick at `ClimaxStart`), `setActive` and a heading hold. Compose screens (Home, Grimoire, Ritual) only render state and send events.

**Tech Stack:** AGP 9.4.1 (Kotlin 2.2.10), Compose BOM 2026.09.00 + Material 3, navigation-compose 2.10.2, lifecycle 2.11.0, activity-compose 1.13.0, **new:** `androidx.datastore:datastore-preferences`, `org.jetbrains.kotlinx:kotlinx-serialization-json`, `androidx.core:core-splashscreen`, `com.android.tools:desugar_jdk_libs` (core library desugaring for `java.time` on API 24–25). JUnit 4 + kotlinx-coroutines-test 1.11.0. Spec: `docs/superpowers/specs/2026-10-16-screen-system-design.md`. Behavioural reference: the web prototype in `roalcantara/mydemon`, branch `claude/cool-rubin-pylwkn` (`src/engine/*`, `src/screens/*`, `src/data/demons.ts`).

**Working directory:** the repo root. All paths are relative to it. Do not edit anything
under `reference/`.

**Commit trailer:** before starting, set `TRAILER` to the attribution trailer your
session requires (the earlier plans end every commit with a `Co-Authored-By:` line):

```bash
TRAILER='Co-Authored-By: <your agent attribution>'
```

Every commit command below passes it as the second `-m`.

**Path shorthand** (for reading only; commands spell out full paths):
- `MAIN` = `app/src/main/java/com/summondemons/safely`
- `TEST` = `app/src/test/java/com/summondemons/safely`
- `RES` = `app/src/main/res`

**Verification gate** (run at the end of every phase, and wherever a task says "gate"):

```bash
./gradlew testDebugUnitTest lint assembleDebug
```
Expected: `BUILD SUCCESSFUL`. Lint warnings are allowed; lint errors are not.

**Kotlin notes for the implementer**
- If the compiler reports an `@OptIn(...)` as unnecessary or unresolved
  (`ExperimentalSharedTransitionApi`, `ExperimentalMaterial3Api`), delete the `@OptIn`
  line and its import. Nothing else changes.
- UI code reads colors, type, shapes and spacing from `MaterialTheme`, `demonColors`,
  `textRoles` and `spacing` only (DESIGN.md). Constants that are not theme tokens
  (durations, alphas, art aspect ratios, the per-use `name` font sizes the spec fixes)
  live as `private const val` / `private val` at the top of the file that uses them.
  This is the existing pattern.
- Every step that shows a whole file means "create or replace the file with exactly
  this". Steps that show a fragment say where it goes.
- This plan was written without an Android SDK at hand, so the Compose code has not
  been compiled. If a Compose API signature differs in BOM 2026.09.00, keep the
  behaviour and adapt the call; note the change in the task's commit message.

**Prototype-only, never ported:** `visitHintSeen`, `tab` / `sheetOpen`, `FlawlessSeal`,
`effort()` / `difficulty()`, `stats`, jump-to-state and freeze-clock debug tools, the
Hero Wars stand-in images.

---

## File map

| Path | Action | Responsibility |
|---|---|---|
| `docs/decisions/0010-…0016-*.md` | Create | Focus clock, Houses/Callings, Court Hours, persistence/flawless, Home/text roles, daily summon/restless/escape, demon card |
| `docs/decisions/0004-*.md`, `0006-*.md`, `0008-*.md` | Modify | Amendments / partial supersession |
| `PRODUCT.md`, `DESIGN.md`, `AGENTS.md` | Modify | Canon |
| `gradle/libs.versions.toml`, `app/build.gradle.kts` | Modify | New dependencies, desugaring |
| `app/src/main/AndroidManifest.xml` | Modify | `SummonApp`, splash launch theme |
| `RES/values/themes.xml`, `RES/values/strings.xml` | Modify | Splash theme; all new copy |
| `RES/xml/backup_rules.xml`, `RES/xml/data_extraction_rules.xml` | Rewrite | Exclude the debug settings file |
| `MAIN/SummonApp.kt`, `MAIN/AppGraph.kt` | Create | Application + process-wide singletons |
| `MAIN/MainActivity.kt` | Modify | Splash held until progress loads |
| `MAIN/data/Demon.kt`, `House.kt`, `Calling.kt`, `DemonCatalog.kt` | Modify / Create | Demon card data (§8) |
| `MAIN/progress/Progress.kt`, `ProgressJson.kt`, `ProgressRules.kt`, `ProgressConfig.kt`, `CourtHours.kt`, `ProgressStore.kt` | Create | §1, §6 |
| `MAIN/time/GameClock.kt`, `DebugSettings.kt` | Create | Time, debug offset and court override (§3, §7) |
| `MAIN/platform/SystemGameClock.kt`, `DataStoreProgressStore.kt`, `DataStoreDebugSettingsStore.kt` | Create | Android implementations |
| `MAIN/platform/GameSounds.kt` | Create | App-scoped one-shots (replaces `TadaPlayer`) |
| `MAIN/platform/TadaPlayer.kt` | Delete | Replaced by `GameSounds` |
| `MAIN/platform/Haptics.kt`, `RitualSounds.kt`, `RitualFeedback.kt` | Modify | `crack`, volume, renamed effects, bed follows phase |
| `MAIN/ritual/RitualConfig.kt`, `RitualState.kt`, `RitualEngine.kt` | Modify / Rewrite | Focus clock (§2) |
| `MAIN/ritual/RitualViewModel.kt` | Modify | `inCourt`, `commitSummon`, `setActive`, heading hold |
| `MAIN/ritual/HeadingMath.kt` | Modify | `headingLetter` |
| `MAIN/ui/GameViewModel.kt` | Create | §3; replaces `DemonViewModel` |
| `MAIN/ui/DemonViewModel.kt` | Delete | Replaced |
| `MAIN/ui/theme/Color.kt`, `DemonColors.kt`, `Type.kt`, `Theme.kt`, `TextRoles.kt` | Modify / Create | New tokens, text roles |
| `MAIN/ui/fx/FxMath.kt`, `Jitter.kt`, `InkedSigil.kt` | Modify / Create | `keyframe`, jitter, sigil flash |
| `MAIN/ui/components/*` | Create / Modify / Delete | New components (§9); `CompassArc` deleted |
| `MAIN/ui/screens/HomeScreen.kt`, `RitualCue.kt`, `DebugSheet.kt` | Create | Home, cue/header logic, debug sheet |
| `MAIN/ui/screens/GrimoireScreen.kt`, `RitualScreen.kt` | Rewrite / Modify | Stage + card; cue chip, altar ring, chant tablet, abandon dialog |
| `MAIN/ui/screens/ProfileScreen.kt` | Delete | Replaced by the demon card |
| `MAIN/navigation/Routes.kt`, `AppNavHost.kt` | Rewrite | Home / Grimoire / Ritual, guard, celebration |
| `TEST/**` | Create / Rewrite | Unit tests (§11) |

---
## Phase 1 — Canon

### Task 1: ADR-0010…0016 and amendments

**Files:**
- Create: `docs/decisions/0010-focus-clock.md`, `0011-houses-and-callings.md`, `0012-court-hours.md`, `0013-persistence-and-flawless.md`, `0014-home-and-text-roles.md`, `0015-daily-summon-restless-escape.md`, `0016-demon-card-replaces-profile.md`
- Modify: `docs/decisions/0004-web-demo-external.md`, `0006-ritual-heading-per-demon.md`, `0008-ritual-2-rules.md`

- [ ] **Step 1: Check the numbers are free**

```bash
ls docs/decisions/
```
Expected: the highest existing number is `0009`. If any of 0010–0016 exists, stop and
renumber this task's ADRs from the next free number (and every "ADR-00xx" reference
in this plan and in the spec).

- [ ] **Step 2: Write ADR-0010**

`docs/decisions/0010-focus-clock.md`:

```markdown
# 0010 — Focus clock

- Date: 2026-10-16
- Status: Accepted (supersedes the drift grace of ADR-0008)

## Context
Ritual 2.0 (ADR-0008) had no real failure mode: silence paused for free, and drifting
off the heading for 1.5 s threw the player back to Seeking and wiped the current
phrase. Play-testing the web prototype showed that a draining clock gives tension
without ever wiping a whole attempt.

## Decision
- **Budget:** phrases II–IV (index *i* ≥ 1) each get
  `round((3 000 + 500 × syllables[i]) × courtMultiplier)` ms of focus. Phrase I has
  no clock (budget 0).
- **Seal:** lighting every syllable of a phrase seals it (`Sealed(phrase)`); the next
  phrase starts with a full budget. The 4th seal starts the climax and reports
  `ClimaxStart(flawless)`.
- **Drain:** every 20 ms tick while Chanting or Wavering on phrases II–IV: 1 × 20 ms on
  heading (**also in silence**), 3 × 20 ms off heading.
- **Crack:** when focus reaches 0 the last sealed phrase reopens (`Cracked(phrase)`,
  `phrase -= 1`, `lit = 0`) with its own full budget (0 on phrase I), and the attempt
  is no longer flawless.
- **No return to Seeking:** locking happens once per attempt. Leaving the tolerance
  (or an unknown heading) while chanting enters Wavering (`Drift`): voice is ignored and
  the clock drains 3×. Facing the demon again resumes chanting (`Resumed`) with nothing
  lost and no re-lock. On phrase I, Wavering simply waits.
- Effects: `PhraseComplete` → `Sealed`, `DriftWarning` → `Drift`; `PhraseLost` is removed.
- Tuning lives in `RitualConfig`: `FOCUS_BASE_MS = 3 000`, `FOCUS_PER_SYLLABLE_MS = 500`,
  `FOCUS_DRIFT_MULTIPLIER = 3`, `COURT_BONUS = 0.25`, `FOCUS_LOW_FRACTION = 0.25`.
  `DRIFT_GRACE_MS` is removed.

## Consequences
Supersedes ADR-0008's grace period and its "silence pauses without penalty" for
phrases II–IV. The worst case is losing one sealed phrase at a time. The clock pauses
while the Ritual screen is stopped, and after a restart ticks are held until the first
heading reading arrives, so a return from the background never counts as drifting.
```

- [ ] **Step 3: Write ADR-0011**

`docs/decisions/0011-houses-and-callings.md`:

```markdown
# 0011 — Houses, Callings and the demon dossier

- Date: 2026-10-16
- Status: Accepted

## Context
The Profile screen showed only a name, title, description and quote. The prototype
review asked for a card that says at a glance where to face, when the demon is
strongest, what he is for and how mighty he is, plus lore worth unlocking.

## Decision
- Each demon has a **House** (`Sol` or `Venus`). A House carries only what is shown:
  its planet (the badge label), a metal colour token (`house-sol`, `house-venus`), its
  court hours (ADR-0012) and a badge tooltip.
- Each demon has a **Calling**: Warlord, Tutor, Oracle, Treasurer or Menace (name and
  tooltip). Asmodeus is a Tutor, Astaroth an Oracle.
- Each demon has a number, a rank, a legion count and an optional liege.
- The demon card shows four badges: **Direction** (with the liege, or "He swears to no
  king"), **House**, **Calling** and **Legions** ("A {rank} at the head of {n} legions of
  spirits."). The badge rim is the House metal; the glyphs are the demon accent, except the
  House glyph, which is drawn in its metal.
- The **dossier** holds six pages of copy per demon: Weakness, Abilities (exactly 3),
  Offices, Appearance, Summoner's warning and Secret. The copy is demon data
  (`DemonCatalog`, PRODUCT.md "Dossiers").

## Consequences
Adding a demon means adding a House/Calling choice and dossier copy, not code. Stats,
levels and power numbers stay a non-goal (PRODUCT.md).
```

- [ ] **Step 4: Write ADR-0012**

`docs/decisions/0012-court-hours.md`:

```markdown
# 0012 — Court Hours

- Date: 2026-10-16
- Status: Accepted

## Context
Houses (ADR-0011) needed a reason to matter in play. The Goetia ties Kings and Dukes
to planetary hours; a simple, local-time version gives the player a reason to summon at
certain times without punishing them at others.

## Decision
- Sunrise and sunset are fixed at 06:00 and 18:00 local time (`ProgressConfig`).
- Court ranges (start inclusive, end exclusive): **Sol** 09–12 and 15–18; **Venus** 06–12.
- A ritual whose demon's House is in court **at the moment of lock** gets
  `courtMultiplier = 1.25` (`COURT_BONUS`) for every focus budget of that attempt. It is
  fixed at lock and never changes during the attempt.
- Venus' "clear weather only" is flavour text in the tooltip; weather is not read.
- Home shows which Houses are in court now ("In court · Sol & Venus" / "Out of office").

## Consequences
No location, sunrise tables or weather APIs. A debug override (auto / in / out) exists
in debug builds only.
```

- [ ] **Step 5: Write ADR-0013**

`docs/decisions/0013-persistence-and-flawless.md`:

```markdown
# 0013 — Persistence and flawless summons

- Date: 2026-10-16
- Status: Accepted

## Context
Unlock state was in memory only (PRODUCT.md non-goal). A daily summon and neglect
rules need progress that survives restarts. Killing the app during the 2.5 s climax
must not lose a summon or allow a second one.

## Decision
- `Progress` (per-demon `bound`, `revealed`, `flawless`, `lastVisitDay`, one-time
  `revealPending` / `escapeBannerPending` flags; `lastSummonDay`, `homeHintSeen`,
  `lastViewed`) is stored in **Preferences DataStore** (file `progress`), one string key
  `progress_v1` holding kotlinx-serialization JSON (`ignoreUnknownKeys = true`).
- The DataStore is a process-wide singleton. Every change is a `store.update { rules(it) }`,
  so a change always applies to the latest stored value.
- The summon is **committed at `ClimaxStart`**, inside the ritual tick (not through the
  lossy effects flow), with the day the ritual **started**.
- Unreadable or corrupt data starts from `Progress()` (logged, never a crash). A failed
  write is logged and that change is lost.
- A summon with no crack is **flawless**; `flawless` is sticky per demon and unlocks the
  dossier's Secret page.
- Debug settings (day offset, court override) live in a separate DataStore file
  `debug_settings`, read only in debug builds and excluded from backup.
- The splash screen stays up until the first progress value has loaded.

## Consequences
Removes "Persistence across app restarts" from the non-goals. A restored Ritual
destination whose summon is already saved pops straight back to the Grimoire.
```

- [ ] **Step 6: Write ADR-0014**

`docs/decisions/0014-home-and-text-roles.md`:

```markdown
# 0014 — Home, text roles and the chant tablet

- Date: 2026-10-16
- Status: Accepted

## Context
The app opened straight on the Grimoire pager, with no place to show what changed
while the player was away. Ritual text mixed sizes and styles without a system.

## Decision
- **Home — "The Restless Grimoire":** a chained, locked book on an inlaid compass rose.
  It idles, hops every 5–8 s and has a silhouette peek out every 10–15 s; while any
  demon is restless it hops every 2–3 s, peeks every 4–6 s and its lock glows in the
  focus demon's accent. Tapping it opens the Grimoire. It also shows the court chip,
  "{n} of 2 spirits bound", a first-run hint and the escape banner. Until Stitch art
  exists the book is a vector placeholder (DESIGN.md Gaps).
- **Text roles** (DESIGN.md Typography): `cue` (label-medium, +0.08 em, upper case),
  `incantation` (Cinzel 15 sp Bold, line 22 sp, +0.02 em), `meta` (label-small,
  +0.14 em, upper case, `outline`), `name` (Cinzel Bold, size per use), `body`
  (body-medium).
- **Ritual:** a **cue chip** replaces the phase hint; the **altar ring** replaces the
  compass arc; the **chant tablet** replaces the chant stack (one row per phrase in
  `incantation`, I–IV medallions, sealed count or focus seconds in the header, a focus
  bar under the current phrase).

## Consequences
Glyphs that the bundled Latin-subset fonts lack (☉ ♀, compass, swords) are drawn as
vector paths. Compose has no text-transform, so `cue` and `meta` strings are upper-cased
at the call site with `Locale.ROOT`.
```

- [ ] **Step 7: Write ADR-0015**

`docs/decisions/0015-daily-summon-restless-escape.md`:

```markdown
# 0015 — Daily summon, restless and escape

- Date: 2026-10-16
- Status: Accepted

## Context
With two demons the game ended after two rituals. A light daily rhythm and a cost for
neglect give a reason to come back without currencies or timers to grind.

## Decision
- **One summon per local calendar day** across all demons:
  `canSummon = lastSummonDay == null || today > lastSummonDay`. A clock moved backwards
  never unlocks early. A ritual begun at 23:59 that climaxes after midnight spends the
  day it started on.
- **Visit:** viewing a bound demon in the Grimoire is the visit (`lastVisitDay = today`).
  Tapping a bound demon does nothing.
- **Restless:** 2 days without a visit (`RESTLESS_AFTER_DAYS`). He jitters, says his
  restless quote for that viewing, and the Home grimoire gets jumpier.
- **Escape:** 3 days without a visit (`ESCAPE_AFTER_DAYS`). He is unbound again, his
  one-time effects are cleared and an escape banner is queued; `revealed` and
  `flawless` are kept ("lore kept").
- `evaluate` runs after the first load and on every activity start. A visit applies
  `evaluate` first in the same update, so a demon left on screen while the app sat in
  the background for 3+ days escapes instead of being visited.
- Where Home opens the Grimoire: the first restless demon; else, if a summon is
  available, the first unbound one; else the last viewed; else the first.

## Consequences
Days are local epoch days taken from an injected clock, so tests and the debug sheet
can move time. Notifications and reminders stay out of scope.
```

- [ ] **Step 8: Write ADR-0016**

`docs/decisions/0016-demon-card-replaces-profile.md`:

```markdown
# 0016 — The demon card replaces Profile

- Date: 2026-10-16
- Status: Accepted

## Context
Profile duplicated the Grimoire page with a bigger picture, and the success screen
lived there. The prototype review merged both into the Grimoire.

## Decision
- The Grimoire is one column under the avatar rail: a **stage** (the demon, his floor
  circle and a fixed 72 dp quote slot) over a **static demon card** (no expand/collapse):
  header, info band, four badges and a six-page dossier carousel.
- The card is 50 % of the area below the rail; the stage takes the rest. Neither changes
  size between demons or states (at the default font scale; at large font scales the
  card may grow so its minimum content fits).
- Tapping a badge shows its explanation in the info band (5 s, another tap, a tap
  elsewhere, or switching demon brings the description back). The band is as tall as
  the tallest text of every demon and badge.
- The new-summon celebration ("New Demon Received!", sunburst, "Tada!", **Awesome!**)
  plays on the Grimoire stage. There is no Summon / View / Back button: tapping the
  sealed demon or the "Tap to summon" cue starts the ritual.
- Viewing a bound demon is the visit (ADR-0015).

## Consequences
`ProfileScreen`, the `Profile` route and the debug Unsummon on Profile are removed
(Unsummon moves to the debug sheet).
```

- [ ] **Step 9: Amend ADR-0004, ADR-0006 and ADR-0008**

In `docs/decisions/0004-web-demo-external.md`, replace the line `- Status: Accepted` with:

```markdown
- Status: Accepted; amended 2026-10-16 (see below)
```

and append at the end of the file:

```markdown

## Amendment (2026-10-16)
For the screen-system pass (`docs/superpowers/specs/2026-10-16-screen-system-design.md`)
the behavioural reference is the web prototype in `roalcantara/mydemon`, branch
`claude/cool-rubin-pylwkn` (`src/engine/*` mirrors the Kotlin rules). It is still not
kept in sync with the app afterwards.
```

In `docs/decisions/0006-ritual-heading-per-demon.md`, replace `- Status: Accepted` with
`- Status: Accepted; amended 2026-10-16`, and replace the sentence
`Both MVP demons use 0° (North).` with:

```markdown
Asmodeus faces East (90°), serving Amaymon, King of the East; Astaroth faces North (0°).
```

In `docs/decisions/0008-ritual-2-rules.md`, replace `- Status: Accepted (supersedes ADR-0007)` with:

```markdown
- Status: Accepted (supersedes ADR-0007); the grace period and "silence pauses" are superseded by ADR-0010
```

- [ ] **Step 10: Commit**

```bash
git add docs/decisions
git commit -q -m "docs(adr): Record screen system, progress and focus clock (ADR-0010…0016)" -m "$TRAILER"
```

### Task 2: PRODUCT.md, DESIGN.md, AGENTS.md

**Files:**
- Rewrite: `PRODUCT.md`, `DESIGN.md`
- Modify: `AGENTS.md`

- [ ] **Step 1: Rewrite `PRODUCT.md`**

Replace the whole file with the text below. The dossier copy is verbatim from the
prototype's `src/data/demons.ts` (keep the typographic apostrophes and dashes exactly).

````markdown
# PRODUCT — Summon Demons Safely

Canonical product spec. Supersedes `reference/prompts/*` and everything else in
`reference/`. Decisions behind these rules live in `docs/decisions/`.

## Pitch

A small native Android game where the player summons cute chibi Goetic demons by
facing each demon's own direction and chanting a Latin incantation into the
microphone. One summon a day; bound demons get restless and escape when neglected.
Dark-fantasy RPG presentation, comedic tone.

## Core loop

Home → tap the grimoire → Grimoire (one demon at a time) → tap a sealed demon →
Ritual (face the demon's direction + chant) → back to the Grimoire, which celebrates
the new summon. One summon per local calendar day across all demons (ADR-0015).

## Screens and rules

| Topic | Rule |
|---|---|
| Home | "The Restless Grimoire" (ADR-0014): the game logo, a court chip ("In court · Sol & Venus" for the Houses in court now, or "Out of office"), a chained grimoire on an inlaid compass rose, "{n} of 2 spirits bound" and, until first used, the hint "Tap the grimoire". The book bobs, hops and has something peek out now and then; while any demon is restless it does so more often and its lock glows in that demon's accent. Tapping it opens the Grimoire on the demon that most needs attention. |
| Escape banner | If a demon escaped while the player was away, Home shows "Seal broken!" over "{Name} slipped his seal while you were away!" (or "Both spirits slipped their seals while you were away!") and "Tap to dismiss". |
| Grimoire | One column under an avatar rail (← back to Home, then one avatar per demon): a **stage** with the demon over a fixed-height **demon card**. Neither changes size between demons or states. Swiping the stage or tapping an avatar switches demon (ADR-0016). System back returns Home. |
| Lock state | A demon is **unbound** (never summoned, or escaped) or **bound**. Unbound: near-black silhouette with an accent rim glow, drifting smoke, his sigil as the avatar. Bound: full colour in front of his dimmed scene art, his portrait as the avatar (the sigil burns away once after a summon), and his quote under him — his restless quote if he was restless when this viewing began. A demon whose lore is not yet revealed shows smudged dossier pages; an escaped demon is unbound but keeps his lore. |
| Summon | No Summon button: tapping the sealed demon, or the pulsing "Tap to summon" cue under him, starts the Ritual. If today's summon is spent the cue reads "Circle cooling · Next summon in {h} h" (hours to local midnight, at least 1) and shakes when tapped. Tapping a bound demon does nothing. |
| Demon card | Number medallion, name, title; an info band with the description; four badges — Direction, House, Calling, Legions. Tapping a badge shows its explanation in the band until it is tapped again, something else is tapped, the demon changes or 5 s pass. Below, a six-page dossier carousel: Weakness, Abilities, Offices, Appearance, Summoner's warning, Secret (ADR-0011, ADR-0016). The Secret is readable only after a **flawless** summon. |
| Daily summon | One summon per local calendar day across all demons; a ritual started before midnight spends the day it started on (ADR-0015). |
| Neglect | Viewing a bound demon in the Grimoire is the visit. 2 days without a visit make him **restless** (jitter, restless quote, a jumpier Home grimoire); 3 days and he **escapes**: unbound again, lore and flawless kept (ADR-0015). |
| Court Hours | Each demon belongs to a House with court hours (Sol 09–12 and 15–18, Venus 06–12, local time). A ritual that locks while his House is in court gets 25 % more focus for the whole attempt (ADR-0012). |
| Ritual setup | Full-bleed altar: a faint ghost of the demon's sigil inside the altar ring (a marker on the ring shows where his direction lies relative to the phone), a cue chip (e.g. "Turn left · Face East") and the chant tablet. |
| Heading gate | `ritualHeading` is a **per-demon property** (compass degrees, tolerance ±20°). Staying within tolerance for **0.6 s** locks the ritual (haptic + sound), once per attempt. Once locked the ritual never returns to seeking: leaving the tolerance makes it **waver** (red ring, pulsing sigil, voice ignored) until the player faces him again; wavering itself loses nothing (ADR-0010). Nothing may hard-code a direction. |
| Chant | The incantation is **exactly 4 phrases** on the chant tablet: sealed phrases in the demon's colour with a filled medallion, the current one lit syllable by syllable (karaoke), upcoming ones dim. A syllable lights on each voiced syllable onset (≥ 120 ms apart) or after 350 ms of continuous voicing; sounds that aren't voice-like (claps, taps, wind) don't count (ADR-0008). All syllables lit → the phrase is **sealed** (chime + haptic). |
| Focus clock | Phrases II–IV each have a focus clock of 3 s + 0.5 s per syllable (+25 % in court). It drains while the phrase is open — also in silence — and 3× as fast while wavering. When it runs out the last seal **cracks**: that phrase reopens with a full clock (phrase I has no clock). Failure never resets the whole ritual (ADR-0010). |
| Abandon | ✕ or back with no phrase sealed returns to the Grimoire. With a seal it asks "Abandon the ritual? / Your seals will fade." (**Abandon** / **Stay**); the ritual keeps running behind the question. |
| Sigil | Vector line art in the demon's accent that **inks itself** as the chant progresses: ink = (sealed phrases + lit/syllables) / 4. The rings and rim letters ink on lock; the glow follows the live mic level above the voice floor (ADR-0009). A seal flares that phrase's stroke group; a crack drains it in red. |
| Success | After phrase 4: a 2.5 s climax (abort, debug controls and error panels are hidden; touches and back are ignored; screen dims, particles converge, rising sound and haptic crescendo, flash, the demon rises out of the sigil), then back to the Grimoire with that demon selected: "New Demon Received!", rotating sunburst, "Tada!" and **Awesome!**. The demon art carries over as a shared element. The summon is saved when the climax starts. A summon with no crack is **flawless** and unlocks his Secret; **Awesome!** then opens the Secret page. |
| Persistence | Progress survives restarts (ADR-0013). |
| Debug aids | Ritual: "Simulate heading" (snaps to the demon's `ritualHeading`) and "Hold to Chant" (synthetic voiced syllables). A "Debug" chip on Home and Grimoire opens a sheet: advance one day, reset the daily summon, court auto / in / out, bind / bind flawless / unsummon per demon, reset all, and a status line per demon. **Debug builds only.** |
| Orientation | Portrait only (the compass reading assumes it). |
| Permissions | `RECORD_AUDIO` requested on entering the Ritual only if not already granted; if denied, explain why and offer a retry (or a shortcut to app settings once the denial is permanent). |
| Motion | If system animations are off: no particles, parallax, floating, jitter, shimmer, sunburst rotation, grimoire bob/hop/peek, dust puff, pulses or climax flash; the grimoire opening, the avatar burn-away and badge/carousel changes become short fades; the climax keeps its timing but the demon simply fades in. Ink-in, karaoke and the focus bar still update. |

## Demons

| | Asmodeus | Astaroth |
|---|---|---|
| id | `asmodeus` | `astaroth` |
| Number | 32 | 29 |
| Rank | Great King | Mighty Duke |
| Legions | 72 | 40 |
| House | Sol | Venus |
| Liege | Amaymon, King of the East | — (swears to no king) |
| Calling | Tutor | Oracle |
| Title | Lord of Arithmetic & Hidden Wealth | Duke of Past, Present & Future |
| Accent | `demon-asmodeus` (gold, see DESIGN.md) | `demon-astaroth` (magenta, see DESIGN.md) |
| Ritual heading | East (90°, ±20°) | North (0°, ±20°) |
| Quote | "Three heads are better than one—especially when two want your soul!" | "I know your past, your future, and that you didn't read the terms of service." |
| Restless quote | "I've counted every grain of dust in here. Twice. In three bases." | "I foresaw you'd forget me. I hate being right. No I don't." |
| Description | Teaches arithmetic, astronomy, geometry, and invisibility. Answers all questions—if you can stand the smugness. | Knows every secret and every tense. Will explain his own fall at length if asked. |

The description "invisibility" fixes an earlier typo ("invincibility"); the Goetia
credits Asmodeus with making men invisible.

### Houses and Callings

| House | Planet (badge) | Court (local) | Metal token | Tooltip |
|---|---|---|---|---|
| Sol | Sol | 09–12 and 15–18 | `house-sol` | Kings of Sol hold court 9 to noon and 3 to sunset. Bring sunglasses. |
| Venus | Venus | 06–12 | `house-venus` | Dukes of Venus hold court from sunrise to noon, in clear weather only. Very particular. |

| Calling | Tooltip |
|---|---|
| Warlord | Summon him for an army. |
| Tutor | Summon him to learn things. |
| Oracle | Summon him to know things. |
| Treasurer | Summon him for loot. |
| Menace | Summon him at your own risk. |

### Dossiers

**Asmodeus**
- Appearance: Three heads (bull, man and ram), so every meeting is a committee. He breathes fire, swishes a serpent’s tail and waddles on goose feet. He arrives on a dragon with a lance and a banner, because subtle is for Dukes.
- Offices: Hands out the Ring of Virtues (one per customer). · Tutors arithmetic, astronomy, geometry and every handicraft. · Answers any question fully, with appendices. · Makes you invisible. · Knows where treasure lies, and guards it.
- Summoner's warning: Summon him outdoors, stand the whole time, and take your hat off. Keep it on and his boss Amaymon pranks your entire ritual.
- Abilities: **Ring of Virtues** — Gives you a ring. The virtues are sold separately. · **Now You Don’t** — Turns you invisible. Great for heists, terrible for selfies. · **Dragon’s Ledger** — Finds buried treasure, counts it, then sits on it.
- Weakness: **Name-Drop** — Ask “Art thou Asmodeus?” and he must admit it and bow to the floor. Three heads, one ego, zero chill.
- Secret: In an older tale, the smoke of a fish’s heart and liver sent him fleeing all the way to Egypt. He does not eat seafood. He does not discuss it.

**Astaroth**
- Appearance: An angel with bad intentions on a dragon-ish beast, holding a viper in his right hand like a fancy pen. His breath arrives before he does.
- Offices: True answers about past, present and future. · Uncovers every secret, including the ones written in pencil. · Explains how the spirits fell, and why his own fall wasn’t his fault. · Makes you a scholar of all the liberal arts.
- Summoner's warning: Never let him get close; his breath is a weapon. Hold your magic ring up to your face like a tiny gas mask and nod politely.
- Abilities: **All Three Tenses** — He’s read ahead and won’t stop hinting. · **Open Secret** — Every secret you have, now his favourite trivia. · **The Fall (Director’s Cut)** — A complete, unrequested account. Spoiler: someone else pushed him.
- Weakness: **Dragon Breath** — Brave summoners hold a ring to their nose and win. He respects personal space only when it’s enforced with jewellery.
- Secret: Before the dragon and the breath, his name belonged to Astarte, a goddess of love and war. He’d rather you didn’t bring it up.

### Chants (4 phrases each)

**Asmodeus**
1. DAEMONIUM RESIDENTE,
2. OSTENDE MIHI FACIEM TUAM POTENTIAM, VENIT AD ME.
3. DONA MIHI PER FOEDUS QUOD EST IRE AD PRAESENS SUM VOBIS,
4. UT VOTA OMNIA OPUS EST.

**Astaroth**
1. TUI GRATIA LOVIS
2. GRATIA SIT CURA.
3. HIC EST ME DIGNUS LEGI
4. SINT BENE OBEDIENTES.

"LOVIS" is kept verbatim from the original prompt; see ADR-0005.

## Non-goals (MVP)

- More than two demons; the roster rail from the mockups.
- Levels, power, stars, stats, synergies.
- Currencies, bottom navigation, reagents.
- Containment-breach / failure screen.
- Word or speech recognition (only loudness, pitch periodicity and syllable onsets are measured).
- Gemini / AI features.
- More than one summon per day; notifications or reminders; cloud sync.
- Weather (Venus' "clear weather only" is flavour text).
````

- [ ] **Step 2: Rewrite `DESIGN.md`**

Replace the whole file with:

````markdown
---
name: Summon Demons Safely
source: reference/stitch/summoner_s_grimoire_tactical_rpg/DESIGN.md
colors:
  background: '#0b1326'
  on-background: '#dae2fd'
  surface: '#0b1326'
  surface-dim: '#0b1326'
  surface-bright: '#31394d'
  surface-container-lowest: '#060e20'
  surface-container-low: '#131b2e'
  surface-container: '#171f33'
  surface-container-high: '#222a3d'
  surface-container-highest: '#2d3449'
  surface-variant: '#2d3449'
  on-surface: '#dae2fd'
  on-surface-variant: '#d8c3ad'
  inverse-surface: '#dae2fd'
  inverse-on-surface: '#283044'
  outline: '#a08e7a'
  outline-variant: '#534434'
  surface-tint: '#ffb95f'
  primary: '#ffc174'
  on-primary: '#472a00'
  primary-container: '#f59e0b'
  on-primary-container: '#613b00'
  inverse-primary: '#855300'
  secondary: '#7bd0ff'
  on-secondary: '#00354a'
  secondary-container: '#00a6e0'
  on-secondary-container: '#00374d'
  tertiary: '#fcb4ff'
  on-tertiary: '#580065'
  tertiary-container: '#f487ff'
  on-tertiary-container: '#770088'
  error: '#ffb4ab'
  on-error: '#690005'
  error-container: '#93000a'
  on-error-container: '#ffdad6'
  demon-asmodeus: '#ffd54f'
  demon-astaroth: '#e040fb'
  ritual-safe: '#4ade80'
  house-sol: '#f2c14e'
  house-venus: '#c87a3c'
  ink-highlight: '#fff6d0'
  stage-home-glow: '#3a2a55'
  stage-home-mid: '#1a1530'
  stage-sol-glow: '#6b4a12'
  stage-sol-mid: '#2a1d0b'
  stage-venus-glow: '#3b2a4a'
  stage-venus-mid: '#2a1a26'
typography:
  display: { family: Cinzel, size: 32sp, weight: 700, line: 40sp }
  headline-large: { family: Cinzel, size: 24sp, weight: 700, line: 32sp }
  headline-medium: { family: Cinzel, size: 20sp, weight: 700, line: 28sp }
  title: { family: Cinzel, size: 18sp, weight: 600, line: 24sp }
  body-large: { family: Inter, size: 16sp, weight: 500, line: 24sp }
  body-medium: { family: Inter, size: 14sp, weight: 400, line: 20sp }
  body-small: { family: Inter, size: 12sp, weight: 400, line: 16sp }
  label-medium: { family: Inter, size: 12sp, weight: 600, line: 16sp }
  label-small: { family: Inter, size: 10sp, weight: 700, line: 12sp }
shapes:
  small: 12dp
  medium: 16dp
  large: 24dp
  full: pill
spacing:
  xs: 4dp
  sm: 8dp
  md: 12dp
  lg: 16dp
  xl: 24dp
---

# DESIGN — Summon Demons Safely

Canonical design system. Product rules are in `PRODUCT.md`; the rationale for the
dark theme is ADR-0001.

## Direction

Dark tactical RPG chrome (obsidian navy surfaces, imperial gold, arcane cyan,
infernal magenta) carrying cute, comedic chibi demon art. The cuteness lives in the
characters and copy; the interface stays dark and glowing.

## Color → Compose mapping

Each Material `colors` key in the frontmatter maps 1:1 to the `darkColorScheme` slot of
the same name in camelCase (`surface-container-high` → `surfaceContainerHigh`). The app
ships a dark scheme only. Extras that have no Material slot are exposed as a
`DemonColors` object provided through a `CompositionLocal`:

| Token | Use |
|---|---|
| `demon-asmodeus` | Asmodeus accent: stage particles, highlighted chant phrase, sigil, badges |
| `demon-astaroth` | Astaroth accent: same roles |
| `ritual-safe` | Altar ring locked / aligned, safe cue chip |
| `house-sol` | Sol metal: badge rims, number medallion, Home court chip |
| `house-venus` | Venus metal: same roles |
| `ink-highlight` | Hot core of flashes: focus-bar court shimmer, sealed medallion, sigil flare, grimoire opening burst |
| `stage-home-glow`, `stage-home-mid` | Home background gradient (edge: `surface-container-lowest`) |
| `stage-sol-glow`, `stage-sol-mid` | Sol stage gradient (edge: `background`) |
| `stage-venus-glow`, `stage-venus-mid` | Venus stage gradient (edge: `background`) |

Smoke particles use `on-surface-variant`; Home embers and the compass rose use `primary`.

## Typography

- Cinzel for display, headlines and titles (names, banners, screen titles, sigil rim letters).
- Inter for body and labels; numbers use tabular figures.
- Both are bundled in `res/font/` as static, Latin-subset instances generated from the
  Google Fonts variable fonts (SIL OFL 1.1; licences in `app/src/main/assets/licenses/`).

### Text roles (ADR-0014)

Read through `MaterialTheme.textRoles`. Compose has no text-transform: `cue` and `meta`
strings are upper-cased at the call site with `upper()` (`uppercase(Locale.ROOT)`).

| Role | Style | Used for |
|---|---|---|
| `cue` | `label-medium`, letter spacing 0.08 em, upper case | What to do now: cue chips, "Tap to summon", "Circle cooling" |
| `incantation` | Cinzel 15 sp Bold, line 22 sp, letter spacing 0.02 em | Every chant phrase on the tablet |
| `meta` | `label-small`, letter spacing 0.14 em, upper case, `outline` | Small headers and counters: "Sacred chant", "1 / 6", "2 of 2 spirits bound" |
| `name` | Cinzel Bold; size per use (`title` / `headline-*`, or a size fixed by the screen spec) | Demon names, numbers, badge labels, ability names |
| `body` | `body-medium` | Descriptions, quotes, dossier pages |

## Shapes

12 / 16 / 24 dp rounded corners; buttons are pills.
**Deliberate deviation:** Stitch's own DESIGN.md asks for tight 4px chamfered
corners. We keep the rounder shapes from prompt 2 so the UI stays friendly; the
dark palette carries the RPG feel.

## Rules

- Composables read colors, type, shapes and spacing from `MaterialTheme` (and
  `DemonColors`, `textRoles`) only. No hardcoded colors or ad-hoc dimensions.
- A demon's accent and House metal come from its data, never from a `when (id)` in UI code.
- Motion respects `LocalReduceMotion` (system animator scale 0): no particles,
  parallax, floating, jitter, shimmer, sunburst rotation, grimoire bob/hop/peek, dust
  puff, quote-slot and floor-circle pulses, or climax flash. The grimoire opening keeps
  a 150 ms fade; the avatar burn-away and badge/carousel changes cross-fade. Ink-in,
  karaoke and the focus bar still update.

## Components (contracts)

| Component | Purpose | States / inputs |
|---|---|---|
| `PrimaryActionButton` | Main CTA (**Awesome!**, **Stay**, Allow microphone) | `text`, `onClick`, `enabled`, `tone`; gold container, bold label, soft glow |
| `RibbonBanner` | "New Demon Received!" on the Grimoire celebration; "Seal broken!" on Home | `text`; primary container color |
| `RotatingSunburstBackground` | Rays behind the demon during the Grimoire celebration | `color`, `rotating` |
| `InkedSigil` | The demon's vector sigil | `sigil` (raw), `inkProgress` 0–1, `ringProgress` 0–1, `rimText`, `accent`, `micLevel` (glow), `ghostAlpha`, `flash` (group, color, duration: seal flare / crack drain) |
| `AltarRing` | Ring around the sigil on the Ritual | `azimuth`, `target`, `phase`, `lockFraction`, `accent`; outline track, `ritual-safe` lock fill / locked ring, `error` while wavering, a marker toward the target |
| `CueChip` | One-line cue | `text`, `tone` (neutral / safe / error), `shake` |
| `FocusBar` | Focus left under the current phrase | `fraction`, `tone` (normal / drift / court), `accent`; heartbeat below `FOCUS_LOW_FRACTION` |
| `ChantTablet` | Framed chant panel | `phrases`, `phrase`, `lit`, `accent`, `active`, `header`, `focusFraction`, `focusTone` (normal / drift / court), `flare`, `cracked`, `micLevel`, `wavering`; contains `Medallion`, `KaraokeLine`, `FocusBar` |
| `KaraokeLine` | Current chant phrase | `text`, `lit` syllables, `accent`, `dim`, `micLevel` (newest-syllable glow) |
| `BadgeRow` | Direction / House / Calling / Legions tiles | `demon`, `selected`, `onSelect`; rim = House metal, glyph = accent (House glyph = metal); `BadgeIcon` draws one glyph |
| `InfoBand` | Description or the selected badge's explanation | `demons`, `demon`, `badge`; height of the tallest text |
| `DossierCarousel` | Six dossier pages | `demon`, `record`, `inkCodex`, `inkBattle`, `page`, `onPage`; ‹ › and swipe, wrapping |
| `Smudge` / `InkLine` | Sealed page lines / ink-in of revealed lines | `lines`; `ink`, `index` |
| `QuoteSlot` | Fixed 72 dp slot under the demon | `state` (celebration / bound / ready / cooling), quote, hours, `shake` |
| `SigilAvatar` | Avatar rail item | `demon`, `bound`, `burn`, `restless`, `selected`, `enabled` |
| `FloorCircle` | Accent ellipse under the demon | `accent`, `pulse` |
| `StageBackground` | Gradient stage (Home, Sol, Venus) with a scrim | `stage`, `dim` |
| `CompassRose` | Inlaid rose on the Home floor | — (`primary`) |
| `RestlessGrimoire` | Vector placeholder for the Home book | `restless`, `accent`, `opening`, `onOpen` |
| Glyphs | `DirectionGlyph`, `HouseGlyph`, `CallingGlyph`, `LegionsGlyph` | vector paths in a 24-unit box; `color`, `size` |
| `DemonCutout` | Cut-out art with accent rim glow | `art`, `accent`, `silhouette` 0–1, `alignment` |
| `ParticleField` | Embers / smoke / converging climax sparks | `color`, `style`, `count`, `converge` 0–1 |
| `DebugChip` | Opens the debug sheet (debug builds only) | `onClick` |
| `HeatShimmer` | Rising heat haze | `color`; AGSL on API 33+, breathing gradient below |
| `rememberParallax` | Gyro offset for art layers | max offset; `TYPE_GAME_ROTATION_VECTOR`, none if absent |

## Asset catalog

Sources are under `reference/stitch/`. Target names are resource names; the app
stores them in `res/drawable-nodpi/` as lossy WebP (`cwebp -q 85`, alpha kept) so
large sources are not density-scaled and the APK stays small.

| Drawable | Source | Notes |
|---|---|---|
| `ic_game_logo` | `summon_demons_safely_game_logo/screen.png` | 1024×1024, opaque |
| `avatar_asmodeus` | `hero_wars_style_mobile_rpg_avatar_icon_portrait_of_cute_funny_chibi_demon_1/screen.png` | 1024×1024, opaque; circle-clipped |
| `avatar_astaroth` | `hero_wars_style_mobile_rpg_avatar_icon_portrait_of_cute_funny_chibi_demon/screen.png` | 1024×1024, opaque; circle-clipped |
| `demon_asmodeus_full` | previous `full_body_…_demon_1/screen.png` (git history before commit 3b17683) | 896×1200 scene, already shipped |
| `demon_astaroth_full` | `full_body_…_demon_1/screen.png` | 896×1200 scene |
| `demon_asmodeus_cutout` | `full_body_…_demon_2/screen.png` | 896×1200, alpha |
| `demon_astaroth_cutout` | `full_body_…_demon_3/screen.png` | 896×1200, alpha |

`full_body_…` = `full_body_cute_funny_stylized_2d_cartoon_fantasy_rpg_character_artwork_of_demon`.
Unused references: `the_sacred_magical_sigil_of_*` (superseded by vector sigils, ADR-0009).

### Sigils

| Raw | Source | Licence |
|---|---|---|
| `sigil_asmodeus` | `art/sigils/asmodeus.svg`, traced from Wikimedia Commons `File:Seal_of_Asmodeus.svg` | CC0 |
| `sigil_astaroth` | `art/sigils/astaroth.svg`, traced from Wikimedia Commons `File:Astaroth_Seal.svg` | Public domain (Mathers & Crowley 1904) |

Format: viewBox `0 0 100 100`, glyph within radius 37 of the centre, groups
`ink-1`…`ink-4` in draw order, one contour per `<path>`. Regenerate with
`python3 -I tools/sigil_to_raw.py art/sigils/<id>.svg app/src/main/res/raw/sigil_<id>.txt`.

### Audio

All sounds are original synthesized works (CC0) from `tools/gen_audio.py`, mono
22.05 kHz WAV: `bed_asmodeus`, `bed_astaroth` (8 s seamless loops), `chime_1`…`chime_4`,
`lock`, `riser`, `boom`, `tada`. Regenerate with
`python3 -I tools/gen_audio.py app/src/main/res/raw`. To replace one with a recording,
use a CC0 file of the same name and record its source URL and licence here.
`chime_4` also opens the Home grimoire (50 %); `boom` at 35 % marks a crack.

### Screen mockups (visual reference only)

`the_grimoire_demon_roster_details`, `the_ritual_active_summoning_gameplay`,
`the_ritual_active_chant_resonance`, `the_ritual_astaroth_summoning`,
`the_ritual_summoning_success_manifested`,
`the_ritual_containment_breach_emergency_banish`.
Use them for mood, color and composition. Ignore everything listed under
Non-goals in `PRODUCT.md` (roster rail, stats, currencies, bottom nav, reagents,
breach screen).

## Gaps

Placeholders ship; art can replace them later. Never ship the prototype's Hero Wars
stand-in images.

| Gap | Placeholder | Stitch prompt |
|---|---|---|
| Home hall background (`stage_home`) | `StageBackground(Home)` gradient | "Portrait 9:19.5 mobile game background, a candle-lit stone summoning hall seen from the doorway, an inlaid brass compass rose on the floor, deep navy and violet shadows, warm gold candle glow, empty centre for a book, cute stylised 2D fantasy RPG art, no characters, no text." |
| Sol stage (`stage_sol`) | `StageBackground(Sol)` gradient | "Portrait 9:19.5 mobile game stage background, a sunlit golden throne-room dais with a sun disc behind it, warm amber and gold fading to deep navy at the edges, soft light rays, empty centre for a character, cute stylised 2D fantasy RPG art, no characters, no text." |
| Venus stage (`stage_venus`) | `StageBackground(Venus)` gradient | "Portrait 9:19.5 mobile game stage background, a copper-and-rose dawn garden pavilion, morning mist, dusky violet fading to deep navy at the edges, empty centre for a character, cute stylised 2D fantasy RPG art, no characters, no text." |
| Home grimoire | `RestlessGrimoire` vector | "Square game asset on a transparent background: a thick cute chained grimoire with a glowing round lock, plum leather cover with gold corners, iron chains crossing it, slightly open with a pair of glowing eyes peeking from the pages, stylised 2D fantasy RPG art, no text." |
| Asmodeus scene art | `demon_asmodeus_full` keeps the earlier render | (reuse the Ritual 2.0 scene prompt) |
````

- [ ] **Step 3: Update `AGENTS.md`**

Replace the `## Current state` section body with:

```markdown
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
```

In the `## Commands` section replace the paragraph that starts
`The interactive web simulator lives in the sibling repo` and its code block with:

````markdown
The interactive web prototype for the screen-system pass lives in
`roalcantara/mydemon`, branch `claude/cool-rubin-pylwkn` (not part of this repo, see
ADR-0004 and its 2026-10-16 amendment):

```bash
git clone -b claude/cool-rubin-pylwkn https://github.com/roalcantara/mydemon ../mydemon
cd ../mydemon && npm install && npm run dev   # http://localhost:3000
```
````

- [ ] **Step 4: Commit**

```bash
git add PRODUCT.md DESIGN.md AGENTS.md
git commit -q -m "docs(product): Screen system, daily summon, focus clock and demon card canon" -m "$TRAILER"
```

---
## Phase 2 — Pure core (TDD)

### Task 3: Dependencies and core library desugaring

**Files:**
- Modify: `gradle/libs.versions.toml`, `app/build.gradle.kts`

- [ ] **Step 1: Add the versions and libraries**

In `gradle/libs.versions.toml`, add to `[versions]` (after `coroutinesTest`):

```toml
datastore = "1.1.7"
serializationJson = "1.9.0"
coreSplashscreen = "1.0.1"
desugarJdkLibs = "2.1.5"
```

and to `[libraries]` (after `androidx-lifecycle-runtime-compose`):

```toml
androidx-datastore-preferences = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "datastore" }
kotlinx-serialization-json = { group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version.ref = "serializationJson" }
androidx-core-splashscreen = { group = "androidx.core", name = "core-splashscreen", version.ref = "coreSplashscreen" }
desugar-jdk-libs = { group = "com.android.tools", name = "desugar_jdk_libs", version.ref = "desugarJdkLibs" }
```

These are the newest stable versions known when the plan was written. If
<https://maven.google.com> (datastore, core-splashscreen, desugar_jdk_libs) or Maven
Central (kotlinx-serialization-json) lists a newer **stable** release that still
compiles with Kotlin 2.2.10, use it instead.

- [ ] **Step 2: Wire them into `app/build.gradle.kts`**

Replace the `compileOptions` block with:

```kotlin
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        // java.time (GameClock, progress days) on API 24–25.
        isCoreLibraryDesugaringEnabled = true
    }
```

and in `dependencies`, after `implementation(libs.androidx.lifecycle.runtime.compose)`, add:

```kotlin
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.core.splashscreen)
    coreLibraryDesugaring(libs.desugar.jdk.libs)
```

- [ ] **Step 3: Align kotlinx-serialization with navigation**

navigation-compose already brings `kotlinx-serialization-core`. The JSON artifact must
match it.

Run: `./gradlew :app:dependencies --configuration debugRuntimeClasspath | grep kotlinx-serialization`
Expected: `kotlinx-serialization-core` and `kotlinx-serialization-json` resolve to the
same version. If `core` resolves higher (e.g. `1.9.0 -> 1.10.0`), set
`serializationJson` to that version and run the command again.

- [ ] **Step 4: Build**

Run: `./gradlew assembleDebug testDebugUnitTest`
Expected: `BUILD SUCCESSFUL` (nothing uses the new libraries yet).

- [ ] **Step 5: Commit**

```bash
git add gradle/libs.versions.toml app/build.gradle.kts
git commit -q -m "build: Add DataStore, serialization JSON, splash screen and desugaring" -m "$TRAILER"
```

### Task 4: Demon data — House, Calling, dossier

**Files:**
- Create: `MAIN/data/House.kt`, `MAIN/data/Calling.kt`, `MAIN/progress/ProgressConfig.kt`
- Rewrite: `MAIN/data/Demon.kt`, `MAIN/data/DemonCatalog.kt`, `TEST/data/DemonCatalogTest.kt`
- Modify: `TEST/ritual/RitualViewModelTest.kt`

- [ ] **Step 1: Write the failing catalog test**

`app/src/test/java/com/summondemons/safely/data/DemonCatalogTest.kt`:

```kotlin
package com.summondemons.safely.data

import com.summondemons.safely.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DemonCatalogTest {

    private val asmodeus = DemonCatalog.byId("asmodeus")!!
    private val astaroth = DemonCatalog.byId("astaroth")!!

    @Test
    fun containsAsmodeusAndAstarothWithUniqueIds() {
        assertEquals(listOf("asmodeus", "astaroth"), DemonCatalog.all.map { it.id })
    }

    @Test
    fun everyDemonHasFourPhrasesThreeAbilitiesAndADossier() {
        DemonCatalog.all.forEach { demon ->
            assertEquals(4, demon.chantPhrases.size)
            assertEquals(3, demon.abilities.size)
            assertTrue(demon.offices.isNotEmpty())
            listOf(demon.appearance, demon.warning, demon.secret, demon.weakness.name, demon.weakness.line, demon.restlessQuote)
                .forEach { assertTrue("${demon.id}: blank dossier text", it.isNotBlank()) }
            assertTrue(demon.ritualHeading >= 0f && demon.ritualHeading < 360f)
        }
    }

    @Test
    fun headingsAreEastAndNorth() {
        assertEquals(90f, asmodeus.ritualHeading)
        assertEquals(0f, astaroth.ritualHeading)
    }

    @Test
    fun asmodeusMatchesProductSpec() {
        assertEquals("Lord of Arithmetic & Hidden Wealth", asmodeus.title)
        assertEquals(32, asmodeus.number)
        assertEquals("Great King", asmodeus.rank)
        assertEquals(72, asmodeus.legions)
        assertEquals(House.Sol, asmodeus.house)
        assertEquals("Amaymon, King of the East", asmodeus.liege)
        assertEquals(Calling.Tutor, asmodeus.calling)
        assertEquals(
            "Teaches arithmetic, astronomy, geometry, and invisibility. Answers all questions—if you can stand the smugness.",
            asmodeus.description,
        )
        assertEquals("I've counted every grain of dust in here. Twice. In three bases.", asmodeus.restlessQuote)
        assertEquals(
            listOf(
                "DAEMONIUM RESIDENTE,",
                "OSTENDE MIHI FACIEM TUAM POTENTIAM, VENIT AD ME.",
                "DONA MIHI PER FOEDUS QUOD EST IRE AD PRAESENS SUM VOBIS,",
                "UT VOTA OMNIA OPUS EST.",
            ),
            asmodeus.chantPhrases,
        )
    }

    @Test
    fun astarothMatchesProductSpec() {
        assertEquals("Duke of Past, Present & Future", astaroth.title)
        assertEquals(29, astaroth.number)
        assertEquals("Mighty Duke", astaroth.rank)
        assertEquals(40, astaroth.legions)
        assertEquals(House.Venus, astaroth.house)
        assertNull(astaroth.liege)
        assertEquals(Calling.Oracle, astaroth.calling)
        assertEquals("Knows every secret and every tense. Will explain his own fall at length if asked.", astaroth.description)
        assertEquals(
            "I know your past, your future, and that you didn't read the terms of service.",
            astaroth.quote,
        )
        assertEquals("I foresaw you'd forget me. I hate being right. No I don't.", astaroth.restlessQuote)
        assertEquals(
            listOf("TUI GRATIA LOVIS", "GRATIA SIT CURA.", "HIC EST ME DIGNUS LEGI", "SINT BENE OBEDIENTES."),
            astaroth.chantPhrases,
        )
    }

    @Test
    fun housesHaveTheirPlanetsAndCourtHours() {
        assertEquals("Sol", House.Sol.planet)
        assertEquals(listOf(9..11, 15..17), House.Sol.court)
        assertEquals("Venus", House.Venus.planet)
        assertEquals(listOf(6..11), House.Venus.court)
    }

    @Test
    fun unknownIdIsNull() {
        assertNull(DemonCatalog.byId("baal"))
    }

    @Test
    fun everyDemonHasDistinctArtSigilAndBed() {
        assertEquals(R.raw.sigil_asmodeus, asmodeus.sigil)
        assertEquals(R.raw.sigil_astaroth, astaroth.sigil)
        assertEquals(R.raw.bed_asmodeus, asmodeus.ambientBed)
        assertEquals(R.raw.bed_astaroth, astaroth.ambientBed)
        assertEquals(R.drawable.demon_asmodeus_cutout, asmodeus.cutoutArt)
        assertEquals(R.drawable.demon_astaroth_cutout, astaroth.cutoutArt)
        assertEquals(R.drawable.demon_astaroth_full, astaroth.sceneArt)
        assertEquals(R.drawable.avatar_astaroth, astaroth.avatarArt)
    }
}
```

- [ ] **Step 2: Run it to see it fail**

Run: `./gradlew testDebugUnitTest --tests '*DemonCatalogTest*'`
Expected: compilation fails (`House`, `Calling`, `number`… unresolved).

- [ ] **Step 3: Create `ProgressConfig`**

`app/src/main/java/com/summondemons/safely/progress/ProgressConfig.kt`:

```kotlin
package com.summondemons.safely.progress

/** Daily summon, neglect and Court Hours tuning (ADR-0012, ADR-0015). Days are local calendar days. */
object ProgressConfig {
    const val RESTLESS_AFTER_DAYS = 2L
    const val ESCAPE_AFTER_DAYS = 3L

    /** Fixed local sunrise / sunset for Court Hours; weather and location are not read. */
    const val SUNRISE_HOUR = 6
    const val SUNSET_HOUR = 18
}
```

- [ ] **Step 4: Create `House` and `Calling`**

`app/src/main/java/com/summondemons/safely/data/House.kt`:

```kotlin
package com.summondemons.safely.data

import com.summondemons.safely.progress.ProgressConfig.SUNRISE_HOUR
import com.summondemons.safely.progress.ProgressConfig.SUNSET_HOUR

/**
 * A demon's House (ADR-0011, ADR-0012). It carries only what is shown: the planet (badge
 * label), the court hours (local hours; each range's end is exclusive) and the badge
 * tooltip. Its metal color is resolved by `DemonColors.metal`.
 */
enum class House(val planet: String, val court: List<IntRange>, val tooltip: String) {
    Sol(
        planet = "Sol",
        court = listOf(9 until 12, 15 until SUNSET_HOUR),
        tooltip = "Kings of Sol hold court 9 to noon and 3 to sunset. Bring sunglasses.",
    ),
    Venus(
        planet = "Venus",
        court = listOf(SUNRISE_HOUR until 12),
        tooltip = "Dukes of Venus hold court from sunrise to noon, in clear weather only. Very particular.",
    ),
}
```

`app/src/main/java/com/summondemons/safely/data/Calling.kt`:

```kotlin
package com.summondemons.safely.data

/** What a demon is summoned for (ADR-0011): the badge label and its explanation. */
enum class Calling(val label: String, val tooltip: String) {
    Warlord("Warlord", "Summon him for an army."),
    Tutor("Tutor", "Summon him to learn things."),
    Oracle("Oracle", "Summon him to know things."),
    Treasurer("Treasurer", "Summon him for loot."),
    Menace("Menace", "Summon him at your own risk."),
}
```

- [ ] **Step 5: Rewrite `Demon`**

`app/src/main/java/com/summondemons/safely/data/Demon.kt`:

```kotlin
package com.summondemons.safely.data

import androidx.annotation.DrawableRes
import androidx.annotation.RawRes

/** Accent token; resolved to a color by `DemonColors.accent` (DESIGN.md). */
enum class DemonAccent { Asmodeus, Astaroth }

/** A named dossier line: an ability or the weakness (ADR-0011). */
data class Ability(val name: String, val line: String)

data class Demon(
    val id: String,
    val name: String,
    val title: String,
    val description: String,
    val quote: String,
    /** Said instead of [quote] for a viewing that began while he was restless (ADR-0015). */
    val restlessQuote: String,
    val accent: DemonAccent,
    /** Goetia number, shown on the card medallion. */
    val number: Int,
    val rank: String,
    val legions: Int,
    val house: House,
    /** The king he serves; null means "He swears to no king". */
    val liege: String?,
    val calling: Calling,
    /** Compass degrees in [0, 360) the player must face (ADR-0006). */
    val ritualHeading: Float,
    /** Exactly 4 phrases (ADR-0005). */
    val chantPhrases: List<String>,
    val appearance: String,
    val offices: List<String>,
    val warning: String,
    /** Exactly 3. */
    val abilities: List<Ability>,
    val weakness: Ability,
    /** Readable only after a flawless summon (ADR-0013). */
    val secret: String,
    /** Full scene with background (dimmed backdrop behind a bound demon on the Grimoire stage). */
    @param:DrawableRes val sceneArt: Int,
    /** Character on a transparent background (Grimoire stage, climax, shared element). */
    @param:DrawableRes val cutoutArt: Int,
    /** Circular portrait (Grimoire avatar rail once bound). */
    @param:DrawableRes val avatarArt: Int,
    /** Stroke data from `tools/sigil_to_raw.py` (ADR-0009). */
    @param:RawRes val sigil: Int,
    /** Looping ambient sound played while the ritual is locked. */
    @param:RawRes val ambientBed: Int,
) {
    init {
        require(chantPhrases.size == 4) { "$id must have exactly 4 chant phrases" }
        require(ritualHeading in 0f..<360f) { "$id ritualHeading must be in [0, 360)" }
        require(abilities.size == 3) { "$id must have exactly 3 abilities" }
        require(offices.isNotEmpty()) { "$id needs at least one office" }
    }
}
```

- [ ] **Step 6: Rewrite `DemonCatalog`**

The dossier copy is verbatim from PRODUCT.md "Dossiers" (typographic apostrophes and
dashes included).

`app/src/main/java/com/summondemons/safely/data/DemonCatalog.kt`:

```kotlin
package com.summondemons.safely.data

import com.summondemons.safely.R

/** The MVP demons. Text is verbatim from PRODUCT.md. */
object DemonCatalog {

    val all: List<Demon> = listOf(
        Demon(
            id = "asmodeus",
            name = "Asmodeus",
            title = "Lord of Arithmetic & Hidden Wealth",
            description = "Teaches arithmetic, astronomy, geometry, and invisibility. " +
                "Answers all questions—if you can stand the smugness.",
            quote = "Three heads are better than one—especially when two want your soul!",
            restlessQuote = "I've counted every grain of dust in here. Twice. In three bases.",
            accent = DemonAccent.Asmodeus,
            number = 32,
            rank = "Great King",
            legions = 72,
            house = House.Sol,
            liege = "Amaymon, King of the East",
            calling = Calling.Tutor,
            ritualHeading = 90f,
            chantPhrases = listOf(
                "DAEMONIUM RESIDENTE,",
                "OSTENDE MIHI FACIEM TUAM POTENTIAM, VENIT AD ME.",
                "DONA MIHI PER FOEDUS QUOD EST IRE AD PRAESENS SUM VOBIS,",
                "UT VOTA OMNIA OPUS EST.",
            ),
            appearance = "Three heads (bull, man and ram), so every meeting is a committee. He breathes fire, " +
                "swishes a serpent’s tail and waddles on goose feet. He arrives on a dragon with a lance and " +
                "a banner, because subtle is for Dukes.",
            offices = listOf(
                "Hands out the Ring of Virtues (one per customer).",
                "Tutors arithmetic, astronomy, geometry and every handicraft.",
                "Answers any question fully, with appendices.",
                "Makes you invisible.",
                "Knows where treasure lies, and guards it.",
            ),
            warning = "Summon him outdoors, stand the whole time, and take your hat off. Keep it on and his " +
                "boss Amaymon pranks your entire ritual.",
            abilities = listOf(
                Ability("Ring of Virtues", "Gives you a ring. The virtues are sold separately."),
                Ability("Now You Don’t", "Turns you invisible. Great for heists, terrible for selfies."),
                Ability("Dragon’s Ledger", "Finds buried treasure, counts it, then sits on it."),
            ),
            weakness = Ability(
                "Name-Drop",
                "Ask “Art thou Asmodeus?” and he must admit it and bow to the floor. Three heads, one ego, zero chill.",
            ),
            secret = "In an older tale, the smoke of a fish’s heart and liver sent him fleeing all the way to " +
                "Egypt. He does not eat seafood. He does not discuss it.",
            sceneArt = R.drawable.demon_asmodeus_full,
            cutoutArt = R.drawable.demon_asmodeus_cutout,
            avatarArt = R.drawable.avatar_asmodeus,
            sigil = R.raw.sigil_asmodeus,
            ambientBed = R.raw.bed_asmodeus,
        ),
        Demon(
            id = "astaroth",
            name = "Astaroth",
            title = "Duke of Past, Present & Future",
            description = "Knows every secret and every tense. Will explain his own fall at length if asked.",
            quote = "I know your past, your future, and that you didn't read the terms of service.",
            restlessQuote = "I foresaw you'd forget me. I hate being right. No I don't.",
            accent = DemonAccent.Astaroth,
            number = 29,
            rank = "Mighty Duke",
            legions = 40,
            house = House.Venus,
            liege = null,
            calling = Calling.Oracle,
            ritualHeading = 0f,
            chantPhrases = listOf(
                "TUI GRATIA LOVIS", // verbatim, see ADR-0005
                "GRATIA SIT CURA.",
                "HIC EST ME DIGNUS LEGI",
                "SINT BENE OBEDIENTES.",
            ),
            appearance = "An angel with bad intentions on a dragon-ish beast, holding a viper in his right hand " +
                "like a fancy pen. His breath arrives before he does.",
            offices = listOf(
                "True answers about past, present and future.",
                "Uncovers every secret, including the ones written in pencil.",
                "Explains how the spirits fell, and why his own fall wasn’t his fault.",
                "Makes you a scholar of all the liberal arts.",
            ),
            warning = "Never let him get close; his breath is a weapon. Hold your magic ring up to your face " +
                "like a tiny gas mask and nod politely.",
            abilities = listOf(
                Ability("All Three Tenses", "He’s read ahead and won’t stop hinting."),
                Ability("Open Secret", "Every secret you have, now his favourite trivia."),
                Ability("The Fall (Director’s Cut)", "A complete, unrequested account. Spoiler: someone else pushed him."),
            ),
            weakness = Ability(
                "Dragon Breath",
                "Brave summoners hold a ring to their nose and win. He respects personal space only when it’s " +
                    "enforced with jewellery.",
            ),
            secret = "Before the dragon and the breath, his name belonged to Astarte, a goddess of love and war. " +
                "He’d rather you didn’t bring it up.",
            sceneArt = R.drawable.demon_astaroth_full,
            cutoutArt = R.drawable.demon_astaroth_cutout,
            avatarArt = R.drawable.avatar_astaroth,
            sigil = R.raw.sigil_astaroth,
            ambientBed = R.raw.bed_astaroth,
        ),
    )

    val ids: List<String> = all.map { it.id }

    fun byId(id: String): Demon? = all.find { it.id == id }
}
```

- [ ] **Step 7: Keep the drift test on Asmodeus' new heading**

Asmodeus now faces East, so a reading of 0° is off heading. In
`app/src/test/java/com/summondemons/safely/ritual/RitualViewModelTest.kt`, in
`driftLosesTheCurrentPhraseOnlyAfterTheGrace`, replace `heading.readings.emit(0f)` with
`heading.readings.emit(demon.ritualHeading)` and `heading.readings.emit(90f)` with
`heading.readings.emit(demon.ritualHeading + 90f)`. (Task 8 replaces this test.)

- [ ] **Step 8: Run the tests**

Run: `./gradlew testDebugUnitTest`
Expected: `BUILD SUCCESSFUL`; `DemonCatalogTest` 8 tests pass, nothing else regresses.

- [ ] **Step 9: Commit**

```bash
git add app/src/main/java/com/summondemons/safely/data app/src/main/java/com/summondemons/safely/progress app/src/test
git commit -q -m "feat(data): Add Houses, Callings, dossiers; Asmodeus faces East" -m "$TRAILER"
```

### Task 5: Progress model and JSON

**Files:**
- Create: `MAIN/progress/Progress.kt`, `MAIN/progress/ProgressJson.kt`, `TEST/progress/ProgressJsonTest.kt`

- [ ] **Step 1: Write the failing test**

`app/src/test/java/com/summondemons/safely/progress/ProgressJsonTest.kt`:

```kotlin
package com.summondemons.safely.progress

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressJsonTest {

    private val full = Progress(
        demons = mapOf(
            "asmodeus" to DemonRecord(
                bound = true,
                revealed = true,
                flawless = true,
                lastVisitDay = 20_000L,
                revealPending = RevealPending(avatar = true, codex = false, battle = true),
                escapeBannerPending = false,
            ),
            "astaroth" to DemonRecord(revealed = true, escapeBannerPending = true),
        ),
        lastSummonDay = 20_001L,
        homeHintSeen = true,
        lastViewed = "astaroth",
    )

    @Test
    fun roundTripKeepsEveryField() {
        assertEquals(full, ProgressJson.decode(ProgressJson.encode(full)))
    }

    @Test
    fun emptyProgressRoundTrips() {
        assertEquals(Progress(), ProgressJson.decode(ProgressJson.encode(Progress())))
    }

    @Test
    fun unknownKeysAreIgnored() {
        val json = """{"demons":{"asmodeus":{"bound":true,"visitHintSeen":true}},"lastSummonDay":5,"stats":{"might":5}}"""
        val progress = ProgressJson.decode(json)
        assertTrue(progress.record("asmodeus").bound)
        assertEquals(5L, progress.lastSummonDay)
    }

    @Test
    fun corruptOrMissingJsonStartsFresh() {
        assertNull(ProgressJson.decodeOrNull("{nope"))
        assertNull(ProgressJson.decodeOrNull("""{"lastSummonDay":"soon"}"""))
        assertEquals(Progress(), ProgressJson.decode("{nope"))
        assertEquals(Progress(), ProgressJson.decode(null))
    }

    @Test
    fun aMissingRecordIsTheDefault() {
        assertEquals(DemonRecord(), Progress().record("baal"))
    }
}
```

- [ ] **Step 2: Run it to see it fail**

Run: `./gradlew testDebugUnitTest --tests '*ProgressJsonTest*'`
Expected: compilation fails (`Progress`, `ProgressJson` unresolved).

- [ ] **Step 3: Write the model**

`app/src/main/java/com/summondemons/safely/progress/Progress.kt`:

```kotlin
package com.summondemons.safely.progress

import kotlinx.serialization.Serializable

/** One-time effects still to play after a summon (ADR-0013): avatar burn-away, dossier ink-in. */
@Serializable
data class RevealPending(
    val avatar: Boolean = false,
    /** Offices, Appearance and Summoner's warning pages. */
    val codex: Boolean = false,
    /** Weakness and Abilities pages. */
    val battle: Boolean = false,
) {
    companion object {
        val All = RevealPending(avatar = true, codex = true, battle = true)
    }
}

/** Which one-time effect to clear (`ProgressRules.consumeReveal`). */
enum class Reveal { Avatar, Codex, Battle }

@Serializable
data class DemonRecord(
    /** Summoned and not escaped. Drives the stage (silhouette, summon cue, avatar sigil). */
    val bound: Boolean = false,
    /** Lore unlocked; stays true after an escape. Drives the dossier (smudged pages). */
    val revealed: Boolean = false,
    /** Ever summoned without a crack (sticky); unlocks the Secret page. */
    val flawless: Boolean = false,
    /** Local epoch day of the last visit while bound. */
    val lastVisitDay: Long? = null,
    val revealPending: RevealPending = RevealPending(),
    val escapeBannerPending: Boolean = false,
)

/** Everything that survives a restart (ADR-0013). Days are local epoch days. */
@Serializable
data class Progress(
    /** A missing id means `DemonRecord()`. */
    val demons: Map<String, DemonRecord> = emptyMap(),
    val lastSummonDay: Long? = null,
    val homeHintSeen: Boolean = false,
    val lastViewed: String? = null,
) {
    fun record(id: String): DemonRecord = demons[id] ?: DemonRecord()
}
```

- [ ] **Step 4: Write the JSON codec**

`app/src/main/java/com/summondemons/safely/progress/ProgressJson.kt`:

```kotlin
package com.summondemons.safely.progress

import kotlinx.serialization.json.Json

/** `Progress` ⇄ the JSON string stored under `progress_v1` (ADR-0013). Pure: no Android types. */
object ProgressJson {
    private val json = Json { ignoreUnknownKeys = true }

    fun encode(progress: Progress): String = json.encodeToString(Progress.serializer(), progress)

    /** Null when [text] is not a readable `Progress`. */
    fun decodeOrNull(text: String): Progress? =
        try {
            json.decodeFromString(Progress.serializer(), text)
        } catch (e: IllegalArgumentException) {
            // SerializationException is an IllegalArgumentException.
            null
        }

    /** Missing, unreadable or corrupt data starts from `Progress()`. */
    fun decode(text: String?): Progress = text?.let(::decodeOrNull) ?: Progress()
}
```

- [ ] **Step 5: Run the tests**

Run: `./gradlew testDebugUnitTest --tests '*ProgressJsonTest*'`
Expected: 5 tests pass.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/summondemons/safely/progress app/src/test/java/com/summondemons/safely/progress
git commit -q -m "feat(progress): Add serializable progress model and JSON codec" -m "$TRAILER"
```

### Task 6: ProgressRules

**Files:**
- Create: `MAIN/progress/ProgressRules.kt`, `TEST/progress/ProgressRulesTest.kt`

- [ ] **Step 1: Write the failing test** (ports `src/engine/progress.test.ts` case for case, plus the edges the spec names)

`app/src/test/java/com/summondemons/safely/progress/ProgressRulesTest.kt`:

```kotlin
package com.summondemons.safely.progress

import com.summondemons.safely.progress.ProgressRules.canSummon
import com.summondemons.safely.progress.ProgressRules.commitSummon
import com.summondemons.safely.progress.ProgressRules.consumeReveal
import com.summondemons.safely.progress.ProgressRules.daysSinceVisit
import com.summondemons.safely.progress.ProgressRules.dismissEscapeBanners
import com.summondemons.safely.progress.ProgressRules.evaluate
import com.summondemons.safely.progress.ProgressRules.focusDemon
import com.summondemons.safely.progress.ProgressRules.isRestless
import com.summondemons.safely.progress.ProgressRules.unsummon
import com.summondemons.safely.progress.ProgressRules.visit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressRulesTest {

    private val ids = listOf("asmodeus", "astaroth")

    // --- Daily summon ---

    @Test
    fun oneSummonPerLocalDayAndNeverUnlocksOnAClockMovedBack() {
        var p = Progress()
        assertTrue(canSummon(p, 100))
        p = commitSummon(p, "asmodeus", 100, flawless = false)
        assertFalse(canSummon(p, 100))
        assertFalse(canSummon(p, 99))
        assertTrue(canSummon(p, 101))
    }

    @Test
    fun commitSetsEveryField() {
        val p = commitSummon(Progress(), "astaroth", 50, flawless = true)
        assertEquals(
            DemonRecord(
                bound = true,
                revealed = true,
                flawless = true,
                lastVisitDay = 50,
                revealPending = RevealPending.All,
                escapeBannerPending = false,
            ),
            p.record("astaroth"),
        )
        assertEquals(50L, p.lastSummonDay)
        assertEquals("astaroth", p.lastViewed)
    }

    @Test
    fun commitNeverMovesTheSummonDayBack() {
        val p = commitSummon(Progress(lastSummonDay = 10), "asmodeus", 9, flawless = false)
        assertEquals(10L, p.lastSummonDay)
    }

    @Test
    fun commitClearsAPendingEscapeBanner() {
        val escaped = evaluate(commitSummon(Progress(), "asmodeus", 100, false), 103)
        assertTrue(escaped.record("asmodeus").escapeBannerPending)
        assertFalse(commitSummon(escaped, "asmodeus", 104, false).record("asmodeus").escapeBannerPending)
    }

    // --- Restless and escape ---

    @Test
    fun turnsRestlessAtTwoDaysAndEscapesAtThreeKeepingLoreAndFlawless() {
        var p = commitSummon(Progress(), "asmodeus", 100, flawless = true)
        assertFalse(isRestless(p.record("asmodeus"), 101))
        assertTrue(isRestless(p.record("asmodeus"), 102))
        assertTrue(evaluate(p, 102).record("asmodeus").bound)
        p = evaluate(p, 103)
        val r = p.record("asmodeus")
        assertFalse(r.bound)
        assertTrue(r.revealed)
        assertTrue(r.flawless)
        assertTrue(r.escapeBannerPending)
        assertEquals(null, r.lastVisitDay)
        assertEquals(RevealPending(), r.revealPending)
    }

    @Test
    fun evaluateWithNothingToDoReturnsTheSameProgress() {
        val p = commitSummon(Progress(), "asmodeus", 100, false)
        assertSame(p, evaluate(p, 101))
    }

    @Test
    fun aVisitResetsTheCount() {
        var p = commitSummon(Progress(), "asmodeus", 100, false)
        p = visit(p, "asmodeus", 102)
        assertFalse(isRestless(p.record("asmodeus"), 103))
        assertTrue(evaluate(p, 104).record("asmodeus").bound)
        assertEquals("asmodeus", p.lastViewed)
    }

    @Test
    fun visitingAnUnboundDemonChangesNothing() {
        val p = Progress(lastViewed = "asmodeus")
        assertSame(p, visit(p, "astaroth", 5))
    }

    @Test
    fun daysSinceVisitIsNeverNegative() {
        val p = commitSummon(Progress(), "asmodeus", 100, false)
        assertEquals(0L, daysSinceVisit(p.record("asmodeus"), 98))
        assertEquals(0L, daysSinceVisit(DemonRecord(), 500))
    }

    @Test
    fun aReSummonKeepsAnEarnedFlawless() {
        var p = commitSummon(Progress(), "asmodeus", 100, flawless = true)
        p = evaluate(p, 103)
        p = commitSummon(p, "asmodeus", 104, flawless = false)
        assertTrue(p.record("asmodeus").flawless)
    }

    // --- One-time flags ---

    @Test
    fun revealFlagsClearOneByOne() {
        var p = commitSummon(Progress(), "astaroth", 1, false)
        p = consumeReveal(p, "astaroth", Reveal.Codex)
        assertEquals(RevealPending(avatar = true, codex = false, battle = true), p.record("astaroth").revealPending)
        p = consumeReveal(p, "astaroth", Reveal.Avatar)
        p = consumeReveal(p, "astaroth", Reveal.Battle)
        assertEquals(RevealPending(), p.record("astaroth").revealPending)
    }

    @Test
    fun dismissingClearsEveryEscapeBanner() {
        var p = commitSummon(commitSummon(Progress(), "asmodeus", 100, false), "astaroth", 101, false)
        p = evaluate(p, 110)
        assertTrue(ids.all { p.record(it).escapeBannerPending })
        p = dismissEscapeBanners(p)
        assertTrue(ids.none { p.record(it).escapeBannerPending })
    }

    @Test
    fun unsummonForgetsTheDemon() {
        val p = unsummon(commitSummon(Progress(), "astaroth", 1, flawless = true), "astaroth")
        assertEquals(DemonRecord(), p.record("astaroth"))
    }

    // --- Focus demon ---

    @Test
    fun prefersRestlessThenUnboundWhileASummonIsAvailableThenLastViewed() {
        var p = Progress()
        assertEquals("asmodeus", focusDemon(p, ids, 10))
        p = commitSummon(p, "asmodeus", 10, false)
        assertEquals("asmodeus", focusDemon(p, ids, 10)) // day used → last viewed
        assertEquals("astaroth", focusDemon(p, ids, 11))
        assertEquals("asmodeus", focusDemon(p, ids, 12)) // restless wins
    }

    @Test
    fun anUnknownLastViewedFallsBackToTheFirstDemon() {
        val p = Progress(lastSummonDay = 10, lastViewed = "baal")
        assertEquals("asmodeus", focusDemon(p, ids, 10))
    }
}
```

- [ ] **Step 2: Run it to see it fail**

Run: `./gradlew testDebugUnitTest --tests '*ProgressRulesTest*'`
Expected: compilation fails (`ProgressRules` unresolved).

- [ ] **Step 3: Implement the rules**

`app/src/main/java/com/summondemons/safely/progress/ProgressRules.kt`:

```kotlin
package com.summondemons.safely.progress

import com.summondemons.safely.progress.ProgressConfig.ESCAPE_AFTER_DAYS
import com.summondemons.safely.progress.ProgressConfig.RESTLESS_AFTER_DAYS

/**
 * Pure progress rules (ADR-0013, ADR-0015). Days are local epoch days; callers pass
 * `today`, so tests and the debug clock can move time.
 */
object ProgressRules {

    /** One summon per local day. Strictly `today > lastSummonDay`: a clock moved back never unlocks early. */
    fun canSummon(p: Progress, today: Long): Boolean = p.lastSummonDay == null || today > p.lastSummonDay

    /** Saved at ClimaxStart. [day] is the day the ritual started, so 23:59 → 00:01 spends the earlier day. */
    fun commitSummon(p: Progress, id: String, day: Long, flawless: Boolean): Progress {
        val old = p.record(id)
        val record = old.copy(
            bound = true,
            revealed = true,
            flawless = old.flawless || flawless,
            lastVisitDay = day,
            revealPending = RevealPending.All,
            escapeBannerPending = false,
        )
        return p.copy(
            demons = p.demons + (id to record),
            lastSummonDay = maxOf(day, p.lastSummonDay ?: day),
            lastViewed = id,
        )
    }

    /** Viewing a bound demon is the visit. Unbound demons are not visited. */
    fun visit(p: Progress, id: String, today: Long): Progress {
        val record = p.record(id)
        if (!record.bound) return p
        return p.copy(demons = p.demons + (id to record.copy(lastVisitDay = today)), lastViewed = id)
    }

    fun daysSinceVisit(r: DemonRecord, today: Long): Long =
        if (r.bound && r.lastVisitDay != null) maxOf(0L, today - r.lastVisitDay) else 0L

    fun isRestless(r: DemonRecord, today: Long): Boolean = daysSinceVisit(r, today) >= RESTLESS_AFTER_DAYS

    /** Neglected demons escape: unbound again, lore and flawless kept, banner queued. Same instance if nothing changes. */
    fun evaluate(p: Progress, today: Long): Progress {
        val escaped = p.demons.filterValues { it.bound && daysSinceVisit(it, today) >= ESCAPE_AFTER_DAYS }
        if (escaped.isEmpty()) return p
        return p.copy(
            demons = p.demons + escaped.mapValues { (_, r) ->
                r.copy(bound = false, lastVisitDay = null, escapeBannerPending = true, revealPending = RevealPending())
            },
        )
    }

    fun consumeReveal(p: Progress, id: String, what: Reveal): Progress {
        val record = p.record(id)
        val pending = when (what) {
            Reveal.Avatar -> record.revealPending.copy(avatar = false)
            Reveal.Codex -> record.revealPending.copy(codex = false)
            Reveal.Battle -> record.revealPending.copy(battle = false)
        }
        return p.copy(demons = p.demons + (id to record.copy(revealPending = pending)))
    }

    fun dismissEscapeBanners(p: Progress): Progress =
        p.copy(demons = p.demons.mapValues { (_, r) -> r.copy(escapeBannerPending = false) })

    /** Debug Unsummon: forget everything about this demon. */
    fun unsummon(p: Progress, id: String): Progress = p.copy(demons = p.demons + (id to DemonRecord()))

    /**
     * Where Home opens the Grimoire: the first restless demon; else, while today's summon is
     * available, the first unbound one; else the last viewed if still valid; else the first.
     */
    fun focusDemon(p: Progress, ids: List<String>, today: Long): String {
        ids.firstOrNull { isRestless(p.record(it), today) }?.let { return it }
        if (canSummon(p, today)) ids.firstOrNull { !p.record(it).bound }?.let { return it }
        p.lastViewed?.takeIf { it in ids }?.let { return it }
        return ids.first()
    }
}
```

- [ ] **Step 4: Run the tests**

Run: `./gradlew testDebugUnitTest --tests '*ProgressRulesTest*'`
Expected: 15 tests pass.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/summondemons/safely/progress app/src/test/java/com/summondemons/safely/progress
git commit -q -m "feat(progress): Daily summon, restless, escape and focus-demon rules" -m "$TRAILER"
```

### Task 7: Court Hours

**Files:**
- Create: `MAIN/progress/CourtHours.kt`, `TEST/progress/CourtHoursTest.kt`

- [ ] **Step 1: Write the failing test**

`app/src/test/java/com/summondemons/safely/progress/CourtHoursTest.kt`:

```kotlin
package com.summondemons.safely.progress

import com.summondemons.safely.data.House
import org.junit.Assert.assertEquals
import org.junit.Test

class CourtHoursTest {

    private fun hours(house: House) = (0..23).filter { CourtHours.inCourt(house, it) }

    @Test
    fun solHoldsCourtNineToNoonAndThreeToSunset() {
        assertEquals((9..11) + (15..17), hours(House.Sol))
    }

    @Test
    fun venusHoldsCourtFromSunriseToNoon() {
        assertEquals((6..11).toList(), hours(House.Venus))
    }

    @Test
    fun rangeEndsAreExclusive() {
        assertEquals(false, CourtHours.inCourt(House.Sol, 12))
        assertEquals(false, CourtHours.inCourt(House.Sol, 18))
        assertEquals(false, CourtHours.inCourt(House.Venus, 12))
        assertEquals(true, CourtHours.inCourt(House.Venus, 6))
    }
}
```

- [ ] **Step 2: Run it to see it fail**

Run: `./gradlew testDebugUnitTest --tests '*CourtHoursTest*'`
Expected: compilation fails (`CourtHours` unresolved).

- [ ] **Step 3: Implement**

`app/src/main/java/com/summondemons/safely/progress/CourtHours.kt`:

```kotlin
package com.summondemons.safely.progress

import com.summondemons.safely.data.House

/** Court Hours (ADR-0012): fixed local sunrise/sunset, no weather. */
object CourtHours {
    /** True when the local [hour] (0..23) falls in one of the House's court ranges. */
    fun inCourt(house: House, hour: Int): Boolean = house.court.any { hour in it }
}
```

- [ ] **Step 4: Run the tests**

Run: `./gradlew testDebugUnitTest --tests '*CourtHoursTest*'`
Expected: 3 tests pass.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/summondemons/safely/progress app/src/test/java/com/summondemons/safely/progress
git commit -q -m "feat(progress): Court Hours per House" -m "$TRAILER"
```

### Task 8: Focus-clock RitualEngine and every caller it breaks

The engine, its state and its effects change together; the build only compiles again
once `RitualFeedback`, `RitualScreen` and the existing tests are updated, so this task
ends with one commit.

**Files:**
- Modify: `MAIN/ritual/RitualConfig.kt`, `MAIN/platform/RitualFeedback.kt`, `MAIN/ui/screens/RitualScreen.kt`, `TEST/ritual/RitualStateTest.kt`, `TEST/ritual/RitualViewModelTest.kt`
- Rewrite: `MAIN/ritual/RitualState.kt`, `MAIN/ritual/RitualEngine.kt`, `TEST/ritual/RitualEngineTest.kt`

- [ ] **Step 1: Write the failing engine test** (keeps the still-valid Ritual 2.0 cases and ports `src/engine/ritualEngine.test.ts` case for case)

`app/src/test/java/com/summondemons/safely/ritual/RitualEngineTest.kt`:

```kotlin
package com.summondemons.safely.ritual

import com.summondemons.safely.ritual.RitualConfig.CLIMAX_MS
import com.summondemons.safely.ritual.RitualConfig.COURT_BONUS
import com.summondemons.safely.ritual.RitualConfig.FOCUS_DRIFT_MULTIPLIER
import com.summondemons.safely.ritual.RitualConfig.FRAME_MS
import com.summondemons.safely.ritual.RitualConfig.LOCK_HOLD_MS
import com.summondemons.safely.ritual.RitualConfig.ONSET_REFRACTORY_MS
import com.summondemons.safely.ritual.RitualEffect.ClimaxStart
import com.summondemons.safely.ritual.RitualEffect.Cracked
import com.summondemons.safely.ritual.RitualEffect.Drift
import com.summondemons.safely.ritual.RitualEffect.Locked
import com.summondemons.safely.ritual.RitualEffect.ProximityTick
import com.summondemons.safely.ritual.RitualEffect.Resumed
import com.summondemons.safely.ritual.RitualEffect.Sealed
import com.summondemons.safely.ritual.RitualEffect.Summoned
import com.summondemons.safely.ritual.RitualEffect.SyllableLit
import com.summondemons.safely.ritual.RitualPhase.Chanting
import com.summondemons.safely.ritual.RitualPhase.Climax
import com.summondemons.safely.ritual.RitualPhase.Done
import com.summondemons.safely.ritual.RitualPhase.Locking
import com.summondemons.safely.ritual.RitualPhase.Seeking
import com.summondemons.safely.ritual.RitualPhase.Wavering
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.ceil

class RitualEngineTest {

    private val silent = VoiceFrame.SILENT
    private val voiced = VoiceFrame(rms = 3_000.0, voiced = true, onset = false)
    private val onset = VoiceFrame(rms = 3_000.0, voiced = true, onset = true)
    private val on = 0f
    private val off = 90f
    private val lockFrames = (LOCK_HOLD_MS / FRAME_MS).toInt()
    private val climaxFrames = (CLIMAX_MS / FRAME_MS).toInt()
    private val refractoryFrames = (ONSET_REFRACTORY_MS / FRAME_MS).toInt()

    /** The prototype's test phrases: 8, 6, 2 and 3 syllables. */
    private val phrases = listOf("DAEMONIUM RESIDENTE,", "GRATIA SIT CURA.", "HIC EST", "UT VOTA")

    private fun fresh(syllables: List<Int> = listOf(2, 2, 2, 2)) = RitualState(target = 0f, syllables = syllables)

    /** Runs [frames] identical ticks and collects every effect. */
    private fun run(
        start: RitualState,
        frames: Int,
        azimuth: Float? = on,
        voice: VoiceFrame = silent,
        inCourt: Boolean = false,
    ): Step {
        var state = start
        val effects = mutableListOf<RitualEffect>()
        repeat(frames) {
            val step = RitualEngine.step(state, RitualInput(azimuth, voice, inCourt))
            state = step.state
            effects += step.effects
        }
        return Step(state, effects)
    }

    private fun locked(syllables: List<Int> = listOf(2, 2, 2, 2)) = run(fresh(syllables), lockFrames).state

    /** The prototype phrases, locked (optionally while the House is in court). */
    private fun lockedPhrases(inCourt: Boolean = false) =
        run(RitualState.start(0f, phrases), lockFrames, inCourt = inCourt).state

    /** One onset frame, then silence for the refractory period. */
    private fun syllable(state: RitualState): Step {
        val lit = run(state, 1, voice = onset)
        val rest = run(lit.state, refractoryFrames)
        return Step(rest.state, lit.effects + rest.effects)
    }

    /** The prototype's helper: [count] × (silence for the refractory period, then one onset). */
    private fun chant(start: RitualState, count: Int, azimuth: Float? = on): Step {
        var state = start
        val effects = mutableListOf<RitualEffect>()
        repeat(count) {
            val rest = run(state, refractoryFrames, azimuth)
            val lit = run(rest.state, 1, azimuth, onset)
            state = lit.state
            effects += rest.effects + lit.effects
        }
        return Step(state, effects)
    }

    /** Phrase I of the prototype phrases sealed: phrase II's clock just started. */
    private fun firstSealed() = lockedPhrases().let { chant(it, it.syllables[0]).state }

    // --- Seeking and locking ---

    @Test
    fun startsSeekingWithNoInk() {
        assertEquals(Seeking, fresh().phase)
        assertEquals(0f, fresh().inkProgress)
    }

    @Test
    fun unknownHeadingStaysSeekingQuietly() {
        val step = run(fresh(), 100, azimuth = null)
        assertEquals(Seeking, step.state.phase)
        assertEquals(emptyList<RitualEffect>(), step.effects)
    }

    @Test
    fun locksAfterExactly600ms() {
        assertEquals(Locking, run(fresh(), lockFrames - 1).state.phase)
        val step = run(fresh(), lockFrames)
        assertEquals(Chanting, step.state.phase)
        assertEquals(1, step.effects.count { it == Locked })
    }

    @Test
    fun leavingBeforeTheLockStartsOver() {
        val holding = run(fresh(), 10).state
        val left = run(holding, 1, azimuth = off)
        assertEquals(Seeking, left.state.phase)
        assertTrue(Locked !in left.effects)
        assertEquals(Locking, run(left.state, lockFrames - 1).state.phase)
    }

    @Test
    fun proximityTicksOnlyWhenGettingCloser() {
        var state = fresh()
        val ticks = listOf(70f, 58f, 57f, 52f, 56f, 50f).map { azimuth ->
            val step = RitualEngine.step(state, RitualInput(azimuth, silent))
            state = step.state
            ProximityTick in step.effects
        }
        assertEquals(listOf(false, true, false, true, false, true), ticks)
    }

    @Test
    fun theCourtMultiplierIsFixedAtLock() {
        assertEquals(1f + COURT_BONUS, lockedPhrases(inCourt = true).courtMultiplier, 0.0001f)
        assertEquals(1f, lockedPhrases(inCourt = false).courtMultiplier, 0.0001f)
        // Court leaving after the lock changes nothing.
        val later = run(lockedPhrases(inCourt = true), 10, inCourt = false).state
        assertEquals(1f + COURT_BONUS, later.courtMultiplier, 0.0001f)
    }

    // --- Chanting ---

    @Test
    fun anOnsetLightsASyllable() {
        val step = run(locked(), 1, voice = onset)
        assertEquals(1, step.state.lit)
        assertEquals(listOf(SyllableLit), step.effects)
    }

    @Test
    fun onsetsInsideTheRefractoryPeriodAreIgnored() {
        val first = run(locked(listOf(5, 2, 2, 2)), 1, voice = onset).state
        val tooSoon = run(run(first, refractoryFrames - 2).state, 1, voice = onset).state
        assertEquals(1, tooSoon.lit)
        val inTime = run(run(first, refractoryFrames - 1).state, 1, voice = onset).state
        assertEquals(2, inTime.lit)
    }

    @Test
    fun sustainedVoicingLightsASyllableEvery350ms() {
        val start = locked(listOf(5, 2, 2, 2))
        assertEquals(0, run(start, 17, voice = voiced).state.lit)
        assertEquals(1, run(start, 18, voice = voiced).state.lit)
        assertEquals(2, run(start, 36, voice = voiced).state.lit)
    }

    @Test
    fun silenceOnPhraseOnePausesWithoutDecay() {
        val one = syllable(locked(listOf(3, 2, 2, 2))).state
        val later = run(one, 500).state
        assertEquals(Chanting, later.phase)
        assertEquals(1, later.lit)
    }

    @Test
    fun litSyllablesSealAPhrase() {
        val step = syllable(syllable(locked()).state)
        assertEquals(1, step.state.phrase)
        assertEquals(0, step.state.lit)
        assertTrue(Sealed(0) in step.effects)
        assertEquals(0.25f, step.state.inkProgress, 0.0001f)
    }

    @Test
    fun theLastPhraseStartsTheClimax() {
        var state = locked(listOf(1, 1, 1, 1))
        repeat(3) { state = syllable(state).state }
        val step = run(state, 1, voice = onset)
        assertEquals(Climax, step.state.phase)
        assertEquals(listOf(SyllableLit, Sealed(3), ClimaxStart(flawless = true)), step.effects)
        assertEquals(1f, step.state.inkProgress)
    }

    // --- Focus clock (prototype suite) ---

    @Test
    fun prototypePhrasesHaveTheExpectedSyllables() {
        assertEquals(listOf(8, 6, 2, 3), lockedPhrases().syllables)
    }

    @Test
    fun phraseOneHasNoClockAndWaveringOnItDoesNotDrain() {
        val s = lockedPhrases()
        assertEquals(0L, s.focusBudgetMs)
        val wavering = run(s, 500, azimuth = off).state
        assertEquals(Wavering, wavering.phase)
        assertEquals(0, wavering.phrase)
        assertEquals(0L, wavering.focusMs)
    }

    @Test
    fun aSealStartsAFullClockForTheNextPhrase() {
        val start = lockedPhrases()
        val step = chant(start, start.syllables[0])
        assertTrue(Sealed(0) in step.effects)
        assertEquals(1, step.state.phrase)
        assertEquals(focusBudget(step.state.syllables[1], 1), step.state.focusBudgetMs)
        assertEquals(step.state.focusBudgetMs, step.state.focusMs)
    }

    @Test
    fun drainsOnceOnHeadingEvenInSilenceAndThreeTimesOffHeading() {
        val sealed = firstSealed()
        val onHeading = run(sealed, 10).state
        assertEquals(10 * FRAME_MS, sealed.focusMs - onHeading.focusMs)
        val offHeading = run(sealed, 10, azimuth = off).state
        assertEquals(10 * FRAME_MS * FOCUS_DRIFT_MULTIPLIER, sealed.focusMs - offHeading.focusMs)
    }

    @Test
    fun syllablesDoNotCountOffHeadingAndResumingNeedsNoReLock() {
        val drifted = chant(firstSealed(), 2, azimuth = off).state
        assertEquals(0, drifted.lit)
        val back = run(drifted, 1, voice = onset)
        assertEquals(Chanting, back.state.phase)
        assertTrue(Resumed in back.effects)
    }

    @Test
    fun aNullHeadingCountsAsOffHeading() {
        assertEquals(Wavering, run(firstSealed(), 1, azimuth = null).state.phase)
    }

    @Test
    fun onceLockedTheRitualNeverReturnsToSeekingAndWarnsOnce() {
        val step = run(locked(), 500, azimuth = null)
        assertEquals(Wavering, step.state.phase)
        assertEquals(1, step.effects.count { it == Drift })
    }

    @Test
    fun voiceIsIgnoredWhileWavering() {
        val wavering = run(syllable(locked(listOf(3, 2, 2, 2))).state, 1, azimuth = off).state
        assertEquals(1, run(wavering, 20, azimuth = off, voice = onset).state.lit)
    }

    @Test
    fun runningOutCracksTheLastSealClearsThePhraseAndEndsFlawless() {
        val s = chant(firstSealed(), 2).state
        assertEquals(2, s.lit)
        val step = run(s, ceil(s.focusMs.toDouble() / FRAME_MS).toInt() + 1)
        assertTrue(Cracked(0) in step.effects)
        assertEquals(0, step.state.phrase)
        assertEquals(0, step.state.lit)
        assertEquals(0L, step.state.focusBudgetMs) // back on phrase I: no clock
        assertFalse(step.state.flawless)
        assertEquals(0f, step.state.inkProgress)
    }

    @Test
    fun aCrackOnPhraseThreeReopensPhraseTwoWithItsOwnFullBudget() {
        val sealed = firstSealed()
        val s = chant(sealed, sealed.syllables[1]).state
        assertEquals(2, s.phrase)
        val frames = ceil(s.focusMs.toDouble() / FRAME_MS / FOCUS_DRIFT_MULTIPLIER).toInt() + 1
        val step = run(s, frames, azimuth = off)
        assertEquals(1, step.state.phrase)
        assertTrue(Cracked(1) in step.effects)
        assertEquals(focusBudget(s.syllables[1], 1), step.state.focusBudgetMs)
    }

    @Test
    fun aFlawlessRunReachesTheClimaxAndReportsIt() {
        var s = lockedPhrases()
        val effects = mutableListOf<RitualEffect>()
        for (p in 0 until 4) {
            val step = chant(s, s.syllables[p])
            s = step.state
            effects += step.effects
        }
        assertEquals(Climax, s.phase)
        assertTrue(ClimaxStart(flawless = true) in effects)
        assertEquals(0f, s.focusFraction)
        assertEquals(Done, run(s, climaxFrames).state.phase)
    }

    @Test
    fun budgetsAreThreeSecondsPlusHalfASecondPerSyllableScaledInCourt() {
        assertEquals(12_000L, focusBudget(18, 1))
        assertEquals(7_500L, focusBudget(6, 1, 1.25f))
        assertEquals(0L, focusBudget(8, 0))
    }

    // --- Climax and done ---

    private val climaxStart = fresh().copy(phase = Climax, phrase = 4)

    @Test
    fun climaxIgnoresInputAndSummonsOnce() {
        val almost = run(climaxStart, climaxFrames - 1, azimuth = off, voice = onset)
        assertEquals(Climax, almost.state.phase)
        assertTrue(almost.effects.isEmpty())
        assertEquals(1f - 1f / climaxFrames, almost.state.climaxFraction, 0.0001f)

        val done = run(climaxStart, climaxFrames, azimuth = off, voice = onset)
        assertEquals(Done, done.state.phase)
        assertEquals(listOf(Summoned), done.effects)
    }

    @Test
    fun doneIsTerminal() {
        val done = run(climaxStart, climaxFrames).state
        val step = RitualEngine.step(done, RitualInput(on, onset))
        assertSame(done, step.state)
        assertTrue(step.effects.isEmpty())
    }
}
```

- [ ] **Step 2: Update `RitualStateTest`**

In `app/src/test/java/com/summondemons/safely/ritual/RitualStateTest.kt` replace the test
`fractionsOnlyApplyInTheirPhase` with:

```kotlin
    @Test
    fun fractionsOnlyApplyInTheirPhase() {
        val state = RitualState(target = 0f, syllables = listOf(1, 1, 1, 1))
        assertEquals(0f, state.focusFraction)
        assertEquals(0.25f, state.copy(focusMs = 1_000, focusBudgetMs = 4_000).focusFraction, 0.0001f)
        assertEquals(0.5f, state.copy(phase = RitualPhase.Climax, climaxMs = 1_250).climaxFraction, 0.0001f)
        assertEquals(1f, state.copy(phase = RitualPhase.Done).climaxFraction)
    }

    @Test
    fun theClockRunsOnPhrasesTwoToFourWhileChantingOrWavering() {
        val state = RitualState(target = 0f, syllables = listOf(1, 1, 1, 1))
        assertFalse(state.copy(phase = RitualPhase.Chanting, phrase = 0).clockActive)
        assertTrue(state.copy(phase = RitualPhase.Chanting, phrase = 1).clockActive)
        assertTrue(state.copy(phase = RitualPhase.Wavering, phrase = 3).clockActive)
        assertFalse(state.copy(phase = RitualPhase.Climax, phrase = 4).clockActive)
        assertFalse(state.copy(phase = RitualPhase.Locking, phrase = 2).clockActive)
    }
```

- [ ] **Step 3: Update `RitualViewModelTest` for the renamed effects and the new drift rule**

In `app/src/test/java/com/summondemons/safely/ritual/RitualViewModelTest.kt`:

1. Replace the import `com.summondemons.safely.ritual.RitualEffect.PhraseComplete` with
   `com.summondemons.safely.ritual.RitualEffect.Sealed`.
2. In `holdToChantWithSimulatedHeadingCompletesTheRitual`, replace the expected list with:

```kotlin
            listOf(Locked, Sealed(0), Sealed(1), Sealed(2), Sealed(3), ClimaxStart(flawless = true), Summoned),
```

3. Replace the whole test `driftLosesTheCurrentPhraseOnlyAfterTheGrace` with:

```kotlin
    @Test
    fun driftWaversWithoutLosingProgressAndNeverReturnsToSeeking() = runTest(dispatcher) {
        val heading = FakeHeading()
        val vm = newVm(demon, heading, silentPcm())
        observe(vm)
        vm.onMicPermission(false)
        vm.setHoldToChant(true)
        heading.readings.emit(demon.ritualHeading)
        advanceTimeBy(1_500); runCurrent()
        val lit = vm.uiState.value.ritual.lit
        assertTrue("lit=$lit", lit > 0)
        assertEquals(0, vm.uiState.value.ritual.phrase) // still phrase I: no clock

        vm.setHoldToChant(false)
        heading.readings.emit(demon.ritualHeading + 90f)
        advanceTimeBy(5_000); runCurrent()
        assertEquals(RitualPhase.Wavering, vm.uiState.value.ritual.phase)
        assertEquals(lit, vm.uiState.value.ritual.lit)
    }
```

- [ ] **Step 4: Run the tests to see them fail**

Run: `./gradlew testDebugUnitTest --tests '*Ritual*'`
Expected: compilation fails (`Sealed`, `Cracked`, `focusBudget`, `focusFraction`… unresolved).

- [ ] **Step 5: Update `RitualConfig`**

In `app/src/main/java/com/summondemons/safely/ritual/RitualConfig.kt`:
- change the KDoc to `/** Ritual-wide tuning (ADR-0008, ADR-0010). Per-demon parameters live in `Demon` (ADR-0006). */`
- delete the line `const val DRIFT_GRACE_MS = 1_500L`
- after `const val PROXIMITY_STEP_DEG = 5f` add:

```kotlin

    // Focus clock (ADR-0010) and the Court Hours bonus (ADR-0012)
    const val FOCUS_BASE_MS = 3_000L
    const val FOCUS_PER_SYLLABLE_MS = 500L
    const val FOCUS_DRIFT_MULTIPLIER = 3
    const val COURT_BONUS = 0.25f
    /** Below this focus fraction the focus bar beats. */
    const val FOCUS_LOW_FRACTION = 0.25f
```

- [ ] **Step 6: Rewrite `RitualState.kt`**

`app/src/main/java/com/summondemons/safely/ritual/RitualState.kt`:

```kotlin
package com.summondemons.safely.ritual

import com.summondemons.safely.ritual.RitualConfig.CLIMAX_MS
import com.summondemons.safely.ritual.RitualConfig.FOCUS_BASE_MS
import com.summondemons.safely.ritual.RitualConfig.FOCUS_PER_SYLLABLE_MS
import com.summondemons.safely.ritual.RitualConfig.ONSET_REFRACTORY_MS
import com.summondemons.safely.ritual.RitualConfig.PHRASE_COUNT
import kotlin.math.roundToLong

enum class RitualPhase { Seeking, Locking, Chanting, Wavering, Climax, Done }

/**
 * One 20 ms tick: the latest heading (null = unknown, which counts as off heading once
 * locked), the analysed mic frame, and whether the demon's House is in court (read every
 * tick, used only at the moment of lock, ADR-0012).
 */
data class RitualInput(val azimuth: Float?, val voice: VoiceFrame, val inCourt: Boolean = false)

/** What happened during a step; the UI turns these into haptics, sounds and flashes. */
sealed interface RitualEffect {
    data object ProximityTick : RitualEffect
    data object Locked : RitualEffect
    data object SyllableLit : RitualEffect

    /** Every syllable of [phrase] lit (ADR-0010). */
    data class Sealed(val phrase: Int) : RitualEffect

    /** Focus ran out: [phrase], the last sealed one, reopened. */
    data class Cracked(val phrase: Int) : RitualEffect
    data object Drift : RitualEffect
    data object Resumed : RitualEffect
    data class ClimaxStart(val flawless: Boolean) : RitualEffect
    data object Summoned : RitualEffect
}

data class RitualState(
    val target: Float,
    /** Syllables per phrase: exactly [PHRASE_COUNT] entries, each >= 1. */
    val syllables: List<Int>,
    val phase: RitualPhase = RitualPhase.Seeking,
    val azimuth: Float? = null,
    /** Phrases sealed so far = index of the current phrase (0..4). */
    val phrase: Int = 0,
    /** Syllables lit in the current phrase. */
    val lit: Int = 0,
    /** Time held on heading while Locking. */
    val lockMs: Long = 0L,
    /** Focus left for the current phrase; 0 and unused on phrase I (ADR-0010). */
    val focusMs: Long = 0L,
    val focusBudgetMs: Long = 0L,
    /** 1, or 1 + COURT_BONUS if the House was in court at the moment of lock; fixed for the attempt. */
    val courtMultiplier: Float = 1f,
    /** No crack so far in this attempt (ADR-0013). */
    val flawless: Boolean = true,
    /** Time spent in the climax. */
    val climaxMs: Long = 0L,
    val sinceSyllableMs: Long = ONSET_REFRACTORY_MS,
    val voicedRunMs: Long = 0L,
    /** Last 5 degree proximity bucket while seeking; null when out of range or unknown. */
    val proximityBucket: Int? = null,
) {
    init {
        require(syllables.size == PHRASE_COUNT) { "need exactly $PHRASE_COUNT phrases" }
        require(syllables.all { it >= 1 }) { "every phrase needs at least one syllable" }
    }

    val aligned: Boolean get() = azimuth != null && isAligned(azimuth, target)

    /** Sigil ink in [0, 1] (ADR-0009). */
    val inkProgress: Float
        get() = if (phrase >= PHRASE_COUNT) 1f else (phrase + lit.toFloat() / syllables[phrase]) / PHRASE_COUNT

    /** Focus left in [0, 1]; 0 when the current phrase has no clock. */
    val focusFraction: Float
        get() = if (focusBudgetMs > 0L) (focusMs.toFloat() / focusBudgetMs).coerceIn(0f, 1f) else 0f

    /** The focus clock runs on phrases II–IV while Chanting or Wavering. */
    val clockActive: Boolean
        get() = phrase in 1 until PHRASE_COUNT && (phase == RitualPhase.Chanting || phase == RitualPhase.Wavering)

    val climaxFraction: Float
        get() = when (phase) {
            RitualPhase.Climax -> (climaxMs.toFloat() / CLIMAX_MS).coerceIn(0f, 1f)
            RitualPhase.Done -> 1f
            else -> 0f
        }

    companion object {
        fun start(target: Float, phrases: List<String>): RitualState =
            RitualState(target, phrases.map { LatinSyllables.count(it).coerceAtLeast(1) })
    }
}

/** Focus budget in ms for phrase index [phrase] with [syllables] syllables (ADR-0010); none for phrase I. */
fun focusBudget(syllables: Int, phrase: Int, courtMultiplier: Float = 1f): Long =
    if (phrase <= 0) 0L else ((FOCUS_BASE_MS + FOCUS_PER_SYLLABLE_MS * syllables) * courtMultiplier).roundToLong()

data class Step(val state: RitualState, val effects: List<RitualEffect> = emptyList())
```

- [ ] **Step 7: Rewrite `RitualEngine.kt`**

`app/src/main/java/com/summondemons/safely/ritual/RitualEngine.kt`:

```kotlin
package com.summondemons.safely.ritual

import com.summondemons.safely.ritual.RitualConfig.CLIMAX_MS
import com.summondemons.safely.ritual.RitualConfig.COURT_BONUS
import com.summondemons.safely.ritual.RitualConfig.FOCUS_DRIFT_MULTIPLIER
import com.summondemons.safely.ritual.RitualConfig.FRAME_MS
import com.summondemons.safely.ritual.RitualConfig.LOCK_HOLD_MS
import com.summondemons.safely.ritual.RitualConfig.ONSET_REFRACTORY_MS
import com.summondemons.safely.ritual.RitualConfig.PHRASE_COUNT
import com.summondemons.safely.ritual.RitualConfig.PROXIMITY_RANGE_DEG
import com.summondemons.safely.ritual.RitualConfig.PROXIMITY_STEP_DEG
import com.summondemons.safely.ritual.RitualConfig.SUSTAIN_SYLLABLE_MS

/** Pure ritual rules (ADR-0008 with the focus clock of ADR-0010): one call per 20 ms tick. */
object RitualEngine {

    fun step(state: RitualState, input: RitualInput): Step {
        if (state.phase == RitualPhase.Done) return Step(state)
        val s = state.copy(azimuth = input.azimuth)
        return when (s.phase) {
            RitualPhase.Seeking, RitualPhase.Locking -> seek(s, input.inCourt)
            RitualPhase.Chanting, RitualPhase.Wavering -> chant(s, input.voice)
            RitualPhase.Climax -> climax(s)
            RitualPhase.Done -> Step(s)
        }
    }

    private fun seek(s: RitualState, inCourt: Boolean): Step {
        val effects = mutableListOf<RitualEffect>()
        val bucket = s.azimuth?.let { proximityBucket(angularDistance(it, s.target)) }
        val previous = s.proximityBucket
        if (bucket != null && (previous == null || bucket < previous)) effects += RitualEffect.ProximityTick
        if (!s.aligned) {
            return Step(s.copy(phase = RitualPhase.Seeking, lockMs = 0L, proximityBucket = bucket), effects)
        }
        val held = (if (s.phase == RitualPhase.Locking) s.lockMs else 0L) + FRAME_MS
        if (held < LOCK_HOLD_MS) {
            return Step(s.copy(phase = RitualPhase.Locking, lockMs = held, proximityBucket = bucket), effects)
        }
        effects += RitualEffect.Locked
        val chanting = s.copy(
            phase = RitualPhase.Chanting,
            lockMs = 0L,
            proximityBucket = bucket,
            courtMultiplier = if (inCourt) 1f + COURT_BONUS else 1f,
            sinceSyllableMs = ONSET_REFRACTORY_MS,
            voicedRunMs = 0L,
        )
        return Step(chanting, effects)
    }

    private fun chant(s: RitualState, voice: VoiceFrame): Step {
        val effects = mutableListOf<RitualEffect>()
        if (!s.aligned) {
            // Off heading: nothing counts; once locked, never back to Seeking (ADR-0010).
            if (s.phase == RitualPhase.Chanting) effects += RitualEffect.Drift
            return drain(s.copy(phase = RitualPhase.Wavering, voicedRunMs = 0L), FOCUS_DRIFT_MULTIPLIER, effects)
        }
        if (s.phase == RitualPhase.Wavering) effects += RitualEffect.Resumed
        val since = s.sinceSyllableMs + FRAME_MS
        val run = if (voice.voiced) s.voicedRunMs + FRAME_MS else 0L
        val lights = voice.voiced && ((voice.onset && since >= ONSET_REFRACTORY_MS) || run >= SUSTAIN_SYLLABLE_MS)
        val chanting = s.copy(phase = RitualPhase.Chanting, sinceSyllableMs = since, voicedRunMs = run)
        if (!lights) return drain(chanting, 1, effects)

        effects += RitualEffect.SyllableLit
        val lit = s.lit + 1
        val reset = chanting.copy(sinceSyllableMs = 0L, voicedRunMs = 0L)
        if (lit < s.syllables[s.phrase]) return drain(reset.copy(lit = lit), 1, effects)

        // Sealed: the next phrase starts with a full clock.
        effects += RitualEffect.Sealed(s.phrase)
        val phrase = s.phrase + 1
        if (phrase < PHRASE_COUNT) {
            val budget = focusBudget(s.syllables[phrase], phrase, s.courtMultiplier)
            return Step(reset.copy(phrase = phrase, lit = 0, focusMs = budget, focusBudgetMs = budget), effects)
        }
        effects += RitualEffect.ClimaxStart(s.flawless)
        val climax = reset.copy(phase = RitualPhase.Climax, phrase = phrase, lit = 0, focusMs = 0L, focusBudgetMs = 0L, climaxMs = 0L)
        return Step(climax, effects)
    }

    /** Drains the focus clock (phrases II–IV only); at 0 the last seal cracks. */
    private fun drain(s: RitualState, multiplier: Int, effects: MutableList<RitualEffect>): Step {
        if (s.phrase < 1) return Step(s, effects)
        val focus = s.focusMs - FRAME_MS * multiplier
        if (focus > 0L) return Step(s.copy(focusMs = focus), effects)
        val phrase = s.phrase - 1
        effects += RitualEffect.Cracked(phrase)
        val budget = focusBudget(s.syllables[phrase], phrase, s.courtMultiplier)
        return Step(s.copy(phrase = phrase, lit = 0, focusMs = budget, focusBudgetMs = budget, flawless = false), effects)
    }

    private fun climax(s: RitualState): Step {
        val elapsed = s.climaxMs + FRAME_MS
        if (elapsed < CLIMAX_MS) return Step(s.copy(climaxMs = elapsed))
        return Step(s.copy(phase = RitualPhase.Done, climaxMs = CLIMAX_MS), listOf(RitualEffect.Summoned))
    }

    private fun proximityBucket(distance: Float): Int? =
        if (distance <= PROXIMITY_RANGE_DEG) (distance / PROXIMITY_STEP_DEG).toInt() else null
}
```

- [ ] **Step 8: Update `RitualFeedback`'s effect mapping** (the final mapping lands in Task 13)

In `app/src/main/java/com/summondemons/safely/platform/RitualFeedback.kt`, replace the
whole `handle` function with:

```kotlin
private fun handle(
    effect: RitualEffect,
    context: Context,
    sounds: RitualSounds,
    fadeBed: (Float, Long) -> Unit,
) {
    when (effect) {
        RitualEffect.ProximityTick -> Haptics.tick(context)
        RitualEffect.Locked -> {
            Haptics.clunk(context)
            sounds.play(Cue.Lock)
            fadeBed(BED_VOLUME, FADE_IN_MS)
        }
        RitualEffect.SyllableLit -> Haptics.syllable(context)
        is RitualEffect.Sealed -> {
            Haptics.chime(context)
            sounds.play(Cue.chime(effect.phrase))
        }
        is RitualEffect.Cracked -> {
            Haptics.warn(context)
            sounds.play(Cue.Boom)
        }
        RitualEffect.Drift -> {
            Haptics.warn(context)
            fadeBed(BED_DUCKED, DUCK_MS)
        }
        RitualEffect.Resumed -> fadeBed(BED_VOLUME, DUCK_MS)
        is RitualEffect.ClimaxStart -> {
            Haptics.crescendo(context)
            sounds.play(Cue.Riser)
            fadeBed(1f, SWELL_MS)
        }
        RitualEffect.Summoned -> {
            Haptics.success(context)
            sounds.play(Cue.Boom)
            fadeBed(0f, FADE_OUT_MS)
        }
    }
}
```

- [ ] **Step 9: Give `RitualScreen` a stand-in for the removed grace fraction** (replaced by `AltarRing` in Task 25)

In `app/src/main/java/com/summondemons/safely/ui/screens/RitualScreen.kt`, replace the
argument `graceFraction = ritual.graceFraction,` with:

```kotlin
                    // Stand-in until the altar ring (Task 25): the red arc shows focus spent.
                    graceFraction = if (ritual.clockActive) 1f - ritual.focusFraction else 0f,
```

- [ ] **Step 10: Run every test**

Run: `./gradlew testDebugUnitTest`
Expected: `BUILD SUCCESSFUL`. `RitualEngineTest` 26, `RitualStateTest` 6 and the
existing `RitualViewModelTest` cases pass.

- [ ] **Step 11: Commit**

```bash
git add app/src/main/java/com/summondemons/safely/ritual app/src/main/java/com/summondemons/safely/platform/RitualFeedback.kt \
  app/src/main/java/com/summondemons/safely/ui/screens/RitualScreen.kt app/src/test/java/com/summondemons/safely/ritual
git commit -q -m "feat(ritual): Focus clock replaces the drift grace; seals crack instead of resetting" -m "$TRAILER"
```

- [ ] **Step 12: Phase gate**

Run the verification gate. Expected: `BUILD SUCCESSFUL`.

---
## Phase 3 — Platform

### Task 9: GameClock and debug settings

**Files:**
- Create: `MAIN/time/GameClock.kt`, `MAIN/time/DebugSettings.kt`, `MAIN/platform/SystemGameClock.kt`, `MAIN/platform/DataStoreDebugSettingsStore.kt`
- Create: `TEST/time/GameClockTest.kt`, `TEST/time/FakeGameClock.kt`

- [ ] **Step 1: Write the failing test**

`app/src/test/java/com/summondemons/safely/time/GameClockTest.kt`:

```kotlin
package com.summondemons.safely.time

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class GameClockTest {

    private val zone = ZoneId.of("America/Sao_Paulo")
    private fun at(hour: Int, minute: Int = 0, second: Int = 0) = ZonedDateTime.of(2026, 10, 16, hour, minute, second, 0, zone)

    @Test
    fun epochDayIsTheLocalCalendarDay() {
        assertEquals(LocalDate.of(2026, 10, 16).toEpochDay(), at(0).epochDay())
        assertEquals(LocalDate.of(2026, 10, 16).toEpochDay(), at(23, 59, 59).epochDay())
        assertEquals(at(0).epochDay() + 1, at(23, 59, 59).plusSeconds(1).epochDay())
    }

    @Test
    fun hoursToMidnightRoundUpAndAreAtLeastOne() {
        assertEquals(24, hoursToMidnight(at(0)))
        assertEquals(7, hoursToMidnight(at(17)))
        assertEquals(8, hoursToMidnight(at(16, 30))) // 7.5 h rounds up
        assertEquals(1, hoursToMidnight(at(23, 30)))
        assertEquals(1, hoursToMidnight(at(23, 59, 59)))
    }
}
```

- [ ] **Step 2: Run it to see it fail**

Run: `./gradlew testDebugUnitTest --tests '*GameClockTest*'`
Expected: compilation fails (`epochDay`, `hoursToMidnight` unresolved).

- [ ] **Step 3: Write `GameClock`**

`app/src/main/java/com/summondemons/safely/time/GameClock.kt`:

```kotlin
package com.summondemons.safely.time

import kotlinx.coroutines.flow.Flow
import java.time.Duration
import java.time.ZonedDateTime
import kotlin.math.ceil

private const val MS_PER_HOUR = 3_600_000.0

/** Local time for the game (§3). In debug builds the debug day offset is already applied. */
interface GameClock {
    fun now(): ZonedDateTime

    /**
     * Emits [now] on collection, at every top of the hour (this covers midnight), on system
     * time / zone / date changes and whenever the debug settings change.
     */
    val ticks: Flow<ZonedDateTime>
}

/** Local epoch day: the unit of every progress rule. */
fun ZonedDateTime.epochDay(): Long = toLocalDate().toEpochDay()

/** Whole hours to the next local midnight, rounded up, at least 1 ("Next summon in {h} h"). */
fun hoursToMidnight(now: ZonedDateTime): Int {
    val midnight = now.toLocalDate().plusDays(1).atStartOfDay(now.zone)
    return maxOf(1, ceil(Duration.between(now, midnight).toMillis() / MS_PER_HOUR).toInt())
}
```

- [ ] **Step 4: Write `DebugSettings`**

`app/src/main/java/com/summondemons/safely/time/DebugSettings.kt`:

```kotlin
package com.summondemons.safely.time

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class CourtOverride { Auto, In, Out }

/** Debug-only knobs (§7): a whole-day clock offset and a Court Hours override for every House. */
data class DebugSettings(val dayOffset: Int = 0, val court: CourtOverride = CourtOverride.Auto)

interface DebugSettingsStore {
    val settings: StateFlow<DebugSettings>
    suspend fun update(transform: (DebugSettings) -> DebugSettings)
}

/** Release builds: always the defaults; changes are ignored. */
object NoDebugSettings : DebugSettingsStore {
    override val settings: StateFlow<DebugSettings> = MutableStateFlow(DebugSettings()).asStateFlow()
    override suspend fun update(transform: (DebugSettings) -> DebugSettings) = Unit
}
```

- [ ] **Step 5: Write the Android clock**

`app/src/main/java/com/summondemons/safely/platform/SystemGameClock.kt`:

```kotlin
package com.summondemons.safely.platform

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import com.summondemons.safely.time.DebugSettings
import com.summondemons.safely.time.GameClock
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import java.time.Duration
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

/** Lands each hourly tick just after the hour turns. */
private const val TOP_OF_HOUR_SLACK_MS = 50L

/** Device-local time plus the debug day offset (§3). */
class SystemGameClock(context: Context, private val debug: StateFlow<DebugSettings>) : GameClock {

    private val appContext = context.applicationContext

    override fun now(): ZonedDateTime = ZonedDateTime.now().plusDays(debug.value.dayOffset.toLong())

    override val ticks: Flow<ZonedDateTime> =
        merge(systemTimeChanges(), topsOfTheHour(), debug.drop(1).map { })
            .map { now() }
            .onStart { emit(now()) }
            .conflate()

    private fun systemTimeChanges(): Flow<Unit> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                trySend(Unit)
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
            addAction(Intent.ACTION_DATE_CHANGED)
        }
        // System broadcasts still reach a non-exported receiver.
        ContextCompat.registerReceiver(appContext, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        awaitClose { appContext.unregisterReceiver(receiver) }
    }

    private fun topsOfTheHour(): Flow<Unit> = flow {
        while (true) {
            val now = ZonedDateTime.now()
            val next = now.truncatedTo(ChronoUnit.HOURS).plusHours(1)
            delay(Duration.between(now, next).toMillis() + TOP_OF_HOUR_SLACK_MS)
            emit(Unit)
        }
    }
}
```

- [ ] **Step 6: Write the debug settings DataStore**

`app/src/main/java/com/summondemons/safely/platform/DataStoreDebugSettingsStore.kt`:

```kotlin
package com.summondemons.safely.platform

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.summondemons.safely.time.CourtOverride
import com.summondemons.safely.time.DebugSettings
import com.summondemons.safely.time.DebugSettingsStore
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.io.IOException

private const val TAG = "DebugSettings"
private val DAY_OFFSET = intPreferencesKey("day_offset")
private val COURT = stringPreferencesKey("court")

/** A separate file, excluded from backup (ADR-0013). Process-wide: one delegate per file. */
private val Context.debugSettingsDataStore by preferencesDataStore(name = "debug_settings")

/** Debug builds only. [settings] starts at the defaults and follows the file once loaded. */
class DataStoreDebugSettingsStore(context: Context, scope: CoroutineScope) : DebugSettingsStore {

    private val dataStore = context.applicationContext.debugSettingsDataStore
    private val state = MutableStateFlow(DebugSettings())
    private val firstLoad = CompletableDeferred<Unit>()

    override val settings: StateFlow<DebugSettings> = state.asStateFlow()

    init {
        scope.launch {
            dataStore.data
                .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
                .collect { prefs ->
                    state.value = prefs.toSettings()
                    firstLoad.complete(Unit)
                }
        }
    }

    suspend fun awaitLoaded() = firstLoad.await()

    override suspend fun update(transform: (DebugSettings) -> DebugSettings) {
        try {
            dataStore.edit { prefs ->
                val next = transform(prefs.toSettings())
                prefs[DAY_OFFSET] = next.dayOffset
                prefs[COURT] = next.court.name
            }
        } catch (e: IOException) {
            Log.w(TAG, "debug settings write failed", e)
        }
    }

    private fun Preferences.toSettings() = DebugSettings(
        dayOffset = this[DAY_OFFSET] ?: 0,
        court = this[COURT]?.let { name -> CourtOverride.entries.firstOrNull { it.name == name } } ?: CourtOverride.Auto,
    )
}
```

- [ ] **Step 7: Add the test fakes used from Task 12 on**

`app/src/test/java/com/summondemons/safely/time/FakeGameClock.kt`:

```kotlin
package com.summondemons.safely.time

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.ZonedDateTime

/** A clock tests move by hand; every change is a tick. */
class FakeGameClock(start: ZonedDateTime) : GameClock {
    val time = MutableStateFlow(start)
    override fun now(): ZonedDateTime = time.value
    override val ticks: Flow<ZonedDateTime> = time

    fun advanceDays(days: Long) = time.update { it.plusDays(days) }
    fun setHour(hour: Int) = time.update { it.withHour(hour) }
}

class FakeDebugSettings(initial: DebugSettings = DebugSettings()) : DebugSettingsStore {
    private val state = MutableStateFlow(initial)
    override val settings: StateFlow<DebugSettings> = state.asStateFlow()
    override suspend fun update(transform: (DebugSettings) -> DebugSettings) = state.update(transform)
}
```

- [ ] **Step 8: Run the tests**

Run: `./gradlew testDebugUnitTest --tests '*GameClockTest*'`
Expected: 2 tests pass. Then `./gradlew assembleDebug` → `BUILD SUCCESSFUL`.

- [ ] **Step 9: Commit**

```bash
git add app/src/main/java/com/summondemons/safely/time app/src/main/java/com/summondemons/safely/platform \
  app/src/test/java/com/summondemons/safely/time
git commit -q -m "feat(time): Game clock with hourly and system-change ticks; debug settings store" -m "$TRAILER"
```

### Task 10: ProgressStore over Preferences DataStore

**Files:**
- Create: `MAIN/progress/ProgressStore.kt`, `MAIN/platform/DataStoreProgressStore.kt`
- Create: `TEST/progress/InMemoryProgressStore.kt`, `TEST/platform/DataStoreProgressStoreTest.kt`
- Rewrite: `RES/xml/backup_rules.xml`, `RES/xml/data_extraction_rules.xml`

- [ ] **Step 1: Write the interface**

`app/src/main/java/com/summondemons/safely/progress/ProgressStore.kt`:

```kotlin
package com.summondemons.safely.progress

import kotlinx.coroutines.flow.Flow

/** The single source of truth for progress (ADR-0013). */
interface ProgressStore {
    /** The stored progress; emits after every change. Unreadable data reads as `Progress()`. */
    val progress: Flow<Progress>

    /** Applies [transform] to the latest stored value and saves it. A failed write is logged and lost. */
    suspend fun update(transform: (Progress) -> Progress)
}
```

- [ ] **Step 2: Write the failing DataStore test**

DataStore's Preferences core runs on the JVM, so the store is tested against a real
file in a temporary folder.

`app/src/test/java/com/summondemons/safely/platform/DataStoreProgressStoreTest.kt`:

```kotlin
package com.summondemons.safely.platform

import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.summondemons.safely.progress.Progress
import com.summondemons.safely.progress.ProgressRules
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.Collections

class DataStoreProgressStoreTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val logs: MutableList<String> = Collections.synchronizedList(mutableListOf())
    private val dataStore by lazy {
        PreferenceDataStoreFactory.create(
            corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
            scope = scope,
            produceFile = { file },
        )
    }
    private val file by lazy { File(folder.root, "progress.preferences_pb") }

    private fun store() = DataStoreProgressStore(dataStore) { message, _ -> logs += message }

    @After
    fun tearDown() = scope.cancel()

    @Test
    fun anEmptyFileReadsFreshProgress() = runBlocking {
        assertEquals(Progress(), store().progress.first())
        assertTrue(logs.isEmpty())
    }

    @Test
    fun updatesApplyToTheLatestValueAndRoundTrip() = runBlocking {
        val store = store()
        store.update { ProgressRules.commitSummon(it, "asmodeus", 10, flawless = true) }
        store.update { ProgressRules.visit(it, "asmodeus", 11) }
        val record = store.progress.first().record("asmodeus")
        assertTrue(record.flawless)
        assertEquals(11L, record.lastVisitDay)
        // A second store over the same DataStore reads the same value.
        assertEquals(11L, store().progress.first().record("asmodeus").lastVisitDay)
    }

    @Test
    fun corruptDataReadsFreshAndIsLoggedOnce() = runBlocking {
        dataStore.edit { it[stringPreferencesKey("progress_v1")] = "{nope" }
        val store = store()
        assertEquals(Progress(), store.progress.first())
        assertEquals(Progress(), store.progress.first())
        assertEquals(1, logs.size)
    }

    @Test
    fun aCorruptFileIsReplacedAndWritesKeepWorking() = runBlocking {
        file.writeBytes(byteArrayOf(0x7f, 0x00, 0x13, 0x37, 0x42))
        val store = store()
        assertEquals(Progress(), store.progress.first())
        store.update { it.copy(homeHintSeen = true) }
        assertEquals(Progress(homeHintSeen = true), store.progress.first())
    }

    @Test
    fun anUpdateOverCorruptDataStartsFromFreshProgress() = runBlocking {
        dataStore.edit { it[stringPreferencesKey("progress_v1")] = "{nope" }
        val store = store()
        store.update { it.copy(homeHintSeen = true) }
        assertEquals(Progress(homeHintSeen = true), store.progress.first())
    }
}
```

- [ ] **Step 3: Run it to see it fail**

Run: `./gradlew testDebugUnitTest --tests '*DataStoreProgressStoreTest*'`
Expected: compilation fails (`DataStoreProgressStore` unresolved).

- [ ] **Step 4: Implement the store**

`app/src/main/java/com/summondemons/safely/platform/DataStoreProgressStore.kt`:

```kotlin
package com.summondemons.safely.platform

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.summondemons.safely.progress.Progress
import com.summondemons.safely.progress.ProgressJson
import com.summondemons.safely.progress.ProgressStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retryWhen
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

private const val TAG = "ProgressStore"
private const val READ_RETRY_MS = 1_000L
private val PROGRESS_KEY = stringPreferencesKey("progress_v1")

/**
 * Process-wide singleton: a top-level delegate, always used through the application context.
 * A corrupt file is replaced by an empty one, so writes keep working (spec §10).
 */
private val Context.progressDataStore by preferencesDataStore(
    name = "progress",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
)

fun Context.progressStore(): ProgressStore = DataStoreProgressStore(applicationContext.progressDataStore)

/** `Progress` as kotlinx-serialization JSON under one string key (ADR-0013). */
class DataStoreProgressStore(
    private val dataStore: DataStore<Preferences>,
    private val log: (String, Throwable?) -> Unit = { message, error -> Log.w(TAG, message, error) },
) : ProgressStore {

    private val corruptLogged = AtomicBoolean(false)

    override val progress: Flow<Progress> = dataStore.data
        // An unreadable file reads as Progress() but keeps the flow alive (a plain `catch`
        // would complete it), so later successful writes still reach the UI.
        .retryWhen { e, _ ->
            if (e !is IOException) return@retryWhen false
            log("progress unreadable; starting fresh", e)
            emit(emptyPreferences())
            delay(READ_RETRY_MS)
            true
        }
        .map { decode(it[PROGRESS_KEY]) }

    override suspend fun update(transform: (Progress) -> Progress) {
        try {
            dataStore.edit { prefs -> prefs[PROGRESS_KEY] = ProgressJson.encode(transform(decode(prefs[PROGRESS_KEY]))) }
        } catch (e: IOException) {
            log("progress write failed; change lost", e)
        }
    }

    private fun decode(raw: String?): Progress {
        if (raw == null) return Progress()
        return ProgressJson.decodeOrNull(raw) ?: Progress().also {
            if (corruptLogged.compareAndSet(false, true)) log("progress corrupt; starting fresh", null)
        }
    }
}
```

- [ ] **Step 5: Add the in-memory store used by ViewModel tests**

`app/src/test/java/com/summondemons/safely/progress/InMemoryProgressStore.kt`:

```kotlin
package com.summondemons.safely.progress

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update

/** A store whose first value can be held back ([loaded] = false until [load]); counts every update. */
class InMemoryProgressStore(initial: Progress = Progress(), loaded: Boolean = true) : ProgressStore {
    val state = MutableStateFlow(initial)
    private val gate = CompletableDeferred<Unit>().apply { if (loaded) complete(Unit) }
    var updates = 0
        private set

    override val progress: Flow<Progress> = flow {
        gate.await()
        emitAll(state)
    }

    fun load() {
        gate.complete(Unit)
    }

    override suspend fun update(transform: (Progress) -> Progress) {
        gate.await()
        updates++
        state.update(transform)
    }
}
```

- [ ] **Step 6: Exclude the debug settings from backup**

`app/src/main/res/xml/backup_rules.xml` (Auto Backup, API ≤ 30):

```xml
<?xml version="1.0" encoding="utf-8"?>
<!-- Everything is backed up except the debug-only settings (ADR-0013). -->
<full-backup-content>
    <exclude domain="file" path="datastore/debug_settings.preferences_pb" />
</full-backup-content>
```

`app/src/main/res/xml/data_extraction_rules.xml` (API 31+):

```xml
<?xml version="1.0" encoding="utf-8"?>
<!-- Everything is backed up or transferred except the debug-only settings (ADR-0013). -->
<data-extraction-rules>
    <cloud-backup>
        <exclude domain="file" path="datastore/debug_settings.preferences_pb" />
    </cloud-backup>
    <device-transfer>
        <exclude domain="file" path="datastore/debug_settings.preferences_pb" />
    </device-transfer>
</data-extraction-rules>
```

- [ ] **Step 7: Run the tests**

Run: `./gradlew testDebugUnitTest --tests '*DataStoreProgressStoreTest*'`
Expected: 5 tests pass.

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/summondemons/safely/progress app/src/main/java/com/summondemons/safely/platform \
  app/src/main/res/xml app/src/test
git commit -q -m "feat(progress): Persist progress in Preferences DataStore; exclude debug settings from backup" -m "$TRAILER"
```

### Task 11: RitualViewModel — court, commit, active, heading hold

**Files:**
- Rewrite: `MAIN/ritual/RitualViewModel.kt`, `TEST/ritual/RitualViewModelTest.kt`
- Modify: `MAIN/ui/screens/RitualScreen.kt`, `MAIN/navigation/AppNavHost.kt`

- [ ] **Step 1: Rewrite the test**

`app/src/test/java/com/summondemons/safely/ritual/RitualViewModelTest.kt`:

```kotlin
package com.summondemons.safely.ritual

import com.summondemons.safely.data.Demon
import com.summondemons.safely.data.DemonCatalog
import com.summondemons.safely.ritual.RitualConfig.COURT_BONUS
import com.summondemons.safely.ritual.RitualConfig.FRAME_MS
import com.summondemons.safely.ritual.RitualEffect.ClimaxStart
import com.summondemons.safely.ritual.RitualEffect.Locked
import com.summondemons.safely.ritual.RitualEffect.ProximityTick
import com.summondemons.safely.ritual.RitualEffect.Sealed
import com.summondemons.safely.ritual.RitualEffect.Summoned
import com.summondemons.safely.ritual.RitualEffect.SyllableLit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

@OptIn(ExperimentalCoroutinesApi::class)
class RitualViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val demon = DemonCatalog.byId("asmodeus")!!

    private class FakeHeading(override val available: Boolean = true) : HeadingSource {
        val readings = MutableSharedFlow<Float>(replay = 1)
        override fun azimuths(): Flow<Float> = readings
    }

    /** 16 kHz frames every 20 ms; [amplitude] gives the 150 Hz sine amplitude of frame i (0 = silence). */
    private class FakePcm(private val amplitude: (Int) -> Double) : PcmSource {
        var subscriptions = 0
        override fun frames(): Flow<PcmFrame> = flow {
            subscriptions++
            var f = 0
            while (true) {
                delay(FRAME_MS)
                val amp = amplitude(f)
                val samples = ShortArray(320) { i -> (amp * sin(2 * PI * 150.0 * (f * 320 + i) / 16_000)).toInt().toShort() }
                emit(PcmFrame(samples, samples.size, 16_000))
                f++
            }
        }
    }

    private fun silentPcm() = FakePcm { 0.0 }

    /** 10 voiced frames then 5 silent: one syllable every 300 ms. */
    private fun chantingPcm() = FakePcm { if (it % 15 < 10) 8_000.0 else 0.0 }

    private fun TestScope.observe(vm: RitualViewModel) = backgroundScope.launch { vm.uiState.collect {} }

    private fun newVm(
        demon: Demon,
        heading: HeadingSource,
        pcm: PcmSource,
        inCourt: () -> Boolean = { false },
        commitSummon: (Boolean) -> Unit = {},
    ) = RitualViewModel(demon, heading, pcm, inCourt, commitSummon, analysisDispatcher = dispatcher)

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun holdToChantWithSimulatedHeadingCompletesTheRitual() = runTest(dispatcher) {
        val pcm = silentPcm()
        val vm = newVm(demon, { emptyFlow() }, pcm)
        observe(vm)
        val effects = mutableListOf<RitualEffect>()
        backgroundScope.launch { vm.effects.collect { effects += it } }
        vm.onMicPermission(false)
        vm.setSimulateHeading(true)
        vm.setHoldToChant(true)

        advanceTimeBy(20_000); runCurrent()

        assertEquals(RitualPhase.Done, vm.uiState.value.ritual.phase)
        assertEquals(
            listOf(Locked, Sealed(0), Sealed(1), Sealed(2), Sealed(3), ClimaxStart(flawless = true), Summoned),
            effects.filter { it != SyllableLit && it != ProximityTick },
        )
        assertEquals(0, pcm.subscriptions)
    }

    @Test
    fun commitSummonIsCalledOnceAtTheClimaxStartEvenWithoutAnEffectCollector() = runTest(dispatcher) {
        val commits = mutableListOf<Boolean>()
        val vm = newVm(demon, { emptyFlow() }, silentPcm(), commitSummon = { commits += it })
        observe(vm)
        vm.onMicPermission(false)
        vm.setSimulateHeading(true)
        vm.setHoldToChant(true)

        while (vm.uiState.value.ritual.phase != RitualPhase.Climax) {
            assertTrue("no commit before the climax", commits.isEmpty())
            advanceTimeBy(FRAME_MS); runCurrent()
        }
        assertEquals(listOf(true), commits) // saved before the climax plays

        advanceTimeBy(5_000); runCurrent()
        assertEquals(RitualPhase.Done, vm.uiState.value.ritual.phase)
        assertEquals(listOf(true), commits)
    }

    @Test
    fun inCourtAtTheLockReachesTheEngine() = runTest(dispatcher) {
        val vm = newVm(demon, { emptyFlow() }, silentPcm(), inCourt = { true })
        observe(vm)
        vm.onMicPermission(false)
        vm.setSimulateHeading(true)
        advanceTimeBy(1_000); runCurrent()
        assertEquals(RitualPhase.Chanting, vm.uiState.value.ritual.phase)
        assertEquals(1f + COURT_BONUS, vm.uiState.value.ritual.courtMultiplier, 0.0001f)
    }

    @Test
    fun noTicksWhileInactive() = runTest(dispatcher) {
        val vm = newVm(demon, { emptyFlow() }, silentPcm())
        observe(vm)
        vm.setActive(false)
        runCurrent()
        assertFalse(vm.uiState.value.active)
        vm.onMicPermission(false)
        vm.setSimulateHeading(true)
        vm.setHoldToChant(true)
        advanceTimeBy(2_000); runCurrent()
        assertEquals(RitualPhase.Seeking, vm.uiState.value.ritual.phase)
        assertEquals(0f, vm.uiState.value.micLevel)

        vm.setActive(true)
        advanceTimeBy(1_000); runCurrent()
        assertEquals(RitualPhase.Chanting, vm.uiState.value.ritual.phase)
    }

    @Test
    fun ticksAreHeldUntilTheFirstHeadingArrives() = runTest(dispatcher) {
        val heading = FakeHeading()
        val vm = newVm(demon, heading, silentPcm())
        observe(vm)
        vm.onMicPermission(false)
        vm.setHoldToChant(true)
        advanceTimeBy(1_000); runCurrent()
        assertEquals(RitualPhase.Seeking, vm.uiState.value.ritual.phase)
        assertEquals(0f, vm.uiState.value.micLevel) // no tick ran

        heading.readings.emit(demon.ritualHeading)
        advanceTimeBy(1_000); runCurrent()
        assertEquals(RitualPhase.Chanting, vm.uiState.value.ritual.phase)
        assertTrue(vm.uiState.value.micLevel > 0f)
    }

    @Test
    fun devicesWithoutAHeadingSensorStillTick() = runTest(dispatcher) {
        val vm = newVm(demon, FakeHeading(available = false), silentPcm())
        observe(vm)
        vm.onMicPermission(false)
        vm.setHoldToChant(true)
        advanceTimeBy(500); runCurrent()
        assertTrue(vm.uiState.value.micLevel > 0f)
        assertEquals(RitualPhase.Seeking, vm.uiState.value.ritual.phase)
    }

    @Test
    fun voicedMicLightsSyllables() = runTest(dispatcher) {
        val vm = newVm(demon, { emptyFlow() }, chantingPcm())
        observe(vm)
        vm.onMicPermission(true)
        vm.setSimulateHeading(true)
        advanceTimeBy(2_000); runCurrent()
        val ritual = vm.uiState.value.ritual
        assertEquals(RitualPhase.Chanting, ritual.phase)
        assertTrue("lit=${ritual.lit}", ritual.lit >= 4)
        assertTrue(vm.uiState.value.micLevel > 0f)
    }

    @Test
    fun silentMicLocksButDoesNotChant() = runTest(dispatcher) {
        val vm = newVm(demon, { emptyFlow() }, silentPcm())
        observe(vm)
        vm.onMicPermission(true)
        vm.setSimulateHeading(true)
        advanceTimeBy(5_000); runCurrent()
        assertEquals(RitualPhase.Chanting, vm.uiState.value.ritual.phase)
        assertEquals(0, vm.uiState.value.ritual.lit)
    }

    @Test
    fun driftWaversWithoutLosingProgressAndNeverReturnsToSeeking() = runTest(dispatcher) {
        val heading = FakeHeading()
        val vm = newVm(demon, heading, silentPcm())
        observe(vm)
        vm.onMicPermission(false)
        vm.setHoldToChant(true)
        heading.readings.emit(demon.ritualHeading)
        advanceTimeBy(1_500); runCurrent()
        val lit = vm.uiState.value.ritual.lit
        assertTrue("lit=$lit", lit > 0)
        assertEquals(0, vm.uiState.value.ritual.phrase) // still phrase I: no clock

        vm.setHoldToChant(false)
        heading.readings.emit(demon.ritualHeading + 90f)
        advanceTimeBy(5_000); runCurrent()
        assertEquals(RitualPhase.Wavering, vm.uiState.value.ritual.phase)
        assertEquals(lit, vm.uiState.value.ritual.lit)
    }

    @Test
    fun simulateHeadingAlignsWithoutASensor() = runTest(dispatcher) {
        val vm = newVm(demon, { emptyFlow() }, silentPcm())
        observe(vm)
        runCurrent()
        assertNull(vm.uiState.value.azimuth)

        vm.setSimulateHeading(true)
        advanceTimeBy(2 * FRAME_MS); runCurrent()
        assertEquals(demon.ritualHeading, vm.uiState.value.azimuth)
        assertTrue(vm.uiState.value.ritual.aligned)

        vm.setSimulateHeading(false)
        runCurrent()
        assertNull(vm.uiState.value.azimuth)
    }

    @Test
    fun restartingThePipelineForgetsTheOldHeading() = runTest(dispatcher) {
        var subscriptions = 0
        val vm = newVm(demon, { flow { if (subscriptions++ == 0) emit(demon.ritualHeading) } }, silentPcm())
        val first = backgroundScope.launch { vm.uiState.collect {} }
        runCurrent()
        assertEquals(demon.ritualHeading, vm.uiState.value.azimuth)

        first.cancel()
        advanceTimeBy(6_000); runCurrent() // past WhileSubscribed(5_000): pipeline stops
        observe(vm)
        runCurrent()
        assertNull(vm.uiState.value.azimuth)
    }

    @Test
    fun micThatEndsImmediatelyIsReportedUnavailableAndTheRitualStillTicks() = runTest(dispatcher) {
        val vm = newVm(demon, { emptyFlow() }, { emptyFlow() })
        observe(vm)
        vm.onMicPermission(true)
        runCurrent()
        assertTrue(vm.uiState.value.micUnavailable)

        vm.setSimulateHeading(true)
        vm.setHoldToChant(true)
        advanceTimeBy(1_500); runCurrent()
        assertTrue(vm.uiState.value.ritual.lit > 0)
    }

    @Test
    fun micThatThrowsIsReportedUnavailable() = runTest(dispatcher) {
        val vm = newVm(demon, { emptyFlow() }, { flow { throw IllegalStateException("mic busy") } })
        observe(vm)
        vm.onMicPermission(true)
        runCurrent()
        assertTrue(vm.uiState.value.micUnavailable)
    }

    @Test
    fun workingMicIsNotReportedUnavailable() = runTest(dispatcher) {
        val vm = newVm(demon, { emptyFlow() }, chantingPcm())
        observe(vm)
        vm.onMicPermission(true)
        advanceTimeBy(1_000); runCurrent()
        assertFalse(vm.uiState.value.micUnavailable)
    }

    @Test
    fun headingChangesDoNotRestartTheMic() = runTest(dispatcher) {
        val heading = FakeHeading()
        val pcm = chantingPcm()
        val vm = newVm(demon, heading, pcm)
        observe(vm)
        vm.onMicPermission(true)
        heading.readings.emit(demon.ritualHeading); advanceTimeBy(300); runCurrent()
        heading.readings.emit(demon.ritualHeading + 90f); advanceTimeBy(300); runCurrent()
        heading.readings.emit(demon.ritualHeading); advanceTimeBy(300); runCurrent()
        assertEquals(1, pcm.subscriptions)
    }

    @Test
    fun headingAvailabilityComesFromTheSource() = runTest(dispatcher) {
        assertTrue(newVm(demon, FakeHeading(available = false), silentPcm()).uiState.value.headingUnavailable)
        assertFalse(newVm(demon, FakeHeading(available = true), silentPcm()).uiState.value.headingUnavailable)
    }
}
```

- [ ] **Step 2: Run it to see it fail**

Run: `./gradlew testDebugUnitTest --tests '*RitualViewModelTest*'`
Expected: compilation fails (`setActive`, `active`, the new constructor parameters).

- [ ] **Step 3: Rewrite `RitualViewModel`**

`app/src/main/java/com/summondemons/safely/ritual/RitualViewModel.kt`:

```kotlin
package com.summondemons.safely.ritual

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.summondemons.safely.data.Demon
import com.summondemons.safely.ritual.RitualConfig.FRAME_MS
import com.summondemons.safely.ritual.RitualConfig.HOLD_TO_CHANT_ONSET_FRAMES
import com.summondemons.safely.ritual.RitualConfig.HOLD_TO_CHANT_RMS
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RitualUiState(
    val demon: Demon,
    val ritual: RitualState,
    /** Latest heading reading (fresher than `ritual.azimuth`, which updates per tick). */
    val azimuth: Float? = null,
    /** Smoothed mic level in [0, 1]; drives glow only. */
    val micLevel: Float = 0f,
    /** null = not checked yet. */
    val micGranted: Boolean? = null,
    val simulateHeading: Boolean = false,
    val holdToChant: Boolean = false,
    /** Mic was granted but stopped or never started (busy, unsupported); the ritual can't hear. */
    val micUnavailable: Boolean = false,
    /** The device has no heading sensor; the ritual can't lock (debug can simulate). */
    val headingUnavailable: Boolean = false,
    /** The Ritual screen is started; no ticks run otherwise, so the focus clock pauses (§2). */
    val active: Boolean = true,
)

@OptIn(ExperimentalCoroutinesApi::class)
class RitualViewModel(
    private val demon: Demon,
    private val headingSource: HeadingSource,
    private val pcmSource: PcmSource,
    /** Whether the demon's House is in court now (debug override applied); read every tick, used at lock. */
    private val inCourt: () -> Boolean = { false },
    /** Saves the summon. Called inside the tick, exactly once, on the step that starts the climax (ADR-0013). */
    private val commitSummon: (flawless: Boolean) -> Unit = {},
    private val analysisDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {

    private val state = MutableStateFlow(
        RitualUiState(
            demon = demon,
            ritual = RitualState.start(demon.ritualHeading, demon.chantPhrases),
            headingUnavailable = !headingSource.available,
        ),
    )
    private val _effects = MutableSharedFlow<RitualEffect>(extraBufferCapacity = EFFECT_BUFFER)

    /** One-shot events for haptics, sounds and flashes. Lossy by design: nothing durable depends on them. */
    val effects: SharedFlow<RitualEffect> = _effects.asSharedFlow()

    private var holdFrames = 0

    private val azimuths: Flow<Float> = state
        .map { it.simulateHeading }
        .distinctUntilChanged()
        .flatMapLatest { simulate -> if (simulate) flowOf(demon.ritualHeading) else headingSource.azimuths() }

    private val voiceFrames: Flow<VoiceFrame> = state
        .map { it.micGranted }
        .distinctUntilChanged()
        .flatMapLatest { granted -> if (granted == true) micFrames() else silentFrames() }

    val uiState: StateFlow<RitualUiState> = channelFlow {
        // On (re)start the sensors restart too: forget the last reading so ticks never run on a stale heading.
        state.update { if (it.simulateHeading) it else it.copy(azimuth = null) }
        launch { azimuths.collect { az -> state.update { it.copy(azimuth = az) } } }
        launch { voiceFrames.collect(::tick) }
        state.collect { send(it) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), state.value)

    fun onMicPermission(granted: Boolean) = state.update { it.copy(micGranted = granted) }

    fun setSimulateHeading(on: Boolean) = state.update {
        if (on) it.copy(simulateHeading = true) else it.copy(simulateHeading = false, azimuth = null)
    }

    fun setHoldToChant(on: Boolean) {
        if (!on) holdFrames = 0
        state.update { it.copy(holdToChant = on) }
    }

    /** Called from the Ritual screen's ON_START / ON_STOP. */
    fun setActive(active: Boolean) = state.update { it.copy(active = active) }

    /** One 20 ms tick. Only this collector changes `ritual`, so read-then-update is safe. */
    private fun tick(micFrame: VoiceFrame) {
        val current = state.value
        if (!current.active || awaitingHeading(current)) return
        val voice = if (current.holdToChant) syntheticVoice() else micFrame
        val step = RitualEngine.step(current.ritual, RitualInput(current.azimuth, voice, inCourt()))
        // The summon is saved here, not through the lossy effects flow, before the climax plays.
        step.effects.filterIsInstance<RitualEffect.ClimaxStart>().firstOrNull()?.let { commitSummon(it.flawless) }
        state.update { it.copy(ritual = step.state, micLevel = smoothMicLevel(it.micLevel, voice.rms)) }
        step.effects.forEach { _effects.tryEmit(it) }
    }

    /**
     * After a (re)start, ticks wait for the first heading reading, so a return from the
     * background never reads as off heading (no drift, no 3× drain, no crack). Devices
     * without a heading sensor tick with a null heading.
     */
    private fun awaitingHeading(s: RitualUiState): Boolean =
        s.azimuth == null && !s.simulateHeading && !s.headingUnavailable

    /** Debug "Hold to Chant": continuous voicing with an onset every ~240 ms. */
    private fun syntheticVoice(): VoiceFrame =
        VoiceFrame(HOLD_TO_CHANT_RMS, voiced = true, onset = holdFrames++ % HOLD_TO_CHANT_ONSET_FRAMES == 0)

    /** Mic frames → analysed voice; if the mic dies (error or end), flag it and fall back to silence. */
    private fun micFrames(): Flow<VoiceFrame> = flow {
        var analyzer: VoiceAnalyzer? = null
        pcmSource.frames().collect { frame ->
            val current = analyzer?.takeIf { it.sampleRate == frame.sampleRate }
                ?: VoiceAnalyzer(frame.sampleRate).also { analyzer = it }
            emit(current.analyze(frame.samples, frame.count))
        }
    }
        .flowOn(analysisDispatcher)
        .onStart { state.update { it.copy(micUnavailable = false) } }
        .catch { }
        .onCompletion { cause ->
            // cause != null only on cancellation (permission change, screen gone): not a mic failure.
            if (cause == null) {
                state.update { it.copy(micUnavailable = true) }
                emitAll(silentFrames())
            }
        }

    private fun silentFrames(): Flow<VoiceFrame> = flow {
        while (true) {
            delay(FRAME_MS)
            emit(VoiceFrame.SILENT)
        }
    }

    companion object {
        private const val EFFECT_BUFFER = 64

        fun factory(
            demon: Demon,
            headingSource: HeadingSource,
            pcmSource: PcmSource,
            inCourt: () -> Boolean = { false },
            commitSummon: (flawless: Boolean) -> Unit = {},
        ): ViewModelProvider.Factory =
            viewModelFactory { initializer { RitualViewModel(demon, headingSource, pcmSource, inCourt, commitSummon) } }
    }
}
```

- [ ] **Step 4: Pause the ritual while the screen is stopped**

In `app/src/main/java/com/summondemons/safely/ui/screens/RitualScreen.kt`, in
`RitualScreen`, right after the line `RitualFeedback(viewModel.effects, state.demon.ambientBed)`, add:

```kotlin
    // The focus clock only runs while the screen is started (§2).
    LifecycleEventEffect(Lifecycle.Event.ON_START) { viewModel.setActive(true) }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.setActive(false) }
```

(`Lifecycle` and `LifecycleEventEffect` are already imported.)

- [ ] **Step 5: Commit the summon at the climax start**

In `app/src/main/java/com/summondemons/safely/navigation/AppNavHost.kt`, in the
`composable<Ritual>` block, replace

```kotlin
                            val factory = remember(demon) {
                                RitualViewModel.factory(demon, AndroidHeadingSource(appContext), AndroidPcmSource())
                            }
```

with

```kotlin
                            val factory = remember(demon) {
                                RitualViewModel.factory(
                                    demon,
                                    AndroidHeadingSource(appContext),
                                    AndroidPcmSource(),
                                    commitSummon = { demonViewModel.unlock(demon.id) },
                                )
                            }
```

and in `onSummoned` delete the line `demonViewModel.unlock(demon.id)` (the summon is
already committed).

- [ ] **Step 6: Run the tests**

Run: `./gradlew testDebugUnitTest`
Expected: `BUILD SUCCESSFUL`; `RitualViewModelTest` 16 tests pass.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/summondemons/safely/ritual app/src/main/java/com/summondemons/safely/ui/screens/RitualScreen.kt \
  app/src/main/java/com/summondemons/safely/navigation/AppNavHost.kt app/src/test/java/com/summondemons/safely/ritual
git commit -q -m "feat(ritual): Court at lock, commit at climax start, pause when stopped, wait for a heading" -m "$TRAILER"
```

### Task 12: GameViewModel, app graph and splash

`GameViewModel` replaces `DemonViewModel` in this task; Profile and the old Grimoire
keep working against it until Phase 5.

**Files:**
- Create: `MAIN/ui/GameViewModel.kt`, `MAIN/AppGraph.kt`, `MAIN/SummonApp.kt`, `TEST/ui/GameViewModelTest.kt`
- Modify: `MAIN/MainActivity.kt`, `MAIN/navigation/AppNavHost.kt`, `MAIN/ui/screens/GrimoireScreen.kt`, `app/src/main/AndroidManifest.xml`, `RES/values/themes.xml`
- Delete: `MAIN/ui/DemonViewModel.kt`, `TEST/ui/DemonViewModelTest.kt`

- [ ] **Step 1: Write the failing test**

`app/src/test/java/com/summondemons/safely/ui/GameViewModelTest.kt`:

```kotlin
package com.summondemons.safely.ui

import androidx.lifecycle.SavedStateHandle
import com.summondemons.safely.data.House
import com.summondemons.safely.progress.InMemoryProgressStore
import com.summondemons.safely.progress.Progress
import com.summondemons.safely.progress.ProgressRules
import com.summondemons.safely.progress.Reveal
import com.summondemons.safely.time.CourtOverride
import com.summondemons.safely.time.DebugSettings
import com.summondemons.safely.time.FakeDebugSettings
import com.summondemons.safely.time.FakeGameClock
import com.summondemons.safely.time.epochDay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val start = ZonedDateTime.of(2026, 10, 16, 10, 0, 0, 0, ZoneId.of("UTC"))
    private val day0 = start.epochDay()
    private val clock = FakeGameClock(start)
    private val debug = FakeDebugSettings()

    private fun boundOn(day: Long, id: String = "asmodeus", flawless: Boolean = false) =
        ProgressRules.commitSummon(Progress(), id, day, flawless)

    private fun vm(store: InMemoryProgressStore, saved: SavedStateHandle = SavedStateHandle()) =
        GameViewModel(store, clock, debug, saved)

    private fun TestScope.observe(vm: GameViewModel) = backgroundScope.launch { vm.uiState.collect {} }

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun evaluateWaitsForTheFirstLoadThenRuns() = runTest(dispatcher) {
        val store = InMemoryProgressStore(boundOn(day0 - 3), loaded = false)
        val vm = vm(store)
        observe(vm)
        vm.evaluate(); runCurrent()
        assertFalse(vm.uiState.value.loaded)
        assertEquals(0, store.updates)

        store.load(); runCurrent()
        assertTrue(vm.uiState.value.loaded)
        assertEquals(1, store.updates) // the evaluate after the first load
        val record = store.state.value.record("asmodeus")
        assertFalse(record.bound)
        assertTrue(record.escapeBannerPending)

        vm.evaluate(); runCurrent()
        assertEquals(2, store.updates)
    }

    @Test
    fun theFirstLoadSelectsTheFocusDemonUnlessASelectionWasRestored() = runTest(dispatcher) {
        val progress = boundOn(day0 - 1) // a summon is available today: the unbound demon is the focus
        val fresh = vm(InMemoryProgressStore(progress))
        observe(fresh); runCurrent()
        assertEquals("astaroth", fresh.uiState.value.selectedId)

        val restored = vm(InMemoryProgressStore(progress), SavedStateHandle(mapOf(GameViewModel.KEY_SELECTED to "asmodeus")))
        observe(restored); runCurrent()
        assertEquals("asmodeus", restored.uiState.value.selectedId)
    }

    @Test
    fun aVisitAfterADayChangeUsesTheNewDay() = runTest(dispatcher) {
        val store = InMemoryProgressStore(boundOn(day0))
        val vm = vm(store)
        observe(vm); runCurrent()
        clock.advanceDays(1); runCurrent()
        assertEquals(day0 + 1, vm.uiState.value.today)
        vm.visit("asmodeus"); runCurrent()
        assertEquals(day0 + 1, store.state.value.record("asmodeus").lastVisitDay)
    }

    @Test
    fun aVisitThreeDaysLaterEscapesTheDemonInsteadOfVisitingHim() = runTest(dispatcher) {
        val store = InMemoryProgressStore(boundOn(day0))
        val vm = vm(store)
        observe(vm); runCurrent()
        clock.advanceDays(3)
        vm.visit("asmodeus"); runCurrent()
        val record = store.state.value.record("asmodeus")
        assertFalse(record.bound)
        assertTrue(record.escapeBannerPending)
        assertNull(record.lastVisitDay)
    }

    @Test
    fun openingFromHomeSelectsTheFocusDemonAndHidesTheHint() = runTest(dispatcher) {
        val store = InMemoryProgressStore(boundOn(day0 - 2)) // restless today
        val vm = vm(store)
        observe(vm); runCurrent()
        vm.select("astaroth")
        vm.openGrimoireFromHome(); runCurrent()
        assertEquals("asmodeus", vm.uiState.value.selectedId)
        assertTrue(store.state.value.homeHintSeen)
    }

    @Test
    fun aFlawlessCelebrationOpensTheSecretOnce() = runTest(dispatcher) {
        val vm = vm(InMemoryProgressStore())
        observe(vm); runCurrent()
        vm.celebrate("astaroth", flawless = true); runCurrent()
        assertEquals("astaroth", vm.uiState.value.selectedId)
        assertEquals(Celebration("astaroth", flawless = true), vm.uiState.value.celebration)

        vm.dismissCelebration(); runCurrent()
        assertNull(vm.uiState.value.celebration)
        assertTrue(vm.uiState.value.openSecret)

        vm.secretShown(); runCurrent()
        assertFalse(vm.uiState.value.openSecret)
    }

    @Test
    fun aCrackedSummonDoesNotOpenTheSecret() = runTest(dispatcher) {
        val vm = vm(InMemoryProgressStore())
        observe(vm); runCurrent()
        vm.celebrate("asmodeus", flawless = false)
        vm.dismissCelebration(); runCurrent()
        assertFalse(vm.uiState.value.openSecret)
    }

    @Test
    fun theCommitUsesTheDayTheRitualStarted() = runTest(dispatcher) {
        val store = InMemoryProgressStore()
        val vm = vm(store)
        observe(vm); runCurrent()
        clock.advanceDays(1) // the climax lands after midnight
        vm.commitSummon("asmodeus", day = day0, flawless = false); runCurrent()
        assertEquals(day0, store.state.value.lastSummonDay)
        assertTrue(vm.uiState.value.canSummonToday)
    }

    @Test
    fun everyMutationGoesThroughTheStore() = runTest(dispatcher) {
        val store = InMemoryProgressStore()
        val vm = vm(store)
        observe(vm); runCurrent()
        assertEquals(1, store.updates) // first-load evaluate
        vm.commitSummon("astaroth", day0, flawless = true); runCurrent()
        vm.visit("astaroth"); runCurrent()
        vm.consumeReveal("astaroth", Reveal.Codex); runCurrent()
        vm.dismissEscapeBanners(); runCurrent()
        assertEquals(5, store.updates)
        assertEquals(store.state.value, vm.uiState.value.progress)
    }

    @Test
    fun courtFollowsTheClockAndTheDebugOverride() = runTest(dispatcher) {
        val vm = vm(InMemoryProgressStore())
        observe(vm); runCurrent()
        assertEquals(setOf(House.Sol, House.Venus), vm.uiState.value.courtHouses) // 10:00

        clock.setHour(13); runCurrent()
        assertEquals(emptySet<House>(), vm.uiState.value.courtHouses)
        assertFalse(vm.inCourtNow(House.Sol))

        vm.setCourt(CourtOverride.In); runCurrent()
        assertEquals(setOf(House.Sol, House.Venus), vm.uiState.value.courtHouses)
        assertTrue(vm.inCourtNow(House.Venus))
    }

    @Test
    fun advancingADayMovesTheOffsetAndEvaluatesTheNextDay() = runTest(dispatcher) {
        val store = InMemoryProgressStore(boundOn(day0 - 2))
        val vm = vm(store)
        observe(vm); runCurrent()
        vm.advanceDay(); runCurrent()
        assertEquals(1, debug.settings.value.dayOffset)
        assertFalse(store.state.value.record("asmodeus").bound)
    }

    @Test
    fun resetAllClearsProgressAndDebugSettings() = runTest(dispatcher) {
        val store = InMemoryProgressStore(boundOn(day0))
        val vm = vm(store)
        observe(vm); runCurrent()
        debug.update { DebugSettings(dayOffset = 2, court = CourtOverride.Out) }
        vm.celebrate("asmodeus", flawless = true)
        vm.resetAll(); runCurrent()
        assertEquals(Progress(), store.state.value)
        assertEquals(DebugSettings(), debug.settings.value)
        assertNull(vm.uiState.value.celebration)
    }
}
```

- [ ] **Step 2: Run it to see it fail**

Run: `./gradlew testDebugUnitTest --tests '*GameViewModelTest*'`
Expected: compilation fails (`GameViewModel` unresolved).

- [ ] **Step 3: Write `GameViewModel`**

`app/src/main/java/com/summondemons/safely/ui/GameViewModel.kt`:

```kotlin
package com.summondemons.safely.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.summondemons.safely.data.DemonCatalog
import com.summondemons.safely.data.House
import com.summondemons.safely.progress.CourtHours
import com.summondemons.safely.progress.DemonRecord
import com.summondemons.safely.progress.Progress
import com.summondemons.safely.progress.ProgressRules
import com.summondemons.safely.progress.ProgressStore
import com.summondemons.safely.progress.Reveal
import com.summondemons.safely.time.CourtOverride
import com.summondemons.safely.time.DebugSettings
import com.summondemons.safely.time.DebugSettingsStore
import com.summondemons.safely.time.GameClock
import com.summondemons.safely.time.epochDay
import com.summondemons.safely.time.hoursToMidnight
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.ZonedDateTime

/** The new-summon party on the Grimoire stage (ADR-0016). Not restored after process death. */
data class Celebration(val demonId: String, val flawless: Boolean)

/** Everything Home and Grimoire render (§3). */
data class GameUiState(
    /** False until the first stored progress has loaded; nothing acts on an unloaded `Progress()`. */
    val loaded: Boolean = false,
    val progress: Progress = Progress(),
    val now: ZonedDateTime,
    val selectedId: String,
    val celebration: Celebration? = null,
    /** Set when a flawless celebration is dismissed: the Grimoire opens the Secret page once. */
    val openSecret: Boolean = false,
    /** Houses in court now, debug override applied. */
    val courtHouses: Set<House> = emptySet(),
) {
    val today: Long get() = now.epochDay()
    val canSummonToday: Boolean get() = ProgressRules.canSummon(progress, today)
    val hoursToNextSummon: Int get() = hoursToMidnight(now)
    fun record(id: String): DemonRecord = progress.record(id)
}

/** Activity-scoped game state (§3). Every progress change goes through [store]. */
class GameViewModel(
    private val store: ProgressStore,
    private val clock: GameClock,
    private val debug: DebugSettingsStore,
    private val savedState: SavedStateHandle,
    private val demonIds: List<String> = DemonCatalog.ids,
) : ViewModel() {

    private data class Session(val celebration: Celebration? = null, val openSecret: Boolean = false)

    /** Null until the first stored value arrives. */
    private val stored: StateFlow<Progress?> = store.progress.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    /** Re-reads the clock on every activity start: hourly delays don't advance while the phone sleeps. */
    private val refresh = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private val time: StateFlow<ZonedDateTime> =
        merge(clock.ticks, refresh.map { clock.now() }).stateIn(viewModelScope, SharingStarted.Eagerly, clock.now())
    private val session = MutableStateFlow(Session())
    private val selected: StateFlow<String> = savedState.getStateFlow(KEY_SELECTED, demonIds.first())

    val uiState: StateFlow<GameUiState> =
        combine(stored, time, selected, session, debug.settings) { progress, now, selectedId, s, settings ->
            GameUiState(
                loaded = progress != null,
                progress = progress ?: Progress(),
                now = now,
                selectedId = selectedId,
                celebration = s.celebration,
                openSecret = s.openSecret,
                courtHouses = House.entries.filterTo(mutableSetOf()) { inCourt(it, now.hour, settings.court) },
            )
        }.stateIn(viewModelScope, SharingStarted.Eagerly, GameUiState(now = clock.now(), selectedId = selected.value))

    init {
        viewModelScope.launch {
            val first = stored.filterNotNull().first()
            val day = today()
            if (!savedState.contains(KEY_SELECTED)) select(ProgressRules.focusDemon(ProgressRules.evaluate(first, day), demonIds, day))
            store.update { ProgressRules.evaluate(it, today()) }
        }
    }

    fun today(): Long = clock.now().epochDay()

    /** For the ritual's court provider: the demon's House against the clock, debug override applied. */
    fun inCourtNow(house: House): Boolean = inCourt(house, clock.now().hour, debug.settings.value.court)

    /** On every activity start. Skipped until loaded (the first load runs its own evaluate). */
    fun evaluate() {
        refresh.tryEmit(Unit)
        if (stored.value == null) return
        val day = today()
        update { ProgressRules.evaluate(it, day) }
    }

    fun openGrimoireFromHome() {
        val progress = stored.value ?: return
        select(ProgressRules.focusDemon(progress, demonIds, today()))
        update { it.copy(homeHintSeen = true) }
    }

    fun select(id: String) {
        savedState[KEY_SELECTED] = id
    }

    /** Viewing a bound demon. Evaluates first, so a demon neglected past the limit escapes instead (§1). */
    fun visit(id: String) {
        val day = today()
        update { ProgressRules.visit(ProgressRules.evaluate(it, day), id, day) }
    }

    /** [day] is the day the ritual started (captured when the Ritual opened). */
    fun commitSummon(id: String, day: Long, flawless: Boolean) =
        update { ProgressRules.commitSummon(it, id, day, flawless) }

    fun celebrate(id: String, flawless: Boolean) {
        select(id)
        session.value = Session(celebration = Celebration(id, flawless))
    }

    fun dismissCelebration() = session.update { Session(openSecret = it.celebration?.flawless == true) }

    fun secretShown() = session.update { it.copy(openSecret = false) }

    fun consumeReveal(id: String, what: Reveal) = update { ProgressRules.consumeReveal(it, id, what) }

    fun dismissEscapeBanners() = update(ProgressRules::dismissEscapeBanners)

    // --- Debug sheet (§7) ---

    fun advanceDay() {
        val next = today() + 1
        viewModelScope.launch {
            debug.update { it.copy(dayOffset = it.dayOffset + 1) }
            store.update { ProgressRules.evaluate(it, next) }
        }
    }

    fun resetDaily() = update { it.copy(lastSummonDay = null) }

    fun setCourt(court: CourtOverride) {
        viewModelScope.launch { debug.update { it.copy(court = court) } }
    }

    fun bind(id: String, flawless: Boolean) = commitSummon(id, today(), flawless)

    fun unsummon(id: String) = update { ProgressRules.unsummon(it, id) }

    fun resetAll() {
        session.value = Session()
        viewModelScope.launch {
            store.update { Progress() }
            debug.update { DebugSettings() }
        }
    }

    private fun update(transform: (Progress) -> Progress) {
        viewModelScope.launch { store.update(transform) }
    }

    companion object {
        const val KEY_SELECTED = "selectedDemon"

        fun factory(store: ProgressStore, clock: GameClock, debug: DebugSettingsStore): ViewModelProvider.Factory =
            viewModelFactory { initializer { GameViewModel(store, clock, debug, createSavedStateHandle()) } }
    }
}

private fun inCourt(house: House, hour: Int, court: CourtOverride): Boolean = when (court) {
    CourtOverride.Auto -> CourtHours.inCourt(house, hour)
    CourtOverride.In -> true
    CourtOverride.Out -> false
}
```

- [ ] **Step 4: Run the test**

Run: `./gradlew testDebugUnitTest --tests '*GameViewModelTest*'`
Expected: 12 tests pass.

- [ ] **Step 5: Add the application and its graph**

`app/src/main/java/com/summondemons/safely/AppGraph.kt`:

```kotlin
package com.summondemons.safely

import android.content.Context
import com.summondemons.safely.platform.DataStoreDebugSettingsStore
import com.summondemons.safely.platform.SystemGameClock
import com.summondemons.safely.platform.progressStore
import com.summondemons.safely.progress.ProgressStore
import com.summondemons.safely.time.DebugSettingsStore
import com.summondemons.safely.time.GameClock
import com.summondemons.safely.time.NoDebugSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first

/** Process-wide singletons: one DataStore per file, one clock (ADR-0013). */
class AppGraph(context: Context) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val debugStore = if (BuildConfig.DEBUG) DataStoreDebugSettingsStore(appContext, scope) else null

    val progressStore: ProgressStore = appContext.progressStore()
    val debugSettings: DebugSettingsStore = debugStore ?: NoDebugSettings
    val clock: GameClock = SystemGameClock(appContext, debugSettings.settings)

    /** Returns once progress (and, in debug builds, the debug settings) have loaded. */
    suspend fun awaitReady() {
        progressStore.progress.first()
        debugStore?.awaitLoaded()
    }
}
```

`app/src/main/java/com/summondemons/safely/SummonApp.kt`:

```kotlin
package com.summondemons.safely

import android.app.Application

class SummonApp : Application() {
    val graph: AppGraph by lazy { AppGraph(this) }
}
```

- [ ] **Step 6: Hold the splash until progress loads**

`app/src/main/res/values/themes.xml`:

```xml
<resources>
    <style name="Theme.SummonDemonsSafely" parent="android:Theme.Material.NoActionBar">
        <item name="android:windowBackground">@color/obsidian</item>
    </style>

    <!-- Launch theme: the splash stays up until progress has loaded (ADR-0013). -->
    <style name="Theme.SummonDemonsSafely.Starting" parent="Theme.SplashScreen">
        <item name="windowSplashScreenBackground">@color/obsidian</item>
        <item name="postSplashScreenTheme">@style/Theme.SummonDemonsSafely</item>
    </style>
</resources>
```

In `app/src/main/AndroidManifest.xml`, add `android:name=".SummonApp"` as the first
attribute of `<application>`, and change the activity's
`android:theme="@style/Theme.SummonDemonsSafely"` to
`android:theme="@style/Theme.SummonDemonsSafely.Starting"` (leave the application's
theme as it is).

`app/src/main/java/com/summondemons/safely/MainActivity.kt`:

```kotlin
package com.summondemons.safely

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.summondemons.safely.navigation.AppNavHost
import com.summondemons.safely.ui.theme.SummonDemonsSafelyTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        // Dark theme only: light-on-dark system bar icons regardless of system setting.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        val graph = (application as SummonApp).graph
        // Covers every destination restored after process death: nothing renders an unloaded Progress().
        var ready = false
        splash.setKeepOnScreenCondition { !ready }
        lifecycleScope.launch {
            graph.awaitReady()
            ready = true
        }
        setContent {
            SummonDemonsSafelyTheme {
                AppNavHost(graph)
            }
        }
    }
}
```

- [ ] **Step 7: Move `DemonEntry` next to the old Grimoire** (deleted with it in Task 24)

In `app/src/main/java/com/summondemons/safely/ui/screens/GrimoireScreen.kt`, delete the
import `com.summondemons.safely.ui.DemonEntry`, add the import
`com.summondemons.safely.data.Demon`, and add above `GrimoireScreen`:

```kotlin
/** A demon and whether he is bound. Temporary: the Grimoire rewrite (Task 24) reads `GameUiState`. */
data class DemonEntry(val demon: Demon, val unlocked: Boolean)
```

- [ ] **Step 8: Switch `AppNavHost` to `GameViewModel`**

`app/src/main/java/com/summondemons/safely/navigation/AppNavHost.kt`:

```kotlin
package com.summondemons.safely.navigation

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.summondemons.safely.AppGraph
import com.summondemons.safely.BuildConfig
import com.summondemons.safely.data.DemonCatalog
import com.summondemons.safely.platform.AndroidHeadingSource
import com.summondemons.safely.platform.AndroidPcmSource
import com.summondemons.safely.ritual.RitualViewModel
import com.summondemons.safely.ui.GameViewModel
import com.summondemons.safely.ui.fx.LocalNavAnimatedVisibilityScope
import com.summondemons.safely.ui.fx.LocalSharedTransitionScope
import com.summondemons.safely.ui.screens.DemonEntry
import com.summondemons.safely.ui.screens.GrimoireScreen
import com.summondemons.safely.ui.screens.ProfileScreen
import com.summondemons.safely.ui.screens.RitualScreen

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AppNavHost(graph: AppGraph, navController: NavHostController = rememberNavController()) {
    val factory = remember(graph) { GameViewModel.factory(graph.progressStore, graph.clock, graph.debugSettings) }
    val game: GameViewModel = viewModel(factory = factory) // activity-scoped
    val state by game.uiState.collectAsStateWithLifecycle()
    val appContext = LocalContext.current.applicationContext
    LifecycleEventEffect(Lifecycle.Event.ON_START) { game.evaluate() }

    if (!state.loaded) {
        // The splash normally covers this; never render screens from an unloaded Progress().
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        return
    }

    SharedTransitionLayout {
        CompositionLocalProvider(LocalSharedTransitionScope provides this) {
            NavHost(navController = navController, startDestination = Grimoire) {
                composable<Grimoire> {
                    ProvideDestinationScope {
                        GrimoireScreen(
                            demons = DemonCatalog.all.map { DemonEntry(it, unlocked = state.record(it.id).bound) },
                            onSummon = { id -> navController.navigate(Ritual(id)) },
                            onView = { id -> navController.navigate(Profile(id, isNewSummon = false)) },
                        )
                    }
                }

                composable<Ritual> { entry ->
                    ProvideDestinationScope {
                        val demon = DemonCatalog.byId(entry.toRoute<Ritual>().demonId)
                        if (demon == null) {
                            LaunchedEffect(Unit) { navController.popBackStack() }
                        } else {
                            // The day the ritual started spends the summon, even if the climax lands after midnight.
                            val startDay = rememberSaveable { game.today() }
                            val ritualFactory = remember(demon) {
                                RitualViewModel.factory(
                                    demon,
                                    AndroidHeadingSource(appContext),
                                    AndroidPcmSource(),
                                    inCourt = { game.inCourtNow(demon.house) },
                                    commitSummon = { flawless -> game.commitSummon(demon.id, startDay, flawless) },
                                )
                            }
                            RitualScreen(
                                viewModel = viewModel(factory = ritualFactory),
                                onSummoned = {
                                    navController.navigate(Profile(demon.id, isNewSummon = true)) { popUpTo<Grimoire>() }
                                },
                                onAbort = dropUnlessResumed { navController.popBackStack() },
                            )
                        }
                    }
                }

                composable<Profile> { entry ->
                    ProvideDestinationScope {
                        val route = entry.toRoute<Profile>()
                        val demon = DemonCatalog.byId(route.demonId)
                        if (demon == null) {
                            LaunchedEffect(Unit) { navController.popBackStack() }
                        } else {
                            ProfileScreen(
                                demon = demon,
                                isNewSummon = route.isNewSummon,
                                onDone = dropUnlessResumed { navController.popBackStack<Grimoire>(inclusive = false) },
                                onUnsummon = if (BuildConfig.DEBUG && !route.isNewSummon) {
                                    dropUnlessResumed {
                                        game.unsummon(demon.id)
                                        navController.popBackStack<Grimoire>(inclusive = false)
                                    }
                                } else {
                                    null
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnimatedVisibilityScope.ProvideDestinationScope(content: @Composable () -> Unit) =
    CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this, content = content)
```

- [ ] **Step 9: Delete `DemonViewModel` and its test**

```bash
git rm -q app/src/main/java/com/summondemons/safely/ui/DemonViewModel.kt \
  app/src/test/java/com/summondemons/safely/ui/DemonViewModelTest.kt
```

- [ ] **Step 10: Build and test**

Run: `./gradlew testDebugUnitTest assembleDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 11: Commit**

```bash
git add -A app/src/main app/src/test
git commit -q -m "feat(app): GameViewModel over persistent progress; splash until loaded; drop DemonViewModel" -m "$TRAILER"
```

### Task 13: Feedback — crack haptic, volume, bed follows phase, GameSounds

**Files:**
- Create: `MAIN/platform/GameSounds.kt`
- Modify: `MAIN/platform/Haptics.kt`, `MAIN/platform/RitualSounds.kt`, `MAIN/ui/screens/RitualScreen.kt`, `MAIN/ui/screens/ProfileScreen.kt`, `MAIN/navigation/AppNavHost.kt`
- Rewrite: `MAIN/platform/RitualFeedback.kt`
- Delete: `MAIN/platform/TadaPlayer.kt`

- [ ] **Step 1: Add the crack haptic**

In `app/src/main/java/com/summondemons/safely/platform/Haptics.kt`, after the
`WARN_AMPLITUDES` line add:

```kotlin
    /** Crack: 80 ms on, 50 ms off, 80 ms on, strong (§5). */
    private val CRACK_TIMINGS = longArrayOf(0, 80, 50, 80)
    private val CRACK_AMPLITUDES = intArrayOf(0, 255, 0, 255)
```

and after `fun warn(...)` add:

```kotlin
    fun crack(context: Context) = waveform(context, CRACK_TIMINGS, CRACK_AMPLITUDES, CRACK_TIMINGS)
```

- [ ] **Step 2: Give `RitualSounds.play` a volume**

In `app/src/main/java/com/summondemons/safely/platform/RitualSounds.kt`, replace `play` with:

```kotlin
    fun play(cue: Cue, volume: Float = 1f) {
        if (released) return
        ids[cue]?.let { pool.play(it, volume, volume, 1, 0, 1f) }
    }
```

- [ ] **Step 3: Rewrite `RitualFeedback`**

`app/src/main/java/com/summondemons/safely/platform/RitualFeedback.kt`:

```kotlin
package com.summondemons.safely.platform

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.annotation.RawRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.summondemons.safely.platform.RitualSounds.Cue
import com.summondemons.safely.ritual.RitualEffect
import com.summondemons.safely.ritual.RitualPhase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

private const val BED_VOLUME = 0.7f
private const val BED_DUCKED = 0.2f
private const val FADE_IN_MS = 1_200L
private const val DUCK_MS = 300L
private const val SWELL_MS = 2_000L
private const val FADE_OUT_MS = 1_500L
private const val CRACK_VOLUME = 0.35f

/** Lets the boom and the bed's fade-out ring on after navigating back to the Grimoire. */
private const val RELEASE_DELAY_MS = 2_500L

/** Turns ritual effects into haptics and sounds (§5). Composed by the Ritual screen. */
@Composable
fun RitualFeedback(effects: Flow<RitualEffect>, @RawRes bed: Int, phase: RitualPhase) {
    val context = LocalContext.current
    val sounds = remember(bed) { RitualSounds(context, bed) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val summoned = remember(sounds) { BooleanArray(1) }
    val currentPhase by rememberUpdatedState(phase)
    DisposableEffect(sounds) {
        onDispose {
            // Leaving mid-ritual: silence the bed now; the delayed release only lets cues finish.
            if (!summoned[0]) sounds.stopBed()
            Handler(Looper.getMainLooper()).postDelayed({ sounds.release() }, RELEASE_DELAY_MS)
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { sounds.stopBed() }

    LaunchedEffect(effects, sounds, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            var fade: Job? = null
            fun fadeBed(to: Float, ms: Long) {
                fade?.cancel()
                fade = launch { sounds.fadeBed(to, ms) }
            }
            // The bed follows the phase, not only Locked/Resumed, so it returns after the background (§2).
            when (currentPhase) {
                RitualPhase.Chanting -> fadeBed(BED_VOLUME, FADE_IN_MS)
                RitualPhase.Wavering -> fadeBed(BED_DUCKED, FADE_IN_MS)
                else -> Unit
            }
            try {
                effects.collect { effect ->
                    summoned[0] = effect == RitualEffect.Summoned
                    handle(effect, context, sounds, ::fadeBed)
                }
            } finally {
                fade?.cancel()
            }
        }
    }
}

private fun handle(
    effect: RitualEffect,
    context: Context,
    sounds: RitualSounds,
    fadeBed: (Float, Long) -> Unit,
) {
    when (effect) {
        RitualEffect.ProximityTick -> Haptics.tick(context)
        RitualEffect.Locked -> {
            Haptics.clunk(context)
            sounds.play(Cue.Lock)
            fadeBed(BED_VOLUME, FADE_IN_MS)
        }
        RitualEffect.SyllableLit -> Haptics.syllable(context)
        is RitualEffect.Sealed -> {
            Haptics.chime(context)
            sounds.play(Cue.chime(effect.phrase))
        }
        is RitualEffect.Cracked -> {
            Haptics.crack(context)
            sounds.play(Cue.Boom, CRACK_VOLUME)
        }
        RitualEffect.Drift -> {
            Haptics.warn(context)
            fadeBed(BED_DUCKED, DUCK_MS)
        }
        RitualEffect.Resumed -> fadeBed(BED_VOLUME, DUCK_MS)
        is RitualEffect.ClimaxStart -> {
            Haptics.crescendo(context)
            sounds.play(Cue.Riser)
            fadeBed(1f, SWELL_MS)
        }
        RitualEffect.Summoned -> {
            Haptics.success(context)
            sounds.play(Cue.Boom)
            fadeBed(0f, FADE_OUT_MS)
        }
    }
}
```

In `app/src/main/java/com/summondemons/safely/ui/screens/RitualScreen.kt`, change
`RitualFeedback(viewModel.effects, state.demon.ambientBed)` to
`RitualFeedback(viewModel.effects, state.demon.ambientBed, state.ritual.phase)`.

- [ ] **Step 4: Write `GameSounds`**

`app/src/main/java/com/summondemons/safely/platform/GameSounds.kt`:

```kotlin
package com.summondemons.safely.platform

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.annotation.RawRes
import androidx.compose.runtime.staticCompositionLocalOf
import com.summondemons.safely.R

/**
 * One-shot sounds outside the Ritual (§5): the Home opening chime and the Grimoire "Tada!".
 * One SoundPool, created once and released with the activity. Main thread only.
 */
class GameSounds(context: Context) {

    enum class Cue(@param:RawRes val res: Int) { Chime4(R.raw.chime_4), Tada(R.raw.tada) }

    private val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_GAME)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()
    private val pool = SoundPool.Builder().setMaxStreams(MAX_STREAMS).setAudioAttributes(attributes).build()
    private val ids: Map<Cue, Int> = Cue.entries.associateWith { pool.load(context.applicationContext, it.res, 1) }
    private var released = false

    fun play(cue: Cue, volume: Float = 1f) {
        if (released) return
        ids[cue]?.let { pool.play(it, volume, volume, 1, 0, 1f) }
    }

    fun release() {
        if (released) return
        released = true
        pool.release()
    }

    private companion object {
        const val MAX_STREAMS = 2
    }
}

/** Provided by AppNavHost for the activity's lifetime; null in previews. */
val LocalGameSounds = staticCompositionLocalOf<GameSounds?> { null }
```

- [ ] **Step 5: Provide it from `AppNavHost`**

In `app/src/main/java/com/summondemons/safely/navigation/AppNavHost.kt`:
- add imports `androidx.compose.runtime.DisposableEffect`,
  `com.summondemons.safely.platform.GameSounds` and
  `com.summondemons.safely.platform.LocalGameSounds`;
- after `val appContext = LocalContext.current.applicationContext` add:

```kotlin
    val sounds = remember { GameSounds(appContext) }
    DisposableEffect(sounds) { onDispose { sounds.release() } }
```

- wrap the `SharedTransitionLayout { … }` call in
  `CompositionLocalProvider(LocalGameSounds provides sounds) { … }`.

- [ ] **Step 6: Play the fanfare through `GameSounds`** (Profile goes away in Task 24)

In `app/src/main/java/com/summondemons/safely/ui/screens/ProfileScreen.kt`, replace the
block

```kotlin
    if (isNewSummon) {
        val context = LocalContext.current
        // Saved so activity recreation (font scale, locale) doesn't replay the fanfare.
        var tadaPlayed by rememberSaveable { mutableStateOf(false) }
        DisposableEffect(Unit) {
            val player = if (tadaPlayed) null else TadaPlayer.play(context).also { tadaPlayed = true }
            onDispose { player?.release() }
        }
    }
```

with

```kotlin
    if (isNewSummon) {
        val sounds = LocalGameSounds.current
        // Saved so activity recreation (font scale, locale) doesn't replay the fanfare.
        var tadaPlayed by rememberSaveable { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            if (!tadaPlayed) {
                sounds?.play(GameSounds.Cue.Tada)
                tadaPlayed = true
            }
        }
    }
```

and fix the imports: remove `androidx.compose.runtime.DisposableEffect`,
`androidx.compose.ui.platform.LocalContext` and
`com.summondemons.safely.platform.TadaPlayer`; add
`com.summondemons.safely.platform.GameSounds` and
`com.summondemons.safely.platform.LocalGameSounds`.

- [ ] **Step 7: Delete `TadaPlayer`**

```bash
git rm -q app/src/main/java/com/summondemons/safely/platform/TadaPlayer.kt
```

- [ ] **Step 8: Build and test**

Run: `./gradlew testDebugUnitTest assembleDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 9: Commit**

```bash
git add -A app/src/main
git commit -q -m "feat(feedback): Crack haptic, cue volume, bed follows phase; GameSounds replaces TadaPlayer" -m "$TRAILER"
```

- [ ] **Step 10: Phase gate**

Run the verification gate. Expected: `BUILD SUCCESSFUL`.

---
## Phase 4 — Components

Components have no unit tests (no instrumented/UI tests, spec §11); pure helpers do.
Each task ends with `./gradlew assembleDebug` (unused-code lint warnings are expected
until Phase 5 wires the components in).

### Task 14: Theme tokens, text roles, copy, motion helpers

**Files:**
- Modify: `MAIN/ui/theme/Color.kt`, `MAIN/ui/theme/DemonColors.kt`, `MAIN/ui/theme/Type.kt`, `MAIN/ui/theme/Theme.kt`, `MAIN/ui/fx/FxMath.kt`, `RES/values/strings.xml`, `TEST/ui/fx/FxMathTest.kt`
- Create: `MAIN/ui/theme/TextRoles.kt`, `MAIN/ui/fx/Jitter.kt`, `MAIN/ui/fx/SvgPath.kt`

- [ ] **Step 1: Add the new palette entries**

In `app/src/main/java/com/summondemons/safely/ui/theme/Color.kt`, after
`val ritualSafe = Color(0xFF4ADE80)` add:

```kotlin
    val houseSol = Color(0xFFF2C14E)
    val houseVenus = Color(0xFFC87A3C)
    val inkHighlight = Color(0xFFFFF6D0)
    val stageHomeGlow = Color(0xFF3A2A55)
    val stageHomeMid = Color(0xFF1A1530)
    val stageSolGlow = Color(0xFF6B4A12)
    val stageSolMid = Color(0xFF2A1D0B)
    val stageVenusGlow = Color(0xFF3B2A4A)
    val stageVenusMid = Color(0xFF2A1A26)
```

- [ ] **Step 2: Rewrite `DemonColors.kt`**

`app/src/main/java/com/summondemons/safely/ui/theme/DemonColors.kt`:

```kotlin
package com.summondemons.safely.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.summondemons.safely.data.DemonAccent
import com.summondemons.safely.data.House

/** The three placeholder stage backgrounds (DESIGN.md Gaps). */
enum class Stage { Home, Sol, Venus }

fun House.stage(): Stage = when (this) {
    House.Sol -> Stage.Sol
    House.Venus -> Stage.Venus
}

/** Radial stage gradient: [glow] at the light, [mid] around it, [edge] at the borders. */
@Immutable
data class StageGradient(val glow: Color, val mid: Color, val edge: Color)

/** DESIGN.md tokens with no Material slot. */
@Immutable
data class DemonColors(
    val asmodeus: Color,
    val astaroth: Color,
    val ritualSafe: Color,
    val houseSol: Color,
    val houseVenus: Color,
    val inkHighlight: Color,
    val stageHome: StageGradient,
    val stageSol: StageGradient,
    val stageVenus: StageGradient,
) {
    /** The only place an accent is resolved to a color; UI never branches on demon id. */
    fun accent(accent: DemonAccent): Color = when (accent) {
        DemonAccent.Asmodeus -> asmodeus
        DemonAccent.Astaroth -> astaroth
    }

    /** House metal: badge rims, number medallion. */
    fun metal(house: House): Color = when (house) {
        House.Sol -> houseSol
        House.Venus -> houseVenus
    }

    fun stage(stage: Stage): StageGradient = when (stage) {
        Stage.Home -> stageHome
        Stage.Sol -> stageSol
        Stage.Venus -> stageVenus
    }
}

internal val DefaultDemonColors = DemonColors(
    asmodeus = Palette.demonAsmodeus,
    astaroth = Palette.demonAstaroth,
    ritualSafe = Palette.ritualSafe,
    houseSol = Palette.houseSol,
    houseVenus = Palette.houseVenus,
    inkHighlight = Palette.inkHighlight,
    stageHome = StageGradient(Palette.stageHomeGlow, Palette.stageHomeMid, Palette.surfaceContainerLowest),
    stageSol = StageGradient(Palette.stageSolGlow, Palette.stageSolMid, Palette.background),
    stageVenus = StageGradient(Palette.stageVenusGlow, Palette.stageVenusMid, Palette.background),
)

val LocalDemonColors = staticCompositionLocalOf { DefaultDemonColors }

val MaterialTheme.demonColors: DemonColors
    @Composable @ReadOnlyComposable get() = LocalDemonColors.current
```

- [ ] **Step 3: Add the text roles**

In `app/src/main/java/com/summondemons/safely/ui/theme/Type.kt`, change
`private val Display = FontFamily(` to `internal val Display = FontFamily(`.

`app/src/main/java/com/summondemons/safely/ui/theme/TextRoles.kt`:

```kotlin
package com.summondemons.safely.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import java.util.Locale

/** DESIGN.md "Text roles" (ADR-0014). `cue` and `meta` text is upper-cased with [upper]. */
@Immutable
data class TextRoles(
    val cue: TextStyle,
    val incantation: TextStyle,
    val meta: TextStyle,
    /** Cinzel Bold; the font size is set per use. */
    val name: TextStyle,
    val body: TextStyle,
)

internal val AppTextRoles = TextRoles(
    cue = AppTypography.labelMedium.copy(letterSpacing = 0.08.em),
    incantation = TextStyle(
        fontFamily = Display,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.02.em,
    ),
    meta = AppTypography.labelSmall.copy(letterSpacing = 0.14.em, color = Palette.outline),
    name = TextStyle(fontFamily = Display, fontWeight = FontWeight.Bold),
    body = AppTypography.bodyMedium,
)

val LocalTextRoles = staticCompositionLocalOf { AppTextRoles }

val MaterialTheme.textRoles: TextRoles
    @Composable @ReadOnlyComposable get() = LocalTextRoles.current

/** Compose has no text-transform: the `cue` and `meta` roles upper-case at the call site. */
fun String.upper(): String = uppercase(Locale.ROOT)
```

In `app/src/main/java/com/summondemons/safely/ui/theme/Theme.kt`, add
`LocalTextRoles provides AppTextRoles,` to the `CompositionLocalProvider` list.

- [ ] **Step 4: Add the copy**

In `app/src/main/res/values/strings.xml`, add before `</resources>` (the old strings
stay until Task 27):

```xml

    <!-- Home (ADR-0014) -->
    <string name="home_court">In court · %1$s</string>
    <string name="home_court_join">" &amp; "</string>
    <string name="home_out_of_office">Out of office</string>
    <string name="home_bound">%1$d of %2$d spirits bound</string>
    <string name="home_hint">Tap the grimoire</string>
    <string name="home_open">Open the grimoire</string>
    <string name="escape_banner">Seal broken!</string>
    <string name="escape_one">%1$s slipped his seal while you were away!</string>
    <string name="escape_all">Both spirits slipped their seals while you were away!</string>
    <string name="escape_dismiss">Tap to dismiss</string>

    <!-- Grimoire (ADR-0016) -->
    <string name="grimoire_back">Back to the chamber</string>
    <string name="avatar_sealed">%1$s, sealed</string>
    <string name="celebration_banner">New Demon Received!</string>
    <string name="quote_tap_to_summon">Tap to summon</string>
    <string name="quote_cooling">Circle cooling</string>
    <string name="quote_next_summon">Next summon in %1$d h</string>
    <string name="cd_summon">Summon %1$s</string>
    <string name="cd_cooling">Circle cooling. Next summon in %1$d hours.</string>
    <string name="badge_direction_text">Face %1$s to summon. %2$s</string>
    <string name="badge_liege">He serves %1$s.</string>
    <string name="badge_no_liege">He swears to no king.</string>
    <string name="badge_legions_label">%1$d legions</string>
    <string name="badge_legions_text">A %1$s at the head of %2$d legions of spirits.</string>
    <string name="badge_explain">%1$s: explain</string>
    <string name="dossier_weakness">Weakness</string>
    <string name="dossier_weakness_named">Weakness · %1$s</string>
    <string name="dossier_abilities">Abilities</string>
    <string name="dossier_offices">Offices</string>
    <string name="dossier_appearance">Appearance</string>
    <string name="dossier_warning">Summoner’s warning</string>
    <string name="dossier_secret">Secret</string>
    <string name="dossier_secret_locked">Summon flawlessly to reveal</string>
    <string name="dossier_sealed">Sealed. Summon to reveal.</string>
    <string name="dossier_page">%1$d / %2$d</string>
    <string name="dossier_previous">Previous page</string>
    <string name="dossier_next">Next page</string>

    <!-- Ritual (ADR-0010, ADR-0014) -->
    <string name="cue_mic_denied">The spirits can\'t hear you</string>
    <string name="cue_mic_unavailable">The microphone wouldn\'t start</string>
    <string name="cue_no_heading">This device can\'t sense direction</string>
    <string name="cue_cracked">The seal cracked · Chant %1$s again</string>
    <string name="cue_chant">Locked · Chant aloud</string>
    <string name="cue_wavering">Face %1$s!</string>
    <string name="cue_wavering_focus">Face %1$s! · Focus slipping</string>
    <string name="chant_title">Sacred chant</string>
    <string name="chant_sealed_count">%1$s / IV</string>
    <string name="chant_seconds">⏱ %1$d s</string>
    <string name="chant_drift">⏱ 3×</string>
    <string name="focus_label">Focus</string>
    <string name="abandon_title">Abandon the ritual?</string>
    <string name="abandon_body">Your seals will fade.</string>
    <string name="action_abandon">Abandon</string>
    <string name="action_stay">Stay</string>

    <!-- Debug sheet (debug builds only, §7) -->
    <string name="debug_chip">Debug</string>
    <string name="debug_advance_day">Advance +1 day</string>
    <string name="debug_reset_daily">Reset daily summon</string>
    <string name="debug_court">Court</string>
    <string name="debug_court_auto">Auto</string>
    <string name="debug_court_in">In</string>
    <string name="debug_court_out">Out</string>
    <string name="debug_bind">Bind</string>
    <string name="debug_bind_flawless">Bind flawless</string>
    <string name="debug_unsummon">Unsummon</string>
    <string name="debug_reset_all">Reset all</string>
    <string name="debug_status_sealed">sealed</string>
    <string name="debug_status_escaped">escaped (lore kept)</string>
    <string name="debug_status_bound">bound · %1$dd since visit</string>
    <string name="debug_status_restless">restless</string>
    <string name="debug_status_flawless">flawless</string>
    <string name="debug_tester_note">Advancing a day while a bound demon is on the Grimoire visits him at once; test restlessness by advancing from Home.</string>
```

- [ ] **Step 5: Add `keyframe` with a test**

Append to `app/src/test/java/com/summondemons/safely/ui/fx/FxMathTest.kt` (inside the class):

```kotlin
    @Test
    fun keyframeInterpolatesEvenlySpacedValues() {
        val values = floatArrayOf(0f, -22f, 0f, -6f, 0f)
        assertEquals(0f, keyframe(0f, values), 0.0001f)
        assertEquals(-11f, keyframe(0.125f, values), 0.0001f)
        assertEquals(-22f, keyframe(0.25f, values), 0.0001f)
        assertEquals(-3f, keyframe(0.625f, values), 0.0001f)
        assertEquals(0f, keyframe(1f, values), 0.0001f)
        assertEquals(0f, keyframe(2f, values), 0.0001f) // clamped
    }
```

(add `import org.junit.Assert.assertEquals` if the file lacks it). Then append to
`app/src/main/java/com/summondemons/safely/ui/fx/FxMath.kt`:

```kotlin

/** Value at [t] in [0, 1] of evenly spaced keyframes [values], linearly interpolated and clamped. */
fun keyframe(t: Float, values: FloatArray): Float {
    require(values.size >= 2) { "need at least two keyframes" }
    val x = t.coerceIn(0f, 1f) * (values.size - 1)
    val i = x.toInt().coerceAtMost(values.size - 2)
    return values[i] + (values[i + 1] - values[i]) * (x - i)
}
```

- [ ] **Step 6: Add the jitter and shake helpers**

`app/src/main/java/com/summondemons/safely/ui/fx/Jitter.kt`:

```kotlin
package com.summondemons.safely.ui.fx

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

private const val JITTER_MS = 180
private val JITTER_X = floatArrayOf(0f, -1f, 0f, 1f, 0f)
private val JITTER_Y = floatArrayOf(0f, 1f, 0f, -1f, 0f)
private val JITTER_DEG = floatArrayOf(0f, -2f, 0f, 2f, 0f)
private const val SHAKE_MS = 350

/** A restless demon's nervous jitter (ADR-0015). Off when [enabled] is false or motion is reduced. */
@Composable
fun Jitter(enabled: Boolean, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val active = enabled && !LocalReduceMotion.current
    val phase = if (active) {
        rememberInfiniteTransition(label = "jitter").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(JITTER_MS, easing = LinearEasing), RepeatMode.Restart),
            label = "jitterPhase",
        )
    } else {
        null
    }
    Box(
        modifier.graphicsLayer {
            val p = phase?.value ?: return@graphicsLayer
            translationX = keyframe(p, JITTER_X) * 1.dp.toPx()
            translationY = keyframe(p, JITTER_Y) * 1.dp.toPx()
            rotationZ = keyframe(p, JITTER_DEG)
        },
        contentAlignment = Alignment.Center,
    ) { content() }
}

/** Horizontal shake offset in dp, replayed whenever [trigger] increases (cooling cue, cue chip). */
@Composable
fun rememberShake(trigger: Int): Animatable<Float, AnimationVector1D> {
    val offset = remember { Animatable(0f) }
    LaunchedEffect(trigger) {
        if (trigger > 0) {
            offset.animateTo(
                0f,
                keyframes {
                    durationMillis = SHAKE_MS
                    -6f at 70
                    6f at 140
                    -4f at 210
                    4f at 280
                },
            )
        }
    }
    return offset
}
```

- [ ] **Step 7: Add the SVG path cache**

`app/src/main/java/com/summondemons/safely/ui/fx/SvgPath.kt`:

```kotlin
package com.summondemons.safely.ui.fx

import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.PathParser

private val cache = HashMap<String, Path>()

/** Parses SVG path data once (glyphs, compass rose, grimoire). Main thread only. */
fun svgPath(data: String): Path = cache.getOrPut(data) { PathParser().parsePathString(data).toPath() }
```

- [ ] **Step 8: Test and build**

Run: `./gradlew testDebugUnitTest --tests '*FxMathTest*' assembleDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 9: Commit**

```bash
git add app/src/main app/src/test
git commit -q -m "feat(theme): House, ink and stage tokens; text roles; screen-system copy; motion helpers" -m "$TRAILER"
```

### Task 15: Glyphs and `headingLetter`

**Files:**
- Create: `MAIN/ui/components/Glyphs.kt`
- Modify: `MAIN/ritual/HeadingMath.kt`, `TEST/ritual/HeadingMathTest.kt`

- [ ] **Step 1: Write the failing test**

Append to `app/src/test/java/com/summondemons/safely/ritual/HeadingMathTest.kt` (inside the class):

```kotlin
    @Test
    fun headingLetterIsTheNearestCompassPointAbbreviation() {
        assertEquals("N", headingLetter(0f))
        assertEquals("E", headingLetter(90f))
        assertEquals("NE", headingLetter(30f))
        assertEquals("N", headingLetter(350f))
        assertEquals("SW", headingLetter(-135f))
    }
```

Run: `./gradlew testDebugUnitTest --tests '*HeadingMathTest*'` → fails (`headingLetter` unresolved).

- [ ] **Step 2: Implement**

In `app/src/main/java/com/summondemons/safely/ritual/HeadingMath.kt`, replace

```kotlin
/** Nearest of 8 compass points, e.g. for "face {direction}". */
fun headingLabel(degrees: Float): String =
    COMPASS_POINTS[((normalizeDegrees(degrees) + 22.5f) / 45f).toInt() % COMPASS_POINTS.size]
```

with

```kotlin
private val COMPASS_LETTERS = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")

private fun compassIndex(degrees: Float): Int = ((normalizeDegrees(degrees) + 22.5f) / 45f).toInt() % COMPASS_POINTS.size

/** Nearest of 8 compass points, e.g. for "face {direction}". */
fun headingLabel(degrees: Float): String = COMPASS_POINTS[compassIndex(degrees)]

/** Abbreviation of [headingLabel] ("N", "NE"…), for the Direction badge glyph. */
fun headingLetter(degrees: Float): String = COMPASS_LETTERS[compassIndex(degrees)]
```

Run the test again → passes.

- [ ] **Step 3: Write the glyphs** (vector paths from the prototype's `src/fx/icons.tsx`, 24-unit box)

`app/src/main/java/com/summondemons/safely/ui/components/Glyphs.kt`:

```kotlin
package com.summondemons.safely.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import com.summondemons.safely.data.Calling
import com.summondemons.safely.data.House
import com.summondemons.safely.ui.fx.svgPath

/** The glyphs are drawn in a 24 × 24 box (the bundled Latin-subset fonts lack ☉ ♀ etc., ADR-0014). */
private const val BOX = 24f
private const val STROKE = 1.7f
private const val COMPASS_STROKE = 1.5f
private const val COMPASS_RING_ALPHA = 0.6f
private const val NEEDLE_TAIL_ALPHA = 0.5f
private const val LETTER_SIZE = 5.5f
private val CENTER = Offset(12f, 12f)

@Composable
private fun Glyph(size: Dp, modifier: Modifier, draw: DrawScope.() -> Unit) {
    Canvas(modifier.size(size)) {
        val unit = this.size.minDimension / BOX
        scale(unit, unit, pivot = Offset.Zero) { draw() }
    }
}

private fun DrawScope.line(data: String, color: Color, width: Float = STROKE) =
    drawPath(svgPath(data), color, style = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round))

/** ☉ for Sol, ♀ for Venus: the classical public-domain planetary signs. */
@Composable
fun HouseGlyph(house: House, color: Color, size: Dp, modifier: Modifier = Modifier) = Glyph(size, modifier) {
    when (house) {
        House.Sol -> {
            drawCircle(color, 8f, CENTER, style = Stroke(STROKE))
            drawCircle(color, 1.8f, CENTER)
        }
        House.Venus -> {
            drawCircle(color, 5f, Offset(12f, 9f), style = Stroke(STROKE))
            line("M12 14v7M8.5 18h7", color)
        }
    }
}

@Composable
fun CallingGlyph(calling: Calling, color: Color, size: Dp, modifier: Modifier = Modifier) = Glyph(size, modifier) {
    when (calling) {
        Calling.Tutor -> {
            line("M12 6c-2-1.5-5-2-8-1.5v13c3-.5 6 0 8 1.5 2-1.5 5-2 8-1.5v-13c-3-.5-6 0-8 1.5z", color)
            line("M12 6v13", color)
        }
        Calling.Oracle -> {
            line("M2.5 12s3.5-6 9.5-6 9.5 6 9.5 6-3.5 6-9.5 6-9.5-6-9.5-6z", color)
            drawCircle(color, 3f, CENTER, style = Stroke(STROKE))
            drawCircle(color, 0.8f, CENTER)
        }
        Calling.Warlord -> line("M5 19L19 5M15 5h4v4M19 19L5 5M5 9V5h4", color)
        Calling.Treasurer -> {
            drawRoundRect(color, Offset(4f, 9f), Size(16f, 10f), CornerRadius(2f), style = Stroke(STROKE))
            line("M4 13h16M12 11v4M6 9c0-3 12-3 12 0", color)
        }
        Calling.Menace -> line("M5 6l3 12 4-8 4 8 3-12", color)
    }
}

/** Crossed swords for the legions a demon commands. */
@Composable
fun LegionsGlyph(color: Color, size: Dp, modifier: Modifier = Modifier) = Glyph(size, modifier) {
    line("M14.5 17.5L3 6V3h3l11.5 11.5M13 19l6-6M16 16l4 4M19 21l2-2", color)
    line("M14.5 6.5L18 3h3v3l-3.5 3.5M5 14l4 4M7 17l-3 3M3 19l2 2", color)
}

/** A compass whose needle points at [heading], with the target's letter in the middle. */
@Composable
fun DirectionGlyph(heading: Float, letter: String, color: Color, size: Dp, modifier: Modifier = Modifier) {
    val measurer = rememberTextMeasurer()
    val style = MaterialTheme.typography.labelSmall
    Canvas(modifier.size(size)) {
        val unit = this.size.minDimension / BOX
        scale(unit, unit, pivot = Offset.Zero) {
            drawCircle(color.copy(alpha = color.alpha * COMPASS_RING_ALPHA), 9f, CENTER, style = Stroke(COMPASS_STROKE))
            rotate(heading, pivot = CENTER) {
                drawPath(svgPath("M12 4.5l2.4 7.5H9.6z"), color)
                drawPath(
                    svgPath("M12 19.5l2.4-7.5H9.6z"),
                    color.copy(alpha = color.alpha * NEEDLE_TAIL_ALPHA),
                    style = Stroke(COMPASS_STROKE),
                )
            }
        }
        val layout = measurer.measure(
            letter,
            style.copy(color = color, fontSize = (LETTER_SIZE * unit).toSp(), lineHeight = TextUnit.Unspecified),
        )
        drawText(layout, topLeft = Offset(center.x - layout.size.width / 2f, center.y - layout.size.height / 2f))
    }
}
```

- [ ] **Step 4: Build**

Run: `./gradlew testDebugUnitTest --tests '*HeadingMathTest*' assembleDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```bash
git add app/src/main app/src/test
git commit -q -m "feat(ui): House, Calling, Legions and Direction glyphs" -m "$TRAILER"
```

### Task 16: Cue chip and the cue / chant-header logic

**Files:**
- Create: `MAIN/ui/components/CueChip.kt`, `MAIN/ui/screens/RitualCue.kt`, `TEST/ui/screens/RitualCueTest.kt`

- [ ] **Step 1: Write the failing test**

`app/src/test/java/com/summondemons/safely/ui/screens/RitualCueTest.kt`:

```kotlin
package com.summondemons.safely.ui.screens

import com.summondemons.safely.data.DemonCatalog
import com.summondemons.safely.ritual.RitualPhase
import com.summondemons.safely.ritual.RitualState
import com.summondemons.safely.ritual.RitualUiState
import com.summondemons.safely.ui.components.CueTone
import org.junit.Assert.assertEquals
import org.junit.Test

class RitualCueTest {

    private val demon = DemonCatalog.byId("asmodeus")!! // faces East (90°)
    private val base = RitualUiState(demon = demon, ritual = RitualState.start(demon.ritualHeading, demon.chantPhrases))

    private fun state(
        phase: RitualPhase = RitualPhase.Seeking,
        phrase: Int = 0,
        azimuth: Float? = null,
        micGranted: Boolean? = true,
    ) = base.copy(ritual = base.ritual.copy(phase = phase, phrase = phrase), azimuth = azimuth, micGranted = micGranted)

    private fun kind(s: RitualUiState, cracked: Int? = null) = ritualCue(s, cracked).kind

    @Test
    fun errorsComeFirstInSpecOrder() {
        val all = state(micGranted = false).copy(micUnavailable = true, headingUnavailable = true)
        assertEquals(CueKind.MicDenied, kind(all, cracked = 1))
        assertEquals(CueKind.MicUnavailable, kind(all.copy(micGranted = true), cracked = 1))
        assertEquals(CueKind.NoHeading, kind(state().copy(headingUnavailable = true), cracked = 1))
        assertEquals(CueKind.Face, kind(state().copy(headingUnavailable = true, simulateHeading = true)))
        assertEquals(CueKind.Face, kind(state().copy(micUnavailable = true, micGranted = null)))
    }

    @Test
    fun aCrackShowsUntilItExpires() {
        val cue = ritualCue(state(RitualPhase.Chanting, phrase = 1), crackedPhrase = 1)
        assertEquals(CueKind.Cracked, cue.kind)
        assertEquals(1, cue.crackedPhrase)
        assertEquals(CueTone.Error, cue.kind.tone)
    }

    @Test
    fun seekingTurnsTowardTheTarget() {
        assertEquals(CueKind.TurnRight, kind(state(azimuth = 0f)))
        assertEquals(CueKind.TurnLeft, kind(state(azimuth = 180f)))
        assertEquals(CueKind.Face, kind(state(azimuth = null)))
        assertEquals(CueKind.Face, kind(state(azimuth = 95f)))
        assertEquals(CueTone.Neutral, CueKind.TurnLeft.tone)
    }

    @Test
    fun phasesMapToTheirCues() {
        assertEquals(CueKind.HoldSteady, kind(state(RitualPhase.Locking)))
        assertEquals(CueKind.Chant, kind(state(RitualPhase.Chanting)))
        assertEquals(CueKind.Wavering, kind(state(RitualPhase.Wavering, phrase = 0)))
        assertEquals(CueKind.WaveringFocus, kind(state(RitualPhase.Wavering, phrase = 2)))
        assertEquals(CueKind.Opening, kind(state(RitualPhase.Climax, phrase = 4)))
        assertEquals(CueTone.Safe, CueKind.Chant.tone)
        assertEquals(CueTone.Error, CueKind.WaveringFocus.tone)
    }

    @Test
    fun theHeaderShowsSealsSecondsOrTheDrainRate() {
        val r = base.ritual
        assertEquals(ChantHeader.SealedCount(0), chantHeader(r))
        assertEquals(ChantHeader.SealedCount(1), chantHeader(r.copy(phase = RitualPhase.Seeking, phrase = 1)))
        assertEquals(
            ChantHeader.Seconds(5),
            chantHeader(r.copy(phase = RitualPhase.Chanting, phrase = 1, focusMs = 4_001, focusBudgetMs = 9_000)),
        )
        assertEquals(ChantHeader.Drifting, chantHeader(r.copy(phase = RitualPhase.Wavering, phrase = 2, focusMs = 100)))
        assertEquals(ChantHeader.SealedCount(0), chantHeader(r.copy(phase = RitualPhase.Wavering, phrase = 0)))
        assertEquals(ChantHeader.SealedCount(4), chantHeader(r.copy(phase = RitualPhase.Climax, phrase = 4)))
    }
}
```

Run: `./gradlew testDebugUnitTest --tests '*RitualCueTest*'` → compilation fails.

- [ ] **Step 2: Write the cue chip**

`app/src/main/java/com/summondemons/safely/ui/components/CueChip.kt`:

```kotlin
package com.summondemons.safely.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.summondemons.safely.ui.fx.rememberShake
import com.summondemons.safely.ui.theme.PillShape
import com.summondemons.safely.ui.theme.demonColors
import com.summondemons.safely.ui.theme.spacing
import com.summondemons.safely.ui.theme.textRoles
import com.summondemons.safely.ui.theme.upper

enum class CueTone { Neutral, Safe, Error }

private const val SAFE_BORDER_ALPHA = 0.4f
private const val ERROR_BORDER_ALPHA = 0.6f
private const val ERROR_CONTAINER_ALPHA = 0.4f
private val DotSize = 8.dp

/** What to do now (ADR-0014): a pill with a status dot, in the `cue` role. */
@Composable
fun CueChip(text: String, tone: CueTone, modifier: Modifier = Modifier, shake: Int = 0) {
    val colors = MaterialTheme.colorScheme
    val safe = MaterialTheme.demonColors.ritualSafe
    val offset = rememberShake(shake)
    val (container, content, border, dot) = when (tone) {
        CueTone.Neutral -> listOf(colors.surfaceContainer, colors.onSurfaceVariant, colors.outlineVariant, colors.outline)
        CueTone.Safe -> listOf(colors.surfaceContainer, safe, safe.copy(alpha = SAFE_BORDER_ALPHA), safe)
        CueTone.Error -> listOf(
            colors.errorContainer.copy(alpha = ERROR_CONTAINER_ALPHA),
            colors.error,
            colors.error.copy(alpha = ERROR_BORDER_ALPHA),
            colors.error,
        )
    }
    Surface(
        shape = PillShape,
        color = container,
        contentColor = content,
        border = BorderStroke(1.dp, border),
        modifier = modifier
            .graphicsLayer { translationX = offset.value.dp.toPx() }
            .semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Row(
            Modifier.padding(horizontal = MaterialTheme.spacing.md, vertical = MaterialTheme.spacing.xs + 2.dp),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(DotSize).background(dot, CircleShape))
            Text(text.upper(), style = MaterialTheme.textRoles.cue)
        }
    }
}
```

- [ ] **Step 3: Write the cue and header logic**

`app/src/main/java/com/summondemons/safely/ui/screens/RitualCue.kt`:

```kotlin
package com.summondemons.safely.ui.screens

import com.summondemons.safely.ritual.RitualConfig.PHRASE_COUNT
import com.summondemons.safely.ritual.RitualPhase
import com.summondemons.safely.ritual.RitualState
import com.summondemons.safely.ritual.RitualUiState
import com.summondemons.safely.ritual.TurnDirection
import com.summondemons.safely.ritual.turnDirection
import com.summondemons.safely.ui.components.CueTone
import kotlin.math.ceil

/** The Ritual cue chip's rows (§4); the first matching row wins. Pure: the screen maps kinds to copy. */
enum class CueKind(val tone: CueTone) {
    MicDenied(CueTone.Error),
    MicUnavailable(CueTone.Error),
    NoHeading(CueTone.Error),
    Cracked(CueTone.Error),
    TurnLeft(CueTone.Neutral),
    TurnRight(CueTone.Neutral),
    Face(CueTone.Neutral),
    HoldSteady(CueTone.Safe),
    Chant(CueTone.Safe),
    Wavering(CueTone.Error),
    WaveringFocus(CueTone.Error),
    Opening(CueTone.Safe),
}

data class RitualCue(val kind: CueKind, val crackedPhrase: Int? = null)

/** [crackedPhrase] is the phrase that cracked within the last 1.5 s, or null. */
fun ritualCue(state: RitualUiState, crackedPhrase: Int?): RitualCue {
    val ritual = state.ritual
    val kind = when {
        state.micGranted == false -> CueKind.MicDenied
        state.micGranted == true && state.micUnavailable -> CueKind.MicUnavailable
        state.headingUnavailable && !state.simulateHeading -> CueKind.NoHeading
        crackedPhrase != null -> CueKind.Cracked
        else -> when (ritual.phase) {
            RitualPhase.Seeking -> when (turnDirection(state.azimuth, state.demon.ritualHeading)) {
                TurnDirection.Left -> CueKind.TurnLeft
                TurnDirection.Right -> CueKind.TurnRight
                null -> CueKind.Face
            }
            RitualPhase.Locking -> CueKind.HoldSteady
            RitualPhase.Chanting -> CueKind.Chant
            RitualPhase.Wavering -> if (ritual.phrase >= 1) CueKind.WaveringFocus else CueKind.Wavering
            RitualPhase.Climax, RitualPhase.Done -> CueKind.Opening
        }
    }
    return RitualCue(kind, crackedPhrase.takeIf { kind == CueKind.Cracked })
}

/** The chant tablet's right-hand header (§4). */
sealed interface ChantHeader {
    /** "{numeral} / IV" ("0 / IV" before the first seal). */
    data class SealedCount(val count: Int) : ChantHeader

    /** "⏱ {s} s": focus left, rounded up. */
    data class Seconds(val seconds: Int) : ChantHeader

    /** "⏱ 3×": wavering while the clock runs. */
    data object Drifting : ChantHeader
}

fun chantHeader(ritual: RitualState): ChantHeader = when {
    ritual.clockActive && ritual.phase == RitualPhase.Wavering -> ChantHeader.Drifting
    ritual.clockActive -> ChantHeader.Seconds(ceil(ritual.focusMs / 1_000.0).toInt())
    else -> ChantHeader.SealedCount(ritual.phrase.coerceAtMost(PHRASE_COUNT))
}
```

- [ ] **Step 4: Run the tests**

Run: `./gradlew testDebugUnitTest --tests '*RitualCueTest*'`
Expected: 5 tests pass.

- [ ] **Step 5: Commit**

```bash
git add app/src/main app/src/test
git commit -q -m "feat(ritual): Cue chip and the cue / chant-header rules" -m "$TRAILER"
```

### Task 17: Chant tablet

**Files:**
- Create: `MAIN/ui/components/ChantTablet.kt`
- Modify: `MAIN/ui/components/KaraokeLine.kt`

- [ ] **Step 1: Let the newest syllable glow with the mic level**

In `app/src/main/java/com/summondemons/safely/ui/components/KaraokeLine.kt`:
- replace `private const val NEWEST_GLOW_BLUR = 18f` with
  `private const val NEWEST_GLOW_MIN = 8f` and `private const val NEWEST_GLOW_MIC = 14f`;
- add a parameter `micLevel: Float = 0f` after `style: TextStyle` (before `modifier`);
- in `remember(text, lit, accent, dim)` add `micLevel` to the keys, and change the newest
  syllable's shadow to `Shadow(accent, blurRadius = NEWEST_GLOW_MIN + NEWEST_GLOW_MIC * micLevel)`.

- [ ] **Step 2: Write the tablet**

`app/src/main/java/com/summondemons/safely/ui/components/ChantTablet.kt`:

```kotlin
package com.summondemons.safely.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.summondemons.safely.R
import com.summondemons.safely.ritual.RitualConfig.FOCUS_LOW_FRACTION
import com.summondemons.safely.ui.fx.LocalReduceMotion
import com.summondemons.safely.ui.theme.demonColors
import com.summondemons.safely.ui.theme.spacing
import com.summondemons.safely.ui.theme.textRoles
import com.summondemons.safely.ui.theme.upper

/** Phrase numerals, I–IV. */
val ROMAN_NUMERALS = listOf("I", "II", "III", "IV")

enum class FocusTone { Normal, Drift, Court }

private enum class MedallionState { Upcoming, Current, Sealed, Cracked }

private const val UPCOMING_ALPHA = 0.55f
private const val WAVERING_BORDER_ALPHA = 0.5f
private const val FLARE_GLOW_BLUR = 14f
private const val FLARE_HALF_MS = 250
private const val MEDALLION_FLARE_SCALE = 2.2f
private const val MEDALLION_SHADE = 0.45f
private const val FOCUS_TWEEN_MS = 120
private const val HEARTBEAT_HALF_MS = 300
private const val HEARTBEAT_MIN = 0.45f
private const val SHIMMER_MS = 2_400
private val MedallionSize = 18.dp
private val MedallionText = 8.sp
private val FocusBarHeight = 4.dp

/**
 * The framed chant panel (ADR-0014): header "Sacred chant" + [header], then one row per phrase
 * in the `incantation` role with an I–IV medallion; the focus bar sits under the current phrase.
 */
@Composable
fun ChantTablet(
    phrases: List<String>,
    phrase: Int,
    lit: Int,
    accent: Color,
    /** False while seeking/locking: the current phrase is readable but dim. */
    active: Boolean,
    header: String,
    headerColor: Color,
    /** Focus left in [0, 1] while the clock runs, else null (no bar). */
    focusFraction: Float?,
    focusTone: FocusTone,
    /** Phrase index that just sealed (flare) or cracked. */
    flare: Int?,
    cracked: Int?,
    micLevel: Float,
    wavering: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val spacing = MaterialTheme.spacing
    val roles = MaterialTheme.textRoles
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = colors.surfaceContainerLow,
        border = BorderStroke(1.dp, if (wavering) colors.error.copy(alpha = WAVERING_BORDER_ALPHA) else colors.outlineVariant),
    ) {
        Column(Modifier.padding(start = spacing.md, end = spacing.md, top = spacing.sm, bottom = spacing.md)) {
            Row(Modifier.fillMaxWidth().padding(bottom = spacing.xs), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.chant_title).upper(), style = roles.meta)
                Text(header, style = roles.meta, color = headerColor)
            }
            phrases.forEachIndexed { i, text ->
                val state = when {
                    cracked == i -> MedallionState.Cracked
                    i < phrase -> MedallionState.Sealed
                    i == phrase -> MedallionState.Current
                    else -> MedallionState.Upcoming
                }
                Row(
                    Modifier.fillMaxWidth().padding(vertical = spacing.xs),
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                    verticalAlignment = Alignment.Top,
                ) {
                    Medallion(ROMAN_NUMERALS[i], state, accent, flare = flare == i)
                    PhraseText(text, state, lit, accent, active, flare = flare == i, micLevel, Modifier.weight(1f))
                }
                if (i == phrase && focusFraction != null) {
                    FocusBar(focusFraction, focusTone, accent, Modifier.padding(start = MedallionSize + spacing.sm, bottom = spacing.xs))
                }
            }
        }
    }
}

@Composable
private fun PhraseText(
    text: String,
    state: MedallionState,
    lit: Int,
    accent: Color,
    active: Boolean,
    flare: Boolean,
    micLevel: Float,
    modifier: Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val style = MaterialTheme.textRoles.incantation
    val glow = remember { Animatable(0f) }
    LaunchedEffect(flare) {
        if (flare) {
            glow.animateTo(1f, tween(FLARE_HALF_MS))
            glow.animateTo(0f, tween(FLARE_HALF_MS))
        }
    }
    if (state == MedallionState.Current) {
        KaraokeLine(
            text = text,
            lit = lit,
            accent = accent,
            dim = if (active) colors.onSurface else colors.outline,
            style = style,
            micLevel = micLevel,
            modifier = modifier,
        )
        return
    }
    val color = when (state) {
        MedallionState.Cracked -> colors.error
        MedallionState.Sealed -> accent
        else -> colors.outline.copy(alpha = UPCOMING_ALPHA)
    }
    Text(
        text,
        style = style.copy(shadow = Shadow(accent, blurRadius = FLARE_GLOW_BLUR * glow.value)),
        color = color,
        textAlign = TextAlign.Center,
        modifier = modifier,
    )
}

@Composable
private fun Medallion(numeral: String, state: MedallionState, accent: Color, flare: Boolean) {
    val colors = MaterialTheme.colorScheme
    val highlight = MaterialTheme.demonColors.inkHighlight
    val reduceMotion = LocalReduceMotion.current
    val scale = remember { Animatable(1f) }
    LaunchedEffect(flare, state) {
        if (flare && state == MedallionState.Sealed && !reduceMotion) {
            scale.snapTo(MEDALLION_FLARE_SCALE)
            scale.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 500f))
        }
    }
    val wobble = remember { Animatable(0f) }
    LaunchedEffect(state) {
        if (state == MedallionState.Cracked && !reduceMotion) {
            wobble.animateTo(0f, keyframes { durationMillis = 400; -12f at 100; 8f at 250 })
        }
    }
    val shade = lerp(accent, colors.surfaceContainerLowest, MEDALLION_SHADE)
    val textColor = when (state) {
        MedallionState.Sealed -> colors.onPrimary
        MedallionState.Cracked -> colors.error
        MedallionState.Current -> accent
        MedallionState.Upcoming -> colors.outlineVariant
    }
    Box(
        Modifier
            .padding(top = 2.dp)
            .size(MedallionSize)
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
                rotationZ = wobble.value
            }
            .drawBehind {
                val ring = 1.dp.toPx()
                when (state) {
                    MedallionState.Sealed -> drawCircle(
                        Brush.radialGradient(
                            0f to highlight, 0.55f to accent, 1f to shade,
                            center = Offset(size.width * 0.35f, size.height * 0.3f),
                            radius = size.minDimension,
                        ),
                    )
                    MedallionState.Current -> drawCircle(accent, style = Stroke(ring))
                    MedallionState.Upcoming -> drawCircle(
                        colors.outlineVariant,
                        style = Stroke(ring, pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 2.dp.toPx()))),
                    )
                    MedallionState.Cracked -> {
                        drawCircle(colors.error, style = Stroke(ring))
                        drawLine(colors.error, Offset(size.width * 0.3f, 0f), Offset(size.width * 0.7f, size.height), ring)
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(numeral, style = MaterialTheme.textRoles.name.copy(fontSize = MedallionText), color = textColor)
    }
}

/**
 * Focus left for the current phrase (ADR-0010): accent; `error` while wavering; a moving gold
 * shimmer when this attempt has the court bonus; a heartbeat below `FOCUS_LOW_FRACTION`.
 */
@Composable
fun FocusBar(fraction: Float, tone: FocusTone, accent: Color, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val highlight = MaterialTheme.demonColors.inkHighlight
    val reduceMotion = LocalReduceMotion.current
    val width by animateFloatAsState(fraction, tween(FOCUS_TWEEN_MS, easing = LinearEasing), label = "focusWidth")
    val transition = rememberInfiniteTransition(label = "focusBar")
    val beat by transition.animateFloat(
        1f, HEARTBEAT_MIN, infiniteRepeatable(tween(HEARTBEAT_HALF_MS), RepeatMode.Reverse), label = "focusBeat",
    )
    val shimmer by transition.animateFloat(
        -1f, 1f, infiniteRepeatable(tween(SHIMMER_MS, easing = LinearEasing), RepeatMode.Restart), label = "focusShimmer",
    )
    val low = fraction < FOCUS_LOW_FRACTION
    val label = stringResource(R.string.focus_label)
    Canvas(
        modifier
            .fillMaxWidth()
            .height(FocusBarHeight)
            .semantics {
                contentDescription = label
                progressBarRangeInfo = ProgressBarRangeInfo(fraction, 0f..1f)
            },
    ) {
        val radius = CornerRadius(size.height / 2f)
        drawRoundRect(colors.surfaceContainerHighest, cornerRadius = radius)
        val filled = size.width * width
        if (filled <= 0f) return@Canvas
        val brush = when (tone) {
            FocusTone.Drift -> SolidColor(colors.error)
            FocusTone.Normal -> SolidColor(accent)
            FocusTone.Court -> {
                val start = if (reduceMotion) 0f else shimmer * size.width
                Brush.horizontalGradient(listOf(accent, highlight, accent), startX = start, endX = start + size.width, tileMode = TileMode.Mirror)
            }
        }
        drawRoundRect(brush, size = Size(filled, size.height), cornerRadius = radius, alpha = if (low) beat else 1f)
    }
}
```

- [ ] **Step 3: Build**

Run: `./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

```bash
git add app/src/main
git commit -q -m "feat(ui): Chant tablet with medallions and the focus bar" -m "$TRAILER"
```

### Task 18: Altar ring and the sigil flash

**Files:**
- Create: `MAIN/ui/components/AltarRing.kt`
- Modify: `MAIN/ui/fx/InkedSigil.kt`

- [ ] **Step 1: Write the altar ring** (replaces `CompassArc`, which is deleted in Task 25)

`app/src/main/java/com/summondemons/safely/ui/components/AltarRing.kt`:

```kotlin
package com.summondemons.safely.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.summondemons.safely.ritual.RitualPhase
import com.summondemons.safely.ritual.isAligned
import com.summondemons.safely.ritual.signedDelta
import com.summondemons.safely.ui.theme.demonColors

private val TrackWidth = 1.5.dp
private val RingWidth = 2.dp
private val MarkerSize = 9.dp
private const val MARKER_HALF_WIDTH = 0.6f

/**
 * The ring around the sigil (ADR-0014): an `outlineVariant` track; while Locking a
 * `ritual-safe` arc fills with [lockFraction]; once locked the full ring is `ritual-safe`,
 * `error` while Wavering. A marker on the ring points to the target relative to the
 * phone's heading, green when within tolerance.
 */
@Composable
fun AltarRing(azimuth: Float?, target: Float, phase: RitualPhase, lockFraction: Float, accent: Color, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val safe = MaterialTheme.demonColors.ritualSafe
    val ringColor by animateColorAsState(if (phase == RitualPhase.Wavering) colors.error else safe, label = "altarRing")
    val near = azimuth != null && isAligned(azimuth, target)
    val markerColor by animateColorAsState(if (near) safe else accent, label = "altarMarker")
    val offset = azimuth?.let { signedDelta(it, target) } ?: 0f
    val sweep = when (phase) {
        RitualPhase.Seeking -> 0f
        RitualPhase.Locking -> 360f * lockFraction.coerceIn(0f, 1f)
        else -> 360f
    }
    Canvas(modifier) {
        val marker = MarkerSize.toPx()
        val ring = RingWidth.toPx()
        val radius = size.minDimension / 2f - marker - ring
        drawCircle(colors.outlineVariant, radius, center, style = Stroke(TrackWidth.toPx()))
        if (sweep > 0f) {
            drawArc(
                ringColor, startAngle = -90f, sweepAngle = sweep, useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius), size = Size(radius * 2, radius * 2),
                style = Stroke(ring, cap = StrokeCap.Round),
            )
        }
        rotate(offset, pivot = center) {
            val tip = center.y - radius - ring / 2f - marker
            drawPath(
                Path().apply {
                    moveTo(center.x, tip)
                    lineTo(center.x + marker * MARKER_HALF_WIDTH, tip + marker)
                    lineTo(center.x - marker * MARKER_HALF_WIDTH, tip + marker)
                    close()
                },
                markerColor,
            )
        }
    }
}
```

- [ ] **Step 2: Add the seal flare / crack drain to `InkedSigil`**

In `app/src/main/java/com/summondemons/safely/ui/fx/InkedSigil.kt`:

1. After the `LoadedSigil` class add:

```kotlin
/**
 * One stroke group lit in [color] that fades over [durationMs]: a seal flare (accent →
 * ink highlight, 700 ms) or a crack drain (`error`, 1.5 s). [id] makes repeats distinct.
 */
@Immutable
data class SigilFlash(val group: Int, val color: Color, val durationMs: Int, val id: Long)
```

2. Add `private const val FLASH_WIDTH = 2f` to the constants.
3. Add the parameter `flash: SigilFlash? = null,` after `ghostAlpha: Float = 0f,`
   (and `* [flash] lights one stroke group for a moment.` to the KDoc).
4. Before `Box(modifier) {` add:

```kotlin
    val flashAlpha = remember { Animatable(0f) }
    LaunchedEffect(flash) {
        if (flash != null) {
            flashAlpha.snapTo(1f)
            flashAlpha.animateTo(0f, tween(flash.durationMs))
        }
    }
```

5. In the second `Canvas`, after `drawRim(...)` add:

```kotlin
            val alpha = flashAlpha.value
            if (flash != null && alpha > 0f) drawGroup(loaded, flash.group, flash.color.copy(alpha = alpha), STROKE * FLASH_WIDTH)
```

6. After `drawSigil` add:

```kotlin
/** Every stroke of ink group [group], fully drawn. */
private fun DrawScope.drawGroup(sigil: LoadedSigil, group: Int, color: Color, width: Float) {
    val unit = size.minDimension / VIEWBOX
    val origin = Offset((size.width - VIEWBOX * unit) / 2f, (size.height - VIEWBOX * unit) / 2f)
    translate(origin.x, origin.y) {
        scale(unit, unit, pivot = Offset.Zero) {
            val stroke = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round)
            sigil.strokes.forEachIndexed { i, s -> if (s.group == group) drawPath(sigil.paths[i], color, style = stroke) }
        }
    }
}
```

7. Add the imports `androidx.compose.animation.core.Animatable` and
   `androidx.compose.runtime.LaunchedEffect`.

- [ ] **Step 3: Build**

Run: `./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

```bash
git add app/src/main
git commit -q -m "feat(ui): Altar ring; sigil seal flare and crack drain" -m "$TRAILER"
```

### Task 19: Badge row and info band

**Files:**
- Create: `MAIN/ui/components/Badges.kt`

- [ ] **Step 1: Write the badges**

`app/src/main/java/com/summondemons/safely/ui/components/Badges.kt`:

```kotlin
package com.summondemons.safely.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.summondemons.safely.R
import com.summondemons.safely.data.Demon
import com.summondemons.safely.ritual.headingLabel
import com.summondemons.safely.ritual.headingLetter
import com.summondemons.safely.ui.theme.demonColors
import com.summondemons.safely.ui.theme.spacing
import com.summondemons.safely.ui.theme.textRoles
import com.summondemons.safely.ui.theme.upper

enum class BadgeKey { Direction, House, Calling, Legions }

data class BadgeInfo(val key: BadgeKey, val label: String, val text: String)

private const val RIM_ALPHA = 0.7f
private const val SELECTED_TINT = 0.22f
private const val BAND_ALPHA = 0.8f
private const val BAND_BORDER_ALPHA = 0.55f
private const val LAYER_FADE_MS = 150
private val BadgeIconSize = 24.dp
private val BandIconSize = 14.dp
private val BadgeLabelSize = 13.sp
private val BandTextSize = 14.sp
private val SelectedGlow = 8.dp

/** The four facts on a demon's card with their explanations (ADR-0011). */
@Composable
fun rememberBadges(demon: Demon): List<BadgeInfo> {
    val direction = headingLabel(demon.ritualHeading)
    val liege = demon.liege?.let { stringResource(R.string.badge_liege, it) } ?: stringResource(R.string.badge_no_liege)
    return listOf(
        BadgeInfo(BadgeKey.Direction, direction, stringResource(R.string.badge_direction_text, direction, liege)),
        BadgeInfo(BadgeKey.House, demon.house.planet, demon.house.tooltip),
        BadgeInfo(BadgeKey.Calling, demon.calling.label, demon.calling.tooltip),
        BadgeInfo(
            BadgeKey.Legions,
            stringResource(R.string.badge_legions_label, demon.legions),
            stringResource(R.string.badge_legions_text, demon.rank, demon.legions),
        ),
    )
}

/** Glyph in the demon accent (the rim carries the House metal). */
@Composable
fun BadgeIcon(key: BadgeKey, demon: Demon, size: Dp) {
    val accent = MaterialTheme.demonColors.accent(demon.accent)
    when (key) {
        BadgeKey.Direction -> DirectionGlyph(demon.ritualHeading, headingLetter(demon.ritualHeading), accent, size)
        BadgeKey.House -> HouseGlyph(demon.house, MaterialTheme.demonColors.metal(demon.house), size)
        BadgeKey.Calling -> CallingGlyph(demon.calling, accent, size)
        BadgeKey.Legions -> LegionsGlyph(accent, size)
    }
}

/** Four equal tiles; tapping one selects it (tapping it again deselects). */
@Composable
fun BadgeRow(demon: Demon, selected: BadgeKey?, onSelect: (BadgeKey?) -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val accent = MaterialTheme.demonColors.accent(demon.accent)
    val rim = MaterialTheme.demonColors.metal(demon.house)
    val shape = MaterialTheme.shapes.small
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)) {
        rememberBadges(demon).forEach { badge ->
            val on = badge.key == selected
            val explain = stringResource(R.string.badge_explain, badge.label)
            Column(
                Modifier
                    .weight(1f)
                    .then(if (on) Modifier.shadow(SelectedGlow, shape, ambientColor = accent, spotColor = accent) else Modifier)
                    .clip(shape)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                if (on) androidx.compose.ui.graphics.lerp(colors.surfaceContainerHigh, accent, SELECTED_TINT) else colors.surfaceContainerHigh,
                                colors.surfaceContainerLow,
                            ),
                        ),
                    )
                    .border(if (on) 1.5.dp else 1.dp, if (on) accent else rim.copy(alpha = RIM_ALPHA), shape)
                    .selectable(selected = on, role = Role.Tab, onClick = { onSelect(if (on) null else badge.key) })
                    .semantics { contentDescription = explain }
                    .padding(vertical = MaterialTheme.spacing.sm, horizontal = MaterialTheme.spacing.xs),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs),
            ) {
                BadgeIcon(badge.key, demon, BadgeIconSize)
                Text(
                    badge.label,
                    style = MaterialTheme.textRoles.name.copy(fontSize = BadgeLabelSize),
                    color = colors.onSurface,
                    maxLines = 1,
                    modifier = Modifier.clearAndSetSemantics { },
                )
            }
        }
    }
}

/**
 * The description, or the selected badge's explanation. Every demon's texts are stacked in
 * one Box, only the current one visible and in semantics, so the band is always as tall as the
 * tallest text and nothing below it moves (ADR-0016).
 */
@Composable
fun InfoBand(demons: List<Demon>, demon: Demon, badge: BadgeKey?, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val roles = MaterialTheme.textRoles
    val accent = MaterialTheme.demonColors.accent(demon.accent)
    val shape = MaterialTheme.shapes.small
    Box(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surfaceContainerHigh.copy(alpha = BAND_ALPHA))
            .then(if (badge != null) Modifier.border(1.dp, accent.copy(alpha = BAND_BORDER_ALPHA), shape) else Modifier)
            .padding(horizontal = MaterialTheme.spacing.lg, vertical = MaterialTheme.spacing.md),
        contentAlignment = Alignment.Center,
    ) {
        demons.forEach { d ->
            val current = d.id == demon.id
            BandLayer(visible = current && badge == null) {
                Text(
                    d.description,
                    style = roles.body.copy(fontSize = BandTextSize, fontWeight = FontWeight.SemiBold),
                    color = colors.primary,
                    textAlign = TextAlign.Center,
                )
            }
            rememberBadges(d).forEach { info ->
                BandLayer(visible = current && badge == info.key) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs + 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = MaterialTheme.spacing.xs),
                        ) {
                            BadgeIcon(info.key, d, BandIconSize)
                            Text(info.label.upper(), style = roles.meta, color = MaterialTheme.demonColors.accent(d.accent))
                        }
                        Text(info.text, style = roles.body.copy(fontSize = BandTextSize), color = colors.onSurface, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

@Composable
private fun BandLayer(visible: Boolean, content: @Composable () -> Unit) {
    // A cross-fade in every motion setting (reduced motion swaps slides for cross-fades, §4).
    val alpha by animateFloatAsState(if (visible) 1f else 0f, tween(LAYER_FADE_MS), label = "bandLayer")
    Box(
        Modifier
            .fillMaxWidth()
            .graphicsLayer { this.alpha = alpha }
            .then(if (visible) Modifier else Modifier.clearAndSetSemantics { }),
        contentAlignment = Alignment.Center,
    ) { content() }
}
```

(Replace the fully qualified `androidx.compose.ui.graphics.lerp` with an import if your
formatter prefers.)

- [ ] **Step 2: Build and commit**

Run: `./gradlew assembleDebug` → `BUILD SUCCESSFUL`.

```bash
git add app/src/main
git commit -q -m "feat(ui): Badge row and fixed-height info band" -m "$TRAILER"
```

### Task 20: Dossier carousel, smudges and ink-in

**Files:**
- Create: `MAIN/ui/components/DossierCarousel.kt`

- [ ] **Step 1: Write the carousel**

`app/src/main/java/com/summondemons/safely/ui/components/DossierCarousel.kt`:

```kotlin
package com.summondemons.safely.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.summondemons.safely.R
import com.summondemons.safely.data.Demon
import com.summondemons.safely.progress.DemonRecord
import com.summondemons.safely.ui.fx.LocalReduceMotion
import com.summondemons.safely.ui.theme.demonColors
import com.summondemons.safely.ui.theme.spacing
import com.summondemons.safely.ui.theme.textRoles
import com.summondemons.safely.ui.theme.upper
import kotlinx.coroutines.delay
import kotlin.math.abs

const val DOSSIER_PAGES = 6
const val SECRET_PAGE = 5

private const val PAGE_ANIM_MS = 180
private const val PAGE_SLIDE_DIVISOR = 12
private const val INK_MS = 900
private const val INK_STAGGER_MS = 120L
private const val SMUDGE_FIRST_WIDTH = 0.92f
private const val SMUDGE_WIDTH_STEP = 0.17f
private const val SMUDGE_STRIPE_SPLIT = 7f / 11f
private val Swipe = 40.dp
private val InkBlur = 6.dp
private val SmudgeHeight = 12.dp
private val SmudgeStripe = 11.dp
private val SmudgeShape = RoundedCornerShape(6.dp)
private val PageTextSize = 13.sp

/**
 * The six dossier pages (ADR-0011): ‹ › and horizontal swipe, wrapping. The page body scrolls
 * inside and never resizes the card. Unrevealed demons show smudges; [inkCodex] / [inkBattle]
 * ink the revealed lines in once after a summon.
 */
@Composable
fun DossierCarousel(
    demon: Demon,
    record: DemonRecord,
    inkCodex: Boolean,
    inkBattle: Boolean,
    page: Int,
    onPage: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val roles = MaterialTheme.textRoles
    val colors = MaterialTheme.colorScheme
    val reduceMotion = LocalReduceMotion.current
    val swipePx = with(LocalDensity.current) { Swipe.toPx() }
    val step: (Int) -> Unit = { delta -> onPage((page + delta + DOSSIER_PAGES) % DOSSIER_PAGES) }
    val currentStep by rememberUpdatedState(step)
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        PageButton("‹", stringResource(R.string.dossier_previous)) { step(-1) }
        Column(
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .pointerInput(Unit) {
                    var total = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { total = 0f },
                        onDragEnd = { if (abs(total) > swipePx) currentStep(if (total < 0f) 1 else -1) },
                    ) { _, amount -> total += amount }
                },
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    pageTitle(page, demon, record).upper(),
                    style = roles.meta,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Text(stringResource(R.string.dossier_page, page + 1, DOSSIER_PAGES), style = roles.meta)
            }
            AnimatedContent(
                targetState = page,
                transitionSpec = {
                    if (reduceMotion) {
                        fadeIn(tween(PAGE_ANIM_MS)) togetherWith fadeOut(tween(PAGE_ANIM_MS))
                    } else {
                        (fadeIn(tween(PAGE_ANIM_MS)) + slideInHorizontally(tween(PAGE_ANIM_MS)) { it / PAGE_SLIDE_DIVISOR }) togetherWith
                            (fadeOut(tween(PAGE_ANIM_MS)) + slideOutHorizontally(tween(PAGE_ANIM_MS)) { -it / PAGE_SLIDE_DIVISOR })
                    }
                },
                label = "dossierPage",
                modifier = Modifier.weight(1f).fillMaxWidth().padding(top = MaterialTheme.spacing.xs + 2.dp),
            ) { p ->
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs),
                ) {
                    PageBody(p, demon, record, inkCodex, inkBattle, colors.onSurface)
                }
            }
        }
        PageButton("›", stringResource(R.string.dossier_next)) { step(1) }
    }
}

@Composable
private fun pageTitle(page: Int, demon: Demon, record: DemonRecord): String = when (page) {
    0 -> if (record.revealed) stringResource(R.string.dossier_weakness_named, demon.weakness.name) else stringResource(R.string.dossier_weakness)
    1 -> stringResource(R.string.dossier_abilities)
    2 -> stringResource(R.string.dossier_offices)
    3 -> stringResource(R.string.dossier_appearance)
    4 -> stringResource(R.string.dossier_warning)
    else -> stringResource(R.string.dossier_secret)
}

@Composable
private fun PageBody(page: Int, demon: Demon, record: DemonRecord, inkCodex: Boolean, inkBattle: Boolean, textColor: androidx.compose.ui.graphics.Color) {
    val body = MaterialTheme.textRoles.body.copy(fontSize = PageTextSize, color = textColor)
    val accent = MaterialTheme.demonColors.accent(demon.accent)
    val nameStyle = MaterialTheme.textRoles.name
    val lines: List<String> = when (page) {
        0 -> listOf(demon.weakness.line)
        2 -> demon.offices.map { "•  $it" }
        3 -> listOf(demon.appearance)
        4 -> listOf(demon.warning)
        else -> emptyList()
    }
    when (page) {
        SECRET_PAGE -> if (record.flawless) {
            Text(demon.secret, style = body.copy(fontStyle = FontStyle.Italic)) // no ink-in, as in the prototype
        } else {
            Smudge(lines = 1)
            Text(stringResource(R.string.dossier_secret_locked).upper(), style = MaterialTheme.textRoles.meta)
        }
        1 -> if (!record.revealed) {
            Smudge(lines = 3)
        } else {
            demon.abilities.forEachIndexed { i, ability ->
                InkLine(inkBattle, i) {
                    Text(
                        buildAnnotatedString {
                            withStyle(SpanStyle(color = accent, fontFamily = nameStyle.fontFamily, fontWeight = nameStyle.fontWeight)) {
                                append("${ability.name}.")
                            }
                            append(" ${ability.line}")
                        },
                        style = body,
                    )
                }
            }
        }
        else -> if (!record.revealed) {
            Smudge(lines = if (page == 0 || page == 4) 2 else 3)
        } else {
            val ink = if (page == 0) inkBattle else inkCodex
            lines.forEachIndexed { i, line -> InkLine(ink, i) { Text(line, style = body) } }
        }
    }
}

@Composable
private fun PageButton(glyph: String, label: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.semantics { contentDescription = label }) {
        Text(glyph, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.outline, modifier = Modifier.clearAndSetSemantics { })
    }
}

/** Revealed text soaking in once: blur → sharp, staggered 120 ms per line, 900 ms each (§4). */
@Composable
fun InkLine(ink: Boolean, index: Int, content: @Composable () -> Unit) {
    val progress = remember { Animatable(if (ink) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (ink) {
            delay(index * INK_STAGGER_MS)
            progress.animateTo(1f, tween(INK_MS, easing = FastOutSlowInEasing))
        }
    }
    Box(Modifier.graphicsLayer { alpha = progress.value }.blur(InkBlur * (1f - progress.value))) { content() }
}

/** Sealed lore: rounded bars with a diagonal stripe pattern (DESIGN.md). */
@Composable
fun Smudge(lines: Int, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val stripe = with(LocalDensity.current) { SmudgeStripe.toPx() }
    val a = colors.surfaceContainerHighest
    val b = colors.surfaceContainerHigh
    val brush = remember(a, b, stripe) {
        Brush.linearGradient(
            0f to a, SMUDGE_STRIPE_SPLIT to a, SMUDGE_STRIPE_SPLIT to b, 1f to b,
            start = Offset.Zero, end = Offset(stripe, stripe * 0.2f), tileMode = TileMode.Repeated,
        )
    }
    val sealed = stringResource(R.string.dossier_sealed)
    Column(
        modifier.semantics { contentDescription = sealed },
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs + 2.dp),
    ) {
        repeat(lines) { i ->
            Box(
                Modifier
                    .fillMaxWidth(SMUDGE_FIRST_WIDTH - i * SMUDGE_WIDTH_STEP)
                    .height(SmudgeHeight)
                    .clip(SmudgeShape)
                    .background(brush),
            )
        }
    }
}
```

- [ ] **Step 2: Build and commit**

Run: `./gradlew assembleDebug` → `BUILD SUCCESSFUL`.

```bash
git add app/src/main
git commit -q -m "feat(ui): Dossier carousel with smudged and inked pages" -m "$TRAILER"
```

### Task 21: Quote slot, sigil avatar, floor circle

**Files:**
- Create: `MAIN/ui/components/QuoteSlot.kt`, `MAIN/ui/components/SigilAvatar.kt`, `MAIN/ui/components/FloorCircle.kt`
- Modify: `MAIN/ui/components/DemonCutout.kt`

- [ ] **Step 1: Let the cut-out stand on the floor**

In `app/src/main/java/com/summondemons/safely/ui/components/DemonCutout.kt`, add the
parameter `alignment: Alignment = Alignment.Center,` after `silhouette: Float = 0f,` and
pass `alignment = alignment,` to both `Image` calls.

- [ ] **Step 2: Write the quote slot**

`app/src/main/java/com/summondemons/safely/ui/components/QuoteSlot.kt`:

```kotlin
package com.summondemons.safely.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.summondemons.safely.R
import com.summondemons.safely.data.Demon
import com.summondemons.safely.ui.fx.LocalReduceMotion
import com.summondemons.safely.ui.fx.rememberShake
import com.summondemons.safely.ui.theme.demonColors
import com.summondemons.safely.ui.theme.spacing
import com.summondemons.safely.ui.theme.textRoles
import com.summondemons.safely.ui.theme.upper

/** What fills the slot under the demon (§4). */
enum class QuoteSlotState { Celebration, Bound, Ready, Cooling }

private const val BORDER_ALPHA = 0.67f
private const val BUBBLE_ALPHA = 0.92f
private const val GHOST_ALPHA = 0.6f
private const val DOT_ALPHA = 0.53f
private const val PULSE_HALF_MS = 900
private const val PULSE_MIN_ALPHA = 0.7f
private const val PULSE_SCALE = 1.03f
private const val POP_FROM = 0.6f
private const val SLOT_FADE_IN_MS = 200
private const val SLOT_FADE_OUT_MS = 150
val QuoteSlotHeight = 72.dp
private val QuoteSlotMaxWidth = 320.dp
private val QuoteSize = 15.sp
/** Three lines + padding must fit the 72 dp slot. */
private val QuoteLineHeight = 18.sp
private val SmallCueSize = 10.sp
private val DashOn = 6.dp
private val DashOff = 4.dp

/** One fixed-height slot, always filled, so the art never resizes between demons or states. */
@Composable
fun QuoteSlot(
    state: QuoteSlotState,
    demon: Demon,
    quote: String,
    hours: Int,
    shake: Int,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = MaterialTheme.demonColors.accent(demon.accent)
    Box(modifier.widthIn(max = QuoteSlotMaxWidth).fillMaxWidth().height(QuoteSlotHeight), contentAlignment = Alignment.Center) {
        AnimatedContent(
            targetState = state,
            transitionSpec = { fadeIn(tween(SLOT_FADE_IN_MS)) togetherWith fadeOut(tween(SLOT_FADE_OUT_MS)) },
            label = "quoteSlot",
        ) { s ->
            when (s) {
                QuoteSlotState.Celebration ->
                    PrimaryActionButton(stringResource(R.string.action_awesome), onTap, Modifier.fillMaxWidth())
                QuoteSlotState.Bound -> SpeechBubble(quote, accent)
                QuoteSlotState.Ready -> SummonCue(demon, accent, onTap)
                QuoteSlotState.Cooling -> CoolingCue(accent, hours, shake, onTap)
            }
        }
    }
}

@Composable
private fun SpeechBubble(quote: String, accent: Color) {
    val reduceMotion = LocalReduceMotion.current
    val pop = remember(quote) { Animatable(if (reduceMotion) 1f else POP_FROM) }
    LaunchedEffect(pop) { pop.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium)) }
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = BUBBLE_ALPHA),
        border = BorderStroke(2.dp, accent.copy(alpha = BORDER_ALPHA)),
        modifier = Modifier.fillMaxWidth().graphicsLayer {
            scaleX = pop.value
            scaleY = pop.value
        },
    ) {
        Text(
            quote,
            style = MaterialTheme.textRoles.body.copy(fontSize = QuoteSize, lineHeight = QuoteLineHeight, fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = MaterialTheme.spacing.lg, vertical = MaterialTheme.spacing.sm),
        )
    }
}

/** Sealed and ready: a dashed ghost bubble that dares you to summon him. */
@Composable
private fun SummonCue(demon: Demon, accent: Color, onTap: () -> Unit) {
    val reduceMotion = LocalReduceMotion.current
    val pulse = if (reduceMotion) {
        null
    } else {
        rememberInfiniteTransition(label = "summonCue").animateFloat(
            0f, 1f, infiniteRepeatable(tween(PULSE_HALF_MS), RepeatMode.Reverse), label = "summonPulse",
        )
    }
    val label = stringResource(R.string.cd_summon, demon.name)
    DashedBubble(
        color = accent.copy(alpha = BORDER_ALPHA),
        modifier = Modifier
            .graphicsLayer {
                val p = pulse?.value ?: 1f
                alpha = PULSE_MIN_ALPHA + (1f - PULSE_MIN_ALPHA) * p
                val s = 1f + (PULSE_SCALE - 1f) * p
                scaleX = s
                scaleY = s
            }
            .clickable(onClick = onTap)
            .clearAndSetSemantics {
                contentDescription = label
                role = Role.Button
            },
    ) {
        Text(stringResource(R.string.quote_tap_to_summon).upper(), style = MaterialTheme.textRoles.cue, color = accent)
    }
}

/** Sealed while the circle cools: dimmed; tapping it (or him) shakes it. */
@Composable
private fun CoolingCue(accent: Color, hours: Int, shake: Int, onTap: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val offset = rememberShake(shake)
    val label = stringResource(R.string.cd_cooling, hours)
    DashedBubble(
        color = colors.outlineVariant,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { translationX = offset.value.dp.toPx() }
            .clickable(onClick = onTap)
            .clearAndSetSemantics {
                contentDescription = label
                liveRegion = LiveRegionMode.Polite
            },
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(MaterialTheme.spacing.sm).clip(CircleShape).background(accent.copy(alpha = DOT_ALPHA)))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.quote_cooling).upper(), style = MaterialTheme.textRoles.cue, color = colors.onSurfaceVariant)
                Text(
                    stringResource(R.string.quote_next_summon, hours).upper(),
                    style = MaterialTheme.textRoles.cue.copy(fontSize = SmallCueSize),
                    color = colors.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun DashedBubble(color: Color, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val shape = MaterialTheme.shapes.medium
    Box(
        modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = GHOST_ALPHA))
            .drawBehind {
                val corner = 16.dp.toPx()
                drawRoundRect(
                    color,
                    cornerRadius = CornerRadius(corner),
                    style = Stroke(2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(DashOn.toPx(), DashOff.toPx()))),
                )
            }
            .padding(horizontal = MaterialTheme.spacing.xl, vertical = MaterialTheme.spacing.sm + 2.dp),
        contentAlignment = Alignment.Center,
    ) { content() }
}
```

(The `16.dp` corner matches `shapes.medium`; if lint flags the literal, read it from a
`private val BubbleCorner = 16.dp`.)

- [ ] **Step 3: Write the sigil avatar**

`app/src/main/java/com/summondemons/safely/ui/components/SigilAvatar.kt`:

```kotlin
package com.summondemons.safely.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.summondemons.safely.R
import com.summondemons.safely.data.Demon
import com.summondemons.safely.ui.fx.Jitter
import com.summondemons.safely.ui.fx.LocalReduceMotion
import com.summondemons.safely.ui.fx.rememberSigil
import com.summondemons.safely.ui.theme.demonColors
import kotlinx.coroutines.delay

private const val VIEWBOX = 100f
private const val SIGIL_STROKE = 3.5f
private const val RING_RADIUS = 46f
private const val RING_STROKE = 2.5f
private const val RING_ALPHA = 0.8f
private const val BURN_DELAY_MS = 400L
private const val BURN_MS = 1_400
private const val BURN_GROW = 0.4f
private val AvatarSize = 40.dp
private val SelectedAvatarSize = 52.dp
private val Ring = 2.dp
private val Glow = 8.dp

/**
 * Avatar rail item (ADR-0016): the demon's sigil in accent strokes while unbound, his portrait
 * once bound. Right after a summon ([burn]) the sigil burns away over the portrait once.
 */
@Composable
fun SigilAvatar(
    demon: Demon,
    bound: Boolean,
    burn: Boolean,
    restless: Boolean,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val accent = MaterialTheme.demonColors.accent(demon.accent)
    val reduceMotion = LocalReduceMotion.current
    val size by animateDpAsState(if (selected) SelectedAvatarSize else AvatarSize, label = "avatarSize")
    val sigil = rememberSigil(demon.sigil)
    val burnProgress = remember(burn) { Animatable(0f) }
    LaunchedEffect(burn) {
        if (burn) {
            delay(BURN_DELAY_MS)
            burnProgress.animateTo(1f, tween(BURN_MS))
        }
    }
    val label = if (bound) demon.name else stringResource(R.string.avatar_sealed, demon.name)
    Jitter(
        enabled = restless && bound,
        modifier = modifier
            .minimumInteractiveComponentSize()
            .selectable(selected = selected, enabled = enabled, role = Role.Tab, onClick = onClick)
            .semantics { contentDescription = label },
    ) {
        Box(
            Modifier
                .size(size)
                .then(if (selected) Modifier.shadow(Glow, CircleShape, ambientColor = accent, spotColor = accent) else Modifier)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(colors.surfaceContainer, colors.surfaceContainerLowest)))
                .border(Ring, if (selected) accent else colors.outlineVariant, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (bound) {
                Image(painterResource(demon.avatarArt), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
            if (!bound || burn) {
                val backdrop = colors.surfaceContainerLowest
                Canvas(
                    Modifier.fillMaxSize().graphicsLayer {
                        val p = if (burn) burnProgress.value else 0f
                        alpha = 1f - p
                        val grow = if (reduceMotion) 1f else 1f + BURN_GROW * p // reduced motion: cross-fade only
                        scaleX = grow
                        scaleY = grow
                    },
                ) {
                    if (burn) drawCircle(backdrop)
                    val unit = this.size.minDimension / VIEWBOX
                    scale(unit, unit, pivot = Offset.Zero) {
                        val stroke = Stroke(SIGIL_STROKE, cap = StrokeCap.Round, join = StrokeJoin.Round)
                        sigil.paths.forEach { drawPath(it, accent, style = stroke) }
                        drawCircle(accent.copy(alpha = RING_ALPHA), RING_RADIUS, Offset(VIEWBOX / 2, VIEWBOX / 2), style = Stroke(RING_STROKE))
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 4: Write the floor circle**

`app/src/main/java/com/summondemons/safely/ui/components/FloorCircle.kt`:

```kotlin
package com.summondemons.safely.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.summondemons.safely.ui.fx.LocalReduceMotion

private const val PULSE_HALF_MS = 800
private const val PULSE_MIN_ALPHA = 0.55f
private const val STATIC_ALPHA = 0.8f
private const val PULSE_SCALE = 1.04f
private const val GLOW_ALPHA = 0.12f
private const val GLOW_PASSES = 3
private const val INNER_ALPHA = 0.6f
private const val INNER_INSET_X = 0.10f
private const val INNER_INSET_Y = 0.14f

/** Accent ellipse with a dashed inner ring at the demon's feet; pulses while he can be summoned. */
@Composable
fun FloorCircle(accent: Color, pulse: Boolean, modifier: Modifier = Modifier) {
    val animate = pulse && !LocalReduceMotion.current
    val phase = if (animate) {
        rememberInfiniteTransition(label = "floorCircle").animateFloat(
            0f, 1f, infiniteRepeatable(tween(PULSE_HALF_MS), RepeatMode.Reverse), label = "floorPulse",
        )
    } else {
        null
    }
    Canvas(
        modifier.graphicsLayer {
            val p = phase?.value
            alpha = if (p == null) STATIC_ALPHA else PULSE_MIN_ALPHA + (1f - PULSE_MIN_ALPHA) * p
            val s = if (p == null) 1f else 1f + (PULSE_SCALE - 1f) * p
            scaleX = s
            scaleY = s
        },
    ) {
        val line = 1.5.dp.toPx()
        for (i in GLOW_PASSES downTo 1) drawOval(accent.copy(alpha = GLOW_ALPHA), style = Stroke(line + i * 6.dp.toPx()))
        drawOval(accent, style = Stroke(line))
        val inset = Offset(size.width * INNER_INSET_X, size.height * INNER_INSET_Y)
        drawOval(
            accent.copy(alpha = INNER_ALPHA),
            topLeft = inset,
            size = Size(size.width - 2 * inset.x, size.height - 2 * inset.y),
            style = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))),
        )
    }
}
```

- [ ] **Step 5: Build and commit**

Run: `./gradlew assembleDebug` → `BUILD SUCCESSFUL`.

```bash
git add app/src/main
git commit -q -m "feat(ui): Quote slot, sigil avatar with burn-away, floor circle" -m "$TRAILER"
```

### Task 22: Stage background, compass rose, restless grimoire

**Files:**
- Create: `MAIN/ui/components/StageBackground.kt`, `MAIN/ui/components/CompassRose.kt`, `MAIN/ui/components/RestlessGrimoire.kt`

- [ ] **Step 1: Write the stage background**

`app/src/main/java/com/summondemons/safely/ui/components/StageBackground.kt`:

```kotlin
package com.summondemons.safely.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import com.summondemons.safely.ui.theme.Stage
import com.summondemons.safely.ui.theme.demonColors

private const val HOME_LIGHT_Y = 0.4f
private const val STAGE_LIGHT_Y = 0.3f
private const val MID_STOP = 0.45f
private const val EDGE_STOP = 0.85f
private const val SCRIM_TOP_EXTRA = 0.15f
private const val SCRIM_MID_FACTOR = 0.4f
private const val SCRIM_BOTTOM_EXTRA = 0.5f
private const val SCRIM_MAX = 0.95f

/** Placeholder stage (DESIGN.md Gaps): a radial gradient under a top/bottom scrim of strength [dim]. */
@Composable
fun StageBackground(stage: Stage, dim: Float, modifier: Modifier = Modifier) {
    val gradient = MaterialTheme.demonColors.stage(stage)
    val scrim = MaterialTheme.colorScheme.surfaceContainerLowest
    val lightY = if (stage == Stage.Home) HOME_LIGHT_Y else STAGE_LIGHT_Y
    Canvas(modifier) {
        drawRect(
            Brush.radialGradient(
                0f to gradient.glow, MID_STOP to gradient.mid, EDGE_STOP to gradient.edge, 1f to gradient.edge,
                center = Offset(size.width / 2f, size.height * lightY),
                radius = size.maxDimension,
            ),
        )
        val bottom = scrim.copy(alpha = (dim + SCRIM_BOTTOM_EXTRA).coerceAtMost(SCRIM_MAX))
        drawRect(
            Brush.verticalGradient(
                0f to scrim.copy(alpha = (dim + SCRIM_TOP_EXTRA).coerceAtMost(1f)),
                0.35f to scrim.copy(alpha = dim * SCRIM_MID_FACTOR),
                0.78f to bottom,
                1f to bottom,
            ),
        )
    }
}
```

- [ ] **Step 2: Write the compass rose**

`app/src/main/java/com/summondemons/safely/ui/components/CompassRose.kt`:

```kotlin
package com.summondemons.safely.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.TextUnit
import com.summondemons.safely.ui.fx.svgPath
import com.summondemons.safely.ui.theme.textRoles

private const val VIEWBOX = 200f
private val C = Offset(100f, 100f)
private const val CARDINAL = "M100 12 L108 100 L100 92 L92 100Z"
private const val INTERCARDINAL = "M100 36 L105 100 L100 95 L95 100Z"
/** Decorative floor letters, not instructions (directions in copy come from demon data). */
private val LETTERS = listOf("N", "E", "S", "W")
private const val LETTER_SIZE = 9f
private const val LETTER_TOP = 2f

/** Inlaid compass rose for the Home floor; static, in `primary` (DESIGN.md). */
@Composable
fun CompassRose(modifier: Modifier = Modifier) {
    val gold = MaterialTheme.colorScheme.primary
    val measurer = rememberTextMeasurer()
    val letterStyle = MaterialTheme.textRoles.name
    Canvas(modifier.aspectRatio(1f)) {
        val unit = size.minDimension / VIEWBOX
        scale(unit, unit, pivot = Offset.Zero) {
            drawCircle(Brush.radialGradient(listOf(gold.copy(alpha = 0.35f), Color.Transparent), center = C, radius = 96f), 96f, C)
            drawCircle(gold.copy(alpha = 0.53f), 92f, C, style = Stroke(1.2f))
            drawCircle(gold.copy(alpha = 0.33f), 80f, C, style = Stroke(0.8f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(2f, 3f))))
            for (i in 0 until 8) {
                val cardinal = i % 2 == 0
                val point = svgPath(if (cardinal) CARDINAL else INTERCARDINAL)
                rotate(i * 45f, pivot = C) {
                    drawPath(point, gold.copy(alpha = if (cardinal) 0.4f else 0.2f))
                    drawPath(point, gold.copy(alpha = 0.67f), style = Stroke(0.6f))
                }
            }
        }
        LETTERS.forEachIndexed { i, letter ->
            val layout = measurer.measure(
                letter,
                letterStyle.copy(color = gold, fontSize = (LETTER_SIZE * unit).toSp(), lineHeight = TextUnit.Unspecified),
            )
            rotate(i * 90f, pivot = center) {
                drawText(layout, topLeft = Offset(center.x - layout.size.width / 2f, LETTER_TOP * unit))
            }
        }
    }
}
```

- [ ] **Step 3: Write the restless grimoire** (vector placeholder until the Stitch art exists; colors from tokens)

`app/src/main/java/com/summondemons/safely/ui/components/RestlessGrimoire.kt`:

```kotlin
package com.summondemons.safely.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.summondemons.safely.R
import com.summondemons.safely.ui.fx.LocalReduceMotion
import com.summondemons.safely.ui.fx.keyframe
import com.summondemons.safely.ui.fx.svgPath
import com.summondemons.safely.ui.theme.demonColors
import kotlinx.coroutines.delay
import kotlin.random.Random

private const val VIEW_W = 180f
private const val BOB_HALF_MS = 2_000
private const val BOB_UNITS = 8f
private const val HOP_MS = 600
private val HOP_Y = floatArrayOf(0f, -22f, 0f, -6f, 0f)
private val HOP_DEG = floatArrayOf(0f, -3f, 2f, -1f, 0f)
private const val PEEK_MS = 300
private const val PEEK_HOLD_MS = 1_300L
private const val COVER_PEEK_DEG = -9f
private val OPEN_SCALE = floatArrayOf(1f, 1.15f, 0.2f)
private val OPEN_ALPHA = floatArrayOf(1f, 1f, 0f)
private const val OPEN_MS = 700
private const val OPEN_REDUCED_MS = 150
private const val BURST_SHARE = 600f / 700f
private const val SILHOUETTE = "M58 36 q6 -24 14 -2 q18 -10 36 0 q8 -22 14 2 q-12 10 -32 10 q-20 0 -32 -10z"
private const val STRAPS = "M22 44h20M22 176h20M130 26v18M130 176v18"
private const val SEAL = "M86 92v36M76 102h20M74 118q12 10 24 0"
private val LOCK = Offset(86f, 110f)

private fun hopDelayMs(restless: Boolean) = if (restless) 2_000L + Random.nextLong(1_000) else 5_000L + Random.nextLong(3_000)
private fun peekDelayMs(restless: Boolean) = if (restless) 4_000L + Random.nextLong(2_000) else 10_000L + Random.nextLong(5_000)

private class GrimoireInk(
    val shadow: Color,
    val pages: Color,
    val cover: Color,
    val coverEdge: Color,
    val trim: Color,
    val chain: Color,
    val silhouette: Color,
    val burst: Color,
)

/**
 * The Home book (ADR-0014), a vector placeholder (DESIGN.md Gaps): idle bob, a hop every 5–8 s
 * (2–3 s when [restless]), a silhouette peeking every 10–15 s (4–6 s), the lock glowing in
 * [accent]. [opening] plays the scale-up and burst (a 150 ms fade under reduced motion).
 */
@Composable
fun RestlessGrimoire(restless: Boolean, accent: Color, opening: Boolean, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val reduceMotion = LocalReduceMotion.current
    val colors = MaterialTheme.colorScheme
    val ink = GrimoireInk(
        shadow = colors.surfaceContainerLowest,
        pages = colors.onSurfaceVariant,
        cover = colors.outlineVariant,
        coverEdge = colors.surfaceContainerHighest,
        trim = colors.primaryContainer,
        chain = colors.outline,
        silhouette = colors.surfaceContainerLowest,
        burst = MaterialTheme.demonColors.inkHighlight,
    )
    val hop = remember { Animatable(0f) }
    val peek = remember { Animatable(0f) }
    val open = remember { Animatable(0f) }
    LaunchedEffect(restless, reduceMotion) {
        if (reduceMotion) return@LaunchedEffect
        while (true) {
            delay(hopDelayMs(restless))
            hop.snapTo(0f)
            hop.animateTo(1f, tween(HOP_MS, easing = LinearEasing))
        }
    }
    LaunchedEffect(restless, reduceMotion) {
        if (reduceMotion) return@LaunchedEffect
        while (true) {
            delay(peekDelayMs(restless))
            peek.animateTo(1f, tween(PEEK_MS))
            delay(PEEK_HOLD_MS)
            peek.animateTo(0f, tween(PEEK_MS))
        }
    }
    LaunchedEffect(opening) {
        if (opening) open.animateTo(1f, tween(if (reduceMotion) OPEN_REDUCED_MS else OPEN_MS, easing = LinearEasing)) else open.snapTo(0f)
    }
    val bob = if (reduceMotion) {
        null
    } else {
        rememberInfiniteTransition(label = "grimoire").animateFloat(
            0f, -BOB_UNITS, infiniteRepeatable(tween(BOB_HALF_MS, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "bob",
        )
    }
    val label = stringResource(R.string.home_open)
    Canvas(
        modifier
            .graphicsLayer {
                val unit = size.width / VIEW_W
                translationY = ((bob?.value ?: 0f) + keyframe(hop.value, HOP_Y)) * unit
                rotationZ = keyframe(hop.value, HOP_DEG)
                val o = open.value
                if (reduceMotion) {
                    alpha = 1f - o
                } else {
                    val s = keyframe(o, OPEN_SCALE)
                    scaleX = s
                    scaleY = s
                    alpha = keyframe(o, OPEN_ALPHA)
                }
            }
            .clickable(onClickLabel = label, role = Role.Button, onClick = onOpen)
            .semantics { contentDescription = label },
    ) {
        val unit = size.width / VIEW_W
        scale(unit, unit, pivot = Offset.Zero) {
            drawGrimoire(ink, accent, peek.value, restless, if (reduceMotion) 0f else open.value)
        }
    }
}

private fun DrawScope.drawGrimoire(ink: GrimoireInk, accent: Color, peek: Float, restless: Boolean, open: Float) {
    drawOval(ink.shadow.copy(alpha = 0.45f), topLeft = Offset(20f, 194f), size = Size(140f, 16f))
    drawRoundRect(ink.pages, Offset(30f, 34f), Size(124f, 160f), CornerRadius(6f))
    if (peek > 0f) {
        translate(0f, (1f - peek) * 10f) {
            drawPath(svgPath(SILHOUETTE), ink.silhouette, alpha = peek)
            drawCircle(accent, 3f, Offset(80f, 30f), alpha = peek)
            drawCircle(accent, 3f, Offset(100f, 30f), alpha = peek)
        }
    }
    rotate(COVER_PEEK_DEG * peek, pivot = Offset(22f, 190f)) {
        drawRoundRect(
            Brush.horizontalGradient(listOf(ink.cover, ink.coverEdge), startX = 22f, endX = 150f),
            Offset(22f, 26f), Size(128f, 168f), CornerRadius(8f),
        )
        drawRoundRect(ink.trim, Offset(22f, 26f), Size(128f, 168f), CornerRadius(8f), style = Stroke(3f))
        drawPath(svgPath(STRAPS), ink.trim, style = Stroke(6f, cap = StrokeCap.Round))
        drawCircle(
            Brush.radialGradient(listOf(accent.copy(alpha = 0.9f), Color.Transparent), center = LOCK, radius = 34f),
            34f, LOCK, alpha = if (restless) 1f else 0.6f,
        )
        drawCircle(accent, 22f, LOCK, style = Stroke(2f))
        drawPath(svgPath(SEAL), accent, style = Stroke(2f, cap = StrokeCap.Round))
        val dash = PathEffect.dashPathEffect(floatArrayOf(8f, 4f))
        drawLine(ink.chain, Offset(14f, 80f), Offset(160f, 140f), 5f, StrokeCap.Round, dash)
        drawLine(ink.chain, Offset(14f, 140f), Offset(160f, 80f), 5f, StrokeCap.Round, dash)
        drawRoundRect(ink.trim, Offset(78f, 102f), Size(16f, 18f), CornerRadius(3f))
    }
    if (open > 0f) {
        val b = (open / BURST_SHARE).coerceAtMost(1f)
        drawCircle(ink.burst, 10f + 150f * b, LOCK, alpha = 1f - b)
    }
}
```

- [ ] **Step 4: Build and commit**

Run: `./gradlew assembleDebug` → `BUILD SUCCESSFUL`.

```bash
git add app/src/main
git commit -q -m "feat(ui): Stage background, compass rose and the restless grimoire placeholder" -m "$TRAILER"
```

- [ ] **Step 5: Phase gate**

Run the verification gate. Expected: `BUILD SUCCESSFUL`.

---
## Phase 5 — Screens

### Task 23: Home screen

**Files:**
- Create: `MAIN/ui/screens/HomeScreen.kt`
- Modify: `MAIN/ui/components/DebugControls.kt`, `MAIN/navigation/Routes.kt`, `MAIN/navigation/AppNavHost.kt`

- [ ] **Step 1: Add the debug chip**

Append to `app/src/main/java/com/summondemons/safely/ui/components/DebugControls.kt`
(add the import `androidx.compose.material3.AssistChip`):

```kotlin

/** Opens the debug sheet from Home and Grimoire. Debug builds only (caller checks BuildConfig.DEBUG). */
@Composable
fun DebugChip(onClick: () -> Unit, modifier: Modifier = Modifier) {
    AssistChip(onClick = onClick, label = { Text(stringResource(R.string.debug_chip)) }, modifier = modifier)
}
```

- [ ] **Step 2: Write the Home screen**

`app/src/main/java/com/summondemons/safely/ui/screens/HomeScreen.kt`:

```kotlin
package com.summondemons.safely.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.summondemons.safely.R
import com.summondemons.safely.data.Demon
import com.summondemons.safely.data.DemonAccent
import com.summondemons.safely.data.House
import com.summondemons.safely.platform.GameSounds
import com.summondemons.safely.platform.LocalGameSounds
import com.summondemons.safely.ui.components.CompassRose
import com.summondemons.safely.ui.components.CueChip
import com.summondemons.safely.ui.components.CueTone
import com.summondemons.safely.ui.components.DebugChip
import com.summondemons.safely.ui.components.RestlessGrimoire
import com.summondemons.safely.ui.components.RibbonBanner
import com.summondemons.safely.ui.components.StageBackground
import com.summondemons.safely.ui.fx.LocalReduceMotion
import com.summondemons.safely.ui.fx.ParticleField
import com.summondemons.safely.ui.fx.ParticleStyle
import com.summondemons.safely.ui.theme.PillShape
import com.summondemons.safely.ui.theme.Stage
import com.summondemons.safely.ui.theme.demonColors
import com.summondemons.safely.ui.theme.spacing
import com.summondemons.safely.ui.theme.textRoles
import com.summondemons.safely.ui.theme.upper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val HOME_DIM = 0.5f
private const val EMBER_ALPHA = 0.53f
private const val EMBER_COUNT = 18
private const val OPEN_MS = 700L
private const val OPEN_REDUCED_MS = 150L
private const val OPEN_CHIME_VOLUME = 0.5f
private const val DAIS_BOTTOM_FRACTION = 0.22f
private const val DAIS_WIDTH_FRACTION = 0.86f
private const val ROSE_TILT_DEG = 64f
/** In units of density; tune on device so the rose reads as a floor. */
private const val ROSE_CAMERA_DISTANCE = 12f
private const val COURT_BORDER_ALPHA = 0.5f
private const val SCRIM_ALPHA = 0.6f
private const val PANEL_ALPHA = 0.95f
private const val DUST_MS = 800
private const val DUST_ALPHA = 0.33f
private const val DUST_START = 0.6f
private const val DUST_END = 1.6f
private const val BANNER_POP_FROM = 0.9f
private val LogoSize = 96.dp
private val DaisHeight = 260.dp
private val RoseDrop = 90.dp
private val GrimoireWidth = 180.dp
private val GrimoireHeight = 210.dp
private val DustWidth = 256.dp
private val DustHeight = 160.dp

/** Home — "The Restless Grimoire" (ADR-0014). */
@Composable
fun HomeScreen(
    boundCount: Int,
    demonCount: Int,
    courtHouses: Set<House>,
    /** The focus demon's accent while any demon is restless; null otherwise. */
    restlessAccent: DemonAccent?,
    showHint: Boolean,
    escaped: List<Demon>,
    onOpen: () -> Unit,
    onDismissEscape: () -> Unit,
    onDebug: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val spacing = MaterialTheme.spacing
    val reduceMotion = LocalReduceMotion.current
    val sounds = LocalGameSounds.current
    val scope = rememberCoroutineScope()
    var opening by remember { mutableStateOf(false) }
    // If the app left Home during the 700 ms opening, the navigation was dropped: show the book again.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { opening = false }
    val open: () -> Unit = {
        if (!opening) {
            opening = true
            sounds?.play(GameSounds.Cue.Chime4, OPEN_CHIME_VOLUME)
            scope.launch {
                delay(if (reduceMotion) OPEN_REDUCED_MS else OPEN_MS)
                onOpen()
            }
        }
    }
    val lockAccent = restlessAccent?.let { MaterialTheme.demonColors.accent(it) } ?: colors.primary

    BoxWithConstraints(modifier.fillMaxSize().background(colors.background)) {
        StageBackground(Stage.Home, HOME_DIM, Modifier.fillMaxSize())
        ParticleField(colors.primary.copy(alpha = EMBER_ALPHA), ParticleStyle.Embers, Modifier.fillMaxSize(), count = EMBER_COUNT, seed = 3)
        DustPuff(Modifier.align(Alignment.Center))

        Column(
            Modifier.fillMaxWidth().statusBarsPadding().padding(top = spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            Image(
                painter = painterResource(R.drawable.ic_game_logo),
                contentDescription = stringResource(R.string.app_name),
                modifier = Modifier.size(LogoSize).clip(CircleShape),
            )
            CourtChip(courtHouses)
        }

        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = maxHeight * DAIS_BOTTOM_FRACTION)
                .fillMaxWidth(DAIS_WIDTH_FRACTION)
                .height(DaisHeight),
        ) {
            CompassRose(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .offset(y = RoseDrop)
                    .graphicsLayer {
                        rotationX = ROSE_TILT_DEG
                        cameraDistance = ROSE_CAMERA_DISTANCE * density
                    },
            )
            RestlessGrimoire(
                restless = restlessAccent != null,
                accent = lockAccent,
                opening = opening,
                onOpen = open,
                modifier = Modifier.align(Alignment.BottomCenter).size(GrimoireWidth, GrimoireHeight),
            )
        }

        Column(
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            Text(stringResource(R.string.home_bound, boundCount, demonCount).upper(), style = MaterialTheme.textRoles.meta)
            if (showHint) CueChip(stringResource(R.string.home_hint), CueTone.Neutral)
        }

        if (onDebug != null) {
            DebugChip(onDebug, Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(spacing.sm))
        }
        if (escaped.isNotEmpty()) EscapeBanner(escaped, onDismissEscape)
    }
}

@Composable
private fun CourtChip(houses: Set<House>) {
    val colors = MaterialTheme.colorScheme
    val inCourt = houses.isNotEmpty()
    val color = if (inCourt) MaterialTheme.demonColors.houseSol else colors.outline
    val join = stringResource(R.string.home_court_join)
    val text = if (inCourt) {
        stringResource(R.string.home_court, House.entries.filter { it in houses }.joinToString(join) { it.planet })
    } else {
        stringResource(R.string.home_out_of_office)
    }
    Surface(
        shape = PillShape,
        color = Color.Transparent,
        border = BorderStroke(1.dp, if (inCourt) color.copy(alpha = COURT_BORDER_ALPHA) else colors.outlineVariant),
    ) {
        Text(
            text.upper(),
            style = MaterialTheme.textRoles.meta,
            color = color,
            modifier = Modifier.padding(horizontal = MaterialTheme.spacing.md, vertical = MaterialTheme.spacing.xs),
        )
    }
}

@Composable
private fun BoxScope.EscapeBanner(escaped: List<Demon>, onDismiss: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val spacing = MaterialTheme.spacing
    val reduceMotion = LocalReduceMotion.current
    val pop = remember { Animatable(if (reduceMotion) 1f else BANNER_POP_FROM) }
    LaunchedEffect(Unit) { pop.animateTo(1f, spring(Spring.DampingRatioMediumBouncy)) }
    val dismiss = stringResource(R.string.escape_dismiss)
    Box(
        Modifier
            .matchParentSize()
            .background(colors.surfaceContainerLowest.copy(alpha = SCRIM_ALPHA))
            .clickable(onClickLabel = dismiss, onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.padding(horizontal = spacing.xl).graphicsLayer {
                scaleX = pop.value
                scaleY = pop.value
            },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            RibbonBanner(stringResource(R.string.escape_banner))
            Surface(shape = MaterialTheme.shapes.medium, color = colors.surfaceContainerHigh.copy(alpha = PANEL_ALPHA)) {
                Column(
                    Modifier.padding(horizontal = spacing.lg, vertical = spacing.sm),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        if (escaped.size > 1) stringResource(R.string.escape_all) else stringResource(R.string.escape_one, escaped.first().name),
                        style = MaterialTheme.textRoles.body,
                        color = colors.onSurface,
                        textAlign = TextAlign.Center,
                    )
                    Text(dismiss.upper(), style = MaterialTheme.textRoles.meta, modifier = Modifier.padding(top = spacing.xs))
                }
            }
        }
    }
}

/** The book "slams" shut when coming back Home: a dust puff (skipped under reduced motion). */
@Composable
private fun DustPuff(modifier: Modifier) {
    if (LocalReduceMotion.current) return
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) { progress.animateTo(1f, tween(DUST_MS)) }
    Canvas(modifier.size(DustWidth, DustHeight)) {
        val p = progress.value
        if (p >= 1f) return@Canvas
        scale(DUST_START + (DUST_END - DUST_START) * p) {
            drawOval(Brush.radialGradient(listOf(color.copy(alpha = DUST_ALPHA), Color.Transparent)), alpha = 1f - p)
        }
    }
}
```

- [ ] **Step 3: Add the `Home` route and destination**

`app/src/main/java/com/summondemons/safely/navigation/Routes.kt` — add above `Grimoire`:

```kotlin
@Serializable
data object Home

```

In `app/src/main/java/com/summondemons/safely/navigation/AppNavHost.kt`:
- change `startDestination = Grimoire` to `startDestination = Home`;
- add these imports: `androidx.lifecycle.compose.dropUnlessResumed` (already there),
  `com.summondemons.safely.progress.ProgressRules`, `com.summondemons.safely.ui.screens.HomeScreen`;
- add this destination before `composable<Grimoire>`:

```kotlin
                composable<Home> {
                    ProvideDestinationScope {
                        val demons = DemonCatalog.all
                        val anyRestless = demons.any { ProgressRules.isRestless(state.record(it.id), state.today) }
                        val focusId = ProgressRules.focusDemon(state.progress, DemonCatalog.ids, state.today)
                        HomeScreen(
                            boundCount = demons.count { state.record(it.id).bound },
                            demonCount = demons.size,
                            courtHouses = state.courtHouses,
                            restlessAccent = if (anyRestless) DemonCatalog.byId(focusId)?.accent else null,
                            showHint = !state.progress.homeHintSeen,
                            escaped = demons.filter { state.record(it.id).escapeBannerPending },
                            onOpen = dropUnlessResumed {
                                game.openGrimoireFromHome()
                                navController.navigate(Grimoire) { launchSingleTop = true }
                            },
                            onDismissEscape = game::dismissEscapeBanners,
                            onDebug = null, // Task 26
                        )
                    }
                }
```

- [ ] **Step 4: Build and check on a device**

Run: `./gradlew assembleDebug installDebug`
Expected: the app opens on Home (logo, court chip, the book bobbing on the rose,
"0 of 2 spirits bound", "Tap the grimoire"); tapping the book chimes and opens the old
Grimoire; back returns Home with a dust puff.

- [ ] **Step 5: Commit**

```bash
git add app/src/main
git commit -q -m "feat(home): The Restless Grimoire home screen" -m "$TRAILER"
```

### Task 24: Grimoire — stage over a static card; remove Profile

**Files:**
- Rewrite: `MAIN/ui/screens/GrimoireScreen.kt`, `MAIN/navigation/AppNavHost.kt`, `MAIN/navigation/Routes.kt`
- Modify: `MAIN/ui/fx/SharedDemonArt.kt`
- Delete: `MAIN/ui/screens/ProfileScreen.kt`

- [ ] **Step 1: Rewrite the Grimoire**

`app/src/main/java/com/summondemons/safely/ui/screens/GrimoireScreen.kt`:

```kotlin
package com.summondemons.safely.ui.screens

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.summondemons.safely.R
import com.summondemons.safely.data.Demon
import com.summondemons.safely.data.DemonCatalog
import com.summondemons.safely.platform.GameSounds
import com.summondemons.safely.platform.LocalGameSounds
import com.summondemons.safely.progress.DemonRecord
import com.summondemons.safely.progress.ProgressRules
import com.summondemons.safely.progress.Reveal
import com.summondemons.safely.ui.GameUiState
import com.summondemons.safely.ui.components.BadgeKey
import com.summondemons.safely.ui.components.BadgeRow
import com.summondemons.safely.ui.components.DebugChip
import com.summondemons.safely.ui.components.DemonCutout
import com.summondemons.safely.ui.components.DossierCarousel
import com.summondemons.safely.ui.components.FloorCircle
import com.summondemons.safely.ui.components.InfoBand
import com.summondemons.safely.ui.components.QuoteSlot
import com.summondemons.safely.ui.components.QuoteSlotState
import com.summondemons.safely.ui.components.RibbonBanner
import com.summondemons.safely.ui.components.RotatingSunburstBackground
import com.summondemons.safely.ui.components.SECRET_PAGE
import com.summondemons.safely.ui.components.SigilAvatar
import com.summondemons.safely.ui.components.StageBackground
import com.summondemons.safely.ui.fx.Jitter
import com.summondemons.safely.ui.fx.LocalReduceMotion
import com.summondemons.safely.ui.fx.ParticleField
import com.summondemons.safely.ui.fx.ParticleStyle
import com.summondemons.safely.ui.fx.SharedDemonArt
import com.summondemons.safely.ui.fx.rememberFrameTimeMs
import com.summondemons.safely.ui.theme.demonColors
import com.summondemons.safely.ui.theme.spacing
import com.summondemons.safely.ui.theme.stage
import com.summondemons.safely.ui.theme.textRoles
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/** Share of the area below the avatar rail taken by the card; raise for more text, lower for bigger art. */
private const val CARD_HEIGHT_FRACTION = 0.5f
private const val BADGE_INFO_MS = 5_000L
private const val AVATAR_BURN_CONSUME_MS = 2_200L
private const val STAGE_CROSSFADE_MS = 500
private const val DIM_BOUND = 0.35f
private const val DIM_SEALED = 0.55f
private const val EMBER_ALPHA = 0.67f
private const val EMBERS = 20
private const val PARTY_EMBERS = 50
private const val SMOKE = 8
private const val SUNBURST_ALPHA = 0.2f
private const val ART_ASPECT = 896f / 1200f
private const val FLOOR_WIDTH = 0.92f
private const val FLOOR_ASPECT = 4f
private const val SCENE_HEIGHT = 0.94f
private const val SCENE_BRIGHTNESS = 0.35f
private const val SCENE_SHADE_START = 0.55f
private const val SCENE_SHADE_ALPHA = 0.7f
private const val FLOAT_PERIOD_S = 4f
private const val PARTY_POP_FROM = 0.85f
private const val CARD_ALPHA = 0.95f
private const val METAL_RING_ALPHA = 0.45f
private val RailHeight = 72.dp
private val FloatAmplitude = 6.dp
private val SceneBlur = 3.dp
private val StageSwipe = 60.dp
private val CarouselMinHeight = 88.dp
private val CarouselTopPadding = 12.dp
private val MedallionSize = 48.dp
private val BannerDrop = 30.dp
private val NameSize = 26.sp
private val NumberSize = 18.sp
private val TitleSize = 12.sp

/** The Grimoire (ADR-0016): avatar rail, a stage over a static demon card, and the new-summon party. */
@Composable
fun GrimoireScreen(
    state: GameUiState,
    onBack: () -> Unit,
    onSelect: (String) -> Unit,
    onSummon: (String) -> Unit,
    onVisit: (String) -> Unit,
    onDismissCelebration: () -> Unit,
    onConsumeReveal: (String, Reveal) -> Unit,
    onSecretShown: () -> Unit,
    onDebug: (() -> Unit)?,
    modifier: Modifier = Modifier,
    demons: List<Demon> = DemonCatalog.all,
) {
    val demon = demons.firstOrNull { it.id == state.selectedId } ?: demons.first()
    val record = state.record(demon.id)
    val bound = record.bound
    val party = state.celebration?.takeIf { it.demonId == demon.id }
    val accent = MaterialTheme.demonColors.accent(demon.accent)
    val colors = MaterialTheme.colorScheme
    val spacing = MaterialTheme.spacing
    val reduceMotion = LocalReduceMotion.current
    val sounds = LocalGameSounds.current
    val density = LocalDensity.current

    // Captured when this demon is first shown: the visit clears restlessness, but he still sulks for this viewing.
    val restless = remember(demon.id, state.today) { ProgressRules.isRestless(record, state.today) }
    // Ink flags are read once per viewing and consumed at once; the remembered values keep the animation alive.
    val inkCodex = remember(demon.id) { record.revealed && record.revealPending.codex }
    val inkBattle = remember(demon.id) { record.revealed && record.revealPending.battle }
    var page by remember(demon.id) { mutableIntStateOf(0) }
    var badge by remember(demon.id) { mutableStateOf<BadgeKey?>(null) }
    var badgeTaps by remember { mutableIntStateOf(0) }
    var shake by remember { mutableIntStateOf(0) }
    var badgeRowBounds by remember { mutableStateOf(Rect.Zero) }
    var rootOrigin by remember { mutableStateOf(Offset.Zero) }
    var tadaFor by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(demon.id, bound, state.today) { if (bound) onVisit(demon.id) }
    LaunchedEffect(demon.id) {
        if (inkCodex) onConsumeReveal(demon.id, Reveal.Codex)
        if (inkBattle) onConsumeReveal(demon.id, Reveal.Battle)
    }
    LaunchedEffect(demon.id, bound, record.revealPending.avatar) {
        if (bound && record.revealPending.avatar) {
            delay(AVATAR_BURN_CONSUME_MS)
            onConsumeReveal(demon.id, Reveal.Avatar)
        }
    }
    LaunchedEffect(state.openSecret) {
        if (state.openSecret) {
            page = SECRET_PAGE
            onSecretShown()
        }
    }
    LaunchedEffect(badge, badgeTaps) {
        if (badge != null) {
            delay(BADGE_INFO_MS)
            badge = null
        }
    }
    // Saved, so recreating the activity doesn't replay the fanfare.
    val partyKey = party?.let { "${it.demonId}@${state.today}" }
    LaunchedEffect(partyKey) {
        if (partyKey != null && tadaFor != partyKey) {
            sounds?.play(GameSounds.Cue.Tada)
            tadaFor = partyKey
        }
    }
    BackHandler(enabled = party != null) { onDismissCelebration() }

    val primary: () -> Unit = {
        when {
            party != null -> onDismissCelebration()
            bound -> Unit
            !state.canSummonToday -> shake++
            else -> onSummon(demon.id)
        }
    }
    val index = demons.indexOf(demon)
    val step: (Int) -> Unit = { delta -> onSelect(demons[(index + delta + demons.size) % demons.size].id) }

    Box(
        modifier
            .fillMaxSize()
            .background(colors.background)
            .onGloballyPositioned { rootOrigin = it.boundsInRoot().topLeft }
            // Observe presses without consuming them: a tap outside the badge row clears the
            // badge and still does its own action (e.g. summon).
            .pointerInput(demon.id) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (event.type == PointerEventType.Press && event.changes.none { badgeRowBounds.contains(it.position + rootOrigin) }) {
                            badge = null
                        }
                    }
                }
            },
    ) {
        Crossfade(targetState = demon.house.stage(), animationSpec = tween(STAGE_CROSSFADE_MS), label = "stage") { stage ->
            StageBackground(stage, dim = if (bound) DIM_BOUND else DIM_SEALED, modifier = Modifier.fillMaxSize())
        }
        if (party != null) RotatingSunburstBackground(accent.copy(alpha = SUNBURST_ALPHA), Modifier.fillMaxSize(), rotating = !reduceMotion)
        ParticleField(
            accent.copy(alpha = EMBER_ALPHA), ParticleStyle.Embers, Modifier.fillMaxSize(),
            count = if (party != null) PARTY_EMBERS else EMBERS, seed = demon.id.hashCode(),
        )
        if (!bound) {
            ParticleField(colors.onSurfaceVariant, ParticleStyle.Smoke, Modifier.fillMaxSize(), count = SMOKE, seed = demon.id.hashCode() + 1)
        }

        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            AvatarRail(demons, state, demon.id, enabled = party == null, onBack, onSelect, onDebug)
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                var fixedPx by remember { mutableIntStateOf(0) }
                val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                // Fixed at the default font scale; grows only if header + band + badges + 88 dp don't fit.
                val minimum = with(density) { fixedPx.toDp() } + spacing.lg + spacing.md + CarouselTopPadding + CarouselMinHeight + navBottom
                val cardHeight = maxOf(maxHeight * CARD_HEIGHT_FRACTION, minimum)
                Column(Modifier.fillMaxSize()) {
                    Stage(
                        demon = demon,
                        bound = bound,
                        restless = restless,
                        party = party != null,
                        canSummon = state.canSummonToday,
                        quote = if (restless) demon.restlessQuote else demon.quote,
                        hours = state.hoursToNextSummon,
                        shake = shake,
                        onPrimary = primary,
                        onSwipe = step,
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                    )
                    DemonCard(
                        demons = demons,
                        demon = demon,
                        record = record,
                        badge = badge,
                        onBadge = {
                            badge = it
                            badgeTaps++
                        },
                        page = page,
                        onPage = { page = it },
                        inkCodex = inkCodex,
                        inkBattle = inkBattle,
                        onFixedHeight = { fixedPx = it },
                        onBadgeRowBounds = { badgeRowBounds = it },
                        modifier = Modifier.fillMaxWidth().height(cardHeight),
                    )
                }
                if (party != null) PartyBanner(Modifier.align(Alignment.TopCenter).padding(top = spacing.sm))
            }
        }
    }
}

@Composable
private fun AvatarRail(
    demons: List<Demon>,
    state: GameUiState,
    selectedId: String,
    enabled: Boolean,
    onBack: () -> Unit,
    onSelect: (String) -> Unit,
    onDebug: (() -> Unit)?,
) {
    val spacing = MaterialTheme.spacing
    val back = stringResource(R.string.grimoire_back)
    Box(Modifier.fillMaxWidth().height(RailHeight).padding(horizontal = spacing.sm)) {
        IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart).semantics { contentDescription = back }) {
            Text("←", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.clearAndSetSemantics { })
        }
        Row(
            Modifier.align(Alignment.Center),
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            demons.forEach { d ->
                val r = state.record(d.id)
                SigilAvatar(
                    demon = d,
                    bound = r.bound,
                    burn = r.bound && r.revealPending.avatar && d.id == selectedId,
                    restless = ProgressRules.isRestless(r, state.today),
                    selected = d.id == selectedId,
                    enabled = enabled,
                    onClick = { onSelect(d.id) },
                )
            }
        }
        if (onDebug != null) DebugChip(onDebug, Modifier.align(Alignment.CenterEnd))
    }
}

@Composable
private fun Stage(
    demon: Demon,
    bound: Boolean,
    restless: Boolean,
    party: Boolean,
    canSummon: Boolean,
    quote: String,
    hours: Int,
    shake: Int,
    onPrimary: () -> Unit,
    onSwipe: (Int) -> Unit,
    modifier: Modifier,
) {
    val accent = MaterialTheme.demonColors.accent(demon.accent)
    val spacing = MaterialTheme.spacing
    val reduceMotion = LocalReduceMotion.current
    val swipePx = with(LocalDensity.current) { StageSwipe.toPx() }
    val currentSwipe by rememberUpdatedState(onSwipe)
    val time by if (reduceMotion) remember { mutableLongStateOf(0L) } else rememberFrameTimeMs()
    val floatPx = with(LocalDensity.current) { FloatAmplitude.toPx() }
    val pop = remember(demon.id, party) { Animatable(if (party && !reduceMotion) PARTY_POP_FROM else 1f) }
    LaunchedEffect(pop) { pop.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow)) }
    val slot = when {
        party -> QuoteSlotState.Celebration
        bound -> QuoteSlotState.Bound
        canSummon -> QuoteSlotState.Ready
        else -> QuoteSlotState.Cooling
    }
    val label = when (slot) {
        QuoteSlotState.Celebration -> stringResource(R.string.action_awesome)
        QuoteSlotState.Bound -> demon.name
        QuoteSlotState.Ready -> stringResource(R.string.cd_summon, demon.name)
        QuoteSlotState.Cooling -> stringResource(R.string.cd_cooling, hours)
    }
    Column(
        modifier
            .pointerInput(party) {
                if (party) return@pointerInput // no swiping during the celebration
                var total = 0f
                detectHorizontalDragGestures(
                    onDragStart = { total = 0f },
                    onDragEnd = { if (abs(total) > swipePx) currentSwipe(if (total < 0f) 1 else -1) },
                ) { _, amount -> total += amount }
            }
            .padding(horizontal = spacing.xl)
            .padding(bottom = spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
            Box(Modifier.aspectRatio(ART_ASPECT, matchHeightConstraintsFirst = true)) {
                if (bound) SceneBackdrop(demon.sceneArt, Modifier.align(Alignment.TopCenter))
                FloorCircle(
                    accent = accent,
                    pulse = !party && !bound && canSummon,
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(FLOOR_WIDTH).aspectRatio(FLOOR_ASPECT),
                )
                SharedDemonArt(
                    demonId = demon.id,
                    modifier = Modifier.fillMaxSize().graphicsLayer {
                        translationY = if (reduceMotion) 0f else sin(time / 1_000f * 2f * PI.toFloat() / FLOAT_PERIOD_S) * floatPx
                        scaleX = pop.value
                        scaleY = pop.value
                    },
                ) {
                    Jitter(enabled = restless, modifier = Modifier.fillMaxSize()) {
                        DemonCutout(
                            art = demon.cutoutArt,
                            contentDescription = null,
                            accent = accent,
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    enabled = !bound || party,
                                    onClick = onPrimary,
                                )
                                .semantics { contentDescription = label },
                            silhouette = if (bound) 0f else 1f,
                            alignment = Alignment.BottomCenter,
                        )
                    }
                }
            }
        }
        QuoteSlot(slot, demon, quote, hours, shake, onTap = onPrimary, modifier = Modifier.padding(top = spacing.md))
    }
}

/** The demon's own scene, dimmed and softened, framing the cut-out. */
@Composable
private fun SceneBackdrop(@DrawableRes art: Int, modifier: Modifier) {
    val shade = MaterialTheme.colorScheme.surfaceContainerLowest
    val dim = remember { ColorMatrix().apply { setToScale(SCENE_BRIGHTNESS, SCENE_BRIGHTNESS, SCENE_BRIGHTNESS, 1f) } }
    Box(modifier.fillMaxWidth().fillMaxHeight(SCENE_HEIGHT).clip(MaterialTheme.shapes.medium)) {
        Image(
            painter = painterResource(art),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            colorFilter = ColorFilter.colorMatrix(dim),
            modifier = Modifier.fillMaxSize().blur(SceneBlur), // API 31+; plain dimmed art below
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(SCENE_SHADE_START to Color.Transparent, 1f to shade.copy(alpha = SCENE_SHADE_ALPHA)),
            ),
        )
    }
}

@Composable
private fun DemonCard(
    demons: List<Demon>,
    demon: Demon,
    record: DemonRecord,
    badge: BadgeKey?,
    onBadge: (BadgeKey?) -> Unit,
    page: Int,
    onPage: (Int) -> Unit,
    inkCodex: Boolean,
    inkBattle: Boolean,
    onFixedHeight: (Int) -> Unit,
    onBadgeRowBounds: (Rect) -> Unit,
    modifier: Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val spacing = MaterialTheme.spacing
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large.copy(bottomStart = CornerSize(0.dp), bottomEnd = CornerSize(0.dp)),
        color = colors.surfaceContainerLow.copy(alpha = CARD_ALPHA),
        border = BorderStroke(1.dp, colors.outlineVariant),
    ) {
        Column(Modifier.navigationBarsPadding().padding(top = spacing.lg, bottom = spacing.md)) {
            Column(Modifier.onSizeChanged { onFixedHeight(it.height) }) {
                CardHeader(demon, record.bound, Modifier.padding(horizontal = spacing.lg))
                InfoBand(demons, demon, badge, Modifier.padding(horizontal = spacing.lg).padding(top = spacing.lg))
                BadgeRow(
                    demon = demon,
                    selected = badge,
                    onSelect = onBadge,
                    modifier = Modifier
                        .padding(horizontal = spacing.lg)
                        .padding(top = spacing.lg)
                        .onGloballyPositioned { onBadgeRowBounds(it.boundsInRoot()) },
                )
            }
            DossierCarousel(
                demon = demon,
                record = record,
                inkCodex = inkCodex,
                inkBattle = inkBattle,
                page = page,
                onPage = onPage,
                modifier = Modifier.weight(1f).fillMaxWidth().padding(top = CarouselTopPadding).padding(horizontal = spacing.xs),
            )
        }
    }
}

/** Tarot-style header: number medallion in the House metal, name, title. */
@Composable
private fun CardHeader(demon: Demon, bound: Boolean, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    val metal = MaterialTheme.demonColors.metal(demon.house)
    val accent = MaterialTheme.demonColors.accent(demon.accent)
    val roles = MaterialTheme.textRoles
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(MedallionSize)
                .background(colors.surfaceContainerLow, CircleShape)
                .then(Modifier.padding(0.dp))
                .clip(CircleShape)
                .background(metal.copy(alpha = METAL_RING_ALPHA), CircleShape)
                .padding(1.dp)
                .background(colors.surfaceContainerLow, CircleShape)
                .padding(2.dp)
                .background(metal, CircleShape)
                .padding(1.5.dp)
                .background(colors.surfaceContainerLow, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(demon.number.toString(), style = roles.name.copy(fontSize = NumberSize), color = metal)
        }
        Column(Modifier.weight(1f)) {
            Text(
                demon.name,
                style = roles.name.copy(fontSize = NameSize),
                color = if (bound) accent else colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                demon.title,
                style = roles.body.copy(fontSize = TitleSize),
                color = colors.outline,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** "New Demon Received!" drops in at the top of the stage. */
@Composable
private fun PartyBanner(modifier: Modifier) {
    val reduceMotion = LocalReduceMotion.current
    val dropPx = with(LocalDensity.current) { BannerDrop.toPx() }
    val drop = remember { Animatable(if (reduceMotion) 0f else -dropPx) }
    LaunchedEffect(Unit) { drop.animateTo(0f, spring(Spring.DampingRatioMediumBouncy)) }
    RibbonBanner(
        stringResource(R.string.celebration_banner),
        modifier.graphicsLayer {
            translationY = drop.value
            alpha = 1f - drop.value / -dropPx * 0.9f
        },
    )
}
```

(The medallion's stacked backgrounds draw the inner and outer metal rings without a
custom Canvas; if you prefer, replace them with two `border` calls — the look is the
same: a 1.5 dp metal ring with a faint outer ring.)

- [ ] **Step 2: Update the shared-element KDoc**

In `app/src/main/java/com/summondemons/safely/ui/fx/SharedDemonArt.kt`, change
`Wraps a demon's cut-out so it glides between Grimoire, Ritual (climax) and Profile.` to
`Wraps a demon's cut-out so it glides from the Ritual climax back to the Grimoire stage.`

- [ ] **Step 3: Remove the Profile route**

`app/src/main/java/com/summondemons/safely/navigation/Routes.kt`:

```kotlin
package com.summondemons.safely.navigation

import kotlinx.serialization.Serializable

@Serializable
data object Home

@Serializable
data object Grimoire

@Serializable
data class Ritual(val demonId: String)
```

```bash
git rm -q app/src/main/java/com/summondemons/safely/ui/screens/ProfileScreen.kt
```

- [ ] **Step 4: Rewrite `AppNavHost`** (final form, including the Ritual guard; the debug sheet is wired in Task 26)

`app/src/main/java/com/summondemons/safely/navigation/AppNavHost.kt`:

```kotlin
package com.summondemons.safely.navigation

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.summondemons.safely.AppGraph
import com.summondemons.safely.data.Demon
import com.summondemons.safely.data.DemonCatalog
import com.summondemons.safely.platform.AndroidHeadingSource
import com.summondemons.safely.platform.AndroidPcmSource
import com.summondemons.safely.platform.GameSounds
import com.summondemons.safely.platform.LocalGameSounds
import com.summondemons.safely.progress.ProgressRules
import com.summondemons.safely.ritual.RitualPhase
import com.summondemons.safely.ritual.RitualViewModel
import com.summondemons.safely.ui.GameUiState
import com.summondemons.safely.ui.GameViewModel
import com.summondemons.safely.ui.fx.LocalNavAnimatedVisibilityScope
import com.summondemons.safely.ui.fx.LocalSharedTransitionScope
import com.summondemons.safely.ui.screens.GrimoireScreen
import com.summondemons.safely.ui.screens.HomeScreen
import com.summondemons.safely.ui.screens.RitualScreen

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AppNavHost(graph: AppGraph, navController: NavHostController = rememberNavController()) {
    val factory = remember(graph) { GameViewModel.factory(graph.progressStore, graph.clock, graph.debugSettings) }
    val game: GameViewModel = viewModel(factory = factory) // activity-scoped
    val state by game.uiState.collectAsStateWithLifecycle()
    val appContext = LocalContext.current.applicationContext
    val sounds = remember { GameSounds(appContext) }
    DisposableEffect(sounds) { onDispose { sounds.release() } }
    LifecycleEventEffect(Lifecycle.Event.ON_START) { game.evaluate() }

    if (!state.loaded) {
        // The splash normally covers this; never render screens from an unloaded Progress().
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        return
    }
    val onDebug: (() -> Unit)? = null // Task 26

    CompositionLocalProvider(LocalGameSounds provides sounds) {
        SharedTransitionLayout {
            CompositionLocalProvider(LocalSharedTransitionScope provides this) {
                NavHost(navController = navController, startDestination = Home) {
                    composable<Home> {
                        ProvideDestinationScope {
                            HomeDestination(state, onDebug, onOpen = dropUnlessResumed {
                                game.openGrimoireFromHome()
                                navController.navigate(Grimoire) { launchSingleTop = true }
                            }, onDismissEscape = game::dismissEscapeBanners)
                        }
                    }

                    composable<Grimoire> {
                        ProvideDestinationScope {
                            val lifecycle = LocalLifecycleOwner.current.lifecycle
                            GrimoireScreen(
                                state = state,
                                onBack = dropUnlessResumed { navController.popBackStack() },
                                onSelect = game::select,
                                onSummon = { id ->
                                    if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) navController.navigate(Ritual(id))
                                },
                                onVisit = game::visit,
                                onDismissCelebration = game::dismissCelebration,
                                onConsumeReveal = game::consumeReveal,
                                onSecretShown = game::secretShown,
                                onDebug = onDebug,
                            )
                        }
                    }

                    composable<Ritual> { entry ->
                        ProvideDestinationScope {
                            val demon = DemonCatalog.byId(entry.toRoute<Ritual>().demonId)
                            if (demon == null) {
                                LaunchedEffect(Unit) { navController.popBackStack() }
                            } else {
                                RitualDestination(demon, game, state, appContext, navController)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeDestination(state: GameUiState, onDebug: (() -> Unit)?, onOpen: () -> Unit, onDismissEscape: () -> Unit) {
    val demons = DemonCatalog.all
    val anyRestless = demons.any { ProgressRules.isRestless(state.record(it.id), state.today) }
    val focusId = ProgressRules.focusDemon(state.progress, DemonCatalog.ids, state.today)
    HomeScreen(
        boundCount = demons.count { state.record(it.id).bound },
        demonCount = demons.size,
        courtHouses = state.courtHouses,
        restlessAccent = if (anyRestless) DemonCatalog.byId(focusId)?.accent else null,
        showHint = !state.progress.homeHintSeen,
        escaped = demons.filter { state.record(it.id).escapeBannerPending },
        onOpen = onOpen,
        onDismissEscape = onDismissEscape,
        onDebug = onDebug,
    )
}

@Composable
private fun RitualDestination(
    demon: Demon,
    game: GameViewModel,
    state: GameUiState,
    appContext: android.content.Context,
    navController: NavHostController,
) {
    // The day the ritual started spends the summon, even if the climax lands after midnight (§1).
    val startDay = rememberSaveable { game.today() }
    val ritualFactory = remember(demon) {
        RitualViewModel.factory(
            demon,
            AndroidHeadingSource(appContext),
            AndroidPcmSource(),
            inCourt = { game.inCourtNow(demon.house) },
            commitSummon = { flawless -> game.commitSummon(demon.id, startDay, flawless) },
        )
    }
    val ritual: RitualViewModel = viewModel(factory = ritualFactory)
    // Checked once, when the Ritual opens (incl. restored after process death): a restored back
    // stack must never allow a second summon (§3). Not re-checked later, so the commit at
    // ClimaxStart can never pop the climax.
    val blocked = remember {
        val phase = ritual.uiState.value.ritual.phase
        val settled = phase == RitualPhase.Climax || phase == RitualPhase.Done
        !settled && (state.record(demon.id).bound || !state.canSummonToday)
    }
    if (blocked) {
        LaunchedEffect(Unit) { navController.popBackStack() }
        return
    }
    RitualScreen(
        viewModel = ritual,
        onSummoned = {
            game.celebrate(demon.id, ritual.uiState.value.ritual.flawless)
            navController.popBackStack<Grimoire>(inclusive = false)
        },
        onAbort = dropUnlessResumed { navController.popBackStack() },
    )
}

@Composable
private fun AnimatedVisibilityScope.ProvideDestinationScope(content: @Composable () -> Unit) =
    CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this, content = content)
```

- [ ] **Step 5: Build, test and check on a device**

Run: `./gradlew testDebugUnitTest assembleDebug installDebug`
Expected: `BUILD SUCCESSFUL`. On the device: Home → Grimoire on Asmodeus (sealed
silhouette, pulsing floor, "TAP TO SUMMON"); the card shows header, band, badges and
smudged pages; tapping a badge swaps the band text and it returns after 5 s; the
carousel wraps; swiping the stage or tapping an avatar switches demon; the card and art
keep their size.

- [ ] **Step 6: Commit**

```bash
git add -A app/src/main
git commit -q -m "feat(grimoire): Stage over a static demon card; celebration on the Grimoire; remove Profile" -m "$TRAILER"
```

### Task 25: Ritual — cue chip, altar ring, chant tablet, abandon dialog

**Files:**
- Rewrite: `MAIN/ui/screens/RitualScreen.kt`
- Delete: `MAIN/ui/components/CompassArc.kt`

- [ ] **Step 1: Rewrite the Ritual screen**

`app/src/main/java/com/summondemons/safely/ui/screens/RitualScreen.kt`:

```kotlin
package com.summondemons.safely.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.summondemons.safely.BuildConfig
import com.summondemons.safely.R
import com.summondemons.safely.data.Demon
import com.summondemons.safely.platform.RitualFeedback
import com.summondemons.safely.ritual.RitualConfig.LOCK_HOLD_MS
import com.summondemons.safely.ritual.RitualEffect
import com.summondemons.safely.ritual.RitualPhase
import com.summondemons.safely.ritual.RitualUiState
import com.summondemons.safely.ritual.RitualViewModel
import com.summondemons.safely.ritual.headingLabel
import com.summondemons.safely.ui.components.AltarRing
import com.summondemons.safely.ui.components.ChantTablet
import com.summondemons.safely.ui.components.CueChip
import com.summondemons.safely.ui.components.DebugControls
import com.summondemons.safely.ui.components.DemonCutout
import com.summondemons.safely.ui.components.FocusTone
import com.summondemons.safely.ui.components.PrimaryActionButton
import com.summondemons.safely.ui.components.ROMAN_NUMERALS
import com.summondemons.safely.ui.fx.InkedSigil
import com.summondemons.safely.ui.fx.LocalReduceMotion
import com.summondemons.safely.ui.fx.ParticleField
import com.summondemons.safely.ui.fx.ParticleStyle
import com.summondemons.safely.ui.fx.SharedDemonArt
import com.summondemons.safely.ui.fx.SigilFlash
import com.summondemons.safely.ui.fx.segment
import com.summondemons.safely.ui.theme.demonColors
import com.summondemons.safely.ui.theme.spacing
import com.summondemons.safely.ui.theme.textRoles
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

private const val SIGIL_WIDTH_FRACTION = 0.86f
private const val SIGIL_IN_RING_FRACTION = 0.84f
private const val SIGIL_MAX_HEIGHT_FRACTION = 0.42f
private const val GHOST_ALPHA = 0.12f
private const val CUTOUT_ASPECT = 896f / 1200f
private const val CUTOUT_WIDTH_FRACTION = 0.8f
private const val RING_ANIM_MS = 700
private const val HEARTBEAT_HALF_MS = 150 // 300 ms heartbeat while wavering
private const val WAVER_MIN_ALPHA = 0.45f
private const val SEAL_FLARE_MS = 700
private const val CRACK_MS = 1_500
private const val SEAL_FLASH_MIX = 0.5f
private const val EMBER_ALPHA = 0.5f
private const val CLIMAX_SCRIM_ALPHA = 0.85f
private const val CLIMAX_PARTICLES = 80
private const val FLASH_PEAK = 0.65f
private const val FLASH_HALF_WIDTH = 0.06f
private const val FLASH_ACCENT_ALPHA = 0.6f
private const val RISE_START_SCALE = 0.4f
private val RiseDistance = 48.dp
private val NameSize = 20.sp

/** Phases in which the sigil's rings are inked (the ritual is or was locked this attempt). */
private val SEALED_PHASES = setOf(RitualPhase.Chanting, RitualPhase.Wavering, RitualPhase.Climax, RitualPhase.Done)

/** A phrase that just sealed or cracked; [id] keeps repeats distinct. */
private data class Mark(val phrase: Int, val id: Long)

@Composable
fun RitualScreen(
    viewModel: RitualViewModel,
    onSummoned: () -> Unit,
    onAbort: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = LocalActivity.current
    RitualFeedback(viewModel.effects, state.demon.ambientBed, state.ritual.phase)
    // The focus clock only runs while the screen is started (§2).
    LifecycleEventEffect(Lifecycle.Event.ON_START) { viewModel.setActive(true) }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.setActive(false) }

    // Ask once; re-check on every resume (e.g. after returning from Settings).
    var requested by rememberSaveable { mutableStateOf(false) }
    var permanentlyDenied by rememberSaveable { mutableStateOf(false) }
    var deniedBefore by rememberSaveable { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        viewModel.onMicPermission(granted)
        // A first denial or a dismissed dialog also reports "no rationale"; only a repeat
        // denial without rationale means "don't ask again".
        permanentlyDenied = !granted && deniedBefore && activity != null &&
            !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.RECORD_AUDIO)
        if (!granted) deniedBefore = true
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        when {
            granted -> viewModel.onMicPermission(true)
            !requested -> {
                requested = true
                launcher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    val done = state.ritual.phase == RitualPhase.Done
    LaunchedEffect(done) {
        if (done) onSummoned()
    }

    // Seal flares and the crack cue come from the lossy effects flow: UI only, nothing durable.
    var flare by remember { mutableStateOf<Mark?>(null) }
    var cracked by remember { mutableStateOf<Mark?>(null) }
    var flash by remember { mutableStateOf<SigilFlash?>(null) }
    val accent = MaterialTheme.demonColors.accent(state.demon.accent)
    val sealColor = lerp(accent, MaterialTheme.demonColors.inkHighlight, SEAL_FLASH_MIX)
    val crackColor = MaterialTheme.colorScheme.error
    LaunchedEffect(viewModel) {
        var serial = 0L
        viewModel.effects.collect { effect ->
            when (effect) {
                is RitualEffect.Sealed -> {
                    val mark = Mark(effect.phrase, ++serial)
                    flare = mark
                    flash = SigilFlash(effect.phrase + 1, sealColor, SEAL_FLARE_MS, mark.id)
                    launch {
                        delay(SEAL_FLARE_MS.toLong())
                        if (flare == mark) flare = null
                    }
                }
                is RitualEffect.Cracked -> {
                    val mark = Mark(effect.phrase, ++serial)
                    cracked = mark
                    flash = SigilFlash(effect.phrase + 1, crackColor, CRACK_MS, mark.id)
                    launch {
                        delay(CRACK_MS.toLong())
                        if (cracked == mark) cracked = null
                    }
                }
                else -> Unit
            }
        }
    }

    RitualContent(
        state = state,
        flare = flare?.phrase,
        cracked = cracked?.phrase,
        flash = flash,
        permanentlyDenied = permanentlyDenied,
        onRetryPermission = { launcher.launch(Manifest.permission.RECORD_AUDIO) },
        onOpenSettings = {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)),
            )
        },
        onSimulateHeadingChange = viewModel::setSimulateHeading,
        onHoldToChantChange = viewModel::setHoldToChant,
        onAbort = onAbort,
        modifier = modifier,
    )
}

@Composable
private fun RitualContent(
    state: RitualUiState,
    flare: Int?,
    cracked: Int?,
    flash: SigilFlash?,
    permanentlyDenied: Boolean,
    onRetryPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    onSimulateHeadingChange: (Boolean) -> Unit,
    onHoldToChantChange: (Boolean) -> Unit,
    onAbort: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val demon = state.demon
    val ritual = state.ritual
    val accent = MaterialTheme.demonColors.accent(demon.accent)
    val colors = MaterialTheme.colorScheme
    val spacing = MaterialTheme.spacing
    val reduceMotion = LocalReduceMotion.current
    val inClimax = ritual.phase == RitualPhase.Climax || ritual.phase == RitualPhase.Done
    var confirmAbort by rememberSaveable { mutableStateOf(false) }
    // With a seal held, leaving asks first; the ritual keeps ticking behind the question (§3).
    val requestClose = { if (ritual.phrase >= 1 && !inClimax) confirmAbort = true else onAbort() }
    BackHandler(enabled = inClimax) {}
    BackHandler(enabled = !inClimax) { requestClose() }
    val chromeAlpha = 1f - segment(ritual.climaxFraction, 0f, 0.3f)
    val ringProgress by animateFloatAsState(
        if (ritual.phase in SEALED_PHASES) 1f else 0f,
        tween(RING_ANIM_MS),
        label = "sigilRing",
    )
    val wavering = ritual.phase == RitualPhase.Wavering
    val heartbeat = if (wavering && !reduceMotion) rememberHeartbeat() else null

    BoxWithConstraints(modifier.fillMaxSize().background(colors.background)) {
        val sigilSide = minOf(maxWidth * SIGIL_WIDTH_FRACTION, maxHeight * SIGIL_MAX_HEIGHT_FRACTION)
        ParticleField(accent.copy(alpha = EMBER_ALPHA), ParticleStyle.Embers, Modifier.fillMaxSize())
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(spacing.lg)
                .graphicsLayer { alpha = chromeAlpha },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    demon.name,
                    style = MaterialTheme.textRoles.name.copy(fontSize = NameSize),
                    color = accent,
                    modifier = Modifier.weight(1f),
                )
                if (!inClimax) {
                    IconButton(onClick = requestClose) {
                        Icon(
                            painter = painterResource(R.drawable.ic_close),
                            contentDescription = stringResource(R.string.action_abort),
                            tint = colors.onSurfaceVariant,
                        )
                    }
                }
            }

            val cue = ritualCue(state, cracked)
            CueChip(cueText(cue, demon), cue.kind.tone)

            Box(Modifier.size(sigilSide), contentAlignment = Alignment.Center) {
                AltarRing(
                    azimuth = state.azimuth,
                    target = demon.ritualHeading,
                    phase = ritual.phase,
                    lockFraction = ritual.lockMs.toFloat() / LOCK_HOLD_MS,
                    accent = accent,
                    modifier = Modifier.fillMaxSize(),
                )
                InkedSigil(
                    sigil = demon.sigil,
                    inkProgress = ritual.inkProgress,
                    accent = accent,
                    rimText = demon.name.uppercase(),
                    modifier = Modifier.fillMaxSize(SIGIL_IN_RING_FRACTION).graphicsLayer {
                        if (wavering) alpha = heartbeat?.value ?: WAVER_MIN_ALPHA
                    },
                    ringProgress = ringProgress,
                    micLevel = state.micLevel,
                    ghostAlpha = GHOST_ALPHA,
                    flash = flash,
                )
            }

            when {
                !inClimax && state.micGranted == false -> ErrorPanel(stringResource(R.string.mic_rationale)) {
                    PrimaryActionButton(
                        text = stringResource(if (permanentlyDenied) R.string.action_open_settings else R.string.action_retry),
                        onClick = if (permanentlyDenied) onOpenSettings else onRetryPermission,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                !inClimax && state.micGranted == true && state.micUnavailable -> ErrorPanel(stringResource(R.string.mic_unavailable))
                !inClimax && state.headingUnavailable && !state.simulateHeading -> ErrorPanel(stringResource(R.string.heading_unavailable))
                else -> {
                    val header = chantHeader(ritual)
                    ChantTablet(
                        phrases = demon.chantPhrases,
                        phrase = ritual.phrase.coerceAtMost(demon.chantPhrases.size),
                        lit = ritual.lit,
                        accent = accent,
                        active = ritual.phase != RitualPhase.Seeking && ritual.phase != RitualPhase.Locking,
                        header = chantHeaderText(header),
                        headerColor = when (header) {
                            ChantHeader.Drifting -> colors.error
                            is ChantHeader.Seconds -> accent
                            is ChantHeader.SealedCount -> colors.outline
                        },
                        focusFraction = if (ritual.clockActive) ritual.focusFraction else null,
                        focusTone = when {
                            wavering -> FocusTone.Drift
                            ritual.courtMultiplier > 1f -> FocusTone.Court
                            else -> FocusTone.Normal
                        },
                        flare = flare,
                        cracked = cracked,
                        micLevel = state.micLevel,
                        wavering = wavering,
                        modifier = Modifier.fillMaxWidth().weight(1f, fill = false).verticalScroll(rememberScrollState()),
                    )
                }
            }

            if (BuildConfig.DEBUG && !inClimax) {
                DebugControls(
                    simulateHeading = state.simulateHeading,
                    onSimulateHeadingChange = onSimulateHeadingChange,
                    onHoldToChantChange = onHoldToChantChange,
                )
            }
        }
        if (inClimax) ClimaxOverlay(demon, accent, ritual.climaxFraction, reduceMotion)
    }

    if (confirmAbort && !inClimax) {
        AlertDialog(
            onDismissRequest = { confirmAbort = false },
            title = { Text(stringResource(R.string.abandon_title), style = MaterialTheme.typography.headlineMedium) },
            text = { Text(stringResource(R.string.abandon_body), style = MaterialTheme.textRoles.body) },
            confirmButton = {
                PrimaryActionButton(stringResource(R.string.action_stay), onClick = { confirmAbort = false })
            },
            dismissButton = {
                OutlinedButton(onClick = {
                    confirmAbort = false
                    onAbort()
                }) { Text(stringResource(R.string.action_abandon)) }
            },
            containerColor = colors.surfaceContainer,
        )
    }
}

@Composable
private fun rememberHeartbeat(): State<Float> = rememberInfiniteTransition(label = "heartbeat").animateFloat(
    1f, WAVER_MIN_ALPHA, infiniteRepeatable(tween(HEARTBEAT_HALF_MS), RepeatMode.Reverse), label = "heartbeatAlpha",
)

@Composable
private fun cueText(cue: RitualCue, demon: Demon): String {
    val direction = headingLabel(demon.ritualHeading)
    return when (cue.kind) {
        CueKind.MicDenied -> stringResource(R.string.cue_mic_denied)
        CueKind.MicUnavailable -> stringResource(R.string.cue_mic_unavailable)
        CueKind.NoHeading -> stringResource(R.string.cue_no_heading)
        CueKind.Cracked -> stringResource(R.string.cue_cracked, ROMAN_NUMERALS[cue.crackedPhrase ?: 0])
        CueKind.TurnLeft -> stringResource(R.string.hint_turn_left, direction)
        CueKind.TurnRight -> stringResource(R.string.hint_turn_right, direction)
        CueKind.Face -> stringResource(R.string.hint_face, direction)
        CueKind.HoldSteady -> stringResource(R.string.hint_hold_steady)
        CueKind.Chant -> stringResource(R.string.cue_chant)
        CueKind.Wavering -> stringResource(R.string.cue_wavering, direction)
        CueKind.WaveringFocus -> stringResource(R.string.cue_wavering_focus, direction)
        CueKind.Opening -> stringResource(R.string.hint_summoning)
    }
}

@Composable
private fun chantHeaderText(header: ChantHeader): String = when (header) {
    is ChantHeader.SealedCount -> stringResource(
        R.string.chant_sealed_count,
        if (header.count == 0) "0" else ROMAN_NUMERALS[header.count - 1],
    )
    is ChantHeader.Seconds -> stringResource(R.string.chant_seconds, header.seconds)
    ChantHeader.Drifting -> stringResource(R.string.chant_drift)
}

@Composable
private fun ErrorPanel(message: String, action: (@Composable () -> Unit)? = null) {
    val colors = MaterialTheme.colorScheme
    Surface(shape = MaterialTheme.shapes.medium, color = colors.errorContainer, contentColor = colors.onErrorContainer) {
        Column(Modifier.padding(MaterialTheme.spacing.lg), verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md)) {
            Text(message, style = MaterialTheme.typography.bodyMedium)
            action?.invoke()
        }
    }
}

/** Climax staging by [fraction]: dim, converge, flash, then the demon rises out of the sigil. */
@Composable
private fun ClimaxOverlay(demon: Demon, accent: Color, fraction: Float, reduceMotion: Boolean) {
    val colors = MaterialTheme.colorScheme
    val dim = segment(fraction, 0f, 0.3f)
    val converge = segment(fraction, 0f, 0.6f)
    val flash = if (reduceMotion) 0f else 1f - (abs(fraction - FLASH_PEAK) / FLASH_HALF_WIDTH).coerceIn(0f, 1f)
    val rise = if (reduceMotion) fraction else segment(fraction, 0.7f, 1f)
    val riseDistance = with(LocalDensity.current) { RiseDistance.toPx() }
    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) {} // swallow touches so nothing behind the overlay is tappable
            .background(colors.surfaceContainerLowest.copy(alpha = CLIMAX_SCRIM_ALPHA * dim)),
        contentAlignment = Alignment.Center,
    ) {
        ParticleField(accent, ParticleStyle.Embers, Modifier.fillMaxSize(), count = CLIMAX_PARTICLES, converge = converge, seed = 29)
        if (flash > 0f) {
            Canvas(Modifier.fillMaxSize()) {
                drawRect(
                    Brush.radialGradient(
                        listOf(colors.onBackground.copy(alpha = flash), accent.copy(alpha = flash * FLASH_ACCENT_ALPHA), Color.Transparent),
                    ),
                )
            }
        }
        SharedDemonArt(
            demonId = demon.id,
            modifier = Modifier
                .fillMaxWidth(CUTOUT_WIDTH_FRACTION)
                .aspectRatio(CUTOUT_ASPECT)
                .graphicsLayer {
                    alpha = rise
                    // Reduced motion: the cut-out only fades in.
                    val scale = if (reduceMotion) 1f else lerp(RISE_START_SCALE, 1f, rise)
                    scaleX = scale
                    scaleY = scale
                    translationY = if (reduceMotion) 0f else (1f - rise) * riseDistance
                },
        ) {
            DemonCutout(demon.cutoutArt, demon.name, accent, Modifier.fillMaxSize(), silhouette = 1f - rise)
        }
    }
}
```

Both `lerp` imports are needed: `androidx.compose.ui.graphics.lerp` (colors) and
`androidx.compose.ui.util.lerp` (floats); Kotlin resolves them by argument type.

- [ ] **Step 2: Delete the compass arc**

```bash
git rm -q app/src/main/java/com/summondemons/safely/ui/components/CompassArc.kt
```

- [ ] **Step 3: Build, test and check on a device**

Run: `./gradlew testDebugUnitTest assembleDebug installDebug`
Expected: `BUILD SUCCESSFUL`. Debug build: Simulate heading + Hold to Chant seals
phrase I ("I / IV", medallion springs in, sigil group flares); phrase II shows the focus
bar and "⏱ n s"; untoggling Simulate heading wavers (red ring, pulsing sigil, "⏱ 3×",
cue "Face East! · Focus slipping"); letting the clock run out cracks (red medallion,
"The seal cracked · Chant I again", boom, crack haptic); ✕ after a seal asks "Abandon
the ritual?"; a full run climaxes, returns to the Grimoire with the celebration, and
**Awesome!** after a flawless run opens the Secret page.

- [ ] **Step 4: Commit**

```bash
git add -A app/src/main
git commit -q -m "feat(ritual): Cue chip, altar ring, chant tablet, seal flares and the abandon dialog" -m "$TRAILER"
```

### Task 26: Debug sheet

**Files:**
- Create: `MAIN/ui/screens/DebugSheet.kt`
- Modify: `MAIN/navigation/AppNavHost.kt`

- [ ] **Step 1: Write the sheet**

`app/src/main/java/com/summondemons/safely/ui/screens/DebugSheet.kt`:

```kotlin
package com.summondemons.safely.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.summondemons.safely.R
import com.summondemons.safely.data.Demon
import com.summondemons.safely.progress.DemonRecord
import com.summondemons.safely.progress.ProgressRules
import com.summondemons.safely.time.CourtOverride
import com.summondemons.safely.ui.GameUiState
import com.summondemons.safely.ui.theme.spacing
import com.summondemons.safely.ui.theme.textRoles
import com.summondemons.safely.ui.theme.upper

/** Debug builds only (§7): move time, override court, bind / unsummon, reset. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugSheet(
    state: GameUiState,
    court: CourtOverride,
    demons: List<Demon>,
    onDismiss: () -> Unit,
    onAdvanceDay: () -> Unit,
    onResetDaily: () -> Unit,
    onCourt: (CourtOverride) -> Unit,
    onBind: (id: String, flawless: Boolean) -> Unit,
    onUnsummon: (String) -> Unit,
    onResetAll: () -> Unit,
) {
    val spacing = MaterialTheme.spacing
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = spacing.lg, vertical = spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            Text(stringResource(R.string.debug_chip), style = MaterialTheme.typography.headlineMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                OutlinedButton(onClick = onAdvanceDay) { Text(stringResource(R.string.debug_advance_day)) }
                OutlinedButton(onClick = onResetDaily) { Text(stringResource(R.string.debug_reset_daily)) }
            }
            Text(stringResource(R.string.debug_court).upper(), style = MaterialTheme.textRoles.meta)
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                CourtOverride.entries.forEach { option ->
                    FilterChip(selected = court == option, onClick = { onCourt(option) }, label = { Text(courtLabel(option)) })
                }
            }
            demons.forEach { demon ->
                Column {
                    Text(demon.name, style = MaterialTheme.typography.titleLarge)
                    Text(status(state.record(demon.id), state.today), style = MaterialTheme.typography.bodySmall)
                    Row {
                        TextButton(onClick = { onBind(demon.id, false) }) { Text(stringResource(R.string.debug_bind)) }
                        TextButton(onClick = { onBind(demon.id, true) }) { Text(stringResource(R.string.debug_bind_flawless)) }
                        TextButton(onClick = { onUnsummon(demon.id) }) { Text(stringResource(R.string.debug_unsummon)) }
                    }
                }
            }
            OutlinedButton(onClick = onResetAll) { Text(stringResource(R.string.debug_reset_all)) }
            Text(
                stringResource(R.string.debug_tester_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

@Composable
private fun courtLabel(option: CourtOverride): String = when (option) {
    CourtOverride.Auto -> stringResource(R.string.debug_court_auto)
    CourtOverride.In -> stringResource(R.string.debug_court_in)
    CourtOverride.Out -> stringResource(R.string.debug_court_out)
}

/** "sealed" / "bound · {d}d since visit · restless · flawless" / "escaped (lore kept)". */
@Composable
private fun status(record: DemonRecord, today: Long): String {
    val bound = stringResource(R.string.debug_status_bound, ProgressRules.daysSinceVisit(record, today).toInt())
    val restless = stringResource(R.string.debug_status_restless)
    val flawless = stringResource(R.string.debug_status_flawless)
    val escaped = stringResource(R.string.debug_status_escaped)
    val sealed = stringResource(R.string.debug_status_sealed)
    return when {
        record.bound -> listOfNotNull(
            bound,
            restless.takeIf { ProgressRules.isRestless(record, today) },
            flawless.takeIf { record.flawless },
        ).joinToString(" · ")
        record.revealed -> escaped
        else -> sealed
    }
}
```

- [ ] **Step 2: Wire it into `AppNavHost`**

In `app/src/main/java/com/summondemons/safely/navigation/AppNavHost.kt`:
- add imports `com.summondemons.safely.BuildConfig`,
  `androidx.compose.runtime.mutableStateOf`, `androidx.compose.runtime.setValue` and
  `com.summondemons.safely.ui.screens.DebugSheet`;
- replace `val onDebug: (() -> Unit)? = null // Task 26` with:

```kotlin
    var debugOpen by rememberSaveable { mutableStateOf(false) }
    val debugSettings by graph.debugSettings.settings.collectAsStateWithLifecycle()
    val onDebug: (() -> Unit)? = if (BuildConfig.DEBUG) ({ debugOpen = true }) else null
```

- after the closing brace of `SharedTransitionLayout { … }` (still inside
  `CompositionLocalProvider(LocalGameSounds …)`), add:

```kotlin
        if (BuildConfig.DEBUG && debugOpen) {
            DebugSheet(
                state = state,
                court = debugSettings.court,
                demons = DemonCatalog.all,
                onDismiss = { debugOpen = false },
                onAdvanceDay = game::advanceDay,
                onResetDaily = game::resetDaily,
                onCourt = game::setCourt,
                onBind = game::bind,
                onUnsummon = game::unsummon,
                onResetAll = {
                    game.resetAll()
                    debugOpen = false
                    navController.popBackStack<Home>(inclusive = false)
                },
            )
        }
```

- [ ] **Step 3: Build and check on a device**

Run: `./gradlew assembleDebug installDebug`
Expected: the "Debug" chip on Home and Grimoire opens the sheet; Advance +1 day twice
from Home makes a bound demon restless (Home hops more, lock glows); a third escapes
(banner on the next Home); Court in/out changes the Home chip at once; Reset all returns
to a fresh Home.

- [ ] **Step 4: Commit**

```bash
git add app/src/main
git commit -q -m "feat(debug): Debug sheet: day offset, court override, bind/unsummon, reset" -m "$TRAILER"
```

- [ ] **Step 5: Phase gate**

Run the verification gate. Expected: `BUILD SUCCESSFUL`.

---

## Phase 6 — Cleanup and verification

### Task 27: Remove retired code and copy; canon catalog check

**Files:**
- Modify: `RES/values/strings.xml`, any file the checks below flag

- [ ] **Step 1: Remove the retired strings**

Delete these entries from `app/src/main/res/values/strings.xml` (each is unused after
Phase 5): `grimoire_title`, `action_summon`, `action_view`, `profile_banner`,
`action_back`, `grimoire_sealed_hint`, `hint_chant`, `hint_mic_needed`,
`hint_face_again`, `action_unsummon`.

Run: `./gradlew lintDebug` and check the report for `UnusedResources`; remove any other
string it lists that this pass made unused (and nothing else).

- [ ] **Step 2: Static checks**

```bash
grep -rn "ProfileScreen\|Profile(\|DemonViewModel\|TadaPlayer\|CompassArc\|CompassMode\|ChantStack\|graceFraction\|phaseMs\|PhraseLost\|PhraseComplete\|DriftWarning\|DRIFT_GRACE" app/src || echo "retired names clean"
grep -rn "Color(0x" app/src/main/java | grep -v "/ui/theme/Color.kt" || echo "colors clean"
grep -rn "when (.*\.id)" app/src/main/java/com/summondemons/safely/ui || echo "no id branching"
grep -rln "Profile" app/src/main/java || echo "no Profile mentions"
git status --short reference/ | grep . && echo "REFERENCE TOUCHED" || echo "reference untouched"
```

Expected: `retired names clean`, `colors clean`, `no id branching`, `no Profile
mentions`, `reference untouched`. Fix anything else the greps print (a KDoc that still
mentions Profile counts).

- [ ] **Step 3: Canon catalog check**

Compare the code with the canon, line by line:
- PRODUCT.md "Demons", "Houses and Callings", "Dossiers" ⇄ `DemonCatalog`, `House`, `Calling`
  (every string identical, typographic apostrophes included).
- DESIGN.md frontmatter colors ⇄ `ui/theme/Color.kt` (each new token present once).
- DESIGN.md "Components (contracts)" ⇄ `ui/components/*` (every listed component exists
  with the listed inputs; nothing undocumented ships).
- ADR-0010 / ADR-0012 / ADR-0015 numbers ⇄ `RitualConfig`, `ProgressConfig`, `House.court`.

Fix the lower source wherever they differ (AGENTS.md: the higher source wins).

- [ ] **Step 4: Full verification**

Run: `./gradlew clean testDebugUnitTest lint assembleDebug assembleRelease`
Expected: `BUILD SUCCESSFUL`. Test classes: `DemonCatalogTest` 8, `ProgressJsonTest` 5,
`ProgressRulesTest` 15, `CourtHoursTest` 3, `RitualEngineTest` 26, `RitualStateTest` 6,
`RitualViewModelTest` 16, `GameClockTest` 2, `DataStoreProgressStoreTest` 5,
`GameViewModelTest` 12, `RitualCueTest` 5, plus the untouched suites (`HeadingMathTest`
+1, `FxMathTest` +1, `LatinSyllablesTest`, `VoiceAnalyzerTest`, `MicLevelTest`,
`RmsTest`, `RotationAzimuthTest`, `SigilPathsTest`). `DemonViewModelTest` is gone.

- [ ] **Step 5: Commit**

```bash
git add -A app/src
git commit -q -m "chore: Remove retired copy and code; canon catalog check" -m "$TRAILER"
```

### Task 28: Device checklist

Run each check on a device or emulator (`./gradlew installDebug`) and note the result in
the PR / hand-over. Use the debug sheet to move time.

1. **First launch:** splash, then Home with "Tap the grimoire"; tapping it opens the
   Grimoire on Asmodeus; the hint is gone next time.
2. **Summon with a crack:** face East (or Simulate heading), chant, let phrase II's clock
   run out → crack; finish → celebration; **Awesome!** stays on page 1; the Secret page
   says "Summon flawlessly to reveal".
3. **Flawless summon** (Reset daily summon, unsummon, repeat without a crack) → **Awesome!**
   opens the Secret page, readable.
4. **Cooling:** after a summon, the other demon shows "Circle cooling · Next summon in
   {h} h"; tapping it shakes.
5. **Restless and escape:** from Home, Advance +1 day twice → restless (Grimoire quote,
   jitter, avatar jitter, Home hops and lock glow); once more → "Seal broken!" banner;
   the demon is an unbound silhouette with readable lore.
6. **Kill during the climax** (swipe the app away during the 2.5 s): relaunch → the demon
   is bound, no celebration, no second summon possible; a restored Ritual pops back.
7. **Background mid-chant** on phrase II and return after 10 s: no drift, no crack, the
   focus clock resumed where it was, the bed resumes (ducked if you were wavering).
8. **App open across midnight** on the Grimoire (or set the device clock): the cooling
   cue clears without a restart.
9. **Court:** in court the phrase II–IV focus bars shimmer gold and budgets are 25 %
   longer; Court out → plain bars.
10. **Badges:** each badge swaps the band text; it returns after 5 s, on a second tap, on a
    tap elsewhere and when switching demon; a tap on the demon both clears the badge and
    summons.
11. **Carousel** on both demons: wraps both ways, swipe works, long pages scroll inside.
12. **Fixed layout:** card and art keep the same size across demons, bound/sealed,
    cooling/ready and the celebration.
13. **Font scale 1.3× and 2.0×** on a small API 24 screen: everything readable; at 2.0×
    the card may grow, nothing overlaps; the app runs on API 24 (desugared `java.time`).
14. **Reduced motion** (Developer options → Animator duration scale off): no particles,
    bob, jitter, pulses, sunburst rotation, dust puff or flash; grimoire opening and
    burn-away fade; ink-in, karaoke and the focus bar still update.
15. **API 33+ device:** AGSL shimmer still fine; system back with the predictive gesture
    behaves (Grimoire → Home; celebration → dismiss; ritual after a seal → dialog).
16. **Backup:** `adb shell bmgr backupnow com.summondemons.safely` succeeds and the
    `debug_settings` file is not in the backup set.
17. `./gradlew assembleRelease` builds; the release APK has no Debug chip and ignores any
    debug settings.

---

## Spec coverage

| Spec section | Task |
|---|---|
| Decisions table, canon updates (§12), ADR-0010…0016, ADR-0004/0006/0008 amendments | 1, 2 |
| §1 Progress data, rules, evaluate-before-visit, Court Hours, config | 4 (config), 5, 6, 7, 12 |
| §2 Ritual: input, state, focus clock, off heading, effects, config | 8 |
| §2 Ticking and lifecycle: active, heading hold, commit in tick, bed follows phase, dialog doesn't pause | 11, 13, 25 |
| §3 Navigation table, Ritual guard, GameViewModel, GameClock, debug re-emits | 9, 12, 23, 24, 25, 26 |
| §4 Text roles | 14 |
| §4 Home | 22, 23 |
| §4 Grimoire: background, avatar rail, stage, quote slot, swipe, celebration, visit, card, badges, carousel, ink-in | 19, 20, 21, 22, 24 |
| §4 Ritual: cue chip, altar ring, sigil flashes, chant tablet, error panel, bed | 16, 17, 18, 25 |
| §4 Motion safety | 14 (jitter), 17, 20, 21, 22, 23, 24, 25 |
| §5 Feedback mapping, crack haptic, volume, GameSounds | 13, 23, 24 |
| §6 Persistence, single source of truth, splash, debug settings file, backup excludes | 9, 10, 12 |
| §7 Debug aids | 9, 12, 26 |
| §8 Demon data | 4 |
| §9 Architecture, dependencies, desugaring, removals | 3, 8, 12, 13, 24, 25, 27 |
| §10 Error handling | 5, 10, 11, 12, 24 |
| §11 Testing (JVM suites, device checklist) | 4–12, 14–16, 27, 28 |
| Gaps (placeholders) | 2 (DESIGN.md Gaps), 22 |
| Phases | Phase 1–6 above |

