package com.bluelotuscoding.eidolonunchained;

import com.bluelotuscoding.eidolonunchained.api.EidolonApiValidation;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/**
 * Eidolon Unchained 2.0: a KubeJS-centric extension layer for Eidolon Repraised.
 * <p>
 * Phase 1 ("clean base") only establishes the mod, its pinned dependency baseline, the KubeJS plugin and an
 * API validation pass. Every later system is added as a thin wrapper over a real Eidolon API (spec §2).
 */
@Mod(EidolonUnchained.MOD_ID)
public final class EidolonUnchained {
    public static final String MOD_ID = "eidolonunchained";
    public static final Logger LOGGER = LogUtils.getLogger();

    public EidolonUnchained(FMLJavaModLoadingContext context) {
        context.registerConfig(ModConfig.Type.COMMON, EUConfig.SPEC);
        context.getModEventBus().addListener(this::commonSetup);
        MinecraftForge.EVENT_BUS.register(this);
        LOGGER.info("Eidolon Unchained {} loading (Eidolon {}, KubeJS {}, GeckoLib {})",
                version(), modVersion("eidolon"), modVersion("kubejs"), modVersion("geckolib"));
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            if (EUConfig.VALIDATE_EIDOLON_API.get()) {
                EidolonApiValidation.run();
            }
        });
    }

    /** This mod's version as declared in its mod file. */
    public static String version() {
        return modVersion(MOD_ID);
    }

    /** The loaded version of a mod, or "absent" when it is not loaded. */
    public static String modVersion(String modId) {
        return ModList.get().getModContainerById(modId)
                .map(c -> c.getModInfo().getVersion().toString())
                .orElse("absent");
    }
}
