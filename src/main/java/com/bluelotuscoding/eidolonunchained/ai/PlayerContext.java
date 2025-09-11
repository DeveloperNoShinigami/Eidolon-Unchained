package com.bluelotuscoding.eidolonunchained.ai;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.block.Block;
import net.minecraft.resources.ResourceLocation;
import com.bluelotuscoding.eidolonunchained.deity.DatapackDeity;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Comprehensive context information about a player for AI decision making
 * The AI should be fully game-aware with access to all relevant game state
 */
public class PlayerContext {
    public String playerName;
    public double reputation;
    public int researchCount;
    public String progressionLevel; // beginner, novice, intermediate, advanced, master
    
    // ========== LOCATION & ENVIRONMENT ==========
    public String location;
    public String biome;
    public String dimension;
    public String timeOfDay; // dawn, day, dusk, night, midnight
    public String weather; // clear, rain, thunder
    public int lightLevel;
    public boolean underground;
    public int yLevel;
    
    // ========== PLAYER STATE ==========
    public int health;
    public int maxHealth;  
    public int hunger;
    public int xpLevel;
    public boolean isOnFire;
    public boolean isInWater;
    public boolean isFlying;
    public boolean isSneaking;
    public boolean isSwimming;
    public List<String> activeEffects = new ArrayList<>();
    
    // ========== INVENTORY & EQUIPMENT ==========
    public String inventorySummary;
    public List<String> equippedArmor = new ArrayList<>();
    public String mainHandItem;
    public String offHandItem;
    public int emptySlots;
    public List<String> notableItems = new ArrayList<>();
    
    // ========== NEARBY ENVIRONMENT ==========
    public List<String> nearbyBlocks = new ArrayList<>(); // 5x5x5 area
    public List<String> nearbyEntities = new ArrayList<>(); // 10 block radius
    public List<String> nearbyStructures = new ArrayList<>();
    public boolean nearWater;
    public boolean nearLava;
    public boolean nearFire;
    public boolean hasNearbyBed;
    public boolean hasNearbyWorkstation;
    
    // ========== DEITY SPECIFIC ==========
    public List<String> recentActions = new ArrayList<>();
    public String lastChantPerformed;
    public String lastPrayerType;
    public long timeSinceLastPrayer;
    
    public PlayerContext(String playerName) {
        this.playerName = playerName;
    }
    
