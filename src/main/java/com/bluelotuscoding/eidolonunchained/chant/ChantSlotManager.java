package com.bluelotuscoding.eidolonunchained.chant;

import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import com.bluelotuscoding.eidolonunchained.config.EidolonUnchainedConfig;
import com.mojang.logging.LogUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages flexible chant slot assignments for players.
 * Allows any datapack chant to be assigned to any of the 4 available slots.
 * Supports multiple casting modes: full chant, individual signs, or hybrid.
 * Handles persistence and validation of chant casting.
 */
@Mod.EventBusSubscriber(modid = EidolonUnchained.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ChantSlotManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String NBT_CHANT_SLOTS = "EidolonUnchained_ChantSlots";
    
    // Cache for player chant assignments
    private static final Map<String, Map<Integer, ResourceLocation>> playerChantAssignments = new HashMap<>();
    
    /**
     * Assigns a chant to a specific slot for a player
     */
    public static boolean assignChantToSlot(ServerPlayer player, int slot, ResourceLocation chantId) {
        if (slot < 1 || slot > 4) {
            player.sendSystemMessage(Component.literal("§cInvalid slot number. Must be 1-4."));
            return false;
        }
        
        // Verify the chant exists
        DatapackChant chant = DatapackChantManager.getChant(chantId);
        if (chant == null) {
            player.sendSystemMessage(Component.literal("§cChant not found: " + chantId));
            return false;
        }
        
        // Store in player NBT
        CompoundTag playerData = player.getPersistentData();
        CompoundTag modData = playerData.getCompound(EidolonUnchained.MODID);
        CompoundTag chantSlots = modData.getCompound(NBT_CHANT_SLOTS);
        
        chantSlots.putString("slot_" + slot, chantId.toString());
        modData.put(NBT_CHANT_SLOTS, chantSlots);
        playerData.put(EidolonUnchained.MODID, modData);
        
        // Update cache
        String playerUUID = player.getUUID().toString();
        playerChantAssignments.computeIfAbsent(playerUUID, k -> new HashMap<>()).put(slot, chantId);
        
        player.sendSystemMessage(Component.literal("§6Assigned '" + chant.getName() + "' to slot " + slot));
        LOGGER.info("Player {} assigned chant {} to slot {}", player.getName().getString(), chantId, slot);
        
        return true;
    }
    
    /**
     * Activates a chant slot for a player
     * Supports different casting modes: full chant, individual signs, or hybrid
     */
    public static boolean activateChantSlot(ServerPlayer player, int slot) {
        ResourceLocation chantId = getChantInSlot(player, slot);
        if (chantId == null) {
            return false;
        }
        
        DatapackChant chant = DatapackChantManager.getChant(chantId);
        if (chant == null) {
            player.sendSystemMessage(Component.literal("§cChant no longer exists: " + chantId));
            return false;
        }
        
        // Check if chant requires effigy (deity-linked chants)
        if (chant.getLinkedDeity() != null) {
            if (!isNearEffigy(player)) {
                player.sendSystemMessage(Component.literal("§cYou must be near an effigy to cast deity chants!"));
                return false;
            }
        }
        
        // Check casting mode configuration
        EidolonUnchainedConfig.ChantCastingMode castingMode = EidolonUnchainedConfig.COMMON.chantCastingMode.get();
        
        switch (castingMode) {
            case FULL_CHANT:
                return executeFullChant(player, chant);
            case INDIVIDUAL_SIGNS:
                return startSignSequence(player, chant);
            case HYBRID:
                // Default to sign sequence (like codex), but allow full chant with special modifier
                // TODO: Check for modifier key (shift, ctrl, etc.) to switch to full chant mode
                return startSignSequence(player, chant);
            default:
                return startSignSequence(player, chant);
        }
    }
    
    /**
     * Execute the full chant immediately (default behavior)
     */
    private static boolean executeFullChant(ServerPlayer player, DatapackChant chant) {
        try {
            // Execute the chant spell directly
            DatapackChantSpell spell = DatapackChantManager.getSpellForChant(chant.getId());
            if (spell != null) {
                spell.cast(player.level(), player.blockPosition(), player);
                player.sendSystemMessage(Component.literal("§6Executed chant: " + chant.getName()));
                return true;
            } else {
                player.sendSystemMessage(Component.literal("§cNo spell found for chant: " + chant.getName()));
                return false;
            }
        } catch (Exception e) {
            LOGGER.error("Failed to execute full chant {}: {}", chant.getId(), e.getMessage());
            player.sendSystemMessage(Component.literal("§cFailed to execute chant: " + e.getMessage()));
            return false;
        }
    }
    
    /**
     * Start sign sequence casting (like in codex)
     */
    private static boolean startSignSequence(ServerPlayer player, DatapackChant chant) {
        // Open sign sequence interface for this chant
        player.sendSystemMessage(Component.literal("§6Starting chant: " + chant.getName()));
        player.sendSystemMessage(Component.literal("§7Draw the sign sequence: " + formatSignSequence(chant.getSignSequence())));
        
        // Store the active chant for validation when signs are drawn
        CompoundTag playerData = player.getPersistentData();
        CompoundTag modData = playerData.getCompound(EidolonUnchained.MODID);
        modData.putString("active_chant", chant.getId().toString());
        modData.putLong("chant_start_time", System.currentTimeMillis());
        playerData.put(EidolonUnchained.MODID, modData);
        
        return true;
    }
    
    /**
     * Gets the chant assigned to a specific slot
     */
    public static ResourceLocation getChantInSlot(ServerPlayer player, int slot) {
        String playerUUID = player.getUUID().toString();
        
        // Check cache first
        Map<Integer, ResourceLocation> assignments = playerChantAssignments.get(playerUUID);
        if (assignments != null && assignments.containsKey(slot)) {
            return assignments.get(slot);
        }
        
        // Load from NBT
        CompoundTag playerData = player.getPersistentData();
        CompoundTag modData = playerData.getCompound(EidolonUnchained.MODID);
        CompoundTag chantSlots = modData.getCompound(NBT_CHANT_SLOTS);
        
        String chantIdString = chantSlots.getString("slot_" + slot);
        if (chantIdString.isEmpty()) {
            return null;
        }
        
        ResourceLocation chantId = new ResourceLocation(chantIdString);
        
        // Update cache
        playerChantAssignments.computeIfAbsent(playerUUID, k -> new HashMap<>()).put(slot, chantId);
        
        return chantId;
    }
    
    /**
     * Gets all chant assignments for a player as a formatted string
     */
    public static String getPlayerChantAssignments(ServerPlayer player) {
        StringBuilder sb = new StringBuilder();
        for (int slot = 1; slot <= 4; slot++) {
            ResourceLocation chantId = getChantInSlot(player, slot);
            if (chantId != null) {
                DatapackChant chant = DatapackChantManager.getChant(chantId);
                String chantName = chant != null ? chant.getName() : chantId.toString();
                sb.append("Slot ").append(slot).append(": ").append(chantName).append("\n");
            } else {
                sb.append("Slot ").append(slot).append(": Empty\n");
            }
        }
        return sb.toString();
    }
    
    /**
     * Clears a specific chant slot
     */
    public static boolean clearChantSlot(ServerPlayer player, int slot) {
        if (slot < 1 || slot > 4) {
            return false;
        }
        
        // Remove from NBT
        CompoundTag playerData = player.getPersistentData();
        CompoundTag modData = playerData.getCompound(EidolonUnchained.MODID);
        CompoundTag chantSlots = modData.getCompound(NBT_CHANT_SLOTS);
        
        chantSlots.remove("slot_" + slot);
        modData.put(NBT_CHANT_SLOTS, chantSlots);
        playerData.put(EidolonUnchained.MODID, modData);
        
        // Update cache
        String playerUUID = player.getUUID().toString();
        Map<Integer, ResourceLocation> assignments = playerChantAssignments.get(playerUUID);
        if (assignments != null) {
            assignments.remove(slot);
        }
        
        player.sendSystemMessage(Component.literal("§6Cleared slot " + slot));
        return true;
    }
    
    /**
     * Checks if player is near an effigy (required for deity chants)
     */
    private static boolean isNearEffigy(ServerPlayer player) {
        Level world = player.level();
        BlockPos playerPos = player.blockPosition();
        
        // Search in a 16x16x16 area around the player
        for (int x = -8; x <= 8; x++) {
            for (int y = -8; y <= 8; y++) {
                for (int z = -8; z <= 8; z++) {
                    BlockPos checkPos = playerPos.offset(x, y, z);
                    String blockName = world.getBlockState(checkPos).getBlock().getDescriptionId();
                    
                    // Check for Eidolon effigy blocks
                    if (blockName.contains("effigy")) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    /**
     * Formats a sign sequence for display
     */
    private static String formatSignSequence(java.util.List<ResourceLocation> signIds) {
        if (signIds == null || signIds.isEmpty()) {
            return "None";
        }
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < signIds.size(); i++) {
            if (i > 0) sb.append(" → ");
            sb.append(signIds.get(i).getPath().replace("_", " "));
        }
        return sb.toString();
    }
    
    /**
     * Load player chant assignments when they join
     */
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            String playerUUID = player.getUUID().toString();
            Map<Integer, ResourceLocation> assignments = new HashMap<>();
            
            CompoundTag playerData = player.getPersistentData();
            CompoundTag modData = playerData.getCompound(EidolonUnchained.MODID);
            CompoundTag chantSlots = modData.getCompound(NBT_CHANT_SLOTS);
            
            for (int slot = 1; slot <= 4; slot++) {
                String chantIdString = chantSlots.getString("slot_" + slot);
                if (!chantIdString.isEmpty()) {
                    assignments.put(slot, new ResourceLocation(chantIdString));
                }
            }
            
            playerChantAssignments.put(playerUUID, assignments);
            LOGGER.info("Loaded {} chant assignments for player {}", assignments.size(), player.getName().getString());
        }
    }
    
    /**
     * Clear cache when player leaves
     */
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            String playerUUID = player.getUUID().toString();
            playerChantAssignments.remove(playerUUID);
        }
    }
}
