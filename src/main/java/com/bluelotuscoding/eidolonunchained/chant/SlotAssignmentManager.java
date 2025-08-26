package com.bluelotuscoding.eidolonunchained.chant;

import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import com.bluelotuscoding.eidolonunchained.config.ChantCastingConfig;
import com.mojang.logging.LogUtils;
import elucent.eidolon.api.spells.Sign;
import elucent.eidolon.registries.Signs;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

/**
 * Manages slot assignments for both individual signs and full chants
 * Supports flexible assignment based on casting mode
 */
public class SlotAssignmentManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    public enum SlotType {
        SIGN,      // Slot contains a single sign (e.g., "eidolon:wicked")
        CHANT      // Slot contains a full chant (e.g., "eidolonunchained:shadow_communion")
    }
    
    public static class SlotAssignment {
        public final SlotType type;
        public final ResourceLocation id;
        public final String displayName;
        
        public SlotAssignment(SlotType type, ResourceLocation id, String displayName) {
            this.type = type;
            this.id = id;
            this.displayName = displayName;
        }
    }
    
    /**
     * Assign a sign to a specific slot
     */
    public static boolean assignSignToSlot(ServerPlayer player, int slot, ResourceLocation signId) {
        if (slot < 1 || slot > 4) {
            player.sendSystemMessage(Component.literal("§cInvalid slot number. Use 1-4."));
            return false;
        }
        
        // Verify the sign exists
        Sign sign = Signs.find(signId);
        if (sign == null) {
            player.sendSystemMessage(Component.literal("§cUnknown sign: " + signId));
            return false;
        }
        
        // Store assignment
        CompoundTag playerData = player.getPersistentData();
        CompoundTag modData = playerData.getCompound(EidolonUnchained.MODID);
        CompoundTag slots = modData.getCompound("slot_assignments");
        
        slots.putString("slot_" + slot + "_type", "SIGN");
        slots.putString("slot_" + slot + "_id", signId.toString());
        slots.putString("slot_" + slot + "_name", getSignDisplayName(signId));
        
        modData.put("slot_assignments", slots);
        playerData.put(EidolonUnchained.MODID, modData);
        
        player.sendSystemMessage(Component.literal("§6Assigned sign '" + getSignDisplayName(signId) + "' to slot " + slot));
        LOGGER.info("Player {} assigned sign {} to slot {}", player.getName().getString(), signId, slot);
        return true;
    }
    
    /**
     * Assign a chant to a specific slot
     */
    public static boolean assignChantToSlot(ServerPlayer player, int slot, ResourceLocation chantId) {
        if (slot < 1 || slot > 4) {
            player.sendSystemMessage(Component.literal("§cInvalid slot number. Use 1-4."));
            return false;
        }
        
        // Verify the chant exists
        DatapackChant chant = DatapackChantManager.getChant(chantId);
        if (chant == null) {
            player.sendSystemMessage(Component.literal("§cUnknown chant: " + chantId));
            return false;
        }
        
        // Store assignment
        CompoundTag playerData = player.getPersistentData();
        CompoundTag modData = playerData.getCompound(EidolonUnchained.MODID);
        CompoundTag slots = modData.getCompound("slot_assignments");
        
        slots.putString("slot_" + slot + "_type", "CHANT");
        slots.putString("slot_" + slot + "_id", chantId.toString());
        slots.putString("slot_" + slot + "_name", chant.getName());
        
        modData.put("slot_assignments", slots);
        playerData.put(EidolonUnchained.MODID, modData);
        
        player.sendSystemMessage(Component.literal("§6Assigned chant '" + chant.getName() + "' to slot " + slot));
        LOGGER.info("Player {} assigned chant {} to slot {}", player.getName().getString(), chantId, slot);
        return true;
    }
    
    /**
     * Get the assignment for a specific slot
     */
    public static SlotAssignment getSlotAssignment(ServerPlayer player, int slot) {
        CompoundTag playerData = player.getPersistentData();
        CompoundTag modData = playerData.getCompound(EidolonUnchained.MODID);
        CompoundTag slots = modData.getCompound("slot_assignments");
        
        String typeStr = slots.getString("slot_" + slot + "_type");
        String idStr = slots.getString("slot_" + slot + "_id");
        String name = slots.getString("slot_" + slot + "_name");
        
        if (typeStr.isEmpty() || idStr.isEmpty()) {
            return null;
        }
        
        try {
            SlotType type = SlotType.valueOf(typeStr);
            ResourceLocation id = new ResourceLocation(idStr);
            return new SlotAssignment(type, id, name);
        } catch (Exception e) {
            LOGGER.warn("Invalid slot assignment for player {} slot {}: {}", player.getName().getString(), slot, e.getMessage());
            return null;
        }
    }
    
    /**
     * Clear a slot assignment
     */
    public static boolean clearSlot(ServerPlayer player, int slot) {
        if (slot < 1 || slot > 4) {
            player.sendSystemMessage(Component.literal("§cInvalid slot number. Use 1-4."));
            return false;
        }
        
        CompoundTag playerData = player.getPersistentData();
        CompoundTag modData = playerData.getCompound(EidolonUnchained.MODID);
        CompoundTag slots = modData.getCompound("slot_assignments");
        
        slots.remove("slot_" + slot + "_type");
        slots.remove("slot_" + slot + "_id");
        slots.remove("slot_" + slot + "_name");
        
        modData.put("slot_assignments", slots);
        playerData.put(EidolonUnchained.MODID, modData);
        
        player.sendSystemMessage(Component.literal("§6Cleared slot " + slot));
        return true;
    }
    
    /**
     * Activate a slot based on current casting mode
     */
    public static boolean activateSlot(ServerPlayer player, int slot, String castingMode) {
        SlotAssignment assignment = getSlotAssignment(player, slot);
        if (assignment == null) {
            player.sendSystemMessage(Component.literal("§cNo assignment in slot " + slot + ". Use /chant assign-sign or /chant assign-chant to configure."));
            return false;
        }
        
        ChantCastingConfig.CastingMode mode = ChantCastingConfig.CastingMode.valueOf(castingMode.toUpperCase());
        
        switch (mode) {
            case INDIVIDUAL_SIGNS -> {
                if (assignment.type == SlotType.SIGN) {
                    return castIndividualSign(player, assignment);
                } else {
                    player.sendSystemMessage(Component.literal("§cSlot " + slot + " contains a chant, but INDIVIDUAL_SIGNS mode requires signs. Use /chant assign-sign to assign a sign."));
                    return false;
                }
            }
            case FULL_CHANT -> {
                if (assignment.type == SlotType.CHANT) {
                    return castFullChant(player, assignment);
                } else {
                    player.sendSystemMessage(Component.literal("§cSlot " + slot + " contains a sign, but FULL_CHANT mode requires chants. Use /chant assign-chant to assign a chant."));
                    return false;
                }
            }
            case HYBRID -> {
                // In hybrid mode, support both
                if (assignment.type == SlotType.SIGN) {
                    return castIndividualSign(player, assignment);
                } else {
                    return castFullChant(player, assignment);
                }
            }
            default -> {
                player.sendSystemMessage(Component.literal("§cUnknown casting mode: " + castingMode));
                return false;
            }
        }
    }
    
    private static boolean castIndividualSign(ServerPlayer player, SlotAssignment assignment) {
        Sign sign = Signs.find(assignment.id);
        if (sign == null) {
            player.sendSystemMessage(Component.literal("§cSign no longer exists: " + assignment.id));
            return false;
        }
        
        player.sendSystemMessage(Component.literal("§6Casting sign: " + assignment.displayName));
        
        // TODO: Integrate with Eidolon's sign casting system
        // This would involve creating a SignSequence and triggering the sign
        LOGGER.info("Player {} cast sign {} from slot", player.getName().getString(), assignment.id);
        
        return true;
    }
    
    private static boolean castFullChant(ServerPlayer player, SlotAssignment assignment) {
        DatapackChant chant = DatapackChantManager.getChant(assignment.id);
        if (chant == null) {
            player.sendSystemMessage(Component.literal("§cChant no longer exists: " + assignment.id));
            return false;
        }
        
        // Use the existing ChantSlotManager logic for full chant casting
        return ChantSlotManager.executeFullChant(player, chant);
    }
    
    private static String getSignDisplayName(ResourceLocation signId) {
        // Convert sign ID to display name
        String path = signId.getPath();
        return path.substring(0, 1).toUpperCase() + path.substring(1);
    }
    
    /**
     * List all available signs for assignment
     */
    public static void listAvailableSigns(ServerPlayer player) {
        player.sendSystemMessage(Component.literal("§6Available Signs:"));
        player.sendSystemMessage(Component.literal("§f• eidolon:wicked - Wicked"));
        player.sendSystemMessage(Component.literal("§f• eidolon:sacred - Sacred"));
        player.sendSystemMessage(Component.literal("§f• eidolon:blood - Blood"));
        player.sendSystemMessage(Component.literal("§f• eidolon:soul - Soul"));
        player.sendSystemMessage(Component.literal("§f• eidolon:mind - Mind"));
        player.sendSystemMessage(Component.literal("§f• eidolon:flame - Flame"));
        player.sendSystemMessage(Component.literal("§f• eidolon:harmony - Harmony"));
        player.sendSystemMessage(Component.literal("§f• eidolon:death - Death"));
        player.sendSystemMessage(Component.literal("§f• eidolon:magic - Magic"));
        player.sendSystemMessage(Component.literal("§f• eidolon:warding - Warding"));
    }
}
