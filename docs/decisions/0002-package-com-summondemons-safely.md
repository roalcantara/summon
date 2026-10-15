# 0002 — Package and applicationId `com.summondemons.safely`

- Date: 2026-10-15
- Status: Accepted

## Context
Three names were in use: `com.example.mydemon` (Android Studio template),
`com.example.summondemonssafely` (draft Kotlin) and `com.summondemons.safely`
(Stitch asset guide). Play Store rejects `com.example.*`.

## Decision
Use `com.summondemons.safely` for namespace, applicationId and Kotlin packages.

## Consequences
The rename happens during the Compose port, not in the docs pass. Until then,
`app/` still uses `com.example.mydemon`.
