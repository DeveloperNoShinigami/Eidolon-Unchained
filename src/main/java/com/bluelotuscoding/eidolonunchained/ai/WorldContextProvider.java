package com.bluelotuscoding.eidolonunchained.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Provides real-time world context to AI about the player's current situation
 * 🔥 LIVE WORLD AWARENESS - not just registry data, but actual game state!
 */
public class WorldContextProvider {
    private static final Logger LOGGER = LogManager.getLogger();
    
    /**
     * Generate comprehensive player world context including deity relationship data
     * This includes ALL information from ai_deities folder configurations
     */
    public static String generatePlayerWorldContext(ServerPlayer player) {
        StringBuilder context = new StringBuilder();
        
        try {
            // 🌍 BASIC WORLD STATE
            context.append(generateLocationContext(player));
            context.append(generatePlayerStateContext(player));
            context.append(generateEnvironmentalContext(player));
            context.append(generateNearbyEntitiesContext(player));
            context.append(generateLocalBlockContext(player));
            
            // 🎯 DEITY RELATIONSHIP CONTEXT (New!)
            context.append(generateDeityRelationshipContext(player));
            
            // 🎯 BASIC SITUATIONAL ASSESSMENT
            context.append("\n=== SITUATION ASSESSMENT ===\n");
            if (player.getHealth() < player.getMaxHealth() / 2) {
                context.append("Player appears to be injured and may need healing.\n");
            }
            if (player.getFoodData().getFoodLevel() < 10) {
                context.append("Player appears to be hungry and may need food.\n");
            }
            if (player.experienceLevel < 10) {
                context.append("Player appears to be new/inexperienced.\n");
            } else if (player.experienceLevel > 50) {
                context.append("Player appears to be very experienced.\n");
            }
            
            return context.toString();
        } catch (Exception e) {
            LOGGER.error("Failed to generate player world context: {}", e.getMessage());
            return "Player in world at " + player.getX() + ", " + player.getY() + ", " + player.getZ();
        }
    }

    private static String generateLocationContext(ServerPlayer player) {
        StringBuilder context = new StringBuilder();
        BlockPos pos = player.blockPosition();

        context.append("--- LOCATION ---\n");
        context.append("Coordinates: ")
            .append(pos.getX()).append(", ")
            .append(pos.getY()).append(", ")
            .append(pos.getZ()).append("\n");
        context.append("Dimension: ").append(player.level().dimension().location()).append("\n");

        ResourceLocation biomeId = player.level().getBiome(pos)
            .unwrapKey()
            .map(key -> key.location())
            .orElse(null);
        if (biomeId != null) {
            context.append("Biome: ").append(biomeId).append("\n");
        }

        return context.toString();
    }

    private static String generateNearbyEntitiesContext(ServerPlayer player) {
        StringBuilder context = new StringBuilder();
        context.append("\n--- NEARBY ENTITIES ---\n");

        AABB area = player.getBoundingBox().inflate(16.0D, 8.0D, 16.0D);
        List<Entity> entities = player.level().getEntities(player, area);

        long living = entities.stream().filter(e -> e instanceof LivingEntity).count();
        long hostiles = entities.stream().filter(e -> e instanceof net.minecraft.world.entity.monster.Monster).count();
        long passives = entities.stream().filter(e -> e instanceof net.minecraft.world.entity.animal.Animal).count();
        long players = entities.stream().filter(e -> e instanceof ServerPlayer).count();

        context.append("Total nearby: ").append(entities.size()).append("\n");
        context.append("Living: ").append(living).append("\n");
        context.append("Hostile: ").append(hostiles).append("\n");
        context.append("Passive: ").append(passives).append("\n");
        context.append("Players: ").append(players).append("\n");

        return context.toString();
    }
    
