# Patron System

The patron system tracks which deity a player has pledged to and how that pledge affects AI response rules, team assignment, and opposition logic.

## Storage

Patron state lives in the player capability layer through `IPatronData`.

## Main Operations

The current command-facing patron operations are:

- choose a patron
- abandon a patron
- view patron status

These operations are command-driven in the current implementation. The codebase does not require a separate patron-exclusive ritual flow for basic patron assignment.

## Patron Rules From AI Configs

`patron_config` in AI deity JSON can define:

- allied deities
- opposing deities
- follower and enemy personality modifiers
- conversation rules
- scoreboard team behavior
- supported mob IDs and friendly-fire settings
- mob follower mana and magic power economy
- enthrall stage gate

## Mob Follower Economy (`patron_config`)

These keys control how mana and magic power scale for enthralled mobs that serve the deity:

```json
"patron_config": {
  "defaultFollowerMobMana": 100,
  "defaultFollowerMobMagicPower": 1.0,
  "followerMobManaByStage": {
    "<progression_title>": 150
  },
  "followerMobMagicPowerByStage": {
    "<progression_title>": 1.5
  },
  "stageRequiredForEntrall": "<progression_title_string>",
  "followerMobIds": ["<namespace>:<mob_entity_name>"]
}
```

- `defaultFollowerMobMana` — baseline mana pool assigned to enthralled mobs. Defaults to 100.
- `defaultFollowerMobMagicPower` — baseline magic power multiplier for mob chant casting.
- `followerMobManaByStage` — keyed by progression title string (the `title` field in a deity stage, not the stage `id`). Overrides the default when the patron's current title matches.
- `followerMobMagicPowerByStage` — same key format as above, overrides magic power bonus per stage.
- `stageRequiredForEntrall` — the progression title the patron player must hold before enthralling any mob in `followerMobIds`. Compared as a string match against the player's current progression title.

Snake_case aliases are accepted: `default_follower_mob_mana`, `default_follower_mob_magic_power`, `follower_mob_mana_by_stage`, `follower_mob_magic_power_by_stage`.

## Scoreboard Team Config (`patron_config`)

```json
"patron_config": {
  "assignsPlayersToTeam": true,
  "teamName": "<scoreboard_team_display_name>",
  "teamColor": "<minecraft_chat_color_name>",
  "friendlyFire": false
}
```

- When `assignsPlayersToTeam` is true, patrons of this deity are added to the named scoreboard team at join/tick.
- `teamColor` uses Minecraft chat color names (`dark_purple`, `gold`, `white`, etc.).
- Enthralled mobs in `followerMobIds` are also synchronized to the team at runtime.

## Abandon Behavior

Abandon consequences are data-driven from deity configuration.

- penalty behavior comes from the deity's abandon settings
- some deities can reset reputation on abandonment
- patron-related text and penalties are not fixed globally# Patron System

The patron system tracks a player's chosen deity relationship and uses AI config data to decide how other deities react.

## Main Commands

The current root command family is:

- `/eidolon-unchained patron choose`
- `/eidolon-unchained patron abandon`
- `/eidolon-unchained patron status`

`/eu` is the short alias for the root command.

## What Patron Config Controls

The patron layer is mainly driven by `patron_config` inside AI deity JSON:

- whether patron status is required
- opposing deities
- allied deities
- follower and enemy response modifiers
- optional team assignment behavior
- supported mobs and friendly-fire settings
- mob follower mana/magic power economy by stage
- enthrall stage gate

## Runtime Effects

Patron status affects:

- whether a deity will answer a player at all
- whether enemy contact causes penalties
- how conversation tone changes
- whether a player is treated as follower, neutral, enemy, allied, or patronless
- which mobs can be enthralled and at what progression stage

## Important Boundary

The current patron system is command- and config-driven. It is not a separate ritual-only unlock layer.

Patronage ritual codex entries (under categories like `light_rites` and `dark_rites`) document the in-world observances associated with choosing a patron, but the actual patron assignment is still command-driven.