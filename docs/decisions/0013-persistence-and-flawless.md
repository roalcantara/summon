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
