package com.bluelotuscoding.eidolonunchained.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * MINIMAL datapack synchronization packet
 * Complex serialization disabled to prevent StackOverflowError crashes
 * Client works with local data instead of synced server data
 */
public class DatapackSyncPacket {

    // Empty data maps - no actual synchronization to prevent crashes
    private final Map<ResourceLocation, String> deityData = new HashMap<>();
    private final Map<ResourceLocation, String> chantData = new HashMap<>();
    private final Map<ResourceLocation, String> codexData = new HashMap<>();
    private final Map<ResourceLocation, String> aiDeityData = new HashMap<>();
    private final Map<ResourceLocation, String> researchChapterData = new HashMap<>();
    private final Map<ResourceLocation, String> researchEntryData = new HashMap<>();

    /**
     * Constructor for creating packet on server
     */
    public DatapackSyncPacket() {
        collectServerData();
    }

    /**
     * Create a sync packet from server data (safe version that doesn't crash)
     */
    public static DatapackSyncPacket createFromServer() {
        try {
            System.out.println("🔥 DatapackSyncPacket.createFromServer() called - starting packet creation");
            DatapackSyncPacket packet = new DatapackSyncPacket();
            System.out.println("🔥 DatapackSyncPacket.createFromServer() completed - packet created successfully");
            return packet;
        } catch (Exception e) {
            System.err.println("🔥 DatapackSyncPacket.createFromServer() FAILED: " + e.getMessage());
            e.printStackTrace();
            return new DatapackSyncPacket();
        }
    }

    /**
     * Constructor for packet decoding
     */
    public DatapackSyncPacket(Map<ResourceLocation, String> deityData,
                             Map<ResourceLocation, String> chantData,
                             Map<ResourceLocation, String> codexData,
                             Map<ResourceLocation, String> aiDeityData,
                             Map<ResourceLocation, String> researchChapterData,
                             Map<ResourceLocation, String> researchEntryData) {
        // Store only AI deity data to prevent circular reference issues with other data
        if (aiDeityData != null) {
            this.aiDeityData.putAll(aiDeityData);
        }
        System.out.println("DatapackSyncPacket: Packet created with " + this.aiDeityData.size() + " AI configs");
    }

