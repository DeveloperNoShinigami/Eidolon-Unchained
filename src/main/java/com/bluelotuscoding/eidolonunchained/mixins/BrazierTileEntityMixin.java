package com.bluelotuscoding.eidolonunchained.mixins;

import com.bluelotuscoding.eidolonunchained.events.RitualEventHandler;
import com.mojang.logging.LogUtils;
import elucent.eidolon.common.tile.BrazierTileEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = BrazierTileEntity.class, remap = false)
public class BrazierTileEntityMixin {
    private static final Logger LOGGER = LogUtils.getLogger();

    @Inject(method = "complete", at = @At("HEAD"))
    private void eidolonunchained$fireCompletionEvent(CallbackInfo callbackInfo) {
        BrazierTileEntity brazier = (BrazierTileEntity) (Object) this;

        // Read the ritual ID from the brazier's saved NBT — avoids @Shadow on package-private field.
        // BrazierTileEntity.saveAdditional() writes ritual as tag "ritual" when non-null.
        CompoundTag tag = new CompoundTag();
        brazier.saveAdditional(tag);

        if (!tag.contains("ritual")) {
            LOGGER.warn("[EidolonUnchained] Brazier complete: no ritual in NBT — skipping event");
            return;
        }

        ResourceLocation ritualId = ResourceLocation.tryParse(tag.getString("ritual"));
        if (ritualId == null) {
            LOGGER.warn("[EidolonUnchained] Brazier complete: could not parse ritual ID from NBT");
            return;
        }

        if (brazier.getLevel() instanceof ServerLevel serverLevel) {
            LOGGER.info("[EidolonUnchained] Brazier complete: firing RitualCompleteEvent for {}", ritualId);
            RitualEventHandler.fireRitualCompletion(serverLevel, brazier.getBlockPos(), ritualId);
        }
    }
}