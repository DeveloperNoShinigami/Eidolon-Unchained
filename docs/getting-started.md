# Quick Start Guide

Get up and running with the Eidolon Unchained datapack system in just a few minutes!

## 🚀 What You'll Create

By the end of this guide, you'll have:
- A custom category in the Eidolon codex
- A chapter with basic content
- An entry that adds additional pages
- Proper translations

## 📋 Prerequisites

- Minecraft with Forge 1.20.1
- Eidolon Repraised mod installed
- Eidolon Unchained mod installed
- Basic understanding of JSON files

## 🛠️ Step 1: Create Your Datapack

Create a new datapack folder structure:

```
your_datapack/
├── pack.mcmeta
└── data/
    └── yourmod/
        ├── codex/
        ├── codex_entries/
        └── assets/
            └── yourmod/
                └── lang/
```

**pack.mcmeta**:
```json
{
  "pack": {
    "pack_format": 15,
    "description": "My First Eidolon Unchained Content"
  }
}
```

## 📁 Step 2: Create a Category

**File**: `data/yourmod/codex/mystical_studies/_category.json`
```json
{
  "key": "mystical_studies",
  "name": "yourmod.codex.category.mystical_studies",
  "icon": "minecraft:enchanted_book",
  "color": "0x9966FF",
  "description": "Advanced studies in mystical arts"
}
```

## 📖 Step 3: Create a Chapter

**File**: `data/yourmod/codex/mystical_studies/crystal_magic.json`
```json
{
  "title": "yourmod.codex.chapter.crystal_magic",
  "icon": "minecraft:amethyst_shard"
}
```

**Note**: Chapters are lightweight definitions with just title and icon. Content goes in separate entries.

## 📝 Step 4: Add an Entry

**File**: `data/yourmod/codex_entries/crystal_basics.json`
```json
{
  "target_chapter": "crystal_magic",
  "pages": [
    {
      "type": "title",
      "text": "yourmod.codex.entry.crystal_basics.title"
    },
    {
      "type": "text",
      "text": "yourmod.codex.entry.crystal_basics.description"
    },
    {
      "type": "recipe",
      "recipe": "minecraft:spyglass"
    }
  ]
}
```

## 🌐 Step 5: Add Translations

**File**: `assets/yourmod/lang/en_us.json`
```json
{
  "yourmod.codex.category.mystical_studies": "Mystical Studies",
  
  "yourmod.codex.chapter.crystal_magic": "Crystal Magic",
  "yourmod.codex.chapter.crystal_magic.intro": "Crystals hold immense magical potential when properly understood and manipulated.",
  
  "yourmod.codex.entry.crystal_basics.title": "Crystal Fundamentals",
  "yourmod.codex.entry.crystal_basics.description": "The basic principles of crystal magic begin with understanding the natural resonance frequencies of different crystal types."
}
```

## 🎮 Step 6: Test Your Datapack

1. Place your datapack in `.minecraft/saves/[world]/datapacks/`
2. Run `/reload` in-game
3. Open the Eidolon codex
4. Look for your "Mystical Studies" category
5. Open the "Crystal Magic" chapter

You should see:
- Your custom category with the enchanted book icon
- The "Crystal Magic" chapter inside it
- Multiple pages including your entry's content
- The spyglass recipe from your entry

## ✅ What You've Learned

- **Categories** organize content with custom icons and colors
- **Chapters** contain the main content within categories  
- **Entries** add additional pages to existing chapters
- **Translations** make everything display properly in-game

## 🔄 Next Steps

Now that you have the basics working:

1. **Add more content**: Create additional entries targeting your chapter
2. **Explore page types**: Try entity pages, ritual pages, etc.
3. **Add research**: Create progression requirements
4. **Study examples**: Look at the [Examples](examples.md) for more ideas

## 🆘 Troubleshooting

### Content Not Appearing?
- Check console for JSON errors
- Verify file paths match exactly
- Ensure translation keys exist
- Run `/reload` after changes

### Category Missing?
- Check `_category.json` syntax
- Verify category folder structure
- Look for duplicate category keys

### Chapter Empty?
- Verify entry `target_chapter` matches chapter name
- Check that chapter file exists
- Confirm JSON syntax is valid

## 📚 Learn More

- [System Overview](system-overview.md) - Understand how everything connects
- [Page Types](page-types.md) - All available page types
- [Research System](research-system.md) - Add progression requirements
- [Examples](examples.md) - Complete working examples

**Ready to dive deeper?** Check out the [System Overview](system-overview.md) to understand how all the pieces fit together!
