# Eidolon Base Commands

These commands are registered by **Eidolon Repraised** itself, not by Eidolon Unchained. They are available whenever Eidolon is loaded.

---

## `/knowledge` — Player Knowledge Management

**Permission required:** level 2 (operator)

Manages a player's codex knowledge state: signs, runes, research entries, and facts.

### Syntax

```
/knowledge <targets> reset signs
/knowledge <targets> reset runes
/knowledge <targets> reset research
/knowledge <targets> reset facts

/knowledge <targets> grant sign <sign>
/knowledge <targets> grant rune <rune>
/knowledge <targets> grant research <research>
/knowledge <targets> grant fact <resource_location>

/knowledge <targets> remove sign <sign>
/knowledge <targets> remove rune <rune>
/knowledge <targets> remove research <research>
/knowledge <targets> remove fact <resource_location>
```

`<targets>` is a standard Minecraft player selector (e.g. `@s`, `@a`, `Steve`).

All `<sign>`, `<rune>`, and `<research>` arguments have tab-completion from the registered registries.

### What Each Subcommand Does

| Subcommand | Effect |
|---|---|
| `reset signs` | Removes all known signs from the player's `IKnowledge` capability |
| `reset runes` | Removes all known runes |
| `reset research` | Removes all completed research entries |
| `reset facts` | Removes all known facts |
| `grant sign <sign>` | Adds a single sign to the player's known signs |
| `grant rune <rune>` | Adds a single rune |
| `grant research <research>` | Adds a research entry to the player's codex |
| `grant fact <id>` | Adds a fact by resource location (e.g. `eidolon:some_fact`) |
| `remove sign <sign>` | Removes a single sign |
| `remove rune <rune>` | Removes a single rune |
| `remove research <research>` | Removes a single research entry |
| `remove fact <id>` | Removes a fact by resource location |

### Notes

- `reset research` only clears the `IKnowledge` capability data. It does **not** clear task-level progress stored by Eidolon Unchained. Use `/eu research clear <player>` from Eidolon Unchained to fully wipe all progress including custom task NBT.
- Signs granted via this command work with full resource locations, including datapack signs registered by Eidolon Unchained (e.g. `eidolonunchained:myrkul_sign`).
- Facts use plain `ResourceLocation` argument syntax, not a custom argument type — they are not validated against a registry.

---

## `/devotion` — Player Devotion / Reputation

**Permission for `get`:** none (any player can query)  
**Permission for `set`:** level 2 (operator)  
**Permission for `tryfix`:** none

Reads and sets a player's devotion (reputation) toward a specific deity, and offers a repair utility for broken knowledge state.

### Syntax

```
/devotion <targets> get <deity>
/devotion <targets> set <deity> <value>
/devotion <targets> tryfix
```

`<deity>` has tab-completion from the registered deity list (all Eidolon and Eidolon Unchained deities are included).

`<value>` is a double in the range `0.0` to `100.0`.

### What Each Subcommand Does

| Subcommand | Effect |
|---|---|
| `get <deity>` | Prints the player's current devotion value for that deity to chat |
| `set <deity> <value>` | Sets the devotion value directly on the level's `IReputation` capability |
| `tryfix` | Runs `KnowledgeUtil.tryFix(player)` — repairs inconsistent or corrupted knowledge state |

### Notes

- Devotion values do not clamp on read — they reflect exactly what is stored in the `IReputation` capability.
- `tryfix` is safe to run on any player at any time. Use it if a player's codex or research state appears broken after a crash, world transfer, or other capability corruption.
- The `set` subcommand writes directly to the world capability. Changes take effect immediately without a relog.

---

## Relationship to Eidolon Unchained Commands

The Eidolon Unchained command root (`/eu` / `/eidolon-unchained`) has its own reputation and research subcommands that wrap or extend these base commands.

It also now includes a debug mana surface for Eidolon soul mana inspection and mutation:

```
/eu debug mana <player>
/eu debug mana <player> get
/eu debug mana <player> set <value>
/eu debug mana <player> restore <amount>
/eu debug mana <player> set-max <value>
/eu debug mana <player> increase-max <amount>
```

These commands are operator-only and write directly to the player's Eidolon `ISoul` capability.

| Base Eidolon | Eidolon Unchained equivalent |
|---|---|
| `/knowledge <p> reset research` | `/eu research clear <p>` — also clears custom task NBT |
| `/knowledge <p> grant research <r>` | `/eu research grant <r> <p>` (if present) |
| `/devotion <p> get <deity>` | `/eu debug reputation <p> <deity>` |
| `/devotion <p> set <deity> <v>` | `/eu fates reputation set <p> <deity> <v>` |

When doing a full research reset, prefer the Eidolon Unchained command — it covers both layers.
