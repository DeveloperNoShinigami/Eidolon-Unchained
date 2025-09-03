package com.bluelotuscoding.eidolonunchained.ai;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Universal AI Context Builder - provides comprehensive world/registry data to ANY AI provider
 * This replaces provider-specific context building and gives complete game knowledge to AI
 */
public class UniversalAIContext {
    private static final Logger LOGGER = LogManager.getLogger();
    
    /**
     * Build complete AI context with full registry access and configurable priority mods
     */
    public static String buildFullContext(ServerPlayer player, AIDeityConfig config) {
        StringBuilder context = new StringBuilder();
        
        // Player status (essential)
        context.append(buildPlayerStatus(player));
        
        // World/environment data
        context.append(buildWorldContext(player));
        
        // COMPLETE registry access with priority mods
        context.append(buildCompleteRegistryContext(config));
        
        // AI behavior guidelines
        context.append(buildAIBehaviorGuidelines());
        
        return context.toString();
    }
    
    /**
     * Build comprehensive registry context - AI gets access to EVERYTHING
     */
    private static String buildCompleteRegistryContext(AIDeityConfig config) {
        StringBuilder registryContext = new StringBuilder();
        registryContext.append("\n=== COMPLETE GAME REGISTRY ACCESS ===\n");
        
        try {
            // Get priority mods from config - use default set if config doesn't specify
            Set<String> priorityMods = new HashSet<>();
            
            // Add default priority mods
            priorityMods.addAll(Arrays.asList("minecraft", "eidolon", "eidolonunchained"));
            
            // ITEMS - Complete access to all loaded items
            registryContext.append("ITEMS: ");
            Map<String, List<String>> itemsByMod = new HashMap<>();
            
            ForgeRegistries.ITEMS.forEach(item -> {
                ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(item);
                String modId = itemId.getNamespace();
                itemsByMod.computeIfAbsent(modId, k -> new ArrayList<>()).add(itemId.toString());
            });
            
            // Priority mods first
            for (String priorityMod : priorityMods) {
                if (itemsByMod.containsKey(priorityMod)) {
                    registryContext.append(String.format("%s(%d items), ", 
                        priorityMod.toUpperCase(), itemsByMod.get(priorityMod).size()));
                }
            }
            
            // All other mods
            int totalOtherItems = 0;
            Set<String> otherMods = new HashSet<>();
            for (Map.Entry<String, List<String>> entry : itemsByMod.entrySet()) {
                if (!priorityMods.contains(entry.getKey())) {
                    totalOtherItems += entry.getValue().size();
                    otherMods.add(entry.getKey());
                }
            }
            
            if (!otherMods.isEmpty()) {
                registryContext.append(String.format("OTHER_MODS(%d items from %s). ", 
                    totalOtherItems, String.join(",", otherMods)));
            }
            
            // BLOCKS - Complete access
            registryContext.append("BLOCKS: ");
            Map<String, Integer> blocksByMod = new HashMap<>();
            ForgeRegistries.BLOCKS.forEach(block -> {
                String modId = ForgeRegistries.BLOCKS.getKey(block).getNamespace();
                blocksByMod.put(modId, blocksByMod.getOrDefault(modId, 0) + 1);
            });
            
            for (String priorityMod : priorityMods) {
                if (blocksByMod.containsKey(priorityMod)) {
                    registryContext.append(String.format("%s(%d), ", priorityMod.toUpperCase(), blocksByMod.get(priorityMod)));
                }
            }
            registryContext.append("+ ").append(blocksByMod.size() - priorityMods.size()).append(" other mods. ");
            
            // EFFECTS - Complete access
            registryContext.append("EFFECTS: ");
            List<String> allEffects = new ArrayList<>();
            ForgeRegistries.MOB_EFFECTS.forEach(effect -> {
                allEffects.add(ForgeRegistries.MOB_EFFECTS.getKey(effect).toString());
            });
            registryContext.append(allEffects.size()).append(" total effects available. ");
            
            // ENTITIES - Complete access
            registryContext.append("ENTITIES: ");
            Map<String, Integer> entitiesByMod = new HashMap<>();
            ForgeRegistries.ENTITY_TYPES.forEach(entityType -> {
                String modId = ForgeRegistries.ENTITY_TYPES.getKey(entityType).getNamespace();
                entitiesByMod.put(modId, entitiesByMod.getOrDefault(modId, 0) + 1);
            });
            registryContext.append(entitiesByMod.values().stream().mapToInt(Integer::intValue).sum())
                          .append(" entity types loaded. ");
            
            // CRITICAL: AI Command Format Reference
            registryContext.append("\nCOMMAND FORMAT: Use exact registry IDs - /give {player} modid:item_name count, ");
            registryContext.append("/effect give {player} modid:effect_name duration amplifier\n");
            
            // CRITICAL: AI can access ANY item/effect/entity from ANY loaded mod
            registryContext.append("AI CAPABILITY: You have COMPLETE access to all loaded game content. ");
            registryContext.append("Use any valid registry ID from any mod. Check modid:item_name format.\n");
            
        } catch (Exception e) {
            LOGGER.error("Error building complete registry context: {}", e.getMessage());
            registryContext.append("Registry access temporarily limited - use common items\n");
        }
        
        return registryContext.toString();
    }
    
