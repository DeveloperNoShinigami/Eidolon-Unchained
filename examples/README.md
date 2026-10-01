# Example script pack

Copy `kubejs/` over your instance's `kubejs/` folder. Everything is declared in startup scripts (client scripts only react).

## Layout

The scripts sit in an `eu_examples/` folder under each script type (KubeJS loads subfolders recursively), split by god:
`common/` holds what is shared or not tied to one god, and `myrkul/`, `light/` and `dark/` hold everything about that god.

| Folder | startup_scripts | server_scripts |
|---|---|---|
| `common/` | `smoke.js`, `signs_runes.js` (Storm sign, Tempest rune), `research.js` (Necromantic Rites), `chants.js` (call_storm), `rites.js` (Storm Rite, Renunciation), `codex.js` (Storm and pantheon chapters) | `smoke.js`, `chants.js` (call_storm and command chant recipes, chant events, golden sword imbue), `rituals.js` (Storm Rite, crafting ritual, Renunciation), `discoveries.js` (sea_memory, deep_dark_whispers, storm_rite_favor, discovery events), `followers.js`, `player_helpers.js` |
| `myrkul/` | `deity.js`, `chants.js` (bone_shield, grave_curse, myrkul_prayer, bone_volley), `rites.js` (Pact of the Grave), `avatar.js` (avatar and bone spear), `hexblade.js` (bone blade), `codex.js` (Necromancy category and chapter) | `recipes.js` (chant, imbue, protect and Pact recipes), `discoveries.js` (forgotten_lore, thrall_master, fortress_omen, myrkul_calls) |
| `light/` | `deity.js`, `avatar.js`, `rites.js` (Vow of Light) | `recipes.js` (Vow of Light), `discoveries.js` (light_calls, bane_of_the_grave) |
| `dark/` | `deity.js`, `avatar.js`, `rites.js` (Vow of Night), `chants.js` (dark_rebuke, shadow_bolt, the dark_caster profile) | `recipes.js` (chant and Vow of Night recipes), `discoveries.js` (dark_calls) |

`client_scripts/eu_examples/common/` holds the client smoke test and the codex event. Scripts of one type share one
scope, so a file that needs the condition library repeats `var C = EidolonUnchained.conditions`; one event (recipes,
discoveries, entity registry) may be handled in several files.

## Trying mob casting (Phase 3)

A skeleton that casts `eu_examples:shadow_bolt` at its target, from the `eu_examples:dark_caster` profile in its NBT:

```
/summon minecraft:skeleton ~ ~ ~ {ForgeData:{eidolonunchained:{v:1,caster:{profile:"eu_examples:dark_caster"}}}}
```

A plain zombie made a caster by a held item carrying a `caster_grant`:

```
/give @p minecraft:iron_sword{eidolonunchained:{v:1,caster_grant:{profile:"eu_examples:dark_caster"}}}
```

Vanilla zombies only pick items up when they spawned with `CanPickUpLoot:1b` (`/summon minecraft:zombie ~ ~ ~ {CanPickUpLoot:1b}`), and a held chant only counts for a mob when it has a `.targetCast` or `.mobCast` (call_storm has neither; grave_curse and shadow_bolt do). Drop the sword near such a zombie (or `/summon minecraft:zombie ~ ~ ~ {HandItems:[{id:"minecraft:iron_sword",Count:1b,tag:{eidolonunchained:{v:1,caster_grant:{profile:"eu_examples:dark_caster"}}}},{}]}`).

## Trying imbued weapons and Deity's Protection (Phase 3, D35)

Layout at Eidolon's worktable: the **weapon in the centre of the 3×3**, a **written chant scroll in any other slot**
(a reagent slot or the ring of the 3×3), and **soul shards in the remaining slots**, at least as many shard slots as the
chant's `.imbueCost` (call_storm: 2; Eidolon's own chants: config `imbueShardsDefault`, 4). One shard is taken from every
filled slot, so use exactly the cost. Any sword/axe/trident, anything in `#eidolonunchained:imbuable`, or a scripted
hexblade can be imbued.

The scroll must be **written**: a chant scroll from the creative menu is blank. Write it at the Scriptorium (parchment +
the signs), or take one from the command:

```
/eu scroll eu_examples:call_storm
```

