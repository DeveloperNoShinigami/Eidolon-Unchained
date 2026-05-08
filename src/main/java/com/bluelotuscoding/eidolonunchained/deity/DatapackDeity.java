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
import java.util.Arrays;
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
    
    // 🎯 ABANDON CONFIGURATION - Loaded from deity JSON
    private double abandonPenalty = 1.0; // Default: lose all progress (100%)
    private boolean abandonResetsReputation = true; // Default: clear all reputation
    private String abandonMessage = "You have abandoned your patron and lost all divine favor.";
    
    // 🎯 REWARD TRACKING SYSTEM - Prevents duplicate rewards
    private final Map<UUID, Set<String>> playerRewardHistory = new HashMap<>();
    
    // 🎭 STAGE TITLE MAPPING - Maps stage IDs to their display titles from JSON
    private final Map<String, String> stageTitles = new HashMap<>();

    // Optional link to a native Eidolon deity — rep changes are mirrored there at 0.5x scale
    private ResourceLocation linkedEidolonDeity = null;
    // Base damage identity for deity-linked combat chants.
    private ResourceLocation deityDamageType = null;

    public void setLinkedEidolonDeity(ResourceLocation id) { this.linkedEidolonDeity = id; }
    public ResourceLocation getLinkedEidolonDeity() { return linkedEidolonDeity; }
    public void setDeityDamageType(ResourceLocation damageType) { this.deityDamageType = damageType; }
    public ResourceLocation getDeityDamageType() { return deityDamageType; }
    
    public DatapackDeity(ResourceLocation id, String name, String description, int red, int green, int blue) {
        super(id, red, green, blue);
        this.displayName = name;
        this.description = description;
    }
    
    /**
     * Get the minimum reputation required to become a patron (first progression stage)
     */
    public double getMinimumPatronReputation() {
        if (this.progression.getSteps().isEmpty()) {
            return 0.0; // No progression stages, start at 0
        }
        
        // Find the lowest reputation requirement
        return this.progression.getSteps().values().stream()
            .mapToDouble(stage -> stage.rep())
            .min()
            .orElse(0.0);
    }
    
    /**
     * Get abandon penalty (0.0 to 1.0, where 1.0 = lose all reputation)
     */
    public double getAbandonPenalty() {
        return abandonPenalty;
    }
    
    /**
     * Whether abandoning this deity resets reputation to 0
     */
    public boolean shouldResetReputationOnAbandon() {
        return abandonResetsReputation;
    }
    
    /**
     * Get the message to show when abandoning this deity
     */
    public String getAbandonMessage() {
        return abandonMessage;
    }
    
    /**
     * Set abandon configuration from JSON data
     */
    public void setAbandonConfiguration(double penalty, boolean resetReputation, String message) {
        this.abandonPenalty = Math.max(0.0, Math.min(1.0, penalty)); // Clamp to 0-1
        this.abandonResetsReputation = resetReputation;
        this.abandonMessage = message != null ? message : "You have abandoned your patron.";
    }
    
    // Getters
    public String getDisplayName() { return displayName; }
    public String getName() { return displayName; } // Alias for getDisplayName()
    public String getDescription() { return description; }
    public Set<String> getPrayerTypes() { return new HashSet<>(prayerTypes); }
    public int getMaxReputation() { return maxReputation; }
    
    /**
     * Get stage rewards for tier progression system
     * @param tierTitle The tier title (e.g., "Shadow Initiate", "Dark Acolyte")
     * @return List of reward commands, or null if not found
     */
    public List<String> getStageRewards(String tierTitle) {
        if (tierTitle == null) return null;
        
        // First try to find rewards by tier title directly
        List<String> rewards = stageRewards.get(tierTitle);
        if (rewards != null && !rewards.isEmpty()) {
            return new ArrayList<>(rewards); // Return copy to prevent modification
        }
        
        // Try to find by stage ID using the stage titles mapping
        for (Map.Entry<String, String> entry : stageTitles.entrySet()) {
            if (tierTitle.equals(entry.getValue())) {
                // Found the stage ID for this title
                String stageId = entry.getKey();
                rewards = stageRewards.get(stageId);
                if (rewards != null && !rewards.isEmpty()) {
                    return new ArrayList<>(rewards);
                }
            }
        }
        
        // Fallback: try to find by loose matching
        for (Map.Entry<String, List<String>> entry : stageRewards.entrySet()) {
            if (entry.getKey().toLowerCase().contains(tierTitle.toLowerCase()) ||
                tierTitle.toLowerCase().contains(entry.getKey().toLowerCase())) {
                return new ArrayList<>(entry.getValue());
            }
        }
        
        return null; // No rewards found
    }
    
    // Configuration methods called by DatapackDeityManager
    public void setMaxReputation(int maxReputation) {
        this.maxReputation = maxReputation;
        this.progression.setMax(maxReputation);
    }
    
    public void addProgressionStage(Stage stage) {
        this.progression.add(stage);
    }
    
    public void addStageReward(String stageId, String type, String data) {
        if ("command".equals(type)) {
            // Direct command format - store as-is
            stageRewards.computeIfAbsent(stageId, k -> new ArrayList<>()).add(data);
        } else {
            // Legacy format - combine type and data
            stageRewards.computeIfAbsent(stageId, k -> new ArrayList<>()).add(type + ":" + data);
        }
    }
    
    public void addPrayerType(String prayerType) {
        this.prayerTypes.add(prayerType);
    }
    
    public void setStageTitle(String stageId, String title) {
        this.stageTitles.put(stageId, title);
    }
    
    @Override
    public void onReputationUnlock(Player player, ResourceLocation lock) {
        UUID playerId = player.getUUID();
        String lockString = lock.toString();
        String stageName = lock.getPath(); // Extract just the stage name from ResourceLocation
        
        // 🎯 CHECK IF REWARDS ALREADY GIVEN - Prevents duplicate rewards!
        Set<String> playerRewards = playerRewardHistory.computeIfAbsent(playerId, k -> new HashSet<>());
        
        if (playerRewards.contains(lockString)) {
            LOGGER.debug("🔄 Player {} already received rewards for {}, skipping duplicate", 
                player.getName().getString(), lockString);
            
            // Still send unlock message for feedback
            if (player instanceof ServerPlayer serverPlayer) {
                String stageTitle = getStageDisplayName(stageName);
                String message = String.format("§6[%s]§r You maintain your rank: %s", displayName, stageTitle);
                serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(Component.literal(message)));
            }
            return;
        }
        
        // 🎁 LOOK FOR REWARDS - Try both full ResourceLocation and just stage name
        List<String> rewards = stageRewards.get(lockString); // Try full ResourceLocation first
        if (rewards == null || rewards.isEmpty()) {
            rewards = stageRewards.get(stageName); // Fallback to just stage name
            LOGGER.debug("🔍 Checking rewards for stage name: {} (found: {})", stageName, rewards != null);
        } else {
            LOGGER.debug("🔍 Found rewards using full ResourceLocation: {}", lockString);
        }
        
        if (rewards != null && !rewards.isEmpty()) {
            LOGGER.info("🎁 Granting {} rewards to {} for unlocking {} (stage: {})", 
                rewards.size(), player.getName().getString(), lockString, stageName);
            
            for (String reward : rewards) {
                applyReward(player, reward);
            }
            
            // 🔒 MARK REWARDS AS GIVEN - This prevents future duplicates
            playerRewards.add(lockString);
            
            LOGGER.info("✅ Rewards granted and tracked for player {} stage {}", 
                player.getName().getString(), lockString);
        } else {
            LOGGER.warn("⚠️ No rewards defined for stage {} (tried keys: '{}' and '{}')", 
                stageName, lockString, stageName);
        }
        
        // Update patron title if this is their patron deity
        if (player instanceof ServerPlayer serverPlayer) {
            updatePatronTitle(serverPlayer);
        }
        
        // Send unlock message
        if (player instanceof ServerPlayer serverPlayer) {
            String stageTitle = getStageDisplayName(stageName); // Use proper stage display name
            String message = String.format("§6[%s]§r You have achieved: %s", displayName, stageTitle);
            serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(Component.literal(message)));
        }
        
        LOGGER.info("Player {} unlocked {} for deity {}", player.getName().getString(), lock, getId());

        // Trigger AI "progression" conversation if this deity has AI configured
        if (player instanceof ServerPlayer serverPlayer) {
            com.bluelotuscoding.eidolonunchained.ai.AIDeityManager mgr = com.bluelotuscoding.eidolonunchained.ai.AIDeityManager.getInstance();
            if (mgr != null && mgr.getAIConfig(getId()) != null) {
                com.bluelotuscoding.eidolonunchained.chat.DeityChat.startConversation(serverPlayer, getId());
            }
        }
    }
    
    @Override
    public void onReputationLock(Player player, ResourceLocation lock) {
        if (player instanceof ServerPlayer serverPlayer) {
            String message = String.format("§c[%s]§r You have lost: %s", displayName, lock.getPath());
            serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(Component.literal(message)));
        }
        
        LOGGER.info("Player {} lost {} for deity {}", player.getName().getString(), lock, getId());
    }
    
    /**
     * ✨ REAL-TIME TITLE UPDATES - Called automatically by Eidolon when reputation changes
     * This is the proper way to handle reputation changes instead of using tick events!
     */
    @Override
    public void onReputationChange(Player player, elucent.eidolon.capability.IReputation rep, double prev, double updated) {
        // 🚫 DON'T call super() - prevents Eidolon's hardcoded rewards from conflicting with our custom system
        // super.onReputationChange(player, rep, prev, updated);
        
        // Handle our custom title updates for patron players
        if (player instanceof ServerPlayer serverPlayer) {
            try {
                // 🎯 DYNAMIC PATRON SELECTION DETECTION - Use deity's minimum reputation
                double minimumRequired = getMinimumPatronReputation();
                if (prev == 0.0 && updated == minimumRequired) {
                    // Player just selected this deity as patron - trigger initial tier advancement
                    LOGGER.info("🎉 INITIAL PATRON SELECTION: Player {} chose {} as patron with initial reputation {} (minimum: {})", 
                        serverPlayer.getName().getString(), getName(), updated, minimumRequired);
                    
                    // Trigger initial tier progression (should give appropriate starting rewards)
                    com.bluelotuscoding.eidolonunchained.chat.DeityChat.checkAndHandleTierProgression(serverPlayer, getId());
                    return; // Early return for patron selection
                }
                
                // Check if this deity is the player's patron
                player.level().getCapability(com.bluelotuscoding.eidolonunchained.capability.CapabilityHandler.PATRON_DATA_CAPABILITY)
                    .ifPresent(patronData -> {
                        ResourceLocation patron = patronData.getPatron(serverPlayer);
                        if (getId().equals(patron)) {
                            // This is the player's patron deity - update title immediately
                            String oldTitle = patronData.getTitle(serverPlayer);
                            updatePatronTitle(serverPlayer);
                            String newTitle = patronData.getTitle(serverPlayer);
                            
                            // Notify player if title changed
                            if (!Objects.equals(oldTitle, newTitle)) {
                                LOGGER.info("🎭 Title updated for {} (reputation: {} → {}): '{}' → '{}'", 
                                    serverPlayer.getName().getString(), prev, updated, oldTitle, newTitle);
                                
                                // Send notification to player
                                if (newTitle != null && !newTitle.isEmpty()) {
                                    serverPlayer.sendSystemMessage(Component.literal(
                                        "§6✨ Your devotion has earned you a new title: §e" + newTitle));
                                }
                            }
                        }
                    });
                
                // 🎉 CHECK FOR TIER PROGRESSION AND AUTO-CONGRATULATION
                // This triggers automatic deity conversations when players advance in tier
                com.bluelotuscoding.eidolonunchained.chat.DeityChat.checkAndHandleTierProgression(serverPlayer, getId());

            } catch (Exception e) {
                LOGGER.error("Error updating title for reputation change on deity {}: {}", getId(), e.getMessage(), e);
            }
        }

        // Mirror rep change to linked native Eidolon deity at 0.5x scale
        if (linkedEidolonDeity != null) {
            double delta = updated - prev;
            double scaled = delta * 0.5;
            if (scaled != 0) {
                try {
                    double eidolonPrev = rep.getReputation(player.getUUID(), linkedEidolonDeity);
                    // Use UUID overload — does NOT call considerChange, preventing a loop back to us
                    rep.addReputation(player.getUUID(), linkedEidolonDeity, scaled);
                    double eidolonUpdated = rep.getReputation(player.getUUID(), linkedEidolonDeity);
                    // Manually fire the Eidolon deity's change handler so signs/facts unlock
                    elucent.eidolon.common.deity.Deities.find(linkedEidolonDeity)
                        .onReputationChange(player, rep, eidolonPrev, eidolonUpdated);
                } catch (Exception e) {
                    LOGGER.warn("Failed to mirror rep to linked Eidolon deity {}: {}", linkedEidolonDeity, e.getMessage());
                }
            }
        }
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
            // Replace placeholders in the command intelligently
            String processedCommand = command
                .replace("{deity_name}", this.displayName)
                .replace("{deity_id}", this.id.toString())
                .replace("{stage}", extraParams);
            
            // Handle {player} replacement based on context
            if (command.startsWith("tellraw") || command.startsWith("say ") || command.startsWith("broadcast")) {
                // For messaging commands, keep player name in message text
                processedCommand = processedCommand.replace("{player}", player.getName().getString());
            } else {
                // For regular game commands (give, effect, title), use @s selector
                processedCommand = processedCommand.replace("{player}", "@s");
            }
            
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
        // Use the same capability access pattern as the rest of the codebase
        if (player instanceof ServerPlayer serverPlayer) {
            IReputation rep = serverPlayer.level().getCapability(IReputation.INSTANCE).orElse(null);
            if (rep != null) {
                double reputation = rep.getReputation(player.getUUID(), this.getId());
                // Debug logging to track reputation calls
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("🔍 REPUTATION DEBUG: Player {} with deity {} has reputation {}", 
                        player.getName().getString(), this.getId(), reputation);
                }
                return reputation;
            }
        }
        
        // Fallback to player capability if level access fails
        IReputation rep = player.getCapability(IReputation.INSTANCE).orElse(null);
        if (rep != null) {
            double reputation = rep.getReputation(player.getUUID(), this.getId());
            LOGGER.debug("🔍 REPUTATION DEBUG (fallback): Player {} with deity {} has reputation {}", 
                player.getName().getString(), this.getId(), reputation);
            return reputation;
        }
        
        LOGGER.warn("⚠️ IReputation capability not found for player {} when checking deity {} (tried both level and player access)", 
            player.getName().getString(), this.getId());
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
     * Gets the appropriate stage title for a given reputation level using the deity's progression stages
     * ✅ FIXED: Now uses the proper basic deity progression stages instead of AI behavior text
     */
    private String getStageForReputation(double reputation) {
        try {
            // Use the actual progression stages defined in the basic deity configuration
            Stage bestStage = null;
            
            // Find the highest stage the player qualifies for
            for (Stage stage : this.progression.getSteps().values()) {
                if (reputation >= stage.rep()) {
                    bestStage = stage;
                } else {
                    break; // Stages should be ordered by reputation requirement
                }
            }
            
            if (bestStage != null) {
                // 🎭 USE STORED TITLE FROM JSON, NOT STAGE ID
                String stageTitle = stageTitles.get(bestStage.id().getPath());
                if (stageTitle == null || stageTitle.isEmpty()) {
                    // Fallback to formatted stage ID if no title stored
                    stageTitle = formatStageIdAsTitle(bestStage.id().getPath());
                    LOGGER.warn("⚠️ No title found for stage {}, using formatted ID: '{}'", 
                        bestStage.id().getPath(), stageTitle);
                }
                
                LOGGER.debug("✅ Progression Stage: Reputation {} maps to stage '{}' (title: '{}') for deity {}", 
                    reputation, bestStage.id().getPath(), stageTitle, this.id);
                return stageTitle;
            } else {
                // Emergency fallback - return the first stage or a default
                if (!this.progression.getSteps().isEmpty()) {
                    Stage firstStage = this.progression.getSteps().values().iterator().next();
                    String firstStageTitle = stageTitles.get(firstStage.id().getPath());
                    if (firstStageTitle == null || firstStageTitle.isEmpty()) {
                        firstStageTitle = formatStageIdAsTitle(firstStage.id().getPath());
                    }
                    LOGGER.debug("⚠️ Fallback: Using first stage '{}' (title: '{}') for reputation {} on deity {}", 
                        firstStage.id().getPath(), firstStageTitle, reputation, this.id);
                    return firstStageTitle;
                } else {
                    LOGGER.warn("⚠️ No progression stages defined for deity {}, using generic fallback", this.id);
                    return "Initiate";
                }
            }
            
        } catch (Exception e) {
            LOGGER.error("❌ Error determining stage for reputation {} on deity {}: {}", 
                reputation, this.id, e.getMessage(), e);
            // Emergency fallback
            return "Initiate";
        }
    }
    
    /**
     * Gets the progression stages map for external access (properly reading from JSON data)
     * 🎯 CRITICAL: This method provides tier progression data to DeityChat system
     */
    public Map<String, Object> getProgressionStages() {
        Map<String, Object> stages = new HashMap<>();
        
        // Build stages map from actual progression data loaded from /deities/ JSON
        for (Stage stage : this.progression.getSteps().values()) {
            Map<String, Object> stageData = new HashMap<>();
            stageData.put("reputationRequired", stage.rep()); // The reputation threshold from JSON
            
            // Use stored title from JSON (set by DatapackDeityManager.loadProgression)
            String title = stageTitles.get(stage.id().getPath());
            if (title == null) {
                // Fallback: format stage ID nicely if no title found
                title = formatStageIdAsTitle(stage.id().getPath());
                LOGGER.warn("No title found for stage {}, using formatted ID: '{}'", stage.id().getPath(), title);
            }
            stageData.put("title", title);
            
            // Default description (could be enhanced later to read from JSON)
            stageData.put("description", "Tier in " + displayName + " devotion");
            stageData.put("isMajor", stage.major());
            
            stages.put(stage.id().getPath(), stageData);
            
            LOGGER.debug("🎯 Mapped progression stage: {} → reputation={}, title='{}'", 
                stage.id().getPath(), stage.rep(), title);
        }
        
        LOGGER.info("📊 Built progression stages map for deity {}: {} stages total", 
            getId(), stages.size());
        
        return stages;
    }
    
    /**
     * Formats stage ID as a readable title (fallback method)
     */
    private String formatStageIdAsTitle(String stageId) {
        return Arrays.stream(stageId.split("_"))
            .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase())
            .collect(Collectors.joining(" "));
    }
    
    /**
     * Gets the display name for a stage based on its ID
     */
    public String getStageDisplayName(String stageId) {
        // For now, just format the stage ID nicely
        return stageId.replace("_", " ").substring(0, 1).toUpperCase() + 
               stageId.replace("_", " ").substring(1).toLowerCase();
    }
    
    /**
     * Get player's magical progression level using JSON data only
     */
    public String getProgressionLevel(Player player) {
        double reputation = getPlayerReputation(player);
        int researchCount = getResearchCount(player);
        
        // Use JSON-loaded progression stages to determine level
        // Find the highest stage the player qualifies for
        String bestStageId = null;
        int highestQualifyingRep = -1;
        
        for (Stage stage : getProgression().getSteps().values()) {
            if (reputation >= stage.rep() && stage.rep() > highestQualifyingRep) {
                highestQualifyingRep = stage.rep();
                bestStageId = stage.id().getPath();
            }
        }
        
        // Convert stage ID to progression level terminology if needed
        if (bestStageId != null) {
            return bestStageId.toLowerCase();
        }
        
        return "unknown";
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
        // Use JSON-loaded progression data only - no hardcoded fallbacks
        // Find the highest stage the player qualifies for based on reputation
        String bestStageId = null;
        int highestQualifyingRep = -1;
        
        for (Stage stage : getProgression().getSteps().values()) {
            if (reputation >= stage.rep() && stage.rep() > highestQualifyingRep) {
                highestQualifyingRep = stage.rep();
                bestStageId = stage.id().getPath();
            }
        }
        
        return bestStageId != null ? bestStageId : "unknown";
    }
    
    /**
     * Get information about the next progression stage using JSON data only
     */
    public String getNextProgressionInfo(double reputation) {
        // Find next stage from JSON progression data
        Stage nextStage = null;
        int lowestHigherRep = Integer.MAX_VALUE;
        
        for (Stage stage : getProgression().getSteps().values()) {
            if (stage.rep() > reputation && stage.rep() < lowestHigherRep) {
                lowestHigherRep = stage.rep();
                nextStage = stage;
            }
        }
        
        if (nextStage != null) {
            double needed = nextStage.rep() - reputation;
            return String.format("%s at %d reputation (%.1f needed)", 
                nextStage.id().getPath(), nextStage.rep(), needed);
        }
        
        return "Maximum stage reached";
    }
}
