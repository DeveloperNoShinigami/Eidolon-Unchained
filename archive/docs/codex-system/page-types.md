# Page Types

Complete reference for all available page types in the Eidolon Unchained datapack system.

## 📄 Overview

Page types define how content is displayed within chapters and entries. Each page type has specific properties and use cases.

# Page Types Reference

**Complete guide to all 9 page types available in Eidolon Unchained**

## Overview

Each page type serves a specific purpose and has its own JSON structure. These are the page types currently implemented and working in the system.

## 1. Title Page

**Purpose**: Entry header with title and introductory content  
**Usage**: Always use as the first page of an entry

```json
{
  "type": "title",
  "text": "your_mod.codex.entry.example"
}
```

**Translation Requirements**:
- `your_mod.codex.entry.example.title` - The title text
- `your_mod.codex.entry.example` - Main content below the title

---

## 2. Text Page

**Purpose**: Additional content pages with formatted text  
**Usage**: Detailed explanations, lore, instructions

```json
{
  "type": "text",
  "text": "your_mod.codex.entry.example.details"
}
```

**Features**:
- Supports Minecraft text formatting codes
- Automatic text wrapping
- Line breaks with proper spacing

---

## 3. Recipe Page

**Purpose**: Display any type of recipe (replaces crafting/smelting/etc.)  
**Usage**: Show crafting, smelting, or any other recipe

```json
{
  "type": "recipe",
  "recipe": "minecraft:crafting_table"
}
```

**Features**:
- Auto-detects recipe type
- Works with vanilla and modded recipes
- Displays ingredients and result

---

## 4. Entity Page

**Purpose**: Display creature information  
**Usage**: Monster studies, creature documentation

```json
{
  "type": "entity",
  "entity": "minecraft:zombie"
}
```

**Features**:
- 3D rotating entity model
- Entity information display
- Works with vanilla and modded entities

---

## 5. List Page

**Purpose**: Bullet-point lists of items or concepts  
**Usage**: Ingredient lists, step-by-step instructions

```json
{
  "type": "list",
  "items": [
    "your_mod.codex.list.step1",
    "your_mod.codex.list.step2",
    "your_mod.codex.list.step3"
  ]
}
```

**Features**:
- Automatic bullet formatting
- Each item can be a translation key
- Supports multiple list items

---

## 6. Ritual Page

**Purpose**: Show Eidolon ritual information  
**Usage**: Document ritual procedures

```json
{
  "type": "ritual",
  "ritual": "eidolon:summon_wraith"
}
```

**Features**:
- Shows ritual structure and layout
- Displays required ingredients
- Integration with Eidolon's ritual system

---

## 7. Crucible Page

**Purpose**: Display Eidolon crucible recipes  
**Usage**: Show alchemical crafting

```json
{
  "type": "crucible",
  "recipe": "eidolon:arcane_gold_ingot"
}
```

**Features**:
- Shows crucible ingredients
- Displays brewing process
- Integration with Eidolon's crucible system

---

## 8. Workbench Page

**Purpose**: Display Eidolon workbench recipes  
**Usage**: Mystical item creation

```json
{
  "type": "workbench",
  "recipe": "eidolon:basic_amulet"
}
```

**Features**:
- Shows workbench layout
- Displays required components
- Integration with Eidolon's workbench

---

## 9. Smelting Page

**Purpose**: Show furnace/smelting recipes  
**Usage**: Metal processing, cooking recipes

```json
{
  "type": "smelting",
  "recipe": "minecraft:iron_ingot"
}
```

**Features**:
- Shows furnace interface
- Displays input, fuel, and output
- Works with modded smelting recipes

---

## Working Examples

The mod includes actual working examples you can reference:

### Text Example
```json
{
  "target_chapter": "getting_started",
  "pages": [
    {
      "type": "title",
      "text": "eidolonunchained.codex.entry.text_example"
    },
    {
      "type": "text",
      "text": "eidolonunchained.codex.entry.text_example.content"
    }
  ]
}
```

### Recipe Example
```json
{
  "target_chapter": "getting_started", 
  "pages": [
    {
      "type": "title",
      "text": "eidolonunchained.codex.entry.recipe_example"
    },
    {
      "type": "recipe",
      "recipe": "minecraft:crafting_table"
    }
  ]
}
```

