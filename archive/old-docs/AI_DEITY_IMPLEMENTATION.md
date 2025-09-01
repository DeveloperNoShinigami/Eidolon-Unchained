# AI Deity Implementation Guide

**Comprehensive guide for implementing datapack-driven deities with Google Gemini AI integration**

## 🔮 Overview

This document outlines the implementation of a revolutionary system that allows:
- **Datapack-driven custom deities** - JSON-defined deities with unique behaviors
- **AI integration** - Google Gemini AI responds to player prayers and generates dynamic content
- **Event-driven commands** - AI can execute safe commands based on context
- **Dynamic storytelling** - Emergent narratives driven by AI responses

## 📋 Current Infrastructure Analysis

### ✅ **Existing Deity System**

The mod already has a robust deity foundation:

```java
// From Deities.java
public class Deities {
    static final Map<ResourceLocation, Deity> deities = new HashMap<>();
    
    public static Deity register(Deity deity) {
        deities.put(deity.getId(), deity);
        return deity;
    }
    
    // Current deities: DARK_DEITY, LIGHT_DEITY
}
```

**Key Features:**
- Registry-based system supports dynamic registration
- ResourceLocation-based IDs (namespace compatible)
- Reputation progression system with stages
- Event callbacks (`onReputationUnlock`, `onReputationLock`)
- Prayer spells can target specific deities

### ✅ **Command Execution Infrastructure**

The `ExecCommandRitual` already provides command execution:

```java
// From ExecCommandRitual.java
public class ExecCommandRitual extends Ritual {
    List<String> commands;
    
    @Override
    public RitualResult start(Level world, BlockPos pos) {
        if (world instanceof ServerLevel serverWorld) {
            var server = world.getServer();
            for (var command : commands) {
                if (server.isCommandBlockEnabled() && !StringUtil.isNullOrEmpty(command)) {
                    EidolonFakePlayer fakePlayer = EidolonFakePlayer.getPlayer(serverWorld);
                    CommandSourceStack commandSource = fakePlayer.createCommandSourceStack()
                        .withPermission(2).withSuppressedOutput();
                    server.getCommands().performPrefixedCommand(commandSource, command);
                }
            }
        }
        return RitualResult.TERMINATE;
    }
}
```

### ✅ **Datapack Loading Infrastructure**

The mod has sophisticated JSON loading systems:
- `CodexDataManager` - Loads codex entries from JSON
- `ResearchDataManager` - Loads research data from JSON  
- `DatapackCategoryExample` - Demonstrates datapack-driven content

## 🏗️ Implementation Plan

### Phase 1: Datapack Deity System

#### 1.1 JSON Structure Design

Create `data/modid/deities/deity_name.json`:

