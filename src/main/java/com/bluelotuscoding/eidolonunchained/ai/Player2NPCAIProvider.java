package com.bluelotuscoding.eidolonunchained.ai;

import com.bluelotuscoding.eidolonunchained.integration.gemini.GeminiAPIClient;
import com.bluelotuscoding.eidolonunchained.integration.player2ai.Player2NPCClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;

/**
 * Player2 NPC AI provider that uses the correct NPC API workflow
 * This replaces the broken OpenAI-compatible chat completions approach
 */
public class Player2NPCAIProvider implements AIProviderFactory.AIProvider {
    private static final Logger LOGGER = LogManager.getLogger();
    private final Player2NPCClient client;

    public Player2NPCAIProvider(Player2NPCClient client) {
        this.client = client;
    }

    @Override
    public CompletableFuture<GeminiAPIClient.AIResponse> generateResponse(
            String prompt, String personality, String context,
            GenerationConfig genConfig, SafetySettings safetySettings) {

        // Extract deity ID and player UUID from context
        String characterId = extractDeityId(context);
        String playerUUID = extractPlayerUUID(context);
        String playerName = extractPlayerName(context);

        LOGGER.info("Player2NPC: Generating response for deity '{}', player '{}' ({})",
                   characterId, playerName, playerUUID);

        return client.generateResponse(characterId, prompt, personality, playerUUID, playerName, genConfig)
                .thenApply(response -> {
                    LOGGER.info("Player2NPC: Successfully generated response for deity '{}'", characterId);
                    return new GeminiAPIClient.AIResponse(true, response, Collections.emptyList());
                })
                .exceptionally(throwable -> {
                    LOGGER.warn("Player2NPC: Failed to generate response for deity '{}': {}",
                               characterId, throwable.getMessage());
                    // Return a more specific fallback based on the error
                    String fallbackMessage = "The deity's attention wavers...";
                    if (throwable.getMessage() != null) {
                        if (throwable.getMessage().contains("authentication")) {
                            fallbackMessage = "The divine connection requires proper authentication. Use /eidolon-unchained player2ai login device";
                        } else if (throwable.getMessage().contains("quota") || throwable.getMessage().contains("credits")) {
                            fallbackMessage = "The deity's voice grows faint as divine energy wanes...";
                        }
                    }
                    return new GeminiAPIClient.AIResponse(false, fallbackMessage, Collections.emptyList());
                });
    }

    @Override
    public String getProviderName() {
        return "Player2 NPC API";
    }

    @Override
    public boolean isAvailable() {
        return client.isAvailable();
    }

    /**
     * Extract deity ID from context string
     */
    private String extractDeityId(String context) {
        if (context != null && context.contains("deity:")) {
            String[] parts = context.split(",");
            for (String part : parts) {
                if (part.startsWith("deity:")) {
                    String deityId = part.substring("deity:".length());
                    // Remove namespace if present for cleaner NPC naming
                    if (deityId.contains(":")) {
                        deityId = deityId.substring(deityId.lastIndexOf(":") + 1);
                    }
                    return deityId;
                }
            }
        }
        return "unknown_deity";
    }

    /**
     * Extract player UUID from context string
     */
    private String extractPlayerUUID(String context) {
        if (context != null && context.contains("player:")) {
            String[] parts = context.split(",");
            for (String part : parts) {
                if (part.startsWith("player:")) {
                    return part.substring("player:".length());
                }
            }
        }
        return "unknown_player";
    }

    /**
     * Extract player name from context string
     */
    private String extractPlayerName(String context) {
        if (context != null && context.contains("name:")) {
            String[] parts = context.split(",");
            for (String part : parts) {
                if (part.startsWith("name:")) {
                    return part.substring("name:".length());
                }
            }
        }
        // Fallback: try to extract from player UUID or use default
        return "Player";
    }
}