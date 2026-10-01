package com.bluelotuscoding.eidolonunchained.patron;

import com.bluelotuscoding.eidolonunchained.EUConfig;
import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import elucent.eidolon.api.deity.Deity;
import elucent.eidolon.api.deity.ReputationEvent;
import elucent.eidolon.capability.IReputation;
import elucent.eidolon.capability.ISoul;
import elucent.eidolon.common.deity.Deities;
import elucent.eidolon.network.Networking;
import elucent.eidolon.network.SoulUpdatePacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Patrons (decision D47, plan 2.1-2.3): one major patron per player plus any number of minor pledges, kept in the
 * player's persistent NBT ({@code eidolonunchained.patron {v:1, major, minor:[...]}}, which survives death).
 * <ul>
 *   <li><b>No patron, no reputation:</b> every {@code patronRequired} deity the player has not pledged to is held with
 *   Eidolon's own reputation lock under EU's key {@link #LOCK_KEY}. EU only locks an unlocked entry and only removes
 *   its own key, so a lock Eidolon (or another mod) placed is never touched.</li>
 *   <li><b>Mana per tier:</b> the highest {@code .maxMana} among the stages a player holds with a pledged deity is a
 *   floor under Eidolon's prayer-driven max mana. See {@link #refreshMana}.</li>
 * </ul>
 * Script settings arrive from {@code DeityBuilder} through {@link #declare}.
 */
@Mod.EventBusSubscriber(modid = EidolonUnchained.MOD_ID)
public final class Patrons {
    /** EU's key on Eidolon's per-deity reputation lock. */
    public static final ResourceLocation LOCK_KEY = new ResourceLocation(EidolonUnchained.MOD_ID, "patron");
    private static final String NBT_ROOT = EidolonUnchained.MOD_ID;
    private static final String NBT_PATRON = "patron";

    /** Script hook (KubeJS layer): (player, deity, previous major patron or null, granted). */
    public interface ChangeListener {
        void changed(ServerPlayer player, ResourceLocation deity, @Nullable ResourceLocation previous, boolean granted);
    }

    public static ChangeListener onChanged = (p, d, prev, g) -> {
    };

    private static final class Settings {
        Boolean required;
        boolean requiresCalling;
        final Map<ResourceLocation, Integer> stageMana = new HashMap<>();
    }

    private static final Map<ResourceLocation, Settings> SETTINGS = new HashMap<>();
    private static final Set<UUID> PENDING = new LinkedHashSet<>();

    private Patrons() {
    }

    // ---- script settings ----

    /**
     * Called by {@code DeityBuilder} when a deity (or an extension) registers. Null leaves a setting as an earlier
     * builder for the same deity left it; stage mana maps merge.
     */
    public static void declare(ResourceLocation deity, @Nullable Boolean required, @Nullable Boolean requiresCalling, Map<ResourceLocation, Integer> stageMana) {
        var s = SETTINGS.computeIfAbsent(deity, k -> new Settings());
        if (required != null) s.required = required;
        if (requiresCalling != null) s.requiresCalling = requiresCalling;
        s.stageMana.putAll(stageMana);
    }

    /** Whether the deity needs a pledge before it gives reputation (script setting; Eidolon's own deities follow the config; else true). */
    public static boolean isRequired(ResourceLocation deity) {
        var s = SETTINGS.get(deity);
        if (s != null && s.required != null) return s.required;
        if (deity.equals(Deities.LIGHT_DEITY_ID) || deity.equals(Deities.DARK_DEITY_ID)) return EUConfig.PATRON_REQUIRED_FOR_EIDOLON_DEITIES.get();
        return true;
    }

    /** Whether the deity's pledge ritual is only open to players it has called (stored now; callings use it later). */
    public static boolean requiresCalling(ResourceLocation deity) {
        var s = SETTINGS.get(deity);
        return s != null && s.requiresCalling;
    }

    // ---- state ----

    @Nullable
    public static ResourceLocation majorPatron(Player player) {
        var major = data(player).getString("major");
        return major.isEmpty() ? null : ResourceLocation.tryParse(major);
    }

    public static List<ResourceLocation> minorPledges(Player player) {
        var list = data(player).getList("minor", Tag.TAG_STRING);
        var out = new ArrayList<ResourceLocation>();
        for (int i = 0; i < list.size(); i++) {
            var rl = ResourceLocation.tryParse(list.getString(i));
            if (rl != null) out.add(rl);
        }
        return out;
    }

    /** Major patron and minor pledges together. */
    public static List<ResourceLocation> pledges(Player player) {
        var out = new ArrayList<ResourceLocation>();
        var major = majorPatron(player);
        if (major != null) out.add(major);
        for (var m : minorPledges(player)) if (!out.contains(m)) out.add(m);
        return out;
    }

    /** True when the deity is the player's major patron or one of their minor pledges. */
    public static boolean pledged(Player player, ResourceLocation deity) {
        return deity.equals(majorPatron(player)) || minorPledges(player).contains(deity);
    }

    /**
     * Pledges the player. A required deity becomes the major patron (refused while another one holds that place);
     * any other deity is added as a minor pledge. Returns null on success, otherwise the reason.
     */
    @Nullable
    public static Component pledge(ServerPlayer player, ResourceLocation deity) {
        if (Deities.find(deity) == null) return Component.translatable("eidolonunchained.patron.unknown_deity", deity.toString());
        if (pledged(player, deity)) return Component.translatable("eidolonunchained.patron.already_pledged", player.getDisplayName(), deity.toString());
        var previous = majorPatron(player);
        var root = data(player);
        if (isRequired(deity)) {
            if (previous != null) return Component.translatable("eidolonunchained.patron.other_major", player.getDisplayName(), previous.toString());
            root.putString("major", deity.toString());
        } else {
            var list = root.getList("minor", Tag.TAG_STRING);
            list.add(StringTag.valueOf(deity.toString()));
            root.put("minor", list);
        }
        save(player, root);
        applyLocks(player);
        refreshMana(player);
        fire(player, deity, previous, true);
        return null;
    }

    /**
     * Ends the pledge to one deity, or every pledge when {@code deity} is null, and leaves the player's reputation with
     * each revoked deity at {@code reputationAfter} (negative allowed: a grudge). Returns the deities revoked.
     */
    public static List<ResourceLocation> revoke(ServerPlayer player, @Nullable ResourceLocation deity, double reputationAfter) {
        var revoked = new ArrayList<ResourceLocation>();
        for (var d : pledges(player)) if (deity == null || d.equals(deity)) revoked.add(d);
        if (revoked.isEmpty()) return revoked;
        for (var d : revoked) {
            var previous = majorPatron(player);
            var root = data(player);
            if (d.equals(previous)) root.remove("major");
            var list = root.getList("minor", Tag.TAG_STRING);
            var out = new ListTag();
            for (int i = 0; i < list.size(); i++) if (!list.getString(i).equals(d.toString())) out.add(list.get(i));
            root.put("minor", out);
            save(player, root);
            setReputation(player, d, reputationAfter);
            applyLocks(player);
            fire(player, d, previous, false);
        }
        refreshMana(player);
        return revoked;
    }

    // ---- reputation lock ----

    @Nullable
    private static IReputation reputation(ServerPlayer player) {
        return player.server.overworld().getCapability(IReputation.INSTANCE).resolve().orElse(null);
    }

    /**
     * Locks every required deity the player has not pledged to (when its entry is unlocked) and lifts EU's own lock
     * wherever it no longer applies. Never removes or replaces a lock with another key.
     */
    public static void applyLocks(ServerPlayer player) {
        var rep = reputation(player);
        if (rep == null) return;
        var uuid = player.getUUID();
        var pledges = pledges(player);
        for (var deity : Deities.getDeities()) {
            var id = deity.getId();
            boolean hold = isRequired(id) && !pledges.contains(id);
            if (hold) {
                if (!rep.isLocked(uuid, id)) rep.lock(uuid, id, LOCK_KEY);
            } else if (rep.hasLock(uuid, id, LOCK_KEY)) {
                rep.unlock(uuid, id, LOCK_KEY);
            }
        }
    }

    /**
     * Sets reputation even while the entry is locked. EU's own lock is lifted for the write and put back after;
     * under another key's lock the value is reached without touching that lock (Eidolon accepts a negative set and
     * any lowering while locked; raising under a foreign lock is not possible and is logged).
     */
    public static void setReputation(ServerPlayer player, ResourceLocation deity, double value) {
        var rep = reputation(player);
        if (rep == null) return;
        var uuid = player.getUUID();
        double prev = rep.getReputation(uuid, deity);
        if (rep.hasLock(uuid, deity, LOCK_KEY)) {
            rep.unlock(uuid, deity, LOCK_KEY);
            rep.setReputation(uuid, deity, value);
            rep.lock(uuid, deity, LOCK_KEY);
        } else if (!rep.isLocked(uuid, deity) || value < 0) {
            rep.setReputation(uuid, deity, value);
        } else if (value <= prev) {
            rep.subtractReputation(uuid, deity, prev - value);
        } else {
            EidolonUnchained.LOGGER.warn("cannot raise {}'s reputation with '{}' to {}: another mod's lock holds it at {}",
                    player.getName().getString(), deity, value, prev);
        }
        if (rep.getReputation(uuid, deity) != prev) rep.considerChange(player, deity, prev);
    }

    // ---- mana per tier ----

    /** The highest {@code .maxMana} among the stages the player holds with deities they are pledged to (0 = none). */
    public static float manaFloor(ServerPlayer player) {
        var rep = reputation(player);
        if (rep == null) return 0;
        float floor = 0;
        for (var id : pledges(player)) {
            var s = SETTINGS.get(id);
            var deity = Deities.find(id);
            if (s == null || s.stageMana.isEmpty() || deity == null) continue;
            double value = rep.getReputation(player, id);
            for (var stage : deity.getProgression().getSteps().values()) {
                var mana = s.stageMana.get(stage.id());
                if (mana != null && mana > floor && holds(player, stage, value)) floor = mana;
            }
        }
        return floor;
    }

    /** A stage is held when the reputation has reached it and its requirements are met. */
    private static boolean holds(Player player, Deity.Stage stage, double rep) {
        if (rep < stage.rep()) return false;
        for (var req : stage.reqs()) {
            try {
                if (!req.isMet(player)) return false;
            } catch (RuntimeException e) {
                EidolonUnchained.LOGGER.error("stage '{}' requirement threw: {}", stage.id(), e.toString());
                return false;
            }
        }
        return true;
    }

    /**
     * Applies the mana floor. Bookkeeping in the patron NBT: {@code eidolon_max} is the max mana that EU did not set
     * (Eidolon only ever raises it, by prayer), {@code mana_floor} the floor last applied and {@code applied_max} the
     * max EU last left. A max that differs from {@code applied_max} was changed by someone else (a prayer, a command)
     * and becomes the new {@code eidolon_max}. The new max is {@code max(eidolon_max, floor)}; when it rises above what
     * the old floor gave, the new headroom is filled once; when it falls, current mana is clamped by Eidolon.
     */
    public static void refreshMana(ServerPlayer player) {
        var soul = player.getCapability(ISoul.INSTANCE).resolve().orElse(null);
        if (soul == null) return;
        var root = data(player);
        float current = soul.getMaxMagic();
        float eidolonMax = current;
        float oldFloor = 0;
        if (root.contains("applied_max") && Math.abs(root.getFloat("applied_max") - current) < 1e-3f) {
            eidolonMax = root.getFloat("eidolon_max");
            oldFloor = root.getFloat("mana_floor");
        }
        float floor = manaFloor(player);
        float newMax = Math.max(eidolonMax, floor);
        float fill = Math.max(0, newMax - Math.max(eidolonMax, oldFloor));
        root.putFloat("eidolon_max", eidolonMax);
        root.putFloat("mana_floor", floor);
        root.putFloat("applied_max", newMax);
        save(player, root);
        if (newMax == current && fill == 0) return;
        soul.setMaxMagic(newMax);                        // clamps current mana when the max drops
        if (fill > 0) soul.setMagic(soul.getMagic() + fill);
        syncSoul(player);
    }

    /** Sends the entity's soul to the clients that see it, as Eidolon does (the player too, for their own bar). */
    public static void syncSoul(LivingEntity entity) {
        if (entity.level().isClientSide()) return;
        if (entity instanceof ServerPlayer sp) Networking.sendTo(sp, new SoulUpdatePacket(sp));
        Networking.sendToTracking(entity.level(), entity.getOnPos(), new SoulUpdatePacket(entity));
    }

    // ---- events ----

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) refresh(sp);
    }

    /** Locks and mana for one player now. */
    public static void refresh(ServerPlayer player) {
        applyLocks(player);
        refreshMana(player);
    }

    // Reputation moves inside Eidolon's own call chain; the player is refreshed at the end of the tick, once.
    @SubscribeEvent
    public static void onReputationChange(ReputationEvent.Change event) {
        if (event.player instanceof ServerPlayer sp) PENDING.add(sp.getUUID());
    }

    @SubscribeEvent
    public static void onStageUnlock(ReputationEvent.Unlock event) {
        if (event.player instanceof ServerPlayer sp) PENDING.add(sp.getUUID());
    }

    @SubscribeEvent
    public static void onStageLock(ReputationEvent.Lock event) {
        if (event.player instanceof ServerPlayer sp) PENDING.add(sp.getUUID());
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || PENDING.isEmpty()) return;
        var ids = new ArrayList<>(PENDING);
        PENDING.clear();
        for (var id : ids) {
            var player = event.getServer().getPlayerList().getPlayer(id);
            if (player != null) refresh(player);
        }
    }

    // ---- internals ----

    private static void fire(ServerPlayer player, ResourceLocation deity, @Nullable ResourceLocation previous, boolean granted) {
        EidolonUnchained.LOGGER.debug("{} {} {}", player.getName().getString(), granted ? "pledged to" : "revoked", deity);
        try {
            onChanged.changed(player, deity, previous, granted);
        } catch (RuntimeException e) {
            EidolonUnchained.LOGGER.error("patronChanged listener threw: {}", e.toString());
        }
    }

    private static CompoundTag data(Player player) {
        return player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).getCompound(NBT_ROOT).getCompound(NBT_PATRON);
    }

    private static void save(Player player, CompoundTag patron) {
        patron.putInt("v", 1);
        var persisted = player.getPersistentData();
        var tag = persisted.getCompound(Player.PERSISTED_NBT_TAG);
        var root = tag.getCompound(NBT_ROOT);
        root.put(NBT_PATRON, patron);
        tag.put(NBT_ROOT, root);
        persisted.put(Player.PERSISTED_NBT_TAG, tag);
    }
}