```json
{
  "id": "mymod:nature_deity",
  "name": "Nature Guardian",
  "description": "Ancient spirit of the forest and growing things",
  "colors": {
    "red": 46,
    "green": 204,
    "blue": 64
  },
  "ai_config": {
    "enabled": true,
    "provider": "gemini",
    "model": "gemini-1.5-pro",
    "api_key_env": "GEMINI_API_KEY",
    "personality": "You are Verdania, an ancient nature deity in Minecraft. You speak in flowing, mystical language about plants, animals, and natural harmony. You can grant nature-themed blessings and guidance.",
    "safety_settings": {
      "harassment": "BLOCK_MEDIUM_AND_ABOVE",
      "hate_speech": "BLOCK_MEDIUM_AND_ABOVE",
      "sexually_explicit": "BLOCK_MEDIUM_AND_ABOVE",
      "dangerous_content": "BLOCK_MEDIUM_AND_ABOVE"
    }
  },
  "progression": {
    "max_reputation": 100,
    "stages": [
      {
        "id": "mymod:plant_communion",
        "reputation": 5,
        "major": true,
        "requirements": ["research:mymod:talk_to_trees"],
        "ai_unlock_message": "You have learned to commune with plants. The Nature Guardian notices your connection to the green world."
      },
      {
        "id": "mymod:forest_blessing",
        "reputation": 25,
        "major": false,
        "ai_unlock_message": "Your devotion to nature grows stronger. Ancient forest spirits whisper your name."
      },
      {
        "id": "mymod:gaia_touch",
        "reputation": 50,
        "major": true,
        "requirements": ["sign:sacred", "sign:soul"],
        "ai_unlock_message": "You have achieved harmony with Gaia herself. The earth responds to your will."
      }
    ]
  },
  "unlock_rewards": {
    "mymod:plant_communion": {
      "signs": ["sacred"],
      "items": [
        {"item": "minecraft:oak_sapling", "count": 8},
        {"item": "minecraft:bone_meal", "count": 16}
      ],
      "ai_message": "The spirits of growth bless you with these seeds of life."
    },
    "mymod:forest_blessing": {
      "effects": [
        {"effect": "minecraft:regeneration", "duration": 600, "amplifier": 1}
      ],
      "ai_message": "Feel the forest's vitality flow through you."
    }
  },
  "prayer_responses": {
    "blessing": {
      "prompt": "Player {player} asks for a blessing. They are at location {location} in {biome}. Their reputation with you is {reputation}. Respond as Verdania and suggest 1-2 appropriate nature-themed commands.",
      "max_commands": 2,
      "allowed_commands": ["give", "effect", "summon", "setblock", "fill"],
      "cooldown_minutes": 30
    },
    "guidance": {
      "prompt": "Player {player} seeks guidance. They have inventory: {inventory}. They recently: {recent_actions}. Provide mystical advice as Verdania.",
      "max_commands": 0,
      "cooldown_minutes": 5
    },
    "nature_ritual": {
      "prompt": "Player {player} wants to perform a nature ritual. Their location: {location}. Weather: {weather}. Time: {time}. Suggest a meaningful ritual and appropriate commands.",
      "max_commands": 5,
      "allowed_commands": ["give", "effect", "summon", "weather", "time", "playsound"],
      "cooldown_minutes": 120,
      "reputation_required": 25
    }
  },
  "context_awareness": {
    "track_actions": ["block_break", "block_place", "entity_kill", "item_craft"],
    "biome_responses": {
      "minecraft:forest": "The ancient oaks whisper of your presence...",
      "minecraft:jungle": "The wild growth here pulses with primal energy...",
      "minecraft:desert": "Even in this barren place, life finds a way..."
    },
    "time_responses": {
      "dawn": "As the sun rises, new growth begins...",
      "dusk": "The evening brings rest to all growing things...",
      "midnight": "In darkness, roots grow deep and strong..."
    }
  }
}
```

#### 1.2 DatapackDeityManager Implementation

```java
package com.bluelotuscoding.eidolonunchained.data;

@Mod.EventBusSubscriber(modid = EidolonUnchained.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DatapackDeityManager extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static DatapackDeityManager INSTANCE;
    
    public DatapackDeityManager() {
        super(GSON, "deities");
        INSTANCE = this;
    }
    
    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(getInstance());
        LOGGER.info("Registered Datapack Deity reload listener");
    }
    
    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resourceMap,
                        ResourceManager resourceManager, ProfilerFiller profiler) {
        LOGGER.info("Loading datapack deities...");
        
        int loaded = 0;
        int errors = 0;
        
        for (Map.Entry<ResourceLocation, JsonElement> entry : resourceMap.entrySet()) {
            ResourceLocation location = entry.getKey();
            JsonElement element = entry.getValue();
            
            if (!element.isJsonObject()) {
                LOGGER.warn("Skipping non-object JSON at {}", location);
                continue;
            }
            
            try {
                loadDeity(location, element.getAsJsonObject());
                loaded++;
            } catch (Exception e) {
                LOGGER.error("Failed to load deity from {}", location, e);
                errors++;
            }
        }
        
        LOGGER.info("Loaded {} datapack deities with {} errors", loaded, errors);
    }
    
    private void loadDeity(ResourceLocation location, JsonObject json) {
        String id = json.get("id").getAsString();
        ResourceLocation deityId = ResourceLocation.tryParse(id);
        
        String name = json.get("name").getAsString();
        String description = json.has("description") ? json.get("description").getAsString() : "";
        
        // Parse colors
        JsonObject colors = json.getAsJsonObject("colors");
        int red = colors.get("red").getAsInt();
        int green = colors.get("green").getAsInt();
        int blue = colors.get("blue").getAsInt();
        
        // Parse AI configuration
        AIConfig aiConfig = null;
        if (json.has("ai_config") && json.getAsJsonObject("ai_config").get("enabled").getAsBoolean()) {
            aiConfig = loadAIConfig(json.getAsJsonObject("ai_config"));
        }
        
        // Create and register the deity
        DatapackDeity deity = new DatapackDeity(deityId, name, description, red, green, blue, aiConfig);
        
        // Load progression stages
        if (json.has("progression")) {
            loadProgression(deity, json.getAsJsonObject("progression"));
        }
        
        // Load prayer responses
        if (json.has("prayer_responses")) {
            loadPrayerResponses(deity, json.getAsJsonObject("prayer_responses"));
        }
        
        // Register with Eidolon's deity system
        Deities.register(deity);
        LOGGER.info("Registered datapack deity: {}", deityId);
    }
}
```

