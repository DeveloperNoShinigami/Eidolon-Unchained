# Language System Documentation

## Overview

Eidolon Unchained uses Minecraft's standard localization system with a structured approach to translation keys. All user-facing text should use `Component.translatable()` with proper translation keys defined in language files.

## Translation Key Structure

All translation keys follow a standardized pattern:
```
eidolonunchained.<system>.<category>.<name>.<type>
```

### Examples:
- `eidolonunchained.prayer.cooldown_wait` - Prayer system cooldown message
- `eidolonunchained.chat.conversation_started` - Chat system conversation message
- `eidolonunchained.command.api_key_set` - Command system confirmation message
- `eidolonunchained.codex.entry.shadow_communion.title` - Codex entry title

## System Categories

### Prayer System (`eidolonunchained.prayer.*`)
Messages related to prayer interactions, deity responses, and prayer restrictions.

**Key Examples:**
- `eidolonunchained.prayer.godless_ignored` - When deity ignores godless prayers
- `eidolonunchained.prayer.cooldown_wait` - Prayer cooldown messages
- `eidolonunchained.prayer.reputation_required` - Reputation requirement messages
- `eidolonunchained.prayer.deity_speaking` - Action bar message format

### Chat System (`eidolonunchained.chat.*`)
AI deity conversation system messages and responses.

**Key Examples:**
- `eidolonunchained.chat.conversation_started` - Conversation initiation
- `eidolonunchained.chat.api_error` - API communication errors
- `eidolonunchained.chat.no_response` - No AI response available

### Task System (`eidolonunchained.task.*`)
Divine task assignment, completion, and reputation tracking.

**Key Examples:**
- `eidolonunchained.task.assigned` - Task assignment confirmation
- `eidolonunchained.task.completed` - Task completion messages
- `eidolonunchained.task.reputation_gained` - Reputation change notifications

### Spell System (`eidolonunchained.spell.*`)
Chant casting, effects, and spell-related messages.

**Key Examples:**
- `eidolonunchained.spell.cast_success` - Successful spell casting
- `eidolonunchained.spell.insufficient_reputation` - Reputation requirements
- `eidolonunchained.spell.cooldown_active` - Spell cooldown messages

### Command System (`eidolonunchained.command.*`)
Administrative commands and configuration messages.

**Key Examples:**
- `eidolonunchained.command.api_key_set` - API key configuration
- `eidolonunchained.command.config_reloaded` - Configuration reload confirmation
- `eidolonunchained.command.invalid_deity` - Invalid deity ID errors

### Codex System (`eidolonunchained.codex.*`)
Codex entries, chapters, and documentation content.

**Key Examples:**
- `eidolonunchained.codex.category.examples.name` - Category names
- `eidolonunchained.codex.entry.shadow_communion.title` - Entry titles
- `eidolonunchained.codex.chapter.getting_started.title` - Chapter titles

## Implementation Guidelines

### Using Translation Keys in Code

**✅ Correct Usage:**
```java
// Simple message
player.sendSystemMessage(Component.translatable("eidolonunchained.prayer.cooldown_wait", remainingMinutes, deity.getDisplayName()));

// With styling
Component message = Component.translatable("eidolonunchained.chat.conversation_started")
    .withStyle(ChatFormatting.GREEN);
```

**❌ Incorrect Usage:**
```java
// Hardcoded literals - DON'T DO THIS
player.sendSystemMessage(Component.literal("§cYou must wait " + minutes + " minutes!"));

// Legacy color codes - DON'T DO THIS
Component.literal("§a" + message);
```

### Parameter Substitution

Translation keys support parameter substitution using `%s` placeholders:

```json
{
  "eidolonunchained.prayer.cooldown_wait": "You must wait %s more minutes before praying to %s again."
}
```

```java
// Parameters are substituted in order
Component.translatable("eidolonunchained.prayer.cooldown_wait", remainingMinutes, deity.getDisplayName())
```

### Color and Formatting

