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
import dev.latvian.mods.kubejs.recipe.schema.RegisterRecipeSchemasEvent;
import dev.latvian.mods.kubejs.script.BindingsEvent;
import dev.latvian.mods.kubejs.script.ScriptType;
import dev.latvian.mods.kubejs.util.ClassFilter;
import net.minecraft.resources.ResourceLocation;

/**
 * The KubeJS plugin. Registered through {@code kubejs.plugins.txt}. Exposes the {@code EidolonUnchained} global, the
 * {@code EidolonUnchainedEvents} group, and generates the client assets scripted content needs.
 */
public class EUKubeJSPlugin extends KubeJSPlugin {
    /** KubeJS item type {@code 'eidolonunchained:hexblade'}: a Hexblades-style blade bound to any deity. */
    @Override
    public void init() {
        dev.latvian.mods.kubejs.registry.RegistryInfo.ITEM.addType("eidolonunchained:hexblade",
                com.bluelotuscoding.eidolonunchained.hexblade.HexbladeItemBuilder.class, com.bluelotuscoding.eidolonunchained.hexblade.HexbladeItemBuilder::new);
    }

    @Override
    public void registerEvents() {
        EUEvents.GROUP.register();
        EUEvents.hookChant();
        EUEvents.hookMobChant();
        EUEvents.hookWeapons();
        EUEvents.hookWorld();
        EUEvents.hookPatrons();
        EidolonUnchained.LOGGER.info("KubeJS plugin registered: event group '{}'", EUEvents.GROUP);
    }

    @Override
    public void registerClasses(ScriptType type, ClassFilter filter) {
        // Scripts may touch Eidolon's public API surface and this mod's own API package, nothing deeper.
        filter.allow("elucent.eidolon.api");
        filter.allow("com.bluelotuscoding.eidolonunchained.api");
        filter.allow("com.bluelotuscoding.eidolonunchained.hexblade");
        filter.deny("com.bluelotuscoding.eidolonunchained.kubejs");
    }

    @Override
    public void registerBindings(BindingsEvent event) {
        event.add("EidolonUnchained", EUBinding.INSTANCE);
    }

    /** T2: chant recipe schemas, so {@code ServerEvents.recipes(e => e.recipes.eidolon.chant(...))} needs no datapack. */
    @Override
    public void registerRecipeSchemas(RegisterRecipeSchemasEvent event) {
        EURecipeSchemas.register(event);
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
        divineDamageLang(generator);
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
    /**
     * Divine damage (D53): each declared divine damage's damage type, and its tags (ignores armour, counts as magic,
     * EU's divine marker). Generated here so the types exist when the world loads (a damage type can't be added later).
     */
    @Override
    public void generateDataJsons(dev.latvian.mods.kubejs.generator.DataJsonGenerator generator) {
        var all = com.bluelotuscoding.eidolonunchained.damage.DivineDamages.all();
        if (all.isEmpty()) return;
        var ids = new JsonArray();
        for (var d : all) {
            var type = new JsonObject();
            type.addProperty("message_id", d.id.getNamespace() + "." + d.id.getPath());
            type.addProperty("scaling", "never");
            type.addProperty("exhaustion", 0.1f);
            generator.json(new ResourceLocation(d.id.getNamespace(), "damage_type/" + d.id.getPath()), type);
            ids.add(d.id.toString());
        }
        for (var tag : new String[]{"minecraft:bypasses_armor", "forge:is_magic", EidolonUnchained.MOD_ID + ":is_divine"}) {
            var rl = new ResourceLocation(tag);
            var json = new JsonObject();
            json.add("values", ids);
            generator.json(new ResourceLocation(rl.getNamespace(), "tags/damage_type/" + rl.getPath()), json);
        }
    }

    /** Names for each divine damage's death messages and attributes (a pack's own lang overrides them). */
    private static void divineDamageLang(AssetJsonGenerator generator) {
        var all = com.bluelotuscoding.eidolonunchained.damage.DivineDamages.all();
        if (all.isEmpty()) return;
        var lang = new JsonObject();
        lang.addProperty("attribute.name." + EidolonUnchained.MOD_ID + ".divine_resistance", "Divine Resistance");
        lang.addProperty("attribute.name." + EidolonUnchained.MOD_ID + ".divine_penetration", "Divine Penetration");
        for (var d : all) {
            String key = d.id.getNamespace() + "." + d.id.getPath(), name = d.displayName();
            lang.addProperty("death.attack." + key, "%1$s was struck down by " + name.toLowerCase() + " power");
            lang.addProperty("death.attack." + key + ".player", "%1$s was struck down by %2$s's " + name.toLowerCase() + " power");
            lang.addProperty("attribute.name." + key + "_damage", name + " Damage");
            lang.addProperty("attribute.name." + key + "_resistance", name + " Resistance");
        }
        generator.json(new ResourceLocation(EidolonUnchained.MOD_ID + "_divine", "lang/en_us"), lang);
    }
}
