package com.bluelotuscoding.eidolonunchained;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

// An example config class. This is not required, but it's a good idea to have one to configure your mod from the config file
@Mod.EventBusSubscriber(modid = EidolonUnchained.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config
{
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.BooleanValue ENABLE_DEBUG_MODE = BUILDER
            .comment("Enable debug mode for Eidolon Unchained")
            .define("enableDebugMode", false);

    private static final ForgeConfigSpec.IntValue RITUAL_POWER_MULTIPLIER = BUILDER
            .comment("Multiplier for ritual power calculations")
            .defineInRange("ritualPowerMultiplier", 1, 1, 10);

    static final ForgeConfigSpec SPEC = BUILDER.build();

    public static boolean enableDebugMode;
    public static int ritualPowerMultiplier;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event)
    {
        enableDebugMode = ENABLE_DEBUG_MODE.get();
        ritualPowerMultiplier = RITUAL_POWER_MULTIPLIER.get();
    }
}