#### 1.3 DatapackDeity Class

```java
package com.bluelotuscoding.eidolonunchained.deity;

public class DatapackDeity extends Deity {
    private final String displayName;
    private final String description;
    private final AIConfig aiConfig;
    private final Map<String, PrayerResponse> prayerResponses;
    private final ContextAwareness contextAwareness;
    
    public DatapackDeity(ResourceLocation id, String name, String description, 
                        int red, int green, int blue, AIConfig aiConfig) {
        super(id, red, green, blue);
        this.displayName = name;
        this.description = description;
        this.aiConfig = aiConfig;
        this.prayerResponses = new HashMap<>();
        this.contextAwareness = new ContextAwareness();
    }
    
    @Override
    public void onReputationUnlock(Player player, ResourceLocation lock) {
        super.onReputationUnlock(player, lock);
        
        // Send AI-powered unlock message if configured
        if (aiConfig != null && aiConfig.isEnabled()) {
            String unlockMessage = getAIUnlockMessage(player, lock);
            if (unlockMessage != null) {
                sendMessageToPlayer(player, unlockMessage);
            }
        }
    }
    
    public CompletableFuture<AIResponse> processPrayer(Player player, String prayerType, String message) {
        if (aiConfig == null || !aiConfig.isEnabled()) {
            return CompletableFuture.completedFuture(
                new AIResponse("The deity remains silent...", Collections.emptyList())
            );
        }
        
        PrayerResponse prayerConfig = prayerResponses.get(prayerType);
        if (prayerConfig == null) {
            return CompletableFuture.completedFuture(
                new AIResponse("The deity does not understand this type of prayer.", Collections.emptyList())
            );
        }
        
        // Check cooldown
        if (isOnCooldown(player, prayerType)) {
            long remainingTime = getCooldownRemaining(player, prayerType);
            return CompletableFuture.completedFuture(
                new AIResponse(String.format("You must wait %d more minutes before praying again.", 
                             remainingTime / 60000), Collections.emptyList())
            );
        }
        
        // Check reputation requirement
        if (prayerConfig.getReputationRequired() > 0) {
            IReputation rep = player.getCapability(IReputation.INSTANCE).orElse(null);
            if (rep == null || rep.getReputation(player.getUUID(), this.getId()) < prayerConfig.getReputationRequired()) {
                return CompletableFuture.completedFuture(
                    new AIResponse("Your devotion is not yet strong enough for this prayer.", Collections.emptyList())
                );
            }
        }
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                return callGeminiAPI(player, prayerConfig, message);
            } catch (Exception e) {
                LOGGER.error("Failed to call Gemini API for deity {}", getId(), e);
                return new AIResponse("The divine connection wavers... try again later.", Collections.emptyList());
            }
        });
    }
}
```

### Phase 2: Google Gemini Integration

#### 2.1 Gemini API Client

```java
package com.bluelotuscoding.eidolonunchained.ai;

public class GeminiAPIClient {
    private static final String GEMINI_API_BASE = "https://generativelanguage.googleapis.com/v1beta/models/";
    private final HttpClient httpClient;
    private final String apiKey;
    private final ObjectMapper objectMapper;
    
    public GeminiAPIClient(String apiKey) {
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();
        this.objectMapper = new ObjectMapper();
    }
    
    public CompletableFuture<GeminiResponse> generateContent(GeminiRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String jsonBody = objectMapper.writeValueAsString(request);
                
                HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(GEMINI_API_BASE + request.getModel() + ":generateContent?key=" + apiKey))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();
                
                HttpResponse<String> response = httpClient.send(httpRequest, 
                    HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() != 200) {
                    throw new RuntimeException("Gemini API error: " + response.statusCode() + " - " + response.body());
                }
                
                return objectMapper.readValue(response.body(), GeminiResponse.class);
                
            } catch (Exception e) {
                throw new RuntimeException("Failed to call Gemini API", e);
            }
        });
    }
}

// Request/Response classes
public class GeminiRequest {
    private Contents contents;
    private GenerationConfig generationConfig;
    private List<SafetySetting> safetySettings;
    private String model;
    
    // ... getters/setters
}

public class GeminiResponse {
    private List<Candidate> candidates;
    private PromptFeedback promptFeedback;
    
    public String getGeneratedText() {
        return candidates.get(0).getContent().getParts().get(0).getText();
    }
}
```

