package com.bluelotuscoding.eidolonunchained.research.triggers;

import com.bluelotuscoding.eidolonunchained.data.ResearchDataManager;
import com.bluelotuscoding.eidolonunchained.research.ResearchEntry;
import elucent.eidolon.registries.Registry;
import elucent.eidolon.registries.Researches;
import elucent.eidolon.api.research.Research;
import elucent.eidolon.util.KnowledgeUtil;
import elucent.eidolon.common.tile.ResearchTableTileEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Handles research discovery triggered by right-clicking entities or blocks.
 * Works with the triggers array in research JSON files - supports both entity and block interactions.
 * Uses Eidolon's action bar notification system for consistency.
 */
@Mod.EventBusSubscriber(modid = "eidolonunchained")
public class InteractionResearchTriggers {
    
    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        
        // Check if player is holding note-taking tools
        ItemStack heldItem = player.getItemInHand(event.getHand());
        if (heldItem.getItem() != Registry.NOTETAKING_TOOLS.get()) {
            return; // Only trigger when using note-taking tools
        }
        
        Entity targetEntity = event.getTarget();
        ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(targetEntity.getType());
        if (entityId == null) return;
        
        // Get entity NBT data
        CompoundTag entityNBT = new CompoundTag();
        targetEntity.saveWithoutId(entityNBT);
        
