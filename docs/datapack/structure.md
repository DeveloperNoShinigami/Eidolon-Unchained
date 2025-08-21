# 📁 Datapack Structure

Eidolon Unchained datapacks follow the standard Minecraft pack layout.  The `data/` folder
contains gameplay JSON while `assets/` stores language and other client resources.

## `data/`

```text
📦 data/
└── 📁 eidolonunchained/                # Your namespace
    ├── 📁 codex/                       # Category folders for new codex tabs
    │   └── 📁 custom_spells/           # Example category folder
    │       ├── 📄 _category.json       # Category metadata
    │       └── 📄 fire_mastery.json    # Example entry inside the category
    ├── 📁 codex_entries/               # 📖 Pages for built-in categories
    │   └── 📄 ritual_mastery.json      # Example codex entry
    ├── 📁 codex_chapters/              # Optional codex chapter definitions
    │   └── 📄 mythology.json           # Example chapter file
    ├── 📁 research_chapters/           # Optional research chapter definitions
    │   └── 📄 void_alchemy.json        # Example research chapter
    └── 📁 research_entries/            # 🔬 Research nodes live here
        └── 📄 ritual_master.json       # Example research entry
```

*Use `codex_entries/` to add pages to existing categories.  Place new categories
and their entries under `codex/` in folders with a `_category.json` file.
`research_entries/` hold individual research nodes, while `research_chapters/`
define the chapters that group them.*

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