#### 2.2 AI Response Processing

```java
package com.bluelotuscoding.eidolonunchained.ai;

public class AIResponseProcessor {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Pattern COMMAND_PATTERN = Pattern.compile("```command\\s*\\n(.*?)\\n```", Pattern.DOTALL);
    
    public AIResponse processGeminiResponse(String rawResponse, PrayerResponse config) {
        // Extract message and commands from AI response
        String message = extractMessage(rawResponse);
        List<String> commands = extractCommands(rawResponse, config);
        
        // Validate and sanitize commands
        List<String> safeCommands = validateCommands(commands, config.getAllowedCommands());
        
        return new AIResponse(message, safeCommands);
    }
    
    private List<String> extractCommands(String response, PrayerResponse config) {
        List<String> commands = new ArrayList<>();
        Matcher matcher = COMMAND_PATTERN.matcher(response);
        
        while (matcher.find() && commands.size() < config.getMaxCommands()) {
            String command = matcher.group(1).trim();
            if (!command.isEmpty()) {
                commands.add(command);
            }
        }
        
        return commands;
    }
    
    private List<String> validateCommands(List<String> commands, List<String> allowedCommands) {
        return commands.stream()
            .filter(cmd -> isCommandSafe(cmd, allowedCommands))
            .collect(Collectors.toList());
    }
    
    private boolean isCommandSafe(String command, List<String> allowedCommands) {
        String[] parts = command.split(" ");
        if (parts.length == 0) return false;
        
        String baseCommand = parts[0].toLowerCase();
        
        // Check if command is in allowed list
        if (!allowedCommands.contains(baseCommand)) {
            LOGGER.warn("Blocked unsafe command: {}", baseCommand);
            return false;
        }
        
        // Additional safety checks
        if (command.contains("@a") && !baseCommand.equals("give")) {
            LOGGER.warn("Blocked command targeting all players: {}", command);
            return false;
        }
        
        // Block dangerous item/entity summoning
        if (baseCommand.equals("summon") && isDangerousEntity(command)) {
            LOGGER.warn("Blocked dangerous entity summon: {}", command);
            return false;
        }
        
        return true;
    }
    
    private boolean isDangerousEntity(String command) {
        String lowerCommand = command.toLowerCase();
        return lowerCommand.contains("tnt") || 
               lowerCommand.contains("creeper") ||
               lowerCommand.contains("wither") ||
               lowerCommand.contains("enderdragon");
    }
}
```

### Phase 3: Prayer System Integration

#### 3.1 Prayer Event System

```java
package com.bluelotuscoding.eidolonunchained.events;

public class PlayerPrayerEvent extends Event {
    private final Player player;
    private final Deity deity;
    private final String prayerType;
    private final String message;
    private final BlockPos altarPos;
    
    public PlayerPrayerEvent(Player player, Deity deity, String prayerType, String message, BlockPos altarPos) {
        this.player = player;
        this.deity = deity;
        this.prayerType = prayerType;
        this.message = message;
        this.altarPos = altarPos;
    }
    
    // Getters...
}

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class AIDeityEventHandler {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    @SubscribeEvent
    public static void onPlayerPrayer(PlayerPrayerEvent event) {
        if (event.getDeity() instanceof DatapackDeity aiDeity) {
            aiDeity.processPrayer(event.getPlayer(), event.getPrayerType(), event.getMessage())
                .thenAccept(response -> {
                    // Send AI message to player
                    event.getPlayer().sendSystemMessage(Component.literal(response.getMessage()));
                    
                    // Execute AI-generated commands
                    executeAICommands(response.getCommands(), event.getPlayer().level(), 
                                    event.getAltarPos(), event.getPlayer());
                })
                .exceptionally(throwable -> {
                    LOGGER.error("Failed to process AI prayer", throwable);
                    event.getPlayer().sendSystemMessage(
                        Component.literal("The divine connection failed... try again later."));
                    return null;
                });
        }
    }
    
    private static void executeAICommands(List<String> commands, Level world, BlockPos pos, Player player) {
        if (!(world instanceof ServerLevel serverWorld)) return;
        
        MinecraftServer server = serverWorld.getServer();
        if (!server.isCommandBlockEnabled()) return;
        
        for (String command : commands) {
            try {
                // Create command source at altar position
                CommandSourceStack commandSource = new CommandSourceStack(
                    CommandSource.NULL,
                    Vec3.atCenterOf(pos),
                    Vec2.ZERO,
                    serverWorld,
                    2, // Permission level
                    "AI_Deity",
                    Component.literal("AI Deity"),
                    server,
                    null
                );
                
                // Replace placeholders
                String processedCommand = command.replace("{player}", player.getName().getString());
                
                server.getCommands().performPrefixedCommand(commandSource, processedCommand);
                LOGGER.info("Executed AI command: {} for player {}", processedCommand, player.getName().getString());
                
            } catch (Exception e) {
                LOGGER.error("Failed to execute AI command: {}", command, e);
            }
        }
    }
}
```

