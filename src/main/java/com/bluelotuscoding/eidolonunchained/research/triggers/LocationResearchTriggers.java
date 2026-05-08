package com.bluelotuscoding.eidolonunchained.research.triggers;

import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import com.bluelotuscoding.eidolonunchained.research.triggers.data.ResearchTrigger;
import com.bluelotuscoding.eidolonunchained.ai.PlayerContextTracker;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;

/**
 * Handles location-based research triggers (dimension, biome, structure) loaded from JSON
 * SMART: Uses AI system's existing biome tracking to avoid duplicate ticking!
 */
@Mod.EventBusSubscriber(modid = EidolonUnchained.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class LocationResearchTriggers {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    // Note: Research trigger tracking is now handled by PlayerContextTracker for persistence

    // Ensure biome listener is registered even if MOD setup event isn't delivered on this bus
    static {
        try {
            PlayerContextTracker.addBiomeChangeListener(LocationResearchTriggers::onBiomeChange);
        } catch (Throwable t) {
            // Safe-guard: avoid class init failure
        }
    }

    /**
     * SMART: Register with AI system's biome tracking to avoid duplicate ticking
     */
    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        // Register our biome change listener with the AI system
        PlayerContextTracker.addBiomeChangeListener(LocationResearchTriggers::onBiomeChange);
        LOGGER.info("LocationResearchTriggers: Registered biome change listener with AI system");
    }

    /**
     * Called by AI system when player biome changes (no ticking needed!)
     */
    private static void onBiomeChange(ServerPlayer player, String newBiome) {
        LOGGER.debug("Player {} biome changed to: {} (via AI callback)", player.getName().getString(), newBiome);
        checkBiomeTriggersForPlayer(player, newBiome);
    }
    
            /**
     * Handle dimension changes - still needed for proper event-driven approach
     */
    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        
        LOGGER.debug("Player {} changed dimension to: {}", player.getName().getString(), event.getTo());
        checkLocationTriggers(player);
    }

    /**
     * Handle player login - check all triggers on login
     */
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        
        LOGGER.debug("Player {} logged in, checking location triggers", player.getName().getString());
        checkLocationTriggers(player);
    }
    
    /**
     * Check all location-based triggers for a player
     */
    private static void checkLocationTriggers(ServerPlayer player) {
        Map<String, List<ResearchTrigger>> allTriggers = ResearchTriggerLoader.getTriggersForAllResearch();
        
        // Early exit if no triggers are configured
        if (allTriggers.isEmpty()) {
            return;
        }
        
        // Check if player has notetaking tools (required for research discovery)
        if (!hasNotetakingTools(player)) {
            return; // No tools, no research discovery
        }
        
        for (Map.Entry<String, List<ResearchTrigger>> entry : allTriggers.entrySet()) {
            String researchId = entry.getKey();

            for (ResearchTrigger trigger : entry.getValue()) {
                if (shouldCheckTrigger(trigger, player)) {
                    // Check max_found limit using persistent tracking
                    long currentCount = PlayerContextTracker.getTriggeredResearchCount(player, researchId);

                    if (currentCount < trigger.getMaxFound()) {
                        // Consume notetaking tool before giving research
                        if (consumeNotetakingTool(player)) {
                            giveResearchNote(player, researchId);

                            // Track this trigger using persistent system
                            PlayerContextTracker.trackTriggeredResearch(player, researchId);

                            LOGGER.debug("Player {} triggered location research '{}' ({}/{} times)",
                                player.getName().getString(), researchId, currentCount + 1, trigger.getMaxFound());
                        } else {
                            LOGGER.warn("Failed to consume notetaking tool for player {}, research discovery cancelled",
                                player.getName().getString());
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Check if a trigger should activate based on location conditions
     */
    private static boolean shouldCheckTrigger(ResearchTrigger trigger, ServerPlayer player) {
        try {
            // Early exit if no location conditions
            if (trigger.getEntity() != null || trigger.getBlock() != null || trigger.getRitual() != null) {
                return false; // These are handled by other trigger classes
            }
            
            // Check dimension triggers
            if (trigger.getDimension() != null) {
                boolean dimensionMatches = checkDimensionTrigger(player, trigger.getDimension().toString());
                if (dimensionMatches) return true;
            }
            
            // Check biome triggers  
            if (trigger.getBiome() != null) {
                boolean biomeMatches = checkBiomeTrigger(player, trigger.getBiome().toString());
                if (biomeMatches) return true;
            }
            
            // Check structure triggers
            if (trigger.getStructure() != null) {
                boolean structureMatches = checkStructureTrigger(player, trigger.getStructure().toString());
                if (structureMatches) return true;
            }
            
            return false;
            
        } catch (Exception e) {
            LOGGER.error("Failed to check trigger conditions for research: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Give a research note to the player instead of directly granting research
     */
    private static void giveResearchNote(ServerPlayer player, String researchId) {
        try {
            // Get the research note item from Eidolon's registry
            var researchNoteItem = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(
                new ResourceLocation("eidolon", "research_notes"));
            
            if (researchNoteItem == null) {
                LOGGER.error("Could not find research_notes item from Eidolon");
                return;
            }
            
            // Normalize research id to a namespaced ID expected by Eidolon
            String namespacedId = researchId.contains(":")
                ? researchId
                : new ResourceLocation(EidolonUnchained.MODID, researchId).toString();

            // Create ItemStack with proper NBT
            ItemStack researchNote = new ItemStack(researchNoteItem);
            CompoundTag tag = researchNote.getOrCreateTag();
            tag.putString("research", namespacedId);
            tag.putInt("stepsDone", 0);
            
            // Add worldSeed for research table compatibility
            ServerLevel serverLevel = (ServerLevel) player.level();
                long worldSeed = elucent.eidolon.common.tile.ResearchTableTileEntity.SEED
                    + 978060631L * serverLevel.getSeed();
            tag.putLong("worldSeed", worldSeed);
            
            // Give to player
            if (!player.addItem(researchNote)) {
                player.drop(researchNote, false); // Drop if inventory full
            }
            
            LOGGER.info("Gave research note for '{}' to player {}", namespacedId, player.getName().getString());
            
        } catch (Exception e) {
            LOGGER.error("Failed to give research note for '{}': {}", researchId, e.getMessage());
        }
    }
    
    /**
     * Check dimension condition
     */
    private static boolean checkDimensionTrigger(ServerPlayer player, String dimension) {
        try {
            String currentDimension = player.level().dimension().location().toString();
            return currentDimension.equals(dimension);
        } catch (Exception e) {
            LOGGER.error("Failed to check dimension trigger: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Check biome condition  
     */
    private static boolean checkBiomeTrigger(ServerPlayer player, String biome) {
        try {
            BlockPos playerPos = player.blockPosition();
            Holder<Biome> biomeHolder = player.level().getBiome(playerPos);
            ResourceLocation biomeLocation = biomeHolder.unwrapKey()
                .map(key -> key.location())
                .orElse(null);
                
            if (biomeLocation == null) {
                return false;
            }
            
            String biomeName = biomeLocation.toString();
            return biomeName.equals(biome);
        } catch (Exception e) {
            LOGGER.error("Failed to check biome trigger: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Check structure condition
     */
    private static boolean checkStructureTrigger(ServerPlayer player, String structure) {
        try {
            BlockPos playerPos = player.blockPosition();
            ServerLevel level = (ServerLevel) player.level();
            
            ResourceLocation structureLocation = new ResourceLocation(structure);
            Structure targetStructure = level.registryAccess()
                .registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE)
                .get(structureLocation);
                
            if (targetStructure == null) {
                return false;
            }
            
            var structureResult = level.structureManager().getStructureWithPieceAt(playerPos, targetStructure);
            return structureResult.isValid();
            
        } catch (Exception e) {
            LOGGER.error("Failed to check structure trigger: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Check biome triggers for a specific player (called by AI callback)
     */
    private static void checkBiomeTriggersForPlayer(ServerPlayer player, String currentBiome) {
        Map<String, List<ResearchTrigger>> allTriggers = ResearchTriggerLoader.getTriggersForAllResearch();
        
        if (allTriggers.isEmpty() || !hasNotetakingTools(player)) {
            return;
        }
        
        LOGGER.debug("Player {} checking biome triggers for: {}", player.getName().getString(), currentBiome);

        for (Map.Entry<String, List<ResearchTrigger>> entry : allTriggers.entrySet()) {
            String researchId = entry.getKey();

            for (ResearchTrigger trigger : entry.getValue()) {
                // Check if this is a biome trigger that matches the current biome
                if (trigger.getBiome() != null &&
                    trigger.getBiome().toString().equals(currentBiome)) {

                    // Check max_found limit using persistent tracking
                    long currentCount = PlayerContextTracker.getTriggeredResearchCount(player, researchId);

                    if (currentCount < trigger.getMaxFound()) {
                        // Consume notetaking tool before giving research
                        if (consumeNotetakingTool(player)) {
                            giveResearchNote(player, researchId);

                            // Track this trigger using persistent system
                            PlayerContextTracker.trackTriggeredResearch(player, researchId);

                            LOGGER.debug("Player {} triggered biome research '{}' in {} ({}/{} times)",
                                player.getName().getString(), researchId, currentBiome, currentCount + 1, trigger.getMaxFound());
                        } else {
                            LOGGER.warn("Failed to consume notetaking tool for player {}, biome research discovery cancelled",
                                player.getName().getString());
                        }
                    }
                }
            }
        }
    }

    /**
     * Check if player has notetaking tools required for research discovery
     */
    private static boolean hasNotetakingTools(ServerPlayer player) {
        try {
            // Check for Eidolon notetaking tools only
            return player.getInventory().hasAnyMatching(stack -> {
                String itemName = stack.getItem().toString().toLowerCase();
                return itemName.contains("notetaking");
            });
        } catch (Exception e) {
            LOGGER.error("Failed to check notetaking tools: {}", e.getMessage());
            return false; // Default to no tools if check fails
        }
    }
    
    /**
     * Consume one notetaking tool from player's inventory
     */
    private static boolean consumeNotetakingTool(ServerPlayer player) {
        try {
            var inventory = player.getInventory();
            
            // Find and consume one notetaking tool
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                ItemStack stack = inventory.getItem(i);
                if (!stack.isEmpty()) {
                    String itemName = stack.getItem().toString().toLowerCase();
                    if (itemName.contains("notetaking")) {
                        stack.shrink(1); // Remove 1 count
                        LOGGER.debug("Consumed 1 notetaking tool from player {}", player.getName().getString());
                        return true;
                    }
                }
            }
            
            LOGGER.warn("Failed to find notetaking tool to consume for player {}", player.getName().getString());
            return false;
        } catch (Exception e) {
            LOGGER.error("Failed to consume notetaking tool: {}", e.getMessage());
            return false;
        }
    }
}
