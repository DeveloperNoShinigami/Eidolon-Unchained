package com.bluelotuscoding.eidolonunchained.capability;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;

/**
 * Capability interface for managing patron deity relationships and titles
 */
public interface IPatronData {
    
    // Capability instance
    Capability<IPatronData> PATRON_DATA = CapabilityManager.get(new CapabilityToken<>(){});
    
    /**
     * Set a player's patron deity
     * @param player The player
     * @param deityId The deity ResourceLocation, or null to remove patron
     */
    void setPatron(ServerPlayer player, ResourceLocation deityId);
    
    /**
     * Get a player's current patron deity
     * @param player The player
     * @return The patron deity ResourceLocation, or null if no patron
     */
    ResourceLocation getPatron(ServerPlayer player);
    
    /**
     * Update a player's title based on their current reputation with their patron
     * @param player The player
     */
    void updateTitle(ServerPlayer player);
    
    /**
     * Get a player's current title
     * @param player The player
     * @return The player's title, or null if no title
     */
    String getTitle(ServerPlayer player);
    
    /**
     * Set a player's title directly
     * @param player The player
     * @param title The title to set
     */
    void setTitle(ServerPlayer player, String title);
    
    /**
     * Get reputation modifier for a deity based on patron relationships
     * @param player The player
     * @param deityId The deity to get modifier for
     * @return Reputation modifier (1.0 = no change, 0.7 = 30% penalty, 1.2 = 20% bonus)
     */
    double getReputationModifier(ServerPlayer player, ResourceLocation deityId);
    
    /**
     * Check if two deities are opposing (for reputation penalties)
     * @param deity1 First deity
     * @param deity2 Second deity
     * @return True if the deities are opposing
     */
    boolean areOpposingDeities(ResourceLocation deity1, ResourceLocation deity2);
    
    // Additional methods needed by PatronSystem
    default boolean hasPatron() { return false; }
    default ResourceLocation getPatronDeity() { return null; }
    default void setPatronDeity(ResourceLocation deity) { }
    default boolean isPatronLocked() { return false; }
    default boolean canSwitchPatron() { return true; }
    default long getTimeSincePatronSwitch() { return 0; }
    default String getCurrentTitle() { return ""; }
    default void setCurrentTitle(String title) { }
    default ResourceLocation getFormerPatron() { return null; }
    default void setFormerPatron(ResourceLocation deity) { }
    default long getPatronSince() { return System.currentTimeMillis(); }
    default void setPatronSince(long time) { }
    default boolean hasRecentConflict(ResourceLocation deity) { return false; }
    default void addConflictWarning(ResourceLocation deity) { }
}
