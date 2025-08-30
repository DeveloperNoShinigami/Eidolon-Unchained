package com.bluelotuscoding.eidolonunchained.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.util.INBTSerializable;
import com.bluelotuscoding.eidolonunchained.data.DatapackDeityManager;
import com.bluelotuscoding.eidolonunchained.deity.DatapackDeity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Implementation of patron data capability
 */
public class PatronData implements IPatronData, INBTSerializable<CompoundTag> {
    
    private final Map<UUID, ResourceLocation> playerPatrons = new HashMap<>();
    private final Map<UUID, String> playerTitles = new HashMap<>();
    
    @Override
    public void setPatron(ServerPlayer player, ResourceLocation deityId) {
        UUID playerId = player.getUUID();
        if (deityId == null) {
            playerPatrons.remove(playerId);
            playerTitles.remove(playerId);
        } else {
            playerPatrons.put(playerId, deityId);
            updateTitle(player);
        }
    }
    
    @Override
    public ResourceLocation getPatron(ServerPlayer player) {
        return playerPatrons.get(player.getUUID());
    }
    
    @Override
    public void updateTitle(ServerPlayer player) {
        ResourceLocation patron = getPatron(player);
        if (patron == null) {
            setTitle(player, null);
            return;
        }
        
        DatapackDeity deity = DatapackDeityManager.getDeity(patron);
        if (deity != null) {
            deity.updatePatronTitle(player);
        }
    }
    
    @Override
    public String getTitle(ServerPlayer player) {
        return playerTitles.get(player.getUUID());
    }
    
    @Override
    public void setTitle(ServerPlayer player, String title) {
        UUID playerId = player.getUUID();
        if (title == null || title.isEmpty()) {
            playerTitles.remove(playerId);
        } else {
            playerTitles.put(playerId, title);
        }
    }
    
    @Override
    public double getReputationModifier(ServerPlayer player, ResourceLocation deityId) {
        ResourceLocation patron = getPatron(player);
        if (patron == null) {
            return 1.0; // No modifier if no patron
        }
        
        if (patron.equals(deityId)) {
            return 1.2; // 20% bonus for patron deity
        }
        
        if (areOpposingDeities(patron, deityId)) {
            return 0.7; // 30% penalty for opposing deities
        }
        
        return 1.0; // No modifier for neutral deities
    }
    
    @Override
    public boolean areOpposingDeities(ResourceLocation deity1, ResourceLocation deity2) {
        // Basic implementation - can be expanded based on deity relationships
        // For now, just check if they're different deities (simple opposition)
        return deity1 != null && deity2 != null && !deity1.equals(deity2);
    }
    
    @Override
    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        
        CompoundTag patronsTag = new CompoundTag();
        playerPatrons.forEach((playerId, deity) -> {
            patronsTag.putString(playerId.toString(), deity.toString());
        });
        nbt.put("patrons", patronsTag);
        
        CompoundTag titlesTag = new CompoundTag();
        playerTitles.forEach((playerId, title) -> {
            titlesTag.putString(playerId.toString(), title);
        });
        nbt.put("titles", titlesTag);
        
        return nbt;
    }
    
    @Override
    public void deserializeNBT(CompoundTag nbt) {
        playerPatrons.clear();
        playerTitles.clear();
        
        if (nbt.contains("patrons")) {
            CompoundTag patronsTag = nbt.getCompound("patrons");
            patronsTag.getAllKeys().forEach(key -> {
                UUID playerId = UUID.fromString(key);
                ResourceLocation deity = new ResourceLocation(patronsTag.getString(key));
                playerPatrons.put(playerId, deity);
            });
        }
        
        if (nbt.contains("titles")) {
            CompoundTag titlesTag = nbt.getCompound("titles");
            titlesTag.getAllKeys().forEach(key -> {
                UUID playerId = UUID.fromString(key);
                String title = titlesTag.getString(key);
                playerTitles.put(playerId, title);
            });
        }
    }
}
