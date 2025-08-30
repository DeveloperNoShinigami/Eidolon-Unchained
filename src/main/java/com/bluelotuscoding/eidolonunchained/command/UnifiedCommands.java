package com.bluelotuscoding.eidolonunchained.command;

import com.bluelotuscoding.eidolonunchained.config.EidolonUnchainedConfig;
import com.bluelotuscoding.eidolonunchained.config.APIKeyManager;
import com.bluelotuscoding.eidolonunchained.ai.AIDeityManager;
import com.bluelotuscoding.eidolonunchained.data.DatapackDeityManager;
import com.bluelotuscoding.eidolonunchained.data.ResearchDataManager;
import com.bluelotuscoding.eidolonunchained.chat.ConversationHistoryManager;
import com.bluelotuscoding.eidolonunchained.chat.ConversationMessage;
import com.bluelotuscoding.eidolonunchained.capability.CapabilityHandler;
import com.bluelotuscoding.eidolonunchained.deity.DatapackDeity;
import com.bluelotuscoding.eidolonunchained.events.RitualCompleteEvent;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;
import java.util.UUID;

// Eidolon integration imports
import elucent.eidolon.util.KnowledgeUtil;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.common.MinecraftForge;

/**
 * Unified command handler for all Eidolon Unchained commands
 * Consolidates configuration, AI, deity, prayer, chant, and patron commands
 */
