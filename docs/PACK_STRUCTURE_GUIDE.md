# Minecraft Datapack and Resource Pack Structure Guide

## Datapack Structure

```
my_datapack/
├── pack.mcmeta
└── data/
    └── eidolonunchained/
        ├── codex_entries/
        │   ├── advanced_monsters.json
        │   ├── ...
        └── research_entries/
            ├── ritual_master.json
            └── ...
```
- Only the `data/` folder and `pack.mcmeta` at the root.
- No `assets/` folder in a datapack.

---

## Resource Pack Structure

```
my_resource_pack/
├── pack.mcmeta
└── assets/
    └── eidolonunchained/
        └── lang/
            └── en_us.json
        └── textures/
            └── ...
```
- Only the `assets/` folder and `pack.mcmeta` at the root.
- No `data/` folder in a resource pack.

---

## Key Points
- Keep datapacks and resource packs separate for Minecraft to load them correctly.
- Each should have its own `pack.mcmeta` file.
- When developing a mod, you can have both in your `src/main/resources/`, but for distribution, split them as above.
