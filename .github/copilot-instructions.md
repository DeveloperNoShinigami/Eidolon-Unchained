# Eidolon Unchained - AI Coding Agent Instructions

## Project Overview

**Eidolon Unchained** is a Minecraft 1.20.1 Forge mod that extends [Eidolon](https://github.com/elucent/eidolon) with AI-powered deity interactions, flexible chant systems, and datapack-driven content creation. The mod enables players to interact with AI deities through Google Gemini API and provides extensive customization through JSON datapacks.

## Architecture Overview

### CRITICAL: System Loading Order Requirements
**MANDATORY LOADING SEQUENCE - NEVER MODIFY TIMING WITHOUT CAREFUL ANALYSIS:**

1. **Datapack Resource Loading Phase** (Resource Reload Listeners - `SimpleJsonResourceReloadListener`)
   - `CodexDataManager` - Loads codex entries and chapters from JSON
   - `ResearchDataManager` - Loads research definitions and converts to Eidolon format
   - `AIDeityManager` - Loads deity configurations and AI personalities
   - `DatapackChantManager` - Loads chant definitions from JSON

2. **Integration Phase** (Called from Resource Managers AFTER loading completes)
   - `EidolonResearchIntegration.injectCustomResearch()` - Called from `ResearchDataManager.apply()`
   - `EidolonCodexIntegration` - Injects codex entries into Eidolon chapters
   - Chant registration with Eidolon spell system

3. **Mod Loading Events** (FMLLoadCompleteEvent, FMLClientSetupEvent) 
   - **WARNING**: These events fire BEFORE resource loading - never use for integration!
   - Only use for mod component initialization, not datapack-dependent operations

**Why This Order Matters:**
- Resource reload listeners run ~9 seconds after mod loading events
- Integration must happen AFTER data is loaded, not during mod initialization
- Race conditions cause "0 entries loaded" errors when integration runs too early

### Core System Boundaries
- **Data Management Layer**: `CodexDataManager`, `ResearchDataManager`, `EidolonResearchDataManager` - Handle JSON datapack loading and validation
- **AI Integration Layer**: `AIDeityManager`, `GeminiAPIClient` - Async AI API communication with comprehensive error handling
- **Game Integration Layer**: `PrayerSystem`, `DeityChat`, `ChantSlotManager` - Player interaction mechanics
- **Eidolon Integration**: Reflection-based integration with parent mod (see `integration/` package)

### Major Systems Architecture

#### 1. AI Deity System (90% Complete)
**Core Components:**
- `AIDeityManager` - Loads AI configurations, links to deities via ResourceLocation
- `AIDeityConfig` - JSON-driven personality configurations with progression-based behavior
- `GeminiAPIClient` - Async Google Gemini API integration with comprehensive error handling
- `DeityChat` - Conversation management with context tracking and command parsing
- `PrayerSystem` - Prayer processing pipeline with cooldown management

**Data Flow:**
```
JSON Datapack → AIDeityManager → Link to Deity → Player Interaction → 
AI API Call → Response Processing → Command Execution/Chat Display
```

#### 2. Flexible Chant System (95% Complete)
**Core Components:**
- `DatapackChantManager` - Loads chant definitions from JSON, registers with Eidolon spell system
- `DatapackChantSpell` - Custom spell implementation that can trigger AI conversations or effects
- `ChantSlotManager` - Player-configurable keybind slots for chant assignment
- `ChantKeybinds` - 4 configurable slots (G,H,J,K) + management interface (C)

**Integration Pattern:**
- Chants load from `data/modid/chants/*.json` 
- Automatically register with Eidolon's spell system using reflection
- Can trigger deity conversations, apply effects, or execute commands
- Sign sequences defined per chant: `["eidolon:harmony", "eidolon:soul", "eidolon:sacred"]`

#### 3. Codex Integration System (85% Complete)
**Core Components:**
- `CodexDataManager` - Loads custom codex entries from JSON datapacks
- `EidolonCodexIntegration` - Injects entries into Eidolon's chapter system via reflection
- `EidolonCategoryExtension` - Uses reflection to access Eidolon's category system
- `EidolonPageConverter` - Converts JSON page definitions to native Eidolon page types

**Page Types Supported:** `title`, `text`, `recipe`, `entity`, `list`, `ritual`, `crucible`, `workbench`, `smelting`

#### 4. Research System (75% Complete)
**Core Components:**
- `ResearchDataManager` - Custom research chapter/entry loading
- `EidolonResearchDataManager` - Integration with Eidolon's research system
- `ResearchTriggerLoader` - Auto-discovery triggers (kill, location, interaction)
- `ResearchRegistrar` - Research registration and validation

#### 5. Prayer & Effigy Integration (60% Complete)
**Core Components:**
- `PrayerSystem` - Enhanced prayer processing with AI integration
- `EffigyInteractionHandler` - Right-click effigy interactions (currently commented out)
- Cooldown integration with Eidolon's existing systems
- Context-aware prayer generation based on player state

### Async-First Design Pattern
All external API calls use `CompletableFuture` to prevent UI blocking:
```java
client.generateResponse(prompt, personality, config, safety)
    .thenAccept(response -> sendDeityMessage(player, deity.getName(), response.dialogue))
    .exceptionally(error -> player.sendMessage("The deity is silent..."));
```

## Critical Errors & Issues Analysis

### Current Error Categories (from latest.log)

#### 0. TIMING ISSUES - HIGHEST PRIORITY PATTERN
**CRITICAL**: Always check loading order when systems show "0 entries loaded" despite files existing:
- **Resource Reload Listeners** (DatapackChantManager, ResearchDataManager, etc.) run 9+ seconds AFTER mod loading events
- **Integration calls** (EidolonResearchIntegration, EidolonCodexIntegration) MUST happen AFTER resource loading
- **Fix Pattern**: Call integration from resource manager's `apply()` method, not from FML events
- **Example Fixed**: ResearchDataManager now calls EidolonResearchIntegration.injectCustomResearch() after loading
- **Warning Signs**: "Injecting 0 entries" followed later by "Loaded X entries" in logs

#### 1. Chant Loading Errors (HIGH PRIORITY)
```
ERROR: Failed to load chant from eidolonunchained:ritual_of_verdant_growth: JsonObject
ERROR: Failed to load chant from eidolonunchained:ritual_of_shadow_ascension: JsonObject  
ERROR: Failed to load chant from eidolonunchained:ritual_of_divine_radiance: JsonObject
```
**Root Cause**: `DatapackChantManager` expects different JSON structure than what's provided in ritual files
**Why**: Ritual JSON files contain complex nested structures that don't match the expected chant schema
**Fix Strategy**: Either update chant loader to handle ritual structure OR separate ritual files from chant files

#### 2. Codex Integration Errors (MEDIUM PRIORITY)
```
ERROR: Target chapter 'eidolonunchained:forbidden_knowledge' not found in Eidolon chapters or custom chapters
ERROR: Available custom chapters: [list shows the chapter EXISTS]
```
**Root Cause**: Race condition between chapter registration and entry loading
**Why**: Codex entries are trying to link to chapters before chapter registration completes
**Fix Strategy**: Ensure proper loading order in `EidolonCodexIntegration.attemptIntegrationIfNeeded()`

#### 3. Category Not Found Warnings (MEDIUM PRIORITY)  
```
WARN: Category 'examples' not found; unable to attach chapter
WARN: Category 'example_chants' not found; unable to attach chapter
```
**Root Cause**: Chapters are trying to attach to categories that don't exist in Eidolon
**Why**: `EidolonCategoryExtension` can't find the target categories via reflection
**Fix Strategy**: Either create custom categories OR map to existing Eidolon categories

#### 4. Invalid Path Warnings (LOW PRIORITY)
```
ERROR: Invalid path in pack: eidolonunchained:codex/README.md, ignoring
```
**Root Cause**: README.md files being picked up by resource scanning
**Why**: Resource managers treat all files as potential resources
**Fix Strategy**: Filter out non-JSON files in resource scanning OR move README files

### System-Specific Issues

#### AI Deity System Issues
- **API Key Validation**: Missing error handling for invalid/expired API keys
- **Rate Limiting**: No protection against API quota exhaustion  
- **Conversation Context**: Memory usage grows unbounded over time
- **Error Recovery**: Some API failures don't have graceful fallbacks

#### Chant System Issues
- **JSON Schema Mismatch**: Ritual files vs chant files have different expected structures
- **Keybind Conflicts**: No validation for keybind conflicts with other mods
- **Effect Validation**: Chant effects aren't validated before execution
- **Sign Validation**: No check if required Eidolon signs exist

#### Codex System Issues  
- **Timing Dependencies**: Race conditions between different loading phases
- **Category Mapping**: Hard-coded category names that may not exist
- **Translation Loading**: I18n keys not pre-validated before use
- **Page Type Safety**: No runtime validation of page type compatibility

## Development Workflows

### Build & Test Commands
```bash
# Primary development cycle
./gradlew compileJava          # Compile after every 5-10 line changes
./gradlew build               # Full build with resource processing
./gradlew runClient           # Launch Minecraft client for testing

# Debug/validation commands (in-game)
/eidolon-unchained config validate   # Validate all configurations
/eidolon-unchained debug status      # System health check
/eidolon-config test gemini          # Test AI API connectivity
```

### Error Diagnosis Workflow
1. **Check latest.log** for ERROR/WARN patterns first
2. **Run validation commands** to get structured error reports
3. **Use debug status** to check system component health
4. **Test individual components** before testing integration
5. **Check datapack loading order** - deities → AI configs → chants → codex

### Configuration Validation Pipeline
The mod includes comprehensive validation systems that should be run after configuration changes:
- Use `/eidolon-unchained config validate` for full system validation
- Check `latest.log` for datapack loading errors during development
- API keys stored in `config/eidolonunchained-common.toml` (use config commands, not direct file edit)

## Project-Specific Conventions

### Java Naming Patterns
- **Java fields**: `camelCase` (e.g., `apiProvider`, `deityName`)
- **JSON fields**: `snake_case` (e.g., `"deity_name"`, `"api_provider"`)
- **ResourceLocation**: `namespace:path` format (e.g., `"eidolonunchained:example_deity"`)

### File Organization
- **Datapack definitions**: `src/main/resources/data/eidolonunchained/`
  - `deities/` - Basic deity properties and progression
  - `ai_deities/` - AI personality configurations linking to deities
  - `chants/` - Chant definitions with sign sequences
  - `codex_entries/` - Codex page content
  - `research/` - Auto-discovery research triggers

### Error Handling Patterns
```java
// Always provide graceful fallbacks for external dependencies
try {
    // AI API operation
} catch (Exception e) {
    LOGGER.warn("AI request failed, using fallback: {}", e.getMessage());
    return createFallbackResponse();
}
```

## Critical Integration Points

### Eidolon Mod Integration
- Uses reflection for accessing Eidolon's internal classes (`EidolonCategoryExtension`)
- Codex positioning is **list-based**, not coordinate-based - entries append to category lists
- Effigy interactions integrate with existing Eidolon cooldown systems
- **Important**: Eidolon dependency is marked as `compileOnly` - integration is optional

### Datapack Loading Order
1. Basic deity definitions (`deities/`) load first
2. AI configurations (`ai_deities/`) link to existing deities via ResourceLocation
3. Chant definitions (`chants/`) register with keybind system
4. Codex entries (`codex_entries/`) integrate with Eidolon's chapter system

### Network Communication
- Uses Forge's networking system for client-server communication
- Chant slot assignments sync via `ChantSlotActivationPacket`
- AI responses are server-side only (client receives formatted chat messages)

## Common Development Patterns

### Command System Architecture
Commands are unified under `/eidolon-unchained` with alias `/eu`:
- Configuration: `/eidolon-unchained config <action>`
- API management: `/eidolon-unchained api set <provider> <key>`
- Debug tools: `/eidolon-unchained debug status`

### Datapack JSON Structure Examples
```json
// Basic Deity Configuration
{
  "name": "Verdania, Guardian of Nature",
  "description": "Ancient protector of forests and growing things",
  "color": "#4a7c59",
  "progression_stages": {
    "novice": { "reputation_required": 0 },
    "adept": { "reputation_required": 50 },
    "master": { "reputation_required": 200 }
  }
}

// AI Deity Personality
{
  "deity": "eidolonunchained:nature_deity",
  "personalities": {
    "novice": {
      "system_prompt": "You are Verdania, a wise nature deity...",
      "allowed_commands": ["give", "effect"],
      "command_restrictions": { "give": { "max_items": 5 } }
    }
  }
}

// Chant Definition (Working Structure)
{
  "name": "Nature's Communion",
  "description": "Connect with the spirit of nature",
  "category": "nature",
  "signs": ["eidolon:harmony", "eidolon:soul", "eidolon:sacred"],
  "effects": [
    { "type": "start_conversation", "deity": "eidolonunchained:nature_deity" },
    { "type": "apply_effect", "effect": "minecraft:regeneration", "duration": 600 }
  ]
}

// Complex Ritual (PROBLEMATIC - causes current errors)
{
  "name": "Ritual of Verdant Growth",
  "ritual_type": "nature_blessing",
  "requirements": [
    { "type": "item", "item": "minecraft:oak_sapling", "count": 4 },
    { "type": "biome", "biomes": ["minecraft:forest"] }
  ],
  "effects": [
    { "type": "area_effect", "radius": 10, "effect": "grow_plants" }
  ]
}
```

### Codex System Integration
- Categories position chapters via **automatic list ordering** (not manual coordinates)
- Use `target_chapter` field to specify Eidolon chapter integration
- Page types: `title`, `text`, `recipe`, `entity`, `list`, `ritual`, `crucible`, `workbench`, `smelting`
- **Critical**: Ensure target chapters exist before entries try to reference them

## Testing & Debugging

### Development Loop
1. Make code changes
2. Run `./gradlew compileJava` immediately (catches errors early)
3. Test with `/eidolon-unchained debug status` in-game
4. Validate configurations with `/eidolon-unchained config validate`
5. Check `latest.log` for runtime issues

### Error Priority Fixing Order
1. **Fix AI API Key Configuration** - Set up proper API key: `/eidolon-unchained api set gemini YOUR_KEY`
2. **Add Research Reset Commands** - Implement `/eidolon-unchained research clear <player>` for testing
3. **Fix Research Triggers** - Debug trigger event listeners and condition evaluation
4. **Fix Chant Cooldown Configuration** - Ensure JSON cooldown values are properly applied
5. **Standardize Language Keys** - Implement consistent `eidolonunchained.<system>.<category>.<name>.<type>` pattern
6. **Clean Content Entries** - Audit all text for consistency, fix missing translations

### Known Technical Debt
- Recent cleanup removed many placeholder classes - some imports may need updating
- In-code documentation needs improvement in newer modules
- Some edge case error handling could be more robust
- Chant system has schema inconsistencies between simple chants and complex rituals

### Performance Considerations
- AI API calls have configurable timeouts (default 30s)
- Conversation history grows over time - implement cleanup if memory becomes an issue
- Large datapack configurations may slow server startup
- Reflection-based Eidolon integration adds minor overhead

### Current System Status (From Implementation Analysis)
- **AI Deity System**: 90% complete, API key configuration issue preventing functionality
- **Flexible Chant System**: 95% complete, cooldown configuration not working properly  
- **Codex Integration**: 85% complete, race condition issues resolved but content needs standardization
- **Research System**: 75% complete, triggers broken, no reset mechanism for testing
- **Prayer System**: 60% complete, effigy integration commented out
- **Overall Completion**: ~85% - **FOCUS ON POLISHING EXISTING FEATURES**

## External Dependencies

- **Eidolon Mod**: Parent mod integration (optional dependency)
- **Google Gemini API**: AI functionality requires API key configuration
- **Curios API**: For item integration features
- **Minecraft Forge 47.1.0**: Core modding framework for 1.20.1

Remember: The mod is designed to degrade gracefully when Eidolon is not present, but full functionality requires both mods and proper API configuration.