Use Minecraft's `ChatFormatting` instead of legacy color codes:

```java
// ✅ Correct
Component.translatable("eidolonunchained.prayer.success")
    .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD);

// ❌ Incorrect
Component.literal("§a§l" + translatedText);
```

## Language File Structure

The main language file is located at:
```
src/main/resources/assets/eidolonunchained/lang/en_us.json
```

### File Organization
```json
{
  "_comment_structure": "Standardized language keys: eidolonunchained.<system>.<category>.<name>.<type>",
  
  "_comment_prayer_system": "=== PRAYER SYSTEM ===",
  "eidolonunchained.prayer.godless_ignored": "%s ignores your godless prayers.",
  "eidolonunchained.prayer.cooldown_wait": "You must wait %s more minutes before praying to %s again.",
  
  "_comment_chat_system": "=== CHAT SYSTEM ===",
  "eidolonunchained.chat.conversation_started": "Conversation with %s has begun.",
  "eidolonunchained.chat.api_error": "Unable to reach %s at this time.",
  
  "_comment_codex_system": "=== CODEX SYSTEM ===",
  "eidolonunchained.codex.category.examples.name": "Examples",
  "eidolonunchained.codex.entry.shadow_communion.title": "Shadow Communion"
}
```

## Adding New Translations

### Step 1: Add to Language File
```json
{
  "eidolonunchained.system.new_message": "Your new message with %s parameters."
}
```

### Step 2: Use in Code
```java
Component message = Component.translatable("eidolonunchained.system.new_message", parameter);
player.sendSystemMessage(message);
```

### Step 3: Test and Validate
- Compile the mod to check for syntax errors
- Test in-game to verify the message displays correctly
- Check parameter substitution works as expected

## Localization Support

To add support for other languages:

1. Create new language files (e.g., `es_es.json`, `fr_fr.json`)
2. Copy the structure from `en_us.json`
3. Translate the values while keeping the keys identical
4. Players can select their language in Minecraft settings

## Best Practices

1. **Always use `Component.translatable()`** - Never hardcode user-facing text
2. **Follow the key structure** - Use the standardized `eidolonunchained.<system>.<category>.<name>.<type>` pattern
3. **Use descriptive key names** - Keys should clearly indicate their purpose
4. **Group related keys** - Organize by system and functionality
5. **Include context in translations** - Provide clear, contextual messages
6. **Test parameter substitution** - Ensure `%s` placeholders work correctly
7. **Use proper formatting** - Apply `ChatFormatting` instead of legacy codes
8. **Document new keys** - Add comments to language files for organization

## Common Patterns

### Success Messages
```json
"eidolonunchained.system.success": "§a✓ Operation completed successfully!"
```

### Error Messages
```json
"eidolonunchained.system.error": "§c✗ Error: %s"
```

### Progress Messages
```json
"eidolonunchained.system.progress": "§e⏳ Processing... (%s%%)"
```

### Confirmation Messages
```json
"eidolonunchained.system.confirm": "§7%s has been updated."
```

## Troubleshooting

### Missing Translation Keys
If a translation key is missing, Minecraft will display the key itself. Add the missing key to the language file.

### Parameter Mismatches
Ensure the number of `%s` placeholders matches the number of parameters passed to `Component.translatable()`.

### Compilation Errors
Check for syntax errors in the JSON language file using a JSON validator.

### Testing Translations
Use the debug command to test specific translation keys:
```
/eidolon-unchained debug translate <key>
```

## Migration from Legacy Code

When updating old code that uses `Component.literal()`:

1. **Identify hardcoded messages** - Look for `Component.literal()` with string content
2. **Create translation keys** - Add appropriate keys to the language file
3. **Replace with `Component.translatable()`** - Update the code to use translation keys
4. **Test thoroughly** - Verify all messages display correctly
5. **Remove legacy formatting** - Replace `§` codes with `ChatFormatting`

This standardized approach ensures consistent, translatable, and maintainable user interface text throughout the mod.
