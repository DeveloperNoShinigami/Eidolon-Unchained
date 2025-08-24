package com.bluelotuscoding.eidolonunchained.ai;

import java.util.List;

/**
 * API settings container
 */
public class APISettings {
    public String apiKeyEnv = "GEMINI_API_KEY";
    public String model = "gemini-1.5-pro";
    public int timeoutSeconds = 30;
    public GenerationConfig generationConfig = new GenerationConfig();
    public SafetySettings safetySettings = new SafetySettings();
    
    // Chant sequence for Eidolon integration
    public List<String> chantSequence = null; // If null, will use default based on personality
}
