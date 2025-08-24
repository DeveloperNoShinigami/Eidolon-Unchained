package com.bluelotuscoding.eidolonunchained.integration;

import com.bluelotuscoding.eidolonunchained.command.UnifiedCommands;
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
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Main integration point for the AI Deity system
 */
@Mod.EventBusSubscriber(modid = EidolonUnchained.MODID)
public class AIDeityIntegration {
    private static final Logger LOGGER = LogUtils.getLogger();
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
     * Both managers register themselves via their own @SubscribeEvent methods
     * to ensure proper singleton instance management and loading order
     */
    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        LOGGER.info("AI deity data managers will register themselves via their own event handlers");
        // DatapackDeityManager and AIDeityManager both register themselves
        // This ensures proper singleton instances and avoids duplicate registrations
    }
    
    /**
     * Register commands
     */
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        LOGGER.info("Registering unified commands");
        UnifiedCommands.register(event.getDispatcher());
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