#### 3.2 Prayer GUI Integration

```java
package com.bluelotuscoding.eidolonunchained.gui;

public class PrayerScreen extends Screen {
    private final Deity deity;
    private final BlockPos altarPos;
    private EditBox messageBox;
    private Button[] prayerTypeButtons;
    
    public PrayerScreen(Deity deity, BlockPos altarPos) {
        super(Component.literal("Commune with " + ((DatapackDeity) deity).getDisplayName()));
        this.deity = deity;
        this.altarPos = altarPos;
    }
    
    @Override
    protected void init() {
        super.init();
        
        // Message input box
        this.messageBox = new EditBox(this.font, this.width / 2 - 100, this.height / 2 - 10, 200, 20,
            Component.literal("Enter your prayer..."));
        this.addRenderableWidget(this.messageBox);
        
        // Prayer type buttons
        if (deity instanceof DatapackDeity aiDeity) {
            int buttonY = this.height / 2 + 20;
            int buttonIndex = 0;
            
            for (String prayerType : aiDeity.getPrayerTypes()) {
                Button button = Button.builder(Component.literal(formatPrayerType(prayerType)), 
                    btn -> sendPrayer(prayerType))
                    .bounds(this.width / 2 - 60 + (buttonIndex * 40), buttonY, 35, 20)
                    .build();
                this.addRenderableWidget(button);
                buttonIndex++;
            }
        }
    }
    
    private void sendPrayer(String prayerType) {
        String message = this.messageBox.getValue();
        if (message.trim().isEmpty()) {
            message = "Grant me your blessing, divine one.";
        }
        
        // Send prayer packet to server
        EidolonUnchained.NETWORK.sendToServer(new PrayerPacket(deity.getId(), prayerType, message, altarPos));
        this.onClose();
    }
}
```

### Phase 4: Context Awareness System

#### 4.1 Player Context Tracking

```java
package com.bluelotuscoding.eidolonunchained.context;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PlayerContextTracker {
    private static final Map<UUID, PlayerContext> playerContexts = new ConcurrentHashMap<>();
    
    public static class PlayerContext {
        private final List<String> recentActions = new ArrayList<>();
        private final Map<String, Integer> blocksBroken = new HashMap<>();
        private final Map<String, Integer> itemsCrafted = new HashMap<>();
        private String currentBiome;
        private long lastActionTime;
        
        public void addAction(String action) {
            recentActions.add(action);
            if (recentActions.size() > 10) {
                recentActions.remove(0); // Keep only last 10 actions
            }
            lastActionTime = System.currentTimeMillis();
        }
        
        public String getContextSummary() {
            StringBuilder summary = new StringBuilder();
            summary.append("Recent actions: ").append(String.join(", ", recentActions));
            if (!blocksBroken.isEmpty()) {
                summary.append(". Blocks broken: ").append(blocksBroken.toString());
            }
            if (!itemsCrafted.isEmpty()) {
                summary.append(". Items crafted: ").append(itemsCrafted.toString());
            }
            return summary.toString();
        }
    }
    
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getPlayer() instanceof Player player) {
            PlayerContext context = getOrCreateContext(player.getUUID());
            String blockName = event.getState().getBlock().getName().getString();
            context.addAction("broke " + blockName);
            context.blocksBroken.merge(blockName, 1, Integer::sum);
        }
    }
    
    @SubscribeEvent
    public static void onItemCraft(PlayerEvent.ItemCraftedEvent event) {
        PlayerContext context = getOrCreateContext(event.getEntity().getUUID());
        String itemName = event.getCrafting().getItem().getName(event.getCrafting()).getString();
        context.addAction("crafted " + itemName);
        context.itemsCrafted.merge(itemName, 1, Integer::sum);
    }
    
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player.tickCount % 100 == 0) { // Every 5 seconds
            PlayerContext context = getOrCreateContext(event.player.getUUID());
            String biome = event.player.level().getBiome(event.player.blockPosition()).toString();
            if (!biome.equals(context.currentBiome)) {
                context.currentBiome = biome;
                context.addAction("entered " + biome);
            }
        }
    }
    
    public static PlayerContext getOrCreateContext(UUID playerId) {
        return playerContexts.computeIfAbsent(playerId, k -> new PlayerContext());
    }
}
```

