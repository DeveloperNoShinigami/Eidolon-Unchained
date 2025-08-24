# Datapack Structure Guide

**Organize your content files correctly for the mod to find them**

## Required Folder Structure

Your datapack must follow this exact three-tier structure for Eidolon Unchained integration:

```
data/
└── your_mod_id/
    ├── codex/                    # Categories and chapters
    │   ├── category1/
    │   │   ├── _category.json    # Category definition
    │   │   ├── chapter1.json     # Chapter definition
    │   │   └── chapter2.json     # Another chapter
    │   └── category2/
    │       ├── _category.json
    │       └── chapter3.json
    │
    ├── codex_entries/            # Individual entry content
    │   ├── entry1.json           # Entry targeting a chapter
    │   ├── entry2.json
    │   └── subfolder/
    │       └── entry3.json
    │
    └── eidolon_research/         # Research discovery files
        ├── research1.json
        ├── research2.json
        └── subdirectory/
            └── research3.json
```

## Three-Tier Hierarchy

### 1. Categories (`codex/category_name/_category.json`)
Define main sections of the codex

### 2. Chapters (`codex/category_name/chapter_name.json`)  
Define subsections within categories

### 3. Entries (`codex_entries/entry_name.json`)
Define actual content pages that target chapters

## Folder Locations Explained

### `data/your_mod_id/`
- Replace `your_mod_id` with your actual mod namespace
- Examples: `data/mymod/`, `data/cool_magic/`, `data/necromancy_plus/`
- Must be lowercase, no spaces, underscores and numbers OK

### `codex/`
- Contains category and chapter definitions
- Each category gets its own subdirectory
- `_category.json` defines the category properties
- Other `.json` files define chapters within that category

### `codex_entries/`
- Contains individual entry content files
- Can have subdirectories for organization
- Files must end in `.json`
- Each file = one entry that targets a specific chapter

### `eidolon_research/`  
- Contains research discovery files
- Can have subdirectories for organization
- Files must end in `.json`
- Each file = one research configuration

## Working Example Structure

Here's a complete example for a mod called "necromancy_expanded":

```
data/
└── necromancy_expanded/
    ├── codex/
    │   ├── undead_studies/
    │   │   ├── _category.json          # Undead Studies category
    │   │   ├── basic_undead.json       # Basic Undead chapter
    │   │   └── advanced_undead.json    # Advanced Undead chapter
    │   ├── magical_theory/
    │   │   ├── _category.json          # Magical Theory category
    │   │   ├── death_magic.json        # Death Magic chapter
    │   │   └── soul_manipulation.json  # Soul Manipulation chapter
    │   └── practical_applications/
    │       ├── _category.json          # Practical Applications category
    │       ├── rituals.json            # Rituals chapter
    │       └── tools.json              # Tools chapter
    │
    ├── codex_entries/
    │   ├── undead/
    │   │   ├── zombie_anatomy.json     # → targets "basic_undead"
    │   │   ├── skeleton_study.json     # → targets "basic_undead"
    │   │   └── wraith_lore.json        # → targets "advanced_undead"
    │   ├── magic/
    │   │   ├── death_signs.json        # → targets "death_magic"
    │   │   └── soul_binding.json       # → targets "soul_manipulation"
    │   └── items/
    │       ├── soul_gems.json          # → targets "tools"
    │       └── bone_tools.json         # → targets "tools"
    │
    └── eidolon_research/
        ├── undead/
        │   ├── zombie_research.json
        │   ├── skeleton_research.json
        │   └── wraith_encounter.json
        ├── elemental/
        │   ├── fire_spirit.json
        │   └── ice_wraith.json
        └── advanced/
            ├── lich_study.json
            └── master_necromancy.json
```

## File Naming Conventions

### Codex Entries
- Use descriptive names: `zombie_anatomy.json`, `fire_rituals.json`
- Match the content theme
- Avoid spaces, use underscores
- Be consistent across related entries

### Research Files
- Use action-oriented names: `zombie_research.json`, `fire_encounter.json`
- Indicate what triggers them
- Group related research in subdirectories
- Keep names short but clear

## Common Mistakes

❌ **Wrong folder names**
```
data/mymod/codex/           # Should be codex_entries
data/mymod/research/        # Should be eidolon_research
```

❌ **Wrong file locations**
```
data/codex_entries/mymod/   # Mod ID comes first
assets/mymod/codex_entries/ # Should be in data, not assets
```

❌ **Case sensitivity issues**
```
data/MyMod/                 # Should be lowercase: mymod
data/mymod/Codex_Entries/   # Should be lowercase: codex_entries
```

❌ **Missing namespace**
```
data/codex_entries/         # Missing mod ID namespace
data/eidolon_research/      # Missing mod ID namespace
```

## Integration with Existing Mods

If you're adding to an existing mod's datapack:

1. **Find the mod's namespace** - Look in their existing data folder
2. **Use their structure** - Follow their existing organization
3. **Avoid conflicts** - Don't overwrite their files
4. **Test carefully** - Make sure your additions don't break their content

## Datapack.mcmeta

Don't forget to include a proper `pack.mcmeta` file in your datapack root:

```json
{
  "pack": {
    "pack_format": 18,
    "description": "Eidolon Unchained content for MyMod"
  }
}
```

## Validation

To check if your structure is correct:

1. **Check folder names** - Exactly `codex_entries` and `eidolon_research`
2. **Verify mod namespace** - Consistent throughout your datapack
3. **Test file discovery** - Load the pack and check if entries appear
4. **Validate JSON** - Make sure all files are valid JSON format

## File Templates

### Minimal Codex Entry
```json
{
  "name": "entry_name",
  "category": "category_name", 
  "targets": ["entity:minecraft:zombie"],
  "pages": [
    {"type": "title", "title": "Entry Title"}
  ]
}
```

### Minimal Research File
```json
{
  "id": "research_name",
  "stars": 2,
  "triggers": ["entity:minecraft:zombie"],
  "tasks": {
    "1": [{"type": "item", "item": "minecraft:rotten_flesh", "count": 3}]
  },
  "rewards": [
    {"type": "sign", "sign": "death"}
  ]
}
```