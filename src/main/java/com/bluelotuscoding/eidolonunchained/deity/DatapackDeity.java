package com.bluelotuscoding.eidolonunchained.deity;

import elucent.eidolon.api.deity.Deity;
import elucent.eidolon.capability.IReputation;
import elucent.eidolon.registries.Signs;
import elucent.eidolon.util.KnowledgeUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import com.mojang.logging.LogUtils;

import java.util.*;
import java.util.stream.Collectors;
import net.minecraft.server.level.ServerPlayer;

/**
 * Datapack-driven deity implementation.
 * Handles core deity functionality without AI integration.
 * AI behavior is handled separately by AIDeityManager.
 */
public class DatapackDeity extends Deity {
    private static final org.slf4j.Logger LOGGER = LogUtils.getLogger();
    
    private final String displayName;
    private final String description;
    private final Map<String, List<String>> stageRewards = new HashMap<>();
    private final Set<String> prayerTypes = new HashSet<>();
    private int maxReputation = 100;
    
    // 🎯 REWARD TRACKING SYSTEM - Prevents duplicate rewards
    private final Map<UUID, Set<String>> playerRewardHistory = new HashMap<>();
    
    public DatapackDeity(ResourceLocation id, String name, String description, int red, int green, int blue) {
        super(id, red, green, blue);
        this.displayName = name;
        this.description = description;
    }
    
    // Getters
    public String getDisplayName() { return displayName; }
    public String getName() { return displayName; } // Alias for getDisplayName()
    public String getDescription() { return description; }
    public Set<String> getPrayerTypes() { return new HashSet<>(prayerTypes); }
    public int getMaxReputation() { return maxReputation; }
    
    // Configuration methods called by DatapackDeityManager
    public void setMaxReputation(int maxReputation) {
        this.maxReputation = maxReputation;
        this.progression.setMax(maxReputation);
    }
    
    public void addProgressionStage(Stage stage) {
        this.progression.add(stage);
    }
    
    public void addStageReward(String stageId, String type, String data) {
        stageRewards.computeIfAbsent(stageId, k -> new ArrayList<>())
                   .add(type + ":" + data);
    }
    
    public void addPrayerType(String prayerType) {
        this.prayerTypes.add(prayerType);
    }
    
    @Override
    public void onReputationUnlock(Player player, ResourceLocation lock) {
        UUID playerId = player.getUUID();
        String lockString = lock.toString();
        
        // 🎯 CHECK IF REWARDS ALREADY GIVEN - Prevents duplicate rewards!
        Set<String> playerRewards = playerRewardHistory.computeIfAbsent(playerId, k -> new HashSet<>());
        
        if (playerRewards.contains(lockString)) {
            LOGGER.debug("🔄 Player {} already received rewards for {}, skipping duplicate", 
                player.getName().getString(), lockString);
            
            // Still send unlock message for feedback
            if (player instanceof ServerPlayer serverPlayer) {
                String stageTitle = getStageDisplayName(lock.getPath());
                String message = String.format("§6[%s]§r You maintain your rank: %s", displayName, stageTitle);
                serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(Component.literal(message)));
            }
            return;
        }
        
        // Apply datapack-defined rewards (only if not already given)
        List<String> rewards = stageRewards.get(lockString);
        
        if (rewards != null && !rewards.isEmpty()) {
            LOGGER.info("🎁 Granting {} rewards to {} for unlocking {}", 
                rewards.size(), player.getName().getString(), lockString);
            
            for (String reward : rewards) {
                applyReward(player, reward);
            }
            
            // 🔒 MARK REWARDS AS GIVEN - This prevents future duplicates
            playerRewards.add(lockString);
            
            LOGGER.info("✅ Rewards granted and tracked for player {} stage {}", 
                player.getName().getString(), lockString);
        }
        
        // Update patron title if this is their patron deity
        if (player instanceof ServerPlayer serverPlayer) {
            updatePatronTitle(serverPlayer);
        }
        
