# Chant Category System Examples

## How the Category System Works

The chant system can add individual chapters to **ANY** category - both existing Eidolon categories and custom datapack categories.

### ✅ Current Examples

All our example chants go to the `"examples"` category:
- **Shadow Communion** → `"category": "examples"`
- **Divine Communion** → `"category": "examples"`  
- **Divine Judgment** → `"category": "examples"`
- **Forbidden Knowledge** → `"category": "examples"`
- **Gaia's Wrath** → `"category": "examples"`
- **Nature's Communion** → `"category": "examples"`

### 🎯 Supported Categories

The system works with:

#### 1. **Custom Categories** (defined in datapacks)
```json
"category": "examples"        // Our custom examples category
"category": "my_mod_stuff"    // Any custom category you create
"category": "advanced_magic"  // Another custom category
```

#### 2. **Existing Eidolon Categories** (if they exist)
```json
"category": "fundamentals"    // If this exists in Eidolon
"category": "theurgy"         // If this exists in Eidolon  
"category": "druidism"        // If this exists in Eidolon
```

### 📝 Creating Your Own Categories

To create a custom category, add a `_category.json` file:

**File:** `data/yourmod/codex/mycategory/_category.json`
```json
{
  "key": "mycategory",
  "name": "yourmod.codex.category.mycategory", 
  "icon": "minecraft:diamond",
  "color": "0x00FFFF",
  "description": "Your custom category description"
}
```

Then assign chants to it:
```json
{
  "name": "My Custom Chant",
  "category": "mycategory",
  "codex_icon": "minecraft:emerald",
  ...
}
```

### 🚀 Flexibility Benefits

- **Datapack Creators:** Can create custom categories for their content
- **Mod Authors:** Can add chants to existing Eidolon categories
- **Users:** Get organized content that fits logically in the codex
- **No Hardcoding:** Everything is JSON-configurable

### ⚡ Key Features

1. **Single Category Field:** No redundant fields, clean JSON structure
2. **Universal Support:** Works with any category (existing or custom)  
3. **Icon Configuration:** Each chant can have its own icon
4. **Automatic Integration:** Uses the same system as Eidolon's research chapters
