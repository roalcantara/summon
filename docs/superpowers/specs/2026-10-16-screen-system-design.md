# Screen system + progress — design

- Date: 2026-10-16
- Status: Agreed in prototype review; independent review applied (web prototype, branch `claude/cool-rubin-pylwkn` of
  `roalcantara/mydemon`); written-spec review applied
- Builds on: `PRODUCT.md`, `DESIGN.md`, ADR-0001…0009, `2026-10-15-ritual-2-design.md`
- Changes canon: PRODUCT.md (Core loop, Grimoire, Lock state, Heading gate, Chant,
  Success, Profile rows removed, Debug aids, Non-goals, Demons table), DESIGN.md
  (house tokens, text roles, components, asset gaps); new ADR-0010…0016; ADR-0008
  partly superseded (drift grace → focus clock).
- Reference implementation: the web prototype in `roalcantara/mydemon`
  (`src/engine/*` mirror the Kotlin rules below; `src/screens/*` show every screen
  state; its debug panel reaches all of them). This amends ADR-0004 for this pass
  (§12). Prototype-only leftovers that are **not** ported: `visitHintSeen`,
  `tab` / `sheetOpen`, `FlawlessSeal`, `effort()` / `difficulty()`, `stats`, the
  jump-to-state and freeze-clock debug tools.

## Goal

Turn the three-screen MVP into a small game with a reason to come back: one summon
per day, demons that get restless and escape when neglected, a home screen, and a
single demon card that replaces the Profile screen. The ritual gets a real failure
mode (a draining focus clock that cracks seals) without ever wiping a whole attempt.

Two demons, still. No new art is required to ship; missing art has vector or
gradient placeholders (see "Gaps").

## Decisions taken in prototype review

| Topic | Decision |
|---|---|
| Screens | **Home → Grimoire → Ritual**. Profile is removed: the Grimoire card shows everything, and the new-summon celebration plays on the Grimoire stage. |
| Ritual pressure | A **focus clock** per phrase (II–IV) replaces the 1.5 s drift grace. Running out **cracks** the last seal (one phrase back). Locking happens once per attempt. |
| Failure | No reset-to-zero ever. Worst case is losing one sealed phrase at a time. |
| Flawless | A summon with no crack is **flawless**; it unlocks the demon's Secret. |
| Daily summon | **One summon per local calendar day** across all demons. |
| Neglect | Not seeing a bound demon for 2 days makes him **restless**; 3 days and he **escapes** (unbound again, lore kept). |
| Visit | **Viewing** a bound demon in the Grimoire is the visit. Tapping a bound demon does nothing. |
| Court Hours | Each demon belongs to a House with court hours; summoning **in court** gives +25 % focus. |
| Persistence | Progress survives restarts (Preferences DataStore, JSON). Saved at **ClimaxStart**, so killing the app mid-climax keeps the summon. |
| Demon card | Static panel (no expand/collapse), fixed height in every state; description band, four badges, dossier carousel. |
| Summon CTA | No Summon button. Tapping the sealed demon (or the "Tap to summon" cue under him) starts the ritual. |
| Stats | Dropped (PRODUCT non-goal stays). |

## 1. Progress rules

All rules are pure Kotlin in a new `progress` package and take `today` as an argument
(a local epoch day, `LocalDate.now(zone).toEpochDay()`), so tests and the debug clock
can move time. `java.time` needs core library desugaring on API 24–25 (§9).

### Data

```
DemonRecord(
  bound: Boolean = false,          // summoned and not escaped
  revealed: Boolean = false,       // lore unlocked (stays true after an escape)
  flawless: Boolean = false,       // ever summoned flawlessly (sticky)
  lastVisitDay: Long? = null,
  revealPending: RevealPending = RevealPending(),   // one-time effects still to play
  escapeBannerPending: Boolean = false,
)
RevealPending(avatar: Boolean = false, codex: Boolean = false, battle: Boolean = false)

Progress(
  demons: Map<String, DemonRecord> = emptyMap(),    // missing id = DemonRecord()
  lastSummonDay: Long? = null,
  homeHintSeen: Boolean = false,
  lastViewed: String? = null,
)
```

`codex` / `battle` keep the prototype's names: they are the two halves of the dossier
that ink in once after a summon (see §4 Carousel).

### Rules (`ProgressRules`)

| Function | Rule |
|---|---|
| `canSummon(p, today)` | `p.lastSummonDay == null || today > p.lastSummonDay`. A clock moved backwards never unlocks early. (One summon per day is this rule, not a tunable.) |
| `commitSummon(p, id, day, flawless)` | `day` is the day the ritual **started** (captured when the Ritual opens), so a ritual begun at 23:59 that climaxes after midnight spends that day's summon, not tomorrow's. |
| ↳ fields | `bound = true`, `revealed = true`, `flawless = old.flawless || flawless`, `lastVisitDay = day`, all `revealPending = true`, `escapeBannerPending = false`; `lastSummonDay = max(day, old ?: day)`; `lastViewed = id`. |
| `visit(p, id, today)` | Bound only: `lastVisitDay = today`, `lastViewed = id`. Unbound: unchanged. |
| `daysSinceVisit(r, today)` | Bound with a visit: `max(0, today - lastVisitDay)`; else 0. |
| `isRestless(r, today)` | `daysSinceVisit >= RESTLESS_AFTER_DAYS (2)`. |
| `evaluate(p, today)` | For every bound record with `daysSinceVisit >= ESCAPE_AFTER_DAYS (3)`: `bound = false`, `lastVisitDay = null`, `escapeBannerPending = true`, `revealPending` all false. `revealed` and `flawless` are kept ("lore kept"). |
| `consumeReveal(p, id, what)` | Clears one `revealPending` flag. |
| `dismissEscapeBanners(p)` | Clears every `escapeBannerPending`. |
| `unsummon(p, id)` | Debug: replaces the record with `DemonRecord()`. |
| `focusDemon(p, ids, today)` | Where Home opens the Grimoire: the first restless demon; else, if `canSummon`, the first unbound one; else `lastViewed` if still valid; else the first id. |

