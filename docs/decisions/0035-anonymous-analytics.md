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
