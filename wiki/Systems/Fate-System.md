# Fate System

The fate system loads deity-linked task templates from `data/*/fates/<deity_id>/*.json` and attaches them into each AI deity config's task list.

## Loader Behavior

`FateDataLoader` requires every fate file to declare `linked_deity`.

When a fate is parsed:

- it becomes a `TaskTemplate`
- it is attached to the matching AI deity config when available
- it is queued in a pending map if the AI config is not ready yet

## Current Load-Order Caveat

The pending-attachment model means fate loading is data-driven but still sensitive to AI-config link order.

That is a real implementation constraint and should be documented as such.

## Current Data Shape

Fates currently support:

- string or object requirements
- reputation rewards
- reward command arrays
- optional progression unlocks
- cooldown and repeatable flags
- serialized AI assignment context

## Command Surface

The runtime exposes both `tasks` and `fates` command trees, and both currently route into the same handler layer.