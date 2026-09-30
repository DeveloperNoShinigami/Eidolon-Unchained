package com.bluelotuscoding.eidolonunchained.casting;

import com.bluelotuscoding.eidolonunchained.EUConfig;
import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import com.bluelotuscoding.eidolonunchained.api.ChantSync;
import com.bluelotuscoding.eidolonunchained.api.Ids;
import com.bluelotuscoding.eidolonunchained.network.ChantStatePacket;
import com.bluelotuscoding.eidolonunchained.network.EUNetwork;
import elucent.eidolon.api.spells.Sign;
import elucent.eidolon.api.spells.SignSequence;
import elucent.eidolon.api.spells.Spell;
import elucent.eidolon.client.particle.SignParticleData;
import elucent.eidolon.network.Networking;
import elucent.eidolon.network.SpellCastPacket;
import elucent.eidolon.registries.EidolonSounds;
import elucent.eidolon.registries.Signs;
import elucent.eidolon.registries.Spells;
import elucent.eidolon.util.KnowledgeUtil;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Active chanting (D37): the server owns the sequence (rule C1). Each sign key adds a sign, shown at once as Eidolon's
 * sign particle in front of the player with the chant-word sound. As soon as the sequence matches a chant it fires
 * directly from the player (D38) — after a short commit window when a longer chant also starts with these signs,
 * immediately otherwise. A sequence no chant starts with fizzles. No cast key. Slot assignments persist in the player's
 * Forge persistent NBT ({@code eidolonunchained.chant_slots}, v1).
 */
public final class PlayerChantState {
    public static final int SLOTS = 9;
    public static final String NBT_ROOT = EidolonUnchained.MOD_ID;

    public enum ClearReason { PLAYER, CAST, TIMEOUT, FIZZLE, LOGOUT, SCRIPT }

    /** Script hooks (set by the KubeJS layer): return false to cancel. */
    public static BiFunction<ServerPlayer, Sign, Boolean> onSign = (p, s) -> true;
    public static BiFunction<ServerPlayer, List<Sign>, Boolean> onCast = (p, s) -> true;
    public static Function<ServerPlayer, Void> onCleared = p -> null;

    private static final Map<UUID, PlayerChantState> STATES = new HashMap<>();

    private final ServerPlayer player;
    private final List<Sign> sequence = new ArrayList<>();
    private final ResourceLocation[] slots = new ResourceLocation[SLOTS];
    private long lastInputTick;
    private long commitAt = -1;                 // game time at which a matched sequence fires (-1 = none pending)
    private @Nullable Spell winding;            // the spell being wound up (D38: cast directly from the player)
    private @Nullable SignSequence windingSeq;
    private long castAt = -1;
    private final List<Sign> building = new ArrayList<>();   // signs still to show for a scripted/imbued cast
    private @Nullable Spell buildingSpell;
    private long nextSignAt = -1;
    private @Nullable Runnable releaseAction;

    private PlayerChantState(ServerPlayer player) {
        this.player = player;
        loadSlots();
    }

    public static PlayerChantState of(ServerPlayer player) {
        return STATES.computeIfAbsent(player.getUUID(), k -> new PlayerChantState(player));
    }

    public static @Nullable PlayerChantState peek(UUID id) {
        return STATES.get(id);
    }

    public static void forget(UUID id) {
        STATES.remove(id);
    }

    // ---- sequence ----

    public List<Sign> sequence() {
        return Collections.unmodifiableList(sequence);
    }

    public void pressSlot(int slot) {
        if (slot < 0 || slot >= SLOTS || slots[slot] == null) return;
        var sign = Signs.find(slots[slot]);
        if (sign == null) return;
        addSign(sign);
    }

    public void addSign(Sign sign) {
        if (isBusy()) return;                                           // a chant is already building or winding up
        if (!KnowledgeUtil.knowsSign(player, sign)) return;             // Eidolon's rule: only known signs
        if (sequence.size() >= EUConfig.MAX_CHANT_LENGTH.get()) { fizzle(); return; }
        if (!onSign.apply(player, sign)) return;
        sequence.add(sign);
        lastInputTick = now();
        showSign(sign);
        var match = resolved();
        boolean longer = ChantSync.hasLongerSequenceStartingWith(sequence);
        if (match != null) {
            commitAt = longer ? now() + EUConfig.CHANT_COMMIT_DELAY_TICKS.get() : now();
            if (!longer) fire();
        } else if (!longer) {
            fizzle();
        } else {
            commitAt = -1;
        }
        sync();
    }

