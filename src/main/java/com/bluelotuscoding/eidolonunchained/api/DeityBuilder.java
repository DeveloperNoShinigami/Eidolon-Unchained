package com.bluelotuscoding.eidolonunchained.api;

import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import com.bluelotuscoding.eidolonunchained.patron.Patrons;
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
    final List<String> followers = new ArrayList<>();
    com.bluelotuscoding.eidolonunchained.patron.Callings.Settings calling;
    com.bluelotuscoding.eidolonunchained.damage.DivineDamages.Curve devotion;
    Boolean patronRequired;
    Boolean requiresCalling;
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
        Integer maxMana;

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

    @dev.latvian.mods.kubejs.typings.Info("The same with a plain function: ctx => boolean (ctx.player)")
    public DeityBuilder require(com.bluelotuscoding.eidolonunchained.api.condition.Conditions.ContextTest fn) {
        return require(com.bluelotuscoding.eidolonunchained.api.condition.Condition.of(fn));
    }

    @Info("Max mana a pledged follower has while holding the last added .stage(...) (a floor under Eidolon's prayer value)")
    public DeityBuilder maxMana(int mana) {
        if (mana < 0) throw new IllegalArgumentException("Eidolon Unchained: deity '" + id + "': maxMana must be 0 or more, got " + mana);
        lastStage().maxMana = mana;
        return this;
    }

    @Info("How this god calls a player (offersPatronage in a discovery): { greeting, question, accept, decline, silence, bound: lang keys or text (%s = the player; bound also gets the other god), yes: ['yes', ...], no: ['no', ...], wait: seconds (60), callAgainAfter: seconds or 'never' (default), voice: a sound id played as the words type out (default 'eidolon:chant_word') }")
    public DeityBuilder calling(java.util.Map<String, Object> options) {
        var def = com.bluelotuscoding.eidolonunchained.patron.Callings.Settings.DEFAULT;
        String greeting = def.greeting(), question = def.question(), accept = def.accept(), decline = def.decline(),
                silence = def.silence(), bound = def.bound();
        List<String> yes = def.yes(), no = def.no();
        int wait = def.waitTicks();
        long again = def.callAgainTicks();
        var voice = def.voice();
        for (var e : options.entrySet()) {
            var v = e.getValue();
            switch (e.getKey()) {
                case "greeting" -> greeting = String.valueOf(v);
                case "question" -> question = String.valueOf(v);
                case "accept" -> accept = String.valueOf(v);
                case "decline" -> decline = String.valueOf(v);
                case "silence" -> silence = String.valueOf(v);
                case "bound" -> bound = String.valueOf(v);
                case "yes" -> yes = words(v, "yes");
                case "no" -> no = words(v, "no");
                case "wait" -> wait = (int) Math.round(seconds(v, "wait") * 20);
                case "voice" -> voice = Ids.of(String.valueOf(v), "sound");
                case "callAgainAfter" -> again = "never".equals(String.valueOf(v)) ? -1 : Math.round(seconds(v, "callAgainAfter") * 20);
                default -> throw new IllegalArgumentException("Eidolon Unchained: deity '" + id + "': unknown calling option '" + e.getKey()
                        + "' (greeting, question, accept, decline, silence, bound, yes, no, wait, callAgainAfter, voice)");
            }
        }
        this.calling = new com.bluelotuscoding.eidolonunchained.patron.Callings.Settings(greeting, question, accept, decline, silence, bound, yes, no, wait, again, voice);
        return this;
    }

    private List<String> words(Object v, String key) {
        var out = new ArrayList<String>();
        if (v instanceof Iterable<?> it) for (var o : it) out.add(String.valueOf(o));
        else out.add(String.valueOf(v));
        if (out.isEmpty()) throw new IllegalArgumentException("Eidolon Unchained: deity '" + id + "': calling '" + key + "' needs at least one phrase");
        return List.copyOf(out);
    }

    private double seconds(Object v, String key) {
        if (v instanceof Number n && n.doubleValue() > 0) return n.doubleValue();
        throw new IllegalArgumentException("Eidolon Unchained: deity '" + id + "': calling '" + key + "' must be a number of seconds > 0" + ("callAgainAfter".equals(key) ? " or 'never'" : ""));
    }

    @Info("Divine damage scaling by the attacker's reputation with this god: points [[rep, multiplier], ...], linear between them and flat beyond the ends")
    public DeityBuilder devotion(java.util.List<?> points) {
        if (points.isEmpty()) throw new IllegalArgumentException("Eidolon Unchained: deity '" + id + "': devotion needs at least one [rep, multiplier] point");
        var out = new double[points.size()][];
        for (int i = 0; i < out.length; i++) {
            if (!(points.get(i) instanceof java.util.List<?> p) || p.size() != 2 || !(p.get(0) instanceof Number r) || !(p.get(1) instanceof Number m))
                throw new IllegalArgumentException("Eidolon Unchained: deity '" + id + "': devotion point " + i + " must be [reputation, multiplier]");
            out[i] = new double[]{r.doubleValue(), m.doubleValue()};
        }
        this.devotion = com.bluelotuscoding.eidolonunchained.damage.DivineDamages.points(out);
        return this;
    }

    @Info("The same with a function: rep => multiplier (if it throws, the multiplier is 1 and the error is logged once)")
    public DeityBuilder devotion(com.bluelotuscoding.eidolonunchained.damage.DivineDamages.Curve fn) {
        this.devotion = fn;
        return this;
    }

    @Info("Mob types that follow this god: entity ids or '#tags', e.g. '#minecraft:skeletons', 'minecraft:wither' (D55)")
    public DeityBuilder followers(String... entityTypesOrTags) {
        for (var spec : entityTypesOrTags) {
            var t = spec.trim();
            Ids.of(t.startsWith("#") ? t.substring(1) : t, "entity type");      // validates the id
            followers.add(t);
        }
        return this;
    }

    @Info("Whether the deity gives reputation only after a pledge (default true: a major god, one at a time; false: a minor spirit anyone may follow)")
    public DeityBuilder patronRequired(boolean required) {
        this.patronRequired = required;
        return this;
    }

    @Info("Whether the deity's pledge ritual is open only to players it has called (default false)")
    public DeityBuilder requiresCalling(boolean calling) {
        this.requiresCalling = calling;
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
        var stageMana = new java.util.HashMap<ResourceLocation, Integer>();
        for (var s : stages) if (s.maxMana != null) stageMana.put(s.id, s.maxMana);
        // D47: required unless the script says otherwise; an extension of Eidolon's own deities leaves it to the config.
        Boolean required = patronRequired != null ? patronRequired : (extend ? null : Boolean.TRUE);
        Patrons.declare(deity.getId(), required, requiresCalling, stageMana);
        if (!followers.isEmpty()) Patrons.declareFollowers(deity.getId(), followers);
        if (calling != null) com.bluelotuscoding.eidolonunchained.patron.Callings.declare(deity.getId(), calling);
        if (devotion != null) com.bluelotuscoding.eidolonunchained.damage.DivineDamages.setCurve(deity.getId(), devotion);
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
