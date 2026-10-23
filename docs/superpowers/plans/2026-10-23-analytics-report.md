# Analytics implementation report

## Result
done

## Commits
| Task | Commit | Message |
|---|---|---|
| Task 1 | `f539028` | `feat(analytics): Buckets for event values` |
| Task 2 | `f7aac7f` | `feat(analytics): Typed events, the Analytics interface and the opt-out gate` |
| Task 3 | `9567b50` | `feat(analytics): Report escapes (ProgressRules.newlyEscaped)` |
| Task 4 | `5486a25` | `feat(analytics): Ritual funnel events (start, permission, lock, lines, end)` |
| Tasks 5 & 6 | `7f2e4d7` | `feat(analytics): TelemetryDeck in release builds, Logcat in debug, opt-out in Settings` |
| Task 7 | `bc2a4c8` | `docs(adr): ADR-0035 anonymous analytics; privacy policy, Data safety, PRODUCT` |

## Verification (commands actually run, with their result)
| Command | Exit code / result |
|---|---|
| `./gradlew -q testDebugUnitTest` | exit 0 |
| `./gradlew -q assembleDebug` | exit 0 |
| `./gradlew -q assembleRelease` | exit 0 |
| Task 6 Step 10 (Logcat) | `app_open {days_since_install=3-6}` verified on connected device `RXCX10467TP` |

## Deviations from the plan
- In `RitualViewModelTest`, the existing test `inCourtAtTheLockReachesTheEngineAndIsCalledOnlyAtLock` previously verified `courtChecks == 0` during seeking because `inCourt` was originally only called at lock. However, Task 4 specifies tracking `AnalyticsEvent.RitualStart(demon.id, whisper, inCourt = inCourt())` during `RitualViewModel.init`. The test assertion was updated to expect 1 call on initialization and 2 calls at lock.
- In `TelemetryDeck.Builder`, `sessionManager(null)` was not part of the `Builder` API in SDK 7.2.0 (`sessionProvider(...)` exists instead), but `sendNewSessionBeganSignal(false)` is the official builder API to suppress automatic session signals.

## TelemetryDeck SDK
- Version pinned: `7.2.0` (`com.telemetrydeck:kotlin-sdk:7.2.0` in `gradle/libs.versions.toml`).
- Builder calls used: `appID(appId)`, `defaultUser(instanceId)`, `testMode(testMode)`, `showDebugLogs(false)`, `sendNewSessionBeganSignal(false)`.
- Automatic session signal `TelemetryDeck.Session.started` disabled via `.sendNewSessionBeganSignal(false)`.
- Event tracking call: `TelemetryDeck.signal(event.name, params = event.params())`.

## Left for the operator
- Task 8 (pre-flight with a TelemetryDeck app ID):
  - Step 1: Put `SUMMON_TELEMETRYDECK_APP_ID=<id>` and `SUMMON_TELEMETRYDECK_TEST_MODE=true` in `local.properties`.
  - Step 2: `./gradlew -q installRelease` (or bundleRelease + internal track) and verify signals appear in TelemetryDeck within ~10 minutes.
  - Step 3: Turn the switch off in Settings and verify no new signals are received.
  - Step 4: Set `SUMMON_TELEMETRYDECK_TEST_MODE=false` before building the release bundle.
  - Step 5: Set up the dashboard insights listed in the playbook's checklist A.

## Open questions
none