    public void clear(ClearReason reason) {
        commitAt = -1;
        if (reason == ClearReason.PLAYER || reason == ClearReason.LOGOUT) {
            winding = null; windingSeq = null; castAt = -1;
            building.clear(); buildingSpell = null; releaseAction = null;
        }
        if (sequence.isEmpty()) return;
        sequence.clear();
        if (reason != ClearReason.LOGOUT) {
            onCleared.apply(player);
            sync();
        }
    }

    /** Casts now, whatever the sequence: used by scripts ({@code chant.cast()}). */
    public boolean cast() {
        if (sequence.isEmpty()) return false;
        return fire();
    }

    /**
     * D38: the chant is cast from the player, not from a spawned chanter entity. Same steps Eidolon's entity performs:
     * resolve, wind up for the spell's delay, canCast (Eidolon's mana check + SpellCastEvent.Pre), cast (the spell spends
     * its mana + SpellCastEvent.Post), and Eidolon's SpellCastPacket so tracking clients get the cast visuals.
     */
    private boolean fire() {
        commitAt = -1;
        var seq = new SignSequence(sequence);
        var spell = Spells.find(seq, player.level());
        if (spell == null) { fizzle(); return false; }
        if (!onCast.apply(player, List.copyOf(sequence))) { clear(ClearReason.PLAYER); return false; }
        startWindup(spell, seq);
        return true;
    }

    /**
     * D35/D38: cast a given chant from the player (imbued weapons, Deity's Protection, scripts). The chant is
     * <em>built up</em> like any other: its signs appear one by one (config {@code chantBuildSignDelayTicks}), then
     * the wind-up, then the cast; the player need not know the signs. {@code action} replaces the player cast path
     * on release (Deity's Protection casts the targeted path at the attacker); null = the normal canCast/cast.
     * False while another chant is building or winding up.
     */
    public boolean castSpell(Spell spell, List<Sign> signs, @Nullable Runnable action) {
        if (winding != null || buildingSpell != null) return false;
        sequence.clear();
        commitAt = -1;
        building.clear();
        building.addAll(signs);
        buildingSpell = spell;
        releaseAction = action;
        nextSignAt = now();
        tick();                                  // the first sign shows at once
        return true;
    }

    public boolean castSpell(Spell spell, List<Sign> signs) {
        return castSpell(spell, signs, null);
    }

    public boolean isWinding() {
        return winding != null;
    }

    public boolean isBusy() {
        return winding != null || buildingSpell != null;
    }

    private void startWindup(Spell spell, SignSequence seq) {
        winding = spell;
        windingSeq = seq;
        castAt = now() + Math.max(0, spell.getDelay());
        sequence.clear();
        sync();
        if (castAt <= now()) release();
    }

