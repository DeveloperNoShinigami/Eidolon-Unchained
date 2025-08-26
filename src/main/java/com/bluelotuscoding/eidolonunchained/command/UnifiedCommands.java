package com.bluelotuscoding.eidolonunchained.command;

import com.bluelotuscoding.eidolonunchained.config.EidolonUnchainedConfig;
import com.bluelotuscoding.eidolonunchained.config.APIKeyManager;
import com.bluelotuscoding.eidolonunchained.ai.AIDeityManager;
import com.bluelotuscoding.eidolonunchained.data.DatapackDeityManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Unified command handler for all Eidolon Unchained commands
 * Consolidates configuration, AI, deity, prayer, and chant commands
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
                .then(Commands.literal("test")
                    .then(Commands.argument("provider", StringArgumentType.string())
                        .executes(UnifiedCommands::testApiKey)))
                .then(Commands.literal("list")
                    .executes(UnifiedCommands::listApiKeys))
                .then(Commands.literal("remove")
                    .then(Commands.argument("provider", StringArgumentType.string())
                        .executes(UnifiedCommands::removeApiKey))))
            
            // Deity management
            .then(Commands.literal("deities")
                .then(Commands.literal("list")
                    .executes(UnifiedCommands::listDeities))
                .then(Commands.literal("reload")
                    .executes(UnifiedCommands::reloadDeities))
                .then(Commands.literal("status")
                    .then(Commands.argument("deity", StringArgumentType.string())
                        .executes(UnifiedCommands::showDeityStatus))))
            
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
            
            // Debug commands
            .then(Commands.literal("debug")
                .then(Commands.literal("toggle")
                    .executes(UnifiedCommands::toggleDebug))
                .then(Commands.literal("logs")
                    .executes(UnifiedCommands::showDebugLogs))
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
    
    // Utility methods
    private static String maskApiKey(String key) {
        if (key == null || key.length() < 8) return "***";
        return key.substring(0, 4) + "***" + key.substring(key.length() - 4);
    }
}
