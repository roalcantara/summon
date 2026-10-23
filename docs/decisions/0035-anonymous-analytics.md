# 0035 — Anonymous analytics

- Date: 2026-10-23
- Status: Accepted. Supersedes ADR-0017 §3 (privacy stance).
- Spec: docs/superpowers/specs/2026-10-23-analytics-design.md
- Evaluation: docs/success/2026-10-23-evaluation-playbook.md

## Context
ADR-0034 needs to know whether players return after day 2, and the chant check needs calibration
from real devices (ADR-0018). The Play Console shows neither.

## Decision
Release builds send 8 typed, bucketed events (app_open, ritual_start, mic_permission,
direction_locked, line_result, ritual_end, demon_escaped, device_check) to TelemetryDeck (EU), with
a random app-instance ID hashed by the SDK. On by default; the player turns it off in Settings
("Share anonymous usage data"). No audio, text, location or personal data; debug builds log to
Logcat and send nothing. The vendor sits behind `Analytics`; moving to Firebase with AdMob (v1.1)
is one class.

## Consequences
- `INTERNET` permission; privacy policy and Play Data safety updated; no consent banner (no
  advertising or device IDs).
- The app ID comes from `SUMMON_TELEMETRYDECK_APP_ID` (environment or local.properties).

## Amendment 2026-10-23: v1 ships without an analytics provider
TelemetryDeck no longer offers a free plan. Rather than delay the release for a vendor decision,
v1 ships with **no analytics**: the TelemetryDeck SDK, its sink and the `INTERNET` permission are
removed, release builds use `NoAnalytics`, and Settings hides the "Share anonymous usage data"
switch while no provider is wired (`AppGraph.analyticsWired`). Debug builds still log the events
to Logcat. The typed events, the opt-out gate and the instance ID stay, so v1.1 adds one sink.
- Play Data safety for v1: **no data collected**. The privacy policy says so.
- v1 is judged from Play Console data only (installs, retained installers, ratings, vitals); the
  in-game metrics and the Psst test wait for v1.1.
- Choosing the provider is v1.1 work (TODO). Candidates as of 2026-10-23: PostHog Cloud (1M free
  events/month, anonymous events, funnels and retention), Firebase Analytics (free, but device IDs to
  declare), Aptabase (private, but no retention or funnels).
