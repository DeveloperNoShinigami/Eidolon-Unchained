package com.bluelotuscoding.eidolonunchained.capability;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Handles registration and attachment of patron capability to worlds
 */
@Mod.EventBusSubscriber(modid = "eidolonunchained")
public class CapabilityHandler {
    
    public static final Capability<IPatronData> PATRON_DATA_CAPABILITY = 
        CapabilityManager.get(new CapabilityToken<>() {});
    
    private static final ResourceLocation PATRON_DATA_ID = 
        new ResourceLocation("eidolonunchained", "patron_data");
    
    @SubscribeEvent
    public static void attachCapabilities(AttachCapabilitiesEvent<Level> event) {
        // Attach patron data capability to worlds
        if (!event.getObject().getCapability(PATRON_DATA_CAPABILITY).isPresent()) {
            event.addCapability(PATRON_DATA_ID, new PatronDataProvider());
        }
    }
    
    /**
     * Capability provider for patron data
     */
    public static class PatronDataProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {
        
        private final LazyOptional<IPatronData> instance = LazyOptional.of(PatronData::new);
        
        @Nonnull
        @Override
        public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
            return cap == PATRON_DATA_CAPABILITY ? instance.cast() : LazyOptional.empty();
        }
        
        @Override
        public CompoundTag serializeNBT() {
            return instance.map(data -> {
                if (data instanceof INBTSerializable<?>serializable) {
                    return (CompoundTag) serializable.serializeNBT();
                }
                return new CompoundTag();
            }).orElse(new CompoundTag());
        }
        
        @Override
        public void deserializeNBT(CompoundTag nbt) {
            instance.ifPresent(data -> {
                if (data instanceof INBTSerializable<?> serializable) {
                    @SuppressWarnings("unchecked")
                    INBTSerializable<CompoundTag> typedSerializable = (INBTSerializable<CompoundTag>) serializable;
                    typedSerializable.deserializeNBT(nbt);
                }
            });
        }
    }
}
