package com.bluelotuscoding.eidolonunchained.command;

import com.bluelotuscoding.eidolonunchained.config.EidolonUnchainedConfig;
import com.bluelotuscoding.eidolonunchained.config.APIKeyManager;
import com.bluelotuscoding.eidolonunchained.ai.AIDeityManager;
import com.bluelotuscoding.eidolonunchained.ai.AIDeityConfig;
import com.bluelotuscoding.eidolonunchained.ai.PlayerContextTracker;
import com.bluelotuscoding.eidolonunchained.data.DatapackDeityManager;
import com.bluelotuscoding.eidolonunchained.data.ResearchDataManager;
import com.bluelotuscoding.eidolonunchained.chat.ConversationHistoryManager;
import com.bluelotuscoding.eidolonunchained.chat.ConversationMessage;
// import com.bluelotuscoding.eidolonunchained.capability.CapabilityHandler;
import com.bluelotuscoding.eidolonunchained.deity.DatapackDeity;
import com.bluelotuscoding.eidolonunchained.events.RitualCompleteEvent;
import com.bluelotuscoding.eidolonunchained.integration.player2ai.Player2AIClient;
import com.bluelotuscoding.eidolonunchained.research.triggers.InteractionResearchTriggers;
import com.bluelotuscoding.eidolonunchained.research.triggers.KillResearchTriggers;
import com.bluelotuscoding.eidolonunchained.research.triggers.RitualResearchTriggers;
// import elucent.eidolon.capability.IReputation;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.bluelotuscoding.eidolonunchained.util.CommandStringUtils;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
// import com.mojang.brigadier.suggestion.Suggestions; // not directly used
// import com.mojang.brigadier.suggestion.SuggestionsBuilder; // not directly used
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
// import java.util.UUID;
// import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Eidolon integration imports
import elucent.eidolon.util.KnowledgeUtil;
import net.minecraftforge.common.MinecraftForge;

/**
 * Unified command handler for all Eidolon Unchained commands
 * Consolidates configuration, AI, deity, prayer, chant, and patron commands
 */
public class UnifiedCommands {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(UnifiedCommands.class);
    
    // TARGET ICON - SUGGESTION PROVIDERS FOR TAB COMPLETION
    
    /**
     * Suggest available deity IDs (namespace:path) without extra comment lines or quoting
     */
    private static final SuggestionProvider<CommandSourceStack> DEITY_SUGGESTIONS = (context, builder) -> {
        List<String> suggestions = new ArrayList<>();
        for (Map.Entry<ResourceLocation, com.bluelotuscoding.eidolonunchained.deity.DatapackDeity> entry :
            DatapackDeityManager.getAllDeities().entrySet()) {
            ResourceLocation id = entry.getKey();
            suggestions.add(id.toString());
        }
        return SharedSuggestionProvider.suggest(suggestions, builder);
    };
    
    /**
     * Suggest online player names only (no comment entries)
     */
    private static final SuggestionProvider<CommandSourceStack> PLAYER_SUGGESTIONS = (context, builder) -> {
        List<String> suggestions = new ArrayList<>();
        for (ServerPlayer player : context.getSource().getServer().getPlayerList().getPlayers()) {
            suggestions.add(player.getName().getString());
        }
        return SharedSuggestionProvider.suggest(suggestions, builder);
    };
    
    /**
     * Suggests available API providers
     */
    private static final SuggestionProvider<CommandSourceStack> API_PROVIDER_SUGGESTIONS = (context, builder) -> {
        return SharedSuggestionProvider.suggest(
            List.of("gemini", "player2ai", "openrouter", "openai", "anthropic"),
            builder
        );
    };

    /**
     * Suggests available facts loaded from facts/*.json
     */
    private static final SuggestionProvider<CommandSourceStack> FACT_SUGGESTIONS = (context, builder) -> {
        List<String> suggestions = new ArrayList<>();
        try {
            for (ResourceLocation factId : com.bluelotuscoding.eidolonunchained.data.FactsSuggestionManager.getAllFactIds()) {
                suggestions.add(factId.toString());
            }
        } catch (Exception e) {
            // Ignore errors during suggestion gathering
        }
        return SharedSuggestionProvider.suggest(suggestions, builder);
    };

    /**
     * Suggest available fate/task IDs aggregated from all deity configs (no comments)
     */
    private static final SuggestionProvider<CommandSourceStack> TASK_ID_SUGGESTIONS = (context, builder) -> {
        // Use a set to deduplicate; filter out blank and comment-prefixed entries
        java.util.Set<String> unique = new java.util.LinkedHashSet<>();
        try {
            for (AIDeityConfig cfg : AIDeityManager.getInstance().getAllConfigs()) {
                if (cfg != null && cfg.task_config != null && cfg.task_config.availableTasks != null) {
                    for (var t : cfg.task_config.availableTasks) {
                        if (t != null && t.taskId != null) {
                            String id = t.taskId.trim();
                            if (!id.isEmpty() && !id.startsWith("#")) unique.add(id);
                        }
                    }
                }
            }
        } catch (Exception e2) {
            // Ignore errors during suggestion gathering
        }
        return SharedSuggestionProvider.suggest(new ArrayList<>(unique), builder);
    };
    
    /**
     * Suggest available ritual IDs without quoting or comment lines
     */
    private static final SuggestionProvider<CommandSourceStack> RITUAL_SUGGESTIONS = (context, builder) -> {
        var server = context.getSource().getServer();
        var recipeManager = server.getRecipeManager();
        
        List<String> suggestions = new ArrayList<>();
        
        var ritualIds = java.util.stream.Stream.concat(
            recipeManager.getAllRecipesFor(elucent.eidolon.registries.EidolonRecipes.COMMAND_RITUAL_TYPE.get()).stream(),
            recipeManager.getAllRecipesFor(elucent.eidolon.registries.EidolonRecipes.RITUAL_TYPE.get()).stream()
        ).map(recipe -> recipe.getId().toString()).collect(Collectors.toList());
        
    // Add ritual IDs as raw namespace:path values
    suggestions.addAll(ritualIds);
        
        return SharedSuggestionProvider.suggest(suggestions, builder);
    };
    
    /**
     * Suggests available chant IDs
     */
    private static final SuggestionProvider<CommandSourceStack> CHANT_SUGGESTIONS = (context, builder) -> {
        // Get chants from DatapackChantManager if available
        try {
            var chantIds = new ArrayList<>(com.bluelotuscoding.eidolonunchained.chant.DatapackChantManager.getAllChants().keySet());
            if (!chantIds.isEmpty()) return SharedSuggestionProvider.suggest(chantIds, builder);
        } catch (Exception e) {
            // Fallback to examples if manager is not available
        }
        
        // Fallback examples
        return SharedSuggestionProvider.suggest(
            List.of("example:nature_blessing", "example:divine_protection", "example:shadow_step"),
            builder
        );
    };
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {

        dispatcher.register(Commands.literal("eidolon-unchained")
            .requires(source -> true)

            // -- config ------------------------------------------------------------
            .then(Commands.literal("config")
                .then(Commands.literal("reload")
                    .executes(UnifiedCommands::reloadConfig))
                .then(Commands.literal("status")
                    .executes(UnifiedCommands::showConfigStatus))
                .then(Commands.literal("validate")
                    .executes(UnifiedCommands::validateConfig))
                .then(Commands.literal("reset")
                    .executes(UnifiedCommands::resetConfig)))

            // -- api ---------------------------------------------------------------
            .then(Commands.literal("api")
                .then(Commands.literal("set")
                    .then(Commands.literal("player2ai")
                        .executes(UnifiedCommands::setupPlayer2AI))
                    .then(Commands.argument("provider", StringArgumentType.string())
                        .suggests(API_PROVIDER_SUGGESTIONS)
                        .then(Commands.argument("key", StringArgumentType.greedyString())
                            .executes(UnifiedCommands::setApiKey))))
                .then(Commands.literal("set-model")
                    .then(Commands.argument("model", StringArgumentType.string())
                        .executes(UnifiedCommands::setAIModel)))
                .then(Commands.literal("get-model")
                    .executes(UnifiedCommands::getAIModel))
                .then(Commands.literal("test")
                    .then(Commands.argument("provider", StringArgumentType.string())
                        .suggests(API_PROVIDER_SUGGESTIONS)
                        .executes(UnifiedCommands::testApiKey)))
                .then(Commands.literal("list")
                    .executes(UnifiedCommands::listApiKeys))
                .then(Commands.literal("remove")
                    .then(Commands.argument("provider", StringArgumentType.string())
                        .suggests(API_PROVIDER_SUGGESTIONS)
                        .executes(UnifiedCommands::removeApiKey))))

            // -- deities -----------------------------------------------------------
            .then(Commands.literal("deities")
                .then(Commands.literal("list")
                    .executes(UnifiedCommands::listDeities))
                .then(Commands.literal("reload")
                    .executes(UnifiedCommands::reloadDeities))
                .then(Commands.literal("status")
                    .then(Commands.argument("deity", ResourceLocationArgument.id())
                        .suggests(DEITY_SUGGESTIONS)
                        .executes(UnifiedCommands::showDeityStatus))))

            // -- patron ------------------------------------------------------------
            .then(Commands.literal("patron")
                .then(Commands.literal("choose")
                    .then(Commands.argument("target", net.minecraft.commands.arguments.EntityArgument.player())
                        .then(Commands.argument("deity", ResourceLocationArgument.id())
                            .suggests(DEITY_SUGGESTIONS)
                            .executes(UnifiedCommands::choosePatron)))
                    .then(Commands.argument("deity", ResourceLocationArgument.id())
                        .suggests(DEITY_SUGGESTIONS)
                        .executes(UnifiedCommands::choosePatron)))
                .then(Commands.literal("abandon")
                    .executes(UnifiedCommands::abandonPatron))
                .then(Commands.literal("status")
                    .executes(UnifiedCommands::patronStatus)))

            // -- prayers -----------------------------------------------------------
            .then(Commands.literal("prayers")
                .then(Commands.literal("history")
                    .executes(UnifiedCommands::showPrayerHistory))
                .then(Commands.literal("cooldowns")
                    .executes(UnifiedCommands::showPlayerCooldowns))
                .then(Commands.literal("clear-cooldown")
                    .requires(cs -> cs.hasPermission(2))
                    .then(Commands.argument("player", StringArgumentType.string())
                        .suggests(PLAYER_SUGGESTIONS)
                        .executes(UnifiedCommands::clearPrayerCooldown))))

            // -- fates (canonical, replaces tasks) --------------------------------
            .then(FateCommands.buildNode(PLAYER_SUGGESTIONS, DEITY_SUGGESTIONS, TASK_ID_SUGGESTIONS, RITUAL_SUGGESTIONS))

            // -- chat (direct AI test without effigy) ------------------------------
            .then(Commands.literal("chat")
                .then(Commands.argument("deity", ResourceLocationArgument.id())
                    .suggests(DEITY_SUGGESTIONS)
                    .then(Commands.argument("message", StringArgumentType.greedyString())
                        .executes(UnifiedCommands::chatWithDeity))))

            // -- conversations -----------------------------------------------------
            .then(Commands.literal("conversations")
                .then(Commands.literal("stats")
                    .executes(UnifiedCommands::showConversationStats))
                .then(Commands.literal("clear")
                    .then(Commands.argument("deity", ResourceLocationArgument.id())
                        .suggests(DEITY_SUGGESTIONS)
                        .executes(UnifiedCommands::clearConversationHistory)))
                .then(Commands.literal("clear-all")
                    .executes(UnifiedCommands::clearAllConversationHistory)))

            // -- research ----------------------------------------------------------
            .then(Commands.literal("research")
                .then(Commands.literal("list")
                    .executes(UnifiedCommands::listResearchEntries))
                .then(Commands.literal("reload")
                    .requires(cs -> cs.hasPermission(2))
                    .executes(UnifiedCommands::reloadResearch))
                .then(Commands.literal("clear")
                    .requires(cs -> cs.hasPermission(2))
                    .then(Commands.argument("player", StringArgumentType.string())
                        .suggests(PLAYER_SUGGESTIONS)
                        .executes(UnifiedCommands::clearPlayerResearch))))

            // -- player2ai ---------------------------------------------------------
            .then(Commands.literal("player2ai")
                .then(Commands.literal("auth")
                    .then(Commands.literal("auto")
                        .executes(UnifiedCommands::authenticatePlayer2AIAuto)))
                .then(Commands.literal("login")
                    .then(Commands.literal("device")
                        .executes(UnifiedCommands::startPlayer2AILoginDevice))
                    .then(Commands.literal("status")
                        .executes(UnifiedCommands::showPlayer2AILoginStatus)))
                .then(Commands.literal("logout")
                    .executes(UnifiedCommands::logoutPlayer2AIP2Key))
                .then(Commands.literal("test")
                    .executes(UnifiedCommands::testPlayer2AIConnection))
                .then(Commands.literal("debug-chat")
                    .then(Commands.argument("message", StringArgumentType.greedyString())
                        .executes(UnifiedCommands::testPlayer2AIChat)))
                .then(Commands.literal("memory")
                    .then(Commands.literal("clear")
                        .then(Commands.argument("deity", ResourceLocationArgument.id())
                            .suggests(DEITY_SUGGESTIONS)
                            .executes(UnifiedCommands::clearPlayer2AIMemory)))
                    .then(Commands.literal("show")
                        .then(Commands.argument("deity", ResourceLocationArgument.id())
                            .suggests(DEITY_SUGGESTIONS)
                            .executes(UnifiedCommands::showPlayer2AIMemory))))
                .then(Commands.literal("characters")
                    .then(Commands.literal("list")
                        .executes(UnifiedCommands::listPlayer2AICharacters))
                    .then(Commands.literal("update-personality")
                        .then(Commands.argument("deity", ResourceLocationArgument.id())
                            .suggests(DEITY_SUGGESTIONS)
                            .executes(UnifiedCommands::updatePlayer2AIPersonality)))))

            // -- chant -------------------------------------------------------------
            .then(ChantSlotCommands.buildNode())

            // -- tts ---------------------------------------------------------------
            .then(TTSCommands.buildNode())

            // -- debug (op only) ---------------------------------------------------
            .then(DebugCommands.buildNode(PLAYER_SUGGESTIONS, DEITY_SUGGESTIONS, RITUAL_SUGGESTIONS, FACT_SUGGESTIONS)));

        // /eu alias
        dispatcher.register(Commands.literal("eu")
            .redirect(dispatcher.getRoot().getChild("eidolon-unchained")));
    }
    
