package com.bluelotuscoding.eidolonunchained.kubejs;

import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import com.bluelotuscoding.eidolonunchained.api.PlayerHelper;
import com.bluelotuscoding.eidolonunchained.api.SpriteSources;
import dev.latvian.mods.kubejs.util.AttachedData;
import net.minecraft.world.entity.player.Player;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.latvian.mods.kubejs.KubeJSPlugin;
import dev.latvian.mods.kubejs.generator.AssetJsonGenerator;
import dev.latvian.mods.kubejs.script.BindingsEvent;
import dev.latvian.mods.kubejs.script.ScriptType;
import dev.latvian.mods.kubejs.util.ClassFilter;
import net.minecraft.resources.ResourceLocation;

/**
 * The KubeJS plugin. Registered through {@code kubejs.plugins.txt}. Exposes the {@code EidolonUnchained} global, the
 * {@code EidolonUnchainedEvents} group, and generates the client assets scripted content needs.
 */
public class EUKubeJSPlugin extends KubeJSPlugin {
    @Override
    public void registerEvents() {
        EUEvents.GROUP.register();
        EidolonUnchained.LOGGER.info("KubeJS plugin registered: event group '{}'", EUEvents.GROUP);
    }

    @Override
    public void registerClasses(ScriptType type, ClassFilter filter) {
        // Scripts may touch Eidolon's public API surface and this mod's own API package, nothing deeper.
        filter.allow("elucent.eidolon.api");
        filter.allow("com.bluelotuscoding.eidolonunchained.api");
        filter.deny("com.bluelotuscoding.eidolonunchained.kubejs");
    }

    @Override
    public void registerBindings(BindingsEvent event) {
        event.add("EidolonUnchained", EUBinding.INSTANCE);
    }

    /** {@code player.eidolon}: knowledge, reputation and soul helpers on every player (decision D29). */
    @Override
    public void attachPlayerData(AttachedData<Player> event) {
        event.add("eidolon", new PlayerHelper(event.getParent()));
    }

    @Override
    public void afterInit() {
        // Startup scripts have been loaded by now; tell them so. Server and client events are fired by EUEvents.
        EUEvents.postStartup();
    }

    /**
     * Sign and rune sprites are drawn from the block atlas. Vanilla reads {@code minecraft:atlases/blocks.json} from
     * every resource pack in the stack and merges the sources, so a generated one adds the scripted sprites without
     * touching anything else.
     */
    @Override
    public void generateAssetJsons(AssetJsonGenerator generator) {
        var sprites = SpriteSources.all();
        if (sprites.isEmpty()) return;
        var sources = new JsonArray();
        for (var sprite : sprites) {
            var source = new JsonObject();
            source.addProperty("type", "single");
            source.addProperty("resource", sprite.toString());
            sources.add(source);
        }
        var json = new JsonObject();
        json.add("sources", sources);
        generator.json(new ResourceLocation("minecraft", "atlases/blocks"), json);
        EidolonUnchained.LOGGER.info("Added {} scripted sign/rune sprite(s) to the block atlas", sprites.size());
    }
}