    /**
     * Build player status analysis
     */
    private static String buildPlayerStatus(ServerPlayer player) {
        StringBuilder status = new StringBuilder();
        status.append("=== PLAYER STATUS ===\n");
        
        // Basic info
        status.append("Name: ").append(player.getName().getString()).append("\n");
        
        // Health analysis
        float health = player.getHealth();
        float maxHealth = player.getMaxHealth();
        float healthPercentage = (health / maxHealth) * 100;
        status.append("Health: ").append(String.format("%.1f/%.1f", health, maxHealth));
        
        if (healthPercentage <= 25) {
            status.append(" (CRITICAL - immediate healing needed)\n");
        } else if (healthPercentage <= 50) {
            status.append(" (INJURED - healing beneficial)\n");
        } else if (healthPercentage <= 75) {
            status.append(" (WOUNDED - minor healing helpful)\n");
        } else {
            status.append(" (HEALTHY)\n");
        }
        
        // Hunger analysis
        int foodLevel = player.getFoodData().getFoodLevel();
        status.append("Hunger: ").append(foodLevel).append("/20");
        if (foodLevel <= 6) {
            status.append(" (STARVING - urgent food needed)\n");
        } else if (foodLevel <= 12) {
            status.append(" (HUNGRY - food beneficial)\n");
        } else {
            status.append(" (WELL FED)\n");
        }
        
        // Experience
        status.append("XP Level: ").append(player.experienceLevel).append("\n");
        
        // Active effects
        if (!player.getActiveEffects().isEmpty()) {
            status.append("Active Effects: ");
            player.getActiveEffects().forEach(effect -> {
                status.append(ForgeRegistries.MOB_EFFECTS.getKey(effect.getEffect()))
                      .append("(").append(effect.getDuration()).append("t), ");
            });
            status.append("\n");
        }
        
        // Equipment summary
        int armorCount = 0;
        for (var armor : player.getArmorSlots()) {
            if (!armor.isEmpty()) armorCount++;
        }
        status.append("Armor: ").append(armorCount).append("/4 pieces equipped\n");
        
        if (!player.getMainHandItem().isEmpty()) {
            status.append("Main Hand: ").append(player.getMainHandItem().getDisplayName().getString()).append("\n");
        }
        
        return status.toString();
    }
    
    /**
     * Build world/environment context
     */
    private static String buildWorldContext(ServerPlayer player) {
        StringBuilder world = new StringBuilder();
        world.append("\n=== WORLD CONTEXT ===\n");
        
        BlockPos pos = player.blockPosition();
        world.append("Location: ").append(pos.getX()).append(", ").append(pos.getY()).append(", ").append(pos.getZ()).append("\n");
        
        // Biome
        String biomeId = player.level().getBiome(pos).unwrapKey()
            .map(key -> key.location().toString())
            .orElse("unknown");
        world.append("Biome: ").append(biomeId).append("\n");
        
        // Time and weather
        long time = player.level().getDayTime() % 24000;
        String timeOfDay = getTimeOfDay(time);
        world.append("Time: ").append(timeOfDay).append("\n");
        
        boolean isRaining = player.level().isRaining();
        boolean isThundering = player.level().isThundering();
        String weather = isThundering ? "thunderstorm" : (isRaining ? "rain" : "clear");
        world.append("Weather: ").append(weather).append("\n");
        
        // Dimension
        String dimension = player.level().dimension().location().toString();
        world.append("Dimension: ").append(dimension).append("\n");
        
        // Environmental dangers
        List<String> dangers = new ArrayList<>();
        if (player.isInLava()) dangers.add("IN_LAVA");
        if (player.isOnFire()) dangers.add("ON_FIRE");
        if (player.isUnderWater() && player.getAirSupply() < player.getMaxAirSupply()) dangers.add("DROWNING");
        if (pos.getY() < 10) dangers.add("DEEP_UNDERGROUND");
        if (pos.getY() > 200) dangers.add("HIGH_ALTITUDE");
        
        if (!dangers.isEmpty()) {
            world.append("Immediate Dangers: ").append(String.join(", ", dangers)).append("\n");
        }
        
        return world.toString();
    }
    
    /**
     * Build AI behavior guidelines - universal for all providers
     */
    private static String buildAIBehaviorGuidelines() {
        StringBuilder guidelines = new StringBuilder();
        guidelines.append("\n=== AI BEHAVIOR GUIDELINES ===\n");
        
        guidelines.append("RESPONSE LENGTH: Keep responses 1-3 sentences maximum for immersion.\n");
        guidelines.append("COMMAND USAGE: Only use commands when specifically helpful. Respect cooldowns.\n");
        guidelines.append("COMMAND HIDING: Never show raw commands to players. Execute silently with immersive messages.\n");
        guidelines.append("ITEM KNOWLEDGE: You have complete access to all loaded mods. Use appropriate items.\n");
        guidelines.append("ROLEPLAY: Stay in character as a deity. Be mystical but helpful.\n");
        guidelines.append("EMERGENCY PRIORITY: Address critical health/hunger situations immediately.\n");
        
        return guidelines.toString();
    }
    
    /**
     * Get time of day string
     */
    private static String getTimeOfDay(long time) {
        if (time < 1000) return "dawn";
        else if (time < 6000) return "morning";
        else if (time < 12000) return "day";
        else if (time < 13000) return "dusk";
        else if (time < 18000) return "night";
        else if (time < 22000) return "late night";
        else return "midnight";
    }
    
    /**
     * Build context for specific AI provider compatibility
     */
    public static String buildProviderSpecificContext(ServerPlayer player, AIDeityConfig config, String providerType) {
        String baseContext = buildFullContext(player, config);
        
        // Add provider-specific formatting if needed
        switch (providerType.toLowerCase()) {
            case "gemini":
                return "=== GEMINI AI CONTEXT ===\n" + baseContext + "\nEnd of context. Respond as deity.\n";
            case "openai":
                return "=== OPENAI CONTEXT ===\n" + baseContext + "\nEnd of context. Respond as deity.\n";
            case "anthropic":
                return "=== ANTHROPIC CONTEXT ===\n" + baseContext + "\nEnd of context. Respond as deity.\n";
            default:
                return baseContext;
        }
    }
}