`evaluate` runs after the first progress load and on every `ON_START` of the activity
(`LifecycleEventEffect(ON_START)` in `AppNavHost`, calling `GameViewModel.evaluate()`).
It only flips flags; screens show the result on their next frame.

**Evaluate before visit.** `GameViewModel.visit` applies `evaluate` first, in the same
`update`: `visit(evaluate(p, today), id, today)`. Otherwise a Grimoire restored on a
bound demon after 3+ days in the background could visit him (from `ticks`) before the
`ON_START` evaluate runs, and he would never escape. With this rule the two can run in
either order.

**Two different "sealed" states.** *Unbound* (`!bound`) drives the stage: silhouette,
summon cue, avatar sigil. *Unrevealed* (`!revealed`) drives the dossier: smudged
pages. A demon that escaped is unbound but revealed: a silhouette whose lore is still
readable.

### Court Hours

`House` = `Sol` | `Venus`. Local hours, fixed sunrise 06:00 and sunset 18:00:

| House | Planet (badge label) | Court | Metal token |
|---|---|---|---|
| Sol | Sol | 09–12 and 15–18 | `house-sol` |
| Venus | Venus | 06–12 | `house-venus` |

A `House` carries only what is shown: planet, metal token, court ranges and the badge
tooltip. (The prototype's House name, metal name, rank word and court text are unused.)

`inCourt(house, hour)` is true when `hour` falls in a range (start inclusive, end
exclusive). Venus' "clear weather only" is flavour text; weather is not read.

### Config (`ProgressConfig`)

`RESTLESS_AFTER_DAYS = 2`, `ESCAPE_AFTER_DAYS = 3`, `SUNRISE_HOUR = 6`,
`SUNSET_HOUR = 18`.

## 2. Ritual rules (changes to Ritual 2.0)

Unchanged from ADR-0008: 20 ms ticks, Seeking → Locking (600 ms within ±20°) →
Chanting, syllable lighting (onset ≥ 120 ms apart or 350 ms of voicing), proximity
ticks, 2.5 s Climax, Done. `VoiceAnalyzer`, `LatinSyllables` and heading math are
unchanged.

### Input

`RitualInput(azimuth: Float?, voice: VoiceFrame, inCourt: Boolean)`. `inCourt` is read
from the demon's House every tick but only **used at lock**.

### State (new / changed fields)

| Field | Meaning |
|---|---|
| `lockMs` | Replaces `phaseMs` for Locking. |
| `climaxMs` | Replaces `phaseMs` for Climax. |
| `focusMs`, `focusBudgetMs` | Focus left / full budget for the current phrase. 0 on phrase I. |
| `courtMultiplier` | 1.0, or `1 + COURT_BONUS` (1.25) if the House was in court at the moment of lock. Fixed for the attempt. |
| `flawless` | True until the first crack of this attempt. |

Derived: `focusFraction = focusMs / focusBudgetMs` (0 when no budget);
`clockActive = phrase in 1..3 && phase in {Chanting, Wavering}`; `inkProgress` and
`climaxFraction` as before. `graceFraction` is removed.

### Focus clock

- **Budget** for phrase index *i* (0-based): `0` for *i* = 0, else
  `round((FOCUS_BASE_MS + FOCUS_PER_SYLLABLE_MS × syllables[i]) × courtMultiplier)`.
  `FOCUS_BASE_MS = 3 000`, `FOCUS_PER_SYLLABLE_MS = 500`. Examples: 18 syllables →
  12 000 ms; 6 syllables in court → 7 500 ms.
- **Seal** (all syllables of a phrase lit): effect `Sealed(phrase)`; the next phrase
  starts with a full budget. After the 4th seal: `ClimaxStart(flawless)`.
- **Drain** (phrases II–IV only), every tick while Chanting or Wavering:
  1 × `FRAME_MS` on heading (**even in silence**), `FOCUS_DRIFT_MULTIPLIER` (3) ×
  `FRAME_MS` off heading.
- **Crack**: when focus reaches 0, effect `Cracked(phrase - 1)`: the last sealed
  phrase is reopened (`phrase -= 1`, `lit = 0`), it gets its own full budget (0 if it
  is phrase I), `flawless = false`.

### Off heading

Once locked, the ritual **never returns to Seeking**. Leaving tolerance (or a `null`
heading) during Chanting → **Wavering** with effect `Drift`. While Wavering, voice is
ignored and the clock drains 3×. Coming back → Chanting with `Resumed`, nothing lost,
no re-lock. On phrase I (no clock) Wavering simply waits.

### Effects

`ProximityTick`, `Locked`, `SyllableLit`, `Sealed(phrase)`, `Cracked(phrase)`, `Drift`,
`Resumed`, `ClimaxStart(flawless)`, `Summoned`. Renamed: `PhraseComplete` → `Sealed`,
`DriftWarning` → `Drift`. Removed: `PhraseLost`.

### Ticking and the app lifecycle

- **Active only while started.** `RitualScreen` calls `viewModel.setActive(true/false)`
  from `LifecycleEventEffect(ON_START / ON_STOP)`. While inactive the VM sends no ticks
  (the focus clock pauses; the `stateIn` 5 s timeout no longer matters). `uiState`
  exposes `active` so the VM test can assert it.
- **No stale heading after a restart.** On (re)start the VM already clears `azimuth`.
  Ticks are then **held until the first heading reading arrives** (or Simulate heading
  is on), so a return from the background never reads as "off heading" (no `Drift`, no
  3× drain, no crack). Devices with no heading sensor tick with `null` as today.
- **Summon commit is not an effect.** `effects` stays lossy (`tryEmit`, no replay), so
  the commit must not depend on it. `RitualViewModel` takes a
  `commitSummon: (flawless: Boolean) -> Unit` and calls it **inside `tick`**, exactly
  once, on the step that produces `ClimaxStart`, before the climax plays.
- **Feedback resumes with the phase.** The ambient bed follows the phase rather than
  the `Locked`/`Resumed` effects alone: `RitualFeedback` starts the bed on `ON_START`
  if the phase is Chanting (normal volume) or Wavering (ducked volume).
- **The Abandon dialog doesn't pause anything.** The ritual keeps ticking (and the
  clock keeps draining) behind it, as in the prototype; the dialog is a decision, not
  a pause.