    /**
     * Generate deity relationship context from ai_deities configurations
     * This includes chosen deity, titles, progression, and patron relationships
     */
    private static String generateDeityRelationshipContext(ServerPlayer player) {
        StringBuilder context = new StringBuilder();
        context.append("\n=== DEITY RELATIONSHIPS ===\n");
        
        try {
            ResourceLocation playerPatron = com.bluelotuscoding.eidolonunchained.patron.PatronState.getPatron(player);
            String title = com.bluelotuscoding.eidolonunchained.patron.PatronState.getTitle(player);

            if (playerPatron != null) {
                context.append("Chosen Patron: ").append(playerPatron).append("\n");

                // Get the deity data for reputation
                var deityManager = com.bluelotuscoding.eidolonunchained.data.DatapackDeityManager.getInstance();
                var deity = deityManager.getDeity(playerPatron);
                if (deity != null) {
                    double reputation = deity.getPlayerReputation(player);
                    context.append("Patron Reputation: ").append((int) reputation).append("\n");

                    // Get progression level
                    String progressionLevel = getDynamicProgressionLevel(deity, player);
                    context.append("Patron Rank: ").append(progressionLevel).append("\n");
                }

                // Get AI deity configuration for additional context
                var aiManager = com.bluelotuscoding.eidolonunchained.ai.AIDeityManager.getInstance();
                var aiConfig = aiManager.getAIConfig(playerPatron);
                if (aiConfig != null) {
                    if (!aiConfig.patron_config.alliedDeities.isEmpty()) {
                        context.append("Patron Allies: ").append(String.join(", ", aiConfig.patron_config.alliedDeities)).append("\n");
                    }
                    if (!aiConfig.patron_config.opposingDeities.isEmpty()) {
                        context.append("Patron Enemies: ").append(String.join(", ", aiConfig.patron_config.opposingDeities)).append("\n");
                    }
                }
            } else {
                context.append("No Patron: Player is godless\n");
            }

            if (title != null && !title.isEmpty()) {
                context.append("Player Title: ").append(title).append("\n");
            } else {
                context.append("No Special Title\n");
            }

            // Check relationships with ALL deities
            var allDeities = com.bluelotuscoding.eidolonunchained.data.DatapackDeityManager.getAllDeities();
            context.append("Deity Reputations: ");
            for (var deityEntry : allDeities.entrySet()) {
                var deity = deityEntry.getValue();
                double rep = deity.getPlayerReputation(player);
                if (rep != 0) { // Only show non-zero reputations
                    context.append(deityEntry.getKey().getPath()).append("(").append((int) rep).append(") ");
                }
            }
            context.append("\n");
            
        } catch (Exception e) {
            LOGGER.warn("Failed to generate deity relationship context: {}", e.getMessage());
            context.append("Deity relationship data unavailable\n");
        }
        
        return context.toString();
    }
    
    /**
     * Helper method to get dynamic progression level
     */
    private static String getDynamicProgressionLevel(com.bluelotuscoding.eidolonunchained.deity.DatapackDeity deity, ServerPlayer player) {
        double reputation = deity.getPlayerReputation(player);
        
        // Get progression stages from deity configuration
        Map<String, Object> stages = deity.getProgressionStages();
        if (stages == null || stages.isEmpty()) {
            // Fallback to basic reputation-based levels
            if (reputation >= 200) return "master";
            if (reputation >= 50) return "adept"; 
            return "novice";
        }
        
        // Find the highest stage the player qualifies for
        String currentStage = "novice";
        int highestRequirement = 0;
        
        for (Map.Entry<String, Object> stage : stages.entrySet()) {
            try {
                int requirement = 0;
                if (stage.getValue() instanceof Number) {
                    requirement = ((Number) stage.getValue()).intValue();
                } else if (stage.getValue() instanceof Map) {
                    Map<?, ?> stageData = (Map<?, ?>) stage.getValue();
                    Object repReq = stageData.get("reputation_required");
                    if (repReq instanceof Number) {
                        requirement = ((Number) repReq).intValue();
                    }
                }
                
                if (reputation >= requirement && requirement > highestRequirement) {
                    currentStage = stage.getKey();
                    highestRequirement = requirement;
                }
            } catch (Exception e) {
                // Skip invalid stage data
            }
        }
        
        return currentStage;
    }
    
