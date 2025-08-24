package com.bluelotuscoding.eidolonunchained.integration;

import com.bluelotuscoding.eidolonunchained.command.PrayerCommands;
import com.bluelotuscoding.eidolonunchained.data.DatapackDeityManager;
import com.bluelotuscoding.eidolonunchained.ai.AIDeityManager;
import com.bluelotuscoding.eidolonunchained.prayer.PrayerSystem;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.common.MinecraftForge;
import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Main integration point for the AI Deity system
 */
@Mod.EventBusSubscriber(modid = EidolonUnchained.MODID)
public class AIDeityIntegration {
    private static final Logger LOGGER = LogManager.getLogger();
    private static ScheduledExecutorService cleanupExecutor;
    
    /**
     * Initialize the AI deity system
     */
    public static void init(FMLCommonSetupEvent event) {
        LOGGER.info("Initializing AI Deity Integration");
        
        // Register prayer system event handler
        MinecraftForge.EVENT_BUS.register(PrayerSystem.class);
        
        // Start cleanup task for prayer cooldowns
        cleanupExecutor = Executors.newScheduledThreadPool(1);
        cleanupExecutor.scheduleAtFixedRate(
            PrayerSystem::cleanupCooldowns,
            1, // Initial delay
            1, // Period
            TimeUnit.HOURS
        );
        
        LOGGER.info("AI Deity Integration initialized");
    }
    
    /**
     * Register reload listeners for datapack content
     * Order matters: DatapackDeityManager must load before AIDeityManager
     */
    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        LOGGER.info("Registering AI deity data managers");
        // Register DatapackDeityManager first to load base deities
        event.addListener(new DatapackDeityManager());
        // Register AIDeityManager second to link AI configs to loaded deities
        event.addListener(new AIDeityManager());
    }
    
    /**
     * Register commands
     */
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        LOGGER.info("Registering prayer commands");
        PrayerCommands.register(event.getDispatcher());
    }
    
    /**
     * Shutdown cleanup
     */
    public static void shutdown() {
        if (cleanupExecutor != null && !cleanupExecutor.isShutdown()) {
            cleanupExecutor.shutdown();
            try {
                if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    cleanupExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                cleanupExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}
