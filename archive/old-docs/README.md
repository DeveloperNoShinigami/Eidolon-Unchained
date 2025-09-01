# Eidolon Unchained Documentation

**Complete guide to creating custom content with Eidolon Unchained**

## Quick Navigation

📚 **[Getting Started](getting-started.md)** - Create your first entry in 5 minutes  

### Major Features

📖 **Codex System**
- **[Overview](codex-system/overview.md)** - How the system works
- **[Page Types](codex-system/page-types.md)** - All 9 available page types

🧪 **Research System**  
- **[Overview](research-system/overview.md)** - Auto-discovery mechanics
- **[Visual Diagrams](research-system/diagrams.md)** - System flow charts
- **[Examples](research-system/examples.md)** - Working research configurations
- **[Integration Guide](research-system/integration.md)** - Connect with codex entries

🏗️ **Datapack Creation**
- **[Structure Guide](datapack/structure.md)** - Organize your projects

### Additional Topics

💡 **Miscellaneous**
- **[Tips & Best Practices](misc/tips-and-future.md)** - Pro techniques and quality assurance

## Learning Path

### Beginner (First Entry)
1. **[Getting Started](getting-started.md)** - Create a basic entry
2. **[Page Types](codex-system/page-types.md)** - Learn available content types
3. Try creating 2-3 simple entries

### Intermediate (Multiple Entries)
1. **[Codex Overview](codex-system/overview.md)** - Understand the system
2. **[Research System](research-system/overview.md)** - Add auto-discovery
3. **[Integration Guide](research-system/integration.md)** - Connect systems together
4. **[Project Structure](datapack/structure.md)** - Organize content

### Advanced (Large Projects)
1. **[Examples Collection](research-system/examples.md)** - Advanced configurations
2. **[Tips & Best Practices](misc/tips-and-future.md)** - Pro techniques
3. Complex multi-stage research chains
4. Large-scale content organization

## What You Can Create

### Content Types
🔮 **Codex Entries** - Add pages to existing Eidolon chapters  
⚗️ **Research Content** - Auto-discoverable progression systems  
📚 **Recipe Integration** - Display crafting, ritual, and crucible recipes  
🌟 **Multi-Language** - Translation support for international audiences

### Current Capabilities
- **9 Page Types** - Title, text, recipe, entity, list, ritual, crucible, workbench, smelting
- **Auto-Discovery Research** - Kill triggers, location discovery, interaction triggers
- **Translation System** - Robust multi-language support with caching
- **Recipe Integration** - Works with vanilla and modded recipes

## Quick Reference

### Basic Entry Structure
```json
{
  "target_chapter": "getting_started",
  "pages": [
    {"type": "title", "text": "mod.entry.name"},
    {"type": "text", "text": "mod.entry.name.details"},
    {"type": "recipe", "recipe": "minecraft:crafting_table"}
  ]
}
```

### Translation Pattern
```json
{
  "mod.entry.name.title": "Entry Title",
  "mod.entry.name": "Main content for title page",
  "mod.entry.name.details": "Additional content for text pages"
}
```

### Actual File Structure
```
data/your_mod/
├── codex_entries/          # Entry definitions
│   ├── basic_entries/
│   └── advanced_entries/
├── eidolon_research/       # Research definitions
└── research_entries/       # Research content

assets/your_mod/lang/
└── en_us.json             # Translation files
```

## Working Examples

The mod includes working examples you can reference:
```
src/main/resources/data/eidolonunchained/codex_entries/
├── text_example.json
├── recipe_example.json  
└── entity_example.json
```

These demonstrate the correct structure and implementation.

## Common Issues & Solutions

### Entry Not Appearing
- ✅ Check `target_chapter` spelling matches existing Eidolon chapters
- ✅ Verify JSON syntax with online validator
- ✅ Ensure datapack is loaded (`/datapack list`)
- ✅ Check game logs for errors

### Translation Problems  
- ✅ Use `%%` for literal percent signs
- ✅ Match translation keys exactly (case sensitive)
- ✅ Provide both base key and `.title` version for title pages
- ✅ Check language file location in `assets/` folder

### Recipe/Entity Pages Empty
- ✅ Verify the ID exists and is spelled correctly
- ✅ Ensure required mods are loaded
- ✅ Test that the recipe/entity works in-game

## Version Information

**Current Version**: 1.0.0  
**Minecraft**: 1.20.1  
**Forge**: 47.4.0+  
**Eidolon**: 0.3.8+

## Getting Help

- **GitHub Issues** - Bug reports and feature requests
- **Working Examples** - Check the included example files
- **Documentation** - You're reading it!

---

**Ready to start?** Jump to **[Getting Started](getting-started.md)** and create your first entry!

