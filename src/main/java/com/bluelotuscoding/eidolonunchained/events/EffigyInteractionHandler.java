package com.bluelotuscoding.eidolonunchained.events;

import com.bluelotuscoding.eidolonunchained.prayer.PrayerSystem;
import com.bluelotuscoding.eidolonunchained.deity.DatapackDeity;
import com.bluelotuscoding.eidolonunchained.data.DatapackDeityManager;
import elucent.eidolon.common.tile.EffigyTileEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Handles effigy interactions to integrate AI deity responses.
 * 
 * ⚠️ DEPRECATED: This handler is disabled by default.
 * The preferred method is the chant system through AIDeityPrayerSpell.
 * 
 * To enable effigy interactions, set "enableEffigyInteraction" to true 
 * in the eidolonunchained-ai.toml config file.
 */
@Mod.EventBusSubscriber(modid = "eidolonunchained")
public class EffigyInteractionHandler {
    private static final Logger LOGGER = LogManager.getLogger();
    
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onEffigyRightClick(PlayerInteractEvent.RightClickBlock event) {
        // Check if effigy interaction is enabled in config
        if (!com.bluelotuscoding.eidolonunchained.config.AIDeityConfig.ENABLE_EFFIGY_INTERACTION.get()) {
            return; // Feature disabled, let Eidolon handle it
        }
        // Only handle server-side interactions
        if (event.getLevel().isClientSide()) {
            return;
        }
        
        // Only handle main hand interactions
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        
        // Check if the player is a server player
        if (!(event.getEntity() instanceof ServerPlayer)) {
            return;
        }
        
        ServerPlayer player = (ServerPlayer) event.getEntity();
        BlockPos pos = event.getPos();
        BlockState state = event.getLevel().getBlockState(pos);
        
        // Check if this is an effigy block
        if (!isEffigyBlock(state)) {
            return;
        }
        
        // Get the effigy tile entity
        if (!(event.getLevel().getBlockEntity(pos) instanceof EffigyTileEntity)) {
            return;
        }
        
        EffigyTileEntity effigy = (EffigyTileEntity) event.getLevel().getBlockEntity(pos);
        
        // Try to determine which deity this effigy represents
        ResourceLocation deityId = getEffigyDeity(effigy, pos);
        if (deityId == null) {
            return; // Let Eidolon handle unknown effigy types
        }
        
        // Check if the effigy is ready for prayer (respects Eidolon's cooldown)
        if (!effigy.ready()) {
            return; // Let Eidolon handle the cooldown message
        }
        
        // Try to handle with AI deity system
        boolean handled = PrayerSystem.handleEffigyInteraction(player, deityId);
        
        if (handled) {
            // Trigger the effigy's pray() method to respect Eidolon's cooldown system
            effigy.pray();
            
            // Cancel the event to prevent Eidolon's default handling
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
            
            LOGGER.debug("AI deity system handled effigy interaction for player {} with deity {}", 
                player.getName().getString(), deityId);
        }
        // If not handled, let Eidolon's default system take over
    }
    
    /**
     * Check if the given block state represents an effigy
     */
    private static boolean isEffigyBlock(BlockState state) {
        ResourceLocation blockName = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(state.getBlock());
        if (blockName == null) {
            return false;
        }
        
        // Check for Eidolon's effigy blocks
        return blockName.toString().contains("effigy");
    }
    
    /**
     * Determine which deity this effigy represents
     * This is a simplified approach - in a full implementation, you might want to:
     * 1. Check NBT data on the effigy
     * 2. Look at nearby altar configuration
     * 3. Use item used to create the effigy
     * 4. Check biome or structure context
     */
    private static ResourceLocation getEffigyDeity(EffigyTileEntity effigy, BlockPos pos) {
        // For now, we'll try to match against our datapack deities
        // In a real implementation, you'd want a more sophisticated mapping system
        
        // Check if there's a specific deity configured for this location
        // This could be done through:
        // 1. NBT data stored on the effigy
        // 2. Nearby blocks or structures
        // 3. Biome-based deity assignment
        // 4. Player-configured deity shrines
        
        // For demonstration, let's check our nature deity
        DatapackDeity natureDeity = DatapackDeityManager.getDeity(new ResourceLocation("eidolonunchained", "nature_deity"));
        if (natureDeity != null) {
            return natureDeity.getId();
        }
        
        // Could also check for other patterns:
        // - Dark deity for unholy effigies
        // - Light deity for blessed effigies  
        // - Biome-specific deities (forest=nature, desert=sun, etc.)
        // - War deity for effigies near battlefields
        // etc.
        
        return null; // Unknown deity, let Eidolon handle it
    }
}
