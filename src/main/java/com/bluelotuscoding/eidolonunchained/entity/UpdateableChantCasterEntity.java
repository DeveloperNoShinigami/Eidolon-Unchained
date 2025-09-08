package com.bluelotuscoding.eidolonunchained.entity;

import elucent.eidolon.api.spells.Sign;
import elucent.eidolon.api.spells.SignSequence;
import elucent.eidolon.common.entity.ChantCasterEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Extended ChantCasterEntity that allows real-time sign sequence updates
 * without entity recreation - fixes the Active Chanting visual issues
 */
public class UpdateableChantCasterEntity extends ChantCasterEntity {
    
    public UpdateableChantCasterEntity(Level level, Player caster, List<Sign> signs, Vec3 look) {
        super(level, caster, signs, look);
    }
    
    /**
     * 🎯 NEW: Update the sign sequence in real-time without recreation
     */
    public void updateSignSequence(List<Sign> newSigns) {
        // Use Eidolon's existing method to update the signs
        setChantTag(newSigns);
        
        // Create new SignSequence with updated signs
        SignSequence sequence = new SignSequence();
        for (Sign sign : newSigns) {
            sequence.addRight(sign); // Use correct method name
        }
        
        // Update the entity data directly - this syncs to client
        getEntityData().set(SIGNS, sequence.serializeNbt());
    }
    
    /**
     * Check if this entity can be updated instead of recreated
     */
    public boolean canBeUpdated() {
        return !isRemoved() && isAlive();
    }
}
