package com.bluelotuscoding.eidolonunchained;

import com.bluelotuscoding.eidolonunchained.command.UnifiedCommands;
import com.bluelotuscoding.eidolonunchained.config.EidolonUnchainedConfig;
import com.bluelotuscoding.eidolonunchained.data.CodexDataManager;
import com.bluelotuscoding.eidolonunchained.data.ResearchDataManager;
import com.bluelotuscoding.eidolonunchained.data.EidolonResearchDataManager;
import com.bluelotuscoding.eidolonunchained.data.DatapackDeityManager;
import com.bluelotuscoding.eidolonunchained.chant.DatapackChantManager;
import com.bluelotuscoding.eidolonunchained.integration.ModIntegration;
import com.bluelotuscoding.eidolonunchained.integration.EidolonVersionDetection;
import com.bluelotuscoding.eidolonunchained.integration.AIDeityIntegration;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(EidolonUnchained.MODID)
public class EidolonUnchained
{
    // Define mod id in a common place for everything to reference
    public static final String MODID = "eidolonunchained";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();
    
    // Create a Deferred Register to hold Blocks which will all be registered under the "eidolonunchained" namespace
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    // Create a Deferred Register to hold Items which will all be registered under the "eidolonunchained" namespace
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    // Create a Deferred Register to hold CreativeModeTabs which will all be registered under the "eidolonunchained" namespace
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public EidolonUnchained()
    {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register the Deferred Register to the mod event bus so blocks get registered
        BLOCKS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so items get registered
        ITEMS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so tabs get registered
        CREATIVE_MODE_TABS.register(modEventBus);

        // Register our unified configuration
        EidolonUnchainedConfig.register();

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);
        
        // Detect Eidolon version capabilities and log results
        EidolonVersionDetection.logFeatureDetection();
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
        // Some common setup code
        LOGGER.info("Eidolon Unchained is loading!");
        LOGGER.info("Expanding the world of Eidolon with new chapters, rituals, and mystical content...");
        
        // TEMPORARILY DISABLE networking system to isolate login issues
        // com.bluelotuscoding.eidolonunchained.network.EidolonUnchainedNetworking.register();
        // LOGGER.info("Eidolon Unchained networking system initialized");
        LOGGER.info("Eidolon Unchained networking system disabled for testing");
        
        // Initialize data managers for codex and research extensions
        CodexDataManager.init();
        ResearchDataManager.init();
        EidolonResearchDataManager.getInstance(); // Initialize the Eidolon research data manager
        
        // Initialize mod integrations
        ModIntegration.init();
        
        // Initialize AI deity system
        AIDeityIntegration.init(event);
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {
        // Do something when the server starts
        LOGGER.info("Eidolon Unchained server starting...");
        
        LOGGER.info("About to log loaded data...");
        // Log our loaded data
        CodexDataManager.logLoadedData();
        ResearchDataManager.logLoadedData();
        
        LOGGER.info("About to attempt integration...");
        // Now inject our custom entries into Eidolon's systems
        try {
            LOGGER.info("Calling attemptIntegrationIfNeeded...");
            com.bluelotuscoding.eidolonunchained.integration.EidolonCodexIntegration.attemptIntegrationIfNeeded();
            LOGGER.info("Integration call completed successfully");
        } catch (Exception e) {
            LOGGER.error("Failed to integrate with Eidolon codex system", e);
        }
        
        // Chant integration is handled by the datapack reload system, not here
        // This prevents early initialization issues that can cause networking problems
        LOGGER.info("Server starting event completed");
    }
    
    // Register commands
    @SubscribeEvent
    public void onCommandsRegister(RegisterCommandsEvent event) {
        UnifiedCommands.register(event.getDispatcher());
        LOGGER.info("Eidolon Unchained unified commands registered");
    }
    
    // Register reload listeners for datapack systems
    @SubscribeEvent
    public void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new DatapackDeityManager());
        event.addListener(new DatapackChantManager());
        LOGGER.info("Eidolon Unchained datapack managers registered");
    }
}