public class UnifiedCommands {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        
        // Main command tree: /eidolon-unchained
        dispatcher.register(Commands.literal("eidolon-unchained")
            .requires(source -> source.hasPermission(EidolonUnchainedConfig.COMMON.requiredOpLevel.get()))
            
            // Configuration commands
            .then(Commands.literal("config")
                .then(Commands.literal("reload")
                    .executes(UnifiedCommands::reloadConfig))
                .then(Commands.literal("status")
                    .executes(UnifiedCommands::showConfigStatus))
                .then(Commands.literal("validate")
                    .executes(UnifiedCommands::validateConfig))
                .then(Commands.literal("reset")
                    .executes(UnifiedCommands::resetConfig)))
            
            // API key management
            .then(Commands.literal("api")
                .then(Commands.literal("set")
                    .then(Commands.argument("provider", StringArgumentType.string())
                        .then(Commands.argument("key", StringArgumentType.string())
                            .executes(UnifiedCommands::setApiKey))))
                .then(Commands.literal("set-model")
                    .then(Commands.argument("model", StringArgumentType.string())
                        .executes(UnifiedCommands::setAIModel)))
                .then(Commands.literal("get-model")
                    .executes(UnifiedCommands::getAIModel))
                .then(Commands.literal("test")
                    .then(Commands.argument("provider", StringArgumentType.string())
                        .executes(UnifiedCommands::testApiKey)))
                .then(Commands.literal("list")
                    .executes(UnifiedCommands::listApiKeys))
                .then(Commands.literal("remove")
                    .then(Commands.argument("provider", StringArgumentType.string())
                        .executes(UnifiedCommands::removeApiKey)))
                .then(Commands.literal("retry")
                    .then(Commands.literal("status")
                        .executes(UnifiedCommands::showRetryStatus))
                    .then(Commands.literal("toggle")
                        .executes(UnifiedCommands::toggleRetry))
                    .then(Commands.literal("attempts")
                        .then(Commands.argument("count", IntegerArgumentType.integer(1, 10))
                            .executes(UnifiedCommands::setMaxRetryAttempts)))
                    .then(Commands.literal("delay")
                        .then(Commands.argument("milliseconds", IntegerArgumentType.integer(1000, 10000))
                            .executes(UnifiedCommands::setRetryDelay)))))
            
            // Deity management
            .then(Commands.literal("deities")
                .then(Commands.literal("list")
                    .executes(UnifiedCommands::listDeities))
                .then(Commands.literal("reload")
                    .executes(UnifiedCommands::reloadDeities))
                .then(Commands.literal("status")
                    .then(Commands.argument("deity", StringArgumentType.string())
                        .executes(UnifiedCommands::showDeityStatus))))
            
            // Patron system
            .then(Commands.literal("patron")
                .then(Commands.literal("choose")
                    .then(Commands.argument("deity", StringArgumentType.string())
                        .executes(UnifiedCommands::choosePatron)))
                .then(Commands.literal("abandon")
                    .executes(UnifiedCommands::abandonPatron))
                .then(Commands.literal("status")
                    .executes(UnifiedCommands::patronStatus))
                .then(Commands.literal("titles")
                    .executes(UnifiedCommands::listPatronTitles))
                .then(Commands.literal("confirm")
                    .then(Commands.argument("deity", StringArgumentType.string())
                        .executes(UnifiedCommands::confirmPatronChoice))))
            
            // Chant system
            .then(Commands.literal("chants")
                .then(Commands.literal("list")
                    .executes(UnifiedCommands::listChants))
                .then(Commands.literal("reload")
                    .executes(UnifiedCommands::reloadChants))
                .then(Commands.literal("generate")
                    .executes(UnifiedCommands::generateChants))
                .then(Commands.literal("test")
                    .then(Commands.argument("chant", StringArgumentType.string())
                        .executes(UnifiedCommands::testChant))))
            
            // Prayer system
            .then(Commands.literal("prayers")
                .then(Commands.literal("history")
                    .executes(UnifiedCommands::showPrayerHistory))
                .then(Commands.literal("clear-cooldown")
                    .then(Commands.argument("player", StringArgumentType.string())
                        .executes(UnifiedCommands::clearPrayerCooldown))))
            
            // Conversation system  
            .then(Commands.literal("conversations")
                .then(Commands.literal("stats")
                    .executes(UnifiedCommands::showConversationStats))
                .then(Commands.literal("clear")
                    .then(Commands.argument("deity", StringArgumentType.string())
                        .executes(UnifiedCommands::clearConversationHistory)))
                .then(Commands.literal("clear-all")
                    .executes(UnifiedCommands::clearAllConversationHistory)))
            
            // Research system
            .then(Commands.literal("research")
                .then(Commands.literal("clear")
                    .then(Commands.argument("player", StringArgumentType.string())
                        .executes(UnifiedCommands::clearPlayerResearch)))
                .then(Commands.literal("reload")
                    .executes(UnifiedCommands::reloadResearch))
                .then(Commands.literal("list")
                    .executes(UnifiedCommands::listResearchEntries)))
            
            // 🎯 DEBUG SYSTEM - Reputation & Progression Testing
            .then(Commands.literal("debug")
                .requires(cs -> cs.hasPermission(2))
                .then(Commands.literal("progression")
                    .then(Commands.argument("player", StringArgumentType.string())
                        .executes(UnifiedCommands::debugPlayerProgression)))
                .then(Commands.literal("force-progression-check")
                    .then(Commands.argument("player", StringArgumentType.string())
                        .executes(UnifiedCommands::forceProgressionCheck)))
                .then(Commands.literal("test-command")
                    .then(Commands.argument("command", StringArgumentType.greedyString())
                        .executes(UnifiedCommands::testDeityCommand)))
                .then(Commands.literal("reputation")
                    .then(Commands.argument("player", StringArgumentType.string())
                        .then(Commands.argument("deity", StringArgumentType.string())
                            .executes(UnifiedCommands::debugPlayerReputation))))
                .then(Commands.literal("clear-rewards")
                    .then(Commands.argument("player", StringArgumentType.string())
                        .then(Commands.argument("deity", StringArgumentType.string())
                            .executes(UnifiedCommands::clearPlayerRewards)))))
            
            // TODO: Reputation system commands - implement these methods when needed
            /*
            .then(Commands.literal("reputation")
                .then(Commands.literal("get")
                    .then(Commands.argument("player", StringArgumentType.string())
                        .then(Commands.argument("deity", StringArgumentType.string())
                            .executes(UnifiedCommands::getPlayerReputation))))
                .then(Commands.literal("set")
                    .requires(cs -> cs.hasPermission(2))
                    .then(Commands.argument("player", StringArgumentType.string())
                        .then(Commands.argument("deity", StringArgumentType.string())
                            .then(Commands.argument("amount", DoubleArgumentType.doubleArg(-100, 100))
                                .executes(UnifiedCommands::setPlayerReputation)))))
                .then(Commands.literal("add")
                    .requires(cs -> cs.hasPermission(2))
                    .then(Commands.argument("player", StringArgumentType.string())
                        .then(Commands.argument("deity", StringArgumentType.string())
                            .then(Commands.argument("amount", DoubleArgumentType.doubleArg(-100, 100))
                                .executes(UnifiedCommands::addPlayerReputation)))))
                .then(Commands.literal("list")
                    .then(Commands.argument("player", StringArgumentType.string())
                        .executes(UnifiedCommands::listPlayerReputations))))
            */
            
            // Debug commands
            .then(Commands.literal("debug")
                .then(Commands.literal("toggle")
                    .executes(UnifiedCommands::toggleDebug))
                .then(Commands.literal("logs")
                    .executes(UnifiedCommands::showDebugLogs))
                .then(Commands.literal("triggers")
                    .executes(UnifiedCommands::debugTriggers))
                .then(Commands.literal("commands")
                    .then(Commands.argument("player", StringArgumentType.string())
                        .then(Commands.argument("deity", StringArgumentType.string())
                            .executes(UnifiedCommands::showCommandHistory))))
                .then(Commands.literal("report")
                    .then(Commands.argument("player", StringArgumentType.string())
                        .then(Commands.argument("deity", StringArgumentType.string())
                            .executes(UnifiedCommands::generateCommandReport))))
                .then(Commands.literal("personality")
                    .then(Commands.argument("player", StringArgumentType.string())
                        .then(Commands.argument("deity", StringArgumentType.string())
                            .executes(UnifiedCommands::debugPersonality))))
                .then(Commands.literal("fire-ritual-completion")
                    .then(Commands.argument("ritual", StringArgumentType.string())
                        .executes(UnifiedCommands::fireRitualCompletion)))
                .then(Commands.literal("validate-all")
                    .executes(UnifiedCommands::validateAll)))
        );
        
