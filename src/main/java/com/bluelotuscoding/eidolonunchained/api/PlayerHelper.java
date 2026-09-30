package com.bluelotuscoding.eidolonunchained.api;

import dev.latvian.mods.kubejs.typings.Info;
import elucent.eidolon.api.deity.Deity;
import elucent.eidolon.capability.IReputation;
import elucent.eidolon.common.deity.Deities;
import elucent.eidolon.registries.Researches;
import elucent.eidolon.registries.Runes;
import elucent.eidolon.registries.Signs;
import elucent.eidolon.util.KnowledgeUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.List;

/**
 * {@code player.eidolon}: knowledge (signs, runes, research, facts), reputation and the soul, all through Eidolon's
 * own {@code KnowledgeUtil}, {@code IReputation} and {@code ISoul}. Attached to every player by the KubeJS plugin.
 * Reputation lives on the server's overworld capability, as Eidolon's own commands read it, so those calls need a
 * server-side player.
 */
public final class PlayerHelper {
    private final Player player;

    public PlayerHelper(Player player) {
        this.player = player;
    }

    // ---- knowledge ----

    public boolean knowsSign(String id) {
        return KnowledgeUtil.knowsSign(player, requireSign(id));
    }

    public void grantSign(String id) {
        KnowledgeUtil.grantSign(player, requireSign(id));
    }

    public void removeSign(String id) {
        KnowledgeUtil.removeSign(player, requireSign(id));
    }

    @Info("Ids of the signs this player knows")
    public List<String> knownSigns() {
        return KnowledgeUtil.getKnownSigns(player).stream().map(s -> s.getRegistryName().toString()).toList();
    }

    public boolean knowsRune(String id) {
        return KnowledgeUtil.knowsRune(player, requireRune(id));
    }

    public void grantRune(String id) {
        KnowledgeUtil.grantRune(player, requireRune(id));
    }

    public void removeRune(String id) {
        KnowledgeUtil.removeRune(player, requireRune(id));
    }

    public boolean knowsResearch(String id) {
        return KnowledgeUtil.knowsResearch(player, Ids.of(id, "research"));
    }

    @Info("Grant research with Eidolon's toast")
    public void grantResearch(String id) {
        var rl = Ids.of(id, "research");
        var research = Researches.find(rl);
        if (research == null) throw new IllegalArgumentException("Eidolon Unchained: unknown research '" + id + "'");
        KnowledgeUtil.grantResearch(player, research);
    }

    @Info("Grant research without the toast")
    public void grantResearchSilently(String id) {
        KnowledgeUtil.grantResearchNoToast(player, Ids.of(id, "research"));
    }

    public void removeResearch(String id) {
        KnowledgeUtil.removeResearch(player, Ids.of(id, "research"));
    }

    public boolean knowsFact(String id) {
        return KnowledgeUtil.knowsFact(player, Ids.of(id, "fact"));
    }

    public void grantFact(String id) {
        KnowledgeUtil.grantFact(player, Ids.of(id, "fact"));
    }

    public void removeFact(String id) {
        KnowledgeUtil.removeFact(player, Ids.of(id, "fact"));
    }

    // ---- reputation ----

    @Info("Reputation with a deity (IReputation.getReputation)")
    public double reputation(String deityId) {
        return rep().getReputation(player, requireDeity(deityId).getId());
    }

    @Info("Add reputation; Eidolon fires its change/unlock events and the deity reacts")
    public void addReputation(String deityId, double amount) {
        rep().addReputation(player, requireDeity(deityId).getId(), amount);
    }

    public void subtractReputation(String deityId, double amount) {
        rep().subtractReputation(player.getUUID(), requireDeity(deityId).getId(), amount);
    }

    public void setReputation(String deityId, double amount) {
        rep().setReputation(player.getUUID(), requireDeity(deityId).getId(), amount);
    }

    public boolean isLocked(String deityId) {
        return rep().isLocked(player.getUUID(), requireDeity(deityId).getId());
    }

    @Info("Id of the player's current stage with the deity, from its progression")
    public String stage(String deityId) {
        var deity = requireDeity(deityId);
        var current = deity.getProgression().last(reputation(deityId));
        return current == null ? null : current.id().toString();
    }

    // ---- soul ----

    public SoulHelper soul() {
        return SoulHelper.of(player);
    }

    public void expendMana(int amount) {
        soul().expendMana(amount);
    }

    // ---- internals ----

    private IReputation rep() {
        if (!(player instanceof ServerPlayer sp)) throw new IllegalStateException("Eidolon Unchained: reputation is read on the server (this is a client player)");
        return sp.server.overworld().getCapability(IReputation.INSTANCE).resolve()
                .orElseThrow(() -> new IllegalStateException("Eidolon Unchained: Eidolon's reputation capability is missing"));
    }

    static Deity requireDeity(String id) {
        var d = Deities.find(Ids.of(id, "deity"));
        if (d == null) throw new IllegalArgumentException("Eidolon Unchained: unknown deity '" + id + "'");
        return d;
    }

    private static elucent.eidolon.api.spells.Sign requireSign(String id) {
        var s = Signs.find(Ids.of(id, "sign"));
        if (s == null) throw new IllegalArgumentException("Eidolon Unchained: unknown sign '" + id + "'");
        return s;
    }

    private static elucent.eidolon.api.spells.Rune requireRune(String id) {
        var r = Runes.find(Ids.of(id, "rune"));
        if (r == null) throw new IllegalArgumentException("Eidolon Unchained: unknown rune '" + id + "'");
        return r;
    }

    static ResourceLocation rl(String id, String what) {
        return Ids.of(id, what);
    }
}
