# Handoff: implement anonymous analytics (Tasks 1–7)

You are implementing a planned feature in an existing Android app. You have no prior context on
this project; everything you need is in the files named below. Work carefully, test first, and
report only what you actually ran and saw.

## The project

**Summon Demons Safely**: a small native Android game in Kotlin and Jetpack Compose (package
`com.summondemons.safely`, min SDK 24, target SDK 37). The player summons chibi demons by facing a
compass direction and chanting a Latin phrase into the microphone. Release is imminent.

Repository root: `/Users/roalcantara/Work/mydemon/mydemon`, branch `fix/pre-release-ui`.

## What to build

Release builds send 8 anonymous, typed events (ritual funnel, daily activity, escapes, device
check) to TelemetryDeck, behind an opt-out switch in Settings; debug builds log them to Logcat and
send nothing.

Read, in this order:
1. `AGENTS.md`: conventions and commands (all player text in string resources, 5 languages,
   colours and spacing from the theme, Conventional Commits).
2. `docs/superpowers/specs/2026-10-23-analytics-design.md`: the approved design.
3. `docs/superpowers/plans/2026-10-23-analytics.md`: **the plan you execute**, task by task. It has
   the code, the tests and the commands.
4. Background only, do not act on: `docs/decisions/0034-retention-roadmap.md`,
   `docs/success/2026-10-23-evaluation-playbook.md`.

## Before you start

1. `git status --short` must show **no modified tracked files** under `app/`. Untracked files under
   `docs/reviews/2026-10-23-final-pass/` are expected; leave them alone. If tracked files under
   `app/` are modified, stop and report: the final-pass fixes are still in progress.
2. `./gradlew -q testDebugUnitTest` must exit 0 before you change anything. If it doesn't, stop and
   report the failure.

## How to work

- Execute **Tasks 1 to 7** in order. **Do not do Task 8** (it needs the operator's TelemetryDeck
  account); list it under "Left for the operator".
- Follow each task's steps: write the failing test, run it and see it fail for the stated reason,
  implement, run it and see it pass, commit with the given message (ending with the line
  `Co-Authored-By: Gemini <noreply@google.com>` instead of the Claude line in the plan).
- **Stay inside the plan's file map.** Don't refactor, rename or "improve" anything else. If a
  step can't be done as written (a name differs, an API differs), make the smallest change that
  keeps the plan's intent and record it under "Deviations" with the reason.
- **The TelemetryDeck SDK** (Task 6): pin the current version from
  https://github.com/TelemetryDeck/KotlinSDK and check the builder and `signal` calls against that
  version's README. Keep every SDK call inside `TelemetryDeckAnalytics.kt`. If the SDK sends
  automatic lifecycle or session signals, turn them off. Record the version and any API
  differences under "Deviations".
- **Translations** (Task 5): use the table in the plan; if `TranslationResourcesTest` fails, shorten
  that translation, never the English. Add the keys to `docs/i18n/review.md`.
- **Never** commit `local.properties`, an app ID or any secret. Never push. Never run
  `git reset --hard`, `git checkout -- <file>`, `git clean` or anything that discards work.
- A device may be connected. You may run Task 6 Step 10 (debug build, Logcat) if `adb devices`
  lists one. Restore anything you change on it. If no device is connected, mark that step "not run".

## Done means

- All 7 tasks committed, one commit per task (Tasks 5 and 6 share one, as the plan says).
- `./gradlew -q testDebugUnitTest` exits 0; `./gradlew -q assembleDebug` and
  `./gradlew -q assembleRelease` exit 0.
- `git status --short` shows no modified tracked files.

## Report

Write `docs/superpowers/plans/2026-10-23-analytics-report.md` (do not commit it), in this format:

```markdown
# Analytics implementation report

## Result
<one line: done / partly done (which tasks) / blocked (why)>

## Commits
| Task | Commit | Message |
|---|---|---|

## Verification (commands actually run, with their result)
| Command | Exit code / result |
|---|---|
| ./gradlew -q testDebugUnitTest | |
| ./gradlew -q assembleDebug | |
| ./gradlew -q assembleRelease | |
| Task 6 Step 10 (Logcat) | <events seen, or "not run: no device"> |

## Deviations from the plan
<each change, with the reason; "none" if none>

## TelemetryDeck SDK
<version pinned; calls used; automatic signals disabled? how>

## Left for the operator
- Task 8 (pre-flight with a TelemetryDeck app ID)
- <anything else>

## Open questions
<anything you were unsure about>
```

Only claim what you ran. If a command wasn't run, say so; if it failed, paste the relevant error lines.
