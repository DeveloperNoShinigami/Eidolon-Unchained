package com.bluelotuscoding.eidolonunchained.deity;

import elucent.eidolon.api.deity.Deity;
import elucent.eidolon.capability.IReputation;
import elucent.eidolon.registries.Signs;
import elucent.eidolon.util.KnowledgeUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import com.mojang.logging.LogUtils;

import java.util.*;

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
        // Apply datapack-defined rewards
        String lockString = lock.toString();
        List<String> rewards = stageRewards.get(lockString);
        
        if (rewards != null) {
            for (String reward : rewards) {
                applyReward(player, reward);
            }
        }
        
        // Send unlock message
        if (player instanceof ServerPlayer serverPlayer) {
            String message = String.format("§6[%s]§r You have unlocked: %s", displayName, lock.getPath());
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
}
