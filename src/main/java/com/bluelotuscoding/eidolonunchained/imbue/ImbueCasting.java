package com.bluelotuscoding.eidolonunchained.imbue;

import com.bluelotuscoding.eidolonunchained.EUConfig;
import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import com.bluelotuscoding.eidolonunchained.api.ChantSync;
import com.bluelotuscoding.eidolonunchained.api.ScriptedSpell;
import com.bluelotuscoding.eidolonunchained.casting.PlayerChantState;
import com.bluelotuscoding.eidolonunchained.network.EUNetwork;
import com.bluelotuscoding.eidolonunchained.network.MobChantStatePacket;
import elucent.eidolon.capability.IReputation;
import elucent.eidolon.capability.ISoul;
import elucent.eidolon.registries.EidolonSounds;
import elucent.eidolon.registries.Spells;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Server side of D35. <b>Imbued weapons:</b> a right-click casts the weapon's active chant from the player (D38: the
 * same wind-up, ring, mana and {@code SpellCastEvent}s as active chanting; the player need not know the signs);
 * the view key + right-click cycles the active chant. <b>Deity's Protection:</b> when a wearer is hurt by a living
 * attacker, the highest-level piece whose bound chant is ready casts that chant's targeted path at the attacker,
 * charging the wearer a share of the mana; the chant's signs flash as a ring over the wearer.
 */
@Mod.EventBusSubscriber(modid = EidolonUnchained.MOD_ID)
public final class ImbueCasting {
    /** Script hooks (KubeJS layer): return false to cancel. */
    public interface ImbueHook {
        boolean test(ServerPlayer player, ItemStack weapon, ResourceLocation chant);
    }

    public interface ProtectionHook {
        boolean test(LivingEntity wearer, LivingEntity attacker, ItemStack piece, ResourceLocation chant, int level);
    }

    public static ImbueHook onImbueCast = (p, w, c) -> true;
    public static ProtectionHook onProtection = (w, a, p, c, l) -> true;