### Entity Example
```json
{
  "target_chapter": "getting_started",
  "pages": [
    {
      "type": "title", 
      "text": "eidolonunchained.codex.entry.entity_example"
    },
    {
      "type": "entity",
      "entity": "minecraft:zombie"
    }
  ]
}
```

## Page Combinations

### Basic Entry Structure
```json
{
  "target_chapter": "wooden_stand",
  "pages": [
    {"type": "title", "text": "mod.entry.main"},
    {"type": "text", "text": "mod.entry.main.details"},
    {"type": "recipe", "recipe": "minecraft:iron_sword"}
  ]
}
```

### Multi-Page Entry
```json
{
  "target_chapter": "crucible",
  "pages": [
    {"type": "title", "text": "mod.alchemy.basics"},
    {"type": "text", "text": "mod.alchemy.basics.theory"},
    {"type": "crucible", "recipe": "eidolon:arcane_gold_ingot"},
    {"type": "text", "text": "mod.alchemy.basics.results"}
  ]
}
```

## Common Issues

### Recipe Pages Show Nothing
- Verify the recipe ID exists and is spelled correctly
- Check that required mods are loaded
- Test the recipe works in-game first

### Entity Pages Empty
- Confirm entity ID is correct
- Check that the entity exists in the game
- Verify entity can be spawned

### Translation Missing
- Check language file syntax
- Ensure key names match exactly
- Verify file is in correct location (`assets/modid/lang/`)

## Best Practices

### Page Ordering
1. **Always start with `title`** - Sets the entry context
2. **Follow with `text`** - Provide explanation
3. **Add interactive content** - Recipes, entities, etc.
4. **End with practical info** - Conclusions or applications

### Translation Keys
- Use descriptive keys: `mod.codex.entry.topic.section`
- Always provide `.title` variants for title pages
- Keep text concise but informative
- Use `%%` for literal percent signs to avoid format errors

## Next Steps

- **[Translation Guide](translation-guide.md)** - Advanced translation techniques
- **[Examples](examples.md)** - More working examples
- **[Research Integration](../research-system/overview.md)** - Add progression mechanics

### Properties
- **`type`**: Must be `"text"`
- **`text`**: Translation key or literal text content

### Best Practices
- Always use translation keys
- Keep paragraphs focused and readable
- Use multiple text pages for long content

## 🏷️ Title Pages

Special pages that display as section headers.

```json
{
  "type": "title", 
  "text": "yourmod.codex.section.title"
}
```

### Properties
- **`type`**: Must be `"title"`
- **`text`**: Translation key for the title

### Usage
- Start entries with title pages (chapters only have title/icon)
- Use to divide long content into sections
- Appears with special formatting in the codex

## 🍽️ Recipe Pages

Display crafting recipes directly in the codex.

### Crafting Recipes
```json
{
  "type": "recipe",
  "recipe": "minecraft:crafting_table"
}
```

### Furnace Recipes
```json
{
  "type": "recipe",
  "recipe": "minecraft:iron_ingot"
}
```

### Custom Mod Recipes
```json
{
  "type": "recipe", 
  "recipe": "eidolon:pewter_blend"
}
```

### Properties
- **`type`**: Must be `"recipe"`
- **`recipe`**: Resource location of the recipe

### Supported Recipe Types
- Crafting table recipes
- Furnace smelting
- Smoking recipes
- Blasting recipes
- Custom mod recipes (if supported)

## 👾 Entity Pages

Render 3D entity models within the codex.

### Basic Entity
```json
{
  "type": "entity",
  "entity": "minecraft:zombie"
}
```

### Scaled Entity
```json
{
  "type": "entity",
  "entity": "minecraft:ender_dragon",
  "scale": 0.3
}
```

### Properties
- **`type`**: Must be `"entity"`
- **`entity`**: Resource location of the entity
- **`scale`** (optional): Scaling factor (default: 1.0)

### Scaling Guidelines
- **Large mobs**: Use 0.3-0.7 scale
- **Normal mobs**: Use 0.8-1.2 scale  
- **Small mobs**: Use 1.0-1.5 scale
- **Bosses**: Use 0.1-0.4 scale

## 🏺 Item Pages

Display item icons and information.

### Basic Item
```json
{
  "type": "item",
  "item": "minecraft:diamond_sword"
}
```