### Config additions (`RitualConfig`)

`FOCUS_BASE_MS = 3_000L`, `FOCUS_PER_SYLLABLE_MS = 500L`, `FOCUS_DRIFT_MULTIPLIER = 3`,
`COURT_BONUS = 0.25f`, `FOCUS_LOW_FRACTION = 0.25f`. Removed: `DRIFT_GRACE_MS`.

## 3. Navigation and app state

Routes (type-safe, `navigation/Routes.kt`): `Home` (start), `Grimoire`, `Ritual(demonId)`.
`Profile` is removed.

| From | Action | Result |
|---|---|---|
| Home | Tap the grimoire | Opening animation (700 ms; 150 ms with reduced motion), `chime_4`, then Grimoire on `focusDemon(...)`; sets `homeHintSeen`. |
| Grimoire | System back **during the celebration** | Dismisses the celebration (same as **Awesome!**); stays on Grimoire. |
| Grimoire | ← or system back | Home (the book "slams" shut: a dust puff). |
| Grimoire | Tap sealed demon or "Tap to summon", summon available | Ritual(demonId). |
| Grimoire | Tap sealed demon or cue while cooling | The cooling cue shakes; nothing else. |
| Ritual | ✕ or back, no phrase sealed (`phrase == 0`, also after a crack back to phrase I) | Grimoire. |
| Ritual | ✕ or back, at least one phrase sealed (`phrase >= 1`), not in climax | Dialog "Abandon the ritual? / Your seals will fade." **Abandon** → Grimoire; **Stay** closes. |
| Ritual | Back during climax | Ignored (as today). |
| Ritual | `Summoned` | Pop to Grimoire with that demon selected and the celebration on. The cut-out stays a shared element (`demon-art-{id}`). |
| Ritual | Opened (incl. restored after process death) while the demon is bound or `!canSummon`, and not already in Climax/Done | Pop back to Grimoire at once. Prevents a second summon from a restored back stack. |

**`GameViewModel`** (activity-scoped, replaces `DemonViewModel`) owns:
`progress: StateFlow<Progress>`, `selectedId`, `celebration: Celebration?`
(`demonId`, `flawless`), `today`, `canSummonToday`, `inCourt(house)`, and the actions
`openGrimoireFromHome`, `select`, `visit`, `commitSummon`, `celebrate`,
`dismissCelebration`, `consumeReveal`, `dismissEscapeBanners`, plus debug actions (§7).
Every progress change goes through `ProgressStore` (§6). `GameViewModel` is built by
a factory (`GameViewModel.factory(store, clock)`) in `AppNavHost`; `selectedId` is kept
in its `SavedStateHandle` and defaults to `focusDemon(...)`. `celebration` is not
restored after process death (the summon is already saved; only the party is lost).

**Time** comes from an injected `GameClock`: `now(): ZonedDateTime` (with the debug day
offset in debug builds) and `ticks: Flow<ZonedDateTime>`, which emits on collection, at
every top of the hour (this covers midnight), on `ACTION_TIME_CHANGED`,
`ACTION_TIMEZONE_CHANGED` and `ACTION_DATE_CHANGED`, and (debug builds) whenever the day
offset or the court override changes, so the debug sheet's changes show at once. `today`, the current hour,
`canSummonToday`, "Next summon in {h} h", the Home court chip and the visit-on-view
all derive from `ticks`, so they stay correct while the app is open across midnight.

`RitualViewModel` gets `inCourt: () -> Boolean` (the demon's House against
`GameClock`, honouring the debug override) and the `commitSummon` callback (§2) through
its factory.

## 4. Screens

Text roles used below (new in DESIGN.md "Typography"):

| Role | Style (built on the existing DESIGN.md scale) |
|---|---|
| `cue` | `label-medium` + letter spacing 0.08 em, upper-case text |
| `incantation` | new: Cinzel 15 sp Bold, line 22 sp, +0.02 em |
| `meta` | `label-small` + letter spacing 0.14 em, upper-case text, `outline` color |
| `name` | Cinzel Bold (`title` / `headline-*` sizes per use) |
| `body` | `body-medium` |

