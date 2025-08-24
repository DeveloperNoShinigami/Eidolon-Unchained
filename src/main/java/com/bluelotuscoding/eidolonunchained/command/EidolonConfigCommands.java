package com.bluelotuscoding.eidolonunchained.command;

import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import com.bluelotuscoding.eidolonunchained.config.APIKeyManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Commands for configuring AI deity system
 */
@Mod.EventBusSubscriber(modid = EidolonUnchained.MODID)
public class EidolonConfigCommands {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        
        dispatcher.register(Commands.literal("eidolon-config")
            .requires(source -> source.hasPermission(4)) // OP level 4 required
            .then(Commands.literal("quick-setup")
                .then(Commands.argument("provider", StringArgumentType.string())
                    .then(Commands.argument("api-key", StringArgumentType.string())
                        .executes(EidolonConfigCommands::quickSetup))))
            .then(Commands.literal("set")
                .then(Commands.argument("key", StringArgumentType.string())
                    .then(Commands.argument("value", StringArgumentType.string())
                        .executes(EidolonConfigCommands::setConfig))))
            .then(Commands.literal("get")
                .then(Commands.argument("key", StringArgumentType.string())
                    .executes(EidolonConfigCommands::getConfig)))
            .then(Commands.literal("list")
                .executes(EidolonConfigCommands::listConfig))
            .then(Commands.literal("test")
                .then(Commands.argument("provider", StringArgumentType.string())
                    .executes(EidolonConfigCommands::testProvider)))
            .then(Commands.literal("remove")
                .then(Commands.argument("key", StringArgumentType.string())
                    .executes(EidolonConfigCommands::removeConfig)))
            .then(Commands.literal("reload")
                .executes(EidolonConfigCommands::reloadConfig))
            .then(Commands.literal("status")
                .executes(EidolonConfigCommands::showStatus))
            .then(Commands.literal("validate-all")
                .executes(EidolonConfigCommands::validateAll))
        );
    }

    private static int quickSetup(CommandContext<CommandSourceStack> context) {
        String provider = StringArgumentType.getString(context, "provider");
        String apiKey = StringArgumentType.getString(context, "api-key");
        
        try {
            // Validate provider
            if (!provider.equals("gemini")) {
                context.getSource().sendFailure(Component.literal("§cUnsupported provider: " + provider + ". Currently only 'gemini' is supported."));
                return 0;
            }
            
            // Set API key
            boolean success = APIKeyManager.setAPIKey(provider, apiKey);
            if (success) {
                context.getSource().sendSuccess(() -> Component.literal("§a✓ API key configured for " + provider), false);
                
                // Test the configuration
                boolean testResult = APIKeyManager.testConnection(provider);
                if (testResult) {
                    context.getSource().sendSuccess(() -> Component.literal("§a✓ Connection test successful"), false);
                } else {
                    context.getSource().sendFailure(Component.literal("§c⚠ API key set but connection test failed. Please verify your key."));
                }
                return 1;
            } else {
                context.getSource().sendFailure(Component.literal("§cFailed to set API key"));
                return 0;
            }
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError: " + e.getMessage()));
            return 0;
        }
    }

    private static int setConfig(CommandContext<CommandSourceStack> context) {
        String key = StringArgumentType.getString(context, "key");
        String value = StringArgumentType.getString(context, "value");
        
        try {
            boolean success = APIKeyManager.setConfigValue(key, value);
            if (success) {
                context.getSource().sendSuccess(() -> Component.literal("§aSet " + key + " = " + maskValue(key, value)), false);
                return 1;
            } else {
                context.getSource().sendFailure(Component.literal("§cFailed to set configuration"));
                return 0;
            }
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError: " + e.getMessage()));
            return 0;
        }
    }

    private static int getConfig(CommandContext<CommandSourceStack> context) {
        String key = StringArgumentType.getString(context, "key");
        
        try {
            String value = APIKeyManager.getConfigValue(key);
            if (value != null) {
                context.getSource().sendSuccess(() -> Component.literal("§e" + key + " = " + maskValue(key, value)), false);
                return 1;
            } else {
                context.getSource().sendFailure(Component.literal("§cConfiguration key not found: " + key));
                return 0;
            }
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError: " + e.getMessage()));
            return 0;
        }
    }

    private static int listConfig(CommandContext<CommandSourceStack> context) {
        try {
            var configs = APIKeyManager.getAllConfigs();
            if (configs.isEmpty()) {
                context.getSource().sendSuccess(() -> Component.literal("§eNo configurations found"), false);
            } else {
                context.getSource().sendSuccess(() -> Component.literal("§eCurrent configurations:"), false);
                configs.forEach((key, value) -> {
                    context.getSource().sendSuccess(() -> Component.literal("§7  " + key + " = " + maskValue(key, value)), false);
                });
            }
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError: " + e.getMessage()));
            return 0;
        }
    }

    private static int testProvider(CommandContext<CommandSourceStack> context) {
        String provider = StringArgumentType.getString(context, "provider");
        
        try {
            boolean success = APIKeyManager.testConnection(provider);
            if (success) {
                context.getSource().sendSuccess(() -> Component.literal("§a✓ " + provider + " API connection successful"), false);
                return 1;
            } else {
                context.getSource().sendFailure(Component.literal("§c✗ " + provider + " API connection failed"));
                return 0;
            }
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError testing " + provider + ": " + e.getMessage()));
            return 0;
        }
    }

    private static int removeConfig(CommandContext<CommandSourceStack> context) {
        String key = StringArgumentType.getString(context, "key");
        
        try {
            boolean success = APIKeyManager.removeConfig(key);
            if (success) {
                context.getSource().sendSuccess(() -> Component.literal("§aRemoved configuration: " + key), false);
                return 1;
            } else {
                context.getSource().sendFailure(Component.literal("§cConfiguration not found: " + key));
                return 0;
            }
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError: " + e.getMessage()));
            return 0;
        }
    }

    private static int reloadConfig(CommandContext<CommandSourceStack> context) {
        try {
            APIKeyManager.reload();
            context.getSource().sendSuccess(() -> Component.literal("§aConfiguration reloaded"), false);
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError reloading: " + e.getMessage()));
            return 0;
        }
    }

    private static int showStatus(CommandContext<CommandSourceStack> context) {
        try {
            var status = APIKeyManager.getSystemStatus();
            context.getSource().sendSuccess(() -> Component.literal("§eEidolon Unchained AI System Status:"), false);
            
            status.forEach((key, value) -> {
                String color = value.toString().contains("✓") ? "§a" : 
                              value.toString().contains("✗") ? "§c" : "§e";
                context.getSource().sendSuccess(() -> Component.literal(color + "  " + key + ": " + value), false);
            });
            
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError getting status: " + e.getMessage()));
            return 0;
        }
    }

    private static int validateAll(CommandContext<CommandSourceStack> context) {
        try {
            var results = APIKeyManager.validateAllConfigurations();
            context.getSource().sendSuccess(() -> Component.literal("§eValidation Results:"), false);
            
            final AtomicInteger passed = new AtomicInteger(0);
            final AtomicInteger failed = new AtomicInteger(0);
            
            for (var entry : results.entrySet()) {
                boolean isValid = (Boolean) entry.getValue();
                String color = isValid ? "§a" : "§c";
                String symbol = isValid ? "✓" : "✗";
                context.getSource().sendSuccess(() -> Component.literal(color + "  " + symbol + " " + entry.getKey()), false);
                
                if (isValid) passed.incrementAndGet();
                else failed.incrementAndGet();
            }
            
            context.getSource().sendSuccess(() -> Component.literal(
                "§eSummary: §a" + passed.get() + " passed§r, §c" + failed.get() + " failed"), false);
            
            return passed.get() > 0 ? 1 : 0;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError during validation: " + e.getMessage()));
            return 0;
        }
    }

    /**
     * Mask sensitive values like API keys for display
     */
    private static String maskValue(String key, String value) {
        if (key.toLowerCase().contains("key") || key.toLowerCase().contains("token")) {
            if (value.length() <= 4) {
                return "****";
            }
            return value.substring(0, 4) + "****" + value.substring(value.length() - 4);
        }
        return value;
    }
}
