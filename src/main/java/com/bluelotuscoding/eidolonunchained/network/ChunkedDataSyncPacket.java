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
 * CRITICAL FIX: Chunked data sync packet to prevent buffer overflow
 * Replaces the massive DatapackSyncPacket with smaller, manageable chunks
 * Fixes IndexOutOfBoundsException: Index 26 out of bounds for length 5
 */
public class ChunkedDataSyncPacket {
    public enum DataType {
        DEITIES, CHANTS, CODEX, AI_CONFIGS, RESEARCH_CHAPTERS, RESEARCH_ENTRIES
    }
    
    private final DataType dataType;
    private final Map<ResourceLocation, String> data;
    private final int chunkIndex;
    private final int totalChunks;
    private final boolean isLastChunk;
    
    // Client-side chunk assembly
    private static final Map<DataType, Map<ResourceLocation, String>> CLIENT_CHUNKS = new HashMap<>();
    private static final Map<DataType, Integer> EXPECTED_CHUNKS = new HashMap<>();
    private static final Map<DataType, Integer> RECEIVED_CHUNKS = new HashMap<>();
    
    public ChunkedDataSyncPacket(DataType dataType, Map<ResourceLocation, String> data, 
                                int chunkIndex, int totalChunks, boolean isLastChunk) {
        this.dataType = dataType;
        this.data = data;
        this.chunkIndex = chunkIndex;
        this.totalChunks = totalChunks;
        this.isLastChunk = isLastChunk;
    }
    
    public static void encode(ChunkedDataSyncPacket packet, FriendlyByteBuf buffer) {
        try {
            buffer.writeEnum(packet.dataType);
            buffer.writeInt(packet.chunkIndex);
            buffer.writeInt(packet.totalChunks);
            buffer.writeBoolean(packet.isLastChunk);
            
            // Write data entries (limited chunk size)
            buffer.writeInt(packet.data.size());
            packet.data.forEach((id, data) -> {
                buffer.writeResourceLocation(id);
                // CRITICAL: Limit individual entry size to prevent overflow
                String truncatedData = data.length() > 32000 ? data.substring(0, 32000) : data;
                buffer.writeUtf(truncatedData, 32767); // Minecraft's max string length
            });
            
            System.out.println("ChunkedDataSyncPacket: Encoded " + packet.dataType + 
                             " chunk " + packet.chunkIndex + "/" + packet.totalChunks + 
                             " with " + packet.data.size() + " entries");
        } catch (Exception e) {
            System.err.println("Failed to encode chunked sync packet: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public static ChunkedDataSyncPacket decode(FriendlyByteBuf buffer) {
        try {
            DataType dataType = buffer.readEnum(DataType.class);
            int chunkIndex = buffer.readInt();
            int totalChunks = buffer.readInt();
            boolean isLastChunk = buffer.readBoolean();
            
            Map<ResourceLocation, String> data = new HashMap<>();
            int entryCount = buffer.readInt();
            
            for (int i = 0; i < entryCount; i++) {
                ResourceLocation id = buffer.readResourceLocation();
                String entryData = buffer.readUtf(32767);
                data.put(id, entryData);
            }
            
            System.out.println("ChunkedDataSyncPacket: Decoded " + dataType + 
                             " chunk " + chunkIndex + "/" + totalChunks + 
                             " with " + data.size() + " entries");
            
            return new ChunkedDataSyncPacket(dataType, data, chunkIndex, totalChunks, isLastChunk);
        } catch (Exception e) {
            System.err.println("Failed to decode chunked sync packet: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    public static void handle(ChunkedDataSyncPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            // CLIENT-SIDE ONLY
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                handleClientSide(packet);
            });
        });
        context.setPacketHandled(true);
    }
    
    private static void handleClientSide(ChunkedDataSyncPacket packet) {
        try {
            // Initialize chunk tracking for this data type
            CLIENT_CHUNKS.computeIfAbsent(packet.dataType, k -> new HashMap<>());
            EXPECTED_CHUNKS.put(packet.dataType, packet.totalChunks);
            
            // Add this chunk's data
            CLIENT_CHUNKS.get(packet.dataType).putAll(packet.data);
            
            // Update received chunk count
            int receivedCount = RECEIVED_CHUNKS.getOrDefault(packet.dataType, 0) + 1;
            RECEIVED_CHUNKS.put(packet.dataType, receivedCount);
            
            System.out.println("CLIENT: Received " + packet.dataType + " chunk " + 
                             packet.chunkIndex + "/" + packet.totalChunks + 
                             " (" + receivedCount + "/" + packet.totalChunks + " total)");
            
            // Check if we have all chunks for this data type
            if (receivedCount >= packet.totalChunks || packet.isLastChunk) {
                Map<ResourceLocation, String> completeData = CLIENT_CHUNKS.get(packet.dataType);
                System.out.println("CLIENT: Assembling complete " + packet.dataType + 
                                 " data with " + completeData.size() + " entries");
                
                // Process the complete data
                processCompleteDataType(packet.dataType, completeData);
                
                // Clean up
                CLIENT_CHUNKS.remove(packet.dataType);
                EXPECTED_CHUNKS.remove(packet.dataType);
                RECEIVED_CHUNKS.remove(packet.dataType);
            }
        } catch (Exception e) {
            System.err.println("CLIENT: Failed to handle chunked data: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void processCompleteDataType(DataType dataType, Map<ResourceLocation, String> data) {
        try {
            switch (dataType) {
                case DEITIES:
                    DatapackSyncPacket.processDeityData(data);
                    break;
                case CHANTS:
                    DatapackSyncPacket.processChantData(data);
                    break;
                case CODEX:
                    DatapackSyncPacket.processCodexData(data);
                    break;
                case AI_CONFIGS:
                    DatapackSyncPacket.processAIConfigData(data);
                    break;
                case RESEARCH_CHAPTERS:
                    DatapackSyncPacket.processResearchChapterData(data);
                    break;
                case RESEARCH_ENTRIES:
                    DatapackSyncPacket.processResearchEntryData(data);
                    break;
            }
            
            System.out.println("CLIENT: Successfully processed " + dataType + " data");
        } catch (Exception e) {
            System.err.println("CLIENT: Failed to process " + dataType + " data: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