Compose `TextStyle` has no text-transform: `cue` and `meta` strings are upper-cased at
the call site with `uppercase(Locale.ROOT)` (a small helper in `ui/theme`).

### Home — "The Restless Grimoire"

Back to front: `StageBackground(Home)`; ember `ParticleField`; top: the game logo
(96 dp, circle) and a chip **"In court · Sol & Venus"** (houses currently in court) or
**"Out of office"**; centre: an inlaid `CompassRose` drawn in perspective as a floor,
with the `RestlessGrimoire` on it; bottom: `meta` "{n} of 2 spirits bound" and, until
`homeHintSeen`, a cue chip "Tap the grimoire".

`RestlessGrimoire` (vector placeholder until art exists): a chained, locked book with
an idle bob. It hops every 5–8 s and a silhouette peeks out every 10–15 s; when any
demon is restless it hops every 2–3 s, peeks every 4–6 s, and the lock glows in the
focus demon's accent. Opening: scale up, burst of light, fade.

**Escape banner:** if any `escapeBannerPending`, a `RibbonBanner` "Seal broken!" over a
panel "{Name} slipped his seal while you were away!" (or "Both spirits slipped their
seals while you were away!") and "Tap to dismiss" → `dismissEscapeBanners`.

### Grimoire — stage over a static card

The screen is one column under the avatar rail: **stage** (takes the rest) over the
**demon card** (fixed height). Neither ever changes size between demons or states.

**Background:** `StageBackground(house)` crossfades (500 ms) when the House changes;
dimmer while sealed. Ember particles in the accent (50 during the celebration);
smoke particles while sealed; `RotatingSunburstBackground` during the celebration.

**Avatar rail** (top): ← back button, then one `SigilAvatar` per demon (52 dp selected
with accent ring and glow, 40 dp otherwise). Unbound: the demon's sigil drawn in
accent strokes. Bound: the avatar portrait. Right after a summon the sigil **burns
away** over the portrait once (1.4 s after 0.4 s delay), then `consumeReveal(avatar)`.
A restless bound demon's avatar jitters. Tapping an avatar selects that demon; avatar
taps are ignored while the celebration is on.

**Stage:**
1. A frame with the demon's aspect (896 × 1200). Bound: `sceneArt` dimmed (brightness
   ≈ 0.35, slight blur on API 31+) as a rounded backdrop.
2. `FloorCircle` at his feet (accent ellipse + dashed inner ring); it pulses while the
   demon is sealed and today's summon is available.
3. The cut-out, floating (6 dp bob, 4 s). Sealed: near-black silhouette with accent rim
   glow. Restless: jitter. Tap: sealed → summon (or shake while cooling); bound → nothing;
   celebration → dismiss.
4. **Quote slot**, fixed 72 dp, always filled:

| State | Slot |
|---|---|
| Celebration | `PrimaryActionButton` **Awesome!** |
| Bound | Speech bubble (accent border, `body` 15 SemiBold): `quote`, or `restlessQuote` if he was restless **when this viewing started** |
| Sealed, summon available | Dashed ghost bubble "TAP TO SUMMON" (`cue`, accent), pulsing (1.8 s); tap = summon |
| Sealed, cooling | Dashed dim bubble: "CIRCLE COOLING" / "Next summon in {h} h" (hours to local midnight, at least 1); tap = shake |

Horizontal swipe on the stage (> 60 dp) selects the previous / next demon (wraps);
disabled during the celebration.

**Celebration** (after `Summoned`): `RibbonBanner` "New Demon Received!" drops in at
the top of the stage, `tada` plays once, sunburst + particles, slot shows **Awesome!**.
Dismissing it after a **flawless** summon moves the carousel to the Secret page, now
readable. (The Secret has no ink-in animation, as in the prototype.)

**Visit:** whenever a bound demon is shown (selection or `today` changes), call
`visit(id)`. Restlessness for the quote and jitter is captured when the demon is first
shown, so a neglected demon still sulks for that viewing.

**Demon card** (`surfaceContainerLow` at 95 %, top corners `large`, top border
`outlineVariant`). Height: 50 % of the area below the rail (`CARD_HEIGHT_FRACTION`).
At large font scales, if header + band + badges + the 88 dp carousel minimum don't fit,
the card grows to fit (the stage shrinks); the fixed-layout promise holds at the
default font scale. Top to bottom:

1. **Header:** number medallion (48 dp circle, `name` 18 sp in the House metal, metal
   ring), `name` 26 sp (accent if bound, `onSurfaceVariant` if sealed), title
   (`body` 12, `outline`).
2. **Info band** (`surfaceContainerHigh` 80 %, `small` shape, centred): the
   description in `primary` (`body` 14 SemiBold). While a badge is selected it shows
   that badge instead: a `meta` heading (badge icon + label, accent) over its
   explanation (`body` 14). The band's height is the tallest of **all** demons'
   descriptions and badge texts, so nothing below it moves when switching demons or
   badges (stack every text in one `Box`, only the current one visible and in
   semantics).
3. **Badge row:** four equal tiles (icon 24 dp + `name` 13 sp label; rim in the House
   metal):

