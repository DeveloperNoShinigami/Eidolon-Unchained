# Contributing

This project follows the working practices in `CODEX_AGENT_OPTIMAL.md`.

Keep changes crisp, verifiable, and user‑centric. When in doubt, prefer clarity over cleverness.

## What “good” looks like

- Small, focused diffs with clear intent and outcomes.
- Concise preambles before non‑trivial actions or tool calls.
- Plans for multi‑step work, updated as you complete steps.
- Accurate, end‑to‑end docs updates for any user‑visible system change.
- Consistent naming (canonical: `/eidolon-unchained ...`), no duplicate roots.
- Immersive UX: AI responds first, then proposes fates; fate completion surfaces confirmation + rewards.
- JSONs validated; configs and examples mirror real behavior.

## Before you open a PR

- Build: `./gradlew build -x test` (or run tests if present).
- Sanity check: run the game, load datapacks where applicable.
- Docs: update wiki pages that changed (systems, specs, examples), and link them from relevant guides.
- Commands: verify autocomplete and canonical roots.
- Language: add or update `assets/<ns>/lang/*.json` keys as needed.

## References

- Project practices: `CODEX_AGENT_OPTIMAL.md`
- Fate JSON (Task Template): `wiki/Datapacks/Task-JSON.md`
- Fates commands + reputation: `wiki/Systems/Fates-Reputation.md`
- AI Deity JSON: `wiki/Datapacks/AI-Deity-JSON.md`
