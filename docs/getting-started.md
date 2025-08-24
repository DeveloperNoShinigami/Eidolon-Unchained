# Getting Started with Eidolon Unchained

**Create your first custom codex entry in 5 minutes!**

## Prerequisites

- Eidolon Unchained installed alongside Eidolon: Repraised
- Basic understanding of JSON format
- Text editor of your choice

## Your First Entry

### Step 1: Create the Entry File

Create a new datapack or use an existing one. Place this file at:
```
# Getting Started with Eidolon Unchained

**Create your first custom codex entry in 5 minutes!**

## Prerequisites

- Eidolon Unchained installed alongside Eidolon: Repraised
- Basic understanding of JSON format
- Text editor of your choice

## Your First Entry

### Step 1: Create the Entry File

The actual datapack structure used by Eidolon Unchained:
```
data/your_mod/codex_entries/my_first_entry.json
```

```json
{
  "target_chapter": "getting_started",
  "pages": [
    {
      "type": "title",
      "text": "your_mod.codex.entry.crystal_basics"
    },
    {
      "type": "text", 
      "text": "your_mod.codex.entry.crystal_basics.details"
    },
    {
      "type": "recipe",
      "recipe": "minecraft:crafting_table"
    }
  ]
}
```

### Step 2: Add Translations

Create your language file at:
```
assets/your_mod/lang/en_us.json
```

```json
{
  "your_mod.codex.entry.crystal_basics.title": "Crystal Basics",
  "your_mod.codex.entry.crystal_basics": "Welcome to crystal magic! This entry covers the fundamentals.",
  "your_mod.codex.entry.crystal_basics.details": "Crystals can amplify magical energies when properly prepared and placed."
}
```

### Step 3: Test In-Game

1. Launch Minecraft with your datapack loaded
2. Open the Eidolon codex (default key: `G`)
3. Navigate to **Fundamentals → Getting Started**
4. Your entry appears as a new page in that chapter

## Understanding the Structure

### How It Works
```
Entry File → targets existing chapter → adds pages → translation system displays content
```

### Available Target Chapters
Based on the current implementation, you can target these existing Eidolon chapters:
- `getting_started` - Basic Eidolon introduction
- `wooden_stand` - Workbench and basic crafting
- `crucible` - Alchemy and brewing
- `research_table` - Research mechanics
- `soul_enchanter` - Enchanting with souls

## Working Examples

Check the actual working examples in the mod:
```
src/main/resources/data/eidolonunchained/codex_entries/
├── text_example.json
├── recipe_example.json  
└── entity_example.json
```

These demonstrate the correct structure and page types that actually work.

## Next Steps

- **[Codex System Overview](codex-system/overview.md)** - Understand how entries work
- **[Page Types Reference](codex-system/page-types.md)** - All available page types
- **[Research System](research-system/overview.md)** - Add auto-discovery mechanics
- **[Datapack Structure](datapack/structure.md)** - Organize larger projects
```

```json
{
  "target_chapter": "wooden_stand",
  "pages": [
    {
      "type": "title",
      "text": "your_mod.codex.entry.crystal_basics"
    },
    {
      "type": "text", 
      "text": "your_mod.codex.entry.crystal_basics.discovery"
    },
    {
      "type": "crafting",
      "recipe": "eidolon:arcane_gold_ingot"
    }
  ]
}
```

### Step 2: Add Translations

Create or edit your language file at:
```
assets/your_mod/lang/en_us.json
```

```json
{
  "your_mod.codex.entry.crystal_basics.title": "Crystal Basics",
  "your_mod.codex.entry.crystal_basics": "Welcome to the mystical world of crystal manipulation! This entry will teach you the fundamentals.",
  "your_mod.codex.entry.crystal_basics.discovery": "Crystals can be found deep underground and amplify magical energies when properly prepared."
}
```

### Step 3: Test In-Game

1. Launch Minecraft with your datapack loaded
2. Open the Eidolon codex
3. Navigate to **Fundamentals → Wooden Stand**
4. Your new entry should appear at the bottom

## Understanding the Structure

### Entry Flow
```
Entry File → Target Chapter → Pages → Translation Keys → Display
```

### Page Types Quick Reference
- `title` - Entry header with title and intro text
- `text` - Additional content pages  
- `crafting` - Show crafting recipes
- `ritual` - Display ritual information
- `entity` - Show creature details

## Common Mistakes

❌ **Missing .title translation** - Always provide both base key and .title version  
❌ **Wrong target_chapter** - Use exact chapter names from Eidolon  
❌ **Invalid JSON** - Use a JSON validator to check syntax  
❌ **Percent signs** - Use `%%` instead of `%` in translations

## Next Steps

- **[Page Types Reference](page-types.md)** - Learn about all 9 page types
- **[Research System](research-system.md)** - Add auto-discovery mechanics  
- **[Datapack Structure](datapack-structure.md)** - Organize larger projects

## Working Example

Check `bundle/eidolonunchained_datapack/` in the mod files for a complete working example including:
- Proper file structure
- Multiple page types
- Translation patterns
- Research integration

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
