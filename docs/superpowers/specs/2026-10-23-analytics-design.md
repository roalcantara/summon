# Anonymous analytics in v1

- Date: 2026-10-23
- Status: Approved (brainstorming)
- Branch: `fix/pre-release-ui`
- Decisions: ADR-0034 (retention roadmap); new ADR-0035 (this change)
- Evaluation: `docs/success/2026-10-23-evaluation-playbook.md` (success criteria, periods, checks)

## Why
v1 must answer two questions the Play Console can't: **do players come back after day 2**
(ADR-0034's baseline) and **does the ritual work in the wild** (direction lock, microphone,
chant check; calibration is an open item of ADR-0018). The app moves from "no network, no data"
to minimal, anonymous analytics.

## Decisions
1. **Vendor:** TelemetryDeck (EU, Kotlin SDK, no device IDs, identifiers hashed on the device),
   behind our own `Analytics` interface; swapping vendors (e.g. Firebase with AdMob in v1.1) is one
   class. Aptabase is the fallback if TelemetryDeck's SDK or terms don't fit.
2. **On by default, opt-out** in Settings ("Share anonymous usage data"), effective immediately.
3. **Debug builds send nothing** (they log events to Logcat); play-tests don't pollute the data.
4. **Exactly 8 events**, typed; no audio, no text, no location, no personal data.
5. A random **app-instance ID** (UUID created on first launch, stored locally, hashed by the SDK)
   lets retention be measured by install cohort; it is not linked to a person and is deleted with
   the app's data.

## Architecture
- `platform/analytics/Analytics.kt`: `interface Analytics { fun track(event: AnalyticsEvent) }`,
  plus `NoAnalytics`.
- `AnalyticsEvent`: a sealed type, one class per event, with enum and bucketed fields only;
  `name` and `params(): Map<String, String>` produce the payload.
- `Buckets.kt`: pure functions mapping raw numbers to fixed buckets (days since install, seconds,
  durations, check scores).
- Implementations: `TelemetryDeckAnalytics` (release, when an app ID is configured), `LogAnalytics`
  (debug), `NoAnalytics` (tests, missing ID). `GatedAnalytics` wraps the active one and drops
  events while the switch is off.
- Configuration: `SUMMON_TELEMETRYDECK_APP_ID` from the environment or `local.properties` into
  `BuildConfig` (like the signing keys, ADR-0017); empty → `NoAnalytics` and a build warning.
- The switch: `AnalyticsSettings` (SharedPreferences, read synchronously at start), default on.
- Wiring: `AppGraph.analytics`; `SummonApp` sends `app_open`; `RitualViewModel` receives an
  `Analytics` and maps its effects and phases; `GameViewModel` sends `demon_escaped`; the device
  check result is sent once when known.

## Events

| Event | When | Fields (values) |
|---|---|---|
| `app_open` | process start, and each return to foreground after 30 min | `days_since_install` (0, 1, 2, 3-6, 7-13, 14-29, 30+) |
| `ritual_start` | the Ritual opens | `demon` (asmodeus, astaroth), `mode` (whisper, lenient), `in_court` (true, false) |
| `mic_permission` | a permission answer arrives | `result` (granted, denied, denied_permanently) |
| `direction_locked` | the lock completes (`RitualEffect.Locked`) | `seconds` (0-5, 5-10, 10-30, 30-60, 60+) |
| `line_result` | a line seals or cracks | `line` (1-4), `result` (sealed, cracked), `score` (whisper only: 0-49, 50-69, 70-84, 85-100; lenient: none) |
| `ritual_end` | the Ritual closes | `outcome` (completed, abandoned), `phase` (intro, seeking, locking, chanting, wavering, error_mic, error_heading), `flawless` (true, false; completed only), `duration` (0-30s, 30-60s, 1-2m, 2-5m, 5m+) |
| `demon_escaped` | evaluation releases a demon | `demon` |
| `device_check` | the first time the check result is known | `mode` (whisper, lenient), `reason` (fixed codes from `DeviceCheck`, e.g. too_slow, no_model, error) |

TelemetryDeck adds its own metadata (app version, OS version, device model, locale); this is
declared in the privacy policy.

## Settings
A row under the language picker in `SettingsSheet`: a `Switch` with "Share anonymous usage data"
and a one-line hint "No audio, no personal data." Strings in 5 languages, none longer than English.

## Privacy and store
- `INTERNET` permission added (analytics only; mic and compass stay on the device).
- ADR-0035 supersedes ADR-0017 §3 (privacy stance).
- `docs/PRIVACY_POLICY.md`: what is sent (the 8 events, SDK metadata, the hashed app-instance ID),
  what is never sent (audio, text, location, contacts, account data), the vendor and region, how
  to turn it off, retention per the vendor.
- `docs/STORE_LISTING.md` Data safety: **App activity → App interactions** and **Device or other
  IDs** (the app-instance ID): collected, not shared, not linked to the user's identity, processed
  ephemerally where applicable, optional (the user can turn it off), encrypted in transit.
- `PRODUCT.md` Privacy row updated.

## Testing
- `BucketsTest`: every bucket boundary; no raw value passes through.
- `AnalyticsEventTest`: names and params of each event.
- `RitualViewModelTest` additions: a fake `RecordingAnalytics` sees `ritual_start`,
  `direction_locked`, `line_result`, `ritual_end(completed, flawless)` for a scripted ritual, and
  `ritual_end(abandoned, seeking)` when cleared early.
- `GatedAnalyticsTest`: switch off → nothing reaches the delegate.
- Device: one debug ritual shows the events in Logcat; one release build sends to TelemetryDeck's
  test mode and the signals appear in its dashboard.

## Out of scope
Ads, consent banners (no device IDs, no ad identifiers), crash reporting (Android vitals covers it),
dashboards beyond what the playbook sets up.
