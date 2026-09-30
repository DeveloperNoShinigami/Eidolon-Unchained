# Example script pack

Copy `kubejs/` over your instance's `kubejs/` folder. Everything is declared in startup scripts (client scripts only react).

## Trying mob casting (Phase 3)

A skeleton that casts `eu_examples:shadow_bolt` at its target, from the `eu_examples:dark_caster` profile in its NBT:

```
/summon minecraft:skeleton ~ ~ ~ {ForgeData:{eidolonunchained:{v:1,caster:{profile:"eu_examples:dark_caster"}}}}
```

A plain zombie made a caster by a held item carrying a `caster_grant`:

```
/give @p minecraft:iron_sword{eidolonunchained:{v:1,caster_grant:{profile:"eu_examples:dark_caster"}}}
```

Drop the sword near a zombie (or `/summon minecraft:zombie ~ ~ ~ {HandItems:[{id:"minecraft:iron_sword",Count:1b,tag:{eidolonunchained:{v:1,caster_grant:{profile:"eu_examples:dark_caster"}}}},{}]}`).
