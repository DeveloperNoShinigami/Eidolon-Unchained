package com.bluelotuscoding.eidolonunchained.api;

import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import dev.latvian.mods.kubejs.typings.Info;
import elucent.eidolon.api.deity.Deity;
import elucent.eidolon.api.research.Research;
import elucent.eidolon.common.deity.Deities;
import elucent.eidolon.registries.Researches;
import elucent.eidolon.registries.Signs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * {@code EidolonUnchained.deity(id)} creates a custom deity ({@code Deities.register(new ScriptedDeity(...))});
 * {@code EidolonUnchained.extendDeity('eidolon:light')} adds stages, requirements, a model and reactions to an existing
 * one through the same public {@code Deity.Progression} API. Reactions registered here are also fired as the global
 * {@code EidolonUnchainedEvents.stageUnlocked} / {@code reputationChanged} events (decision D29).
 */
public final class DeityBuilder {
    final ResourceLocation id;
    final boolean extend;
    Integer color;
    ResourceLocation model;
    Integer maxReputation;
    final List<StageDecl> stages = new ArrayList<>();
    final List<BiConsumer<Player, String>> onUnlock = new ArrayList<>();
    final List<BiConsumer<Player, String>> onLock = new ArrayList<>();
    final List<ReputationListener> onChange = new ArrayList<>();

    public interface ReputationListener {
        void changed(Player player, double oldRep, double newRep);
    }

    static final class StageDecl {
        final ResourceLocation id;
        final int rep;
        final boolean major;
        final List<Deity.StageRequirement> reqs = new ArrayList<>();
        final List<ResourceLocation> signReqs = new ArrayList<>();

        StageDecl(ResourceLocation id, int rep, boolean major) {
            this.id = id;
            this.rep = rep;
            this.major = major;
        }
    }

    DeityBuilder(ResourceLocation id, boolean extend) {
        this.id = id;
        this.extend = extend;
        EURegistry.declare(EURegistry.Stage.DEITIES, id, extend ? "deity extension" : "deity", this::register);
    }

    @Info("Signature colour (custom deities only; Eidolon owns the colour of its own deities)")
    public DeityBuilder color(int r, int g, int b) {
        if (extend) throw new IllegalStateException("Eidolon Unchained: cannot recolour existing deity '" + id + "'");
        this.color = Ids.rgb(r, g, b);
        return this;
    }

    public DeityBuilder color(int packedRgb) {
        if (extend) throw new IllegalStateException("Eidolon Unchained: cannot recolour existing deity '" + id + "'");
        this.color = Ids.rgb(packedRgb);
        return this;
    }

    @Info("GeckoLib model id for the deity's avatar (plain id in Phase 2; Phase 6 gives it meaning)")
    public DeityBuilder model(String modelId) {
        this.model = Ids.of(modelId, "deity model");
        return this;
    }

    @Info("Add a progression stage: id, reputation needed, whether it is a major stage")
    public DeityBuilder stage(String stageId, int reputation, boolean major) {
        stages.add(new StageDecl(Ids.newId(stageId, "stage"), reputation, major));
        return this;
    }

    @Info("The last added stage also needs this research")
    public DeityBuilder requireResearch(String researchId) {
        lastStage().reqs.add(new Deity.ResearchRequirement(Ids.of(researchId, "research")));
        return this;
    }

    @Info("The last added stage also needs this sign known")
    public DeityBuilder requireSign(String signId) {
        var rl = Ids.of(signId, "sign");
        // Signs register in an earlier stage than deities (T1 order), so the lookup is resolved at registration time.
        lastStage().signReqs.add(rl);
        return this;
    }

    @dev.latvian.mods.kubejs.typings.Info("A condition (EidolonUnchained.conditions) the last .stage(...) also requires; the stage stays locked until it holds")
    public DeityBuilder require(com.bluelotuscoding.eidolonunchained.api.condition.Condition condition) {
        lastStage().reqs.add(player -> condition.test(player));
        return this;
    }

    public DeityBuilder maxReputation(int max) {
        this.maxReputation = max;
        return this;
    }

    @Info("(player, stageId) => …, when the player unlocks a stage of this deity")
    public DeityBuilder onStageUnlocked(BiConsumer<Player, String> fn) {
        onUnlock.add(fn);
        return this;
    }

    @Info("(player, stageId) => …, when the player loses a stage of this deity")
    public DeityBuilder onStageLocked(BiConsumer<Player, String> fn) {
        onLock.add(fn);
        return this;
    }

    @Info("(player, oldRep, newRep) => …, when the player's reputation with this deity changes")
    public DeityBuilder onReputationChanged(ReputationListener fn) {
        onChange.add(fn);
        return this;
    }

    private StageDecl lastStage() {
        if (stages.isEmpty()) throw new IllegalStateException("Eidolon Unchained: deity '" + id + "': add a .stage(...) before its requirements");
        return stages.get(stages.size() - 1);
    }

    private void register(ResourceLocation id) {
        Deity deity;
        if (extend) {
            deity = Deities.find(id);
            if (deity == null) throw new IllegalStateException("extendDeity('" + id + "'): no such deity is registered");
        } else {
            if (Deities.find(id) != null) throw new IllegalStateException("a deity with id '" + id + "' already exists (use extendDeity)");
            if (color == null) throw new IllegalStateException("deity '" + id + "' has no .color(...)");
            deity = Deities.register(new ScriptedDeity(id, (color >> 16) & 255, (color >> 8) & 255, color & 255));
        }
        for (var s : stages) {
            var stage = new Deity.Stage(s.id, s.rep, s.major);
            for (var r : s.reqs) stage = stage.requirement(r);
            for (var rl : s.signReqs) {
                var sign = Signs.find(rl);
                if (sign == null) throw new IllegalStateException("deity '" + id + "' stage '" + s.id + "': unknown sign '" + rl + "'");
                stage = stage.requirement(new Deity.SignRequirement(sign));
            }
            deity.getProgression().add(stage);
        }
        if (maxReputation != null) deity.getProgression().setMax(maxReputation);
        DeityHooks.register(deity.getId(), model, onUnlock, onLock, onChange);
        EidolonUnchained.LOGGER.debug("deity '{}': {} stage(s), model {}", id, stages.size(), model);
    }

    /** EU abstraction: a Deity whose reactions are script hooks; Eidolon's abstract lock/unlock callbacks route to them. */
    public static final class ScriptedDeity extends Deity {
        ScriptedDeity(ResourceLocation id, int r, int g, int b) {
            super(id, r, g, b);
        }

        @Override
        public void onReputationUnlock(Player player, ResourceLocation lock) {
            DeityHooks.unlocked(getId(), player, lock);
        }

        @Override
        public void onReputationLock(Player player, ResourceLocation lock) {
            DeityHooks.locked(getId(), player, lock);
        }
    }

    static Research researchOrNull(ResourceLocation rl) {
        return Researches.find(rl);
    }
}
