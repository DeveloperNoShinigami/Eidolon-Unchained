# File Structure

Complete directory layout and organization standards for the Eidolon Unchained datapack system.

## 📁 Complete Directory Structure

```
your_datapack/
├── pack.mcmeta                          # Datapack metadata
├── data/
│   └── yourmod/                         # Your mod's namespace
│       ├── codex/                       # Category-organized content
│       │   ├── category_name/           # Category folder
│       │   │   ├── _category.json       # Category definition
│       │   │   ├── chapter1.json       # Chapter in this category
│       │   │   └── chapter2.json       # Another chapter
│       │   └── other_category/          # Another category
│       │       ├── _category.json       
│       │       └── chapter.json        
│       ├── codex_entries/               # Entries that extend chapters
│       │   ├── entry1.json             # Entry targeting any chapter
│       │   ├── entry2.json             
│       │   └── subfolder/               # Optional organization
│       │       └── specialized.json    
│       ├── research_chapters/           # Research progression chapters
│       │   ├── basic_research.json     
│       │   └── advanced_research.json  
│       └── research_entries/            # Individual research tasks
│           ├── simple_task.json        
│           └── complex_task.json       
└── assets/
    └── yourmod/                         # Namespace for assets
        ├── lang/                        # Localization files
        │   ├── en_us.json              # English translations
        │   ├── es_es.json              # Spanish translations (optional)
        │   └── fr_fr.json              # French translations (optional)
        └── textures/                    # Custom images
            └── codex/                   # Codex-specific images
                ├── diagrams/            
                └── icons/               
```

## 📋 File Naming Standards

### Categories
- **Folder**: `data/yourmod/codex/category_name/`
- **Definition**: `_category.json` (always starts with underscore)
- **Naming**: Lowercase, underscores for spaces

### Chapters
- **Location**: `data/yourmod/codex/category_name/chapter_name.json`
- **Naming**: Lowercase, underscores for spaces
- **Convention**: Descriptive, topic-focused names

### Entries
- **Location**: `data/yourmod/codex_entries/entry_name.json`
- **Naming**: Descriptive of content added
- **Subfolders**: Optional for organization

### Research
- **Chapters**: `data/yourmod/research_chapters/name.json`
- **Entries**: `data/yourmod/research_entries/name.json`
- **Naming**: Reflects progression or difficulty

### Translations
- **Location**: `assets/yourmod/lang/language_code.json`
- **Format**: Standard Minecraft language codes
- **Required**: `en_us.json` at minimum

## 🏗️ Required Files

### Mandatory Files
1. **`pack.mcmeta`** - Datapack identification
2. **`assets/yourmod/lang/en_us.json`** - English translations
3. **At least one content file** (category, chapter, entry, or research)

### Recommended Files
1. **Category definition** if using category structure
2. **Chapter file** if creating new chapters
3. **README.md** for documentation

## 📄 File Content Standards

### JSON Formatting
- **Indentation**: 2 spaces (no tabs)
- **Encoding**: UTF-8
- **Line endings**: LF (Unix style)

### Translation Keys
- **Format**: `namespace.system.type.name.property`
- **Examples**:
  - `yourmod.codex.category.magic`
  - `yourmod.codex.chapter.spells.title`
  - `yourmod.research.task.description`

### Resource Locations
- **Format**: `namespace:path`
- **Examples**:
  - `minecraft:iron_sword`
  - `eidolon:void_amulet`
  - `yourmod:custom_item`

## 🗂️ Organization Strategies

### By Theme
```
codex/
├── magic/
│   ├── _category.json
│   ├── basic_spells.json
│   └── advanced_spells.json
├── combat/
│   ├── _category.json
│   ├── weapons.json
│   └── armor.json
└── exploration/
    ├── _category.json
    └── world_generation.json
```

### By Difficulty
```
codex/
├── beginner/
│   ├── _category.json
│   └── getting_started.json
├── intermediate/
│   ├── _category.json
│   └── advanced_techniques.json
└── expert/
    ├── _category.json
    └── mastery.json
```

### By Content Type
```
codex/
├── lore/
│   ├── _category.json
│   └── world_history.json
├── mechanics/
│   ├── _category.json
│   └── game_systems.json
└── recipes/
    ├── _category.json
    └── crafting_guide.json
```

## 🔧 Asset Organization

### Textures
```
assets/yourmod/textures/
├── codex/
│   ├── backgrounds/        # Page backgrounds
│   ├── diagrams/          # Explanatory diagrams
│   ├── icons/             # Custom category icons
│   └── ui/                # UI elements
├── items/                 # Item textures
└── blocks/                # Block textures
```

### Languages
```
assets/yourmod/lang/
├── en_us.json            # English (required)
├── en_gb.json            # British English
├── es_es.json            # Spanish (Spain)
├── es_mx.json            # Spanish (Mexico)
├── fr_fr.json            # French
├── de_de.json            # German
├── it_it.json            # Italian
├── pt_br.json            # Portuguese (Brazil)
├── ru_ru.json            # Russian
├── ja_jp.json            # Japanese
├── ko_kr.json            # Korean
└── zh_cn.json            # Chinese (Simplified)
```

## 📐 Size Limitations

### File Size Guidelines
- **JSON files**: Keep under 1MB each
- **Images**: Recommended 512x512 or smaller
- **Total datapack**: Under 50MB for distribution

### Content Limits
- **Pages per chapter**: No hard limit, but 20-30 is reasonable
- **Entries per chapter**: No limit
- **Categories**: Recommend 5-10 for organization

## 🔒 Naming Restrictions

### Forbidden Characters
- **Spaces**: Use underscores instead
- **Special chars**: Avoid `!@#$%^&*()+={}[]|\"':;<>?`
- **Unicode**: ASCII only for file/folder names

### Reserved Names
- **`_category`**: Reserved for category definitions
- **`pack`**: Avoid starting files with this
- **Minecraft namespaces**: Don't use `minecraft:` for your content

## ✅ Validation Checklist

### Structure Validation
- [ ] `pack.mcmeta` exists and is valid
- [ ] Namespace folders exist under `data/` and `assets/`
- [ ] Category folders contain `_category.json`
- [ ] Translation files are valid JSON
- [ ] No forbidden characters in names

### Content Validation
- [ ] All translation keys referenced exist
- [ ] Resource locations are valid
- [ ] JSON syntax is correct in all files
- [ ] Entry target chapters exist
- [ ] Research tasks are achievable

### Asset Validation
- [ ] Image files are valid PNG/JPG
- [ ] Textures are reasonable sizes
- [ ] Language files contain all referenced keys
- [ ] Custom icons work with UI scaling

## 📚 Related Documentation

- [Getting Started](getting-started.md) - Creating your first structure
- [Categories](categories.md) - Category organization details
- [Chapters](chapters.md) - Chapter structure requirements
- [Translations](translations.md) - Localization best practices
