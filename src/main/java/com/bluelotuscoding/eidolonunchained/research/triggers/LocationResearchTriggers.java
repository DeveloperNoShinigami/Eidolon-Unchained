package com.bluelotuscoding.eidolonunchained.research.triggers;

import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import com.bluelotuscoding.eidolonunchained.data.ResearchDataManager;
import com.bluelotuscoding.eidolonunchained.research.ResearchEntry;
import com.bluelotuscoding.eidolonunchained.research.conditions.DimensionCondition;
import elucent.eidolon.registries.Registry;
import elucent.eidolon.registries.Researches;
import elucent.eidolon.api.research.Research;
import elucent.eidolon.util.KnowledgeUtil;
import elucent.eidolon.common.tile.ResearchTableTileEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles automatic research triggers based on biome and dimension changes.
 * This allows research to unlock automatically when players enter specific locations,
 * but only if they have note-taking tools in their inventory.
 */
@Mod.EventBusSubscriber(modid = "eidolonunchained")
public class LocationResearchTriggers {
    
    // Cache to track player locations and prevent spam
    private static final Map<String, ResourceLocation> playerLastDimension = new HashMap<>();
    private static final Map<String, ResourceLocation> playerLastBiome = new HashMap<>();
    private static final Map<String, Integer> playerTickCounter = new HashMap<>();
    
    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        
        ResourceLocation newDimension = player.level().dimension().location();
        String playerKey = player.getStringUUID();
        
        // Check if dimension actually changed
        ResourceLocation lastDimension = playerLastDimension.get(playerKey);
        if (newDimension.equals(lastDimension)) return;
        
        playerLastDimension.put(playerKey, newDimension);
        
