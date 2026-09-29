package com.bluelotuscoding.eidolonunchained.api;

import com.bluelotuscoding.eidolonunchained.EidolonUnchained;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Checks, by name, that every Eidolon surface this mod wraps still exists in the loaded Eidolon jar (spec §2: EU only
 * wraps real Eidolon APIs). Runs once at common setup and logs a report. It is reflection used for <em>diagnostics</em>
 * only; normal integration paths call the APIs directly (spec §3.8.4).
 * <p>
 * The list is the "Verified" set from the v4 spec review (Eidolon Repraised 0.3.13, branch 1.20.1). Add to it whenever
 * a phase wraps a new surface.
 */
public final class EidolonApiValidation {
    /** class name, then the method names that must exist on it (any overload). */
    private static final String[][] SURFACES = {
            {"elucent.eidolon.common.deity.Deities", "register", "find", "getDeities"},
            {"elucent.eidolon.api.deity.Deity", "getId", "getRed", "getProgression", "onReputationChange"},
            {"elucent.eidolon.api.deity.ReputationEvent"},
            {"elucent.eidolon.capability.IReputation", "getReputation", "addReputation", "setReputation", "isLocked", "pray", "canPray"},
            {"elucent.eidolon.registries.Signs", "register", "find", "getSigns"},
            {"elucent.eidolon.api.spells.Sign", "getRegistryName", "getSprite", "getColor"},
            {"elucent.eidolon.registries.Runes", "register", "find", "getRunes"},
            {"elucent.eidolon.api.spells.Rune"},
            {"elucent.eidolon.api.spells.SignSequence"},
            {"elucent.eidolon.registries.Spells", "register", "registerWithFallback", "find", "getSpells"},
            {"elucent.eidolon.api.spells.Spell", "getRegistryName", "matches", "canCast", "cast", "getCost"},
            {"elucent.eidolon.api.spells.SpellCastEvent"},
            {"elucent.eidolon.capability.ISoul", "getMagic", "getMaxMagic", "setMagic", "takeMagic", "giveMagic", "expendMana"},
            {"elucent.eidolon.util.KnowledgeUtil", "knowsSign", "grantSign", "knowsRune", "grantRune", "knowsResearch", "grantResearch", "knowsFact", "grantFact"},
            {"elucent.eidolon.capability.Facts"},
            {"elucent.eidolon.api.research.Research", "getRegistryName", "getStars", "onLearned"},
            {"elucent.eidolon.codex.CodexEvents"},
            {"elucent.eidolon.registries.Registry"},
            {"elucent.eidolon.common.block.EffigyBlock"},
            {"elucent.eidolon.network.AttemptCastPacket"},
    };

    private static String lastReport = "(not run)";

    private EidolonApiValidation() {
    }

    public static void run() {
        var lines = new ArrayList<String>();
        int missing = 0;
        for (var surface : SURFACES) {
            var className = surface[0];
            Class<?> cls;
            try {
                cls = Class.forName(className, false, EidolonApiValidation.class.getClassLoader());
            } catch (ClassNotFoundException e) {
                lines.add("MISSING  " + className);
                missing++;
                continue;
            }
            var lost = new ArrayList<String>();
            for (var method : Arrays.copyOfRange(surface, 1, surface.length)) {
                if (Arrays.stream(cls.getMethods()).noneMatch(m -> m.getName().equals(method))
                        && Arrays.stream(cls.getDeclaredMethods()).noneMatch(m -> m.getName().equals(method))) {
                    lost.add(method);
                }
            }
            if (lost.isEmpty()) {
                lines.add("ok       " + className);
            } else {
                lines.add("CHANGED  " + className + " (no method named: " + String.join(", ", lost) + ")");
                missing++;
            }
        }
        lastReport = "Eidolon API validation (" + (SURFACES.length - missing) + "/" + SURFACES.length + " surfaces ok, Eidolon "
                + EidolonUnchained.modVersion("eidolon") + ")\n  " + String.join("\n  ", lines);
        if (missing == 0) {
            EidolonUnchained.LOGGER.info(lastReport);
        } else {
            EidolonUnchained.LOGGER.error("{}\nEidolon Unchained was built against Eidolon Repraised 0.3.13. Features that use the missing surfaces will not work.", lastReport);
        }
    }

    public static String report() {
        return lastReport;
    }

    public static List<String> surfaces() {
        return Arrays.stream(SURFACES).map(s -> s[0]).toList();
    }
}
