# Language Key Standardization Guide

Eidolon Unchained uses a **standardized language key system** for consistent localization across all features. This guide documents the **exact patterns** used in the codebase.

---

## 🎯 Standard Pattern

**Format**: `eidolonunchained.<system>.<category>.<name>.<type>`

```json
{
  "eidolonunchained.codex.entry.shadow_communion.title": "Shadow Communion",
  "eidolonunchained.deity.dark_deity.progression.shadow_initiate.title": "Shadow Initiate",
  "eidolonunchained.ui.command.api.success": "API key set successfully",
  "eidolonunchained.keybind.chant.slot1": "Chant Slot 1"
}
```

---

## 📋 System Categories

### 1. **Codex System** (`codex`)

#### Categories
```json
{
  "eidolonunchained.codex.category.examples.name": "Examples",
  "eidolonunchained.codex.category.example_chants.name": "Example Chants", 
  "eidolonunchained.codex.category.custom_research.name": "Custom Research",
  "eidolonunchained.codex.category.ai_deity_chants.name": "AI Deity Communion"
}
```

#### Chapters  
```json
{
  "eidolonunchained.codex.chapter.getting_started.title": "Getting Started",
  "eidolonunchained.codex.chapter.examples.title": "Example Chants",
  "eidolonunchained.codex.chapter.ai_deity_chants.title": "AI Deity Communion",
  "eidolonunchained.codex.chapter.custom_research.title": "Custom Research",
  "eidolonunchained.codex.chapter.divine_rituals.title": "Divine Rituals"
}
```

#### Entries
```json
{
  "eidolonunchained.codex.entry.shadow_communion.title": "Shadow Communion",
  "eidolonunchained.codex.entry.shadow_communion.description": "Commune with Nyxathel the Shadow Lord through dark rituals.",
  "eidolonunchained.codex.entry.shadow_communion.chant_description": "Draw the signs of Wicked, Wicked, and Blood to establish contact with the shadow realm."
}
```

### 2. **Deity System** (`deity`)

#### Deity Names & Descriptions
```json
{
  "eidolonunchained.deity.dark_deity.name": "Nyxathel, Shadow Lord",
  "eidolonunchained.deity.dark_deity.description": "Master of shadows and forbidden knowledge",
  "eidolonunchained.deity.light_deity.name": "Lumina, Sacred Guardian", 
  "eidolonunchained.deity.nature_deity.name": "Verdania, Guardian of Nature"
}
```

#### Progression Stages
```json
{
  "eidolonunchained.deity.dark_deity.progression.shadow_initiate.title": "Shadow Initiate",
  "eidolonunchained.deity.dark_deity.progression.shadow_initiate.description": "A new soul embraced by the shadows",
  "eidolonunchained.deity.dark_deity.progression.dark_acolyte.title": "Dark Acolyte",
  "eidolonunchained.deity.dark_deity.progression.shadow_priest.title": "Shadow Priest"
}
```

### 3. **Command System** (`command`)

#### Success Messages
```json
{
  "eidolonunchained.command.api.set.success": "API key set successfully for %s",
  "eidolonunchained.command.api.test.success": "API connection successful",
  "eidolonunchained.command.patron.choose.success": "You are now a follower of %s",
  "eidolonunchained.command.config.set.success": "Configuration updated: %s = %s"
}
```

#### Error Messages  
```json
{
  "eidolonunchained.command.api.set.error": "Failed to set API key: %s",
  "eidolonunchained.command.api.test.error": "API connection failed: %s",
  "eidolonunchained.command.patron.choose.error": "Cannot choose patron: %s",
  "eidolonunchained.command.deity.not_found": "Deity not found: %s"
}
```

#### Help Text
```json
{
  "eidolonunchained.command.api.help": "Manage AI provider API keys",
  "eidolonunchained.command.patron.help": "Manage deity patron relationships",
  "eidolonunchained.command.config.help": "Configure mod settings",
  "eidolonunchained.command.status.help": "Show system status information"
}
```

### 4. **UI System** (`ui`)

#### Dialogs & Prompts
```json
{
  "eidolonunchained.ui.patron.choose.confirm": "Are you sure you want to become a follower of %s?",
  "eidolonunchained.ui.patron.abandon.confirm": "Abandoning your patron will have consequences. Continue?",
  "eidolonunchained.ui.deity.api_key_required": "API key required for AI deity interactions",
  "eidolonunchained.ui.conversation.ended": "Conversation with %s has ended"
}
```

