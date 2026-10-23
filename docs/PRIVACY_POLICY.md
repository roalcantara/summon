# Privacy Policy — Summon Demons Safely

**Effective date:** 2026-10-23
**Developer:** `Rogério Rodrigues de Alcântara`
**Contact:** `rogerio.alcantara@gmail.com`

Summon Demons Safely ("the app") is a single-player native Android game. This policy explains
what the app touches on your device, what anonymous usage data is sent, and what never leaves your device.
In short: **the game sends only anonymous, aggregated game events so we know whether rituals work
and whether players return. It never collects or transmits audio, text, location, or personal information.**

## Anonymous analytics

Release builds of the app send minimal, anonymous game events to TelemetryDeck (TelemetryDeck GmbH, EU)
so we can evaluate game balance, fix ritual difficulties across phone models, and measure retention.

### What is collected
1. **The 8 typed game events and bucketed values:**
   - `app_open`: days since install (in buckets: 0, 1, 2, 3-6, 7-13, 14-29, 30+).
   - `ritual_start`: demon summoned, check mode (whisper or lenient), and whether the demon's House was in court.
   - `mic_permission`: dialog result (granted, denied, or denied permanently).
   - `direction_locked`: seconds taken to face the required direction (bucketed: 0-5, 5-10, 10-30, 30-60, 60+).
   - `line_result`: line index (1–4), result (sealed or cracked), and chant score bucket (0-49, 50-69, 70-84, 85-100; whisper only).
   - `ritual_end`: outcome (completed or abandoned), phase where it ended, whether flawless, and duration bucket (0-30s, 30-60s, 1-2m, 2-5m, 5m+).
   - `demon_escaped`: demon identifier when a neglected seal breaks.
   - `device_check`: check mode and diagnostic outcome code (e.g. ok, too_slow, load_error).
2. **Device and app metadata added by TelemetryDeck:** app version, operating system version, device model, and locale.
3. **App-instance ID:** A random UUID generated locally on first launch. It is hashed on your device before transmission and is never associated with your name, account, or personal identity.

### What is never collected or sent
- **No audio:** microphone input is analysed in memory on your device in real time and immediately discarded.
- **No speech transcripts or free-form text:** chant validation runs on device.
- **No location data:** compass readings are processed locally to check heading.
- **No personal data:** no names, emails, contacts, photos, files, or advertising identifiers.

### How to turn it off (opt-out)
Anonymous usage sharing is on by default. You can turn it off at any time directly in the game:
- Open **Settings** (gear icon) → turn off **Share anonymous usage data**.
When turned off, the game immediately stops recording and sending any events.

### Data retention and deletion
The random app-instance ID is stored only in the app's local storage and is permanently deleted
if you clear app data or uninstall the app. Stored data on TelemetryDeck is retained in accordance
with TelemetryDeck's privacy standards and data retention policy.

## Microphone (`RECORD_AUDIO`)

The ritual asks you to chant an incantation out loud. To tell whether you are chanting,
the app uses the microphone **strictly on your device, in real time, for voice activity
detection**:

- The microphone is only used while the Ritual screen is open, and only after you grant
  the permission. When you leave the Ritual screen, the microphone stops.
- Audio is processed in memory to measure loudness, voice-like pitch and syllable timing.
- Speech is recognised on your device, only to check that you chant the line. Nothing you say is stored or sent anywhere.
- **No audio is ever recorded, saved to storage, or transmitted.**
- If you deny the permission, the ritual cannot hear you, and the rest of the app still
  works.

## Compass and motion sensors

The ritual asks you to face a particular direction. The app reads the device's
orientation sensors while the Ritual screen is open, to know which way the phone is
pointing. These readings are used immediately on the device and are never stored or
transmitted. The app does **not** use your location.

## Vibration

The app vibrates for game feedback (for example when a phrase is sealed). This needs no
personal data.

## Game progress stored on your device

The app saves your game progress on your device: which demons are bound, which lore is
unlocked, and the dates you summoned or visited them, so it can apply the daily-summon
and "restless demon" rules. This data contains nothing that identifies you, and it never
leaves your device through the app.

If you have Android's backup turned on, Android may include this progress in your
device backup (for example to your Google account) and restore it on a new device. That
backup is handled by Android and Google under their own privacy terms. The developer
cannot access it. You can delete your progress at any time by clearing the app's storage
or uninstalling the app.

## What the app does not do

- No accounts, sign-in or registration.
- No advertising or ad-tracking SDKs.
- No in-app purchases.
- No location, contacts, camera, photos or files.
- No selling or sharing of personal data to anyone.

## Children

The app is not directed at children under 13 and does not knowingly collect personal
information from anyone, including children.

## Changes to this policy

If this policy changes, the new version will be published at this address with a new
effective date. A change that affects how your data is handled will also be noted in the
app's release notes.

## Contact

Questions about this policy: `rogerio.alcantara@gmail.com`.
