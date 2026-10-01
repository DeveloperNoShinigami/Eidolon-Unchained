package com.bluelotuscoding.eidolonunchained.api.condition;

import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import com.bluelotuscoding.eidolonunchained.api.Ids;
import com.bluelotuscoding.eidolonunchained.api.PlayerHelper;
import dev.latvian.mods.kubejs.typings.Info;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * A discovery (Phase 4, spec §8): "when this happens, and these conditions hold, give this". Declared in server scripts
 * through {@code EidolonUnchainedEvents.discoveries(e => e.create(id)…)} (rebuilt on server start and {@code /reload}) or
 * as {@code research(id).discoveredBy(...)} in startup scripts. Once per player by default, remembered in the player's
 * persistent NBT {@code eidolonunchained.discoveries} (rule C6).
 */
public final class Discovery {
    public static final Set<String> KINDS = Set.of("kill", "biome", "biome_left", "structure", "structure_left", "dimension",
            "pickup", "craft", "equip", "ritual", "tame", "enthrall", "chant", "research", "fact", "sign", "rune", "stage");

    public interface Action {
        void run(EventContext ctx);
    }

    final ResourceLocation id;
    String kind;
    @Nullable ResourceLocation kindId;
    final List<Condition> conditions = new ArrayList<>();
    final List<ResourceLocation> research = new ArrayList<>(), facts = new ArrayList<>(), signs = new ArrayList<>(), runes = new ArrayList<>();
    final List<Object[]> reputation = new ArrayList<>();
    @Nullable String message;
    @Nullable Action action;
    boolean once = true;
    /** With repeatable: how many times per player (0 = no limit). */
    int times = 0;

    public Discovery(ResourceLocation id) {
        this.id = id;
    }

    public ResourceLocation id() {
        return id;
    }

    @Info("What triggers it: kill | biome | biome_left | structure | structure_left | dimension | pickup | craft | equip | ritual | tame | enthrall | chant | research | fact | sign | rune | stage")
    public Discovery on(String kind) {
        var k = kind.toLowerCase();
        if (!KINDS.contains(k)) throw new IllegalArgumentException("Eidolon Unchained: discovery '" + id + "': unknown trigger '" + kind + "', expected one of " + KINDS);
        this.kind = k;
        return this;
    }

    @Info("Trigger plus the id it must be about, e.g. .on('biome', 'minecraft:deep_dark') or .on('ritual', 'mypack:storm_rite')")
    public Discovery on(String kind, String id) {
        on(kind);
        this.kindId = Ids.of(id, kind);
        return this;
    }

    @Info("A condition that must also hold (several .when calls must all hold)")
    public Discovery when(Condition condition) {
        conditions.add(condition);
        return this;
    }

    @Info("The same with a plain function: ctx => boolean")
    public Discovery when(Conditions.ContextTest fn) {
        return when(Condition.of(fn));
    }

    public Discovery grantResearch(String id) {
        research.add(Ids.of(id, "research"));
        return this;
    }

    public Discovery grantFact(String id) {
        facts.add(Ids.of(id, "fact"));
        return this;
    }

    public Discovery grantSign(String id) {
        signs.add(Ids.of(id, "sign"));
        return this;
    }

    public Discovery grantRune(String id) {
        runes.add(Ids.of(id, "rune"));
        return this;
    }

    @Info("Change reputation with a deity (negative to lose it)")
    public Discovery reputation(String deityId, double amount) {
        reputation.add(new Object[]{deityId, amount});
        return this;
    }

    @Info("An action-bar line: a lang key or plain text")
    public Discovery message(String keyOrText) {
        this.message = keyOrText;
        return this;
    }

    @Info("Any script: ctx => … (ctx.player, ctx.entity, ctx.item, ctx.id, ctx.kind)")
    public Discovery run(Action fn) {
        this.action = fn;
        return this;
    }

    @Info("Once per player (the default)")
    public Discovery once() {
        this.once = true;
        return this;
    }

    @Info("Every time it triggers")
    public Discovery repeatable() {
        this.once = false;
        this.times = 0;
        return this;
    }

    @Info("Up to this many times per player")
    public Discovery repeatable(int times) {
        if (times < 1) throw new IllegalArgumentException("Eidolon Unchained: discovery '" + id + "': repeatable(n) needs n >= 1");
        this.once = times == 1;
        this.times = times;
        return this;
    }

