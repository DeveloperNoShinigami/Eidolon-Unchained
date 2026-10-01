package com.bluelotuscoding.eidolonunchained.kubejs;

import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import com.bluelotuscoding.eidolonunchained.api.CasterBuilder;
import com.bluelotuscoding.eidolonunchained.api.CasterHelper;
import com.bluelotuscoding.eidolonunchained.api.DeityBuilder;
import com.bluelotuscoding.eidolonunchained.api.EUApi;
import com.bluelotuscoding.eidolonunchained.api.EidolonApiValidation;
import com.bluelotuscoding.eidolonunchained.api.PlayerHelper;
import com.bluelotuscoding.eidolonunchained.api.ProjectileBuilder;
import com.bluelotuscoding.eidolonunchained.api.ResearchBuilder;
import com.bluelotuscoding.eidolonunchained.api.RitualBuilder;
import com.bluelotuscoding.eidolonunchained.api.RuneBuilder;
import com.bluelotuscoding.eidolonunchained.api.SignBuilder;
import com.bluelotuscoding.eidolonunchained.api.SoulHelper;
import com.bluelotuscoding.eidolonunchained.api.SpellBuilder;
import com.bluelotuscoding.eidolonunchained.api.codex.CodexApi;
import dev.latvian.mods.kubejs.typings.Info;
import elucent.eidolon.api.deity.Deity;
import elucent.eidolon.api.spells.Rune;
import elucent.eidolon.api.spells.Sign;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.ModList;

import java.util.List;

/**
 * The {@code EidolonUnchained} global available in every script type. The contract is the script API map
 * (ICM: knowledge/SCRIPT_API.md). Declaring builders are for startup scripts; lookups work anywhere after registration.
 */
public final class EUBinding {
    public static final EUBinding INSTANCE = new EUBinding();

    /** {@code EidolonUnchained.codex.chapter(...)} / {@code .category(...)}: script-authored codex content (D31). */
    public final CodexApi codex = CodexApi.INSTANCE;

    /** {@code EidolonUnchained.tasks.items(...)} / {@code .xp(...)} for research builders. */
    public final ResearchBuilder.Tasks tasks = ResearchBuilder.Tasks.INSTANCE;

    /** {@code EidolonUnchained.conditions}: the Phase 4 condition library (any script type). */
    public final com.bluelotuscoding.eidolonunchained.api.condition.Conditions conditions = com.bluelotuscoding.eidolonunchained.api.condition.Conditions.INSTANCE;

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

    @Info("Declare a chant: .cost(n).delay(t).canCast((level,pos,player)=>bool).cast((level,pos,player)=>…).deity(id).minReputation(n); its signs are an eidolon:chant recipe with the same id")
    public SpellBuilder chant(String id) {
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

    @Info("Declare a mob caster profile: .chants(...).deity(id).mana(max, regen).castInterval(t).signDelay(t).range(min,max).requireLineOfSight(b).cooldown(spell,t).targetPolicy(p)")
    public CasterBuilder caster(String id) {
        return EUApi.caster(id);
    }

    @Info("A mob's caster state: isCaster(), profile(), chants(), setProfile(id), clearProfile(), grantChant(id), refresh()")
    public CasterHelper caster(LivingEntity entity) {
        return EUApi.caster(entity);
    }

    public List<String> casterProfiles() {
        return EUApi.casterProfiles();
    }

    @Info("Inside a cast body: shoot a projectile from the caster. .entity(id) shoots any projectile entity type (EntityJS/GeckoLib ones included); otherwise EU's chant_projectile with .color/.size/.speed/.gravity/.lifetime/.homing/.onHit")
    public ProjectileBuilder projectile(LivingEntity caster) {
        return EUApi.projectile(caster);
    }

    // ---- helpers (any script type with a server-side entity) ----

    @Info("Knowledge, reputation and soul helper of a player; the same object as player.data.eidolon")
    public PlayerHelper player(Player player) {
        return EUApi.player(player);
    }

    @Info("Declare a divine damage (startup): .deity(god) required, .name(text); creates its damage type and <id>_damage / <id>_resistance attributes")
    public com.bluelotuscoding.eidolonunchained.api.DivineDamageBuilder divineDamage(String id) {
        return EUApi.divineDamage(id);
    }

    @Info("A damage source of any damage type id caused by an entity, for KubeJS's entity.hurt(source, amount); divine damages get EU's divine handling")
    public net.minecraft.world.damagesource.DamageSource damageSource(String typeId, net.minecraft.world.entity.Entity attacker) {
        return com.bluelotuscoding.eidolonunchained.damage.DivineDamages.source(com.bluelotuscoding.eidolonunchained.api.Ids.of(typeId, "damage type"), attacker.level(), attacker);
    }

    @Info("The same with no attacker (environmental)")
    public net.minecraft.world.damagesource.DamageSource damageSource(String typeId, net.minecraft.world.level.Level level) {
        return com.bluelotuscoding.eidolonunchained.damage.DivineDamages.source(com.bluelotuscoding.eidolonunchained.api.Ids.of(typeId, "damage type"), level, null);
    }

    @dev.latvian.mods.kubejs.typings.Info("The patron of a player or mob ('ns:id'), or null (D55)")
    public String patronOf(net.minecraft.world.entity.Entity entity) {
        var id = com.bluelotuscoding.eidolonunchained.patron.Patrons.patronOf(entity);
        return id == null ? null : id.toString();
    }

    @Info("Eidolon's soul of any living entity: mana, maxMana, takeMana, giveMana, setMaxMana, ethereal health")
    public SoulHelper soul(LivingEntity entity) {
        return EUApi.soul(entity);
    }

    @Info("Imbued chants on a weapon and Deity's Protection on a piece: chants(), active(), imbue(id), cast(player), protect(id), protectionChant()")
    public com.bluelotuscoding.eidolonunchained.api.ImbueHelper imbued(net.minecraft.world.item.ItemStack stack) {
        return EUApi.imbued(stack);
    }

    @Info("A hexblade (scripted 'eidolonunchained:hexblade' item or a Hexblades Renewed blade): isHexblade(), isAwakened(), elementalPower(), deity(), setAwakened(player, b)")
    public com.bluelotuscoding.eidolonunchained.api.HexbladeHelper hexblade(net.minecraft.world.item.ItemStack stack) {
        return EUApi.hexblade(stack);
    }

    @Info("A written chant scroll (Eidolon's item) for a chant that has a recipe; empty otherwise")
    public net.minecraft.world.item.ItemStack scroll(String chantId) {
        return EUApi.scroll(chantId);
    }

    public boolean hexbladesLoaded() {
        return com.bluelotuscoding.eidolonunchained.api.HexbladeHelper.hexbladesLoaded();
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

    @Info("Ids of every registered chant, Eidolon's and scripted")
    public List<String> chants() {
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
