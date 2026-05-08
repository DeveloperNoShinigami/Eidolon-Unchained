package com.bluelotuscoding.eidolonunchained.util;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Centralized Gson instance configured to avoid reflecting into JDK internals
 * like Thread/Executor, preventing Java module access errors without JVM flags.
 */
public final class JsonUtils {
    private JsonUtils() {}

    private static final ExclusionStrategy RUNTIME_EXCLUSION = new ExclusionStrategy() {
        @Override
        public boolean shouldSkipField(FieldAttributes f) {
            Class<?> type = f.getDeclaredClass();
            String fieldName = f.getName();
            if (type == null) return false;

            // Exclude specific fields that cause circular references in mod classes
            if ("playerRewardHistory".equals(fieldName)) return true;
            if ("stageTitles".equals(fieldName)) return true;
            if ("stageRewards".equals(fieldName)) return true;
            if ("prayerTypes".equals(fieldName)) return true;

            // Exclude any field containing UUID maps that could create cycles
            if (fieldName.toLowerCase().contains("history") && java.util.Map.class.isAssignableFrom(type)) return true;
            if (fieldName.toLowerCase().contains("cache") && java.util.Map.class.isAssignableFrom(type)) return true;

            // Exclude problematic runtime types from serialization
            if (Thread.class.isAssignableFrom(type)) return true;
            if (ExecutorService.class.isAssignableFrom(type)) return true;
            if (ScheduledExecutorService.class.isAssignableFrom(type)) return true;
            if (ClassLoader.class.isAssignableFrom(type)) return true;

            // Exclude java.lang.ref.Reference types (WeakReference, SoftReference, etc.)
            if (java.lang.ref.Reference.class.isAssignableFrom(type)) return true;

            // Avoid serializing any java.lang/reflect proxies by default
            Package p = type.getPackage();
            String pn = p != null ? p.getName() : "";
            if (pn.startsWith("java.lang.reflect")) return true;
            if (pn.startsWith("java.lang.ref")) return true;
            if (pn.startsWith("java.util.concurrent")) return true;

            return false;
        }

        @Override
        public boolean shouldSkipClass(Class<?> clazz) {
            if (clazz == null) return false;
            if (Thread.class.isAssignableFrom(clazz)) return true;
            if (ExecutorService.class.isAssignableFrom(clazz)) return true;
            if (ScheduledExecutorService.class.isAssignableFrom(clazz)) return true;
            if (ClassLoader.class.isAssignableFrom(clazz)) return true;
            if (java.lang.ref.Reference.class.isAssignableFrom(clazz)) return true;

            Package p = clazz.getPackage();
            String pn = p != null ? p.getName() : "";
            if (pn.startsWith("java.lang.ref")) return true;
            if (pn.startsWith("java.util.concurrent")) return true;

            return false;
        }
    };

    private static final TypeAdapterFactory OPTIONAL_ADAPTER_FACTORY = new TypeAdapterFactory() {
        @SuppressWarnings("unchecked")
        @Override
        public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
            if (!java.util.Optional.class.isAssignableFrom(type.getRawType())) return null;

            java.lang.reflect.Type[] typeArgs = ((java.lang.reflect.ParameterizedType) type.getType()).getActualTypeArguments();
            final TypeAdapter<Object> elementAdapter = (TypeAdapter<Object>) gson.getAdapter(TypeToken.get(typeArgs[0]));

            return (TypeAdapter<T>) new TypeAdapter<java.util.Optional<?>>() {
                @Override
                public void write(JsonWriter out, java.util.Optional<?> value) throws java.io.IOException {
                    if (value == null || value.isEmpty()) {
                        out.nullValue();
                    } else {
                        elementAdapter.write(out, value.get());
                    }
                }

                @Override
                public java.util.Optional<?> read(JsonReader in) throws java.io.IOException {
                    if (in.peek() == JsonToken.NULL) {
                        in.nextNull();
                        return java.util.Optional.empty();
                    }
                    return java.util.Optional.ofNullable(elementAdapter.read(in));
                }
            };
        }
    };

    public static final Gson GSON = new GsonBuilder()
            .addSerializationExclusionStrategy(RUNTIME_EXCLUSION)
            .registerTypeAdapterFactory(OPTIONAL_ADAPTER_FACTORY)
            .disableHtmlEscaping()
            .setLenient()  // Allow malformed JSON to be more forgiving
            .setPrettyPrinting()  // Make debugging easier
            .create();
}