| Badge | Icon | Label | Explanation |
|---|---|---|---|
| Direction | `DirectionGlyph` (compass needle + letter) | e.g. "East" | "Face East to summon. He serves Amaymon, King of the East." / "…He swears to no king." |
| House | `HouseGlyph` (☉ / ♀ drawn as paths) | "Sol" / "Venus" | the House tooltip |
| Calling | `CallingGlyph` | "Tutor" / "Oracle" | the Calling tooltip |
| Legions | `LegionsGlyph` (crossed swords) | "{n} legions" | "A {rank} at the head of {n} legions of spirits." |

   Tapping a badge selects it (accent rim + glow, `Role.Tab`-like selected semantics);
   tapping it again, tapping anywhere outside the row, switching demon, or **5 s**
   (`BADGE_INFO_MS`, restarted on every badge tap) returns the description. "Outside"
   is detected by a root `pointerInput` that observes presses on
   `PointerEventPass.Initial` without consuming them; so a tap on the demon both clears
   the badge and does its own action (summon), as in the prototype.

4. **Dossier carousel** (fills the rest, at least 88 dp; 12 dp top padding): ‹ › buttons
   and horizontal swipe (> 40 dp), wrapping; header `meta` page title and "{i} / 6".
   The page body scrolls inside the carousel and never resizes the card. It opens on
   page 1 whenever the demon changes or the Grimoire is re-entered.

| # | Page | Content | Sealed shows | Inks with |
|---|---|---|---|---|
| 1 | Weakness · {name} | weakness line | 2 smudge lines, title "Weakness" | battle |
| 2 | Abilities | name (accent, `name`) + line, ×3 | 3 smudge lines | battle |
| 3 | Offices | bullet list | 3 smudge lines | codex |
| 4 | Appearance | paragraph | 3 smudge lines | codex |
| 5 | Summoner's warning | paragraph | 2 smudge lines | codex |
| 6 | Secret | italic paragraph, **only if `flawless`** | smudge + "Summon flawlessly to reveal" | — |

   "Sealed" here means *unrevealed* (`!revealed`, §1). A smudge line is a rounded bar
   with a diagonal stripe pattern (`surfaceContainerHighest` / `surfaceContainerHigh`).
   After a summon, the **first viewing** of that demon inks in every page opened during
   it (blur → sharp, staggered 120 ms per line, 900 ms). The ink flags are read once
   into `remember(demon.id)` when the demon is first shown, and `consumeReveal(codex)` /
   `consumeReveal(battle)` are called right away, exactly once each; the remembered
   value keeps the animation alive even though the store write lands first.

### Ritual — changes to the altar

- **Cue chip** under the title replaces the phase hint (tone neutral / safe / error;
  the first matching row wins):

| Situation | Cue | Tone |
|---|---|---|
| Mic denied | "The spirits can't hear you" | error |
| Mic unavailable | "The microphone wouldn't start" | error |
| No heading sensor | "This device can't sense direction" | error |
| Just cracked (1.5 s) | "The seal cracked · Chant {numeral} again" | error |
| Seeking | "Turn left · Face {dir}" / "Turn right · Face {dir}" / "Face {dir}" | neutral |
| Locking | "Hold steady…" | safe |
| Chanting | "Locked · Chant aloud" | safe |
| Wavering | "Face {dir}! · Focus slipping" (phrase ≥ II) / "Face {dir}!" | error |
| Climax | "The seal opens…" | safe |

- **Altar ring** (`CompassArc` is rewritten as `AltarRing`; `CompassMode` and the
  grace arc go): outer ring in `outlineVariant`;
  while Locking a `ritual-safe` arc fills clockwise with `lockMs / LOCK_HOLD_MS`; once
  locked the full ring is `ritual-safe`; Wavering `error`. A small triangle on the ring
  points to the target **relative to the player's heading** (`signedDelta`), green when
  within tolerance. The sigil pulses (300 ms heartbeat) while Wavering.
- **Sigil flashes:** on `Sealed(n)` group *n+1* flares in the accent (700 ms); on
  `Cracked(n)` it drains in `error` (1.5 s).
- **Chant tablet** (replaces `ChantStack`; reuses `KaraokeLine` for the current
  phrase): framed panel, header `meta` "Sacred chant" and, on the right, the sealed
  count "{numeral} / IV" ("0 / IV" before the first seal), or "⏱ {s} s" (focus left,
  rounded up) while the clock runs, or "⏱ 3×" (error) while Wavering on a clock. One row per phrase, all in `incantation` at the
  same size: a medallion I–IV (dashed while upcoming; accent ring while current; a
  filled accent coin once sealed, springing in; a red cracked coin on a crack), then the
  text (sealed: accent; current: karaoke with the newest syllable glowing with the mic
  level; upcoming: dim; cracked: error). Under the current phrase while the clock runs:
  a 4 dp **focus bar** (accent; `error` while Wavering; a moving gold shimmer when this
  attempt has the court bonus, i.e. `courtMultiplier > 1`, not the live court state;
  heartbeat below `FOCUS_LOW_FRACTION`).
- **Error panel** copy as today; mic denied keeps "Allow microphone" / "Open settings".
- **Ambient bed** plays while Chanting or Wavering (not while Seeking).

### Motion safety

`LocalReduceMotion` also disables: grimoire bob/hop/peek and opening scale (keeps a
150 ms fade), Home dust puff, cut-out float and jitter, quote-slot pulse, floor-circle
pulse, sunburst rotation, avatar burn-away (cross-fade instead), badge/carousel slide
(cross-fade instead). Ink-in, karaoke and focus bar still update.