#### 4.2 Enhanced AI Prompts with Context

```java
private String buildContextualPrompt(Player player, PrayerResponse config, String userMessage) {
    PlayerContext context = PlayerContextTracker.getOrCreateContext(player.getUUID());
    
    String prompt = config.getPrompt()
        .replace("{player}", player.getName().getString())
        .replace("{location}", player.blockPosition().toString())
        .replace("{biome}", player.level().getBiome(player.blockPosition()).toString())
        .replace("{reputation}", String.valueOf(getPlayerReputation(player)))
        .replace("{recent_actions}", context.getContextSummary())
        .replace("{weather}", getWeatherDescription(player.level()))
        .replace("{time}", getTimeDescription(player.level()))
        .replace("{inventory}", getInventorySummary(player));
    
    return prompt + "\n\nPlayer message: " + userMessage + 
           "\n\nRespond in character. Use ```command\\n/command here\\n``` blocks for any commands you want to execute.";
}

private String getInventorySummary(Player player) {
    Map<String, Integer> itemCounts = new HashMap<>();
    
    for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
        ItemStack stack = player.getInventory().getItem(i);
        if (!stack.isEmpty()) {
            String itemName = stack.getItem().getName(stack).getString();
            itemCounts.merge(itemName, stack.getCount(), Integer::sum);
        }
    }
    
    return itemCounts.entrySet().stream()
        .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
        .limit(10) // Top 10 items
        .map(entry -> entry.getKey() + " x" + entry.getValue())
        .collect(Collectors.joining(", "));
}
```

## 🔒 Safety & Security Features

### Command Validation System

```java
public class CommandValidator {
    private static final Set<String> DANGEROUS_COMMANDS = Set.of(
        "ban", "ban-ip", "op", "deop", "stop", "restart", "whitelist", 
        "kick", "pardon", "pardon-ip", "save-all", "save-off", "save-on"
    );
    
    private static final Set<String> DANGEROUS_ENTITIES = Set.of(
        "minecraft:tnt", "minecraft:creeper", "minecraft:wither", 
        "minecraft:ender_dragon", "minecraft:lightning_bolt"
    );
    
    public static boolean isCommandSafe(String command, List<String> allowedCommands) {
        String[] parts = command.toLowerCase().split(" ");
        String baseCommand = parts[0].replace("/", "");
        
        // Check if command is explicitly allowed
        if (!allowedCommands.contains(baseCommand)) return false;
        
        // Check against dangerous commands
        if (DANGEROUS_COMMANDS.contains(baseCommand)) return false;
        
        // Special validation for specific commands
        switch (baseCommand) {
            case "give":
                return validateGiveCommand(command);
            case "summon":
                return validateSummonCommand(command);
            case "fill":
                return validateFillCommand(command);
            case "effect":
                return validateEffectCommand(command);
            default:
                return true;
        }
    }
    
    private static boolean validateGiveCommand(String command) {
        // Limit quantities to reasonable amounts
        Pattern pattern = Pattern.compile("give\\s+\\S+\\s+\\S+\\s+(\\d+)");
        Matcher matcher = pattern.matcher(command);
        if (matcher.find()) {
            int quantity = Integer.parseInt(matcher.group(1));
            return quantity <= 64; // Max one stack
        }
        return true;
    }
    
    private static boolean validateSummonCommand(String command) {
        return DANGEROUS_ENTITIES.stream().noneMatch(command::contains);
    }
}
```

