# Chantable Mobs JSON

[Overview](Overview.md) | [AI Deity JSON](AI-Deity-JSON.md) | [Chant JSON](Chant-JSON.md) | [Deity JSON](Deity-JSON.md) | [Codex JSON](Codex-JSON.md) | [Research JSON](Research-JSON.md) | [Fate JSON](Fate-JSON.md) | [Facts JSON](Facts-JSON.md) | [Keybind Settings JSON](Keybind-Settings-JSON.md) | [Ritual And Recipe Notes](Ritual-Recipes.md) | [Localization](Localization.md)

Source of truth: `ChantableMobManager` and `MobChantCastingGoal`.

## Path

`ChantableMobManager` reads `data/<namespace>/chantable_mobs/<file>.json`.

Each file configures one mob type. The filename is not significant — the entity is identified by the `mob` field inside the JSON.

## Full Authoring Shape

```json
{
  "mob": "minecraft:skeleton",
  "assigned_deity": "<namespace>:<deity_name>",
  "starting_title": "Shadow Touched",
  "enabled": true,
  "chant_rotation_ids": [
    "<namespace>:<chant_id>",
    "<namespace>:<chant_id>"
  ],
  "chant_interval_ticks": 60,
  "min_range": 6.0,
  "max_range": 18.0,
  "mob_combat_policy": {
    "allow_native_combat_ai": false,
    "movement_speed": 0.35,
    "max_threshold_casts_per_cycle": 2,
    "role_order": ["movement", "offense", "cc", "support", "defense"],
    "support_hp_thresholds": {
      "self": { "below": 0.45, "chant": "<namespace>:<chant_id>" },
      "ally": { "below": 0.55, "chant": "<namespace>:<chant_id>" }
    },
    "support_mana_thresholds": {
      "self": { "below": 0.30, "chant": "<namespace>:<chant_id>" },
      "ally": { "below": 0.35, "chant": "<namespace>:<chant_id>" }
    }
  }
}
```

## Field Notes

### Required fields

| Field | Notes |
|---|---|
| `mob` | Entity type resource location, e.g. `minecraft:skeleton`. Aliases: `entity`, `entity_id`. |
| `assigned_deity` | Resource location of the deity this mob belongs to. Aliases: `deity_id`, `patron_id`, `god_id`. |
| `starting_title` | Faith title assigned when the mob spawns. Must not be empty. Aliases: `title`, `starting_rank`. |
| `chant_rotation_ids` | Array of chant resource locations the mob will cast. Alias: `chant_ids`. At least one entry required. |

### Optional top-level fields

| Field | Default | Notes |
|---|---|---|
| `enabled` | `true` | Set `false` to disable this config without deleting the file. |
| `chant_interval_ticks` | `20` | Minimum ticks between casts. Clamped to `≥ 1`. Alias: `interval_ticks`. |
| `min_range` | `2.5` | Minimum distance to target before the mob will cast. Alias: `chant_rotation_min_range`. |
| `max_range` | `16.0` | Maximum distance to target. Alias: `chant_rotation_max_range`. |
| `mob_combat_policy` | defaults below | Configures full combat AI behavior. See section below. |

---

## mob_combat_policy

The `mob_combat_policy` block is the heart of the mob combat AI. All fields inside it are optional and fall back to safe defaults when omitted.

### Policy fields

| Field | Default | Notes |
|---|---|---|
| `allow_native_combat_ai` | `false` | When `false`, vanilla melee, ranged bow, and crossbow attack goals are stripped on join so the mob fights exclusively through chants. Set `true` to keep vanilla attacks alongside chant casting. |
| `movement_speed` | `0.35` | Speed bonus applied as a `MULTIPLY_TOTAL` modifier while the mob has a valid combat target. `0.35` = 35% faster. Set to `0.0` for no bonus. |
| `max_threshold_casts_per_cycle` | `2` | Maximum number of threshold-triggered (support/hp/mana) chants allowed per role cycle before returning to normal priority. |
| `role_order` | `["defense","support","cc","movement","offense","melee"]` | Ordered list of combat roles to cycle through. Each cast advances to the next role in the list. Only roles that are range-eligible at the time of casting are considered. See Role Order section below. |
| `support_hp_thresholds` | none | Optional override chants triggered when HP falls below a fraction. See Thresholds section. |
| `support_mana_thresholds` | none | Optional override chants triggered when mana falls below a fraction. See Thresholds section. |

---

## Role Order

`role_order` defines the behavioral cycle the mob follows each cast. It is not a priority list — the mob advances through the list sequentially, one role per cast.

Each chant in the mob's `chant_rotation_ids` must declare a `combat_role` in its own chant JSON (see [Chant JSON](Chant-JSON.md)). The mob planner matches chants to the current role slot before casting.

### Valid role values

| Role | Casting zone | Behavior |
|---|---|---|
| `offense` | Between `min_range` and `max_range` | Offensive damage spells. |
| `defense` | Between `min_range` and `max_range` | Protective / defensive buffs. |
| `support` | Between `min_range` and `max_range` | Buff or heal allies (or self via threshold rules). |
| `cc` | Between `min_range` and `max_range` | Crowd-control spells (roots, slows, etc.). |
| `melee` | Distance `≤ min_range` | Close-range strikes. Mob must be within melee reach. |
| `movement` | Repositioning zone — see notes | Teleport / dash / gap-closer spells. Eligible zone depends on whether the mob also has a `melee` role in `role_order` (see below). |

### Movement role eligibility zones

The `movement` role uses range-aware eligibility so the mob only repositions when it actually needs to:

| Mob archetype | Movement eligible when |
|---|---|
| Pure ranged (no `melee` in role_order) | Target is **beyond** `max_range` only. Does not reposition when target is already in range. |
| Melee or hybrid (has `melee` in role_order) | Target is **outside** the casting band (beyond `max_range` or closer than `min_range`). |

