# Chant JSON

[Overview](Overview.md) | [AI Deity JSON](AI-Deity-JSON.md) | [Chant JSON](Chant-JSON.md) | [Deity JSON](Deity-JSON.md) | [Codex JSON](Codex-JSON.md) | [Research JSON](Research-JSON.md) | [Fate JSON](Fate-JSON.md) | [Facts JSON](Facts-JSON.md) | [Keybind Settings JSON](Keybind-Settings-JSON.md) | [Ritual And Recipe Notes](Ritual-Recipes.md) | [Localization](Localization.md)

Source of truth: `DatapackChantManager` and `DatapackChant`.

## Path

`DatapackChantManager` reads `data/<namespace>/chants/<subfolder>/<file>.json`.

Behavior note:

- The runtime normalizes the chant ID to the file name, not the full subfolder path.
- If `category` is omitted, the first subfolder under `chants/` becomes the category.
- The spell registered into Eidolon uses the normalized chant ID, not the folder path.

## Full Authoring Shape

```json
{
  "name": "<translation_key_or_literal>",
  "description": "<translation_key_or_literal>",
  "category": "<category_name>",
  "codex_icon": "<namespace>:<item_name>",
  "difficulty": 1,
  "mana_cost": 0,
  "cooldown": 0,
  "show_in_codex": true,
  "linked_deity": "<namespace>:<deity_name>",
  "prayer_effect_type": "<prayer_type_name>",
  "requires_effigy": false,
  "signs": [
    "<namespace>:<sign_name>",
    "<namespace>:<sign_name>"
  ],
  "requirements": [
    "reputation:<namespace>:<deity_name>:<minimum_value>",
    "item:<namespace>:<item_name>:<count>",
    "has_item:<namespace>:<item_name>",
    "fact:<namespace>:<fact_id>",
    "research:<namespace>:<research_id>"
  ],
  "effects": [
    {
      "type": "send_message"
    }
  ]
}
```

## Field Notes

- `name` is required.
- `description` is optional and defaults to an empty string.
- `category` is optional if the file is already under `chants/<category>/`.
- `codex_icon` is optional.
- `difficulty` accepts either a number or a string label.
- `mana_cost` defaults to the common config value if omitted.
- `cooldown` defaults to the common config value if omitted.
- `show_in_codex` defaults to `true`.
- `linked_deity` is optional.
- when `linked_deity` is present, combat effects can inherit that deity's configured `deity_damage_type`.
- `prayer_effect_type` is optional, but only meaningful for deity-linked chants.
- `requires_effigy` defaults to `true` when `linked_deity` is set and defaults to `false` otherwise.
- `signs` is the actual chant sequence and is parsed into `ResourceLocation` values in order.
- `effects` defaults to an empty list if omitted.
- `requirements` defaults to an empty list if omitted.
- `combat_role` classifies this chant for the mob planner. See below.
- `mob_priority_weight` is an integer hint used by the mob planner when multiple chants of the same role are eligible. Higher values are preferred.

## Mob Combat Role

Two fields on a chant JSON control how `MobChantCastingGoal` classifies and prioritizes the chant when a mob selects its next cast:

```json
{
  "combat_role": "offense",
  "mob_priority_weight": 8
}
```

### `combat_role`