### Item with Count
```json
{
  "type": "item",
  "item": "minecraft:gold_ingot",
  "count": 8
}
```

### Properties
- **`type`**: Must be `"item"`
- **`item`**: Resource location of the item
- **`count`** (optional): Stack size to display (default: 1)

## 🔮 Ritual Pages

*Note: Available only if Eidolon ritual system is present*

```json
{
  "type": "ritual",
  "ritual": "eidolon:summon_ritual"
}
```

### Properties
- **`type`**: Must be `"ritual"`
- **`ritual`**: Resource location of the ritual

## 🖼️ Image Pages

Display custom images within the codex.

```json
{
  "type": "image",
  "image": "yourmod:textures/codex/diagram.png",
  "width": 128,
  "height": 128
}
```

### Properties
- **`type`**: Must be `"image"`
- **`image`**: Resource location of the image
- **`width`** (optional): Display width in pixels
- **`height`** (optional): Display height in pixels

### Image Requirements
- **Format**: PNG recommended
- **Size**: Powers of 2 work best (64x64, 128x128, 256x256)
- **Location**: `assets/yourmod/textures/codex/`

## 📊 Table Pages

*Advanced feature for structured data*

```json
{
  "type": "table",
  "headers": ["yourmod.codex.table.item", "yourmod.codex.table.effect"],
  "rows": [
    ["minecraft:apple", "yourmod.codex.table.healing"],
    ["minecraft:golden_apple", "yourmod.codex.table.regeneration"]
  ]
}
```

### Properties
- **`type`**: Must be `"table"`
- **`headers`**: Array of translation keys for column headers
- **`rows`**: Array of arrays containing row data

## 🔗 Link Pages

*Create references to other chapters*

```json
{
  "type": "link",
  "text": "yourmod.codex.link.see_also",
  "target": "advanced_techniques"
}
```

### Properties
- **`type`**: Must be `"link"`
- **`text`**: Display text (translation key)
- **`target`**: Chapter to link to

## 🎨 Custom Page Types

Advanced users can create custom page types by extending the system.

### Requirements
- Java mod development knowledge
- Implementation of `ICodexPage` interface
- Registration with the page type registry

### Example Structure
```json
{
  "type": "yourmod:custom_page",
  "custom_property": "value",
  "another_property": 123
}
```

## 💡 Best Practices

### Page Organization
1. **Start with title** - Always begin with a title page
2. **Mix content types** - Combine text, images, recipes for variety
3. **Logical flow** - Order pages in a sensible sequence

### Content Design
1. **Keep text concise** - Break long text into multiple pages
2. **Use appropriate scaling** - Scale entities for optimal viewing
3. **Test recipes** - Ensure recipes work and are obtainable

### Performance
1. **Optimize images** - Use appropriate sizes and formats
2. **Limit entity pages** - Too many can impact performance
3. **Cache considerations** - Pages are cached for reuse

## ⚠️ Common Issues

### Page Not Displaying
- Check JSON syntax
- Verify page type is spelled correctly
- Ensure required properties are present

### Recipe Not Found
- Confirm recipe exists in the game
- Check resource location format
- Verify mod dependencies are loaded

### Entity Not Rendering
- Ensure entity exists
- Check for client-side only entities
- Verify entity resource location

### Image Not Loading
- Check file path and format
- Ensure image is in correct assets folder
- Verify image dimensions are reasonable

## 📋 Page Type Quick Reference

| Type | Purpose | Required Properties | Optional Properties |
|------|---------|-------------------|-------------------|
| `text` | Display text | `text` | none |
| `title` | Section headers | `text` | none |
| `recipe` | Show recipes | `recipe` | none |
| `entity` | 3D entity models | `entity` | `scale` |
| `item` | Item display | `item` | `count` |
| `ritual` | Ritual info | `ritual` | none |
| `image` | Custom images | `image` | `width`, `height` |
| `table` | Structured data | `headers`, `rows` | none |
| `link` | Chapter links | `text`, `target` | none |

## 📚 Related Documentation

- [Codex Entries](codex-entries.md) - Using pages in entries
- [System Overview](system-overview.md) - Understanding the chapter system
- [Examples](examples.md) - Working examples of each type
- [Translations](translations.md) - Localizing page content
