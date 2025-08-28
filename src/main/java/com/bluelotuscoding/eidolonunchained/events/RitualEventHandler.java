package com.bluelotuscoding.eidolonunchained.events;

import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import com.bluelotuscoding.eidolonunchained.ai.PlayerContextTracker;
import com.bluelotuscoding.eidolonunchained.research.triggers.ResearchTriggerLoader;
import com.bluelotuscoding.eidolonunchained.research.triggers.ItemRequirementChecker;
import com.bluelotuscoding.eidolonunchained.research.triggers.data.ResearchTrigger;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.common.MinecraftForge;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Handles ritual completion detection and fires custom events
 */
@Mod.EventBusSubscriber(modid = EidolonUnchained.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class RitualEventHandler {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    // Track active rituals by their block positions
    private static final Map<BlockPos, RitualTracker> activeRituals = new ConcurrentHashMap<>();
    
    private static class RitualTracker {
        public final ResourceLocation ritualId;
        public final long startTime;
        public boolean wasActive;
        
        public RitualTracker(ResourceLocation ritualId) {
            this.ritualId = ritualId;
            this.startTime = System.currentTimeMillis();
            this.wasActive = true;
        }
    }
    
    @SubscribeEvent
    public static void onRitualComplete(RitualCompleteEvent event) {
        // Handle our custom ritual completion event
        if (event.isSuccessful()) {
            PlayerContextTracker.onRitualComplete(event.getPlayer(), event.getRitualId(), true);
            
            // Trigger research advancement for ritual completion
            triggerRitualResearch(event.getPlayer(), event.getRitualId());
            
            // Send notification to player
            event.getPlayer().sendSystemMessage(
                net.minecraft.network.chat.Component.literal(
                    "§6[Ritual Complete] §e" + event.getRitualId().getPath().replace("_", " ") + " §6completed!"
                )
            );
        }
    }
    
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            // Check every 20 ticks (1 second) for ritual completions
            if (event.getServer().getTickCount() % 20 == 0) {
                checkForRitualCompletions(event.getServer());
            }
        }
    }
    
    private static void checkForRitualCompletions(net.minecraft.server.MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            // Use reflection to access loaded block entities
            try {
                // Try to access the block entity ticker
                Class<?> levelClass = level.getClass();
                java.lang.reflect.Field tickerField = null;
                
                // Search for the field that contains block entities
                for (java.lang.reflect.Field field : levelClass.getDeclaredFields()) {
                    if (field.getType().getSimpleName().contains("BlockEntityTicker") || 
                        field.getName().contains("blockEntity")) {
                        field.setAccessible(true);
                        tickerField = field;
                        break;
                    }
                }
                
                if (tickerField != null) {
                    Object ticker = tickerField.get(level);
                    if (ticker != null) {
                        // Try to get tickingBlockEntities from the ticker
                        java.lang.reflect.Field entitiesField = null;
                        for (java.lang.reflect.Field field : ticker.getClass().getDeclaredFields()) {
                            if (field.getName().contains("tickingBlockEntities") || 
                                field.getName().contains("blockEntities")) {
                                field.setAccessible(true);
                                entitiesField = field;
                                break;
                            }
                        }
                        
                        if (entitiesField != null) {
                            @SuppressWarnings("unchecked")
                            Iterable<BlockEntity> entities = (Iterable<BlockEntity>) entitiesField.get(ticker);
                            
                            for (BlockEntity blockEntity : entities) {
                                if (blockEntity.getClass().getName().contains("BrazierTileEntity")) {
                                    checkBrazierForRitualCompletion(blockEntity, level);
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // Silently fail reflection - this is expected in development
                LOGGER.debug("Could not access block entities via reflection: {}", e.getMessage());
            }
        }
    }
    
    private static void checkBrazierForRitualCompletion(BlockEntity blockEntity, ServerLevel level) {
        BlockPos pos = blockEntity.getBlockPos();
        
        try {
            // Use reflection to access the ritual field
            java.lang.reflect.Field ritualField = blockEntity.getClass().getDeclaredField("ritual");
            ritualField.setAccessible(true);
            Object ritual = ritualField.get(blockEntity);
            
            java.lang.reflect.Field burningField = blockEntity.getClass().getDeclaredField("burning");
            burningField.setAccessible(true);
            boolean burning = burningField.getBoolean(blockEntity);
            
            if (ritual != null && burning) {
                // Ritual is active
                java.lang.reflect.Method getRegistryName = ritual.getClass().getMethod("getRegistryName");
                ResourceLocation ritualId = (ResourceLocation) getRegistryName.invoke(ritual);
                
                if (ritualId != null) {
                    if (!activeRituals.containsKey(pos)) {
                        activeRituals.put(pos, new RitualTracker(ritualId));
                    }
                }
            } else {
                // Check if ritual just completed
                RitualTracker tracker = activeRituals.get(pos);
                if (tracker != null && tracker.wasActive) {
                    // Ritual completed!
                    findNearbyPlayersAndFireEvent(level, pos, tracker.ritualId);
                    activeRituals.remove(pos);
                }
            }
            
        } catch (Exception e) {
            // Reflection failed, skip this brazier
            // This is expected when dealing with obfuscated code
        }
    }
    
    private static void findNearbyPlayersAndFireEvent(ServerLevel level, BlockPos pos, ResourceLocation ritualId) {
        // Find players within 10 blocks of the completed ritual
        List<ServerPlayer> nearbyPlayers = level.getEntitiesOfClass(
            ServerPlayer.class, 
            new net.minecraft.world.phys.AABB(pos).inflate(10.0D)
        );
        
        for (ServerPlayer player : nearbyPlayers) {
            // Fire our custom event
            RitualCompleteEvent ritualEvent = new RitualCompleteEvent(player, ritualId, true);
            MinecraftForge.EVENT_BUS.post(ritualEvent);
        }
    }
    
    /**
     * Manual method to fire ritual completion for testing/admin commands
     */
    public static void fireRitualCompletion(ServerPlayer player, ResourceLocation ritualId) {
        RitualCompleteEvent event = new RitualCompleteEvent(player, ritualId, true);
        MinecraftForge.EVENT_BUS.post(event);
    }
    
    /**
     * Trigger research discovery based on ritual completion
     */
    private static void triggerRitualResearch(ServerPlayer player, ResourceLocation ritualId) {
        try {
            // Check all research files for ritual triggers using the research-based mapping
            for (Map.Entry<String, List<ResearchTrigger>> entry : ResearchTriggerLoader.getTriggersForAllResearch().entrySet()) {
                String researchId = entry.getKey();
                
                for (ResearchTrigger trigger : entry.getValue()) {
                    if ("ritual".equals(trigger.getType()) && 
                        trigger.getRitual() != null && 
                        trigger.getRitual().equals(ritualId)) {
                        
                        // Check item requirements
                        if (ItemRequirementChecker.checkItemRequirements(player, trigger.getItemRequirements())) {
                            elucent.eidolon.util.KnowledgeUtil.grantResearchNoToast(player, 
                                new ResourceLocation("eidolonunchained", researchId));
                            LOGGER.info("Granted research '{}' to player {} after completing ritual '{}'", 
                                researchId, player.getName().getString(), ritualId);
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            LOGGER.error("Failed to grant research for ritual completion", e);
        }
    }
}
