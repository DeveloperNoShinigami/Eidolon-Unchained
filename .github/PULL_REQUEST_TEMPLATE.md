## Summary

Explain the problem and the change in 2–3 sentences. Link related issues.

## Checklist (must)

- [ ] Follows `CODEX_AGENT_OPTIMAL.md` (concise, focused, verifiable).
- [ ] Updated relevant docs (wiki/…): systems, specs, examples, commands.
- [ ] Kept canonical command roots (`/eidolon-unchained …`); tested autocomplete.
- [ ] Validated JSONs (AI Deity, Fates, Chants, Research, Recipes) load without errors.
- [ ] Language keys added/updated where user text changed.
- [ ] Build passes locally: `./gradlew build -x test` (or with tests if applicable).

## Fates / AI (if applicable)

- [ ] AI responds first, then proposes fate (immersion preserved).
- [ ] Fate completion confirms in chat and grants rewards automatically.
- [ ] Progressive fates use `progression_unlock` (and gating) consistently.
- [ ] Examples reflect actual runtime behavior.

## Notes

Add anything reviewers need to know to verify changes quickly.
