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
    
    @SubscribeEvent
    public static void onBlockInteract(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        
        // Check if player is holding note-taking tools
        ItemStack heldItem = player.getItemInHand(event.getHand());
        if (heldItem.getItem() != Registry.NOTETAKING_TOOLS.get()) {
            return; // Only trigger when using note-taking tools
        }
        
        Block targetBlock = event.getLevel().getBlockState(event.getPos()).getBlock();
        ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(targetBlock);
        if (blockId == null) return;
        
        // Check all research entries for block interaction triggers
        for (ResearchEntry entry : ResearchDataManager.getLoadedResearchEntries().values()) {
            if (isBlockInTriggers(entry, blockId)) {
                if (tryTriggerResearch(player, entry.getId(), blockId.toString(), new CompoundTag(), "interact")) {
                    event.setCancellationResult(InteractionResult.SUCCESS);
                    event.setCanceled(true);
                    break; // Only trigger one research per interaction
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
    
    private static ResourceLocation convertToEidolonId(ResourceLocation ourId) {
        // Convert our research IDs to the format Eidolon expects
        return ourId;
    }
}