## 5. Feedback

`RitualFeedback` mapping:

| Effect | Haptic | Sound |
|---|---|---|
| `ProximityTick` | tick | — |
| `Locked` | clunk | `lock`; bed fades in |
| `SyllableLit` | syllable | — |
| `Sealed(n)` | chime | `chime_{n+1}` |
| `Cracked(n)` | `crack` (new: 80 ms on, 50 ms off, 80 ms on, strong) | `boom` at 35 % |
| `Drift` | warn | bed ducks |
| `Resumed` | — | bed back to normal |
| `ClimaxStart` | crescendo | `riser` |
| `Summoned` | success | `boom` |

Grimoire celebration: `tada` once. Home opening: `chime_4` at 50 %. No new sound files.

Playback: `RitualSounds.play(cue, volume = 1f)` gains a volume. Sounds outside the
Ritual (Home `chime_4`, Grimoire `tada`) go through a new app-scoped `GameSounds`
(one `SoundPool`, created once, released with the activity) that absorbs `TadaPlayer`.

## 6. Persistence

`ProgressStore` interface (`progress: Flow<Progress>`, `suspend fun update((Progress) -> Progress)`),
implemented by `platform/DataStoreProgressStore` over **Preferences DataStore**: one
string key `progress_v1` holding the `Progress` as kotlinx-serialization JSON
(`ignoreUnknownKeys = true`). Unreadable or corrupt data → `Progress()` (log, don't
crash). The DataStore is a **process-wide singleton** (a top-level
`preferencesDataStore("progress")` delegate on `Context`, used through the application
context), never one per ViewModel.

**Single source of truth.** `GameViewModel.progress` is the store's flow (`stateIn`).
Every mutation, `evaluate` included, is a `store.update { rules(it) }`, so a mutation
always applies to the latest stored value and nothing acts on an unloaded `Progress()`.
`evaluate` on `ON_START` is skipped until the first value has loaded. If a write fails,
it is logged and that change is lost (the UI keeps showing the stored state).