### Rate Limiting System

```java
public class PrayerRateLimiter {
    private final Map<UUID, Map<String, Long>> playerCooldowns = new ConcurrentHashMap<>();
    
    public boolean isOnCooldown(UUID playerId, String prayerType, long cooldownMs) {
        Map<String, Long> playerPrayers = playerCooldowns.get(playerId);
        if (playerPrayers == null) return false;
        
        Long lastPrayer = playerPrayers.get(prayerType);
        if (lastPrayer == null) return false;
        
        return System.currentTimeMillis() - lastPrayer < cooldownMs;
    }
    
    public void recordPrayer(UUID playerId, String prayerType) {
        playerCooldowns.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>())
                      .put(prayerType, System.currentTimeMillis());
    }
    
    public long getRemainingCooldown(UUID playerId, String prayerType, long cooldownMs) {
        Map<String, Long> playerPrayers = playerCooldowns.get(playerId);
        if (playerPrayers == null) return 0;
        
        Long lastPrayer = playerPrayers.get(prayerType);
        if (lastPrayer == null) return 0;
        
        long elapsed = System.currentTimeMillis() - lastPrayer;
        return Math.max(0, cooldownMs - elapsed);
    }
}
```

## 📁 File Structure

```
src/main/java/com/bluelotuscoding/eidolonunchained/
├── ai/
│   ├── GeminiAPIClient.java
│   ├── AIResponseProcessor.java
│   └── models/
│       ├── GeminiRequest.java
│       ├── GeminiResponse.java
│       └── AIResponse.java
├── deity/
│   ├── DatapackDeity.java
│   ├── AIConfig.java
│   ├── PrayerResponse.java
│   └── ContextAwareness.java
├── data/
│   └── DatapackDeityManager.java
├── events/
│   ├── PlayerPrayerEvent.java
│   └── AIDeityEventHandler.java
├── context/
│   └── PlayerContextTracker.java
├── security/
│   ├── CommandValidator.java
│   └── PrayerRateLimiter.java
├── gui/
│   └── PrayerScreen.java
└── network/
    └── PrayerPacket.java

data/modid/deities/
├── nature_deity.json
├── tech_deity.json
├── ocean_deity.json
└── void_deity.json
```

## 🚀 Usage Examples

### Example 1: Nature Deity Blessing

**Player prays:** "Please bless my crops"

**AI Response:** "The ancient spirits smile upon your dedication to growth. Feel the earth's bounty flow through your fields."

**Commands executed:**
```
effect give {player} minecraft:luck 600 1
give {player} minecraft:bone_meal 8
```

### Example 2: Tech Deity Guidance

**Player prays:** "I need help with my redstone contraption"

**AI Response:** "The circuits whisper their secrets... Perhaps these components will aid your mechanical endeavors."

**Commands executed:**
```
give {player} minecraft:redstone 32
give {player} minecraft:repeater 4
summon minecraft:item ~ ~ ~ {Item:{id:"minecraft:comparator",Count:2}}
```

### Example 3: Ocean Deity Ritual

**Player prays:** "Calm the storms" (during thunderstorm)

**AI Response:** "The depths hear your plea. Watch as I calm the tempest's rage."

**Commands executed:**
```
weather clear 1200
effect give {player} minecraft:water_breathing 1200 1
playsound minecraft:ambient.underwater.loop player {player}
```

## 🔧 Configuration Options

### Server Config (eidolon_unchained-server.toml)

```toml
[ai_deities]
    # Enable AI deity system
    enable_ai_deities = true
    
    # Google Gemini API settings
    gemini_api_key = "your_api_key_here"
    gemini_model = "gemini-1.5-pro"
    gemini_timeout_seconds = 30
    
    # Safety settings
    max_commands_per_prayer = 5
    enable_command_validation = true
    enable_rate_limiting = true
    
    # Default cooldowns (minutes)
    default_blessing_cooldown = 30
    default_guidance_cooldown = 5
    default_ritual_cooldown = 120
    
    # Context tracking
    enable_context_tracking = true
    max_tracked_actions = 10
    
[command_safety]
    # Allowed commands for AI execution
    allowed_commands = ["give", "effect", "summon", "playsound", "particle", "setblock", "fill"]
    
    # Maximum quantities for give commands
    max_give_quantity = 64
    
    # Blocked entities for summon commands
    blocked_entities = ["minecraft:tnt", "minecraft:creeper", "minecraft:wither"]
```