#### Status Messages
```json
{
  "eidolonunchained.ui.status.provider.active": "Active AI Provider: %s",
  "eidolonunchained.ui.status.patron.current": "Current Patron: %s",
  "eidolonunchained.ui.status.patron.none": "No Patron",
  "eidolonunchained.ui.status.reputation": "Reputation with %s: %d"
}
```

### 5. **Keybind System** (`keybind`)

#### Chant Keybinds
```json
{
  "eidolonunchained.keybind.chant.slot1": "Chant Slot 1",
  "eidolonunchained.keybind.chant.slot2": "Chant Slot 2", 
  "eidolonunchained.keybind.chant.slot3": "Chant Slot 3",
  "eidolonunchained.keybind.chant.slot4": "Chant Slot 4"
}
```

#### Category Names
```json
{
  "eidolonunchained.keybind.category.chant": "Eidolon Unchained: Chants",
  "eidolonunchained.keybind.category.deity": "Eidolon Unchained: Deities"
}
```

### 6. **Research System** (`research`)

#### Task Types
```json
{
  "eidolonunchained.research.task.kill_entities": "Kill %d %s",
  "eidolonunchained.research.task.craft_items": "Craft %d %s",
  "eidolonunchained.research.task.interact_blocks": "Interact with %s",
  "eidolonunchained.research.task.discover_biome": "Discover the %s biome",
  "eidolonunchained.research.task.reach_dimension": "Enter the %s dimension"
}
```

#### Progress Messages
```json
{
  "eidolonunchained.research.progress.discovered": "Research discovered: %s",
  "eidolonunchained.research.progress.completed": "Research completed: %s",
  "eidolonunchained.research.progress.task_progress": "Progress: %d/%d %s"
}
```

### 7. **Chant System** (`chant`)

#### Chant Names & Descriptions
```json
{
  "eidolonunchained.chant.shadow_communion.name": "Shadow Communion",
  "eidolonunchained.chant.shadow_communion.description": "Commune with Nyxathel the Shadow Lord",
  "eidolonunchained.chant.divine_communion.name": "Divine Communion",
  "eidolonunchained.chant.natures_communion.name": "Nature's Communion"
}
```

#### Chant Effects
```json
{
  "eidolonunchained.chant.effect.communication": "Establishes communication with deity",
  "eidolonunchained.chant.effect.blessing": "Requests divine blessing",
  "eidolonunchained.chant.effect.judgment": "Calls upon divine judgment"
}
```

### 8. **Patron System** (`patron`)

#### Relationship Status
```json
{
  "eidolonunchained.patron.relationship.follower": "Follower",
  "eidolonunchained.patron.relationship.enemy": "Enemy",
  "eidolonunchained.patron.relationship.neutral": "Neutral",
  "eidolonunchained.patron.relationship.allied": "Allied",
  "eidolonunchained.patron.relationship.no_patron": "No Patron"
}
```

#### Title Progression
```json
{
  "eidolonunchained.patron.title.shadow_initiate": "Shadow Initiate",
  "eidolonunchained.patron.title.dark_acolyte": "Dark Acolyte",
  "eidolonunchained.patron.title.shadow_priest": "Shadow Priest"
}
```

---

## 🎨 Formatting Conventions

### Color Codes
```json
{
  "eidolonunchained.deity.dark_deity.formatted_name": "§5Nyxathel, Shadow Lord§r",
  "eidolonunchained.ui.success": "§a✓ Success: %s§r",
  "eidolonunchained.ui.error": "§c✗ Error: %s§r",
  "eidolonunchained.ui.warning": "§e⚠ Warning: %s§r"
}
```

**Standard Colors:**
- **§a** (Green): Success messages
- **§c** (Red): Error messages  
- **§e** (Yellow): Warning messages
- **§6** (Gold): Important information
- **§7** (Gray): Secondary information
- **§r** (Reset): End formatting

