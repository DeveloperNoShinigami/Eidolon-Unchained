# Reputation System

Eidolon Unchained builds on Eidolon's reputation capability and then layers deity progression and patron-side effects on top.

## Core Model

- reputation is tracked per player and per deity
- deity progression stages are defined in deity JSON
- progression titles and rewards are unlocked from those stages

## Where Reputation Changes Happen

Successful prayer flows can add reputation directly.

The code also triggers progression checks and UI refresh behavior when reputation changes are applied.

## Important Runtime Rules

- stage thresholds are per-deity data, not a shared hardcoded ladder
- patron relationships can change how reputation is interpreted or penalized
- progression checks are event-driven and also forced during some prayer paths

## Practical Effect

Reputation is the bridge between deity identity, AI response quality, patron status, fate availability, and codex or research progression.# Reputation System

Reputation in Eidolon Unchained is built on top of Eidolon's reputation capability and then extended through deity progression logic, patron logic, prayer rewards, and progression update handlers.

## What Reputation Drives

Reputation affects:

- deity progression stage unlocks
- prayer availability
- patron-related behavior
- task and fate gating
- codex or progression messaging tied to deity advancement

## Stage Thresholds

There is no single universal hardcoded stage ladder.

Stage thresholds come from each deity JSON, which means different deities can declare different layouts and titles.

## Where Reputation Changes Happen

Reputation can move through:

- normal Eidolon-side changes
- prayer success handling
- stage reward application
- task or fate rewards
- patron conflict logic

## Documentation Rule

When writing or authoring content, document stage thresholds from the actual deity JSON in use instead of assuming a fixed five-stage model.