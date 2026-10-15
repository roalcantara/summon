# 0003 — Docs first; generated inputs live in `reference/`

- Date: 2026-10-15
- Status: Accepted

## Context
Prompts, Stitch output, AI chat summaries and draft code disagreed with each other
and sat in `assets/`, a name that collides with Android's `src/main/assets`.

## Decision
Write root canon first (`AGENTS.md`, `PRODUCT.md`, `DESIGN.md`, ADRs), then port
code. Move all generated inputs to `reference/` and treat it as read-only history.
Truth order: PRODUCT/DESIGN → ADRs → code → `reference/`.

## Consequences
The Compose port gets its own spec built on these docs. Agents must not edit
`reference/`.
