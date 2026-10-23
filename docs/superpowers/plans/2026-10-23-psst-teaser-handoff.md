# Handoff: implement the "Psst" fake-door test

You are implementing a small planned feature in an existing Android app. You have no prior context
on this project; everything you need is in the files named below. Work carefully, test first, and
report only what you actually ran and saw.

## The project

**Summon Demons Safely**: a small native Android game in Kotlin and Jetpack Compose (package
`com.summondemons.safely`). The player summons chibi demons by facing a compass direction and
chanting a Latin phrase. Repository root: `/Users/roalcantara/Work/mydemon/mydemon`, branch
`fix/pre-release-ui`.

## What to build

From the day after a demon is summoned (once his lore is read), a balloon by his head says
"Psst… come closer." Tapping it, or him, answers "Heh heh heh… Come back soon… if you dare,
mortal." for 3 seconds with a haptic. Two analytics events measure how many players tap. It is a
fake-door test: nothing new unlocks.

Read, in this order:
1. `AGENTS.md`: conventions (player text only in string resources, 5 languages, theme tokens only,
   Conventional Commits).
2. `docs/superpowers/specs/2026-10-23-psst-teaser-design.md`: the approved design.
3. `docs/superpowers/plans/2026-10-23-psst-teaser.md`: **the plan you execute** (6 tasks, with code,
   tests and commands).

## Before you start

1. The analytics work must be in: `app/src/main/java/com/summondemons/safely/platform/analytics/`
   must contain `Analytics.kt` and `AnalyticsEvent.kt`, and `GameViewModel` must take an
   `analytics` parameter. If not, stop and report: the analytics handoff hasn't been run yet.
2. `git status --short` must show no modified tracked files under `app/`. If it does, stop and report.
3. `./gradlew -q testDebugUnitTest` must exit 0 before you change anything.

## How to work

- Execute Tasks 1 to 6 in order, following each step: failing test → see it fail → implement → see
  it pass → commit with the given message, ending with `Co-Authored-By: Gemini <noreply@google.com>`.
- **Stay inside the plan's files.** No refactors or renames elsewhere. If a step can't be done as
  written (a name differs), make the smallest change that keeps the intent and record it under
  "Deviations".
- **Translations:** use the plan's table; if `TranslationResourcesTest` fails, shorten that
  translation, never the English.
- **The laugh:** add the `GameSounds.playLaughIfPresent` hook as written, but **do not add any
  sound file**. The operator supplies a CC0 clip later.
- Never commit secrets or `local.properties`. Never push. Never run `git reset --hard`,
  `git checkout -- <file>`, `git clean` or anything that discards work.
- If `adb devices` lists a device, run Task 5 Step 6's device check and restore any setting you
  change (font scale back to 1.0). Otherwise mark it "not run".

## Done means

- Tasks 1–6 committed (Tasks 4 and 5 share one commit, as the plan says).
- `./gradlew -q testDebugUnitTest`, `./gradlew -q assembleDebug` and `./gradlew -q assembleRelease`
  exit 0.
- `git status --short` shows no modified tracked files.

## Report

Write `docs/superpowers/plans/2026-10-23-psst-teaser-report.md` (do not commit it):

```markdown
# Psst teaser implementation report

## Result
<done / partly done (which tasks) / blocked (why)>

## Commits
| Task | Commit | Message |
|---|---|---|

## Verification (commands actually run, with their result)
| Command | Exit code / result |
|---|---|
| ./gradlew -q testDebugUnitTest | |
| ./gradlew -q assembleDebug | |
| ./gradlew -q assembleRelease | |
| Task 5 Step 6 (device) | <what you saw, or "not run: no device"> |

## Deviations from the plan
<each change with its reason; "none" if none>

## Left for the operator
- Add a CC0 laugh clip as app/src/main/res/raw/demon_laugh.ogg (optional)
- <anything else>

## Open questions
```

Only claim what you ran. If a command wasn't run, say so; if it failed, paste the relevant error lines.
