package com.bluelotuscoding.eidolonunchained.patron;

import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import com.bluelotuscoding.eidolonunchained.api.ScriptedSpell;
import elucent.eidolon.api.deity.Deity;
import elucent.eidolon.api.spells.Spell;
import elucent.eidolon.api.spells.SpellCastEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Only followers use a major god's power (D47.7): Eidolon's own deity-bound spells (its prayers, sacrifices and light
 * chants all keep the god in a {@code Deity deity} field) are cancelled on {@code SpellCastEvent.Pre} for a player who
 * does not follow that god. Scripted chants check the same rule in their own {@code canCast}.
 */
@Mod.EventBusSubscriber(modid = EidolonUnchained.MOD_ID)
public final class FollowerGate {
    private static final Map<Class<?>, Field> DEITY_FIELDS = new ConcurrentHashMap<>();
    private static final Field NONE;

    static {
        try {
            NONE = FollowerGate.class.getDeclaredField("NONE");
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException(e);
        }
    }

    private FollowerGate() {
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onCast(SpellCastEvent.Pre event) {
        if (event.player == null || event.world.isClientSide() || event.spell instanceof ScriptedSpell) return;
        var deity = deityOf(event.spell);
        if (deity == null || Patrons.isFollower(event.player, deity)) return;
        event.setCanceled(true);
        event.player.displayClientMessage(Component.translatable("eidolonunchained.spell.not_follower", Patrons.deityName(deity)), true);
    }

    /** The god an Eidolon spell is bound to, or null. */
    public static @Nullable ResourceLocation deityOf(Spell spell) {
        var field = DEITY_FIELDS.computeIfAbsent(spell.getClass(), FollowerGate::findDeityField);
        if (field == NONE) return null;
        try {
            return field.get(spell) instanceof Deity d ? d.getId() : null;
        } catch (IllegalAccessException e) {
            return null;
        }
    }

    private static Field findDeityField(Class<?> type) {
        for (var c = type; c != null && c != Object.class; c = c.getSuperclass()) {
            for (var f : c.getDeclaredFields()) {
                if (Deity.class.isAssignableFrom(f.getType()) && !java.lang.reflect.Modifier.isStatic(f.getModifiers())) {
                    f.setAccessible(true);
                    return f;
                }
            }
        }
        return NONE;
    }
}