Holding the weapon: **right-click casts** the active chant (same wind-up and ring as chanting), **sneak + right-click** switches chants when the weapon holds more than one (config `maxImbuedChants`); with a single chant sneak + right-click stays the weapon's own use, so a hexblade still awakens. Hold Left Alt over the tooltip to see the
chants and their signs. The dev log says why a layout gives no result (`worktable imbue: no result: …`, DEBUG).

One chant per weapon: once a weapon carries a chant, the scroll of any other chant does nothing on it (config
`maxImbuedChants`, default 1). Two levels, both through Eidolon's chant scroll (nothing to script for the scroll itself:
chants declared in startup scripts are already writable at the Scriptorium). The recipe above is the **global default**
for every imbuable weapon that no scripted recipe names.
A **scripted recipe** pins a weapon (item or tag) to a chant; the table then only accepts the scroll of that chant for
that weapon, and a scroll of any other chant does nothing on it (server script; `shards` -1 = the chant's own cost):

```js
ServerEvents.recipes(event => {
    event.recipes.eidolonunchained.imbue('eu_examples:call_storm', 'minecraft:golden_sword', 2)
    event.recipes.eidolonunchained.imbue('eu_examples:grave_curse', 'eu_examples:bone_blade', 3)   // several lines = several allowed chants
    event.recipes.eidolonunchained.protect('eu_examples:grave_curse', '#minecraft:chest_armor', 1)
})
```

Shortcut for testing (no table):

```
/give @p minecraft:iron_sword{eidolonunchained:{v:1,caster_grant:{chants:["eu_examples:call_storm","eu_examples:grave_curse"]}}}
```

Deity's Protection: enchant a helmet/chest/legs/boots/shield (enchanting table or loot chest), then at the worktable put
the piece in the centre, the scroll of a **deity-bound** chant (`/eu scroll eu_examples:grave_curse`) in a reagent slot and
one soul shard (its `.protectionCost`). Wear it and get hit: the chant's targeted path fires at the attacker, the wearer
pays the level's mana share, and the signs flash over the wearer. Test shortcut:

```
/give @p minecraft:iron_chestplate{Enchantments:[{id:"eidolonunchained:deitys_protection",lvl:2}],eidolonunchained:{v:1,protection:{chant:"eu_examples:grave_curse",deity:"eu_examples:myrkul"}}}
```

## Trying a scripted hexblade (Phase 3, Hexblades Renewed's system with any deity)

`eu_examples:bone_blade` is bound to Myrkul. Right-click awakens it (only when fully charged); awakened it drains energy each tick, hits restore some, and the extra damage scales with your Myrkul devotion. Hold Shift on the tooltip for the numbers. It can also be imbued like any weapon (then right-click casts; sneak + right-click awakens).

```
/give @p eu_examples:bone_blade
```

## Deity avatars and a GeckoLib projectile (EntityJS)

The three studio models are registered through EntityJS (`startup_scripts/eu_examples/{myrkul,light,dark}/avatar.js`); the geo/animation files were
converted from the Blockbench projects with `knowledge/deity-models/tools/bb2gecko.py`. Idle loops; the other clips are
triggerable controllers named after the clip.

```
/summon eu_examples:myrkul_avatar
```

```
/summon eu_examples:dark_avatar
```

```
/summon eu_examples:light_avatar
```

Play a clip from a server script: `entity.triggerAnim('manifest', 'manifest')` (controller name, trigger name), e.g.
`EntityEvents.spawned('eu_examples:myrkul_avatar', e => e.entity.triggerAnim('manifest', 'manifest'))`.

The bone spear is a GeckoLib projectile (`eu_examples:bone_spear`) thrown by the Myrkul chant `bone_volley`
(soul, wicked, soul): `EidolonUnchained.projectile(caster).entity('eu_examples:bone_spear').speed(1.8).shoot()`.

## Conditions, events and discoveries (Phase 4)

`server_scripts/eu_examples/*/discoveries.js`: walk into any ocean for the first test (a fact and an action-bar line); the others
need the Deep Dark, a zombie named "The Forgotten" (`/summon minecraft:zombie ~ ~ ~ {CustomName:'"The Forgotten"'}`),
the Storm Rite, Eidolon's enthrall chant on an undead, or a Nether fortress with 10 Myrkul reputation (Eidolon caps the Dark at 3 until "Sacrifice Mob" is known).

```
/eu discoveries list
```

```
/eu discoveries reset
```

Conditions also gate the Storm Rite and `call_storm` (not in the Nether) and Myrkul's reaper stage (needs Necromantic Rites).