    private void release() {
        var spell = winding; var seq = windingSeq; var action = releaseAction;
        winding = null; windingSeq = null; castAt = -1; releaseAction = null;
        sync();                                                             // the ring disappears on release
        if (spell == null || seq == null) return;
        var level = player.level();
        var pos = player.blockPosition();
        if (action != null) {
            action.run();
            Networking.sendToTracking(level, pos, new SpellCastPacket(player, pos, spell, seq));
        } else if (spell.canCast(level, pos, player, seq)) {
            spell.cast(level, pos, player, seq);
            Networking.sendToTracking(level, pos, new SpellCastPacket(player, pos, spell, seq));
        } else if (level instanceof ServerLevel sl) {
            sl.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 1.0f, 1.0f);
        }
    }

    private void fizzle() {
        if (player.level() instanceof ServerLevel sl) {
            var look = player.getLookAngle();
            sl.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE, player.getX() + look.x * 0.8, player.getEyeY() - 0.2, player.getZ() + look.z * 0.8, 8, 0.2, 0.2, 0.2, 0.01);
            sl.playSound(null, player.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.5f, 1.4f);
        }
        clear(ClearReason.FIZZLE);
    }

    private void showSign(Sign sign) {
        if (!(player.level() instanceof ServerLevel sl)) return;
        sl.playSound(null, player.blockPosition(), EidolonSounds.CHANT_WORD.get(), SoundSource.PLAYERS, 0.7f, player.getRandom().nextFloat() * 0.375f + 0.625f);
    }

    /** The spell the current sequence resolves to, or null (never casts). */
    public @Nullable Spell resolved() {
        if (buildingSpell != null) return buildingSpell;
        if (sequence.isEmpty()) return null;
        return Spells.find(new SignSequence(sequence), player.level());
    }

    /** Every tick: release a wound-up cast; fire a pending match once its commit window passed; fizzle an idle sequence. */
    public void tick() {
        long t = now();
        if (buildingSpell != null) {                                        // a scripted/imbued chant building its signs
            if (t >= nextSignAt) {
                if (building.isEmpty()) {
                    var spell = buildingSpell; buildingSpell = null;
                    startWindup(spell, new SignSequence(new ArrayList<>(sequence)));
                } else {
                    var sign = building.remove(0);
                    sequence.add(sign);
                    showSign(sign);
                    nextSignAt = t + EUConfig.CHANT_BUILD_SIGN_DELAY_TICKS.get();
                    sync();
                }
            }
            return;
        }
        if (winding != null) {
            if (t >= castAt) release();
            return;                                                         // the ring renderer shows the wind-up (D41)
        }
        if (sequence.isEmpty()) return;
        if (commitAt >= 0 && t >= commitAt) { fire(); return; }
        if (t - lastInputTick > EUConfig.CHANT_IDLE_CLEAR_SECONDS.get() * 20L) fizzle();
    }

    private long now() {
        return player.level().getGameTime();
    }

    // ---- slots ----

    public List<String> slotIds() {
        var out = new ArrayList<String>(SLOTS);
        for (var s : slots) out.add(s == null ? "" : s.toString());
        return out;
    }

    public void assign(int slot, @Nullable String signId) {
        if (slot < 0 || slot >= SLOTS) return;
        if (signId == null || signId.isBlank()) {
            slots[slot] = null;
        } else {
            var rl = Ids.of(signId, "sign");
            var sign = Signs.find(rl);
            if (sign == null || !KnowledgeUtil.knowsSign(player, sign)) return;   // assign only known signs
            slots[slot] = rl;
        }
        saveSlots();
        sync();
    }

    private void loadSlots() {
        var root = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).getCompound(NBT_ROOT);
        var list = root.getList("chant_slots", Tag.TAG_STRING);
        for (int i = 0; i < SLOTS && i < list.size(); i++) {
            var s = list.getString(i);
            slots[i] = s.isEmpty() ? null : ResourceLocation.tryParse(s);
        }
    }

    private void saveSlots() {
        var persisted = player.getPersistentData();
        var tag = persisted.getCompound(Player.PERSISTED_NBT_TAG);
        var root = tag.getCompound(NBT_ROOT);
        root.putInt("v", 1);
        var list = new ListTag();
        for (var s : slots) list.add(StringTag.valueOf(s == null ? "" : s.toString()));
        root.put("chant_slots", list);
        tag.put(NBT_ROOT, root);
        persisted.put(Player.PERSISTED_NBT_TAG, tag);
    }

    // ---- sync (client-side state for scripts and future presentation; no HUD is drawn, D37) ----

    public void sync() {
        boolean isWinding = winding != null && windingSeq != null;
        var shown = isWinding ? java.util.Arrays.asList(windingSeq.toArray()) : sequence;
        var spell = isWinding ? winding : resolved();
        var ids = shown.stream().map(s -> s.getRegistryName().toString()).toList();
        EUNetwork.sendToTrackingAndSelf(player, new ChantStatePacket(player.getId(), isWinding, ids,
                spell == null ? "" : spell.getRegistryName().toString(), spell == null ? 0 : spell.getCost(), slotIds()));
    }
}
