package com.bluelotuscoding.eidolonunchained.command;

import com.bluelotuscoding.eidolonunchained.ai.UniversalAIContext;
import com.bluelotuscoding.eidolonunchained.ai.AIDeityManager;
import com.bluelotuscoding.eidolonunchained.ai.AIDeityConfig;
import com.bluelotuscoding.eidolonunchained.ai.UniversalAIProvider;
import com.bluelotuscoding.eidolonunchained.chat.DeityChat;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;

/**
 * Debug commands to test AI context and provider functionality
 */
public class AIDebugCommand {
    private static final Logger LOGGER = LogManager.getLogger();

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("eidolon-unchained")
                .then(Commands.literal("debug")
                    .requires(source -> source.hasPermission(2)) // Require operator permissions
                    .then(Commands.literal("ai-context")
                        .then(Commands.argument("player", EntityArgument.player())
                            .then(Commands.argument("deity", StringArgumentType.string())
                                .executes(ctx -> showAIContext(ctx))
                            )
                        )
                    )
                    .then(Commands.literal("test-ai")
                        .then(Commands.argument("player", EntityArgument.player())
                            .then(Commands.argument("deity", StringArgumentType.string())
                                .then(Commands.argument("prompt", StringArgumentType.greedyString())
                                    .executes(ctx -> testAIResponse(ctx))
                                )
                            )
                        )
                    )
                    .then(Commands.literal("registry-stats")
                        .executes(ctx -> showRegistryStats(ctx))
                    )
                )
        );
    }

    /**
     * Show the full AI context that would be provided to the AI for a player and deity
     */
    private static int showAIContext(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer targetPlayer = EntityArgument.getPlayer(ctx, "player");
            String deityIdString = StringArgumentType.getString(ctx, "deity");
            ResourceLocation deityId = new ResourceLocation(deityIdString);
            
            // Get AI config for the deity
            AIDeityConfig aiConfig = AIDeityManager.getInstance().getAIConfig(deityId);
            if (aiConfig == null) {
                ctx.getSource().sendFailure(Component.literal("§cNo AI config found for deity: " + deityId));
                return 0;
            }
            
            // Build the full context
            String fullContext = UniversalAIContext.buildFullContext(targetPlayer, aiConfig);
            
            // Send context to command sender (split into multiple messages due to length)
            ctx.getSource().sendSuccess(() -> Component.literal("§a=== AI Context for " + targetPlayer.getName().getString() + " → " + deityId + " ==="), false);
            
            // Split context into chunks to avoid chat message length limits
            String[] lines = fullContext.split("\\n");
            StringBuilder currentChunk = new StringBuilder();
            
            for (String line : lines) {
                if (currentChunk.length() + line.length() + 1 > 500) {
                    // Send current chunk
                    String chunk = currentChunk.toString();
                    ctx.getSource().sendSuccess(() -> Component.literal("§7" + chunk), false);
                    currentChunk = new StringBuilder();
                }
                currentChunk.append(line).append("\\n");
            }
            
            // Send remaining chunk
            if (currentChunk.length() > 0) {
                String chunk = currentChunk.toString();
                ctx.getSource().sendSuccess(() -> Component.literal("§7" + chunk), false);
            }
            
            ctx.getSource().sendSuccess(() -> Component.literal("§a=== End AI Context ==="), false);
            return 1;
            
        } catch (Exception e) {
            LOGGER.error("Error showing AI context", e);
            ctx.getSource().sendFailure(Component.literal("§cError: " + e.getMessage()));
            return 0;
        }
    }

    /**
     * Test an AI response with a specific prompt and deity
     */
    private static int testAIResponse(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer targetPlayer = EntityArgument.getPlayer(ctx, "player");
            String deityIdString = StringArgumentType.getString(ctx, "deity");
            String prompt = StringArgumentType.getString(ctx, "prompt");
            ResourceLocation deityId = new ResourceLocation(deityIdString);
            
            // Get AI config for the deity
            AIDeityConfig aiConfig = AIDeityManager.getInstance().getAIConfig(deityId);
            if (aiConfig == null) {
                ctx.getSource().sendFailure(Component.literal("§cNo AI config found for deity: " + deityId));
                return 0;
            }
            
            // Test AI provider directly
            String testMessage = "Hello, this is a test message from Eidolon Unchained.";
            switch (aiConfig.ai_provider.toLowerCase()) {
                case "gemini": {
                    var geminiClient = new com.bluelotuscoding.eidolonunchained.integration.gemini.GeminiAPIClient(
                        com.bluelotuscoding.eidolonunchained.config.APIKeyManager.getAPIKey("gemini"),
                        aiConfig.model, 30);
                    
                    geminiClient.generateResponse(testMessage, "You are a helpful test deity.", 
                        null, null)
                        .thenAccept(response -> {
                            ctx.getSource().sendSuccess(() -> Component.literal("§a✓ Gemini API test successful!"), false);
                            ctx.getSource().sendSuccess(() -> Component.literal("§7Response: " + response.dialogue), false);
                        })
                        .exceptionally(error -> {
                            ctx.getSource().sendFailure(Component.literal("§c✗ Gemini API test failed: " + error.getMessage()));
                            return null;
                        });
                    break;
                }
                case "openrouter": {
                    var openRouterClient = new com.bluelotuscoding.eidolonunchained.integration.openrouter.OpenRouterClient(
                        com.bluelotuscoding.eidolonunchained.config.APIKeyManager.getAPIKey("openrouter"),
                        aiConfig.model, 30);
                    // Add similar test for OpenRouter if it has a generateResponse method
                    ctx.getSource().sendSuccess(() -> Component.literal("§e⚠ OpenRouter testing not implemented yet"), false);
                    break;
                }
                case "player2ai": {
                    var player2aiClient = new com.bluelotuscoding.eidolonunchained.integration.player2ai.Player2AIClient(30);
                    // Add similar test for Player2AI if it has a generateResponse method
                    ctx.getSource().sendSuccess(() -> Component.literal("§e⚠ Player2AI testing not implemented yet"), false);
                    break;
                }
                default:
                    ctx.getSource().sendFailure(Component.literal("§cUnsupported AI provider: " + aiConfig.ai_provider));
                    return 0;
            }
            
            return 1;
            
        } catch (Exception e) {
            LOGGER.error("Error testing AI response", e);
            ctx.getSource().sendFailure(Component.literal("§cError: " + e.getMessage()));
            return 0;
        }
    }

    /**
     * Show registry statistics to verify what data is available to AI
     */
    private static int showRegistryStats(CommandContext<CommandSourceStack> ctx) {
        try {
            int totalItems = ForgeRegistries.ITEMS.getValues().size();
            int totalBlocks = ForgeRegistries.BLOCKS.getValues().size();
            int totalEffects = ForgeRegistries.MOB_EFFECTS.getValues().size();
            int totalEntities = ForgeRegistries.ENTITY_TYPES.getValues().size();
            int totalBiomes = ForgeRegistries.BIOMES.getValues().size();
            
            // Count items by mod
            var itemsByMod = ForgeRegistries.ITEMS.getValues().stream()
                .collect(java.util.stream.Collectors.groupingBy(
                    item -> ForgeRegistries.ITEMS.getKey(item).getNamespace(),
                    java.util.stream.Collectors.counting()
                ));
            
            ctx.getSource().sendSuccess(() -> Component.literal("§a=== Registry Statistics ==="), false);
            ctx.getSource().sendSuccess(() -> Component.literal("§7Total Items: " + totalItems), false);
            ctx.getSource().sendSuccess(() -> Component.literal("§7Total Blocks: " + totalBlocks), false);
            ctx.getSource().sendSuccess(() -> Component.literal("§7Total Effects: " + totalEffects), false);
            ctx.getSource().sendSuccess(() -> Component.literal("§7Total Entities: " + totalEntities), false);
            ctx.getSource().sendSuccess(() -> Component.literal("§7Total Biomes: " + totalBiomes), false);
            
            ctx.getSource().sendSuccess(() -> Component.literal("§a=== Top Mods by Item Count ==="), false);
            itemsByMod.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(10)
                .forEach(entry -> {
                    ctx.getSource().sendSuccess(() -> Component.literal("§7" + entry.getKey() + ": " + entry.getValue() + " items"), false);
                });
            
            return 1;
            
        } catch (Exception e) {
            LOGGER.error("Error showing registry stats", e);
            ctx.getSource().sendFailure(Component.literal("§cError: " + e.getMessage()));
            return 0;
        }
    }
}