| Value | When the mob uses it |
|---|---|
| `offense` | Default. Standard damage spell cast when in range. |
| `defense` | Protective or buff spell, same range band as offense. |
| `support` | Healing or ally-buff spell. Also used for threshold emergency casts. |
| `cc` | Crowd-control (roots, slows, stuns). Same range band as offense. |
| `movement` | Repositioning spell (teleport, dash). Eligibility zone depends on mob archetype — see [Chantable Mobs JSON](Chantable-Mobs-JSON.md#movement-role-eligibility-zones). |
| `melee` | Close-range attack. Only eligible when the mob is within `min_range`. |

Default when omitted: `offense`.

### `mob_priority_weight`

Optional integer. Default `0`. A higher value makes this chant preferred over lower-weight chants of the same role when the planner scans the rotation pool. Useful when you have multiple offense chants and want one to fire more often.

---

## Difficulty Field

`DatapackChant.fromJson` supports two `difficulty` styles:

- numeric difficulty, for example `1`
- string difficulty label, for example `"easy"`, `"intermediate"`, `"hard"`, `"master"`, or an explicit progression stage ID

When a string label is used and the chant is linked to a deity, the runtime can gate casting against that deity's progression stages.

## Requirement String Formats

`DatapackChant.checkRequirement` currently parses these prefixes:

- `reputation:<namespace>:<deity_name>:<minimum_value>`
- `item:<namespace>:<item_name>:<count>`
- `item:<namespace>:<item_name>:<count>:<nbt_payload>`
- `has_item:<namespace>:<item_name>`
- `fact:<namespace>:<fact_id>`
- `research:<namespace>:<research_id>`

Unknown requirement prefixes currently fall through permissively for backward compatibility.

## Effect Containers

Each effect is a JSON object with at least a `type` key. `DatapackChant.ChantEffect.apply` currently recognizes:

- `give_item`
- `apply_effect`
- `play_sound`
- `effigy_sound`
- `run_command`
- `send_message`
- `start_conversation`
- `communication`
- `restore_mana`
- `increase_max_mana`
- `modify_attribute`
- `modify_magic_power` (compatibility alias for `modify_attribute`)
- `apply_cooldown`
- `consume_alt_resource`
- `raycast_effect`
- `area_effect`
- `projectile_effect`
- `magic_weapon`

The effect object is otherwise a freeform data container consumed by the specific effect handler.

### `apply_effect`

```json
{
  "type": "apply_effect",
  "effect": "<namespace>:<mob_effect_name>",
  "duration": 600,
  "amplifier": 0
}
```

### `play_sound`

```json
{
  "type": "play_sound",
  "sound": "<namespace>:<sound_event_name>",
  "volume": 1.0,
  "pitch": 1.0,
  "category": "VOICE"
}
```

### `effigy_sound`

```json
{
  "type": "effigy_sound",
  "sound": "<namespace>:<sound_event_name>",
  "volume": 1.0,
  "pitch": 1.0,
  "category": "BLOCKS"
}
```

Behavior note:

- `effigy_sound` plays at a nearby effigy when one is found, otherwise it falls back to the player position.

### `run_command`

```json
{
  "type": "run_command",
  "command": "<command_string>"
}
```

### `send_message`

```json
{
  "type": "send_message",
  "message": "<message_text>"
}
```

### `start_conversation` and `communication`

```json
{
  "type": "start_conversation",
  "deity": "<namespace>:<deity_name>"
}
```

`communication` is handled the same way as `start_conversation`.

### `give_item`

```json
{
  "type": "give_item",
  "item": "<namespace>:<item_name>",
  "count": 1
}
```

Behavior notes:

- the effect now grants the item to the player's inventory
- if inventory is full, the item stack is dropped near the player

### `restore_mana`

```json
{
  "type": "restore_mana",
  "amount": 20.0
}
```

Behavior notes:

- uses Eidolon's `ISoul.giveMagic`
- syncs mana state to tracking clients after the restore

### `increase_max_mana`

```json
{
  "type": "increase_max_mana",
  "amount": 5.0
}
```

Behavior notes:

- increases `ISoul.maxMagic` directly
- persistent unless another system later changes max mana
- syncs mana state to tracking clients

### `modify_attribute`

```json
{
  "type": "modify_attribute",
  "attribute": "eidolonunchained:divine_resistance",
  "amount": 0.10,
  "operation": "add",
  "duration_ticks": 1200,
  "typed_resistances": {
    "dark": 0.25,
    "light": -0.10
  },
  "modifier_id": "my_pack.divine_resist_window",
  "sign_icon": "eidolon:wicked",
  "status_effect": "eidolonunchained:shadow_burden_effect",
  "particle": "minecraft:smoke",
  "particle_count": 16,
  "particle_spread": 0.35,
  "particle_speed": 0.02
}
```

Behavior notes:

- this is the generic attribute modifier effect
- supported `operation` values:
  - `add` or `addition`
  - `multiply_base`
  - `multiply_total`
- the special timed custom-effect path only activates when all of these are present:
  - `duration_ticks`
  - `sign_icon`
  - `status_effect`
  - `particle`
- if any of those four fields is missing, the modifier is applied as a permanent attribute modifier instead
- when the full timed custom-effect bundle is present, the modifier is temporary and removed after `duration_ticks`
- `modifier_id` is optional; when omitted, a deterministic ID is generated from attribute + operation + amount
- optional typed resistance fields are supported:
  - `typed_resistance_key` + `typed_resistance_amount` (single channel)
  - `typed_resistances` object for multi-channel application
- typed resistance values write to target persistent NBT under `eu_divine_resistances.<key>`
- if `duration_ticks > 0`, typed resistance additions are temporary and are automatically removed at expiry
- if `duration_ticks` is missing/0, typed resistance additions are applied as persistent tag values
- when the full timed custom-effect bundle is present, `modify_attribute` also becomes a timed chant effect, whether the amount is positive or negative
- otherwise it stays only an attribute modifier and does not create a chant-effect HUD entry
- timed chant-effect ID resolution order:
  - explicit `status_effect` if provided
  - fallback `<chant_id_path>_effect`
- that resolved ID is treated as the custom identity of the timed chant effect for HUD/sign rendering only; it is not a normal `MobEffect` and `/effect` will not find it
- `sign_icon` lets you render a registered sign sprite in the potion-effects HUD area while that timed modifier is active
- `particle` emits around the affected entity when the timed custom-effect modifier is applied
- particle options currently supported:
  - `particle_count` (default `12`)
  - `particle_spread` (default `0.3`)
  - `particle_speed` (default `0.01`)

Compatibility note:

- `modify_magic_power` remains accepted and maps to this same handler
- if no `attribute` is provided, it defaults to `eidolon:magic_power`

### `apply_cooldown`

```json
{
  "type": "apply_cooldown",
  "chant": "<namespace>:<chant_name>",
  "duration_seconds": 60
}
```

Behavior notes:

- sets an explicit cooldown for another chant ID on the same player
- `duration_seconds` defaults to `60` if omitted

### `projectile_effect`

```json
{
  "type": "projectile_effect",
  "projectile_id": "blank",
  "speed": 1.9,
  "damage": 7.0,
  "damage_type": "mymod:dark_magic"
}
```

Behavior notes:

- `damage_type` accepts a real damage type ID from the damage type registry
- compatibility aliases `magic`, `indirect_magic`, and `indirectmagic` are still accepted
- for custom damage types, the runtime resolves the registry key directly instead of forcing `indirectMagic`
- if `damage_type` is omitted, the runtime falls back to the chant's `linked_deity` and uses that deity's `deity_damage_type` when available
- if neither the effect nor the linked deity provides a damage type, the hit falls back to vanilla-style magic behavior
- to have Eidolon magic-power scaling, the configured damage type must be included in `forge:is_magic`
- divine resistance is applied when the resolved damage type is mapped in `data/*/divine_resistance_types/*.json`
- mapping schema: `{ "damage_type": "<ns>:<damage_type>", "resistance_key": "<key>", "default_resistance": 0.0 }`
- bundled defaults map `eidolonunchained:light_divine_power` -> `light` and `eidolonunchained:dark_divine_power` -> `dark`
- global reduction always includes the target's `eidolonunchained:divine_resistance` attribute
- typed reduction reads target persistent NBT at `eu_divine_resistances.<resistance_key>` (falls back to `default_resistance` when absent)
- built-in floating damage-value coloring is applied for non-dummy targets using the linked deity color when a deity-backed damage type resolves
- dummy targets are rendered by Dummmmmmy, so matching colors there require Dummmmmmy client config entries for the same damage type IDs

### `magic_weapon`

```json
{
  "type": "magic_weapon",
  "slot": "mainhand",
  "duration": 600,
  "damage_type": "mymod:dark_magic"
}
```

Behavior notes:

- converts weapon hits into magical damage events
- `damage_type` is optional; resolution order is explicit effect `damage_type` -> linked deity `deity_damage_type` -> magic-style conversion
- custom `damage_type` values are resolved through the damage type registry
- to have Eidolon magic-power scaling, the configured damage type must be included in `forge:is_magic`
- divine resistance is applied when the resolved damage type is mapped in `data/*/divine_resistance_types/*.json`
- lore pattern: set `linked_deity` on the chant and let the deity provide its own `deity_damage_type` unless the chant needs to override it explicitly

### `consume_alt_resource`

```json
{
  "type": "consume_alt_resource",
  "resource_type": "xp",
  "amount": 10
}
```

Behavior notes:

- currently implemented resource type: `xp`
- intended as the extension hook for optional compat bridges (including Iron's in a later phase)

## Cast-Time Behavior

`DatapackChantSpell` adds several gameplay behaviors that matter for authoring:

- `mana_cost` is used as the actual soul/magic cost gate.
- `cooldown` is applied after a successful cast through `ChantCooldownManager`.
- if `requires_effigy` is true, a nearby ready effigy is required or the chant fails.
- if `linked_deity` and `prayer_effect_type` are both set, the chant executes its effects and then routes into deity conversation.
- if `linked_deity` is set but `prayer_effect_type` is blank, the chant still executes effects and records deity context, but it does not automatically start a prayer conversation.

## Current Authoring Reality

- `cooldown` is the field the loader reads, not `cooldown_seconds`.
- `difficulty` may be used numerically or as a deity progression label/stage ID surrogate.
- `show_in_codex` only controls codex visibility; it does not disable casting.
- chant requirements are checked before cast, and mana plus effigy gating are checked by the spell runtime.

## Minimal Placeholder Example

```json
{
  "name": "<namespace>.chant.<chant_name>.name",
  "description": "<namespace>.chant.<chant_name>.description",
  "linked_deity": "<namespace>:<deity_name>",
  "prayer_effect_type": "conversation",
  "signs": [
    "eidolon:<sign_name>",
    "eidolon:<sign_name>",
    "eidolon:<sign_name>"
  ],
  "requirements": [],
  "effects": [
    {
      "type": "start_conversation",
      "deity": "<namespace>:<deity_name>"
    }
  ]
}
```

[Overview](Overview.md) | [AI Deity JSON](AI-Deity-JSON.md) | [Chant JSON](Chant-JSON.md) | [Deity JSON](Deity-JSON.md) | [Codex JSON](Codex-JSON.md) | [Research JSON](Research-JSON.md) | [Fate JSON](Fate-JSON.md) | [Facts JSON](Facts-JSON.md) | [Keybind Settings JSON](Keybind-Settings-JSON.md) | [Ritual And Recipe Notes](Ritual-Recipes.md) | [Localization](Localization.md)