        // Check for dimension-based research triggers
        checkDimensionResearchTriggers(player, newDimension);
    }
    
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;
        
        String playerKey = player.getStringUUID();
        
        // Only check biome every 40 ticks (2 seconds) to reduce performance impact
        int tickCount = playerTickCounter.getOrDefault(playerKey, 0);
        if (tickCount % 40 != 0) {
            playerTickCounter.put(playerKey, tickCount + 1);
            return;
        }
        playerTickCounter.put(playerKey, 0);
        
        // Get current biome
        ResourceLocation currentBiome = player.level().getBiome(player.blockPosition())
            .unwrapKey()
            .map(k -> k.location())
            .orElse(null);
            
        if (currentBiome == null) return;
        
        // Check if biome changed
        ResourceLocation lastBiome = playerLastBiome.get(playerKey);
        if (currentBiome.equals(lastBiome)) return;
        
        playerLastBiome.put(playerKey, currentBiome);
        
        // Check for biome-based research triggers
        checkBiomeResearchTriggers(player, currentBiome);
    }
    
    private static void checkDimensionResearchTriggers(ServerPlayer player, ResourceLocation dimension) {
        // Check all loaded research entries for dimension triggers
        for (ResearchEntry entry : ResearchDataManager.getLoadedResearchEntries().values()) {
            if (shouldTriggerForDimension(entry, dimension)) {
                tryTriggerResearch(player, entry.getId(), "dimension", dimension.toString());
            }
        }
    }
    
    private static void checkBiomeResearchTriggers(ServerPlayer player, ResourceLocation biome) {
        // Check all loaded research entries for biome triggers
        for (ResearchEntry entry : ResearchDataManager.getLoadedResearchEntries().values()) {
            if (shouldTriggerForBiome(entry, biome)) {
                tryTriggerResearch(player, entry.getId(), "biome", biome.toString());
            }
        }
    }
    
    private static boolean shouldTriggerForDimension(ResearchEntry entry, ResourceLocation dimension) {
        // Check if the research has dimension-based trigger conditions or tasks
        boolean hasCondition = entry.getConditions().stream().anyMatch(condition -> {
            if (condition instanceof DimensionCondition dimCondition) {
                return dimCondition.getDimension().equals(dimension);
            }
            return false;
        });
        
        // Also check if research name suggests dimension-based content
        String entryPath = entry.getId().getPath();
        boolean nameMatches = switch (dimension.toString()) {
            case "minecraft:nether" -> entryPath.contains("nether") || entryPath.contains("hell") || entryPath.contains("fire");
            case "minecraft:the_end" -> entryPath.contains("end") || entryPath.contains("void") || entryPath.contains("dragon");
            case "minecraft:overworld" -> entryPath.contains("overworld") || entryPath.contains("surface");
            default -> false;
        };
        
        return hasCondition || nameMatches;
    }
    
    private static boolean shouldTriggerForBiome(ResearchEntry entry, ResourceLocation biome) {
        // Check for specific biome mappings
        String entryPath = entry.getId().getPath();
        String biomeName = biome.getPath();
        
        return switch (biomeName) {
            case "desert" -> entryPath.contains("desert") || entryPath.contains("heat") || entryPath.contains("sand");
            case "jungle" -> entryPath.contains("jungle") || entryPath.contains("tropical");
            case "nether_wastes", "crimson_forest", "warped_forest", "soul_sand_valley", "basalt_deltas" -> 
                entryPath.contains("nether") || entryPath.contains("hell") || entryPath.contains("fire");
            case "end_highlands", "end_midlands", "end_barrens", "small_end_islands" -> 
                entryPath.contains("end") || entryPath.contains("void") || entryPath.contains("dragon");
            case "ocean", "deep_ocean", "warm_ocean", "cold_ocean" -> 
                entryPath.contains("ocean") || entryPath.contains("sea") || entryPath.contains("water");
            case "swamp" -> entryPath.contains("swamp") || entryPath.contains("marsh");
            case "taiga", "snowy_taiga" -> entryPath.contains("taiga") || entryPath.contains("cold") || entryPath.contains("snow");
            case "plains" -> entryPath.contains("plains") || entryPath.contains("grass");
            case "forest" -> entryPath.contains("forest") || entryPath.contains("tree") || entryPath.contains("wood");
            case "mountains" -> entryPath.contains("mountain") || entryPath.contains("peak") || entryPath.contains("cliff");
            default -> entryPath.contains(biomeName);
        };
    }
    
    private static void tryTriggerResearch(ServerPlayer player, ResourceLocation researchId, String triggerType, String location) {
        // Get the research entry to check for block requirements
        ResearchEntry entry = ResearchDataManager.getLoadedResearchEntries().get(researchId);
        if (entry == null) return;
        
        // Check if research has block triggers - if so, require manual interaction
        if (hasBlockTriggers(entry)) {
            // Alert player they need to interact with specific blocks using Eidolon's action bar
            Component message = Component.literal("You sense ancient knowledge in this ")
                .append(Component.literal(triggerType).withStyle(ChatFormatting.YELLOW))
                .append(Component.literal("... Investigate with "))
                .append(Component.literal("Note-Taking Tools").withStyle(ChatFormatting.AQUA))
                .append(Component.literal(" on relevant structures."));
            
            player.connection.send(new ClientboundSetActionBarTextPacket(message));
            return;
        }
        
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
        
        if (!hasNoteTakingTools) {
            // Alert player that they need note-taking tools using Eidolon's action bar
            Component message = Component.literal("You sense something significant about this ")
                .append(Component.literal(triggerType).withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(", but you need "))
                .append(Component.literal("Note-Taking Tools").withStyle(ChatFormatting.AQUA))
                .append(Component.literal(" to investigate further."));
            
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
        showEidolonStyleDiscovery(player, research, triggerType, location);
    }
    
    private static boolean hasBlockTriggers(ResearchEntry entry) {
        // Check if research has block-based triggers defined
        // This could be enhanced to read from JSON triggers array
        // For now, assume any research with "structure", "ruins", "altar" etc. needs block interaction
        String entryPath = entry.getId().getPath();
        return entryPath.contains("structure") || 
               entryPath.contains("ruins") || 
               entryPath.contains("altar") || 
               entryPath.contains("temple") ||
               entryPath.contains("shrine") ||
               entryPath.contains("monument");
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
    
    private static void showEidolonStyleDiscovery(ServerPlayer player, Research research, String triggerType, String location) {
        // Play discovery sound (same as Eidolon uses for research completion)
        player.level().playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, 
            SoundSource.PLAYERS, 1.0f, 1.2f);
        
        // Use Eidolon's action bar notification system (same as research/sign notifications)
        String locationName = location.substring(location.indexOf(':') + 1).replace("_", " ");
        Component message = Component.literal("📜 ").withStyle(ChatFormatting.GOLD)
            .append(Component.literal("Research Discovered").withStyle(ChatFormatting.YELLOW))
            .append(Component.literal(" in ").withStyle(ChatFormatting.GRAY))
            .append(Component.literal(locationName).withStyle(ChatFormatting.AQUA))
            .append(Component.literal("! "))
            .append(Component.literal(research.getName()).withStyle(ChatFormatting.GREEN));
        
        player.connection.send(new ClientboundSetActionBarTextPacket(message));
        
        System.out.println("Location-triggered research '" + research.getRegistryName() + 
            "' for player " + player.getName().getString() + " in " + location);
    }
    
    private static ResourceLocation convertToEidolonId(ResourceLocation ourId) {
        // Convert our research IDs to the format Eidolon expects
        return ourId;
    }
}