    private void collectServerData() {
        try {
            System.out.println("DatapackSyncPacket: Starting data collection for essential AI configs");

            // Only collect AI deity configuration data - this is essential for API calls
            try {
                // Get AI configurations from AIDeityManager if available
                com.bluelotuscoding.eidolonunchained.ai.AIDeityManager manager =
                    com.bluelotuscoding.eidolonunchained.ai.AIDeityManager.getInstance();

                System.out.println("DatapackSyncPacket: AIDeityManager instance retrieved: " + (manager != null));

                if (manager != null) {
                    var allConfigs = manager.getAllConfigs();
                    System.out.println("DatapackSyncPacket: Found " + allConfigs.size() + " AI configs from manager");

                    // Collect only basic AI config data (avoiding complex nested objects)
                    for (com.bluelotuscoding.eidolonunchained.ai.AIDeityConfig config : allConfigs) {
                        if (config != null && config.deity_id != null) {
                            try {
                                System.out.println("DatapackSyncPacket: Processing AI config for deity: " + config.deity_id);
                                System.out.println("  - AI Provider: " + config.ai_provider);
                                System.out.println("  - Model: " + config.model);

                                // Create minimal JSON representation
                                com.google.gson.JsonObject minimalConfig = new com.google.gson.JsonObject();
                                minimalConfig.addProperty("deity", config.deity_id.toString());
                                if (config.ai_provider != null) {
                                    minimalConfig.addProperty("ai_provider", config.ai_provider);
                                }
                                if (config.model != null) {
                                    minimalConfig.addProperty("model", config.model);
                                }
                                if (config.personality != null && config.personality.length() < 500) {
                                    // Truncate personality to avoid large payloads
                                    minimalConfig.addProperty("personality", config.personality.substring(0, Math.min(500, config.personality.length())));
                                }

                                // Store as string to avoid circular references
                                String jsonString = minimalConfig.toString();
                                aiDeityData.put(config.deity_id, jsonString);
                                System.out.println("DatapackSyncPacket: Successfully collected AI config for: " + config.deity_id);
                            } catch (Exception ex) {
                                System.err.println("Failed to serialize AI config for " + config.deity_id + ": " + ex.getMessage());
                                ex.printStackTrace();
                            }
                        } else {
                            System.out.println("DatapackSyncPacket: Skipping null config or config with null deity_id");
                        }
                    }
                    System.out.println("DatapackSyncPacket: Collection complete. Total collected: " + aiDeityData.size() + " AI configurations");
                } else {
                    System.err.println("DatapackSyncPacket: AIDeityManager instance is null - cannot collect AI configs");
                }
            } catch (Exception ex) {
                System.err.println("DatapackSyncPacket: Failed to collect AI deity data: " + ex.getMessage());
                ex.printStackTrace();
            }

        } catch (Exception e) {
            System.err.println("DatapackSyncPacket: Error during data collection: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Encode packet data to network buffer
     */
    public static void encode(DatapackSyncPacket packet, FriendlyByteBuf buffer) {
        try {
            // Write empty maps for most data to prevent serialization issues
            buffer.writeInt(0); // deityData size
            buffer.writeInt(0); // chantData size
            buffer.writeInt(0); // codexData size

            // Write AI deity data (essential for API calls)
            buffer.writeInt(packet.aiDeityData.size());
            for (Map.Entry<ResourceLocation, String> entry : packet.aiDeityData.entrySet()) {
                buffer.writeResourceLocation(entry.getKey());
                buffer.writeUtf(entry.getValue(), 32767); // Max Minecraft string length
            }

            buffer.writeInt(0); // researchChapterData size
            buffer.writeInt(0); // researchEntryData size

            System.out.println("DatapackSyncPacket: Encoded packet with " + packet.aiDeityData.size() + " AI configs");
        } catch (Exception e) {
            System.err.println("Failed to encode DatapackSyncPacket: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Decode packet data from network buffer
     */
    public static DatapackSyncPacket decode(FriendlyByteBuf buffer) {
        try {
            // Read the empty maps
            int deitySize = buffer.readInt();
            int chantSize = buffer.readInt();
            int codexSize = buffer.readInt();

            // Read AI deity data
            int aiSize = buffer.readInt();
            Map<ResourceLocation, String> aiData = new HashMap<>();
            for (int i = 0; i < aiSize; i++) {
                ResourceLocation id = buffer.readResourceLocation();
                String data = buffer.readUtf(32767);
                aiData.put(id, data);
            }

            int researchChapterSize = buffer.readInt();
            int researchEntrySize = buffer.readInt();

            System.out.println("DatapackSyncPacket: Decoded packet with " + aiSize + " AI configs");

            return new DatapackSyncPacket(new HashMap<>(), new HashMap<>(), new HashMap<>(),
                                        aiData, new HashMap<>(), new HashMap<>());
        } catch (Exception e) {
            System.err.println("Failed to decode DatapackSyncPacket: " + e.getMessage());
            e.printStackTrace();
            return new DatapackSyncPacket();
        }
    }

    /**
     * Handle packet on receiving side
     */
    public static void handle(DatapackSyncPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            try {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    System.out.println("DatapackSyncPacket: Processing AI config sync on client");

                    // Process only AI deity data to enable API calls
                    if (!packet.aiDeityData.isEmpty()) {
                        processAIConfigData(packet.aiDeityData);
                        System.out.println("DatapackSyncPacket: Processed " + packet.aiDeityData.size() + " AI configs on client");
                    } else {
                        System.out.println("DatapackSyncPacket: No AI configs to sync");
                    }
                });

                System.out.println("DatapackSyncPacket: AI config sync packet handled successfully");
            } catch (Exception e) {
                System.err.println("Failed to handle DatapackSyncPacket: " + e.getMessage());
                e.printStackTrace();
            }
        });

        context.setPacketHandled(true);
    }

    // Stub methods for ChunkedDataSyncPacket compatibility
    public static void processDeityData(Map<ResourceLocation, String> data) {
        System.out.println("DatapackSyncPacket: processDeityData called - minimal implementation");
        // No processing to prevent circular reference issues
    }

    public static void processChantData(Map<ResourceLocation, String> data) {
        System.out.println("DatapackSyncPacket: processChantData called - minimal implementation");
        // No processing to prevent circular reference issues
    }

    public static void processCodexData(Map<ResourceLocation, String> data) {
        System.out.println("DatapackSyncPacket: processCodexData called - minimal implementation");
        // No processing to prevent circular reference issues
    }

    public static void processAIConfigData(Map<ResourceLocation, String> data) {
        System.out.println("DatapackSyncPacket: processAIConfigData called with " + data.size() + " entries");
        try {
            // Process AI configuration data on client side
            for (Map.Entry<ResourceLocation, String> entry : data.entrySet()) {
                ResourceLocation configId = entry.getKey();
                String jsonData = entry.getValue();

                try {
                    // Parse the JSON data and register with AIDeityManager
                    com.google.gson.JsonElement element = com.google.gson.JsonParser.parseString(jsonData);
                    if (element.isJsonObject()) {
                        // Extract deity ID from the JSON config
                        com.google.gson.JsonObject jsonObj = element.getAsJsonObject();
                        if (jsonObj.has("deity")) {
                            String deityIdString = jsonObj.get("deity").getAsString();
                            net.minecraft.resources.ResourceLocation deityId = net.minecraft.resources.ResourceLocation.tryParse(deityIdString);

                            if (deityId != null) {
                                // If AIDeityManager already has a fully-loaded config (from client resource pack
                                // reload), don't overwrite it with the stripped server-sync version which lacks
                                // tts_config and other fields parsed only on the client.
                                com.bluelotuscoding.eidolonunchained.ai.AIDeityManager aiManager =
                                    com.bluelotuscoding.eidolonunchained.ai.AIDeityManager.getInstance();
                                if (aiManager.getAIConfig(deityId) != null &&
                                        aiManager.getAIConfig(deityId).tts_config != null) {
                                    System.out.println("DatapackSyncPacket: Skipping sync for " + deityId + " — full config already loaded");
                                    continue;
                                }

                                // Full parse via AIDeityManager so tts_config and all other fields are populated
                                aiManager.handleSyncedAIConfig(deityId, jsonObj);
                                System.out.println("DatapackSyncPacket: Registered client AI config for deity: " + deityId);
                            }
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Failed to parse AI config " + configId + ": " + e.getMessage());
                }
            }
            System.out.println("DatapackSyncPacket: AI config processing completed");
        } catch (Exception e) {
            System.err.println("Failed to process AI config data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void processResearchChapterData(Map<ResourceLocation, String> data) {
        System.out.println("DatapackSyncPacket: processResearchChapterData called - minimal implementation");
        // No processing to prevent circular reference issues
    }

    public static void processResearchEntryData(Map<ResourceLocation, String> data) {
        System.out.println("DatapackSyncPacket: processResearchEntryData called - minimal implementation");
        // No processing to prevent circular reference issues
    }
}