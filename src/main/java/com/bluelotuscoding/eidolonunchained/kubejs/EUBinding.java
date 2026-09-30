package com.bluelotuscoding.eidolonunchained.kubejs;

import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import com.bluelotuscoding.eidolonunchained.api.DeityBuilder;
import com.bluelotuscoding.eidolonunchained.api.EUApi;
import com.bluelotuscoding.eidolonunchained.api.EidolonApiValidation;
import com.bluelotuscoding.eidolonunchained.api.ResearchBuilder;
import com.bluelotuscoding.eidolonunchained.api.RitualBuilder;
import com.bluelotuscoding.eidolonunchained.api.RuneBuilder;
import com.bluelotuscoding.eidolonunchained.api.SignBuilder;
import com.bluelotuscoding.eidolonunchained.api.SoulHelper;
import com.bluelotuscoding.eidolonunchained.api.SpellBuilder;
import dev.latvian.mods.kubejs.typings.Info;
import elucent.eidolon.api.deity.Deity;
import elucent.eidolon.api.spells.Rune;
import elucent.eidolon.api.spells.Sign;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.fml.ModList;

import java.util.List;

/**
 * The {@code EidolonUnchained} global available in every script type. The contract is the script API map
 * (ICM: knowledge/SCRIPT_API.md). Declaring builders are for startup scripts; lookups work anywhere after registration.
 */
public final class EUBinding {
    public static final EUBinding INSTANCE = new EUBinding();

    /** {@code EidolonUnchained.tasks.items(...)} / {@code .xp(...)} for research builders. */
    public final ResearchBuilder.Tasks tasks = ResearchBuilder.Tasks.INSTANCE;

    private EUBinding() {
    }

    // ---- Phase 2 declarations (startup scripts) ----

    @Info("Declare a sign: .sprite('ns:particle/x_sign').color(r, g, b)")
    public SignBuilder sign(String id) {
        return EUApi.sign(id);
    }

    @Info("Declare a rune: .sprite('ns:rune/x').effect(seq => 'pass' | 'fail')")
    public RuneBuilder rune(String id) {
        return EUApi.rune(id);
    }

    @Info("Declare research: .stars(n).foundOn('block or entity id').task(step, EidolonUnchained.tasks.items(...))")
    public ResearchBuilder research(String id) {
        return EUApi.research(id);
    }

    @Info("Declare a custom deity: .color(r,g,b).model(id).stage(id, rep, major).requireResearch(id)…")
    public DeityBuilder deity(String id) {
        return EUApi.deity(id);
    }

    @Info("Extend an existing deity (e.g. 'eidolon:light'): stages, requirements, model, reactions")
    public DeityBuilder extendDeity(String id) {
        return EUApi.extendDeity(id);
    }

    @Info("Declare a scripted spell: .cost(n).delay(t).canCast((level,pos,player)=>bool).cast((level,pos,player)=>…).deity(id).minReputation(n); its signs are an eidolon:chant recipe with the same id")
    public SpellBuilder spell(String id) {
        return EUApi.spell(id);
    }

    @Info("Declare an effigy prayer (Eidolon's PrayerSpell): .deity(id).cost(n).reputation(n).power(x).signs(...)")
    public SpellBuilder.Prayer prayer(String id) {
        return EUApi.prayer(id);
    }

    @Info("Declare a ritual: .symbol(rl).color(rgb).require(item, n).requireHealth(n).requireFocus(item).invariant(item).onComplete((level,pos)=>…); a ritual_brazier recipe names it")
    public RitualBuilder ritual(String id) {
        return EUApi.ritual(id);
    }

    // ---- helpers (any script type with a server-side entity) ----

    @Info("Eidolon's soul of any living entity: mana, maxMana, takeMana, giveMana, setMaxMana, ethereal health")
    public SoulHelper soul(LivingEntity entity) {
        return EUApi.soul(entity);
    }

    // ---- lookups ----

    public List<String> signs() {
        return EUApi.signs();
    }

    public Sign findSign(String id) {
        return EUApi.findSign(id);
    }

    public List<String> runes() {
        return EUApi.runes();
    }

    public Rune findRune(String id) {
        return EUApi.findRune(id);
    }

    public List<String> researches() {
        return EUApi.researches();
    }

    public List<String> spells() {
        return EUApi.spells();
    }

    public List<String> rituals() {
        return EUApi.rituals();
    }

    public List<String> deities() {
        return EUApi.deities();
    }

    public Deity findDeity(String id) {
        return EUApi.findDeity(id);
    }

    @Info("Stage ids of a deity's progression, lowest reputation first")
    public List<String> stages(String deityId) {
        return EUApi.stages(deityId);
    }

    @Info("The avatar model id a script gave a deity, or null")
    public String deityModel(String deityId) {
        return EUApi.deityModel(deityId);
    }

    // ---- Phase 1: diagnostics ----

    public String version() {
        return EidolonUnchained.version();
    }

    public String eidolonVersion() {
        return EidolonUnchained.modVersion("eidolon");
    }

    public boolean isLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    public String apiReport() {
        return EidolonApiValidation.report();
    }

    public void log(String message) {
        EidolonUnchained.LOGGER.info("[script] {}", message);
    }
}
