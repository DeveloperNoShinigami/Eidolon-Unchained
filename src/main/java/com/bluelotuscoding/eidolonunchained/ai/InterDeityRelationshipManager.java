package com.bluelotuscoding.eidolonunchained.ai;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

/**
 * Manages inter-deity relationships and opinions
 * Provides gods with knowledge about each other for more immersive conversations
 */
public class InterDeityRelationshipManager {
    private static final Logger LOGGER = LogManager.getLogger();
    
    /**
     * Generate context about other deities for AI conversations
     * This gives gods opinions and knowledge about their fellow deities
     */
    public static String generateInterDeityContext(ResourceLocation currentDeityId, ServerPlayer player) {
        StringBuilder context = new StringBuilder();
        context.append("\n\n=== KNOWLEDGE OF OTHER DEITIES ===\n");
        
        try {
            AIDeityConfig currentConfig = AIDeityManager.getInstance().getAIConfig(currentDeityId);
            if (currentConfig == null) {
                return context.append("No inter-deity knowledge available.\n").toString();
            }
            
            // Get all AI deity configurations
            var allAIDeities = AIDeityManager.getInstance().getAllConfigs();
            var deityManager = com.bluelotuscoding.eidolonunchained.data.DatapackDeityManager.getInstance();
            
            // Generate opinions about allied deities
            if (!currentConfig.patron_config.alliedDeities.isEmpty()) {
                context.append("Allied Deities: ");
                for (String alliedDeityId : currentConfig.patron_config.alliedDeities) {
                    ResourceLocation alliedId = ResourceLocation.tryParse(alliedDeityId);
                    if (alliedId != null) {
                        var alliedDeity = deityManager.getDeity(alliedId);
                        if (alliedDeity != null) {
                            context.append(alliedDeity.getName()).append(" (").append(alliedId.getPath()).append(") ");
                            // Add relationship status with player
                            double reputation = alliedDeity.getPlayerReputation(player);
                            context.append("[Player rep: ").append((int)reputation).append("] ");
                        }
                    }
                }
                context.append("\n");
                context.append("You view your allies favorably and may speak positively about them.\n");
            }
            
            // Generate opinions about opposing deities
            if (!currentConfig.patron_config.opposingDeities.isEmpty()) {
                context.append("Opposing Deities: ");
                for (String opposingDeityId : currentConfig.patron_config.opposingDeities) {
                    ResourceLocation opposingId = ResourceLocation.tryParse(opposingDeityId);
                    if (opposingId != null) {
                        var opposingDeity = deityManager.getDeity(opposingId);
                        if (opposingDeity != null) {
                            context.append(opposingDeity.getName()).append(" (").append(opposingId.getPath()).append(") ");
                            // Add relationship status with player
                            double reputation = opposingDeity.getPlayerReputation(player);
                            context.append("[Player rep: ").append((int)reputation).append("] ");
                        }
                    }
                }
                context.append("\n");
                context.append("You harbor deep animosity toward these opposing forces and may warn players about them.\n");
            }
            
            // Generate knowledge about neutral deities
            context.append("Other Known Deities: ");
            int neutralCount = 0;
            for (var otherConfig : allAIDeities) {
                ResourceLocation otherId = otherConfig.deity_id;
                if (!otherId.equals(currentDeityId) && 
                    !currentConfig.patron_config.alliedDeities.contains(otherId.toString()) &&
                    !currentConfig.patron_config.opposingDeities.contains(otherId.toString())) {
                    
                    var otherDeity = deityManager.getDeity(otherId);
                    if (otherDeity != null) {
                        context.append(otherDeity.getName()).append(" (").append(otherId.getPath()).append(") ");
                        double reputation = otherDeity.getPlayerReputation(player);
                        context.append("[Player rep: ").append((int)reputation).append("] ");
                        neutralCount++;
                    }
                }
            }
            
            if (neutralCount > 0) {
                context.append("\n");
                context.append("You maintain neutral relationships with these deities - neither ally nor enemy.\n");
            } else {
                context.append("None.\n");
            }
            
            // Add player's patron deity context
            var patronCapability = player.level().getCapability(
                com.bluelotuscoding.eidolonunchained.capability.CapabilityHandler.PATRON_DATA_CAPABILITY);
            
            if (patronCapability.isPresent()) {
                var patronData = patronCapability.orElse(null);
                if (patronData != null) {
                    ResourceLocation playerPatron = patronData.getPatron(player);
                    if (playerPatron != null && !playerPatron.equals(currentDeityId)) {
                        var patronDeity = deityManager.getDeity(playerPatron);
                        if (patronDeity != null) {
                            context.append("\nPlayer's Current Patron: ").append(patronDeity.getName());
                            context.append(" (").append(playerPatron.getPath()).append(")\n");
                            
                            // Add your opinion about player's patron
                            if (currentConfig.patron_config.alliedDeities.contains(playerPatron.toString())) {
                                context.append("This pleases you - they follow your ally.\n");
                            } else if (currentConfig.patron_config.opposingDeities.contains(playerPatron.toString())) {
                                context.append("This concerns you - they follow your enemy.\n");
                            } else {
                                context.append("You acknowledge their choice neutrally.\n");
                            }
                        }
                    } else if (playerPatron != null && playerPatron.equals(currentDeityId)) {
                        context.append("\nThis player is YOUR devoted follower.\n");
                    } else {
                        context.append("\nThis player has no patron deity (godless).\n");
                    }
                }
            }
            
        } catch (Exception e) {
            LOGGER.warn("Failed to generate inter-deity context: {}", e.getMessage());
            context.append("Inter-deity knowledge temporarily unavailable.\n");
        }
        
        return context.toString();
    }
    
