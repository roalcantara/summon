# 0004 — Web demo stays in its own repo

- Date: 2026-10-15
- Status: Accepted

## Context
`assets/web` was a symlink to `../../mdweb`, an AI Studio React/Vite simulator in a
separate git repo. The link only resolved on the author's machine.

## Decision
Delete the symlink. Document the sibling repo `../mdweb` and how to run it in
`AGENTS.md`.

## Consequences
Clones of this repo no longer contain a dangling link. The web demo is optional
and not kept in sync with the Android app.
