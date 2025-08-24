package com.bluelotuscoding.eidolonunchained.ai;

/**
 * API settings container
 */
public class APISettings {
    public String apiKeyEnv = "GEMINI_API_KEY";
    public String model = "gemini-1.5-pro";
    public int timeoutSeconds = 30;
    public GenerationConfig generationConfig = new GenerationConfig();
    public SafetySettings safetySettings = new SafetySettings();
}
