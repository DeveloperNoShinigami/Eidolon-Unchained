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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Handles research discovery triggered by killing entities.
 * Uses dynamic entity ID matching and NBT data for flexible trigger conditions.
 * All notifications use Eidolon's action bar system for consistency.
 */
@Mod.EventBusSubscriber(modid = "eidolonunchained")
public class KillResearchTriggers {
    
    @SubscribeEvent
    public static void onEntityKilled(LivingDeathEvent event) {
        // Only process if killed by a player
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        
        LivingEntity killedEntity = event.getEntity();
        ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(killedEntity.getType());
        if (entityId == null) return;
        
        // Get entity NBT data for additional matching
        CompoundTag entityNBT = new CompoundTag();
        killedEntity.saveWithoutId(entityNBT);
        
        // Check all research entries for entity kill triggers
        for (ResearchEntry entry : ResearchDataManager.getLoadedResearchEntries().values()) {
            if (isEntityInTriggers(entry, entityId)) {
                tryTriggerResearch(player, entry.getId(), entityId, entityNBT, "kill");
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
    
    private static boolean hasNBTMatch(String entryPath, CompoundTag entityNBT) {
        // Check custom name
        if (entityNBT.contains("CustomName")) {
            String customName = entityNBT.getString("CustomName").toLowerCase()
                .replace("\"", "").replace(" ", "_");
            if (entryPath.contains(customName)) {
                return true;
            }
        }
        
        // Check variant data
        if (entityNBT.contains("Variant")) {
            String variant = entityNBT.getString("Variant").toLowerCase();
            if (entryPath.contains(variant)) {
                return true;
            }
        }
        
        // Check profession (for villagers)
        if (entityNBT.contains("VillagerData")) {
            CompoundTag villagerData = entityNBT.getCompound("VillagerData");
            if (villagerData.contains("profession")) {
                String profession = villagerData.getString("profession").toLowerCase();
                if (entryPath.contains(profession.replace("minecraft:", ""))) {
                    return true;
                }
            }
        }
        
        // Check age/baby status
        if (entityNBT.contains("IsBaby") && entityNBT.getBoolean("IsBaby")) {
            if (entryPath.contains("baby") || entryPath.contains("young")) {
                return true;
            }
        }
        
        // Check tags (for custom tagged entities)
        if (entityNBT.contains("Tags")) {
            String tags = entityNBT.toString().toLowerCase();
            String[] pathParts = entryPath.split("_");
            for (String part : pathParts) {
                if (part.length() > 3 && tags.contains(part)) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    private static boolean hasEntityCategoryMatch(String entryPath, ResourceLocation entityId) {
        String entityName = entityId.getPath().toLowerCase();
        
        // Check broad entity categories that might be relevant for research
        if (isUndeadEntity(entityName) && (entryPath.contains("undead") || entryPath.contains("necro") || entryPath.contains("death"))) {
            return true;
        }
        
        if (isNetherEntity(entityName) && (entryPath.contains("nether") || entryPath.contains("hell") || entryPath.contains("fire"))) {
            return true;
        }
        
        if (isEndEntity(entityName) && (entryPath.contains("end") || entryPath.contains("void") || entryPath.contains("ender"))) {
            return true;
        }
        
        if (isMagicalEntity(entityName) && (entryPath.contains("magic") || entryPath.contains("spell") || entryPath.contains("witch"))) {
            return true;
        }
        
        if (isConstructEntity(entityName) && (entryPath.contains("golem") || entryPath.contains("construct") || entryPath.contains("artificial"))) {
            return true;
        }
        
        if (isAquaticEntity(entityName) && (entryPath.contains("ocean") || entryPath.contains("sea") || entryPath.contains("water"))) {
            return true;
        }
        
        return false;
    }
    
    private static boolean isUndeadEntity(String entityName) {
        return entityName.contains("zombie") || entityName.contains("skeleton") || 
               entityName.contains("wither") || entityName.contains("phantom");
    }
    
    private static boolean isNetherEntity(String entityName) {
        return entityName.contains("blaze") || entityName.contains("ghast") || 
               entityName.contains("piglin") || entityName.contains("hoglin") ||
               entityName.contains("strider") || entityName.contains("magma");
    }
    
    private static boolean isEndEntity(String entityName) {
        return entityName.contains("enderman") || entityName.contains("ender_dragon") ||
               entityName.contains("shulker") || entityName.contains("endermite");
    }
    
    private static boolean isMagicalEntity(String entityName) {
        return entityName.contains("witch") || entityName.contains("evoker") ||
               entityName.contains("vex") || entityName.contains("illusioner");
    }
    
    private static boolean isConstructEntity(String entityName) {
        return entityName.contains("golem") || entityName.contains("snow_golem");
    }
    
    private static boolean isAquaticEntity(String entityName) {
        return entityName.contains("guardian") || entityName.contains("squid") ||
               entityName.contains("dolphin") || entityName.contains("fish");
    }
    
    private static void tryTriggerResearch(ServerPlayer player, ResourceLocation researchId, ResourceLocation entityId, CompoundTag entityNBT, String triggerType) {
        // Check if player has note-taking tools
        boolean hasNoteTakingTools = false;
        ItemStack noteTakingTools = ItemStack.EMPTY;
        
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() == Registry.NOTETAKING_TOOLS.get()) {
                hasNoteTakingTools = true;
                noteTakingTools = stack;
                break;
            }
        }
        
        String entityDisplayName = getEntityDisplayName(entityId, entityNBT);
        
        if (!hasNoteTakingTools) {
            // Alert player that they need note-taking tools using Eidolon's action bar
            Component message = Component.literal("Defeating this ")
                .append(Component.literal(entityDisplayName).withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(" reveals hidden knowledge... You need "))
                .append(Component.literal("Note-Taking Tools").withStyle(ChatFormatting.AQUA))
                .append(Component.literal(" to record it!"));
            
            player.connection.send(new ClientboundSetActionBarTextPacket(message));
            return;
        }
        
        // Convert our research ID to Eidolon research ID format
        ResourceLocation eidolonResearchId = convertToEidolonId(researchId);
        
        Research research = Researches.find(eidolonResearchId);
        if (research == null) {
            System.out.println("Could not find Eidolon research for ID: " + eidolonResearchId);
            return;
        }
        
        // Check if player already knows this research
        if (KnowledgeUtil.knowsResearch(player, eidolonResearchId)) {
            return; // Player already has this research
        }
        
        // Create research notes automatically
        createResearchNotes(player, research, noteTakingTools);
        
        // Show discovery message using Eidolon's notification system
        showEidolonStyleDiscovery(player, research, entityDisplayName, triggerType);
    }
    
    private static String getEntityDisplayName(ResourceLocation entityId, CompoundTag entityNBT) {
        // Check for custom name first
        if (entityNBT.contains("CustomName")) {
            return entityNBT.getString("CustomName").replace("\"", "");
        }
        
        // Otherwise use entity type name, cleaned up
        return entityId.getPath().replace("_", " ");
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
    
    private static void showEidolonStyleDiscovery(ServerPlayer player, Research research, String entityDisplayName, String triggerType) {
        // Play discovery sound (same as Eidolon uses for research completion)
        player.level().playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, 
            SoundSource.PLAYERS, 1.0f, 1.2f);
        
        // Use Eidolon's action bar notification system (same as research/sign notifications)
        Component message = Component.literal("⚔️ ").withStyle(ChatFormatting.DARK_RED)
            .append(Component.literal("Research Discovered").withStyle(ChatFormatting.YELLOW))
            .append(Component.literal(" from defeating ").withStyle(ChatFormatting.GRAY))
            .append(Component.literal(entityDisplayName).withStyle(ChatFormatting.RED))
            .append(Component.literal("! "))
            .append(Component.literal(research.getName()).withStyle(ChatFormatting.GREEN));
        
        player.connection.send(new ClientboundSetActionBarTextPacket(message));
        
        System.out.println("Kill-triggered research '" + research.getRegistryName() + 
            "' for player " + player.getName().getString() + " after defeating " + entityDisplayName);
    }
    
    private static ResourceLocation convertToEidolonId(ResourceLocation ourId) {
        // Convert our research IDs to the format Eidolon expects
        return ourId;
    }
}