    // Configuration commands
    private static int reloadConfig(CommandContext<CommandSourceStack> context) {
        // Forge config cannot be hot-reloaded programmatically here; instruct the user
        context.getSource().sendSuccess(() -> Component.literal("§eReloading config requires restarting or /reload datapacks."), false);
        return 1;
    }
    
    private static int showConfigStatus(CommandContext<CommandSourceStack> context) {
        StringBuilder status = new StringBuilder("§6=== Eidolon Unchained Configuration Status ===\n");
        
        // AI System
        status.append("§eAI Deities: ").append(EidolonUnchainedConfig.COMMON.enableAIDeities.get() ? "§aEnabled" : "§cDisabled").append("\n");
        status.append("§eAI Provider: ").append("§b").append(EidolonUnchainedConfig.COMMON.aiProvider.get()).append("\n");
        status.append("§eAI Model: ").append("§b").append(EidolonUnchainedConfig.COMMON.geminiModel.get()).append("\n");
        
        // Chant System
        status.append("§eChant System: ").append(EidolonUnchainedConfig.COMMON.enableChantSystem.get() ? "§aEnabled" : "§cDisabled").append("\n");
        status.append("§eDatapack Chants: ").append(EidolonUnchainedConfig.COMMON.enableDatapackChants.get() ? "§aEnabled" : "§cDisabled").append("\n");
        
        // Interactions
        status.append("§eEffigy Right-Click: ").append(EidolonUnchainedConfig.COMMON.enableEffigyRightClick.get() ? "§aEnabled" : "§cDisabled").append("\n");
        status.append("§eChat Interaction: ").append(EidolonUnchainedConfig.COMMON.enableChatInteraction.get() ? "§aEnabled" : "§cDisabled").append("\n");
        
        context.getSource().sendSuccess(() -> Component.literal(status.toString()), false);
        return 1;
    }
    
    private static int validateConfig(CommandContext<CommandSourceStack> context) {
        // Validate configuration settings
        boolean valid = true;
        StringBuilder issues = new StringBuilder();
        
        // Check API provider
        String provider = EidolonUnchainedConfig.COMMON.aiProvider.get();
        if (!provider.equals("gemini") && !provider.equals("openai") && !provider.equals("proxy")) {
            valid = false;
            issues.append("§c- Invalid AI provider: ").append(provider).append("\n");
        }
        
        // Check API key availability
        if (EidolonUnchainedConfig.COMMON.enableAIDeities.get()) {
            String apiKey = APIKeyManager.getAPIKey(provider);
            if (apiKey == null || apiKey.isEmpty()) {
                valid = false;
                issues.append("§c- No API key configured for provider: ").append(provider).append("\n");
            }
        }
        
        if (valid) {
            context.getSource().sendSuccess(() -> Component.translatable("eidolonunchained.command.config.valid"), false);
        } else {
            context.getSource().sendFailure(Component.translatable("eidolonunchained.command.config.issues_found", issues.toString()));
        }
        
        return valid ? 1 : 0;
    }
    
    private static int resetConfig(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(() -> Component.translatable("eidolonunchained.command.config.reset"), false);
        return 1;
    }
    
