package com.bluelotuscoding.eidolonunchained.command;

import com.bluelotuscoding.eidolonunchained.config.AIProviderConfig;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

public class AIConfigCommands {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("eidolon-ai")
            .requires(source -> source.hasPermission(2)) // Requires OP level 2
            .then(Commands.literal("configure")
                .then(Commands.literal("enable")
                    .executes(AIConfigCommands::enableAI))
                .then(Commands.literal("disable")
                    .executes(AIConfigCommands::disableAI))
                .then(Commands.literal("set-provider")
                    .then(Commands.argument("provider", StringArgumentType.string())
                        .suggests((context, builder) -> {
                            builder.suggest("direct");
                            builder.suggest("proxy");
                            builder.suggest("hybrid");
                            return builder.buildFuture();
                        })
                        .executes(AIConfigCommands::setProvider)))
                .then(Commands.literal("set-api-key")
                    .then(Commands.argument("key", StringArgumentType.greedyString())
                        .executes(AIConfigCommands::setApiKey)))
                .then(Commands.literal("set-proxy-url")
                    .then(Commands.argument("url", StringArgumentType.greedyString())
                        .executes(AIConfigCommands::setProxyUrl)))
                .then(Commands.literal("status")
                    .executes(AIConfigCommands::showStatus))
                .then(Commands.literal("test")
                    .executes(AIConfigCommands::testConnection)))
            .then(Commands.literal("help")
                .executes(AIConfigCommands::showHelp)));
    }
    
    private static int enableAI(CommandContext<CommandSourceStack> context) {
        // Enable AI in config
        AIProviderConfig.INSTANCE.enableAI.set(true);
        context.getSource().sendSuccess(() -> 
            Component.literal("✅ AI deities enabled!").withStyle(ChatFormatting.GREEN), true);
        return 1;
    }
    
    private static int disableAI(CommandContext<CommandSourceStack> context) {
        AIProviderConfig.INSTANCE.enableAI.set(false);
        context.getSource().sendSuccess(() -> 
            Component.literal("❌ AI deities disabled!").withStyle(ChatFormatting.RED), true);
        return 1;
    }
    
    private static int setProvider(CommandContext<CommandSourceStack> context) {
        String provider = StringArgumentType.getString(context, "provider");
        AIProviderConfig.INSTANCE.providerType.set(provider);
        context.getSource().sendSuccess(() -> 
            Component.literal("🔧 AI provider set to: " + provider).withStyle(ChatFormatting.YELLOW), true);
        return 1;
    }
    
    private static int setApiKey(CommandContext<CommandSourceStack> context) {
        String key = StringArgumentType.getString(context, "key");
        // Store securely (you'd implement secure storage here)
        storeApiKeySecurely(key);
        context.getSource().sendSuccess(() -> 
            Component.literal("🔑 API key configured! (Key hidden for security)").withStyle(ChatFormatting.GREEN), true);
        return 1;
    }
    
    private static int setProxyUrl(CommandContext<CommandSourceStack> context) {
        String url = StringArgumentType.getString(context, "url");
        AIProviderConfig.INSTANCE.proxyServiceUrl.set(url);
        context.getSource().sendSuccess(() -> 
            Component.literal("🌐 Proxy URL set to: " + url).withStyle(ChatFormatting.YELLOW), true);
        return 1;
    }
    
    private static int showStatus(CommandContext<CommandSourceStack> context) {
        boolean enabled = AIProviderConfig.INSTANCE.enableAI.get();
        String provider = AIProviderConfig.INSTANCE.providerType.get();
        String endpoint = AIProviderConfig.INSTANCE.apiEndpoint.get();
        
        context.getSource().sendSuccess(() -> 
            Component.literal("🤖 AI Deity Status:\n")
                .append(Component.literal("Enabled: " + (enabled ? "✅" : "❌") + "\n"))
                .append(Component.literal("Provider: " + provider + "\n"))
                .append(Component.literal("Endpoint: " + endpoint))
                .withStyle(ChatFormatting.AQUA), false);
        return 1;
    }
    
    private static int testConnection(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(() -> 
            Component.literal("🧪 Testing AI connection...").withStyle(ChatFormatting.YELLOW), true);
        
        // TODO: Implement actual connection test
        // testAIConnection().thenAccept(success -> {
        //     if (success) {
        //         context.getSource().sendSuccess(() -> 
        //             Component.literal("✅ AI connection successful!").withStyle(ChatFormatting.GREEN), true);
        //     } else {
        //         context.getSource().sendFailure(
        //             Component.literal("❌ AI connection failed!").withStyle(ChatFormatting.RED));
        //     }
        // });
        
        return 1;
    }
    
    private static int showHelp(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(() -> 
            Component.literal("🔮 Eidolon AI Commands:\n")
                .append(Component.literal("/eidolon-ai configure enable - Enable AI deities\n"))
                .append(Component.literal("/eidolon-ai configure disable - Disable AI deities\n"))
                .append(Component.literal("/eidolon-ai configure set-provider <direct|proxy|hybrid>\n"))
                .append(Component.literal("/eidolon-ai configure set-api-key <key> - Set API key\n"))
                .append(Component.literal("/eidolon-ai configure set-proxy-url <url> - Set proxy service\n"))
                .append(Component.literal("/eidolon-ai configure status - Show current config\n"))
                .append(Component.literal("/eidolon-ai configure test - Test AI connection"))
                .withStyle(ChatFormatting.GOLD), false);
        return 1;
    }
    
    private static void storeApiKeySecurely(String key) {
        // TODO: Implement secure storage
        // This could store in:
        // 1. Server config file (encrypted)
        // 2. Environment variable
        // 3. Secure key storage system
        System.setProperty("eidolon.ai.key", key);
    }
}
