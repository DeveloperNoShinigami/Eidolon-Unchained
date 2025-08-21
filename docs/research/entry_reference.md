# 📖 Research Node Field Reference

This document describes the JSON structure for individual research nodes in Eidolon Unchained. Nodes define progression steps in a research chapter and can also gate access to codex entries.

## 🧱 Core Fields

| Field | Description | Example |
|-------|-------------|---------|
| `id` | Namespaced identifier for the node. | `"eidolonunchained:void_manipulation"` from Void Manipulation【F:src/main/resources/data/eidolonunchained/research_entries/void_walker.json†L2-L3】 |
| `chapter` | ID of the research chapter containing the node. | `"eidolonunchained:void_amulet"`【F:src/main/resources/data/eidolonunchained/research_entries/void_walker.json†L3】 |
| `title` | Display name shown in the research UI. | `"Void Manipulation"`【F:src/main/resources/data/eidolonunchained/research_entries/void_walker.json†L4】 |
| `description` | Short summary of the node's purpose. | `"Learn to harness and control void energies for advanced magical purposes."`【F:src/main/resources/data/eidolonunchained/research_entries/void_walker.json†L5】 |
| `icon` | Item displayed as the node's icon. | `{ "item": "eidolon:void_amulet" }`【F:src/main/resources/data/eidolonunchained/research_entries/void_walker.json†L6】 |
| `prerequisites` | Research IDs that must be completed first. | `["eidolon:void_amulet"]`【F:src/main/resources/data/eidolonunchained/research_entries/void_walker.json†L7】 |
| `unlocks` | Research IDs enabled after completion. | `[]` in Advanced Soul Manipulation, meaning no further unlocks【F:src/main/resources/data/eidolonunchained/research_entries/advanced_soul_manipulation.json†L7-L8】 |
| `x`, `y` | Grid coordinates within the chapter screen. | `0, 0`【F:src/main/resources/data/eidolonunchained/research_entries/advanced_soul_manipulation.json†L9-L10】 |
| `type` | Difficulty tier such as `basic`, `advanced`, or `forbidden`. | `"advanced"`【F:src/main/resources/data/eidolonunchained/research_entries/advanced_soul_manipulation.json†L11】 |
| `required_stars` | Star cost required before progress can begin. | `5` stars for Advanced Soul Manipulation【F:src/main/resources/data/eidolonunchained/research_entries/advanced_soul_manipulation.json†L12】 |
| `tasks` | Tiered objectives the player must complete. | See tiers in Void Manipulation【F:src/main/resources/data/eidolonunchained/research_entries/void_walker.json†L13-L29】 |

### Optional: `additional`
Some legacy nodes wrap `tasks` inside an `additional` object, as seen in Advanced Soul Manipulation【F:src/main/resources/data/eidolonunchained/research_entries/advanced_soul_manipulation.json†L13-L24】. Newer entries place `tasks` directly at the top level.

## 📚 Chapters
Research nodes live inside chapters that group related topics and control ordering.

| Field | Description | Example |
|-------|-------------|---------|
| `id` | Namespaced identifier for the chapter. | `"eidolonunchained:advanced_necromancy"`【F:src/main/resources/data/eidolonunchained/research_chapters/advanced_necromancy.json†L2】 |
| `title` | Chapter title shown in the UI. | `"Advanced Necromancy"`【F:src/main/resources/data/eidolonunchained/research_chapters/advanced_necromancy.json†L3】 |
| `description` | Chapter overview text. | `"Chapter covering advanced necromantic techniques and forbidden knowledge"`【F:src/main/resources/data/eidolonunchained/research_chapters/advanced_necromancy.json†L4】 |
| `category` | Tab where the chapter appears, such as `signs` or `spells`. | `"signs"` in the Wicked Sign chapter【F:src/main/resources/data/eidolonunchained/research_chapters/wicked_sign.json†L5】 |
| `icon` | Item used as the chapter icon. | `{ "item": "eidolon:deathbringer_scythe" }`【F:src/main/resources/data/eidolonunchained/research_chapters/advanced_necromancy.json†L6-L7】 |
| `sort_order` | Numerical order within the category tab. | `100`【F:src/main/resources/data/eidolonunchained/research_chapters/advanced_necromancy.json†L9】 |
| `secret` | If `true`, hides the chapter until unlocked. | `true` for Wicked Sign【F:src/main/resources/data/eidolonunchained/research_chapters/wicked_sign.json†L9-L10】 |

## 🔗 Interaction with the Codex
Codex entries can require completed research nodes before becoming visible. Each codex entry lists research prerequisites, allowing research progression to unlock deeper lore or recipes. The codex reference highlights this linkage: `"prerequisites"` is a list of research IDs needed to view an entry【F:docs/codex_reference.md†L7-L16】. When a player completes a node such as `eidolonunchained:void_manipulation`, any codex entry that names this ID in its `prerequisites` array will appear in the codex.

## 🛠️ Effects and Rewards
Completing a node may trigger gameplay effects:

- **Unlocks:** New nodes become available through the `unlocks` field.
- **Star progression:** Consumed stars reduce the player's total and gate higher tiers.
- **Codex access:** Newly unlocked codex entries provide lore, recipes, or guidance.

Use these tools to build rich progression trees that tie research achievements to the in‑game codex.

