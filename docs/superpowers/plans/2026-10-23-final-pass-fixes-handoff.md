# Handoff: fix the final-pass review findings (Tasks 0–9)

You are fixing review findings in an existing Android app. You have no prior context on this
project; everything you need is in the files named below. Work carefully and report only what you
actually ran and saw.

## The project

**Summon Demons Safely**: a small native Android game in Kotlin and Jetpack Compose (package
`com.summondemons.safely`, min SDK 24, target SDK 37). The player summons chibi demons by facing a
compass direction and chanting a Latin phrase into the microphone. Release is imminent.

Repository root: `/Users/roalcantara/Work/mydemon/mydemon`, branch `fix/pre-release-ui`.

## What to do

Three reviewers audited the app; their findings were checked against the code and triaged. You
implement the fixes: per-frame recomposition, Back consistency, intro alignment, the paused colour,
large-text caps, reduce-motion gaps, accessibility semantics and a little motion polish.

Read, in this order:
1. `AGENTS.md`: conventions and commands (colours, type and spacing from the theme; all player text
   in string resources, 5 languages; Conventional Commits).
2. `docs/superpowers/specs/2026-10-23-final-pass-fixes-design.md`: the approved triage and the
   acceptance criteria per task.
3. `docs/superpowers/plans/2026-10-23-final-pass-fixes.md`: **the plan you execute**, task by task,
   with the code, the commands and the device checks.
4. Background only, do not act on: `docs/reviews/2026-10-23-final-pass/report-*.md`. Do not fix
   anything the spec marks Deferred or Won't fix.

## Before you start

1. `git status --short` must show **no modified tracked files**. If it does, stop and report.
2. `./gradlew -q testDebugUnitTest` must exit 0 before you change anything. If not, stop and report.
3. `adb devices` should list the Galaxy S24. Without a device, do the code steps, mark every device
   check "not run: no device", and say so in the Result line.

## How to work

- Execute **Tasks 0 to 9** in order, one commit per task (Task 0 has no commit). End every commit
  message with the line `Co-Authored-By: Gemini <noreply@google.com>`.
- Task 8 starts with a unit test: run it and see it fail for the stated reason before implementing.
- **Stay inside the plan's file map.** Don't refactor, rename or "improve" anything else. If a step
  can't be done as written (a name differs, an import path differs, a check fails), make the smallest
  change that keeps the plan's intent and record it under "Deviations" with the reason.
- Two steps decide, they don't assume: Task 7 Step 1 (is the summon cue already clickable?) and Task
  5 Step 4 (is there a double bottom inset?). Record what you found either way.
- **No new player strings, no new dependencies.** If you think one is needed, stop that step and
  list it under "Open questions".
- Evidence goes in `build/final-pass/` with the file names the plan gives. A check you could not run
  is "not run", never "passed".
- **The device:** restore everything you change: font scale `1.0`, language en, gestural navigation,
  animations `1`, TalkBack off. If `tools/ui_matrix.sh` is interrupted, run `adb shell wm density 420`.
  Never uninstall the app or clear its data except through the debug panel / `ui_matrix.sh`.
- Do not edit anything under `reference/`. Never push. Never run `git reset --hard`,
  `git checkout -- <file>`, `git clean` or anything that discards work.

## Done means

- Tasks 1–9 committed, one commit each.
- `./gradlew -q testDebugUnitTest` and `./gradlew -q assembleDebug` exit 0.
- `git status --short` shows no modified tracked files.
- The device is back at density 420, font scale 1.0, language en.

## Report

Write `docs/superpowers/plans/2026-10-23-final-pass-fixes-report.md` (do not commit it), in this
format:

```markdown
# Final-pass fixes report

## Result
<one line: done / partly done (which tasks) / blocked (why)>

## Commits
| Task | Commit | Message |
|---|---|---|

## Acceptance (one row per AC in the spec)
| Task.AC | Result (pass / fail / not run) | Evidence (command + exit code, or file in build/final-pass/) |
|---|---|---|

## Decisions taken on the device
- R1-1 (summon cue clickable?): <what the dump showed; changed or not>
- R1-12 (double bottom inset?): <what the screenshot showed; changed or not>

## Deviations from the plan
<each change, with the reason; "none" if none>

## Open questions
<anything you were unsure about>
```

Only claim what you ran. If a command wasn't run, say so; if it failed, paste the relevant error
lines.