    public PlayerContext(ServerPlayer player, DatapackDeity deity) {
        this.playerName = player.getName().getString();
        this.reputation = deity.getPlayerReputation(player);
        this.progressionLevel = deity.getProgressionLevel(player);
        this.researchCount = deity.getResearchCount(player);
        
        // ========== LOCATION & ENVIRONMENT ==========
        BlockPos playerPos = player.blockPosition();
        this.location = playerPos.toString();
        this.biome = player.level().getBiome(playerPos).unwrapKey()
            .map(key -> key.location().toString()).orElse("unknown");
        this.dimension = player.level().dimension().location().toString();
        this.yLevel = playerPos.getY();
        this.underground = playerPos.getY() < 50;
        this.lightLevel = player.level().getBrightness(net.minecraft.world.level.LightLayer.BLOCK, playerPos);
        
        // Initialize time context
        long dayTime = player.level().getDayTime() % 24000;
        if (dayTime < 6000) {
            this.timeOfDay = "day";
        } else if (dayTime < 12000) {
            this.timeOfDay = "afternoon"; 
        } else if (dayTime < 13000) {
            this.timeOfDay = "dusk";
        } else if (dayTime < 22000) {
            this.timeOfDay = "night";
        } else {
            this.timeOfDay = "dawn";
        }
        
        // Initialize weather context
        if (player.level().isThundering()) {
            this.weather = "thunder";
        } else if (player.level().isRaining()) {
            this.weather = "rain";
        } else {
            this.weather = "clear";
        }
        
        // ========== PLAYER STATE ==========
        this.health = (int) player.getHealth();
        this.maxHealth = (int) player.getMaxHealth();
        this.hunger = player.getFoodData().getFoodLevel();
        this.xpLevel = player.experienceLevel;
        this.isOnFire = player.isOnFire();
        this.isInWater = player.isInWater();
        this.isFlying = player.getAbilities().flying;
        this.isSneaking = player.isShiftKeyDown();
        this.isSwimming = player.isSwimming();
        
        // Active effects
        this.activeEffects = player.getActiveEffects().stream()
            .map(effect -> effect.getEffect().getDescriptionId())
            .collect(Collectors.toList());
        
        // ========== INVENTORY & EQUIPMENT ==========
        this.mainHandItem = player.getMainHandItem().getDescriptionId();
        this.offHandItem = player.getOffhandItem().getDescriptionId();
        
        // Equipped armor
        for (ItemStack armor : player.getArmorSlots()) {
            if (!armor.isEmpty()) {
                this.equippedArmor.add(armor.getDescriptionId());
            }
        }
        
        // Inventory analysis
        int totalSlots = 0;
        int emptySlots = 0;
        List<String> significantItems = new ArrayList<>();
        
        for (ItemStack stack : player.getInventory().items) {
            totalSlots++;
            if (stack.isEmpty()) {
                emptySlots++;
            } else {
                // Track notable items (tools, weapons, rare items)
                String itemName = stack.getDescriptionId();
                if (itemName.contains("sword") || itemName.contains("pickaxe") || itemName.contains("axe") || 
                    itemName.contains("shovel") || itemName.contains("eidolon") || stack.isEnchanted()) {
                    significantItems.add(itemName + "x" + stack.getCount());
                }
            }
        }
        
        this.emptySlots = emptySlots;
        this.notableItems = significantItems;
        this.inventorySummary = String.format("%d/%d slots used, notable items: %s", 
            totalSlots - emptySlots, totalSlots, String.join(", ", significantItems));
        
        // ========== NEARBY ENVIRONMENT ==========
        scanNearbyEnvironment(player, playerPos);
    }
    
    /**
     * Scan the environment around the player for contextual awareness
     */
    private void scanNearbyEnvironment(ServerPlayer player, BlockPos center) {
        Set<String> nearbyBlockTypes = new HashSet<>();
        boolean foundWater = false, foundLava = false, foundFire = false;
        boolean foundBed = false, foundWorkstation = false;
        
        // Scan 5x5x5 area around player
        for (int x = -2; x <= 2; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -2; z <= 2; z++) {
                    BlockPos pos = center.offset(x, y, z);
                    BlockState state = player.level().getBlockState(pos);
                    Block block = state.getBlock();
                    
                    String blockName = block.getDescriptionId();
                    if (!blockName.contains("air")) {
                        nearbyBlockTypes.add(blockName);
                        
                        // Check for specific important blocks
                        if (blockName.contains("water")) foundWater = true;
                        if (blockName.contains("lava")) foundLava = true;
                        if (blockName.contains("fire")) foundFire = true;
                        if (blockName.contains("bed")) foundBed = true;
                        if (blockName.contains("crafting") || blockName.contains("furnace") || 
                            blockName.contains("anvil") || blockName.contains("enchanting")) {
                            foundWorkstation = true;
                        }
                    }
                }
            }
        }
        
        this.nearbyBlocks = new ArrayList<>(nearbyBlockTypes).subList(0, Math.min(10, nearbyBlockTypes.size()));
        this.nearWater = foundWater;
        this.nearLava = foundLava;
        this.nearFire = foundFire;
        this.hasNearbyBed = foundBed;
        this.hasNearbyWorkstation = foundWorkstation;
        
        // Scan for nearby entities (simplified)
        this.nearbyEntities = player.level().getEntities(player, player.getBoundingBox().inflate(10.0))
            .stream()
            .map(entity -> entity.getType().getDescriptionId())
            .distinct()
            .limit(5)
            .collect(Collectors.toList());
    }
}