    private static final EquipmentSlot[] PROTECTION_SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND};
    private static final Map<LivingEntity, EnumMap<EquipmentSlot, Long>> COOLDOWNS = new WeakHashMap<>();
    private static final Map<LivingEntity, Long> FLASHES = new WeakHashMap<>();
    private static final Set<ResourceLocation> WARNED = new HashSet<>();

    private ImbueCasting() {
    }

    // ---- imbued weapons ----

    /** The imbued weapon in the player's hands (main hand first), or empty. */
    public static ItemStack heldImbued(Player player) {
        var main = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (ImbueNbt.isImbued(main)) return main;
        var off = player.getItemInHand(InteractionHand.OFF_HAND);
        return ImbueNbt.isImbued(off) ? off : ItemStack.EMPTY;
    }

    public static boolean castImbued(ServerPlayer player, ItemStack weapon) {
        var id = ImbueNbt.activeChant(weapon);
        if (id == null) return false;
        if (player.getCooldowns().isOnCooldown(weapon.getItem())) return false;
        var spell = Spells.find(id);
        if (spell == null) {
            player.displayClientMessage(Component.translatable("eidolonunchained.imbue.unknown_chant", id.toString()), true);
            return false;
        }
        if (PlayerChantState.of(player).isBusy()) return false;               // already chanting something
        if (!onImbueCast.test(player, weapon, id)) return false;
        boolean ok = PlayerChantState.of(player).castSpell(spell, ChantSync.sequenceOf(id));   // builds up, winds up, casts
        if (ok) player.getCooldowns().addCooldown(weapon.getItem(), EUConfig.IMBUE_CAST_COOLDOWN_TICKS.get());
        return ok;
    }

    public static void cycleImbued(ServerPlayer player, ItemStack weapon) {
        var chants = ImbueNbt.chants(weapon);
        if (chants.size() < 2) return;
        int next = (ImbueNbt.active(weapon) + 1) % chants.size();
        ImbueNbt.setActive(weapon, next);
        player.displayClientMessage(Component.translatable("eidolonunchained.imbue.active", ImbueNbt.chantName(chants.get(next))), true);
        player.level().playSound(null, player.blockPosition(), EidolonSounds.CHANT_WORD.get(), SoundSource.PLAYERS, 0.5f, 1.3f);
    }

    // ---- Deity's Protection ----

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        var wearer = event.getEntity();
        if (wearer.level().isClientSide() || event.getAmount() <= 0) return;
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker) || attacker == wearer) return;
        var pieces = new ArrayList<ItemStack[]>();                       // [piece], ordered by level (highest first)
        var slots = new ArrayList<EquipmentSlot>();
        for (var slot : PROTECTION_SLOTS) {
            var piece = wearer.getItemBySlot(slot);
            if (ImbueNbt.protectionLevel(piece) > 0 && ImbueNbt.protectionChant(piece) != null) { pieces.add(new ItemStack[]{piece}); slots.add(slot); }
        }
        if (pieces.isEmpty()) return;
        var order = new ArrayList<Integer>();
        for (int i = 0; i < pieces.size(); i++) order.add(i);
        order.sort((a, b) -> Integer.compare(ImbueNbt.protectionLevel(pieces.get(b)[0]), ImbueNbt.protectionLevel(pieces.get(a)[0])));
        for (int i : order) {
            if (retaliate(wearer, attacker, pieces.get(i)[0], slots.get(i))) break;       // one retaliation per hit
        }
    }

    private static boolean retaliate(LivingEntity wearer, LivingEntity attacker, ItemStack piece, EquipmentSlot slot) {
        int level = ImbueNbt.protectionLevel(piece);
        var id = ImbueNbt.protectionChant(piece);
        if (id == null) return false;
        long now = wearer.level().getGameTime();
        var cds = COOLDOWNS.computeIfAbsent(wearer, k -> new EnumMap<>(EquipmentSlot.class));
        if (cds.getOrDefault(slot, 0L) > now) return skip(wearer, id, "cooldown");
        if (!(Spells.find(id) instanceof ScriptedSpell spell) || spell.deity() == null) return skip(wearer, id, "not a deity-bound scripted chant");
        if (!spell.hasMobPath()) {
            if (WARNED.add(id)) EidolonUnchained.LOGGER.warn("Deity's Protection: chant '{}' has no .targetCast/.mobCast, so it cannot retaliate", id);
            return false;
        }
        if (wearer instanceof ServerPlayer sp) {                                     // the deity must still answer
            double rep = sp.server.overworld().getCapability(IReputation.INSTANCE).resolve().map(r -> r.getReputation(sp, spell.deity())).orElse(0.0);
            if (rep < spell.minReputation()) return skip(wearer, id, "reputation " + rep + " < " + spell.minReputation());
        }
        var shares = EUConfig.PROTECTION_MANA_SHARE.get();
        double share = shares.isEmpty() ? 1.0 : shares.get(Math.min(level, shares.size()) - 1);
        int cost = (int) Math.ceil(spell.getCost() * share);
        var soul = wearer.getCapability(ISoul.INSTANCE).resolve().orElse(null);
        // creative players pay nothing; a mob without a mana pool (no caster profile) is carried by the deity
        boolean free = (wearer instanceof Player p && p.isCreative()) || (!(wearer instanceof Player) && (soul == null || soul.getMaxMagic() <= 0));
        if (!free && cost > 0 && (soul == null || soul.getMagic() < cost)) return skip(wearer, id, "not enough mana (" + (soul == null ? "no soul" : soul.getMagic() + "/" + cost) + ")");
        if (!onProtection.test(wearer, attacker, piece, id, level)) return skip(wearer, id, "cancelled by a script");
        if (!free && cost > 0) soul.setMagic(soul.getMagic() - cost);
        var cools = EUConfig.PROTECTION_COOLDOWN_TICKS.get();
        cds.put(slot, now + (cools.isEmpty() ? 100 : cools.get(Math.min(level, cools.size()) - 1)));
        // every cast builds up: the signs one by one, the wind-up, then the targeted path at the attacker
        Runnable cast = () -> {
            if (!attacker.isAlive()) return;
            if (!spell.castAt(wearer.level(), wearer, attacker)) spell.castByMob(wearer.level(), wearer.blockPosition(), wearer, attacker);
        };
        var signs = ChantSync.sequenceOf(id);
        if (wearer instanceof ServerPlayer sp) {
            if (!PlayerChantState.of(sp).castSpell(spell, signs, cast)) { cds.remove(slot); return false; }   // busy chanting: try the next hit
        } else {
            build(wearer, signs, spell.getDelay(), cast);
        }
        return true;
    }

    private static boolean skip(LivingEntity wearer, ResourceLocation chant, String why) {
        EidolonUnchained.LOGGER.debug("Deity's Protection on {}: {} not cast: {}", wearer.getName().getString(), chant, why);
        return false;
    }

    // ---- mob build-up: the same ring, the signs appearing one by one, then the wind-up, then the action ----

    private static final class Build {
        final List<String> signs; final int windup; final Runnable action;
        int shown = 0; long nextAt; boolean winding = false;
        Build(List<String> signs, int windup, Runnable action) { this.signs = signs; this.windup = windup; this.action = action; }
    }

    private static final Map<LivingEntity, Build> BUILDS = new WeakHashMap<>();

    /** Builds a chant over a non-player entity: signs every {@code chantBuildSignDelayTicks}, then the wind-up, then {@code action}. */
    public static void build(LivingEntity entity, List<elucent.eidolon.api.spells.Sign> signs, int windup, Runnable action) {
        if (BUILDS.containsKey(entity)) return;
        var b = new Build(signs.stream().map(s -> s.getRegistryName().toString()).toList(), Math.max(0, windup), action);
        b.nextAt = entity.level().getGameTime();
        BUILDS.put(entity, b);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!BUILDS.isEmpty()) {
            for (Iterator<Map.Entry<LivingEntity, Build>> it = BUILDS.entrySet().iterator(); it.hasNext(); ) {
                var e = it.next();
                var entity = e.getKey(); var b = e.getValue();
                if (entity == null || !entity.isAlive()) { it.remove(); continue; }
                long t = entity.level().getGameTime();
                if (t < b.nextAt) continue;
                if (b.shown < b.signs.size()) {
                    b.shown++;
                    EUNetwork.sendToTrackingAndSelf(entity, new MobChantStatePacket(entity.getId(), b.signs.subList(0, b.shown)));
                    if (entity.level() instanceof ServerLevel sl)
                        sl.playSound(null, entity.blockPosition(), EidolonSounds.CHANT_WORD.get(), SoundSource.HOSTILE, 0.7f, entity.getRandom().nextFloat() * 0.375f + 0.625f);
                    b.nextAt = t + EUConfig.CHANT_BUILD_SIGN_DELAY_TICKS.get();
                    if (b.shown == b.signs.size()) { b.nextAt = t + b.windup; b.winding = true; }
                } else {
                    EUNetwork.sendToTrackingAndSelf(entity, new MobChantStatePacket(entity.getId(), List.of()));
                    it.remove();
                    try { b.action.run(); } catch (RuntimeException ex) { EidolonUnchained.LOGGER.error("chant build action threw: {}", ex.toString()); }
                }
            }
        }
        if (FLASHES.isEmpty()) return;
        for (Iterator<Map.Entry<LivingEntity, Long>> it = FLASHES.entrySet().iterator(); it.hasNext(); ) {
            var e = it.next();
            var entity = e.getKey();
            if (entity == null || !entity.isAlive() || entity.level().getGameTime() >= e.getValue()) {
                if (entity != null) EUNetwork.sendToTrackingAndSelf(entity, new MobChantStatePacket(entity.getId(), List.of()));
                it.remove();
            }
        }
    }

    /** The chant's signs as a ring over the entity for a moment (scripts). */
    public static void flash(LivingEntity entity, ResourceLocation chant) {
        var signs = ChantSync.sequenceOf(chant).stream().map(s -> s.getRegistryName().toString()).toList();
        if (signs.isEmpty()) return;
        EUNetwork.sendToTrackingAndSelf(entity, new MobChantStatePacket(entity.getId(), signs));
        FLASHES.put(entity, entity.level().getGameTime() + 16);
    }

    /** Script helper: the bound chant of a Deity's Protection piece the entity wears in that slot, or null. */
    public static @Nullable ResourceLocation protectionOf(LivingEntity entity, EquipmentSlot slot) {
        var piece = entity.getItemBySlot(slot);
        return ImbueNbt.protectionLevel(piece) > 0 ? ImbueNbt.protectionChant(piece) : null;
    }
}
