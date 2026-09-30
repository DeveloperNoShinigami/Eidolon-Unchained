package com.bluelotuscoding.eidolonunchained.casting;

import com.bluelotuscoding.eidolonunchained.EUConfig;
import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import elucent.eidolon.api.spells.SpellCastEvent;
import elucent.eidolon.common.spell.ExecCommandSpell;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * D34: command chants ({@code eidolon:command_chant} recipes, Eidolon's {@link ExecCommandSpell}) need the chanter to have
 * permission level {@code commandChantPermissionLevel} (config, default 0 = anyone). Eidolon itself always runs them at
 * level 2 and never checks who chanted. The gate sits on Eidolon's {@code SpellCastEvent.Pre}, which every path goes
 * through: active chanting, the codex, chant scrolls, imbued weapons. Machine casts (a fake player) are not gated.
 * Note: Eidolon only runs the commands when the server allows command blocks ({@code enable-command-block=true} on a
 * dedicated server; always on in single player).
 */
@Mod.EventBusSubscriber(modid = EidolonUnchained.MOD_ID)
public final class CommandChantPermission {
    private CommandChantPermission() {
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onCast(SpellCastEvent.Pre event) {
        if (!(event.spell instanceof ExecCommandSpell) || !(event.player instanceof ServerPlayer sp) || sp instanceof FakePlayer) return;
        int level = EUConfig.COMMAND_CHANT_PERMISSION_LEVEL.get();
        if (level <= 0 || sp.hasPermissions(level)) return;
        event.setCanceled(true);
        sp.displayClientMessage(Component.translatable("eidolonunchained.chant.command_not_permitted"), true);
        EidolonUnchained.LOGGER.debug("{} tried command chant {} without permission level {}", sp.getGameProfile().getName(), event.spell.getRegistryName(), level);
    }
}