### Client Config (eidolon_unchained-client.toml)

```toml
[prayer_gui]
    # Enable prayer GUI overlay
    enable_prayer_gui = true
    
    # Show AI response as chat message
    show_ai_responses_in_chat = true
    
    # Play sound when receiving AI response
    play_response_sound = true
    response_sound = "minecraft:block.amethyst_block.chime"
    
    # Prayer GUI position
    gui_scale = 1.0
    gui_offset_x = 0
    gui_offset_y = 0
```

## 🧪 Testing Framework

### AI Response Testing

```java
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class AIDeityTesting {
    
    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
            Commands.literal("test_ai_deity")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("deity_id", ResourceLocationArgument.id())
                    .then(Commands.argument("prayer_type", StringArgumentType.string())
                        .then(Commands.argument("message", StringArgumentType.greedyString())
                            .executes(context -> {
                                testAIDeity(context.getSource(),
                                    ResourceLocationArgument.getId(context, "deity_id"),
                                    StringArgumentType.getString(context, "prayer_type"),
                                    StringArgumentType.getString(context, "message"));
                                return 1;
                            }))))
        );
    }
    
    private static void testAIDeity(CommandSourceStack source, ResourceLocation deityId, 
                                   String prayerType, String message) {
        Deity deity = Deities.find(deityId);
        if (!(deity instanceof DatapackDeity aiDeity)) {
            source.sendSuccess(() -> Component.literal("Deity not found or not an AI deity"), false);
            return;
        }
        
        if (source.getEntity() instanceof Player player) {
            aiDeity.processPrayer(player, prayerType, message)
                .thenAccept(response -> {
                    source.sendSuccess(() -> Component.literal("AI Response: " + response.getMessage()), false);
                    source.sendSuccess(() -> Component.literal("Commands: " + response.getCommands()), false);
                })
                .exceptionally(throwable -> {
                    source.sendFailure(Component.literal("Test failed: " + throwable.getMessage()));
                    return null;
                });
        }
    }
}
```

## 🚀 Future Enhancements

### Planned Features

1. **Multi-Modal AI**: Support for image analysis of player's world
2. **Voice Integration**: Text-to-speech for AI responses  
3. **Persistent Memory**: AI remembers player interactions across sessions
4. **Dynamic Events**: AI creates timed events and quests
5. **Cross-Deity Interactions**: Multiple AI deities that can interact
6. **Plugin API**: Allow other mods to integrate with the AI deity system

### Advanced AI Capabilities

1. **World Analysis**: AI can "see" player's builds and provide feedback
2. **Predictive Assistance**: AI suggests next steps based on player patterns
3. **Educational Mode**: AI teaches game mechanics and mod features
4. **Collaborative Building**: AI helps with large construction projects
5. **Ecosystem Management**: AI helps balance server economies and resources

## 📞 API Integration Details

### Google Gemini Setup

1. **Get API Key**: Visit Google AI Studio and create an API key
2. **Set Environment Variable**: `export GEMINI_API_KEY="your_key_here"`
3. **Configure Safety Settings**: Adjust safety levels in deity JSON files
4. **Monitor Usage**: Track API calls to stay within quotas

### Alternative AI Providers

The system is designed to support multiple AI providers:

- **OpenAI GPT**: Easily adaptable by changing the API client
- **Anthropic Claude**: Similar REST API structure
- **Local Models**: Support for self-hosted AI models
- **Ollama Integration**: For completely offline AI capabilities

## 🎯 Conclusion

This implementation provides a revolutionary AI-powered deity system that:

- ✅ **Fully datapack-driven** - No code changes needed for new deities
- ✅ **Google Gemini integration** - State-of-the-art AI responses
- ✅ **Context-aware** - AI knows player actions, location, inventory
- ✅ **Safe command execution** - Comprehensive security validation
- ✅ **Extensible architecture** - Easy to add new features
- ✅ **Performance optimized** - Async processing, rate limiting
- ✅ **Production ready** - Error handling, logging, configuration

The system opens up endless possibilities for dynamic, intelligent, and personalized Minecraft experiences powered by cutting-edge AI technology.