### How cycling works

1. On each cast opportunity, the planner reads the current `role_order_index` from the mob's NBT.
2. Starting from that index it scans forward through the role list.
3. The first role that is range-eligible AND has at least one ready chant (not on cooldown, enough mana) wins.
4. That chant is cast, and the `role_order_index` advances by one (wrapping around).
5. If all roles are blocked this tick, the planner waits for the inter-cast cooldown and retries.

Because the index is persisted to NBT, the cycle is maintained across goal deactivations and chunk unloads.

---

## Thresholds

`support_hp_thresholds` and `support_mana_thresholds` let you define emergency override chants that fire when the mob (or a nearby ally) falls below a critical resource level. These chants interrupt the normal role cycle.

```json
"support_hp_thresholds": {
  "self": { "below": 0.45, "chant": "<namespace>:<heal_self_chant>" },
  "ally": { "below": 0.55, "chant": "<namespace>:<heal_ally_chant>" }
},
"support_mana_thresholds": {
  "self": { "below": 0.30, "chant": "<namespace>:<restore_mana_self_chant>" },
  "ally": { "below": 0.35, "chant": "<namespace>:<restore_mana_ally_chant>" }
}
```

| Field | Type | Notes |
|---|---|---|
| `below` | float `0.0–1.0` | Fraction of max resource. `0.45` = triggers when below 45%. |
| `chant` | resource location | The chant to cast when the threshold is met. Must be in the mob's `chant_rotation_ids`. |

Both `self` and `ally` are optional. Omit either to disable that half of the threshold check.

`max_threshold_casts_per_cycle` limits how many threshold chants fire per role cycle to prevent infinite healing loops.

---

## Combat AI Behavior

When `allow_native_combat_ai: false`, `MobChantCastingGoal` takes over the mob's entire combat behavior:

- **Hard facing lock**: the mob's yaw, head yaw, and body rotation are force-set to face the target every tick while casting or between casts. Movement roles exempt the mob from the facing requirement during sign-build so the mob can reposition freely.
- **Strafing**: while in range and not on a movement spell, the mob strafes laterally relative to the target. Ranged mobs orbit wider; melee mobs orbit tighter.
- **Speed bonus**: `movement_speed` is applied as a `MULTIPLY_TOTAL` attribute modifier the moment the mob acquires a target and removed when it loses one.
- **Line-of-sight**: casts always require LOS. Movement spells do not require facing before building signs.

---

## Runtime Behavior

### Config refresh on join

Every time a chantable mob enters the world, the goal compares the mob's persisted NBT against the live config. If anything differs, it overwrites:

- Rotation IDs (and clamps the index)
- Stale cooldowns (cleared on rotation change)
- Interval, range, policy fields

Config changes take effect on existing mobs the next time they enter the world — no `/kill` required.

### Mana

Chants cost mana as defined in their own chant JSON. The mob is given a mana pool on join. A chant is skipped if its mana cost cannot be met; the planner continues to the next eligible chant.

### Enthrall interaction

If the mob is enthralled, the chant system coexists with Eidolon's thrall target goals. Chanting takes priority; the mob still follows and defends its owner between casts.

---

## Full Example — Skeleton (pure ranged caster)

```json
{
  "mob": "minecraft:skeleton",
  "assigned_deity": "eidolonunchained:dark_deity",
  "starting_title": "Shadow Touched",
  "chant_rotation_ids": [
    "eidolonunchained:modded_projectiel_debug",
    "eidolonunchained:irons_cast_teleport_movement_debug",
    "eidolonunchained:irons_cast_root_cc_debug",
    "eidolonunchained:magic_power_surge_support_debug"
  ],
  "chant_interval_ticks": 60,
  "min_range": 6.0,
  "max_range": 18.0,
  "mob_combat_policy": {
    "allow_native_combat_ai": false,
    "movement_speed": 0.35,
    "max_threshold_casts_per_cycle": 2,
    "role_order": ["movement", "offense", "cc", "support", "defense"],
    "support_hp_thresholds": {
      "self": { "below": 0.45, "chant": "eidolonunchained:heal_self" },
      "ally": { "below": 0.55, "chant": "eidolonunchained:heal_ally" }
    },
    "support_mana_thresholds": {
      "self": { "below": 0.30, "chant": "eidolonunchained:restore_mana_self" },
      "ally": { "below": 0.35, "chant": "eidolonunchained:restore_mana_ally" }
    }
  }
}
```

## Full Example — Zombie (melee hybrid)

```json
{
  "mob": "minecraft:zombie",
  "assigned_deity": "eidolonunchained:dark_deity",
  "starting_title": "Shadow Touched",
  "chant_rotation_ids": [
    "eidolonunchained:irons_cast_flaming_strike_melee_debug",
    "eidolonunchained:irons_cast_teleport_movement_debug",
    "eidolonunchained:irons_cast_root_cc_debug",
    "eidolonunchained:magic_power_surge_support_debug"
  ],
  "chant_interval_ticks": 50,
  "min_range": 2.5,
  "max_range": 10.0,
  "mob_combat_policy": {
    "allow_native_combat_ai": false,
    "movement_speed": 0.35,
    "max_threshold_casts_per_cycle": 2,
    "role_order": ["movement", "melee", "cc", "support", "defense"]
  }
}
```

## Disabling a config without deleting

Set `"enabled": false` to make the loader skip the file entirely. The mob will behave like a normal unaffected mob.

```json
{
  "mob": "minecraft:skeleton",
  "enabled": false,
  "assigned_deity": "eidolonunchained:dark_deity",
  "starting_title": "Shadow Touched",
  "chant_rotation_ids": []
}
```
