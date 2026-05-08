package com.bluelotuscoding.eidolonunchained.data;

import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import com.bluelotuscoding.eidolonunchained.deity.DatapackDeity;
import com.bluelotuscoding.eidolonunchained.util.UnifiedDynamicSystemLoader;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import elucent.eidolon.api.spells.Sign;
import elucent.eidolon.registries.Signs;
import elucent.eidolon.util.ColorUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads datapack sign definitions from data/&lt;namespace&gt;/signs/*.json and
 * registers them into Eidolon's global Signs registry.
 *
 * Sign JSON schema:
 * <pre>
 * {
 *   "id":           "eidolonunchained:example_sign",
 *   "sprite":       "eidolon:particle/wicked_sign",
 *   "color":        "0xFF9A4DFF",          // optional if linked_deity is set
 *   "linked_deity": "eidolonunchained:dark_deity",  // optional; overrides color with deity color
 *   "display_name": "Example Sign"
 * }
 * </pre>
 */
@Mod.EventBusSubscriber(modid = EidolonUnchained.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DatapackSignManager extends UnifiedDynamicSystemLoader {
    private static final org.slf4j.Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = com.bluelotuscoding.eidolonunchained.util.JsonUtils.GSON;

    private static DatapackSignManager INSTANCE;

    // Loaded signs keyed by their registry ResourceLocation
    private static final Map<ResourceLocation, Sign> loadedSigns = new HashMap<>();

    public DatapackSignManager() {
        super(GSON, "signs", "eidolonunchained");
        INSTANCE = this;
    }

    public static DatapackSignManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new DatapackSignManager();
        }
        return INSTANCE;
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(getInstance());
        LOGGER.info("Registered Datapack Sign reload listener");
    }

    public static Map<ResourceLocation, Sign> getLoadedSigns() {
        return Collections.unmodifiableMap(loadedSigns);
    }

    public static Sign getSign(ResourceLocation id) {
        return loadedSigns.get(id);
    }

    @Override
    protected void apply(Map<net.minecraft.resources.ResourceLocation, com.google.gson.JsonElement> resourceMap,
                         ResourceManager resourceManager, ProfilerFiller profiler) {
        LOGGER.info("Loading datapack signs...");
        loadedSigns.clear();
        super.apply(resourceMap, resourceManager, profiler);
        LOGGER.info("Loaded {} datapack sign(s)", loadedSigns.size());
        MinecraftForge.EVENT_BUS.post(new DatapackSignsLoadedEvent(new HashMap<>(loadedSigns)));
    }

    @Override
    protected void handleEntry(ResourceLocation location, JsonObject json) {
        if (json == null) {
            LOGGER.warn("Skipping null JSON at {}", location);
            return;
        }

        String rawId = json.has("id") ? json.get("id").getAsString() : location.toString();
        ResourceLocation signId = ResourceLocation.tryParse(rawId);
        if (signId == null) {
            LOGGER.warn("Invalid sign id '{}' at {}, skipping", rawId, location);
            return;
        }

        String rawSprite = json.get("sprite").getAsString();
        ResourceLocation sprite = ResourceLocation.tryParse(rawSprite);
        if (sprite == null) {
            LOGGER.warn("Invalid sprite '{}' for sign {}, skipping", rawSprite, signId);
            return;
        }

        int color = resolveColor(json, signId);

        Sign sign = new Sign(signId, sprite, color);
        Signs.register(sign);
        loadedSigns.put(signId, sign);

        LOGGER.debug("Registered datapack sign: {} (sprite={}, color=#{:06X})", signId, sprite, color);
    }

    /**
     * Resolves the sign color.
     * Priority: linked_deity color > explicit "color" field > default white.
     */
    private int resolveColor(JsonObject json, ResourceLocation signId) {
        if (json.has("linked_deity")) {
            ResourceLocation deityId = ResourceLocation.tryParse(json.get("linked_deity").getAsString());
            if (deityId != null) {
                DatapackDeity deity = DatapackDeityManager.getDeity(deityId);
                if (deity != null) {
                    int r = Math.round(deity.getRed()   * 255f);
                    int g = Math.round(deity.getGreen() * 255f);
                    int b = Math.round(deity.getBlue()  * 255f);
                    return ColorUtil.packColor(255, r, g, b);
                } else {
                    LOGGER.warn("linked_deity '{}' not found for sign {}, falling back to color field", deityId, signId);
                }
            }
        }

        if (json.has("color")) {
            try {
                return (int) Long.decode(json.get("color").getAsString()).longValue();
            } catch (NumberFormatException e) {
                LOGGER.warn("Invalid color value for sign {}: {}", signId, json.get("color").getAsString());
            }
        }

        // Default: full-alpha white
        return ColorUtil.packColor(255, 255, 255, 255);
    }
}