**First frame.** `installSplashScreen().setKeepOnScreenCondition { !loaded }` in
`MainActivity` (`androidx.core:core-splashscreen`) holds the splash until the first
progress value is loaded, which covers every destination restored after process death.
This needs a `Theme.SplashScreen` launch theme (with `postSplashScreenTheme` pointing at
today's theme) on the activity, and `installSplashScreen()` before `super.onCreate`.

**Debug settings** (day offset, court override) live in a separate DataStore file
`debug_settings`, read only in debug builds and excluded from backup. The progress file
is backed up (current backup rules already include everything; only the debug file
needs an exclude: `<exclude domain="file" path="datastore/debug_settings.preferences_pb"/>`
in `backup_rules.xml` (API ≤ 30) and in both `<cloud-backup>` and `<device-transfer>`
of `data_extraction_rules.xml` (API 31+)).

## 7. Debug aids (debug builds only)

Ritual keeps **Simulate heading** and **Hold to Chant**. A new debug sheet
(`ModalBottomSheet`, opened from a small "Debug" chip on Home and Grimoire) adds:

| Control | Effect |
|---|---|
| Advance +1 day | `GameClock` day offset +1, then `evaluate` |
| Reset daily summon | `lastSummonDay = null` |
| Court: auto / in / out | overrides `inCourt` for every House |
| Per demon: Bind / Bind flawless / Unsummon | `commitSummon(id, today, false/true)` / `unsummon` |
| Reset all | `Progress()`, offset 0, court auto, back to Home |
| Status line per demon | "sealed" / "bound · {d}d since visit · restless · flawless" / "escaped (lore kept)" |

Note for testers: advancing a day while a bound demon is on the Grimoire visits him at
once (visit-on-view), so test restlessness by advancing from Home.

The prototype's "jump to ritual state" and "freeze clock" stay prototype-only.

## 8. Demon data

`Demon` gains:

| Field | Asmodeus | Astaroth |
|---|---|---|
| `number` | 32 | 29 |
| `rank` | Great King | Mighty Duke |
| `legions` | 72 | 40 |
| `house` | Sol | Venus |
| `liege` | "Amaymon, King of the East" | null |
| `calling` | Tutor | Oracle |
| `ritualHeading` | **90 (East)** | 0 (North) |
| `restlessQuote` | "I've counted every grain of dust in here. Twice. In three bases." | "I foresaw you'd forget me. I hate being right. No I don't." |
| `appearance`, `offices` (list), `warning`, `abilities` (3 × name + line), `weakness` (name + line), `secret` | verbatim from the prototype `src/data/demons.ts` | same |

Descriptions change to the prototype's text: Asmodeus "Teaches arithmetic, astronomy,
geometry, and invisibility. Answers all questions—if you can stand the smugness.";
Astaroth "Knows every secret and every tense. Will explain his own fall at length if
asked." Quotes and chants are unchanged.

`Calling` = Warlord | Tutor | Oracle | Treasurer | Menace (name + tooltip each, from
the prototype); `House` carries planet, metal token, court ranges and tooltip only
(§1). The Legions badge uses the demon's own `rank` ("Great King"), not a House rank
word. Copy lives in `strings.xml` where it is UI chrome, and in `DemonCatalog`
where it is demon data (as today).

## 9. Architecture

Pure Kotlin (JVM-tested):

| Unit | Responsibility |
|---|---|
| `progress/Progress`, `DemonRecord`, `RevealPending` | data (`@Serializable`) |
| `progress/ProgressRules` | §1 functions |
| `progress/CourtHours` | `inCourt(house, hour)` |
| `progress/ProgressConfig` | constants |
| `ritual/RitualState`, `RitualEngine`, `RitualConfig` | §2 changes |
| `data/Demon`, `House`, `Calling`, `DemonCatalog` | §8 |

Android / Compose:

| Unit | Responsibility |
|---|---|
| `time/GameClock` (+ `SystemGameClock`) | local time with debug offset, `ticks` flow (§3) |
| `progress/ProgressStore`, `platform/DataStoreProgressStore` | §6 |
| `ui/GameViewModel` (+ factory) | §3; replaces `DemonViewModel` |
| `ritual/RitualViewModel` | `inCourt` provider, `commitSummon` callback, `setActive`, heading hold after restart (§2) |
| `platform/RitualFeedback`, `RitualSounds`, `Haptics` | renamed effects, `crack` haptic, volume, bed follows phase |
| `platform/GameSounds` | app-scoped one-shots; absorbs `TadaPlayer` |
| `ui/screens/HomeScreen` | new |
| `ui/screens/GrimoireScreen` | rewritten (stage + card) |
| `ui/screens/RitualScreen` | cue chip, altar ring, chant tablet, abandon dialog |
| `ui/components/*` | new: `CueChip`, `ChantTablet` (with `FocusBar`, `Medallion`), `BadgeRow`, `InfoBand`, `DossierCarousel`, `QuoteSlot`, `SigilAvatar`, `FloorCircle`, `StageBackground`, `CompassRose`, `RestlessGrimoire`, `Smudge`/`InkIn`, glyphs (`DirectionGlyph`, `HouseGlyph`, `CallingGlyph`, `LegionsGlyph`) |
| `navigation/AppNavHost` | Home / Grimoire / Ritual; shared element Ritual → Grimoire |
| `ui/components/DebugSheet` | §7 |

Removed: `ProfileScreen`, `Profile` route, `DemonViewModel` (and `DemonViewModelTest`),
`CompassArc` / `CompassMode` (→ `AltarRing`), `ChantStack`, `graceFraction`, `phaseMs`,
`PhraseLost`, `DRIFT_GRACE_MS`, `TadaPlayer` (→ `GameSounds`). Also update:
`RitualStateTest` (drop `graceFraction` / `phaseMs` cases), and the KDocs in
`SharedDemonArt.kt` and `data/Demon.kt` that mention Profile.

New dependencies: `androidx.datastore:datastore-preferences`,
`org.jetbrains.kotlinx:kotlinx-serialization-json` (the serialization plugin is
already applied), `androidx.core:core-splashscreen`, and
`com.android.tools:desugar_jdk_libs` with `isCoreLibraryDesugaringEnabled = true`
(`java.time` on API 24–25). Pin the newest stable versions that compile with Kotlin
2.2.10.

## 10. Error handling

| Case | Behaviour |
|---|---|
| Progress unreadable / corrupt | Start from `Progress()`; log once. |
| DataStore write fails | Log; that change is lost; the UI keeps the stored state. |
| Clock moved backwards | `canSummon` stays false until the stored day is passed; restlessness uses `max(0, …)`. |
| App killed during the climax | Summon already committed. If the Ritual destination is restored, its guard (§3) pops to Grimoire; the demon shows bound, no celebration. |
| App killed mid-ritual before the climax | Nothing saved; the daily summon is not spent. |
| Mic / heading errors | As Ritual 2.0, shown through the cue chip and error panel. |
| No vibrator, API < 31 / < 33 | As Ritual 2.0. |

## 11. Testing

JVM unit tests (existing pattern; no instrumented tests). Port the prototype's Vitest
suites case for case:

- `ProgressRulesTest`: one summon per day, never unlocks on a clock moved back;
  restless at 2 days and escape at 3, keeping lore and flawless; a visit resets the
  count; a re-summon keeps an earned flawless; reveal flags clear one by one; unsummon
  forgets the demon; `focusDemon` order (restless → unbound while available → last
  viewed → first).
- `CourtHoursTest`: Sol 9–12 and 15–18, Venus 6–12, boundaries exclusive at the end.
- `RitualEngineTest` (rewritten): lock at 600 ms; court multiplier fixed at lock;
  phrase I has no clock and Wavering on it does not drain; a seal starts a full clock;
  drain 1× on heading even when silent and 3× off heading; syllables don't count off
  heading and resuming needs no re-lock; null heading = off heading; running out
  cracks the last seal, clears the phrase and ends flawless; a crack on phrase III
  reopens phrase II with its own budget; a flawless run reaches the climax and reports
  `ClimaxStart(flawless = true)`; budgets (18 syl → 12 000 ms; 6 syl in court →
  7 500 ms; phrase I → 0).
- `ProgressJsonTest`: round trip; unknown keys ignored; corrupt JSON → `Progress()`.
- `GameViewModelTest` (fake clock + in-memory store): `evaluate` after load and not
  before; every mutation goes through `update`; visit on selection and on a `ticks`
  day change; a visit 3+ days after the last one escapes the demon instead of
  visiting him; `openGrimoireFromHome` uses `focusDemon`; celebration flow and Secret
  jump after a flawless summon; commit uses the ritual's start day.
- `RitualViewModelTest`: `inCourt` reaches the engine; no ticks while inactive; ticks
  held after restart until a heading arrives; `commitSummon` called exactly once with
  the right `flawless`, even with no effect collector.
- `DemonCatalogTest`: new fields present; exactly 3 abilities; headings 90 / 0.

Manual device checklist (in the plan): first launch → Home hint → Grimoire on
Asmodeus; summon East with a crack (Secret stays sealed) and flawless (Secret readable);
cooling cue after one summon; advance days → restless (quote, jitter, Home hops) →
escape banner; kill the app during the climax (relaunch: bound, no second summon);
background mid-chant and return (no drift, bed resumes); app open across midnight
(cooling cue clears); court in/out focus bars; badge band with 5 s return; carousel on
both demons; card and art size identical in every state; font scale 1.3× and 2.0× on a
small API 24 screen; reduced motion; API 33+ device.

## 12. Canon updates

New ADRs (numbers as cited by the prototype source; reconcile if the originals exist):

- **ADR-0010 Focus clock** — budgets, drain, crack, no return to Seeking; supersedes
  ADR-0008's drift grace and "silence pauses without penalty" (silence now drains
  phrases II–IV).