    /**
     * Generate player's current state information
     */
    private static String generatePlayerStateContext(ServerPlayer player) {
        StringBuilder context = new StringBuilder();

        context.append("\n--- PLAYER STATE ---\n");
        context.append("Name: ").append(player.getName().getString()).append("\n");
        context.append("Health: ").append(String.format("%.1f/%.1f", player.getHealth(), player.getMaxHealth())).append("\n");
        context.append("Food: ").append(player.getFoodData().getFoodLevel()).append("/20\n");
        context.append("Experience Level: ").append(player.experienceLevel).append("\n");
        context.append("Game Mode: ").append(player.gameMode.getGameModeForPlayer().getName()).append("\n");

        // Active effects
        Collection<MobEffectInstance> effects = player.getActiveEffects();
        if (effects.isEmpty()) {
            context.append("Active Effects: None\n");
        } else {
            context.append("Active Effects: ");
            String joined = effects.stream()
                .map(effect -> {
                    ResourceLocation id = BuiltInRegistries.MOB_EFFECT.getKey(effect.getEffect());
                    int amplifier = effect.getAmplifier() + 1;
                    return id + " " + amplifier;
                })
                .collect(Collectors.joining(", "));
            context.append(joined).append("\n");
        }

        return context.toString();
    }
    
    /**
     * Generate environmental context
     */
    private static String generateEnvironmentalContext(ServerPlayer player) {
        StringBuilder context = new StringBuilder();

        context.append("\n--- ENVIRONMENT ---\n");

        BlockPos pos = player.blockPosition();

        // Dimension and biome
        ResourceKey<Level> dimension = player.level().dimension();
        context.append("Dimension: ").append(dimension.location()).append("\n");

        ResourceLocation biomeId = player.level().getBiome(pos)
            .unwrapKey()
            .map(key -> key.location())
            .orElse(new ResourceLocation("minecraft", "unknown"));
        context.append("Biome: ").append(biomeId).append("\n");

        if (player.level() instanceof ServerLevel serverLevel) {
            long timeOfDay = serverLevel.getDayTime() % 24000;
            context.append("Time: ").append(getTimeDescription(timeOfDay))
                .append(" (").append(timeOfDay).append("/24000)\n");

            if (dimension.equals(Level.OVERWORLD)) {
                context.append("Weather: ");
                if (serverLevel.isThundering()) {
                    context.append("Thunderstorm");
                } else if (serverLevel.isRaining()) {
                    context.append("Rain");
                } else {
                    context.append("Clear");
                }
                context.append("\n");
            }
        }

        // Light level
        int lightLevel = player.level().getMaxLocalRawBrightness(pos);
        context.append("Light Level: ").append(lightLevel).append("/15\n");

        // Underground/surface
        boolean isUnderground = pos.getY() < 50;
        boolean isInCave = isUnderground && lightLevel < 8;
        context.append("Location Type: ");
        if (isInCave) context.append("Cave/Underground");
        else if (isUnderground) context.append("Underground");
        else context.append("Surface");
        context.append("\n");

        // In water/lava
        if (player.isInWater()) {
            context.append("Player is currently in water\n");
        }
        if (player.isInLava()) {
            context.append("Player is currently in lava\n");
        }

        context.append("Coordinates: ")
            .append(pos.getX()).append(", ")
            .append(pos.getY()).append(", ")
            .append(pos.getZ()).append("\n");

        return context.toString();
    }
    
