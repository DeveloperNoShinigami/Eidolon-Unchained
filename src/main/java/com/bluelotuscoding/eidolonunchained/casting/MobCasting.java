package com.bluelotuscoding.eidolonunchained.casting;

import com.bluelotuscoding.eidolonunchained.EUConfig;
import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import elucent.eidolon.capability.ISoul;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Wires mob casting into the world: Eidolon's {@code ISoul} on every mob (rule C5), profile resolution on join and
 * equipment change (rule C4), the {@link MobChantGoal} while a profile resolves, mana regeneration, and the big-hit
 * interrupt (D34).
 */
@Mod.EventBusSubscriber(modid = EidolonUnchained.MOD_ID)
public final class MobCasting {
    private static final Map<Mob, MobChantGoal> GOALS = new WeakHashMap<>();

    private MobCasting() {
    }

    @SubscribeEvent
    public static void attachSoul(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Mob && !(event.getObject() instanceof Player)) {
            event.addCapability(new ResourceLocation(EidolonUnchained.MOD_ID, "soul"), new ISoul.Provider());
        }
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof Mob mob) refresh(mob);
    }

    @SubscribeEvent
    public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        if (event.getEntity() instanceof Mob mob && !mob.level().isClientSide()) refresh(mob);
    }

    @SubscribeEvent
    public static void onTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Mob mob) || mob.level().isClientSide() || mob.tickCount % 20 != 0) return;
        var p = CasterProfileResolver.cached(mob);
        float regen = p != null ? p.regenPerSecond : EUConfig.MOB_DEFAULT_MANA_REGEN.get().floatValue();
        if (regen <= 0) return;
        mob.getCapability(ISoul.INSTANCE).ifPresent(soul -> {
            if (soul.getMagic() < soul.getMaxMagic()) soul.setMagic(Math.min(soul.getMaxMagic(), soul.getMagic() + regen));
        });
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Mob mob)) return;
        var goal = GOALS.get(mob);
        if (goal == null) return;
        if (event.getAmount() >= mob.getMaxHealth() * EUConfig.MOB_CHANT_INTERRUPT_FRACTION.get()) goal.interrupt();
    }

    /** Re-resolves a mob's profile and adds/removes the chant goal and soul sizing accordingly. */
    public static CasterProfile refresh(Mob mob) {
        var before = CasterProfileResolver.cached(mob);
        var p = CasterProfileResolver.resolve(mob);
        if (p != null) {
            mob.getCapability(ISoul.INSTANCE).ifPresent(soul -> {
                if (soul.getMaxMagic() != p.maxMana) { soul.setMaxMagic(p.maxMana); if (before == null) soul.setMagic(p.maxMana); }
            });
            if (!GOALS.containsKey(mob)) {
                var goal = new MobChantGoal(mob);
                mob.goalSelector.addGoal(2, goal);
                GOALS.put(mob, goal);
                EidolonUnchained.LOGGER.debug("{} became a caster ({} spell(s))", mob.getName().getString(), p.spells.size());
            }
        } else {
            var goal = GOALS.remove(mob);
            if (goal != null) mob.goalSelector.removeGoal(goal);
            // rule C5: every mob has a mana pool (config mobDefaultMaxMana) so held weapons and armour can charge it
            float base = EUConfig.MOB_DEFAULT_MAX_MANA.get().floatValue();
            mob.getCapability(ISoul.INSTANCE).ifPresent(soul -> {
                if (soul.getMaxMagic() != base) { soul.setMaxMagic(base); soul.setMagic(Math.min(soul.getMagic() > 0 && before == null ? soul.getMagic() : base, base)); }
            });
        }
        return p;
    }
}