- **ADR-0011 Houses and Callings** — House (planet, metal, court hours), Calling,
  badges, dossier data; stats stay a non-goal.
- **ADR-0012 Court Hours** — fixed sunrise/sunset, +25 % focus, fixed at lock, no weather.
- **ADR-0013 Persistence and flawless** — DataStore JSON, commit at ClimaxStart,
  sticky flawless unlocks the Secret.
- **ADR-0014 Home and text roles** — the Restless Grimoire home, `cue` / `incantation` /
  `meta` / `name` / `body` roles, chant tablet.
- **ADR-0015 Daily summon, restless, escape** — one per local day, 2 / 3 days, lore kept.
- **ADR-0016 Demon card replaces Profile** — stage + static card, visit on view (no tap
  on bound demons), badge explanations in the band, fixed layout across demons/states.

Amended ADRs:

- **ADR-0004 Web demo** — for this pass the prototype in `roalcantara/mydemon` is the
  behavioural reference; it is still not kept in sync afterwards.
- **ADR-0006 Ritual heading per demon** — drop "Both MVP demons use 0° (North)":
  Asmodeus faces East (90°).

**PRODUCT.md:** Pitch ("North for the first two" → each demon's own direction); Core
loop (Home → Grimoire → Ritual → Grimoire); replace Grimoire, Lock state, Ritual setup,
Heading gate, Chant, Success and Motion rows; remove both Profile rows; Debug aids (§7);
Demons table (§8, incl. Asmodeus East and new descriptions); Non-goals: drop
"Persistence across app restarts", keep stats, add "weather".

**DESIGN.md:** tokens `house-sol` `#f2c14e`, `house-venus` `#c87a3c`, plus tokens for
the literal colours the prototype uses: `stage-home` / `stage-sol` / `stage-venus`
gradient stops (the three placeholder backgrounds), `ink-highlight` `#fff6d0` (focus
shimmer, flash core), smoke = `on-surface-variant`, Home embers and rose = `primary`;
text roles (§4); component contracts: new ones (§9), `AltarRing` replacing
`CompassArc`, `KaraokeLine` inside `ChantTablet`, `PrimaryActionButton` CTAs now
**Awesome!** and Abandon/Stay (no Summon / View / Back), `RibbonBanner` and
`RotatingSunburstBackground` used on the Grimoire celebration (not Profile); motion
list (§4); gaps (below).

**AGENTS.md:** current state; `progress/` and `time/` packages; the prototype location
(replacing the `../mdweb` pointer for this pass).

## Gaps (placeholders ship; art can replace them later)

- Home hall background and House stage backgrounds (`stage_sol`, `stage_venus`):
  gradients until art exists (the prototype's Hero Wars stand-ins must never ship).
- The Home grimoire: vector `RestlessGrimoire` until a Stitch illustration exists.
- Asmodeus scene art: `demon_asmodeus_full` keeps the earlier render.

## Phases

1. Canon: ADR-0010…0016, ADR-0004/0006 amendments, PRODUCT.md, DESIGN.md, AGENTS.md.
2. Pure core (TDD): `progress/*`, `CourtHours`, demon data, focus-clock `RitualEngine`
   **together with every caller it breaks** (`RitualStateTest`, `RitualFeedback`'s
   `when`, `RitualScreen`'s use of `graceFraction` → a minimal stand-in until phase 5).
3. Platform: `GameClock`, `ProgressStore` + DataStore + splash, `GameViewModel`
   **replacing `DemonViewModel` in `AppNavHost` in the same phase** (Profile keeps
   working against it until phase 5), `RitualViewModel` changes, feedback, `GameSounds`.
4. Components: text roles, glyphs, cue chip, chant tablet, altar ring, badge row + info
   band, carousel + smudge/ink-in, quote slot, sigil avatar, floor circle, stage
   background, compass rose, restless grimoire.
5. Screens: Home, Grimoire (stage + card + celebration), Ritual changes, navigation and
   Ritual guard, debug sheet; remove Profile.
6. Cleanup, canon catalog check, device checklist.

Each phase updates every caller of what it changes, so it ends with
`./gradlew testDebugUnitTest lint assembleDebug` green and a commit.

## Out of scope

More demons, weather, notifications/reminders, cloud sync, more than one summon per
day, stats/levels/currencies, the "LOVIS" question (ADR-0005), instrumented/UI tests,
the prototype's jump-to-state debug tool.
