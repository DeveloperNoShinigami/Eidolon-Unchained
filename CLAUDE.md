# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Eidolon Unchained** is a Minecraft Forge mod for 1.20.1 that extends the Eidolon: Repraised mod with advanced AI-powered deity systems, datapack-driven chant systems, and enhanced codex integration. It integrates Google Gemini AI to create dynamic, context-aware deity interactions and provides tools for content creators to add custom chants, deities, and codex entries through datapacks.

## Development Environment

### Build Commands
- **Build the mod**: `./gradlew build`
- **Run client for testing**: `./gradlew runClient`
- **Run server for testing**: `./gradlew runServer`
- **Generate data**: `./gradlew runData`
- **Clean build**: `./gradlew clean`

### Key Dependencies
- **Minecraft**: 1.20.1
- **Forge**: 47.1.0+
- **Eidolon Repraised**: 0.3.8+ (required dependency)
- **Curios API**: 5.14.1+ (required dependency)

### Configuration Files
- Main config: `src/main/resources/META-INF/mods.toml`
- Gradle properties: `gradle.properties`
- Chant configurations: `run/config/eidolonunchained/`
- API keys: `run/config/eidolonunchained/api-keys.properties`

## Architecture Overview

### Core Systems

**AI Deity Integration** (`src/main/java/com/bluelotuscoding/eidolonunchained/ai/`):
- `AIDeityManager.java` - Manages AI deity interactions
- `GeminiAPIClient.java` - Google Gemini API integration
- Context-aware AI responses based on player actions, location, and reputation

**Datapack Chant System** (`src/main/java/com/bluelotuscoding/eidolonunchained/chant/`):
- `DatapackChantManager.java` - Loads custom chants from datapacks
- `DatapackChantSpell.java` - Integrates datapack chants with Eidolon's spell system
- `ChantCooldownManager.java` - Manages per-chant cooldowns

**Codex Integration** (`src/main/java/com/bluelotuscoding/eidolonunchained/data/`):
- `CodexDataManager.java` - Manages datapack-driven codex entries
- `ResearchDataManager.java` - Handles research system integration
- `EidolonCodexIntegration.java` - Bridges with Eidolon's codex system

**Networking** (`src/main/java/com/bluelotuscoding/eidolonunchained/network/`):
- `EidolonUnchainedNetworking.java` - Packet registration and handling
- Client-server synchronization for chants, deity data, and UI updates

### Data Structure

**Datapack Content Locations**:
- Chants: `data/modid/chants/`
- AI Deities: `data/modid/ai_deities/`
- Deities: `data/modid/deities/`
- Codex Entries: `data/modid/codex_entries/`
- Recipes: `data/modid/recipes/`

**Resource Pack Content**:
- Translations: `assets/modid/lang/`
- Textures and models: `assets/modid/`

## Development Guidelines

### Adding New Features
1. **Chants**: Create JSON files in `data/modid/chants/` following the schema in existing examples
2. **Deities**: Define in `data/modid/ai_deities/` with AI personality and behavior rules
3. **Codex Entries**: Add to `data/modid/codex_entries/` targeting existing Eidolon chapters
4. **Research**: Define progression requirements in `data/modid/eidolon_research/`

### AI Deity System
- **API Configuration**: Set up Google Gemini API keys in `api-keys.properties`
- **Safety Features**: All AI commands are validated and sanitized before execution
- **Context Tracking**: System tracks player actions, location, weather, and inventory for contextual responses
- **Reputation System**: Integrates with Eidolon's reputation system for progressive deity relationships

### Mixin Usage
The mod uses Mixins for codex integration:
- `eidolonunchained.mixins.json` defines mixin configurations
- Mixins are used sparingly and only for essential integration points

### Testing and Debugging
- **Development Environment**: Use `runClient` task with test world in `run/`
- **Debug Commands**: `/eidolonunchained debug` for system diagnostics
- **Log Monitoring**: Check `run/logs/latest.log` for system events and errors
- **API Testing**: Use `/test_chant` commands to validate chant functionality

### Common Patterns

**Event Handling**:
```java
@Mod.EventBusSubscriber(modid = EidolonUnchained.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class EventHandler {
    @SubscribeEvent
    public static void onEvent(EventType event) {
        // Handle event
    }
}
```

**Datapack Loading**:
```java
public class DataManager extends SimpleJsonResourceReloadListener {
    public DataManager() {
        super(GSON, "data_type");
    }
    
    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resourceMap, 
                        ResourceManager resourceManager, ProfilerFiller profiler) {
        // Process loaded data
    }
}
```

### Security Considerations
- **API Key Management**: Store keys in `api-keys.properties`, never in code
- **Command Validation**: All AI-generated commands are validated against whitelist
- **Input Sanitization**: User inputs are sanitized before sending to AI APIs
- **Rate Limiting**: Built-in cooldown systems prevent API abuse

### Error Handling
- Use proper try-catch blocks around API calls
- Log errors with appropriate severity levels
- Provide fallback behavior when AI systems are unavailable
- Validate JSON data thoroughly during datapack loading

## Important Notes
- The mod requires both Eidolon: Repraised and Curios API to function
- AI features require valid Google Gemini API keys
- Datapack content follows Minecraft's standard datapack format with mod-specific extensions
- All user-facing text should be translatable via language files