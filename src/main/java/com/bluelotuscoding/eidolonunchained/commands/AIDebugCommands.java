package com.bluelotuscoding.eidolonunchained.commands;

import com.bluelotuscoding.eidolonunchained.integration.ai.RegistryContextProvider;
import com.bluelotuscoding.eidolonunchained.ai.AIDeityManager;
import com.bluelotuscoding.eidolonunchained.ai.AIDeityConfig;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Arrays;
import java.util.List;

/**
 * Debug commands to test AI knowledge of the world
 * These commands verify that the AI actually knows what exists in the world
 */
public class AIDebugCommands {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("eidolon-unchained")
            .then(Commands.literal("ai-debug")
                .then(Commands.literal("test-world-knowledge")
                    .requires(source -> source.hasPermission(2))
                    .executes(AIDebugCommands::testWorldKnowledge))
                .then(Commands.literal("test-item-search")
                    .requires(source -> source.hasPermission(2))
                    .then(Commands.argument("item", StringArgumentType.greedyString())
                        .executes(AIDebugCommands::testItemSearch)))
                .then(Commands.literal("test-deity-context")
                    .requires(source -> source.hasPermission(2))
                    .then(Commands.argument("deity", StringArgumentType.word())
                        .executes(AIDebugCommands::testDeityContext)))
                .then(Commands.literal("registry-report")
                    .requires(source -> source.hasPermission(2))
                    .executes(AIDebugCommands::registryReport))));
    }
    
    /**
     * Test if AI has access to world knowledge through registry context
     */
    private static int testWorldKnowledge(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        try {
            source.sendSuccess(() -> Component.literal("§6=== AI WORLD KNOWLEDGE TEST ==="), false);
            
            // Test basic registry access
            List<String> testMods = Arrays.asList("minecraft", "eidolon", "eidolonunchained");
            String worldContext = RegistryContextProvider.generateContextForMods(testMods);
            
            source.sendSuccess(() -> Component.literal("§a✅ Registry access working"), false);
            source.sendSuccess(() -> Component.literal("§7Context length: " + worldContext.length() + " characters"), false);
            
            // Test item search
            List<ResourceLocation> diamondMatches = RegistryContextProvider.findMatchingItems("diamond", testMods);
            source.sendSuccess(() -> Component.literal("§7'diamond' search results: " + diamondMatches.size()), false);
            
            List<ResourceLocation> soulMatches = RegistryContextProvider.findMatchingItems("soul", testMods);
            source.sendSuccess(() -> Component.literal("§7'soul' search results: " + soulMatches.size()), false);
            
            // Test if AI would get this context
            if (worldContext.contains("diamond") && worldContext.contains("item")) {
                source.sendSuccess(() -> Component.literal("§a✅ AI would receive item context"), false);
            } else {
                source.sendSuccess(() -> Component.literal("§c❌ AI context missing item data"), false);
            }
            
            return 1;
            
        } catch (Exception e) {
            source.sendFailure(Component.literal("§c❌ World knowledge test failed: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Test searching for a specific item across registries
     */
    private static int testItemSearch(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String itemSearch = StringArgumentType.getString(context, "item");
        
        try {
            source.sendSuccess(() -> Component.literal("§6=== ITEM SEARCH TEST: '" + itemSearch + "' ==="), false);
            
            List<String> testMods = Arrays.asList("minecraft", "eidolon", "eidolonunchained");
            List<ResourceLocation> matches = RegistryContextProvider.findMatchingItems(itemSearch, testMods);
            
            if (matches.isEmpty()) {
                source.sendSuccess(() -> Component.literal("§c❌ No matches found for '" + itemSearch + "'"), false);
                source.sendSuccess(() -> Component.literal("§7This means AI would not be able to give this item"), false);
            } else {
                source.sendSuccess(() -> Component.literal("§a✅ Found " + matches.size() + " matches:"), false);
                for (int i = 0; i < Math.min(10, matches.size()); i++) {
                    ResourceLocation match = matches.get(i);
                    final int index = i + 1; // Make effectively final for lambda
                    source.sendSuccess(() -> Component.literal("§7  " + index + ". " + match.toString()), false);
                }
                if (matches.size() > 10) {
                    source.sendSuccess(() -> Component.literal("§7  ... and " + (matches.size() - 10) + " more"), false);
                }
                
                ResourceLocation bestMatch = matches.get(0);
                source.sendSuccess(() -> Component.literal("§a✅ AI would give: " + bestMatch.toString()), false);
            }
            
            return 1;
            
        } catch (Exception e) {
            source.sendFailure(Component.literal("§c❌ Item search test failed: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Test what context a specific deity would receive
     */
    private static int testDeityContext(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String deityName = StringArgumentType.getString(context, "deity");
        
        try {
            source.sendSuccess(() -> Component.literal("§6=== DEITY CONTEXT TEST: '" + deityName + "' ==="), false);
            
            ResourceLocation deityId = new ResourceLocation("eidolonunchained", deityName + "_deity");
            AIDeityConfig config = AIDeityManager.getInstance().getAIConfig(deityId);
            
            if (config == null) {
                source.sendFailure(Component.literal("§c❌ Deity '" + deityName + "' not found"));
                return 0;
            }
            
            // Get the mod context IDs from config
            List<String> modContextIds = config.mod_context_ids;
            if (modContextIds == null || modContextIds.isEmpty()) {
                source.sendSuccess(() -> Component.literal("§c❌ No mod_context_ids configured for this deity"), false);
                return 0;
            }
            
            source.sendSuccess(() -> Component.literal("§a✅ Deity mod context: " + modContextIds), false);
            
            // Generate the actual context the AI would receive
            String aiContext = RegistryContextProvider.generateContextForMods(modContextIds);
            
            source.sendSuccess(() -> Component.literal("§7Context length: " + aiContext.length() + " characters"), false);
            source.sendSuccess(() -> Component.literal("§7Sample context:"), false);
            
            // Show first 200 characters of context
            String sample = aiContext.length() > 200 ? aiContext.substring(0, 200) + "..." : aiContext;
            source.sendSuccess(() -> Component.literal("§7" + sample), false);
            
            return 1;
            
        } catch (Exception e) {
            source.sendFailure(Component.literal("§c❌ Deity context test failed: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Generate a comprehensive registry report
     */
    private static int registryReport(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        try {
            source.sendSuccess(() -> Component.literal("§6=== MINECRAFT REGISTRY REPORT ==="), false);
            
            String report = RegistryContextProvider.generateDebugReport();
            
            // Split report into manageable chunks for chat
            String[] lines = report.split("\\n");
            for (String line : lines) {
                if (line.trim().isEmpty()) continue;
                source.sendSuccess(() -> Component.literal("§7" + line), false);
            }
            
            return 1;
            
        } catch (Exception e) {
            source.sendFailure(Component.literal("§c❌ Registry report failed: " + e.getMessage()));
            return 0;
        }
    }
}
