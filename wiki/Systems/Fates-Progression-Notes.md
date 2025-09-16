# Fates Progression & Assignment Notes

This page summarizes recent changes to fate progression, assignment gating, and commands.

## Progression Stage IDs (canonical)
- All fates now use each deity's progression stage IDs for `progression_tier` and any references.
- Allowed values include each deity's defined stages, plus the special bypass:
  - `none` — permits non-attuned players to receive certain introductory fates.

## AI Assignment Context (required)
- Every fate JSON includes `ai_assignment_context` with a `trigger_conditions` block.
- Common gates you can use:
  - `min_reputation`, `max_reputation`
  - `required_items`, `required_biomes`, `dimension_requirements`, `time_conditions`
  - `completed_tasks`, `unlocked_progressions`, `required_research`

## Rewards and Progression Unlocks
- `rewards.progression_unlock` uses the same stage ID system as the deity config.
- Avoid legacy tokens like `*_stepN`. All references should be canonical stage IDs.

## Command UX updates
- Unified root: `/eidolon-unchained` with alias `/eu`.
- Fates commands are under `/eidolon-unchained fates ...` with autocomplete.
- Suggestion providers output clean tokens (no quotes or `#`) and support `namespace:path` IDs.
- Chant/sign suggestions are dynamic:
  - Signs enumerate from the Eidolon `Signs` registry.
  - Chants enumerate from the Datapack Chant Manager.

## Migration checklist
- [x] Replace legacy progression tokens with stage IDs
- [x] Ensure `progression_tier` points to a valid stage ID or `none`
- [x] Include `ai_assignment_context` on all fate JSONs
- [x] Clean reward commands; allow colons in suggestions
- [x] Consolidate commands under unified root

See also:
- `wiki/Commands/Commands.md`
- `wiki/Systems/Fates-Complete-Reference.md`
