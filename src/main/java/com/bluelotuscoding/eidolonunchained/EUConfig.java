package com.bluelotuscoding.eidolonunchained;

import net.minecraftforge.common.ForgeConfigSpec;

/** Common config. Only Phase 1 options for now; later phases add their own sections. */
public final class EUConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue VALIDATE_EIDOLON_API;
    public static final ForgeConfigSpec.BooleanValue LOG_SCRIPT_EVENTS;

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
        SPEC = b.build();
    }

    private EUConfig() {
    }
}
