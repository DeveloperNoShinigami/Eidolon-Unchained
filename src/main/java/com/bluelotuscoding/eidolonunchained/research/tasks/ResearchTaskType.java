package com.bluelotuscoding.eidolonunchained.research.tasks;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Function;

/**
 * Describes a type of {@link ResearchTask} and provides a factory for decoding
 * instances from JSON.
 */
public record ResearchTaskType(ResourceLocation id, Function<JsonObject, ResearchTask> decoder) {}
