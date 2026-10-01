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
    public static final ForgeConfigSpec.IntValue CHANT_BUILD_SIGN_DELAY_TICKS;
    public static final ForgeConfigSpec.IntValue COMMAND_CHANT_PERMISSION_LEVEL;
    public static final ForgeConfigSpec.DoubleValue MOB_CHANT_INTERRUPT_FRACTION;
    public static final ForgeConfigSpec.DoubleValue MOB_DEFAULT_MAX_MANA;
    public static final ForgeConfigSpec.DoubleValue MOB_DEFAULT_MANA_REGEN;
    public static final ForgeConfigSpec.IntValue MAX_IMBUED_CHANTS;
    public static final ForgeConfigSpec.IntValue IMBUE_SHARDS_DEFAULT;
    public static final ForgeConfigSpec.IntValue IMBUE_CAST_COOLDOWN_TICKS;
    public static final ForgeConfigSpec.ConfigValue<java.util.List<? extends Double>> PROTECTION_MANA_SHARE;
    public static final ForgeConfigSpec.ConfigValue<java.util.List<? extends Integer>> PROTECTION_COOLDOWN_TICKS;
    public static final ForgeConfigSpec.BooleanValue PATRON_REQUIRED_FOR_EIDOLON_DEITIES;
    public static final ForgeConfigSpec.DoubleValue DIVINE_WEAKNESS_FLOOR;

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
                .defineInRange("chantCommitDelayTicks", 4, 0, 60);
        CHANT_BUILD_SIGN_DELAY_TICKS = b.comment("Ticks between signs when a chant is cast for the player (imbued weapon, Deity's Protection, scripts): every cast builds up.")
                .defineInRange("chantBuildSignDelayTicks", 6, 0, 40);
        COMMAND_CHANT_PERMISSION_LEVEL = b.comment("Permission level a player needs to trigger command chants (decision D34: 0 = anyone; the pack author is trusted).",
                        "Checked on every path (chanting, codex, scrolls, imbued weapons). The commands themselves run at level 2, as Eidolon does,",
                        "and only when the server allows command blocks (enable-command-block=true on a dedicated server).")
                .defineInRange("commandChantPermissionLevel", 0, 0, 4);
        MOB_DEFAULT_MAX_MANA = b.comment("Mana pool every mob gets (rule C5), so held imbued weapons and Deity's Protection can charge them. 0 = only caster profiles have mana.")
                .defineInRange("mobDefaultMaxMana", 100.0, 0.0, 10000.0);
        MOB_DEFAULT_MANA_REGEN = b.comment("Mana a mob without a caster profile regenerates per second.")
                .defineInRange("mobDefaultManaRegen", 0.5, 0.0, 1000.0);
        MOB_CHANT_INTERRUPT_FRACTION = b.comment("A single hit of at least this fraction of a mob's max health interrupts its chant (D34: 0.25). 0 = any hit, >1 = never.")
                .defineInRange("mobChantInterruptFraction", 0.25, 0.0, 2.0);
        b.pop();
        b.push("imbue");
        MAX_IMBUED_CHANTS = b.comment("One chant per weapon (default 1): once a weapon carries a chant, the scroll of any other chant does nothing on it.",
                        "Above 1 a weapon holds several chants and the view key + right-click cycles them.")
                .defineInRange("maxImbuedChants", 1, 1, 9);
        IMBUE_SHARDS_DEFAULT = b.comment("Soul shards (one per reagent slot, 1-4) to imbue a chant that sets no .imbueCost, e.g. Eidolon's own chants.")
                .defineInRange("imbueShardsDefault", 4, 1, 4);
        IMBUE_CAST_COOLDOWN_TICKS = b.comment("Item cooldown after casting an imbued chant by right-click.")
                .defineInRange("imbueCastCooldownTicks", 20, 0, 1200);
        PROTECTION_MANA_SHARE = b.comment("Deity's Protection: the share of the chant's mana the wearer pays, per enchantment level I, II, III.")
                .defineList("deitysProtectionManaShare", java.util.List.of(1.0, 0.66, 0.33), o -> o instanceof Number);
        PROTECTION_COOLDOWN_TICKS = b.comment("Deity's Protection: ticks between retaliations of one piece, per enchantment level I, II, III.")
                .defineList("deitysProtectionCooldownTicks", java.util.List.of(200, 140, 80), o -> o instanceof Integer);
        b.pop();
        b.push("patrons");
        PATRON_REQUIRED_FOR_EIDOLON_DEITIES = b.comment("Eidolon's own deities (eidolon:light, eidolon:dark) give no reputation until the player pledges to them (D47),",
                        "unless a script sets .patronRequired(...) through extendDeity. Every other deity defaults to true in its script.")
                .define("patronRequiredForEidolonDeities", true);
        b.pop();
        b.push("divine_damage");
        DIVINE_WEAKNESS_FLOOR = b.comment("Lowest effective percent resistance to divine damage after penetration: -1.0 means a weakness can at most double a hit.")
                .defineInRange("weaknessFloor", -1.0, -10.0, 0.0);
        b.pop();
        SPEC = b.build();
    }

    private EUConfig() {
    }
}
