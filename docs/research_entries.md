# 📚 Research Entry Reference

This guide explains how to define research entries for Eidolon Unchained. Each entry extends the research tree with custom progression nodes loaded by the [`ResearchDataManager`](../src/main/java/com/bluelotuscoding/eidolonunchained/data/ResearchDataManager.java).

## 🔑 Field Definitions

| Field | Description |
|-------|-------------|
| `id` | Unique resource ID for the entry. |
| `chapter` | Chapter ID the entry belongs to. |
| `icon` | Item used as the entry's icon (supports `item`, optional `count` and `nbt`). |
| `prerequisites` | Array of research IDs required before this entry unlocks. |
| `unlocks` | Research IDs made available after completion. |
| `type` | Entry category: `basic`, `advanced`, `forbidden`, `ritual`, or `crafting`. |
| `x`, `y` | Coordinates on the research screen grid. |
| `conditions` | Optional conditional requirements (dimension, biome, player state, etc.). Implemented via dedicated condition classes. |
| `tasks` | Objectives the player must complete. Each task uses a specific task class such as `KillEntitiesTask`, `CollectItemsTask`, or `UseRitualTask`. |
| `rewards` | Extra benefits granted on completion (items, advancements, unlocks). |

## ⭐ Star Requirements

The `required_stars` value gates access to higher‑tier research. Star costs should scale with entry complexity and reward.

## 📊 Task Tiers

Tasks are grouped into difficulty tiers:

- `tier_1` – introductory tasks
- `tier_2` – intermediate challenges
- `tier_3` – end‑game objectives

Higher tiers may require more stars or advanced resources. Task classes handle validation and completion logic.

## ⚖️ Balancing Tips

- Start with low star costs and simple tasks, increasing difficulty gradually.
- Mix task types (combat, crafting, exploration) to keep progression varied.
- Ensure rewards justify the required stars and effort.
- Use conditions to gate entries to specific environments or player states.

## 🔗 Related Code

- [`ResearchDataManager`](../src/main/java/com/bluelotuscoding/eidolonunchained/data/ResearchDataManager.java): loads chapters and entries from datapacks.
- Task implementations (e.g., `ResearchTask` and its subclasses) validate tiered objectives.
- Condition classes evaluate context such as biome, time, or player properties before tasks become available.