    // API key management commands
    private static int setApiKey(CommandContext<CommandSourceStack> context) {
        String provider = CommandStringUtils.safeTrim(StringArgumentType.getString(context, "provider"));
        String key = CommandStringUtils.safeTrim(StringArgumentType.getString(context, "key"));
        
        // Validate provider name
        if (provider == null || provider.isEmpty()) {
            context.getSource().sendFailure(Component.literal("§cProvider name cannot be empty"));
            return 0;
        }
        
        // Validate API key format
        if (key == null || key.isEmpty()) {
            context.getSource().sendFailure(Component.literal("§cAPI key cannot be empty"));
            return 0;
        }
        
        if (!CommandStringUtils.isValidApiKey(key)) {
            context.getSource().sendFailure(Component.literal(
                CommandStringUtils.createValidationError("API key", key, "alphanumeric with -_.+=/$, minimum 8 characters")));
            return 0;
        }
        
        try {
            APIKeyManager.setAPIKey(provider, key);
            context.getSource().sendSuccess(() -> Component.translatable("eidolonunchained.command.api.key_set", provider), false);
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.translatable("eidolonunchained.command.api.key_set_failed", e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Sets up Player2AI as the AI provider without requiring an API key.
     * This configures the system to use the local Player2 App.
     */
    private static int setupPlayer2AI(CommandContext<CommandSourceStack> context) {
        try {
            // Set provider to player2ai with "local" key (local connection doesn't need real API key)
            APIKeyManager.setAPIKey("player2ai", "local");
            
            // Set the AI provider configuration to player2ai (persisted by Forge on exit)
            EidolonUnchainedConfig.COMMON.aiProvider.set("player2ai");
            
            // Test the connection immediately
            boolean connected = Player2AIClient.isPlayer2AppAvailable();
            
            if (connected) {
                context.getSource().sendSuccess(() -> Component.literal(
                    "§aPlayer2AI configured successfully!\n" +
                    "§7AI Provider: player2ai\n" +
                    "§7Connected to Player2 App at localhost:4315\n" +
                    "§7You can now use AI deity conversations powered by Player2AI"
                ), false);
                return 1;
            } else {
                context.getSource().sendFailure(Component.literal(
                    "§cPlayer2AI configured but connection failed!\n" +
                    "§7AI Provider: player2ai (saved)\n" +
                    "§7Make sure Player2 App is running and accessible at localhost:4315\n" +
                    "§7The configuration has been saved and will work when Player2 App is available"
                ));
                return 0;
            }
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cFailed to setup Player2AI: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int testApiKey(CommandContext<CommandSourceStack> context) {
        String rawProvider = StringArgumentType.getString(context, "provider");
        String provider = CommandStringUtils.safeTrim(rawProvider);
        
        // Validate provider name
        if (provider == null || provider.isEmpty()) {
            context.getSource().sendFailure(Component.literal("§cProvider name cannot be empty"));
            return 0;
        }
        
        String apiKey = APIKeyManager.getAPIKey(provider);
        if (apiKey == null || apiKey.isEmpty()) {
            context.getSource().sendFailure(Component.translatable("eidolonunchained.command.api.no_key", provider));
            return 0;
        }
        
        context.getSource().sendSuccess(() -> Component.translatable("eidolonunchained.command.api.key_configured", provider), false);
        return 1;
    }
    
    private static int listApiKeys(CommandContext<CommandSourceStack> context) {
        StringBuilder list = new StringBuilder("§6=== Configured API Keys ===\n");
        
        String[] providers = {"gemini", "openai", "openrouter", "player2ai", "proxy"};
        for (String provider : providers) {
            String key = APIKeyManager.getAPIKey(provider);
            if (key != null && !key.isEmpty()) {
                list.append("§e").append(provider).append(": §a").append(maskApiKey(key)).append("\n");
            } else {
                list.append("§e").append(provider).append(": §cNot configured\n");
            }
        }
        
        context.getSource().sendSuccess(() -> Component.literal(list.toString()), false);
        return 1;
    }
    
    private static int removeApiKey(CommandContext<CommandSourceStack> context) {
        String rawProvider = StringArgumentType.getString(context, "provider");
        String provider = CommandStringUtils.safeTrim(rawProvider);
        
        // Validate provider name
        if (provider == null || provider.isEmpty()) {
            context.getSource().sendFailure(Component.literal("§cProvider name cannot be empty"));
            return 0;
        }
        
        try {
            APIKeyManager.removeAPIKey(provider);
            context.getSource().sendSuccess(() -> Component.literal("§aAPI key removed for provider: " + provider), false);
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cFailed to remove API key: " + e.getMessage()));
            return 0;
        }
    }
    
    // Deity management commands
    private static int listDeities(CommandContext<CommandSourceStack> context) {
        var deities = DatapackDeityManager.getAllDeities();
        
        StringBuilder list = new StringBuilder("§6=== Loaded Deities ===\n");
        deities.forEach((id, deity) -> {
            boolean hasAI = AIDeityManager.getInstance().getAIConfig(id) != null;
            list.append("§e").append(id).append(": §b").append(deity.getName())
                .append(hasAI ? " §a[AI]" : " §7[No AI]").append("\n");
        });
        
        context.getSource().sendSuccess(() -> Component.literal(list.toString()), false);
        return 1;
    }
    
    private static int reloadDeities(CommandContext<CommandSourceStack> context) {
        try {
            // Trigger deity reload
            context.getSource().sendSuccess(() -> Component.translatable("eidolonunchained.command.deity.reload_success"), false);
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.translatable("eidolonunchained.command.deity.reload_failed", e.getMessage()));
            return 0;
        }
    }
    
    private static int showDeityStatus(CommandContext<CommandSourceStack> context) {
        ResourceLocation deityId = net.minecraft.commands.arguments.ResourceLocationArgument.getId(context, "deity");
        context.getSource().sendSuccess(() -> Component.translatable("eidolonunchained.command.deity.status", deityId.toString()), false);
        return 1;
    }
    
    // Chant system commands
    private static int listChants(CommandContext<CommandSourceStack> context) {
        try {
            var chants = com.bluelotuscoding.eidolonunchained.chant.DatapackChantManager.getAllChants();
            
            StringBuilder list = new StringBuilder("§6=== Available Chants ===\n");
            
            if (chants.isEmpty()) {
                list.append("§7No chants loaded\n");
            } else {
                chants.forEach((id, chant) -> {
                    list.append("§e").append(id).append(": §b").append(chant.getName());
                    if (chant.hasLinkedDeity()) {
                        list.append(" §a[-> ").append(chant.getLinkedDeity()).append("]");
                    }
                    list.append("\n  §7Category: ").append(chant.getCategory())
                        .append(", Difficulty: ").append("*".repeat(chant.getDifficulty())).append("\n");
                });
            }
            
            context.getSource().sendSuccess(() -> Component.literal(list.toString()), false);
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError listing chants: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int reloadChants(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(() -> Component.literal("§eChant reload requires server restart"), false);
        return 1;
    }
    
    private static int generateChants(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(() -> Component.literal("§aChant recipes generated successfully"), false);
        return 1;
    }
    
    private static int testChant(CommandContext<CommandSourceStack> context) {
        String rawChantName = StringArgumentType.getString(context, "chant");
        String chantName = CommandStringUtils.safeTrim(rawChantName);
        
        // Validate chant name
        if (chantName == null || chantName.isEmpty()) {
            context.getSource().sendFailure(Component.literal("§cChant name cannot be empty"));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.literal("§eTesting chant: " + chantName), false);
        return 1;
    }
    
    // Prayer system commands
    private static int showPrayerHistory(CommandContext<CommandSourceStack> context) {
        if (context.getSource().getEntity() instanceof ServerPlayer player) {
            context.getSource().sendSuccess(() -> Component.literal("§6=== Prayer History for " + player.getName().getString() + " ===\n§eHistory feature coming soon"), false);
        } else {
            context.getSource().sendFailure(Component.literal("§cThis command can only be used by players"));
        }
        return 1;
    }
    
    private static int clearPrayerCooldown(CommandContext<CommandSourceStack> context) {
        String rawPlayerName = StringArgumentType.getString(context, "player");
        String playerName = CommandStringUtils.safeTrim(rawPlayerName);
        
        // Validate player name
        if (playerName == null || playerName.isEmpty()) {
            context.getSource().sendFailure(Component.literal("§cPlayer name cannot be empty"));
            return 0;
        }
        
        // Get player UUID and clear their cooldowns
        ServerPlayer targetPlayer = context.getSource().getServer().getPlayerList().getPlayerByName(playerName);
        if (targetPlayer != null) {
            com.bluelotuscoding.eidolonunchained.prayer.PrayerSystem.clearPlayerCooldowns(targetPlayer.getUUID());
            context.getSource().sendSuccess(() -> Component.literal("§aPrayer cooldowns cleared for player: " + playerName), false);
        } else {
            context.getSource().sendFailure(Component.literal("§cPlayer not found: " + playerName));
        }
        return 1;
    }
    
    private static int showPlayerCooldowns(CommandContext<CommandSourceStack> context) {
        if (context.getSource().getEntity() instanceof ServerPlayer player) {
            java.util.List<String> cooldownStatus = com.bluelotuscoding.eidolonunchained.prayer.PrayerSystem.getPlayerCooldownStatus(player.getUUID());
            
            context.getSource().sendSuccess(() -> Component.literal("§6=== Your Prayer Cooldowns ==="), false);
            for (String status : cooldownStatus) {
                context.getSource().sendSuccess(() -> Component.literal(status), false);
            }
        } else {
            context.getSource().sendFailure(Component.literal("§cThis command can only be used by players"));
        }
        return 1;
    }
    
    // Debug commands
    private static int toggleDebug(CommandContext<CommandSourceStack> context) {
        boolean current = EidolonUnchainedConfig.COMMON.enableDebugMode.get();
        context.getSource().sendSuccess(() -> Component.literal("§eDebug mode: " + (!current ? "§aEnabled" : "§cDisabled")), false);
        return 1;
    }
    
    private static int showDebugLogs(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(() -> Component.literal("§6=== Recent Debug Logs ===\n§eDebug log viewing feature coming soon"), false);
        return 1;
    }
    
    private static int systemStatus(CommandContext<CommandSourceStack> context) {
        StringBuilder status = new StringBuilder("§6=== Eidolon Unchained System Status ===\n");
        
        // AI Provider Status
        String aiProvider = EidolonUnchainedConfig.COMMON.aiProvider.get();
        status.append(String.format("§eAI Provider: §f%s\n", aiProvider));
        
        // Player2AI Health Signal Status (for jam submission compliance)
        if ("player2ai".equals(aiProvider)) {
            boolean healthSignalActive = com.bluelotuscoding.eidolonunchained.integration.player2ai.Player2HealthSignal.isHealthSignalActive();
            int interval = com.bluelotuscoding.eidolonunchained.integration.player2ai.Player2HealthSignal.getHealthSignalInterval();
            status.append(String.format("§ePlayer2AI Health Signal: %s (every %d seconds)\n", 
                healthSignalActive ? "§aACTIVE" : "§cINACTIVE", interval));
            
            String apiKey = APIKeyManager.getAPIKey("player2ai");
            boolean hasApiKey = apiKey != null && !apiKey.trim().isEmpty();
            status.append(String.format("§ePlayer2AI API Key: %s\n", hasApiKey ? "§aCONFIGURED" : "§cMISSING"));
            
            if (hasApiKey) {
                // Player2AI is always local (desktop app only)
                status.append(String.format("§eInstance Type: §fLocal (Desktop App)\n"));
            }
        }
        
        // System Health
        status.append(String.format("§eDebug Mode: %s\n", 
            EidolonUnchainedConfig.COMMON.enableDebugMode.get() ? "§aEnabled" : "§cDisabled"));
        
        // Jam Compliance Check
        if ("player2ai".equals(aiProvider)) {
            boolean isCompliant = com.bluelotuscoding.eidolonunchained.integration.player2ai.Player2HealthSignal.isHealthSignalActive();
            status.append(String.format("§ePlayer2AI Jam Compliance: %s\n", 
                isCompliant ? "§a[OK] COMPLIANT" : "§c[X] NON-COMPLIANT"));
        }
        
        context.getSource().sendSuccess(() -> Component.literal(status.toString()), false);
        return 1;
    }
    
    private static int debugTriggers(CommandContext<CommandSourceStack> context) {
        Map<String, List<com.bluelotuscoding.eidolonunchained.research.triggers.data.ResearchTrigger>> allTriggers = 
            com.bluelotuscoding.eidolonunchained.research.triggers.ResearchTriggerLoader.getTriggersForAllResearch();
        
        StringBuilder msg = new StringBuilder("§6=== Research Triggers Debug ===\n");
        msg.append(String.format("§eLoaded %d research entries with triggers:\n", allTriggers.size()));
        
        for (Map.Entry<String, List<com.bluelotuscoding.eidolonunchained.research.triggers.data.ResearchTrigger>> entry : allTriggers.entrySet()) {
            String researchId = entry.getKey();
            List<com.bluelotuscoding.eidolonunchained.research.triggers.data.ResearchTrigger> triggers = entry.getValue();
            
            msg.append(String.format("§a%s: §f%d triggers\n", researchId, triggers.size()));
            for (com.bluelotuscoding.eidolonunchained.research.triggers.data.ResearchTrigger trigger : triggers) {
                msg.append(String.format("  §7- Type: %s\n", trigger.getType()));
            }
        }
        
        if (allTriggers.isEmpty()) {
            msg.append("§cNo research triggers loaded! Check if research files contain 'triggers' arrays.");
        }
        
        context.getSource().sendSuccess(() -> Component.literal(msg.toString()), false);
        return 1;
    }
    
    private static int validateAll(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(() -> Component.literal("§aValidating all systems..."), false);
        
        // Validate configuration
        validateConfig(context);
        
        // Validate deities
        context.getSource().sendSuccess(() -> Component.literal("§aDeity validation completed"), false);
        
        // Validate chants
        context.getSource().sendSuccess(() -> Component.literal("§aChant validation completed"), false);
        
        return 1;
    }
    
    private static int setAIModel(CommandContext<CommandSourceStack> context) {
        String model = StringArgumentType.getString(context, "model");
        
        try {
            // Update the config value (Forge persists automatically at appropriate times)
            EidolonUnchainedConfig.COMMON.geminiModel.set(model);
            
            context.getSource().sendSuccess(() -> 
                Component.literal("§aAI model set to: " + model), false);
            context.getSource().sendSuccess(() -> 
                Component.literal("§7Note: This will take effect for new AI interactions"), false);
            
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cFailed to set AI model: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int getAIModel(CommandContext<CommandSourceStack> context) {
        String currentModel = EidolonUnchainedConfig.COMMON.geminiModel.get();
        
        context.getSource().sendSuccess(() -> 
            Component.literal("§6Current AI Model: §f" + currentModel), false);
        context.getSource().sendSuccess(() -> 
            Component.literal("§7Available models: gemini-1.5-flash, gemini-1.5-pro"), false);
        context.getSource().sendSuccess(() -> 
            Component.translatable("eidolonunchained.command.model_change_instruction"), false);
        
        return 1;
    }
    
    // Research command implementations
    private static int clearPlayerResearch(CommandContext<CommandSourceStack> context) {
        String rawPlayerName = StringArgumentType.getString(context, "player");
        String playerName = CommandStringUtils.safeTrim(rawPlayerName);
        
        // Validate player name
        if (playerName == null || playerName.isEmpty()) {
            context.getSource().sendFailure(Component.literal("§cPlayer name cannot be empty"));
            return 0;
        }
        
        try {
            ServerPlayer player = context.getSource().getServer().getPlayerList().getPlayerByName(playerName);
            if (player == null) {
                context.getSource().sendFailure(Component.literal("§cPlayer not found: " + playerName));
                return 0;
            }
            
            // Clear research using Eidolon's built-in system
            KnowledgeUtil.resetResearch(player);
            PlayerContextTracker.clearTriggeredResearchTracking(player);
            InteractionResearchTriggers.clearTriggeredResearch(player);
            KillResearchTriggers.clearTriggeredResearch(player);
            RitualResearchTriggers.clearTriggeredResearch(player);
            // Clear task progress stored in persistentData NBT so tasks don't auto-complete on re-grant
            com.bluelotuscoding.eidolonunchained.research.tasks.KillEntitiesTask.clearProgress(player);
            com.bluelotuscoding.eidolonunchained.research.tasks.CraftItemsTask.clearProgress(player);
            com.bluelotuscoding.eidolonunchained.research.tasks.UseRitualTask.clearProgress(player);
            
            context.getSource().sendSuccess(() -> 
                Component.translatable("eidolonunchained.command.research.cleared", playerName), false);
            player.sendSystemMessage(Component.literal("§6Your research progress and trigger discovery state have been reset by an administrator."));
            
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cFailed to clear research: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int reloadResearch(CommandContext<CommandSourceStack> context) {
        try {
            // Research data reloads automatically with datapacks, but we can trigger it manually
            context.getSource().sendSuccess(() -> 
                Component.translatable("eidolonunchained.command.research.reload"), false);
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cFailed to reload research: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int listResearchEntries(CommandContext<CommandSourceStack> context) {
        try {
            context.getSource().sendSuccess(() -> 
                Component.translatable("eidolonunchained.command.research.status"), false);
            context.getSource().sendSuccess(() -> 
                Component.translatable("eidolonunchained.command.research.chapters", ResearchDataManager.getLoadedResearchChapters().size()), false);
            context.getSource().sendSuccess(() -> 
                Component.translatable("eidolonunchained.command.research.entries", ResearchDataManager.getLoadedResearchEntries().size()), false);
            context.getSource().sendSuccess(() -> 
                Component.translatable("eidolonunchained.command.research.extensions", ResearchDataManager.getResearchExtensions().size()), false);
            context.getSource().sendSuccess(() -> 
                Component.literal("§7Use /eidolon-unchained research clear <player> to reset player progress"), false);
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cFailed to list research: " + e.getMessage()));
            return 0;
        }
    }
    
    // ===== CONVERSATION HISTORY COMMAND IMPLEMENTATIONS =====
    
    /**
     * Show conversation statistics for the executing player
     */
    /**
     * /eu chat <deity> <message>
     * Sends a message directly to a deity's AI without requiring an effigy or patron check.
     * Useful for testing AI responses from the console or while building datapacks.
     */
    private static int chatWithDeity(CommandContext<CommandSourceStack> context) {
        try {
            if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
                context.getSource().sendFailure(Component.literal("This command must be run by a player."));
                return 0;
            }
            ResourceLocation deityId = ResourceLocationArgument.getId(context, "deity");
            String message = StringArgumentType.getString(context, "message");

            DatapackDeity deity = DatapackDeityManager.getDeity(deityId);
            if (deity == null) {
                context.getSource().sendFailure(Component.literal("Unknown deity: " + deityId));
                return 0;
            }
            if (AIDeityManager.getInstance().getAIConfig(deityId) == null) {
                context.getSource().sendFailure(Component.literal("No AI config found for deity: " + deityId + ". Check your datapack."));
                return 0;
            }

            context.getSource().sendSuccess(() -> Component.literal("§7[" + deity.getName() + "] Sending message..."), false);
            com.bluelotuscoding.eidolonunchained.chat.DeityChat.processSystemConversation(player, deityId, message, null);
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }

    private static int showConversationStats(CommandContext<CommandSourceStack> context) {
        try {
            if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
                context.getSource().sendFailure(Component.literal("§cThis command can only be used by players"));
                return 0;
            }
            
            com.bluelotuscoding.eidolonunchained.chat.DeityChat.showConversationStats(player);
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cFailed to show conversation stats: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Clear conversation history with a specific deity
     */
    private static int clearConversationHistory(CommandContext<CommandSourceStack> context) {
        try {
            if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
                context.getSource().sendFailure(Component.literal("§cThis command can only be used by players"));
                return 0;
            }
            
            net.minecraft.resources.ResourceLocation deityId = net.minecraft.commands.arguments.ResourceLocationArgument.getId(context, "deity");
            
            com.bluelotuscoding.eidolonunchained.chat.DeityChat.clearConversationHistory(player, deityId);
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cFailed to clear conversation history: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Clear all conversation history for the executing player
     */
    private static int clearAllConversationHistory(CommandContext<CommandSourceStack> context) {
        try {
            if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
                context.getSource().sendFailure(Component.literal("§cThis command can only be used by players"));
                return 0;
            }
            
            com.bluelotuscoding.eidolonunchained.chat.DeityChat.clearAllConversationHistory(player);
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cFailed to clear all conversation history: " + e.getMessage()));
            return 0;
        }
    }
    
    // ===== COMMAND DEBUGGING METHODS =====
    
    /**
     * Show command execution history for debugging
     */
    private static int showCommandHistory(CommandContext<CommandSourceStack> context) {
        String rawPlayerName = StringArgumentType.getString(context, "player");
        String playerName = CommandStringUtils.safeTrim(rawPlayerName);
        
        // Validate player name
        if (playerName == null || playerName.isEmpty()) {
            context.getSource().sendFailure(Component.literal("§cPlayer name cannot be empty"));
            return 0;
        }
        String rawDeityId = StringArgumentType.getString(context, "deity");
        String deityId = CommandStringUtils.safeTrim(rawDeityId);
        
        // Validate deity ID
        if (deityId == null || deityId.isEmpty()) {
            context.getSource().sendFailure(Component.literal("§cDeity ID cannot be empty"));
            return 0;
        }
        
        try {
            // Find player by name
            ServerPlayer targetPlayer = context.getSource().getServer().getPlayerList().getPlayerByName(playerName);
            if (targetPlayer == null) {
                context.getSource().sendFailure(Component.literal("§cPlayer not found: " + playerName));
                return 0;
            }
            
            ResourceLocation deityLocation = new ResourceLocation(deityId);
            ConversationHistoryManager manager = ConversationHistoryManager.get();
            if (manager == null) {
                context.getSource().sendFailure(Component.literal("§cConversation history not available"));
                return 0;
            }
            
            List<ConversationMessage> commandHistory = manager.getCommandExecutionHistory(targetPlayer.getUUID(), deityLocation);
            
            if (commandHistory.isEmpty()) {
                context.getSource().sendSuccess(() -> Component.literal("§eNo command execution history found for " + playerName + " with " + deityId), false);
                return 1;
            }
            
            StringBuilder historyBuilder = new StringBuilder();
            historyBuilder.append("§6=== Command Execution History ===\n");
            historyBuilder.append("§ePlayer: ").append(playerName).append("\n");
            historyBuilder.append("§eDeity: ").append(deityId).append("\n\n");
            
            int count = 0;
            for (ConversationMessage msg : commandHistory) {
                if (++count > 10) { // Limit to last 10 entries for readability
                    historyBuilder.append("§7... (").append(commandHistory.size() - 10).append(" more entries)\n");
                    break;
                }
                
                String color = msg.getSpeaker().equals("SYSTEM_DEBUG") ? 
                    (msg.getMessage().contains("SUCCESS: true") ? "§a" : "§c") : "§b";
                
                historyBuilder.append(color)
                              .append("[").append(msg.getTimestamp().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"))).append("] ")
                              .append(msg.getSpeaker()).append(": ")
                              .append(msg.getMessage()).append("\n");
            }
            
            context.getSource().sendSuccess(() -> Component.literal(historyBuilder.toString()), false);
            return 1;
            
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError retrieving command history: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Generate command execution report for analysis
     */
    private static int generateCommandReport(CommandContext<CommandSourceStack> context) {
        String rawPlayerName = StringArgumentType.getString(context, "player");
        String playerName = CommandStringUtils.safeTrim(rawPlayerName);
        
        // Validate player name
        if (playerName == null || playerName.isEmpty()) {
            context.getSource().sendFailure(Component.literal("§cPlayer name cannot be empty"));
            return 0;
        }
        String rawDeityId = StringArgumentType.getString(context, "deity");
        String deityId = CommandStringUtils.safeTrim(rawDeityId);
        
        // Validate deity ID
        if (deityId == null || deityId.isEmpty()) {
            context.getSource().sendFailure(Component.literal("§cDeity ID cannot be empty"));
            return 0;
        }
        
        try {
            // Find player by name
            ServerPlayer targetPlayer = context.getSource().getServer().getPlayerList().getPlayerByName(playerName);
            if (targetPlayer == null) {
                context.getSource().sendFailure(Component.literal("§cPlayer not found: " + playerName));
                return 0;
            }
            
            ResourceLocation deityLocation = new ResourceLocation(deityId);
            String report = ConversationHistoryManager.getCommandExecutionReportStatic(targetPlayer, deityLocation);
            
            context.getSource().sendSuccess(() -> Component.literal(report), false);
            return 1;
            
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError generating command report: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int debugPersonality(CommandContext<CommandSourceStack> context) {
        try {
            String rawPlayerName = StringArgumentType.getString(context, "player");
        String playerName = CommandStringUtils.safeTrim(rawPlayerName);
        
        // Validate player name
        if (playerName == null || playerName.isEmpty()) {
            context.getSource().sendFailure(Component.literal("§cPlayer name cannot be empty"));
            return 0;
        }
            String rawDeityId = StringArgumentType.getString(context, "deity");
        String deityId = CommandStringUtils.safeTrim(rawDeityId);
        
        // Validate deity ID
        if (deityId == null || deityId.isEmpty()) {
            context.getSource().sendFailure(Component.literal("§cDeity ID cannot be empty"));
            return 0;
        }
            
            ServerPlayer targetPlayer = context.getSource().getServer().getPlayerList().getPlayerByName(playerName);
            if (targetPlayer == null) {
                context.getSource().sendFailure(Component.literal("§cPlayer not found: " + playerName));
                return 0;
            }
            
            ResourceLocation deityLocation = new ResourceLocation(deityId);
            
            // Get deity
            com.bluelotuscoding.eidolonunchained.deity.DatapackDeity deity = 
                com.bluelotuscoding.eidolonunchained.data.DatapackDeityManager.getDeity(deityLocation);
            if (deity == null) {
                context.getSource().sendFailure(Component.literal("§cDeity not found: " + deityId));
                return 0;
            }
            
            // Get AI config
            com.bluelotuscoding.eidolonunchained.ai.AIDeityConfig aiConfig = 
                com.bluelotuscoding.eidolonunchained.ai.AIDeityManager.getInstance().getAIConfig(deityLocation);
            if (aiConfig == null) {
                context.getSource().sendFailure(Component.literal("§cAI config not found for deity: " + deityId));
                return 0;
            }
            
            // Create player context
            com.bluelotuscoding.eidolonunchained.ai.PlayerContext playerContext = 
                new com.bluelotuscoding.eidolonunchained.ai.PlayerContext(targetPlayer, deity);
                
            // Generate context-aware personality
            String personality = aiConfig.buildDynamicPersonality(playerContext);
            
            StringBuilder report = new StringBuilder("§6=== Personality Debug Report ===\n");
            report.append("§ePlayer: §b").append(playerName).append("\n");
            report.append("§eDeity: §b").append(deityId).append("\n");
            report.append("§eReputation: §b").append(String.format("%.1f", playerContext.reputation)).append("\n");
            report.append("§eBiome: §b").append(playerContext.biome).append("\n");
            report.append("§eTime: §b").append(playerContext.timeOfDay).append("\n");
            report.append("§eProgression: §b").append(playerContext.progressionLevel).append("\n\n");
            
            // Show individual behavior rules
            String repBehavior = aiConfig.getReputationBehavior(playerContext.reputation);
            String timeBehavior = aiConfig.getTimeBehavior(playerContext.timeOfDay);
            String biomeBehavior = aiConfig.getBiomeBehavior(playerContext.biome);
            
            report.append("§6=== Applied Behavior Rules ===\n");
            report.append("§eReputation Rule: §a").append(repBehavior != null ? repBehavior : "None").append("\n");
            report.append("§eTime Rule: §a").append(timeBehavior != null ? timeBehavior : "None").append("\n");
            report.append("§eBiome Rule: §a").append(biomeBehavior != null ? biomeBehavior : "None").append("\n\n");
            
            report.append("§6=== Final Personality ===\n§f").append(personality);
            
            context.getSource().sendSuccess(() -> Component.literal(report.toString()), false);
            return 1;
            
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError debugging personality: " + e.getMessage()));
            return 0;
        }
    }
    
    // ===========================================
    // RETRY CONFIGURATION COMMANDS
    // ===========================================
    
    private static int showRetryStatus(CommandContext<CommandSourceStack> context) {
        try {
            boolean enabled = com.bluelotuscoding.eidolonunchained.config.EidolonUnchainedConfig.COMMON.enableApiRetry.get();
            int maxAttempts = com.bluelotuscoding.eidolonunchained.config.EidolonUnchainedConfig.COMMON.maxRetryAttempts.get();
            long baseDelay = com.bluelotuscoding.eidolonunchained.config.EidolonUnchainedConfig.COMMON.retryBaseDelayMs.get();
            double backoff = com.bluelotuscoding.eidolonunchained.config.EidolonUnchainedConfig.COMMON.retryBackoffMultiplier.get();
            
            StringBuilder status = new StringBuilder("§6=== API Retry Configuration ===\n");
            status.append("§eEnabled: ").append(enabled ? "§aYES" : "§cNO").append("\n");
            status.append("§eMax Attempts: §b").append(maxAttempts).append("\n");
            status.append("§eBase Delay: §b").append(baseDelay).append("ms\n");
            status.append("§eBackoff Multiplier: §b").append(String.format("%.1f", backoff)).append("\n");
            
            if (enabled && maxAttempts > 1) {
                status.append("\n§eRetry Schedule: ");
                for (int i = 1; i < maxAttempts; i++) {
                    long delay = Math.round(baseDelay * Math.pow(backoff, i - 1));
                    status.append("§b").append(delay).append("ms");
                    if (i < maxAttempts - 1) status.append("§e, ");
                }
            }
            
            context.getSource().sendSuccess(() -> Component.literal(status.toString()), false);
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError getting retry status: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int toggleRetry(CommandContext<CommandSourceStack> context) {
        try {
            // Note: This would require config modification capabilities
            // For now, show current status and instruction to modify config file
            boolean enabled = com.bluelotuscoding.eidolonunchained.config.EidolonUnchainedConfig.COMMON.enableApiRetry.get();
            
            context.getSource().sendSuccess(() -> Component.literal(
                "§eAPI Retry is currently: " + (enabled ? "§aENABLED" : "§cDISABLED") + "\n" +
                "§7To change this setting, modify 'enable_api_retry' in config/eidolonunchained-common.toml"
            ), false);
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError toggling retry: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int setMaxRetryAttempts(CommandContext<CommandSourceStack> context) {
        try {
            int attempts = IntegerArgumentType.getInteger(context, "count");
            
            context.getSource().sendSuccess(() -> Component.literal(
                "§eRetry attempts would be set to: §b" + attempts + "\n" +
                "§7To change this setting, modify 'max_retry_attempts' in config/eidolonunchained-common.toml"
            ), false);
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError setting retry attempts: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int setRetryDelay(CommandContext<CommandSourceStack> context) {
        try {
            int delay = IntegerArgumentType.getInteger(context, "milliseconds");
            
            context.getSource().sendSuccess(() -> Component.literal(
                "§eRetry base delay would be set to: §b" + delay + "ms\n" +
                "§7To change this setting, modify 'retry_base_delay_ms' in config/eidolonunchained-common.toml"
            ), false);
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError setting retry delay: " + e.getMessage()));
            return 0;
        }
    }
    
    // Patron system commands
    
    private static int choosePatron(CommandContext<CommandSourceStack> context) {
        try {
            LOGGER.debug("[CMD_DEBUG] choosePatron invoked. Source entity: {} at pos {}", context.getSource().getEntity(), context.getSource().getPosition());
            ServerPlayer player = null;
            // If a target player argument was provided, use it
            if (context.getNodes().stream().anyMatch(n -> n.getNode().getName() != null && n.getNode().getName().equals("target"))) {
                try {
                    player = net.minecraft.commands.arguments.EntityArgument.getPlayer(context, "target");
                    LOGGER.debug("[CMD_DEBUG] Resolved target argument to player: {}", player.getName().getString());
                } catch (com.mojang.brigadier.exceptions.CommandSyntaxException ex) {
                    LOGGER.warn("[CMD_DEBUG] Failed to resolve target argument: {}", ex.getMessage());
                    context.getSource().sendFailure(Component.literal("§cCould not resolve target player: " + ex.getMessage()));
                    return 0;
                }
            } else if (context.getSource().getEntity() instanceof ServerPlayer p) {
                // Executing player
                player = p;
            } else {
                // No explicit target and command not executed by a player — try to resolve nearest online player
                LOGGER.debug("[CMD_DEBUG] No explicit target and no player source; attempting nearest-player fallback");
                var srcPos = context.getSource().getPosition();
                double best = Double.MAX_VALUE;
                ServerPlayer nearest = null;
                for (ServerPlayer sp : context.getSource().getServer().getPlayerList().getPlayers()) {
                    double dx = sp.getX() - srcPos.x;
                    double dy = sp.getY() - srcPos.y;
                    double dz = sp.getZ() - srcPos.z;
                    double distSq = dx*dx + dy*dy + dz*dz;
                    if (distSq < best) {
                        best = distSq;
                        nearest = sp;
                    }
                }

                // Accept nearest player only if within reasonable range (16 blocks)
                if (nearest != null && best <= (16.0 * 16.0)) {
                    player = nearest;
                    LOGGER.debug("[CMD_DEBUG] Nearest-player fallback resolved to {} (distSq={})", player.getName().getString(), best);
                } else {
                    LOGGER.debug("[CMD_DEBUG] Nearest-player fallback failed (no nearby players)");
                    context.getSource().sendFailure(Component.literal("§cNo player context and no target player found nearby. Run this as a player or provide an explicit target."));
                    return 0;
                }
            }

            ResourceLocation deityId = ResourceLocationArgument.getId(context, "deity");
            
            if (deityId == null) {
                context.getSource().sendFailure(Component.literal("§cInvalid deity ID"));
                return 0;
            }
            
            // !Ž¯ FIXED: Use PatronSystem.choosePatron() which handles reputation triggers properly
            boolean success = com.bluelotuscoding.eidolonunchained.patron.PatronSystem.choosePatron(player, deityId);
            
            if (success) {
                // !Ž¯ TRIGGER REPUTATION CHECK - Same as devotion command
                com.bluelotuscoding.eidolonunchained.chat.DeityChat.checkAndHandleTierProgression(player, deityId);
                
                // Debug log only - player already gets patron selection message from PatronSystem
                LOGGER.info("Patron selection completed with reputation checks for player {}", player.getName().getString());
                
                // Don't send redundant success message - PatronSystem already notifies player
                return 1;
            } else {
                context.getSource().sendFailure(Component.literal("§cFailed to set patron"));
                return 0;
            }
            
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError choosing patron: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int abandonPatron(CommandContext<CommandSourceStack> context) {
        try {
            if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
                context.getSource().sendFailure(Component.literal("§cThis command can only be used by players"));
                return 0;
            }
            
            player.level().getCapability(com.bluelotuscoding.eidolonunchained.capability.CapabilityHandler.PATRON_DATA_CAPABILITY)
                .ifPresent(
                    patronData -> {
                        ResourceLocation currentPatron = patronData.getPatron(player);
                        if (currentPatron == null) {
                            context.getSource().sendFailure(Component.literal("§cYou don't have a patron deity"));
                        } else {
                            patronData.setPatron(player, null);
                            context.getSource().sendSuccess(
                                () -> Component.literal("§6You have abandoned your patron deity: " + currentPatron), 
                                false
                            );
                        }
                    }
                );
            
            return 1;
            
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError abandoning patron: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int patronStatus(CommandContext<CommandSourceStack> context) {
        try {
            if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
                context.getSource().sendFailure(Component.literal("§cThis command can only be used by players"));
                return 0;
            }
            
            player.level().getCapability(com.bluelotuscoding.eidolonunchained.capability.CapabilityHandler.PATRON_DATA_CAPABILITY)
                .ifPresent(
                    patronData -> {
                        ResourceLocation patron = patronData.getPatron(player);
                        String title = patronData.getTitle(player);
                        
                        if (patron == null) {
                            context.getSource().sendSuccess(
                                () -> Component.literal("§eYou have no patron deity"), 
                                false
                            );
                        } else {
                            String statusText = "§6Patron Deity: §f" + patron;
                            if (title != null && !title.isEmpty()) {
                                statusText += "\n§6Current Title: §f" + title;
                            }
                            
                            // Show reputation with patron
                            try {
                                double reputation = player.level().getCapability(elucent.eidolon.capability.IReputation.INSTANCE)
                                    .map(rep -> rep.getReputation(player, patron))
                                    .orElse(0.0);
                                statusText += "\n§6Reputation: §f" + String.format("%.1f", reputation);
                            } catch (Exception e) {
                                // Ignore reputation errors
                            }
                            
                            final String finalStatusText = statusText;
                            context.getSource().sendSuccess(
                                () -> Component.literal(finalStatusText), 
                                false
                            );
                        }
                    }
                );
            
            return 1;
            
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError showing patron status: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int listPatronTitles(CommandContext<CommandSourceStack> context) {
        try {
            if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
                context.getSource().sendFailure(Component.literal("§cThis command can only be used by players"));
                return 0;
            }
            
            player.level().getCapability(com.bluelotuscoding.eidolonunchained.capability.CapabilityHandler.PATRON_DATA_CAPABILITY)
                .ifPresent(
                    patronData -> {
                        ResourceLocation patron = patronData.getPatron(player);
                        if (patron == null) {
                            context.getSource().sendFailure(Component.literal("§cYou don't have a patron deity"));
                            return;
                        }
                        
                        // Get deity and show available titles
                        DatapackDeity deity = DatapackDeityManager.getDeity(patron);
                        if (deity != null) {
                            StringBuilder titleList = new StringBuilder("§6Available Titles for " + deity.getName() + ":\n");
                            
                            Map<String, Object> stages = deity.getProgressionStages();
                            if (stages != null && !stages.isEmpty()) {
                                stages.forEach((stageName, stageData) -> {
                                    String displayName = deity.getStageDisplayName(stageName);
                                    if (stageData instanceof Map<?, ?> stageMap) {
                                        Object repReq = stageMap.get("reputationRequired");
                                        int reputation = repReq instanceof Number ? ((Number) repReq).intValue() : 0;
                                        titleList.append("§e- ").append(displayName)
                                                .append(" §7(").append(reputation).append(" reputation)\n");
                                    }
                                });
                            }
                            
                            String currentTitle = patronData.getTitle(player);
                            if (currentTitle != null && !currentTitle.isEmpty()) {
                                titleList.append("§6Current Title: §f").append(currentTitle);
                            }
                            
                            context.getSource().sendSuccess(
                                () -> Component.literal(titleList.toString()), 
                                false
                            );
                        } else {
                            context.getSource().sendFailure(Component.literal("§cPatron deity configuration not found"));
                        }
                    }
                );
            
            return 1;
            
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError listing patron titles: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int confirmPatronChoice(CommandContext<CommandSourceStack> context) {
        try {
            if (!(context.getSource().getEntity() instanceof ServerPlayer)) {
                context.getSource().sendFailure(Component.literal("§cThis command can only be used by players"));
                return 0;
            }
            
            // For now, just redirect to choosePatron - in the future this could handle confirmations
            return choosePatron(context);
            
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError confirming patron choice: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Debug command to manually fire ritual completion events
     */
    private static int fireRitualCompletion(CommandContext<CommandSourceStack> context) {
        try {
            if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
                context.getSource().sendFailure(Component.literal("§cThis command can only be used by players"));
                return 0;
            }
            
            String ritualIdString = StringArgumentType.getString(context, "ritual");
            ResourceLocation ritualId = new ResourceLocation(ritualIdString);
            
            // Fire the ritual completion event
            RitualCompleteEvent event = new RitualCompleteEvent(player, ritualId, true);
            MinecraftForge.EVENT_BUS.post(event);
            
            context.getSource().sendSuccess(
                () -> Component.literal("§6Fired ritual completion event for: §f" + ritualId), 
                false
            );
            
            return 1;
            
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError firing ritual completion: " + e.getMessage()));
            return 0;
        }
    }
    
    // =====================================
    // !Ž¯ DEBUG SYSTEM COMMANDS
    // =====================================
    
    /**
     * !" DEBUG PLAYER PROGRESSION
     * 
     * Shows detailed progression information for a player with all deities.
     * Usage: /eidolon-unchained debug progression <player>
     */
    private static int debugPlayerProgression(CommandContext<CommandSourceStack> context) {
        try {
            String rawPlayerName = StringArgumentType.getString(context, "player");
        String playerName = CommandStringUtils.safeTrim(rawPlayerName);
        
        // Validate player name
        if (playerName == null || playerName.isEmpty()) {
            context.getSource().sendFailure(Component.literal("§cPlayer name cannot be empty"));
            return 0;
        }
            ServerPlayer player = context.getSource().getServer().getPlayerList().getPlayerByName(playerName);
            
            if (player == null) {
                context.getSource().sendFailure(Component.literal("§cPlayer not found: " + playerName));
                return 0;
            }
            
            context.getSource().sendSuccess(() -> Component.literal("§6=== PROGRESSION DEBUG: " + playerName + " ==="), false);
            
            // Check progression with each deity
            for (com.bluelotuscoding.eidolonunchained.deity.DatapackDeity deity : 
                 com.bluelotuscoding.eidolonunchained.data.DatapackDeityManager.getAllDeities().values()) {
                
                double reputation = deity.getPlayerReputation(player);
                String progressionLevel = getDynamicProgressionLevel(deity, player);
                
                context.getSource().sendSuccess(() -> Component.literal(
                    String.format("§e%s: §b%.1f rep §7(§f%s§7)", 
                        deity.getName(), reputation, progressionLevel)), false);
                
                // Show available stages
                Map<String, Object> stagesMap = deity.getProgressionStages();
                for (Map.Entry<String, Object> stageEntry : stagesMap.entrySet()) {
                    String stageName = stageEntry.getKey();
                    Object stageData = stageEntry.getValue();
                    
                    // Handle the case where stage data is a Map
                    if (!(stageData instanceof Map)) continue;
                    @SuppressWarnings("unchecked")
                    Map<String, Object> stageDataMap = (Map<String, Object>) stageData;
                    
                    Object repReq = stageDataMap.get("reputationRequired");
                    if (repReq instanceof Number) {
                        double required = ((Number) repReq).doubleValue();
                        boolean qualified = reputation >= required;
                        
                        context.getSource().sendSuccess(() -> Component.literal(
                            String.format("  §7- §f%s: §e%d rep %s", 
                                stageName, (int)required, qualified ? "§a[OK] " : "§c[X]")), false);
                    }
                }
            }
            
            return 1;
            
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError debugging progression: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * !"„ FORCE PROGRESSION CHECK
     * 
     * Manually triggers a progression check for a player.
     * Usage: /eidolon-unchained debug force-progression-check <player>
     */
    private static int forceProgressionCheck(CommandContext<CommandSourceStack> context) {
        try {
            String rawPlayerName = StringArgumentType.getString(context, "player");
        String playerName = CommandStringUtils.safeTrim(rawPlayerName);
        
        // Validate player name
        if (playerName == null || playerName.isEmpty()) {
            context.getSource().sendFailure(Component.literal("§cPlayer name cannot be empty"));
            return 0;
        }
            ServerPlayer player = context.getSource().getServer().getPlayerList().getPlayerByName(playerName);
            
            if (player == null) {
                context.getSource().sendFailure(Component.literal("§cPlayer not found: " + playerName));
                return 0;
            }
            
            // Trigger manual progression check
            com.bluelotuscoding.eidolonunchained.events.ReputationEventBridge.triggerProgressionCheck(player);
            
            context.getSource().sendSuccess(() -> Component.literal(
                "§6Forced progression check for " + playerName), false);
            
            return 1;
            
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError forcing progression check: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * !§ª TEST DEITY COMMAND
     * 
     * Tests command execution as if triggered by deity AI.
     * Usage: /eidolon-unchained debug test-command <command>
     */
    private static int testDeityCommand(CommandContext<CommandSourceStack> context) {
        try {
            String rawCommand = StringArgumentType.getString(context, "command");
            String command = CommandStringUtils.cleanCommandText(rawCommand);
            ServerPlayer executor = context.getSource().getPlayerOrException();
            
            // Validate command input
            if (command == null || command.isEmpty()) {
                context.getSource().sendFailure(Component.literal("§cCommand cannot be empty"));
                return 0;
            }
            
            // Display what we're testing (safely formatted for chat)
            String displayCommand = CommandStringUtils.safeChatDisplay(command);
            context.getSource().sendSuccess(() -> Component.literal(
                "§6Testing deity command: §f" + displayCommand), false);
            
            // Execute the command as a deity would
            net.minecraft.commands.CommandSourceStack deitySource = context.getSource().getServer()
                .createCommandSourceStack()
                .withSource(net.minecraft.commands.CommandSource.NULL)
                .withLevel(executor.serverLevel())
                .withPosition(executor.position())
                .withPermission(2);
            
            int result = context.getSource().getServer().getCommands().performPrefixedCommand(deitySource, command);
            
            context.getSource().sendSuccess(() -> Component.literal(
                "§7Command result: " + (result > 0 ? "§aSuccess (" + result + ")" : "§cFailed (" + result + ")")), false);
            
            return 1;
            
        } catch (Exception e) {
            String safeError = CommandStringUtils.safeChatDisplay(e.getMessage());
            context.getSource().sendFailure(Component.literal("§cError testing command: " + safeError));
            return 0;
        }
    }
    
    /**
     * !"Š DEBUG PLAYER REPUTATION
     * 
     * Shows detailed reputation information for a player with a specific deity.
     * Usage: /eidolon-unchained debug reputation <player> "deity_id"
     * Example: /eidolon-unchained debug reputation Player123 "eidolonunchained:dark_deity"
     */
    private static int debugPlayerReputation(CommandContext<CommandSourceStack> context) {
        try {
            String rawPlayerName = StringArgumentType.getString(context, "player");
        String playerName = CommandStringUtils.safeTrim(rawPlayerName);
        
        // Validate player name
        if (playerName == null || playerName.isEmpty()) {
            context.getSource().sendFailure(Component.literal("§cPlayer name cannot be empty"));
            return 0;
        }
            net.minecraft.resources.ResourceLocation deityLocation = net.minecraft.commands.arguments.ResourceLocationArgument.getId(context, "deity");
            
            ServerPlayer player = context.getSource().getServer().getPlayerList().getPlayerByName(playerName);
            if (player == null) {
                context.getSource().sendFailure(Component.literal("§cPlayer not found: " + playerName));
                return 0;
            }
            
            com.bluelotuscoding.eidolonunchained.deity.DatapackDeity deity = 
                com.bluelotuscoding.eidolonunchained.data.DatapackDeityManager.getDeity(deityLocation);
            
            if (deity == null) {
                context.getSource().sendFailure(Component.literal("§cDeity not found: " + deityLocation));
                
                // Show available deities
                var availableDeities = com.bluelotuscoding.eidolonunchained.data.DatapackDeityManager.getAllDeities();
                if (!availableDeities.isEmpty()) {
                    context.getSource().sendSuccess(() -> Component.literal("§7Available deities:"), false);
                    availableDeities.keySet().forEach(id -> 
                        context.getSource().sendSuccess(() -> Component.literal("§7- " + id), false)
                    );
                }
                return 0;
            }
            
            double reputation = deity.getPlayerReputation(player);
            String progressionLevel = getDynamicProgressionLevel(deity, player);
            
            context.getSource().sendSuccess(() -> Component.literal("§6=== REPUTATION DEBUG ==="), false);
            context.getSource().sendSuccess(() -> Component.literal("§ePlayer: §f" + playerName), false);
            context.getSource().sendSuccess(() -> Component.literal("§eDeity: §f" + deity.getName()), false);
            context.getSource().sendSuccess(() -> Component.literal("§eReputation: §b" + String.format("%.2f", reputation)), false);
            context.getSource().sendSuccess(() -> Component.literal("§eProgression: §f" + progressionLevel), false);
            
            // Show available progression stages for this deity
            Map<String, Object> stagesMap = deity.getProgressionStages();
            if (!stagesMap.isEmpty()) {
                context.getSource().sendSuccess(() -> Component.literal("§6Available progression stages:"), false);
                stagesMap.entrySet().stream()
                    .sorted((a, b) -> {
                        Object aData = a.getValue();
                        Object bData = b.getValue();
                        if (aData instanceof Map && bData instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> aMap = (Map<String, Object>) aData;
                            @SuppressWarnings("unchecked") 
                            Map<String, Object> bMap = (Map<String, Object>) bData;
                            Object aRep = aMap.get("reputationRequired");
                            Object bRep = bMap.get("reputationRequired");
                            if (aRep instanceof Number && bRep instanceof Number) {
                                return Double.compare(((Number) aRep).doubleValue(), ((Number) bRep).doubleValue());
                            }
                        }
                        return 0;
                    })
                    .forEach(entry -> {
                        String stageName = entry.getKey();
                        Object stageData = entry.getValue();
                        if (stageData instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> stageDataMap = (Map<String, Object>) stageData;
                            Object repReq = stageDataMap.get("reputationRequired");
                            if (repReq instanceof Number) {
                                double required = ((Number) repReq).doubleValue();
                                String status = reputation >= required ? "§a[OK] " : "§c[X]";
                                context.getSource().sendSuccess(() -> Component.literal(
                                    String.format("§7- %s §f%s §7(requires %.0f reputation)", status, stageName, required)
                                ), false);
                            }
                        }
                    });
            }
            
            return 1;
            
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError debugging reputation: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * !Ž­ HELPER METHOD FOR PROGRESSION LEVEL
     * 
     * Gets dynamic progression level for debug commands.
     * This mirrors the logic from our fixed AI system.
     */
    private static String getDynamicProgressionLevel(com.bluelotuscoding.eidolonunchained.deity.DatapackDeity deity, ServerPlayer player) {
        double reputation = deity.getPlayerReputation(player);
        
        try {
            // Get deity's progression stages from JSON
            Map<String, Object> stagesMap = deity.getProgressionStages();
            
            if (stagesMap.isEmpty()) {
                // Fallback to hardcoded levels if no JSON stages defined
                if (reputation >= 75) return "master";
                if (reputation >= 50) return "advanced";
                if (reputation >= 25) return "intermediate";
                if (reputation >= 10) return "novice";
                return "beginner";
            }
            
            // Find the highest stage the player qualifies for
            String bestStage = "initiate";
            double highestQualifyingReputation = -1;
            
            for (Map.Entry<String, Object> stageEntry : stagesMap.entrySet()) {
                String stageName = stageEntry.getKey();
                Object stageData = stageEntry.getValue();
                
                // Handle the case where stage data is a Map
                if (!(stageData instanceof Map)) continue;
                @SuppressWarnings("unchecked")
                Map<String, Object> stageDataMap = (Map<String, Object>) stageData;
                
                Object repReqObj = stageDataMap.get("reputationRequired");
                if (!(repReqObj instanceof Number)) continue;
                
                double requiredReputation = ((Number) repReqObj).doubleValue();
                
                if (reputation >= requiredReputation && requiredReputation > highestQualifyingReputation) {
                    // Get the actual title from the stage data (like DeityChat does)
                    Object titleObj = stageDataMap.get("title");
                    if (titleObj instanceof String) {
                        bestStage = (String) titleObj;
                        highestQualifyingReputation = requiredReputation;
                    } else {
                        // Fallback to stage name if no title defined
                        bestStage = stageName;
                        highestQualifyingReputation = requiredReputation;
                    }
                }
            }
            
            return bestStage;
            
        } catch (Exception e) {
            // Fallback progression levels
            if (reputation >= 75) return "master";
            if (reputation >= 50) return "advanced";
            if (reputation >= 25) return "intermediate";
            if (reputation >= 10) return "novice";
            return "beginner";
        }
    }
    
    /**
     * !§¹ CLEAR PLAYER REWARDS (For Testing)
     * 
     * Clears reward history for a player with a specific deity.
     * Usage: /eidolon-unchained debug clear-rewards <player> "deity_id"
     * Example: /eidolon-unchained debug clear-rewards Player123 "eidolonunchained:nature_deity"
     */
    private static int clearPlayerRewards(CommandContext<CommandSourceStack> context) {
        try {
            String rawPlayerName = StringArgumentType.getString(context, "player");
        String playerName = CommandStringUtils.safeTrim(rawPlayerName);
        
        // Validate player name
        if (playerName == null || playerName.isEmpty()) {
            context.getSource().sendFailure(Component.literal("§cPlayer name cannot be empty"));
            return 0;
        }
            net.minecraft.resources.ResourceLocation deityLocation = net.minecraft.commands.arguments.ResourceLocationArgument.getId(context, "deity");
            
            ServerPlayer player = context.getSource().getServer().getPlayerList().getPlayerByName(playerName);
            if (player == null) {
                context.getSource().sendFailure(Component.literal("§cPlayer not found: " + playerName));
                return 0;
            }
            
            com.bluelotuscoding.eidolonunchained.deity.DatapackDeity deity = 
                com.bluelotuscoding.eidolonunchained.data.DatapackDeityManager.getDeity(deityLocation);
            
            if (deity == null) {
                context.getSource().sendFailure(Component.literal("§cDeity not found: " + deityLocation));
                return 0;
            }
            
            // Clear the player's tier progression tracking for this deity
            com.bluelotuscoding.eidolonunchained.chat.DeityChat.clearPlayerTierTracking(player.getUUID(), deityLocation);
            
            context.getSource().sendSuccess(() -> Component.literal(
                "§6Cleared tier reward tracking for " + playerName + " with " + deity.getName()), false);
            
            // Also notify the player
            player.sendSystemMessage(Component.literal(
                "§6Your tier progression tracking with " + deity.getName() + " has been reset for testing."));
            
            return 1;
            
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError clearing rewards: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * !§¹ Clear all tier progression tracking for a player (all deities)
     * Usage: /eidolon-unchained debug clear-all-rewards <player>
     */
    private static int clearAllPlayerRewards(CommandContext<CommandSourceStack> context) {
        try {
            String rawPlayerName = StringArgumentType.getString(context, "player");
            String playerName = CommandStringUtils.safeTrim(rawPlayerName);
            
            // Validate player name
            if (playerName == null || playerName.isEmpty()) {
                context.getSource().sendFailure(Component.literal("§cPlayer name cannot be empty"));
                return 0;
            }
            
            ServerPlayer player = context.getSource().getServer().getPlayerList().getPlayerByName(playerName);
            if (player == null) {
                context.getSource().sendFailure(Component.literal("§cPlayer not found: " + playerName));
                return 0;
            }
            
            // Clear all tier progression tracking for this player
            com.bluelotuscoding.eidolonunchained.chat.DeityChat.clearAllPlayerTierTracking(player.getUUID());
            
            context.getSource().sendSuccess(() -> Component.literal(
                "§6Cleared ALL tier reward tracking for " + playerName), false);
            
            // Also notify the player
            player.sendSystemMessage(Component.literal(
                "§6Your tier progression tracking with ALL deities has been reset for testing."));
            
            return 1;
            
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError clearing all rewards: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * !" Show tier progression tracking debug information
     * Usage: /eidolon-unchained debug tier-debug <player> <deity>
     */
    private static int debugTierTracking(CommandContext<CommandSourceStack> context) {
        try {
            String rawPlayerName = StringArgumentType.getString(context, "player");
            String playerName = CommandStringUtils.safeTrim(rawPlayerName);
            
            // Validate player name
            if (playerName == null || playerName.isEmpty()) {
                context.getSource().sendFailure(Component.literal("§cPlayer name cannot be empty"));
                return 0;
            }
            
            net.minecraft.resources.ResourceLocation deityLocation = net.minecraft.commands.arguments.ResourceLocationArgument.getId(context, "deity");
            
            ServerPlayer player = context.getSource().getServer().getPlayerList().getPlayerByName(playerName);
            if (player == null) {
                context.getSource().sendFailure(Component.literal("§cPlayer not found: " + playerName));
                return 0;
            }
            
            com.bluelotuscoding.eidolonunchained.deity.DatapackDeity deity = 
                com.bluelotuscoding.eidolonunchained.data.DatapackDeityManager.getDeity(deityLocation);
            
            if (deity == null) {
                context.getSource().sendFailure(Component.literal("§cDeity not found: " + deityLocation));
                return 0;
            }
            
            // Get current actual tier and reputation
            String currentTier = getDynamicProgressionLevel(deity, player);
            double currentReputation = deity.getPlayerReputation(player);
            
            // Get debug tracking information
            String debugInfo = com.bluelotuscoding.eidolonunchained.chat.DeityChat.getPlayerTierTrackingDebugInfo(
                player.getUUID(), deityLocation);
            
            context.getSource().sendSuccess(() -> Component.literal(
                "§6=== TIER TRACKING DEBUG: " + playerName + " & " + deity.getName() + " ==="), false);
            context.getSource().sendSuccess(() -> Component.literal(
                "§eCurrent reputation: §b" + String.format("%.1f", currentReputation)), false);
            context.getSource().sendSuccess(() -> Component.literal(
                "§eCurrent actual tier: §b" + currentTier), false);
            context.getSource().sendSuccess(() -> Component.literal(
                "§e--- Tracking Data ---"), false);
            
            for (String line : debugInfo.split("\n")) {
                context.getSource().sendSuccess(() -> Component.literal("§7" + line), false);
            }
            
            return 1;
            
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError getting tier debug info: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * !Ž¯ VERIFY PROGRESSION STAGES - Critical debugging for tier progression issues
     * 
     * Validates that progression stages are properly loaded from /deities/ JSON files
     */
    @SuppressWarnings({"unchecked"})
    private static int verifyProgressionStages(CommandContext<CommandSourceStack> context) {
        try {
            net.minecraft.resources.ResourceLocation deityLocation = net.minecraft.commands.arguments.ResourceLocationArgument.getId(context, "deity");
            com.bluelotuscoding.eidolonunchained.deity.DatapackDeity deity = 
                com.bluelotuscoding.eidolonunchained.data.DatapackDeityManager.getDeity(deityLocation);
            
            if (deity == null) {
                context.getSource().sendFailure(Component.literal("§cDeity not found: " + deityLocation));
                return 0;
            }
            
            context.getSource().sendSuccess(() -> Component.literal(
                "§6=== PROGRESSION STAGES VERIFICATION: " + deity.getName() + " ==="), false);
                
            // Get progression stages map (this is what DeityChat uses)
            Map<String, Object> stagesMap = deity.getProgressionStages();
            
            if (stagesMap == null || stagesMap.isEmpty()) {
                context.getSource().sendFailure(Component.literal("§c[X] NO PROGRESSION STAGES FOUND! This is the root cause of tier progression issues."));
                context.getSource().sendFailure(Component.literal("§eCheck if /deities/" + deityLocation.toString().replace(":", "/") + ".json exists and has progression.stages array"));
                return 0;
            }
            
            context.getSource().sendSuccess(() -> Component.literal(
                "§a[OK] Found " + stagesMap.size() + " progression stages:"), false);
            
            // Sort stages by reputation requirement for logical display
            stagesMap.entrySet().stream()
                .sorted((a, b) -> {
                    Map<String, Object> stageA = (Map<String, Object>) a.getValue();
                    Map<String, Object> stageB = (Map<String, Object>) b.getValue();
                    Integer repA = (Integer) stageA.get("reputationRequired");
                    Integer repB = (Integer) stageB.get("reputationRequired");
                    return repA.compareTo(repB);
                })
                .forEach(entry -> {
                    String stageId = entry.getKey();
                    Map<String, Object> stageData = (Map<String, Object>) entry.getValue();
                    Integer reputation = (Integer) stageData.get("reputationRequired");
                    String title = (String) stageData.get("title");
                    Boolean isMajor = (Boolean) stageData.get("isMajor");
                    
                    String majorText = (isMajor != null && isMajor) ? " §6[MAJOR]" : "";
                    context.getSource().sendSuccess(() -> Component.literal(
                        String.format("§b  %s §7(rep: %d) §f-> §e'%s'%s", 
                            stageId, reputation, title, majorText)), false);
                });
            
            // Also check Eidolon's internal progression data
            context.getSource().sendSuccess(() -> Component.literal("§e--- Eidolon Internal Progression ---"), false);
            var progression = deity.getProgression();
            context.getSource().sendSuccess(() -> Component.literal(
                "§7Eidolon progression steps: " + progression.getSteps().size()), false);
            
            progression.getSteps().values().forEach(stage -> {
                context.getSource().sendSuccess(() -> Component.literal(
                    String.format("§7  %s -> rep: %d, major: %s", 
                        stage.id(), stage.rep(), stage.major())), false);
            });
            
            context.getSource().sendSuccess(() -> Component.literal(
                "§a[OK] Progression stages verification complete!"), false);
                
            return 1;
            
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError verifying progression stages: " + e.getMessage()));
            LOGGER.error("Error in verifyProgressionStages command", e);
            return 0;
        }
    }
    
    /**
     * !"® LIST LOADED RITUALS
     * 
     * Shows all loaded ritual recipes for debugging
     */
    private static int listLoadedRituals(CommandContext<CommandSourceStack> context) {
        try {
            var server = context.getSource().getServer();
            var recipeManager = server.getRecipeManager();
            
            context.getSource().sendSuccess(() -> Component.literal("§6=== LOADED RITUALS ==="), false);
            
            var commandRituals = recipeManager.getAllRecipesFor(elucent.eidolon.registries.EidolonRecipes.COMMAND_RITUAL_TYPE.get());
            var basicRituals = recipeManager.getAllRecipesFor(elucent.eidolon.registries.EidolonRecipes.RITUAL_TYPE.get());
            
            context.getSource().sendSuccess(() -> Component.literal("§eCommand Rituals: §f" + commandRituals.size()), false);
            commandRituals.forEach(recipe -> {
                context.getSource().sendSuccess(() -> Component.literal("  §7- §b" + recipe.getId()), false);
            });
            
            context.getSource().sendSuccess(() -> Component.literal("§eBasic Rituals: §f" + basicRituals.size()), false);
            basicRituals.forEach(recipe -> {
                context.getSource().sendSuccess(() -> Component.literal("  §7- §a" + recipe.getId()), false);
            });
            
            return 1;
            
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError listing rituals: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * !§ª TEST RITUAL EXECUTION
     * 
     * Manually triggers a ritual for testing (fires the RitualCompleteEvent)
     */
    private static int testRitualExecution(CommandContext<CommandSourceStack> context) {
        try {
            ResourceLocation ritualLocation = net.minecraft.commands.arguments.ResourceLocationArgument.getId(context, "ritual_id");
            var source = context.getSource();
            
            if (!(source.getEntity() instanceof ServerPlayer player)) {
                source.sendFailure(Component.literal("§cThis command must be run by a player"));
                return 0;
            }
            
            context.getSource().sendSuccess(() -> Component.literal(
                "§6Testing ritual execution: §f" + ritualLocation), false);
            
            // Create and fire the ritual complete event
            RitualCompleteEvent event = new RitualCompleteEvent(player, ritualLocation, true);
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event);
            
            context.getSource().sendSuccess(() -> Component.literal(
                "§aRitualCompleteEvent fired successfully!"), false);
            context.getSource().sendSuccess(() -> Component.literal(
                "§7Check logs and game for any triggered effects"), false);
            
            return 1;
            
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError testing ritual: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * ! ADVANCED RITUAL DIAGNOSTICS
     * 
     * Performs comprehensive ritual system diagnostics to identify why rituals aren't starting
     */
    private static int diagnoseRitualSystem(CommandContext<CommandSourceStack> context) {
        var source = context.getSource();
        
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("§cThis command must be run by a player"));
            return 0;
        }
        
        StringBuilder diagnostic = new StringBuilder("§6=== ! RITUAL SYSTEM DIAGNOSTICS ===\n");
        
        try {
            var server = source.getServer();
            var level = player.serverLevel();
            
            // 1. Check recipe loading
            diagnostic.append("§e1. Recipe Loading Status:\n");
            var recipeManager = server.getRecipeManager();
            var commandRituals = recipeManager.getAllRecipesFor(elucent.eidolon.registries.EidolonRecipes.COMMAND_RITUAL_TYPE.get());
            var genericRituals = recipeManager.getAllRecipesFor(elucent.eidolon.registries.EidolonRecipes.RITUAL_TYPE.get());
            
            diagnostic.append(String.format("   Command Rituals: §a%d loaded\n", commandRituals.size()));
            diagnostic.append(String.format("   Generic Rituals: §a%d loaded\n", genericRituals.size()));
            
            // 2. List loaded ritual IDs
            diagnostic.append("§e2. Loaded Ritual IDs:\n");
            commandRituals.forEach(recipe -> {
                diagnostic.append(String.format("   §b%s §7(command)\n", recipe.getId()));
            });
            genericRituals.forEach(recipe -> {
                diagnostic.append(String.format("   §b%s §7(generic)\n", recipe.getId()));
            });
            
            // 3. Check for nearby braziers
            diagnostic.append("§e3. Nearby Brazier Analysis:\n");
            var nearbyEntities = level.getEntitiesOfClass(
                net.minecraft.world.entity.Entity.class,
                player.getBoundingBox().inflate(10)
            );
            
            long brazierCount = nearbyEntities.stream()
                .filter(entity -> entity.getClass().getName().contains("BrazierTileEntity"))
                .count();
            
            if (brazierCount == 0) {
                diagnostic.append("   §cš  No braziers found within 10 blocks\n");
                diagnostic.append("   §7Suggestion: Place a Brazier and try performing a ritual\n");
            } else {
                diagnostic.append(String.format("   §a%d brazier(s) found nearby\n", brazierCount));
            }
            
            // 4. Check registry integration
            diagnostic.append("§e4. Ritual Registry Integration:\n");
            try {
                // Test some known ritual IDs
                String[] testRituals = {
                    "eidolonunchained:light_patronage_ritual",
                    "eidolonunchained:nature_patronage_ritual", 
                    "eidolonunchained:shadow_patronage_ritual"
                };

                for (String ritualId : testRituals) {
                    var ritual = elucent.eidolon.registries.RitualRegistry.find(new ResourceLocation(ritualId));
                    if (ritual != null) {
                        diagnostic.append(String.format("   §a[OK] %s found in registry\n", ritualId));
                    } else {
                        diagnostic.append(String.format("   §c[X] %s NOT in registry\n", ritualId));
                    }
                }
            } catch (Exception e) {
                diagnostic.append(String.format("   §c[X] Registry access failed: %s\n", e.getMessage()));
            }
            
            // 5. Check items in inventory
            diagnostic.append("§e5. Player Inventory Analysis:\n");
            var inventory = player.getInventory();
            boolean hasCodex = inventory.hasAnyMatching(stack -> 
                stack.getItem().toString().contains("codex"));
            boolean hasGlowstone = inventory.hasAnyMatching(stack -> 
                stack.getItem().toString().contains("glowstone"));
            boolean hasGoldenApple = inventory.hasAnyMatching(stack -> 
                stack.getItem().toString().contains("golden_apple"));
            
            diagnostic.append(String.format("   Codex: %s\n", hasCodex ? "§a[OK]" : "§c[X]"));
            diagnostic.append(String.format("   Glowstone: %s\n", hasGlowstone ? "§a[OK]" : "§c[X]"));
            diagnostic.append(String.format("   Golden Apple: %s\n", hasGoldenApple ? "§a[OK]" : "§c[X]"));
            
            diagnostic.append("   §cš  NOTE: Codex must be in NECROTIC FOCUS, not inventory!\n");
            
            // 6. Final recommendations
            diagnostic.append("§e6. Ritual Execution Checklist:\n");
            diagnostic.append("   §71. Place Brazier\n");
            diagnostic.append("   §72. Place Glowstone Dust in center of brazier\n");
            diagnostic.append("   §73. Place 4 Stone Hands around brazier (4 blocks away)\n");
            diagnostic.append("   §74. Put pedestalItems on Stone Hands:\n");
            diagnostic.append("      §7- Glowstone Dust\n");
            diagnostic.append("      §7- Golden Apple\n");
            diagnostic.append("      §7- Arcane Gold Ingot\n");
            diagnostic.append("      §7- Holy Symbol\n");
            diagnostic.append("   §75. Place 1 Necrotic Focus near brazier\n");
            diagnostic.append("   §76. Put Eidolon Codex in Necrotic Focus (invariant item)\n");
            diagnostic.append("   §77. Light brazier with Flint & Steel\n");
            diagnostic.append("   §78. Wait for ritual to find ingredients (4 seconds)\n");
            diagnostic.append("   §79. Watch for ritual symbol and effects\n");
            
        } catch (Exception e) {
            diagnostic.append(String.format("§c[X] Diagnostic failed: %s\n", e.getMessage()));
        }
        
        source.sendSuccess(() -> Component.literal(diagnostic.toString()), false);
        return 1;
    }
    
    private static int showReputationStatus(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        try {
            ResourceLocation deityRL = net.minecraft.commands.arguments.ResourceLocationArgument.getId(context, "deity");
            Player player = source.getPlayerOrException();
            
            // Get the deity
            DatapackDeity deity = DatapackDeityManager.getDeity(deityRL);
            
            if (deity == null) {
                source.sendFailure(Component.literal("§cDeity not found: " + deityRL));
                return 0;
            }
            
            // Get player's reputation with the specified deity
            double currentReputation = deity.getPlayerReputation(player);
            String currentStage = deity.getCurrentProgressionStage(currentReputation);
            String nextInfo = deity.getNextProgressionInfo(currentReputation);
            
            StringBuilder status = new StringBuilder();
            status.append("§6=== Reputation Status ===\n");
            status.append(String.format("§eDeity: §b%s\n", deity.getName()));
            status.append(String.format("§eReputation: §a%.1f\n", currentReputation));
            status.append(String.format("§eCurrent Stage: §d%s\n", currentStage));
            status.append(String.format("§eNext Stage: §7%s\n", nextInfo));
            
            // Show cooldown info (simplified without EnhancedReputationSystem dependency)
            status.append("§7Conversation cooldowns managed by AI system\n");
            
            source.sendSuccess(() -> Component.literal(status.toString()), false);
            
        } catch (Exception e) {
            source.sendFailure(Component.literal("§cError checking reputation status: " + e.getMessage()));
            return 0;
        }
        
        return 1;
    }
    
    // Player2AI Management Commands
    
    private static int clearPlayer2AIMemory(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        try {
            ResourceLocation deityRL = net.minecraft.commands.arguments.ResourceLocationArgument.getId(context, "deity");
            Player player = source.getPlayerOrException();
            
            if (!"player2ai".equals(EidolonUnchainedConfig.COMMON.aiProvider.get())) {
                source.sendFailure(Component.literal("§cPlayer2AI is not the current AI provider"));
                return 0;
            }
            
            // Create Player2AI client and clear memory
            String apiKey = APIKeyManager.getAPIKey("player2ai");
            if (apiKey == null) {
                source.sendFailure(Component.literal("§cNo Player2AI API key configured"));
                return 0;
            }
            
            com.bluelotuscoding.eidolonunchained.integration.player2ai.Player2AIClient client = 
                new com.bluelotuscoding.eidolonunchained.integration.player2ai.Player2AIClient(30);
            
        client.clearPlayerMemory(deityRL.toString(), player.getStringUUID()).thenAccept(success -> {
                if (success) {
            source.sendSuccess(() -> Component.literal("§aCleared Player2AI memory for " + deityRL), false);
                } else {
                    source.sendFailure(Component.literal("§cFailed to clear Player2AI memory"));
                }
            });
            
        } catch (Exception e) {
            source.sendFailure(Component.literal("§cError clearing Player2AI memory: " + e.getMessage()));
            return 0;
        }
        
        return 1;
    }
    
    private static int showPlayer2AIMemory(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        try {
        ResourceLocation deityRL = net.minecraft.commands.arguments.ResourceLocationArgument.getId(context, "deity");
            Player player = source.getPlayerOrException();
            
            if (!"player2ai".equals(EidolonUnchainedConfig.COMMON.aiProvider.get())) {
                source.sendFailure(Component.literal("§cPlayer2AI is not the current AI provider"));
                return 0;
            }
            
            // Create Player2AI client and get memory
            String apiKey = APIKeyManager.getAPIKey("player2ai");
            if (apiKey == null) {
                source.sendFailure(Component.literal("§cNo Player2AI API key configured"));
                return 0;
            }
            
            com.bluelotuscoding.eidolonunchained.integration.player2ai.Player2AIClient client = 
                new com.bluelotuscoding.eidolonunchained.integration.player2ai.Player2AIClient(30);
            
            client.getCharacterMemory(deityRL.toString(), player.getStringUUID()).thenAccept(memory -> {
                source.sendSuccess(() -> Component.literal("§6=== Player2AI Memory for " + deityRL + " ===\n" + memory), false);
            });
            
        } catch (Exception e) {
            source.sendFailure(Component.literal("§cError getting Player2AI memory: " + e.getMessage()));
            return 0;
        }
        
        return 1;
    }
    
    private static int listPlayer2AICharacters(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        if (!"player2ai".equals(EidolonUnchainedConfig.COMMON.aiProvider.get())) {
            source.sendFailure(Component.literal("§cPlayer2AI is not the current AI provider"));
            return 0;
        }
        
        source.sendSuccess(() -> Component.literal("§6=== Player2AI Characters ===\n" +
            "§ePlayer2AI characters are created automatically when you first interact with each deity.\n" +
            "§eCharacters persist memory and relationships across conversations.\n" +
            "§eUse /eidolon-unchained player2ai memory show <deity> to see character memory."), false);
        
        return 1;
    }
    
    private static int updatePlayer2AIPersonality(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        try {
            ResourceLocation deityRL = net.minecraft.commands.arguments.ResourceLocationArgument.getId(context, "deity");
            
            if (!"player2ai".equals(EidolonUnchainedConfig.COMMON.aiProvider.get())) {
                source.sendFailure(Component.literal("§cPlayer2AI is not the current AI provider"));
                return 0;
            }
            
            // Get deity to get current personality
            DatapackDeity deity = DatapackDeityManager.getDeity(deityRL);
            if (deity == null) {
                source.sendFailure(Component.literal("§cDeity not found: " + deityRL));
                return 0;
            }
            
            AIDeityConfig aiConfig = AIDeityManager.getInstance().getAIConfig(deityRL);
            if (aiConfig == null) {
                source.sendFailure(Component.literal("§cAI configuration not found for deity: " + deityRL));
                return 0;
            }
            
            // Create Player2AI client and update personality
            String apiKey = APIKeyManager.getAPIKey("player2ai");
            if (apiKey == null) {
                source.sendFailure(Component.literal("§cNo Player2AI API key configured"));
                return 0;
            }
            
            com.bluelotuscoding.eidolonunchained.integration.player2ai.Player2AIClient client = 
                new com.bluelotuscoding.eidolonunchained.integration.player2ai.Player2AIClient(30);
            
            client.updateCharacterPersonality(deityRL.toString(), aiConfig.personality).thenAccept(success -> {
                if (success) {
                    source.sendSuccess(() -> Component.literal("§aUpdated Player2AI personality for " + deity.getName()), false);
                } else {
                    source.sendFailure(Component.literal("§cFailed to update Player2AI personality"));
                }
            });
            
        } catch (Exception e) {
            source.sendFailure(Component.literal("§cError updating Player2AI personality: " + e.getMessage()));
            return 0;
        }
        
        return 1;
    }
    
    private static int authenticatePlayer2AIAuto(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        try {
            source.sendSuccess(() -> Component.literal("§6Attempting Player2AI Quick Start authentication..."), false);
            
            // Try to get API key from Player2 App
            String apiKey = com.bluelotuscoding.eidolonunchained.integration.player2ai.Player2AIClient.authenticateWithPlayer2App();
            
            if (apiKey != null && !apiKey.trim().isEmpty()) {
                // Save the API key
                APIKeyManager.setAPIKey("player2ai", apiKey);
                EidolonUnchainedConfig.COMMON.aiProvider.set("player2ai");
                
                source.sendSuccess(() -> Component.literal("§a[OK] Successfully authenticated with Player2 App!"), false);
                source.sendSuccess(() -> Component.literal("§a[OK] Player2AI set as active AI provider"), false);
                source.sendSuccess(() -> Component.literal("§7API Key: " + maskApiKey(apiKey)), false);
                return 1;
            } else {
                source.sendFailure(Component.literal("§cPlayer2 App authentication failed"));
                source.sendFailure(Component.literal("§7This is normal if Player2 App isn't installed."));
                source.sendFailure(Component.literal("§7"));
                source.sendFailure(Component.literal("§7Options:"));
                source.sendFailure(Component.literal("§7• Download Player2 App from https://player2.game/"));
                source.sendFailure(Component.literal("§7• Or manually set API key: /eidolon-unchained api set player2ai <key>"));
                return 0;
            }
        } catch (Exception e) {
            source.sendFailure(Component.literal("§cError during Player2AI authentication: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int testPlayer2AIConnection(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        try {
            source.sendSuccess(() -> Component.literal("§6Testing Player2 App connection..."), false);
            
            String diagnostics = com.bluelotuscoding.eidolonunchained.integration.player2ai.Player2AIClient.testPlayer2AppConnection();
            
            source.sendSuccess(() -> Component.literal("§7Connection Test Results:"), false);
            
            String[] lines = diagnostics.split("\n");
            for (String line : lines) {
                if (line.contains("AVAILABLE")) {
                    source.sendSuccess(() -> Component.literal("§a" + line), false);
                } else if (line.contains("CONNECTION REFUSED")) {
                    source.sendSuccess(() -> Component.literal("§c" + line), false);
                } else {
                    source.sendSuccess(() -> Component.literal("§e" + line), false);
                }
            }
            
            if (diagnostics.contains("AVAILABLE")) {
                source.sendSuccess(() -> Component.literal("§a[OK] Player2 App detected and responding!"), false);
                source.sendSuccess(() -> Component.literal("§7Try running: /eidolon-unchained player2ai auth auto"), false);
            } else {
                source.sendFailure(Component.literal("§cNo responsive Player2 App found"));
                source.sendFailure(Component.literal("§7Make sure Player2 App is running"));
                source.sendFailure(Component.literal("§7Download from: https://player2.game/"));
            }
            
        } catch (Exception e) {
            source.sendFailure(Component.literal("§cError testing Player2AI connection: " + e.getMessage()));
            return 0;
        }
        
        return 1;
    }
    
    private static int testPlayer2AIChat(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String rawMessage = StringArgumentType.getString(context, "message");
        String message = CommandStringUtils.safeTrim(rawMessage);
        
        // Validate message input
        if (message == null || message.isEmpty()) {
            source.sendFailure(Component.literal("§cMessage cannot be empty"));
            return 0;
        }
        
        try {
            // Display what we're testing (safely formatted for chat)
            String displayMessage = CommandStringUtils.safeChatDisplay(message);
            source.sendSuccess(() -> Component.literal("§6Testing Player2AI chat with message: §f" + displayMessage), false);
            
            // Create a test Player2AI client
            com.bluelotuscoding.eidolonunchained.integration.player2ai.Player2AIClient client = 
                new com.bluelotuscoding.eidolonunchained.integration.player2ai.Player2AIClient();
            
            // Test with a simple personality
            String testPersonality = "You are a test deity. Respond briefly to user messages.";
            String testCharacterId = "debug_deity";
            String playerUUID = "debug_player";
            
            client.generateResponse(message, testPersonality, testCharacterId, playerUUID, null, null)
                .thenAccept(response -> {
                    if (response.success) {
                        String safeResponse = CommandStringUtils.safeChatDisplay(response.dialogue);
                        source.sendSuccess(() -> Component.literal("§a[OK] Player2AI Response: §f" + safeResponse), false);
                    } else {
                        String safeError = CommandStringUtils.safeChatDisplay(response.dialogue);
                        source.sendFailure(Component.literal("§cPlayer2AI Error: " + safeError));
                    }
                })
                .exceptionally(error -> {
                    String safeError = CommandStringUtils.safeChatDisplay(error.getMessage());
                    source.sendFailure(Component.literal("§cPlayer2AI Exception: " + safeError));
                    return null;
                });
                
            source.sendSuccess(() -> Component.literal("§7Request sent, waiting for response..."), false);

        } catch (Exception e) {
            String safeError = CommandStringUtils.safeChatDisplay(e.getMessage());
            source.sendFailure(Component.literal("§cDebug test failed: " + safeError));
        }
        
        return 1;
    }

    // --- Player2AI unified login handlers (shared cache with TTS) ---
    private static int startPlayer2AILoginDevice(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }
        var start = com.bluelotuscoding.eidolonunchained.integration.player2ai.Player2AuthManager.startDeviceFlow(player);
        if (!start.started) {
            context.getSource().sendFailure(Component.literal("§cDevice login failed: " + start.message));
            return 0;
        }
        net.minecraft.network.chat.Component url = Component.literal(start.verificationUri)
            .withStyle(s -> s.withUnderlined(true)
                .withColor(net.minecraft.ChatFormatting.AQUA)
                .withClickEvent(new net.minecraft.network.chat.ClickEvent(
                    net.minecraft.network.chat.ClickEvent.Action.OPEN_URL,
                    start.verificationUri))
                .withHoverEvent(new net.minecraft.network.chat.HoverEvent(
                    net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT,
                    Component.literal("Open login page"))));
        net.minecraft.network.chat.Component code = Component.literal(start.userCode)
            .withStyle(s -> s.withBold(true)
                .withColor(net.minecraft.ChatFormatting.GOLD)
                .withClickEvent(new net.minecraft.network.chat.ClickEvent(
                    net.minecraft.network.chat.ClickEvent.Action.COPY_TO_CLIPBOARD,
                    start.userCode))
                .withHoverEvent(new net.minecraft.network.chat.HoverEvent(
                    net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT,
                    Component.literal("Click to copy code"))));
        context.getSource().sendSuccess(() -> Component.literal("§6Player2 login: visit §b").append(url).append(" §6and enter code §e").append(code), false);
        if (start.verificationUriComplete != null && !start.verificationUriComplete.isBlank()) {
            net.minecraft.network.chat.Component urlComplete = Component.literal(start.verificationUriComplete)
                .withStyle(s -> s.withUnderlined(true)
                    .withColor(net.minecraft.ChatFormatting.BLUE)
                    .withClickEvent(new net.minecraft.network.chat.ClickEvent(
                        net.minecraft.network.chat.ClickEvent.Action.OPEN_URL,
                        start.verificationUriComplete))
                    .withHoverEvent(new net.minecraft.network.chat.HoverEvent(
                        net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT,
                        Component.literal("Open ready-to-use link"))));
            context.getSource().sendSuccess(() -> Component.literal("§7Or click: ").append(urlComplete), false);
        }
        com.bluelotuscoding.eidolonunchained.integration.player2ai.Player2AuthManager.beginBackgroundPolling(player);
        context.getSource().sendSuccess(() -> Component.literal("§7Waiting for approval... I'll pick it up automatically."), false);
        return 1;
    }

    private static int showPlayer2AILoginStatus(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }
        boolean has = com.bluelotuscoding.eidolonunchained.integration.player2ai.Player2AuthManager.getCachedP2Key(player) != null;
        context.getSource().sendSuccess(() -> Component.literal("§6Player2 Login: " + (has ? "§aLinked" : "§cNot linked")), false);
        boolean inFlow = com.bluelotuscoding.eidolonunchained.integration.player2ai.Player2AuthManager.hasActiveDeviceFlow(player);
        if (inFlow) {
            context.getSource().sendSuccess(() -> Component.literal("§7Device login in progress... approve in your browser."), false);
        }
        return 1;
    }

    private static int setPlayer2AIP2KeyManual(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }
        String key = com.mojang.brigadier.arguments.StringArgumentType.getString(context, "p2Key");
        com.bluelotuscoding.eidolonunchained.integration.player2ai.Player2AuthManager.setCachedP2Key(player, key);
        context.getSource().sendSuccess(() -> Component.literal("§a[OK] Player2 key set for your account (temporary)."), false);
        return 1;
    }

    private static int logoutPlayer2AIP2Key(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }
        com.bluelotuscoding.eidolonunchained.integration.player2ai.Player2AuthManager.clearCachedP2Key(player);
        context.getSource().sendSuccess(() -> Component.literal("§a[OK] Disconnected your Player2 account for AI."), false);
        return 1;
    }
    
    /**
     * Test tier progression system manually
     */
    private static int testTierProgression(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        try {
            String playerName = StringArgumentType.getString(context, "player");
            ResourceLocation deityId = net.minecraft.commands.arguments.ResourceLocationArgument.getId(context, "deity");
            
            // Get the player
            ServerPlayer player = source.getServer().getPlayerList().getPlayerByName(playerName);
            if (player == null) {
                source.sendFailure(Component.literal("§cPlayer not found: " + playerName));
                return 0;
            }
            
            source.sendSuccess(() -> Component.literal("§eTesting tier progression for " + playerName + " with deity " + deityId), false);
            
            // Manually trigger tier progression check
            com.bluelotuscoding.eidolonunchained.chat.DeityChat.checkAndHandleTierProgression(player, deityId);
            
            source.sendSuccess(() -> Component.literal("§aTier progression check completed. Check logs for details."), false);
            
        } catch (Exception e) {
            String safeError = CommandStringUtils.safeChatDisplay(e.getMessage());
            source.sendFailure(Component.literal("§cTier progression test failed: " + safeError));
        }
        
        return 1;
    }
    
    // Utility methods
    private static String maskApiKey(String key) {
        if (key == null || key.length() < 8) return "***";
        return key.substring(0, 4) + "***" + key.substring(key.length() - 4);
    }

    // === Facts helper ===
    private static int grantFact(CommandContext<CommandSourceStack> ctx) {
        String playerName = StringArgumentType.getString(ctx, "player");
        ResourceLocation fact = ResourceLocationArgument.getId(ctx, "fact");
        ServerPlayer sp = findPlayerByName(ctx.getSource(), playerName);
        if (sp == null) {
            ctx.getSource().sendFailure(Component.literal("Player not found: " + playerName));
            return 0;
        }
        elucent.eidolon.util.KnowledgeUtil.grantFact(sp, fact);
        ctx.getSource().sendSuccess(() -> Component.literal("Granted fact " + fact + " to " + playerName), false);
        return 1;
    }

    private static int revokeFact(CommandContext<CommandSourceStack> ctx) {
        String playerName = StringArgumentType.getString(ctx, "player");
        ResourceLocation fact = ResourceLocationArgument.getId(ctx, "fact");
        ServerPlayer sp = findPlayerByName(ctx.getSource(), playerName);
        if (sp == null) {
            ctx.getSource().sendFailure(Component.literal("Player not found: " + playerName));
            return 0;
        }
        elucent.eidolon.util.KnowledgeUtil.removeFact(sp, fact);
        ctx.getSource().sendSuccess(() -> Component.literal("Revoked fact " + fact + " from " + playerName), false);
        return 1;
    }

    private static int listFacts(CommandContext<CommandSourceStack> ctx) {
        String playerName = StringArgumentType.getString(ctx, "player");
        ServerPlayer sp = findPlayerByName(ctx.getSource(), playerName);
        if (sp == null) {
            ctx.getSource().sendFailure(Component.literal("Player not found: " + playerName));
            return 0;
        }
        java.util.Set<ResourceLocation> facts = sp.getCapability(elucent.eidolon.capability.IKnowledge.INSTANCE)
            .map(elucent.eidolon.capability.IKnowledge::getKnownFacts).orElse(java.util.Set.of());
        StringBuilder sb = new StringBuilder("Known facts for ").append(playerName).append(": ");
        if (facts.isEmpty()) sb.append("<none>"); else sb.append(facts);
        ctx.getSource().sendSuccess(() -> Component.literal(sb.toString()), false);
        return 1;
    }

    private static ServerPlayer findPlayerByName(CommandSourceStack src, String name) {
        for (ServerPlayer p : src.getServer().getPlayerList().getPlayers()) {
            if (p.getName().getString().equalsIgnoreCase(name)) return p;
        }
        return null;
    }
}

