package com.bluelotuscoding.eidolonunchained.world;

import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import com.bluelotuscoding.eidolonunchained.api.condition.Discoveries;
import com.bluelotuscoding.eidolonunchained.api.condition.EventContext;
import elucent.eidolon.api.deity.ReputationEvent;
import elucent.eidolon.api.spells.SpellCastEvent;
import elucent.eidolon.capability.IKnowledge;
import elucent.eidolon.common.spell.ThrallSpell;
import elucent.eidolon.util.EntityUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.AnimalTameEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Phase 4 event detection (spec §7.1). Eidolon posts only reputation, spell-cast and codex events, so EU detects the rest:
 * biome / structure / dimension changes (checked every second), knowledge gained by any route (snapshot compared every
 * half second), enthralling (Eidolon's enthrall chant + its thrall marker), taming, kills, pickups, crafting, equipping,
 * chants cast, stages reached, and rituals finishing (the brazier mixin calls {@link #ritualCompleted}). Every event
 * goes to the script hook (KubeJS events) and to the discoveries.
 */
@Mod.EventBusSubscriber(modid = EidolonUnchained.MOD_ID)
public final class WorldEvents {
    /** Set by the KubeJS layer: posts the matching {@code EidolonUnchainedEvents} event. */
    public static Consumer<EventContext> onScript = ctx -> { };

    private static final Map<UUID, PlayerTrack> TRACKS = new HashMap<>();

    private WorldEvents() {
    }

    public static void fire(EventContext ctx) {
        try {
            onScript.accept(ctx);
        } catch (RuntimeException e) {
            EidolonUnchained.LOGGER.error("script event for {} threw: {}", ctx, e.toString());
        }
        Discoveries.handle(ctx);
    }

    // ---- per-player tracking: location every second, knowledge every half second ----

    private static final class PlayerTrack {
        @Nullable ResourceLocation biome;
        Set<ResourceLocation> structures = new HashSet<>();
        Set<ResourceLocation> research, facts, signs, runes;
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer sp) || sp instanceof FakePlayer) return;
        var t = TRACKS.computeIfAbsent(sp.getUUID(), k -> new PlayerTrack());
        if (sp.tickCount % 10 == 0) checkKnowledge(sp, t);
        if (sp.tickCount % 20 == 5) checkLocation(sp, t);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        TRACKS.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        TRACKS.remove(event.getEntity().getUUID());
    }

    private static void checkLocation(ServerPlayer sp, PlayerTrack t) {
        var level = sp.serverLevel();
        var pos = sp.blockPosition();
        var biome = level.getBiome(pos).unwrapKey().map(k -> k.location()).orElse(null);
        if (biome != null && !biome.equals(t.biome)) {
            if (t.biome != null) fire(new EventContext("biome_left").player(sp).id(t.biome).with("biome", t.biome));
            t.biome = biome;
            fire(new EventContext("biome").player(sp).id(biome).with("biome", biome));   // joining the world counts as entering
        }
        var now = structuresAt(level, pos);
        for (var s : now) if (!t.structures.contains(s)) fire(new EventContext("structure").player(sp).id(s).with("structure", s));
        for (var s : t.structures) if (!now.contains(s)) fire(new EventContext("structure_left").player(sp).id(s).with("structure", s));
        t.structures = now;
    }

    private static Set<ResourceLocation> structuresAt(ServerLevel level, BlockPos pos) {
        var out = new HashSet<ResourceLocation>();
        var registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        for (var structure : level.structureManager().getAllStructuresAt(pos).keySet()) {
            if (level.structureManager().getStructureWithPieceAt(pos, structure).isValid()) {
                var key = registry.getKey(structure);
                if (key != null) out.add(key);
            }
        }
        return out;
    }

    private static void checkKnowledge(ServerPlayer sp, PlayerTrack t) {
        sp.getCapability(IKnowledge.INSTANCE).ifPresent(k -> {
            var research = new HashSet<>(k.getKnownResearches());
            var facts = new HashSet<>(k.getKnownFacts());
            var signs = new HashSet<ResourceLocation>();
            for (var s : k.getKnownSigns()) signs.add(s.getRegistryName());
            var runes = new HashSet<ResourceLocation>();
            for (var r : k.getKnownRunes()) runes.add(r.getRegistryName());
            if (t.research != null) {                                    // the first snapshot only records
                diff("research", sp, t.research, research);
                diff("fact", sp, t.facts, facts);
                diff("sign", sp, t.signs, signs);
                diff("rune", sp, t.runes, runes);
            }
            t.research = research;
            t.facts = facts;
            t.signs = signs;
            t.runes = runes;
        });
    }

    private static void diff(String kind, ServerPlayer sp, Set<ResourceLocation> before, Set<ResourceLocation> after) {
        for (var id : after) if (!before.contains(id)) fire(new EventContext(kind).player(sp).id(id).with(kind, id));
    }

    // ---- Forge / Eidolon events ----

    @SubscribeEvent
    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp && !(sp instanceof FakePlayer)) {
            var id = event.getTo().location();
            fire(new EventContext("dimension").player(sp).id(id).with("dimension", id));
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        var killer = event.getSource().getEntity();
        if (killer instanceof Projectile p && p.getOwner() != null) killer = p.getOwner();
        ServerPlayer player = killer instanceof ServerPlayer sp && !(sp instanceof FakePlayer) ? sp : null;
        if (player == null && killer instanceof LivingEntity thrall && EntityUtil.isEnthralled(thrall)) {
            var owner = thrall.getPersistentData().getUUID(EntityUtil.THRALL_KEY);
            if (thrall.level() instanceof ServerLevel sl && sl.getPlayerByUUID(owner) instanceof ServerPlayer sp) player = sp;   // a thrall's kill is its master's
        }
        if (player == null) return;
        var victim = event.getEntity();
        var id = ForgeRegistries.ENTITY_TYPES.getKey(victim.getType());
        fire(new EventContext("kill").player(player).entity(victim).at(victim.level(), victim.blockPosition()).id(id).with("kill", id));
    }

    @SubscribeEvent
    public static void onPickup(PlayerEvent.ItemPickupEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp && !(sp instanceof FakePlayer)) {
            var stack = event.getStack();
            var id = ForgeRegistries.ITEMS.getKey(stack.getItem());
            fire(new EventContext("pickup").player(sp).item(stack).id(id));
        }
    }

    @SubscribeEvent
    public static void onCraft(PlayerEvent.ItemCraftedEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp && !(sp instanceof FakePlayer)) {
            var stack = event.getCrafting();
            fire(new EventContext("craft").player(sp).item(stack).id(ForgeRegistries.ITEMS.getKey(stack.getItem())));
        }
    }

    @SubscribeEvent
    public static void onEquip(LivingEquipmentChangeEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp && !(sp instanceof FakePlayer) && !event.getTo().isEmpty()
                && !net.minecraft.world.item.ItemStack.isSameItemSameTags(event.getFrom(), event.getTo())) {
            var stack = event.getTo();
            fire(new EventContext("equip").player(sp).item(stack).id(ForgeRegistries.ITEMS.getKey(stack.getItem())));
        }
    }

    @SubscribeEvent
    public static void onTame(AnimalTameEvent event) {
        if (event.getTamer() instanceof ServerPlayer sp && !(sp instanceof FakePlayer)) {
            var id = ForgeRegistries.ENTITY_TYPES.getKey(event.getAnimal().getType());
            fire(new EventContext("tame").player(sp).entity(event.getAnimal()).id(id).with("tame", id));
        }
    }

    private static final Map<UUID, LivingEntity> ENTHRALL_TARGETS = new HashMap<>();

    @SubscribeEvent
    public static void onCastPre(SpellCastEvent.Pre event) {
        if (event.spell instanceof ThrallSpell && event.player instanceof ServerPlayer sp
                && elucent.eidolon.common.spell.StaticSpell.rayTrace(sp, sp.getBlockReach() + 3, 0, false) instanceof EntityHitResult hit
                && hit.getEntity() instanceof LivingEntity living) {
            ENTHRALL_TARGETS.put(sp.getUUID(), living);
        }
    }

    @SubscribeEvent
    public static void onCastPost(SpellCastEvent.Post event) {
        if (!(event.player instanceof ServerPlayer sp) || sp instanceof FakePlayer || event.world.isClientSide()) return;
        var chant = event.spell.getRegistryName();
        fire(new EventContext("chant").player(sp).id(chant).with("chant", chant));
        var target = ENTHRALL_TARGETS.remove(sp.getUUID());
        if (event.spell instanceof ThrallSpell && target != null && EntityUtil.isEnthralledBy(target, sp)) {
            var id = ForgeRegistries.ENTITY_TYPES.getKey(target.getType());
            fire(new EventContext("enthrall").player(sp).entity(target).id(id).with("enthrall", id));
        }
    }

    @SubscribeEvent
    public static void onStage(ReputationEvent.Unlock event) {
        if (event.player instanceof ServerPlayer sp && !(sp instanceof FakePlayer)) {
            var deity = event.deity.getId();
            fire(new EventContext("stage").player(sp).id(event.stage.id()).with("deity", deity));
        }
    }

    /** Called by the brazier mixin when any ritual finishes; the performer is the nearest player within 16 blocks. */
    public static void ritualCompleted(ServerLevel level, BlockPos pos, ResourceLocation ritual) {
        Player nearest = level.getNearestPlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 16, false);
        var ctx = new EventContext("ritual").at(level, pos).id(ritual).with("ritual", ritual);
        if (nearest instanceof ServerPlayer sp && !(sp instanceof FakePlayer)) {
            ctx.player(sp);
            ctx.at(level, pos);
        }
        fire(ctx);
    }

    /** For scripts and tests: the players within a box (unused by detection). */
    static java.util.List<ServerPlayer> playersNear(ServerLevel level, BlockPos pos, double r) {
        return level.getEntitiesOfClass(ServerPlayer.class, new AABB(pos).inflate(r));
    }
}
