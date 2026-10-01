package com.bluelotuscoding.eidolonunchained.damage;

import com.bluelotuscoding.eidolonunchained.EUConfig;
import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import com.bluelotuscoding.eidolonunchained.patron.Patrons;
import elucent.eidolon.capability.IReputation;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Divine damage (D48, D49, D52, D53). A startup script declares a divine damage by name with its owning god; EU then
 * provides, for each one: a damage type (generated data, so it exists when the world loads), and two real attributes
 * on every living entity, {@code <id>_damage} and {@code <id>_resistance}. Two global attributes apply to all divine
 * damage: {@code eidolonunchained:divine_resistance} and {@code eidolonunchained:divine_penetration}.
 * <p>
 * An attribute is read in two parts: its base plus ADDITION modifiers are the <b>flat</b> part, its MULTIPLY_BASE and
 * MULTIPLY_TOTAL modifiers the <b>percent</b> part (so tooltips read "+4 Necrotic Resistance", "+25% Necrotic
 * Resistance"). Every hit of a divine damage, however it is dealt ({@code entity.hurt(...)} from a script, a chant,
 * another mod), goes through {@link #onHurt}: follower check, devotion, the attacker's percent bonus, then the target's
 * flat and percent resistance less the attacker's penetration (negative = weakness; percent at 100 % or more blocks).
 */
@Mod.EventBusSubscriber(modid = EidolonUnchained.MOD_ID)
public final class DivineDamages {
    /** One declared divine damage and its two attributes. */
    public static final class Declared {
        public final ResourceLocation id;
        public ResourceLocation owner;
        public String name;
        Attribute damage, resistance;

        Declared(ResourceLocation id) {
            this.id = id;
        }

        public ResourceKey<net.minecraft.world.damagesource.DamageType> key() {
            return ResourceKey.create(Registries.DAMAGE_TYPE, id);
        }

        /** The generated {@code <id>_damage} attribute (null before attributes register). */
        public Attribute damageAttribute() {
            return damage;
        }

        /** "Necrotic" for eu_examples:necrotic unless the script named it. */
        public String displayName() {
            if (name != null) return name;
            var words = id.getPath().replace('_', ' ').split(" ");
            var sb = new StringBuilder();
            for (var w : words) if (!w.isEmpty()) sb.append(sb.length() == 0 ? "" : " ").append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
            return sb.toString();
        }
    }

    /** A god's devotion curve: reputation -> damage multiplier. */
    public interface Curve {
        double at(double reputation);
    }

    private static final Map<ResourceLocation, Declared> DECLARED = new LinkedHashMap<>();
    private static final Map<ResourceLocation, Curve> CURVES = new LinkedHashMap<>();
    private static final java.util.Set<ResourceLocation> CURVE_WARNED = new java.util.HashSet<>();
    private static Attribute globalResistance, globalPenetration;
    private static boolean attributesRegistered;

    public static boolean attributesRegistered() {
        return attributesRegistered;
    }

    private DivineDamages() {
    }

    // ---- declarations (startup scripts) ----

    public static Declared declare(ResourceLocation id) {
        return DECLARED.computeIfAbsent(id, Declared::new);
    }

    public static List<Declared> all() {
        return new ArrayList<>(DECLARED.values());
    }

    public static @Nullable Declared get(@Nullable ResourceLocation id) {
        return DECLARED.get(id);
    }

    public static void setCurve(ResourceLocation deity, Curve curve) {
        CURVES.put(deity, curve);
    }

    /** Points [[rep, multiplier], ...], linear between them and flat beyond the ends. */
    public static Curve points(double[][] points) {
        var sorted = points.clone();
        java.util.Arrays.sort(sorted, java.util.Comparator.comparingDouble(p -> p[0]));
        return rep -> {
            if (rep <= sorted[0][0]) return sorted[0][1];
            for (int i = 1; i < sorted.length; i++) {
                if (rep <= sorted[i][0]) {
                    double t = (rep - sorted[i - 1][0]) / (sorted[i][0] - sorted[i - 1][0]);
                    return sorted[i - 1][1] + t * (sorted[i][1] - sorted[i - 1][1]);
                }
            }
            return sorted[sorted.length - 1][1];
        };
    }

    // ---- registration (mod bus) ----

    @Mod.EventBusSubscriber(modid = EidolonUnchained.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        private Registration() {
        }

        /** Attributes are registered at mod load; startup scripts (which declare the damages) have run by now. */
        @SubscribeEvent
        public static void onRegister(RegisterEvent event) {
            event.register(ForgeRegistries.Keys.ATTRIBUTES, helper -> {
                attributesRegistered = true;
                globalResistance = attribute("divine_resistance");
                globalPenetration = attribute("divine_penetration");
                helper.register(new ResourceLocation(EidolonUnchained.MOD_ID, "divine_resistance"), globalResistance);
                helper.register(new ResourceLocation(EidolonUnchained.MOD_ID, "divine_penetration"), globalPenetration);
                for (var d : DECLARED.values()) {
                    d.damage = new RangedAttribute("attribute.name." + d.id.getNamespace() + "." + d.id.getPath() + "_damage", 0, -1024, 1024).setSyncable(true);
                    d.resistance = new RangedAttribute("attribute.name." + d.id.getNamespace() + "." + d.id.getPath() + "_resistance", 0, -1024, 1024).setSyncable(true);
                    helper.register(new ResourceLocation(d.id.getNamespace(), d.id.getPath() + "_damage"), d.damage);
                    helper.register(new ResourceLocation(d.id.getNamespace(), d.id.getPath() + "_resistance"), d.resistance);
                }
                if (!DECLARED.isEmpty()) EidolonUnchained.LOGGER.info("Divine damage: {} type(s), {} attribute(s)", DECLARED.size(), DECLARED.size() * 2 + 2);
            });
        }

        private static Attribute attribute(String path) {
            return new RangedAttribute("attribute.name." + EidolonUnchained.MOD_ID + "." + path, 0, -1024, 1024).setSyncable(true);
        }

        @SubscribeEvent
        public static void onAttributes(EntityAttributeModificationEvent event) {
            for (var type : event.getTypes()) {
                add(event, type, globalResistance);
                add(event, type, globalPenetration);
                for (var d : DECLARED.values()) {
                    add(event, type, d.damage);
                    add(event, type, d.resistance);
                }
            }
        }

        private static void add(EntityAttributeModificationEvent event, net.minecraft.world.entity.EntityType<? extends LivingEntity> type, @Nullable Attribute attribute) {
            if (attribute != null && !event.has(type, attribute)) event.add(type, attribute);
        }
    }

    /** Checked at common setup with the other declarations: every divine damage needs a registered god. */
    public static void validate() {
        for (var d : DECLARED.values()) {
            if (d.owner == null) EidolonUnchained.LOGGER.error("divine damage '{}' has no .deity(...): a divine damage always belongs to a god", d.id);
            else if (elucent.eidolon.common.deity.Deities.find(d.owner) == null) EidolonUnchained.LOGGER.error("divine damage '{}': unknown deity '{}'", d.id, d.owner);
        }
    }

    // ---- sources ----

    /** A damage source of any damage type id, caused by {@code attacker} (KubeJS 2001 has no way to make one by id). */
    public static DamageSource source(ResourceLocation type, Level level, @Nullable Entity attacker) {
        var holder = level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolder(ResourceKey.create(Registries.DAMAGE_TYPE, type))
                .orElseThrow(() -> new IllegalArgumentException("Eidolon Unchained: unknown damage type '" + type + "'"));
        return new DamageSource(holder, attacker);
    }

    // ---- the hit ----

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onHurt(LivingHurtEvent event) {
        var source = event.getSource();
        var target = event.getEntity();
        if (target.level().isClientSide()) return;
        var typeId = source.typeHolder().unwrapKey().map(ResourceKey::location).orElse(null);
        var d = typeId == null ? null : DECLARED.get(typeId);
        var attacker = source.getEntity() instanceof LivingEntity l ? l : null;
        if (d == null) {
            if (attacker != null && source.getDirectEntity() == attacker && (source.is(DamageTypes.PLAYER_ATTACK) || source.is(DamageTypes.MOB_ATTACK))) {
                var held = attacker.getMainHandItem();
                var whole = held.getItem() instanceof com.bluelotuscoding.eidolonunchained.hexblade.HexbladeItem hb ? DECLARED.get(hb.awakenedDivineDamage(held)) : null;
                if (whole != null && whole.owner != null && Patrons.isFollower(attacker, whole.owner)) {   // awakened hexblade: the whole hit is divine
                    QUEUE.add(new Hit(target, attacker, whole, event.getAmount()));   // the elemental power is already in the attack damage
                    event.setCanceled(true);
                }
                queueMeleeBonus(target, attacker);
            }
            return;
        }
        if (d.owner == null || (attacker != null && !Patrons.isFollower(attacker, d.owner))) return;   // not the god's power: plain magic
        double amount = event.getAmount();
        if (attacker instanceof ServerPlayer sp) amount *= devotion(d.owner, sp);
        if (attacker != null) amount *= 1 + parts(attacker, d.damage)[1];
        var res = parts(target, d.resistance);
        var gres = parts(target, globalResistance);
        var pen = attacker == null ? new double[2] : parts(attacker, globalPenetration);
        double flat = res[0] + gres[0] - pen[0];
        double pct = Math.max(res[1] + gres[1] - pen[1], EUConfig.DIVINE_WEAKNESS_FLOOR.get());
        double raw = event.getAmount(), dev = attacker instanceof ServerPlayer sp2 ? devotion(d.owner, sp2) : 1, bonus = attacker == null ? 0 : parts(attacker, d.damage)[1];
        amount -= flat;                                        // negative flat resistance adds damage
        amount = pct >= 1 ? 0 : amount * (1 - pct);           // negative percent amplifies, down to the floor
        if (amount <= 0) event.setCanceled(true);
        else event.setAmount((float) amount);
        if (!DEBUG.isEmpty()) report(attacker, target, String.format("%s %.1f -> x%.2f devotion -> x%.2f bonus -> -%.1f flat -> -%.0f%% -> %.1f (%s %.1f/%.1f hp)",
                d.displayName().toLowerCase(), raw, dev, 1 + bonus, flat, pct * 100, Math.max(0, amount), target.getName().getString(), target.getHealth(), target.getMaxHealth()));
    }

    // ---- /eu damage debug ----

    private static final java.util.Set<java.util.UUID> DEBUG = new java.util.HashSet<>();

    /** Toggles the divine damage readout for a player; true when it is now on. */
    public static boolean toggleDebug(ServerPlayer player) {
        if (DEBUG.remove(player.getUUID())) return false;
        DEBUG.add(player.getUUID());
        return true;
    }

    private static void report(@Nullable LivingEntity attacker, LivingEntity target, String line) {
        for (var e : new LivingEntity[]{attacker, target})
            if (e instanceof ServerPlayer sp && DEBUG.contains(sp.getUUID())) sp.sendSystemMessage(net.minecraft.network.chat.Component.literal(line).withStyle(net.minecraft.ChatFormatting.GRAY));
    }

    /** {flat, percent} of an attribute: base + ADDITION modifiers, and the sum of the multiply modifiers. */
    static double[] parts(LivingEntity entity, @Nullable Attribute attribute) {
        var out = new double[2];
        if (attribute == null) return out;
        var inst = entity.getAttribute(attribute);
        if (inst == null) return out;
        out[0] = inst.getBaseValue();
        for (var m : inst.getModifiers()) {
            if (m.getOperation() == AttributeModifier.Operation.ADDITION) out[0] += m.getAmount();
            else out[1] += m.getAmount();
        }
        return out;
    }

    private static double devotion(ResourceLocation deity, ServerPlayer player) {
        var curve = CURVES.get(deity);
        if (curve == null) return 1;
        double rep = player.server.overworld().getCapability(IReputation.INSTANCE).resolve().map(r -> r.getReputation(player, deity)).orElse(0.0);
        try {
            return Math.max(0, curve.at(rep));
        } catch (RuntimeException e) {
            if (CURVE_WARNED.add(deity)) EidolonUnchained.LOGGER.error("deity '{}' devotion curve threw ({}); using 1", deity, e.toString());
            return 1;
        }
    }

    // ---- extra divine hits (gear's flat _damage, scripted hexblades) ----

    private record Hit(LivingEntity target, LivingEntity attacker, Declared damage, float amount) {
    }

    private static final List<Hit> QUEUE = new ArrayList<>();

    /** A melee hit landed: each divine damage the attacker carries a flat bonus of adds its own divine hit. */
    private static void queueMeleeBonus(LivingEntity target, LivingEntity attacker) {
        for (var d : DECLARED.values()) {
            double flat = parts(attacker, d.damage)[0];
            if (flat > 0 && d.owner != null && Patrons.isFollower(attacker, d.owner)) QUEUE.add(new Hit(target, attacker, d, (float) flat));
        }
    }

    /** Deals {@code amount} of a divine damage after this tick's hit (its own hurt, past the target's hurt cooldown). */
    public static void queueHit(LivingEntity target, LivingEntity attacker, ResourceLocation damage, float amount) {
        var d = DECLARED.get(damage);
        if (d != null && amount > 0) QUEUE.add(new Hit(target, attacker, d, amount));
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || QUEUE.isEmpty()) return;
        var hits = new ArrayList<>(QUEUE);
        QUEUE.clear();
        for (var h : hits) {
            if (!h.target.isAlive()) continue;
            h.target.invulnerableTime = 0;
            h.target.hurt(source(h.damage.id, h.target.level(), h.attacker), h.amount);
        }
    }
}