    // ---- evaluation ----

    boolean accepts(EventContext ctx) {
        if (kind == null || !kind.equals(ctx.getKind()) || ctx.getPlayer() == null) return false;
        if (kindId != null && !kindId.equals(ctx.idOf(kind))) return false;
        for (var c : conditions) if (!c.matches(ctx)) return false;
        return true;
    }

    void apply(EventContext ctx) {
        var player = ctx.getPlayer();
        if (once && hasDiscovered(player, id)) return;
        if (!once && times > 0 && count(player, id) >= times) return;
        var helper = new PlayerHelper(player);
        try {
            for (var r : research) helper.grantResearch(r.toString());
            for (var f : facts) helper.grantFact(f.toString());
            for (var s : signs) helper.grantSign(s.toString());
            for (var r : runes) helper.grantRune(r.toString());
            for (var rep : reputation) {
                double amount = ((Number) rep[1]).doubleValue();
                if (amount >= 0) helper.addReputation((String) rep[0], amount); else helper.subtractReputation((String) rep[0], -amount);
            }
            if (message != null) player.displayClientMessage(Component.translatableWithFallback(message, message), true);
            if (action != null) action.run(ctx);
        } catch (RuntimeException e) {
            EidolonUnchained.LOGGER.error("discovery '{}' failed: {}", id, e.toString());
        }
        if (once) markDiscovered(player, id);
        else if (times > 0) addCount(player, id);
        EidolonUnchained.LOGGER.debug("discovery {} for {} ({})", id, player.getName().getString(), ctx);
    }

    // ---- per-player memory ----

    private static final String ROOT = EidolonUnchained.MOD_ID;

    public static boolean hasDiscovered(Player player, ResourceLocation id) {
        var list = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).getCompound(ROOT).getList("discoveries", Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) if (list.getString(i).equals(id.toString())) return true;
        return false;
    }

    public static void markDiscovered(Player player, ResourceLocation id) {
        var persisted = player.getPersistentData();
        var tag = persisted.getCompound(Player.PERSISTED_NBT_TAG);
        var root = tag.getCompound(ROOT);
        root.putInt("v", 1);
        var list = root.getList("discoveries", Tag.TAG_STRING);
        list.add(StringTag.valueOf(id.toString()));
        root.put("discoveries", list);
        tag.put(ROOT, root);
        persisted.put(Player.PERSISTED_NBT_TAG, tag);
    }

    /** How many times a limited repeatable discovery has fired for the player. */
    public static int count(Player player, ResourceLocation id) {
        return player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).getCompound(ROOT).getCompound("discovery_counts").getInt(id.toString());
    }

    private static void addCount(Player player, ResourceLocation id) {
        var persisted = player.getPersistentData();
        var tag = persisted.getCompound(Player.PERSISTED_NBT_TAG);
        var root = tag.getCompound(ROOT);
        root.putInt("v", 1);
        var counts = root.getCompound("discovery_counts");
        counts.putInt(id.toString(), counts.getInt(id.toString()) + 1);
        root.put("discovery_counts", counts);
        tag.put(ROOT, root);
        persisted.put(Player.PERSISTED_NBT_TAG, tag);
    }

    public static void forget(Player player, @Nullable ResourceLocation id) {
        var persisted = player.getPersistentData();
        var tag = persisted.getCompound(Player.PERSISTED_NBT_TAG);
        var root = tag.getCompound(ROOT);
        if (id == null) { root.remove("discoveries"); root.remove("discovery_counts"); }
        else {
            var list = root.getList("discoveries", Tag.TAG_STRING);
            var out = new ListTag();
            for (int i = 0; i < list.size(); i++) if (!list.getString(i).equals(id.toString())) out.add(list.get(i));
            root.put("discoveries", out);
            var counts = root.getCompound("discovery_counts");
            counts.remove(id.toString());
            root.put("discovery_counts", counts);
        }
        tag.put(ROOT, root);
        persisted.put(Player.PERSISTED_NBT_TAG, tag);
    }

    @Override
    public String toString() {
        return "Discovery[" + id + " on " + kind + (kindId == null ? "" : " " + kindId) + ", " + conditions.size() + " condition(s)]";
    }
}