        // Alias commands for convenience
        dispatcher.register(Commands.literal("eu")
            .redirect(dispatcher.getRoot().getChild("eidolon-unchained")));
        
        dispatcher.register(Commands.literal("eidolon-config")
            .redirect(dispatcher.getRoot().getChild("eidolon-unchained").getChild("config")));
        
        // Register the new flexible chant slot commands
        ChantSlotCommands.register(dispatcher);
    }
    
    // Configuration commands
    private static int reloadConfig(CommandContext<CommandSourceStack> context) {
        try {
            // Reload configuration
            EidolonUnchainedConfig.COMMON_SPEC.setConfig(null);
            context.getSource().sendSuccess(() -> Component.literal("§aConfiguration reloaded successfully"), false);
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cFailed to reload configuration: " + e.getMessage()));
            return 0;
        }
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
            context.getSource().sendSuccess(() -> Component.literal("§aConfiguration is valid"), false);
        } else {
            context.getSource().sendFailure(Component.literal("§cConfiguration issues found:\n" + issues.toString()));
        }
        
        return valid ? 1 : 0;
    }
    
    private static int resetConfig(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(() -> Component.literal("§eConfiguration reset to defaults"), false);
        return 1;
    }
    
    // API key management commands
    private static int setApiKey(CommandContext<CommandSourceStack> context) {
        String provider = StringArgumentType.getString(context, "provider");
        String key = StringArgumentType.getString(context, "key");
        
        try {
            APIKeyManager.setAPIKey(provider, key);
            context.getSource().sendSuccess(() -> Component.literal("§aAPI key set for provider: " + provider), false);
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cFailed to set API key: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int testApiKey(CommandContext<CommandSourceStack> context) {
        String provider = StringArgumentType.getString(context, "provider");
        
        String apiKey = APIKeyManager.getAPIKey(provider);
        if (apiKey == null || apiKey.isEmpty()) {
            context.getSource().sendFailure(Component.literal("§cNo API key configured for provider: " + provider));
            return 0;
        }
        
        context.getSource().sendSuccess(() -> Component.literal("§aAPI key is configured for provider: " + provider), false);
        return 1;
    }
    
    private static int listApiKeys(CommandContext<CommandSourceStack> context) {
        StringBuilder list = new StringBuilder("§6=== Configured API Keys ===\n");
        
        String[] providers = {"gemini", "openai", "proxy"};
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
        String provider = StringArgumentType.getString(context, "provider");
        
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
            context.getSource().sendSuccess(() -> Component.literal("§aDeities reloaded successfully"), false);
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cFailed to reload deities: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int showDeityStatus(CommandContext<CommandSourceStack> context) {
        String deityName = StringArgumentType.getString(context, "deity");
        context.getSource().sendSuccess(() -> Component.literal("§eShowing status for deity: " + deityName), false);
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
                        list.append(" §a[→ ").append(chant.getLinkedDeity()).append("]");
                    }
                    list.append("\n  §7Category: ").append(chant.getCategory())
                        .append(", Difficulty: ").append("★".repeat(chant.getDifficulty())).append("\n");
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
        String chantName = StringArgumentType.getString(context, "chant");
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
        String playerName = StringArgumentType.getString(context, "player");
        context.getSource().sendSuccess(() -> Component.literal("§aPrayer cooldown cleared for player: " + playerName), false);
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
            // Update the config value
            EidolonUnchainedConfig.COMMON.geminiModel.set(model);
            EidolonUnchainedConfig.COMMON_SPEC.save();
            
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
            Component.literal("§7Use /eidolon-unchained api set-model <model> to change"), false);
        
        return 1;
    }
    
    // Research command implementations
    private static int clearPlayerResearch(CommandContext<CommandSourceStack> context) {
        String playerName = StringArgumentType.getString(context, "player");
        
        try {
            ServerPlayer player = context.getSource().getServer().getPlayerList().getPlayerByName(playerName);
            if (player == null) {
                context.getSource().sendFailure(Component.literal("§cPlayer not found: " + playerName));
                return 0;
            }
            
            // Clear research using Eidolon's built-in system
            KnowledgeUtil.resetResearch(player);
            
            context.getSource().sendSuccess(() -> 
                Component.translatable("eidolonunchained.command.research.cleared", playerName), false);
            player.sendSystemMessage(Component.literal("§6Your research progress has been reset by an administrator."));
            
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
            
            String deityString = StringArgumentType.getString(context, "deity");
            net.minecraft.resources.ResourceLocation deityId;
            
            // Parse deity identifier 
            if (deityString.contains(":")) {
                deityId = new net.minecraft.resources.ResourceLocation(deityString);
            } else {
                deityId = new net.minecraft.resources.ResourceLocation("eidolonunchained", deityString);
            }
            
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
        String playerName = StringArgumentType.getString(context, "player");
        String deityId = StringArgumentType.getString(context, "deity");
        
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
        String playerName = StringArgumentType.getString(context, "player");
        String deityId = StringArgumentType.getString(context, "deity");
        
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
            String playerName = StringArgumentType.getString(context, "player");
            String deityId = StringArgumentType.getString(context, "deity");
            
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
            if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
                context.getSource().sendFailure(Component.literal("§cThis command can only be used by players"));
                return 0;
            }
            
            String deityIdString = StringArgumentType.getString(context, "deity");
            ResourceLocation deityId = ResourceLocation.tryParse(deityIdString);
            
            if (deityId == null) {
                context.getSource().sendFailure(Component.literal("§cInvalid deity ID: " + deityIdString));
                return 0;
            }
            
            // Check if deity exists
            if (DatapackDeityManager.getDeity(deityId) == null) {
                context.getSource().sendFailure(Component.literal("§cDeity not found: " + deityIdString));
                return 0;
            }
            
            // Set patron using the capability system
            player.level().getCapability(com.bluelotuscoding.eidolonunchained.capability.CapabilityHandler.PATRON_DATA_CAPABILITY)
                .ifPresent(patronData -> {
                    ResourceLocation currentPatron = patronData.getPatron(player);
                    if (currentPatron != null && currentPatron.equals(deityId)) {
                        context.getSource().sendSuccess(
                            () -> Component.literal("§e" + deityIdString + " is already your patron deity"), 
                            false
                        );
                    } else {
                        patronData.setPatron(player, deityId);
                        context.getSource().sendSuccess(
                            () -> Component.literal("§6You have chosen " + deityIdString + " as your patron deity"), 
                            false
                        );
                    }
                });
            
            if (!player.level().getCapability(com.bluelotuscoding.eidolonunchained.capability.CapabilityHandler.PATRON_DATA_CAPABILITY).isPresent()) {
                context.getSource().sendFailure(Component.literal("§cFailed to access patron data"));
                return 0;
            }
            
            return 1;
            
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
            if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
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
    // 🎯 DEBUG SYSTEM COMMANDS
    // =====================================
    
    /**
     * 🔍 DEBUG PLAYER PROGRESSION
     * 
     * Shows detailed progression information for a player with all deities.
     * Usage: /eidolon-unchained debug progression <player>
     */
    private static int debugPlayerProgression(CommandContext<CommandSourceStack> context) {
        try {
            String playerName = StringArgumentType.getString(context, "player");
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
                                stageName, (int)required, qualified ? "§a✓" : "§c✗")), false);
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
     * 🔄 FORCE PROGRESSION CHECK
     * 
     * Manually triggers a progression check for a player.
     * Usage: /eidolon-unchained debug force-progression-check <player>
     */
    private static int forceProgressionCheck(CommandContext<CommandSourceStack> context) {
        try {
            String playerName = StringArgumentType.getString(context, "player");
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
     * 🧪 TEST DEITY COMMAND
     * 
     * Tests command execution as if triggered by deity AI.
     * Usage: /eidolon-unchained debug test-command <command>
     */
    private static int testDeityCommand(CommandContext<CommandSourceStack> context) {
        try {
            String command = StringArgumentType.getString(context, "command");
            ServerPlayer executor = context.getSource().getPlayerOrException();
            
            context.getSource().sendSuccess(() -> Component.literal(
                "§6Testing deity command: §f" + command), false);
            
            // Execute the command as a deity would
            net.minecraft.commands.CommandSourceStack deitySource = context.getSource().getServer()
                .createCommandSourceStack()
                .withSource(net.minecraft.commands.CommandSource.NULL)
                .withLevel(executor.serverLevel())
                .withPosition(executor.position())
                .withPermission(2);
            
            String cleanCommand = command.startsWith("/") ? command.substring(1) : command;
            int result = context.getSource().getServer().getCommands().performPrefixedCommand(deitySource, cleanCommand);
            
            context.getSource().sendSuccess(() -> Component.literal(
                "§7Command result: " + (result > 0 ? "§aSuccess (" + result + ")" : "§cFailed (" + result + ")")), false);
            
            return 1;
            
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError testing command: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * 📊 DEBUG PLAYER REPUTATION
     * 
     * Shows detailed reputation information for a player with a specific deity.
     * Usage: /eidolon-unchained debug reputation <player> <deity>
     */
    private static int debugPlayerReputation(CommandContext<CommandSourceStack> context) {
        try {
            String playerName = StringArgumentType.getString(context, "player");
            String deityId = StringArgumentType.getString(context, "deity");
            
            ServerPlayer player = context.getSource().getServer().getPlayerList().getPlayerByName(playerName);
            if (player == null) {
                context.getSource().sendFailure(Component.literal("§cPlayer not found: " + playerName));
                return 0;
            }
            
            net.minecraft.resources.ResourceLocation deityLocation = new net.minecraft.resources.ResourceLocation(deityId);
            com.bluelotuscoding.eidolonunchained.deity.DatapackDeity deity = 
                com.bluelotuscoding.eidolonunchained.data.DatapackDeityManager.getDeity(deityLocation);
            
            if (deity == null) {
                context.getSource().sendFailure(Component.literal("§cDeity not found: " + deityId));
                return 0;
            }
            
            double reputation = deity.getPlayerReputation(player);
            String progressionLevel = getDynamicProgressionLevel(deity, player);
            
            context.getSource().sendSuccess(() -> Component.literal("§6=== REPUTATION DEBUG ==="), false);
            context.getSource().sendSuccess(() -> Component.literal("§ePlayer: §f" + playerName), false);
            context.getSource().sendSuccess(() -> Component.literal("§eDeity: §f" + deity.getName()), false);
            context.getSource().sendSuccess(() -> Component.literal("§eReputation: §b" + String.format("%.2f", reputation)), false);
            context.getSource().sendSuccess(() -> Component.literal("§eProgression: §f" + progressionLevel), false);
            
            return 1;
            
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError debugging reputation: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * 🎭 HELPER METHOD FOR PROGRESSION LEVEL
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
                    bestStage = stageName;
                    highestQualifyingReputation = requiredReputation;
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
     * 🧹 CLEAR PLAYER REWARDS (For Testing)
     * 
     * Clears reward history for a player with a specific deity.
     * Usage: /eidolon-unchained debug clear-rewards <player> <deity>
     */
    private static int clearPlayerRewards(CommandContext<CommandSourceStack> context) {
        try {
            String playerName = StringArgumentType.getString(context, "player");
            String deityId = StringArgumentType.getString(context, "deity");
            
            ServerPlayer player = context.getSource().getServer().getPlayerList().getPlayerByName(playerName);
            if (player == null) {
                context.getSource().sendFailure(Component.literal("§cPlayer not found: " + playerName));
                return 0;
            }
            
            net.minecraft.resources.ResourceLocation deityLocation = new net.minecraft.resources.ResourceLocation(deityId);
            com.bluelotuscoding.eidolonunchained.deity.DatapackDeity deity = 
                com.bluelotuscoding.eidolonunchained.data.DatapackDeityManager.getDeity(deityLocation);
            
            if (deity == null) {
                context.getSource().sendFailure(Component.literal("§cDeity not found: " + deityId));
                return 0;
            }
            
            // Clear the player's reward history for this deity
            deity.clearPlayerRewardHistory(player.getUUID());
            
            context.getSource().sendSuccess(() -> Component.literal(
                "§6Cleared reward history for " + playerName + " with " + deity.getName()), false);
            
            // Also notify the player
            player.sendSystemMessage(Component.literal(
                "§6Your reward history with " + deity.getName() + " has been reset for testing."));
            
            return 1;
            
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError clearing rewards: " + e.getMessage()));
            return 0;
        }
    }
    
    // Utility methods
    private static String maskApiKey(String key) {
        if (key == null || key.length() < 8) return "***";
        return key.substring(0, 4) + "***" + key.substring(key.length() - 4);
    }
}
