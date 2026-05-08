package com.bluelotuscoding.eidolonunchained.chant;

import com.google.gson.*;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import com.bluelotuscoding.eidolonunchained.util.UnifiedDynamicSystemLoader;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.util.*;
import com.bluelotuscoding.eidolonunchained.util.UnifiedDynamicLoader;

/**
 * Loads keybind sign effects from datapacks:
 * data/<namespace>/keybind_settings/sign_effects.json
 *
 * Schema (flexible):
 * {
 *   "per_sign": {
 *     "eidolon:wicked": "say @s performs wicked",
 *     "eidolon:soul": ["playsound minecraft:entity.warden.heartbeat voice @s ~ ~ ~ 1 1"]
 *   },
 *   "sequences": {
 *     "eidolonunchained:shadow_communion": {
 *       "commands": [
 *         "effect give @s minecraft:glowing 2 0 true",
 *         "playsound minecraft:block.enchantment_table.use voice @s ~ ~ ~ 0.7 1.2",
 *         "title @s actionbar {\"text\":\"Shadow communion...\",\"color\":\"dark_purple\"}"
 *       ]
 *     }
 *   }
 * }
 */
@Mod.EventBusSubscriber(modid = "eidolonunchained", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class KeybindSignEffectsManager extends UnifiedDynamicSystemLoader {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = com.bluelotuscoding.eidolonunchained.util.JsonUtils.GSON;

    private static KeybindSignEffectsManager INSTANCE;

    // per-sign effects: sign id -> commands
    private static final Map<ResourceLocation, List<String>> PER_SIGN = new HashMap<>();

    // per-chant sequence effects: chant id -> commands per sign index
    private static final Map<ResourceLocation, List<String>> SEQUENCES = new HashMap<>();

    public KeybindSignEffectsManager() {
        super(GSON, "keybind_settings", "eidolonunchained", "sequences");
        INSTANCE = this;
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent e) {
        e.addListener(getInstance());
        LOGGER.info("Registered KeybindSignEffectsManager reload listener");
    }

    public static KeybindSignEffectsManager getInstance() {
        if (INSTANCE == null) INSTANCE = new KeybindSignEffectsManager();
        return INSTANCE;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resourceMap, ResourceManager resourceManager, ProfilerFiller profiler) {
        PER_SIGN.clear();
        SEQUENCES.clear();

        int processed = 0;
        // Parse per-file data (per_sign) using legacy approach to preserve flexibility
        for (Map.Entry<ResourceLocation, JsonElement> entry : resourceMap.entrySet()) {
            if (!entry.getKey().getPath().endsWith("sign_effects")) continue;
            JsonElement el = entry.getValue();
            if (!el.isJsonObject()) continue;
            try {
                JsonObject root = el.getAsJsonObject();
                if (root.has("per_sign")) {
                    JsonObject per = root.getAsJsonObject("per_sign");
                    for (Map.Entry<String, JsonElement> e : per.entrySet()) {
                        ResourceLocation signId = ResourceLocation.tryParse(e.getKey());
                        if (signId == null) continue;
                        List<String> cmds = parseCommands(e.getValue());
                        if (!cmds.isEmpty()) PER_SIGN.put(signId, cmds);
                    }
                }
                processed++;
            } catch (Exception ex) {
                LOGGER.error("Failed to parse sign effects at {}: {}", entry.getKey(), ex.getMessage());
            }
        }

        // Normalize sequences across files: support both top-level 'sequences' object and file-per-entry styles
        Map<ResourceLocation, JsonObject> seqEntries = UnifiedDynamicLoader.normalizeResourceMap(resourceMap, "eidolonunchained", "sequences");
        for (Map.Entry<ResourceLocation, JsonObject> se : seqEntries.entrySet()) {
            ResourceLocation chantId = se.getKey();
            JsonObject obj = se.getValue();
            List<String> cmds = new ArrayList<>();
            if (obj.has("commands")) cmds.addAll(parseCommands(obj.get("commands")));
            if (!cmds.isEmpty()) SEQUENCES.put(chantId, cmds);
        }
        LOGGER.info("Loaded keybind sign effects: {} files, {} per-sign mappings, {} sequences",
            processed, PER_SIGN.size(), SEQUENCES.size());
    }

    private void parseSignEffects(JsonObject root) {
        if (root.has("per_sign")) {
            JsonObject per = root.getAsJsonObject("per_sign");
            for (Map.Entry<String, JsonElement> e : per.entrySet()) {
                ResourceLocation signId = ResourceLocation.tryParse(e.getKey());
                if (signId == null) continue;
                List<String> cmds = parseCommands(e.getValue());
                if (!cmds.isEmpty()) PER_SIGN.put(signId, cmds);
            }
        }
        if (root.has("sequences")) {
            JsonObject seq = root.getAsJsonObject("sequences");
            for (Map.Entry<String, JsonElement> e : seq.entrySet()) {
                ResourceLocation chantId = normalizeChantId(e.getKey());
                if (chantId == null) continue;
                JsonElement v = e.getValue();
                List<String> cmds = new ArrayList<>();
                if (v.isJsonArray()) {
                    cmds.addAll(parseCommands(v));
                } else if (v.isJsonObject()) {
                    JsonObject obj = v.getAsJsonObject();
                    if (obj.has("commands")) cmds.addAll(parseCommands(obj.get("commands")));
                }
                if (!cmds.isEmpty()) SEQUENCES.put(chantId, cmds);
            }
        }
    }

    @Override
    protected void handleEntry(ResourceLocation id, JsonObject json) {
        if (json == null) return;
        // preserve legacy per-file parsing behavior
        parseSignEffects(json);
    }

    private static ResourceLocation normalizeChantId(String id) {
        if (id == null || id.isEmpty()) return null;
        if (id.contains(":")) return ResourceLocation.tryParse(id);
        // default to our mod namespace if none supplied
        return new ResourceLocation("eidolonunchained", id);
    }

    private static List<String> parseCommands(JsonElement el) {
        List<String> cmds = new ArrayList<>();
        if (el == null) return cmds;
        if (el.isJsonPrimitive() && el.getAsJsonPrimitive().isString()) {
            cmds.add(el.getAsString());
        } else if (el.isJsonArray()) {
            for (JsonElement x : el.getAsJsonArray()) if (x.isJsonPrimitive()) cmds.add(x.getAsString());
        }
        return cmds;
    }

    /**
     * Execute sign effects for a per-sign and sequence context.
     * @param player the player
     * @param signId the sign pressed
     * @param signIndex index in the current sequence (0-based)
     * @param chantId the chant id if the sequence is known/prefix-matching, else null
     */
    public static void runSignEffects(ServerPlayer player, ResourceLocation signId, int signIndex, ResourceLocation chantId) {
        // Per sign commands
        List<String> per = PER_SIGN.get(signId);
        if (per != null) runCommands(player, per);

        // Sequence commands (by index)
        if (chantId != null) {
            List<String> seq = SEQUENCES.get(chantId);
            if (seq != null && signIndex >= 0 && signIndex < seq.size()) {
                runCommands(player, Collections.singletonList(seq.get(signIndex)));
            }
        }
    }

    private static void runCommands(ServerPlayer player, List<String> commands) {
        if (commands == null || commands.isEmpty()) return;
        var server = player.getServer();
        if (server == null) return;
        for (String cmd : commands) {
            if (cmd == null || cmd.isBlank()) continue;
            try {
                var src = player.createCommandSourceStack().withPermission(2).withSuppressedOutput();
                String processed = cmd.replace("@s", player.getName().getString());
                server.getCommands().performPrefixedCommand(src, processed);
            } catch (Exception e) {
                LOGGER.debug("Failed to run sign effect command '{}': {}", cmd, e.getMessage());
            }
        }
    }
}