    /**
     * Generate detailed deity opinion context for more nuanced relationships
     */
    public static String generateDetailedDeityOpinions(ResourceLocation currentDeityId) {
        StringBuilder context = new StringBuilder();
        context.append("\n=== DETAILED DEITY OPINIONS ===\n");
        
        try {
            AIDeityConfig currentConfig = AIDeityManager.getInstance().getAIConfig(currentDeityId);
            if (currentConfig == null) return context.toString();
            
            // Generate specific personality-based opinions
            generatePersonalityBasedOpinions(context, currentDeityId, currentConfig);
            
        } catch (Exception e) {
            LOGGER.warn("Failed to generate detailed deity opinions: {}", e.getMessage());
        }
        
        return context.toString();
    }
    
    /**
     * Generate opinions based on personality compatibility
     */
    private static void generatePersonalityBasedOpinions(StringBuilder context, ResourceLocation currentDeityId, AIDeityConfig currentConfig) {
        var allConfigs = AIDeityManager.getInstance().getAllConfigs();
        var deityManager = com.bluelotuscoding.eidolonunchained.data.DatapackDeityManager.getInstance();
        
        for (var otherConfig : allConfigs) {
            ResourceLocation otherId = otherConfig.deity_id;
            
            if (otherId.equals(currentDeityId)) continue;
            
            var otherDeity = deityManager.getDeity(otherId);
            if (otherDeity == null) continue;
            
            // Generate opinion based on personality analysis
            String opinion = analyzePersonalityCompatibility(currentConfig, otherConfig);
            if (opinion != null && !opinion.isEmpty()) {
                context.append("Opinion of ").append(otherDeity.getName()).append(": ");
                context.append(opinion).append("\n");
            }
        }
    }
    
    /**
     * Analyze personality compatibility between two deities
     */
    private static String analyzePersonalityCompatibility(AIDeityConfig currentConfig, AIDeityConfig otherConfig) {
        String currentPersonality = currentConfig.personality.toLowerCase();
        String otherPersonality = otherConfig.personality.toLowerCase();
        
        // Nature vs Dark themes
        if (currentPersonality.contains("nature") && otherPersonality.contains("dark")) {
            return "Their dark nature conflicts with your love of growth and light.";
        }
        if (currentPersonality.contains("dark") && otherPersonality.contains("nature")) {
            return "Their attachment to growth and light irritates your shadowy essence.";
        }
        
        // Light vs Dark themes
        if (currentPersonality.contains("light") && otherPersonality.contains("dark")) {
            return "Their darkness is the antithesis of everything you represent.";
        }
        if (currentPersonality.contains("dark") && otherPersonality.contains("light")) {
            return "Their blinding radiance disgusts your shadow-loving soul.";
        }
        
        // Wisdom-based compatibility
        if (currentPersonality.contains("wisdom") && otherPersonality.contains("wisdom")) {
            return "You respect their pursuit of knowledge, though your approaches may differ.";
        }
        
        // Balance vs Chaos themes
        if (currentPersonality.contains("balance") && otherPersonality.contains("chaos")) {
            return "Their chaotic nature disrupts the harmony you seek to maintain.";
        }
        
        // Protection themes
        if (currentPersonality.contains("protect") && otherPersonality.contains("protect")) {
            return "You understand their protective instincts, even if your domains differ.";
        }
        
        // Default neutral
        return "You acknowledge their existence with professional respect.";
    }
    
    /**
     * Check if two deities have conflicting domains
     */
    public static boolean hasConflictingDomains(AIDeityConfig config1, AIDeityConfig config2) {
        // This could be expanded to check domain conflicts from JSON configs
        String p1 = config1.personality.toLowerCase();
        String p2 = config2.personality.toLowerCase();
        
        return (p1.contains("light") && p2.contains("dark")) ||
               (p1.contains("dark") && p2.contains("light")) ||
               (p1.contains("order") && p2.contains("chaos")) ||
               (p1.contains("chaos") && p2.contains("order"));
    }
    
    /**
     * Generate relationship advice for players
     */
    public static String generatePatronAdvice(ResourceLocation currentDeityId, ResourceLocation playerPatron) {
        if (playerPatron == null) {
            return "Consider choosing a patron deity to gain divine protection and guidance.";
        }
        
        if (playerPatron.equals(currentDeityId)) {
            return "You have chosen wisely in following me.";
        }
        
        AIDeityConfig currentConfig = AIDeityManager.getInstance().getAIConfig(currentDeityId);
        if (currentConfig == null) return "";
        
        if (currentConfig.patron_config.alliedDeities.contains(playerPatron.toString())) {
            return "I approve of your choice to follow my ally.";
        } else if (currentConfig.patron_config.opposingDeities.contains(playerPatron.toString())) {
            return "Your allegiance to my enemy troubles me greatly.";
        } else {
            return "Your patron and I maintain neutral relations.";
        }
    }
}
