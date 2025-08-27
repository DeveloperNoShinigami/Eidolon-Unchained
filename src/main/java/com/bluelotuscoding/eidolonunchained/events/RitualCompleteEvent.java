package com.bluelotuscoding.eidolonunchained.events;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.Event;

/**
 * Event fired when a player completes a ritual
 */
public class RitualCompleteEvent extends Event {
    private final ServerPlayer player;
    private final ResourceLocation ritualId;
    private final boolean successful;
    
    public RitualCompleteEvent(ServerPlayer player, ResourceLocation ritualId, boolean successful) {
        this.player = player;
        this.ritualId = ritualId;
        this.successful = successful;
    }
    
    public ServerPlayer getPlayer() {
        return player;
    }
    
    public ResourceLocation getRitualId() {
        return ritualId;
    }
    
    public boolean isSuccessful() {
        return successful;
    }
}
