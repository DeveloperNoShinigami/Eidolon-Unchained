package com.bluelotuscoding.eidolonunchained.debug;

import com.bluelotuscoding.eidolonunchained.integration.EidolonCodexIntegration;
import com.bluelotuscoding.eidolonunchained.integration.EidolonPageConverter;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

/**
 * Debug utilities for testing the codex translation system.
 * Since this deals with client-side codex integration, it's marked as client-only.
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class CodexDebugCommands {
    private static final Logger LOGGER = LogUtils.getLogger();

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
            Commands.literal("eidolonunchained")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("test_translations")
                    .executes(context -> {
                        testTranslations(context.getSource());
                        return 1;
                    }))
                .then(Commands.literal("reload_codex")
                    .executes(context -> {
                        reloadCodex(context.getSource());
                        return 1;
                    }))
        );
    }

    private static void testTranslations(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("Testing translation system..."), false);
        
        // Test some translation keys that actually exist
        String[] testKeys = {
            "eidolonunchained.codex.entry.text_example.title",
            "eidolonunchained.codex.entry.recipe_example.title",
            "eidolonunchained.codex.entry.entity_example.title"
        };
        
        LOGGER.info("=== TRANSLATION TEST START ===");
        for (String key : testKeys) {
            Component translated = Component.translatable(key);
            String result = translated.getString();
            LOGGER.info("Key: '{}' -> Result: '{}'", key, result);
            source.sendSuccess(() -> Component.literal("§7" + key + " §f-> §a" + result), false);
        }
        LOGGER.info("=== TRANSLATION TEST END ===");
        
        source.sendSuccess(() -> Component.literal("Translation test complete - check logs for details"), false);
    }

    private static void reloadCodex(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("Attempting to reload codex integration..."), false);
        
        try {
            // Force re-initialization
            EidolonPageConverter.initialize();
            EidolonCodexIntegration.attemptIntegrationIfNeeded();
            
            source.sendSuccess(() -> Component.literal("§aCodex integration reloaded successfully!"), false);
        } catch (Exception e) {
            LOGGER.error("Failed to reload codex integration", e);
            source.sendFailure(Component.literal("§cFailed to reload codex: " + e.getMessage()));
        }
    }
}
