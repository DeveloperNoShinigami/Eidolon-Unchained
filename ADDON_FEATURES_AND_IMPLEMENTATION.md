## Eidolon Unchained Addon: Features & Implementation

### 1. Codex Categories & Chapters
- **Categories**: Expansions, Community Rituals, Custom Spells, Advanced Techniques
- **Chapters**: Rituals, Summoning Rituals, Community Rituals, Arcane Gold, Void Amulet, Monsters, Warded Mail, Crystal Ritual, etc.
- All categories and chapters are defined via datapack JSONs in `data/eidolonunchained/codex/` and `data/eidolonunchained/codex_chapters/`.
- Each category and chapter has a lang key for display, e.g. `eidolon.codex.category.expansions`.

### 2. Codex Entries
- All entries are defined in `data/eidolonunchained/codex_entries/` and subfolders.
- Each entry now uses the `"chapter"` field (not `target_chapter`) to assign it to a chapter.
- Chapter IDs are lowercase and must match the chapter JSONs.
- Example entries: Community Summoning, Ritual Binding, Expansion Content Pack, Elemental Fusion, Fire Mastery, Ice Control, Shadow Manipulation, Advanced Monster Studies, Rare Monster Variants, Arcane Gold, Void Amulet, Warded Mail, Crystal Rituals, etc.

### 3. Research System
- Research chapters: Advanced Necromancy, Arcane Gold, Crystal Ritual, Monsters, Sacred Sign, Summon Ritual, Void Amulet, Void Mastery, Wicked Sign, etc.
- Research entries: Advanced Soul Manipulation, Master Necromancer, Ritual Master, Soul Binding Mastery, Void Walker, etc.
- All research entries now use `"id"` and `"chapter"` fields (not `research_id` or `target_research`).
- Research chapters and entries have lang keys for display.

### 4. Data-Driven Integration
- All content is loaded from datapack JSONs—no hardcoded or reflection-based integration remains.
- Categories, chapters, entries, and research are fully extensible via datapacks.
- All lang keys for categories, chapters, and entries must be present in `assets/eidolonunchained/lang/en_us.json`.

### 5. Implementation Notes
- All datapack files must use consistent, lowercase IDs for chapters and categories.
- Each codex entry and research entry must reference a valid chapter.
- If an entry or chapter is missing in-game, check for typos in IDs or missing lang keys.

### 6. Example JSON Snippet
```json
{
  "chapter": "rituals",
  "title": "Fire Mastery",
  "icon": "minecraft:fire_charge",
  "pages": [
    { "type": "text", "content": "Master the ancient art of fire magic..." }
  ]
}
```

### 7. Troubleshooting
- If a category, chapter, or entry does not appear, ensure:
  - The `chapter` field matches a real chapter ID
  - All lang keys are present
  - JSON syntax is valid (no duplicate keys)
- For research, ensure `id` and `chapter` fields are correct and match the chapter JSONs.

---
For further details, see the datapack folders and lang file for all implemented content.
