# 📁 Datapack Structure

Eidolon Unchained datapacks follow the standard Minecraft pack layout.  The `data/` folder
contains gameplay JSON while `assets/` stores language and other client resources.

## `data/`

```text
📦 data/
└── 📁 eidolonunchained/                # Your namespace
    ├── 📁 codex/                       # Category folders with `_category.json`
    │   └── 📁 custom_spells/           # Example category folder
    │       └── 📄 _category.json       # Category definition file
    ├── 📁 codex_chapters/              # Optional new chapter definitions
    │   └── 📄 mythology.json           # Example chapter file
    ├── 📁 codex_entries/               # 📖 Codex pages live here
    │   └── 📄 ritual_mastery.json      # Example codex entry
    └── 📁 research_entries/            # 🔬 Research nodes live here
        └── 📄 ritual_master.json       # Example research entry
```

*`codex/`, `codex_entries/`, and `research_entries/` hold the JSON that adds new
categories, pages, and progression to the mod.*

Example `custom_spells/_category.json`:

```json
{
  "key": "custom_spells",
  "name": "eidolonunchained.codex.category.custom_spells",
  "icon": "minecraft:enchanted_book",
  "color": "0x4169E1",
  "description": "Community-created magical techniques"
}
```

## `assets/`

```text
📦 assets/
└── 📁 eidolonunchained/                # Same namespace as above
    └── 📁 lang/
        └── 📄 en_us.json               # Translation keys for codex & research
```

Put any additional textures, models, or sounds under `assets/<namespace>/` as needed.
Translation keys referenced by your codex and research files belong in the language
JSON shown above.

For more detailed explanations of the JSON formats see:
- [Codex Reference](../codex_reference.md)
- [Research Entries](../research_entries.md)
