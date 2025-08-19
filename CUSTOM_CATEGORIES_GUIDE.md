# Custom Categories & Chapters System

## 🎯 Overview

Yes! **You can absolutely create new categories and chapters** using Eidolon's event system! This approach uses **JSON datapacks** and **direct imports** instead of reflection for clean, maintainable code.

## 🔧 How It Works

### Event-Driven Approach (No Reflection!)
Instead of using reflection, we hook into Eidolon's `CodexEvents`:

- **`PreInit` Event**: Fired BEFORE categories are populated - perfect for adding new categories
- **`PostInit` Event**: Fired AFTER initialization - perfect for adding chapters to existing categories
- **Direct Access**: Events provide direct access to categories list and item mappings

### Primary Method: JSON Datapack-Driven (Recommended)

#### **Fully Configurable via JSON**
```java
// Clean event-driven approach - no reflection needed!
@SubscribeEvent
public static void onCodexPreInit(CodexEvents.PreInit event) {
    // Direct access to categories list
    DatapackCategoryExample.addDatapackCategories(event.categories, event.itemToEntryMap);
}
```

#### **Builder Pattern for Complex Cases**
```java
// Clean, type-safe category creation
Category moddedCategory = new CustomCategoryBuilder("modded")
    .icon(new ItemStack(Items.COMMAND_BLOCK))
    .color(0xFF9900) // Orange
    .addChapter("chapter.key", "Chapter Title", new ItemStack(Items.BOOK))
    .build(itemToEntryMap);
    
categories.add(moddedCategory); // Direct addition - no reflection!
```

## 📁 Directory Structure for JSON Categories

```
src/main/resources/data/eidolonunchained/codex/
├── custom_spells/          ← New category directory
│   ├── shadow_manipulation.json
│   ├── elemental_fusion.json
│   └── time_magic.json
├── community_rituals/      ← Another new category
│   ├── blood_pact.json
│   ├── soul_binding.json
│   └── dimensional_gate.json
└── expansions/            ← Third new category
    ├── new_artifacts.json
    └── advanced_techniques.json
```

## 🎨 Custom Category Features

### Full Customization
- **Icons**: Any Minecraft item as category icon
- **Colors**: Custom hex color codes for category tabs
- **Chapters**: Unlimited chapters per category
- **Integration**: Works seamlessly with existing Eidolon system

### JSON Support
Each JSON file becomes a chapter:
```json
{
  "title_key": "eidolonunchained.codex.chapter.shadow_magic",
  "title": "Shadow Magic",
  "icon": "minecraft:coal_block",
  "target_chapter": "SHADOW_SPELLS",
  "pages": [
    {
      "type": "text",
      "text": "Your chapter content here..."
    },
    {
      "type": "crafting_recipe",
      "recipe": "eidolonunchained:shadow_crystal"
    }
  ]
}
```

## 🚀 Usage Examples

### Adding a "Modded Content" Category
```java
Category moddedCategory = new CustomCategoryBuilder("modded")
    .icon(new ItemStack(Items.COMMAND_BLOCK))
    .color(0xFF9900) // Orange color
    .addChapter("eidolonunchained.codex.chapter.custom_monsters", 
               "Custom Monsters Guide", 
               new ItemStack(Items.ZOMBIE_SPAWN_EGG))
    .addChapter("eidolonunchained.codex.chapter.advanced_techniques", 
               "Advanced Techniques", 
               new ItemStack(Items.ENCHANTED_BOOK))
    .build(itemToEntryMap);

categories.add(moddedCategory); // Appears in codex GUI!
```

### Loading from JSON Files
```java
// Automatically scans directory and creates chapters
Category jsonCategory = createDatapackCategory(
    "custom_spells",                              // Category key
    new ItemStack(Items.ENCHANTED_BOOK),          // Category icon  
    0x4169E1,                                     // Royal Blue color
    "data/eidolonunchained/codex/custom_spells/", // JSON directory
    dataManager,
    itemToEntryMap
);
```

## ✅ Benefits Over Reflection

1. **Clean Integration**: Uses Eidolon's intended extension points
2. **No Brittle Code**: Won't break with Eidolon updates  
3. **Event Safety**: Guaranteed to run at the right time
4. **Full Access**: Complete access to categories list and item mappings
5. **Performance**: No runtime reflection overhead
6. **Type Safety**: Compile-time checking and IDE support
7. **Maintainable**: Clear code structure and dependencies

## 🎮 Result

New categories will appear as **actual tabs** in the Eidolon codex GUI, alongside the original 6 categories (Nature, Rituals, Artifice, Theurgy, Signs, Spells).

Players can browse your custom categories just like the built-in ones, with full support for:
- Custom icons and colors
- Chapter navigation  
- Item-based chapter lookup
- Translation keys
- All page types (text, recipes, images, etc.)

## 🔧 Files Created

### **PRIMARY SYSTEM (JSON Datapack-Driven)**
- `EidolonCategoryExtension.java` - Main event handler (NO REFLECTION)
- `CustomCategoryBuilder.java` - Builder pattern for easy category creation (NO REFLECTION)
- `DatapackCategoryExample.java` - JSON datapack integration (NO REFLECTION)
- Example JSON files showing structure

### **LEGACY COMPATIBILITY (Minimal Reflection)**  
- `EidolonCodexIntegration.java` - Content injection to existing chapters (reflection only where necessary)
- `EidolonResearchIntegration.java` - Research system integration (reflection only for API limitations)

**Bottom Line**: Yes, you can create completely new categories and chapters! The event system with JSON datapacks is the perfect, clean, reflection-free solution! 🎉