        // Send unlock message
        if (player instanceof ServerPlayer serverPlayer) {
            String stageTitle = getStageDisplayName(lock.getPath());
            String message = String.format("§6[%s]§r You have achieved: %s", displayName, stageTitle);
            serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(Component.literal(message)));
        }
        
        LOGGER.info("Player {} unlocked {} for deity {}", player.getName().getString(), lock, getId());
    }
    
    @Override
    public void onReputationLock(Player player, ResourceLocation lock) {
        if (player instanceof ServerPlayer serverPlayer) {
            String message = String.format("§c[%s]§r You have lost: %s", displayName, lock.getPath());
            serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(Component.literal(message)));
        }
        
        LOGGER.info("Player {} lost {} for deity {}", player.getName().getString(), lock, getId());
    }
    
    private void applyReward(Player player, String reward) {
        String[] parts = reward.split(":", 3);
        if (parts.length < 2) return;
        
        String type = parts[0];
        String data = parts[1];
        
        try {
            switch (type) {
                case "sign":
                    grantSign(player, data);
                    break;
                case "item":
                    if (parts.length >= 3) {
                        giveItem(player, data, Integer.parseInt(parts[2]));
                    } else {
                        giveItem(player, data, 1);
                    }
                    break;
                case "effect":
                    if (parts.length >= 3) {
                        String[] effectParts = parts[2].split(":");
                        int duration = effectParts.length > 0 ? Integer.parseInt(effectParts[0]) : 200;
                        int amplifier = effectParts.length > 1 ? Integer.parseInt(effectParts[1]) : 0;
                        giveEffect(player, data, duration, amplifier);
                    }
                    break;
                case "command":
                    executeProgressionCommand(player, data, parts.length >= 3 ? parts[2] : "");
                    break;
                default:
                    LOGGER.warn("Unknown reward type: {}", type);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to apply reward '{}' to player {}", reward, player.getName().getString(), e);
        }
    }
    
    private void grantSign(Player player, String signName) {
        // Map sign names to actual signs
        switch (signName.toLowerCase()) {
            case "soul":
                KnowledgeUtil.grantSign(player, Signs.SOUL_SIGN);
                break;
            case "death":
                KnowledgeUtil.grantSign(player, Signs.DEATH_SIGN);
                break;
            case "flame":
                KnowledgeUtil.grantSign(player, Signs.FLAME_SIGN);
                break;
            case "winter":
                KnowledgeUtil.grantSign(player, Signs.WINTER_SIGN);
                break;
            case "sacred":
                KnowledgeUtil.grantSign(player, Signs.SACRED_SIGN);
                break;
            case "wicked":
                KnowledgeUtil.grantSign(player, Signs.WICKED_SIGN);
                break;
            case "blood":
                KnowledgeUtil.grantSign(player, Signs.BLOOD_SIGN);
                break;
            case "mind":
                KnowledgeUtil.grantSign(player, Signs.MIND_SIGN);
                break;
            default:
                LOGGER.warn("Unknown sign name: {}", signName);
        }
    }
    
    private void giveItem(Player player, String itemId, int count) {
        ResourceLocation itemRL = ResourceLocation.tryParse(itemId);
        if (itemRL != null) {
            Item item = ForgeRegistries.ITEMS.getValue(itemRL);
            if (item != null) {
                ItemStack stack = new ItemStack(item, count);
                if (!player.getInventory().add(stack)) {
                    player.drop(stack, false);
                }
                LOGGER.debug("Gave {} x{} to player {}", itemId, count, player.getName().getString());
            }
        }
    }
    
    private void giveEffect(Player player, String effectId, int duration, int amplifier) {
        ResourceLocation effectRL = ResourceLocation.tryParse(effectId);
        if (effectRL != null) {
            MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(effectRL);
            if (effect != null) {
                player.addEffect(new MobEffectInstance(effect, duration, amplifier));
                LOGGER.debug("Applied effect {} ({}:{}) to player {}", effectId, duration, amplifier, player.getName().getString());
            }
        }
    }
    
    /**
     * Execute a command as a progression reward
     * Supports command placeholders: {player}, {deity_name}, {stage}
     */
    private void executeProgressionCommand(Player player, String command, String extraParams) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            LOGGER.warn("Cannot execute progression command for non-server player");
            return;
        }
        
        try {
            // Replace placeholders in the command
            String processedCommand = command
                .replace("{player}", player.getName().getString())
                .replace("{deity_name}", this.displayName)
                .replace("{deity_id}", this.id.toString())
                .replace("{stage}", extraParams);
            
            // Get the server command manager
            MinecraftServer server = serverPlayer.getServer();
            if (server != null) {
                CommandSourceStack commandSource = server.createCommandSourceStack()
                    .withEntity(serverPlayer)
                    .withLevel(serverPlayer.serverLevel())
                    .withPosition(serverPlayer.position())
                    .withPermission(2); // Admin permission level
                
                // Execute the command
                Commands commandManager = server.getCommands();
                int result = commandManager.performPrefixedCommand(commandSource, processedCommand);
                
                if (result > 0) {
                    LOGGER.info("✅ Executed progression command for {}: {}", player.getName().getString(), processedCommand);
                } else {
                    LOGGER.warn("⚠️ Progression command returned 0: {}", processedCommand);
                }
            }
        } catch (Exception e) {
            LOGGER.error("❌ Failed to execute progression command '{}' for player {}", command, player.getName().getString(), e);
        }
    }
    
    /**
     * Get player's current reputation with this deity
     */
    public double getPlayerReputation(Player player) {
        IReputation rep = player.getCapability(IReputation.INSTANCE).orElse(null);
        if (rep != null) {
            return rep.getReputation(player.getUUID(), this.getId());
        }
        return 0.0;
    }
    
    /**
     * Check if player has specific research
     */
    public boolean hasResearch(Player player, ResourceLocation research) {
        return KnowledgeUtil.knowsResearch(player, research);
    }
    
    /**
     * Get count of player's research (for AI context)
     */
    public int getResearchCount(Player player) {
        // This would need access to the research capability
        // For now, return estimated count based on signs
        int signCount = 0;
        if (KnowledgeUtil.knowsSign(player, Signs.SOUL_SIGN)) signCount++;
        if (KnowledgeUtil.knowsSign(player, Signs.DEATH_SIGN)) signCount++;
        if (KnowledgeUtil.knowsSign(player, Signs.FLAME_SIGN)) signCount++;
        if (KnowledgeUtil.knowsSign(player, Signs.WINTER_SIGN)) signCount++;
        if (KnowledgeUtil.knowsSign(player, Signs.SACRED_SIGN)) signCount++;
        if (KnowledgeUtil.knowsSign(player, Signs.WICKED_SIGN)) signCount++;
        if (KnowledgeUtil.knowsSign(player, Signs.BLOOD_SIGN)) signCount++;
        if (KnowledgeUtil.knowsSign(player, Signs.MIND_SIGN)) signCount++;
        
        return signCount * 3; // Estimate 3 research per sign
    }
    
    /**
     * Updates the player's title if this deity is their patron (public method for capability integration)
     */
    public void updatePatronTitle(ServerPlayer player) {
        try {
            player.level().getCapability(com.bluelotuscoding.eidolonunchained.capability.CapabilityHandler.PATRON_DATA_CAPABILITY)
                .ifPresent(patronData -> {
                    ResourceLocation patron = patronData.getPatron(player);
                    if (getId().equals(patron)) {
                        // Get current reputation to determine appropriate title
                        try {
                            player.level().getCapability(elucent.eidolon.capability.IReputation.INSTANCE)
                                .ifPresent(reputation -> {
                                    double rep = reputation.getReputation(player, getId());
                                    
                                    // Find the highest stage the player qualifies for
                                    String stageTitle = getStageForReputation(rep);
                                    patronData.setTitle(player, stageTitle);
                                    LOGGER.debug("Updated patron title for {} to: {}", player.getName().getString(), stageTitle);
                                });
                        } catch (Exception e) {
                            LOGGER.warn("Failed to get reputation for title update: {}", e.getMessage());
                        }
                    }
                });
        } catch (Exception e) {
            LOGGER.warn("Failed to update patron title for player {}: {}", player.getName().getString(), e.getMessage());
        }
    }
    
    /**
     * Gets the appropriate stage title for a given reputation level using translation keys
     */
    private String getStageForReputation(double reputation) {
        String bestStageId = "initiate"; // Default stage ID
        
        // Use Eidolon's progression system to find the highest unlocked stage
        try {
            // Find the highest stage this reputation qualifies for
            if (reputation >= 100) bestStageId = "champion";
            else if (reputation >= 75) bestStageId = "high_priest";
            else if (reputation >= 50) bestStageId = "priest";
            else if (reputation >= 25) bestStageId = "acolyte";
            else bestStageId = "initiate";
            
            // Create translation key based on deity ID and stage ID
            String translationKey = String.format("eidolonunchained.patron.title.%s.%s", 
                this.id.getPath(), bestStageId);
            
            // Try to resolve the translation using the server-side I18n if available
            // For now, return a formatted fallback since we can't easily resolve server-side
            return getDisplayNameForStage(bestStageId);
            
        } catch (Exception e) {
            LOGGER.warn("Error determining stage for reputation {}: {}", reputation, e.getMessage());
            // Fallback to generic title
            return getDisplayNameForStage(bestStageId);
        }
    }
    
    /**
     * Gets a display name for a stage ID based on the deity
     */
    private String getDisplayNameForStage(String stageId) {
        String deityPath = this.id.getPath();
        
        // Map deity-specific stages to display names
        switch (deityPath) {
            case "dark_deity":
                switch (stageId) {
                    case "initiate": return "Shadow Initiate";
                    case "acolyte": return "Dark Acolyte";
                    case "priest": return "Shadow Priest";
                    case "high_priest": return "Void Master";
                    case "champion": return "Shadow Champion";
                }
                break;
            case "light_deity":
                switch (stageId) {
                    case "initiate": return "Light Bearer";
                    case "acolyte": return "Sacred Acolyte";
                    case "priest": return "Divine Priest";
                    case "high_priest": return "Radiant Oracle";
                    case "champion": return "Light Champion";
                }
                break;
            case "nature_deity":
                switch (stageId) {
                    case "initiate": return "Nature's Child";
                    case "acolyte": return "Grove Keeper";
                    case "priest": return "Druid";
                    case "high_priest": return "Elder Druid";
                    case "champion": return "Nature's Champion";
                }
                break;
        }
        
        // Generic fallback
        switch (stageId) {
            case "initiate": return "Initiate";
            case "acolyte": return "Acolyte";
            case "priest": return "Priest";
            case "high_priest": return "High Priest";
            case "champion": return "Champion";
            default: return "Initiate";
        }
    }
    
    /**
     * Gets the progression stages map for external access (simplified implementation)
     */
    public Map<String, Object> getProgressionStages() {
        // Return a simple map of stage names to reputation requirements
        Map<String, Object> stages = new HashMap<>();
        stages.put("initiate", Map.of("reputationRequired", 0));
        stages.put("acolyte", Map.of("reputationRequired", 25));
        stages.put("priest", Map.of("reputationRequired", 50));
        stages.put("high_priest", Map.of("reputationRequired", 75));
        stages.put("champion", Map.of("reputationRequired", 100));
        return stages;
    }
    
    /**
     * Converts a stage name to a display name based on JSON configuration
     */
    public String getStageDisplayName(String stageName) {
        // First, try to get custom display name from progression stages in JSON
        String displayName = getCustomStageDisplayName(stageName);
        if (displayName != null) {
            return displayName;
        }
        
        // Fallback: Convert snake_case to Title Case
        return Arrays.stream(stageName.replace("_", " ").split(" "))
                     .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase())
                     .collect(Collectors.joining(" "));
    }
    
    /**
     * Gets custom display name from JSON progression stages if available
     */
    private String getCustomStageDisplayName(String stageName) {
        // This would be populated from JSON if we add display_name fields to stages
        // For now, use the stage ID as the title
        
        // Example mappings based on your dark_deity.json:
        switch (stageName) {
            case "shadow_initiate":
                return "Shadow Initiate";
            case "dark_scholar":
                return "Dark Scholar";
            case "shadow_master":
                return "Shadow Master";
            default:
                return null; // Use fallback formatting
        }
    }
    
    /**
     * Get player's magical progression level (for AI context)
     */
    public String getProgressionLevel(Player player) {
        double reputation = getPlayerReputation(player);
        int researchCount = getResearchCount(player);
        
        if (reputation >= 75 && researchCount >= 15) {
            return "master";
        } else if (reputation >= 50 && researchCount >= 10) {
            return "advanced";
        } else if (reputation >= 25 && researchCount >= 5) {
            return "intermediate";
        } else if (reputation >= 10 || researchCount >= 2) {
            return "novice";
        } else {
            return "beginner";
        }
    }
    
    // =====================================
    // 🎯 REWARD TRACKING UTILITIES
    // =====================================
    
    /**
     * 🧹 Clear reward history for a player (useful for testing)
     */
    public void clearPlayerRewardHistory(UUID playerId) {
        playerRewardHistory.remove(playerId);
        LOGGER.info("🧹 Cleared reward history for player {}", playerId);
    }
    
    /**
     * 🔍 Check if player has received rewards for a specific stage
     */
    public boolean hasReceivedRewards(UUID playerId, String stage) {
        Set<String> playerRewards = playerRewardHistory.get(playerId);
        return playerRewards != null && playerRewards.contains(stage);
    }
    
    /**
     * 📊 Get reward history for debugging
     */
    public Set<String> getPlayerRewardHistory(UUID playerId) {
        return playerRewardHistory.getOrDefault(playerId, new HashSet<>());
    }
    
    /**
     * Get current progression stage name based on reputation
     */
    public String getCurrentProgressionStage(double reputation) {
        // This would need to parse the JSON progression data
        // For now, return a simple mapping based on reputation thresholds
        if (reputation >= 100) return "Champion";
        if (reputation >= 75) return "High Priest";
        if (reputation >= 50) return "Priest";
        if (reputation >= 25) return "Acolyte";
        return "Initiate";
    }
    
    /**
     * Get information about the next progression stage
     */
    public String getNextProgressionInfo(double reputation) {
        if (reputation >= 100) return "Maximum stage reached";
        if (reputation >= 75) return String.format("Champion at 100 reputation (%.1f needed)", 100 - reputation);
        if (reputation >= 50) return String.format("High Priest at 75 reputation (%.1f needed)", 75 - reputation);
        if (reputation >= 25) return String.format("Priest at 50 reputation (%.1f needed)", 50 - reputation);
        return String.format("Acolyte at 25 reputation (%.1f needed)", 25 - reputation);
    }
}