        // Check all research entries for entity interaction triggers
        for (ResearchEntry entry : ResearchDataManager.getLoadedResearchEntries().values()) {
            if (isEntityInTriggers(entry, entityId)) {
                if (tryTriggerResearch(player, entry.getId(), entityId.toString(), entityNBT, "interact")) {
                    event.setCancellationResult(InteractionResult.SUCCESS);
                    event.setCanceled(true);
                    break; // Only trigger one research per interaction
                }
            }
        }
    }
    
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onBlockInteract(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        
        System.out.println("DEBUG: Block interaction detected by " + player.getName().getString());
        
        // Check if player is holding note-taking tools
        ItemStack heldItem = player.getItemInHand(event.getHand());
        System.out.println("DEBUG: Player holding: " + heldItem.getItem());
        if (heldItem.getItem() != Registry.NOTETAKING_TOOLS.get()) {
            System.out.println("DEBUG: Not holding note-taking tools, skipping research check");
            return; // Only trigger when using note-taking tools
        }
        
        Block targetBlock = event.getLevel().getBlockState(event.getPos()).getBlock();
        ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(targetBlock);
        System.out.println("DEBUG: Right-clicked block: " + blockId);
        if (blockId == null) return;
        
        // Check all research entries for block interaction triggers
        for (ResearchEntry entry : ResearchDataManager.getLoadedResearchEntries().values()) {
            if (isBlockInTriggers(entry, blockId)) {
                System.out.println("DEBUG: Found matching research for block " + blockId + ": " + entry.getId());
                if (tryTriggerResearch(player, entry.getId(), blockId.toString(), new CompoundTag(), "interact")) {
                    System.out.println("DEBUG: Successfully triggered research " + entry.getId() + " for block " + blockId);
                    // For container blocks, don't cancel the event to allow GUI opening
                    if (isContainerBlock(targetBlock)) {
                        System.out.println("DEBUG: Container block detected, allowing GUI to open");
                        // Let the container open normally after research discovery
                        break;
                    } else {
                        System.out.println("DEBUG: Non-container block, canceling interaction");
                        // For non-container blocks, cancel the event as before
                        event.setCancellationResult(InteractionResult.SUCCESS);
                        event.setCanceled(true);
                        break;
                    }
                } else {
                    System.out.println("DEBUG: Failed to trigger research " + entry.getId() + " for block " + blockId);
                }
            }
        }
    }
    
    /**
     * Check if the entity is listed in the research's triggers array
     */
    private static boolean isEntityInTriggers(ResearchEntry entry, ResourceLocation entityId) {
        if (entry.getAdditionalData() == null || !entry.getAdditionalData().has("triggers")) {
            return false;
        }
        
        String entityIdString = entityId.toString();
        var triggersArray = entry.getAdditionalData().getAsJsonArray("triggers");
        
        for (int i = 0; i < triggersArray.size(); i++) {
            String trigger = triggersArray.get(i).getAsString();
            if (trigger.equals(entityIdString)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Check if the block is listed in the research's triggers array
     */
    private static boolean isBlockInTriggers(ResearchEntry entry, ResourceLocation blockId) {
        if (entry.getAdditionalData() == null || !entry.getAdditionalData().has("triggers")) {
            return false;
        }
        
        String blockIdString = blockId.toString();
        var triggersArray = entry.getAdditionalData().getAsJsonArray("triggers");
        
        for (int i = 0; i < triggersArray.size(); i++) {
            String trigger = triggersArray.get(i).getAsString();
            if (trigger.equals(blockIdString)) {
                return true;
            }
        }
        
        return false;
    }
    
    private static boolean tryTriggerResearch(ServerPlayer player, ResourceLocation researchId, String targetId, CompoundTag targetNBT, String triggerType) {
        // Convert our research ID to Eidolon research ID format
        ResourceLocation eidolonResearchId = convertToEidolonId(researchId);
        
        Research research = Researches.find(eidolonResearchId);
        if (research == null) {
            System.out.println("Could not find Eidolon research for ID: " + eidolonResearchId);
            return false;
        }
        
        // Check if player already knows this research
        if (KnowledgeUtil.knowsResearch(player, eidolonResearchId)) {
            return false; // Player already has this research
        }
        
        // Get the note-taking tools from player's hand
        ItemStack noteTakingTools = null;
        if (player.getMainHandItem().getItem() == Registry.NOTETAKING_TOOLS.get()) {
            noteTakingTools = player.getMainHandItem();
        } else if (player.getOffhandItem().getItem() == Registry.NOTETAKING_TOOLS.get()) {
            noteTakingTools = player.getOffhandItem();
        }
        
        if (noteTakingTools == null) {
            return false; // Shouldn't happen since we check above, but safety first
        }
        
        // Create research notes automatically
        createResearchNotes(player, research, noteTakingTools);
        
        // Show discovery message using Eidolon's notification system
        String displayName = getDisplayName(targetId, targetNBT);
        showEidolonStyleDiscovery(player, research, displayName, triggerType);
        
        return true;
    }
    
    private static String getDisplayName(String targetId, CompoundTag targetNBT) {
        // Check for custom name first
        if (targetNBT.contains("CustomName")) {
            return targetNBT.getString("CustomName").replace("\"", "");
        }
        
        // Otherwise use ID name, cleaned up
        return targetId.substring(targetId.indexOf(':') + 1).replace("_", " ");
    }
    
    private static void createResearchNotes(ServerPlayer player, Research research, ItemStack noteTakingTools) {
        ServerLevel serverLevel = (ServerLevel) player.level();
        ItemStack notes = new ItemStack(Registry.RESEARCH_NOTES.get(), 1);
        var tag = notes.getOrCreateTag();
        tag.putString("research", research.getRegistryName().toString());
        tag.putInt("stepsDone", 0);
        tag.putLong("worldSeed", ResearchTableTileEntity.SEED + 978060631 * serverLevel.getSeed());
        
        // Consume one note-taking tool
        noteTakingTools.shrink(1);
        
        // Give notes to player
        if (!player.getInventory().add(notes)) {
            player.drop(notes, false);
        }
    }
    
    private static void showEidolonStyleDiscovery(ServerPlayer player, Research research, String targetDisplayName, String triggerType) {
        // Play discovery sound (same as Eidolon uses for research completion)
        player.level().playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, 
            SoundSource.PLAYERS, 1.0f, 1.2f);
        
        // Use Eidolon's action bar notification system
        Component message = Component.literal("📖 ").withStyle(ChatFormatting.AQUA)
            .append(Component.literal("Research Discovered").withStyle(ChatFormatting.YELLOW))
            .append(Component.literal(" by studying ").withStyle(ChatFormatting.GRAY))
            .append(Component.literal(targetDisplayName).withStyle(ChatFormatting.GREEN))
            .append(Component.literal("! "))
            .append(Component.literal(research.getName()).withStyle(ChatFormatting.GOLD));
        
        player.connection.send(new ClientboundSetActionBarTextPacket(message));
        
        System.out.println("Interaction-triggered research '" + research.getRegistryName() + 
            "' for player " + player.getName().getString() + " by interacting with " + targetDisplayName);
    }
    
    /**
     * Check if a block is a container that opens a GUI when right-clicked
     * This helps determine whether to cancel the interaction event or allow it to continue
     */
    private static boolean isContainerBlock(Block block) {
        ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(block);
        if (blockId == null) return false;
        
        String blockIdString = blockId.toString();
        
        // Common container blocks that should open GUIs
        return blockIdString.equals("minecraft:crafting_table") ||
               blockIdString.equals("minecraft:chest") ||
               blockIdString.equals("minecraft:ender_chest") ||
               blockIdString.equals("minecraft:trapped_chest") ||
               blockIdString.equals("minecraft:furnace") ||
               blockIdString.equals("minecraft:blast_furnace") ||
               blockIdString.equals("minecraft:smoker") ||
               blockIdString.equals("minecraft:brewing_stand") ||
               blockIdString.equals("minecraft:enchanting_table") ||
               blockIdString.equals("minecraft:anvil") ||
               blockIdString.equals("minecraft:chipped_anvil") ||
               blockIdString.equals("minecraft:damaged_anvil") ||
               blockIdString.equals("minecraft:smithing_table") ||
               blockIdString.equals("minecraft:cartography_table") ||
               blockIdString.equals("minecraft:fletching_table") ||
               blockIdString.equals("minecraft:grindstone") ||
               blockIdString.equals("minecraft:loom") ||
               blockIdString.equals("minecraft:stonecutter") ||
               blockIdString.equals("minecraft:barrel") ||
               blockIdString.equals("minecraft:hopper") ||
               blockIdString.equals("minecraft:dropper") ||
               blockIdString.equals("minecraft:dispenser") ||
               blockIdString.equals("minecraft:shulker_box") ||
               blockIdString.contains("shulker_box") || // Colored shulker boxes
               // Eidolon containers
               blockIdString.equals("eidolon:research_table") ||
               blockIdString.equals("eidolon:crucible") ||
               blockIdString.equals("eidolon:soul_enchanter") ||
               blockIdString.equals("eidolon:altar") ||
               blockIdString.equals("eidolon:brazier") ||
               blockIdString.equals("eidolon:worktable");
    }
    
    private static ResourceLocation convertToEidolonId(ResourceLocation ourId) {
        // Convert our research IDs to the format Eidolon expects
        return ourId;
    }
}
