package com.bluelotuscoding.eidolonunchained.entity;

import elucent.eidolon.api.spells.Sign;
import elucent.eidolon.api.spells.SignSequence;
import elucent.eidolon.common.entity.ChantCasterEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

/**
 * Extended ChantCasterEntity that supports real-time sign sequence updates
 * without requiring entity recreation.
 */
public class UpdateableChantCasterEntity extends ChantCasterEntity {
    private static final Logger LOGGER = LogManager.getLogger();
    
    public UpdateableChantCasterEntity(Level level, Player caster, List<Sign> runes, Vec3 look) {
        super(level, caster, runes, look);
    }
    
    public UpdateableChantCasterEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }
    
    /**
     * Update the sign sequence in real-time without recreating the entity
     */
    public void updateSignSequence(List<Sign> newSigns) {
        try {
            // Create new sequence with all signs
            SignSequence newSequence = new SignSequence();
            for (Sign sign : newSigns) {
                newSequence.addRight(sign);
            }
            
            // Update the entity's sequence using the proper field access
            // Use reflection to set the sequence field
            java.lang.reflect.Field sequenceField = ChantCasterEntity.class.getDeclaredField("sequence");
            sequenceField.setAccessible(true);
            sequenceField.set(this, newSequence);
            
            // Sync to clients by marking data as dirty
            this.getEntityData().set(ChantCasterEntity.SIGNS, newSequence.serializeNbt());
            
            LOGGER.info("Updated ChantCasterEntity sequence with {} signs", newSigns.size());
            
        } catch (Exception e) {
            LOGGER.warn("Failed to update sign sequence: {}", e.getMessage());
        }
    }
    
    /**
     * Check if this entity can be updated (is alive and valid)
     */
    public boolean canBeUpdated() {
        return this.isAlive() && !this.isRemoved();
    }
}