### Unicode Symbols
```json
{
  "eidolonunchained.ui.symbol.success": "✓",
  "eidolonunchained.ui.symbol.error": "✗", 
  "eidolonunchained.ui.symbol.warning": "⚠",
  "eidolonunchained.ui.symbol.info": "ℹ",
  "eidolonunchained.ui.symbol.deity": "⚜",
  "eidolonunchained.ui.symbol.patron": "👑"
}
```

---

## 📝 Naming Guidelines

### File Naming
- **Lowercase only**: `en_us.json`, `en_gb.json`
- **Underscore separator**: Use `_` not `-` or spaces
- **Standard codes**: Follow Minecraft language code conventions

### Key Naming Rules

#### ✅ DO:
- Use descriptive names: `shadow_communion` not `sc`
- Follow hierarchy: `system.category.name.type`
- Use consistent patterns: `title`, `description`, `help`
- Separate words with underscores: `api_key_required`

#### ❌ DON'T:
- Mix case: `shadowCommunion` (use `shadow_communion`)
- Use spaces: `shadow communion` (use `shadow_communion`)
- Skip hierarchy: `shadow_communion_title` (use `codex.entry.shadow_communion.title`)
- Use abbreviations: `sc_title` (use `shadow_communion.title`)

### Placeholder Conventions
```json
{
  "eidolonunchained.command.api.set.success": "API key set for %s",
  "eidolonunchained.ui.reputation.current": "Reputation: %d/%d",
  "eidolonunchained.research.progress": "Progress: %d/%d %s completed"
}
```

**Placeholder Types:**
- **%s**: String values (names, IDs, text)
- **%d**: Integer values (numbers, counts, levels)
- **%f**: Float values (percentages, decimals)

---

## 🔧 Implementation Example

### Adding New Language Keys

#### 1. **Define in JSON**
```json
{
  "eidolonunchained.deity.fire_deity.name": "Pyrion the Flame Eternal",
  "eidolonunchained.deity.fire_deity.description": "Ancient god of fire and forge",
  "eidolonunchained.deity.fire_deity.progression.spark_bearer.title": "Spark Bearer"
}
```

#### 2. **Use in Code**
```java
// Get translated text
Component deityName = Component.translatable("eidolonunchained.deity.fire_deity.name");

// With placeholders
Component message = Component.translatable("eidolonunchained.command.patron.choose.success", deityName);

// Send to player
player.sendSystemMessage(message);
```

#### 3. **Register in Language File**
```json
{
  "eidolonunchained.deity.fire_deity.name": "Pyrion the Flame Eternal"
}
```

---

## 📊 Current Language Statistics

### Implemented Systems
- ✅ **Codex System**: 25+ keys for categories, chapters, entries
- ✅ **Deity System**: 15+ keys per deity (names, descriptions, stages)
- ✅ **Command System**: 30+ keys for success/error/help messages
- ✅ **UI System**: 20+ keys for dialogs and status messages
- ✅ **Keybind System**: 8+ keys for chant slot assignments
- ✅ **Research System**: 10+ keys for task types and progress
- ✅ **Chant System**: 20+ keys for chant names and descriptions
- ✅ **Patron System**: 15+ keys for relationships and titles

### File Locations
- **Primary**: `src/main/resources/assets/eidolonunchained/lang/en_us.json`
- **Backup**: `src/main/resources/assets/eidolonunchained/lang/en_us_backup.json`
- **Standardized**: `src/main/resources/assets/eidolonunchained/lang/en_us_standardized.json`

---

## 🌐 Localization Support

### Adding New Languages

#### 1. **Create Language File**
```
src/main/resources/assets/eidolonunchained/lang/fr_fr.json
```

#### 2. **Copy Key Structure**
```json
{
  "eidolonunchained.deity.dark_deity.name": "Nyxathel, Seigneur des Ombres",
  "eidolonunchained.deity.dark_deity.description": "Maître des ombres et des connaissances interdites"
}
```

#### 3. **Test in Game**
```bash
# Change language in Minecraft settings
# All text should automatically use new translations
```

### Translation Guidelines
- **Preserve Formatting**: Keep color codes and placeholders
- **Maintain Tone**: Match the mystical/fantasy theme
- **Cultural Adaptation**: Adapt names and concepts appropriately
- **Placeholder Order**: Ensure %s, %d placeholders match original order

---

**🎯 Ready to add custom language keys?** Use this standard pattern for all new features and content!
