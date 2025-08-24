package com.bluelotuscoding.eidolonunchained.datagen;

import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.concurrent.CompletableFuture;

/**
 * Data generation for Eidolon Unchained mod
 */
@Mod.EventBusSubscriber(modid = EidolonUnchained.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class EidolonUnchainedDatagen {
    
    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator gen = event.getGenerator();
        ExistingFileHelper fileHelper = event.getExistingFileHelper();
        CompletableFuture<HolderLookup.Provider> provider = event.getLookupProvider();
        PackOutput output = gen.getPackOutput();
        
        // Register AI deity chant provider for server-side data generation
        gen.addProvider(event.includeServer(), new AIDeityChantProvider(gen));
    }
}
