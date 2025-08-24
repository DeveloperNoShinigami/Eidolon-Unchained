package com.bluelotuscoding.eidolonunchained.ai;

/**
 * Generation configuration for AI API calls
 */
public class GenerationConfig {
    public float temperature = 0.7f;
    public int max_output_tokens = 1000;
    public int top_k = 40;
    public float top_p = 0.95f;
}
