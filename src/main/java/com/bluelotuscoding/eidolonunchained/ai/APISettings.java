package com.bluelotuscoding.eidolonunchained.ai;

import com.bluelotuscoding.eidolonunchained.config.EidolonUnchainedConfig;

import java.util.List;

/**
 * API settings container
 */
public class APISettings {
    public String apiKeyEnv = "GEMINI_API_KEY";
    public String model = null; // Will be initialized from config
    public int timeoutSeconds = 30;
    public GenerationConfig generationConfig = new GenerationConfig();
    public SafetySettings safetySettings = new SafetySettings();
    
    // Chant sequence for Eidolon integration
    public List<String> chantSequence = null; // If null, will use default based on personality
    
    /**
     * Initialize model from config if not set
     */
    public String getModel() {
        if (model == null) {
            return EidolonUnchainedConfig.COMMON.geminiModel.get();
        }
        return model;
    }
}