    /**
     * Generate local block context around player
     */
    private static String generateLocalBlockContext(ServerPlayer player) {
        StringBuilder context = new StringBuilder();
        
        context.append("\n--- LOCAL BLOCKS (5 block radius) ---\n");
        
        BlockPos playerPos = player.blockPosition();
        Map<String, Integer> blockCounts = new HashMap<>();
        
        // Scan 5x5x5 area around player
        for (int x = -2; x <= 2; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -2; z <= 2; z++) {
                    BlockPos checkPos = playerPos.offset(x, y, z);
                    BlockState blockState = player.level().getBlockState(checkPos);
                    Block block = blockState.getBlock();
                    
                    ResourceLocation blockId = block.builtInRegistryHolder().key().location();
                    String blockName = blockId.toString();
                    
                    // Skip air blocks for cleaner output
                    if (!blockName.equals("minecraft:air")) {
                        blockCounts.merge(blockName, 1, Integer::sum);
                    }
                }
            }
        }
        
        // Show most common blocks
        if (blockCounts.isEmpty()) {
            context.append("Only air blocks nearby\n");
        } else {
            blockCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(8) // Show top 8 most common blocks
                .forEach(entry -> context.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n"));
        }
        
        // Block player is standing on
        BlockPos standingPos = playerPos.below();
        BlockState standingBlock = player.level().getBlockState(standingPos);
        ResourceLocation standingBlockId = standingBlock.getBlock().builtInRegistryHolder().key().location();
        context.append("Standing on: ").append(standingBlockId.toString()).append("\n");
        
        return context.toString();
    }
    
    /**
     * Generate player inventory context
     */
    private static String generateInventoryContext(ServerPlayer player) {
        StringBuilder context = new StringBuilder();
        
        context.append("\n--- INVENTORY ---\n");
        
        // Count items in inventory
        Map<String, Integer> itemCounts = new HashMap<>();
        
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.isEmpty()) {
                ResourceLocation itemId = stack.getItem().builtInRegistryHolder().key().location();
                itemCounts.merge(itemId.toString(), stack.getCount(), Integer::sum);
            }
        }
        
        if (itemCounts.isEmpty()) {
            context.append("Empty inventory\n");
        } else {
            context.append("Items (").append(itemCounts.size()).append(" types): ");
            
            // Show most significant items
            List<Map.Entry<String, Integer>> sortedItems = itemCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(10) // Show top 10 most numerous items
                .collect(Collectors.toList());
            
            for (int i = 0; i < sortedItems.size(); i++) {
                Map.Entry<String, Integer> entry = sortedItems.get(i);
                context.append(entry.getKey()).append("(").append(entry.getValue()).append(")");
                if (i < sortedItems.size() - 1) context.append(", ");
            }
            context.append("\n");
        }
        
        // Held item
        ItemStack heldItem = player.getMainHandItem();
        if (!heldItem.isEmpty()) {
            ResourceLocation heldItemId = heldItem.getItem().builtInRegistryHolder().key().location();
            context.append("Holding: ").append(heldItemId.toString()).append("\n");
        }
        
        return context.toString();
    }
    
    /**
     * Convert time of day to human readable description
     */
    private static String getTimeDescription(long timeOfDay) {
        if (timeOfDay >= 0 && timeOfDay < 6000) return "Morning";
        else if (timeOfDay >= 6000 && timeOfDay < 12000) return "Day";
        else if (timeOfDay >= 12000 && timeOfDay < 13800) return "Evening";
        else if (timeOfDay >= 13800 && timeOfDay < 22200) return "Night";
        else return "Late Night";
    }
    
    /**
     * Generate contextual situation assessment for AI
     */
    public static String generateSituationAssessment(ServerPlayer player) {
        StringBuilder assessment = new StringBuilder();
        
        assessment.append("\n--- SITUATION ASSESSMENT ---\n");
        
        // Danger assessment
        boolean lowHealth = player.getHealth() < player.getMaxHealth() * 0.3f;
        boolean lowFood = player.getFoodData().getFoodLevel() < 6;
        boolean inDanger = player.isOnFire() || player.isInLava();
        boolean inDarkness = player.level().getMaxLocalRawBrightness(player.blockPosition()) < 8;
        boolean underground = player.blockPosition().getY() < 50;
        
        if (lowHealth) assessment.append("⚠ Player has low health\n");
        if (lowFood) assessment.append("🍖 Player is hungry\n");
        if (inDanger) assessment.append("🔥 Player is in immediate danger\n");
        if (inDarkness && underground) assessment.append("🌑 Player is in dark underground area\n");
        
        // Opportunity assessment
        boolean hasGoodItems = !player.getInventory().items.isEmpty();
        boolean goodLocation = !underground && player.level().getDayTime() % 24000 < 12000;
        
        if (hasGoodItems) assessment.append("✅ Player has items available\n");
        if (goodLocation) assessment.append("☀ Good time and location for activities\n");
        
        return assessment.toString();
    }
}
