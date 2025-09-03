package com.bluelotuscoding.eidolonunchained.spells;

import com.bluelotuscoding.eidolonunchained.data.DatapackDeityManager;
import com.bluelotuscoding.eidolonunchained.deity.DatapackDeity;
import com.bluelotuscoding.eidolonunchained.ai.AIDeityManager;
import com.bluelotuscoding.eidolonunchained.ai.AIDeityConfig;
import com.bluelotuscoding.eidolonunchained.chat.DeityChat;
import elucent.eidolon.api.spells.Sign;
import elucent.eidolon.common.spell.PrayerSpell;
import elucent.eidolon.api.deity.Deity;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;

/**
 * Custom prayer spell that integrates with AI deity conversation system.
 * When performed, opens a chat interface with the AI deity instead of 
 * executing standard prayer effects.
 */
public class AIDeityPrayerSpell extends PrayerSpell {
    private final ResourceLocation aiDeityId;
    
    public AIDeityPrayerSpell(ResourceLocation name, ResourceLocation aiDeityId, Sign... signs) {
        // Use a dummy deity for the parent class, we'll handle the AI deity separately
        super(name, createDummyDeity(aiDeityId), signs);
        this.aiDeityId = aiDeityId;
    }
    
    public AIDeityPrayerSpell(ResourceLocation name, ResourceLocation aiDeityId, int reputation, double powerMult, Sign... signs) {
        super(name, createDummyDeity(aiDeityId), reputation, powerMult, signs);
        this.aiDeityId = aiDeityId;
    }
    
    public AIDeityPrayerSpell(ResourceLocation name, ResourceLocation aiDeityId, int cost, int reputation, double powerMult, Sign... signs) {
        super(name, createDummyDeity(aiDeityId), cost, reputation, powerMult, signs);
        this.aiDeityId = aiDeityId;
    }
    
    private static Deity createDummyDeity(ResourceLocation aiDeityId) {
        // Create a minimal deity implementation for the parent class
        return new Deity(aiDeityId, 128, 128, 128) {
            @Override
            public void onReputationUnlock(Player player, ResourceLocation lock) {
                // No-op for AI deities
            }
            
            @Override
            public void onReputationLock(Player player, ResourceLocation lock) {
                // No-op for AI deities
            }
        };
    }
    
    @Override
    public void cast(Level world, BlockPos pos, Player player) {
        if (world.isClientSide || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        
        // Check if the chant system is enabled in config
        if (!com.bluelotuscoding.eidolonunchained.config.EidolonUnchainedConfig.COMMON.enableChantSystem.get()) {
            // Fall back to standard prayer behavior if chant system is disabled
            super.cast(world, pos, player);
            return;
        }
        
        // Check if the AI deity system can handle this prayer
        DatapackDeity aiDeity = DatapackDeityManager.getDeity(aiDeityId);
        if (aiDeity == null) {
            player.sendSystemMessage(Component.translatable("eidolonunchained.spell.deity_unknown", aiDeityId));
            return;
        }
        
        // Check if AI is enabled for this deity
        AIDeityConfig aiConfig = AIDeityManager.getInstance().getAIConfig(aiDeityId);
        if (aiConfig == null) {
            // Fall back to standard prayer behavior
            super.cast(world, pos, player);
            return;
        }
        
        // Verify effigy and altar setup (respects Eidolon's mechanics)
        var effigy = getEffigy(world, pos);
        if (effigy == null) {
            player.sendSystemMessage(Component.translatable("eidolonunchained.spell.no_effigy"));
            return;
        }
        
        if (!effigy.ready()) {
            player.sendSystemMessage(Component.translatable("eidolonunchained.spell.effigy_not_ready"));
            return;
        }
        
        // Check reputation requirements
        if (!reputationCheck(world, player, getBaseRep())) {
            player.sendSystemMessage(Component.translatable("eidolonunchained.spell.insufficient_devotion", aiDeity.getDisplayName()));
            return;
        }
        
        // Trigger the effigy's prayer method to respect cooldowns
        effigy.pray();
        
        // Start AI conversation instead of standard prayer effects
        DeityChat.startConversation(serverPlayer, aiDeityId);
        
        player.sendSystemMessage(Component.translatable("eidolonunchained.spell.deity_listening", aiDeity.getDisplayName()));
    }
    
    public ResourceLocation getAIDeityId() {
        return aiDeityId;
    }
}
