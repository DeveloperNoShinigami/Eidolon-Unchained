# Page Types

Complete reference for all available page types in the Eidolon Unchained datapack system.

## 📄 Overview

Page types define how content is displayed within chapters and entries. Each page type has specific properties and use cases.

## 📝 Text Pages

The most common page type for explanatory content.

### Basic Text
```json
{
  "type": "text",
  "text": "yourmod.codex.page.description"
}
```

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
- Start chapters and entries with title pages
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
- [Chapters](chapters.md) - Organizing pages in chapters
- [Examples](examples.md) - Working examples of each type
- [Translations](translations.md) - Localizing page content
