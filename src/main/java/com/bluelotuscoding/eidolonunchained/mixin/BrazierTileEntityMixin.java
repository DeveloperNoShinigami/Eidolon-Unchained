package com.bluelotuscoding.eidolonunchained.mixin;

import com.bluelotuscoding.eidolonunchained.api.RitualBuilder;
import com.bluelotuscoding.eidolonunchained.world.WorldEvents;
import elucent.eidolon.api.ritual.Ritual;
import elucent.eidolon.common.tile.BrazierTileEntity;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Phase 4 decision 3: Eidolon posts nothing when a brazier ritual ends, so this reads the ritual as {@code complete()}
 * begins (before Eidolon clears it) and hands it to {@link WorldEvents#ritualCompleted}. Eidolon's own names, so no
 * remapping. A scripted ritual that refused to start ({@code .requires} failed) is not reported as completed.
 */
@Mixin(value = BrazierTileEntity.class, remap = false)
public abstract class BrazierTileEntityMixin {
    @Shadow
    Ritual ritual;

    @Inject(method = "complete", at = @At("HEAD"))
    private void eidolonunchained$onComplete(CallbackInfo ci) {
        var self = (BrazierTileEntity) (Object) this;
        if (ritual == null || !(self.getLevel() instanceof ServerLevel level)) return;
        if (RitualBuilder.ScriptedRitual.consumeRefusal(level, self.getBlockPos())) return;
        var id = ritual.getRegistryName();
        if (id != null) WorldEvents.ritualCompleted(level, self.getBlockPos(), id);
    }
}
