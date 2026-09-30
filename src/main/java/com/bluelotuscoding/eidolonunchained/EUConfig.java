package com.bluelotuscoding.eidolonunchained;

import net.minecraftforge.common.ForgeConfigSpec;

/** Common config. Only Phase 1 options for now; later phases add their own sections. */
public final class EUConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue VALIDATE_EIDOLON_API;
    public static final ForgeConfigSpec.BooleanValue LOG_SCRIPT_EVENTS;
    public static final ForgeConfigSpec.IntValue MAX_CHANT_LENGTH;
    public static final ForgeConfigSpec.IntValue CHANT_IDLE_CLEAR_SECONDS;
    public static final ForgeConfigSpec.IntValue CHANT_COMMIT_DELAY_TICKS;
    public static final ForgeConfigSpec.IntValue COMMAND_CHANT_PERMISSION_LEVEL;
    public static final ForgeConfigSpec.DoubleValue MOB_CHANT_INTERRUPT_FRACTION;

    static {
        var b = new ForgeConfigSpec.Builder();
        b.push("diagnostics");
        VALIDATE_EIDOLON_API = b
                .comment("On startup, check that every Eidolon API surface this mod wraps still exists and log a report.",
                        "Turn off only on a server where startup time matters more than the diagnostics.")
                .define("validateEidolonApi", true);
        LOG_SCRIPT_EVENTS = b
                .comment("Log when Eidolon Unchained fires its KubeJS events (startup, server, client). Useful when checking script loading.")
                .define("logScriptEvents", true);
        b.pop();
        b.push("casting");
        MAX_CHANT_LENGTH = b.comment("Longest sign sequence a player can build outside the codex (Eidolon's codex allows 18).")
                .defineInRange("maxChantLength", 18, 1, 32);
        CHANT_IDLE_CLEAR_SECONDS = b.comment("Seconds without input after which an unfinished chant fizzles.")
                .defineInRange("chantIdleClearSeconds", 8, 1, 120);
        CHANT_COMMIT_DELAY_TICKS = b.comment("Active chanting: when the signs already match a chant but a longer chant starts the same way, wait this many ticks for another sign before firing.")
                .defineInRange("chantCommitDelayTicks", 8, 0, 60);
        COMMAND_CHANT_PERMISSION_LEVEL = b.comment("Permission level a player needs to trigger command chants (decision D34: 0 = anyone; the pack author is trusted).")
                .defineInRange("commandChantPermissionLevel", 0, 0, 4);
        MOB_CHANT_INTERRUPT_FRACTION = b.comment("A single hit of at least this fraction of a mob's max health interrupts its chant (D34: 0.25). 0 = any hit, >1 = never.")
                .defineInRange("mobChantInterruptFraction", 0.25, 0.0, 2.0);
        b.pop();
        SPEC = b.build();
    }

    private EUConfig() {
    }
}
